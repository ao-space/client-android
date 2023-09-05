package com.google.zxing.client.android.util;

import com.google.zxing.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataUtil {
    private static Map<String, Result> resultMap;

    static {
        resultMap = new HashMap<>();
    }

    public static Result getResult(String requestId) {
        Result result = null;
        if (requestId != null && resultMap != null && resultMap.containsKey(requestId)) {
            result = resultMap.get(requestId);
            try {
                resultMap.remove(requestId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String setResult(Result result) {
        String requestId = null;
        if (result != null && resultMap != null) {
            do {
                requestId = UUID.randomUUID().toString();
            } while (resultMap.containsKey(requestId));
            resultMap.put(requestId, result);
        }
        return requestId;
    }
}
