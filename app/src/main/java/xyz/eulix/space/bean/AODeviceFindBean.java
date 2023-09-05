package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;

public class AODeviceFindBean implements EulixKeep {
    private int deviceModelNumber;
    private String sn;
    private int bindStatus;
    private boolean isBinding;
    private boolean isOpenSource = false;

    public int getDeviceModelNumber() {
        return deviceModelNumber;
    }

    public void setDeviceModelNumber(int deviceModelNumber) {
        this.deviceModelNumber = deviceModelNumber;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public int getBindStatus() {
        return bindStatus;
    }

    public void setBindStatus(int bindStatus) {
        this.bindStatus = bindStatus;
    }

    public boolean isBinding() {
        return isBinding;
    }

    public void setBinding(boolean binding) {
        isBinding = binding;
    }

    public boolean isOpenSource() {
        return isOpenSource;
    }

    public void setOpenSource(boolean openSource) {
        isOpenSource = openSource;
    }

    @Override
    public String toString() {
        return "AODeviceFindBean{" +
                "deviceModelNumber=" + deviceModelNumber +
                ", sn='" + sn + '\'' +
                ", bindStatus=" + bindStatus +
                ", isBinding=" + isBinding +
                ", isOpenSource=" + isOpenSource +
                '}';
    }
}
