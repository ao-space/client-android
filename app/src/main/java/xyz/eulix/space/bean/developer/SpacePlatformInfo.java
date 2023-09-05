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

package xyz.eulix.space.bean.developer;

import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/17 14:08
 */
public class SpacePlatformInfo {
    private boolean isPrivateSpacePlatform;
    private String platformServerUrl;

    public static boolean compare(SpacePlatformInfo spacePlatformInfo1, SpacePlatformInfo spacePlatformInfo2) {
        boolean isEqual = false;
        if (spacePlatformInfo1 == null && spacePlatformInfo2 == null) {
            isEqual = true;
        } else if (spacePlatformInfo1 != null && spacePlatformInfo2 != null) {
            boolean isPrivateSpacePlatform1 = spacePlatformInfo1.isPrivateSpacePlatform;
            String platformServerUrl1 = spacePlatformInfo1.platformServerUrl;
            boolean isPrivateSpacePlatform2 = spacePlatformInfo2.isPrivateSpacePlatform;
            String platformServerUrl2 = spacePlatformInfo2.platformServerUrl;
            if (isPrivateSpacePlatform1 == isPrivateSpacePlatform2 && StringUtil.compare(platformServerUrl1, platformServerUrl2)) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    public boolean isPrivateSpacePlatform() {
        return isPrivateSpacePlatform;
    }

    public void setPrivateSpacePlatform(boolean privateSpacePlatform) {
        isPrivateSpacePlatform = privateSpacePlatform;
    }

    public String getPlatformServerUrl() {
        return platformServerUrl;
    }

    public void setPlatformServerUrl(String platformServerUrl) {
        this.platformServerUrl = platformServerUrl;
    }

    @Override
    public String toString() {
        return "SpacePlatformInfo{" +
                "isPrivateSpacePlatform=" + isPrivateSpacePlatform +
                ", platformServerUrl='" + platformServerUrl + '\'' +
                '}';
    }
}
