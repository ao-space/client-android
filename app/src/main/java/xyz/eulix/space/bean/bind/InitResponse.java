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

package xyz.eulix.space.bean.bind;

import java.util.List;

import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/29 16:22
 */
public class InitResponse implements EulixKeep {
    public static final int PAIRED_BOUND = 0;
    public static final int PAIRED_NEW_DEVICE = PAIRED_BOUND + 1;
    private String key;
    private String iv;
    private boolean newBindProcessSupport = false;
    private String boxName;
    private String boxUuid;
    private String clientUuid;
    private String productId;
    // 0: 已绑定；1：新设备；2：已绑定过但当前没有绑定的设备
    private int paired;
    private int connected;
    private DeviceAbility deviceAbility;
    private int initialEstimateTimeSec;
    private List<InitResponseNetwork> network;
    private String generationEn;
    private String spaceVersion;
    private String sspUrl;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public boolean isNewBindProcessSupport() {
        return newBindProcessSupport;
    }

    public void setNewBindProcessSupport(boolean newBindProcessSupport) {
        this.newBindProcessSupport = newBindProcessSupport;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getPaired() {
        return paired;
    }

    public void setPaired(int paired) {
        this.paired = paired;
    }

    public int getConnected() {
        return connected;
    }

    public void setConnected(int connected) {
        this.connected = connected;
    }

    public DeviceAbility getDeviceAbility() {
        return deviceAbility;
    }

    public void setDeviceAbility(DeviceAbility deviceAbility) {
        this.deviceAbility = deviceAbility;
    }

    public int getInitialEstimateTimeSec() {
        return initialEstimateTimeSec;
    }

    public void setInitialEstimateTimeSec(int initialEstimateTimeSec) {
        this.initialEstimateTimeSec = initialEstimateTimeSec;
    }

    public List<InitResponseNetwork> getNetwork() {
        return network;
    }

    public void setNetwork(List<InitResponseNetwork> network) {
        this.network = network;
    }

    public String getGenerationEn() {
        return generationEn;
    }

    public void setGenerationEn(String generationEn) {
        this.generationEn = generationEn;
    }

    public String getSpaceVersion() {
        return spaceVersion;
    }

    public void setSpaceVersion(String spaceVersion) {
        this.spaceVersion = spaceVersion;
    }

    public String getSspUrl() {
        return sspUrl;
    }

    public void setSspUrl(String sspUrl) {
        this.sspUrl = sspUrl;
    }

    @Override
    public String toString() {
        return "InitResponse{" +
                "key='" + key + '\'' +
                ", iv='" + iv + '\'' +
                ", newBindProcessSupport=" + newBindProcessSupport +
                ", boxName='" + boxName + '\'' +
                ", boxUuid='" + boxUuid + '\'' +
                ", clientUuid='" + clientUuid + '\'' +
                ", productId='" + productId + '\'' +
                ", paired=" + paired +
                ", connected=" + connected +
                ", deviceAbility=" + deviceAbility +
                ", initialEstimateTimeSec=" + initialEstimateTimeSec +
                ", network=" + network +
                ", generationEn='" + generationEn + '\'' +
                ", spaceVersion='" + spaceVersion + '\'' +
                ", sspUrl='" + sspUrl + '\'' +
                '}';
    }
}
