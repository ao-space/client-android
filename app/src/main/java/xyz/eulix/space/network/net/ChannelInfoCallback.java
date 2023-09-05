package xyz.eulix.space.network.net;

public interface ChannelInfoCallback {
    void onSuccess(int code, String source, String message, String requestId, ChannelInfoResult result);
    void onFail(int code, String source, String message, String requestId);
    void onError(String errMsg);
}
