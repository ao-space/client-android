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
 * date: 2022/12/2 15:50
 */
public class NetworkConfigDNSInfo {
    private String ipv4Dns1;
    private String ipv4Dns2;
    private String ipv6Dns1;
    private String ipv6Dns2;

    public String getIpv4Dns1() {
        return ipv4Dns1;
    }

    public void setIpv4Dns1(String ipv4Dns1) {
        this.ipv4Dns1 = ipv4Dns1;
    }

    public String getIpv4Dns2() {
        return ipv4Dns2;
    }

    public void setIpv4Dns2(String ipv4Dns2) {
        this.ipv4Dns2 = ipv4Dns2;
    }

    public String getIpv6Dns1() {
        return ipv6Dns1;
    }

    public void setIpv6Dns1(String ipv6Dns1) {
        this.ipv6Dns1 = ipv6Dns1;
    }

    public String getIpv6Dns2() {
        return ipv6Dns2;
    }

    public void setIpv6Dns2(String ipv6Dns2) {
        this.ipv6Dns2 = ipv6Dns2;
    }
}
