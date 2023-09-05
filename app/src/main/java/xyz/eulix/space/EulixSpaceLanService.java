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

package xyz.eulix.space;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import xyz.eulix.space.callback.EulixSpaceLanCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * date: 2021/6/22 16:52
 */
public class EulixSpaceLanService extends Service {
    private static final String TAG = EulixSpaceLanService.class.getSimpleName();
    private static final int DISCOVERY_DEVICE = 1;
    private static final int RESOLVE_DEVICE = DISCOVERY_DEVICE + 1;
    private static final int RESOLVE_DEVICE_RESULT = RESOLVE_DEVICE + 1;
    private EulixSpaceLanBinder mBinder = new EulixSpaceLanBinder();
    private EulixSpaceLanCallback mCallback;
    private NsdManager nsdManager;
    private Map<String, NsdServiceInfo> nsdServiceInfoMap;
    private boolean isDiscovery;
    private int resolveDeviceTask;
    private Queue<NsdServiceInfo> resolveDeviceQueue;
    private EulixSpaceLanHandler mHandler;

    private boolean needStartAfterStop = false;
    private String mStartAfterStopType;

    private NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Logger.w(TAG, "on registration failed, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                    + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort() + ", error code: " + errorCode);
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Logger.w(TAG, "on unregistration failed, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                    + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort() + ", error code: " + errorCode);
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Logger.d(TAG, "on service registered, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                    + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort());
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Logger.d(TAG, "on service unregistered, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                    + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort());
        }
    };

    private NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Logger.w(TAG, "on start discovery failed, service type: " + serviceType + ", error code: " + errorCode);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Logger.w(TAG, "on stop discovery failed, service type: " + serviceType + ", error code: " + errorCode);
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Logger.d(TAG, "on discovery started: " + serviceType);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Logger.d(TAG, "on discovery stopped: " + serviceType);
            if (needStartAfterStop) {
                startAfterStop();
            }
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            Logger.i(TAG, "on service found, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                    + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort());
            Map<String, byte[]> attributes = serviceInfo.getAttributes();
            if (attributes != null) {
                Set<Map.Entry<String, byte[]>> entrySet = attributes.entrySet();
                for (Map.Entry<String, byte[]> entry : entrySet) {
                    if (entry != null && entry.getValue() != null) {
                        Logger.d(TAG, "key: " + entry.getKey() + ", value: " + StringUtil.byteArrayToString(entry.getValue(), StandardCharsets.UTF_8));
                    }
                }
            }
            String serviceName = serviceInfo.getServiceName();
            String serviceType = serviceInfo.getServiceType();
            if (!TextUtils.isEmpty(serviceName) && mHandler != null) {
                Message message = mHandler.obtainMessage(DISCOVERY_DEVICE);
                Bundle data = new Bundle();
                data.putString(ConstantField.NSD_SERVICE_INFO.SERVICE_NAME, serviceName);
                data.putString(ConstantField.NSD_SERVICE_INFO.SERVICE_TYPE, (TextUtils.isEmpty(serviceType)
                        ? "_eulixspace-sd._tcp." : serviceType));
                message.setData(data);
                mHandler.sendMessage(message);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Logger.w(TAG, "on service lost, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                    + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort());
        }
    };

    static class EulixSpaceLanHandler extends Handler {
        private WeakReference<EulixSpaceLanService> eulixSpaceLanServiceWeakReference;

        public EulixSpaceLanHandler(EulixSpaceLanService service) {
            eulixSpaceLanServiceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixSpaceLanService service = eulixSpaceLanServiceWeakReference.get();
            if (service == null) {
                super.handleMessage(msg);
            } else {
                Bundle data = msg.peekData();
                switch (msg.what) {
                    case DISCOVERY_DEVICE:
                        if (data != null) {
                            service.handleDiscoveryDevice(data.getString(ConstantField.NSD_SERVICE_INFO.SERVICE_NAME, "")
                                    , data.getString(ConstantField.NSD_SERVICE_INFO.SERVICE_TYPE, ""));
                        }
                        break;
                    case RESOLVE_DEVICE:
                        if (data != null) {
                            service.handleResolveDevice(data.getString(ConstantField.NSD_SERVICE_INFO.SERVICE_NAME, "")
                                    , (msg.arg1 != 0), data.getString(ConstantField.NSD_SERVICE_INFO.SERIAL_NUMBER, "")
                                    , data.getString(ConstantField.NSD_SERVICE_INFO.HOST_ADDRESS, null), msg.arg2);
                        }
                        break;
                    case RESOLVE_DEVICE_RESULT:
                        service.resolveDeviceResult();
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    private boolean startDiscoveryService(String serviceType) {
        if (nsdManager != null && !isDiscovery) {
            if (isDiscovery){
                needStartAfterStop = true;
                mStartAfterStopType = serviceType;
                stopDiscoveryService();
            } else {
                try {
                    nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
                    isDiscovery = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return isDiscovery;
    }

    //结束成功后再开始
    private void startAfterStop(){
        try {
            nsdManager.discoverServices(mStartAfterStopType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            isDiscovery = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        isDiscovery = true;
    }

    private void stopDiscoveryService() {
        if (nsdManager != null && isDiscovery) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            isDiscovery = false;
            if (nsdServiceInfoMap != null) {
                nsdServiceInfoMap.clear();
            }
        }
    }

    private void handleDiscoveryDevice(String serviceName, String serviceType) {
        if (!TextUtils.isEmpty(serviceName)) {
            NsdServiceInfo resolveNsdServiceInfo = new NsdServiceInfo();
            resolveNsdServiceInfo.setServiceName(serviceName);
            resolveNsdServiceInfo.setServiceType(TextUtils.isEmpty(serviceType) ? "_eulixspace-sd._tcp." : serviceType);
            if (resolveDeviceTask > 0) {
                if (resolveDeviceQueue != null) {
                    try {
                        resolveDeviceQueue.offer(resolveNsdServiceInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                resolveDevice(resolveNsdServiceInfo);
            }
            resolveDeviceTask = Math.max(resolveDeviceTask, 0);
            resolveDeviceTask += 1;
        }
    }

    private void resolveDevice(NsdServiceInfo resolveNsdServiceInfo) {
        try {
            nsdManager.resolveService(resolveNsdServiceInfo, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Logger.w(TAG, "on resolve failed, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                            + ", host:" + serviceInfo.getHost() + ", port: " + serviceInfo.getPort() + ", error code: " + errorCode);
                    resolveDeviceRequest();
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    Logger.i(TAG, "on service resolved, name: " + serviceInfo.getServiceName() + ", type: " + serviceInfo.getServiceType()
                            + ", host: " + serviceInfo.getHost() + ", port: " + serviceInfo.getPort());
                    resolveDeviceRequest();
                    Map<String, byte[]> attributes = serviceInfo.getAttributes();
                    if (attributes != null) {
                        Set<Map.Entry<String, byte[]>> entrySet = attributes.entrySet();
                        for (Map.Entry<String, byte[]> entry : entrySet) {
                            if (entry != null) {
                                Logger.d(TAG, "key: " + entry.getKey());
                                if (entry.getValue() != null) {
                                    Logger.d(TAG, "value: " + new String(entry.getValue(), StandardCharsets.UTF_8));
                                }
                            }
                        }
                    }
                    InetAddress inetAddress = serviceInfo.getHost();
                    if (mCallback != null) {
                        mCallback.resolveDevice(serviceInfo);
                    }
                    if (inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address) {
                        if (mHandler != null) {
                            boolean isIpv6 = (inetAddress instanceof Inet6Address);
                            Message message = mHandler.obtainMessage(RESOLVE_DEVICE, isIpv6 ? 1 : 0, serviceInfo.getPort());
                            Bundle data = new Bundle();
                            data.putString(ConstantField.NSD_SERVICE_INFO.SERVICE_NAME, serviceInfo.getServiceName());
                            data.putString(ConstantField.NSD_SERVICE_INFO.SERIAL_NUMBER, (isIpv6 ? "[" : "")
                                    + inetAddress.getHostAddress() + (isIpv6 ? "]" : "") + ":" + serviceInfo.getPort());
                            data.putString(ConstantField.NSD_SERVICE_INFO.HOST_ADDRESS, inetAddress.getHostAddress());
                            message.setData(data);
                            mHandler.sendMessage(message);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resolveDeviceRequest() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(RESOLVE_DEVICE_RESULT);
        }
    }

    private void resolveDeviceResult() {
        resolveDeviceTask = Math.max((resolveDeviceTask - 1), 0);
        while (resolveDeviceQueue != null && resolveDeviceQueue.size() > 0) {
            NsdServiceInfo nsdServiceInfo = resolveDeviceQueue.poll();
            if (nsdServiceInfo != null) {
                resolveDevice(nsdServiceInfo);
                break;
            } else {
                resolveDeviceTask = Math.max((resolveDeviceTask - 1), 0);
            }
        }
    }

    private void handleResolveDevice(String serviceName, boolean isIpv6, String serialNumber, String hostAddress, int port) {
        if (hostAddress != null) {
            if (nsdServiceInfoMap == null) {
                nsdServiceInfoMap = new HashMap<>();
            }
        }
    }

    public class EulixSpaceLanBinder extends Binder {
        public void registerCallback(EulixSpaceLanCallback callback) {
            mCallback = callback;
        }

        public void unregisterCallback() {
            mCallback = null;
        }

        public boolean discoverService(String serviceType) {
            return startDiscoveryService(serviceType);
        }

        public void stopServiceDiscovery() {
            stopDiscoveryService();
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        resolveDeviceTask = 0;
        mHandler = new EulixSpaceLanHandler(this);
        resolveDeviceQueue = new ConcurrentLinkedQueue<>();
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        isDiscovery = false;
        NsdServiceInfo registerNsdServiceInfo = new NsdServiceInfo();
        registerNsdServiceInfo.setServiceName("r2VTgYrvX2ujIsAddnsHD7H3xMPHFp8q");
        registerNsdServiceInfo.setServiceType("_http.tcp");
        registerNsdServiceInfo.setPort(80);
    }

    @Override
    public void onDestroy() {
        stopDiscoveryService();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        resolveDeviceTask = 0;
        super.onDestroy();
    }
}
