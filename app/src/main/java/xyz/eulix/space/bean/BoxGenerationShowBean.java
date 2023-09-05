package xyz.eulix.space.bean;

import androidx.annotation.DrawableRes;

import xyz.eulix.space.interfaces.EulixKeep;

public class BoxGenerationShowBean implements EulixKeep {
    private String boxName;
    @DrawableRes private int boxResId;

    public BoxGenerationShowBean(String boxName, int boxResId) {
        this.boxName = boxName;
        this.boxResId = boxResId;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public int getBoxResId() {
        return boxResId;
    }

    public void setBoxResId(int boxResId) {
        this.boxResId = boxResId;
    }
}
