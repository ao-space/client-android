package xyz.eulix.space.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DistributeWLAN;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.network.agent.PairingBoxInfo;

public class AODeviceDiscoveryManager {
    private static AODeviceDiscoveryManager instance;
    private static AODeviceDiscoverySourceCallback aoDeviceDiscoverySourceCallback;
    private static Map<String, AODeviceDiscoverySinkCallback> aoDeviceDiscoverySinkCallbackMap;
    private Map<String, String> requestActivityIdMap = new HashMap<>();
    private Map<Integer, String> bluetoothSequenceNumberRequestIdMap = new HashMap<>();
    private Map<String, String> lanRequestUuidIdMap = new HashMap<>();
    public static final int STEP_SUSPEND = 0;
    public static final int STEP_PUBLIC_KEY_EXCHANGE = STEP_SUSPEND + 1;
    public static final int STEP_KEY_EXCHANGE = STEP_PUBLIC_KEY_EXCHANGE + 1;
    public static final int STEP_PAIR_INIT = STEP_KEY_EXCHANGE + 1;
    public static final int STEP_WIFI_LIST = STEP_PAIR_INIT + 1;
    public static final int STEP_SET_WIFI = STEP_WIFI_LIST + 1;
    public static final int STEP_PAIRING = STEP_SET_WIFI + 1;
    public static final int STEP_REVOKE = STEP_PAIRING + 1;
    public static final int STEP_SET_PASSWORD = STEP_REVOKE + 1;
    public static final int STEP_INITIAL = STEP_SET_PASSWORD + 1;
    public static final int STEP_SPACE_READY_CHECK = STEP_INITIAL + 1;
    public static final int STEP_DISK_RECOGNITION = STEP_SPACE_READY_CHECK + 1;
    public static final int STEP_DISK_INITIALIZE = STEP_DISK_RECOGNITION + 1;
    public static final int STEP_DISK_INITIALIZE_PROGRESS = STEP_DISK_INITIALIZE + 1;
    public static final int STEP_DISK_MANAGEMENT_LIST = STEP_DISK_INITIALIZE_PROGRESS + 1;
    public static final int STEP_AO_SYSTEM_SHUTDOWN = STEP_DISK_MANAGEMENT_LIST + 1;
    public static final int STEP_AO_SYSTEM_REBOOT = STEP_AO_SYSTEM_SHUTDOWN + 1;
    public static final int STEP_GET_NETWORK_CONFIG = STEP_AO_SYSTEM_REBOOT + 1;
    public static final int STEP_SET_NETWORK_CONFIG = STEP_GET_NETWORK_CONFIG + 1;
    public static final int STEP_IGNORE_NETWORK = STEP_SET_NETWORK_CONFIG + 1;
    public static final int STEP_SWITCH_PLATFORM = STEP_IGNORE_NETWORK + 1;
    public static final int STEP_SWITCH_STATUS = STEP_SWITCH_PLATFORM + 1;
    public static final int STEP_BIND_COM_START = STEP_SWITCH_STATUS + 1;
    public static final int STEP_BIND_COM_PROGRESS = STEP_BIND_COM_START + 1;
    public static final int STEP_BIND_SPACE_CREATE = STEP_BIND_COM_PROGRESS + 1;
    public static final int STEP_BIND_REVOKE = STEP_BIND_SPACE_CREATE + 1;
    public static final int STEP_NEW_DEVICE_APPLY_RESET_PASSWORD = STEP_BIND_REVOKE + 1;
    public static final int STEP_SECURITY_MESSAGE_POLL = STEP_NEW_DEVICE_APPLY_RESET_PASSWORD + 1;
    public static final int STEP_SECURITY_EMAIL_SETTING = STEP_SECURITY_MESSAGE_POLL + 1;
    public static final int STEP_VERIFY_SECURITY_EMAIL = STEP_SECURITY_EMAIL_SETTING + 1;
    public static final int STEP_NEW_DEVICE_RESET_PASSWORD = STEP_VERIFY_SECURITY_EMAIL + 1;
    private InitResponse initResponse;
    private String boxPublicKey;
    private String deviceName;
    private String bluetoothAddress;
    private String bluetoothId;
    private DistributeWLAN distributeWLAN;
    private String adminName;
    private String adminPassword;
    private PairingBoxInfo pairingBoxInfo;
    private String platformApiBase;

    private AODeviceDiscoveryManager() {}

    private interface AODeviceDiscoveryBaseCallback {
        void onFinish();
    }

