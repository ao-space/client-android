package com.google.zxing.client.android.bean;

import java.io.Serializable;
import java.util.List;

public class PhotoUpImageBucket implements Serializable {
    private int count = 0;
    private String bucketId;
    private String bucketName;
    private List<LocalMediaUpItem> imageList;
    private boolean checked = false;

    public int getCount() {
        if (imageList == null || imageList.isEmpty()) {
            return 0;
        }else {
            return imageList.size();
        }
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public List<LocalMediaUpItem> getImageList() {
        return imageList;
    }

    public void setImageList(List<LocalMediaUpItem> imageList) {
        this.imageList = imageList;
    }

    public void setChecked(boolean checked){
        this.checked = checked;
    }

    public boolean isChecked() {
        return this.checked;
    }
}
