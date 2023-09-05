package xyz.eulix.space.network.net;

public interface InternetServiceConfigCallback {
    void onSuccess(int code, String source, String message, String requestId, InternetServiceConfigResult result);
    void onFail(int code, String source, String message, String requestId);
    void onError(String errMsg);
}
