package xyz.eulix.space.event;

public class SpaceOnlineCallbackEvent {
    private String boxUuid;
    private String boxBind;
    private boolean isOnline;

    public SpaceOnlineCallbackEvent(String boxUuid, String boxBind, boolean isOnline) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.isOnline = isOnline;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public boolean isOnline() {
        return isOnline;
    }
}
