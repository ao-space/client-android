package xyz.eulix.space.ui.bind;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.AODeviceFindBean;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DistributeWLAN;
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.bean.WLANItem;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.bridge.AODeviceFindBridge;
import xyz.eulix.space.bridge.BindFailBridge;
import xyz.eulix.space.bridge.EulixAuthenticationBridge;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.ui.discovery.AODeviceDiscoveryActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;

public class AODeviceBindActivity extends AODeviceDiscoveryActivity implements View.OnClickListener
        , AODeviceFindBridge.AODeviceFindSourceCallback, BindFailBridge.BindFailSourceCallback
        , EulixAuthenticationBridge.EulixAuthenticationSourceCallback {
    private ImageButton back;
    private TextView title;
    private LottieAnimationView deviceSearching;
    private LinearLayout searchingContainer;
    private LinearLayout emptyContainer;
    private Button reSearch;
    private boolean isDeviceSearchingAnimate;
    private boolean isFastDiskInitialize;
    private DistributeWLAN mDistributeWLAN;
    private Boolean isDiskInitializeNoMainStorage;
    private List<AODeviceFindBean> mAoDeviceFindBeans;
    private AODeviceFindBridge aoDeviceFindBridge;
    private BindFailBridge bindFailBridge;
    private EulixAuthenticationBridge eulixAuthenticationBridge;

    @Override
    public void initView() {
        setContentView(R.layout.activity_ao_device_bind);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        deviceSearching = findViewById(R.id.device_searching);
        searchingContainer = findViewById(R.id.searching_container);
        emptyContainer = findViewById(R.id.empty_container);
        reSearch = findViewById(R.id.re_search);
    }

    @Override
    public void initViewData() {
        title.setText("");
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        reSearch.setOnClickListener(this);
        super.initEvent();
    }

    private void setDeviceSearching(boolean isAnimate) {
        if (deviceSearching != null) {
            if (isAnimate) {
                if (!isDeviceSearchingAnimate) {
                    LottieUtil.loop(deviceSearching, "search_box.json");
                    isDeviceSearchingAnimate = true;
                }
            } else {
                if (isDeviceSearchingAnimate) {
                    LottieUtil.stop(deviceSearching, "search_box.json");
                    isDeviceSearchingAnimate = false;
                }
            }
        }
    }

    private void setSearchPattern(Boolean isSearching) {
        if (searchingContainer != null) {
            searchingContainer.setVisibility((isSearching != null && isSearching) ? View.VISIBLE : View.GONE);
        }
        if (emptyContainer != null) {
            emptyContainer.setVisibility((isSearching != null && !isSearching) ? View.VISIBLE : View.GONE);
        }
    }

    private void setReSearch(boolean isVisible) {
        if (reSearch != null) {
            reSearch.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 本地检查该盒子是否可以绑定
     * @param boxUuid 盒子的uuid
     * @return 0：表示可以绑定，±1：表示已绑定；2：未初始化
     */
    private int checkBoxUuid(String boxUuid, boolean isShowToast) {
        int status = 0;
        status = AOSpaceUtil.checkBoxUuidStatus(getApplicationContext(), boxUuid);
        if (isShowToast && status != 0) {
            switch (status) {
                case 1:
                    showImageTextToast(R.drawable.toast_refuse, R.string.bind_duplicate_administrator);
                    break;
                case -1:
                    showImageTextToast(R.drawable.toast_refuse, R.string.bind_duplicate_member);
                    break;
                default:
                    break;
            }
        }
        return status;
    }

    @Override
    protected void bindDeviceStatusChange(int status) {
        super.bindDeviceStatusChange(status);
        switch (status) {
            case STATUS_ORIGIN:
            case STATUS_COMMUNICATING:
                setReSearch(false);
                setSearchPattern(null);
                setDeviceSearching(false);
                break;
            case STATUS_SEARCHING:
            case STATUS_CONNECTING:
            case STATUS_PAIRING:
                setReSearch(false);
                setSearchPattern(true);
                setDeviceSearching(true);
                break;
            case STATUS_EMPTY:
                setReSearch(true);
                setSearchPattern(false);
                setDeviceSearching(false);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPairInitComplete(int code, String source, InitResponse initResponse) {
        super.onPairInitComplete(code, source, initResponse);
        int boxStatus = 0;
        boolean isPairedSelf = false;
        boolean connected = false;
        boolean hasNetworkConnected = false;
        boolean isOpenSource = false;
        List<InitResponseNetwork> networks = null;
        if (initResponse != null) {
            boxStatus = checkBoxUuid(initResponse.getBoxUuid(), (offlineBluetoothFunction == 0));
            isPairedSelf = AOSpaceUtil.checkPairedSelf(getApplicationContext(), initResponse.getPaired(), initResponse.getClientUuid());
            networks = initResponse.getNetwork();
            connected = (initResponse.getConnected() == 0);
            if (initResponse.isNewBindProcessSupport()) {
                hasNetworkConnected = (networks != null && !networks.isEmpty());
            } else {
                hasNetworkConnected = (connected && (networks != null && !networks.isEmpty()));
            }
            DeviceAbility deviceAbility = initResponse.getDeviceAbility();
            if (deviceAbility != null) {
                Boolean isOpenSourceValue = deviceAbility.getOpenSource();
                if (isOpenSourceValue != null) {
                    isOpenSource = isOpenSourceValue;
                }
            }
        }
        boolean isDistributeNetworkOnly = (offlineBluetoothFunction == ConstantField.OfflineBluetoothFunction.DISTRIBUTE_NETWORK);
        if (isDistributeNetworkOnly || boxStatus == 0 || (boxStatus == 2 && !isPairedSelf)) {
            if (mAoDeviceFindBeans == null) {
                mAoDeviceFindBeans = new ArrayList<>();
            } else {
                mAoDeviceFindBeans.clear();
            }
            int size = 0;
            switch (mCommunicationType) {
                case AODeviceDiscoveryManager.AODeviceDiscoveryBean.TYPE_LAN:
                    if (mIpBean != null || mNsdServiceInfo != null) {
                        size = 1;
                    } else if (mNsdServiceInfos != null) {
                        size = mNsdServiceInfos.size();
                    }
                    break;
                default:
                    break;
            }
            if (size > 0 && initResponse != null) {
                for (int i = 0; i < size; i++) {
                    AODeviceFindBean aoDeviceFindBean = new AODeviceFindBean();
                    int deviceModelNumber = 0;
                    DeviceAbility deviceAbility = initResponse.getDeviceAbility();
                    if (deviceAbility != null) {
                        Integer deviceModelNumberValue = deviceAbility.getDeviceModelNumber();
                        if (deviceModelNumberValue != null) {
                            deviceModelNumber = deviceModelNumberValue;
                        }
                        aoDeviceFindBean.setOpenSource(deviceAbility.getOpenSource());
                    }
                    aoDeviceFindBean.setDeviceModelNumber(deviceModelNumber);
                    aoDeviceFindBean.setSn(mSN);
                    aoDeviceFindBean.setBindStatus(initResponse.getPaired());
                    aoDeviceFindBean.setBinding(false);
                    mAoDeviceFindBeans.add(aoDeviceFindBean);
                }
            }
            aoDeviceFindBridge = AODeviceFindBridge.getInstance();
            aoDeviceFindBridge.registerSourceCallback(this);
            AODeviceFindActivity.startThisActivity(AODeviceBindActivity.this, mAoDeviceFindBeans, mCommunicationType);
        } else if (boxStatus == 2) {
            isFastDiskInitialize = true;
            showLoading("");
            if (isOpenSource || hasNetworkConnected) {
                handleCommunicationStep(AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
            } else {
                mDistributeWLAN = new DistributeWLAN();
                boolean isConnected = (connected && (networks != null && !networks.isEmpty()));
                mDistributeWLAN.setConnect(isConnected);
                boolean isNetworkConfigSupport = true;
                DeviceAbility deviceAbility = initResponse.getDeviceAbility();
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
                handleCommunicationStep(AODeviceDiscoveryManager.STEP_WIFI_LIST, null);
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onWifiListComplete(int code, String source, List<WifiInfo> wifiInfoList, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (isFastDiskInitialize) {
            fastWifiListHandle(source, code, wifiInfoList);
        } else {
            super.onWifiListComplete(code, source, wifiInfoList, bean);
        }
    }

    private void fastWifiListHandle(String source, int code, List<WifiInfo> wifiInfoList) {
        handlerPost(() -> {
            closeLoading();
            if (code >= 200 && code < 400) {
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
                Intent distributeIntent = new Intent(AODeviceBindActivity.this, DistributeNetworkActivity.class);
                distributeIntent.putExtra(ConstantField.WIFI_SSIDS, new Gson().toJson(mDistributeWLAN, DistributeWLAN.class));
                distributeIntent.putExtra(ConstantField.FAST_DISK_INITIALIZE, true);
                startActivity(distributeIntent);
            } else {
                showServerExceptionToast();
                finish();
            }
        });
    }

    @Override
    protected void onSpaceReadyCheckComplete(int code, String source, ReadyCheckResult readyCheckResult, AODeviceDiscoveryManager.AODeviceDiscoveryBean bean) {
        if (isFastDiskInitialize) {
            fastSpaceReadyCheckHandle(source, code, readyCheckResult);
        } else {
            super.onSpaceReadyCheckComplete(code, source, readyCheckResult, bean);
        }
    }

    private void fastSpaceReadyCheckHandle(String source, int code, ReadyCheckResult result) {
        handlerPost(() -> {
            closeLoading();
            if (code >= 200 && code < 400 && result != null) {
                isDiskInitializeNoMainStorage = result.getMissingMainStorage();
                mDiskInitializeCode = result.getDiskInitialCode();
                String boxUuid = null;
                String wlanSsid = null;
                Boolean isWire = null;
                InitResponse initResponse = null;
                if (mManager != null) {
                    initResponse = mManager.getInitResponse();
                    if (initResponse != null) {
                        boxUuid = initResponse.getBoxUuid();
                        List<InitResponseNetwork> networks = initResponse.getNetwork();
                        if (networks != null) {
                            Collections.sort(networks, FormatUtil.wireFirstComparator);
                            for (InitResponseNetwork network : networks) {
                                if (network != null) {
                                    String networkWifiName = network.getWifiName();
                                    if (wlanSsid == null) {
                                        wlanSsid = networkWifiName;
                                        if (wlanSsid != null) {
                                            isWire = network.isWire();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Intent intent = new Intent(this, BindResultActivity.class);
                intent.putExtra(ConstantField.BIND_RESULT_RETRY, true);
                if (wlanSsid != null) {
                    intent.putExtra(ConstantField.WLAN_SSID, wlanSsid);
                }
                if (isWire != null) {
                    intent.putExtra(ConstantField.IS_WIRE, (isWire ? 1 : -1));
                }
                if (mDiskInitializeCode != null) {
                    intent.putExtra(ConstantField.DISK_INITIALIZE, mDiskInitializeCode);
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
                showServerExceptionToast();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (eulixAuthenticationBridge != null) {
            eulixAuthenticationBridge.unregisterSourceCallback();
            eulixAuthenticationBridge = null;
        }
        if (bindFailBridge != null) {
            bindFailBridge.unregisterSourceCallback();
            bindFailBridge = null;
        }
        if (aoDeviceFindBridge != null) {
            aoDeviceFindBridge.unregisterSourceCallback();
            aoDeviceFindBridge = null;
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

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.re_search:
                    bindDeviceStatusChange(STATUS_ORIGIN);
                    resetStatus();
                    startSearching(false);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void aoDeviceFindSelectDevice(int index) {
        handlerPost(() -> {
            if (index >= 0 && mAoDeviceFindBeans != null && mAoDeviceFindBeans.size() > index) {
                AODeviceFindBean aoDeviceFindBean = mAoDeviceFindBeans.get(index);
                if (aoDeviceFindBean != null) {
                    int bindStatus = aoDeviceFindBean.getBindStatus();
                    if (bindStatus == 1) {
                        ;
                    } else {
                        bindFailBridge = BindFailBridge.getInstance();
                        bindFailBridge.registerSourceCallback(this);
                    }
                }
            }
        });
    }

    @Override
    public void handleFinish() {
        // Do nothing
    }

    @Override
    public void handleUnbindDevice(String password) {

    }

    @Override
    public void handleUnbindResult(boolean isSuccess, int code, String password) {
        if (isSuccess) {
            if (aoDeviceFindBridge != null) {
                aoDeviceFindBridge.unbindResult(true, password);
            }
        } else {
            boolean isHandle = false;
            if (mManager != null && (code != 0 || !mManager.isNewBindProcessSupport())) {
                if (code == 0 && !mManager.isNewBindProcessSupport()) {
                    code = 400;
                }
                isHandle = true;
                mManager.finishAllSink();
                Intent intent = new Intent(this, BindResultActivity.class);
                if (mDiskInitializeCode != null) {
                    intent.putExtra(ConstantField.DISK_INITIALIZE, mDiskInitializeCode);
                }
                String boxUuid = null;
                InitResponse initResponse = mManager.getInitResponse();
                if (initResponse != null) {
                    boxUuid = initResponse.getBoxUuid();
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
                finish();
            }
            if (!isHandle && aoDeviceFindBridge != null) {
                aoDeviceFindBridge.unbindResult(false, null);
            }
        }
    }

    @Override
    public void handleVerification() {
        eulixAuthenticationBridge = EulixAuthenticationBridge.getInstance();
        eulixAuthenticationBridge.registerSourceCallback(this);
    }

    @Override
    public void handleNewDeviceApplyResetPassword(String applyId) {

    }

    @Override
    public void handleSecurityMessagePoll(String applyId) {

    }

    @Override
    public void followUp(boolean isFollowUp) {

    }

    @Override
    public void handleAuthenticationFinish() {
        if (eulixAuthenticationBridge != null) {
            eulixAuthenticationBridge.unregisterSourceCallback();
            eulixAuthenticationBridge = null;
        }
    }
}
