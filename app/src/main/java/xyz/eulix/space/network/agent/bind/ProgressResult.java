package xyz.eulix.space.network.agent.bind;

import xyz.eulix.space.interfaces.EulixKeep;

public class ProgressResult implements EulixKeep {
    public static final int COM_STATUS_CONTAINERS_STARTING = 0;
    public static final int COM_STATUS_CONTAINERS_NOT_STARTED = COM_STATUS_CONTAINERS_STARTING - 1;
    public static final int COM_STATUS_CONTAINERS_WAIT_OS_READY = COM_STATUS_CONTAINERS_NOT_STARTED - 1;
    public static final int COM_STATUS_CONTAINERS_STARTED = COM_STATUS_CONTAINERS_STARTING + 1;
    public static final int COM_STATUS_CONTAINERS_START_FAILED = COM_STATUS_CONTAINERS_STARTED + 1;
    public static final int COM_STATUS_CONTAINERS_DOWNLOADING = COM_STATUS_CONTAINERS_START_FAILED + 1;
    public static final int COM_STATUS_CONTAINERS_DOWNLOADED = COM_STATUS_CONTAINERS_DOWNLOADING + 1;
    public static final int COM_STATUS_CONTAINERS_DOWNLOADED_FAIL = COM_STATUS_CONTAINERS_DOWNLOADED + 1;
    private int comStatus;
    private int progress;

    public int getComStatus() {
        return comStatus;
    }

    public void setComStatus(int comStatus) {
        this.comStatus = comStatus;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
