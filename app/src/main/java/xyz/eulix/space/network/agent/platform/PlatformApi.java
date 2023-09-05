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
public class PlatformApi implements Serializable, EulixKeep {
    public static final String TYPE_BASIC_API = "basic_api";
    public static final String TYPE_EXTENSION_API = "extension_api";
    public static final String TYPE_PRODUCT_SERVICE_API = "product_service_api";

    @SerializedName("method")
    private String method;
    @SerializedName("uri")
    private String uri;
    @SerializedName("briefUri")
    private String briefUri;
    @SerializedName("compatibleVersions")
    private List<Integer> compatibleVersions;
    @SerializedName("type")
    private String type;
    @SerializedName("desc")
    private String desc;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getBriefUri() {
        return briefUri;
    }

    public void setBriefUri(String briefUri) {
        this.briefUri = briefUri;
    }

    public List<Integer> getCompatibleVersions() {
        return compatibleVersions;
    }

    public void setCompatibleVersions(List<Integer> compatibleVersions) {
        this.compatibleVersions = compatibleVersions;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "PlatformApi{" +
                "method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", briefUri='" + briefUri + '\'' +
                ", compatibleVersions=" + compatibleVersions +
                ", type='" + type + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }
}
