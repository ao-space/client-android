package xyz.eulix.space.did.bean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

public class DIDDocument implements Serializable, EulixKeep {
    public static final String KEY_CONTEXT = "@context";
    public static final String KEY_ID = "id";
    public static final String KEY_VERIFICATION_METHOD = "verificationMethod";
    public static final String KEY_CAPABILITY_INVOCATION = "capabilityInvocation";

    @SerializedName("@context")
    private List<String> context;
    @SerializedName("id")
    private String id;
    @SerializedName("verificationMethod")
    private List<VerificationMethod> verificationMethods;
    @SerializedName("capabilityInvocation")
    private List<String> capabilityInvocation;

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<VerificationMethod> getVerificationMethods() {
        return verificationMethods;
    }

    public void setVerificationMethods(List<VerificationMethod> verificationMethods) {
        this.verificationMethods = verificationMethods;
    }

    public List<String> getCapabilityInvocation() {
        return capabilityInvocation;
    }

    public void setCapabilityInvocation(List<String> capabilityInvocation) {
        this.capabilityInvocation = capabilityInvocation;
    }

    @Override
    public String toString() {
        return "DIDDocument{" +
                "context=" + context +
                ", id='" + id + '\'' +
                ", verificationMethods=" + verificationMethods +
                ", capabilityInvocation=" + capabilityInvocation +
                '}';
    }
}
