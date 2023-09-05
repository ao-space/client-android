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

import org.greenrobot.eventbus.EventBus;

/**
 * Author:      Zhu Fuyu
 * Description: EventBus工具类
 * History:     2021/8/26
 */
public class EventBusUtil {

    public static void register(Object any) {
        if (!EventBus.getDefault().isRegistered(any)) {
            EventBus.getDefault().register(any);
        }
    }

    public static void  unRegister(Object any) {
        EventBus.getDefault().unregister(any);
    }

    public static void  post(Object any) {
        EventBus.getDefault().post(any);
    }

    public static void  postSticky(Object any) {
        EventBus.getDefault().postSticky(any);
    }
}
