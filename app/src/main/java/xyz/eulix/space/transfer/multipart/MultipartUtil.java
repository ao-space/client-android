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

package xyz.eulix.space.transfer.multipart;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import xyz.eulix.space.transfer.multipart.bean.UploadChunkBean;
import xyz.eulix.space.transfer.multipart.bean.UploadPartBean;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.StringUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 分片传输工具类
 * History:     2022/2/16
 */
public class MultipartUtil {
    //计算betag分片大小：4M
    private static final long betagChunkSize = 4 * 1024 * 1024;
    //上传时文件分片大小：4M
    private static final long uploadChunkSize = 4 * 1024 * 1024;

    //下载时文件分片大小：4M
    private static final long DOWNLOAD_CHUNK_SIZE = 4 * 1024 * 1024;

    //上传数量
    public static int LIMIT_COUNT_UPLOAD = 2;
    //下载并发数量
    public static int LIMIT_COUNT_DOWNLOAD = 2;

    //失败重试次数
    public static final int RETRY_TIME = 3;

    //P2P补偿系数
    public static final double P2P_COMPENSATION = (1 - ConstantField.GOLD_RATIO);

    /**
     * 计算文件betag
     *
     * @param filePath 源文件绝对路径
     * @return
     */
    @Deprecated
    public static String getFileBetag(String filePath) {
        String betag = null;
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        long fileSize = file.length();
        String sizeFlag = getSizeFlag(fileSize);
        Logger.d("zfy", "sizeFlag = " + sizeFlag);
        //片数
        int chunkCount = (int) Math.ceil((double) fileSize / (double) betagChunkSize);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r")) {
            ArrayList<byte[]> md5ByteList = new ArrayList<>();
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            for (int i = 0; i < chunkCount; i++) {
                //计算各片md5值
                long start = betagChunkSize * i;
                randomAccessFile.seek(start);
                byte[] buffer = new byte[1024];
                int numRead = 0;
                long currentLength = 0;
                while (currentLength < betagChunkSize && (numRead = randomAccessFile.read(buffer)) > 0) {
                    currentLength += numRead;
                    messagedigest.update(buffer, 0, numRead);
                }
                byte[] md5Byte = messagedigest.digest();
                md5ByteList.add(md5Byte);
                String md5 = MD5Util.bufferToHex(md5Byte);
                Logger.d("zfy", "chunk " + i + ",md5:" + md5);
            }
            String chunksMd5 = "";
            if (md5ByteList.size() > 1) {
                //多片，组合各片md5值，再进行md5计算
                MessageDigest messagedigestChunks = MessageDigest.getInstance("MD5");
                for (byte[] md5Byte : md5ByteList) {
                    messagedigestChunks.update(md5Byte);
                }
                chunksMd5 = MD5Util.bufferToHex(messagedigestChunks.digest());
            } else if (md5ByteList.size() == 1) {
                //单片，直接取md5值
                chunksMd5 = MD5Util.bufferToHex(md5ByteList.get(0));
            }
            Logger.d("zfy", "chunksMd5 = " + chunksMd5);
            betag = sizeFlag + chunksMd5;
            Logger.d("zfy", "betag = " + betag);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return betag;
    }

    /**
     * 计算sizeFlag
     * 规则：文件长度二进制最高位1的位置，转为16进制
     *
     * @param fileSize
     * @return
     */
    private static String getSizeFlag(long fileSize) {
        int sizeFlagLen = Long.toBinaryString(fileSize).length() - 1;
        String sizeFlagStr = Integer.toHexString(sizeFlagLen);
        if (sizeFlagStr.length() < 2) {
            //高位补0
            sizeFlagStr = "0" + sizeFlagStr;
        }
        return sizeFlagStr;
    }

    /**
     * 获取文件分片数据列表（不考虑已上传）
     *
     * @param filePath
     * @return
     */
    public static ArrayList<UploadChunkBean> getChunksInfo(String filePath) {
        ArrayList<UploadChunkBean> chunkList = new ArrayList<>();
        File file = new File(filePath);
        if (file.exists()) {
            long fileSize = file.length();
            //片数
            int chunkCount = (int) Math.ceil((double) fileSize / (double) uploadChunkSize);
            for (int i = 0; i < chunkCount; i++) {
                //计算各片md5值
                long start = uploadChunkSize * i;
                long end;
                if (start + uploadChunkSize >= fileSize) {
                    end = fileSize;
                } else {
                    end = start + uploadChunkSize;
                }
                UploadChunkBean chunkBean = new UploadChunkBean();
                chunkBean.start = start;
                chunkBean.end = end;
                chunkList.add(chunkBean);
            }
        }
        return chunkList;
    }


