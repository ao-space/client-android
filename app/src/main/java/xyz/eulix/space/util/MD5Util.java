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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/19 11:13
 */
public class MD5Util {
    /**
     * 默认的密码字符串组合，用来将字节转换成 16 进制表示的字符,apache校验下载的文件的正确性用的就是默认的这个组合
     */
    protected static char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private MD5Util() {
        throw new AssertionError("not allow to be instantiation!");
    }

    /**
     * 生成字符串的md5校验值
     *
     * @param s
     * @return
     */
    public static String getMD5String(String s) {
        return getMD5String(s.getBytes());
    }

    /**
     * 判断字符串的md5校验码是否与一个已知的md5码相匹配
     *
     * @param password  要校验的字符串
     * @param md5PwdStr 已知的md5校验码
     * @return
     */
    public static boolean checkPassword(String password, String md5PwdStr) {
        String s = getMD5String(password);
        return s.equals(md5PwdStr);
    }

    /**
     * 生成文件的md5校验值
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static String getFileMD5String(String filePath) throws IOException {
        File file = new File(filePath);
        if (file == null || !file.exists()) {
            return null;
        }
        return getFileMD5String(file);
    }

    /**
     * 生成文件的md5校验值
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String getFileMD5String(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int numRead = 0;
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            while ((numRead = fis.read(buffer)) > 0) {
                messagedigest.update(buffer, 0, numRead);
            }
            return bufferToHex(messagedigest.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成文件片段的md5校验值
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String getFileChunkMD5String(File file, long start, long length) {
        try (InputStream fis = new FileInputStream(file)) {
            fis.skip(start);
            byte[] buffer = new byte[1024];
            int numRead = 0;
            long currentLength = 0;
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            long leftLength = length - currentLength;
            long readLength = leftLength < 1024 ? leftLength : 1024;
            while (currentLength < length && (numRead = fis.read(buffer, 0, (int) readLength)) > 0) {
                currentLength += numRead;
                leftLength = length - currentLength;
                readLength = leftLength < 1024 ? leftLength : 1024;
                messagedigest.update(buffer, 0, numRead);
            }
            return bufferToHex(messagedigest.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * JDK1.4中不支持以MappedByteBuffer类型为参数update方法，并且网上有讨论要慎用MappedByteBuffer，
     * 原因是当使用 FileChannel.map 方法时，MappedByteBuffer 已经在系统内占用了一个句柄， 而使用
     * FileChannel.close 方法是无法释放这个句柄的，且FileChannel有没有提供类似 unmap 的方法，
     * 因此会出现无法删除文件的情况。
     * <p>
     * 不推荐使用
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String getFileMD5String_old(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            FileChannel ch = in.getChannel();
            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0,
                    file.length());
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            messagedigest.update(byteBuffer);
            return bufferToHex(messagedigest.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            in.close();
        }
    }

    public static String getMD5String(byte[] bytes) {
        MessageDigest messagedigest = null;
        try {
            messagedigest = MessageDigest.getInstance("MD5");
            messagedigest.update(bytes);
            return bufferToHex(messagedigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);
    }

    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = hexDigits[(bt & 0xf0) >> 4]; //取字节中高 4 位的数字转换, >>>为逻辑右移，将符号位一起右移, 此处未发现两种符号有何不同
        char c1 = hexDigits[bt & 0xf]; //取字节中低 4 位的数字转换
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }
}
