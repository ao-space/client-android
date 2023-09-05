package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;

public class IPBean implements EulixKeep {
    private String IPV4Address;
    private String IPV6Address;
    private int port;

    public String getIPV4Address() {
        return IPV4Address;
    }

    public void setIPV4Address(String IPV4Address) {
        this.IPV4Address = IPV4Address;
    }

    public String getIPV6Address() {
        return IPV6Address;
    }

    public void setIPV6Address(String IPV6Address) {
        this.IPV6Address = IPV6Address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "IPBean{" +
                "IPV4Address='" + IPV4Address + '\'' +
                ", IPV6Address='" + IPV6Address + '\'' +
                ", port=" + port +
                '}';
    }
}
