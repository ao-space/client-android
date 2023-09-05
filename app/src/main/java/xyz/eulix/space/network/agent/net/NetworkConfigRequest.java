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

import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/30 18:41
 */
public class NetworkConfigRequest implements EulixKeep {
    private String dNS1;
    private String dNS2;
    private String ipv6DNS1;
    private String ipv6DNS2;
    private List<NetworkAdapter> networkAdapters;

    public String getdNS1() {
        return dNS1;
    }

    public void setdNS1(String dNS1) {
        this.dNS1 = dNS1;
    }

    public String getdNS2() {
        return dNS2;
    }

    public void setdNS2(String dNS2) {
        this.dNS2 = dNS2;
    }

    public String getIpv6DNS1() {
        return ipv6DNS1;
    }

    public void setIpv6DNS1(String ipv6DNS1) {
        this.ipv6DNS1 = ipv6DNS1;
    }

    public String getIpv6DNS2() {
        return ipv6DNS2;
    }

    public void setIpv6DNS2(String ipv6DNS2) {
        this.ipv6DNS2 = ipv6DNS2;
    }

    public List<NetworkAdapter> getNetworkAdapters() {
        return networkAdapters;
    }

    public void setNetworkAdapters(List<NetworkAdapter> networkAdapters) {
        this.networkAdapters = networkAdapters;
    }

    @Override
    public String toString() {
        return "NetworkConfigRequest{" +
                "dNS1='" + dNS1 + '\'' +
                ", dNS2='" + dNS2 + '\'' +
                ", ipv6DNS1='" + ipv6DNS1 + '\'' +
                ", ipv6DNS2='" + ipv6DNS2 + '\'' +
                ", networkAdapters=" + networkAdapters +
                '}';
    }
}
