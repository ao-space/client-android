package xyz.eulix.space.event;

public class AccessInfoRequestEvent {
    private String boxUuid;
    private String boxBind;
    private String requestUuid;

    public AccessInfoRequestEvent(String boxUuid, String boxBind) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
    }

    public AccessInfoRequestEvent(String boxUuid, String boxBind, String requestUuid) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.requestUuid = requestUuid;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public String getRequestUuid() {
        return requestUuid;
    }
}
