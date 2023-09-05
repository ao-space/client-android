package xyz.eulix.space.presenter;

import java.util.Map;
import java.util.Set;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.util.DataUtil;

public class AOCompletePresenter extends AbsPresenter<AOCompletePresenter.IAOComplete> {
    public interface IAOComplete extends IBaseView {}

    public AOSpaceAccessBean getSpecificAOSpaceAccessBean(String boxUuid, String boxBind) {
        return EulixSpaceDBUtil.getSpecificAOSpaceBean(context, boxUuid, boxBind);
    }


    public String getBoxDomain(String boxUuid, String boxBind) {
        String boxDomain = null;
        if (boxUuid != null && boxBind != null) {
            boxDomain = EulixSpaceDBUtil.getBoxDomain(context, boxUuid, boxBind);
        }
        return boxDomain;
    }

    public boolean getBoxOnline(String boxUuid, String boxBind, boolean defaultValue) {
        boolean isOnline = defaultValue;
        if (boxUuid != null && boxBind != null) {
            isOnline = DataUtil.isSpaceStatusOnline(EulixSpaceDBUtil.getDeviceStatus(context, boxUuid, boxBind), defaultValue);
        }
        return isOnline;
    }

    private EulixUser generateEulixUser(String clientUuid, Map<String, UserInfo> userInfoMap) {
        EulixUser eulixUser = null;
        if (userInfoMap != null) {
            Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
            for (Map.Entry<String, UserInfo> entry : entrySet) {
                if (entry != null) {
                    String uuid = entry.getKey();
                    UserInfo userInfo = entry.getValue();
                    if (uuid != null && userInfo != null && uuid.equals(clientUuid)) {
                        eulixUser = new EulixUser();
                        boolean isAdmin = userInfo.isAdmin();
                        eulixUser.setUuid(uuid);
                        eulixUser.setMyself(true);
                        eulixUser.setAvatarPath(userInfo.getAvatarPath());
                        eulixUser.setNickName(userInfo.getNickName());
                        eulixUser.setUserId(userInfo.getUserId());
                        eulixUser.setUserCreateTimestamp(userInfo.getUserCreateTimestamp());
                        eulixUser.setAdmin(isAdmin);
                        eulixUser.setUsedSize(userInfo.getUsedSize());
                        eulixUser.setUserDomain(userInfo.getUserDomain());
                        break;
                    }
                }
            }
        }
        return eulixUser;
    }

    public EulixUser generateEulixUser(String clientUuid) {
        return generateEulixUser(clientUuid, EulixSpaceDBUtil.getActiveUserInfo(context));
    }

    public EulixUser generateEulixUser(String clientUuid, String boxUuid, String boxBind) {
        return generateEulixUser(clientUuid, EulixSpaceDBUtil.getSpecificUserInfo(context, boxUuid, boxBind));
    }

    private EulixUser generateEulixUserWithLoginIdentity(String aoId, Map<String, UserInfo> userInfoMap) {
        EulixUser eulixUser = null;
        if (userInfoMap != null) {
            Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
            for (Map.Entry<String, UserInfo> entry : entrySet) {
                if (entry != null) {
                    String uuid = entry.getKey();
                    UserInfo userInfo = entry.getValue();
                    if (uuid != null && userInfo != null) {
                        String userId = userInfo.getUserId();
                        if (userId != null && userId.equals(aoId)) {
                            eulixUser = new EulixUser();
                            boolean isAdmin = userInfo.isAdmin();
                            eulixUser.setMyself(true);
                            eulixUser.setAvatarPath(userInfo.getAvatarPath());
                            eulixUser.setNickName(userInfo.getNickName());
                            eulixUser.setUserId(userInfo.getUserId());
                            eulixUser.setUserCreateTimestamp(userInfo.getUserCreateTimestamp());
                            eulixUser.setAdmin(isAdmin);
                            eulixUser.setUsedSize(userInfo.getUsedSize());
                            eulixUser.setUserDomain(userInfo.getUserDomain());
                            eulixUser.setUuid(uuid);
                            break;
                        }
                    }
                }
            }
        }
        return eulixUser;
    }

    public EulixUser generateEulixUserWithLoginIdentity(String aoId) {
        return generateEulixUserWithLoginIdentity(aoId, EulixSpaceDBUtil.getActiveUserInfo(context));
    }

    public EulixUser generateEulixUserWithLoginIdentity(String aoId, String boxUuid, String boxBind) {
        return generateEulixUserWithLoginIdentity(aoId, EulixSpaceDBUtil.getSpecificUserInfo(context, boxUuid, boxBind));
    }
}
