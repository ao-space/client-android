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

package xyz.eulix.space.network.interceptor;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import kotlin.Pair;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.DeviceVersionInfoResponseBody;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.network.DIDDocumentResponse;
import xyz.eulix.space.event.GranteeTokenInvalidEvent;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.LocalIpInfo;
import xyz.eulix.space.network.agent.NetworkConfigRealResponse;
import xyz.eulix.space.network.agent.NetworkInfo;
import xyz.eulix.space.network.agent.ScanRealWifiList;
import xyz.eulix.space.network.box.BKeyCheckResponseBody;
import xyz.eulix.space.network.box.BoxLoginAuthCodeResponseBody;
import xyz.eulix.space.network.box.DeviceInfoResult;
import xyz.eulix.space.network.developer.GetDevelopOptionsSwitchResponse;
import xyz.eulix.space.network.developer.PostDevelopOptionsSwitchResponse;
import xyz.eulix.space.network.disk.DiskExpandProgressResponse;
import xyz.eulix.space.network.disk.DiskManageListResponse;
import xyz.eulix.space.network.disk.RaidInfoResponse;
import xyz.eulix.space.network.files.AsyncTaskStatusResponseBody;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.files.FileRsp;
import xyz.eulix.space.network.files.FolderInfoResponseBody;
import xyz.eulix.space.network.files.GetFileListResponseBody;
import xyz.eulix.space.network.files.NewFolderRsp;
import xyz.eulix.space.network.files.RecycledListResponse;
import xyz.eulix.space.network.files.UploadResponseBodyResult;
import xyz.eulix.space.network.gateway.CallRequest;
import xyz.eulix.space.network.gateway.RealCallRequest;
import xyz.eulix.space.network.gateway.RealCallResult;
import xyz.eulix.space.network.net.ChannelInfoResponse;
import xyz.eulix.space.network.net.DeviceAbilityResponse;
import xyz.eulix.space.network.net.InternetServiceConfigResponse;
import xyz.eulix.space.network.net.NetworkStatusResponse;
import xyz.eulix.space.network.notification.GetNotificationResponse;
import xyz.eulix.space.network.notification.NotificationAllResponse;
import xyz.eulix.space.network.notification.NotificationDeleteResponse;
import xyz.eulix.space.network.security.DeviceHardwareInfoResponse;
import xyz.eulix.space.network.security.SecurityMessagePollResponse;
import xyz.eulix.space.network.security.VerifySecurityResponse;
import xyz.eulix.space.network.upgrade.UpgradeConfigResponseBody;
import xyz.eulix.space.network.upgrade.UpgradeStatusResponseBody;
import xyz.eulix.space.network.userinfo.AccountInfoResult;
import xyz.eulix.space.network.userinfo.MemberNameUpdateResult;
import xyz.eulix.space.network.userinfo.MemberUsedStorageResponseBody;
import xyz.eulix.space.network.userinfo.PutPersonalInfoResponseBody;
import xyz.eulix.space.network.userinfo.TerminalListResponse;
import xyz.eulix.space.network.userinfo.TerminalOfflineResponse;
import xyz.eulix.space.transfer.multipart.bean.GetCertResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCompleteResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCreateResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadListResponseBody;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * date: 2021/7/7 11:22
 */
public class EulixGatewayInterceptor implements Interceptor {
    private static final String TAG = EulixGatewayInterceptor.class.getSimpleName();
    private String boxDomainUrl;
    private UUID requestId;
    private String requestType;
    private String algorithm;
    private String provider;
    private String ivParams;
    private String accessToken;
    private String secret;
    private MediaType requestMediaType;
    private MediaType responseMediaType;
    private String apiVersion;
    //忽略结果处理，直接返回
    private boolean ignoreResponse = false;

