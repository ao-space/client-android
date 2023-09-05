package xyz.eulix.space.network.agent.bind;

import xyz.eulix.space.interfaces.EulixKeep;

public class PasswordVerifyResult implements EulixKeep {
    protected String agentToken;

    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }
}
