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

/**
 * Author:      Zhu Fuyu
 * Description: 局域网设备信息
 * History:     2023/3/17
 */
public class LanDeviceInfoBean {
    //设备型号
    public String devicemodel;
    //ip地址
    public String ipAddress;
    //http端口号
    public String webport;
    //https端口号
    public String sslport;
    //beidhase 用于判断是否为傲空间设备
    public String btidhash;
}
