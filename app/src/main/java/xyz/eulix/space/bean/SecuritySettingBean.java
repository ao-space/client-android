package xyz.eulix.space.bean;

public class SecuritySettingBean {
    public static final int FUNCTION_SPACE_ACCOUNT = 1;
    public static final int FUNCTION_SECURITY_EMAIL = FUNCTION_SPACE_ACCOUNT + 1;
    public static final int FUNCTION_SECURITY_PASSWORD = FUNCTION_SECURITY_EMAIL + 1;
    public static final int FUNCTION_APPLICATION_LOCK = FUNCTION_SECURITY_PASSWORD + 1;
    public static final int FUNCTION_AUTHENTICATOR = FUNCTION_APPLICATION_LOCK + 1;
    private int securitySettingFunction;
    private boolean isClick;
    private String hintText;

    public SecuritySettingBean(int securitySettingFunction) {
        this.securitySettingFunction = securitySettingFunction;
    }

    public int getSecuritySettingFunction() {
        return securitySettingFunction;
    }

    public boolean isClick() {
        return isClick;
    }

    public void setClick(boolean click) {
        isClick = click;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @Override
    public String toString() {
        return "SecuritySettingBean{" +
                "securitySettingFunction=" + securitySettingFunction +
                ", isClick=" + isClick +
                ", hintText='" + hintText + '\'' +
                '}';
    }
}
