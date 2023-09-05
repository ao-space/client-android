package xyz.eulix.space.network.net;

import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.agent.bind.ConnectedNetwork;

public class InternetServiceConfigResult implements EulixKeep {
    private Boolean enableLAN;
    private Boolean enableP2P;
    private Boolean enableInternetAccess;
    private String userDomain;
    private List<ConnectedNetwork> connectedNetwork;
    private String platformApiBase;

    public Boolean getEnableLAN() {
        return enableLAN;
    }

    public void setEnableLAN(Boolean enableLAN) {
        this.enableLAN = enableLAN;
    }

    public Boolean getEnableP2P() {
        return enableP2P;
    }

    public void setEnableP2P(Boolean enableP2P) {
        this.enableP2P = enableP2P;
    }

    public Boolean getEnableInternetAccess() {
        return enableInternetAccess;
    }

    public void setEnableInternetAccess(Boolean enableInternetAccess) {
        this.enableInternetAccess = enableInternetAccess;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public List<ConnectedNetwork> getConnectedNetwork() {
        return connectedNetwork;
    }

    public void setConnectedNetwork(List<ConnectedNetwork> connectedNetwork) {
        this.connectedNetwork = connectedNetwork;
    }

    public String getPlatformApiBase() {
        return platformApiBase;
    }

    public void setPlatformApiBase(String platformApiBase) {
        this.platformApiBase = platformApiBase;
    }

    @Override
    public String toString() {
        return "InternetServiceConfigResult{" +
                "enableLAN=" + enableLAN +
                ", enableP2P=" + enableP2P +
                ", enableInternetAccess=" + enableInternetAccess +
                ", userDomain='" + userDomain + '\'' +
                ", connectedNetwork=" + connectedNetwork +
                ", platformApiBase='" + platformApiBase + '\'' +
                '}';
    }
}
