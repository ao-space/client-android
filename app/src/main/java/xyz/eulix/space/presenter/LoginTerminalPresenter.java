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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixTerminal;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.TerminalInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.userinfo.TerminalOfflineCallback;
import xyz.eulix.space.network.userinfo.TerminalOfflineResult;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.GatewayUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/21 17:26
 */
public class LoginTerminalPresenter extends AbsPresenter<LoginTerminalPresenter.ILoginTerminal> {
    private TerminalOfflineCallback terminalOfflineCallback = new TerminalOfflineCallback() {
        @Override
        public void onSuccess(int code, String message, String requestId, TerminalOfflineResult results, String customizeSource) {
            if (iView != null) {
                iView.handleTerminalOffline(code, customizeSource);
            }
        }

        @Override
        public void onFail(int code, String message, String requestId) {
            if (iView != null) {
                iView.handleTerminalOffline(code, "");
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleTerminalOffline(-1, "");
            }
        }
    };

    public interface ILoginTerminal extends IBaseView {
        void handleTerminalOffline(int code, String customizeSource);
    }

    public EulixBoxBaseInfo getActiveBoxUuid() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
    }

    public List<EulixTerminal> getEulixTerminalList() {
        List<EulixTerminal> eulixTerminals = new ArrayList<>();
        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getActiveBoxOtherInfo(context);
        String myClientUuid = DataUtil.getClientUuid(context);
        String granterClientUuid = DataUtil.getCompatibleClientUuid(context);
        if (eulixBoxOtherInfo != null) {
            Map<String, TerminalInfo> terminalInfoMap = eulixBoxOtherInfo.getTerminalInfoMap();
            if (terminalInfoMap != null) {
                Set<Map.Entry<String, TerminalInfo>> entrySet = terminalInfoMap.entrySet();
                for (Map.Entry<String, TerminalInfo> entry : entrySet) {
                    if (entry != null) {
                        String uuid = entry.getKey();
                        TerminalInfo terminalInfo = entry.getValue();
                        if (uuid != null) {
                            EulixTerminal eulixTerminal = new EulixTerminal();
                            eulixTerminal.setTerminalUuid(uuid);
                            eulixTerminal.setTerminalName((terminalInfo == null ? null : terminalInfo.getName()));
                            eulixTerminal.setTerminalType((terminalInfo == null ? null : terminalInfo.getType()));
                            eulixTerminal.setTerminalPlace((terminalInfo == null ? null : terminalInfo.getPlace()));
                            eulixTerminal.setTerminalTimestamp((terminalInfo == null ? 0L : terminalInfo.getTimestamp()));
                            eulixTerminal.setMyself(uuid.equals(myClientUuid));
                            eulixTerminal.setGranter(uuid.equals(granterClientUuid));
                            eulixTerminals.add(eulixTerminal);
                        }
                    }
                }
            }
        }
        return eulixTerminals;
    }

    public void offlineTerminal(String clientUuid) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            UserInfoUtil.offlineTerminal(context, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxDomain()
                    , EulixSpaceDBUtil.getClientAoId(context, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxBind())
                    , clientUuid, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), true, terminalOfflineCallback);
        } else if (iView != null) {
            iView.handleTerminalOffline(-1, null);
        }
    }
}
