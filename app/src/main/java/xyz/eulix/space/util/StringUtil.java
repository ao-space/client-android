/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.util;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;

/**
 * @author: chenjiawei
 * date: 2021/6/17 15:06
 */
public class StringUtil {
    private static final String WRAP_BEGIN_PUBLIC_KEY = "BEGIN PUBLIC KEY";
    private static final String WRAP_END_PUBLIC_KEY = "END PUBLIC KEY";
    private static final String WRAP_BEGIN_PRIVATE_KEY = "BEGIN PRIVATE KEY";
    private static final String WRAP_END_PRIVATE_KEY = "END PRIVATE KEY";
    private static final String WRAP_BEGIN_RSA_PRIVATE_KEY = "BEGIN RSA PRIVATE KEY";
    private static final String WRAP_END_RSA_PRIVATE_KEY = "END RSA PRIVATE KEY";
    private static final String UUID_SEPARATOR = "-";
    private static final List<String> hexList;

    private StringUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    static {
        hexList = new ArrayList<>();
        hexList.add("0");
        hexList.add("1");
        hexList.add("2");
        hexList.add("3");
        hexList.add("4");
        hexList.add("5");
        hexList.add("6");
        hexList.add("7");
        hexList.add("8");
        hexList.add("9");
        hexList.add("a");
        hexList.add("b");
        hexList.add("c");
        hexList.add("d");
        hexList.add("e");
        hexList.add("f");
    }

    public static boolean compare(String s1, String s2) {
        boolean isEqual = false;
        if (s1 == null && s2 == null) {
            isEqual = true;
        } else if (s1 != null && s2 != null) {
            isEqual = s1.equals(s2);
        }
        return isEqual;
    }

    public static boolean containsBlank(String inValue) {
        boolean isContainsBlank = false;
        if (inValue != null && inValue.length() > 0) {
            isContainsBlank = inValue.contains(" ");
        }
        return isContainsBlank;
    }

    public static String replaceBlank(String inValue) {
        if (inValue != null) {
            inValue = inValue.replace("\t", "");
            inValue = inValue.replace("\r", "");
            inValue = inValue.replace("\n", "");
            inValue = inValue.replace(" ", "");
        }
        return inValue;
    }

    @NonNull
    public static String nullToEmpty(String inValue) {
        return (inValue == null ? "" : inValue);
    }

    public static String trim(String inValue) {
        return (inValue == null ? null : inValue.trim());
    }

    public static boolean checkContentValid(String content) {
        boolean result = false;
        if (content != null) {
            Pattern pattern = Pattern.compile("^[a-zA-Z0-9_\u4e00-\u9fa5]+$");
            Matcher matcher = pattern.matcher(content);
            result = matcher.matches();
        }
        return result;
    }

    public static String stringFilter(String str, String regEx) {
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    public static boolean checkTextMatch(String content, String reg) {
        boolean result = false;
        if (content != null) {
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(content);
            result = matcher.matches();
        }
        return result;
    }

    public static boolean checkCharacterLength(String content, int characterMinLength, int characterMaxLength) {
        boolean result = true;
        if ((characterMinLength >= 0 || characterMaxLength >= 0) && !(characterMaxLength >= 0 && characterMinLength > characterMaxLength)) {
            int characterLength = 0;
            if (content != null) {
                int length = content.length();
                for (int i = 0; i < length; i++) {
                    String s = content.substring(i, (i + 1));
                    if (checkTextMatch(s, ConstantField.StringPattern.CHINESE_REG)) {
                        characterLength += 2;
                    } else {
                        characterLength += 1;
                    }
                    if (characterMaxLength >= 0 && characterLength > characterMaxLength) {
                        result = false;
                        break;
                    }
                }
            }
            if (result && characterMinLength >= 0 && characterLength < characterMinLength) {
                result = false;
            }
        }
        return result;
    }

    public static String checkCharacter(String content, String patternMatch, int characterMinLength, int characterMaxLength, String defaultAppend) {
        String result = content;
        if (content != null) {
            int length = content.length();
            if (isNonBlankString(patternMatch)) {
                StringBuilder resultBuilder = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    String s = content.substring(i, (i + 1));
                    if (checkTextMatch(s, patternMatch)) {
                        resultBuilder.append(s);
                    }
                }
                result = resultBuilder.toString();
            }
            length = result.length();
            if ((characterMinLength >= 0 || characterMaxLength >= 0) && !(characterMaxLength >= 0 && characterMinLength > characterMaxLength)) {
                if (characterMinLength >= 0 && length < characterMinLength) {
                    if (defaultAppend != null && !defaultAppend.isEmpty()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(result);
                        while (result.length() < characterMinLength) {
                            stringBuilder.append(defaultAppend);
                            result = stringBuilder.toString();
                        }
                    }
                }
                if (characterMaxLength >= 0 && length > characterMaxLength) {
                    result = result.substring(0, characterMaxLength);
                }
            }
        }
        return result;
    }

