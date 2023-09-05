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

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import xyz.eulix.space.transfer.TransferProgressListener;

/**
 * @author: chenjiawei
 * date: 2021/6/1 14:51
 */
public class EncryptionUtil {
    private static final String TAG = EncryptionUtil.class.getSimpleName();

//    static {
//        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
//        int index = Security.addProvider(new BouncyCastleProvider());
//        Logger.d(TAG, BouncyCastleProvider.PROVIDER_NAME + " index: " + index);
//    }

    private EncryptionUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static SecretKey generateSecretKey(String algorithm, String provider, int keySize) {
        SecretKey secretKey = null;
        if (!TextUtils.isEmpty(algorithm)) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            KeyGenerator keyGenerator = null;
            try {
                if (TextUtils.isEmpty(provider)) {
                    keyGenerator = KeyGenerator.getInstance(baseAlgorithm);
                } else {
                    keyGenerator = KeyGenerator.getInstance(baseAlgorithm, provider);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
            if (keyGenerator != null) {
                boolean isSuccess = false;
                try {
                    keyGenerator.init(keySize);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (isSuccess) {
                    secretKey = keyGenerator.generateKey();
                }
            }
        }
        return secretKey;
    }

    public static KeyPair generateKeyPair(String algorithm, String provider, int keySize) {
        KeyPair keyPair = null;
        if (!TextUtils.isEmpty(algorithm)) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            KeyPairGenerator keyPairGenerator = null;
            try {
                if (TextUtils.isEmpty(provider)) {
                    keyPairGenerator = KeyPairGenerator.getInstance(baseAlgorithm);
                } else {
                    keyPairGenerator = KeyPairGenerator.getInstance(baseAlgorithm, provider);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                Logger.e(TAG, "no such algorithm");
            } catch (NoSuchProviderException ee) {
                ee.printStackTrace();
                Logger.e(TAG, "no such provider");
            }
            if (keyPairGenerator != null) {
                boolean isSuccess = false;
                try {
                    keyPairGenerator.initialize(keySize);
                    isSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (isSuccess) {
                    keyPair = keyPairGenerator.generateKeyPair();
                }
            }
        }
        return keyPair;
    }

    public static Cipher getCipher(String algorithm, String baseAlgorithm, String provider) {
        Cipher cipher = null;
        if (algorithm != null && baseAlgorithm != null) {
            try {
                if (provider == null) {
                    cipher = Cipher.getInstance(algorithm);
                } else {
                    cipher = Cipher.getInstance(algorithm, provider);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
                try {
                    if (provider == null) {
                        cipher = Cipher.getInstance(baseAlgorithm);
                    } else {
                        cipher = Cipher.getInstance(baseAlgorithm, provider);
                    }
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                } catch (NoSuchPaddingException ex) {
                    ex.printStackTrace();
                } catch (NoSuchProviderException ex) {
                    ex.printStackTrace();
                    try {
                        cipher = Cipher.getInstance(baseAlgorithm);
                    } catch (NoSuchAlgorithmException exc) {
                        exc.printStackTrace();
                    } catch (NoSuchPaddingException exc) {
                        exc.printStackTrace();
                    }
                }
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
                try {
                    cipher = Cipher.getInstance(algorithm);
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                } catch (NoSuchPaddingException ex) {
                    ex.printStackTrace();
                    try {
                        cipher = Cipher.getInstance(baseAlgorithm);
                    } catch (NoSuchAlgorithmException exc) {
                        exc.printStackTrace();
                    } catch (NoSuchPaddingException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        }
        return cipher;
    }

    //加密文件，返回临时生成文件
    public static File encrypt(String algorithm, String provider, File file, String encryptKey, Charset charset, String ivParams, String encryptFilePath) {
        File resFile=null;
        if (algorithm != null) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            Cipher cipher = getCipher(algorithm, baseAlgorithm, provider);
            if (cipher != null && baseAlgorithm != null) {
                byte[] key = null;
                try {
                    switch (baseAlgorithm) {
                        case ConstantField.Algorithm.AES:
                            if (charset == null) {
                                key = StringUtil.stringToByteArray(encryptKey);
                            } else {
                                key = StringUtil.stringToByteArray(encryptKey, charset);
                            }
                            SecretKeySpec secretKeySpec = new SecretKeySpec(key, baseAlgorithm);
                            if (charset == null) {
                                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
                            } else {
                                byte[] ivs = StringUtil.stringToByteArray(ivParams);
                                if (ivs == null) {
                                    ivs = new byte[16];
                                }
                                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivs));
                            }
                            break;
                        case ConstantField.Algorithm.RSA:
                        case ConstantField.Algorithm.ECC:
                            key = StringUtil.stringToByteArray(encryptKey);
                            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
                            if (TextUtils.isEmpty(provider)) {
                                cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance(baseAlgorithm).generatePublic(keySpec));
                            } else {
                                cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance(baseAlgorithm, provider).generatePublic(keySpec));
                            }
                            break;
                        default:
                            break;
                    }
                    String resFilePath = encryptFilePath;
                    File resFileDir = new File(resFilePath);
                    if (!resFileDir.exists()){
                        resFileDir.mkdirs();
                    }
                    resFile=new File(resFilePath, "en_"+file.getName());
                    if (resFile.exists()){
                        boolean result = resFile.delete();
                        Logger.d("zfy", "file delete "+result);
                    }
                    try( FileInputStream fis = new FileInputStream(file);
                         FileOutputStream fos=new FileOutputStream(resFile);
                         CipherInputStream cin=new CipherInputStream(fis,cipher)){

                        byte[] bts=new byte[1024];
                        int len=0;
                        while ((len=cin.read(bts))!=-1){
                            fos.write(bts,0,len);
                            fos.flush();
                        }
                        cin.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return resFile;
    }

    //解密输入流，并生成文件
    public static File decrypt(String algorithm, String provider, InputStream inputStream, String decryptKey, Charset charset, String ivParams,
                               String decryptFilePath, String decryptFileName, long fileSize, TransferProgressListener progressListener) {
        File resultFile = null;
        if (algorithm != null) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            Cipher cipher = getCipher(algorithm, baseAlgorithm, provider);
            if (cipher != null && baseAlgorithm != null) {
                byte[] key = null;
                try {
                    switch (baseAlgorithm) {
                        case ConstantField.Algorithm.AES:
                            if (charset == null) {
                                key = StringUtil.stringToByteArray(decryptKey);
                            } else {
                                key = StringUtil.stringToByteArray(decryptKey, charset);
                            }
                            SecretKeySpec secretKeySpec = new SecretKeySpec(key, baseAlgorithm);
                            if (charset == null) {
                                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                            } else {
                                byte[] ivs = StringUtil.stringToByteArray(ivParams);
                                if (ivs == null) {
                                    ivs = new byte[16];
                                }
                                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivs));
                            }
                            break;
                        case ConstantField.Algorithm.RSA:
                        case ConstantField.Algorithm.ECC:
                            key = StringUtil.stringToByteArray(decryptKey);
                            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
                            if (TextUtils.isEmpty(provider)) {
                                cipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance(baseAlgorithm).generatePrivate(keySpec));
                            } else {
                                cipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance(baseAlgorithm, provider).generatePrivate(keySpec));
                            }
                            break;
                        default:
                            break;
                    }

                    File resFile=null;
                    String resFilePath = decryptFilePath;
                    resFile = new File(resFilePath, decryptFileName);
                    if (resFile.exists()) {
                        boolean result = resFile.delete();
                        Logger.d("zfy", "file delete "+result);
                    }
                    try (InputStream fis = inputStream;
                         FileOutputStream fos = new FileOutputStream(resFile);){
                        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                        long currentSize = 0L;
                        int oldPercent = 0; //上次进度
                        int currentPercent;
                        byte[] bts = new byte[1024];
                        int len = 0;
                        boolean isPercentChange = false;

                        while ((len = fis.read(bts)) != -1) {
                            cos.write(bts, 0, len);
                            cos.flush();
                            currentSize += len;
                            if (progressListener != null && fileSize>=0){
                                if (fileSize>0) {
                                    currentPercent = (int) (currentSize * 100 / fileSize);
                                    //进度有变化时再回调，减少回调次数
                                    if (currentPercent > oldPercent) {
                                        oldPercent = currentPercent;
                                        isPercentChange = true;
                                    } else {
                                        isPercentChange = false;
                                    }
                                    progressListener.onProgress(currentSize, fileSize, len, isPercentChange,false);
                                } else {
                                    progressListener.onProgress(currentSize, fileSize, len, false,false);
                                }
                            }
//                            Logger.d("zfy", "currentSize = " + currentSize);
                        }
                        cos.close();
                        resultFile = resFile;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return resultFile;
    }

    public static byte[] encrypt(String algorithm, String provider, byte[] data, String encryptKey, Charset charset, String ivParams) {
        byte[] result = null;
        if (algorithm != null) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            Cipher cipher = getCipher(algorithm, baseAlgorithm, provider);
            if (cipher != null && baseAlgorithm != null) {
                byte[] key = null;
                try {
                    switch (baseAlgorithm) {
                        case ConstantField.Algorithm.AES:
                            if (charset == null) {
                                key = StringUtil.stringToByteArray(encryptKey);
                            } else {
                                key = StringUtil.stringToByteArray(encryptKey, charset);
                            }
                            SecretKeySpec secretKeySpec = new SecretKeySpec(key, baseAlgorithm);
                            if (charset == null) {
                                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
                            } else {
                                byte[] ivs = StringUtil.stringToByteArray(ivParams);
                                if (ivs == null) {
                                    ivs = new byte[16];
                                }
                                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivs));
                            }
                            break;
                        case ConstantField.Algorithm.RSA:
                        case ConstantField.Algorithm.ECC:
                            key = StringUtil.stringToByteArray(encryptKey);
                            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
                            if (TextUtils.isEmpty(provider)) {
                                cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance(baseAlgorithm).generatePublic(keySpec));
                            } else {
                                cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance(baseAlgorithm, provider).generatePublic(keySpec));
                            }
                            break;
                        default:
                            break;
                    }
                    result = cipher.doFinal(data);
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e1) {
                    e1.printStackTrace();
                } catch (NoSuchAlgorithmException e2) {
                    e2.printStackTrace();
                } catch (NoSuchProviderException e3) {
                    e3.printStackTrace();
                } catch (BadPaddingException e4) {
                    e4.printStackTrace();
                } catch (IllegalBlockSizeException e5) {
                    e5.printStackTrace();
                } catch (Exception e6) {
                    e6.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String encrypt(String algorithm, String provider, String data, String encryptKey, Charset charset, String ivParams) {
        return StringUtil.byteArrayToString(encrypt(algorithm, provider, StringUtil.stringToByteArray(data, StandardCharsets.UTF_8), encryptKey, charset, ivParams));
    }

    public static byte[] decrypt(String algorithm, String provider, byte[] data, String decryptKey, Charset charset, String ivParams) {
        byte[] result = null;
        if (algorithm != null) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            Cipher cipher = getCipher(algorithm, baseAlgorithm, provider);
            if (cipher != null && baseAlgorithm != null) {
                byte[] key = null;
                try {
                    switch (baseAlgorithm) {
                        case ConstantField.Algorithm.AES:
                            if (charset == null) {
                                key = StringUtil.stringToByteArray(decryptKey);
                            } else {
                                key = StringUtil.stringToByteArray(decryptKey, charset);
                            }
                            SecretKeySpec secretKeySpec = new SecretKeySpec(key, baseAlgorithm);
                            if (charset == null) {
                                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                            } else {
                                byte[] ivs = StringUtil.stringToByteArray(ivParams);
                                if (ivs == null) {
                                    ivs = new byte[16];
                                }
                                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivs));
                            }
                            break;
                        case ConstantField.Algorithm.RSA:
                        case ConstantField.Algorithm.ECC:
                            key = StringUtil.stringToByteArray(decryptKey);
                            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
                            if (TextUtils.isEmpty(provider)) {
                                cipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance(baseAlgorithm).generatePrivate(keySpec));
                            } else {
                                cipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance(baseAlgorithm, provider).generatePrivate(keySpec));
                            }
                            break;
                        default:
                            break;
                    }
                    result = cipher.doFinal(data);
                } catch (Exception e6) {
                    e6.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String decrypt(String algorithm, String provider, String data, String decryptKey, Charset charset, String ivParams) {
        return StringUtil.byteArrayToString(decrypt(algorithm, provider, StringUtil.stringToByteArray(data), decryptKey, charset, ivParams), StandardCharsets.UTF_8);
    }

    public static String signRSAPrivateKey(String algorithm, String provider, String data, String rsaPrivateKey, Charset charset) {
        String result = data;
        if (algorithm != null && data != null && rsaPrivateKey != null) {
            byte[] rawData = data.getBytes(charset);
            byte[] key = StringUtil.stringToByteArray(rsaPrivateKey);
            PrivateKey privateKey = null;
            if (rawData != null && key != null) {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
                try {
                    if (TextUtils.isEmpty(provider) || provider == null) {
                        privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
                    } else {
                        privateKey = KeyFactory.getInstance("RSA", provider).generatePrivate(keySpec);
                    }
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (privateKey != null) {
                Signature signature = null;
                try {
                    if (provider == null) {
                        signature = Signature.getInstance(algorithm);
                    } else {
                        signature = Signature.getInstance(algorithm, provider);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (signature != null) {
                    try {
                        signature.initSign(privateKey);
                        signature.update(rawData);
                        result = StringUtil.byteArrayToString(signature.sign());
                    } catch (InvalidKeyException | SignatureException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }

    public static boolean verifyRSAPublicKey(String algorithm, String provider, String data, String verifyData, String rsaPublicKey, Charset charset) {
        boolean result = false;
        if (algorithm != null && data != null && verifyData != null && rsaPublicKey != null) {
            byte[] rawData = StringUtil.stringToByteArray(data);
            byte[] rawVerifyData = verifyData.getBytes(charset);
            byte[] key = StringUtil.stringToByteArray(rsaPublicKey);
            PublicKey publicKey = null;
            if (rawData != null && rawVerifyData != null && key != null) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
                try {
                    if (TextUtils.isEmpty(provider) || provider == null) {
                        publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
                    } else {
                        publicKey = KeyFactory.getInstance("RSA", provider).generatePublic(keySpec);
                    }
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (publicKey != null) {
                Signature signature = null;
                try {
                    if (provider == null) {
                        signature = Signature.getInstance(algorithm);
                    } else {
                        signature = Signature.getInstance(algorithm, provider);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (signature != null) {
                    try {
                        signature.initVerify(publicKey);
                        signature.update(rawVerifyData);
                        result = signature.verify(rawData);
                        Logger.d(TAG, "verify: " + result);
                    } catch (InvalidKeyException | SignatureException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }

    public static boolean checkKey(String algorithm, String provider, String encryptKey, String decryptKey, Charset charset) {
        boolean result = false;
        String checkData = "0123456789abcdef";
        String encryptData = encrypt(algorithm, provider, checkData, encryptKey, charset, null);
        if (encryptData != null) {
            String decryptData = decrypt(algorithm, provider, encryptData, decryptKey, charset, null);
            result = (decryptData != null && decryptData.equals(checkData));
        }
        Logger.d(TAG, "check result: " + result);
        return result;
    }

    public static String rsaEncrypt(String algorithm, String provider, String data, String encryptKey) {
        String result = null;
        byte[] key = null;
        try {
            key = StringUtil.stringToByteArray(encryptKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (key != null && algorithm != null) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            Cipher cipher = getCipher(algorithm, baseAlgorithm, provider);
            if (cipher != null) {
                try {
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
                    PublicKey publicKey = null;
                    if (TextUtils.isEmpty(provider)) {
                        publicKey = KeyFactory.getInstance(baseAlgorithm).generatePublic(keySpec);
                    } else {
                        publicKey = KeyFactory.getInstance(baseAlgorithm, provider).generatePublic(keySpec);
                    }
                    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                    result = StringUtil.byteArrayToString(cipher.doFinal(StringUtil.stringToByteArray(data, StandardCharsets.UTF_8)));
//                if (publicKey instanceof RSAPublicKey) {
//                    result = StringUtil.byteArrayToString(rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE
//                            , StringUtil.stringToByteArray(data, StandardCharsets.UTF_8), ((RSAPublicKey) publicKey).getModulus().bitLength()));
//                } else {
//
//                }
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e1) {
                    e1.printStackTrace();
                } catch (NoSuchAlgorithmException e2) {
                    e2.printStackTrace();
                } catch (NoSuchProviderException e3) {
                    e3.printStackTrace();
                } catch (Exception e6) {
                    e6.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String rsaDecrypt(String algorithm, String provider, String data, String decryptKey) {
        String result = null;
        byte[] key = null;
        try {
            key = StringUtil.stringToByteArray(decryptKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (key != null) {
            String baseAlgorithm = algorithm;
            if (algorithm.contains("/")) {
                baseAlgorithm = algorithm.split("/")[0];
            }
            Cipher cipher = getCipher(algorithm, baseAlgorithm, provider);
            if (cipher != null) {
                try {
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
                    PrivateKey privateKey = null;
                    if (TextUtils.isEmpty(provider)) {
                        privateKey = KeyFactory.getInstance(baseAlgorithm).generatePrivate(keySpec);
                    } else {
                        privateKey = KeyFactory.getInstance(baseAlgorithm, provider).generatePrivate(keySpec);
                    }
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                    result = StringUtil.byteArrayToString(cipher.doFinal(StringUtil.stringToByteArray(data)), StandardCharsets.UTF_8);
//                if (privateKey instanceof RSAPrivateKey) {
//                    result = StringUtil.byteArrayToString(rsaSplitCodec(cipher, Cipher.DECRYPT_MODE
//                            , StringUtil.stringToByteArray(data), ((RSAPrivateKey) privateKey).getModulus().bitLength()), StandardCharsets.UTF_8);
//                } else {
//
//                }
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e1) {
                    e1.printStackTrace();
                } catch (NoSuchAlgorithmException e2) {
                    e2.printStackTrace();
                } catch (NoSuchProviderException e3) {
                    e3.printStackTrace();
                } catch (Exception e6) {
                    e6.printStackTrace();
                }
            }
        }
        return result;
    }

    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] datas, int keySize) {
        int maxBlock = 0;
        if (opmode == Cipher.DECRYPT_MODE) {
            maxBlock = keySize / 8;
        } else {
            maxBlock = keySize / 8 - 11;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] buff;
        int i = 0;
        try {
            while (datas.length > offSet) {
                if (datas.length - offSet > maxBlock) {
                    buff = cipher.doFinal(datas, offSet, maxBlock);
                } else {
                    buff = cipher.doFinal(datas, offSet, datas.length - offSet);
                }
                out.write(buff, 0, buff.length);
                i++;
                offSet = i * maxBlock;
            }
        } catch (Exception e) {
            throw new RuntimeException("加解密阀值为[" + maxBlock + "]的数据时发生异常", e);
        }
        byte[] resultDatas = out.toByteArray();
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultDatas;
    }
}
