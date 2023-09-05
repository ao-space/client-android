package xyz.eulix.space.did.network;

import xyz.eulix.space.interfaces.EulixKeep;

public class DIDDocumentResult implements EulixKeep {
    private String didDoc;
    private String encryptedPriKeyBytes;

    public String getDidDoc() {
        return didDoc;
    }

    public void setDidDoc(String didDoc) {
        this.didDoc = didDoc;
    }

    public String getEncryptedPriKeyBytes() {
        return encryptedPriKeyBytes;
    }

    public void setEncryptedPriKeyBytes(String encryptedPriKeyBytes) {
        this.encryptedPriKeyBytes = encryptedPriKeyBytes;
    }

    @Override
    public String toString() {
        return "DIDDocumentResult{" +
                "didDoc='" + didDoc + '\'' +
                ", encryptedPriKeyBytes='" + encryptedPriKeyBytes + '\'' +
                '}';
    }
}
