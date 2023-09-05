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

package xyz.eulix.space.network.agent.net;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/30 18:02
 */
public class NetworkAdapter implements EulixKeep {
    private String adapterName;
    private boolean connected;
    private String defaultGateway;
    private String ipv4;
    private boolean ipv4UseDhcp;
    private String ipv6;
    private String ipv6DefaultGateway;
    private boolean ipv6UseDhcp;
    private String mACAddress;
    private String subNetMask;
    private String subNetPreLen;
    private String wIFIAddress;
    private String wIFIName;
    private String wIFIPassword;
    private boolean wired;

    public NetworkAdapter cloneSelf() {
        NetworkAdapter adapter = new NetworkAdapter();
        adapter.setAdapterName(getAdapterName());
        adapter.setConnected(isConnected());
        adapter.setDefaultGateway(getDefaultGateway());
        adapter.setIpv4(getIpv4());
        adapter.setIpv4UseDhcp(isIpv4UseDhcp());
        adapter.setIpv6(getIpv6());
        adapter.setIpv6DefaultGateway(getIpv6DefaultGateway());
        adapter.setIpv6UseDhcp(isIpv6UseDhcp());
        adapter.setmACAddress(getmACAddress());
        adapter.setSubNetMask(getSubNetMask());
        adapter.setSubNetPreLen(getSubNetPreLen());
        adapter.setwIFIAddress(getwIFIAddress());
        adapter.setwIFIName(getwIFIName());
        adapter.setwIFIPassword(getwIFIPassword());
        adapter.setWired(isWired());
        return adapter;
    }

    public String getAdapterName() {
        return adapterName;
    }

    public void setAdapterName(String adapterName) {
        this.adapterName = adapterName;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getDefaultGateway() {
        return defaultGateway;
    }

    public void setDefaultGateway(String defaultGateway) {
        this.defaultGateway = defaultGateway;
    }

    public String getIpv4() {
        return ipv4;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }

    public boolean isIpv4UseDhcp() {
        return ipv4UseDhcp;
    }

    public void setIpv4UseDhcp(boolean ipv4UseDhcp) {
        this.ipv4UseDhcp = ipv4UseDhcp;
    }

    public String getIpv6() {
        return ipv6;
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
    }

    public String getIpv6DefaultGateway() {
        return ipv6DefaultGateway;
    }

    public void setIpv6DefaultGateway(String ipv6DefaultGateway) {
        this.ipv6DefaultGateway = ipv6DefaultGateway;
    }

    public boolean isIpv6UseDhcp() {
        return ipv6UseDhcp;
    }

    public void setIpv6UseDhcp(boolean ipv6UseDhcp) {
        this.ipv6UseDhcp = ipv6UseDhcp;
    }

    public String getmACAddress() {
        return mACAddress;
    }

    public void setmACAddress(String mACAddress) {
        this.mACAddress = mACAddress;
    }

    public String getSubNetMask() {
        return subNetMask;
    }

    public void setSubNetMask(String subNetMask) {
        this.subNetMask = subNetMask;
    }

    public String getSubNetPreLen() {
        return subNetPreLen;
    }

    public void setSubNetPreLen(String subNetPreLen) {
        this.subNetPreLen = subNetPreLen;
    }

    public String getwIFIAddress() {
        return wIFIAddress;
    }

    public void setwIFIAddress(String wIFIAddress) {
        this.wIFIAddress = wIFIAddress;
    }

    public String getwIFIName() {
        return wIFIName;
    }

    public void setwIFIName(String wIFIName) {
        this.wIFIName = wIFIName;
    }

    public String getwIFIPassword() {
        return wIFIPassword;
    }

    public void setwIFIPassword(String wIFIPassword) {
        this.wIFIPassword = wIFIPassword;
    }

    public boolean isWired() {
        return wired;
    }

    public void setWired(boolean wired) {
        this.wired = wired;
    }

    @Override
    public String toString() {
        return "NetworkAdapter{" +
                "adapterName='" + adapterName + '\'' +
                ", connected=" + connected +
                ", defaultGateway='" + defaultGateway + '\'' +
                ", ipv4='" + ipv4 + '\'' +
                ", ipv4UseDhcp=" + ipv4UseDhcp +
                ", ipv6='" + ipv6 + '\'' +
                ", ipv6DefaultGateway='" + ipv6DefaultGateway + '\'' +
                ", ipv6UseDhcp=" + ipv6UseDhcp +
                ", mACAddress='" + mACAddress + '\'' +
                ", subNetMask='" + subNetMask + '\'' +
                ", subNetPreLen='" + subNetPreLen + '\'' +
                ", wIFIAddress='" + wIFIAddress + '\'' +
                ", wIFIName='" + wIFIName + '\'' +
                ", wIFIPassword='" + wIFIPassword + '\'' +
                ", wired=" + wired +
                '}';
    }
}
