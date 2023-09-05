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
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.agent.InitialCallback;
import xyz.eulix.space.network.agent.disk.DiskUtil;
import xyz.eulix.space.network.agent.disk.ReadyCheckCallback;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 18:32
 */
public class InitSpacePresenter extends AbsPresenter<InitSpacePresenter.IInitSpace> {
    private Integer mResult;
    private InitialCallback initialCallback = new InitialCallback() {
        @Override
        public void onSuccess(String message, Integer code, Integer result) {
            mResult = code;
            if (iView != null) {
                iView.initialResult(code);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.initialResult(null);
            }
        }

        @Override
        public void onError(String msg, String url, String bleKey, String bleIv) {
            initialize(url, bleKey, bleIv);
        }

        @Override
        public void onError(String msg, String url, String password, String bleKey, String bleIv) {
            initialize(url, password, bleKey, bleIv);
        }
    };

    public interface IInitSpace extends IBaseView {
        void initialResult(Integer result);
        void handleSpaceReadyCheck(String source, int code, ReadyCheckResult result);
    }

    public void requestUseBox(String boxUuid, boolean isDiskInitialize) {
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isDiskInitialize ? ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED : ConstantField.EulixDeviceStatus.REQUEST_USE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        EulixSpaceDBUtil.updateBox(context, boxValue);
    }

//    private void changeActiveBox(String boxUuid, long currentTimestamp, long expireTimestamp) {
//        EulixSpaceDBUtil.readAppointPush(context, boxUuid, "1", true);
//        List<Map<String, String>> activeBoxValues = EulixSpaceDBUtil.queryBox(context
//                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
//        if (activeBoxValues != null) {
//            for (Map<String, String> activeBoxValue : activeBoxValues) {
//                if (activeBoxValue != null && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
//                        && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
//                    String activeBoxUuid = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
//                    String activeBoxBind = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
//                    Integer activeAlarmId = DataUtil.getTokenAlarmId(activeBoxUuid, activeBoxBind);
//                    if (activeAlarmId != null) {
//                        AlarmUtil.cancelAlarm(context, activeAlarmId);
//                    }
//                    String isBindValue = "";
//                    if (activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
//                        isBindValue = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
//                    }
//                    Map<String, String> requestUseBoxValue = new HashMap<>();
//                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, activeBoxUuid);
//                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, activeBoxBind);
//                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf((("1".equals(isBindValue) || "-1".equals(isBindValue))
//                            ? ConstantField.EulixDeviceStatus.REQUEST_USE
//                            : ConstantField.EulixDeviceStatus.REQUEST_LOGIN)));
//                    EulixSpaceDBUtil.updateBox(context, requestUseBoxValue);
//                }
//            }
//        }
//        List<Map<String, String>> offlineUseBoxValues = EulixSpaceDBUtil.queryBox(context
//                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
//        if (offlineUseBoxValues != null) {
//            for (Map<String, String> offlineUseBox : offlineUseBoxValues) {
//                if (offlineUseBox != null && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
//                        && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
//                    String offlineUseBoxUuid = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_UUID);
//                    String offlineUseBoxBind = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_BIND);
//                    Map<String, String> offlineBoxValue = new HashMap<>();
//                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, offlineUseBoxUuid);
//                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, offlineUseBoxBind);
//                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE));
//                    EulixSpaceDBUtil.updateBox(context, offlineBoxValue);
//                }
//            }
//        }
//        Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, "1");
//        if (boxAlarmId != null) {
//            AlarmUtil.cancelAlarm(context, boxAlarmId);
//        }
//        Map<String, String> boxValue = new HashMap<>();
//        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
//        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
//        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
//        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
//        EulixSpaceDBUtil.updateBox(context, boxValue);
//        EulixBoxTokenDetail eulixBoxTokenDetail = new EulixBoxTokenDetail();
//        eulixBoxTokenDetail.setBoxUuid(boxUuid);
//        eulixBoxTokenDetail.setBoxBind("1");
//        eulixBoxTokenDetail.setTokenExpire(expireTimestamp);
//        DataUtil.setLastBoxToken(eulixBoxTokenDetail);
//        DataUtil.setLastEulixSpace(context, boxUuid, "1");
//        int alarmId = AlarmUtil.getAlarmId();
//        DataUtil.setTokenAlarmId(boxUuid, "1", alarmId);
//        long diffTimestamp = 60 * 1000L;
//        if (expireTimestamp > currentTimestamp) {
//            diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
//            AlarmUtil.setAlarm(context, (expireTimestamp - diffTimestamp), alarmId, boxUuid, "1", (diffTimestamp / 2));
//        } else {
//            AlarmUtil.setAlarm(context, (currentTimestamp + diffTimestamp), alarmId, boxUuid, "1", (diffTimestamp / 2));
//        }
//        EulixSpaceDBUtil.offlineTemperateBox(context);
//        EventBusUtil.post(new BoxOnlineRequestEvent(true));
//        EventBusUtil.post(new SpaceChangeEvent(true));
//    }

    public void initialize(String baseUrl, String bleKey, String bleIv) {
        try {
            ThreadPool.getInstance().execute(() -> AgentUtil.initial(baseUrl, bleKey, bleIv, initialCallback));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void initialize(String baseUrl, String password, String bleKey, String bleIv) {
        try {
            ThreadPool.getInstance().execute(() -> AgentUtil.initial(baseUrl, password, bleKey, bleIv, initialCallback));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void spaceReadyCheck(String baseUrl, String bleKey, String bleIv) {
        try {
            ThreadPool.getInstance().execute(() -> DiskUtil.getSpaceReadyCheck(baseUrl, bleKey, bleIv, new ReadyCheckCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String requestId, ReadyCheckResult result) {
                    if (iView != null) {
                        iView.handleSpaceReadyCheck(source, code, result);
                    }
                }

                @Override
                public void onFail(int code, String source, String message, String requestId) {
                    if (iView != null) {
                        iView.handleSpaceReadyCheck(source, code, null);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    spaceReadyCheck(baseUrl, bleKey, bleIv);
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

//    public boolean handleDBChange(String boxUuid) {
//        boolean result = false;
//        long currentTimestamp = System.currentTimeMillis();
//        long expireTimestamp = 0L;
//        if (boxUuid != null) {
//            Map<String, String> queryMap = new HashMap<>();
//            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
//            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
//            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
//            if (boxValues != null) {
//                for (Map<String, String> boxValue : boxValues) {
//                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
//                        String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
//                        if (boxTokenValue != null) {
//                            EulixBoxToken eulixBoxToken = null;
//                            try {
//                                eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
//                            } catch (JsonSyntaxException e) {
//                                e.printStackTrace();
//                            }
//                            if (eulixBoxToken != null) {
//                                expireTimestamp = eulixBoxToken.getTokenExpire();
//                                result = (expireTimestamp > currentTimestamp);
//                            }
//                        }
//                        break;
//                    }
//                }
//            }
//        }
//        if (result) {
//            changeActiveBox(boxUuid, currentTimestamp, expireTimestamp);
//        }
//        return result;
//    }
}
