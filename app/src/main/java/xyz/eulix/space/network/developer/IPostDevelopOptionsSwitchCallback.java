package xyz.eulix.space.network.developer;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:16
 */
public interface IPostDevelopOptionsSwitchCallback {
    void onResponse(PostDevelopOptionsSwitchResponse response);
    void onError(String errMsg);
}
