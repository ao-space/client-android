package xyz.eulix.space.event;

public class SpaceValidEvent {
    private String boxUuid;
    private String boxBind;
    private boolean isValid;

    public SpaceValidEvent(String boxUuid, String boxBind, boolean isValid) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.isValid = isValid;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public boolean isValid() {
        return isValid;
    }
}
