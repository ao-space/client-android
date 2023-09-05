package xyz.eulix.space.ui.bind;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DistributeWLAN;
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.bean.WLANItem;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.bind.ProgressResult;
import xyz.eulix.space.network.agent.bind.SpaceCreateRequest;
import xyz.eulix.space.presenter.AOLocaleSettingsPresenter;
import xyz.eulix.space.ui.AOSpaceInformationActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;

public class AOLocaleSettingsActivity extends AbsActivity<AOLocaleSettingsPresenter.IAOLocaleSettings, AOLocaleSettingsPresenter> implements AOLocaleSettingsPresenter.IAOLocaleSettings
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback {
    private static final int STEP_BIND_COMMUNICATION_PROGRESS = 1;
    private static final int STEP_WIFI_LIST = STEP_BIND_COMMUNICATION_PROGRESS + 1;
    private String activityId;
    private ImageButton backNoTitle;
    private LinearLayout loadingButtonContainer;
    private LottieAnimationView loadingAnimation;
    private TextView loadingContent;
    private AODeviceDiscoveryManager mManager;
    private AOLocaleSettingsHandler mHandler;
    private int mStep;

    static class AOLocaleSettingsHandler extends Handler {
        private WeakReference<AOLocaleSettingsActivity> aoLocaleSettingsActivityWeakReference;

        public AOLocaleSettingsHandler(AOLocaleSettingsActivity activity) {
            aoLocaleSettingsActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AOLocaleSettingsActivity activity = aoLocaleSettingsActivityWeakReference.get();
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
        setContentView(R.layout.activity_ao_locale_settings);
        backNoTitle = findViewById(R.id.back_no_title);
        loadingButtonContainer = findViewById(R.id.loading_button_container);
        loadingAnimation = findViewById(R.id.loading_animation);
        loadingContent = findViewById(R.id.loading_content);
    }

    @Override
    public void initData() {
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
        mHandler = new AOLocaleSettingsHandler(this);
    }

    @Override
    public void initViewData() {

    }

    @Override
    public void initEvent() {
        backNoTitle.setOnClickListener(this);
        setLoadingButtonContainerPattern(false);
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

    private boolean hasNetwork() {
        boolean hasNetwork = false;
        if (mManager != null) {
            DistributeWLAN distributeWLAN = mManager.getDistributeWLAN();
            InitResponse initResponse = mManager.getInitResponse();
            if (distributeWLAN != null) {
                List<NetworkAccessBean> networkAccessBeanList = distributeWLAN.getNetworkAccessBeanList();
                hasNetwork = (networkAccessBeanList != null && !networkAccessBeanList.isEmpty());
            } else if (initResponse != null) {
                List<InitResponseNetwork> networks = initResponse.getNetwork();
                hasNetwork = (networks != null && !networks.isEmpty());
            }
        }
        return hasNetwork;
    }

    private boolean isOpenSource() {
        boolean isOpenSource = false;
        if (mManager != null) {
            InitResponse initResponse = mManager.getInitResponse();
            if (initResponse != null) {
                DeviceAbility deviceAbility = initResponse.getDeviceAbility();
                if (deviceAbility != null) {
                    Boolean isOpenSourceValue = deviceAbility.getOpenSource();
                    if (isOpenSourceValue != null) {
                        isOpenSource = isOpenSourceValue;
                    }
                }
            }
        }
        return isOpenSource;
    }

    private void handleRequestEvent() {
        boolean isHandle = (mManager != null);
        if (isHandle) {
            switch (mStep) {
                case STEP_BIND_COMMUNICATION_PROGRESS:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS, null);
                    break;
                case STEP_WIFI_LIST:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_WIFI_LIST, null);
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
        showServerExceptionToast();
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
    public AOLocaleSettingsPresenter createPresenter() {
        return new AOLocaleSettingsPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back_no_title:
                    finish();
                    break;
                case R.id.loading_button_container:
                    setLoadingButtonContainerPattern(true);
                    if (isOpenSource() || hasNetwork()) {
                        mStep = STEP_BIND_COMMUNICATION_PROGRESS;
                    } else {
                        mStep = STEP_WIFI_LIST;
                    }
                    handleRequestEvent();
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
                    case AODeviceDiscoveryManager.STEP_WIFI_LIST:
                        List<WifiInfo> wifiInfoList = null;
                        if (bodyJson != null) {
                            try {
                                wifiInfoList = new Gson().fromJson(bodyJson, new TypeToken<List<WifiInfo>>(){}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        setLoadingButtonContainerPattern(false);
                        DistributeWLAN distributeWLAN = null;
                        String adminPassword = null;
                        if (mManager != null) {
                            distributeWLAN = mManager.getDistributeWLAN();
                            adminPassword = mManager.getAdminPassword();
                        }
                        if (wifiInfoList == null || distributeWLAN == null) {
                            showServerExceptionToast();
                        } else {
                            List<WLANItem> wlanItems = new ArrayList<>();
                            for (WifiInfo wifiInfo : wifiInfoList) {
                                if (wifiInfo != null) {
                                    WLANItem wlanItem = new WLANItem();
                                    wlanItem.setWlanSsid(wifiInfo.getName());
                                    wlanItem.setWlanAddress(wifiInfo.getSsid());
                                    wlanItems.add(wlanItem);
                                }
                            }
                            distributeWLAN.setWlanItemList(wlanItems);
                            if (mManager != null) {
                                mManager.setDistributeWLAN(distributeWLAN);
                            }
                            Intent distributeIntent = new Intent(AOLocaleSettingsActivity.this, DistributeNetworkActivity.class);
                            distributeIntent.putExtra(ConstantField.WIFI_SSIDS, new Gson().toJson(distributeWLAN, DistributeWLAN.class));
                            if (adminPassword != null) {
                                distributeIntent.putExtra(ConstantField.PASSWORD, adminPassword);
                            }
                            startActivity(distributeIntent);
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS:
                        ProgressResult progressResult = null;
                        if (bodyJson != null) {
                            progressResult = new Gson().fromJson(bodyJson, ProgressResult.class);
                        }
                        if (code == ConstantField.KnownError.BindError.BOUND_CODE && ConstantField.KnownSource.AGENT.equals(source)) {
                            setLoadingButtonContainerPattern(false);
                            if (mManager != null) {
                                showImageTextToast(R.drawable.toast_refuse, R.string.binding_initializing_space_change_phone_hint);
                                mManager.finishSource();
                                finish();
                            }
                        } else if (progressResult != null) {
                            if (isOpenSource() || hasNetwork()) {
                                int comStatus = progressResult.getComStatus();
                                if (!isOpenSource() && (comStatus < 0 || comStatus == ProgressResult.COM_STATUS_CONTAINERS_DOWNLOADED)) {
                                    mStep = STEP_WIFI_LIST;
                                    handleRequestEvent();
                                } else if (comStatus == ProgressResult.COM_STATUS_CONTAINERS_STARTED) {
                                    setLoadingButtonContainerPattern(false);
                                    AOSpaceInformationActivity.administratorStartThisActivity(this, null);
                                } else {
                                    setLoadingButtonContainerPattern(false);
                                    AODeviceInitialActivity.startThisActivity(AOLocaleSettingsActivity.this
                                            , (ProgressResult.COM_STATUS_CONTAINERS_STARTING == comStatus));
                                }
                            } else {
                                mStep = STEP_WIFI_LIST;
                                handleRequestEvent();
                            }
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
