package xyz.eulix.space.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

public class BouncyCastleProviderUtil {
    private static final String TAG = BouncyCastleProviderUtil.class.getSimpleName();

    static {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        int index = Security.addProvider(new BouncyCastleProvider());
        Logger.d(TAG, "add provider: " + BouncyCastleProvider.PROVIDER_NAME + ", index: " + index);
    }

    public static byte[] getSHA3256Stream(byte[] inValue) {
        byte[] outValue = inValue;
        if (inValue != null) {
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA3-256", BouncyCastleProvider.PROVIDER_NAME);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
            if (messageDigest != null) {
                outValue = messageDigest.digest(inValue);
            }
        }
        return outValue;
    }

    public static byte[] getSHA3256Stream(String inValue) {
        byte[] outValue = null;
        if (inValue != null) {
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA3-256", BouncyCastleProvider.PROVIDER_NAME);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
            if (messageDigest != null) {
                outValue = messageDigest.digest(StringUtil.stringToByteArray(inValue, StandardCharsets.UTF_8));
            }
        }
        return outValue;
    }
}
