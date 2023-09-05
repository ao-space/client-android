package xyz.eulix.space.did.bean;

import xyz.eulix.space.interfaces.EulixKeep;

public class CredentialInformationBean {
    public static final int CREDENTIAL_TYPE_AO_SPACE_SERVER = 1;
    public static final int CREDENTIAL_TYPE_BIND_PHONE = CREDENTIAL_TYPE_AO_SPACE_SERVER + 1;
    public static final int CREDENTIAL_TYPE_SECURITY_PASSWORD = CREDENTIAL_TYPE_BIND_PHONE + 1;
    public static final int CREDENTIAL_TYPE_AUTHORIZE_PHONE = CREDENTIAL_TYPE_SECURITY_PASSWORD + 1;
    public static final int CREDENTIAL_TYPE_FRIEND = CREDENTIAL_TYPE_AUTHORIZE_PHONE + 1;
    private int credentialType;
    private String publicKey;
    private StorageLocation storageLocation;
    private Long lastUpdateTimestamp;

    public CredentialInformationBean() {
    }

    public CredentialInformationBean(int credentialType, String publicKey, StorageLocation storageLocation, Long lastUpdateTimestamp) {
        this.credentialType = credentialType;
        this.publicKey = publicKey;
        this.storageLocation = storageLocation;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public CredentialInformationBean cloneSelf() {
        return new CredentialInformationBean(credentialType, publicKey, storageLocation, lastUpdateTimestamp);
    }

    public int getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(int credentialType) {
        this.credentialType = credentialType;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public Long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(Long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public static class StorageLocation implements EulixKeep {
        public static final int LOCATION_SERVER = 1;
        public static final int LOCATION_CLIENT_BINDER = LOCATION_SERVER + 1;
        private Integer locationIndex;
        private String phoneModel;
        private String nickname;
        private String avatarPath;

        public Integer getLocationIndex() {
            return locationIndex;
        }

        public void setLocationIndex(Integer locationIndex) {
            this.locationIndex = locationIndex;
        }

        public String getPhoneModel() {
            return phoneModel;
        }

        public void setPhoneModel(String phoneModel) {
            this.phoneModel = phoneModel;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatarPath() {
            return avatarPath;
        }

        public void setAvatarPath(String avatarPath) {
            this.avatarPath = avatarPath;
        }

        @Override
        public String toString() {
            return "StorageLocation{" +
                    "locationIndex=" + locationIndex +
                    ", phoneModel='" + phoneModel + '\'' +
                    ", nickname='" + nickname + '\'' +
                    ", avatarPath='" + avatarPath + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CredentialInformationBean{" +
                "credentialType=" + credentialType +
                ", publicKey='" + publicKey + '\'' +
                ", storageLocation=" + storageLocation +
                ", lastUpdateTimestamp=" + lastUpdateTimestamp +
                '}';
    }
}
