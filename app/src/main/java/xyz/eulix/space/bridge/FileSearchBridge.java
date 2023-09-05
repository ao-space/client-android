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

package xyz.eulix.space.bridge;

import java.util.UUID;

import xyz.eulix.space.abs.AbsBridge;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/1/11 15:26
 */
public class FileSearchBridge extends AbsBridge {
    private static final FileSearchBridge INSTANCE = new FileSearchBridge();

    public interface FileSearchSourceCallback extends SourceCallback {
        void setFolder(UUID folderUuid);
        void startSelf(String fileUuid);
    }

    public interface FileSearchSinkCallback extends SinkCallback {
        void finishSearch();
    }

    private FileSearchBridge() {}

    public static FileSearchBridge getInstance() {
        return INSTANCE;
    }

    public void enterFolder(UUID folderUuid) {
        if (mSourceCallback != null && mSourceCallback instanceof FileSearchSourceCallback) {
            ((FileSearchSourceCallback) mSourceCallback).setFolder(folderUuid);
        }
    }

    public void startSelf(String fileUuid) {
        if (mSourceCallback != null && mSourceCallback instanceof FileSearchSourceCallback) {
            ((FileSearchSourceCallback) mSourceCallback).startSelf(fileUuid);
        }
    }

    public void finishFileSearch() {
        if (mSinkCallback != null && mSinkCallback instanceof FileSearchSinkCallback) {
            ((FileSearchSinkCallback) mSinkCallback).finishSearch();
        }
    }
}
