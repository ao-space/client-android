package xyz.eulix.space.network.developer;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.EulixBaseResponse;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:14
 */
public class GetDevelopOptionsSwitchResponse extends EulixBaseResponse implements EulixKeep, Serializable {
    @SerializedName("results")
    private DevelopOptionsSwitchInfo results;

    public DevelopOptionsSwitchInfo getResults() {
        return results;
    }

    public void setResults(DevelopOptionsSwitchInfo results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "GetDevelopOptionsSwitchResponse{" +
                "results=" + results +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
