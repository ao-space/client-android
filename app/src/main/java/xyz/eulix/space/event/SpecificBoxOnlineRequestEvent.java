package xyz.eulix.space.event;

public class SpecificBoxOnlineRequestEvent {
    private String boxUuid;
    private String boxBind;
    private String requestId;

    public SpecificBoxOnlineRequestEvent(String boxUuid, String boxBind, String requestId) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.requestId = requestId;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public String getRequestId() {
        return requestId;
    }
}
