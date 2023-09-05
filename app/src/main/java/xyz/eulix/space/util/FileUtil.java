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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.network.files.FileListItem;

/**
 * @author: chenjiawei
 * date: 2021/6/10 15:34
 */
public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    private FileUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean existFile(String path) {
        boolean isExist = false;
        if (path != null) {
            File file = new File(path);
            isExist = file.exists();
        }
        return isExist;
    }

    public static File[] getChildFile(String path) {
        return (TextUtils.isEmpty(path) ? null : getChildFile(new File(path)));
    }

    public static File[] getChildFile(File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return null;
        } else {
            return file.listFiles();
        }
    }

    public static void mkFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    public static boolean deleteFile(String filePath) {
        boolean result = true;
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                result = file.delete();
            }
        }
        return result;
    }

    public static long getFileModifiedTimestamp(String filePath) {
        long timestamp = -1L;
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                timestamp = file.lastModified();
            }
        }
        return timestamp;
    }

    public static long getFileSize(String filePath) {
        long size = 0L;
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                size = file.length();
            }
        }
        return size;
    }

    public synchronized static String readFile(String filePath) {
        String fileContent = "";
        try (FileInputStream inputStream = new FileInputStream(filePath);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            StringBuilder lineBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!TextUtils.isEmpty(lineBuilder.toString())) {
                    lineBuilder.append("\n");
                }
                lineBuilder.append(line);
            }
            fileContent = lineBuilder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ee) {
            ee.printStackTrace();
        } catch (Exception eee) {
            eee.printStackTrace();
        }
        Logger.d(TAG, "read file: " + filePath + " , content: " + fileContent);
        return fileContent;
    }

    public synchronized static void writeFile(String filePath, String fileContent) {
        Logger.d(TAG, "write file: " + filePath + " , content: " + fileContent);
        try (FileOutputStream outputStream = new FileOutputStream(filePath);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
             BufferedWriter writer = new BufferedWriter(outputStreamWriter)) {
            writer.write(fileContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void writeFile(@NonNull ContentResolver contentResolver, Uri fileUri, String fileContent) {
        try (OutputStream outputStream = contentResolver.openOutputStream(fileUri);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
             BufferedWriter writer = new BufferedWriter(outputStreamWriter)) {
            writer.write(fileContent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ee) {
            ee.printStackTrace();
        } catch (Exception eee) {
            eee.printStackTrace();
        }
    }

    public static CustomizeFile convertToCustomFile(File file) {
        CustomizeFile customizeFile = new CustomizeFile();
        if (file != null) {
            String fileName = file.getName();
            customizeFile.setId(file.getAbsolutePath());
            customizeFile.setName(fileName);
            customizeFile.setTimestamp(file.lastModified());
            customizeFile.setSize(file.length());
            boolean isDirectory = file.isDirectory();
            if (isDirectory) {
                customizeFile.setMime(ConstantField.MimeType.FOLDER);
            } else {
                String suffix = ConstantField.MimeType.UNKNOWN;
                if (!TextUtils.isEmpty(fileName)) {
                    int index = fileName.lastIndexOf(".");
                    if (index >= 0 && index < fileName.length() - 1) {
                        suffix = fileName.substring(index + 1).toLowerCase();
                    }
                }
                customizeFile.setMime(getMimeType(suffix));
                String fileMD5 = "";
                try {
                    fileMD5 = MD5Util.getFileMD5String(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                customizeFile.setMd5(fileMD5);
            }
            if (isDirectory) {
                File[] childFile = getChildFile(file);
                List<CustomizeFile> customizeFiles = new ArrayList<>();
                if (childFile != null) {
                    for (File child : childFile) {
                        customizeFiles.add(convertToCustomFile(child));
                    }
                }
                customizeFile.setContent(customizeFiles);
            } else {
                customizeFile.setContent(null);
            }
        }
        return customizeFile;
    }

    public static List<CustomizeFile> convertToCustomFileList(File[] files) {
        List<CustomizeFile> customizeFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    customizeFiles.add(convertToCustomFile(file));
                }
            }
        }
        return customizeFiles;
    }

    public static CustomizeFile convertToCustomFile(FileListItem fileListItem) {
        CustomizeFile customizeFile = new CustomizeFile();
        if (fileListItem != null) {
            customizeFile.setId(fileListItem.getUuid());
            customizeFile.setName(fileListItem.getName());
            customizeFile.setMime(fileListItem.getMime());
            customizeFile.setSize(fileListItem.getSize());
            customizeFile.setPath(fileListItem.getPath());
            customizeFile.setTimestamp(fileListItem.getOperationAt());
            customizeFile.setMd5(fileListItem.getMd5sum());
            Boolean isDirectory = fileListItem.getIs_dir();
            if (isDirectory != null && isDirectory) {
                customizeFile.setMime(ConstantField.MimeType.FOLDER);
            } else {
                customizeFile.setMime(fileListItem.getMime());
            }
        }
        return customizeFile;
    }

    public static List<CustomizeFile> convertToCustomFileList(List<FileListItem> fileListItems) {
        List<CustomizeFile> customizeFiles = new ArrayList<>();
        if (fileListItems != null) {
            for (FileListItem fileListItem : fileListItems) {
                if (fileListItem != null) {
                    customizeFiles.add(convertToCustomFile(fileListItem));
                }
            }
        }
        return customizeFiles;
    }

    public static String getMimeTypeByPath(String path) {
        String mimeType = "*/*";
        if (!TextUtils.isEmpty(path)) {
            int typeIndex = path.lastIndexOf(".");
            String suffix = path.substring(typeIndex + 1);
            mimeType = getMimeType(suffix);
        }
        return mimeType;
    }

    public static String getMimeType(String mime) {
        String mimeType = "*/*";
        if (!TextUtils.isEmpty(mime)) {
            for (String[] mimeArray : ConstantField.MimeType.MIME_MAP_TABLE) {
                if (mimeArray != null && mimeArray.length >= 2 && mimeArray[0].equalsIgnoreCase(mime)) {
                    mimeType = mimeArray[1];
                    break;
                }
            }
        }
        return mimeType;
    }

    @DrawableRes
    public static int getMimeIcon(String mime) {
        if (!TextUtils.isEmpty(mime)) {
            if (mime.equalsIgnoreCase(ConstantField.MimeType.FOLDER)) {
                return R.drawable.icon_file_type_folder;
            } else if (mime.contains("pdf")) {
                return R.drawable.icon_file_type_pdf;
            } else if (mime.endsWith("word")) {
                return R.drawable.icon_file_type_word;
            } else if (mime.contains("excel")) {
                return R.drawable.icon_file_type_excel;
            } else if (mime.startsWith("image")) {
                return R.drawable.icon_file_type_image;
            } else if (mime.equals("text/plain")) {
                return R.drawable.icon_file_type_txt;
            } else if (mime.endsWith("powerpoint")) {
                return R.drawable.icon_file_type_ppt;
            } else if (mime.contains("image/")) {
                return R.drawable.icon_file_type_image;
            } else if (mime.contains("audio/")) {
                return R.drawable.icon_file_type_audio;
            } else if (mime.contains("video/")) {
                return R.drawable.icon_file_type_video;
            } else if (mime.equals("text/html")) {
                return R.drawable.icon_file_type_html;
            } else if (mime.contains("compress") || mime.contains("zip") || mime.contains("tar")) {
                return R.drawable.icon_file_type_compress;
            }
        }
        return R.drawable.icon_file_type_unknow;
    }


    //获取文件夹大小
    public static long getFolderSize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        long size = 0L;
        try {
            java.io.File[] fileList = file.listFiles();
            if (fileList != null) {
                for (File value : fileList) {
                    if (value.isDirectory()) {
                        size = size + getFolderSize(value);

                    } else {
                        size = size + value.length();

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    //清空文件夹
    public static void clearFolder(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return;
        }
        try {
            java.io.File[] fileList = folder.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {
                    clearFolder(fileList[i]);
                }
                boolean result = fileList[i].delete();
                Logger.d("result: " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制文件
     *
     * @param sourceAbsolute 文件全路径
     * @param targetPath     目标文件夹
     * @return
     */
    public static boolean copyFile(String sourceAbsolute, String targetPath) {
        File sourceFile = new File(sourceAbsolute);
        if (!sourceFile.exists()) {
            return false;
        }
        File targetFolder = new File(targetPath);
        if (!targetFolder.exists()) {
            boolean result = targetFolder.mkdir();
            Logger.d("zfy", "target folder mkdir: " + result);
        }
        String targetFileName = targetPath + sourceFile.getName();
        Logger.d("zfy", "targetFileName = " + targetFileName);
        File targetFile = new File(targetPath, sourceFile.getName());
        if (targetFile.exists()) {
            boolean result = targetFile.delete();
            Logger.d("result: " + result);
        }
        try (InputStream inputStream = new FileInputStream(sourceFile);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //读取txt文件内容
    public static String readTxtFile(String absolutePath) {
        StringBuilder content = new StringBuilder();
        File file = new File(absolutePath);
        try (InputStream inputStream = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(inputStream, getFileCharset(file));
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    //判断编码格式方法
    public static String getFileCharset(File sourceFile) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile))) {
            boolean checked = false;
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) {
                return charset; //文件编码为 ANSI
            } else if (first3Bytes[0] == (byte) 0xFF
                    && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE"; //文件编码为 Unicode
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE"; //文件编码为 Unicode big endian
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8"; //文件编码为 UTF-8
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF) // 单独出现BF以下的，也算是GBK
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) // 双字节 (0xC0 - 0xDF)
                            // (0x80
                            // - 0xBF),也可能在GB编码内
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {// 也有可能出错，但是几率较小
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Logger.d("zfy", "txt charset:" + charset);
        return charset;
    }

    public static String getFilepath(ContentResolver contentResolver, Uri fileContentUri) {
        String filepath = null;
        if (contentResolver != null && fileContentUri != null) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(fileContentUri, new String[]{MediaStore.Images.ImageColumns.DATA}
                        , null, null, MediaStore.Images.ImageColumns.DATE_ADDED + " desc");
                if (cursor != null && cursor.moveToFirst()) {
                    int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (dataIndex >= 0) {
                        filepath = cursor.getString(dataIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (filepath == null && fileContentUri != null) {
            filepath = fileContentUri.getPath();
        }
        return filepath;
    }

    //检查文件是否支持预览
    public static boolean checkIsSupportPreview(String mimeType) {
        boolean isSupport = false;
        if (mimeType.contains("image")) {
            isSupport = isImageSupportView(mimeType);
        } else if (mimeType.contains("video")) {
            isSupport = isVideoSupportView(mimeType);
        } else if (mimeType.contains("text") || mimeType.contains("pdf")
                || isOfficeFile(mimeType)) {
            isSupport = true;
        }
        return isSupport;
    }

    public static boolean isImageSupportView(String mimeType) {
        if (mimeType.contains("jpeg") || mimeType.contains("bmp") || mimeType.contains("gif")
                || mimeType.contains("webp") || mimeType.contains("heic") || mimeType.contains("png")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isVideoSupportView(String mimeType) {
        if (mimeType.contains("3gp") || mimeType.contains("mp4") || mimeType.contains("quicktime")
                || mimeType.contains("mpeg") || mimeType.contains("msvideo") || mimeType.contains("mkv")) {
            return true;
        } else {
            return false;
        }
    }

    //是否为Micro Office文档
    public static boolean isOfficeFile(String mimeType) {
        //word、excel、ppt
        if (mimeType.contains("msword") || mimeType.contains("ms-excel")
                || mimeType.contains("powerpoint")) {
            return true;
        } else {
            return false;
        }
    }

    //是否为文档
    public static boolean isDocument(String mimeType) {
        if (TextUtils.isEmpty(mimeType)){
            return false;
        }
        boolean isDocument = false;
        if (isOfficeFile(mimeType) || mimeType.contains("text/plain") || mimeType.contains("pdf")){
            isDocument = true;
        }
        return isDocument;
    }

    //解压资源文件到指定路径
    public static boolean unZipFolder(String zipPath, String outPathString) {
        boolean result;
        if (TextUtils.isEmpty(zipPath)) {
            return false;
        }
        File zipFle = new File(zipPath);
        if (!zipFle.exists()) {
            return false;
        }

        File root = new File(outPathString);
        if (!root.exists()) {
            root.mkdirs();
        }
        FileOutputStream out = null;
        try (InputStream inputStream = new FileInputStream(zipPath);
             ZipInputStream inZip = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            String szName = "";
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    //获取部件的文件夹名
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPathString + File.separator + szName);
                    folder.mkdirs();
                } else {
                    File file = new File(outPathString + File.separator + szName);
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    // 获取文件的输出流
                    out = new FileOutputStream(file);
                    int len;
                    byte[] buffer = new byte[2048];
                    // 读取（字节）字节到缓冲区
                    while ((len = inZip.read(buffer)) != -1) {
                        // 从缓冲区（0）位置写入（字节）字节
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                    out.close();
                }
            }
            result = true;
        } catch (Exception e) {
            Logger.e(e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
