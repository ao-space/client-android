package xyz.eulix.space.network.agent.bind;

import xyz.eulix.space.interfaces.EulixKeep;

public class PasswordVerifyRequest implements EulixKeep {
    protected String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
