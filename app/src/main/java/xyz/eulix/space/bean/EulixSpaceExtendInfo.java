package xyz.eulix.space.bean;

public class EulixSpaceExtendInfo extends EulixSpaceInfo {
    private String boxUpdateTime;

    public String getBoxUpdateTime() {
        return boxUpdateTime;
    }

    public void setBoxUpdateTime(String boxUpdateTime) {
        this.boxUpdateTime = boxUpdateTime;
    }

    @Override
    public String toString() {
        return "EulixSpaceExtendInfo{" +
                "boxUpdateTime='" + boxUpdateTime + '\'' +
                ", boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                '}';
    }
}
