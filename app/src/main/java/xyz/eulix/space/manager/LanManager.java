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

package xyz.eulix.space.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdServiceInfo;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceLanService;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.LanDeviceInfoBean;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.callback.EulixSpaceLanCallback;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.agent.DeviceUtil;
import xyz.eulix.space.network.agent.LocalIpInfoCallback;
import xyz.eulix.space.network.gateway.GatewayManager;
import xyz.eulix.space.network.gateway.ISpaceStatusCallback;
import xyz.eulix.space.network.gateway.SpaceStatusResult;
import xyz.eulix.space.transfer.multipart.MultipartUtil;
import xyz.eulix.space.transfer.multipart.lan.LanHttpsUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description: 局域网访问管理类
 * History:     2021/12/24
 */
public class LanManager {
    private static LanManager sInstance;
    private volatile boolean isLanEnable = false;
    //局域网http domain
    private String lanDomain;
    private Context context;
    private final long NORMAL_POLLING_INTERVAL_TIME = 30 * 1000;    //常规轮询间隔 ms
    private final long LONG_POLLING_INTERVAL_TIME = 60 * 1000;  //长轮询间隔
    private long mPollingInterval = NORMAL_POLLING_INTERVAL_TIME;
    private HashMap<String, Integer> ipFailTimeMap = new HashMap<>();

    //局域网自签名证书
    private volatile X509Certificate lanCert;
    //局域网https domain
    private String lanHttpsDomain;
    //https
    private OkHttpClient httpsClient;
    //局域网查询失败次数
    private int mCheckFailedTime = 0;
    //局域网查询最大次数，超过则认为该WiFi下不可使用局域网
    private final static int LAN_CHECK_MAX_COUNT = 5;
    //https通道开关
    private boolean mHttpsSwitch = true;

    private ExecutorService mPollCheckExecutor;
    private PoolCheckRunnable mPollCheckRunnable;

    private volatile LanSearchServiceConnection lanSearchServiceConnection;

    private CountDownTimer countDownTimer;
    private static final int SECOND_UNIT = 1000;
    private static final int timeSecond = 20;

    public static synchronized LanManager getInstance() {
        if (sInstance == null) {
            sInstance = new LanManager();
            sInstance.context = EulixSpaceApplication.getContext();
        }
        return sInstance;
    }

    public boolean isLanEnable() {
        return isLanEnable;
    }

    public void setLanEnable(boolean isLanEnable) {
        setLanEnableCore(isLanEnable, true);
    }

    private void setLanEnableCore(boolean isLanEnable, boolean isStopPoll) {
        Logger.d("zfy", "#setLanEnableCore isLanEnable=" + isLanEnable + ",isStopPoll=" + isStopPoll);
        this.isLanEnable = isLanEnable;
        if (!isLanEnable) {
            //非局域网
            this.lanDomain = null;
            this.lanHttpsDomain = null;
            mCheckFailedTime++;
            Logger.d("zfy", "check lan failed time:" + mCheckFailedTime);
            if (mCheckFailedTime >= LAN_CHECK_MAX_COUNT) {
                Logger.d("zfy", "lan check failed time reach limit, turn long interval time");
                mPollingInterval = LONG_POLLING_INTERVAL_TIME;
            }
            if (isStopPoll) {
                Logger.d("zfy", "stop poll");
                stopPollTask();
                mCheckFailedTime = 0;
                mPollingInterval = NORMAL_POLLING_INTERVAL_TIME;
            }
            ipFailTimeMap.clear();
            lanCert = null;
            httpsClient = null;
            MultipartUtil.LIMIT_COUNT_UPLOAD = 2;
            MultipartUtil.LIMIT_COUNT_DOWNLOAD = 2;
            mHttpsSwitch = true;
            if (NetUtils.isWifiConnected(context)) {
//                PreferenceUtil.saveLanDomain(context, null);
            }
        } else {
            //局域网
            MultipartUtil.LIMIT_COUNT_UPLOAD = 4;
            MultipartUtil.LIMIT_COUNT_DOWNLOAD = 4;
            mCheckFailedTime = 0;
            mPollingInterval = NORMAL_POLLING_INTERVAL_TIME;
            PreferenceUtil.saveLanDomain(context, lanDomain);
        }
    }

