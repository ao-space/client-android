package xyz.eulix.space.network.developer;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:16
 */
public interface IGetDevelopOptionsSwitchCallback {
    void onResponse(GetDevelopOptionsSwitchResponse response);
    void onError(String errMsg);
}
