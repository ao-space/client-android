package xyz.eulix.space.did.network;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.EulixBaseResponse;

public class DIDDocumentResponse extends EulixBaseResponse implements EulixKeep {
    private DIDDocumentResult results;

    public DIDDocumentResult getResults() {
        return results;
    }

    public void setResults(DIDDocumentResult results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "DIDDocumentResponse{" +
                "results=" + results +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
