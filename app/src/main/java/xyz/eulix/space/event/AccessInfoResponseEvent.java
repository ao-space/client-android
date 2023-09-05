package xyz.eulix.space.event;

import xyz.eulix.space.network.net.InternetServiceConfigResult;

public class AccessInfoResponseEvent {
    private InternetServiceConfigResult internetServiceConfigResult;
    private String requestUuid;

    public AccessInfoResponseEvent(InternetServiceConfigResult internetServiceConfigResult, String requestUuid) {
        this.internetServiceConfigResult = internetServiceConfigResult;
        this.requestUuid = requestUuid;
    }

    public InternetServiceConfigResult getInternetServiceConfigResult() {
        return internetServiceConfigResult;
    }

    public String getRequestUuid() {
        return requestUuid;
    }
}
