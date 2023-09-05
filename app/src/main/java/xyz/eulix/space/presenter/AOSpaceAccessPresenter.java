package xyz.eulix.space.presenter;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.net.ChannelInfoCallback;
import xyz.eulix.space.network.net.ChannelInfoResult;
import xyz.eulix.space.network.net.EulixNetUtil;
import xyz.eulix.space.network.net.InternetServiceConfigCallback;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.GatewayUtils;

public class AOSpaceAccessPresenter extends AbsPresenter<AOSpaceAccessPresenter.IAOSpaceAccess> {
    public interface IAOSpaceAccess extends IBaseView {
        void setInternetServiceConfigCallback(int code, String source, String message, InternetServiceConfigResult result);
    }

    public AOSpaceAccessBean getActiveAOSpaceAccessBean() {
        AOSpaceAccessBean aoSpaceAccessBean = null;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            aoSpaceAccessBean = eulixBoxInfo.getAoSpaceAccessBean();
        }
        return aoSpaceAccessBean;
    }

    public void setInternetServiceConfig(boolean isEnableInternetAccess, String platformApiBase) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            EulixNetUtil.setInternetServiceConfig(DataUtil.getClientUuid(context), isEnableInternetAccess, platformApiBase, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(), true, new InternetServiceConfigCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String requestId, InternetServiceConfigResult result) {
                    if (code >= 200 && code < 400) {
                        EulixSpaceDBUtil.setAOSpaceBean(context, boxUuid, boxBind, result, true);
                    }
                    if (iView != null) {
                        iView.setInternetServiceConfigCallback(code, source, message, result);
                    }
                }

                @Override
                public void onFail(int code, String source, String message, String requestId) {
                    if (iView != null) {
                        iView.setInternetServiceConfigCallback(code, source, message, null);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.setInternetServiceConfigCallback(ConstantField.SERVER_EXCEPTION_CODE, null, errMsg, null);
                    }
                }
            });
        } else if (iView != null) {
            iView.setInternetServiceConfigCallback(500, null, null, null);
        }
    }

    public boolean isActiveAdmin() {
        int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity);
    }
}
