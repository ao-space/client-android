package xyz.eulix.space.network.net;

import xyz.eulix.space.interfaces.EulixKeep;

public class ChannelWanRequest implements EulixKeep {
    private boolean wan;

    public boolean isWan() {
        return wan;
    }

    public void setWan(boolean wan) {
        this.wan = wan;
    }

    @Override
    public String toString() {
        return "ChannelWanRequest{" +
                "wan=" + wan +
                '}';
    }
}
