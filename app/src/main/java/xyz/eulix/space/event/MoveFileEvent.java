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

package xyz.eulix.space.event;

/**
 * @author: chenjiawei
 * Description: 文件移动事件
 * date: 2022/1/21 10:33
 */
public class MoveFileEvent {
    private String uuid;
    private String fileName;

    public MoveFileEvent(String uuid, String fileName) {
        this.uuid = uuid;
        this.fileName = fileName;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFileName() {
        return fileName;
    }
}
