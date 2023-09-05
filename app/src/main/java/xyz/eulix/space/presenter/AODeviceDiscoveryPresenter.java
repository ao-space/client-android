package xyz.eulix.space.presenter;

import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.bind.KeyExchangeReq;
import xyz.eulix.space.bean.bind.KeyExchangeRsp;
import xyz.eulix.space.bean.bind.PubKeyExchangeReq;
import xyz.eulix.space.bean.bind.PubKeyExchangeRsp;
import xyz.eulix.space.bean.bind.RvokInfo;
import xyz.eulix.space.bean.bind.WifiRequest;
import xyz.eulix.space.bean.bind.WpwdInfo;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.agent.AgentEncryptedResultsCallback;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.agent.DeviceUtil;
import xyz.eulix.space.network.agent.KeyExchangeCallbackV2;
import xyz.eulix.space.network.agent.NewDeviceApplyResetPasswordRequest;
import xyz.eulix.space.network.agent.NewDeviceResetPasswordRequest;
import xyz.eulix.space.network.agent.PairingClientInfo;
import xyz.eulix.space.network.agent.PasswordInfo;
import xyz.eulix.space.network.agent.PubKeyExchangeCallbackV2;
import xyz.eulix.space.network.agent.SecurityMessagePollRequest;
import xyz.eulix.space.network.agent.bind.BindRevokeRequest;
import xyz.eulix.space.network.agent.bind.BindUtil;
import xyz.eulix.space.network.agent.bind.SpaceCreateRequest;
import xyz.eulix.space.network.agent.disk.DiskInitializeRequest;
import xyz.eulix.space.network.agent.disk.DiskUtil;
import xyz.eulix.space.network.agent.net.EulixNetUtil;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.network.agent.platform.EulixPlatformUtil;
import xyz.eulix.space.network.agent.platform.SwitchPlatformRequest;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;

public class AODeviceDiscoveryPresenter extends AbsPresenter<AODeviceDiscoveryPresenter.IAODeviceDiscovery> {

    private String lanAddress;

    public interface IAODeviceDiscovery extends IBaseView {
        void publicKeyExchangeResponse(int code, String source, String message, PubKeyExchangeRsp rsp, String requestUuid);
        void keyExchangeResponse(int code, String source, String message, KeyExchangeRsp rsp, String requestUuid);
        void pairInitResponse(int code, String source, String message, String results, String requestUuid);
        void wifiListResponse(int code, String source, String message, String results, String requestUuid);
        void setWifiResponse(int code, String source, String message, String results, String requestUuid);
        void pairingResponse(int code, String source, String message, String results, String requestUuid);
        void revokeResponse(int code, String source, String message, String results, String requestUuid);
        void setPasswordResponse(int code, String source, String message, String results, String requestUuid);
        void initialResponse(int code, String source, String message, String results, String requestUuid);
        void spaceReadyCheckResponse(int code, String source, String message, String results, String requestUuid);
        void diskRecognitionResponse(int code, String source, String message, String results, String requestUuid);
        void diskInitializeResponse(int code, String source, String message, String requestUuid);
        void diskInitializeProgressResponse(int code, String source, String message, String results, String requestUuid);
        void diskManagementListResponse(int code, String source, String message, String results, String requestUuid);
        void aoSystemShutdownResponse(int code, String source, String message, String requestUuid);
        void aoSystemRebootResponse(int code, String source, String message, String requestUuid);
        void getNetworkConfigResponse(int code, String source, String message, String results, String requestUuid);
        void setNetworkConfigResponse(int code, String source, String message, String requestUuid);
        void ignoreNetworkResponse(int code, String source, String message, String requestUuid);
        void switchPlatformResponse(int code, String source, String message, String results, String requestUuid);
        void switchStatusResponse(int code, String source, String message, String results, String requestUuid);
        void bindCommunicationStartResponse(int code, String source, String message, String requestUuid);
        void bindCommunicationProgressResponse(int code, String source, String message, String results, String requestUuid);
        void bindSpaceCreateResponse(int code, String source, String message, String results, String requestUuid);
        void bindRevokeResponse(int code, String source, String message, String results, String requestUuid);
        void newDeviceApplyResetPasswordResponse(int code, String source, String message, String results, String requestUuid);
        void securityMessagePollResponse(int code, String source, String message, String results, String requestUuid);
        void newDeviceResetPasswordResponse(int code, String source, String message, String results, String requestUuid);
    }

