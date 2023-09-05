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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.network.userinfo.MemberNameUpdateCallback;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.ui.mine.NickOrSignatureEditActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.PreferenceUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/9/15
 */
public class NickOrSignatureEditPresenter extends AbsPresenter<NickOrSignatureEditPresenter.INickOrSignatureEdit> {
    public interface INickOrSignatureEdit extends IBaseView {
        void onUpdateResult(Boolean result, String errorMsg);
    }

    public void updateUserInfo(Context context, String content, int type) {
        String clientUuid = DataUtil.getCompatibleClientUuid(context);
        String nickname;
        String signature;
        if (type == NickOrSignatureEditActivity.TYPE_NICK) {
            nickname = content;
            signature = null;
        } else {
            signature = content;
            nickname = null;
        }
        String boxUuid = null;
        String boxBind = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.isEmpty()) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                }
            }
        }
        if (boxUuid == null || boxBind == null) {
            iView.onUpdateResult(false, null);
        } else {
            String finalBoxUuid = boxUuid;
            String finalBoxBind = boxBind;
            UserInfoUtil.updateUserInfo(context, nickname, signature, true, (result, errorMsg) -> {
                if (result != null && result) {
                    Map<String, String> userMap = new HashMap<>();
                    if (type == NickOrSignatureEditActivity.TYPE_NICK) {
                        userMap.put(UserInfoUtil.NICKNAME, nickname);
                        PreferenceUtil.saveNickname(context, nickname);
                    } else {
                        userMap.put(UserInfoUtil.SIGNATURE, signature);
                        PreferenceUtil.saveSignature(context, signature);
                    }
                    UserInfoUtil.updateUserInfoDB(context, finalBoxUuid, finalBoxBind, clientUuid, userMap);
                    //发送广播更新
                    EulixBoxBaseInfo boxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
                    if (boxBaseInfo != null) {
                        String activeBoxUuid = boxBaseInfo.getBoxUuid();
                        String activeBoxBind = boxBaseInfo.getBoxBind();
                        if (activeBoxUuid != null && activeBoxUuid.equals(finalBoxUuid)
                                && activeBoxBind != null && activeBoxBind.equals(finalBoxBind)) {
                            if (type == NickOrSignatureEditActivity.TYPE_NICK) {
                                EventBusUtil.post(new UserInfoEvent(UserInfoEvent.TYPE_NAME, null, nickname, null));
                            } else {
                                EventBusUtil.post(new UserInfoEvent(UserInfoEvent.TYPE_SIGN, null, null, signature));
                            }
                        }
                    }
                }
                iView.onUpdateResult(result, errorMsg);
            });
        }
    }

    public void updateMemberNickname(String userUuid, String nickname, int type) {
        if (userUuid != null && nickname != null && type == NickOrSignatureEditActivity.TYPE_NICK) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
            if (gatewayCommunicationBase != null) {
                String boxUuid = null;
                String boxBind = null;
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                                && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                            boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            break;
                        }
                    }
                }
                String finalBoxUuid = boxUuid;
                String finalBoxBind = boxBind;
                UserInfo userInfo = EulixSpaceDBUtil.getActiveUserInfo(context, userUuid);
                String aoId = null;
                if (userInfo != null) {
                    aoId = userInfo.getUserId();
                }
                if (aoId != null) {
                    UserInfoUtil.updateMemberName(context, finalBoxUuid, aoId, nickname
                            , gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                            , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams()
                            , true, new MemberNameUpdateCallback() {
                                @Override
                                public void onSuccess(String code, String message, String requestId) {
                                    Map<String, String> userMap = new HashMap<>();
                                    userMap.put(UserInfoUtil.NICKNAME, nickname);
                                    if (finalBoxUuid != null && finalBoxBind != null) {
                                        UserInfoUtil.updateUserInfoDB(context, finalBoxUuid, finalBoxBind, userUuid, userMap);
                                    }
                                    if (iView != null) {
                                        iView.onUpdateResult(true, null);
                                    }
                                }

                                @Override
                                public void onFailed(String code, String message, String requestId) {
                                    if (iView != null) {
                                        iView.onUpdateResult(false, message);
                                    }
                                }

                                @Override
                                public void onError(String msg) {
                                    if (iView != null) {
                                        iView.onUpdateResult(null, msg);
                                    }
                                }
                            });
                }
            }
        }
    }

    public UserInfo getActiveUserInfo() {
        return EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
    }

    public UserInfo getActiveUserInfo(String userUuid) {
        return EulixSpaceDBUtil.getActiveUserInfo(context, userUuid);
    }
}