    public EulixGatewayInterceptor(String boxDomainUrl, UUID requestId, String requestType, String algorithm, String provider, String ivParams, String accessToken, String secret, MediaType requestMediaType, MediaType responseMediaType, String apiVersion) {
        this.boxDomainUrl = boxDomainUrl;
        this.requestId = requestId;
        this.requestType = requestType;
        this.algorithm = algorithm;
        this.provider = provider;
        this.ivParams = ivParams;
        this.accessToken = accessToken;
        this.secret = secret;
        this.requestMediaType = requestMediaType;
        this.responseMediaType = responseMediaType;
        this.apiVersion = apiVersion;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        RequestBody plainRequestBody = request.body();
        boolean isEncrypt = false;
        boolean isDecrypt = false;
        String plainRequestText = null;
        if (plainRequestBody != null) {
            Buffer buffer = new Buffer();
            plainRequestBody.writeTo(buffer);
            plainRequestText = buffer.readUtf8();
            buffer.close();
        }
        String apiUrl = "";
        String finalUrl = "";
        String finalHeader = "";
        String requestBodyContent = "";
        if (requestType != null && !requestType.equals(ConstantField.ServiceFunction.UPLOAD_FILE)) {
            RealCallRequest realCallRequest = new RealCallRequest();
            switch (requestType) {
                case ConstantField.ServiceFunction.LIST_FOLDERS:
                case ConstantField.ServiceFunction.FOLDER_INFO:
                case ConstantField.ServiceFunction.MODIFY_FILE:
                case ConstantField.ServiceFunction.COPY_FILE:
                case ConstantField.ServiceFunction.MOVE_FILE:
                case ConstantField.ServiceFunction.DELETE_FILE:
                case ConstantField.ServiceFunction.CREATE_FOLDER:
                case ConstantField.ServiceFunction.SEARCH_FILES:
                case ConstantField.ServiceFunction.FILE_INFO:
                case ConstantField.ServiceFunction.LIST_RECYCLED:
                case ConstantField.ServiceFunction.RESTORE_RECYCLED:
                case ConstantField.ServiceFunction.CLEAR_RECYCLED:
                case ConstantField.ServiceFunction.DOWNLOAD_FILE:
                case ConstantField.ServiceFunction.DOWNLOAD_RESTORE_HEADER:
                case ConstantField.ServiceFunction.MULTIPART_CREATE_UPLOAD:
                case ConstantField.ServiceFunction.MULTIPART_LIST_UPLOAD:
                case ConstantField.ServiceFunction.MULTIPART_COMPLETE_UPLOAD:
                case ConstantField.ServiceFunction.MULTIPART_DELETE_UPLOAD:
                case ConstantField.ServiceFunction.ASYNC_TASK_INFO:
                    if (requestType.equals(ConstantField.ServiceFunction.DOWNLOAD_FILE)
                            || requestType.equals(ConstantField.ServiceFunction.DOWNLOAD_RESTORE_HEADER)) {
                        ignoreResponse = true;
                        apiUrl = ConstantField.URL.DOWNLOAD_GATEWAY_API;
                    } else {
                        apiUrl = ConstantField.URL.CALL_GATEWAY_API;
                    }
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_FILE_SERVICE);
                    manageRequest(realCallRequest, request);
                    break;
                case ConstantField.ServiceFunction.UPLOAD_FILE:
                    apiUrl = ConstantField.URL.UPLOAD_GATEWAY_API;
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_FILE_SERVICE);
                    realCallRequest.setApiName(requestType);
                    realCallRequest.setApiVersion(apiVersion);
                    break;
                case ConstantField.ServiceFunction.STORAGE_INFO_SHOW:
                case ConstantField.ServiceFunction.PERSONALINFO_UPDATE:
                case ConstantField.ServiceFunction.PERSONALINFO_SHOW:
                case ConstantField.ServiceFunction.PERSONAL_INFO_UPDATE:
                case ConstantField.ServiceFunction.MEMBER_USED_STORAGE:
                case ConstantField.ServiceFunction.MEMBER_LIST:
                case ConstantField.ServiceFunction.TERMINAL_INFO_ALL_SHOW:
                case ConstantField.ServiceFunction.TERMINAL_INFO_DELETE:
                case ConstantField.ServiceFunction.MEMBER_NAME_UPDATE:
                case ConstantField.ServiceFunction.NOTIFICATION_GET:
                case ConstantField.ServiceFunction.NOTIFICATION_GET_ALL:
                case ConstantField.ServiceFunction.NOTIFICATION_DELETE_ALL:
                case ConstantField.ServiceFunction.DEVICE_HARDWARE_INFO:
                case ConstantField.ServiceFunction.SECURITY_EMAIL_CONFIGURATIONS:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_VERIFY:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_BINDER:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_AUTHORIZED_APPLY:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_AUTHORIZED_APPLY:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER_ACCEPT:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_BINDER_ACCEPT:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_AUTHORIZED:
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_AUTHORIZED:
                case ConstantField.ServiceFunction.SECURITY_MESSAGE_POLL:
                case ConstantField.ServiceFunction.ADMINISTRATOR_GET_DEVELOP_OPTIONS_SWITCH:
                case ConstantField.ServiceFunction.ADMINISTRATOR_UPDATE_DEVELOP_OPTIONS_SWITCH:
                case ConstantField.ServiceFunction.BOX_LOGIN_BKEY_VERIFY:
                case ConstantField.ServiceFunction.BOX_LOGIN_GET_AUTH_CODE:
                case ConstantField.ServiceFunction.BOX_LOGIN_POLL_AUTH_RESULT:
                case ConstantField.ServiceFunction.GET_NETWORK_CHANNEL_INFO:
                case ConstantField.ServiceFunction.SET_NETWORK_CHANNEL_WAN:
                    apiUrl = ConstantField.URL.CALL_GATEWAY_API;
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_ACCOUNT_SERVICE);
                    manageRequest(realCallRequest, request);
                    break;
                case ConstantField.ServiceFunction.IMAGE_SHOW:
                    ignoreResponse = true;
                    apiUrl = ConstantField.URL.DOWNLOAD_GATEWAY_API;
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_ACCOUNT_SERVICE);
                    manageRequest(realCallRequest, request);
                    break;
                case ConstantField.ServiceFunction.DOWNLOAD_THUMBNAILS:
                case ConstantField.ServiceFunction.DOWNLOAD_COMPRESSED:
                case ConstantField.ServiceFunction.DOWNLOAD_PREVIEW:
                    ignoreResponse = true;
                    apiUrl = ConstantField.URL.DOWNLOAD_GATEWAY_API;
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_PREVIEW_SERVICE);
                    manageRequest(realCallRequest, request);
                    break;
                case ConstantField.ServiceFunction.GET_UPGRADE_CONFIG:
                case ConstantField.ServiceFunction.SET_UPGRADE_CONFIG:
                case ConstantField.ServiceFunction.UPGRADE_STATUS:
                case ConstantField.ServiceFunction.UPGRADE_START_PULL:
                case ConstantField.ServiceFunction.UPGRADE_START_UPGRADE:
                case ConstantField.ServiceFunction.LOCAL_IPS:
                case ConstantField.ServiceFunction.NETWORK:
                case ConstantField.ServiceFunction.NET_CONFIG:
                case ConstantField.ServiceFunction.NET_CONFIG_SETTING:
                case ConstantField.ServiceFunction.DEVICE_INFO:
                case ConstantField.ServiceFunction.DISK_MANAGEMENT_LIST:
                case ConstantField.ServiceFunction.DISK_MANAGEMENT_RAID_INFO:
                case ConstantField.ServiceFunction.SYSTEM_SHUTDOWN:
                case ConstantField.ServiceFunction.DISK_MANAGEMENT_EXPAND:
                case ConstantField.ServiceFunction.DISK_MANAGEMENT_EXPAND_PROGRESS:
                case ConstantField.ServiceFunction.NETWORK_CONFIG:
                case ConstantField.ServiceFunction.NETWORK_CONFIG_UPDATE:
                case ConstantField.ServiceFunction.NETWORK_IGNORE:
                case ConstantField.ServiceFunction.DEVICE_ABILITY:
                case ConstantField.ServiceFunction.INTERNET_SERVICE_GET_CONFIG:
                case ConstantField.ServiceFunction.INTERNET_SERVICE_CONFIG:
                case ConstantField.ServiceFunction.GET_DID_DOCUMENT:
                case ConstantField.ServiceFunction.GET_HTTPS_CERT:
                    apiUrl = ConstantField.URL.CALL_GATEWAY_API;
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_AGENT_SERVICE);
                    manageRequest(realCallRequest, request);
                    break;
                case ConstantField.ServiceFunction.MEDIA_VOD_CHECK:
                case ConstantField.ServiceFunction.MEDIA_VOD_M3U8_DOWNLOAD:
                    if (requestType.equals(ConstantField.ServiceFunction.MEDIA_VOD_M3U8_DOWNLOAD)) {
                        ignoreResponse = true;
                        apiUrl = ConstantField.URL.DOWNLOAD_GATEWAY_API;
                    } else {
                        apiUrl = ConstantField.URL.CALL_GATEWAY_API;
                    }
                    realCallRequest.setServiceName(ConstantField.ServiceName.EULIXSPACE_MEDIA_VOD_SERVICE);
                    manageRequest(realCallRequest, request);
                    break;
                default:
                    break;
            }
            if (plainRequestText != null) {
                Map<String, Object> entityMap = JSON.parseObject(plainRequestText);
                if (entityMap != null) {
                    realCallRequest.setEntity(entityMap);
                }
            }
            if (requestId == null) {
                requestId = UUID.randomUUID();
            }
            realCallRequest.setRequestId(requestId.toString());
            CallRequest callRequest = new CallRequest();
            callRequest.setAccessToken(accessToken);
            callRequest.setBody(EncryptionUtil.encrypt(algorithm, provider
                    , new Gson().toJson(realCallRequest, RealCallRequest.class), secret, StandardCharsets.UTF_8, ivParams));
            isEncrypt = true;
            requestBodyContent = new Gson().toJson(callRequest, CallRequest.class);
            RequestBody cipherRequestBody = RequestBody.create(requestBodyContent, requestMediaType);
            String baseUrl = boxDomainUrl;
            if (baseUrl == null) {
                baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
            } else {
                while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                    baseUrl = baseUrl.substring(1);
                }
                if (TextUtils.isEmpty(baseUrl)) {
                    baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
                } else {
                    if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                        baseUrl = "https://" + baseUrl;
                    }
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }
                }
            }
            request = request.newBuilder()
                    .header("Request-Id", requestId.toString())
                    .post(cipherRequestBody)
                    .url(baseUrl + apiUrl)
                    .build();
            Logger.d("zfy", "request realCallRequest:" + new Gson().toJson(realCallRequest, RealCallRequest.class));
            Logger.d("zfy", "request url:" + baseUrl + apiUrl);
            Logger.d("zfy", "request header:" + request.headers().toString());
            Logger.d("zfy", "request body:" + new Gson().toJson(callRequest, CallRequest.class));
            finalUrl = (baseUrl + apiUrl);
            finalHeader = request.headers().toString();
        }
        if (requestType != null && requestType.equals(ConstantField.ServiceFunction.UPLOAD_FILE)) {
            isEncrypt = true;
        }
        Response response = chain.proceed(request);
        int responseCode = response.code();
        ResponseBody cipherResponseBody = response.body();
        if (requestType == null || ignoreResponse) {
            //下载相关response不做处理
            return response;
        }
        boolean errorInterceptor = false;
        if (isEncrypt) {
            if (cipherResponseBody != null) {
                String cipherResponseText = null;
                try {
                    cipherResponseText = cipherResponseBody.string();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String plainResponseText = cipherResponseText;
                Logger.d(TAG, "cipher response text: " + cipherResponseText);
                ResponseManageResult responseManageResult = null;
                if (!TextUtils.isEmpty(cipherResponseText)) {
                    RealCallResult realCallResult = null;
                    try {
                        realCallResult = new Gson().fromJson(cipherResponseText, RealCallResult.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    if (realCallResult != null && requestType != null) {
                        String requestId = realCallResult.getRequestId();
                        String body = realCallResult.getBody();
                        String codeValue = realCallResult.getCode();
                        if (ConstantField.URL.CALL_GATEWAY_API.equals(apiUrl)) {
                            int code = DataUtil.stringCodeToInt(codeValue);
                            EulixBoxBaseInfo baseInfo = null;
                            switch (code) {
                                case ConstantField.KnownError.GatewayCallError.ACCESS_TOKEN_ERROR:
                                    baseInfo = EulixSpaceDBUtil.getBoxSpaceWithAccessToken(EulixSpaceApplication.getContext(), accessToken);
                                    if (baseInfo == null) {
                                        baseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(EulixSpaceApplication.getContext(), true);
                                    }
                                    if (baseInfo != null) {
                                        String boxUuid = baseInfo.getBoxUuid();
                                        String boxBind = baseInfo.getBoxBind();
                                        String boxDomain = baseInfo.getBoxDomain();
                                        GranteeTokenInvalidEvent granteeTokenInvalidEvent = new GranteeTokenInvalidEvent(boxUuid, boxBind, boxDomain);
                                        EventBusUtil.post(granteeTokenInvalidEvent);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        switch (requestType) {
                            case ConstantField.ServiceFunction.LIST_FOLDERS:
                            case ConstantField.ServiceFunction.SEARCH_FILES:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        GetFileListResponseBody fileListResponseBody = null;
                                        try {
                                            fileListResponseBody = new Gson().fromJson(decryptBody, GetFileListResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (fileListResponseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            fileListResponseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(fileListResponseBody, GetFileListResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.FOLDER_INFO:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        FolderInfoResponseBody folderInfoResponseBody = null;
                                        try {
                                            folderInfoResponseBody = new Gson().fromJson(decryptBody, FolderInfoResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (folderInfoResponseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            folderInfoResponseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(folderInfoResponseBody, FolderInfoResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MODIFY_FILE:
                            case ConstantField.ServiceFunction.COPY_FILE:
                            case ConstantField.ServiceFunction.MOVE_FILE:
                            case ConstantField.ServiceFunction.DELETE_FILE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        FileRsp fileRsp = null;
                                        try {
                                            fileRsp = new Gson().fromJson(decryptBody, FileRsp.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (fileRsp == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            fileRsp.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(fileRsp, FileRsp.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.CREATE_FOLDER:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        NewFolderRsp newFolderRsp = null;
                                        try {
                                            newFolderRsp = new Gson().fromJson(decryptBody, NewFolderRsp.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (newFolderRsp == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            newFolderRsp.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(newFolderRsp, NewFolderRsp.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.FILE_INFO:
                            case ConstantField.ServiceFunction.UPLOAD_FILE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "decryptBody = " + decryptBody);
                                    if (decryptBody != null) {
                                        UploadResponseBodyResult fileListResponseBody = null;
                                        try {
                                            fileListResponseBody = new Gson().fromJson(decryptBody, UploadResponseBodyResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (fileListResponseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            fileListResponseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(fileListResponseBody, UploadResponseBodyResult.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.LIST_RECYCLED:
                            case ConstantField.ServiceFunction.RESTORE_RECYCLED:
                            case ConstantField.ServiceFunction.CLEAR_RECYCLED:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        RecycledListResponse recycledListResponse = null;
                                        try {
                                            recycledListResponse = new Gson().fromJson(decryptBody, RecycledListResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (recycledListResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            recycledListResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(recycledListResponse, RecycledListResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.STORAGE_INFO_SHOW:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        DeviceInfoResult deviceInfoResult = null;
                                        try {
                                            deviceInfoResult = new Gson().fromJson(decryptBody, DeviceInfoResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (deviceInfoResult == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            deviceInfoResult.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(deviceInfoResult, DeviceInfoResult.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.PERSONALINFO_UPDATE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "decryptBody = " + decryptBody);
                                    if (decryptBody != null) {
                                        AccountInfoResult accountInfoResult = null;
                                        try {
                                            accountInfoResult = new Gson().fromJson(decryptBody, AccountInfoResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (accountInfoResult == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(accountInfoResult, AccountInfoResult.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.PERSONALINFO_SHOW:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "PERSONALINFO_SHOW decryptBody = " + decryptBody);
                                    if (decryptBody != null) {
                                        AccountInfoResult accountInfoResult = null;
                                        try {
                                            accountInfoResult = new Gson().fromJson(decryptBody, AccountInfoResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (accountInfoResult == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(accountInfoResult, AccountInfoResult.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.PERSONAL_INFO_UPDATE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        PutPersonalInfoResponseBody putPersonalInfoResponseBody = null;
                                        try {
                                            putPersonalInfoResponseBody = new Gson().fromJson(decryptBody, PutPersonalInfoResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (putPersonalInfoResponseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(putPersonalInfoResponseBody, PutPersonalInfoResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.IMAGE_SHOW:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "decryptBody = " + decryptBody);

                                    isDecrypt = true;
                                }
                                break;
                            case ConstantField.ServiceFunction.MEMBER_USED_STORAGE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        MemberUsedStorageResponseBody memberUsedStorageResponseBody = null;
                                        try {
                                            memberUsedStorageResponseBody = new Gson().fromJson(decryptBody, MemberUsedStorageResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (memberUsedStorageResponseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            memberUsedStorageResponseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(memberUsedStorageResponseBody, MemberUsedStorageResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MEMBER_LIST:
                                Logger.d(TAG, "code: " + responseCode);
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        AccountInfoResult accountInfoResult = null;
                                        try {
                                            accountInfoResult = new Gson().fromJson(decryptBody, AccountInfoResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (accountInfoResult == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            accountInfoResult.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(accountInfoResult, AccountInfoResult.class);
                                        }
                                        isDecrypt = true;
                                    }
                                } else {
                                    int resultCode = DataUtil.stringCodeToInt(realCallResult.getCode());
                                    String message = realCallResult.getMessage();
                                    Logger.d(TAG, "code: " + resultCode + ", message: " + message);
                                    AccountInfoResult accountInfoResult = new AccountInfoResult();
                                    accountInfoResult.setCode(String.valueOf(ConstantField.KnownError.MEMBER_LIST_ERROR_CODE));
                                    accountInfoResult.setMessage(message);
                                    accountInfoResult.setRequestId(realCallResult.getRequestId());
                                    plainResponseText = new Gson().toJson(accountInfoResult, AccountInfoResult.class);
                                    errorInterceptor = true;
                                    isDecrypt = true;
                                }
                                break;
                            case ConstantField.ServiceFunction.TERMINAL_INFO_ALL_SHOW:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        TerminalListResponse terminalListResponse = null;
                                        try {
                                            terminalListResponse = new Gson().fromJson(decryptBody, TerminalListResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (terminalListResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            terminalListResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(terminalListResponse, TerminalListResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.TERMINAL_INFO_DELETE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "terminal delete body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        TerminalOfflineResponse terminalOfflineResponse = null;
                                        try {
                                            terminalOfflineResponse = new Gson().fromJson(decryptBody, TerminalOfflineResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (terminalOfflineResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            terminalOfflineResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(terminalOfflineResponse, TerminalOfflineResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MEMBER_NAME_UPDATE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        MemberNameUpdateResult memberNameUpdateResult = null;
                                        try {
                                            memberNameUpdateResult = new Gson().fromJson(decryptBody, MemberNameUpdateResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (memberNameUpdateResult == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            memberNameUpdateResult.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(memberNameUpdateResult, MemberNameUpdateResult.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NOTIFICATION_GET:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        GetNotificationResponse getNotificationResponse = null;
                                        try {
                                            getNotificationResponse = new Gson().fromJson(decryptBody, GetNotificationResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (getNotificationResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            getNotificationResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(getNotificationResponse, GetNotificationResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NOTIFICATION_GET_ALL:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        NotificationAllResponse notificationAllResponse = null;
                                        try {
                                            notificationAllResponse = new Gson().fromJson(decryptBody, NotificationAllResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (notificationAllResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            notificationAllResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(notificationAllResponse, NotificationAllResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NOTIFICATION_DELETE_ALL:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        NotificationDeleteResponse notificationDeleteResponse = null;
                                        try {
                                            notificationDeleteResponse = new Gson().fromJson(decryptBody, NotificationDeleteResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (notificationDeleteResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            notificationDeleteResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(notificationDeleteResponse, NotificationDeleteResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.DEVICE_HARDWARE_INFO:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        DeviceHardwareInfoResponse deviceHardwareInfoResponse = null;
                                        try {
                                            deviceHardwareInfoResponse = new Gson().fromJson(decryptBody, DeviceHardwareInfoResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (deviceHardwareInfoResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            deviceHardwareInfoResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(deviceHardwareInfoResponse, DeviceHardwareInfoResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.SECURITY_EMAIL_CONFIGURATIONS:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        plainResponseText = decryptBody;
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_BINDER:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_AUTHORIZED_APPLY:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_AUTHORIZED_APPLY:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER_ACCEPT:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_BINDER_ACCEPT:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_AUTHORIZED:
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_AUTHORIZED:
                            case ConstantField.ServiceFunction.SYSTEM_SHUTDOWN:
                            case ConstantField.ServiceFunction.DISK_MANAGEMENT_EXPAND:
                            case ConstantField.ServiceFunction.NETWORK_CONFIG_UPDATE:
                            case ConstantField.ServiceFunction.NETWORK_IGNORE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        EulixBaseResponse eulixBaseResponse = null;
                                        try {
                                            eulixBaseResponse = new Gson().fromJson(decryptBody, EulixBaseResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (eulixBaseResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            eulixBaseResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(eulixBaseResponse, EulixBaseResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.SECURITY_PASSWORD_VERIFY:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        VerifySecurityResponse verifySecurityResponse = null;
                                        try {
                                            verifySecurityResponse = new Gson().fromJson(decryptBody, VerifySecurityResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (verifySecurityResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            verifySecurityResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(verifySecurityResponse, VerifySecurityResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.SECURITY_MESSAGE_POLL:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        SecurityMessagePollResponse securityMessagePollResponse = null;
                                        try {
                                            securityMessagePollResponse = new Gson().fromJson(decryptBody, SecurityMessagePollResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (securityMessagePollResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            securityMessagePollResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(securityMessagePollResponse, SecurityMessagePollResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.DISK_MANAGEMENT_LIST:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        DiskManageListResponse diskManageListResponse = null;
                                        try {
                                            diskManageListResponse = new Gson().fromJson(decryptBody, DiskManageListResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (diskManageListResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            diskManageListResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(diskManageListResponse, DiskManageListResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.DISK_MANAGEMENT_RAID_INFO:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        RaidInfoResponse raidInfoResponse = null;
                                        try {
                                            raidInfoResponse = new Gson().fromJson(decryptBody, RaidInfoResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (raidInfoResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            raidInfoResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(raidInfoResponse, RaidInfoResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.DISK_MANAGEMENT_EXPAND_PROGRESS:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        DiskExpandProgressResponse diskExpandProgressResponse = null;
                                        try {
                                            diskExpandProgressResponse = new Gson().fromJson(decryptBody, DiskExpandProgressResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (diskExpandProgressResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            diskExpandProgressResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(diskExpandProgressResponse, DiskExpandProgressResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NETWORK_CONFIG:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        NetworkStatusResponse networkStatusResponse = null;
                                        try {
                                            networkStatusResponse = new Gson().fromJson(decryptBody, NetworkStatusResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (networkStatusResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            networkStatusResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(networkStatusResponse, NetworkStatusResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.DEVICE_ABILITY:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        DeviceAbilityResponse deviceAbilityResponse = null;
                                        try {
                                            deviceAbilityResponse = new Gson().fromJson(decryptBody, DeviceAbilityResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (deviceAbilityResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            deviceAbilityResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(deviceAbilityResponse, DeviceAbilityResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.GET_NETWORK_CHANNEL_INFO:
                            case ConstantField.ServiceFunction.SET_NETWORK_CHANNEL_WAN:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        ChannelInfoResponse channelInfoResponse = null;
                                        try {
                                            channelInfoResponse = new Gson().fromJson(decryptBody, ChannelInfoResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (channelInfoResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            channelInfoResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(channelInfoResponse, ChannelInfoResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.INTERNET_SERVICE_GET_CONFIG:
                            case ConstantField.ServiceFunction.INTERNET_SERVICE_CONFIG:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        InternetServiceConfigResponse internetServiceConfigResponse = null;
                                        try {
                                            internetServiceConfigResponse = new Gson().fromJson(decryptBody, InternetServiceConfigResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (internetServiceConfigResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            internetServiceConfigResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(internetServiceConfigResponse, InternetServiceConfigResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.GET_DID_DOCUMENT:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        DIDDocumentResponse didDocumentResponse = null;
                                        try {
                                            didDocumentResponse = new Gson().fromJson(decryptBody, DIDDocumentResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (didDocumentResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            didDocumentResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(didDocumentResponse, DIDDocumentResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.ADMINISTRATOR_GET_DEVELOP_OPTIONS_SWITCH:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        GetDevelopOptionsSwitchResponse getDevelopOptionsSwitchResponse = null;
                                        try {
                                            getDevelopOptionsSwitchResponse = new Gson().fromJson(decryptBody, GetDevelopOptionsSwitchResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (getDevelopOptionsSwitchResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            getDevelopOptionsSwitchResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(getDevelopOptionsSwitchResponse, GetDevelopOptionsSwitchResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.ADMINISTRATOR_UPDATE_DEVELOP_OPTIONS_SWITCH:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d(TAG, "body: " + body + ", decrypt body: " + decryptBody);
                                    if (decryptBody != null) {
                                        PostDevelopOptionsSwitchResponse postDevelopOptionsSwitchResponse = null;
                                        try {
                                            postDevelopOptionsSwitchResponse = new Gson().fromJson(decryptBody, PostDevelopOptionsSwitchResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (postDevelopOptionsSwitchResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            postDevelopOptionsSwitchResponse.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(postDevelopOptionsSwitchResponse, PostDevelopOptionsSwitchResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.GET_UPGRADE_CONFIG:
                            case ConstantField.ServiceFunction.SET_UPGRADE_CONFIG:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "get/set upgrade config decryptBody = " + decryptBody);
                                    if (!TextUtils.isEmpty(decryptBody)) {
                                        UpgradeConfigResponseBody responseBody = null;
                                        try {
                                            responseBody = new Gson().fromJson(decryptBody, UpgradeConfigResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (responseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(responseBody, UpgradeConfigResponseBody.class);
                                        }
                                    }
                                    isDecrypt = true;
                                }
                                break;
                            case ConstantField.ServiceFunction.UPGRADE_STATUS:
                                if (responseCode == 400) {
                                    //当前没有升级任务（升级已完成或未开始升级）
                                    Logger.d("zfy", "check upgrade status, response 400");
//                                    break;
                                }
                            case ConstantField.ServiceFunction.UPGRADE_START_PULL:
                            case ConstantField.ServiceFunction.UPGRADE_START_UPGRADE:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (requestType.equals(ConstantField.ServiceFunction.UPGRADE_START_UPGRADE)) {
                                        Logger.d("zfy", "start upgrade decryptBody = " + decryptBody);
                                    } else {
                                        Logger.d("zfy", "get upgrade status decryptBody = " + decryptBody);
                                    }
                                    if (!TextUtils.isEmpty(decryptBody)) {
                                        UpgradeStatusResponseBody responseBody = null;
                                        try {
                                            responseBody = new Gson().fromJson(decryptBody, UpgradeStatusResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (responseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(responseBody, UpgradeStatusResponseBody.class);
                                        }
                                    }
                                    isDecrypt = true;
                                }
                                break;
                            case ConstantField.ServiceFunction.LOCAL_IPS:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        LocalIpInfo localIpInfo = null;
                                        try {
                                            localIpInfo = new Gson().fromJson(decryptBody, LocalIpInfo.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (localIpInfo == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(localIpInfo, LocalIpInfo.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NETWORK:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        NetworkInfo networkInfo = null;
                                        try {
                                            networkInfo = new Gson().fromJson(decryptBody, NetworkInfo.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (networkInfo == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(networkInfo, NetworkInfo.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NET_CONFIG:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        ScanRealWifiList scanRealWifiList = null;
                                        try {
                                            scanRealWifiList = new Gson().fromJson(decryptBody, ScanRealWifiList.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (scanRealWifiList == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(scanRealWifiList, ScanRealWifiList.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.NET_CONFIG_SETTING:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    if (decryptBody != null) {
                                        NetworkConfigRealResponse networkConfigRealResponse = null;
                                        try {
                                            networkConfigRealResponse = new Gson().fromJson(decryptBody, NetworkConfigRealResponse.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (networkConfigRealResponse == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            plainResponseText = new Gson().toJson(networkConfigRealResponse, NetworkConfigRealResponse.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MULTIPART_CREATE_UPLOAD:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "multipart_create decryptBody:" + decryptBody);
                                    if (decryptBody != null) {
                                        UploadCreateResponseBody responseBody = null;
                                        try {
                                            responseBody = new Gson().fromJson(decryptBody, UploadCreateResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (responseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            responseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(responseBody, UploadCreateResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MULTIPART_LIST_UPLOAD:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "multipart_list decryptBody:" + decryptBody);

                                    if (decryptBody != null) {
                                        UploadListResponseBody responseBody = null;
                                        try {
                                            responseBody = new Gson().fromJson(decryptBody, UploadListResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (responseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            responseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(responseBody, UploadListResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MULTIPART_COMPLETE_UPLOAD:
                                if (body != null) {
                                    String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "multipart_complete decryptBody=" + decryptBody);
                                    if (decryptBody != null) {
                                        UploadCompleteResponseBody responseBody = null;
                                        try {
                                            responseBody = new Gson().fromJson(decryptBody, UploadCompleteResponseBody.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (responseBody == null) {
                                            plainResponseText = decryptBody;
                                        } else {
                                            responseBody.setRequestId(requestId);
                                            plainResponseText = new Gson().toJson(responseBody, UploadCompleteResponseBody.class);
                                        }
                                        isDecrypt = true;
                                    }
                                }
                                break;
                            case ConstantField.ServiceFunction.MEDIA_VOD_CHECK:
                                //查询是否支持在线播放
                                responseManageResult = manageResponse(body, BaseResponseBody.class);
                                break;
                            case ConstantField.ServiceFunction.DEVICE_INFO:
                                //设备版本信息详情
                                responseManageResult = manageResponse(body, DeviceVersionInfoResponseBody.class);
                                break;
                            case ConstantField.ServiceFunction.GET_HTTPS_CERT:
                                responseManageResult = manageResponse(body, GetCertResponseBody.class);
                                break;
                            case ConstantField.ServiceFunction.BOX_LOGIN_GET_AUTH_CODE:
                                responseManageResult = manageResponse(body, BoxLoginAuthCodeResponseBody.class);
                                break;
                            case ConstantField.ServiceFunction.BOX_LOGIN_POLL_AUTH_RESULT:
                            case ConstantField.ServiceFunction.BOX_LOGIN_BKEY_VERIFY:
                                responseManageResult = manageResponse(body, BKeyCheckResponseBody.class);
                                break;
                            case ConstantField.ServiceFunction.ASYNC_TASK_INFO:
                                responseManageResult = manageResponse(body, AsyncTaskStatusResponseBody.class);
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (responseManageResult != null) {
                    plainResponseText = responseManageResult.plainResponseText;
                    isDecrypt = responseManageResult.isDecrypt;
                }
                cipherResponseBody.close();
                if (isDecrypt) {
                    ResponseBody plainResponseBody = ResponseBody.create(plainResponseText, responseMediaType);
                    Logger.d("zfy", "plainResponseText=" + plainResponseText);
                    if (errorInterceptor) {
                        response = response.newBuilder()
                                .code(200)
                                .body(plainResponseBody)
                                .build();
                    } else {
                        response = response.newBuilder()
                                .body(plainResponseBody)
                                .build();
                    }
                }
            }
        }
        response.close();
        return response;
    }

    //处理request请求header、query
    private void manageRequest(RealCallRequest realCallRequest, Request request) {
        Map<String, String> headers = null;
        Map<String, String> queries = null;
        Iterator<Pair<String, String>> headersPairIterator = request.headers().iterator();
        HttpUrl requestUrl = request.url();
        Set<String> requestQueryParameterNames = requestUrl.queryParameterNames();
        realCallRequest.setApiName(requestType);
        realCallRequest.setApiVersion(apiVersion);
        while (headersPairIterator.hasNext()) {
            Pair<String, String> headersPair = headersPairIterator.next();
            if (headersPair != null) {
                String headersPairFirst = headersPair.getFirst();
                String headersPairSecond = headersPair.getSecond();
                if (headersPairFirst != null && headersPairSecond != null) {
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    headers.put(headersPairFirst, headersPairSecond);
                }
            }
        }
        for (String requestQueryParameterName : requestQueryParameterNames) {
            if (requestQueryParameterName != null) {
                String requestQueryParameter = requestUrl.queryParameter(requestQueryParameterName);
                if (requestQueryParameter != null) {
                    if (queries == null) {
                        queries = new HashMap<>();
                    }
                    queries.put(requestQueryParameterName, requestQueryParameter);
                }
            }
        }
        if (requestId == null) {
            try {
                requestId = UUID.fromString(ConstantField.UUID.FILE_ROOT_UUID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        realCallRequest.setHeaders(headers);
        realCallRequest.setQueries(queries);
    }

    private <T> ResponseManageResult manageResponse(String body, Class<T> clazz) {
        String plainResponseText = null;
        boolean isDecrypt = false;
        if (body != null) {
            String decryptBody = EncryptionUtil.decrypt(algorithm, provider, body, secret, StandardCharsets.UTF_8, ivParams);
            Logger.d("zfy", "decryptBody:" + decryptBody);
            if (decryptBody != null) {
                T responseBody = null;
                try {
                    responseBody = new Gson().fromJson(decryptBody, (Type) clazz);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (responseBody == null) {
                    plainResponseText = decryptBody;
                } else {
                    plainResponseText = new Gson().toJson(responseBody, clazz);
                }
                isDecrypt = true;
            }
        }
        return new ResponseManageResult(isDecrypt, plainResponseText);
    }

    //响应处理结果
    private class ResponseManageResult {
        public boolean isDecrypt;
        public String plainResponseText;

        public ResponseManageResult(boolean isDecrypt, String plainResponseText) {
            this.isDecrypt = isDecrypt;
            this.plainResponseText = plainResponseText;
        }
    }
}
