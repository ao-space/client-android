package xyz.eulix.space.network.developer;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:12
 */
public class DevelopOptionsSwitchInfo implements EulixKeep, Serializable {
    public static final String STATUS_ON = "on";
    public static final String STATUS_OFF = "off";

    @SerializedName("status")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DevelopOptionsSwitchInfo{" +
                "status='" + status + '\'' +
                '}';
    }
}
