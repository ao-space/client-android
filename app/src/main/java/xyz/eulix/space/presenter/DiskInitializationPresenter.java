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

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressCallback;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.disk.DiskExpandProgressCallback;
import xyz.eulix.space.network.disk.DiskExpandProgressResult;
import xyz.eulix.space.network.disk.DiskUtil;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.GatewayUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/25 9:51
 */
public class DiskInitializationPresenter extends AbsPresenter<DiskInitializationPresenter.IDiskInitialization> {
    public interface IDiskInitialization extends IBaseView {
        void completeStorageDiskManageList();
        void diskExpandResponse(int code, String source);
        void diskExpandProgressResponse(int code, String source, DiskExpandProgressResult result);
    }

    public void requestUseBox(String boxUuid, DiskManageListResult diskManageListResult) {
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.REQUEST_USE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        EulixSpaceDBUtil.updateBox(context, boxValue);
        if (diskManageListResult != null) {
            boolean isStorageDiskManageList = false;
            boolean isHandle = false;
            JSONObject jsonObject = null;
            EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, "1");
            if (eulixSpaceDBBoxManager != null) {
                isHandle = true;
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("diskManageListResult", new Gson().toJson(diskManageListResult, DiskManageListResult.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                    isHandle = false;
                }
            }
            if (isHandle) {
                int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                    if (iView != null) {
                        iView.completeStorageDiskManageList();
                    }
                });
                isStorageDiskManageList = (result >= 0);
            } else {
                isStorageDiskManageList = true;
                EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, "1");
                if (eulixBoxInfo == null) {
                    eulixBoxInfo = new EulixBoxInfo();
                }
                eulixBoxInfo.setDiskManageListResult(diskManageListResult);
                Map<String, String> boxV = new HashMap<>();
                boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
                boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                EulixSpaceDBUtil.updateBox(context, boxV);
            }
            if (isStorageDiskManageList && iView != null) {
                iView.completeStorageDiskManageList();
            }
        }
    }

    /**
     * 更改活跃盒子
     * @param boxUuid
     * @param expireTimestamp
     */
    public void changeActiveBox(String boxUuid, long expireTimestamp) {
        EulixSpaceDBUtil.readAppointPush(context, boxUuid, "1", true);
        List<Map<String, String>> activeBoxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (activeBoxValues != null) {
            for (Map<String, String> activeBoxValue : activeBoxValues) {
                if (activeBoxValue != null && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String activeBoxUuid = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String activeBoxBind = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    Integer activeAlarmId = DataUtil.getTokenAlarmId(activeBoxUuid, activeBoxBind);
                    if (activeAlarmId != null) {
                        AlarmUtil.cancelAlarm(context, activeAlarmId);
                    }
                    String isBindValue = "";
                    if (activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        isBindValue = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    }
                    Map<String, String> requestUseBoxValue = new HashMap<>();
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, activeBoxUuid);
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, activeBoxBind);
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf((("1".equals(isBindValue) || "-1".equals(isBindValue))
                            ? ConstantField.EulixDeviceStatus.REQUEST_USE
                            : ConstantField.EulixDeviceStatus.REQUEST_LOGIN)));
                    EulixSpaceDBUtil.updateBox(context, requestUseBoxValue);
                }
            }
        }
        List<Map<String, String>> offlineUseBoxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        if (offlineUseBoxValues != null) {
            for (Map<String, String> offlineUseBox : offlineUseBoxValues) {
                if (offlineUseBox != null && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String offlineUseBoxUuid = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String offlineUseBoxBind = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    Map<String, String> offlineBoxValue = new HashMap<>();
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, offlineUseBoxUuid);
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, offlineUseBoxBind);
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE));
                    EulixSpaceDBUtil.updateBox(context, offlineBoxValue);
                }
            }
        }
        long currentTimestamp = System.currentTimeMillis();
        Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, "1");
        if (boxAlarmId != null) {
            AlarmUtil.cancelAlarm(context, boxAlarmId);
        }
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
        EulixSpaceDBUtil.updateBox(context, boxValue);
        EulixBoxTokenDetail eulixBoxTokenDetail = new EulixBoxTokenDetail();
        eulixBoxTokenDetail.setBoxUuid(boxUuid);
        eulixBoxTokenDetail.setBoxBind("1");
        eulixBoxTokenDetail.setTokenExpire(expireTimestamp);
        DataUtil.setLastBoxToken(eulixBoxTokenDetail);
        DataUtil.setLastEulixSpace(context, boxUuid, "1");
        int alarmId = AlarmUtil.getAlarmId();
        DataUtil.setTokenAlarmId(boxUuid, "1", alarmId);
        long diffTimestamp = 60 * 1000L;
        if (expireTimestamp > currentTimestamp) {
            diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
            AlarmUtil.setAlarm(context, (expireTimestamp - diffTimestamp), alarmId, boxUuid, "1", (diffTimestamp / 2));
        } else {
            AlarmUtil.setAlarm(context, (currentTimestamp + diffTimestamp), alarmId, boxUuid, "1", (diffTimestamp / 2));
        }
        EulixSpaceDBUtil.offlineTemperateBox(context);
        EventBusUtil.post(new BoxOnlineRequestEvent(true));
        EventBusUtil.post(new SpaceChangeEvent(true));
    }

    public void deleteBox(String boxUuid) {
        String boxBind = "1";
        DataUtil.boxUnavailable(boxUuid, boxBind);
        Map<String, String> deleteMap = new HashMap<>();
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        EulixSpaceDBUtil.deleteBox(context, deleteMap);
        BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
        EventBusUtil.post(boxInsertDeleteEvent);
    }

    public DiskManageListResult getActiveDiskManageListResult() {
        DiskManageListResult result = null;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            result = eulixBoxInfo.getDiskManageListResult();
        }
        return result;
    }

    public DiskManageListResult getDiskManageListResult(String boxUuid, String boxBind) {
        DiskManageListResult result = null;
        if (boxUuid != null && boxBind != null) {
            EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
            if (eulixBoxInfo != null) {
                result = eulixBoxInfo.getDiskManageListResult();
            }
        }
        return result;
    }

    public void diskExpand(List<String> storageHardwareIds, boolean isRaid, List<String> raidDiskHardwareIds) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            DiskUtil.diskExpand(storageHardwareIds, isRaid, raidDiskHardwareIds
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    if (iView != null) {
                        iView.diskExpandResponse(code, source);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.diskExpandResponse(500, null);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.diskExpandResponse(ConstantField.SERVER_EXCEPTION_CODE, null);
                    }
                }
            });
        }
    }

    public void getDiskExpandProgress() {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            DiskUtil.getDiskExpandProgress(gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), true, new DiskExpandProgressCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, DiskExpandProgressResult result) {
                            if (iView != null) {
                                iView.diskExpandProgressResponse(code, source, result);
                            }
                        }

                        @Override
                        public void onFail(int code, String source, String message, String requestId) {
                            if (iView != null) {
                                iView.diskExpandProgressResponse(code, source, null);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (iView != null) {
                                iView.diskExpandProgressResponse(ConstantField.SERVER_EXCEPTION_CODE, null, null);
                            }
                        }
                    });
        }
    }

    public EulixBoxBaseInfo getActiveBoxBaseInfo() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
    }
}
