package xyz.eulix.space.network.net;

import xyz.eulix.space.interfaces.EulixKeep;

public class InternetServiceConfigRequest implements EulixKeep {
    private String clientUUID;
    private boolean enableInternetAccess;
    private String platformApiBase;

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    public boolean isEnableInternetAccess() {
        return enableInternetAccess;
    }

    public void setEnableInternetAccess(boolean enableInternetAccess) {
        this.enableInternetAccess = enableInternetAccess;
    }

    public String getPlatformApiBase() {
        return platformApiBase;
    }

    public void setPlatformApiBase(String platformApiBase) {
        this.platformApiBase = platformApiBase;
    }

    @Override
    public String toString() {
        return "InternetServiceConfigRequest{" +
                "clientUUID='" + clientUUID + '\'' +
                ", enableInternetAccess=" + enableInternetAccess +
                ", platformApiBase='" + platformApiBase + '\'' +
                '}';
    }
}
