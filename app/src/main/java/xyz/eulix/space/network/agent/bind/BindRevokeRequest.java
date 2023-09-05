package xyz.eulix.space.network.agent.bind;

public class BindRevokeRequest extends PasswordVerifyRequest {
    private String clientUuid;

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }
}