    public static boolean isNonBlankString(String inValue) {
        boolean result = false;
        if (inValue != null) {
            result = !inValue.trim().isEmpty();
        }
        return result;
    }

    /**
     * 转换成http URL格式字符串
     * @param inValue
     * @return 返回通过http URL校验的字符串，如果返回为null表明校验不通过
     */
    public static String toHttpUrlString(String inValue) {
        String outValue = inValue;
        if (inValue != null) {
            HttpUrl httpUrl = null;
            try {
                httpUrl = HttpUrl.get(inValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (httpUrl != null) {
                outValue = nullToEmpty(httpUrl.toString());
            } else {
                outValue = null;
            }
        }
        return outValue;
    }

    public static byte[] stringToByteArray(String inValue, Charset charset) {
        byte[] outValue = null;
        if (inValue != null && charset != null) {
            outValue = inValue.getBytes(charset);
        }
        return outValue;
    }

    public static String byteArrayToString(byte[] inValue, Charset charset) {
        String outValue = null;
        if (inValue != null && charset != null) {
            try {
                outValue = new String(inValue, charset);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outValue;
    }

    public static byte[] stringToByteArray(String inValue) {
        byte[] outValue = null;
        if (inValue != null) {
            boolean isSuccess = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    outValue = java.util.Base64.getDecoder().decode(inValue);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!isSuccess) {
                try {
                    outValue = Base64.decode(inValue, Base64.NO_WRAP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return outValue;
    }

    public static String byteToHexString(byte[] inValue) {
        StringBuilder dataBuilder = new StringBuilder();
        if (inValue != null) {
            for (byte element : inValue) {
                String hexStr = Integer.toHexString(element & 0xff);
                if (hexStr.length() == 1) {
                    dataBuilder.append(0);
                }
                dataBuilder.append(hexStr);
            }
        }
        return dataBuilder.toString();
    }

    public static byte[] base64Encode(byte[] inValue) {
        byte[] outValue = null;
        if (inValue != null) {
            boolean isSuccess = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    outValue = java.util.Base64.getEncoder().encode(inValue);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!isSuccess) {
                try {
                    outValue = Base64.encode(inValue, Base64.NO_WRAP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return outValue;
    }

    public static String base64Encode(String inValue, Charset charset) {
        String outValue = null;
        if (inValue != null) {
            byte[] valueOut = base64Encode(stringToByteArray(inValue, charset));
            if (valueOut != null) {
                outValue = byteArrayToString(valueOut, charset);
            }
        }
        return outValue;
    }

    public static String base64Encode(byte[] inValue, Charset charset) {
        String outValue = null;
        if (inValue != null) {
            byte[] valueOut = base64Encode(inValue);
            if (valueOut != null) {
                outValue = byteArrayToString(valueOut, charset);
            }
        }
        return outValue;
    }

    public static String base64EncodeToString(String inValue, Charset charset) {
        String outValue = null;
        if (inValue != null) {
            byte[] valueIn = stringToByteArray(inValue, charset);
            boolean isSuccess = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    outValue = java.util.Base64.getEncoder().encodeToString(valueIn);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!isSuccess) {
                try {
                    outValue = Base64.encodeToString(valueIn, Base64.NO_WRAP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return outValue;
    }

    public static byte[] base64Decode(byte[] inValue) {
        byte[] outValue = null;
        if (inValue != null) {
            boolean isSuccess = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    outValue = java.util.Base64.getDecoder().decode(inValue);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!isSuccess) {
                try {
                    outValue = Base64.decode(inValue, Base64.NO_WRAP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return outValue;
    }

    public static String base64Decode(String inValue, Charset charset) {
        String outValue = null;
        if (inValue != null) {
            byte[] valueOut = base64Decode(stringToByteArray(inValue, charset));
            if (valueOut != null) {
                outValue = byteArrayToString(valueOut, charset);
            }
        }
        return outValue;
    }

    public static String base64Decode(byte[] inValue, Charset charset) {
        String outValue = null;
        if (inValue != null) {
            byte[] valueOut = base64Decode(inValue);
            if (valueOut != null) {
                outValue = byteArrayToString(valueOut, charset);
            }
        }
        return outValue;
    }

    public static String byteArrayToString(byte[] inValue) {
        String outValue = null;
        if (inValue != null) {
            boolean isSuccess = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    outValue = java.util.Base64.getEncoder().encodeToString(inValue);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!isSuccess) {
                try {
                    outValue = Base64.encodeToString(inValue, Base64.NO_WRAP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return outValue;
    }

    public static String urlToHost(String url) {
        String host = url;
        if (url != null) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                host = uri.getHost();
            }
        }
        return host;
    }

    public static String filterNumber(String inValue) {
        String outValue = inValue;
        if (inValue != null) {
            int length = inValue.length();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                char element = inValue.charAt(i);
                if (element >= '0' && element <= '9') {
                    stringBuilder.append(element);
                }
            }
            outValue = stringBuilder.toString();
        }
        return outValue;
    }

    public static int getIntegerInputNumber(String editTextContent, int minValue, int maxValue) {
        int number = 0;
        if (minValue > maxValue) {
            int tempValue = maxValue;
            maxValue = minValue;
            minValue = tempValue;
        }
        if (editTextContent != null) {
            int length = editTextContent.length();
            for (int i = 0; i < length; i++) {
                char element = editTextContent.charAt(i);
                if (element >= '0' && element <= '9') {
                    number = number * 10 + (element - '0');
                    if (number > maxValue) {
                        break;
                    }
                }
            }
        }
        if (number > maxValue) {
            number = (maxValue + 1);
        } else if (number < minValue) {
            number = (minValue - 1);
        }
        return number;
    }

    public static UUID stringToUUID(String inValue) {
        UUID outValue = null;
        if (inValue != null) {
            try {
                outValue = UUID.fromString(inValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outValue;
    }

    public static String unwrapPublicKey(String rawKey) {
        String realKey = rawKey;
        if (rawKey != null) {
            if (realKey.contains(WRAP_BEGIN_PUBLIC_KEY)) {
                int index = realKey.indexOf(WRAP_BEGIN_PUBLIC_KEY);
                if (index >= 0) {
                    int startIndex = index + WRAP_BEGIN_PUBLIC_KEY.length();
                    if (realKey.length() > WRAP_BEGIN_PUBLIC_KEY.length()) {
                        realKey = realKey.substring(startIndex);
                        while (realKey.startsWith("-")) {
                            realKey = realKey.substring(1);
                        }
                    }
                }
            }
            if (realKey.contains(WRAP_END_PUBLIC_KEY)) {
                int index = realKey.lastIndexOf(WRAP_END_PUBLIC_KEY);
                if (index >= 0) {
                    if (realKey.length() > WRAP_END_PUBLIC_KEY.length()) {
                        realKey = realKey.substring(0, index);
                        while (realKey.endsWith("-")) {
                            realKey = realKey.substring(0, (realKey.length() - 1));
                        }
                    }
                }
            }
            realKey = replaceBlank(realKey);
        }
        return realKey;
    }

    public static String wrapPublicKey(String key) {
        String wrapKey = key;
        if (key != null) {
            if (!key.contains("\n")) {
                int length = key.length();
                int segment = length / 64;
                int remainder = length % 64;
                if (remainder > 0) {
                    segment += 1;
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < segment; i++) {
                    stringBuilder.append(key.substring((i * 64), Math.min(((i + 1) * 64), length)));
                    stringBuilder.append("\n");
                }
                wrapKey = stringBuilder.toString();
            }
            if (!key.contains(WRAP_BEGIN_PUBLIC_KEY)) {
                wrapKey = ("-----" + WRAP_BEGIN_PUBLIC_KEY + "-----" + "\n" + wrapKey);
            }
            if (!key.contains(WRAP_END_PUBLIC_KEY)) {
                wrapKey = (wrapKey + "-----" + WRAP_END_PUBLIC_KEY + "-----");
            }
        }
        return wrapKey;
    }

    public static String wrapPublicKeyNewLine(String key) {
        String wrapKey = wrapPublicKey(key);
        if (wrapKey != null && !wrapKey.endsWith("\n")) {
            wrapKey = (wrapKey + "\n");
        }
        return wrapKey;
    }

    public static String wrapPrivateKey(String key) {
        String wrapKey = key;
        if (key != null) {
            if (!key.contains("\n")) {
                int length = key.length();
                int segment = length / 64;
                int remainder = length % 64;
                if (remainder > 0) {
                    segment += 1;
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < segment; i++) {
                    stringBuilder.append(key.substring((i * 64), Math.min(((i + 1) * 64), length)));
                    stringBuilder.append("\n");
                }
                wrapKey = stringBuilder.toString();
            }
            if (!key.contains(WRAP_BEGIN_PRIVATE_KEY)) {
                wrapKey = ("-----" + WRAP_BEGIN_PRIVATE_KEY + "-----" + "\n" + wrapKey);
            }
            if (!key.contains(WRAP_END_PRIVATE_KEY)) {
                wrapKey = (wrapKey + "-----" + WRAP_END_PRIVATE_KEY + "-----");
            }
        }
        return wrapKey;
    }

    public static String wrapRSAPrivateKey(String key) {
        String wrapKey = key;
        if (key != null) {
            if (!key.contains("\n")) {
                int length = key.length();
                int segment = length / 64;
                int remainder = length % 64;
                if (remainder > 0) {
                    segment += 1;
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < segment; i++) {
                    stringBuilder.append(key.substring((i * 64), Math.min(((i + 1) * 64), length)));
                    stringBuilder.append("\n");
                }
                wrapKey = stringBuilder.toString();
            }
            if (!key.contains(WRAP_BEGIN_RSA_PRIVATE_KEY)) {
                wrapKey = ("-----" + WRAP_BEGIN_RSA_PRIVATE_KEY + "-----" + "\n" + wrapKey);
            }
            if (!key.contains(WRAP_END_RSA_PRIVATE_KEY)) {
                wrapKey = (wrapKey + "-----" + WRAP_END_RSA_PRIVATE_KEY + "-----");
            }
        }
        return wrapKey;
    }

    public static Map<String, Object> jsonToMap(String jsonContent) {
        Map<String, Object> result = null;
        if (jsonContent != null && jsonContent.length() > 0) {
            char firstChar = jsonContent.charAt(0);
            result = new HashMap<>();
            try {
                if (firstChar == '[') {
                    JSONArray jsonArray = new JSONArray(jsonContent);
                    int length = jsonArray.length();
                    for (int i = 0; i < length; i++) {
                        Object value = jsonArray.get(i);
                        if (value instanceof JSONArray || value instanceof JSONObject) {
                            result.put(String.valueOf(i), jsonToMap(value.toString()));
                        } else {
                            result.put(String.valueOf(i), value.toString());
                        }
                    }
                } else if (firstChar == '{') {
                    JSONObject jsonObject = new JSONObject(jsonContent);
                    Iterator<String> iterator = jsonObject.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        if (key != null) {
                            Object value = jsonObject.get(key);
                            if (value instanceof JSONArray || value instanceof JSONObject) {
                                result.put(key, jsonToMap(value.toString()));
                            } else {
                                result.put(key, value.toString());
                            }
                        }
                    }
                } else {
                    result = null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                result = null;
            }
        }
        return result;
    }

    @NonNull
    public static String insertApiSeparator(String api) {
        String result = nullToEmpty(api);
        if (!result.startsWith("/")) {
            result = ("/" + result);
        }
        if (!result.endsWith("/")) {
            result = (result + "/");
        }
        return result;
    }

    public static String generateUuid(String id) {
        StringBuilder uuidBuilder = new StringBuilder();
        String[] uuid1 = new String[] {"0", "0", "0", "0", "0", "0", "0", "0"};
        String[] uuid2 = new String[] {"0", "0", "0", "0"};
        String[] uuid3 = new String[] {"0", "0", "0", "0"};
        String[] uuid4 = new String[] {"0", "0", "0", "0"};
        String[] uuid5 = new String[] {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};
        if (id != null) {
            id = id.toLowerCase();
            int size = id.length();
            for (int i = 0; i < size; i++) {
                String element = String.valueOf(id.charAt(i));
                if (!hexList.contains(element)) {
                    element = "0";
                }
                if (i < 8) {
                    uuid1[i] = element;
                } else if (i < 12) {
                    uuid2[(i - 8)] = element;
                } else if (i < 16) {
                    uuid3[(i - 12)] = element;
                } else if (i < 20) {
                    uuid4[(i - 16)] = element;
                } else if (i < 32) {
                    uuid5[(i - 20)] = element;
                } else {
                    break;
                }
            }
        }
        for (String id1 : uuid1) {
            uuidBuilder.append(id1);
        }
        uuidBuilder.append(UUID_SEPARATOR);
        for (String id2 : uuid2) {
            uuidBuilder.append(id2);
        }
        uuidBuilder.append(UUID_SEPARATOR);
        for (String id3 : uuid3) {
            uuidBuilder.append(id3);
        }
        uuidBuilder.append(UUID_SEPARATOR);
        for (String id4 : uuid4) {
            uuidBuilder.append(id4);
        }
        uuidBuilder.append(UUID_SEPARATOR);
        for (String id5 : uuid5) {
            uuidBuilder.append(id5);
        }
        return uuidBuilder.toString();
    }

    public static String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    public static String generateHost(String urlStr) {
        String host = urlStr;
        if (urlStr != null) {
            int totalLength = urlStr.length();
            String prefix = "://";
            int prefixIndex = urlStr.indexOf(prefix);
            int startIndex = (prefixIndex + prefix.length());
            if (prefixIndex >= 0 && startIndex <= totalLength) {
                if (startIndex == totalLength) {
                    host = "";
                } else {
                    String nUrlStr = urlStr.substring(startIndex);
                    host = nUrlStr;
                    String suffix = "/";
                    int suffixIndex = nUrlStr.indexOf(suffix);
                    if (suffixIndex >= 0) {
                        if (suffixIndex == 0) {
                            host = "";
                        } else {
                            host = nUrlStr.substring(0, suffixIndex);
                        }
                    }
                }
            }
        }
        return host;
    }

    public static String getCustomizeSecret32(String secret) {
        String customizeSecret = null;
        if (secret != null) {
            int hashCodeV = Math.abs(secret.hashCode());
            // d 代表参数为正数型
            customizeSecret = String.format("%032d", hashCodeV);
        }
        return customizeSecret;
    }

    @NonNull
    public static byte[] getRandom(int length) {
        length = Math.max(length, 1);
        char[] toBase64 = {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
        };
        byte[] randomStream = new byte[length];
        for (int i = 0; i < length; i++) {
            int randomIndex = Math.max(Math.min((int) (Math.floor(Math.random() * 64)), toBase64.length), 0);
            randomStream[i] = (byte) (toBase64[randomIndex] & 0xFF);
        }
        return randomStream;
    }
}