    public interface AODeviceDiscoverySourceCallback extends AODeviceDiscoveryBaseCallback {
        boolean onRequest(int step, String bodyJson, String requestId);
    }

    public interface AODeviceDiscoverySinkCallback extends AODeviceDiscoveryBaseCallback {
        void onResponse(int code, String source, int step, String bodyJson);
    }

    private static synchronized void initSinkCallbackMap() {
        if (aoDeviceDiscoverySinkCallbackMap == null) {
            aoDeviceDiscoverySinkCallbackMap = new ConcurrentHashMap<>();
        }
    }

    private static synchronized void resetSinkCallbackMap() {
        if (aoDeviceDiscoverySinkCallbackMap != null) {
            aoDeviceDiscoverySinkCallbackMap.clear();
            aoDeviceDiscoverySinkCallbackMap = null;
        }
    }

    public synchronized static AODeviceDiscoveryManager getInstance() {
        if (instance == null) {
            instance = new AODeviceDiscoveryManager();
        }
        return instance;
    }

    public void resetInstance() {
        resetSinkCallbackMap();
        unregisterCallback();
        instance = null;
    }

    public void registerCallback(AODeviceDiscoverySourceCallback callback) {
        aoDeviceDiscoverySourceCallback = callback;
    }

    public void unregisterCallback() {
        aoDeviceDiscoverySourceCallback = null;
    }

    public void registerCallback(String id, AODeviceDiscoverySinkCallback callback) {
        if (id != null && callback != null) {
            initSinkCallbackMap();
            aoDeviceDiscoverySinkCallbackMap.put(id, callback);
        }
    }

    public void unregisterCallback(String id) {
        if (id != null && aoDeviceDiscoverySinkCallbackMap != null) {
            aoDeviceDiscoverySinkCallbackMap.remove(id);
        }
    }

    public void finishSource() {
        if (aoDeviceDiscoverySourceCallback != null) {
            aoDeviceDiscoverySourceCallback.onFinish();
        }
    }

    public void finishSink(List<String> activityIds) {
        if (activityIds != null && !activityIds.isEmpty()) {
            for (String activityId : activityIds) {
                if (activityId != null && aoDeviceDiscoverySinkCallbackMap != null && aoDeviceDiscoverySinkCallbackMap.containsKey(activityId)) {
                    AODeviceDiscoverySinkCallback callback = aoDeviceDiscoverySinkCallbackMap.get(activityId);
                    if (callback != null) {
                        callback.onFinish();
                    }
                }
            }
        }
    }

    public void finishAllSink() {
        if (aoDeviceDiscoverySinkCallbackMap != null) {
            Collection<AODeviceDiscoverySinkCallback> callbacks = aoDeviceDiscoverySinkCallbackMap.values();
            for (AODeviceDiscoverySinkCallback callback : callbacks) {
                if (callback != null) {
                    callback.onFinish();
                }
            }
        }
    }

    public void finishAll() {
        finishAllSink();
        finishSource();
    }

    public boolean request(String activityId, String requestId, int step, String bodyJson) {
        boolean isRequest = false;
        if (activityId != null && requestId != null) {
            requestActivityIdMap.put(requestId, activityId);
        }
        if (aoDeviceDiscoverySourceCallback != null) {
            isRequest = aoDeviceDiscoverySourceCallback.onRequest(step, bodyJson, requestId);
        }
        if (!isRequest) {
            requestActivityIdMap.remove(requestId);
        }
        return isRequest;
    }

