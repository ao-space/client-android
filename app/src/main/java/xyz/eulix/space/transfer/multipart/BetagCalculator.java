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

import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;

import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;

/**
 * Author:      Zhu Fuyu
 * Description: betag计算
 * History:     2022/3/15
 */
public class BetagCalculator {
    private String filePath;
    private static final long betagChunkSize = 4 * 1024 * 1024;
    private boolean mIsStop = false;
    //是否为相册同步过程中的计算
    private boolean mIsForSync = false;

    public BetagCalculator(String filePath) {
        this.filePath = filePath;
    }

    public void stopCalculate() {
        this.mIsStop = true;
    }

    public void setIsForSync(boolean isForSync) {
        this.mIsForSync = isForSync;
    }

    /**
     * 计算betag（必须在子线程调用！）
     *
     * @return
     */
    public String getFileBetag() {
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
                if (!isTaskContinue()) {
                    return null;
                }
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
    private String getSizeFlag(long fileSize) {
        int sizeFlagLen = Long.toBinaryString(fileSize).length() - 1;
        String sizeFlagStr = Integer.toHexString(sizeFlagLen);
        if (sizeFlagStr.length() < 2) {
            //高位补0
            sizeFlagStr = "0" + sizeFlagStr;
        }
        return sizeFlagStr;
    }

    private boolean isTaskContinue() {
        if (mIsStop) {
            return false;
        }
        return true;
    }
}
