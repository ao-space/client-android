/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.ui.bind;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.bind.DistributeNetworkAdapter;
import xyz.eulix.space.adapter.bind.NetworkAccessAdapter;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.CommonDeviceInfo;
import xyz.eulix.space.bean.DistributeWLAN;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.bean.NetworkConfigDNSInfo;
import xyz.eulix.space.bean.WLANItem;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.bean.bind.WpwdInfo;
import xyz.eulix.space.bridge.DistributeNetworkBridge;
import xyz.eulix.space.bridge.NetworkConfigurationBridge;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.DIDUtils;
import xyz.eulix.space.did.bean.DIDCredentialBean;
import xyz.eulix.space.did.bean.DIDProviderBean;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.event.BoxNetworkRequestEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.DeviceNetworkEvent;
import xyz.eulix.space.event.DeviceNetworkResponseEvent;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.manager.BoxNetworkCheckManager;
import xyz.eulix.space.network.agent.InitialResults;
import xyz.eulix.space.network.agent.NetworkConfigResult;
import xyz.eulix.space.network.agent.PairingBoxInfo;
import xyz.eulix.space.network.agent.PasswordInfo;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.bind.ConnectedNetwork;
import xyz.eulix.space.network.agent.bind.ProgressResult;
import xyz.eulix.space.network.agent.bind.SpaceCreateRequest;
import xyz.eulix.space.network.agent.bind.SpaceCreateResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.presenter.DistributeNetworkPresenter;
import xyz.eulix.space.ui.AOCompleteActivity;
import xyz.eulix.space.ui.AOSpaceInformationActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ViewUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 14:05
 */
