package xyz.eulix.space.did;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.HttpUrl;
import xyz.eulix.space.did.bean.DIDDocument;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.util.BouncyCastleProviderUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StringUtil;

public class DIDUtils {
    public static String generateDIDString(byte[] versionStream, String publicKey, boolean isWrapped) {
        String didString = null;
        String wrapPublicKey = (isWrapped ? publicKey : StringUtil.wrapPublicKey(publicKey));
        if (wrapPublicKey != null) {
            byte[] publicKeyStream = BouncyCastleProviderUtil.getSHA3256Stream(wrapPublicKey);
            int length = 0;
            int versionLength = 0;
            if (versionStream != null) {
                versionLength = versionStream.length;
                length = versionLength;
            }
            if (publicKeyStream != null) {
                length += Math.max(Math.min(publicKeyStream.length, 8), 0);
            }
            if (length > 0) {
                byte[] data = new byte[length];
                for (int i = 0; i < length; i++) {
                    if (i < versionLength) {
                        data[i] = versionStream[i];
                    } else {
                        data[i] = publicKeyStream[(i - versionLength)];
                    }
                }
                byte[] checkSum = BouncyCastleProviderUtil.getSHA3256Stream(data);
                int checkDataLength = length;
                int checkSumLength = 0;
                if (checkSum != null) {
                    checkSumLength = Math.max(Math.min(checkSum.length, 4), 0);
                    checkDataLength += checkSumLength;
                }
                byte[] checkData = (checkSumLength > 0 ? new byte[checkDataLength] : data);
                if (checkSumLength > 0) {
                    for (int j = 0; j < checkDataLength; j++) {
                        if (j < length) {
                            checkData[j] = data[j];
                        } else {
                            checkData[j] = checkSum[(j - length)];
                        }
                    }
                }
                didString = StringUtil.base64Encode(checkData, StandardCharsets.UTF_8);
            }
        }
        return didString;
    }

    public static VerificationMethod generateDIDVerificationMethod(String idPrefix, byte[] version, Map<String, String> queryMap, String publicKeyMultiBase, String publicKeyPem, String type) {
        VerificationMethod verificationMethod = null;
        String didString = generateDIDString(version, publicKeyPem, true);
        if (didString != null) {
            verificationMethod = new VerificationMethod();
            verificationMethod.setPublicKeyMultiBase(publicKeyMultiBase);
            verificationMethod.setPublicKeyPem(publicKeyPem);
            verificationMethod.setType(type);
            StringBuilder idBuilder = new StringBuilder();
            if (idPrefix != null) {
                idBuilder.append(idPrefix);
            }
            idBuilder.append(didString);
            if (queryMap != null && !queryMap.isEmpty()) {
                HttpUrl httpUrl = null;
                try {
                    httpUrl = HttpUrl.parse(ConstantField.URL.SERVICE_AO_SPACE_URL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (httpUrl != null) {
                    HttpUrl.Builder builder = httpUrl.newBuilder();
                    Set<Map.Entry<String, String>> entrySet = queryMap.entrySet();
                    for (Map.Entry<String, String> entry : entrySet) {
                        if (entry != null) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (key != null && value != null) {
                                builder.addQueryParameter(key, value);
                            }
                        }
                    }
                    httpUrl = builder.build();
                    String query = httpUrl.query();
                    if (query != null && StringUtil.isNonBlankString(query)) {
                        if (!query.startsWith("?")) {
                            idBuilder.append("?");
                        }
                        idBuilder.append(query);
                    }
                }
            }
            verificationMethod.setId(idBuilder.toString());
        }
        return verificationMethod;
    }

    public static DIDDocument parseDIDDoc(String didDoc) {
        DIDDocument didDocument = null;
        if (didDoc != null) {
            String didDocDecode = StringUtil.base64Decode(didDoc, StandardCharsets.UTF_8);
            if (didDocDecode != null) {
                try {
                    didDocument = new Gson().fromJson(didDocDecode, DIDDocument.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return didDocument;
    }

    public static Map<String, String> getDIDQueryMap(String didString) {
        Map<String, String> queryMap = null;
        if (didString != null) {
            int queryIndex = didString.indexOf("?");
            if (queryIndex >= 0 && (queryIndex + 1) < didString.length()) {
                String queryString = didString.substring(queryIndex);
                Uri uri = null;
                try {
                    uri = Uri.parse(ConstantField.URL.SERVICE_AO_SPACE_URL + queryString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (uri != null) {
                    Set<String> keySet = uri.getQueryParameterNames();
                    if (keySet != null && !keySet.isEmpty()) {
                        queryMap = new HashMap<>();
                        for (String key : keySet) {
                            String value = null;
                            if (key != null) {
                                value = uri.getQueryParameter(key);
                            }
                            if (value != null) {
                                queryMap.put(key, value);
                            }
                        }
                    }
                }
            }
        }
        return queryMap;
    }
}
