package xyz.eulix.space.network.net;

public interface IInternetServiceConfigCallback {
    void onResponse(InternetServiceConfigResponse response);
    void onError(String errMsg);
}
