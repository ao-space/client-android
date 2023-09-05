package xyz.eulix.space.event;

/**
 * Author:      Zhu Fuyu
 * Description: 关闭banner
 * History:     2022/12/27
 */
public class CloseBannerEvent {
    private String bannerId;

    public CloseBannerEvent(String bannerId) {
        this.bannerId = bannerId;
    }

    public String getBannerId() {
        return bannerId;
    }
}
