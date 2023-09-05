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
 * Author:      Zhu Fuyu
 * Description: 个人信息更新通知
 * History:     2021/9/18
 */
public class UserInfoEvent {
    //更新昵称
    public static final int TYPE_NAME = 1;
    //更新签名
    public static final int TYPE_SIGN = 2;
    //更新头像
    public static final int TYPE_HEADER = 3;
    public int type;
    public String headerPath;
    public String nickName;
    public String signature;

    public UserInfoEvent(int type, String headerPath, String nickName, String signature) {
        this.type = type;
        this.headerPath = headerPath;
        this.nickName = nickName;
        this.signature = signature;
    }
}
