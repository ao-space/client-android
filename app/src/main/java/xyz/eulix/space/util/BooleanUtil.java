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
 * date: 2022/12/2 17:36
 */
public class BooleanUtil {
    private BooleanUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean compare(Boolean b1, Boolean b2) {
        boolean isEqual = false;
        if (b1 == null && b2 == null) {
            isEqual = true;
        } else if (b1 != null && b2 != null) {
            isEqual = b1.equals(b2);
        }
        return isEqual;
    }
}
