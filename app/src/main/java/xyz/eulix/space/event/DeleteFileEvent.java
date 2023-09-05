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


import java.util.ArrayList;
import java.util.List;

/**
 * Author:      Zhu Fuyu
 * Description: 文件删除事件
 * History:     2021/9/14
 */
public class DeleteFileEvent {
    public List<String> uuidList = new ArrayList<>();

    public DeleteFileEvent(List<String> uuidList){
        if (uuidList != null && !uuidList.isEmpty()){
            this.uuidList.addAll(uuidList);
        }
    }
}
