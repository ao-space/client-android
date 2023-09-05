package xyz.eulix.space.ui.discovery;

import static xyz.eulix.space.manager.AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_BLUETOOTH;
import static xyz.eulix.space.manager.AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_LAN;
import static xyz.eulix.space.manager.AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_NONE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Patterns;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import xyz.eulix.space.EulixSpaceLanService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.KeyExchangeReq;
import xyz.eulix.space.bean.bind.KeyExchangeRsp;
import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.bean.bind.PubKeyExchangeReq;
import xyz.eulix.space.bean.bind.PubKeyExchangeRsp;
import xyz.eulix.space.bean.bind.RvokInfo;
import xyz.eulix.space.bean.bind.WifiRequest;
import xyz.eulix.space.bean.bind.WpwdInfo;
import xyz.eulix.space.callback.EulixSpaceLanCallback;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.AdminRevokeResults;
import xyz.eulix.space.network.agent.InitialResults;
import xyz.eulix.space.network.agent.NetworkConfigResult;
import xyz.eulix.space.network.agent.NewDeviceApplyResetPasswordEntity;
import xyz.eulix.space.network.agent.NewDeviceApplyResetPasswordRequest;
import xyz.eulix.space.network.agent.NewDeviceResetPasswordEntity;
import xyz.eulix.space.network.agent.NewDeviceResetPasswordRequest;
import xyz.eulix.space.network.agent.PairingClientInfo;
import xyz.eulix.space.network.agent.PasswordInfo;
import xyz.eulix.space.network.agent.SecurityMessagePollEntity;
import xyz.eulix.space.network.agent.SecurityMessagePollRequest;
import xyz.eulix.space.network.agent.SecurityMessagePollResponse;
import xyz.eulix.space.network.agent.SetPasswordResults;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.bind.BindRevokeRequest;
import xyz.eulix.space.network.agent.bind.BindRevokeResult;
import xyz.eulix.space.network.agent.bind.ProgressResult;
import xyz.eulix.space.network.agent.bind.SpaceCreateRequest;
import xyz.eulix.space.network.agent.bind.SpaceCreateResult;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskInitializeRequest;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.network.agent.platform.SwitchPlatformRequest;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.network.agent.platform.SwitchStatusQuery;
import xyz.eulix.space.network.agent.platform.SwitchStatusResult;
import xyz.eulix.space.presenter.AODeviceDiscoveryPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ToastUtil;

