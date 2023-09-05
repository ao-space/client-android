package xyz.eulix.space.network.net;

import xyz.eulix.space.interfaces.EulixKeep;

public class InternetServiceConfigQuery implements EulixKeep {
    private String clientUUID;

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    @Override
    public String toString() {
        return "InternetServiceConfigQuery{" +
                "clientUUID='" + clientUUID + '\'' +
                '}';
    }
}
