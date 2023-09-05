package xyz.eulix.space.network.net;

import xyz.eulix.space.interfaces.EulixKeep;

public class ChannelInfoResult implements EulixKeep {
    private Boolean lan;
    private Boolean p2p;
    private Boolean wan;
    private String userDomain;

    public Boolean getLan() {
        return lan;
    }

    public void setLan(Boolean lan) {
        this.lan = lan;
    }

    public Boolean getP2p() {
        return p2p;
    }

    public void setP2p(Boolean p2p) {
        this.p2p = p2p;
    }

    public Boolean getWan() {
        return wan;
    }

    public void setWan(Boolean wan) {
        this.wan = wan;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    @Override
    public String toString() {
        return "ChannelInfoResult{" +
                "lan=" + lan +
                ", p2p=" + p2p +
                ", wan=" + wan +
                ", userDomain='" + userDomain + '\'' +
                '}';
    }
}
