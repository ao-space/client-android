package xyz.eulix.space.did.bean;

import xyz.eulix.space.interfaces.EulixKeep;

public class DIDProviderBean implements EulixKeep {
    private String boxUuid;
    private String boxBind;
    private String aoId;
    private String didDoc;
    private String didDocDecode;
    private long timestamp;

    public DIDProviderBean(String boxUuid, String boxBind) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public void setBoxBind(String boxBind) {
        this.boxBind = boxBind;
    }

    public String getAoId() {
        return aoId;
    }

    public void setAoId(String aoId) {
        this.aoId = aoId;
    }

    public String getDidDoc() {
        return didDoc;
    }

    public void setDidDoc(String didDoc) {
        this.didDoc = didDoc;
    }

    public String getDidDocDecode() {
        return didDocDecode;
    }

    public void setDidDocDecode(String didDocDecode) {
        this.didDocDecode = didDocDecode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DIDProviderBean{" +
                "boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                ", aoId='" + aoId + '\'' +
                ", didDoc='" + didDoc + '\'' +
                ", didDocDecode='" + didDocDecode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
