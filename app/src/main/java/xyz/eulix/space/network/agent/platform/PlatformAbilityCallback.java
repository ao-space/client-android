package xyz.eulix.space.network.agent.platform;

import java.util.List;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/2/6 18:09
 */
public interface PlatformAbilityCallback {
    void onSuccess(List<PlatformApi> platformApis);
    void onFailed();
    void onError(String errMsg);
}
