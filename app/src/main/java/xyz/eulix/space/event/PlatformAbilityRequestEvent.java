package xyz.eulix.space.event;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/2/7 14:20
 */
public class PlatformAbilityRequestEvent {
    private String platformServerUrl;
    private boolean isChange;
    private boolean isFore;

    public PlatformAbilityRequestEvent(String platformServerUrl) {
        this(platformServerUrl, false);
    }

    public PlatformAbilityRequestEvent(String platformServerUrl, boolean isChange) {
        this(platformServerUrl, isChange, false);
    }

    public PlatformAbilityRequestEvent(String platformServerUrl, boolean isChange, boolean isFore) {
        this.platformServerUrl = platformServerUrl;
        this.isChange = isChange;
        this.isFore = isFore;
    }

    public String getPlatformServerUrl() {
        return platformServerUrl;
    }

    public boolean isChange() {
        return isChange;
    }

    public boolean isFore() {
        return isFore;
    }
}
