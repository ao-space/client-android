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

package xyz.eulix.space.transfer.event;

/**
 * Author:      Zhu Fuyu
 * Description: 传输文件状态变化事件
 * History:     2021/8/26
 */
public class TransferStateEvent {
    public String keyName;
    public int transferType;
    public int state;
    public String uniqueTag;

    public TransferStateEvent(String keyName, int transferType, int state, String uniqueTag){
        this.keyName = keyName;
        this.state = state;
        this.transferType = transferType;
        this.uniqueTag = uniqueTag;
    }
}
