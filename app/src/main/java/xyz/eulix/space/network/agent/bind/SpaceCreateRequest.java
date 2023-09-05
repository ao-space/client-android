package xyz.eulix.space.network.agent.bind;

import java.util.List;

import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.interfaces.EulixKeep;

public class SpaceCreateRequest implements EulixKeep {
    private String clientPhoneModel;
    private String clientUuid;
    private Boolean enableInternetAccess;
    private String password;
    private String spaceName;
    private String platformApiBase;
    private List<VerificationMethod> verificationMethod;

    public String getClientPhoneModel() {
        return clientPhoneModel;
    }

    public void setClientPhoneModel(String clientPhoneModel) {
        this.clientPhoneModel = clientPhoneModel;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public Boolean getEnableInternetAccess() {
        return enableInternetAccess;
    }

    public void setEnableInternetAccess(Boolean enableInternetAccess) {
        this.enableInternetAccess = enableInternetAccess;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getPlatformApiBase() {
        return platformApiBase;
    }

    public void setPlatformApiBase(String platformApiBase) {
        this.platformApiBase = platformApiBase;
    }

    public List<VerificationMethod> getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(List<VerificationMethod> verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    @Override
    public String toString() {
        return "SpaceCreateRequest{" +
                "clientPhoneModel='" + clientPhoneModel + '\'' +
                ", clientUuid='" + clientUuid + '\'' +
                ", enableInternetAccess=" + enableInternetAccess +
                ", password='" + password + '\'' +
                ", spaceName='" + spaceName + '\'' +
                ", platformApiBase='" + platformApiBase + '\'' +
                ", verificationMethod=" + verificationMethod +
                '}';
    }
}
