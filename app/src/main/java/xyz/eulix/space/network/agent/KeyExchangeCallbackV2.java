package xyz.eulix.space.network.agent;

import xyz.eulix.space.bean.bind.KeyExchangeRsp;

public interface KeyExchangeCallbackV2 {
    void onSuccess(int code, String source, String message, KeyExchangeRsp keyExchangeRsp);
    void onFailed(int code, String source, String message);
    void onError(String errMsg);
}