    public void process(AODeviceDiscoveryBean bean, String requestId) {
        if (bean != null) {
            switch (bean.type) {
                case AODeviceDiscoveryBean.TYPE_BLUETOOTH:
                    Integer sequenceNumber = bean.bluetoothSequenceNumber;
                    if (sequenceNumber != null) {
                        bluetoothSequenceNumberRequestIdMap.put(sequenceNumber, requestId);
                    }
                    break;
                case AODeviceDiscoveryBean.TYPE_LAN:
                    String requestUuid = bean.lanRequestUuid;
                    if (requestUuid != null) {
                        lanRequestUuidIdMap.put(requestUuid, requestId);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void response(AODeviceDiscoveryBean bean, int code, String source, int step, String bodyJson) {
        String requestId = null;
        if (bean != null) {
            switch (bean.type) {
                case AODeviceDiscoveryBean.TYPE_BLUETOOTH:
                    Integer sequenceNumber = bean.bluetoothSequenceNumber;
                    if (sequenceNumber != null && bluetoothSequenceNumberRequestIdMap.containsKey(sequenceNumber)) {
                        requestId = bluetoothSequenceNumberRequestIdMap.get(sequenceNumber);
                        bluetoothSequenceNumberRequestIdMap.remove(sequenceNumber);
                    }
                    break;
                case AODeviceDiscoveryBean.TYPE_LAN:
                    String requestUuid = bean.lanRequestUuid;
                    if (requestUuid != null && lanRequestUuidIdMap.containsKey(requestUuid)) {
                        requestId = lanRequestUuidIdMap.get(requestUuid);
                        lanRequestUuidIdMap.remove(requestUuid);
                    }
                    break;
                default:
                    break;
            }
        }
        String activityId = null;
        if (requestId != null && requestActivityIdMap.containsKey(requestId)) {
            activityId = requestActivityIdMap.get(requestId);
            requestActivityIdMap.remove(requestId);
        }
        if (activityId != null && aoDeviceDiscoverySinkCallbackMap != null && aoDeviceDiscoverySinkCallbackMap.containsKey(activityId)) {
            AODeviceDiscoverySinkCallback callback = aoDeviceDiscoverySinkCallbackMap.get(activityId);
            if (callback != null) {
                callback.onResponse(code, source, step, bodyJson);
            }
        }
    }

    public boolean isUnpairedBox() {
        boolean isUnpaired = false;
        if (initResponse != null) {
            isUnpaired = (initResponse.getPaired() == 1);
        }
        return isUnpaired;
    }

    public boolean isNewBindProcessSupport() {
        boolean newBindProcessSupport = false;
        if (initResponse != null) {
            newBindProcessSupport = initResponse.isNewBindProcessSupport();
        }
        return newBindProcessSupport;
    }

    public String getBoxUuid() {
        String boxUuid = null;
        if (initResponse != null) {
            boxUuid = initResponse.getBoxUuid();
        }
        return boxUuid;
    }

    public DeviceAbility getDeviceAbility() {
        DeviceAbility deviceAbility = null;
        if (initResponse != null) {
            deviceAbility = initResponse.getDeviceAbility();
        }
        return deviceAbility;
    }

    public boolean isInnerDiskSupport() {
        boolean isInnerDiskSupport = false;
        DeviceAbility deviceAbility = null;
        if (initResponse != null) {
            deviceAbility = initResponse.getDeviceAbility();
        }
        DeviceAbility nDeviceAbility = DeviceAbility.generateDefault(deviceAbility);
        if (nDeviceAbility != null) {
            Boolean isInnerDiskSupportValue = nDeviceAbility.getInnerDiskSupport();
            if (isInnerDiskSupportValue != null) {
                isInnerDiskSupport = isInnerDiskSupportValue;
            }
        }
        return isInnerDiskSupport;
    }

    public InitResponse getInitResponse() {
        return initResponse;
    }

    public void setInitResponse(InitResponse initResponse) {
        this.initResponse = initResponse;
    }

    public String getBoxPublicKey() {
        return boxPublicKey;
    }

    public void setBoxPublicKey(String boxPublicKey) {
        this.boxPublicKey = boxPublicKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getBluetoothId() {
        return bluetoothId;
    }

    public void setBluetoothId(String bluetoothId) {
        this.bluetoothId = bluetoothId;
    }

    public String getPlatformApiBase() {
        return platformApiBase;
    }

    public void setPlatformApiBase(String platformApiBase) {
        this.platformApiBase = platformApiBase;
    }

    public DistributeWLAN getDistributeWLAN() {
        return distributeWLAN;
    }

    public void setDistributeWLAN(DistributeWLAN distributeWLAN) {
        this.distributeWLAN = distributeWLAN;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public PairingBoxInfo getPairingBoxInfo() {
        return pairingBoxInfo;
    }

    public void setPairingBoxInfo(PairingBoxInfo pairingBoxInfo) {
        this.pairingBoxInfo = pairingBoxInfo;
    }

    public static class AODeviceDiscoveryBean {
        public static final int TYPE_NONE = 0;
        public static final int TYPE_BLUETOOTH = TYPE_NONE + 1;
        public static final int TYPE_LAN = TYPE_BLUETOOTH + 1;
        private int type;
        private Integer bluetoothSequenceNumber;
        private String lanRequestUuid;

        public AODeviceDiscoveryBean(Integer bluetoothSequenceNumber) {
            this.bluetoothSequenceNumber = bluetoothSequenceNumber;
            type = TYPE_BLUETOOTH;
        }

        public AODeviceDiscoveryBean(String lanRequestUuid) {
            this.lanRequestUuid = lanRequestUuid;
            type = TYPE_LAN;
        }
    }
}
