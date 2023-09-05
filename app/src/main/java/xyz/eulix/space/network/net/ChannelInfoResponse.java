package xyz.eulix.space.network.net;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.EulixBaseResponse;

public class ChannelInfoResponse extends EulixBaseResponse implements EulixKeep {
    private ChannelInfoResult results;

    public ChannelInfoResult getResults() {
        return results;
    }

    public void setResults(ChannelInfoResult results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "ChannelInfoResponse{" +
                "results=" + results +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
