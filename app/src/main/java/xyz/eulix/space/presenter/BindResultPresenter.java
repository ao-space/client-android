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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 15:29
 */
public class BindResultPresenter extends AbsPresenter<BindResultPresenter.IBindResult> {
    public interface IBindResult extends IBaseView {}

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
}
