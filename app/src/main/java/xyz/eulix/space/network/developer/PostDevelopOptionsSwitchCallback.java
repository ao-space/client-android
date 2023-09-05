package xyz.eulix.space.network.developer;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:17
 */
public interface PostDevelopOptionsSwitchCallback {
    void onSuccess(int code, String source, String message, String requestId, Boolean result);
    void onFailed(int code, String source, String message, String requestId);
    void onError(String errMsg);
}
