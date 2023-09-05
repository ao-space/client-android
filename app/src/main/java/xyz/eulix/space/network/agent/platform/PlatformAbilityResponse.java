package xyz.eulix.space.network.agent.platform;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/2/6 17:36
 */
public class PlatformAbilityResponse implements Serializable, EulixKeep {
    @SerializedName("platformApis")
    private List<PlatformApi> platformApis;

    public List<PlatformApi> getPlatformApis() {
        return platformApis;
    }

    public void setPlatformApis(List<PlatformApi> platformApis) {
        this.platformApis = platformApis;
    }

    @Override
    public String toString() {
        return "PlatformAbilityResponse{" +
                "platformApis=" + platformApis +
                '}';
    }
}
