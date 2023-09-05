package xyz.eulix.space.network.agent.bind;

import xyz.eulix.space.interfaces.EulixKeep;

public class ConnectedNetwork implements EulixKeep {
    private String ip;
    private Integer port;
    private Integer tlsPort;
    private String wifiName;
    private Boolean wire;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(Integer tlsPort) {
        this.tlsPort = tlsPort;
    }

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }

    public Boolean getWire() {
        return wire;
    }

    public void setWire(Boolean wire) {
        this.wire = wire;
    }

    public String generateIpAddressUrl() {
        StringBuilder ipAddressUrlBuilder = null;
        if (ip != null) {
            ipAddressUrlBuilder = new StringBuilder();
            boolean isIpv6 = false;
            ipAddressUrlBuilder.append("http://");
            if (ip.contains(":")) {
                isIpv6 = true;
            }
            if (isIpv6) {
                ipAddressUrlBuilder.append("[");
            }
            ipAddressUrlBuilder.append(ip);
            if (isIpv6) {
                ipAddressUrlBuilder.append("]");
            }
            if (port != null) {
                ipAddressUrlBuilder.append(":");
                ipAddressUrlBuilder.append(port);
            }
            ipAddressUrlBuilder.append("/");
        }
        return (ipAddressUrlBuilder == null ? null : ipAddressUrlBuilder.toString());
    }

    @Override
    public String toString() {
        return "ConnectedNetwork{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", tlsPort=" + tlsPort +
                ", wifiName='" + wifiName + '\'' +
                ", wire=" + wire +
                '}';
    }
}
