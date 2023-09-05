package xyz.eulix.space.network.developer;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:17
 */
public interface GetDevelopOptionsSwitchCallback {
    void onSuccess(int code, String source, String message, String requestId, DevelopOptionsSwitchInfo result);
    void onFailed(int code, String source, String message, String requestId);
    void onError(String errMsg);
}