public class DistributeNetworkActivity extends AbsActivity<DistributeNetworkPresenter.IDistributeNetwork, DistributeNetworkPresenter> implements DistributeNetworkPresenter.IDistributeNetwork
        , View.OnClickListener, NetworkAccessAdapter.OnItemClickListener, DistributeNetworkAdapter.OnItemClickListener
        , AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback
        , DistributeNetworkBridge.DistributeNetworkSinkCallback, NetworkConfigurationBridge.NetworkConfigurationSourceCallback {
    private static final int UPDATE_STATE_ALL = 0;
    private static final int UPDATE_STATE_ONLY_LIST = 1;
    private static final int UPDATE_STATE_WITHOUT_LIST = -1;
    private static final int REQUEST_DEVICE_IP_ADDRESS_PERIOD = 15000;
    private static final int MINUTE_PERIOD = 60000;
    private static final int REQUEST_DEVICE_IP_ADDRESS = 1;
    private static final int ONLINE_CONNECT_WAIT = REQUEST_DEVICE_IP_ADDRESS + 1;
    private static final int ONLINE_IGNORE_WAIT = ONLINE_CONNECT_WAIT + 1;
    private String activityId;
    private DistributeWLAN mDistributeWLAN;
    private RelativeLayout titleContainer;
    private ImageButton back;
    private ImageButton backNoTitle;
    private TextView title;
    private SwipeRefreshLayout swipeRefreshContainer;
    private NestedScrollView distributeNetworkScroll;
    private ImageView titleHeaderImage;
    private TextView titleHeaderText;
    private TextView titleHeaderIntroduction;
    private RecyclerView networkAccessList;
    private View networkAccessSplit;
    private LinearLayout refreshNetworkContainer;
    private ImageView refreshNetworkIndicator;
    private TextView refreshNetworkHint;
    private RecyclerView wlanList;
    private LinearLayout emptyWlanListContainer;
    private LinearLayout loadingButtonContainer;
    private LottieAnimationView loadingAnimation;
    private TextView loadingContent;
    private Dialog distributeNetworkDialog;
    private View distributeNetworkDialogView;
    private TextView dialogTitle;
    private EditText dialogInput;
    private ImageButton dialogPrivate;
    private Button dialogCancel;
    private Button dialogConfirm;
    private Dialog emptyWlanDialog;
    private TextView emptyWlanDialogTitle;
    private TextView emptyWlanDialogContent;
    private Button emptyWlanDialogCancel;
    private Button emptyWlanDialogConfirm;
    private Dialog initialProgressDialog;
    private ProgressBar initialProgress;
    private TextView initialProgressText;
    private NetworkAccessAdapter networkAccessAdapter;
    private DistributeNetworkAdapter mAdapter;
    private boolean isOnlineDistributeNetwork = false;
    private boolean isDistributeNetworkOnly = false;
    private boolean isFastDiskInitialize = false;
    private boolean isNewBindProgress = false;
    private int mInitialEstimateTimeSec = 60;
    private boolean mLoading;
    private boolean mPrivate;
    private DistributeNetworkHandler mHandler;
    private DistributeNetworkBridge mBridge;
    private NetworkConfigurationBridge networkConfigurationBridge;
    private String mSsid;
    private String mAddress;
    private String mPassword;
    private String mAdminPassword;
    private String mPlatformUrl;
    private InitResponse mInitResponse;
    private PairingBoxInfo mPairingBoxInfo;
    private ReadyCheckResult mReadyCheckResult;
    private InputMethodManager inputMethodManager;
    private boolean onlineDistributeWait = false;
    private boolean onlineDistributeExpire = false;
    private boolean onlineIgnoreWait = false;
    private boolean onlineIgnoreExpire = false;
    private long mExitTime = 0L;
    private int progressState;
    private boolean isShowDetail;
    private Boolean isRefreshAccessNetwork;
    private Boolean isRefreshWifiList;
    private NetworkStatusResult mNetworkStatusResult;
    private boolean mNetworkConfigEnable = true;
    private AODeviceDiscoveryManager mManager;

    private TextWatcher inputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s != null) {
                setDialogButtonPattern(dialogConfirm, !s.toString().isEmpty());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Do nothing
        }
    };

    static class DistributeNetworkHandler extends Handler {
        private WeakReference<DistributeNetworkActivity> distributeNetworkActivityWeakReference;

        public DistributeNetworkHandler(DistributeNetworkActivity activity) {
            distributeNetworkActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            DistributeNetworkActivity activity = distributeNetworkActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case REQUEST_DEVICE_IP_ADDRESS:
                        String boxUuid = null;
                        String boxBind = null;
                        EulixSpaceInfo eulixBoxBaseInfo = DataUtil.getLastEulixSpace(activity.getApplicationContext());
                        if (eulixBoxBaseInfo != null) {
                            boxUuid = eulixBoxBaseInfo.getBoxUuid();
                            boxBind = eulixBoxBaseInfo.getBoxBind();
                        }
                        EventBusUtil.post(new BoxNetworkRequestEvent(boxUuid, boxBind));
                        sendEmptyMessageDelayed(REQUEST_DEVICE_IP_ADDRESS, REQUEST_DEVICE_IP_ADDRESS_PERIOD);
                        break;
                    case ONLINE_CONNECT_WAIT:
                        activity.onlineDistributeExpire = true;
                        break;
                    case ONLINE_IGNORE_WAIT:
                        activity.onlineIgnoreExpire = true;
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_distribute_network);
        titleContainer = findViewById(R.id.title_container);
        back = findViewById(R.id.back);
        backNoTitle = findViewById(R.id.back_no_title);
        title = findViewById(R.id.title);
//        networkSsid = findViewById(R.id.network_ssid);
//        ipAddress = findViewById(R.id.ip_address);
        swipeRefreshContainer = findViewById(R.id.swipe_refresh_container);
        distributeNetworkScroll = findViewById(R.id.distribute_network_scroll);
        titleHeaderImage = findViewById(R.id.title_header_image);
        titleHeaderText = findViewById(R.id.title_header_text);
        titleHeaderIntroduction = findViewById(R.id.title_header_introduction);
        networkAccessList = findViewById(R.id.network_access_list);
        networkAccessSplit = findViewById(R.id.network_access_split);
        refreshNetworkContainer = findViewById(R.id.refresh_network_container);
        refreshNetworkIndicator = findViewById(R.id.refresh_network_indicator);
        refreshNetworkHint = findViewById(R.id.refresh_network_hint);
        wlanList = findViewById(R.id.wlan_list);
        emptyWlanListContainer = findViewById(R.id.empty_wlan_list_container);
        loadingButtonContainer = findViewById(R.id.loading_button_container);
        loadingAnimation = findViewById(R.id.loading_animation);
        loadingContent = findViewById(R.id.loading_content);

        distributeNetworkDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_edit_private_dialog, null);
        dialogTitle = distributeNetworkDialogView.findViewById(R.id.dialog_title);
        dialogInput = distributeNetworkDialogView.findViewById(R.id.dialog_input);
        dialogPrivate = distributeNetworkDialogView.findViewById(R.id.dialog_private);
        dialogCancel = distributeNetworkDialogView.findViewById(R.id.dialog_cancel);
        dialogConfirm = distributeNetworkDialogView.findViewById(R.id.dialog_confirm);
        distributeNetworkDialog = new Dialog(this, R.style.EulixDialog);
        distributeNetworkDialog.setCancelable(false);
        distributeNetworkDialog.setContentView(distributeNetworkDialogView);

        View emptyWlanDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        emptyWlanDialogTitle = emptyWlanDialogView.findViewById(R.id.dialog_title);
        emptyWlanDialogContent = emptyWlanDialogView.findViewById(R.id.dialog_content);
        emptyWlanDialogCancel = emptyWlanDialogView.findViewById(R.id.dialog_cancel);
        emptyWlanDialogConfirm = emptyWlanDialogView.findViewById(R.id.dialog_confirm);
        emptyWlanDialog = new Dialog(this, R.style.EulixDialog);
        emptyWlanDialog.setCancelable(false);
        emptyWlanDialog.setContentView(emptyWlanDialogView);

        View initialProgressDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_initial_progress, null);
        initialProgress = initialProgressDialogView.findViewById(R.id.initial_progress);
        initialProgressText = initialProgressDialogView.findViewById(R.id.initial_progress_text);
        initialProgressDialog = new Dialog(this, R.style.EulixDialog);
        initialProgressDialog.setCancelable(false);
        initialProgressDialog.setContentView(initialProgressDialogView);
    }

    @Override
    public void initData() {
        mHandler = new DistributeNetworkHandler(this);
        EventBusUtil.register(this);
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
        mInitResponse = mManager.getInitResponse();
        mBridge = DistributeNetworkBridge.getInstance();
        mBridge.registerSinkCallback(this);
        mLoading = false;
        handleIntent(getIntent());
        progressState = (isFastDiskInitialize ? DistributeNetworkBridge.PROGRESS_SPACE_READY_CHECK
                : (mManager.isNewBindProcessSupport() ? ((mInitResponse.getPaired() == 1)
                ? DistributeNetworkBridge.PROGRESS_BIND_COMMUNICATION_PROGRESS
                : DistributeNetworkBridge.PROGRESS_BIND_SPACE_CREATE)
                : DistributeNetworkBridge.PROGRESS_PAIR));
        if (mInitResponse != null) {
            isNewBindProgress = mInitResponse.isNewBindProcessSupport();
            mPlatformUrl = mInitResponse.getSspUrl();
            if (!isDistributeNetworkOnly) {
                mInitialEstimateTimeSec = Math.max(mInitResponse.getInitialEstimateTimeSec(), 1);
            }
        }
        if (!isDistributeNetworkOnly) {
            mDistributeWLAN = mManager.getDistributeWLAN();
        }
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void initViewData() {
        back.setVisibility((isDistributeNetworkOnly || isNewBindProgress) ? View.VISIBLE : View.GONE);
        backNoTitle.setVisibility((isDistributeNetworkOnly || isNewBindProgress) ? View.VISIBLE : View.GONE);
        title.setText(R.string.network);
        titleContainer.setBackgroundColor(Color.WHITE);
        titleHeaderImage.setImageResource(R.drawable.image_network_2x);
        titleHeaderText.setText(R.string.network);
        titleHeaderIntroduction.setVisibility(View.VISIBLE);
        titleHeaderIntroduction.setText(R.string.network_introduction);
        refreshNetworkContainer.setVisibility(View.GONE);
        dialogInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        dialogInput.setHint(R.string.please_input_password);
        dialogConfirm.setText(R.string.join);
        dialogInput.setOnEditorActionListener((v, actionId, event) -> {
            if (!dialogInput.getText().toString().isEmpty() && actionId == EditorInfo.IME_ACTION_DONE) {
                handleDialogConfirm();
            }
            return false;
        });
        loadingButtonContainer.setVisibility(isDistributeNetworkOnly ? View.GONE : View.VISIBLE);

        emptyWlanDialogTitle.setText(R.string.empty_wlan_title);
        emptyWlanDialogContent.setText(R.string.empty_wlan_content);
        emptyWlanDialogConfirm.setText(R.string.ok);
        setRefreshNetworkPattern(false);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        backNoTitle.setOnClickListener(this);
//        refreshNetworkContainer.setOnClickListener(this);
        back.setClickable(isDistributeNetworkOnly || isNewBindProgress);
        backNoTitle.setClickable(isDistributeNetworkOnly || isNewBindProgress);
        loadingButtonContainer.setOnClickListener(this);
        loadingButtonContainer.setClickable(!isDistributeNetworkOnly);
        swipeRefreshContainer.setOnRefreshListener(this::requestRefreshDistributeNetwork);
        swipeRefreshContainer.setEnabled(false);
        dialogPrivate.setOnClickListener(this);
        dialogCancel.setOnClickListener(v -> dismissDistributeNetworkDialog());
        dialogConfirm.setOnClickListener(v -> handleDialogConfirm());
        emptyWlanDialogCancel.setOnClickListener(v -> dismissEmptyWlanDialog());
        emptyWlanDialogConfirm.setOnClickListener(v -> {
            dismissEmptyWlanDialog();
            if (isDistributeNetworkOnly) {
                distributeOnlyPrepareFinish();
                finish();
            } else if (mBridge != null) {
                handleResult(false, false);
            }
        });
        updateLoading(UPDATE_STATE_ALL);
        List<WLANItem> wlanItems = null;
        if (mDistributeWLAN != null) {
            wlanItems = mDistributeWLAN.getWlanItemList();
        }
        if (wlanItems == null) {
            wlanItems = new ArrayList<>();
        }
        List<NetworkAccessBean> networkAccessBeans = null;
        if (mDistributeWLAN != null) {
            networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
        }
        if (networkAccessSplit != null) {
            networkAccessSplit.setVisibility((networkAccessBeans != null && networkAccessBeans.size() > 0) ? View.VISIBLE : View.GONE);
        }
        networkAccessAdapter = new NetworkAccessAdapter(this, networkAccessBeans);
        networkAccessAdapter.setOnItemClickListener(this);
        networkAccessList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        networkAccessList.addItemDecoration(new NetworkAccessAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        networkAccessList.setAdapter(networkAccessAdapter);
        mAdapter = new DistributeNetworkAdapter(this, wlanItems);
        mAdapter.setOnItemClickListener(this);
        wlanList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        wlanList.addItemDecoration(new DistributeNetworkAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        wlanList.setAdapter(mAdapter);
        if (isDistributeNetworkOnly) {
            if (isOnlineDistributeNetwork) {
                setRefreshNetworkPattern(true);
                requestWifiList();
            }
            refreshAccessNetwork();
        } else {
            handleWlanListUpdate(wlanItems);
            if (wlanItems.size() <= 0) {
                showEmptyWlanDialog();
            }
//            if (mBridge != null) {
//                mBridge.getAccessNetwork();
//            }
            if (mManager != null) {
                mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG, null);
            }
        }

        setTitlePattern(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            distributeNetworkScroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> setTitlePattern(Math.abs(scrollY) < getResources().getDimensionPixelSize(R.dimen.dp_121)));
        }
    }

    private void setTitlePattern(boolean isScrollToTop) {
        titleContainer.setVisibility(isScrollToTop ? View.GONE : View.VISIBLE);
        if (isDistributeNetworkOnly || isNewBindProgress) {
            backNoTitle.setVisibility(isScrollToTop ? View.VISIBLE : View.GONE);
        }
    }

    private void requestRefreshDistributeNetwork() {
        isRefreshAccessNetwork = false;
        isRefreshWifiList = false;
        setRefreshNetworkPattern(true);
        if (!refreshWlanList()) {
            isRefreshWifiList = true;
            setRefreshNetworkPattern(false);
            checkRefreshComplete();
        }
        if (isRefreshWifiList || isOnlineDistributeNetwork) {
            refreshAccessNetwork(true);
        }
    }

    private void checkRefreshComplete() {
        if (isRefreshWifiList != null && isRefreshWifiList && isRefreshAccessNetwork != null && isRefreshAccessNetwork) {
            isRefreshWifiList = null;
            isRefreshAccessNetwork = null;
            if (swipeRefreshContainer != null) {
                swipeRefreshContainer.setRefreshing(false);
            }
        }
    }

    private boolean handleIntent(Intent intent) {
        boolean result = false;
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            isOnlineDistributeNetwork = intent.getBooleanExtra(ConstantField.ONLINE_DISTRIBUTE, false);
            isDistributeNetworkOnly = intent.getBooleanExtra(ConstantField.DISTRIBUTE_NETWORK, false);
            isFastDiskInitialize = intent.getBooleanExtra(ConstantField.FAST_DISK_INITIALIZE, false);
            if (intent.hasExtra(ConstantField.WIFI_SSIDS)) {
                String distributeWlanValue = intent.getStringExtra(ConstantField.WIFI_SSIDS);
                if (distributeWlanValue != null) {
                    DistributeWLAN distributeWLAN = null;
                    try {
                        distributeWLAN = new Gson().fromJson(distributeWlanValue, DistributeWLAN.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    if (distributeWLAN != null) {
                        mNetworkConfigEnable = distributeWLAN.isNetworkConfigEnable();
                        mDistributeWLAN = distributeWLAN;
                        updateLoading(UPDATE_STATE_ALL);
                        result = true;
                        if (!isDistributeNetworkOnly) {
                            List<WLANItem> wlanItems = distributeWLAN.getWlanItemList();
                            if (wlanItems == null || wlanItems.size() <= 0) {
                                showEmptyWlanDialog();
                            }
                        }
                    }
                }
            }
            if (intent.hasExtra(ConstantField.PASSWORD)) {
                mAdminPassword = intent.getStringExtra(ConstantField.PASSWORD);
            }
            if (isOnlineDistributeNetwork && mHandler != null) {
                while (mHandler.hasMessages(REQUEST_DEVICE_IP_ADDRESS)) {
                    mHandler.removeMessages(REQUEST_DEVICE_IP_ADDRESS);
                }
                mHandler.sendEmptyMessageDelayed(REQUEST_DEVICE_IP_ADDRESS, REQUEST_DEVICE_IP_ADDRESS_PERIOD);
            }
        }
        return result;
    }

    private void handleWlanListUpdate(List<WLANItem> wlanItemList) {
        if (wlanList != null && emptyWlanListContainer != null) {
            if (wlanItemList == null || wlanItemList.isEmpty()) {
                wlanList.setVisibility(View.GONE);
                emptyWlanListContainer.setVisibility(View.VISIBLE);
            } else {
                emptyWlanListContainer.setVisibility(View.GONE);
                wlanList.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateLoading(int state) {
        boolean isEnable = false;
        String networkSsidValue = null;
        String ipAddressValue = null;
        if (mDistributeWLAN != null) {
            networkSsidValue = mDistributeWLAN.getConnectedWlanSsid();
            List<String> ipAddresses = mDistributeWLAN.getIpAddresses();
            if (ipAddresses != null && ipAddresses.size() > 0) {
                ipAddressValue = ipAddresses.get(0);
                if (ipAddressValue != null) {
                    int index = ipAddressValue.lastIndexOf(":");
                    if (index > 0 && index < ipAddressValue.length()) {
                        ipAddressValue = ipAddressValue.substring(0, index);
                    }
                }
            }
            if (isNewBindProgress) {
                List<NetworkAccessBean> networkAccessBeanList = mDistributeWLAN.getNetworkAccessBeanList();
                isEnable = (networkAccessBeanList != null && !networkAccessBeanList.isEmpty());
            } else {
                isEnable = (mDistributeWLAN.isConnect());
            }
            if (mAdapter != null && (state == UPDATE_STATE_ALL || state == UPDATE_STATE_ONLY_LIST)) {
                List<WLANItem> wlanItemList = mDistributeWLAN.getWlanItemList();
                mAdapter.updateData(wlanItemList, isShowDetail);
                handleWlanListUpdate(wlanItemList);
            }
            if (networkAccessAdapter != null && (state == UPDATE_STATE_ALL || state == UPDATE_STATE_WITHOUT_LIST)) {
                List<NetworkAccessBean> networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
                networkAccessAdapter.updateData(networkAccessBeans);
                if (networkAccessSplit != null) {
                    networkAccessSplit.setVisibility((networkAccessBeans != null && networkAccessBeans.size() > 0)
                            ? View.VISIBLE : View.GONE);
                }
            }
        }
        if (!isDistributeNetworkOnly && mManager != null) {
            mManager.setDistributeWLAN(mDistributeWLAN);
        }
//        if (networkSsid != null && ipAddress != null && (state == UPDATE_STATE_ALL || state == UPDATE_STATE_WITHOUT_LIST)) {
//            networkSsid.setText((isEnable ? StringUtil.nullToEmpty(networkSsidValue) : getString(R.string.device_offline)));
//            networkSsid.setTextColor(getResources().getColor(isEnable
//                    ? R.color.gray_ff85899c : R.color.blue_ff337aff));
//            ipAddress.setText((ipAddressValue == null ? "" : ipAddressValue));
//        }
//        if (back != null) {
//            back.setClickable(isDistributeNetworkOnly || isNewBindProgress);
//            back.setVisibility((isDistributeNetworkOnly || isNewBindProgress) ? View.VISIBLE : View.GONE);
//        }
        if (loadingButtonContainer != null) {
            loadingButtonContainer.setClickable(!isDistributeNetworkOnly);
            loadingButtonContainer.setVisibility(isDistributeNetworkOnly ? View.GONE : View.VISIBLE);
        }
        isEnable = (isEnable || !mNetworkConfigEnable);
        updateLoading(isEnable, mLoading);
    }

    private void updateLoading(boolean isEnable, boolean isLoading) {
        isLoading = (isLoading && isEnable);
        if (loadingButtonContainer != null) {
            loadingButtonContainer.setBackgroundResource(isEnable ? R.drawable.background_ff337aff_ff16b9ff_rectangle_10
                    : R.drawable.background_ffdfe0e5_rectangle_10);
            loadingButtonContainer.setClickable(isEnable && !isLoading);
        }
        if (loadingContent != null) {
            loadingContent.setText(isNewBindProgress ? R.string.string_continue
                    : (isLoading ? R.string.register_device : R.string.next_step));
        }
        if (loadingAnimation != null) {
            if (isLoading) {
                loadingAnimation.setVisibility(View.VISIBLE);
                LottieUtil.loop(loadingAnimation, "loading_button.json");
            } else {
                LottieUtil.stop(loadingAnimation);
                loadingAnimation.setVisibility(View.GONE);
            }
        }
    }

    private void handleLoadingButtonContainerClickEvent() {
        if (!isDistributeNetworkOnly) {
            if (!isNewBindProgress && presenter != null && mDistributeWLAN != null && mManager != null) {
                if (initialProgress != null) {
                    initialProgress.setMax(mInitialEstimateTimeSec);
                    initialProgress.setProgress(0);
                }
                if (initialProgressText != null) {
                    String content = (getString(R.string.initial_progress_indicator) + "...");
                    initialProgressText.setText(content);
                }
                presenter.startCountdown(mInitialEstimateTimeSec);
                showInitialProgressDialog();
                mLoading = true;
            }
            updateLoading(true, true);
            handleRequestEvent();
        }
    }

    private void handleRequestEvent() {
        boolean isHandle = (mManager != null);
        if (isHandle) {
            switch (progressState) {
                case DistributeNetworkBridge.PROGRESS_PAIR:
                    //                    mBridge.pairing();
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_PAIRING, null);
                    break;
                case DistributeNetworkBridge.PROGRESS_INITIALIZE:
                    PasswordInfo passwordInfo = new PasswordInfo();
                    passwordInfo.setPassword(mAdminPassword);
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_INITIAL
                            , new Gson().toJson(passwordInfo, PasswordInfo.class));
                    break;
                case DistributeNetworkBridge.PROGRESS_BIND_COMMUNICATION_PROGRESS:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS, null);
                    break;
                case DistributeNetworkBridge.PROGRESS_BIND_SPACE_CREATE:
                    String adminPassword = mManager.getAdminPassword();
                    SpaceCreateRequest spaceCreateRequest = new SpaceCreateRequest();
                    spaceCreateRequest.setClientPhoneModel(SystemUtil.getPhoneModel());
                    spaceCreateRequest.setClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                    spaceCreateRequest.setPassword(adminPassword);
                    Map<String, String> queryMapBinder = new HashMap<>();
                    String encodeClientUuid = null;
                    try {
                        encodeClientUuid = URLEncoder.encode(DataUtil.getClientUuid(getApplicationContext()), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (encodeClientUuid != null) {
                        queryMapBinder.put(VerificationMethod.QUERY_CLIENT_UUID, encodeClientUuid);
                    }
                    queryMapBinder.put(VerificationMethod.QUERY_CREDENTIAL_TYPE, VerificationMethod.CREDENTIAL_TYPE_BINDER);
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
                    AOSpaceUtil.insertOrUpdateDID(getApplicationContext(), mManager.getBoxUuid(), "1", didCredentialBean);

                    List<VerificationMethod> verificationMethods = null;
                    VerificationMethod binderMethod = null;
                    if (binderPublicKey != null) {
                        binderMethod = DIDUtils.generateDIDVerificationMethod(VerificationMethod.DID_AO_SPACE_KEY_PREFIX
                                , version, queryMapBinder, null, StringUtil.wrapPublicKeyNewLine(binderPublicKey)
                                , VerificationMethod.TYPE_RSA_VERIFICATION_KEY_2018);
                    }
                    if (binderMethod != null/* || passwordOnBinderMethod != null*/) {
                        verificationMethods = new ArrayList<>();
                        verificationMethods.add(binderMethod);
                    }
                    spaceCreateRequest.setVerificationMethod(verificationMethods);
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE
                            , new Gson().toJson(spaceCreateRequest, SpaceCreateRequest.class));
                    break;
                case DistributeNetworkBridge.PROGRESS_SPACE_READY_CHECK:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
                    break;
                case DistributeNetworkBridge.PROGRESS_DISK_RECOGNITION:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_RECOGNITION, null);
                    break;
                case DistributeNetworkBridge.PROGRESS_DISK_MANAGEMENT_LIST:
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
        mLoading = false;
        if (presenter != null) {
            presenter.stopCountdown();
        }
        dismissInitialProgressDialog();
        setRefreshNetworkPattern(false);
        checkRefreshComplete();
        updateLoading(UPDATE_STATE_WITHOUT_LIST);
        showServerExceptionToast();
    }

    private void handleConnectErrorEvent(int code) {
        mLoading = false;
        if (presenter != null) {
            presenter.stopCountdown();
        }
        dismissInitialProgressDialog();
        updateLoading(UPDATE_STATE_WITHOUT_LIST);
        switch (code) {
            case ConstantField.SERVER_EXCEPTION_CODE:
                showServerExceptionToast();
                break;
            default:
                showImageTextToast(R.drawable.toast_wrong, R.string.connect_fail);
                break;
        }
    }

    private void setRefreshNetworkPattern(boolean isRefresh) {
//        refreshNetworkContainer.setClickable(!isRefresh);
        if (isRefresh) {
            refreshNetworkContainer.setVisibility(View.VISIBLE);
            ViewUtils.setLoadingAnim(this, refreshNetworkIndicator);
        } else {
            clearAni(refreshNetworkIndicator);
            refreshNetworkContainer.setVisibility(View.GONE);
        }
//        refreshNetworkHint.setText(isRefresh ? R.string.loading_hint : R.string.click_to_refresh);
    }


    private void clearAni(ImageView imageView) {
        if (imageView != null) {
            imageView.clearAnimation();
        }
    }

    private void requestWifiList() {
        if (presenter != null) {
            presenter.getWifiList();
        }
    }

    private void requestAccessNetwork(boolean isRefresh) {
        if (presenter != null) {
            presenter.getAccessNetwork(isRefresh);
        }
    }

    private String getWlanListSsid(int position) {
        String ssid = null;
        if (mDistributeWLAN != null) {
            List<WLANItem> wlanItems = mDistributeWLAN.getWlanItemList();
            if (position >= 0 && wlanItems != null && wlanItems.size() > position) {
                WLANItem wlanItem = wlanItems.get(position);
                if (wlanItem != null) {
                    ssid = wlanItem.getWlanSsid();
                }
            }
        }
        return ssid;
    }

    private String getWlanListAddress(int position) {
        String address = null;
        if (mDistributeWLAN != null) {
            List<WLANItem> wlanItems = mDistributeWLAN.getWlanItemList();
            if (position >= 0 && wlanItems != null && wlanItems.size() > position) {
                WLANItem wlanItem = wlanItems.get(position);
                if (wlanItem != null) {
                    address = wlanItem.getWlanAddress();
                }
            }
        }
        return address;
    }

    private void showDistributeNetworkDialog() {
        if (distributeNetworkDialog != null && !distributeNetworkDialog.isShowing()) {
            dialogInput.setEnabled(true);
            String passwordContent = StringUtil.nullToEmpty(DataUtil.getNetworkPassword(getApplicationContext(), mAddress));
            dialogInput.setText(passwordContent);
            setDialogPrivate(true);
            setDialogButtonPattern(dialogCancel, true);
            setDialogButtonPattern(dialogConfirm, !passwordContent.isEmpty());
            distributeNetworkDialog.show();
            Window window = distributeNetworkDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), getResources().getDimensionPixelSize(R.dimen.dp_133));
            }
        }
    }

    private void dismissDistributeNetworkDialog() {
        if (distributeNetworkDialog != null && distributeNetworkDialog.isShowing()) {
            dialogInput.removeTextChangedListener(inputWatcher);
            distributeNetworkDialog.dismiss();
        }
    }

    private void showEmptyWlanDialog() {
        if (mNetworkConfigEnable && !isFinishing() && emptyWlanDialog != null && !emptyWlanDialog.isShowing()) {
            emptyWlanDialog.show();
            Window window = emptyWlanDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissEmptyWlanDialog() {
        if (emptyWlanDialog != null && emptyWlanDialog.isShowing()) {
            emptyWlanDialog.dismiss();
        }
    }

    private void showInitialProgressDialog() {
        if (initialProgressDialog != null && !initialProgressDialog.isShowing()) {
            initialProgressDialog.show();
            Window window = initialProgressDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_288), getResources().getDimensionPixelSize(R.dimen.dp_336));
            }
        }
    }

    private void dismissInitialProgressDialog() {
        if (initialProgressDialog != null && initialProgressDialog.isShowing()) {
            initialProgressDialog.dismiss();
        }
    }

    private void setInitialProgressPattern(int progress, int maxProgress) {
        if (initialProgressText != null && initialProgress != null) {
            if (initialProgress.getMax() != maxProgress && maxProgress >= progress) {
                initialProgress.setMax(Math.max(maxProgress, 1));
            }
            int realProgress = Math.max(Math.min(progress, mInitialEstimateTimeSec), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                initialProgress.setProgress(realProgress, true);
            } else {
                initialProgress.setProgress(realProgress);
            }
//            int index = (realProgress % 4);
            int index = 3;
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(getString(R.string.initial_progress_indicator));
            while (index > 0) {
                contentBuilder.append(".");
                index -= 1;
            }
            initialProgressText.setText(contentBuilder);
        }
    }

    private void setDialogPrivate(boolean nPrivate) {
        mPrivate = nPrivate;
        dialogPrivate.setImageResource(mPrivate ? R.drawable.icon_private_2x : R.drawable.icon_public_2x);
        dialogInput.removeTextChangedListener(inputWatcher);
        dialogInput.setInputType(mPrivate ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        dialogInput.setSelection(dialogInput.getText().length());
        dialogInput.addTextChangedListener(inputWatcher);
    }

    private void setDialogButtonPattern(Button button, boolean isWork) {
        if (button != null) {
            button.setClickable(isWork);
            button.setTextColor(getResources().getColor(isWork ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
        }
    }

    private boolean refreshWlanList() {
        boolean isRefresh = true;
        if (isOnlineDistributeNetwork) {
            requestWifiList();
        } else if (isDistributeNetworkOnly) {
            if (mBridge != null) {
                mBridge.getWlanList();
            } else {
                isRefresh = false;
            }
        } else if (mManager != null) {
            isRefresh = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_WIFI_LIST, null);
        } else {
            isRefresh = false;
        }
        return isRefresh;
    }

    private void refreshAccessNetwork() {
        refreshAccessNetwork(false);
    }

    private void refreshAccessNetwork(boolean isRefresh) {
        if (isOnlineDistributeNetwork) {
            requestAccessNetwork(isRefresh);
        } else if (isDistributeNetworkOnly) {
            if (mBridge != null) {
                mBridge.getAccessNetwork();
            }
        } else if (mManager != null) {
            if (!mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG, null)) {
                handleAccessNetworkResult(ConstantField.SERVER_EXCEPTION_CODE, null, null);
            }
        }
    }

    private void cancelOnlineDistributeNetworkWait() {
        if (mHandler != null) {
            while (mHandler.hasMessages(ONLINE_CONNECT_WAIT)) {
                mHandler.removeMessages(ONLINE_CONNECT_WAIT);
            }
        }
        onlineDistributeWait = false;
        BoxNetworkCheckManager.setShowOffline(true);
    }

    private void cancelOnlineIgnoreNetworkWait() {
        if (mHandler != null) {
            while (mHandler.hasMessages(ONLINE_IGNORE_WAIT)) {
                mHandler.removeMessages(ONLINE_IGNORE_WAIT);
            }
        }
        onlineIgnoreWait = false;
    }

    private void handleDialogConfirm() {
        forceHideSoftInput(distributeNetworkDialogView.getWindowToken());
        mPassword = dialogInput.getText().toString();
        DataUtil.setNetworkPassword(getApplicationContext(), mAddress, mPassword);
        if (isOnlineDistributeNetwork) {
            cancelOnlineDistributeNetworkWait();
            if (presenter != null) {
                onlineDistributeWait = true;
                BoxNetworkCheckManager.setShowOffline(false);
                dialogInput.setEnabled(false);
                setDialogButtonPattern(dialogCancel, false);
                setDialogButtonPattern(dialogConfirm, false);
                presenter.setWifi(mSsid, mAddress, mPassword);
                String connectContent = getString(R.string.connect_wlan_content_part_1)
                        + mSsid + getString(R.string.connect_wlan_content_part_2);
//                dismissDistributeNetworkDialog();
//                showPureTextToast(connectContent);
                showLoading("");
            }
            if (mHandler != null) {
                onlineDistributeExpire = false;
                mHandler.sendEmptyMessageDelayed(ONLINE_CONNECT_WAIT, MINUTE_PERIOD);
            }
        } else if (isDistributeNetworkOnly) {
            if (mBridge != null) {
                dialogInput.setEnabled(false);
                setDialogButtonPattern(dialogCancel, false);
                setDialogButtonPattern(dialogConfirm, false);
                mBridge.selectWlan(mSsid, mAddress, mPassword);
                String connectContent = getString(R.string.connect_wlan_content_part_1)
                        + mSsid + getString(R.string.connect_wlan_content_part_2);
                showLoading("");
            } else {
                dismissDistributeNetworkDialog();
            }
        } else {
            if (mManager != null) {
                WpwdInfo wpwdInfo = new WpwdInfo();
                wpwdInfo.setAddr(mAddress);
                wpwdInfo.setPwd(mPassword);
                if (mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SET_WIFI
                        , new Gson().toJson(wpwdInfo, WpwdInfo.class))) {
                    String connectContent = getString(R.string.connect_wlan_content_part_1)
                            + mSsid + getString(R.string.connect_wlan_content_part_2);
                    dialogInput.setEnabled(false);
                    setDialogButtonPattern(dialogCancel, false);
                    setDialogButtonPattern(dialogConfirm, false);
                    showLoading("");
                } else {
                    showServerExceptionToast();
                }
            } else {
                dismissDistributeNetworkDialog();
            }
        }
    }

    /**
     * 收起指定窗口软键盘
     * @param windowToken
     */
    private void forceHideSoftInput(IBinder windowToken) {
        if (inputMethodManager != null && windowToken != null) {
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    private void startNetworkConfigurationActivity(NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter) {
        networkConfigurationBridge = NetworkConfigurationBridge.getInstance();
        networkConfigurationBridge.registerSourceCallback(this);
        Intent intent = new Intent(DistributeNetworkActivity.this, NetworkConfigurationActivity.class);
        if (networkConfigDNSInfo != null) {
            intent.putExtra(ConstantField.NETWORK_CONFIG_DNS, new Gson().toJson(networkConfigDNSInfo, NetworkConfigDNSInfo.class));
        }
        String dataUuid = null;
        if (networkAdapter != null) {
            dataUuid = DataUtil.setData(new Gson().toJson(networkAdapter, NetworkAdapter.class));
        }
        if (dataUuid != null) {
            intent.putExtra(ConstantField.DATA_UUID, dataUuid);
        }
        intent.putExtra(ConstantField.DISTRIBUTE_NETWORK, isDistributeNetworkOnly);
        startActivity(intent);
    }

    private void startBindResultActivity(boolean isBind, int bindResultCode) {
        if (mBridge != null) {
            mBridge.handleBindResult(true);
        }
        Intent intent = new Intent(DistributeNetworkActivity.this, BindResultActivity.class);
        intent.putExtra(ConstantField.BIND_TYPE, isBind);
        intent.putExtra(ConstantField.BIND_RESULT, bindResultCode);
        if (mPlatformUrl != null) {
            intent.putExtra(ConstantField.PLATFORM_URL, mPlatformUrl);
        }
        startActivityForResult(intent, ConstantField.RequestCode.BIND_RESULT_CODE);
    }

    private void exitBindResult() {
        progressState = (mManager.isNewBindProcessSupport() ? DistributeNetworkBridge.PROGRESS_BIND_SPACE_CREATE
                : DistributeNetworkBridge.PROGRESS_PAIR);
        handleLoadingButtonContainerClickEvent();
    }

    private void distributeOnlyPrepareFinish() {
        if (!isOnlineDistributeNetwork && isDistributeNetworkOnly && mBridge != null) {
            mBridge.handleDistributeNetwork();
        }
    }

    private void handleResult(boolean isOk, boolean isSourceFinish) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        intent.putExtra("source_finish", isSourceFinish);
        finish();
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            distributeOnlyPrepareFinish();
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public DistributeNetworkPresenter createPresenter() {
        return new DistributeNetworkPresenter();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (handleIntent(intent) && mAdapter != null && mDistributeWLAN != null) {
            List<WLANItem> wlanItemList = mDistributeWLAN.getWlanItemList();
            mAdapter.updateData(wlanItemList, isShowDetail);
            handleWlanListUpdate(wlanItemList);
        }
        // 不能连续搜索两次wlan
//        if (isOnlineDistributeNetwork) {
//            requestWifiList();
//        }
    }

    @Override
    protected void onDestroy() {
        EventBusUtil.unRegister(this);
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        activityId = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isDistributeNetworkOnly || isNewBindProgress) {
            distributeOnlyPrepareFinish();
            super.onBackPressed();
        } else {
            confirmForceExit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isDistributeNetworkOnly || isNewBindProgress || keyCode != KeyEvent.KEYCODE_BACK) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                distributeOnlyPrepareFinish();
            }
            return super.onKeyDown(keyCode, event);
        } else {
            confirmForceExit();
            return true;
        }
    }

    @Override
    public void wifiListCallback(List<WifiInfo> wifiInfoList) {
        if (mDistributeWLAN == null) {
            mDistributeWLAN = new DistributeWLAN();
        }
        List<WLANItem> wlanItems = null;
        if (wifiInfoList != null) {
            wlanItems = new ArrayList<>();
            for (WifiInfo wifiInfo : wifiInfoList) {
                if (wifiInfo != null) {
                    WLANItem wlanItem = new WLANItem();
                    wlanItem.setWlanAddress(wifiInfo.getSsid());
                    wlanItem.setWlanSsid(wifiInfo.getName());
                    wlanItems.add(wlanItem);
                }
            }
        }
        mDistributeWLAN.setWlanItemList(wlanItems);
        if (mHandler != null) {
            List<WLANItem> finalWlanItems = wlanItems;
            mHandler.post(() -> {
                if (isRefreshWifiList != null) {
                    isRefreshWifiList = true;
                }
                checkRefreshComplete();
                updateLoading(UPDATE_STATE_ONLY_LIST);
                setRefreshNetworkPattern(false);
                if (isDistributeNetworkOnly && (finalWlanItems == null || finalWlanItems.size() <= 0)) {
                    showEmptyWlanDialog();
                }
            });
        }
    }

    @Override
    public void networkAccessCallback(Boolean isSuccess, NetworkStatusResult networkStatusResult) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (swipeRefreshContainer != null) {
                    if (!swipeRefreshContainer.isEnabled()) {
                        swipeRefreshContainer.setEnabled(true);
                    }
                }
                if (isRefreshAccessNetwork != null) {
                    isRefreshAccessNetwork = true;
                }
                checkRefreshComplete();
                if (isSuccess != null && !isShowDetail) {
                    isShowDetail = isSuccess;
                }
                if (networkStatusResult != null) {
                    mNetworkStatusResult = networkStatusResult;
                    if (mDistributeWLAN == null) {
                        mDistributeWLAN = new DistributeWLAN();
                    }
                    mDistributeWLAN.setConnect(networkStatusResult.isInternetAccess());
                    List<NetworkAccessBean> networkAccessBeans = null;
                    List<NetworkAdapter> networkAdapters = networkStatusResult.getNetworkAdapters();
                    if (networkAdapters != null) {
                        networkAccessBeans = new ArrayList<>();
                        for (NetworkAdapter networkAdapter : networkAdapters) {
                            if (networkAdapter != null && networkAdapter.isConnected()) {
                                boolean isWired = networkAdapter.isWired();
                                NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                                networkAccessBean.setShowDetail(isShowDetail);
                                networkAccessBean.setConnect(networkStatusResult.isInternetAccess());
                                networkAccessBean.setWired(networkAdapter.isWired());
                                networkAccessBean.setNetworkName(isWired ? networkAdapter.getAdapterName() : networkAdapter.getwIFIName());
                                networkAccessBeans.add(networkAccessBean);
                            }
                        }
                    }
                    mDistributeWLAN.setConnect((networkStatusResult.isInternetAccess() && networkAccessBeans != null && !networkAccessBeans.isEmpty()));
                    mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeans);
                    updateLoading(UPDATE_STATE_ALL);
                }
                NetworkConfigDNSInfo networkConfigDNSInfo = null;
                NetworkAdapter networkAdapter = null;
                List<NetworkAdapter> networkAdapters = null;
                if (mNetworkStatusResult != null) {
                    networkConfigDNSInfo = new NetworkConfigDNSInfo();
                    networkConfigDNSInfo.setIpv4Dns1(mNetworkStatusResult.getdNS1());
                    networkConfigDNSInfo.setIpv4Dns2(mNetworkStatusResult.getdNS2());
                    networkConfigDNSInfo.setIpv6Dns1(mNetworkStatusResult.getIpv6DNS1());
                    networkConfigDNSInfo.setIpv6Dns2(mNetworkStatusResult.getIpv6DNS2());
                    networkAdapters = mNetworkStatusResult.getNetworkAdapters();
                }
                if (networkConfigurationBridge != null) {
                    networkAdapter = networkConfigurationBridge.getCurrentNetworkAdapter();
                    if (networkAdapter != null) {
                        boolean isWired = networkAdapter.isWired();
                        String name = (isWired ? networkAdapter.getAdapterName() : networkAdapter.getwIFIName());
                        if (networkAdapters != null) {
                            for (NetworkAdapter adapter : networkAdapters) {
                                if (adapter != null && (isWired == adapter.isWired()) && name != null
                                        && ((isWired && name.equals(adapter.getAdapterName()))
                                        || (!isWired && name.equals(adapter.getwIFIName())))) {
                                    networkAdapter = adapter;
                                }
                            }
                        }
                    }
                    networkConfigurationBridge.handleRefreshAccessNetwork(networkConfigDNSInfo, networkAdapter);
                }
            });
        }
    }

    @Override
    public void setNetworkConfigCallback(int code, String source) {
        if (code < 100 || code >= 200) {
            cancelOnlineDistributeNetworkWait();
        }
        handleSetNetworkConfigCallback(code, source);
    }

    private void handleSetNetworkConfigCallback(int code, String source) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (code >= 200 && code < 300) {
                    if (mSsid != null) {
                        List<NetworkAccessBean> networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
                        boolean isContains = false;
                        if (networkAccessBeans != null) {
                            for (NetworkAccessBean networkAccessBean : networkAccessBeans) {
                                if (networkAccessBean != null && !networkAccessBean.isWired() && mSsid.equals(networkAccessBean.getNetworkName())) {
                                    networkAccessBean.setConnect(true);
                                    isContains = true;
                                }
                            }
                        }
                        if (!isContains) {
                            if (networkAccessBeans != null) {
                                Iterator<NetworkAccessBean> iterator = networkAccessBeans.iterator();
                                while (iterator.hasNext()) {
                                    NetworkAccessBean bean = iterator.next();
                                    if (bean != null && !bean.isWired()) {
                                        iterator.remove();
                                    }
                                }
                            } else {
                                networkAccessBeans = new ArrayList<>();
                            }
                            NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                            networkAccessBean.setWired(false);
                            networkAccessBean.setShowDetail(isShowDetail);
                            networkAccessBean.setNetworkName(mSsid);
                            networkAccessBean.setConnect(true);
                            networkAccessBeans.add(networkAccessBean);
                        }
                        mDistributeWLAN.setConnect(true);
                        mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeans);
                        updateLoading(UPDATE_STATE_WITHOUT_LIST);
                    }
                    refreshAccessNetwork();
                }
                if (networkConfigurationBridge != null) {
                    networkConfigurationBridge.handleNetworkConfigurationSetWifi(code, source);
                }
            });
        }
    }

    @Override
    public void ignoreNetworkCallback(int code, String source) {
        cancelOnlineIgnoreNetworkWait();
        handleIgnoreNetworkCallback(code, source);
    }

    private void handleIgnoreNetworkCallback(int code, String source) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (code >= 200 && code < 300) {
                    if (mSsid != null) {
                        boolean isConnect = false;
                        List<NetworkAccessBean> networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
                        if (networkAccessBeans != null) {
                            Iterator<NetworkAccessBean> iterator = networkAccessBeans.iterator();
                            while (iterator.hasNext()) {
                                NetworkAccessBean bean = iterator.next();
                                if (bean != null) {
                                    if (!bean.isWired() && mSsid.equals(bean.getNetworkName())) {
                                        iterator.remove();
                                    } else if (!isConnect) {
                                        isConnect = bean.isConnect();
                                    }
                                }
                            }
                        } else {
                            networkAccessBeans = new ArrayList<>();
                        }
                        mDistributeWLAN.setConnect(isConnect);
                        mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeans);
                        updateLoading(UPDATE_STATE_WITHOUT_LIST);
                    }
                    refreshAccessNetwork();
                }
                if (networkConfigurationBridge != null) {
                    networkConfigurationBridge.handleNetworkConfigurationIgnoreWifi(code, source);
                }
            });
        }
    }

    @Override
    public void distributeNetworkCallback(String ssid, List<String> ipAddresses, Boolean isSuccess) {
        cancelOnlineDistributeNetworkWait();
        if (mDistributeWLAN != null) {
            if (isSuccess != null && isSuccess) {
                boolean isFind = false;
                if (ssid != null) {
                    List<WLANItem> wlanItems = mDistributeWLAN.getWlanItemList();
                    if (wlanItems != null) {
                        for (WLANItem wlanItem : wlanItems) {
                            if (wlanItem != null) {
                                String address = wlanItem.getWlanAddress();
                                if (address != null && address.equals(ssid)) {
                                    ssid = wlanItem.getWlanSsid();
                                    isFind = true;
                                    break;
                                }
                            }
                        }
                    }
                    mDistributeWLAN.setConnectedWlanSsid(ssid);
                }

                mDistributeWLAN.setConnect(true);
                if (mHandler != null) {
                    String finalSsid = ssid;
                    boolean finalIsFind = isFind;
                    mHandler.post(() -> {
                        if (finalIsFind && finalSsid != null) {
                            List<NetworkAccessBean> networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
                            boolean isContains = false;
                            if (networkAccessBeans != null) {
                                for (NetworkAccessBean networkAccessBean : networkAccessBeans) {
                                    if (networkAccessBean != null && !networkAccessBean.isWired() && finalSsid.equals(networkAccessBean.getNetworkName())) {
                                        networkAccessBean.setConnect(true);
                                        isContains = true;
                                    }
                                }
                            }
                            if (!isContains) {
                                if (networkAccessBeans != null) {
                                    Iterator<NetworkAccessBean> iterator = networkAccessBeans.iterator();
                                    while (iterator.hasNext()) {
                                        NetworkAccessBean bean = iterator.next();
                                        if (bean != null && !bean.isWired()) {
                                            iterator.remove();
                                        }
                                    }
                                } else {
                                    networkAccessBeans = new ArrayList<>();
                                }
                                NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                                networkAccessBean.setWired(false);
                                networkAccessBean.setShowDetail(isShowDetail);
                                networkAccessBean.setNetworkName(finalSsid);
                                networkAccessBean.setConnect(true);
                                networkAccessBeans.add(networkAccessBean);
                            }
                            mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeans);
                        }
                        closeLoading();
                        dismissDistributeNetworkDialog();
                        updateLoading(UPDATE_STATE_WITHOUT_LIST);
                        refreshAccessNetwork();
                        if (networkConfigurationBridge != null) {
                            networkConfigurationBridge.handleNetworkConfigurationSetWifi(200, null);
                        } else {
                            showImageTextToast(R.drawable.toast_right, R.string.connect_success);
                        }
                        if (presenter != null && isOnlineDistributeNetwork) {
                            CommonDeviceInfo commonDeviceInfo = presenter.getCommonDeviceInfo();
                            if (commonDeviceInfo != null) {
                                DeviceNetworkEvent event = new DeviceNetworkEvent(commonDeviceInfo.getBoxUuid()
                                        , commonDeviceInfo.getBoxBind(), commonDeviceInfo.getBoxDomain());
                                EventBusUtil.post(event);
                            }
                        }

                    });
                }
            } else if (mHandler != null) {
                mHandler.post(() -> {
                    closeLoading();
                    if (networkConfigurationBridge != null) {
                        networkConfigurationBridge.handleNetworkConfigurationSetWifi((isSuccess == null ? 500 : 561), null);
                    } else {
                        showImageTextToast(R.drawable.toast_wrong, (isSuccess == null ? R.string.connect_fail : R.string.connect_fail_wrong_password));
                        updateLoading(UPDATE_STATE_WITHOUT_LIST);
                        dialogInput.setEnabled(true);
                        setDialogButtonPattern(dialogCancel, true);
                        setDialogButtonPattern(dialogConfirm, true);
                    }
                });
            }
        }
    }

    @Override
    public void countdownTime(int timeSecond, int totalTimeSecond) {
        setInitialProgressPattern((totalTimeSecond - timeSecond), totalTimeSecond);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                case R.id.back_no_title:
                    distributeOnlyPrepareFinish();
                    if (isDistributeNetworkOnly || isNewBindProgress) {
                        finish();
                    }
                    break;
                case R.id.refresh_network_container:
                    setRefreshNetworkPattern(true);
                    if (!refreshWlanList()) {
                        setRefreshNetworkPattern(false);
                    }
                    break;
                case R.id.loading_button_container:
                    handleLoadingButtonContainerClickEvent();
                    break;
                case R.id.dialog_private:
                    setDialogPrivate(!mPrivate);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onNetworkAccessItemClick(View view, int position) {
        List<NetworkAccessBean> networkAccessBeans = null;
        if (mDistributeWLAN != null) {
            networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
        }
        NetworkConfigDNSInfo networkConfigDNSInfo = null;
        NetworkAdapter networkAdapter = null;
        List<NetworkAdapter> networkAdapters = null;
        if (mNetworkStatusResult != null) {
            networkConfigDNSInfo = new NetworkConfigDNSInfo();
            networkConfigDNSInfo.setIpv4Dns1(mNetworkStatusResult.getdNS1());
            networkConfigDNSInfo.setIpv4Dns2(mNetworkStatusResult.getdNS2());
            networkConfigDNSInfo.setIpv6Dns1(mNetworkStatusResult.getIpv6DNS1());
            networkConfigDNSInfo.setIpv6Dns2(mNetworkStatusResult.getIpv6DNS2());
            networkAdapters = mNetworkStatusResult.getNetworkAdapters();
        }
        if (position >= 0 && networkAccessBeans != null && networkAccessBeans.size() > position) {
            NetworkAccessBean networkAccessBean = networkAccessBeans.get(position);
            if (networkAccessBean != null) {
                String networkName = networkAccessBean.getNetworkName();
                boolean isWired = networkAccessBean.isWired();
                if (networkName != null && networkAdapters != null) {
                    for (NetworkAdapter adapter : networkAdapters) {
                        if (adapter != null && adapter.isWired() == isWired && ((isWired && networkName.equals(adapter.getAdapterName()))
                                || (!isWired && networkName.equals(adapter.getwIFIName())))) {
                            networkAdapter = adapter;
                            break;
                        }
                    }
                }
            }
        }
        if (networkConfigDNSInfo != null && networkAdapter != null) {
            startNetworkConfigurationActivity(networkConfigDNSInfo, networkAdapter);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (!mLoading) {
            mSsid = getWlanListSsid(position);
            mAddress = getWlanListAddress(position);
            if (mSsid != null) {
                String title = getString(R.string.input_password_title_part_1) + mSsid + getString(R.string.input_password_title_part_2);
                dialogTitle.setText(title);
                showDistributeNetworkDialog();
            }
        }
    }

    @Override
    public void onDetailClick(View view, int position) {
        if (!mLoading) {
            mSsid = getWlanListSsid(position);
            mAddress = getWlanListAddress(position);
            boolean isConnect = false;
            NetworkConfigDNSInfo networkConfigDNSInfo = null;
            NetworkAdapter networkAdapter = null;
            if (mNetworkStatusResult != null) {
                boolean isAccess = false;
                List<NetworkAdapter> networkAdapters = mNetworkStatusResult.getNetworkAdapters();
                if (networkAdapters != null) {
                    for (NetworkAdapter adapter : networkAdapters) {
                        if (adapter != null && !adapter.isWired() && mAddress != null && mAddress.equals(adapter.getwIFIAddress())) {
                            isAccess = true;
                            isConnect = adapter.isConnected();
                            networkAdapter = adapter;
                            break;
                        }
                    }
                }
                if (isAccess) {
                    networkConfigDNSInfo = new NetworkConfigDNSInfo();
                    networkConfigDNSInfo.setIpv4Dns1(mNetworkStatusResult.getdNS1());
                    networkConfigDNSInfo.setIpv4Dns2(mNetworkStatusResult.getdNS2());
                    networkConfigDNSInfo.setIpv6Dns1(mNetworkStatusResult.getIpv6DNS1());
                    networkConfigDNSInfo.setIpv6Dns2(mNetworkStatusResult.getIpv6DNS2());
                }
            }
            if (networkAdapter == null) {
                networkAdapter = new NetworkAdapter();
                networkAdapter.setWired(false);
                networkAdapter.setConnected(isConnect);
                networkAdapter.setwIFIAddress(mAddress);
                networkAdapter.setwIFIName(mSsid);
            }
            startNetworkConfigurationActivity(networkConfigDNSInfo, networkAdapter);
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
                        List<WLANItem> wlanItems = null;
                        if (wifiInfoList != null) {
                            wlanItems = new ArrayList<>();
                            for (WifiInfo wifiInfo : wifiInfoList) {
                                if (wifiInfo != null) {
                                    WLANItem wlanItem = new WLANItem();
                                    wlanItem.setWlanSsid(wifiInfo.getName());
                                    wlanItem.setWlanAddress(wifiInfo.getSsid());
                                    wlanItems.add(wlanItem);
                                }
                            }
                        }
                        handleWlanListResult(wlanItems);
                        break;
                    case AODeviceDiscoveryManager.STEP_SET_WIFI:
                        NetworkConfigResult networkConfigResult = null;
                        if (bodyJson != null) {
                            try {
                                networkConfigResult = new Gson().fromJson(bodyJson, NetworkConfigResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        boolean isSuccess = false;
                        List<String> ipAddresses = null;
                        String address = null;
                        if (networkConfigResult != null) {
                            isSuccess = (networkConfigResult.getStatus() == 0);
                            ipAddresses = networkConfigResult.getIpAddresses();
                            address = networkConfigResult.getAddress();
                        }
                        handleDistributionResult(address, ipAddresses, isSuccess);
                        break;
                    case AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG:
                        NetworkStatusResult networkStatusResult = null;
                        if (bodyJson != null) {
                            try {
                                networkStatusResult = new Gson().fromJson(bodyJson, NetworkStatusResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        networkAccessCallback((networkStatusResult != null), networkStatusResult);
                        break;
                    case AODeviceDiscoveryManager.STEP_SET_NETWORK_CONFIG:
                        handleSetNetworkConfigCallback(code, source);
                        break;
                    case AODeviceDiscoveryManager.STEP_IGNORE_NETWORK:
                        handleIgnoreNetworkCallback(code, source);
                        break;
                    case AODeviceDiscoveryManager.STEP_PAIRING:
                        PairingBoxResults pairingBoxResults = null;
                        if (bodyJson != null) {
                            try {
                                pairingBoxResults = new Gson().fromJson(bodyJson, PairingBoxResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        PairingBoxInfo pairingBoxInfo = null;
                        int contentCode = -1;
                        if (pairingBoxResults != null) {
                            pairingBoxInfo = pairingBoxResults.getResults();
                            String contentCodeValue = pairingBoxResults.getCode();
                            if (contentCodeValue != null) {
                                contentCode = DataUtil.stringCodeToInt(contentCodeValue);
                            }
                        }
                        if (mManager != null) {
                            mManager.setPairingBoxInfo(pairingBoxInfo);
                        }
                        if (code >= 200 && code < 400 && contentCode != ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR && pairingBoxInfo != null) {
                            if (mInitResponse != null && mManager != null) {
                                AOSpaceUtil.handleSaveBindingBox(getApplicationContext(), pairingBoxInfo, mManager.getBoxPublicKey()
                                        , mManager.getDeviceName(), mManager.getBluetoothAddress(), mManager.getBluetoothId()
                                        , mInitResponse.getDeviceAbility());
                                int paired = mInitResponse.getPaired();
                                if (paired == 1) {
                                    Intent intent = new Intent(DistributeNetworkActivity.this, SecurityPasswordSettingActivity.class);
                                    startActivity(intent);
                                } else if (mManager != null) {
                                    progressState = DistributeNetworkBridge.PROGRESS_INITIALIZE;
                                    handleRequestEvent();
                                }
                            }
                        } else {
                            mLoading = false;
                            if (presenter != null) {
                                presenter.stopCountdown();
                            }
                            dismissInitialProgressDialog();
                            updateLoading(UPDATE_STATE_WITHOUT_LIST);
                            boolean isHandle = true;
                            if (contentCode == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR) {
                                showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                            } else {
                                switch (code) {
                                    case ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED:
                                        startBindResultActivity(true, code);
                                        break;
                                    case ConstantField.SERVER_EXCEPTION_CODE:
                                        showServerExceptionToast();
                                        break;
                                    default:
                                        isHandle = false;
                                        break;
                                }
                            }
                            if (!isHandle) {
                                showImageTextToast(R.drawable.toast_wrong, R.string.connect_fail);
                            }
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_INITIAL:
                        InitialResults initialResults = null;
                        if (bodyJson != null) {
                            try {
                                initialResults = new Gson().fromJson(bodyJson, InitialResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        int resultCode = (initialResults == null ? 500 : DataUtil.stringCodeToInt(initialResults.getCode()));
                        if (resultCode == 200 && mManager != null) {
                            boolean isInnerDiskSupport = mManager.isInnerDiskSupport();
                            AOSpaceUtil.requestAdministratorBindUseBox(getApplicationContext(), mManager.getBoxUuid(), isInnerDiskSupport);
                            if (isInnerDiskSupport) {
                                progressState = DistributeNetworkBridge.PROGRESS_SPACE_READY_CHECK;
                                handleRequestEvent();
                            } else {
                                AOCompleteActivity.startThisActivity(DistributeNetworkActivity.this, mManager.getBoxUuid(), "1");
                                mManager.finishSource();
                            }
                        } else {
                            handleConnectErrorEvent(code);
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS:
                        ProgressResult progressResult = null;
                        if (bodyJson != null) {
                            progressResult = new Gson().fromJson(bodyJson, ProgressResult.class);
                        }
                        if (code == ConstantField.KnownError.BindError.BOUND_CODE && ConstantField.KnownSource.AGENT.equals(source)) {
                            updateLoading(UPDATE_STATE_WITHOUT_LIST);
                            if (mManager != null) {
                                showImageTextToast(R.drawable.toast_refuse, R.string.binding_initializing_space_change_phone_hint);
                                mManager.finishSource();
                                finish();
                            }
                        } else if (progressResult != null) {
                            updateLoading(UPDATE_STATE_WITHOUT_LIST);
                            int comStatus = progressResult.getComStatus();
                            if (comStatus == ProgressResult.COM_STATUS_CONTAINERS_STARTED) {
                                AOSpaceInformationActivity.administratorStartThisActivity(this, null);
                            } else {
                                AODeviceInitialActivity.startThisActivity(DistributeNetworkActivity.this
                                        , (ProgressResult.COM_STATUS_CONTAINERS_STARTING == comStatus));
                            }
                        } else {
                            handleConnectErrorEvent(code);
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE:
                        SpaceCreateResult spaceCreateResult = null;
                        if (bodyJson != null) {
                            try {
                                spaceCreateResult = new Gson().fromJson(bodyJson, SpaceCreateResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        PairingBoxResults pairingBoxResultsNew = null;
                        int contentCodeNew = -1;
                        if (spaceCreateResult != null) {
                            pairingBoxResultsNew = spaceCreateResult.getSpaceUserInfo();
                            if (pairingBoxResultsNew != null) {
                                String contentCodeValueNew = pairingBoxResultsNew.getCode();
                                if (contentCodeValueNew != null) {
                                    contentCodeNew = DataUtil.stringCodeToInt(contentCodeValueNew);
                                }
                            }
                        }
                        if (code == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED && ConstantField.KnownSource.AGENT.equals(source)) {
                            Intent intent = new Intent(DistributeNetworkActivity.this, BindResultActivity.class);
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
                        } else if (contentCodeNew == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR) {
                            showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                        } else if (code >= 200 && code < 400 && spaceCreateResult != null) {
                            PairingBoxInfo pairingBoxInfoNew = null;
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
                            if (pairingBoxResultsNew != null) {
                                pairingBoxInfoNew = pairingBoxResultsNew.getResults();
                            }
                            mPairingBoxInfo = pairingBoxInfoNew;
                            if (mManager != null) {
                                mManager.setPairingBoxInfo(pairingBoxInfoNew);
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
                                    if (enableInternetAccess != null) {
                                        aoSpaceAccessBean = new AOSpaceAccessBean();
                                        aoSpaceAccessBean.setLanAccess(true);
                                        aoSpaceAccessBean.setP2PAccess(enableInternetAccess);
                                        aoSpaceAccessBean.setInternetAccess(enableInternetAccess);
                                    }
                                    boolean bindResult = AOSpaceUtil.requestUseBox(getApplicationContext(), boxUuid, "1"
                                            , pairingBoxInfoNew, aoSpaceAccessBean, StringUtil.nullToEmpty(ipAddressUrl)
                                            , mManager.getBoxPublicKey(), mManager.getDeviceName()
                                            , mManager.getBluetoothAddress(), mManager.getBluetoothId()
                                            , mManager.getDeviceAbility(), isInnerDiskSupport);
                                    if (bindResult) {
                                        if (isInnerDiskSupport) {
                                            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
                                        } else {
                                            String avatarUrl = null;
                                            if (pairingBoxInfoNew != null) {
                                                avatarUrl = pairingBoxInfoNew.getAvatarUrl();
                                            }
                                            AOCompleteActivity.startThisActivity(DistributeNetworkActivity.this, boxUuid, "1", avatarUrl);
                                            mManager.finishSource();
                                        }
                                    } else {
                                        Intent intent = new Intent(this, BindResultActivity.class);
                                        intent.putExtra(ConstantField.BIND_TYPE, true);
                                        intent.putExtra(ConstantField.BIND_RESULT, 500);
                                        startActivity(intent);
                                    }
                                }
                            }
                        } else {
                            handleConnectErrorEvent(code);
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
                        if (isFastDiskInitialize) {
                            if (code >= 200 && code < 400 && readyCheckResult != null) {
                                Boolean isDiskInitializeNoMainStorage = readyCheckResult.getMissingMainStorage();
                                String boxUuid = null;
                                String networkName = null;
                                Boolean isWire = null;
                                if (mManager != null) {
                                    InitResponse initResponse = mManager.getInitResponse();
                                    if (initResponse != null) {
                                        boxUuid = initResponse.getBoxUuid();
                                    }
                                }
                                if (mDistributeWLAN != null) {
                                    List<NetworkAccessBean> networkAccessBeanList = mDistributeWLAN.getNetworkAccessBeanList();
                                    if (networkAccessBeanList != null) {
                                        for (NetworkAccessBean networkAccessBean : networkAccessBeanList) {
                                            if (networkAccessBean != null) {
                                                String networkWifiName = networkAccessBean.getNetworkName();
                                                if (networkName == null) {
                                                    networkName = networkWifiName;
                                                    if (networkName != null) {
                                                        isWire = networkAccessBean.isWired();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Intent intent = new Intent(this, BindResultActivity.class);
                                intent.putExtra(ConstantField.BIND_RESULT_RETRY, true);
                                if (networkName != null) {
                                    intent.putExtra(ConstantField.WLAN_SSID, networkName);
                                }
                                if (isWire != null) {
                                    intent.putExtra(ConstantField.IS_WIRE, (isWire ? 1 : -1));
                                }
                                if (diskInitialCode != null) {
                                    intent.putExtra(ConstantField.DISK_INITIALIZE, diskInitialCode);
                                }
                                if (boxUuid != null) {
                                    intent.putExtra(ConstantField.BOX_UUID, boxUuid);
                                }
                                intent.putExtra(ConstantField.BIND_TYPE, true);
                                intent.putExtra(ConstantField.BIND_RESULT, code);
                                if (isDiskInitializeNoMainStorage != null) {
                                    intent.putExtra(ConstantField.DISK_INITIALIZE_NO_MAIN_STORAGE, isDiskInitializeNoMainStorage.booleanValue());
                                }
                                startActivity(intent);
                            } else {
                                handleConnectErrorEvent(code);
                            }
                        } else {
                            if (diskInitialCode != null) {
                                if (diskInitialCode == ReadyCheckResult.DISK_NORMAL) {
                                    progressState = DistributeNetworkBridge.PROGRESS_DISK_MANAGEMENT_LIST;
                                } else {
                                    progressState = DistributeNetworkBridge.PROGRESS_DISK_RECOGNITION;
                                }
                                handleRequestEvent();
                            } else {
                                handleConnectErrorEvent(code);
                            }
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
                            Intent intent = new Intent(DistributeNetworkActivity.this, DiskInitializationActivity.class);
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
                            handleConnectErrorEvent(code);
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
                            AOCompleteActivity.startThisActivity(DistributeNetworkActivity.this, boxUuid, "1", avatarUrl);
                            mManager.finishSource();
                        } else {
                            handleConnectErrorEvent(code);
                        }
                        break;
                    default:
                        break;
                }
            });
        }
    }

    @Override
    public void handleWlanListResult(List<WLANItem> wlanItemList) {
        if (mDistributeWLAN == null) {
            mDistributeWLAN = new DistributeWLAN();
        }
        mDistributeWLAN.setWlanItemList(wlanItemList);
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isRefreshWifiList != null) {
                    isRefreshWifiList = true;
                }
                checkRefreshComplete();
                updateLoading(UPDATE_STATE_ONLY_LIST);
                setRefreshNetworkPattern(false);
                isRefreshWifiList = true;
                checkRefreshComplete();
                if (wlanItemList == null || wlanItemList.size() <= 0) {
                    showEmptyWlanDialog();
                }
            });
        }
        if (isRefreshAccessNetwork != null && !isRefreshAccessNetwork) {
            refreshAccessNetwork();
        }
    }

    @Override
    public void handleAccessNetworkResult(int code, String source, NetworkStatusResult networkStatusResult) {
        networkAccessCallback((networkStatusResult != null), networkStatusResult);
    }

    @Override
    public void handleSetNetworkConfigResult(int code, String source) {
        handleSetNetworkConfigCallback(code, source);
    }

    @Override
    public void handleIgnoreNetworkConfigResult(int code, String source) {
        handleIgnoreNetworkCallback(code, source);
    }

    @Override
    public void handleDistributionResult(String ssid, List<String> ipAddresses, boolean isSuccess) {
        if (mDistributeWLAN != null) {
            boolean isFind = false;
            List<WLANItem> wlanItems = mDistributeWLAN.getWlanItemList();
            if (wlanItems != null) {
                for (WLANItem wlanItem : wlanItems) {
                    if (wlanItem != null) {
                        String address = wlanItem.getWlanAddress();
                        if (address != null && address.equals(ssid)) {
                            ssid = wlanItem.getWlanSsid();
                            isFind = true;
                            break;
                        }
                    }
                }
            }
            if (isSuccess) {
                if (mHandler != null) {
                    String finalSsid = ssid;
                    boolean finalIsFind = isFind;
                    mHandler.post(() -> {
                        mDistributeWLAN.setConnectedWlanSsid(finalSsid);
                        mDistributeWLAN.setIpAddresses(ipAddresses);
                        mDistributeWLAN.setConnect(isSuccess);

                        if (finalIsFind && finalSsid != null) {
                            List<NetworkAccessBean> networkAccessBeans = mDistributeWLAN.getNetworkAccessBeanList();
                            boolean isContains = false;
                            if (networkAccessBeans != null) {
                                for (NetworkAccessBean networkAccessBean : networkAccessBeans) {
                                    if (networkAccessBean != null && !networkAccessBean.isWired() && finalSsid.equals(networkAccessBean.getNetworkName())) {
                                        networkAccessBean.setConnect(true);
                                        isContains = true;
                                    }
                                }
                            }
                            if (!isContains) {
                                if (networkAccessBeans != null) {
                                    Iterator<NetworkAccessBean> iterator = networkAccessBeans.iterator();
                                    while (iterator.hasNext()) {
                                        NetworkAccessBean bean = iterator.next();
                                        if (bean != null && !bean.isWired()) {
                                            iterator.remove();
                                        }
                                    }
                                } else {
                                    networkAccessBeans = new ArrayList<>();
                                }
                                NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                                networkAccessBean.setWired(false);
                                networkAccessBean.setShowDetail(isShowDetail);
                                networkAccessBean.setNetworkName(finalSsid);
                                networkAccessBean.setConnect(true);
                                networkAccessBeans.add(networkAccessBean);
                            }
                            mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeans);
                        }

                        closeLoading();
                        dismissDistributeNetworkDialog();
                        updateLoading(UPDATE_STATE_WITHOUT_LIST);
                        if (isDistributeNetworkOnly) {
                            EventBusUtil.post(new BoxOnlineRequestEvent(true, true));
                        }
                        refreshAccessNetwork();
                        if (networkConfigurationBridge != null) {
                            networkConfigurationBridge.handleNetworkConfigurationSetWifi(200, null);
                        } else {
                            showImageTextToast(R.drawable.toast_right, R.string.connect_success);
                        }
                    });
                }
            } else if (mHandler != null) {
                mHandler.post(() -> {
                    closeLoading();
                    showImageTextToast(R.drawable.toast_wrong, R.string.connect_fail_wrong_password);
                    updateLoading(UPDATE_STATE_WITHOUT_LIST);
                    dialogInput.setEnabled(true);
                    setDialogButtonPattern(dialogCancel, true);
                    setDialogButtonPattern(dialogConfirm, true);
                    setRefreshNetworkPattern(true);
                    if (!refreshWlanList()) {
                        setRefreshNetworkPattern(false);
                    }
                });
            }
        }
    }

    @Override
    public void pairingResultCallback(boolean isSuccess, Integer code) {
        if (mHandler != null) {
            mHandler.post(() -> {
                mLoading = false;
                if (presenter != null) {
                    presenter.stopCountdown();
                }
                dismissInitialProgressDialog();
                updateLoading(UPDATE_STATE_WITHOUT_LIST);
                if (isSuccess && (code == null || (code >= 0 && code < 400))) {
                    handleResult(true, false);
                } else {
                    boolean isHandle = false;
                    if (code != null) {
                        isHandle = true;
                        switch (code) {
                            case ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED:
                                startBindResultActivity(true, code);
                                break;
                            default:
                                isHandle = false;
                                break;
                        }
                    }
                    if (!isHandle) {
                        showImageTextToast(R.drawable.toast_wrong, R.string.connect_fail);
                    }
                }
            });
        }
    }

    @Override
    public void handleSpaceReadyCheck(int code, String source, ReadyCheckResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                mLoading = false;
                if (presenter != null) {
                    presenter.stopCountdown();
                }
                dismissInitialProgressDialog();
                updateLoading(UPDATE_STATE_WITHOUT_LIST);
                if (code >= 200 && code < 400 && result != null) {
                    handleResult(true, false);
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.connect_fail);
                }
            });
        }
    }

    @Override
    public void handleProgressState(int state) {
        progressState = state;
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(() -> {
                mLoading = false;
                if (networkConfigurationBridge != null) {
                    networkConfigurationBridge.handleDisconnect();
                }
                if (presenter != null) {
                    presenter.stopCountdown();
                }
                dismissInitialProgressDialog();
                dismissDistributeNetworkDialog();
                dismissEmptyWlanDialog();
                finish();
            });
        }
    }

    @Override
    public void handleDestroy() {
        if (networkConfigurationBridge != null) {
            networkConfigurationBridge.unregisterSourceCallback();
            networkConfigurationBridge = null;
        }
    }

    @Override
    public void networkConfigurationSetWifi(String ssid, String address, String password, NetworkConfigDNSInfo tempNetworkConfigDNSInfo, NetworkAdapter tempNetworkAdapter) {
        if (tempNetworkConfigDNSInfo == null && tempNetworkAdapter == null) {
            if (isOnlineDistributeNetwork) {
                cancelOnlineDistributeNetworkWait();
                if (presenter != null) {
                    onlineDistributeWait = true;
                    BoxNetworkCheckManager.setShowOffline(false);
                    presenter.setWifi(ssid, address, password);
                }
                if (mHandler != null) {
                    onlineDistributeExpire = false;
                    mHandler.sendEmptyMessageDelayed(ONLINE_CONNECT_WAIT, MINUTE_PERIOD);
                }
            } else if (isDistributeNetworkOnly) {
                if (mBridge != null) {
                    mBridge.selectWlan(ssid, address, password);
                }
            } else if (mManager != null) {
                WpwdInfo wpwdInfo = new WpwdInfo();
                wpwdInfo.setAddr(address);
                wpwdInfo.setPwd(password);
                if (!mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SET_WIFI
                        , new Gson().toJson(wpwdInfo, WpwdInfo.class))) {
                    if (networkConfigurationBridge != null) {
                        networkConfigurationBridge.handleNetworkConfigurationSetWifi(ConstantField.SERVER_EXCEPTION_CODE, null);
                    }
                }
            }
        } else {
            boolean isChangeWifi = (password != null);
            String ipv4Dns1 = null;
            String ipv4Dns2 = null;
            String ipv6Dns1 = null;
            String ipv6Dns2 = null;
            if (tempNetworkConfigDNSInfo != null) {
                ipv4Dns1 = tempNetworkConfigDNSInfo.getIpv4Dns1();
                ipv4Dns2 = tempNetworkConfigDNSInfo.getIpv4Dns2();
                ipv6Dns1 = tempNetworkConfigDNSInfo.getIpv6Dns1();
                ipv6Dns2 = tempNetworkConfigDNSInfo.getIpv6Dns2();
            }
            List<NetworkAdapter> networkAdapters = null;
            if (tempNetworkAdapter != null) {
                networkAdapters = new ArrayList<>();
                if (tempNetworkAdapter.isIpv6UseDhcp() || tempNetworkAdapter.isIpv4UseDhcp()) {
                    NetworkAdapter adapter = tempNetworkAdapter.cloneSelf();
                    if (adapter.isIpv6UseDhcp()) {
                        adapter.setIpv6(null);
                        adapter.setIpv6DefaultGateway(null);
                        ipv6Dns1 = null;
                        ipv6Dns2 = null;
                    }
                    if (adapter.isIpv4UseDhcp()) {
                        adapter.setSubNetMask(null);
                        adapter.setSubNetPreLen(null);
                        adapter.setIpv4(null);
                        adapter.setDefaultGateway(null);
                        ipv4Dns1 = null;
                        ipv4Dns2 = null;
                    }
                    networkAdapters.add(adapter);
                } else {
                    networkAdapters.add(tempNetworkAdapter);
                }
            }
            if (isOnlineDistributeNetwork) {
                if (isChangeWifi) {
                    cancelOnlineDistributeNetworkWait();
                }
                if (presenter != null) {
                    if (isChangeWifi) {
                        onlineDistributeWait = true;
                        BoxNetworkCheckManager.setShowOffline(false);
                    }
                    presenter.setNetworkConfig(ipv4Dns1, ipv4Dns2, ipv6Dns1, ipv6Dns2, networkAdapters, isChangeWifi);
                }
                if (isChangeWifi && mHandler != null) {
                    onlineDistributeExpire = false;
                    mHandler.sendEmptyMessageDelayed(ONLINE_CONNECT_WAIT, MINUTE_PERIOD);
                }
            } else if (isDistributeNetworkOnly) {
                if (mBridge != null) {
                    mBridge.setNetworkConfig(ipv4Dns1, ipv4Dns2, ipv6Dns1, ipv6Dns2, networkAdapters);
                }
            } else if (mManager != null) {
                NetworkConfigRequest networkConfigRequest = new NetworkConfigRequest();
                networkConfigRequest.setdNS1(ipv4Dns1);
                networkConfigRequest.setdNS2(ipv4Dns2);
                networkConfigRequest.setIpv6DNS1(ipv6Dns1);
                networkConfigRequest.setIpv6DNS2(ipv6Dns2);
                networkConfigRequest.setNetworkAdapters(networkAdapters);
                if (!mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SET_NETWORK_CONFIG
                        , new Gson().toJson(networkConfigRequest, NetworkConfigRequest.class))) {
                    if (networkConfigurationBridge != null) {
                        networkConfigurationBridge.handleNetworkConfigurationSetWifi(ConstantField.SERVER_EXCEPTION_CODE, null);
                    }
                }
            }
        }
    }

    @Override
    public void networkConfigurationIgnoreWifi(String ssid, String address) {
        DataUtil.setNetworkPassword(getApplicationContext(), mAddress, null);
        if (isOnlineDistributeNetwork) {
            cancelOnlineIgnoreNetworkWait();
            if (presenter != null) {
                onlineIgnoreWait = true;
                presenter.ignoreNetwork(ssid);
            }
            if (mHandler != null) {
                onlineIgnoreExpire = false;
                mHandler.sendEmptyMessageDelayed(ONLINE_IGNORE_WAIT, MINUTE_PERIOD);
            }
        } else if (isDistributeNetworkOnly) {
            if (mBridge != null) {
                mBridge.ignoreNetworkConfig(ssid);
            }
        } else if (mManager != null) {
            NetworkIgnoreRequest networkIgnoreRequest = new NetworkIgnoreRequest();
            networkIgnoreRequest.setwIFIName(ssid);
            if (!mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_IGNORE_NETWORK
                    , new Gson().toJson(networkIgnoreRequest, NetworkIgnoreRequest.class))) {
                handleIgnoreNetworkConfigResult(ConstantField.SERVER_EXCEPTION_CODE, null);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceNetworkResponseEvent event) {
        if (event != null && isOnlineDistributeNetwork && mDistributeWLAN != null) {
            String boxUuid = event.getBoxUuid();
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(getApplicationContext());
            String lastBoxUuid = null;
            if (eulixSpaceInfo != null) {
                lastBoxUuid = eulixSpaceInfo.getBoxUuid();
            }
            if (boxUuid != null && boxUuid.equals(lastBoxUuid)) {
                List<InitResponseNetwork> networkData = event.getNetworkData();
                List<String> ipAddressesList = null;
                List<NetworkAccessBean> networkAccessBeans = null;
                String wifiName = null;
                if (networkData != null) {
                    Collections.sort(networkData, FormatUtil.wireFirstComparator);
                    ipAddressesList = new ArrayList<>();
                    networkAccessBeans = new ArrayList<>();
                    boolean isWriteSsid = true;
                    for (InitResponseNetwork network : networkData) {
                        if (network != null) {
                            String networkWifiName = network.getWifiName();
                            String ipAddress = network.getIp();
                            NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                            networkAccessBean.setConnect((ipAddress != null));
                            networkAccessBean.setWired(network.isWire());
                            networkAccessBean.setNetworkName(networkWifiName);
                            networkAccessBean.setShowDetail(isShowDetail);
                            networkAccessBeans.add(networkAccessBean);
                            if (!network.isWire() && isWriteSsid) {
                                wifiName = networkWifiName;
                                isWriteSsid = false;
                            }
                            if (ipAddress != null) {
                                ipAddressesList.add(ipAddress);
                            }
                        }
                    }
                }
                boolean isUpdate = true;
                if (onlineDistributeWait) {
                    boolean isHandle = false;
                    if ((wifiName != null && wifiName.equals(mSsid))) {
                        if (mDistributeWLAN == null || !wifiName.equals(mDistributeWLAN.getConnectedWlanSsid())) {
                            if (presenter != null) {
                                presenter.resetSetWifiUuid();
                            }
                            cancelOnlineDistributeNetworkWait();
                            closeLoading();
                            dismissDistributeNetworkDialog();
                            if (networkConfigurationBridge != null) {
                                networkConfigurationBridge.handleNetworkConfigurationSetWifi(200, null);
                            } else {
                                showImageTextToast(R.drawable.toast_right, R.string.connect_success);
                            }
                            refreshAccessNetwork();
                            isHandle = true;
                        }
                    }
                    if (!isHandle && isOnlineDistributeNetwork && onlineDistributeExpire) {
                        if (presenter != null) {
                            presenter.resetSetWifiUuid();
                        }
                        onlineDistributeWait = false;
                        BoxNetworkCheckManager.setShowOffline(true);
                        closeLoading();
                        if (networkConfigurationBridge != null) {
                            networkConfigurationBridge.handleNetworkConfigurationSetWifi(600, null);
                        } else {
                            showPureTextToast(R.string.connection_time_out);
                            dialogInput.setEnabled(true);
                            setDialogButtonPattern(dialogCancel, true);
                            setDialogButtonPattern(dialogConfirm, true);
                        }
                        onlineDistributeExpire = false;
                        setRefreshNetworkPattern(true);
                        if (!refreshWlanList()) {
                            setRefreshNetworkPattern(false);
                        }
                    }
                }
                if (onlineIgnoreWait) {
                    boolean isHandle = false;
                    if (wifiName == null || !wifiName.equals(mSsid)) {
                        cancelOnlineIgnoreNetworkWait();
                        if (networkConfigurationBridge != null) {
                            networkConfigurationBridge.handleNetworkConfigurationIgnoreWifi(200, null);
                        }
                        refreshAccessNetwork();
                        isHandle = true;
                    }
                    if (!isHandle && isOnlineDistributeNetwork && onlineIgnoreExpire) {
                        onlineIgnoreWait = false;
                        if (networkConfigurationBridge != null) {
                            networkConfigurationBridge.handleNetworkConfigurationIgnoreWifi(600, null);
                        }
                        onlineIgnoreExpire = false;
                        setRefreshNetworkPattern(true);
                        if (!refreshWlanList()) {
                            setRefreshNetworkPattern(false);
                        }
                    }
                }
                if (isUpdate) {
                    mDistributeWLAN.setConnectedWlanSsid(wifiName);
                    mDistributeWLAN.setIpAddresses(ipAddressesList);
                    mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeans);
                    mDistributeWLAN.setConnect((ipAddressesList != null && ipAddressesList.size() > 0));
                    updateLoading(UPDATE_STATE_WITHOUT_LIST);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.BIND_RESULT_CODE:
                if (mBridge != null) {
                    mBridge.handleBindResult(false);
                }
                if (resultCode == Activity.RESULT_OK) {
                    exitBindResult();
                } else {
                    handleResult(false, true);
                }
                break;
            default:
                break;
        }
    }
}
