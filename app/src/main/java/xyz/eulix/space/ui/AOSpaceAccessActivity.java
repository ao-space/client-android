package xyz.eulix.space.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.DIDUtils;
import xyz.eulix.space.did.bean.DIDCredentialBean;
import xyz.eulix.space.did.bean.DIDProviderBean;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.PairingBoxInfo;
import xyz.eulix.space.network.agent.bind.ConnectedNetwork;
import xyz.eulix.space.network.agent.bind.SpaceCreateRequest;
import xyz.eulix.space.network.agent.bind.SpaceCreateResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.presenter.AOSpaceAccessPresenter;
import xyz.eulix.space.ui.bind.BindResultActivity;
import xyz.eulix.space.ui.bind.DiskInitializationActivity;
import xyz.eulix.space.ui.mine.developer.DeveloperOptionsActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.view.dialog.PlatformAddressInputDialog;

public class AOSpaceAccessActivity extends AbsActivity<AOSpaceAccessPresenter.IAOSpaceAccess, AOSpaceAccessPresenter> implements AOSpaceAccessPresenter.IAOSpaceAccess
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback {
    private static final String IS_BIND_PROGRESS = "is_bind_progress";
    private static final int STEP_BIND_PROGRESS_BIND_SPACE_CREATE = 0;
    private static final int STEP_BIND_PROGRESS_SPACE_READY_CHECK = STEP_BIND_PROGRESS_BIND_SPACE_CREATE + 1;
    private static final int STEP_BIND_PROGRESS_DISK_RECOGNITION = STEP_BIND_PROGRESS_SPACE_READY_CHECK + 1;
    private static final int STEP_BIND_PROGRESS_DISK_MANAGEMENT_LIST = STEP_BIND_PROGRESS_DISK_RECOGNITION + 1;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private Button functionText;
    private ImageView titleHeaderImage;
    private TextView titleHeaderText;
    private TextView titleHeaderIntroduction;
    private TextView lanAccessHint;
    private ImageButton internetAccessSwitch;
    private LinearLayout layoutPlatformAddress;
    private TextView tvPlatformAddress;
    private LinearLayout layoutPlatformChange;
    private LinearLayout internetAccessOffHintContainer;
    private LinearLayout loadingButtonContainer;
    private LottieAnimationView loadingAnimation;
    private TextView loadingContent;
    private Dialog closeInternetAccessDialog;
    private Button closeInternetAccessDialogConfirm;
    private Button closeInternetAccessDialogCancel;
    private Dialog internetAccessExceptionDialog;
    private TextView internetAccessExceptionDialogTitle;
    private TextView internetAccessExceptionDialogContent;
    private Button internetAccessExceptionDialogConfirm;
    private AODeviceDiscoveryManager mManager;
    private AOSpaceAccessHandler mHandler;
    private boolean isBindProgress;
    private boolean isBusy;
    private long mExitTime = 0L;
    private int stepBindProgress;
    private PairingBoxInfo mPairingBoxInfo;
    private ReadyCheckResult mReadyCheckResult;
    private AOSpaceAccessBean mAoSpaceAccessBean = new AOSpaceAccessBean();
    private String mPlatformAddressStr;

    static class AOSpaceAccessHandler extends Handler {
        private WeakReference<AOSpaceAccessActivity> aoSpaceAccessActivityWeakReference;

        public AOSpaceAccessHandler(AOSpaceAccessActivity activity) {
            aoSpaceAccessActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AOSpaceAccessActivity activity = aoSpaceAccessActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_ao_space_access);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        titleHeaderImage = findViewById(R.id.title_header_image);
        titleHeaderText = findViewById(R.id.title_header_text);
        titleHeaderIntroduction = findViewById(R.id.title_header_introduction);
        lanAccessHint = findViewById(R.id.lan_access_hint);
        internetAccessSwitch = findViewById(R.id.internet_access_switch);
        layoutPlatformAddress = findViewById(R.id.layout_platform_address);
        tvPlatformAddress = findViewById(R.id.tv_platform_address);
        layoutPlatformChange = findViewById(R.id.layout_change_platform);
        internetAccessOffHintContainer = findViewById(R.id.internet_access_off_hint_container);
        loadingButtonContainer = findViewById(R.id.loading_button_container);
        loadingAnimation = findViewById(R.id.loading_animation);
        loadingContent = findViewById(R.id.loading_content);

        View closeInternetAccessDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_close_internet_access, null);
        closeInternetAccessDialogConfirm = closeInternetAccessDialogView.findViewById(R.id.dialog_confirm);
        closeInternetAccessDialogCancel = closeInternetAccessDialogView.findViewById(R.id.dialog_cancel);
        closeInternetAccessDialog = new BottomDialog(this);
        closeInternetAccessDialog.setCancelable(false);
        closeInternetAccessDialog.setContentView(closeInternetAccessDialogView);

        View internetAccessExceptionDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        internetAccessExceptionDialogTitle = internetAccessExceptionDialogView.findViewById(R.id.dialog_title);
        internetAccessExceptionDialogContent = internetAccessExceptionDialogView.findViewById(R.id.dialog_content);
        internetAccessExceptionDialogConfirm = internetAccessExceptionDialogView.findViewById(R.id.dialog_confirm);
        internetAccessExceptionDialog = new Dialog(this, R.style.EulixDialog);
        internetAccessExceptionDialog.setCancelable(false);
        internetAccessExceptionDialog.setContentView(internetAccessExceptionDialogView);
    }

    @Override
    public void initData() {
        mHandler = new AOSpaceAccessHandler(this);
        handleIntent(getIntent());
        if (isBindProgress) {
            activityId = UUID.randomUUID().toString();
            mManager = AODeviceDiscoveryManager.getInstance();
            mManager.registerCallback(activityId, this);
            stepBindProgress = STEP_BIND_PROGRESS_BIND_SPACE_CREATE;
            AOSpaceAccessBean aoSpaceAccessBean = DataUtil.getAoSpaceAccessBean();
            if (aoSpaceAccessBean == null) {
                mAoSpaceAccessBean.setLanAccess(true);
                mAoSpaceAccessBean.setP2PAccess(true);
                mAoSpaceAccessBean.setInternetAccess(true);
                DataUtil.setAoSpaceAccessBean(mAoSpaceAccessBean);
            } else {
                mAoSpaceAccessBean = aoSpaceAccessBean;
            }
        }
    }

    @Override
    public void initViewData() {
        title.setText("");
        functionText.setText(R.string.run_background);
        titleHeaderImage.setImageResource(R.drawable.image_space_access_2x);
        titleHeaderText.setText(R.string.space_access);
        titleHeaderIntroduction.setVisibility(View.VISIBLE);
        titleHeaderIntroduction.setText(R.string.space_access_introduction);
        if (!isBindProgress && presenter != null) {
            AOSpaceAccessBean aoSpaceAccessBean = presenter.getActiveAOSpaceAccessBean();
            if (aoSpaceAccessBean == null || aoSpaceAccessBean.getLanAccess() == null || aoSpaceAccessBean.getP2PAccess() == null || aoSpaceAccessBean.getInternetAccess() == null) {
                finish();
            } else {
                mAoSpaceAccessBean = aoSpaceAccessBean;
            }
        }
        setLanAccessPattern(mAoSpaceAccessBean.getLanAccess());
        setInternetAccessPattern(mAoSpaceAccessBean.getInternetAccess());
        resetPlatformAddressView(mAoSpaceAccessBean.getInternetAccess(), mAoSpaceAccessBean.getPlatformApiBase());

        internetAccessExceptionDialogTitle.setText(R.string.notice_hint);
        internetAccessExceptionDialogContent.setText(R.string.unable_internet_channel_hint);
        internetAccessExceptionDialogConfirm.setText(R.string.i_know);

        boolean needShowChangLayout = presenter.isActiveAdmin() && !isBindProgress;
        layoutPlatformChange.setVisibility(needShowChangLayout ? View.VISIBLE : View.GONE);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        internetAccessSwitch.setOnClickListener(this);
        setLoadingButtonContainerPattern(false);
        closeInternetAccessDialogCancel.setOnClickListener(v -> dismissCloseInternetAccessDialog());
        closeInternetAccessDialogConfirm.setOnClickListener(v -> {
            dismissCloseInternetAccessDialog();
            setInternetAccessPattern(Boolean.FALSE);
        });
        internetAccessExceptionDialogConfirm.setOnClickListener(v -> dismissInternetAccessExceptionDialog());
        tvPlatformAddress.setOnClickListener(v -> {
            if (isBindProgress) {
                EulixDialogUtil.showPlatformInputDialog(this, mPlatformAddressStr, new PlatformAddressInputDialog.OnConfirmListener() {
                    @Override
                    public void onResult(String address) {
                        mPlatformAddressStr = address;
                        mAoSpaceAccessBean.setPlatformApiBase(mPlatformAddressStr);
                        resetPlatformAddressView(mAoSpaceAccessBean.getInternetAccess(), mPlatformAddressStr);
                    }
                });
            }
        });
        layoutPlatformChange.setOnClickListener(v -> {
            Intent developerOptionsIntent = new Intent(AOSpaceAccessActivity.this, DeveloperOptionsActivity.class);
            startActivity(developerOptionsIntent);
        });
    }

    private void resetPlatformAddressView(boolean isInternetEnable, String address) {
        if (TextUtils.isEmpty(address)) {
            tvPlatformAddress.setText(getResources().getString(R.string.input_platform_url_hint));
            tvPlatformAddress.setTextColor(getResources().getColor(R.color.c_ffbcbfcd));
        } else {
            tvPlatformAddress.setText(address);
            tvPlatformAddress.setTextColor(getResources().getColor(R.color.black_ff333333));
        }
        if (isInternetEnable && TextUtils.isEmpty(address)) {
            loadingButtonContainer.setEnabled(false);
            loadingButtonContainer.setBackgroundResource(R.drawable.background_ffdfe0e5_rectangle_10);
        } else {
            loadingButtonContainer.setEnabled(true);
            loadingButtonContainer.setBackgroundResource(R.drawable.background_ff337aff_ff16b9ff_rectangle_10);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            isBindProgress = intent.getBooleanExtra(IS_BIND_PROGRESS, false);
        }
    }

    private void setHintPattern(TextView hintView, boolean isOn) {
        if (hintView != null) {
            hintView.setBackgroundResource(isOn ? R.drawable.background_ffdae6ff_rectangle_secondary_diagonal_10
                    : R.drawable.background_ffe4e7ed_rectangle_secondary_diagonal_10);
            hintView.setText(isOn ? R.string.binding_enable : R.string.binding_disable);
            hintView.setTextColor(getResources().getColor(isOn ? R.color.blue_ff337aff : R.color.gray_ff85899c));
        }
    }

    private void setLanAccessPattern(Boolean isOn) {
        if (isOn != null) {
            mAoSpaceAccessBean.setLanAccess(isOn);
            DataUtil.setAoSpaceAccessBean(mAoSpaceAccessBean);
            setHintPattern(lanAccessHint, isOn);
        }
    }


    private void setInternetAccessPattern(Boolean isOn) {
        if (isOn != null) {
            mAoSpaceAccessBean.setInternetAccess(isOn);
            DataUtil.setAoSpaceAccessBean(mAoSpaceAccessBean);
            if (internetAccessSwitch != null) {
                internetAccessSwitch.setImageResource(isOn ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
            }
            if (internetAccessOffHintContainer != null) {
                internetAccessOffHintContainer.setVisibility(isOn ? View.GONE : View.VISIBLE);
            }
            layoutPlatformAddress.setVisibility(isOn ? View.VISIBLE : View.GONE);
            resetPlatformAddressView(isOn, mAoSpaceAccessBean.getPlatformApiBase());
        }
    }

    private void setInternetAccessSwitchClickable(boolean isClickable) {
        if (internetAccessSwitch != null) {
            internetAccessSwitch.setClickable(isClickable);
        }
    }

    private void setLoadingButtonContainerPattern(boolean isLoading) {
        isBusy = isLoading;
        if (isLoading) {
            if (isBindProgress) {
                back.setClickable(false);
                back.setVisibility(View.GONE);
//                functionText.setVisibility(View.VISIBLE);
//                functionText.setClickable(true);
            }
            loadingButtonContainer.setClickable(false);
            loadingContent.setText(isBindProgress ? R.string.string_continue : R.string.save);
            loadingAnimation.setVisibility(View.VISIBLE);
            LottieUtil.loop(loadingAnimation, "loading_button.json");
        } else {
            if (isBindProgress) {
                back.setVisibility(View.VISIBLE);
                back.setClickable(true);
//                functionText.setClickable(false);
//                functionText.setVisibility(View.GONE);
            }
            loadingButtonContainer.setClickable(true);
            loadingButtonContainer.setOnClickListener(this);
            LottieUtil.stop(loadingAnimation);
            loadingAnimation.setVisibility(View.GONE);
            loadingContent.setText(isBindProgress ? R.string.string_continue : R.string.save);
        }
    }

    private void showCloseInternetAccessDialog() {
        if (closeInternetAccessDialog != null && !closeInternetAccessDialog.isShowing()) {
            closeInternetAccessDialog.show();
            Window window = closeInternetAccessDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissCloseInternetAccessDialog() {
        if (closeInternetAccessDialog != null && closeInternetAccessDialog.isShowing()) {
            closeInternetAccessDialog.dismiss();
        }
    }

    private void showInternetAccessExceptionDialog() {
        if (internetAccessExceptionDialog != null && !internetAccessExceptionDialog.isShowing()) {
            internetAccessExceptionDialog.show();
            Window window = internetAccessExceptionDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissInternetAccessExceptionDialog() {
        if (internetAccessExceptionDialog != null && internetAccessExceptionDialog.isShowing()) {
            internetAccessExceptionDialog.dismiss();
        }
    }

    private void handleBindProgressEvent() {
        if (mManager != null) {
            setInternetAccessSwitchClickable(false);
            setLoadingButtonContainerPattern(true);
            handleRequestEvent();
        }
    }

    private void handleRequestEvent() {
        boolean isHandle = (mManager != null);
        if (isHandle) {
            switch (stepBindProgress) {
                case STEP_BIND_PROGRESS_BIND_SPACE_CREATE:
                    String adminPassword = mManager.getAdminPassword();
                    SpaceCreateRequest spaceCreateRequest = new SpaceCreateRequest();
                    spaceCreateRequest.setClientPhoneModel(SystemUtil.getPhoneModel());
                    spaceCreateRequest.setClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                    spaceCreateRequest.setEnableInternetAccess(mAoSpaceAccessBean.getInternetAccess());
                    spaceCreateRequest.setPassword(adminPassword);
                    spaceCreateRequest.setSpaceName(mManager.getAdminName());
                    spaceCreateRequest.setPlatformApiBase(mAoSpaceAccessBean.getPlatformApiBase());
                    Map<String, String> queryMapBinder = new HashMap<>();
//                    Map<String, String> queryMapPasswordOnBinder = new HashMap<>();
                    String encodeClientUuid = null;
                    try {
                        encodeClientUuid = URLEncoder.encode(DataUtil.getClientUuid(getApplicationContext()), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (encodeClientUuid != null) {
                        queryMapBinder.put(VerificationMethod.QUERY_CLIENT_UUID, encodeClientUuid);
//                        queryMapPasswordOnBinder.put(VerificationMethod.QUERY_CLIENT_UUID, encodeClientUuid);
                    }
                    queryMapBinder.put(VerificationMethod.QUERY_CREDENTIAL_TYPE, VerificationMethod.CREDENTIAL_TYPE_BINDER);
//                    queryMapPasswordOnBinder.put(VerificationMethod.QUERY_CREDENTIAL_TYPE, VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_BINDER);
                    byte[] version = StringUtil.stringToByteArray("\0\0", StandardCharsets.UTF_8);

                    DIDCredentialBean didCredentialBean = EulixSpaceDBUtil.getSpecificDIDCredentialBean(getApplicationContext(), mManager.getBoxUuid(), "1", null);
                    DIDCredentialBean.BinderCredential binderCredential = null;
//                    DIDCredentialBean.PasswordCredential passwordCredential = null;
                    if (didCredentialBean == null) {
                        didCredentialBean = new DIDCredentialBean();
                    } else {
                        binderCredential = didCredentialBean.getBinderCredential();
//                        passwordCredential = didCredentialBean.getPasswordCredential();
                    }
                    String binderPublicKey = null;
//                    String passwordPublicKey = null;
                    if (binderCredential == null) {
                        KeyPair keyPair = EncryptionUtil.generateKeyPair(ConstantField.Algorithm.RSA, null, 2048);
                        if (keyPair != null) {
                            String publicKey = StringUtil.byteArrayToString(keyPair.getPublic().getEncoded());
                            String privateKey = StringUtil.byteArrayToString(keyPair.getPrivate().getEncoded());
                            if (publicKey != null && privateKey != null) {
                                binderPublicKey = publicKey;
                                binderCredential = new DIDCredentialBean.BinderCredential();
                                binderCredential.setBinderPublicKey(publicKey);
                                binderCredential.setBinderPrivateKey(privateKey);
                                didCredentialBean.setBinderCredential(binderCredential);
                            }
                        }
                    } else {
                        binderPublicKey = binderCredential.getBinderPublicKey();
                    }
//                    boolean isGeneratePasswordKeyPair = (passwordCredential == null);
//                    if (passwordCredential != null) {
//                        String privateKey = null;
//                        if (passwordCredential.isEncryptPrivateKey()) {
//                            privateKey = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                                    , null, passwordCredential.getPasswordPrivateKey()
//                                    , StringUtil.getCustomizeSecret32(adminPassword), StandardCharsets.UTF_8
//                                    , passwordCredential.getPasswordIv());
//                        } else {
//                            privateKey = passwordCredential.getPasswordPrivateKey();
//                        }
//                        if (passwordCredential.getPasswordPublicKey() == null || privateKey == null) {
//                            isGeneratePasswordKeyPair = true;
//                            passwordCredential = null;
//                        }
//                    }
//                    if (isGeneratePasswordKeyPair) {
//                        KeyPair keyPair = EncryptionUtil.generateKeyPair(ConstantField.Algorithm.RSA, null, 2048);
//                        if (keyPair != null) {
//                            String publicKey = StringUtil.byteArrayToString(keyPair.getPublic().getEncoded());
//                            String privateKey = StringUtil.byteArrayToString(keyPair.getPrivate().getEncoded());
//                            if (publicKey != null && privateKey != null) {
//                                passwordPublicKey = publicKey;
//                                passwordCredential = new DIDCredentialBean.PasswordCredential();
//                                passwordCredential.setPasswordPublicKey(publicKey);
//                                byte[] iv = StringUtil.getRandom(16);
//                                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
//                                String ivParams = StringUtil.byteArrayToString(ivParameterSpec.getIV());
//                                passwordCredential.setPasswordIv(ivParams);
//                                String encryptPrivateKey = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                                        , null, privateKey, StringUtil.getCustomizeSecret32(adminPassword), StandardCharsets.UTF_8, ivParams);
//                                passwordCredential.setEncryptPrivateKey(encryptPrivateKey != null);
//                                passwordCredential.setPasswordPrivateKey((encryptPrivateKey == null ? privateKey : encryptPrivateKey));
//                                didCredentialBean.setPasswordCredential(passwordCredential);
//                            }
//                        }
//                    } else {
//                        passwordPublicKey = passwordCredential.getPasswordPublicKey();
//                    }
                    AOSpaceUtil.insertOrUpdateDID(getApplicationContext(), mManager.getBoxUuid(), "1", didCredentialBean);

                    List<VerificationMethod> verificationMethods = null;
                    VerificationMethod binderMethod = null;
                    if (binderPublicKey != null) {
                        binderMethod = DIDUtils.generateDIDVerificationMethod(VerificationMethod.DID_AO_SPACE_KEY_PREFIX
                                , version, queryMapBinder, null, StringUtil.wrapPublicKeyNewLine(binderPublicKey)
                                , VerificationMethod.TYPE_RSA_VERIFICATION_KEY_2018);
                    }
//                    VerificationMethod passwordOnBinderMethod = null;
//                    if (passwordPublicKey != null) {
//                        passwordOnBinderMethod = DIDUtils.generateDIDVerificationMethod(VerificationMethod.DID_AO_SPACE_KEY_PREFIX
//                                , version, queryMapPasswordOnBinder, null, StringUtil.wrapPublicKey(passwordPublicKey)
//                                , VerificationMethod.TYPE_RSA_VERIFICATION_KEY_2018);
//                    }
                    if (binderMethod != null/* || passwordOnBinderMethod != null*/) {
                        verificationMethods = new ArrayList<>();
                        verificationMethods.add(binderMethod);
//                        if (passwordOnBinderMethod != null) {
//                            verificationMethods.add(passwordOnBinderMethod);
//                        }
                    }
                    spaceCreateRequest.setVerificationMethod(verificationMethods);

                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE
                            , new Gson().toJson(spaceCreateRequest, SpaceCreateRequest.class));
                    break;
                case STEP_BIND_PROGRESS_SPACE_READY_CHECK:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
                    break;
                case STEP_BIND_PROGRESS_DISK_RECOGNITION:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_RECOGNITION, null);
                    break;
                case STEP_BIND_PROGRESS_DISK_MANAGEMENT_LIST:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST, null);
                    break;
                default:
                    isHandle = false;
                    break;
            }
        }
        if (!isHandle) {
            handleErrorEvent();
        }
    }

    private void handleErrorEvent() {
        setLoadingButtonContainerPattern(false);
        setInternetAccessSwitchClickable(true);
        showServerExceptionToast();
    }

    private void handleFinish() {
        if (mManager != null) {
            mManager.finishSource();
            finish();
        } else {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    @Override
    protected void onDestroy() {
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isBindProgress && isBusy) {
            confirmForceExit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isBindProgress && isBusy && KeyEvent.KEYCODE_BACK == keyCode) {
            confirmForceExit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            handleFinish();
        }
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public AOSpaceAccessPresenter createPresenter() {
        return new AOSpaceAccessPresenter();
    }

    public static void startThisActivity(Context context, boolean isBindProgress) {
        if (context != null) {
            Intent intent = new Intent(context, AOSpaceAccessActivity.class);
            intent.putExtra(IS_BIND_PROGRESS, isBindProgress);
            context.startActivity(intent);
        }
    }

    @Override
    public void setInternetServiceConfigCallback(int code, String source, String message, InternetServiceConfigResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                Boolean isInternetAccess = null;
                if (mAoSpaceAccessBean != null) {
                    isInternetAccess = mAoSpaceAccessBean.getInternetAccess();
                }
                closeLoading();
                if (code >= 200 && code < 400 && result != null) {
                    if (!isBindProgress) {
                        showImageTextToast(R.drawable.toast_right, R.string.setting_success);
                    }
                    finish();
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else if (isInternetAccess != null && isInternetAccess && code == ConstantField.KnownError.BindError.CALL_SERVICE_FAILED && ConstantField.KnownSource.AGENT.equals(source)) {
                    showInternetAccessExceptionDialog();
                    setInternetAccessPattern(Boolean.FALSE);
                } else if (code == ConstantField.PLATFORM_UNAVAILABLE_CODE) {
                    showImageTextToast(R.drawable.toast_refuse, R.string.platform_regular_toast_unavailable);
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.save_fail);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.function_text:
                    handleFinish();
                    break;
                case R.id.internet_access_switch:
                    Boolean isInternetAccess = mAoSpaceAccessBean.getInternetAccess();
                    if (isInternetAccess != null) {
                        if (isInternetAccess) {
                            showCloseInternetAccessDialog();
                        } else {
                            setInternetAccessPattern(Boolean.TRUE);
                        }
                    }
                    break;
                case R.id.loading_button_container:
                    if (mAoSpaceAccessBean.getInternetAccess()) {
                        //判断url合规性
                        if (TextUtils.isEmpty(mAoSpaceAccessBean.getPlatformApiBase())) {
                            showImageTextToast(R.drawable.toast_refuse, R.string.platform_regular_toast_empty);
                            return;
                        } else if (!mAoSpaceAccessBean.getPlatformApiBase().startsWith("https://")) {
                            showImageTextToast(R.drawable.toast_refuse, R.string.platform_regular_toast_only_https);
                            return;
                        } else {
                            try {
                                URL url = new URL(mAoSpaceAccessBean.getPlatformApiBase());
                            } catch (MalformedURLException e) {
                                showImageTextToast(R.drawable.toast_refuse, R.string.platform_regular_toast_malformed);
                                return;
                            }
                        }
                    }

                    if (isBindProgress) {
                        handleBindProgressEvent();
                    } else if (presenter != null) {
                        if (AOSpaceAccessBean.compareAccess(mAoSpaceAccessBean, presenter.getActiveAOSpaceAccessBean())) {
                            finish();
                        } else {
                            showLoading("");
                            presenter.setInternetServiceConfig(mAoSpaceAccessBean.getInternetAccess(), mPlatformAddressStr);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFinish() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void onResponse(int code, String source, int step, String bodyJson) {
        if (mHandler != null) {
            mHandler.post(() -> {
                switch (step) {
                    case AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE:
                        SpaceCreateResult spaceCreateResult = null;
                        if (bodyJson != null) {
                            try {
                                spaceCreateResult = new Gson().fromJson(bodyJson, SpaceCreateResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        Boolean isInternetAccess = null;
                        if (mAoSpaceAccessBean != null) {
                            isInternetAccess = mAoSpaceAccessBean.getInternetAccess();
                        }
                        PairingBoxResults pairingBoxResults = null;
                        int contentCode = -1;
                        if (spaceCreateResult != null) {
                            pairingBoxResults = spaceCreateResult.getSpaceUserInfo();
                            if (pairingBoxResults != null) {
                                String contentCodeValue = pairingBoxResults.getCode();
                                if (contentCodeValue != null) {
                                    contentCode = DataUtil.stringCodeToInt(contentCodeValue);
                                }
                            }
                        }
                        if (code == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED && ConstantField.KnownSource.AGENT.equals(source)) {
                            setLoadingButtonContainerPattern(false);
                            setInternetAccessSwitchClickable(true);
                            Intent intent = new Intent(AOSpaceAccessActivity.this, BindResultActivity.class);
                            intent.putExtra(ConstantField.BIND_TYPE, true);
                            intent.putExtra(ConstantField.BIND_RESULT, code);
                            String platformUrl = null;
                            if (mManager != null) {
                                InitResponse initResponse = mManager.getInitResponse();
                                if (initResponse != null) {
                                    platformUrl = initResponse.getSspUrl();
                                }
                            }
                            if (platformUrl != null) {
                                intent.putExtra(ConstantField.PLATFORM_URL, platformUrl);
                            }
                            startActivity(intent);
                        } else if (isInternetAccess != null && isInternetAccess && (contentCode == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR
                                || (code == ConstantField.KnownError.BindError.CALL_SERVICE_FAILED && ConstantField.KnownSource.AGENT.equals(source)))) {
                            setLoadingButtonContainerPattern(false);
                            setInternetAccessSwitchClickable(true);
                            showInternetAccessExceptionDialog();
                            setInternetAccessPattern(Boolean.FALSE);
                        } else if (code >= 200 && code < 400 && spaceCreateResult != null) {
                            PairingBoxInfo pairingBoxInfo = null;
                            List<ConnectedNetwork> connectedNetworks = spaceCreateResult.getConnectedNetwork();
                            Boolean enableInternetAccess = spaceCreateResult.getEnableInternetAccess();
                            String didDoc = spaceCreateResult.getDidDoc();
                            String boxUuid = null;
                            if (mManager != null) {
                                boxUuid = mManager.getBoxUuid();
                            }
                            if (didDoc != null) {
                                String didDocDecode = StringUtil.base64Decode(didDoc, StandardCharsets.UTF_8);
                                DIDProviderBean didProviderBean = new DIDProviderBean(boxUuid, "1");
                                didProviderBean.setAoId(null);
                                didProviderBean.setDidDoc(didDoc);
                                didProviderBean.setDidDocDecode(didDocDecode);
                                didProviderBean.setTimestamp(System.currentTimeMillis());
                                AOSpaceUtil.insertOrUpdateDIDWithPasswordEncryptPrivateKey(getApplicationContext()
                                        , didProviderBean, spaceCreateResult.getEncryptedPriKeyBytes());
                            }
                            if (pairingBoxResults != null) {
                                pairingBoxInfo = pairingBoxResults.getResults();
                            }
                            mPairingBoxInfo = pairingBoxInfo;
                            if (mManager != null) {
                                mManager.setPairingBoxInfo(pairingBoxInfo);
                                boolean isInnerDiskSupport = mManager.isInnerDiskSupport();
                                if (boxUuid != null) {
                                    String ipAddressUrl = null;
                                    if (connectedNetworks != null) {
                                        for (ConnectedNetwork connectedNetwork : connectedNetworks) {
                                            if (connectedNetwork != null) {
                                                ipAddressUrl = connectedNetwork.generateIpAddressUrl();
                                                if (ipAddressUrl != null) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    AOSpaceAccessBean aoSpaceAccessBean = null;
                                    if (mAoSpaceAccessBean != null) {
                                        aoSpaceAccessBean = new AOSpaceAccessBean();
                                        aoSpaceAccessBean.setLanAccess(mAoSpaceAccessBean.getLanAccess());
                                        if (enableInternetAccess != null) {
                                            aoSpaceAccessBean.setP2PAccess(enableInternetAccess);
                                            aoSpaceAccessBean.setInternetAccess(enableInternetAccess);
                                        } else {
                                            aoSpaceAccessBean.setP2PAccess(mAoSpaceAccessBean.getP2PAccess());
                                            aoSpaceAccessBean.setInternetAccess(mAoSpaceAccessBean.getInternetAccess());
                                        }
                                    }
                                    boolean bindResult = AOSpaceUtil.requestUseBox(getApplicationContext(), boxUuid, "1"
                                            , pairingBoxInfo, aoSpaceAccessBean, StringUtil.nullToEmpty(ipAddressUrl)
                                            , mManager.getBoxPublicKey(), mManager.getDeviceName()
                                            , mManager.getBluetoothAddress(), mManager.getBluetoothId()
                                            , mManager.getDeviceAbility(), isInnerDiskSupport);
                                    if (bindResult) {
                                        if (isInnerDiskSupport) {
                                            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
                                        } else {
                                            setLoadingButtonContainerPattern(false);
                                            setInternetAccessSwitchClickable(true);
                                            String avatarUrl = null;
                                            if (pairingBoxInfo != null) {
                                                avatarUrl = pairingBoxInfo.getAvatarUrl();
                                            }
                                            AOCompleteActivity.startThisActivity(AOSpaceAccessActivity.this, boxUuid, "1", avatarUrl);
                                            mManager.finishSource();
                                        }
                                    } else {
                                        setLoadingButtonContainerPattern(false);
                                        setInternetAccessSwitchClickable(true);
                                        Intent intent = new Intent(this, BindResultActivity.class);
                                        intent.putExtra(ConstantField.BIND_TYPE, true);
                                        intent.putExtra(ConstantField.BIND_RESULT, 500);
                                        startActivity(intent);
                                    }
                                }
                            }
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK:
                        ReadyCheckResult readyCheckResult = null;
                        if (bodyJson != null) {
                            try {
                                readyCheckResult = new Gson().fromJson(bodyJson, ReadyCheckResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        mReadyCheckResult = readyCheckResult;
                        Integer diskInitialCode = null;
                        if (readyCheckResult != null) {
                            diskInitialCode = readyCheckResult.getDiskInitialCode();
                        }
                        if (diskInitialCode != null) {
                            if (diskInitialCode == ReadyCheckResult.DISK_NORMAL) {
                                stepBindProgress = STEP_BIND_PROGRESS_DISK_MANAGEMENT_LIST;
                            } else {
                                stepBindProgress = STEP_BIND_PROGRESS_DISK_RECOGNITION;
                            }
                            handleRequestEvent();
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_DISK_RECOGNITION:
                        DiskRecognitionResult diskRecognitionResult = null;
                        if (bodyJson != null) {
                            try {
                                diskRecognitionResult = new Gson().fromJson(bodyJson, DiskRecognitionResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (diskRecognitionResult != null) {
                            String dataUuid = DataUtil.setData(new Gson().toJson(diskRecognitionResult, DiskRecognitionResult.class));
                            boolean isNoMainStorage = false;
                            Integer diskInitialCodeValue = null;
                            if (mReadyCheckResult != null) {
                                diskInitialCodeValue = mReadyCheckResult.getDiskInitialCode();
                                Boolean isNoMainStorageValue = mReadyCheckResult.getMissingMainStorage();
                                if (isNoMainStorageValue != null) {
                                    isNoMainStorage = isNoMainStorageValue;
                                }
                            }
                            Intent intent = new Intent(AOSpaceAccessActivity.this, DiskInitializationActivity.class);
                            intent.putExtra(ConstantField.DISK_INITIALIZE_NO_MAIN_STORAGE, isNoMainStorage);
                            String boxUuid = null;
                            if (mManager != null) {
                                boxUuid = mManager.getBoxUuid();
                            }
                            if (boxUuid != null) {
                                intent.putExtra(ConstantField.BOX_UUID, boxUuid);
                            }
                            if (dataUuid != null) {
                                intent.putExtra(ConstantField.DATA_UUID, dataUuid);
                            }
                            if (diskInitialCodeValue != null) {
                                intent.putExtra(ConstantField.DISK_INITIALIZE, diskInitialCodeValue.intValue());
                            }
                            startActivity(intent);
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST:
                        DiskManageListResult diskManageListResult = null;
                        if (bodyJson != null) {
                            try {
                                diskManageListResult = new Gson().fromJson(bodyJson, DiskManageListResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        String boxUuid = null;
                        if (mManager != null) {
                            boxUuid = mManager.getBoxUuid();
                        }
                        if (boxUuid != null && diskManageListResult != null) {
                            AOSpaceUtil.requestUseBox(getApplicationContext(), boxUuid, "1", diskManageListResult);
                            String avatarUrl = null;
                            if (mPairingBoxInfo != null) {
                                avatarUrl = mPairingBoxInfo.getAvatarUrl();
                            }
                            AOCompleteActivity.startThisActivity(AOSpaceAccessActivity.this, boxUuid, "1", avatarUrl);
                            mManager.finishSource();
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    default:
                        break;
                }
            });
        }
    }
}
