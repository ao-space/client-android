package xyz.eulix.space.network.agent;

public interface AgentEncryptedResultsCallback {
    void onSuccess(int code, String source, String message, String results);
    void onFailed(int code, String source, String message);
    void onError(String errMsg);
}
