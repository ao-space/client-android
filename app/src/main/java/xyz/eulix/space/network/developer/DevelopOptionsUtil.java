package xyz.eulix.space.network.developer;

import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:49
 */
public class DevelopOptionsUtil {
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    // server exception handle
    public static void getDevelopOptionsSwitch(String accessToken, String secret, String ivParams, boolean isFore, GetDevelopOptionsSwitchCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> DevelopOptionsManager.getDevelopOptionsSwitch(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IGetDevelopOptionsSwitchCallback() {
                @Override
                public void onResponse(GetDevelopOptionsSwitchResponse response) {
                    DevelopOptionsSwitchInfo result = null;
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        result = response.getResults();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void postDevelopOptionsSwitch(boolean isOn, String accessToken, String secret, String ivParams, PostDevelopOptionsSwitchCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        DevelopOptionsSwitchInfo request = new DevelopOptionsSwitchInfo();
        request.setStatus(isOn ? DevelopOptionsSwitchInfo.STATUS_ON : DevelopOptionsSwitchInfo.STATUS_OFF);
        try {
            ThreadPool.getInstance().execute(() -> DevelopOptionsManager.postDevelopOptionsSwitch(request
                    , finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IPostDevelopOptionsSwitchCallback() {
                        @Override
                        public void onResponse(PostDevelopOptionsSwitchResponse response) {
                            Boolean result = null;
                            int code = -1;
                            String source = null;
                            String message = null;
                            String requestId = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                code = DataUtil.stringCodeToInt(codeValue);
                                source = DataUtil.stringCodeGetSource(codeValue);
                                message = response.getMessage();
                                requestId = response.getRequestId();
                                result = response.getResults();
                            }
                            if (result == null) {
                                if (callback != null) {
                                    callback.onFailed(code, source, message, requestId);
                                }
                            } else if (callback != null) {
                                callback.onSuccess(code, source, message, requestId, result);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }
}