    /**
     * 根据已上传分片列表，获取文件未上传的分片数据信息
     *
     * @param filePath     源文件绝对路径
     * @param uploadedList 已上传的片段列表
     * @return
     */
    public static ArrayList<UploadChunkBean> getUnUploadedChunksInfo(String filePath, List<UploadPartBean> uploadedList) {
        ArrayList<UploadChunkBean> chunkList = new ArrayList<>();
        File file = new File(filePath);
        if (file.exists()) {
            long fileSize = file.length();
            ArrayList<UploadPartBean> unUploadPartList = new ArrayList<>();
            if (uploadedList == null || uploadedList.isEmpty()) {
                //没有已上传完成的片段，整个文件都为未上传
                UploadPartBean unUploadPart = new UploadPartBean();
                unUploadPart.start = 0;
                unUploadPart.end = fileSize;
                unUploadPartList.add(unUploadPart);
            } else {
                //有已上传完成的片段，计算得到未上传片段
                if (uploadedList.get(0).start > 0) {
                    UploadPartBean unUploadPart = new UploadPartBean();
                    unUploadPart.start = 0;
                    unUploadPart.end = uploadedList.get(0).start;
                    unUploadPartList.add(unUploadPart);
                }
                for (int i = 0; i < uploadedList.size(); i++) {
                    UploadPartBean unUploadPart = new UploadPartBean();
                    if (i < uploadedList.size() - 1) {
                        unUploadPart.start = uploadedList.get(i).end;
                        unUploadPart.end = uploadedList.get(i + 1).start;
                    } else {
                        unUploadPart.start = uploadedList.get(i).end;
                        unUploadPart.end = fileSize;
                    }
                    if (unUploadPart.end > unUploadPart.start) {
                        unUploadPartList.add(unUploadPart);
                    }
                }
            }
            for (UploadPartBean unUploadPartItem : unUploadPartList) {
                //片数
                long currentBigPartSize = unUploadPartItem.end - unUploadPartItem.start;
                int chunkCount = (int) Math.ceil((double) currentBigPartSize / (double) uploadChunkSize);
                for (int i = 0; i < chunkCount; i++) {
                    //计算各片md5值
                    long start = uploadChunkSize * i + unUploadPartItem.start;
                    long end;
                    if (start + uploadChunkSize >= unUploadPartItem.end) {
                        end = unUploadPartItem.end;
                    } else {
                        end = start + uploadChunkSize;
                    }
                    UploadChunkBean chunkBean = new UploadChunkBean();
                    chunkBean.start = start;
                    chunkBean.end = end;
                    chunkList.add(chunkBean);
                }
            }

        }
        return chunkList;
    }

    /**
     * 创建分片：生成分片文件，并计算md5
     *
     * @param filePath     源文件地址
     * @param cacheDirPath 缓存路径
     * @param start        起始位置（包含）
     * @param end          结束位置（不包含）
     * @return
     */
    public static UploadChunkBean createChunkFile(String filePath, String cacheDirPath, long start, long end) {
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.d("zfy", "file not exist");
            return null;
        }
        if (end > file.length()) {
            Logger.d("zfy", "end position overSize");
            return null;
        }
        FileOutputStream fileOutputStream = null;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r")) {
            randomAccessFile.seek(start);

