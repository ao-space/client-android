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

package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/30 15:40
 */
public class SpaceStatusStatusLineInfo implements EulixKeep {
    private int code;
    private String message;

    public static boolean compare(SpaceStatusStatusLineInfo spaceStatusStatusLineInfo1, SpaceStatusStatusLineInfo spaceStatusStatusLineInfo2) {
        boolean isEqual = false;
        if (spaceStatusStatusLineInfo1 == null && spaceStatusStatusLineInfo2 == null) {
            isEqual = true;
        } else if (spaceStatusStatusLineInfo1 != null && spaceStatusStatusLineInfo2 != null) {
            int code1 = spaceStatusStatusLineInfo1.code;
            String message1 = spaceStatusStatusLineInfo1.message;
            int code2 = spaceStatusStatusLineInfo2.code;
            String message2 = spaceStatusStatusLineInfo2.message;
            if (code1 == code2 && StringUtil.compare(message1, message2)) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SpaceStatusStatusLineInfo{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
