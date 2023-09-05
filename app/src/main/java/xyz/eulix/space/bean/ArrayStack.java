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

import java.util.ArrayList;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/13 11:15
 */
public class ArrayStack<E> extends ArrayList<E> {
    public boolean empty() {
        return super.isEmpty();
    }

    public E peek() {
        E e = null;
        int size = super.size();
        if (size > 0) {
            e = super.get((size - 1));
        }
        return e;
    }

    public void push(E e) {
        super.add(e);
    }

    public E pop() {
        E e = null;
        int size = super.size();
        if (size > 0) {
            int index = (size - 1);
            e = super.get(index);
            try {
                e = super.remove(index);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return e;
    }
}
