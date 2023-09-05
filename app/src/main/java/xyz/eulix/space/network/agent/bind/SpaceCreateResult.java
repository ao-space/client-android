package xyz.eulix.space.network.agent.bind;

import java.util.List;

import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.interfaces.EulixKeep;

public class SpaceCreateResult implements EulixKeep {
    private String agentToken;
    private List<ConnectedNetwork> connectedNetwork;
    private Boolean enableInternetAccess;
    // Base64
    private String didDoc;
    private String encryptedPriKeyBytes;
    private PairingBoxResults spaceUserInfo;

    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    public List<ConnectedNetwork> getConnectedNetwork() {
        return connectedNetwork;
    }

    public void setConnectedNetwork(List<ConnectedNetwork> connectedNetwork) {
        this.connectedNetwork = connectedNetwork;
    }

    public Boolean getEnableInternetAccess() {
        return enableInternetAccess;
    }

    public void setEnableInternetAccess(Boolean enableInternetAccess) {
        this.enableInternetAccess = enableInternetAccess;
    }

    public String getDidDoc() {
        return didDoc;
    }

    public void setDidDoc(String didDoc) {
        this.didDoc = didDoc;
    }

    public String getEncryptedPriKeyBytes() {
        return encryptedPriKeyBytes;
    }

    public void setEncryptedPriKeyBytes(String encryptedPriKeyBytes) {
        this.encryptedPriKeyBytes = encryptedPriKeyBytes;
    }

    public PairingBoxResults getSpaceUserInfo() {
        return spaceUserInfo;
    }

    public void setSpaceUserInfo(PairingBoxResults spaceUserInfo) {
        this.spaceUserInfo = spaceUserInfo;
    }

    @Override
    public String toString() {
        return "SpaceCreateResult{" +
                "agentToken='" + agentToken + '\'' +
                ", connectedNetwork=" + connectedNetwork +
                ", enableInternetAccess=" + enableInternetAccess +
                ", didDoc='" + didDoc + '\'' +
                ", encryptedPriKeyBytes='" + encryptedPriKeyBytes + '\'' +
                ", spaceUserInfo=" + spaceUserInfo +
                '}';
    }
}
