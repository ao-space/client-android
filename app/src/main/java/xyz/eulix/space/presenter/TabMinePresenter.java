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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.DeviceNetworkEvent;
import xyz.eulix.space.event.StorageInfoRequestEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.network.agent.disk.DiskManageInfo;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.util.ClipboardUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description: 我的Presenter
 * History:     2021/7/16
 */
public class TabMinePresenter extends AbsPresenter<TabMinePresenter.ITabMine> {
    public interface ITabMine extends IBaseView {
        void onRefreshMessageAllRead(boolean isAllRead);
    }

    //查询当前缓存
    public long checkCacheSize(Context context) {
        long size = 0L;
        String cachePath = context.getExternalCacheDir().getAbsolutePath();
        File cacheDirFile = new File(cachePath);
        if (cacheDirFile.exists()) {
            size = FileUtil.getFolderSize(cacheDirFile);
        }
        return size;
    }

    //清理缓存
    public void clearCache(Context context, ResultCallback callback) {
        ThreadPool.getInstance().execute(() -> {
            //清空文件夹
            String cachePath = context.getExternalCacheDir().getAbsolutePath();
            File cacheDirFile = new File(cachePath);
            if (cacheDirFile.exists()) {
                FileUtil.clearFolder(cacheDirFile);
            }
            //删除下载数据库内容
            TransferDBManager.getInstance(context).deleteByType(TransferHelper.TYPE_CACHE);
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onResult(true, null);
            });
        });

//        iView.onRefreshCacheSize("0.0B");
    }

    public String getActiveBoxDomain() {
        String boxDomain = null;
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (eulixBoxBaseInfo != null) {
            boxDomain = eulixBoxBaseInfo.getBoxDomain();
        }
        return boxDomain;
    }

    public boolean isPhysicalDevice() {
        return isPhysicalDevice(EulixSpaceDBUtil.getActiveDeviceAbility(context, true));
    }

    public EulixBoxInfo getBoxInfo() {
        EulixBoxInfo eulixBoxInfo = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }

        String boxUuid = null;
        String boxBind = null;

        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                    boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    String eulixBoxInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                    if (eulixBoxInfoValue != null) {
                        try {
                            eulixBoxInfo = new Gson().fromJson(eulixBoxInfoValue, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
        return eulixBoxInfo;
    }

    public UserInfo getUserInfo() {
        UserInfo userInfo = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null && !boxValues.isEmpty()) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    boolean isGranter = ("1".equals(boxBind) || "-1".equals(boxBind));
                    userInfo = DataUtil.getSpecificUserInfo(boxValue, (isGranter
                            ? DataUtil.getClientUuid(context) : boxBind), isGranter);
                    break;
                }
            }
        }
        return userInfo;
    }

    public boolean isActiveUserAdmin() {
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == EulixSpaceDBUtil.getActiveDeviceUserIdentity(context));
    }

    public UserInfo getActiveUserInfo() {
        return EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
    }

    public AOSpaceAccessBean getActiveAOSpaceAccessBean() {
        return EulixSpaceDBUtil.getActiveAOSpaceBean(context);
    }

    public int getIdentity() {
        return EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
    }

    public DeviceAbility getActiveDeviceAbility() {
        return EulixSpaceDBUtil.getActiveDeviceAbility(context, true);
    }

    public boolean isDiskExpand() {
        boolean isExpand = false;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            DiskManageListResult diskManageListResult = eulixBoxInfo.getDiskManageListResult();
            if (diskManageListResult != null) {
                List<DiskManageInfo> diskManageInfos = diskManageListResult.getDiskManageInfos();
                if (diskManageInfos != null) {
                    for (DiskManageInfo diskManageInfo : diskManageInfos) {
                        if (diskManageInfo != null) {
                            Integer diskException = diskManageInfo.getDiskException();
                            if (diskException != null) {
                                switch (diskException) {
                                    case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_NOT_EXPAND:
                                    case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_EXPANDING:
                                        isExpand = true;
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        if (isExpand) {
                            break;
                        }
                    }
                }
            }
        }
        return isExpand;
    }

    public boolean copyWebUrl(String webUrl) {
        return ClipboardUtil.setClipData(context, webUrl);
    }

    public void queryMessageAllRead() {
        try {
            ThreadPool.getInstance().execute(() -> {
                boolean isAllRead = true;
                String boxUuid = null;
                String boxBind = null;
                EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
                if (eulixBoxBaseInfo != null) {
                    boxUuid = eulixBoxBaseInfo.getBoxUuid();
                    boxBind = eulixBoxBaseInfo.getBoxBind();
                }
                if (boxUuid != null && boxBind != null) {
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                    List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
                    if (pushValues != null) {
                        for (Map<String, String> pushValue : pushValues) {
                            if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                                String consumeValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                                int consume = -1;
                                if (consumeValue != null) {
                                    try {
                                        consume = Integer.parseInt(consumeValue);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (consume >= 0 && consume <= 3) {
                                    isAllRead = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (iView != null) {
                    iView.onRefreshMessageAllRead(isAllRead);
                }
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void updateStorageInfo() {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            String boxDomain = eulixBoxBaseInfo.getBoxDomain();
            if (boxUuid != null && boxDomain != null) {
                StorageInfoRequestEvent requestEvent = new StorageInfoRequestEvent(boxUuid, boxBind, boxDomain);
                EventBusUtil.post(requestEvent);
                if (NetUtils.isNetAvailable(EulixSpaceApplication.getContext())) {
                    DeviceNetworkEvent deviceNetworkEvent = new DeviceNetworkEvent(boxUuid, boxBind, boxDomain);
                    EventBusUtil.post(deviceNetworkEvent);
                }
            }
        }
    }
}
