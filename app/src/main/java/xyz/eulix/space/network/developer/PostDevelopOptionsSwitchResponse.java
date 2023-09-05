package xyz.eulix.space.network.developer;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.EulixBaseResponse;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:32
 */
public class PostDevelopOptionsSwitchResponse extends EulixBaseResponse implements EulixKeep, Serializable {
    @SerializedName("results")
    private Boolean results;

    public Boolean getResults() {
        return results;
    }

    public void setResults(Boolean results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "PostDevelopOptionsSwitchResponse{" +
                "results=" + results +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
