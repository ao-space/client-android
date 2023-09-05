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
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 14:49
 */
public class WLANItem {
    private String wlanSsid;
    private String wlanAddress;
    private int wlanState;

    public String getWlanSsid() {
        return wlanSsid;
    }

    public void setWlanSsid(String wlanSsid) {
        this.wlanSsid = wlanSsid;
    }

    public String getWlanAddress() {
        return wlanAddress;
    }

    public void setWlanAddress(String wlanAddress) {
        this.wlanAddress = wlanAddress;
    }

    public int getWlanState() {
        return wlanState;
    }

    public void setWlanState(int wlanState) {
        this.wlanState = wlanState;
    }

    @Override
    public String toString() {
        return "WLANItem{" +
                "wlanSsid='" + wlanSsid + '\'' +
                ", wlanAddress='" + wlanAddress + '\'' +
                ", wlanState=" + wlanState +
                '}';
    }
}