            long chunkLength = end - start;
            byte[] buffer = new byte[1024];
            int numRead = 0;
            long currentLength = 0;
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            File cacheDir = new File(cacheDirPath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            String chunkName = file.getName() + "_chunk_" + start + "_" + end;
            File chunkFile = new File(cacheDir, chunkName);
            if (chunkFile.exists()) {
                chunkFile.delete();
            }
            //写入分片文件，并计算md5
            fileOutputStream = new FileOutputStream(chunkFile);
            while (currentLength < chunkLength && (numRead = randomAccessFile.read(buffer)) > 0) {
                currentLength += numRead;
                fileOutputStream.write(buffer, 0, numRead);
                messagedigest.update(buffer, 0, numRead);
            }
            String chunkMd5 = MD5Util.bufferToHex(messagedigest.digest());

            UploadChunkBean uploadChunkBean = new UploadChunkBean();
            uploadChunkBean.start = start;
            uploadChunkBean.end = end;
            uploadChunkBean.md5 = chunkMd5;
            uploadChunkBean.path = chunkFile.getAbsolutePath();
            Logger.d("zfy", "chunk name:" + chunkFile.getName());
//            Logger.d("zfy", "chunk md5:" + uploadChunkBean.md5 + ";realMd5:" + MD5Util.getFileMD5String(chunkFile));
            return uploadChunkBean;
        } catch (Exception e) {
            Logger.e(e.getMessage());
            return null;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 创建加密分片，并计算md5
     *
     * @param filePath
     * @param cacheDirPath
     * @param start
     * @param end
     * @return
     */
    public static UploadChunkBean createEncryptChunkFile(String filePath, String cacheDirPath, long start, long end,
                                                         String algorithm, String encryptKey, String ivParams) {
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.d("zfy", "file not exist");
            return null;
        }
        if (end > file.length()) {
            Logger.d("zfy", "end position overSize");
            return null;
        }

        if (TextUtils.isEmpty(algorithm)) {
            Logger.d("zfy", "algorithm is null");
            return null;
        }

        String baseAlgorithm = algorithm;
        if (algorithm.contains("/")) {
            baseAlgorithm = algorithm.split("/")[0];
        }
        Cipher cipher = EncryptionUtil.getCipher(algorithm, baseAlgorithm, null);
        Charset charset = StandardCharsets.UTF_8;
        if (cipher == null || baseAlgorithm == null) {
            Logger.d("zfy", "ciper is null");
            return null;
        }

        RandomAccessFile randomAccessFile = null;
        FileOutputStream fileOutputStream = null;
        try {
            byte[] key = null;
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
                    cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance(baseAlgorithm).generatePublic(keySpec));
                    break;
                default:
                    break;
            }

            randomAccessFile = new RandomAccessFile(filePath, "r");
            randomAccessFile.seek(start);

            long chunkLength = end - start;
            byte[] buffer = new byte[1024];
            int numRead = 0;
            long currentLength = 0;
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            File cacheDir = new File(cacheDirPath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            String chunkName = file.getName() + "_chunk_" + start + "_" + end;
            File chunkFile = new File(cacheDir, chunkName);
            if (chunkFile.exists()) {
                chunkFile.delete();
            }
            //写入分片文件，并计算md5
            fileOutputStream = new FileOutputStream(chunkFile);
            while (currentLength < chunkLength && (numRead = randomAccessFile.read(buffer)) > 0) {
                currentLength += numRead;
//                fileOutputStream.write(buffer, 0, numRead);
                messagedigest.update(buffer, 0, numRead);
                //直接写加密数据到文件
                fileOutputStream.write(cipher.update(buffer, 0, numRead));
            }
            fileOutputStream.write(cipher.doFinal());

            String chunkMd5 = MD5Util.bufferToHex(messagedigest.digest());

            UploadChunkBean uploadChunkBean = new UploadChunkBean();
            uploadChunkBean.start = start;
            uploadChunkBean.end = end;
            uploadChunkBean.md5 = chunkMd5;
            uploadChunkBean.path = chunkFile.getAbsolutePath();
            Logger.d("zfy", "chunk name:" + chunkFile.getName());
//            Logger.d("zfy", "chunk md5:" + uploadChunkBean.md5 + ";realMd5:" + MD5Util.getFileMD5String(chunkFile));
            return uploadChunkBean;
        } catch (Exception e) {
            Logger.e(e.getMessage());
            return null;
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //获取明文上传分片信息，主要补充md5变量
    public static UploadChunkBean getPlainChunkInfo(String filePath, long start, long end){
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.d("zfy", "file not exist");
            return null;
        }
        if (end > file.length()) {
            Logger.d("zfy", "end position overSize");
            return null;
        }


        UploadChunkBean uploadChunkBean = new UploadChunkBean();
        uploadChunkBean.start = start;
        uploadChunkBean.end = end;
        uploadChunkBean.md5 = MD5Util.getFileChunkMD5String(file, start, end-start);
        uploadChunkBean.path = filePath;

        return uploadChunkBean;
    }

