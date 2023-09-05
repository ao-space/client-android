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

package xyz.eulix.space.transfer.net;

import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.network.files.UploadResponseBodyResult;
import xyz.eulix.space.network.gateway.RealCallResult;
import xyz.eulix.space.transfer.TransferProgressListener;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 传输网络工具类，采用HttpURLConnection，上传可计算进度
 * History:     2021/8/27
 */
public class TransferNetUtil {
    //连接超时时间 ms
    private static int TIME_OUT_MILLISECOND = 10 * 60 * 1000;
    private static String Content_Length = "Content-Length: ";

    private TransferNetUtil() {
        throw new IllegalStateException("Utility class");
    }

    public interface TransferNetP2PUploadCallback {
        void onResult(String resultResponse);
        void onError(String errMsg);
    }

    private static String generateHost(String urlStr) {
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

    public static String postFile(String urlStr, String fileName, File file, String accessToken, String callRequest, String requestId, String secret, String transformation, String ivParams, TransferProgressListener listener) {
        Logger.d("GarveyP2P", "http upload file start");
        String resultResponseStr = null;
        String dash = "--";
        String boundary = UUID.randomUUID().toString();
        String newLine = "\r\n";
        HttpURLConnection connection = null;

        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null) {
            return resultResponseStr;
        }
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (connection == null) {
            return resultResponseStr;
        }
        connection.setConnectTimeout(TIME_OUT_MILLISECOND);
        connection.setReadTimeout(TIME_OUT_MILLISECOND);
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        //accessToken
        String accessTokenHeader = dash + boundary + newLine +
                "Content-Disposition: form-data; name=\"" + "accessToken" + "\"; filename=\"accessToken\"" + newLine +
                "Content-Type: text/plain; charset=utf-8" + newLine +
                Content_Length + accessToken.length() + newLine +
                newLine;// important !

        //callRequest
        String callRequestHeader = dash + boundary + newLine +
                "Content-Disposition: form-data; name=\"" + "callRequest" + "\"; filename=\"callRequest\"" + newLine +
                "Content-Type: application/json; charset=utf-8" + newLine +
                Content_Length + callRequest.length() + newLine +
                newLine;// important !

        // file data
        long fileLength = file.length();
        String fileHeader = dash + boundary + newLine +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + newLine +
                "Content-Type:application/octet-stream" + newLine +
                Content_Length + fileLength + newLine +
                newLine;// important !

        //计算输出流总大小
        long fixedLength = accessTokenHeader.length() + accessToken.length() + newLine.length()
                + callRequestHeader.length() + callRequest.length() + newLine.length()
                + fileHeader.length() + fileLength + newLine.length() + (dash + boundary + dash + newLine).length();
        Logger.d("zfy", "fixedLength = " + fixedLength);

        connection.setRequestProperty("request-id", requestId);
        connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Accept", "*/*");
        String cookies = null;
        try {
            cookies = connection.getRequestProperty(ConstantField.CookieHeader.COOKIE_HEADER_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String clientUuidCookie = (ConstantField.CookieHeader.CookieName.CLIENT_UUID + ConstantField.CookieHeader.COOKIE_ASSIGN
                + DataUtil.getClientUuid(EulixSpaceApplication.getContext()));
        if (cookies == null) {
            cookies = clientUuidCookie;
        } else {
            cookies = cookies + ConstantField.CookieHeader.COOKIE_SPLIT + clientUuidCookie;
        }
        connection.setRequestProperty(ConstantField.CookieHeader.COOKIE_HEADER_NAME, cookies);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        //禁用缓存，设置传输大小，可获取到更准确的速度
        connection.setUseCaches(false);
        connection.setFixedLengthStreamingMode((int) fixedLength);
        connection.setAllowUserInteraction(false);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
             FileInputStream inputStream = new FileInputStream(file);) {

            outputStream.writeBytes(accessTokenHeader);
            outputStream.writeBytes(accessToken);
            outputStream.writeBytes(newLine);


            outputStream.writeBytes(callRequestHeader);
            outputStream.writeBytes(callRequest);
            outputStream.writeBytes(newLine);

            // file data
            outputStream.writeBytes(fileHeader);
            Logger.d("zfy", "totalSize=" + fileLength);
            long currentSize = 0L;
            byte[] buffer = new byte[2048];
            int count;
            int oldPercent = 0; //上次进度
            int currentPercent;
            boolean isPercentChange = false;
            while (true) {
                count = inputStream.read(buffer);
                if (count == -1) {
                    break;
                }
                currentSize += count;
                if (listener != null && fileLength > 0) {
                    currentPercent = (int) (currentSize * 100 / fileLength);
                    //进度有变化时再回调，减少回调次数
                    if (currentPercent > oldPercent) {
//                        Logger.d("zfy", "currentSize = " + currentSize);
                        isPercentChange = true;
                        oldPercent = currentPercent;
                    } else {
                        isPercentChange = false;
                    }
                    listener.onProgress(currentSize, fileLength, count, isPercentChange, false);

                }
                outputStream.write(buffer, 0, count);
            }

//            TaskSpeed.getInstance().removeTask(uniqueTag);

            outputStream.writeBytes(newLine);// important !
            outputStream.writeBytes(dash + boundary + dash + newLine);
            outputStream.flush();
            int statusCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();

            Logger.d("zfy", "transformNetUtil response=" + statusCode);
            //解析返回数据
            if (statusCode >= 200 && statusCode < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                String cipherResponseText = response.toString();
                Logger.d("zfy", "cipherResponseText" + cipherResponseText);
                if (!TextUtils.isEmpty(cipherResponseText)) {
                    RealCallResult realCallResult = null;
                    realCallResult = new Gson().fromJson(cipherResponseText, RealCallResult.class);
                    if (realCallResult != null) {
                        String responseRequestId = realCallResult.getRequestId();
                        String body = realCallResult.getBody();
                        if (body != null) {
                            String decryptBody = EncryptionUtil.decrypt(transformation, null,
                                    body, secret, StandardCharsets.UTF_8, ivParams);
                            Logger.d("zfy", "decryptBody = " + decryptBody);
                            resultResponseStr = decryptBody;
                        }
                    }
                }
            } else {
                UploadResponseBodyResult responseBodyResult = new UploadResponseBodyResult();
                responseBodyResult.setCode(statusCode + "");
                responseBodyResult.setMessage(connection.getResponseMessage());
                resultResponseStr = new Gson().toJson(responseBodyResult, UploadResponseBodyResult.class);
            }
        } catch (IOException e) {
            Logger.d("zfy", "transformNetUtil exception");
            e.printStackTrace();
        }
        Logger.d("zfy", "close connect");
        connection.disconnect();

        Logger.d("GarveyP2P", "http upload file end");
        return resultResponseStr;
    }
}