public abstract class AODeviceDiscoveryActivity extends AbsActivity<AODeviceDiscoveryPresenter.IAODeviceDiscovery, AODeviceDiscoveryPresenter> implements AODeviceDiscoveryPresenter.IAODeviceDiscovery
        , EulixSpaceLanCallback, AODeviceDiscoveryManager.AODeviceDiscoverySourceCallback {
    // 公共部分
    private static final String TAG = AODeviceDiscoveryActivity.class.getSimpleName();
    private static final long BIND_EULIX_SPACE_DISCOVERY_SERVICE_DELAY = (2 * ConstantField.TimeUnit.SECOND_UNIT);
    private static final long GET_EULIX_SPACE_DISCOVERY_BINDER_DELAY = ConstantField.TimeUnit.SECOND_UNIT;
    // 初始状态
    protected static final int STATUS_ORIGIN = 0;
    // 未找到设备
    protected static final int STATUS_EMPTY = STATUS_ORIGIN - 1;
    // 搜索设备中
    protected static final int STATUS_SEARCHING = STATUS_ORIGIN + 1;
    // 连接设备中（蓝牙指gatt和验证密钥，局域网仅验证密钥）
    protected static final int STATUS_CONNECTING = STATUS_SEARCHING + 1;
    // 与设备配对中，仅绑定时使用
    protected static final int STATUS_PAIRING = STATUS_CONNECTING + 1;
    // 通信中
    protected static final int STATUS_COMMUNICATING = STATUS_PAIRING + 1;

    // 暂停，不做任何通信

    protected int mBindDeviceStatus;
    private boolean isScanExact = true;
    private String qrCodeResult;
    protected String mSN;
    private String mBtid;
    private String mDeviceName;
    private String mBluetoothAddress;
    // 指定ip地址和端口号时，不为空
    protected IPBean mIpBean;
    protected int mCommunicationType;
    private AODeviceDiscoveryHandler mHandler;
    // 交换的密钥是否可信
    private boolean isVerifyKey;
    // 绑定时表示是否已经走过pairInit，非绑定需要直接置为true
    private boolean isPairInit;
    // 流程进行
    protected int mStep;

    protected String authenticationUuid;
    protected String granterDataUuid;
    protected int authenticationFunction = 0;
    protected int mHardwareFunction = 0;
    protected int securityFunction = 0;
    protected int passthroughFunction = 0;
    protected Boolean isAdministratorGranter = null;
    protected int offlineBluetoothFunction = 0;

    private String boxKey;
    private String boxIv;
    protected Integer mDiskInitializeCode = null;

    protected AODeviceDiscoveryManager mManager;

    // 局域网部分
    private static final int BIND_EULIX_SPACE_LAN_SERVICE = -1;
    private static final int GET_EULIX_SPACE_LAN_BINDER = BIND_EULIX_SPACE_LAN_SERVICE - 1;
    private boolean isLanSearchEnable;
    private boolean isBindEulixSpaceLanService;
    private EulixSpaceLanService.EulixSpaceLanBinder eulixSpaceLanBinder;
    private volatile boolean isLanEnable;
    private boolean isLanSearch;
    // 局域网已找到设备
//    private boolean isLanConnect;
    // 局域网就绪，当前通道丢失时作为凭证切换
    private boolean isLanAvailable;
    // 搜索时间结束后，如果未找到为false
    private boolean isLanEmpty = false;
    // 局域网已经产生了错误，后续作为立即重连还是等蓝牙连接的凭证，用过之后回到false
    private boolean isLanError = false;
    protected List<NsdServiceInfo> mNsdServiceInfos;
    protected NsdServiceInfo mNsdServiceInfo;
    private int lanTotalCountDownSecond;
    private Boolean lanCountDownStatus;
    private CountDownTimer lanCountDownTimer;

    // TODO 表示局域网有没有正在进行中的请求
    private Map<String, Integer> mRequestUuidStepMap = new ConcurrentHashMap<>();

    private Runnable startLanSearchRunnable = this::startLanSearch;

    private Runnable stopLanSearchRunnable = this::stopLanSearch;

    private ServiceConnection eulixSpaceLanServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof EulixSpaceLanService.EulixSpaceLanBinder) {
                eulixSpaceLanBinder = (EulixSpaceLanService.EulixSpaceLanBinder) service;
                isBindEulixSpaceLanService = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBindEulixSpaceLanService = false;
            eulixSpaceLanBinder = null;
            if (mHandler != null) {
                mHandler.sendEmptyMessageDelayed(BIND_EULIX_SPACE_LAN_SERVICE, BIND_EULIX_SPACE_DISCOVERY_SERVICE_DELAY);
            }
        }
    };


    static class AODeviceDiscoveryHandler extends Handler {
        private WeakReference<AODeviceDiscoveryActivity> aoDeviceDiscoveryActivityWeakReference;

        public AODeviceDiscoveryHandler(AODeviceDiscoveryActivity activity) {
            aoDeviceDiscoveryActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AODeviceDiscoveryActivity activity = aoDeviceDiscoveryActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case BIND_EULIX_SPACE_LAN_SERVICE:
                        if (activity.bindEulixSpaceLanService()) {
                            sendEmptyMessage(GET_EULIX_SPACE_LAN_BINDER);
                        } else {
                            sendEmptyMessageDelayed(BIND_EULIX_SPACE_LAN_SERVICE, BIND_EULIX_SPACE_DISCOVERY_SERVICE_DELAY);
                        }
                        break;
                    case GET_EULIX_SPACE_LAN_BINDER:
                        if (activity.eulixSpaceLanBinder == null) {
                            sendEmptyMessageDelayed(GET_EULIX_SPACE_LAN_BINDER, GET_EULIX_SPACE_DISCOVERY_BINDER_DELAY);
                        } else {
                            activity.eulixSpaceLanBinder.registerCallback(activity);
                            activity.handleLanEnable();
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    protected void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            qrCodeResult = intent.getStringExtra(ConstantField.QR_CODE_RESULT);
            mHardwareFunction = intent.getIntExtra(ConstantField.HARDWARE_FUNCTION, 0);
            securityFunction = intent.getIntExtra(ConstantField.SECURITY_FUNCTION, 0);
            offlineBluetoothFunction = intent.getIntExtra(ConstantField.OFFLINE_BLUETOOTH_FUNCTION, 0);
            if (intent.hasExtra(ConstantField.BLUETOOTH_ADDRESS)) {
                mBluetoothAddress = intent.getStringExtra(ConstantField.BLUETOOTH_ADDRESS);
            } else {
                mBluetoothAddress = null;
            }
            if (intent.hasExtra(ConstantField.BLUETOOTH_ID)) {
                mBtid = intent.getStringExtra(ConstantField.BLUETOOTH_ID);
            } else {
                mBtid = null;
            }
            if (intent.hasExtra(ConstantField.DEVICE_NAME)) {
                mDeviceName = intent.getStringExtra(ConstantField.DEVICE_NAME);
            } else if (offlineBluetoothFunction != 0) {
                mDeviceName = null;
            }
            if (intent.hasExtra(ConstantField.AUTHENTICATION_UUID)) {
                authenticationUuid = intent.getStringExtra(ConstantField.AUTHENTICATION_UUID);
            }
            authenticationFunction = intent.getIntExtra(ConstantField.AUTHENTICATION_FUNCTION, 0);
            if (intent.hasExtra(ConstantField.GRANTER_DATA_UUID)) {
                granterDataUuid = intent.getStringExtra(ConstantField.GRANTER_DATA_UUID);
            }
            if (qrCodeResult != null || mBluetoothAddress != null || mDeviceName != null || mBtid != null) {
                isScanExact = true;
            }
        }
    }

    @Override
    public void initData() {
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(this);
        handleIntent(getIntent());
        mManager.setBluetoothId(mBtid);
        mManager.setBluetoothAddress(mBluetoothAddress);
        mManager.setDeviceName(mDeviceName);
        handleQrCodeResult();
        mHandler = new AODeviceDiscoveryHandler(this);
    }

    @Override
    public void initEvent() {
        bindDeviceStatusChange(STATUS_ORIGIN);
        resetStatus();
        startSearching(true);
    }


    protected void handlerPost(Runnable runnable) {
        if (mHandler != null && runnable != null) {
            mHandler.post(runnable);
        }
    }

    protected void handlerPostDelayed(Runnable runnable, long delayMillis) {
        if (mHandler != null && runnable != null) {
            mHandler.postDelayed(runnable, delayMillis);
        }
    }

    protected void handleQrCodeResult() {
        if (qrCodeResult != null && (Patterns.WEB_URL.matcher(qrCodeResult).matches() || URLUtil.isValidUrl(qrCodeResult))) {
            Uri uri = Uri.parse(qrCodeResult);
            if (uri != null) {
                String sn = null;
                String btid = null;
                String snHashHeader = null;
                String ipAddress = null;
                String portValue = null;
                int port = -1;
                Set<String> querySet = uri.getQueryParameterNames();
                if (querySet != null) {
                    for (String query : querySet) {
                        if (ConstantField.SN.equals(query)) {
                            sn = uri.getQueryParameter(ConstantField.SN);
                            mSN = sn;
                        }
                        if (ConstantField.BTID.equals(query)) {
                            btid = uri.getQueryParameter(ConstantField.BTID);
                        }
                        if (ConstantField.IPADDR.equals(query)) {
                            ipAddress = uri.getQueryParameter(ConstantField.IPADDR);
                            String decodeIpAddress = null;
                            if (ipAddress != null) {
                                try {
                                    decodeIpAddress = URLDecoder.decode(ipAddress, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (decodeIpAddress != null) {
                                ipAddress = decodeIpAddress;
                            }
                        }
                        if (ConstantField.PORT.equals(query)) {
                            portValue = uri.getQueryParameter(ConstantField.PORT);
                        }
                    }
                }
                if (sn != null) {
                    String snHash = FormatUtil.getSHA256String(sn);
                    if (snHash != null && snHash.length() > 16) {
                        snHashHeader = snHash.substring(0, 16);
                    } else {
                        snHashHeader = snHash;
                    }
                }
                if (portValue != null) {
                    try {
                        port = Integer.parseInt(portValue);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (StringUtil.isNonBlankString(snHashHeader) || btid != null) {
                    if (ipAddress != null && port >= 0) {
                        mIpBean = new IPBean();
                        mIpBean.setIPV4Address(ipAddress);
                        mIpBean.setPort(port);
                    }
                    if (StringUtil.isNonBlankString(snHashHeader)) {
                        mBtid = snHashHeader;
                    } else if (btid != null) {
                        mBtid = btid;
                    }
                    if (mManager != null) {
                        mManager.setBluetoothId(mBtid);
                        mManager.setBluetoothAddress(mBluetoothAddress);
                    }
                }
            }
        }
    }

    /**
     * 重置所有状态，搜索前调用，包括初始时和未找到设备重新搜索
     */
    protected void resetStatus() {
        mCommunicationType = TYPE_NONE;
        lanCountDownStatus = null;
        isLanEmpty = false;
        isLanError = false;
        isVerifyKey = false;
    }

    protected void startSearching(boolean isInit) {
        if (mIpBean != null) {
            isLanSearchEnable = false;
            bindDeviceStatusChange(STATUS_CONNECTING);
            prepareLanCommunication();
        } else {
            ToastUtil.showToast(getResources().getString(R.string.service_exception_hint));
            finish();
        }
    }

    /**
     * 正式开始蓝牙和局域网搜索
     *
     * @param isBluetoothPermit 是否允许蓝牙扫描
     * @param isLanProgress     是否开始局域网扫描，蓝牙断开重连时为false
     */
    private void startSearchEvent(boolean isBluetoothPermit, boolean isLanProgress) {
        if (isBluetoothPermit || isLanProgress) {
            if (mBindDeviceStatus < STATUS_SEARCHING) {
                bindDeviceStatusChange(STATUS_SEARCHING);
            }
        }

        if (isLanProgress) {
            handleLanEnable();
        }
    }

    private void stopSearchEvent(int searchType, boolean isForce) {
        switch (searchType) {
            case TYPE_LAN:
                if (isForce || (lanCountDownStatus != null && lanCountDownStatus)) {
                    lanCountDownStatus = false;
                }
                break;
            default:
                break;
        }
        boolean isLanFindDevice = (mNsdServiceInfos != null && !mNsdServiceInfos.isEmpty());
        switch (searchType) {
            case TYPE_LAN:
                if (isLanFindDevice && isScanExact) {
                    mNsdServiceInfo = mNsdServiceInfos.get(0);
                    if (mNsdServiceInfo != null) {
                        if (mBindDeviceStatus == STATUS_SEARCHING) {
                            bindDeviceStatusChange(STATUS_CONNECTING);
                        }
                        prepareLanCommunication();
                    }
                }
                break;
            default:
                break;
        }
        if (lanCountDownStatus != null && !lanCountDownStatus) {
            if (isLanSearch) {
                stopLanSearch();
            }
            isLanEmpty = !isLanFindDevice;
            handleEmpty();
        }
    }

    private void handleEmpty() {
        if (mBindDeviceStatus >= STATUS_COMMUNICATING) {
            if (isLanEmpty) {
                if (isLanSearch) {
                    stopLanSearch();
                }
                retryStartLanSearch((10 * ConstantField.TimeUnit.SECOND_UNIT));
            }
        } else if (!isLanSearchEnable || isLanEmpty || isLanError) {
            unfocusedCommunicationType(TYPE_NONE);
            bindDeviceStatusChange(STATUS_EMPTY);
        }
    }

    protected void bindDeviceStatusChange(int status) {
        mBindDeviceStatus = status;
    }

    private void startCountDown(int type) {
        if (mBindDeviceStatus < STATUS_SEARCHING) {
            bindDeviceStatusChange(STATUS_SEARCHING);
        }
        switch (type) {
            case TYPE_LAN:
                lanCountDownStatus = true;
                startCountDown(type, lanTotalCountDownSecond);
                break;
            default:
                break;
        }

    }

    private void startCountDown(int type, int timeSecond) {
        stopCountDown(type);
        switch (type) {
            case TYPE_LAN:
                lanCountDownTimer = new CountDownTimer((timeSecond * ConstantField.TimeUnit.SECOND_UNIT), ConstantField.TimeUnit.SECOND_UNIT) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        // Do nothing
                    }

                    @Override
                    public void onFinish() {
                        stopSearchEvent(TYPE_LAN, false);
                    }
                };
                lanCountDownTimer.start();
                break;
            default:
                break;
        }
    }

    private void stopCountDown(int type) {
        switch (type) {
            case TYPE_LAN:
                if (lanCountDownTimer != null) {
                    lanCountDownTimer.cancel();
                    lanCountDownTimer = null;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 抢占交互通道
     *
     * @param type
     * @return 是否抢占成功
     */
    private synchronized boolean focusCommunicationType(int type) {
        boolean isFocused = false;
        if (type != TYPE_NONE && mCommunicationType == TYPE_NONE) {
            mCommunicationType = type;
            isFocused = true;
        }
        Logger.d(TAG, "connected type: " + type + ", is focused: " + isFocused);
        return isFocused;
    }

    /**
     * 因为error导致失去交互通道，并由其它通道代为接管
     *
     * @param type
     * @return 是否换占成功
     */
    private synchronized boolean unfocusedCommunicationType(int type) {
        boolean isRefocused = false;
        if (type == TYPE_NONE) {
            mCommunicationType = TYPE_NONE;
        } else if (mCommunicationType == type) {
            mCommunicationType = TYPE_NONE;
            Logger.d(TAG, "connected type: " + type + " is unfocused");
            switch (type) {
                case TYPE_BLUETOOTH:
                    if (isLanAvailable) {
                        isRefocused = focusCommunicationType(TYPE_LAN);
                    }
                    break;
                default:
                    break;
            }
        }
        return isRefocused;
    }

    /**
     * 设备已经连接上调用，接下来可能需要走交换密钥
     */
    private void prepareCommunication() {
        // 进行初始通信或者重新恢复通信
        if (isVerifyKey) {
            startCommunication();
        } else {
            handleCommunicationStep(AODeviceDiscoveryManager.STEP_PUBLIC_KEY_EXCHANGE, null);
        }
    }

    /**
     * 密钥交换过走该流程
     */
    private void startCommunication() {
        if (isVerifyKey) {
            if (isPairInit) {
                bindDeviceStatusChange(STATUS_COMMUNICATING);
                if (mCommunicationType != TYPE_LAN && (isLanEmpty || isLanError)) {
                    retryStartLanSearch(0);
                }
                notifyCommunication();
            } else {
                bindDeviceStatusChange(STATUS_PAIRING);
            }
        } else {
            prepareCommunication();
        }
    }

    public boolean resetCommunication(int code, String source) {
        boolean isReset = false;
        if ((code == ConstantField.KnownError.BindError.BAD_REQUEST || code == ConstantField.KnownError.BindError.SERVER_ERROR)
                && ConstantField.KnownSource.AGENT.equals(source)) {
            isReset = true;
            isVerifyKey = false;
            boxKey = null;
            boxIv = null;
        }
        return isReset;
    }

    /**
     * 用于中途中断通信后，恢复重新请求使用
     */
    protected void notifyCommunication() {
    }

    protected boolean handleCommunicationStep(int step, String id) {
        return handleCommunicationStep(step, null, id);
    }

    protected boolean handleCommunicationStep(int step, String bodyJson, String id) {
        boolean isAvailable = false;
        switch (mCommunicationType) {
            case TYPE_LAN:
                isAvailable = true;
                break;
            default:
                break;
        }
        if (isAvailable) {
            mStep = step;
            switch (step) {
                case AODeviceDiscoveryManager.STEP_PUBLIC_KEY_EXCHANGE:
                    publicKeyExchangeRequest();
                    break;
                case AODeviceDiscoveryManager.STEP_KEY_EXCHANGE:
                    keyExchangeRequest();
                    break;
                case AODeviceDiscoveryManager.STEP_PAIR_INIT:
                    pairInitRequest();
                    break;
                case AODeviceDiscoveryManager.STEP_WIFI_LIST:
                    wifiListRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_SET_WIFI:
                    setWifiRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_PAIRING:
                    pairingRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_REVOKE:
                    revokeRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_SET_PASSWORD:
                    setPasswordRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_INITIAL:
                    initialRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK:
                    spaceReadyCheckRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_DISK_RECOGNITION:
                    diskRecognitionRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_DISK_INITIALIZE:
                    diskInitializeRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS:
                    diskInitializeProgressRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST:
                    diskManagementListRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_AO_SYSTEM_SHUTDOWN:
                    aoSystemShutdownRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_AO_SYSTEM_REBOOT:
                    aoSystemRebootRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG:
                    getNetworkConfigRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_SET_NETWORK_CONFIG:
                    setNetworkConfigRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_IGNORE_NETWORK:
                    ignoreNetworkRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_SWITCH_PLATFORM:
                    switchPlatformRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_SWITCH_STATUS:
                    switchStatusRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_BIND_COM_START:
                    bindCommunicationStartRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS:
                    bindCommunicationProgressRequest(id);
                    break;
                case AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE:
                    bindSpaceCreateRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_BIND_REVOKE:
                    bindRevokeRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_NEW_DEVICE_APPLY_RESET_PASSWORD:
                    newDeviceApplyResetPasswordRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_SECURITY_MESSAGE_POLL:
                    securityMessagePollRequest(bodyJson, id);
                    break;
                case AODeviceDiscoveryManager.STEP_NEW_DEVICE_RESET_PASSWORD:
                    newDeviceResetPasswordRequest(bodyJson, id);
                    break;
                default:
                    break;
            }
        }
        return isAvailable;
    }

    private AODeviceDiscoveryManager.AODeviceDiscoveryBean generateBean(Integer sequenceNumber) {
        return new AODeviceDiscoveryManager.AODeviceDiscoveryBean(sequenceNumber);
    }

    private void processManager(String id, Integer sequenceNumber) {
        if (mManager != null && id != null) {
            mManager.process(generateBean(sequenceNumber), id);
        }
    }

    private AODeviceDiscoveryManager.AODeviceDiscoveryBean generateBean(String requestUuid) {
        return new AODeviceDiscoveryManager.AODeviceDiscoveryBean(requestUuid);
    }

    private void processManager(String id, String requestUuid) {
        if (mManager != null && id != null) {
            mManager.process(generateBean(requestUuid), id);
        }
    }

    private void handleCommunicationException(int communicationType, boolean isExpire) {
        int errorCode = (isExpire ? ConstantField.SERVER_EXCEPTION_CODE : 500);
        switch (communicationType) {
            case TYPE_LAN:
                if (!mRequestUuidStepMap.isEmpty()) {
                    Set<Map.Entry<String, Integer>> entrySet = mRequestUuidStepMap.entrySet();
                    for (Map.Entry<String, Integer> entry : entrySet) {
                        if (entry != null) {
                            String requestUuid = entry.getKey();
                            Integer step = entry.getValue();
                            if (step != null) {
                                handleStepException(step, errorCode, generateBean(requestUuid));
                            }
                        }
                    }
                    mRequestUuidStepMap.clear();
                }
                break;
            default:
                break;
        }
    }

    private void handleStepException(int step, int errorCode, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        switch (step) {
            case AODeviceDiscoveryManager.STEP_PUBLIC_KEY_EXCHANGE:
                publicKeyExchangeResponse(errorCode, null, null, null);
                break;
            case AODeviceDiscoveryManager.STEP_KEY_EXCHANGE:
                keyExchangeResponse(errorCode, null, null, null);
                break;
            case AODeviceDiscoveryManager.STEP_PAIR_INIT:
                pairInitResponse(errorCode, null, null, null);
                break;
            case AODeviceDiscoveryManager.STEP_WIFI_LIST:
                wifiListResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SET_WIFI:
                setWifiResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_PAIRING:
                pairingResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_REVOKE:
                revokeResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SET_PASSWORD:
                setPasswordResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_INITIAL:
                initialResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK:
                spaceReadyCheckResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_DISK_RECOGNITION:
                diskRecognitionResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_DISK_INITIALIZE:
                diskInitializeResponse(errorCode, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS:
                diskInitializeProgressResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST:
                diskManagementListResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_AO_SYSTEM_SHUTDOWN:
                aoSystemShutdownResponse(errorCode, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_AO_SYSTEM_REBOOT:
                aoSystemRebootResponse(errorCode, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG:
                getNetworkConfigResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SET_NETWORK_CONFIG:
                setNetworkConfigResponse(errorCode, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_IGNORE_NETWORK:
                ignoreNetworkResponse(errorCode, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SWITCH_PLATFORM:
                switchPlatformResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SWITCH_STATUS:
                switchStatusResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_BIND_COM_START:
                bindCommunicationStartResponse(errorCode, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS:
                bindCommunicationProgressResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE:
                bindSpaceCreateResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_BIND_REVOKE:
                bindRevokeResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_NEW_DEVICE_APPLY_RESET_PASSWORD:
                newDeviceApplyResetPasswordResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_SECURITY_MESSAGE_POLL:
                securityMessagePollResponse(errorCode, null, null, null, bean);
                break;
            case AODeviceDiscoveryManager.STEP_NEW_DEVICE_RESET_PASSWORD:
                newDeviceResetPasswordResponse(errorCode, null, null, null, bean);
                break;
            default:
                break;
        }
    }


    // 局域网部分

    private boolean bindEulixSpaceLanService() {
        Intent intent = new Intent(this, EulixSpaceLanService.class);
        return bindService(intent, eulixSpaceLanServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private synchronized void handleLanEnable() {
        if (isLanEnable) {
            startLanSearch();
        } else {
            isLanEnable = true;
        }
    }

    private void startLanSearch() {
        if (isLanSearchEnable) {
            isLanError = false;
            isLanEmpty = false;
            if (isBindEulixSpaceLanService && eulixSpaceLanBinder != null) {
                if (!isLanSearch) {
                    isLanSearch = true;
                    if (mNsdServiceInfos == null) {
                        mNsdServiceInfos = new CopyOnWriteArrayList<>();
                    } else {
                        mNsdServiceInfos.clear();
                    }
                    lanTotalCountDownSecond = 30;
                    startCountDown(TYPE_LAN);
                    if (!eulixSpaceLanBinder.discoverService(ConstantField.ServiceType.EULIXSPACE_SD_TCP)) {
                        stopCountDown(TYPE_LAN);
                        stopLanSearch();
                        stopSearchEvent(TYPE_LAN, true);
                    }
                }
            } else {
                retryStartLanSearch(GET_EULIX_SPACE_DISCOVERY_BINDER_DELAY);
            }
        }
    }

    private void retryStartLanSearch(long delay) {
        if (mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                while (mHandler.hasCallbacks(startLanSearchRunnable)) {
                    mHandler.removeCallbacks(startLanSearchRunnable);
                }
            } else {
                try {
                    mHandler.removeCallbacks(startLanSearchRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (delay > 0) {
                mHandler.postDelayed(startLanSearchRunnable, delay);
            } else {
                mHandler.post(startLanSearchRunnable);
            }
        }
    }

    private void stopLanSearch() {
        if (isBindEulixSpaceLanService && eulixSpaceLanBinder != null) {
            if (isLanSearch) {
                isLanSearch = false;
                eulixSpaceLanBinder.stopServiceDiscovery();
            }
        } else {
            retryStopLanSearch(GET_EULIX_SPACE_DISCOVERY_BINDER_DELAY);
        }
    }

    private void retryStopLanSearch(long delay) {
        if (mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                while (mHandler.hasCallbacks(stopLanSearchRunnable)) {
                    mHandler.removeCallbacks(stopLanSearchRunnable);
                }
            } else {
                try {
                    mHandler.removeCallbacks(stopLanSearchRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (delay > 0) {
                mHandler.postDelayed(stopLanSearchRunnable, delay);
            } else {
                mHandler.post(stopLanSearchRunnable);
            }
        }
    }

    /**
     * 将搜索到的设备加入列表
     *
     * @param nsdServiceInfo
     */
    private void insertLanDevice(NsdServiceInfo nsdServiceInfo) {
        if (mNsdServiceInfos != null && nsdServiceInfo != null) {
            boolean isAdd = false;
            if (!isScanExact) {
                isAdd = true;
            } else if (mBtid != null) {
                String btidHash = FormatUtil.getSHA256String(("eulixspace-" + mBtid));
                Logger.d(TAG, "btid hash: " + btidHash);
                if (btidHash != null) {
                    Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
                    if (attributes != null) {
                        Set<Map.Entry<String, byte[]>> entrySet = attributes.entrySet();
                        for (Map.Entry<String, byte[]> entry : entrySet) {
                            if (entry != null) {
                                String key = entry.getKey();
                                String value = StringUtil.byteArrayToString(entry.getValue(), StandardCharsets.UTF_8);
                                if (value != null && "btidhash".equals(key) && btidHash.startsWith(value)) {
                                    isAdd = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (isAdd) {
                mNsdServiceInfos.add(nsdServiceInfo);
            }
        }
        if (isScanExact && mNsdServiceInfos != null && !mNsdServiceInfos.isEmpty() && isLanSearch) {
            stopCountDown(TYPE_LAN);
            stopLanSearch();
            stopSearchEvent(TYPE_LAN, true);
        }
    }

    private void prepareLanCommunication() {
        isLanAvailable = true;
        if (presenter != null) {
            if (mIpBean == null) {
                presenter.generateBaseUrl(mNsdServiceInfo);
            } else {
                presenter.generateBaseUrl(mIpBean);
            }
        }
        if (focusCommunicationType(TYPE_LAN)) {
            prepareCommunication();
        }
    }

    private void handleLanFinishStepCommunication(String requestUuid) {
        if (requestUuid != null) {
            mRequestUuidStepMap.remove(requestUuid);
        }
    }

    private void handleLanError() {
        handleLanError(false);
    }

    private void handleLanError(boolean isExpire) {
        if (!isLanError) {
            isLanError = true;
            isLanAvailable = false;
            if (mNsdServiceInfos != null) {
                mNsdServiceInfos.clear();
            }
            mNsdServiceInfo = null;
            boolean isRefocused = unfocusedCommunicationType(TYPE_LAN);
            if (mBindDeviceStatus >= STATUS_COMMUNICATING) {
                // 准备重连
                if (mIpBean == null) {
                    if (isLanSearch) {
                        stopLanSearch();
                    }
                    retryStartLanSearch((10 * ConstantField.TimeUnit.SECOND_UNIT));
                    if (isRefocused) {
                        prepareCommunication();
                    }
                } else {
                    isLanError = false;
                    isLanEmpty = false;
                    prepareLanCommunication();
                }
            } else {
                handleEmpty();
            }
        }
        handleCommunicationException(TYPE_LAN, isExpire);
    }

    // TODO 业务部分

    /**
     * 用于处理发生错误时的交互
     *
     * @param communicationType
     */
    private void handleCommunicationError(int communicationType) {
        switch (communicationType) {
            case TYPE_LAN:
                handleLanError();
                break;
            default:
                break;
        }
    }

    private void publicKeyExchangeRequest() {
        if (presenter != null) {
            PubKeyExchangeReq pubKeyExchangeReq = new PubKeyExchangeReq();
            pubKeyExchangeReq.setClientPubKey(StringUtil.wrapPublicKey(DataUtil.getClientPublicKey(getApplicationContext())));
            pubKeyExchangeReq.setSignedBtid(EncryptionUtil.signRSAPrivateKey(ConstantField.Algorithm.SignatureAlgorithm.SHA256_WITH_RSA
                    , null, mBtid, DataUtil.getClientPrivateKey(getApplicationContext()), StandardCharsets.UTF_8));
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_PUBLIC_KEY_EXCHANGE);
                    if (!presenter.exchangePublicKey(pubKeyExchangeReq, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void publicKeyExchangeResponse(int code, String source, String message, PubKeyExchangeRsp rsp, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!publicKeyExchangeResponse(code, source, message, rsp)) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    private boolean publicKeyExchangeResponse(int code, String source, String message, PubKeyExchangeRsp rsp) {
        boolean isOk = true;
        boolean isSuccess = (code >= 200 && code < 400 && rsp != null);
        String boxPublicKey = null;
        if (rsp != null) {
            boxPublicKey = StringUtil.unwrapPublicKey(rsp.getBoxPubKey());
            isSuccess = EncryptionUtil.verifyRSAPublicKey(ConstantField.Algorithm.SignatureAlgorithm.SHA256_WITH_RSA
                    , null, rsp.getSignedBtid(), mBtid, boxPublicKey, StandardCharsets.UTF_8);

        }
        if (isSuccess && boxPublicKey != null) {
            if (mManager != null) {
                mManager.setBoxPublicKey(boxPublicKey);
            }
            handleCommunicationStep(AODeviceDiscoveryManager.STEP_KEY_EXCHANGE, null);
        } else {
            isOk = false;
        }
        return isOk;
    }

    private void keyExchangeRequest() {
        if (presenter != null) {
            String boxPublicKey = null;
            if (mManager != null) {
                boxPublicKey = mManager.getBoxPublicKey();
            }
            KeyExchangeReq keyExchangeReq = new KeyExchangeReq();
            if (boxPublicKey == null) {
                keyExchangeReq.setClientPreSecret(DataUtil.generateRandomNumber(32));
                keyExchangeReq.setEncBtid(mBtid);
            } else {
                keyExchangeReq.setClientPreSecret(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1, null
                        , DataUtil.generateRandomNumber(32), boxPublicKey, null, null));
                keyExchangeReq.setEncBtid(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1, null
                        , mBtid, boxPublicKey, null, null));
            }

            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_KEY_EXCHANGE);
                    if (!presenter.exchangeSecretKey(keyExchangeReq, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void keyExchangeResponse(int code, String source, String message, KeyExchangeRsp rsp, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!keyExchangeResponse(code, source, message, rsp)) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean keyExchangeResponse(int code, String source, String message, KeyExchangeRsp rsp) {
        boolean isOk = true;
        boolean isSuccess = (code >= 200 && code < 400 && rsp != null);
        if (rsp != null) {
            String sharedSecret = rsp.getSharedSecret();
            String iv = rsp.getIv();
            if (sharedSecret != null) {
                boxKey = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                        , null, sharedSecret, DataUtil.getClientPrivateKey(getApplicationContext()), null, null);
                boxIv = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                        , null, iv, DataUtil.getClientPrivateKey(getApplicationContext()), null, null);
//                                            boxIv = StringUtil.base64Encode(EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
//                                                    , null, iv, DataUtil.getClientPrivateKey(getApplicationContext()), null, null), StandardCharsets.UTF_8);
                Logger.d(TAG, "box key: " + boxKey + ", iv: " + boxIv);
            }
        }
        if (isSuccess && boxKey != null && boxIv != null) {
            isVerifyKey = true;
            startCommunication();
            if (!isPairInit) {
                handleCommunicationStep(AODeviceDiscoveryManager.STEP_PAIR_INIT, null);
            }
        } else {
            isOk = false;
        }
        return isOk;
    }

    private void pairInitRequest() {
        mDiskInitializeCode = null;
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_PAIR_INIT);
                    if (!presenter.pairInit(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void pairInitResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!pairInitResponse(code, source, message, results)) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean pairInitResponse(int code, String source, String message, String results) {
        boolean isOk = true;
        InitResponse initResponse = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "init response: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    initResponse = new Gson().fromJson(decryptedContent, InitResponse.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || initResponse == null) {
            isOk = false;
        } else {
            isPairInit = true;
            startCommunication();
            onPairInitComplete(code, source, initResponse);
        }
        return isOk;
    }

    protected void onPairInitComplete(int code, String source, InitResponse initResponse) {
        if (mManager != null) {
            mManager.setInitResponse(initResponse);
        }
    }

    private void wifiListRequest(String id) {
        if (presenter != null) {
            WifiRequest wifiRequest = new WifiRequest();
            wifiRequest.setCount(30);
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_WIFI_LIST);
                    processManager(id, requestUuid);
                    if (!presenter.getWifiList(wifiRequest, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void wifiListResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!wifiListResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean wifiListResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        List<WifiInfo> wifiInfoList = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "wifi info list: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    wifiInfoList = new Gson().fromJson(decryptedContent, new TypeToken<List<WifiInfo>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || wifiInfoList == null) {
            isOk = false;
        }
        onWifiListComplete(code, source, wifiInfoList, bean);
        return isOk;
    }

    protected void onWifiListComplete(int code, String source, List<WifiInfo> wifiInfoList, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_WIFI_LIST, new Gson().toJson(wifiInfoList, new TypeToken<List<WifiInfo>>() {
            }.getType()));
        }
    }

    private void setWifiRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            WpwdInfo wpwdInfo = null;
            try {
                wpwdInfo = new Gson().fromJson(bodyJson, WpwdInfo.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (wpwdInfo == null) {
                wpwdInfo = new WpwdInfo();
            }
            wpwdInfo.setAddr(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, wpwdInfo.getAddr(), boxKey, StandardCharsets.UTF_8, boxIv));
            wpwdInfo.setPwd(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, wpwdInfo.getPwd(), boxKey, StandardCharsets.UTF_8, boxIv));
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SET_WIFI);
                    processManager(id, requestUuid);
                    if (!presenter.setWifi(wpwdInfo, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void setWifiResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!setWifiResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean setWifiResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        NetworkConfigResult networkConfigResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "network config result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    networkConfigResult = new Gson().fromJson(decryptedContent, NetworkConfigResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || networkConfigResult == null) {
            isOk = false;
        }
        onSetWifiComplete(code, source, networkConfigResult, bean);
        return isOk;
    }

    protected void onSetWifiComplete(int code, String source, NetworkConfigResult networkConfigResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SET_WIFI, new Gson().toJson(networkConfigResult, NetworkConfigResult.class));
        }
    }

    private void pairingRequest(String id) {
        if (presenter != null) {
            PairingClientInfo pairingClientInfo = presenter.generatePairingClientInfo();
//            pairingClientInfo.setClientPubKey(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                    , null, pairingClientInfo.getClientPubKey(), boxKey, StandardCharsets.UTF_8, boxIv));
            pairingClientInfo.setClientUuid(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, pairingClientInfo.getClientUuid(), boxKey, StandardCharsets.UTF_8, boxIv));
            pairingClientInfo.setClientPhoneModel(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, pairingClientInfo.getClientPhoneModel(), boxKey, StandardCharsets.UTF_8, boxIv));
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_PAIRING);
                    processManager(id, requestUuid);
                    if (!presenter.pairing(pairingClientInfo, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void pairingResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!pairingResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean pairingResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        PairingBoxResults pairingBoxResults = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "pair result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    pairingBoxResults = new Gson().fromJson(decryptedContent, PairingBoxResults.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || pairingBoxResults == null) {
            isOk = false;
        }
        onPairingComplete(code, source, pairingBoxResults, bean);
        return isOk;
    }

    protected void onPairingComplete(int code, String source, PairingBoxResults pairingBoxResults, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_PAIRING, new Gson().toJson(pairingBoxResults, PairingBoxResults.class));
        }
    }

    private void revokeRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            RvokInfo rvokInfo = null;
            try {
                rvokInfo = new Gson().fromJson(bodyJson, RvokInfo.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (rvokInfo == null) {
                rvokInfo = new RvokInfo();
            }
            rvokInfo.setClientUUID(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, rvokInfo.getClientUUID(), boxKey, StandardCharsets.UTF_8, boxIv));
            rvokInfo.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, rvokInfo.getPassword(), boxKey, StandardCharsets.UTF_8, boxIv));
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_REVOKE);
                    processManager(id, requestUuid);
                    if (!presenter.revoke(rvokInfo, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void revokeResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!revokeResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean revokeResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        AdminRevokeResults adminRevokeResults = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "revoke result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    adminRevokeResults = new Gson().fromJson(decryptedContent, AdminRevokeResults.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onRevokeComplete(code, source, adminRevokeResults, bean);
        return isOk;
    }

    protected void onRevokeComplete(int code, String source, AdminRevokeResults adminRevokeResults, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_REVOKE, new Gson().toJson(adminRevokeResults, AdminRevokeResults.class));
        }
    }

    private void setPasswordRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            PasswordInfo passwordInfo = null;
            try {
                passwordInfo = new Gson().fromJson(bodyJson, PasswordInfo.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (passwordInfo == null) {
                passwordInfo = new PasswordInfo();
            }
            passwordInfo.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, passwordInfo.getPassword(), boxKey, StandardCharsets.UTF_8, boxIv));
            String oldPassword = passwordInfo.getOldPassword();
            if (oldPassword != null) {
                passwordInfo.setOldPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, oldPassword, boxKey, StandardCharsets.UTF_8, boxIv));
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SET_PASSWORD);
                    processManager(id, requestUuid);
                    if (!presenter.setPassword(passwordInfo, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void setPasswordResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!setPasswordResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean setPasswordResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        SetPasswordResults setPasswordResults = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "set password result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    setPasswordResults = new Gson().fromJson(decryptedContent, SetPasswordResults.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || setPasswordResults == null) {
            isOk = false;
        }
        onSetPasswordComplete(code, source, setPasswordResults, bean);
        return isOk;
    }

    protected void onSetPasswordComplete(int code, String source, SetPasswordResults setPasswordResults, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SET_PASSWORD, new Gson().toJson(setPasswordResults, SetPasswordResults.class));
        }
    }

    private void initialRequest(String bodyJson, String id) {
        if (presenter != null) {
            PasswordInfo passwordInfo = null;
            if (bodyJson != null) {
                try {
                    passwordInfo = new Gson().fromJson(bodyJson, PasswordInfo.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (passwordInfo == null) {
                    passwordInfo = new PasswordInfo();
                }
                passwordInfo.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, passwordInfo.getPassword(), boxKey, StandardCharsets.UTF_8, boxIv));
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_INITIAL);
                    processManager(id, requestUuid);
                    if (!presenter.initial(passwordInfo, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void initialResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!initialResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean initialResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        InitialResults initialResults = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "initial result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    initialResults = new Gson().fromJson(decryptedContent, InitialResults.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || initialResults == null) {
            isOk = false;
        }
        onInitialComplete(code, source, initialResults, bean);
        return isOk;
    }

    protected void onInitialComplete(int code, String source, InitialResults initialResults, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_INITIAL, new Gson().toJson(initialResults, InitialResults.class));
        }
    }

    private void spaceReadyCheckRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK);
                    processManager(id, requestUuid);
                    if (!presenter.spaceReadyCheck(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void spaceReadyCheckResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!spaceReadyCheckResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean spaceReadyCheckResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        ReadyCheckResult readyCheckResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "space ready check result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    readyCheckResult = new Gson().fromJson(decryptedContent, ReadyCheckResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || readyCheckResult == null) {
            isOk = false;
        }
        onSpaceReadyCheckComplete(code, source, readyCheckResult, bean);
        return isOk;
    }

    protected void onSpaceReadyCheckComplete(int code, String source, ReadyCheckResult readyCheckResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, new Gson().toJson(readyCheckResult, ReadyCheckResult.class));
        }
    }

    private void diskRecognitionRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_DISK_RECOGNITION);
                    processManager(id, requestUuid);
                    if (!presenter.diskRecognition(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void diskRecognitionResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!diskRecognitionResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean diskRecognitionResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        DiskRecognitionResult diskRecognitionResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "disk recognition result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    diskRecognitionResult = new Gson().fromJson(decryptedContent, DiskRecognitionResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || diskRecognitionResult == null) {
            isOk = false;
        }
        onDiskRecognitionComplete(code, source, diskRecognitionResult, bean);
        return isOk;
    }

    protected void onDiskRecognitionComplete(int code, String source, DiskRecognitionResult diskRecognitionResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_DISK_RECOGNITION, new Gson().toJson(diskRecognitionResult, DiskRecognitionResult.class));
        }
    }

    private void diskInitializeRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            DiskInitializeRequest diskInitializeRequest = null;
            try {
                diskInitializeRequest = new Gson().fromJson(bodyJson, DiskInitializeRequest.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (diskInitializeRequest == null) {
                diskInitializeRequest = new DiskInitializeRequest();
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_DISK_INITIALIZE);
                    processManager(id, requestUuid);
                    if (!presenter.diskInitialize(diskInitializeRequest, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void diskInitializeResponse(int code, String source, String message, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!diskInitializeResponse(code, source, message, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean diskInitializeResponse(int code, String source, String message, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onDiskInitializeComplete(code, source, bean);
        return isOk;
    }

    protected void onDiskInitializeComplete(int code, String source, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        // todo 进入存档点
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_DISK_INITIALIZE, null);
        }
    }

    private void diskInitializeProgressRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS);
                    processManager(id, requestUuid);
                    if (!presenter.diskInitializeProgress(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void diskInitializeProgressResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!diskInitializeProgressResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean diskInitializeProgressResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        DiskInitializeProgressResult diskInitializeProgressResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "disk initialize progress result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    diskInitializeProgressResult = new Gson().fromJson(decryptedContent, DiskInitializeProgressResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || diskInitializeProgressResult == null) {
            isOk = false;
        }
        onDiskInitializeProgressComplete(code, source, diskInitializeProgressResult, bean);
        return isOk;
    }

    protected void onDiskInitializeProgressComplete(int code, String source, DiskInitializeProgressResult diskInitializeProgressResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS, new Gson().toJson(diskInitializeProgressResult, DiskInitializeProgressResult.class));
        }
    }

    private void diskManagementListRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST);
                    processManager(id, requestUuid);
                    if (!presenter.diskManagementList(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void diskManagementListResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!diskManagementListResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean diskManagementListResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        DiskManageListResult diskManageListResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "disk management list result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    diskManageListResult = new Gson().fromJson(decryptedContent, DiskManageListResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || diskManageListResult == null) {
            isOk = false;
        }
        onDiskManagementListComplete(code, source, diskManageListResult, bean);
        return isOk;
    }

    protected void onDiskManagementListComplete(int code, String source, DiskManageListResult diskManageListResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        // todo 进入存档点
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST, new Gson().toJson(diskManageListResult, DiskManageListResult.class));
        }
    }

    private void aoSystemShutdownRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_AO_SYSTEM_SHUTDOWN);
                    processManager(id, requestUuid);
                    if (!presenter.aoSystemShutdown(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void aoSystemShutdownResponse(int code, String source, String message, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!aoSystemShutdownResponse(code, source, message, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean aoSystemShutdownResponse(int code, String source, String message, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onAOSystemShutdownComplete(code, source, bean);
        return isOk;
    }

    protected void onAOSystemShutdownComplete(int code, String source, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_AO_SYSTEM_SHUTDOWN, null);
        }
    }

    private void aoSystemRebootRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_AO_SYSTEM_REBOOT);
                    processManager(id, requestUuid);
                    if (!presenter.aoSystemReboot(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void aoSystemRebootResponse(int code, String source, String message, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!aoSystemRebootResponse(code, source, message, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean aoSystemRebootResponse(int code, String source, String message, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onAOSystemRebootComplete(code, source, bean);
        return isOk;
    }

    protected void onAOSystemRebootComplete(int code, String source, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_AO_SYSTEM_REBOOT, null);
        }
    }

    private void getNetworkConfigRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG);
                    processManager(id, requestUuid);
                    if (!presenter.getNetworkConfig(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void getNetworkConfigResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!getNetworkConfigResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean getNetworkConfigResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        NetworkStatusResult networkStatusResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "get network config result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    networkStatusResult = new Gson().fromJson(decryptedContent, NetworkStatusResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onGetNetworkConfigComplete(code, source, networkStatusResult, bean);
        return isOk;
    }

    protected void onGetNetworkConfigComplete(int code, String source, NetworkStatusResult networkStatusResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_GET_NETWORK_CONFIG, new Gson().toJson(networkStatusResult, NetworkStatusResult.class));
        }
    }

    private void setNetworkConfigRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            NetworkConfigRequest networkConfigRequest = null;
            try {
                networkConfigRequest = new Gson().fromJson(bodyJson, NetworkConfigRequest.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (networkConfigRequest == null) {
                networkConfigRequest = new NetworkConfigRequest();
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SET_NETWORK_CONFIG);
                    processManager(id, requestUuid);
                    if (!presenter.setNetworkConfig(networkConfigRequest, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void setNetworkConfigResponse(int code, String source, String message, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!setNetworkConfigResponse(code, source, message, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean setNetworkConfigResponse(int code, String source, String message, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onSetNetworkConfigComplete(code, source, bean);
        return isOk;
    }

    protected void onSetNetworkConfigComplete(int code, String source, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SET_NETWORK_CONFIG, null);
        }
    }

    private void ignoreNetworkRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            NetworkIgnoreRequest networkIgnoreRequest = null;
            try {
                networkIgnoreRequest = new Gson().fromJson(bodyJson, NetworkIgnoreRequest.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (networkIgnoreRequest == null) {
                networkIgnoreRequest = new NetworkIgnoreRequest();
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_IGNORE_NETWORK);
                    processManager(id, requestUuid);
                    if (!presenter.ignoreNetwork(networkIgnoreRequest, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void ignoreNetworkResponse(int code, String source, String message, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!ignoreNetworkResponse(code, source, message, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean ignoreNetworkResponse(int code, String source, String message, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onIgnoreNetworkComplete(code, source, bean);
        return isOk;
    }

    protected void onIgnoreNetworkComplete(int code, String source, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_IGNORE_NETWORK, null);
        }
    }

    private void switchPlatformRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            SwitchPlatformRequest switchPlatformRequest = null;
            try {
                switchPlatformRequest = new Gson().fromJson(bodyJson, SwitchPlatformRequest.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (switchPlatformRequest == null) {
                switchPlatformRequest = new SwitchPlatformRequest();
            }
            switchPlatformRequest.setTransId(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, switchPlatformRequest.getTransId(), boxKey, StandardCharsets.UTF_8, boxIv));
            switchPlatformRequest.setDomain(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, switchPlatformRequest.getDomain(), boxKey, StandardCharsets.UTF_8, boxIv));
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SWITCH_PLATFORM);
                    processManager(id, requestUuid);
                    if (!presenter.switchPlatform(switchPlatformRequest, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void switchPlatformResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!switchPlatformResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean switchPlatformResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        SwitchPlatformResult switchPlatformResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "switch platform result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    switchPlatformResult = new Gson().fromJson(decryptedContent, SwitchPlatformResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || switchPlatformResult == null) {
            isOk = false;
        }
        onSwitchPlatformComplete(code, source, switchPlatformResult, bean);
        return isOk;
    }

    protected void onSwitchPlatformComplete(int code, String source, SwitchPlatformResult switchPlatformResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SWITCH_PLATFORM, new Gson().toJson(switchPlatformResult, SwitchPlatformResult.class));
        }
    }

    private void switchStatusRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            SwitchStatusQuery switchStatusQuery = null;
            try {
                switchStatusQuery = new Gson().fromJson(bodyJson, SwitchStatusQuery.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (switchStatusQuery == null) {
                switchStatusQuery = new SwitchStatusQuery();
            }
            switchStatusQuery.setTransId(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, switchStatusQuery.getTransId(), boxKey, StandardCharsets.UTF_8, boxIv));
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SWITCH_STATUS);
                    processManager(id, requestUuid);
                    if (!presenter.switchStatus(switchStatusQuery.getTransId(), requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void switchStatusResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!switchStatusResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean switchStatusResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        SwitchStatusResult switchStatusResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "switch status query result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    switchStatusResult = new Gson().fromJson(decryptedContent, SwitchStatusResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || switchStatusResult == null) {
            isOk = false;
        }
        onSwitchStatusComplete(code, source, switchStatusResult, bean);
        return isOk;
    }

    protected void onSwitchStatusComplete(int code, String source, SwitchStatusResult switchStatusResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SWITCH_STATUS, new Gson().toJson(switchStatusResult, SwitchStatusResult.class));
        }
    }

    private void bindCommunicationStartRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_BIND_COM_START);
                    processManager(id, requestUuid);
                    if (!presenter.bindCommunicationStart(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void bindCommunicationStartResponse(int code, String source, String message, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!bindCommunicationStartResponse(code, source, message, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean bindCommunicationStartResponse(int code, String source, String message, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onBindCommunicationStartComplete(code, source, bean);
        return isOk;
    }

    protected void onBindCommunicationStartComplete(int code, String source, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_BIND_COM_START, null);
        }
    }

    private void bindCommunicationProgressRequest(String id) {
        if (presenter != null) {
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS);
                    processManager(id, requestUuid);
                    if (!presenter.getBindCommunicationProgress(requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void bindCommunicationProgressResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!bindCommunicationProgressResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean bindCommunicationProgressResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        ProgressResult progressResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "bind communication progress result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    progressResult = new Gson().fromJson(decryptedContent, ProgressResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || progressResult == null) {
            isOk = false;
        }
        onBindCommunicationProgressComplete(code, source, progressResult, bean);
        return isOk;
    }

    protected void onBindCommunicationProgressComplete(int code, String source, ProgressResult progressResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS, new Gson().toJson(progressResult, ProgressResult.class));
        }
    }

    private void bindSpaceCreateRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            SpaceCreateRequest spaceCreateRequest = null;
            try {
                spaceCreateRequest = new Gson().fromJson(bodyJson, SpaceCreateRequest.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (spaceCreateRequest == null) {
                spaceCreateRequest = new SpaceCreateRequest();
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE);
                    processManager(id, requestUuid);
                    if (!presenter.bindSpaceCreate(spaceCreateRequest, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void bindSpaceCreateResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!bindSpaceCreateResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean bindSpaceCreateResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        SpaceCreateResult spaceCreateResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "bind space create result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    spaceCreateResult = new Gson().fromJson(decryptedContent, SpaceCreateResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || spaceCreateResult == null) {
            isOk = false;
        }
        onBindSpaceCreateComplete(code, source, spaceCreateResult, bean);
        return isOk;
    }

    protected void onBindSpaceCreateComplete(int code, String source, SpaceCreateResult spaceCreateResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_BIND_SPACE_CREATE, new Gson().toJson(spaceCreateResult, SpaceCreateResult.class));
        }
    }

    private void bindRevokeRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            BindRevokeRequest bindRevokeRequest = null;
            try {
                bindRevokeRequest = new Gson().fromJson(bodyJson, BindRevokeRequest.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (bindRevokeRequest == null) {
                bindRevokeRequest = new BindRevokeRequest();
            }
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_BIND_REVOKE);
                    processManager(id, requestUuid);
                    if (!presenter.bindRevoke(bindRevokeRequest, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void bindRevokeResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!bindRevokeResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean bindRevokeResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        BindRevokeResult bindRevokeResult = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "bind revoke result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    bindRevokeResult = new Gson().fromJson(decryptedContent, BindRevokeResult.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onBindRevokeComplete(code, source, bindRevokeResult, bean);
        return isOk;
    }

    protected void onBindRevokeComplete(int code, String source, BindRevokeResult bindRevokeResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_BIND_REVOKE, new Gson().toJson(bindRevokeResult, BindRevokeResult.class));
        }
    }

    private void newDeviceApplyResetPasswordRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            NewDeviceApplyResetPasswordEntity entity = null;
            try {
                entity = new Gson().fromJson(bodyJson, NewDeviceApplyResetPasswordEntity.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            NewDeviceApplyResetPasswordRequest request = new NewDeviceApplyResetPasswordRequest();
            request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_APPLY_LOCAL);
            request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
            request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_APPLY_LOCAL);
            request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
            request.setEntity(entity);
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_NEW_DEVICE_APPLY_RESET_PASSWORD);
                    processManager(id, requestUuid);
                    if (!presenter.newDeviceApplyResetPassword(request, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void newDeviceApplyResetPasswordResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!newDeviceApplyResetPasswordResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean newDeviceApplyResetPasswordResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        EulixBaseResponse eulixBaseResponse = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "new device apply reset password result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    eulixBaseResponse = new Gson().fromJson(decryptedContent, EulixBaseResponse.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onNewDeviceApplyResetPasswordComplete(code, source, eulixBaseResponse, bean);
        return isOk;
    }

    protected void onNewDeviceApplyResetPasswordComplete(int code, String source, EulixBaseResponse eulixBaseResponse, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_NEW_DEVICE_APPLY_RESET_PASSWORD, new Gson().toJson(eulixBaseResponse, EulixBaseResponse.class));
        }
    }

    private void securityMessagePollRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            SecurityMessagePollEntity entity = null;
            try {
                entity = new Gson().fromJson(bodyJson, SecurityMessagePollEntity.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            SecurityMessagePollRequest request = new SecurityMessagePollRequest();
            request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_MESSAGE_POLL_LOCAL);
            request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
            request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_MESSAGE_POLL_LOCAL);
            request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
            request.setEntity(entity);
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_SECURITY_MESSAGE_POLL);
                    processManager(id, requestUuid);
                    if (!presenter.securityMessagePoll(request, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void securityMessagePollResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!securityMessagePollResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean securityMessagePollResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        SecurityMessagePollResponse securityMessagePollResponse = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "security message poll result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    securityMessagePollResponse = new Gson().fromJson(decryptedContent, SecurityMessagePollResponse.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onSecurityMessagePollComplete(code, source, securityMessagePollResponse, bean);
        return isOk;
    }

    protected void onSecurityMessagePollComplete(int code, String source, SecurityMessagePollResponse securityMessagePollResponse, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_SECURITY_MESSAGE_POLL, new Gson().toJson(securityMessagePollResponse, SecurityMessagePollResponse.class));
        }
    }


    private void newDeviceResetPasswordRequest(String bodyJson, String id) {
        if (presenter != null && bodyJson != null) {
            NewDeviceResetPasswordEntity entity = null;
            try {
                entity = new Gson().fromJson(bodyJson, NewDeviceResetPasswordEntity.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            NewDeviceResetPasswordRequest request = new NewDeviceResetPasswordRequest();
            request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_LOCAL);
            request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
            request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_LOCAL);
            request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
            request.setEntity(entity);
            switch (mCommunicationType) {
                case TYPE_LAN:
                    String requestUuid = UUID.randomUUID().toString();
                    mRequestUuidStepMap.put(requestUuid, AODeviceDiscoveryManager.STEP_NEW_DEVICE_RESET_PASSWORD);
                    processManager(id, requestUuid);
                    if (!presenter.newDeviceResetPassword(request, boxKey, boxIv, requestUuid)) {
                        handleLanFinishStepCommunication(requestUuid);
                        handleLanError();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void newDeviceResetPasswordResponse(int code, String source, String message, String results, String requestUuid) {
        handleLanFinishStepCommunication(requestUuid);
        if (!newDeviceResetPasswordResponse(code, source, message, results, generateBean(requestUuid))) {
            handleCommunicationError(TYPE_LAN);
        }
    }

    protected boolean newDeviceResetPasswordResponse(int code, String source, String message, String results, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        boolean isOk = true;
        EulixBaseResponse eulixBaseResponse = null;
        if (results != null) {
            String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, results, boxKey, StandardCharsets.UTF_8, boxIv);
            Logger.d(TAG, "new device reset password result: " + decryptedContent);
            if (decryptedContent != null) {
                try {
                    eulixBaseResponse = new Gson().fromJson(decryptedContent, EulixBaseResponse.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resetCommunication(code, source) || code == ConstantField.SERVER_EXCEPTION_CODE) {
            isOk = false;
        }
        onNewDeviceResetPasswordComplete(code, source, eulixBaseResponse, bean);
        return isOk;
    }

    protected void onNewDeviceResetPasswordComplete(int code, String source, EulixBaseResponse eulixBaseResponse, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (mManager != null) {
            mManager.response(bean, code, source, AODeviceDiscoveryManager.STEP_NEW_DEVICE_RESET_PASSWORD, new Gson().toJson(eulixBaseResponse, EulixBaseResponse.class));
        }
    }

    @Override
    protected void onDestroy() {
        resetStatus();
        if (isBindEulixSpaceLanService && eulixSpaceLanBinder != null) {
            try {
                unbindService(eulixSpaceLanServiceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                eulixSpaceLanBinder = null;
            }
            isBindEulixSpaceLanService = false;
        }

        if (mManager != null) {
            mManager.finishAllSink();
            mManager.unregisterCallback();
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @NotNull
    @Override
    public AODeviceDiscoveryPresenter createPresenter() {
        return new AODeviceDiscoveryPresenter();
    }

    @Override
    public void discoveryChange(int number) {
        // Do nothing
    }

    @Override
    public void resolveDevice(NsdServiceInfo nsdServiceInfo) {
        if (mHandler == null) {
            insertLanDevice(nsdServiceInfo);
        } else {
            mHandler.post(() -> insertLanDevice(nsdServiceInfo));
        }
    }

    @Override
    public void onFinish() {
        if (mManager != null) {
            mManager.finishAllSink();
        }
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public boolean onRequest(int step, String bodyJson, String requestId) {
        return (isVerifyKey && handleCommunicationStep(step, bodyJson, requestId));
    }
}