    @NonNull
    public PairingClientInfo generatePairingClientInfo() {
        PairingClientInfo pairingClientInfo = new PairingClientInfo();
        EulixSpaceSharePreferenceHelper sharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (sharePreferenceHelper != null && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)
                && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)) {
            pairingClientInfo.setClientUuid(sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID));
            pairingClientInfo.setClientPhoneModel(SystemUtil.getPhoneModel());
        }
        return pairingClientInfo;
    }


    // TODO 局域网部分
    public void generateBaseUrl(IPBean ipBean) {
        if (ipBean != null) {
            String ipv6Address = ipBean.getIPV6Address();
            String ipv4Address = ipBean.getIPV4Address();
            int port = ipBean.getPort();
            if (StringUtil.isNonBlankString(ipv6Address)) {
                lanAddress = "http://[" + ipv6Address + "]:" + port + "/";
            } else if (StringUtil.isNonBlankString(ipv4Address)) {
                lanAddress = "http://" + ipv4Address +  ":" + port + "/";
            }
        }
    }

    public void generateBaseUrl(NsdServiceInfo serviceInfo) {
        if (serviceInfo != null) {
            InetAddress inetAddress = serviceInfo.getHost();
            if (inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address) {
                boolean isIpv6 = (inetAddress instanceof Inet6Address);
                String hostAddress = inetAddress.getHostAddress();
                int port = serviceInfo.getPort();
                if (!TextUtils.isEmpty(hostAddress)) {
                    if (isIpv6) {
                        lanAddress = "http://[" + hostAddress + "]:" + port + "/";
//                        baseUrl = "http://" + device.getHostName() + ".local:" + port + "/";
                    } else {
                        lanAddress = "http://" + hostAddress +  ":" + port + "/";
                    }
                }
            }
        }
    }

    public boolean exchangePublicKey(PubKeyExchangeReq pubKeyExchangeReq, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && pubKeyExchangeReq != null) {
            isProgress = true;
            AgentUtil.exchangePublicKey(lanAddress, pubKeyExchangeReq, new PubKeyExchangeCallbackV2() {
                @Override
                public void onSuccess(int code, String source, String message, PubKeyExchangeRsp pubKeyExchangeRsp) {
                    if (iView != null) {
                        iView.publicKeyExchangeResponse(code, source, message, pubKeyExchangeRsp, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.publicKeyExchangeResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.publicKeyExchangeResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean exchangeSecretKey(KeyExchangeReq keyExchangeReq, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && keyExchangeReq != null) {
            isProgress = true;
            AgentUtil.exchangeSecretKey(lanAddress, keyExchangeReq, new KeyExchangeCallbackV2() {
                @Override
                public void onSuccess(int code, String source, String message, KeyExchangeRsp keyExchangeRsp) {
                    if (iView != null) {
                        iView.keyExchangeResponse(code, source, message, keyExchangeRsp, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.keyExchangeResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.keyExchangeResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean pairInit(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            AgentUtil.pairInit(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.pairInitResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.pairInitResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.pairInitResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean getWifiList(WifiRequest wifiRequest, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && wifiRequest != null) {
            isProgress = true;
            DeviceUtil.getWifiList(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.wifiListResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.wifiListResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.wifiListResponse(ConstantField.SERVER_EXCEPTION_CODE, null, null, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean setWifi(WpwdInfo wpwdInfo, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && wpwdInfo != null) {
            isProgress = true;
            DeviceUtil.setWifi(lanAddress, wpwdInfo, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.setWifiResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.setWifiResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.setWifiResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean pairing(PairingClientInfo pairingClientInfo, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && pairingClientInfo != null) {
            isProgress = true;
            AgentUtil.pairingEnc(lanAddress, pairingClientInfo, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.pairingResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.pairingResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.pairingResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean revoke(RvokInfo rvokInfo, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && rvokInfo != null) {
            isProgress = true;
            AgentUtil.revoke(lanAddress, rvokInfo, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.revokeResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.revokeResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.revokeResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean setPassword(PasswordInfo passwordInfo, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && passwordInfo != null) {
            isProgress = true;
            AgentUtil.setPassword(lanAddress, passwordInfo, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.setPasswordResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.setPasswordResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.setPasswordResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean initial(PasswordInfo passwordInfo, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            AgentEncryptedResultsCallback initialCallback = new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.initialResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.initialResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.initialResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            };
            if (passwordInfo == null) {
                AgentUtil.initial(lanAddress, initialCallback);
            } else {
                AgentUtil.initial(lanAddress, passwordInfo, initialCallback);
            }
        }
        return isProgress;
    }

    public boolean spaceReadyCheck(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            DiskUtil.getSpaceReadyCheck(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.spaceReadyCheckResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.spaceReadyCheckResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.spaceReadyCheckResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean diskRecognition(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            DiskUtil.getDiskRecognition(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.diskRecognitionResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.diskRecognitionResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.diskRecognitionResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean diskInitialize(DiskInitializeRequest diskInitializeRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && diskInitializeRequest != null) {
            isProgress = true;
            String request = new Gson().toJson(diskInitializeRequest, DiskInitializeRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            DiskUtil.diskInitialize(lanAddress, request, new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.diskInitializeResponse(code, source, message, requestUuid);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.diskInitializeResponse(500, null, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.diskInitializeResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean diskInitializeProgress(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            DiskUtil.getDiskInitializeProgress(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.diskInitializeProgressResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.diskInitializeProgressResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.diskInitializeProgressResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean diskManagementList(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            DiskUtil.getDiskManagementList(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.diskManagementListResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.diskManagementListResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.diskManagementListResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean aoSystemShutdown(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            DiskUtil.eulixSystemShutdown(lanAddress, new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.aoSystemShutdownResponse(code, source, message, requestUuid);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.aoSystemShutdownResponse(500, null, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.aoSystemShutdownResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean aoSystemReboot(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            DiskUtil.eulixSystemShutdown(lanAddress, new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.aoSystemRebootResponse(code, source, message, requestUuid);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.aoSystemRebootResponse(500, null, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.aoSystemRebootResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean getNetworkConfig(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            EulixNetUtil.getNetworkConfig(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.getNetworkConfigResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.getNetworkConfigResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.getNetworkConfigResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean setNetworkConfig(NetworkConfigRequest networkConfigRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            String request = new Gson().toJson(networkConfigRequest, NetworkConfigRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            EulixNetUtil.setNetworkConfig(lanAddress, request, new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.setNetworkConfigResponse(code, source, message, requestUuid);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.setNetworkConfigResponse(500, null, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.setNetworkConfigResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean ignoreNetwork(NetworkIgnoreRequest networkIgnoreRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            String request = new Gson().toJson(networkIgnoreRequest, NetworkIgnoreRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            EulixNetUtil.ignoreNetwork(lanAddress, request, new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.ignoreNetworkResponse(code, source, message, requestUuid);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.ignoreNetworkResponse(500, null, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.ignoreNetworkResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean switchPlatform(SwitchPlatformRequest switchPlatformRequest, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && switchPlatformRequest != null) {
            isProgress = true;
            EulixPlatformUtil.switchPlatform(lanAddress, switchPlatformRequest, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.switchPlatformResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.switchPlatformResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.switchPlatformResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean switchStatus(String transId, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            EulixPlatformUtil.getSwitchPlatformStatus(lanAddress, transId, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.switchStatusResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.switchStatusResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.switchStatusResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean bindCommunicationStart(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            BindUtil.bindCommunicationStart(lanAddress, new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.bindCommunicationStartResponse(code, source, message, requestUuid);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.bindCommunicationStartResponse(500, null, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.bindCommunicationStartResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean getBindCommunicationProgress(final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            BindUtil.getBindCommunicationProgress(lanAddress, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.bindCommunicationProgressResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.bindCommunicationProgressResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.bindCommunicationProgressResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean bindSpaceCreate(SpaceCreateRequest spaceCreateRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            String request = new Gson().toJson(spaceCreateRequest, SpaceCreateRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            BindUtil.bindSpaceCreate(lanAddress, request, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.bindSpaceCreateResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.bindSpaceCreateResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.bindSpaceCreateResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean bindRevoke(BindRevokeRequest bindRevokeRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null) {
            isProgress = true;
            String request = new Gson().toJson(bindRevokeRequest, BindRevokeRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            BindUtil.bindRevoke(lanAddress, request, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.bindRevokeResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.bindRevokeResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.bindRevokeResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean newDeviceApplyResetPassword(NewDeviceApplyResetPasswordRequest newDeviceApplyResetPasswordRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && newDeviceApplyResetPasswordRequest != null) {
            isProgress = true;
            String request = new Gson().toJson(newDeviceApplyResetPasswordRequest, NewDeviceApplyResetPasswordRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            AgentUtil.newDeviceApplyResetSecurityPassword(lanAddress, request, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.newDeviceApplyResetPasswordResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.newDeviceApplyResetPasswordResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.newDeviceApplyResetPasswordResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean securityMessagePoll(SecurityMessagePollRequest securityMessagePollRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && securityMessagePollRequest != null) {
            isProgress = true;
            String request = new Gson().toJson(securityMessagePollRequest, SecurityMessagePollRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            AgentUtil.securityMessagePoll(lanAddress, request, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.securityMessagePollResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.securityMessagePollResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.securityMessagePollResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }

    public boolean newDeviceResetPassword(NewDeviceResetPasswordRequest newDeviceResetPasswordRequest, String boxKey, String boxIv, final String requestUuid) {
        boolean isProgress = false;
        if (lanAddress != null && newDeviceResetPasswordRequest != null) {
            isProgress = true;
            String request = new Gson().toJson(newDeviceResetPasswordRequest, NewDeviceResetPasswordRequest.class);
            if (boxKey != null && boxIv != null) {
                request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, request, boxKey, StandardCharsets.UTF_8, boxIv);
            }
            AgentUtil.resetSecurityPassword(lanAddress, request, new AgentEncryptedResultsCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String results) {
                    if (iView != null) {
                        iView.newDeviceResetPasswordResponse(code, source, message, results, requestUuid);
                    }
                }

                @Override
                public void onFailed(int code, String source, String message) {
                    if (iView != null) {
                        iView.newDeviceResetPasswordResponse(code, source, message, null, requestUuid);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.newDeviceResetPasswordResponse(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null, requestUuid);
                    }
                }
            });
        }
        return isProgress;
    }
}
