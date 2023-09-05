package xyz.eulix.space.network.agent;

import xyz.eulix.space.bean.bind.PubKeyExchangeRsp;

public interface PubKeyExchangeCallbackV2 {
    void onSuccess(int code, String source, String message, PubKeyExchangeRsp pubKeyExchangeRsp);
    void onFailed(int code, String source, String message);
    void onError(String errMsg);
}
