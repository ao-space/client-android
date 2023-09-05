package xyz.eulix.space.ui.bind;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.bind.AODeviceFindAdapter;
import xyz.eulix.space.bean.AODeviceFindBean;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DistributeWLAN;
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.bean.WLANItem;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.bridge.AODeviceFindBridge;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.DIDUtils;
import xyz.eulix.space.did.bean.DIDCredentialBean;
import xyz.eulix.space.did.bean.DIDProviderBean;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.PairingBoxInfo;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.bind.ConnectedNetwork;
import xyz.eulix.space.network.agent.bind.ProgressResult;
import xyz.eulix.space.network.agent.bind.SpaceCreateRequest;
import xyz.eulix.space.network.agent.bind.SpaceCreateResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.presenter.AODeviceFindPresenter;
import xyz.eulix.space.ui.AOCompleteActivity;
import xyz.eulix.space.ui.AOSpaceInformationActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;

public class AODeviceFindActivity extends AbsActivity<AODeviceFindPresenter.IAODeviceFind, AODeviceFindPresenter> implements AODeviceFindPresenter.IAODeviceFind
        , View.OnClickListener, AODeviceFindAdapter.OnItemClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback
        , AODeviceFindBridge.AODeviceFindSinkCallback {
    private static final String DEVICES_FIND = "devices_find";
    private static final String COMMUNICATION_TYPE = "communication_type";
    private static final int STEP_VERIFY = 0;
    private static final int STEP_BIND_COMMUNICATION_PROGRESS = STEP_VERIFY + 1;
    private static final int STEP_WIFI_LIST = STEP_BIND_COMMUNICATION_PROGRESS + 1;
    private static final int STEP_BIND_SPACE_CREATE = STEP_WIFI_LIST + 1;
    private static final int STEP_SPACE_READY_CHECK = STEP_BIND_SPACE_CREATE + 1;
    private static final int STEP_DISK_RECOGNITION = STEP_SPACE_READY_CHECK + 1;
    private static final int STEP_DISK_MANAGEMENT_LIST = STEP_DISK_RECOGNITION + 1;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private RecyclerView aoDeviceFindList;
    private AODeviceFindAdapter mAdapter;
    private int mCommunicationType;
    private InitResponse mInitResponse;
    private int mPosition;
    private List<AODeviceFindBean> mAoDeviceFindBeanList;
    private AODeviceDiscoveryManager mManager;
    private AODeviceFindBridge mBridge;
    private AODeviceFindHandler mHandler;
    private int mStep;
    private DistributeWLAN mDistributeWLAN;
    private String adminPassword;
    private PairingBoxInfo mPairingBoxInfo;
    private ReadyCheckResult mReadyCheckResult;

    static class AODeviceFindHandler extends Handler {
        private WeakReference<AODeviceFindActivity> aoDeviceFindActivityWeakReference;

        public AODeviceFindHandler(AODeviceFindActivity activity) {
            aoDeviceFindActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AODeviceFindActivity activity = aoDeviceFindActivityWeakReference.get();
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
        setContentView(R.layout.activity_ao_device_find);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        aoDeviceFindList = findViewById(R.id.ao_device_find_list);
    }

    @Override
    public void initData() {
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
        mInitResponse = mManager.getInitResponse();
        handleIntent(getIntent());
        if (mInitResponse != null) {
            mDistributeWLAN = new DistributeWLAN();
            List<InitResponseNetwork> networks = mInitResponse.getNetwork();
            boolean isConnected = ((mInitResponse.getConnected() == 0) && (networks != null && !networks.isEmpty()));
            mDistributeWLAN.setConnect(isConnected);
            boolean isNetworkConfigSupport = true;
            DeviceAbility deviceAbility = mInitResponse.getDeviceAbility();
            DeviceAbility nDeviceAbility = DeviceAbility.generateDefault(deviceAbility);
            if (nDeviceAbility != null) {
                Boolean isNetworkConfigSupportValue = nDeviceAbility.getNetworkConfigSupport();
                if (isNetworkConfigSupportValue != null) {
                    isNetworkConfigSupport = isNetworkConfigSupportValue;
                }
            }
            mDistributeWLAN.setNetworkConfigEnable(isNetworkConfigSupport);
            List<String> ipAddressList = null;
            List<NetworkAccessBean> networkAccessBeanList = null;
            String wifiName = null;
            if (networks != null) {
                Collections.sort(networks, FormatUtil.wireFirstComparator);
                ipAddressList = new ArrayList<>();
                networkAccessBeanList = new ArrayList<>();
                for (InitResponseNetwork network : networks) {
                    if (network != null) {
                        String networkWifiName = network.getWifiName();
                        NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                        networkAccessBean.setConnect(isConnected);
                        networkAccessBean.setWired(network.isWire());
                        networkAccessBean.setNetworkName(networkWifiName);
                        networkAccessBean.setShowDetail(false);
                        networkAccessBeanList.add(networkAccessBean);
                        ipAddressList.add(network.getIp());
                        if (wifiName == null) {
                            wifiName = networkWifiName;
                        }
                    }
                }
            }
            mDistributeWLAN.setConnectedWlanSsid(wifiName);
            mDistributeWLAN.setIpAddresses(ipAddressList);
            mDistributeWLAN.setNetworkAccessBeanList(networkAccessBeanList);
            mManager.setDistributeWLAN(mDistributeWLAN);
        }
        mHandler = new AODeviceFindHandler(this);
        if (mAoDeviceFindBeanList == null || mAoDeviceFindBeanList.isEmpty()) {
            finish();
        }
        mBridge = AODeviceFindBridge.getInstance();
        mBridge.registerSinkCallback(this);
        mStep = STEP_VERIFY;
    }

    @Override
    public void initViewData() {
        title.setText("");
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        mAdapter = new AODeviceFindAdapter(this, mAoDeviceFindBeanList);
        mAdapter.setOnItemClickListener(this);
        aoDeviceFindList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        aoDeviceFindList.addItemDecoration(new AODeviceFindAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_19)), Color.TRANSPARENT));
        aoDeviceFindList.setAdapter(mAdapter);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            String devicesFindValue = null;
            if (intent.hasExtra(DEVICES_FIND)) {
                devicesFindValue = intent.getStringExtra(DEVICES_FIND);
            }
            if (devicesFindValue != null) {
                try {
                    mAoDeviceFindBeanList = new Gson().fromJson(devicesFindValue, new TypeToken<List<AODeviceFindBean>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            mCommunicationType = intent.getIntExtra(COMMUNICATION_TYPE, AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_NONE);
        }
    }

    private void completeData() {
        if (mAoDeviceFindBeanList != null && mPosition >= 0 && mAoDeviceFindBeanList.size() > mPosition) {
            AODeviceFindBean bean = mAoDeviceFindBeanList.get(mPosition);
            if (bean != null) {
                bean.setBinding(false);
            }
            updateData();
        }
    }

    private void updateData() {
        if (mAdapter != null) {
            mAdapter.updateData(mAoDeviceFindBeanList);
        }
    }

    private boolean hasNetwork() {
        boolean hasNetwork = false;
        boolean isHandle = false;
        if (mManager != null) {
            DistributeWLAN distributeWLAN = mManager.getDistributeWLAN();
            if (distributeWLAN != null) {
                isHandle = true;
                List<NetworkAccessBean> networkAccessBeanList = distributeWLAN.getNetworkAccessBeanList();
                hasNetwork = (networkAccessBeanList != null && !networkAccessBeanList.isEmpty());
            }
        }
        if (!isHandle && mInitResponse != null) {
            List<InitResponseNetwork> networks = mInitResponse.getNetwork();
            hasNetwork = (networks != null && !networks.isEmpty());
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
                case STEP_WIFI_LIST:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_WIFI_LIST, null);
                    break;
                case STEP_BIND_COMMUNICATION_PROGRESS:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS, null);
                    break;
                case STEP_BIND_SPACE_CREATE:
                    String adminPassword = mManager.getAdminPassword();
                    SpaceCreateRequest spaceCreateRequest = new SpaceCreateRequest();
                    spaceCreateRequest.setClientPhoneModel(SystemUtil.getPhoneModel());
                    spaceCreateRequest.setClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                    spaceCreateRequest.setPassword(adminPassword);
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
                case STEP_SPACE_READY_CHECK:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
                    break;
                case STEP_DISK_RECOGNITION:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_RECOGNITION, null);
                    break;
                case STEP_DISK_MANAGEMENT_LIST:
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
        completeData();
        showServerExceptionToast();
    }

    private void handleFinish() {
        if (!handleFinishEvent()) {
            finish();
        }
    }

    private boolean handleFinishEvent() {
        boolean isHandle = false;
        if (mManager != null) {
            isHandle = true;
            mManager.finishSource();
            finish();
        }
        return isHandle;
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        activityId = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!handleFinishEvent()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return ((KeyEvent.KEYCODE_BACK == keyCode && handleFinishEvent()) || super.onKeyDown(keyCode, event));
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
    public AODeviceFindPresenter createPresenter() {
        return new AODeviceFindPresenter();
    }

    public static void startThisActivity(Context context, List<AODeviceFindBean> aoDeviceFindBeans, int communicationType) {
        if (context != null) {
            Intent intent = new Intent(context, AODeviceFindActivity.class);
            if (aoDeviceFindBeans != null) {
                intent.putExtra(DEVICES_FIND, new Gson().toJson(aoDeviceFindBeans, new TypeToken<List<AODeviceFindBean>>(){}.getType()));
            }
            intent.putExtra(COMMUNICATION_TYPE, communicationType);
            context.startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleFinish();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        mPosition = position;
        if (position >= 0 && mAoDeviceFindBeanList != null && mAoDeviceFindBeanList.size() > position) {
            AODeviceFindBean aoDeviceFindBean = mAoDeviceFindBeanList.get(position);
            if (aoDeviceFindBean != null) {
                int paired = aoDeviceFindBean.getBindStatus();
                boolean isNewBindProcessSupport = (mManager != null && mManager.isNewBindProcessSupport());
                switch (mStep) {
                    case STEP_WIFI_LIST:
                        aoDeviceFindBean.setBinding(true);
                        updateData();
                        if (!isNewBindProcessSupport || (!isOpenSource() && !hasNetwork())) {
                            mStep = STEP_WIFI_LIST;
                        } else if (paired != 1) {
                            mStep = STEP_BIND_SPACE_CREATE;
                        } else {
                            mStep = STEP_BIND_COMMUNICATION_PROGRESS;
                        }
                        handleRequestEvent();
                        break;
                    case STEP_BIND_COMMUNICATION_PROGRESS:
                    case STEP_BIND_SPACE_CREATE:
                    case STEP_SPACE_READY_CHECK:
                    case STEP_DISK_RECOGNITION:
                    case STEP_DISK_MANAGEMENT_LIST:
                        aoDeviceFindBean.setBinding(true);
                        updateData();
                        handleRequestEvent();
                        break;
                    default:
                        if (mBridge != null) {
                            mBridge.selectDevice(position);
                        }
                        aoDeviceFindBean.setBinding(true);
                        updateData();
                        Intent intent = null;
                        if (paired == 1) {
                            if (isNewBindProcessSupport || isOpenSource()) {
                                mStep = STEP_BIND_COMMUNICATION_PROGRESS;
                            } else {
                                mStep = STEP_WIFI_LIST;
                            }
                            handleRequestEvent();
                        } else {
                            int bluetoothValue = 0;
                            switch (mCommunicationType) {
                                case AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_BLUETOOTH:
                                    bluetoothValue = 1;
                                    break;
                                case AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_LAN:
                                    bluetoothValue = -1;
                                    break;
                                default:
                                    break;
                            }
                            intent = new Intent(this, UnbindDeviceActivity.class);
                            intent.putExtra("bluetooth", bluetoothValue);
                            intent.putExtra(ConstantField.BOUND, paired);
                        }
                        if (intent != null) {
                            startActivity(intent);
                        }
                        break;
                }
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
                    case AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS:
                        ProgressResult progressResult = null;
                        if (bodyJson != null) {
                            progressResult = new Gson().fromJson(bodyJson, ProgressResult.class);
                        }
                        if (code == ConstantField.KnownError.BindError.BOUND_CODE && ConstantField.KnownSource.AGENT.equals(source)) {
                            completeData();
                            if (mManager != null) {
                                showImageTextToast(R.drawable.toast_refuse, R.string.binding_initializing_space_change_phone_hint);
                                mManager.finishSource();
                                finish();
                            }
                        } else if (progressResult != null) {
                            int comStatus = progressResult.getComStatus();
                            if (comStatus < 0 || comStatus == ProgressResult.COM_STATUS_CONTAINERS_DOWNLOADED) {
                                completeData();
                                Intent intent = new Intent(AODeviceFindActivity.this, AOLocaleSettingsActivity.class);
                                startActivity(intent);
                            } else if (!isOpenSource() && !hasNetwork()) {
                                mStep = STEP_WIFI_LIST;
                                handleRequestEvent();
                            } else {
                                completeData();
                                if (comStatus == ProgressResult.COM_STATUS_CONTAINERS_STARTED) {
                                    AOSpaceInformationActivity.administratorStartThisActivity(this, null);
                                } else {
                                    AODeviceInitialActivity.startThisActivity(AODeviceFindActivity.this
                                            , (ProgressResult.COM_STATUS_CONTAINERS_STARTING == comStatus));
                                }
                            }
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_WIFI_LIST:
                        List<WifiInfo> wifiInfoList = null;
                        if (bodyJson != null) {
                            try {
                                wifiInfoList = new Gson().fromJson(bodyJson, new TypeToken<List<WifiInfo>>(){}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        completeData();
                        if (wifiInfoList == null || mDistributeWLAN == null) {
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
                            mDistributeWLAN.setWlanItemList(wlanItems);
                            if (mManager != null) {
                                mManager.setDistributeWLAN(mDistributeWLAN);
                            }
                            Intent distributeIntent = new Intent(AODeviceFindActivity.this, DistributeNetworkActivity.class);
                            distributeIntent.putExtra(ConstantField.WIFI_SSIDS, new Gson().toJson(mDistributeWLAN, DistributeWLAN.class));
                            if (adminPassword != null) {
                                distributeIntent.putExtra(ConstantField.PASSWORD, adminPassword);
                            }
                            startActivity(distributeIntent);
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
                            Intent intent = new Intent(AODeviceFindActivity.this, BindResultActivity.class);
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
                        } else if (contentCode == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR) {
                            completeData();
                            showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
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
                                    if (enableInternetAccess != null) {
                                        aoSpaceAccessBean = new AOSpaceAccessBean();
                                        aoSpaceAccessBean.setLanAccess(true);
                                        aoSpaceAccessBean.setP2PAccess(enableInternetAccess);
                                        aoSpaceAccessBean.setInternetAccess(enableInternetAccess);
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
                                            String avatarUrl = null;
                                            if (pairingBoxInfo != null) {
                                                avatarUrl = pairingBoxInfo.getAvatarUrl();
                                            }
                                            AOCompleteActivity.startThisActivity(AODeviceFindActivity.this, boxUuid, "1", avatarUrl);
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
                                mStep = STEP_DISK_MANAGEMENT_LIST;
                            } else {
                                mStep = STEP_DISK_RECOGNITION;
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
                            Intent intent = new Intent(AODeviceFindActivity.this, DiskInitializationActivity.class);
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
                            AOCompleteActivity.startThisActivity(AODeviceFindActivity.this, boxUuid, "1", avatarUrl);
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

    @Override
    public void unbindResult(boolean isSuccess, String password) {
        if (!isSuccess) {
            mStep = STEP_VERIFY;
            completeData();
        } else {
            adminPassword = password;
            boolean isNewBindProcessSupport = false;
            if (mManager != null) {
                mManager.setAdminPassword(adminPassword);
                isNewBindProcessSupport = mManager.isNewBindProcessSupport();
            }
            if (!isNewBindProcessSupport || (!isOpenSource() && !hasNetwork())) {
                mStep = STEP_WIFI_LIST;
            } else {
                mStep = STEP_BIND_SPACE_CREATE;
            }
            handleRequestEvent();
        }
    }
}
