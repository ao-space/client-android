package xyz.eulix.space.did.event;

public class DIDDocumentRequestEvent {
    private String boxUuid;
    private String boxBind;
    private String requestUuid;

    public DIDDocumentRequestEvent(String boxUuid, String boxBind) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
    }

    public DIDDocumentRequestEvent(String boxUuid, String boxBind, String requestUuid) {
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
