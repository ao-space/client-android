package xyz.eulix.space.network.agent.platform;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/2/6 18:07
 */
public interface IPlatformAbilityCallback {
    void onResponse(PlatformAbilityResponse response);
    void onError(String errMsg);
}
