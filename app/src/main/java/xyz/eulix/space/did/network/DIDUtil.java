package xyz.eulix.space.did.network;

import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.net.EulixNetManager;
import xyz.eulix.space.network.net.IInternetServiceConfigCallback;
import xyz.eulix.space.network.net.InternetServiceConfigCallback;
import xyz.eulix.space.network.net.InternetServiceConfigResponse;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.ThreadPool;

public class DIDUtil {
    private static final String TAG = DIDUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    public static void getDIDDocument(String aoId, String did, String boxDomain, String accessToken, String secret, String ivParams, boolean isFore, DIDDocumentCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> DIDManager.getDIDDocument(aoId, did
                    , boxDomain, accessToken, secret, ivParams, API_VERSION, new IDIDDocumentCallback() {
                        @Override
                        public void onResponse(DIDDocumentResponse response) {
                            DIDDocumentResult result = null;
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
                                    callback.onFail(code, source, message, requestId);
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
}
