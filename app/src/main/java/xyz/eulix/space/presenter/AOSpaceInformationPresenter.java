package xyz.eulix.space.presenter;

import android.text.TextUtils;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.InviteParams;
import xyz.eulix.space.network.userinfo.MemberCreateCallback;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;

public class AOSpaceInformationPresenter extends AbsPresenter<AOSpaceInformationPresenter.IAOSpaceInformation> {
    public interface IAOSpaceInformation extends IBaseView {
        void createMemberCallback(boolean isSuccess, int code, String message, String boxUuid);
    }

    public void createMember(InviteParams inviteParams, String boxPublicKey, String nickname) {
        String clientUuid = DataUtil.getClientUuid(context);
        if (inviteParams != null && boxPublicKey != null && clientUuid != null) {
            String secretKey = DataUtil.getUID(UUID.randomUUID());
            String phoneModel = SystemUtil.getPhoneModel();
            String boxDomain = inviteParams.getSubDomain();
            String aoId = inviteParams.getAoId();
            try {
                ThreadPool.getInstance().execute(() -> UserInfoUtil.createMember(boxPublicKey, clientUuid
                        , inviteParams.getInviteCode(), secretKey, nickname
                        , phoneModel, boxDomain, aoId, new MemberCreateCallback() {
                            @Override
                            public void onSuccess(String boxUuid, String authKey, String userDomain, String code, String message) {
                                if (iView != null) {
                                    boolean isSuccess = (boxUuid != null && authKey != null && userDomain != null);
                                    if (isSuccess) {
                                        isSuccess = AOSpaceUtil.requestMemberBindUseBox(context, boxUuid, userDomain, authKey, boxPublicKey, (aoId != null));
                                    }

                                    iView.createMemberCallback(isSuccess, DataUtil.stringCodeToInt(code), message, boxUuid);
                                }
                            }

                            @Override
                            public void onFailed(String code, String message) {
                                if (iView != null) {
                                    iView.createMemberCallback(false, DataUtil.stringCodeToInt(code), message, null);
                                }
                            }

                            @Override
                            public void onError(String msg) {
                                int errorCode = ConstantField.SERVER_EXCEPTION_CODE;
                                if (!TextUtils.isEmpty(msg)) {
                                    try {
                                        errorCode = Integer.parseInt(msg);
                                    } catch (Exception e) {
                                        Logger.e(e.getMessage());
                                    }
                                }
                                if (iView != null) {
                                    iView.createMemberCallback(false, errorCode, msg, null);
                                }
                            }
                        }), true);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else {
            if (iView != null) {
                iView.createMemberCallback(false, -1, null, null);
            }
        }
    }
}
