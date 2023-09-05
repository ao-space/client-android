package xyz.eulix.space.did.bean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

public class VerificationMethod implements Serializable, EulixKeep {
    public static final String KEY_ID = "id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_CONTROLLER = "controller";
    public static final String KEY_PUBLIC_KEY_MULTI_BASE = "publicKeyMultibase";
    public static final String KEY_PUBLIC_KEY_PEM = "publicKeyPem";
    public static final String KEY_CONDITION_OR = "conditionOr";
    public static final String KEY_CONDITION_AND = "conditionAnd";

    public static final String QUERY_VERSION_TIME = "versionTime";
    public static final String QUERY_CREDENTIAL_TYPE = "credentialType";
    public static final String QUERY_DEVICE_NAME = "deviceName";
    public static final String QUERY_CLIENT_UUID = "clientUUID";

    public static final String CREDENTIAL_TYPE_DEVICE = "device";
    public static final String CREDENTIAL_TYPE_BINDER = "binder";
    public static final String CREDENTIAL_TYPE_PASSWORD = "password";
    public static final String CREDENTIAL_TYPE_PASSWORD_ON_DEVICE = "passwordondevice";
    public static final String CREDENTIAL_TYPE_PASSWORD_ON_BINDER = "passwordonbinder";

    public static final String DID_AO_SPACE_KEY_PREFIX = "did:aospacekey:";
    public static final String TYPE_RSA_VERIFICATION_KEY_2018 = "RsaVerificationKey2018";

    @SerializedName("id")
    private String id;
    @SerializedName("type")
    private String type;
    @SerializedName("controller")
    private String controller;
    @SerializedName("publicKeyMultibase")
    private String publicKeyMultiBase;
    @SerializedName("publicKeyPem")
    private String publicKeyPem;
    @SerializedName("conditionOr")
    private List<Object> conditionOr;
    @SerializedName("conditionAnd")
    private List<Object> conditionAnd;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getPublicKeyMultiBase() {
        return publicKeyMultiBase;
    }

    public void setPublicKeyMultiBase(String publicKeyMultiBase) {
        this.publicKeyMultiBase = publicKeyMultiBase;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public List<Object> getConditionOr() {
        return conditionOr;
    }

    public void setConditionOr(List<Object> conditionOr) {
        this.conditionOr = conditionOr;
    }

    public List<Object> getConditionAnd() {
        return conditionAnd;
    }

    public void setConditionAnd(List<Object> conditionAnd) {
        this.conditionAnd = conditionAnd;
    }

    @Override
    public String toString() {
        return "VerificationMethod{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", controller='" + controller + '\'' +
                ", publicKeyMultiBase='" + publicKeyMultiBase + '\'' +
                ", publicKeyPem='" + publicKeyPem + '\'' +
                ", conditionOr=" + conditionOr +
                ", conditionAnd=" + conditionAnd +
                '}';
    }
}
