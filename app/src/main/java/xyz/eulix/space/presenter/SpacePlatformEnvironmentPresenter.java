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

package xyz.eulix.space.presenter;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.SwitchPlatformTaskBean;
import xyz.eulix.space.bean.developer.SpacePlatformInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ClipboardUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FormatUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/17 17:01
 */
public class SpacePlatformEnvironmentPresenter extends AbsPresenter<SpacePlatformEnvironmentPresenter.ISpacePlatformEnvironment> {
    private String mBoxUuid;
    private String mBoxBind;

    public interface ISpacePlatformEnvironment extends IBaseView {}

    public EulixBoxBaseInfo generateBoxUuidAndBind() {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (eulixBoxBaseInfo != null) {
            mBoxUuid = eulixBoxBaseInfo.getBoxUuid();
            mBoxBind = eulixBoxBaseInfo.getBoxBind();
        }
        return eulixBoxBaseInfo;
    }

    public SpacePlatformInfo getSpacePlatformInfo() {
        EulixBoxBaseInfo eulixBoxBaseInfo = generateBoxUuidAndBind();
        SpacePlatformInfo spacePlatformInfo = null;
        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getBoxOtherInfo(context, mBoxUuid, mBoxBind);
        if (eulixBoxOtherInfo != null) {
            spacePlatformInfo = eulixBoxOtherInfo.getSpacePlatformInfo();
        }
        if (spacePlatformInfo == null) {
            if (eulixBoxBaseInfo != null) {
                String boxDomain = eulixBoxBaseInfo.getBoxDomain();
                if (boxDomain != null) {
                    if (!(boxDomain.startsWith("http://") || boxDomain.startsWith("https://"))) {
                        boxDomain = "https://" + boxDomain;
                    }
                    Uri uri = null;
                    try {
                        uri = Uri.parse(boxDomain);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String subHost = null;
                    if (uri != null) {
                        String host = uri.getHost();
                        if (host != null) {
                            int pointIndex = host.indexOf(".");
                            if (pointIndex >= 0 && (pointIndex + 1) < host.length()) {
                                subHost = host.substring((pointIndex + 1));
                            }
                        }
                    }
                    if (subHost != null && !TextUtils.isEmpty(subHost)) {
                        boolean isOfficialSpaceDomain = true;
                        String serverUrl = null;
                        switch (subHost) {
                            case ConstantField.URL.PROD_SPACE_API:
                                serverUrl = ConstantField.URL.PROD_WEB_BASE_URL;
                                break;
                            case ConstantField.URL.RC_SPACE_API:
                                serverUrl = ConstantField.URL.RC_WEB_BASE_URL;
                                break;
                            case ConstantField.URL.DEV_SPACE_API:
                                serverUrl = ConstantField.URL.DEV_WEB_BASE_URL;
                                break;
                            case ConstantField.URL.TEST_SPACE_API:
                                serverUrl = ConstantField.URL.TEST_WEB_BASE_URL;
                                break;
                            case ConstantField.URL.QA_SPACE_API:
                                serverUrl = ConstantField.URL.QA_WEB_BASE_URL;
                                break;
                            case ConstantField.URL.SIT_SPACE_API:
                                serverUrl = ConstantField.URL.SIT_WEB_BASE_URL;
                                break;
                            default:
                                isOfficialSpaceDomain = false;
                                serverUrl = "https://" + subHost;
                                break;
                        }
                        spacePlatformInfo = new SpacePlatformInfo();
                        spacePlatformInfo.setPrivateSpacePlatform(!isOfficialSpaceDomain);
                        spacePlatformInfo.setPlatformServerUrl(serverUrl);
                    }
                }
            }
        }
        return spacePlatformInfo;
    }

    public boolean copyWebUrl(String webUrl) {
        return ClipboardUtil.setClipData(context, webUrl);
    }

    public boolean updateBoxDomain(String boxDomain) {
        boolean isHandle = false;
        if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(boxDomain)) && AOSpaceUtil.isInternetAccessEnable(context, mBoxUuid, mBoxBind)) {
            isHandle = true;
            EulixSpaceDBUtil.updateBoxDomain(context, mBoxUuid, mBoxBind, boxDomain);
        }
        return isHandle;
    }

    public List<SwitchPlatformTaskBean> getTaskIds() {
        List<SwitchPlatformTaskBean> taskIds = null;
        if (mBoxUuid != null && mBoxBind != null) {
            taskIds = DataUtil.getSwitchPlatformTaskList(context, mBoxUuid, mBoxBind);
        }
        return taskIds;
    }

    @NonNull
    public String addTaskId(boolean isPrivatePlatform, String platformUrl) {
        long taskTimestamp = System.currentTimeMillis();
        String taskId = String.valueOf(taskTimestamp);
        if (mBoxUuid != null && mBoxBind != null) {
            List<SwitchPlatformTaskBean> taskIds = DataUtil.getSwitchPlatformTaskList(context, mBoxUuid, mBoxBind);
            if (taskIds == null) {
                taskIds = new ArrayList<>();
            } else {
                for (SwitchPlatformTaskBean bean : taskIds) {
                    if (bean != null && taskId.equals(bean.getTaskId())) {
                        taskId = taskId + "_" + UUID.randomUUID().toString();
                        break;
                    }
                }
            }
            SwitchPlatformTaskBean switchPlatformTaskBean = new SwitchPlatformTaskBean();
            switchPlatformTaskBean.setTaskId(taskId);
            switchPlatformTaskBean.setTaskTimestamp(taskTimestamp);
            switchPlatformTaskBean.setPrivatePlatform(isPrivatePlatform);
            switchPlatformTaskBean.setPlatformUrl(platformUrl);
            taskIds.add(switchPlatformTaskBean);
            DataUtil.setSwitchPlatformTaskList(context, mBoxUuid, mBoxBind, taskIds, false);
        }
        return taskId;
    }

    public List<SwitchPlatformTaskBean> removeTaskId(String taskId, boolean isImmediate) {
        List<SwitchPlatformTaskBean> switchPlatformTaskBeanList = null;
        if (mBoxUuid != null && mBoxBind != null && taskId != null) {
            List<SwitchPlatformTaskBean> taskIds = DataUtil.getSwitchPlatformTaskList(context, mBoxUuid, mBoxBind);
            if (taskIds != null) {
                Iterator<SwitchPlatformTaskBean> iterator = taskIds.iterator();
                while (iterator.hasNext()) {
                    SwitchPlatformTaskBean bean = iterator.next();
                    if (bean != null && taskId.equals(bean.getTaskId())) {
                        if (switchPlatformTaskBeanList == null) {
                            switchPlatformTaskBeanList = new ArrayList<>();
                        }
                        switchPlatformTaskBeanList.add(bean);
                        iterator.remove();
                    }
                }
                DataUtil.setSwitchPlatformTaskList(context, mBoxUuid, mBoxBind, taskIds, isImmediate);
            }
        }
        return switchPlatformTaskBeanList;
    }
}
