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
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.eulix.space.EulixSpaceLanActivity;
import xyz.eulix.space.R;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DistributeWLAN;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.LanServiceInfo;
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.bean.WLANItem;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.bridge.BindFailBridge;
import xyz.eulix.space.bridge.BindResultBridge;
import xyz.eulix.space.bridge.DiskInitializeBridge;
import xyz.eulix.space.bridge.DistributeNetworkBridge;
import xyz.eulix.space.bridge.EulixAuthenticationBridge;
import xyz.eulix.space.bridge.LanFindBoxBridge;
import xyz.eulix.space.bridge.SecurityPasswordBridge;
import xyz.eulix.space.bridge.SimpleHardwareVerificationBridge;
import xyz.eulix.space.network.agent.AdminRevokeResult;
import xyz.eulix.space.network.agent.SecurityMessagePollResult;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DrawableUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.ScreenUtil;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/2 15:13
 */
public class LanBindBoxActivity extends EulixSpaceLanActivity implements LanFindBoxBridge.LanFindBoxSourceCallback
        , BindFailBridge.BindFailSourceCallback, DistributeNetworkBridge.DistributeNetworkSourceCallback
        , EulixAuthenticationBridge.EulixAuthenticationSourceCallback
        , SecurityPasswordBridge.SecurityPasswordSourceCallback, BindResultBridge.BindResultSourceCallback, DiskInitializeBridge.DiskInitializeSourceCallback {
    public static final String TAG = LanBindBoxActivity.class.getSimpleName();
    private ImageView bindDeviceMantle;
    private ImageButton back;
    private TextView title;
    private LottieAnimationView deviceSearching;
    private TextView deviceSearchTitle;
    private TextView deviceSearchContent;
    private Button bindDeviceFunction;
    private boolean prepareBack = false;
    private boolean prepareFind = false;
    private boolean isScan = false;
    private LanFindBoxBridge lanFindBoxBridge;
    private BindFailBridge bindFailBridge;
    private BindResultBridge bindResultBridge;
    private DiskInitializeBridge diskInitializeBridge;
    private DistributeNetworkBridge distributeNetworkBridge;
    private EulixAuthenticationBridge eulixAuthenticationBridge;
    private SimpleHardwareVerificationBridge simpleHardwareVerificationBridge;
    private SecurityPasswordBridge securityPasswordBridge;
    private boolean isRefreshWifi;
    private DistributeWLAN mDistributeWLAN;
    private int mCode;
    private int mPaired;
    private String mPassword;
    private int mInitialEstimateTimeSec;
    private String mSspUrl;
    private boolean mNetworkConfigEnable = true;
    private boolean isDiskInitialize;
    private Boolean isDiskInitializeNoMainStorage;
    private Integer mDiskInitializeCode = null;
    private boolean isFastDiskInitialize;
    private DeviceAbility mDeviceAbility;

    @Override
    public void initView() {
        setContentView(R.layout.activity_lan_bind_box);
        bindDeviceMantle = findViewById(R.id.bind_device_mantle);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        deviceSearching = findViewById(R.id.device_searching);
        deviceSearchTitle = findViewById(R.id.device_search_title);
        deviceSearchContent = findViewById(R.id.device_search_content);
        bindDeviceFunction = findViewById(R.id.bind_device_function);
    }

    @Override
    public void initData() {
        mPaired = 0;
        super.initData();
//        mHandler = new LanBindBoxHandler(this);
//        boxObserver = new ContentObserver(mHandler) {
//            @Override
//            public void onChange(boolean selfChange, @Nullable Uri uri) {
//                super.onChange(selfChange, uri);
//                handleEulixSpaceDBChange();
//            }
//        };
//        isActiveStartSearchDevice = false;
//        isBindEulixSpaceLanService = false;
//        handleIntent(getIntent());
    }

    @Override
    public void initViewData() {
        title.setText(R.string.bind_device);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        bindDeviceFunction.setOnClickListener(this);
        bindDeviceStatusChange(ConstantField.BindDeviceStatus.SEARCHING);
        if (mIpBean == null && presenter != null) {
            presenter.startCountdown(SCAN_COUNTDOWN_SECOND);
        }
        super.initEvent();
    }

    private void setBoxSearching(boolean isWork) {
        if (isWork) {
            LottieUtil.loop(deviceSearching, "search_box.json");
        } else {
            LottieUtil.stop(deviceSearching, "search_box.json");
        }
    }

    @Override
    protected void setBoxSearingIndicator(int index) {
        super.setBoxSearingIndicator(index);
        if (index < 0) {
            deviceSearchTitle.setVisibility(View.INVISIBLE);
        } else {
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(getString(isScan ? R.string.searching_box_indicator : R.string.connecting));
            while (index > 0) {
                contentBuilder.append(".");
                index -= 1;
            }
            deviceSearchTitle.setVisibility(View.VISIBLE);
            deviceSearchTitle.setText(contentBuilder.toString());
        }
    }

    private void distributeNetwork() {
        if (mDistributeWLAN != null) {
            distributeNetworkBridge = DistributeNetworkBridge.getInstance();
            distributeNetworkBridge.registerSourceCallback(this);
            Intent distributeIntent = new Intent(LanBindBoxActivity.this, DistributeNetworkActivity.class);
            distributeIntent.putExtra(ConstantField.WIFI_SSIDS, new Gson().toJson(mDistributeWLAN, DistributeWLAN.class));
            distributeIntent.putExtra(ConstantField.DISTRIBUTE_NETWORK, false);
            distributeIntent.putExtra(ConstantField.INITIAL_ESTIMATE_TIME, mInitialEstimateTimeSec);
            if (mSspUrl != null) {
                distributeIntent.putExtra(ConstantField.PLATFORM_URL, mSspUrl);
            }
            startActivityForResult(distributeIntent, ConstantField.RequestCode.WIFI_DISTRIBUTE_CODE);
        }
    }

    private void handleDistributeNetwork(boolean isSuccess) {
        if (isSuccess) {
            bindResultBridge = BindResultBridge.getInstance();
            bindResultBridge.registerSourceCallback(this);
            String boxUuid = null;
            if (presenter != null) {
                boxUuid = presenter.getBoxUuid();
            }
            handlePaired(mCode, boxUuid);
        } else {
            bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
        }
    }

    @Override
    protected void bindDeviceStatusChange(int status) {
        super.bindDeviceStatusChange(status);
        bindDeviceStatus = status;
        switch (bindDeviceStatus) {
            case ConstantField.BindDeviceStatus.SEARCHING:
                isScan = true;
                setBoxSearching(true);
                deviceSearchTitle.setTextColor(getResources().getColor(R.color.blue_ff337aff));
                deviceSearchTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.dp_12));
                deviceSearchTitle.setTypeface(Typeface.DEFAULT);
                deviceSearchContent.setText(R.string.bind_device_hint);
                deviceSearchContent.setTextColor(getResources().getColor(R.color.gray_ff85899c));
                bindDeviceFunction.setClickable(false);
                bindDeviceFunction.setVisibility(View.INVISIBLE);
                break;
            case ConstantField.BindDeviceStatus.EMPTY:
                isScan = false;
                setBoxSearching(false);
                deviceSearchTitle.setText(R.string.no_device_found);
                deviceSearchTitle.setTextColor(getResources().getColor(R.color.black_ff333333));
                deviceSearchTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.dp_17));
                deviceSearchTitle.setTypeface(Typeface.DEFAULT_BOLD);
                deviceSearchContent.setText(Html.fromHtml((getString(R.string.no_device_found_hint_part_1)
                        + "<b><tt>" + getString(R.string.rescan) + "</tt></b>"
                        + getString(R.string.no_device_found_hint_part_2))));
                bindDeviceFunction.setVisibility(View.VISIBLE);
                bindDeviceFunction.setText(R.string.rescan);
                bindDeviceFunction.setClickable(true);
                break;
            case ConstantField.BindDeviceStatus.BINDING:
                isScan = false;
                setBoxSearingIndicator(0);
                break;
            case ConstantField.BindDeviceStatus.BIND_FAILED:
                isScan = false;
                setBoxSearching(false);
                setBoxSearingIndicator(-1);
                break;
            default:
                break;
        }
    }

    private void startFindBoxActivity(String boxName, String productId) {
        if (!prepareBack && !prepareFind) {
            prepareFind = true;
            bindDeviceMantle.setVisibility(View.VISIBLE);
            bindDeviceMantle.setBackground(DrawableUtil.bitmapToDrawable(DrawableUtil.blur(ScreenUtil
                    .screenShot(this), this), getResources()));
            lanFindBoxBridge = LanFindBoxBridge.getInstance();
            lanFindBoxBridge.registerSourceCallback(this);
            Intent intent = new Intent(LanBindBoxActivity.this, LanFindBoxActivity.class);
            if (nsdServiceInfos != null && nsdServiceInfos.size() > 0) {
                intent.putExtra(ConstantField.EULIX_DEVICE, nsdServiceInfos.toArray(new Parcelable[0]));
            } else if (mIpBean != null) {
                intent.putExtra(ConstantField.OTHER_DEVICE, new Gson().toJson(mIpBean, IPBean.class));
            }
            if (boxName != null) {
                intent.putExtra(ConstantField.BOX_NAME, boxName);
            }
            if (productId != null) {
                intent.putExtra(ConstantField.PRODUCT_ID, productId);
            }
            intent.putExtra(ConstantField.BOUND, mPaired);
            if (mDeviceAbility != null) {
                int deviceModelNumber = 0;
                Integer deviceModelNumberValue = mDeviceAbility.getDeviceModelNumber();
                if (deviceModelNumberValue != null) {
                    deviceModelNumber = deviceModelNumberValue;
                }
                intent.putExtra(ConstantField.DEVICE_MODEL_NUMBER, deviceModelNumber);
            }
            startActivityForResult(intent, ConstantField.RequestCode.FIND_DEVICE_CODE);
            ScreenUtil.screenShotReset(this);
        }
    }

    private void handlePaired(Integer code, String boxUuid) {
        if (code != null) {
            if (code >= 400) {
                Intent intent = new Intent(LanBindBoxActivity.this, BindResultActivity.class);
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
            } else if (code == ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE) {
                Intent intent = new Intent(LanBindBoxActivity.this, BindResultActivity.class);
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
                Intent passwordIntent = new Intent(LanBindBoxActivity.this, SecurityPasswordSettingActivity.class);
                passwordIntent.putExtra(ConstantField.DISK_INITIALIZE_ENABLE, isDiskInitialize);
                passwordIntent.putExtra(ConstantField.BIND_RESULT, code);
                if (boxUuid != null) {
                    passwordIntent.putExtra(ConstantField.BOX_UUID, boxUuid);
                }
                if (presenter != null) {
                    String baseUrl = presenter.getBaseUrl();
                    String domain = presenter.getUserDomain();
                    String bleKey = presenter.getBleKey();
                    String bleIv = presenter.getBleIv();
                    if (baseUrl != null) {
                        passwordIntent.putExtra(ConstantField.BASE_URL, baseUrl);
                    }
                    if (domain != null) {
                        passwordIntent.putExtra(ConstantField.DOMAIN, domain);
                    }
                    if (bleKey != null) {
                        passwordIntent.putExtra(ConstantField.BLE_KEY, bleKey);
                    }
                    if (bleIv != null) {
                        passwordIntent.putExtra(ConstantField.BLE_IV, bleIv);
                    }
                }
                startActivity(passwordIntent);
            }
        }
    }

    private void prepareDiskInitialize() {
        if (bindResultBridge != null) {
            bindResultBridge.unregisterSourceCallback();
            bindResultBridge = null;
        }
        diskInitializeBridge = DiskInitializeBridge.getInstance();
        diskInitializeBridge.registerSourceCallback(this);
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (lanFindBoxBridge != null) {
            lanFindBoxBridge.unregisterSourceCallback();
            lanFindBoxBridge = null;
        }
        if (diskInitializeBridge != null) {
            diskInitializeBridge.unregisterSourceCallback();
            diskInitializeBridge = null;
        }
        if (bindResultBridge != null) {
            bindResultBridge.unregisterSourceCallback();
            bindResultBridge = null;
        }
        if (bindFailBridge != null) {
            bindFailBridge.unregisterSourceCallback();
            bindFailBridge = null;
        }
        if (distributeNetworkBridge != null) {
            distributeNetworkBridge.unregisterSourceCallback();
            distributeNetworkBridge = null;
        }
        if (eulixAuthenticationBridge != null) {
            eulixAuthenticationBridge.unregisterSourceCallback();
            eulixAuthenticationBridge = null;
        }
        if (simpleHardwareVerificationBridge != null) {
            simpleHardwareVerificationBridge.unregisterSourceCallback();
            simpleHardwareVerificationBridge = null;
        }

        if (securityPasswordBridge != null) {
            securityPasswordBridge.unregisterSourceCallback();
            securityPasswordBridge = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    prepareBack = true;
                    finish();
                    break;
                case R.id.bind_device_function:
                    if (bindDeviceStatus == ConstantField.BindDeviceStatus.EMPTY) {
                        handleConnectEvent();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void handleInitCallback(InitResponse initResponse) {
        super.handleInitCallback(initResponse);
        if (initResponse == null) {
            countDownFinish();
        } else {
            mDiskInitializeCode = null;
            String boxUuid = initResponse.getBoxUuid();
            mSspUrl = initResponse.getSspUrl();
            mInitialEstimateTimeSec = initResponse.getInitialEstimateTimeSec();
            DeviceAbility deviceAbility = initResponse.getDeviceAbility();
            mDeviceAbility = deviceAbility;
            DeviceAbility nDeviceAbility = DeviceAbility.generateDefault(deviceAbility);
            boolean isInnerDiskSupport = false;
            boolean isNetworkConfigSupport = true;
            if (nDeviceAbility != null) {
                Boolean isInnerDiskSupportValue = nDeviceAbility.getInnerDiskSupport();
                Boolean isNetworkConfigSupportValue = nDeviceAbility.getNetworkConfigSupport();
                if (isInnerDiskSupportValue != null) {
                    isInnerDiskSupport = isInnerDiskSupportValue;
                }
                if (isNetworkConfigSupportValue != null) {
                    isNetworkConfigSupport = isNetworkConfigSupportValue;
                }
            }
            mNetworkConfigEnable = isNetworkConfigSupport;
            isDiskInitialize = isInnerDiskSupport;
            mDistributeWLAN = new DistributeWLAN();
            List<InitResponseNetwork> networks = initResponse.getNetwork();
            boolean isConnect = ((initResponse.getConnected() == 0) && (networks != null && !networks.isEmpty()));
            mDistributeWLAN.setConnect(isConnect);
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
                        networkAccessBean.setConnect(isConnect);
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
            int boxStatus = checkBoxUuid(boxUuid, true);
            mPaired = initResponse.getPaired();
            String pairClientUuid = initResponse.getClientUuid();
            boolean isPairedSelf = false;
            if (presenter != null) {
                isPairedSelf = presenter.checkPairedSelf(mPaired, pairClientUuid);
            }
            if (boxStatus == 0 || (boxStatus == 2 && !isPairedSelf)) {
                isFastDiskInitialize = false;
                startFindBoxActivity(initResponse.getBoxName(), initResponse.getProductId());
            } else if (boxStatus == 2) {
                isFastDiskInitialize = true;
                if (presenter != null) {
                    mDiskInitializeCode = null;
                    presenter.spaceReadyCheck();
                }
            } else {
                bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
            }
        }
    }

    @Override
    protected void handleRevokeCallback(Integer code, String message, AdminRevokeResult adminRevokeResult) {
        super.handleRevokeCallback(code, message, adminRevokeResult);
        if (bindFailBridge != null) {
            String boxUuid = null;
            int errorTimes = -1;
            int leftTryTimes = -1;
            int tryAfterSeconds = -1;
            if (adminRevokeResult != null) {
                boxUuid = adminRevokeResult.getBoxUuid();
                errorTimes = adminRevokeResult.getErrorTimes();
                leftTryTimes = adminRevokeResult.getLeftTryTimes();
                tryAfterSeconds = adminRevokeResult.getTryAfterSeconds();
            }
            bindFailBridge.unbindResult((code == null ? -1 : code), boxUuid, errorTimes, leftTryTimes, tryAfterSeconds);
        }
    }

    @Override
    protected void wifiListCallbackHandle(List<WifiInfo> wifiInfoList) {
        super.wifiListCallbackHandle(wifiInfoList);
        if (isRefreshWifi) {
            if (distributeNetworkBridge != null) {
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
                distributeNetworkBridge.setWlanList(wlanItems);
            }
        } else {
            boolean isSuccess = false;
            if (wifiInfoList != null) {
                isSuccess = true;
                if (mDistributeWLAN == null) {
                    mDistributeWLAN = new DistributeWLAN();
                    mDistributeWLAN.setNetworkConfigEnable(mNetworkConfigEnable);
                }
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
            }
            if (lanFindBoxBridge != null) {
                lanFindBoxBridge.bindResult(isSuccess, false);
            }
        }
    }

    @Override
    protected void setWifiCallbackHandle(String ssid, List<String> ipAddresses, int status) {
        super.setWifiCallbackHandle(ssid, ipAddresses, status);
        boolean isSuccess = (status == 0);
        if (isSuccess && mDistributeWLAN != null) {
            mDistributeWLAN.setConnect(true);
            mDistributeWLAN.setIpAddresses(ipAddresses);
        }
        distributeNetworkBridge.distributeNetworkResult(ssid, ipAddresses, isSuccess);
    }

    @Override
    protected void pairingCallbackHandle(Integer code, String boxUuid) {
        super.pairingCallbackHandle(code, boxUuid);
        if (code != null) {
            mCode = code;
        }
        if (distributeNetworkBridge != null) {
            distributeNetworkBridge.pairingResult((code != null || boxUuid != null), code);
        }
    }

    @Override
    protected void handleOnInitialCallback(Integer code) {
        super.handleOnInitialCallback(code);
        mCode = code;
        if (presenter != null) {
            if (distributeNetworkBridge != null) {
                distributeNetworkBridge.progressStateChange(DistributeNetworkBridge.PROGRESS_INITIALIZE);
            }
            if (mPassword == null) {
                presenter.initialize();
            } else {
                presenter.initialize(mPassword);
            }
        }
    }

    @Override
    protected void initialResultCallback(Integer result) {
        super.initialResultCallback(result);
        if (result != null && result == 200) {
            if (presenter != null) {
                if (isDiskInitialize) {
                    if (distributeNetworkBridge != null) {
                        distributeNetworkBridge.progressStateChange(DistributeNetworkBridge.PROGRESS_SPACE_READY_CHECK);
                    }
                    mDiskInitializeCode = null;
                    presenter.spaceReadyCheck();
                } else {
                    presenter.requestUseBox(false);
                    pairingCallback(presenter.getCode(), presenter.getBoxUuid());
                }
            }
        }
        if (!(result != null && result == 200) && distributeNetworkBridge != null) {
            distributeNetworkBridge.pairingResult(false, result);
        }
    }

    @Override
    protected void spaceReadyCheckHandle(String source, int code, ReadyCheckResult result) {
        super.spaceReadyCheckHandle(source, code, result);
        if (isFastDiskInitialize) {
            fastSpaceReadyCheckHandle(source, code, result);
        } else {
            if (result != null) {
                isDiskInitializeNoMainStorage = result.getMissingMainStorage();
                mDiskInitializeCode = result.getDiskInitialCode();
            }
            if (presenter != null) {
                presenter.requestUseBox(true);
            }
            if (distributeNetworkBridge != null) {
                distributeNetworkBridge.spaceReadyCheck(code, source, result);
            }
        }

    }


    private void fastSpaceReadyCheckHandle(String source, int code, ReadyCheckResult result) {
        handlerPost(() -> {
            boolean isSuccess = (code >= 200 && code < 400 && result != null);
            if (isSuccess) {
                bindResultBridge = BindResultBridge.getInstance();
                bindResultBridge.registerSourceCallback(this);
            }
            if (result != null) {
                isDiskInitializeNoMainStorage = result.getMissingMainStorage();
                mDiskInitializeCode = result.getDiskInitialCode();
            }
            String boxUuid = null;
            if (presenter != null) {
                boxUuid = presenter.getBoxUuid();
            }
            String wlanSsid = null;
            if (mDistributeWLAN != null) {
                wlanSsid = mDistributeWLAN.getConnectedWlanSsid();
            }
            Intent intent = new Intent(this, BindResultActivity.class);
            intent.putExtra(ConstantField.BIND_RESULT_RETRY, true);
            if (!isSuccess) {
                intent.putExtra(ConstantField.BIND_RESULT_RETRY_FAIL, true);
            }
            if (wlanSsid != null) {
                intent.putExtra(ConstantField.WLAN_SSID, wlanSsid);
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
        });
    }

    @Override
    protected void diskRecognitionHandle(String source, int code, DiskRecognitionResult result) {
        super.diskRecognitionHandle(source, code, result);
        if (bindResultBridge != null) {
            bindResultBridge.responseDiskRecognition(code, source, result);
        }
    }

    @Override
    protected void diskInitializeHandle(String source, int code) {
        super.diskInitializeHandle(source, code);
        if (diskInitializeBridge != null) {
            diskInitializeBridge.responseDiskInitialize(code, source);
        }
    }

    @Override
    protected void diskInitializeProgressHandle(String source, int code, DiskInitializeProgressResult result) {
        super.diskInitializeProgressHandle(source, code, result);
        if (diskInitializeBridge != null) {
            diskInitializeBridge.responseDiskInitializeProgress(code, source, result);
        }
    }

    @Override
    protected void diskManagementListHandle(String source, int code, DiskManageListResult result) {
        super.diskManagementListHandle(source, code, result);
        if (diskInitializeBridge != null) {
            diskInitializeBridge.responseDiskManagementList(code, source, result);
        }
    }

    @Override
    protected void systemShutdownHandle(String source, int code) {
        super.systemShutdownHandle(source, code);
        if (diskInitializeBridge != null) {
            diskInitializeBridge.responseEulixSystemShutdown(code, source);
        }
    }

    @Override
    protected void getNetworkConfig(String source, int code, NetworkStatusResult result) {
        super.getNetworkConfig(source, code, result);
        if (distributeNetworkBridge != null) {
            distributeNetworkBridge.setAccessNetwork(code, source, result);
        }
    }

    @Override
    protected void updateNetworkConfig(String source, int code) {
        super.updateNetworkConfig(source, code);
        if (distributeNetworkBridge != null) {
            distributeNetworkBridge.handleSetNetworkConfigResult(code, source);
        }
    }

    @Override
    protected void ignoreNetworkConfig(String source, int code) {
        super.ignoreNetworkConfig(source, code);
        if (distributeNetworkBridge != null) {
            distributeNetworkBridge.handleIgnoreNetworkConfigResult(code, source);
        }
    }

    @Override
    public void handleNewDeviceApplyResetPasswordCallback(String source, int code) {
        super.handleNewDeviceApplyResetPasswordCallback(source, code);
        if (bindFailBridge != null) {
            bindFailBridge.newDeviceApplyResetPasswordResponse(source, code);
        }
    }

    @Override
    public void handleSecurityMessagePollCallback(String source, int code, String applyId, SecurityMessagePollResult result) {
        super.handleSecurityMessagePollCallback(source, code, applyId, result);
        if (bindFailBridge != null) {
            bindFailBridge.securityMessagePollResponse(source, code, applyId, result);
        }
    }


    @Override
    protected void handleNewDeviceResetPasswordCallback(String source, int code) {
        super.handleNewDeviceResetPasswordCallback(source, code);
        if (securityPasswordBridge != null) {
            securityPasswordBridge.newDeviceResetPasswordResponse(source, code);
        }
    }

    @Override
    protected void handleSwitchPlatformCallback(String source, int code, SwitchPlatformResult result, boolean isError) {
        super.handleSwitchPlatformCallback(source, code, result, isError);
        if (bindResultBridge != null) {
            bindResultBridge.handleSwitchPlatformResponse(code, source, result);
        }
    }

    @Override
    public void connectBox(LanServiceInfo serviceInfo, int paired) {
        if (paired == 0 || paired == 2) {
            bindFailBridge = BindFailBridge.getInstance();
            bindFailBridge.registerSourceCallback(this);
            Intent intent = new Intent(LanBindBoxActivity.this, UnbindDeviceActivity.class);
            intent.putExtra("bluetooth", -1);
            intent.putExtra(ConstantField.BOUND, paired);
            String baseUrl = null;
            String bleKey = null;
            String bleIv = null;
            if (presenter != null) {
                baseUrl = presenter.getBaseUrl();
                bleKey = presenter.getBleKey();
                bleIv = presenter.getBleIv();
            }
            if (baseUrl != null) {
                intent.putExtra(ConstantField.BASE_URL, baseUrl);
            }
            if (bleKey != null) {
                intent.putExtra(ConstantField.BLE_KEY, bleKey);
            }
            if (bleIv != null) {
                intent.putExtra(ConstantField.BLE_IV, bleIv);
            }
            startActivity(intent);
        } else if (presenter != null) {
            isRefreshWifi = false;
            presenter.getWifiList();
        }
    }

    @Override
    public void handleFinish() {
        handlerPost(this::finish);
//        if (mHandler != null) {
//            mHandler.post(this::finish);
//        }
    }

    @Override
    public void handleUnbindDevice(String password) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.revoke(password);
            }
        });
//        if (mHandler != null) {
//            mHandler.post(() -> {
//                if (presenter != null) {
//                    presenter.revoke(password);
//                }
//            });
//        }
    }

    @Override
    public void handleUnbindResult(boolean isSuccess, int code, String password) {
        handlerPost(() -> {
            if (isSuccess) {
                mPassword = password;
                if (presenter != null) {
                    isRefreshWifi = false;
                    presenter.getWifiList();
                }
            } else {
                if (lanFindBoxBridge != null) {
                    lanFindBoxBridge.bindResult(false, true);
                }
                Intent intent = new Intent(LanBindBoxActivity.this, BindResultActivity.class);
                if (mDiskInitializeCode != null) {
                    intent.putExtra(ConstantField.DISK_INITIALIZE, mDiskInitializeCode);
                }
                String boxUuid = null;
                if (presenter != null) {
                    boxUuid = presenter.getBoxUuid();
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
        });
//        if (mHandler != null) {
//            mHandler.post(() -> {
//                if (isSuccess) {
//                    mPassword = password;
//                    if (presenter != null) {
//                        isRefreshWifi = false;
//                        presenter.getWifiList();
//                    }
//                } else {
//                    if (lanFindBoxBridge != null) {
//                        lanFindBoxBridge.bindResult(false, true);
//                    }
//                    Intent intent = new Intent(LanBindBoxActivity.this, BindResultActivity.class);
//                    intent.putExtra(ConstantField.BIND_TYPE, true);
//                    intent.putExtra(ConstantField.BIND_RESULT, code);
//                    startActivity(intent);
//                    finish();
//                }
//            });
//        }
    }

    @Override
    public void handleVerification() {
        eulixAuthenticationBridge = EulixAuthenticationBridge.getInstance();
        eulixAuthenticationBridge.registerSourceCallback(this);
    }

    @Override
    public void handleNewDeviceApplyResetPassword(String applyId) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.newDeviceApplyResetPassword(applyId);
            }
        });
    }

    @Override
    public void handleSecurityMessagePoll(String applyId) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.securityMessagePoll(applyId);
            }
        });
    }

    @Override
    public void requestWlanList() {
        if (presenter != null) {
            isRefreshWifi = true;
            presenter.getWifiList();
        }
    }

    @Override
    public void requestAccessNetwork() {
        if (presenter != null) {
            presenter.getNetworkConfig();
        }
    }

    @Override
    public void setNetworkConfig(String dns1, String dns2, String ipv6DNS1, String ipv6DNS2, List<NetworkAdapter> networkAdapters) {
        if (presenter != null) {
            NetworkConfigRequest networkConfigRequest = new NetworkConfigRequest();
            networkConfigRequest.setdNS1(dns1);
            networkConfigRequest.setdNS2(dns2);
            networkConfigRequest.setIpv6DNS1(ipv6DNS1);
            networkConfigRequest.setIpv6DNS2(ipv6DNS2);
            networkConfigRequest.setNetworkAdapters(networkAdapters);
            presenter.updateNetworkConfig(networkConfigRequest);
        }
    }

    @Override
    public void ignoreNetworkConfig(String wifiName) {
        if (presenter != null) {
            NetworkIgnoreRequest networkIgnoreRequest = new NetworkIgnoreRequest();
            networkIgnoreRequest.setwIFIName(wifiName);
            presenter.ignoreNetwork(networkIgnoreRequest);
        }
    }

    @Override
    public void distributeWlan(String ssid, String address, String password) {
        if (ssid != null && address != null && password != null) {
            handlerPost(() -> {
                if (presenter != null) {
                    presenter.setWifi(ssid, address, password);
                }
            });
        }
//        if (mHandler != null && ssid != null && password != null) {
//            mHandler.post(() -> {
//                if (presenter != null) {
//                    presenter.setWifi(ssid, address, password);
//                }
//            });
//        }
    }

    @Override
    public void handlePairing() {
        handlerPost(() -> {
            boolean isSuccess = false;
            if (presenter != null && mDistributeWLAN != null) {
                presenter.bindDevice(mPaired);
                isSuccess = true;
            }
            if (distributeNetworkBridge != null && !isSuccess) {
                distributeNetworkBridge.pairingResult(false, mCode);
            }
        });
//        if (mHandler != null) {
//            mHandler.post(() -> {
//                boolean isSuccess = false;
//                if (presenter != null && mDistributeWLAN != null) {
//                    presenter.bindDevice(mPaired);
//                    isSuccess = true;
//                }
//                if (distributeNetworkBridge != null && !isSuccess) {
//                    distributeNetworkBridge.pairingResult(false, mCode);
//                }
//            });
//        }
    }

    @Override
    public void handleInitializeProgress() {
        handlerPost(() -> {
            if (presenter != null) {
                if (mPassword == null) {
                    presenter.initialize();
                } else {
                    presenter.initialize(mPassword);
                }
            }
        });
    }

    @Override
    public void handleSpaceReadyCheckProgress() {
        handlerPost(() -> {
            if (presenter != null) {
                mDiskInitializeCode = null;
                presenter.spaceReadyCheck();
            }
        });
    }

    @Override
    public void distributeNetworkCallback() {
        handlerPost(this::finish);
//        if (mHandler != null) {
//            mHandler.post(this::finish);
//        }
    }

    @Override
    public void handleBindResult(boolean isHandle) {
        handlerPost(() -> {
            if (isHandle) {
                bindResultBridge = BindResultBridge.getInstance();
                bindResultBridge.registerSourceCallback(this);
            } else if (bindResultBridge != null) {
                bindResultBridge.unregisterSourceCallback();
                bindResultBridge = null;
            }
        });
    }

    @Override
    public void followUp(boolean isFollowUp) {
        if (isFollowUp) {
            if (simpleHardwareVerificationBridge == null) {
                simpleHardwareVerificationBridge = SimpleHardwareVerificationBridge.getInstance();
            }
            simpleHardwareVerificationBridge.registerSourceCallback(this);
        } else if (simpleHardwareVerificationBridge != null) {
            simpleHardwareVerificationBridge.unregisterSourceCallback();
            simpleHardwareVerificationBridge = null;
        }
    }

    @Override
    public void handleAuthenticationFinish() {
        if (eulixAuthenticationBridge != null) {
            eulixAuthenticationBridge.unregisterSourceCallback();
            eulixAuthenticationBridge = null;
        }
    }

    @Override
    public void handleSecurityPassword(String oldPassword, String password) {
        // Do nothing
    }

    @Override
    public void handleResetSecurityPassword(boolean isGranter, String password, String granterSecurityToken) {
        // Do nothing
    }

    @Override
    public void handleNewDeviceResetPassword(String acceptSecurityToken, String emailSecurityToken, String granterClientUuid, String password) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.newDeviceResetPassword(acceptSecurityToken, emailSecurityToken, granterClientUuid, password);
            }
        });
    }

    @Override
    public void handleBindResultHardwareFinishCallback() {
        ;
    }

    @Override
    public void handleBindResultFinishCallback() {
        ;
    }

    @Override
    public void handleDiskRecognitionRequest() {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.diskRecognition();
            }
        });
    }

    @Override
    public void remindDiskInitializeBridge() {
        handlerPost(this::prepareDiskInitialize);
    }

    @Override
    public void handleForceRequestSwitchPlatform(String taskId, String platformUrl) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.switchPlatform(taskId, platformUrl);
            }
        });
    }

    @Override
    public void handleDiskInitializeRequest(boolean isDiskEncrypt, boolean isRaid, List<String> primaryStorageHardwareIds, List<String> secondaryStorageHardwareIds, List<String> raidDiskHardwareIds) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.diskInitialize(isDiskEncrypt, isRaid, primaryStorageHardwareIds, secondaryStorageHardwareIds, raidDiskHardwareIds);
            }
        });
    }

    @Override
    public void handleDiskInitializeProgressRequest() {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.getDiskInitializeProgress();
            }
        });
    }

    @Override
    public void handleDiskManagementListRequest() {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.getDiskManagementList();
            }
        });
    }

    @Override
    public void handleEulixSystemShutdownRequest() {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.systemShutdown();
            }
        });
    }

    @Override
    public void handleDiskInitializeHardwareFinishCallback() {
        ;
    }

    @Override
    public void handleDiskInitializeFinishCallback() {
        ;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.FIND_DEVICE_CODE:
                if (lanFindBoxBridge != null) {
                    lanFindBoxBridge.unregisterSourceCallback();
                }
                prepareFind = false;
                handlerPost(() -> {
                    bindDeviceMantle.setVisibility(View.GONE);
                    if (resultCode == Activity.RESULT_OK) {
                        distributeNetwork();
                    } else {
                        countDownFinish();
                    }
                });
//                if (mHandler != null) {
//                    mHandler.post(() -> {
//                        bindDeviceMantle.setVisibility(View.GONE);
//                        if (resultCode == Activity.RESULT_OK) {
//                            distributeNetwork();
//                        } else {
//                            countDownFinish();
//                        }
//                    });
//                }
                break;
            case ConstantField.RequestCode.WIFI_DISTRIBUTE_CODE:
                handleDistributeNetwork((resultCode == Activity.RESULT_OK));
                boolean isFinish = false;
                if (data != null) {
                    isFinish = data.getBooleanExtra("source_finish", false);
                }
                if (isFinish) {
                    finish();
                }
                break;
            default:
                break;
        }
    }
}
