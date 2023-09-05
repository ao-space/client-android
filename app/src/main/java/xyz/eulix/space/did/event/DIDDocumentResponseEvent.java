package xyz.eulix.space.did.event;

import xyz.eulix.space.did.network.DIDDocumentResult;

public class DIDDocumentResponseEvent {
    private DIDDocumentResult didDocumentResult;
    private String requestUuid;

    public DIDDocumentResponseEvent(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public DIDDocumentResponseEvent(DIDDocumentResult didDocumentResult, String requestUuid) {
        this.didDocumentResult = didDocumentResult;
        this.requestUuid = requestUuid;
    }

    public DIDDocumentResult getDidDocumentResult() {
        return didDocumentResult;
    }

    public String getRequestUuid() {
        return requestUuid;
    }
}
