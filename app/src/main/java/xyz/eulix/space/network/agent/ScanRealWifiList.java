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

package xyz.eulix.space.network.agent;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/17 14:52
 */
public class ScanRealWifiList implements Serializable, EulixKeep {
    @SerializedName("code")
    private String code;
    @SerializedName("message")
    private String message;
    @SerializedName("results")
    private List<WifiInfo> wifiInfoList;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<WifiInfo> getWifiInfoList() {
        return wifiInfoList;
    }

    public void setWifiInfoList(List<WifiInfo> wifiInfoList) {
        this.wifiInfoList = wifiInfoList;
    }

    @Override
    public String toString() {
        return "ScanRealWifiList{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", wifiInfoList=" + wifiInfoList +
                '}';
    }
}
