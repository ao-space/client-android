package xyz.eulix.space.network.agent.bind;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.agent.AdminRevokeResult;

public class BindRevokeResult extends PasswordVerifyResult implements EulixKeep {
    private String code;
    private String message;
    private String requestId;
    private AdminRevokeResult results;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public AdminRevokeResult getResults() {
        return results;
    }

    public void setResults(AdminRevokeResult results) {
        this.results = results;
    }
}
