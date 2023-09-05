package xyz.eulix.space.event;

public class SpecificBoxOnlineResponseEvent {
    private String boxUuid;
    private String boxBind;
    private String requestId;
    private Boolean isOnline;

    public SpecificBoxOnlineResponseEvent(String boxUuid, String boxBind, String requestId, Boolean isOnline) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.requestId = requestId;
        this.isOnline = isOnline;
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

    public Boolean getOnline() {
        return isOnline;
    }
}
