package xyz.eulix.space.did.bean;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.util.BooleanUtil;
import xyz.eulix.space.util.StringUtil;

public class DIDCredentialBean implements EulixKeep {
    private BinderCredential binderCredential;
    private PasswordCredential passwordCredential;

    public BinderCredential getBinderCredential() {
        return binderCredential;
    }

    public void setBinderCredential(BinderCredential binderCredential) {
        this.binderCredential = binderCredential;
    }

    public PasswordCredential getPasswordCredential() {
        return passwordCredential;
    }

    public void setPasswordCredential(PasswordCredential passwordCredential) {
        this.passwordCredential = passwordCredential;
    }

    public static class BinderCredential implements EulixKeep{
        private String binderPublicKey;
        private String binderPrivateKey;

        public static boolean compare(BinderCredential binderCredential1, BinderCredential binderCredential2) {
            boolean isEqual = false;
            if (binderCredential1 == null && binderCredential2 == null) {
                isEqual = true;
            } else if (binderCredential1 != null && binderCredential2 != null) {
                isEqual = (StringUtil.compare(binderCredential1.binderPublicKey, binderCredential2.binderPublicKey)
                        && StringUtil.compare(binderCredential1.binderPrivateKey, binderCredential2.binderPrivateKey));
            }
            return isEqual;
        }

        public String getBinderPublicKey() {
            return binderPublicKey;
        }

        public void setBinderPublicKey(String binderPublicKey) {
            this.binderPublicKey = binderPublicKey;
        }

        public String getBinderPrivateKey() {
            return binderPrivateKey;
        }

        public void setBinderPrivateKey(String binderPrivateKey) {
            this.binderPrivateKey = binderPrivateKey;
        }

        @Override
        public String toString() {
            return "BinderCredential{" +
                    "binderPublicKey='" + binderPublicKey + '\'' +
                    ", binderPrivateKey='" + binderPrivateKey + '\'' +
                    '}';
        }
    }

    @Deprecated
    public static class PasswordCredential implements EulixKeep{
        private boolean isEncryptPrivateKey;
        private String passwordPublicKey;
        private String passwordIv;
        private String passwordPrivateKey;

        public static boolean compare(PasswordCredential passwordCredential1, PasswordCredential passwordCredential2) {
            boolean isEqual = false;
            if (passwordCredential1 == null && passwordCredential2 == null) {
                isEqual = true;
            } else if (passwordCredential1 != null && passwordCredential2 != null) {
                isEqual = (BooleanUtil.compare(passwordCredential1.isEncryptPrivateKey, passwordCredential2.isEncryptPrivateKey)
                        && StringUtil.compare(passwordCredential1.passwordPublicKey, passwordCredential2.passwordPublicKey)
                        && StringUtil.compare(passwordCredential1.passwordIv, passwordCredential2.passwordIv)
                        && StringUtil.compare(passwordCredential1.passwordPrivateKey, passwordCredential2.passwordPrivateKey));
            }
            return isEqual;
        }

        public boolean isEncryptPrivateKey() {
            return isEncryptPrivateKey;
        }

        public void setEncryptPrivateKey(boolean encryptPrivateKey) {
            isEncryptPrivateKey = encryptPrivateKey;
        }

        public String getPasswordPublicKey() {
            return passwordPublicKey;
        }

        public void setPasswordPublicKey(String passwordPublicKey) {
            this.passwordPublicKey = passwordPublicKey;
        }

        public String getPasswordIv() {
            return passwordIv;
        }

        public void setPasswordIv(String passwordIv) {
            this.passwordIv = passwordIv;
        }

        public String getPasswordPrivateKey() {
            return passwordPrivateKey;
        }

        public void setPasswordPrivateKey(String passwordPrivateKey) {
            this.passwordPrivateKey = passwordPrivateKey;
        }

        @Override
        public String toString() {
            return "PasswordCredential{" +
                    "isEncryptPrivateKey=" + isEncryptPrivateKey +
                    ", passwordPublicKey='" + passwordPublicKey + '\'' +
                    ", passwordIv='" + passwordIv + '\'' +
                    ", passwordPrivateKey='" + passwordPrivateKey + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DIDCredentialBean{" +
                "binderCredential=" + binderCredential +
                ", passwordCredential=" + passwordCredential +
                '}';
    }
}