    public void refreshLanState(ResultCallback callback) {
        refreshLanState(callback, false);
    }

    //刷新局域网状态
    public void refreshLanState(ResultCallback callback, boolean isFore) {
        Logger.d("zfy", "refreshLanState");
        if (!NetUtils.isWifiConnected(context)) {
            setLanEnableCore(false, false);
            callback.onResult(false, "client not wifi");
            return;
        }

        getLocalIp(null, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                if (result && extraObj != null) {
                    List<InitResponseNetwork> ipList = (List<InitResponseNetwork>) extraObj;
                    checkAfterGetIpList(ipList, callback);
                } else {
                    setLanEnableCore(false, false);
                    if (callback != null) {
                        callback.onResult(false, "");
                    }
                }
            }

            @Override
            public void onError(String msg) {
                setLanEnableCore(false, false);
                if (callback != null) {
                    callback.onResult(false, "");
                }
            }
        });
    }

    //查询ip地址是否可用
    private void checkIpListConnect(List<InitResponseNetwork> ipList, final int index, ResultCallback callback) {
        Logger.d("zfy", "checkIpListConnect index=" + index);
        if (!NetUtils.isWifiConnected(context) || ipList == null || ipList.isEmpty() || index >= ipList.size()) {
            callback.onResult(false, null);
            Logger.d("zfy", "no available lan");
            return;
        }
        String baseDomain = genDomainUrl(ipList.get(index));
        InitResponseNetwork finalIpItem = ipList.get(index);
        if (ipFailTimeMap.containsKey(finalIpItem.getIp()) && ipFailTimeMap.get(finalIpItem.getIp()) > 3) {
            Logger.d("zfy", finalIpItem.getIp() + " failed too many times,try next");
            int nextIndex = index + 1;
            checkIpListConnect(ipList, nextIndex, callback);
            return;
        }
        checkConnectStates(baseDomain, new ResultCallback() {
            @Override
            public void onResult(boolean result, String extraMsg) {
                if (result) {
//                    isLanEnable = true;
                    lanDomain = genDomainUrl(finalIpItem);
                    lanHttpsDomain = genHttpsDomainUrl(finalIpItem);
                    Logger.d("zfy", "get available lan ip:" + lanDomain);
                    if (!isLanEnable) {
                        lanDomain = genDomainUrl(finalIpItem);
                        lanHttpsDomain = genHttpsDomainUrl(finalIpItem);
                        setLanEnableCore(true, false);
                        Logger.d("zfy", "get available lan ip:" + lanDomain);
                        //获取局域网证书
                        if (mHttpsSwitch) {
                            LanHttpsUtil.getCert(context, new ResultCallbackObj() {
                                @Override
                                public void onResult(boolean result, Object extraObj) {
                                    if (result && extraObj != null) {
                                        Logger.d("zfy", "get https cert success");
                                        lanCert = (X509Certificate) extraObj;
                                    }
                                }

                                @Override
                                public void onError(String msg) {

                                }
                            });
                        }
                    }
                    callback.onResult(true, null);
                } else {
                    if (ipFailTimeMap.containsKey(finalIpItem.getIp())) {
                        ipFailTimeMap.put(finalIpItem.getIp(), ipFailTimeMap.get(finalIpItem.getIp()) + 1);
                    } else {
                        ipFailTimeMap.put(finalIpItem.getIp(), 1);
                    }
                    if (index < ipList.size() - 1) {
                        Logger.d("zfy", "no available lan");
                        return;
                    }
                    Logger.d("zfy", finalIpItem.getIp() + " not available,try next");
                    int nextIndex = index + 1;
                    checkIpListConnect(ipList, nextIndex, callback);
                }
            }
        });
    }

    //获取局域网ip
    private void getLocalIp(String ipDomain, ResultCallbackObj callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String ipDomainFinal;
            if (!TextUtils.isEmpty(ipDomain)) {
                ipDomainFinal = ipDomain;
            } else {
                ipDomainFinal = gatewayCommunicationBase.getBoxDomain();
            }
            ThreadPool.getInstance().execute(() -> DeviceUtil.getLocalIps(ipDomainFinal,
                    gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(),
                    gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams(),
                    new LocalIpInfoCallback() {
                        @Override
                        public void onSuccess(String code, String message, List<InitResponseNetwork> ipList) {
                            if (callback != null) {
                                callback.onResult(ipList != null, ipList);
                            }
                        }

                        @Override
                        public void onFailed() {
                            setLanEnableCore(false, false);
                            callback.onResult(false, null);
                        }

                        @Override
                        public void onError(String errMsg) {
                            setLanEnableCore(false, false);
                            callback.onResult(false, errMsg);
                        }
                    }));
        }
    }

    private void checkAfterGetIpList(List<InitResponseNetwork> ipList, ResultCallback callback) {
        if (ipList != null) {
            //过滤ip为空数据
            for (int i = 0; i < ipList.size(); i++) {
                if (TextUtils.isEmpty(ipList.get(i).getIp())) {
                    ipList.remove(i);
                    i--;
                }
            }

            if (ipList.isEmpty()) {
                Logger.d("zfy", "has no local ips");
                setLanEnableCore(false, false);
                callback.onResult(false, null);
                return;
            }

            //优先使用有线ip
            InitResponseNetwork wireIpItem = null;
            for (InitResponseNetwork ipItem : ipList) {
                if (ipItem.isWire()) {
                    wireIpItem = ipItem;
                    break;
                }
            }
            if (wireIpItem != null) {
                String baseDomain = genDomainUrl(wireIpItem);
                InitResponseNetwork finalWireIpItem = wireIpItem;
                if (ipFailTimeMap.containsKey(finalWireIpItem.getIp()) && ipFailTimeMap.get(finalWireIpItem.getIp()) > 3) {
                    Logger.d("zfy", finalWireIpItem.getIp() + " failed too many times,try wireless");
                    ipList.remove(finalWireIpItem);
                    checkIpListConnect(ipList, 0, callback);
                    return;
                }
                checkConnectStates(baseDomain, new ResultCallback() {
                    @Override
                    public void onResult(boolean result, String extraMsg) {
                        if (result) {
                            //局域网可用
                            if (!isLanEnable) {
                                lanDomain = genDomainUrl(finalWireIpItem);
                                lanHttpsDomain = genHttpsDomainUrl(finalWireIpItem);
                                setLanEnableCore(true, false);
                                Logger.d("zfy", "get available lan ip:" + lanDomain);
                                if (mHttpsSwitch) {
                                    //获取局域网证书
                                    LanHttpsUtil.getCert(context, new ResultCallbackObj() {
                                        @Override
                                        public void onResult(boolean result, Object extraObj) {
                                            if (result && extraObj != null) {
                                                Logger.d("zfy", "get https cert success");
                                                lanCert = (X509Certificate) extraObj;
                                            } else {
                                                Logger.d("zfy", "get https cert failed");
                                            }
                                        }

                                        @Override
                                        public void onError(String msg) {
                                            Logger.d("zfy", "get https cert error");
                                        }
                                    });
                                }
                            }
                            callback.onResult(true, null);
                        } else {
                            Logger.d("zfy", "wire not available, try others");
                            if (ipFailTimeMap.containsKey(finalWireIpItem.getIp())) {
                                ipFailTimeMap.put(finalWireIpItem.getIp(), ipFailTimeMap.get(finalWireIpItem.getIp()) + 1);
                            } else {
                                ipFailTimeMap.put(finalWireIpItem.getIp(), 1);
                            }
                            ipList.remove(finalWireIpItem);
                            checkIpListConnect(ipList, 0, callback);
                        }
                    }
                });
            } else {
                Logger.d("zfy", "no wire lan,check wireless list");
                checkIpListConnect(ipList, 0, callback);
            }
        } else {
            Logger.d("zfy", "has no local ips");
            setLanEnableCore(false, false);
            callback.onResult(false, null);
        }
    }

    //生成局域网domain
    private String genDomainUrl(InitResponseNetwork networkItem) {
        if (networkItem == null) {
            return null;
        }
        String ipAddress = networkItem.getIp() + ":" + networkItem.getPort();
        if (!ipAddress.startsWith("http:")) {
            ipAddress = "http://" + ipAddress;
        }
        if (!ipAddress.endsWith("/")) {
            ipAddress = ipAddress + "/";
        }
        return ipAddress;
    }

    //生成局域网https domain
    private String genHttpsDomainUrl(InitResponseNetwork networkItem) {
        if (networkItem == null) {
            return null;
        }
        String ipAddress = networkItem.getIp() + ":" + ((networkItem.getTlsPort() > 0) ? networkItem.getTlsPort() : 443) + "/";
        if (!ipAddress.startsWith("https:")) {
            ipAddress = "https://" + ipAddress;
        }
        return ipAddress;
    }

    private void checkConnectStates(final String finalBaseUrl, ResultCallback callback) {
        GatewayManager gatewayManager = new GatewayManager(finalBaseUrl);
        ThreadPool.getInstance().execute(() -> gatewayManager.getSpaceStatus(new ISpaceStatusCallback() {
            @Override
            public void onResult(SpaceStatusResult result) {
                Logger.d("zfy", "checkConnectStates on result: " + result);
                if (result != null) {
                    callback.onResult(true, null);
                } else {
                    callback.onResult(false, null);
                }
            }

            @Override
            public void onError(String errMsg) {
                Logger.d("zfy", "checkConnectStates on error: " + errMsg);
                callback.onResult(false, null);
            }
        }));
    }


    //通过在局域网内发现设备的方式刷新局域网状态-不走平台
    public void refreshLanStateBySearchDevice(ResultCallback callback) {
        Logger.d("zfy", "searchLanDevice");
        if (!NetUtils.isWifiConnected(context)) {
            setLanEnableCore(false, false);
            if (callback != null) {
                callback.onResult(false, "client not wifi");
            }
            return;
        }

        EulixBoxInfo boxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (boxInfo == null) {
            return;
        }
        String currentBtidhash = FormatUtil.getSHA256String(("eulixspace-" + boxInfo.getBluetoothId()));

        if (lanSearchServiceConnection == null) {
            lanSearchServiceConnection = new LanSearchServiceConnection(context, new LanCallback() {
                @Override
                public void onResult(boolean result, LanDeviceInfoBean deviceInfo, LanSearchServiceConnection connection) {
                    if (deviceInfo != null && deviceInfo.btidhash != null && currentBtidhash.startsWith(deviceInfo.btidhash)) {
                        Logger.d("zfy", "find lan device with same btidhash, ip=" + deviceInfo.ipAddress);
                        if (connection != null) {
                            connection.stopDiscovery();
                        }

                        String ipDomain = "http://" + deviceInfo.ipAddress + ":" + (deviceInfo.webport == null ? "80" : deviceInfo.webport);

                        //获取局域网ip列表
                        getLocalIp(ipDomain, new ResultCallbackObj() {
                            @Override
                            public void onResult(boolean result, Object extraObj) {
                                if (result && extraObj != null) {
                                    List<InitResponseNetwork> ipList = (List<InitResponseNetwork>) extraObj;
                                    checkAfterGetIpList(ipList, callback);
                                } else {
                                    setLanEnableCore(false, false);
                                    if (callback != null) {
                                        callback.onResult(false, "");
                                    }
                                }
                            }

                            @Override
                            public void onError(String msg) {
                                setLanEnableCore(false, false);
                                if (callback != null) {
                                    callback.onResult(false, "");
                                }
                            }
                        });
                    }
                }

                @Override
                public void onOverTimeEnd() {
                    if (!isLanEnable) {
                        //超时回调失败
                        if (callback != null) {
                            callback.onResult(false, "");
                        }
                    }
                }
            });
            context.bindService(new Intent(context, EulixSpaceLanService.class), lanSearchServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            lanSearchServiceConnection.stopDiscovery();
            lanSearchServiceConnection.startDiscovery();
        }
    }

    //局域网设备发现connection
    private class LanSearchServiceConnection implements ServiceConnection, EulixSpaceLanCallback {
        EulixSpaceLanService.EulixSpaceLanBinder eulixSpaceLanBinder;
        LanCallback callback;
        Context context;

        public LanSearchServiceConnection(Context context, LanCallback callbackObj) {
            this.callback = callbackObj;
            this.context = context;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof EulixSpaceLanService.EulixSpaceLanBinder) {
                eulixSpaceLanBinder = (EulixSpaceLanService.EulixSpaceLanBinder) service;
                eulixSpaceLanBinder.registerCallback(LanSearchServiceConnection.this);
                eulixSpaceLanBinder.discoverService(ConstantField.ServiceType.EULIXSPACE_SD_TCP);
                startCountdown(callback);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //绑定服务失败
            eulixSpaceLanBinder = null;
        }

        public void startDiscovery() {
            if (eulixSpaceLanBinder != null) {
                eulixSpaceLanBinder.discoverService(ConstantField.ServiceType.EULIXSPACE_SD_TCP);
                startCountdown(callback);
            }
        }

        public void stopDiscovery() {
            if (eulixSpaceLanBinder != null) {
                eulixSpaceLanBinder.stopServiceDiscovery();
            }
        }

        @Override
        public void discoveryChange(int number) {

        }

        @Override
        public void resolveDevice(NsdServiceInfo serviceInfo) {
            //发现设备
            Logger.d("zfy", "#resolveDevice serviceName=" + serviceInfo.getServiceName() + ",host=" + serviceInfo.getHost());
            InetAddress inetAddress = serviceInfo.getHost();
            if (inetAddress instanceof Inet6Address) {
                //过滤IPv6
                return;
            }

            LanDeviceInfoBean deviceItem = new LanDeviceInfoBean();
            deviceItem.ipAddress = inetAddress.getHostAddress();
            Map<String, byte[]> attributes = serviceInfo.getAttributes();
            if (attributes != null) {
                Set<Map.Entry<String, byte[]>> entrySet = attributes.entrySet();
                for (Map.Entry<String, byte[]> entry : entrySet) {
                    if (entry != null) {
                        Logger.d("zfy", "key: " + entry.getKey());
                        if (entry.getValue() != null) {
                            Logger.d("zfy", "value: " + new String(entry.getValue(), StandardCharsets.UTF_8));
                        }
                        String key = entry.getKey();
                        if (key.equals("btidhash") && entry.getValue() != null) {
                            deviceItem.btidhash = new String(entry.getValue(), StandardCharsets.UTF_8);
                        } else if (key.equals("devicemodel") && entry.getValue() != null) {
                            deviceItem.devicemodel = new String(entry.getValue(), StandardCharsets.UTF_8);
                        } else if (key.equals("webport") && entry.getValue() != null) {
                            deviceItem.webport = new String(entry.getValue(), StandardCharsets.UTF_8);
                        } else if (key.equals("sslport") && entry.getValue() != null) {
                            deviceItem.sslport = new String(entry.getValue(), StandardCharsets.UTF_8);
                        }
                    }
                }
            }

            //判断设备信息，刷新列表
            if (deviceItem.btidhash != null && callback != null) {
                callback.onResult(true, deviceItem, this);
            }
        }

    }

    private void startCountdown(LanCallback callback) {
        new Handler(Looper.getMainLooper()).post(() -> {
            stopCountdown();
            countDownTimer = new CountDownTimer(((long) timeSecond * SECOND_UNIT), SECOND_UNIT) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    if (lanSearchServiceConnection != null) {
                        lanSearchServiceConnection.stopDiscovery();
                    }
                    callback.onOverTimeEnd();
                }
            };
            countDownTimer.start();
        });
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public interface LanCallback {
        void onResult(boolean result, LanDeviceInfoBean deviceInfo, LanSearchServiceConnection connection);

        void onOverTimeEnd();
    }


    //获取局域网IP地址+端口号
    public String getIpAddress() {
        return lanDomain;
    }

    //获取局域网IP地址+端口号
    public String getLanHttpsDomain() {
        return lanHttpsDomain;
    }


    //Https通道是否可用
    public boolean isHttpsAvailable() {
        return mHttpsSwitch && lanCert != null && lanHttpsDomain != null;
    }

    //关闭HTTPS通道
    public void closeHttpsChannel() {
        this.lanCert = null;
        LanHttpsUtil.clearCacheCert(context);
        this.lanHttpsDomain = null;
        this.mHttpsSwitch = false;
    }

    //获取用于https访问的client
    public OkHttpClient getHttpsClient() {
        if (httpsClient == null) {
            httpsClient = LanHttpsUtil.createTrustCustomOkHttpClient(lanCert);
        }
        return httpsClient;
    }

    public void resetHttpsCertInfo() {
        lanCert = null;
        httpsClient = null;
        LanHttpsUtil.clearCacheCert(context);
        //获取局域网证书
        LanHttpsUtil.getCert(context, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                if (result && extraObj != null) {
                    Logger.d("zfy", "get https cert success");
                    lanCert = (X509Certificate) extraObj;
                }
            }

            @Override
            public void onError(String msg) {

            }
        });
    }

    //启动轮询
    public void startPollCheckTask() {
        Logger.d("zfy", "#startPollCheckTask");
        stopPollTask();

        if (!NetUtils.isWifiConnected(context)) {
            Logger.d("zfy", "network is not wifi,stop poll");
            setLanEnableCore(false, true);
            return;
        }

        mPollCheckRunnable = new PoolCheckRunnable();
        mPollCheckExecutor = Executors.newSingleThreadExecutor();
        mPollCheckExecutor.execute(mPollCheckRunnable);
    }

    public class PoolCheckRunnable implements Runnable {

        public boolean isStop = false;

        @Override
        public void run() {
            while (!isStop) {
                Logger.d("zfy", "runnable poll check");
                //应用在后台，且当前没有正在传输的任务，不进行局域网状态查询
                if (!((EulixSpaceApplication) EulixSpaceApplication.getContext()).getIsAppForeground()
                        && (TransferTaskManager.getInstance().getTransferringCount() <= 0)) {
                    Logger.d("zfy", "app is background and no transfer task, jump lan poll check");
                } else {
                    checkLanState();
                }
                try {
                    Thread.sleep(mPollingInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    //查询局域网状态
    public void checkLanState() {
        if (isLanEnable) {
            //当前已在局域网，仅检查ip可用性
            checkConnectStates(lanDomain, (result, extraMsg) -> {
                if (!result) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        boolean needSendEvent = false;
                        if (isLanEnable) {
                            needSendEvent = true;
                        }
                        setLanEnableCore(false, false);
                        if (needSendEvent) {
                            EventBusUtil.post(new LanStatusEvent(false));
                        }
                    });
                }
            });
        } else {
            //当前不在局域网
            String cacheDomain = PreferenceUtil.getLanDomain(context);
            if (!TextUtils.isEmpty(cacheDomain)) {
                checkConnectStates(cacheDomain, (result, extraMsg) -> {
                    if (result) {
                        getLocalIp(cacheDomain, new ResultCallbackObj() {
                            @Override
                            public void onResult(boolean result, Object extraObj) {
                                if (result && extraObj != null) {
                                    //通过域名获取ip列表成功
                                    List<InitResponseNetwork> ipList = (List<InitResponseNetwork>) extraObj;
                                    checkAfterGetIpList(ipList, new ResultCallback() {
                                        @Override
                                        public void onResult(boolean result, String extraMsg) {
                                            Logger.d("zfy", "refreshLanStateBySearchDevice onResult:" + result);
                                            setLanEnableCore(result, false);
                                            if (result) {
                                                EventBusUtil.post(new LanStatusEvent(true));
                                            }
                                        }
                                    });
                                } else {
                                    checkLanStateCore();
                                }
                            }

                            @Override
                            public void onError(String msg) {
                                checkLanStateCore();
                            }
                        });
                    } else {
                        checkLanStateCore();
                    }
                });
            } else {
                checkLanStateCore();
            }
        }
    }

    private void checkLanStateCore() {

        getLocalIp(null, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                if (result && extraObj != null) {
                    //通过域名获取ip列表成功
                    List<InitResponseNetwork> ipList = (List<InitResponseNetwork>) extraObj;
                    checkAfterGetIpList(ipList, new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            Logger.d("zfy", "refreshLanStateBySearchDevice onResult:" + result);
                            setLanEnableCore(result, false);
                            if (result) {
                                EventBusUtil.post(new LanStatusEvent(true));
                            }
                        }
                    });
                } else {
                    refreshLanStateBySearchDevice(new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            Logger.d("zfy", "refreshLanStateBySearchDevice onResult:" + result);
                            setLanEnableCore(result, false);
                            if (result) {
                                EventBusUtil.post(new LanStatusEvent(true));
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String msg) {
                refreshLanStateBySearchDevice(new ResultCallback() {
                    @Override
                    public void onResult(boolean result, String extraMsg) {
                        Logger.d("zfy", "refreshLanStateBySearchDevice onResult:" + result);
                        setLanEnableCore(result, false);
                        if (result) {
                            EventBusUtil.post(new LanStatusEvent(true));
                        }
                    }
                });
            }
        });
    }

    //停止轮询
    private void stopPollTask() {
        if (mPollCheckRunnable != null) {
            mPollCheckRunnable.isStop = true;
        }
        if (mPollCheckExecutor != null) {
            mPollCheckExecutor.shutdownNow();
        }
        mPollCheckRunnable = null;
        mPollCheckExecutor = null;
    }

    //设置局域网信息
    public void setLanDomainInfo(String lanIp, String port, String tlsPort) {
        if (TextUtils.isEmpty(lanIp)) {
            return;
        }
        this.lanDomain = "http://" + lanIp + (!TextUtils.isEmpty(port) ? ":" + port : "") + "/";

        //获取局域网证书
        lanHttpsDomain = "https://" + lanIp + (!TextUtils.isEmpty(tlsPort) ? ":" + tlsPort : ":443") + "/";
        setLanEnableCore(true, false);
        EventBusUtil.post(new LanStatusEvent(true));
        Logger.d("zfy", "get available lan ip:" + lanDomain);
        if (mHttpsSwitch) {
            //获取局域网证书
            LanHttpsUtil.getCert(context, new ResultCallbackObj() {
                @Override
                public void onResult(boolean result, Object extraObj) {
                    if (result && extraObj != null) {
                        Logger.d("zfy", "get https cert success");
                        lanCert = (X509Certificate) extraObj;
                    } else {
                        Logger.d("zfy", "get https cert failed");
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.d("zfy", "get https cert error");
                }
            });
        }
    }

}
