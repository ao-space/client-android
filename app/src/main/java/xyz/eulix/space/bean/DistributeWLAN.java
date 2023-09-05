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

import java.util.List;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 14:50
 */
public class DistributeWLAN {
    private boolean isConnect = false;
    private String connectedWlanSsid;
    private List<String> ipAddresses;
    private List<NetworkAccessBean> networkAccessBeanList;
    private List<WLANItem> wlanItemList;
    private boolean networkConfigEnable = true;

    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }

    public String getConnectedWlanSsid() {
        return connectedWlanSsid;
    }

    public void setConnectedWlanSsid(String connectedWlanSsid) {
        this.connectedWlanSsid = connectedWlanSsid;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public List<NetworkAccessBean> getNetworkAccessBeanList() {
        return networkAccessBeanList;
    }

    public void setNetworkAccessBeanList(List<NetworkAccessBean> networkAccessBeanList) {
        this.networkAccessBeanList = networkAccessBeanList;
    }

    public List<WLANItem> getWlanItemList() {
        return wlanItemList;
    }

    public void setWlanItemList(List<WLANItem> wlanItemList) {
        this.wlanItemList = wlanItemList;
    }

    public boolean isNetworkConfigEnable() {
        return networkConfigEnable;
    }

    public void setNetworkConfigEnable(boolean networkConfigEnable) {
        this.networkConfigEnable = networkConfigEnable;
    }

    @Override
    public String toString() {
        return "DistributeWLAN{" +
                "isConnect=" + isConnect +
                ", connectedWlanSsid='" + connectedWlanSsid + '\'' +
                ", ipAddresses=" + ipAddresses +
                ", networkAccessBeanList=" + networkAccessBeanList +
                ", wlanItemList=" + wlanItemList +
                ", networkConfigEnable=" + networkConfigEnable +
                '}';
    }
}
