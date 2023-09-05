package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.util.BooleanUtil;
import xyz.eulix.space.util.StringUtil;

public class AOSpaceAccessBean implements EulixKeep {
    private Boolean isLanAccess;
    private Boolean isP2PAccess;
    private Boolean isInternetAccess;
    private String userDomain;
    private String ipAddressUrl;
    //平台地址
    private String platformApiBase;

    public static boolean compareAccess(AOSpaceAccessBean aoSpaceAccessBean1, AOSpaceAccessBean aoSpaceAccessBean2) {
        boolean isEqual = false;
        if (aoSpaceAccessBean1 == null && aoSpaceAccessBean2 == null) {
            isEqual = true;
        } else if (aoSpaceAccessBean1 != null && aoSpaceAccessBean2 != null) {
            isEqual = (BooleanUtil.compare(aoSpaceAccessBean1.isLanAccess, aoSpaceAccessBean2.isLanAccess)
                    && BooleanUtil.compare(aoSpaceAccessBean1.isP2PAccess, aoSpaceAccessBean2.isP2PAccess)
                    && BooleanUtil.compare(aoSpaceAccessBean1.isInternetAccess, aoSpaceAccessBean2.isInternetAccess)
                    && StringUtil.compare(aoSpaceAccessBean1.platformApiBase, aoSpaceAccessBean2.platformApiBase));
        }
        return isEqual;
    }

    public static boolean compare(AOSpaceAccessBean aoSpaceAccessBean1, AOSpaceAccessBean aoSpaceAccessBean2) {
        boolean isEqual = false;
        if (aoSpaceAccessBean1 == null && aoSpaceAccessBean2 == null) {
            isEqual = true;
        } else if (aoSpaceAccessBean1 != null && aoSpaceAccessBean2 != null) {
            isEqual = (BooleanUtil.compare(aoSpaceAccessBean1.isLanAccess, aoSpaceAccessBean2.isLanAccess)
                    && BooleanUtil.compare(aoSpaceAccessBean1.isP2PAccess, aoSpaceAccessBean2.isP2PAccess)
                    && BooleanUtil.compare(aoSpaceAccessBean1.isInternetAccess, aoSpaceAccessBean2.isInternetAccess)
                    && StringUtil.compare(aoSpaceAccessBean1.userDomain, aoSpaceAccessBean2.userDomain)
                    && StringUtil.compare(aoSpaceAccessBean1.ipAddressUrl, aoSpaceAccessBean2.ipAddressUrl)
                    && StringUtil.compare(aoSpaceAccessBean1.platformApiBase, aoSpaceAccessBean2.platformApiBase));
        }
        return isEqual;
    }

    public Boolean getLanAccess() {
        return isLanAccess;
    }

    public void setLanAccess(Boolean lanAccess) {
        isLanAccess = lanAccess;
    }

    public Boolean getP2PAccess() {
        return isP2PAccess;
    }

    public void setP2PAccess(Boolean p2PAccess) {
        isP2PAccess = p2PAccess;
    }

    public Boolean getInternetAccess() {
        return isInternetAccess;
    }

    public void setInternetAccess(Boolean internetAccess) {
        isInternetAccess = internetAccess;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getIpAddressUrl() {
        return ipAddressUrl;
    }

    public void setIpAddressUrl(String ipAddressUrl) {
        this.ipAddressUrl = ipAddressUrl;
    }

    public String getPlatformApiBase() {
        return platformApiBase;
    }

    public void setPlatformApiBase(String platformApiBase) {
        this.platformApiBase = platformApiBase;
    }

    @Override
    public String toString() {
        return "AOSpaceAccessBean{" +
                "isLanAccess=" + isLanAccess +
                ", isP2PAccess=" + isP2PAccess +
                ", isInternetAccess=" + isInternetAccess +
                ", userDomain='" + userDomain + '\'' +
                ", ipAddressUrl='" + ipAddressUrl + '\'' +
                ", platformApiBase='" + platformApiBase + '\'' +
                '}';
    }
}
