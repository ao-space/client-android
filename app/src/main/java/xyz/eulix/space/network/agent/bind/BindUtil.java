package xyz.eulix.space.network.agent.bind;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.network.agent.AgentEncryptedResultsCallback;
import xyz.eulix.space.network.agent.IAgentBaseResponseCallback;
import xyz.eulix.space.network.agent.disk.DiskManager;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

public class BindUtil {
    private static final String TAG = BindUtil.class.getSimpleName();
    private static Map<String, BindManager> managerMap = new HashMap<>();

    private BindUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static BindManager generateManager(String baseUrl) {
        BindManager bindManager = null;
        if (managerMap.containsKey(baseUrl)) {
            bindManager = managerMap.get(baseUrl);
        }
        if (bindManager == null) {
            bindManager = new BindManager(baseUrl);
            managerMap.put(baseUrl, bindManager);
        }
        return bindManager;
    }

    public static void bindCommunicationStart(@NonNull String baseUrl, EulixBaseResponseExtensionCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).bindCommunicationStart(new IEulixBaseResponseCallback() {
                @Override
                public void onResult(EulixBaseResponse response) {
                    Logger.i(TAG, "on result: " + response);
                    int code = 500;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        if (codeValue != null) {
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                        }
                        message = response.getMessage();
                        requestId = response.getRequestId();
                    }
                    if (response == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else if (callback != null) {
                        callback.onSuccess(source, code, message, requestId);
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

    public static void getBindCommunicationProgress(@NonNull String baseUrl, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getBindCommunicationProgress(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    Logger.i(TAG, "on result: " + response);
                    String results = null;
                    int code = 500;
                    String source = null;
                    String message = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        results = response.getResults();
                    }
                    if (results == null) {
                        if (callback != null) {
                            callback.onFailed(code, source, message);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, results);
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

    public static void bindSpaceCreate(@NonNull String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).bindSpaceCreate(eulixBaseRequest
                    , new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    Logger.i(TAG, "on result: " + response);
                    String results = null;
                    int code = 500;
                    String source = null;
                    String message = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        results = response.getResults();
                    }
                    if (results == null) {
                        if (callback != null) {
                            callback.onFailed(code, source, message);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, results);
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

    public static void bindRevoke(@NonNull String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).bindRevoke(eulixBaseRequest
                    , new IAgentBaseResponseCallback() {
                        @Override
                        public void onResponse(AgentBaseResponse response) {
                            Logger.i(TAG, "on result: " + response);
                            String results = null;
                            int code = 500;
                            String source = null;
                            String message = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                code = DataUtil.stringCodeToInt(codeValue);
                                source = DataUtil.stringCodeGetSource(codeValue);
                                message = response.getMessage();
                                results = response.getResults();
                            }
                            if (results == null) {
                                if (callback != null) {
                                    callback.onFailed(code, source, message);
                                }
                            } else if (callback != null) {
                                callback.onSuccess(code, source, message, results);
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
