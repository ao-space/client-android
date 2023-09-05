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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/29 18:33
 */
public class BoxPairingInfo implements EulixKeep {
    private String boxPubKey;
    private String boxKey;
    private String boxIv;
    private String boxName;
    private String boxUuid;
    private String productId;
    private int pairStatus;

    public String getBoxPubKey() {
        return boxPubKey;
    }

    public void setBoxPubKey(String boxPubKey) {
        this.boxPubKey = boxPubKey;
    }

    public String getBoxKey() {
        return boxKey;
    }

    public void setBoxKey(String boxKey) {
        this.boxKey = boxKey;
    }

    public String getBoxIv() {
        return boxIv;
    }

    public void setBoxIv(String boxIv) {
        this.boxIv = boxIv;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getPairStatus() {
        return pairStatus;
    }

    public void setPairStatus(int pairStatus) {
        this.pairStatus = pairStatus;
    }

    @Override
    public String toString() {
        return "BoxPairingInfo{" +
                "boxPubKey='" + boxPubKey + '\'' +
                ", boxKey='" + boxKey + '\'' +
                ", boxIv='" + boxIv + '\'' +
                ", boxName='" + boxName + '\'' +
                ", boxUuid='" + boxUuid + '\'' +
                ", productId='" + productId + '\'' +
                ", pairStatus=" + pairStatus +
                '}';
    }
}
