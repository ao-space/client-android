package xyz.eulix.space.network.net;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.EulixBaseResponse;

public class InternetServiceConfigResponse extends EulixBaseResponse implements EulixKeep {
    private InternetServiceConfigResult results;

    public InternetServiceConfigResult getResults() {
        return results;
    }

    public void setResults(InternetServiceConfigResult results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "InternetServiceConfigResponse{" +
                "results=" + results +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
