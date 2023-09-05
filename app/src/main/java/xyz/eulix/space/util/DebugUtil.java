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

package xyz.eulix.space.util;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.developer.SpacePlatformInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/31 14:08
 */
public class DebugUtil {
    private static boolean isDebug = false;
    private static boolean isTest = false;
    private static boolean isDev = false;
    private static int debugDevice = 10064;
    private static int environmentIndex = 0;

    private DebugUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setDebug(boolean isDebug) {
        DebugUtil.isDebug = isDebug;
    }

    public static boolean isTest() {
        return isTest;
    }

    public static void setTest(boolean isTest) {
        DebugUtil.isTest = isTest;
    }

    public static boolean isDev() {
        return isDev;
    }

    public static void setDev(boolean isDev) {
        DebugUtil.isDev = isDev;
    }

    public static int getDebugDevice() {
        return debugDevice;
    }

    public static void setDebugDevice(int debugDevice) {
        DebugUtil.debugDevice = debugDevice;
    }

    public static int getEnvironmentIndex() {
        int boxEnvironment = environmentIndex;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance();
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.BOX_ENVIRONMENT)) {
            Integer boxEnvironmentValue = eulixSpaceSharePreferenceHelper.getInt(ConstantField.EulixSpaceSPKey.BOX_ENVIRONMENT, environmentIndex);
            if (boxEnvironmentValue != null) {
                boxEnvironment = boxEnvironmentValue;
            }
        }
        environmentIndex = boxEnvironment;
        return boxEnvironment;
    }

    public static void setEnvironmentIndex(int environmentIndex) {
        DebugUtil.environmentIndex = environmentIndex;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance();
        if (eulixSpaceSharePreferenceHelper != null) {
            eulixSpaceSharePreferenceHelper.setInt(ConstantField.EulixSpaceSPKey.BOX_ENVIRONMENT, environmentIndex, true);
        }
    }

    public static String getOfficialEnvironmentWeb() {
        String web;
        switch (environmentIndex) {
            case -1:
                web = ConstantField.URL.EULIX_XYZ_URL;
                break;
            case 1:
                web = ConstantField.URL.DEV_EULIX_XYZ_URL;
                break;
            case 2:
                web = ConstantField.URL.TEST_EULIX_XYZ_URL;
                break;
            case 3:
                web = ConstantField.URL.QA_EULIX_XYZ_URL;
                break;
            case 4:
                web = ConstantField.URL.SIT_EULIX_XYZ_URL;
                break;
            case 5:
                web = ConstantField.URL.AO_SPACE_URL;
                break;
            default:
                web = ConstantField.URL.EULIX_TOP_URL;
                break;
        }
        return web;
    }


    public static String getOfficialEnvironmentServices() {
        String services;
        switch (environmentIndex) {
            case -1:
                services = ConstantField.URL.SERVICE_URL;
                break;
            case 1:
                services = ConstantField.URL.DEV_SERVICE_URL;
                break;
            case 2:
                services = ConstantField.URL.TEST_SERVICE_URL;
                break;
            case 3:
                services = ConstantField.URL.QA_SERVICE_URL;
                break;
            case 4:
                services = ConstantField.URL.SIT_SERVICE_URL;
                break;
            case 5:
                services = ConstantField.URL.SERVICE_AO_SPACE_URL;
                break;
            default:
                services = ConstantField.URL.SERVICE_TOP_URL;
                break;
        }
        return services;
    }

    public static String getEnvironmentServices() {
        SpacePlatformInfo spacePlatformInfo = null;
        String services;
        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getActiveBoxOtherInfo(EulixSpaceApplication.getContext());
        if (eulixBoxOtherInfo != null) {
            spacePlatformInfo = eulixBoxOtherInfo.getSpacePlatformInfo();
        }
        if (spacePlatformInfo != null && spacePlatformInfo.isPrivateSpacePlatform() && spacePlatformInfo.getPlatformServerUrl() != null) {
            services = spacePlatformInfo.getPlatformServerUrl();
            if (!services.endsWith("/")) {
                services = services + "/";
            }
        } else {
            switch (environmentIndex) {
                case -1:
                    services = ConstantField.URL.SERVICE_URL;
                    break;
                case 1:
                    services = ConstantField.URL.DEV_SERVICE_URL;
                    break;
                case 2:
                    services = ConstantField.URL.TEST_SERVICE_URL;
                    break;
                case 3:
                    services = ConstantField.URL.QA_SERVICE_URL;
                    break;
                case 4:
                    services = ConstantField.URL.SIT_SERVICE_URL;
                    break;
                case 5:
                    services = ConstantField.URL.SERVICE_AO_SPACE_URL;
                    break;
                default:
                    services = ConstantField.URL.SERVICE_TOP_URL;
                    break;
            }
        }
        return services;
    }

    public static String getEnvironmentServices(int boxEnvironment) {
        SpacePlatformInfo spacePlatformInfo = null;
        String services;
        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getActiveBoxOtherInfo(EulixSpaceApplication.getContext());
        if (eulixBoxOtherInfo != null) {
            spacePlatformInfo = eulixBoxOtherInfo.getSpacePlatformInfo();
        }
        if (spacePlatformInfo != null && spacePlatformInfo.isPrivateSpacePlatform()) {
            services = spacePlatformInfo.getPlatformServerUrl();
            if (!services.endsWith("/")) {
                services = services + "/";
            }
        } else {
            switch (boxEnvironment) {
                case -1:
                    services = ConstantField.URL.SERVICE_URL;
                    break;
                case 1:
                    services = ConstantField.URL.DEV_SERVICE_URL;
                    break;
                case 2:
                    services = ConstantField.URL.TEST_SERVICE_URL;
                    break;
                case 3:
                    services = ConstantField.URL.QA_SERVICE_URL;
                    break;
                case 4:
                    services = ConstantField.URL.SIT_SERVICE_URL;
                    break;
                case 5:
                    services = ConstantField.URL.SERVICE_AO_SPACE_URL;
                    break;
                default:
                    services = ConstantField.URL.SERVICE_TOP_URL;
                    break;
            }
        }
        return services;
    }

}
