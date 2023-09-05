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

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/3/18 17:46
 */
public class OperationUtil {
    private static boolean fileAllRefresh = false;
    private static boolean fileImageRefresh = false;
    private static boolean fileVideoRefresh = false;
    private static boolean fileDocumentRefresh = false;
    private static boolean fileOtherRefresh = false;

    public static boolean isFileAllRefresh() {
        return fileAllRefresh;
    }

    public static void setFileAllRefresh(boolean fileAllRefresh) {
        OperationUtil.fileAllRefresh = fileAllRefresh;
    }

    public static boolean isFileImageRefresh() {
        return fileImageRefresh;
    }

    public static void setFileImageRefresh(boolean fileImageRefresh) {
        OperationUtil.fileImageRefresh = fileImageRefresh;
    }

    public static boolean isFileVideoRefresh() {
        return fileVideoRefresh;
    }

    public static void setFileVideoRefresh(boolean fileVideoRefresh) {
        OperationUtil.fileVideoRefresh = fileVideoRefresh;
    }

    public static boolean isFileDocumentRefresh() {
        return fileDocumentRefresh;
    }

    public static void setFileDocumentRefresh(boolean fileDocumentRefresh) {
        OperationUtil.fileDocumentRefresh = fileDocumentRefresh;
    }

    public static boolean isFileOtherRefresh() {
        return fileOtherRefresh;
    }

    public static void setFileOtherRefresh(boolean fileOtherRefresh) {
        OperationUtil.fileOtherRefresh = fileOtherRefresh;
    }

    public static void setAllFileRefresh(boolean isRefresh) {
        OperationUtil.fileAllRefresh = isRefresh;
        OperationUtil.fileImageRefresh = isRefresh;
        OperationUtil.fileVideoRefresh = isRefresh;
        OperationUtil.fileDocumentRefresh = isRefresh;
        OperationUtil.fileOtherRefresh = isRefresh;
    }
}