    /**
     * 获取未下载的分片信息列表
     *
     * @param fileUuid
     * @param fileSize
     * @param cacheDirPath
     * @return
     */
    public synchronized static ArrayList<UploadChunkBean> getUnDownloadChunksInfo(String fileUuid, long fileSize, String cacheDirPath) {
        ArrayList<UploadChunkBean> chunkList = new ArrayList<>();

        File dirFile = new File(cacheDirPath);
        //获取已下载完成片段列表
        ArrayList<UploadChunkBean> finishChunkList = new ArrayList<>();
        if (dirFile.exists()) {
            String[] allFileList = dirFile.list();
            if (allFileList != null) {
                for (String fileName : allFileList) {
                    if (fileName.startsWith(fileUuid) && fileName.contains("_") && !fileName.endsWith("temp")) {
                        UploadChunkBean chunkBean = new UploadChunkBean();
                        chunkBean.start = getChunkStartFlag(fileName);
                        chunkBean.end = getChunkEndFlag(fileName);
                        finishChunkList.add(chunkBean);
                    }
                }
                //按start排序
                Collections.sort(finishChunkList, new Comparator<UploadChunkBean>() {
                    @Override
                    public int compare(UploadChunkBean o1, UploadChunkBean o2) {
                        long lStart = o1.start;
                        long rStart = o2.start;
                        return Long.compare(lStart, rStart);
                    }
                });
            }
        }

        //计算未下载片段
        ArrayList<UploadChunkBean> unDownloadList = new ArrayList<>();
        if (finishChunkList.isEmpty()) {
            //没有已下载完成的片段，整个文件都为未下载
            UploadChunkBean unDownloadPart = new UploadChunkBean();
            unDownloadPart.start = 0;
            unDownloadPart.end = fileSize - 1;
            unDownloadList.add(unDownloadPart);
        } else {
            if (finishChunkList.get(0).start > 0) {
                UploadChunkBean chunkBean = new UploadChunkBean();
                chunkBean.start = 0;
                chunkBean.end = finishChunkList.get(0).start - 1;
                unDownloadList.add(chunkBean);
            }
            for (int i = 0; i < finishChunkList.size(); i++) {
                UploadChunkBean unDownloadPart = new UploadChunkBean();
                if (i < finishChunkList.size() - 1) {
                    unDownloadPart.start = finishChunkList.get(i).end + 1;
                    unDownloadPart.end = finishChunkList.get(i + 1).start - 1;
                } else {
                    unDownloadPart.start = finishChunkList.get(i).end + 1;
                    unDownloadPart.end = fileSize - 1;
                }
                if (unDownloadPart.end > unDownloadPart.start) {
                    unDownloadList.add(unDownloadPart);
                }
            }
        }

        for (UploadChunkBean unDownloadPartItem : unDownloadList) {
            //片数
            long currentBigPartSize = unDownloadPartItem.end - unDownloadPartItem.start + 1;
            int chunkCount = (int) Math.ceil((double) currentBigPartSize / (double) DOWNLOAD_CHUNK_SIZE);
            for (int i = 0; i < chunkCount; i++) {
                long start = DOWNLOAD_CHUNK_SIZE * i + unDownloadPartItem.start;
                long end;
                if (start + DOWNLOAD_CHUNK_SIZE > unDownloadPartItem.end) {
                    end = unDownloadPartItem.end;
                } else {
                    end = start + DOWNLOAD_CHUNK_SIZE - 1;
                }
                UploadChunkBean chunkBean = new UploadChunkBean();
                chunkBean.start = start;
                chunkBean.end = end;
                chunkBean.path = getDownloadChunkName(fileUuid, chunkBean.start, chunkBean.end);
                chunkList.add(chunkBean);
            }
        }

        return chunkList;
    }

    public static String getDownloadChunkName(String fileUuid, long start, long end) {
        return fileUuid + "_" + start + "_" + end;
    }

