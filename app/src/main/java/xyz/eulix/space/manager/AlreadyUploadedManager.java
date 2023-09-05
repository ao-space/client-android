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

package xyz.eulix.space.manager;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.util.BaseParamsUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description: 已上传文件管理类，用于处理选择列表“仅显示未上传”逻辑
 * History:     2021/9/6
 */
public class AlreadyUploadedManager {
    private static AlreadyUploadedManager sInstance;
    private HashMap<String, String> mUploadedMap = new HashMap<>();
    private String mFolderPath;
    private String mFileName;
    private final String SPLIT_CHARS = "&;";
    private final String STORE_PATH = "/Documents/";

    public static AlreadyUploadedManager getInstance() {
        if (sInstance == null) {
            sInstance = new AlreadyUploadedManager();
        }
        return sInstance;
    }

    public HashMap<String, String> getUploadedMap() {
        return mUploadedMap;
    }

    //初始化，加载本地数据（必须先执行一次初始化，再调用其他方法；更换绑定设备或账号时，调用此方法进行刷新）
    public void init(Context context) {
        mFolderPath = Environment.getExternalStorageDirectory() + STORE_PATH;
        File folderFile = new File(mFolderPath);
        if (!folderFile.exists()) {
            folderFile.mkdir();
            return;
        }
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context.getApplicationContext()
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        String activeDeviceName = "";
        String activeDeviceUUID = "";
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null) {
                    activeDeviceUUID = (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            ? boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID) : "");
                    break;
                }
            }
        }

        UserInfo userInfo = EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
        String userId = "";
        if (userInfo != null) {
            userId = userInfo.getUserId();
        }
        mFileName = activeDeviceUUID + "_" + BaseParamsUtil.sAndroidId + "_" + userId;
        String filePath = mFolderPath + mFileName;
        File uploadedFile = new File(filePath);
        if (!uploadedFile.exists()) {
            mUploadedMap.clear();
            return;
        }
        ThreadPool.getInstance().execute(() -> {
            try (FileInputStream inputStream = new FileInputStream(uploadedFile);) {
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                mUploadedMap.clear();
                while ((line = br.readLine()) != null) {
                    Logger.d("zfy", "line=" + line);
                    //解析数据
                    line = line.replace("\r\n", "");
                    String[] paramsArray = line.split(SPLIT_CHARS);
                    if (paramsArray.length > 1) {
                        String absolutePath = new String(paramsArray[0].getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                        Logger.d("zfy", "uploaded file path:" + absolutePath);
                        String md5 = paramsArray[1];
                        mUploadedMap.put(absolutePath, md5);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //插入一条已上传数据
    public synchronized void insertItem(String absolutePath, String md5) {
        if (TextUtils.isEmpty(mFolderPath) || TextUtils.isEmpty(mFileName)) {
            Logger.d("zfy", "UploadedManager not init");
            return;
        }
        FileUtil.mkFile(mFolderPath);
        if (mUploadedMap.containsKey(absolutePath)) {
            Logger.d("zfy", "uploaded map already has " + absolutePath);
            return;
        }
        ThreadPool.getInstance().execute(() -> {
            try (RandomAccessFile randomFile = new RandomAccessFile(mFolderPath + mFileName, "rw");) {
                // 文件长度，字节数
                long fileLength = randomFile.length();
                // 将写文件指针移到文件尾。
                randomFile.seek(fileLength);
                String content = absolutePath + SPLIT_CHARS + md5 + "\r\n";
                randomFile.write(content.getBytes(StandardCharsets.UTF_8));
                mUploadedMap.put(absolutePath, md5);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized void removeItemByAbsolutePath(String absolutePath) {
        if (mUploadedMap.containsKey(absolutePath)) {
            Logger.d("zfy", "uploaded map remove:" + absolutePath);
            mUploadedMap.remove(absolutePath);
        }
    }

    //刷新本地文件（删除之后保存到本地）
    public void refreshUploadedRecord() {
        String folderPath = Environment.getExternalStorageDirectory() + STORE_PATH;
        FileUtil.mkFile(folderPath);
        File uploadedFile = new File(folderPath, mFileName);
        if (uploadedFile.exists()) {
            boolean result = uploadedFile.delete();
            Logger.d("zfy", "uploaded file delete result: " + result);
        }
        ThreadPool.getInstance().execute(() -> {
            try (RandomAccessFile randomFile = new RandomAccessFile(mFolderPath + mFileName, "rw")) {
                randomFile.seek(0);
                Iterator<Map.Entry<String, String>> iterator = mUploadedMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    String content = entry.getKey() + SPLIT_CHARS + entry.getValue() + "\r\n";
                    randomFile.write(content.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //清空所有记录
    public void clearAllRecords() {
        mUploadedMap.clear();
        String folderPath = Environment.getExternalStorageDirectory() + STORE_PATH;
        FileUtil.mkFile(folderPath);
        File uploadedFile = new File(folderPath, mFileName);
        if (uploadedFile.exists()) {
            boolean result = uploadedFile.delete();
            Logger.d("zfy", "uploaded file delete: " + result);
        }
    }

}
