package xyz.eulix.space.did.network;

public interface DIDDocumentCallback {
    void onSuccess(int code, String source, String message, String requestId, DIDDocumentResult result);
    void onFail(int code, String source, String message, String requestId);
    void onError(String errMsg);
}
