package xyz.eulix.space.bean;

import android.net.nsd.NsdServiceInfo;

import xyz.eulix.space.interfaces.EulixKeep;

public class LanServiceInfo implements EulixKeep {
    private boolean isNsd;
    private NsdServiceInfo nsdServiceInfo;
    private IPBean ipBean;

    public LanServiceInfo(NsdServiceInfo nsdServiceInfo) {
        this.nsdServiceInfo = nsdServiceInfo;
        isNsd = true;
    }

    public LanServiceInfo(IPBean ipBean) {
        this.ipBean = ipBean;
        isNsd = false;
    }

    public boolean isNsd() {
        return isNsd;
    }

    public void setNsd(boolean nsd) {
        isNsd = nsd;
    }

    public NsdServiceInfo getNsdServiceInfo() {
        return nsdServiceInfo;
    }

    public void setNsdServiceInfo(NsdServiceInfo nsdServiceInfo) {
        this.nsdServiceInfo = nsdServiceInfo;
    }

    public IPBean getIpBean() {
        return ipBean;
    }

    public void setIpBean(IPBean ipBean) {
        this.ipBean = ipBean;
    }
}
