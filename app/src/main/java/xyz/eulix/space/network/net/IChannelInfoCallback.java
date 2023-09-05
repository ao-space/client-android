package xyz.eulix.space.network.net;

public interface IChannelInfoCallback {
    void onResponse(ChannelInfoResponse response);
    void onError(String errMsg);
}
