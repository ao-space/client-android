package xyz.eulix.space.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.InviteParams;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.presenter.AOSpaceInformationPresenter;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

public class AOSpaceInformationActivity extends AbsActivity<AOSpaceInformationPresenter.IAOSpaceInformation, AOSpaceInformationPresenter> implements AOSpaceInformationPresenter.IAOSpaceInformation
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback {
    private static final String IS_ADMINISTRATOR = "is_administrator";
    private static final String SPACE_NAME = "space_name";
    private static final String BOX_PUBLIC_KEY = "box_public_key";
    private String activityId;
    private boolean isAdministrator;
    private String spaceName;
    private String mBoxPublicKey;
    private InviteParams mInviteParams;
    private ImageView titleHeaderImage;
    private TextView titleHeaderText;
    private TextView titleHeaderIntroduction;
    private EditText spaceNameInput;
    private ImageButton spaceNameClear;
    private LinearLayout loadingButtonContainer;
    private LottieAnimationView loadingAnimation;
    private TextView loadingContent;
    private AODeviceDiscoveryManager mManager;
    private AOSpaceInformationHandler mHandler;
    private long mExitTime = 0L;

    static class AOSpaceInformationHandler extends Handler {
        private WeakReference<AOSpaceInformationActivity> aoSpaceInformationActivityWeakReference;

        public AOSpaceInformationHandler(AOSpaceInformationActivity activity) {
            aoSpaceInformationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AOSpaceInformationActivity activity = aoSpaceInformationActivityWeakReference.get();
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
        setContentView(R.layout.activity_ao_space_information);
        titleHeaderImage = findViewById(R.id.title_header_image);
        titleHeaderText = findViewById(R.id.title_header_text);
        titleHeaderIntroduction = findViewById(R.id.title_header_introduction);
        spaceNameInput = findViewById(R.id.space_name_input);
        spaceNameClear = findViewById(R.id.space_name_clear);
        loadingButtonContainer = findViewById(R.id.loading_button_container);
        loadingAnimation = findViewById(R.id.loading_animation);
        loadingContent = findViewById(R.id.loading_content);
    }

    @Override
    public void initData() {
        mHandler = new AOSpaceInformationHandler(this);
        handleIntent(getIntent());
        if (isAdministrator) {
            activityId = UUID.randomUUID().toString();
            mManager = AODeviceDiscoveryManager.getInstance();
            mManager.registerCallback(activityId, this);
        }
    }

    @Override
    public void initViewData() {
        titleHeaderImage.setImageResource(R.drawable.image_space_information_2x);
        titleHeaderText.setText(R.string.space_information);
        titleHeaderIntroduction.setVisibility(View.VISIBLE);
        titleHeaderIntroduction.setText(R.string.space_information_introduction);
        spaceNameInput.setText(StringUtil.isNonBlankString(spaceName) ? spaceName : "我的空间");
    }

    @Override
    public void initEvent() {
        spaceNameClear.setOnClickListener(this);
        setLoadingButtonContainerPattern(false);

        spaceNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSpaceNameClearVisibility((s == null ? 0 : s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
        spaceNameInput.setOnFocusChangeListener((v, hasFocus) -> setSpaceNameClearVisibility(spaceNameInput.length()));
        spaceNameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (EditorInfo.IME_ACTION_DONE == actionId && spaceNameInput.isFocused()) {
                spaceNameInput.clearFocus();
            }
            return false;
        });
        setSpaceNameClearVisibility(spaceNameInput.length());
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            isAdministrator = intent.getBooleanExtra(IS_ADMINISTRATOR, false);
            if (intent.hasExtra(SPACE_NAME)) {
                spaceName = intent.getStringExtra(SPACE_NAME);
            }
            if (!isAdministrator) {
                if (intent.hasExtra(BOX_PUBLIC_KEY)) {
                    mBoxPublicKey = intent.getStringExtra(BOX_PUBLIC_KEY);
                }
                if (intent.hasExtra(ConstantField.DATA_UUID)) {
                    String dataUuid = intent.getStringExtra(ConstantField.DATA_UUID);
                    if (dataUuid != null) {
                        String data = DataUtil.getData(dataUuid);
                        if (data != null) {
                            try {
                                mInviteParams = new Gson().fromJson(data, InviteParams.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    private void setSpaceNameClearVisibility(int inputLength) {
        if (spaceNameClear != null) {
            spaceNameClear.setVisibility(((inputLength > 0 && spaceNameInput != null && spaceNameInput.isFocused())
                    ? View.VISIBLE : View.GONE));
        }
    }

    private void setLoadingButtonContainerPattern(boolean isLoading) {
        if (isLoading) {
            loadingButtonContainer.setClickable(false);
            loadingContent.setText(R.string.string_continue);
            loadingAnimation.setVisibility(View.VISIBLE);
            LottieUtil.loop(loadingAnimation, "loading_button.json");
        } else {
            loadingButtonContainer.setClickable(true);
            loadingButtonContainer.setOnClickListener(this);
            LottieUtil.stop(loadingAnimation);
            loadingAnimation.setVisibility(View.GONE);
            loadingContent.setText(R.string.string_continue);
        }
    }

    private void handleMemberFailResult(int code) {
        boolean isFinish = false;
        switch (code) {
            case ConstantField.MemberInviteResultCode.MEMBER_DUPLICATE:
                showImageTextToast(R.drawable.toast_wrong, R.string.join_fail_member_duplicate);
                isFinish = true;
                break;
            case ConstantField.MemberInviteResultCode.MEMBER_NICKNAME_ILLEGAL:
                showImageTextToast(R.drawable.toast_refuse, R.string.join_fail_nickname_illegal);
                break;
            case ConstantField.MemberInviteResultCode.SPACE_ID_REPEAT:
                showImageTextToast(R.drawable.toast_refuse, R.string.modify_nick_failed_repeat);
                break;
            case ConstantField.MemberInviteResultCode.INVITE_CODE_INVALID:
                showImageTextToast(R.drawable.toast_wrong, R.string.join_fail_invalid_invite_code);
                isFinish = true;
                break;
            case ConstantField.MemberInviteResultCode.MEMBER_FULL:
                showImageTextToast(R.drawable.toast_wrong, R.string.join_fail_member_full);
                isFinish = true;
                break;
            case ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR:
                showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                isFinish = true;
                break;
            default:
                if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.join_fail);
                }
                break;
        }
        if (isFinish) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        DataUtil.resetAoSpaceAccessBean();
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
        confirmForceExit();
//        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
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
        } else if (!isAdministrator || mManager == null) {
            EulixSpaceApplication.popAllOldActivity(null);
        } else {
            mManager.finishSource();
            finish();
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
    public AOSpaceInformationPresenter createPresenter() {
        return new AOSpaceInformationPresenter();
    }

    /**
     * 管理员使用
     * @param context
     * @param spaceName
     */
    public static void administratorStartThisActivity(Context context, String spaceName) {
        if (context != null) {
            Intent intent = new Intent(context, AOSpaceInformationActivity.class);
            intent.putExtra(IS_ADMINISTRATOR, true);
            if (spaceName != null) {
                intent.putExtra(SPACE_NAME, spaceName);
            }
            context.startActivity(intent);
        }
    }

    /**
     * 成员使用
     * @param context
     * @param inviteParams
     * @param boxPublicKey
     */
    public static void memberStartThisActivity(Context context, InviteParams inviteParams, String boxPublicKey) {
        if (context != null) {
            Intent intent = new Intent(context, AOSpaceInformationActivity.class);
            intent.putExtra(IS_ADMINISTRATOR, false);
            String spaceName = null;
            String dataUuid = null;
            if (inviteParams != null) {
                spaceName = inviteParams.getMember();
                dataUuid = DataUtil.setData(new Gson().toJson(inviteParams, InviteParams.class));
            }
            if (spaceName != null) {
                intent.putExtra(SPACE_NAME, spaceName);
            }
            if (boxPublicKey != null) {
                intent.putExtra(BOX_PUBLIC_KEY, boxPublicKey);
            }
            if (dataUuid != null) {
                intent.putExtra(ConstantField.DATA_UUID, dataUuid);
            }
            context.startActivity(intent);
        }
    }

    @Override
    public void createMemberCallback(boolean isSuccess, int code, String message, String boxUuid) {
        if (mHandler != null) {
            mHandler.post(() -> {
                setLoadingButtonContainerPattern(false);
                if (isSuccess) {
                    AOCompleteActivity.startThisActivity(AOSpaceInformationActivity.this, boxUuid, "-1");
                    finish();
                } else {
                    handleMemberFailResult(code);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.space_name_clear:
                    spaceNameInput.setText("");
                    break;
                case R.id.loading_button_container:
                    if (spaceNameInput != null) {
                        String spaceName = spaceNameInput.getText().toString();
                        int length = spaceName.length();
                        if (length < 1 || length > 24) {
                            if (length <= 0) {
                                showImageTextToast(R.drawable.toast_refuse, R.string.empty_input_warning);
                            } else {
                                showImageTextToast(R.drawable.toast_refuse, R.string.over_maxcount);
                            }
                        } else if (!StringUtil.checkTextMatch(spaceName, ConstantField.StringPattern.SPACE_NAME_REG)) {
                            showImageTextToast(R.drawable.toast_refuse, R.string.join_fail_nickname_illegal);
                        } else if (isAdministrator) {
                            if (mManager != null) {
                                mManager.setAdminName(spaceName);
                            }
                            Intent intent = new Intent(AOSpaceInformationActivity.this, AOSecurityPasswordActivity.class);
                            startActivity(intent);
                        } else if (presenter != null) {
                            setLoadingButtonContainerPattern(true);
                            presenter.createMember(mInviteParams, mBoxPublicKey, spaceName);
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
        // Do nothing
    }
}
