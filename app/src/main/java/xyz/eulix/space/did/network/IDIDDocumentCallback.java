package xyz.eulix.space.did.network;

public interface IDIDDocumentCallback {
    void onResponse(DIDDocumentResponse response);
    void onError(String errMsg);
}
