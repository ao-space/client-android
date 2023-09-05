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

package xyz.eulix.space.transfer.multipart.lan;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.transfer.multipart.network.MultipartNetworkManger;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 局域网Https工具类
 * History:     2023/2/6
 */
public class LanHttpsUtil {
    private static final String TAG = "zfy";

    /**
     * 创建信任指定证书的OkHttpClien
     *
     * @return
     */
    public static OkHttpClient createTrustCustomOkHttpClient(X509Certificate certificate) {
        X509TrustManager customTrustManager = new CustomTrustManager(certificate);
        return createSSLClient(customTrustManager);
    }


    public static class CustomTrustManager implements X509TrustManager {
        private X509Certificate clientCertificate;

        public CustomTrustManager(X509Certificate certificate) {
            this.clientCertificate = certificate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            if (clientCertificate == null) {
                throw new CertificateException("checkClientTrusted: https lan cert is null");
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            Logger.d("zfy", "checkServerTrusted");
            if (chain == null || chain.length == 0) {
                throw new CertificateException("checkServerTrusted: X509Certificate array is null");
            }

            if (clientCertificate == null) {
                throw new CertificateException("checkServerTrusted: https lan cert is null");
            }
            if (!(null != authType && authType.equals("ECDHE_RSA"))) {
                throw new CertificateException("checkServerTrusted: AuthType is not ECDHE_RSA");
            }
//            //系统默认的验证
//            try {
//                TrustManagerFactory factory = TrustManagerFactory.getInstance("X509");
//                factory.init((KeyStore) null);
//                for (TrustManager trustManager : factory.getTrustManagers()) {
//                    ((X509TrustManager) trustManager).checkServerTrusted(chain, authType);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            //获取本地证书中的信息
            String clientEncoded;//公钥
            String clientPublicKeyEncode = bytes2Hex(clientCertificate.getPublicKey().getEncoded());
            clientEncoded = new BigInteger(1, clientCertificate.getPublicKey().getEncoded()).toString(16);

            //获取网络中的证书信息
            X509Certificate certificate = chain[0];
            PublicKey publicKey = certificate.getPublicKey();
            String serverPublicKeyEncode = bytes2Hex(publicKey.getEncoded());
            String serverEncoded = new BigInteger(1, publicKey.getEncoded()).toString(16);

            if (!clientEncoded.equals(serverEncoded)) {
                //证书验证失败，重新下载
                Logger.d("zfy", "verify cert failed!");
                LanManager.getInstance().resetHttpsCertInfo();
                Logger.d("zfy", "clientPublicKeyEncode=" + clientPublicKeyEncode);
                Logger.d("zfy", "serverPublicKeyEncode=" + serverPublicKeyEncode);
                throw new CertificateException("server's PublicKey is not equals to client's PublicKey");
            }
            Logger.d("zfy", "verify cert pass");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static OkHttpClient createSSLClient(X509TrustManager x509TrustManager) {
        SSLSocketFactory sslSocketFactory = createSSLSocketFactory(x509TrustManager);
        if (sslSocketFactory != null) {
            OkHttpClient.Builder builder = OkHttpUtil.generateOkHttpClient(true).newBuilder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .sslSocketFactory(sslSocketFactory, x509TrustManager)
                    .hostnameVerifier(new CustomHostnameVerifier());
            return builder.build();
        } else {
            return null;
        }
    }

    private static SSLSocketFactory createSSLSocketFactory(TrustManager trustManager) {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssfFactory;
    }

    private static class CustomHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            //域名校验
            Logger.d(TAG, "verify hostname = " + hostname);
            return true;
        }
    }

    private static String bytes2Hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            boolean flag = false;
            if (b < 0) flag = true;
            int absB = Math.abs(b);
            if (flag) absB = absB | 0x80;
//            System.out.println(absB & 0xFF);
            String tmp = Integer.toHexString(absB & 0xFF);
            if (tmp.length() == 1) { //转化的十六进制不足两位，需要补0
                hex.append("0");
            }
            hex.append(tmp.toLowerCase());
        }
        return hex.toString();
    }

    public static void clearCacheCert(Context context) {
        File localCacheCert = new File(getCertCacheFolderPath(context), getCertCacheName(context));
        if (localCacheCert.exists()) {
            localCacheCert.delete();
        }
    }

    //获取证书对象
    public static void getCert(Context context, ResultCallbackObj callback) {
        //判断本地是否有缓存文件
        File localCacheCert = new File(getCertCacheFolderPath(context), getCertCacheName(context));
        if (localCacheCert.exists()) {
            try (InputStream inputStream = new FileInputStream(localCacheCert)) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(inputStream);
                Logger.d(TAG, "get cert from local cache! issuerDN =" + cert.getIssuerDN());
                if (callback != null) {
                    callback.onResult(true, cert);
                }
            } catch (Exception e) {
                Logger.e(e.getMessage());
                Logger.d(TAG, "parse local cache cert failed, clear " + localCacheCert.getAbsolutePath());
                localCacheCert.delete();
                getCertObjectFromServerAndCache(context, callback);
            }
        } else {
            getCertObjectFromServerAndCache(context, callback);
        }
    }

    //从服务端获取证书文件并缓存到本地
    private static void getCertObjectFromServerAndCache(Context context, ResultCallbackObj callback) {
        getCertStrFromServer(context, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                if (result && extraObj != null) {
                    String certBase64Str = (String) extraObj;
                    byte[] certByteArr = Base64.decode(certBase64Str, Base64.DEFAULT);
                    FileOutputStream fos = null;
                    try (InputStream inputStream = new ByteArrayInputStream(certByteArr)) {
                        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
                        Logger.d(TAG, "get cert from server success." + certificate.getIssuerDN());

                        //缓存到本地
                        File cacheCertFile = new File(getCertCacheFolderPath(context), getCertCacheName(context));
                        if (cacheCertFile.exists()) {
                            cacheCertFile.delete();
                        }
                        File cacheFolder = new File(getCertCacheFolderPath(context));
                        if (!cacheFolder.exists()) {
                            cacheFolder.mkdirs();
                        }

                        fos = new FileOutputStream(cacheCertFile);
                        fos.write(certByteArr);

                        if (callback != null) {
                            callback.onResult(true, certificate);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                Logger.e(e.getMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(String msg) {
                if (callback != null) {
                    callback.onError(msg);
                }
            }
        });
    }

    //获取Base64编码后的字符串
    public static void getCertStrFromServer(Context context, ResultCallbackObj callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            ThreadPool.getInstance().execute(() -> MultipartNetworkManger.getHttpsCert(gatewayCommunicationBase, callback));
        } else if (callback != null) {
            callback.onError("gatewayCommunicationBase is null");
        }
    }


    //获取局域网https传输校验token
    //Token 的生成算法： {aoid}-{hex(md5({aoid}-bp-{secret}))的前20个字符}
    public static String getLanHttpsHeaderToken(String aoid, String secretKey) {
        String suffixSb = aoid + "-bp-" + secretKey;
        String md5String = MD5Util.getMD5String(suffixSb);
        String md5Pre20 = md5String.substring(0, 20);
        String token = aoid + "-" + md5Pre20;
        return token;
    }

    //获取本地缓存证书文件位置
    private static String getCertCacheFolderPath(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/cache/cert";
    }

    //获取本地缓存证书文件名称
    private static String getCertCacheName(Context context) {
        return EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_cert.der";
    }
}



