package xyz.eulix.space.did.bean;

import xyz.eulix.space.interfaces.EulixKeep;

public class DIDReserveBean implements EulixKeep {
    private String passwordEncryptedPriKeyBytes;

    public String getPasswordEncryptedPriKeyBytes() {
        return passwordEncryptedPriKeyBytes;
    }

    public void setPasswordEncryptedPriKeyBytes(String passwordEncryptedPriKeyBytes) {
        this.passwordEncryptedPriKeyBytes = passwordEncryptedPriKeyBytes;
    }

    @Override
    public String toString() {
        return "DIDReserveBean{" +
                "passwordEncryptedPriKeyBytes='" + passwordEncryptedPriKeyBytes + '\'' +
                '}';
    }
}