    /**
     * 合并连续分片
     *
     * @param fileUuid
     * @param chunkToMerge
     * @param dirFilePath
     */
    public synchronized static void mergeDownloadChunk(String fileUuid, UploadChunkBean chunkToMerge, String dirFilePath) {
        if (chunkToMerge == null || TextUtils.isEmpty(dirFilePath)) {
            return;
        }
        List<File> finishChunkFileList = getFinishDownloadChunks(fileUuid, dirFilePath);
        File fileToMerge = new File(dirFilePath, chunkToMerge.path);
        for (int i = 0; i < finishChunkFileList.size(); i++) {
            if (finishChunkFileList.get(i).getName().equals(fileToMerge.getName())) {
                finishChunkFileList.remove(i);
            }
        }
        //判断是否为第一片
        if (finishChunkFileList.size() > 1) {
            File listFirstChunk = finishChunkFileList.get(0);
            long firstChunkStart = getChunkStartFlag(listFirstChunk.getName());
            if (chunkToMerge.start < firstChunkStart) {
                //比已完成第一批更靠前
                if (chunkToMerge.end + 1 < firstChunkStart) {
                    //与其他片段不连续
                    Logger.d("zfy", "no continuous file,quit");
                } else {
                    FileInputStream fileInputStream = null;
                    try (RandomAccessFile firstRandomAccessFile = new RandomAccessFile(fileToMerge, "rwd")) {
                        long firstChunkEnd = getChunkEndFlag(listFirstChunk.getName());
                        long newTargetLength = firstChunkEnd - chunkToMerge.start + 1;
                        long seekPos = firstChunkStart - chunkToMerge.start;
                        firstRandomAccessFile.seek(seekPos);
                        byte[] buffer = new byte[1024];
                        int numRead;
                        fileInputStream = new FileInputStream(listFirstChunk);
                        while ((numRead = fileInputStream.read(buffer)) > 0) {
                            firstRandomAccessFile.write(buffer, 0, numRead);
                        }
                        firstRandomAccessFile.setLength(newTargetLength);
                        //删除并重命名
                        listFirstChunk.delete();
                        String mergedName = fileUuid + "_" + chunkToMerge.start + "_" + firstChunkEnd;
                        fileToMerge.renameTo(new File(fileToMerge.getParent(), mergedName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return;
            }
        }
        //查找需要写入的文件
        for (int i = 0; i < finishChunkFileList.size(); i++) {
            File fileItem = finishChunkFileList.get(i);
            long itemStart = getChunkStartFlag(fileItem.getName());
            long itemEnd = getChunkEndFlag(fileItem.getName());
            if (chunkToMerge.start > itemStart && chunkToMerge.start <= itemEnd + 1 && chunkToMerge.end >= itemEnd) {
                if (chunkToMerge.end <= itemEnd) {
                    //需合并片段全部包含在已完成片段内
                    Logger.d("zfy", "chunkToMerge include");
                    break;
                } else {
                    RandomAccessFile nextRandomAccessFile = null;
                    FileInputStream fileInputStream = null;
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileItem, "rwd")) {
                        long seekPos = chunkToMerge.start - itemStart;
                        randomAccessFile.seek(seekPos);
                        if (!fileToMerge.exists()) {
                            //分片文件不存在
                            Logger.d("zfy", "chunk to merge file not exist");
                            return;
                        }
                        long currentLength = seekPos;
                        long targetLength = chunkToMerge.end - itemStart + 1;
                        byte[] buffer = new byte[1024];
                        fileInputStream = new FileInputStream(fileToMerge);
                        int numRead;
                        while (currentLength < targetLength && (numRead = fileInputStream.read(buffer)) > 0) {
                            currentLength += numRead;
                            randomAccessFile.write(buffer, 0, numRead);
                        }

                        //合并完成，删除被合并片段，并重命名合并完成片段
                        fileToMerge.delete();
                        String mergedName = fileUuid + "_" + itemStart + "_" + chunkToMerge.end;
                        File mergedFile = new File(fileItem.getParent(), mergedName);
                        fileItem.renameTo(mergedFile);

                        //判断与下一片是否连续
                        if (i < finishChunkFileList.size() - 1) {
                            File nextChunk = finishChunkFileList.get(i + 1);
                            if (!nextChunk.exists()) {
                                return;
                            }
                            long nextChunkStart = getChunkStartFlag(nextChunk.getName());
                            if (chunkToMerge.end + 1 >= nextChunkStart) {
                                //与下一片连续，进行合并
                                long nextSeekPos = nextChunkStart - itemStart;
                                nextRandomAccessFile = new RandomAccessFile(mergedFile, "rwd");
                                nextRandomAccessFile.seek(nextSeekPos);
                                long nextChunkEnd = getChunkEndFlag(nextChunk.getName());
                                long nextTargetLength = nextChunkEnd - itemStart + 1;
                                while ((numRead = fileInputStream.read(buffer)) > 0) {
                                    randomAccessFile.write(buffer, 0, numRead);
                                }
                                nextRandomAccessFile.setLength(nextTargetLength);

                                //合并完成，删除被合并片段，并重命名合并完成片段
                                nextChunk.delete();
                                String nextMergedName = fileUuid + "_" + itemStart + "_" + nextChunkEnd;
                                File nextMergedFile = new File(mergedFile.getParent(), nextMergedName);
                                mergedFile.renameTo(nextMergedFile);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (nextRandomAccessFile != null) {
                            try {
                                nextRandomAccessFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            }
        }
    }

    private static long getChunkStartFlag(String fileName) {
        long start = -1L;
        String[] list = fileName.split("_");
        if (list.length > 2) {
            start = Long.parseLong(list[list.length - 2]);
        }
        return start;
    }

    private static long getChunkEndFlag(String fileName) {
        long end = -1L;
        String[] list = fileName.split("_");
        if (list.length > 2) {
            end = Long.parseLong(list[list.length - 1]);
        }
        return end;
    }


    //获取已下周完成分片文件数据
    private synchronized static ArrayList<File> getFinishDownloadChunks(String fileUuid, String dirFilePath) {
        ArrayList<File> finishChunkList = new ArrayList<>();
        File dirFile = new File(dirFilePath);
        if (dirFile.exists()) {
            String[] allFileList = dirFile.list();
            if (allFileList != null) {
                for (String fileName : allFileList) {
                    if (fileName.startsWith(fileUuid) && fileName.contains("_") && !fileName.endsWith("temp")) {
                        File file = new File(dirFile, fileName);
                        finishChunkList.add(file);
                    }
                }
                //按start排序
                Collections.sort(finishChunkList, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        long lStart = getChunkStartFlag(o1.getName());
                        long rStart = getChunkStartFlag(o2.getName());
                        return Long.compare(lStart, rStart);
                    }
                });
            }
        }
        return finishChunkList;
    }

    //合并所有下载完成分片
    public static boolean mergeAllDownloadChunks(String fileUuid, String cacheDirPath,
                                                 String targetDirPath, String fileName, long fileSize) {
        Logger.d("zfy", "call mergeAllDownloadChunks");
        if (TextUtils.isEmpty(fileUuid) || TextUtils.isEmpty(cacheDirPath)
                || TextUtils.isEmpty(fileName) || TextUtils.isEmpty(targetDirPath) || fileSize < 0) {
            return false;
        }
        ArrayList<File> allChunks = getFinishDownloadChunks(fileUuid, cacheDirPath);
        if (allChunks.isEmpty()) {
            return false;
        }

        File targetDirFile = new File(targetDirPath);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdirs();
        } else {
            if (!targetDirFile.isDirectory()) {
                targetDirFile.delete();
                targetDirFile.mkdirs();
            }
        }
        File resultFile = new File(targetDirFile, fileName);
        if (resultFile.exists()) {

            resultFile.delete();
        }
        if (allChunks.size() == 1) {
            //只有一片
            return allChunks.get(0).renameTo(new File(targetDirFile, fileName));
        }

        //合并分片文件
        try (FileChannel resultFileChannel = new FileOutputStream(resultFile, true).getChannel()) {
            for (int i = 0; i < allChunks.size(); i++) {
                try (FileChannel blk = new FileInputStream(allChunks.get(i)).getChannel()) {
                    resultFileChannel.transferFrom(blk, resultFileChannel.size(), blk.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        //删除缓存分片文件
        for (int i = 0; i < allChunks.size(); i++) {
            allChunks.get(i).delete();
        }

        //校验文件大小
        if (resultFile.length() != fileSize) {
            Logger.d("zfy", "check file size error");
            resultFile.delete();
            return false;
        }

        return true;
    }

    /**
     * 清理下载缓存分片
     *
     * @param cacheDirPath
     * @param fileUuid
     */
    public static synchronized void clearDownloadCacheChunks(String cacheDirPath, String fileUuid) {
        File cacheDirFile = new File(cacheDirPath);
        if (cacheDirFile.exists()) {
            File[] itemFiles = cacheDirFile.listFiles();
            for (int i = 0; i < itemFiles.length; i++) {
                File itemFile = itemFiles[i];
                if (itemFile.getName().startsWith(fileUuid)) {
                    Logger.d("zfy", "clear " + itemFile.getName());
                    itemFile.delete();
                }
            }
        }
    }

}
