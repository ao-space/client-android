package xyz.eulix.space.util;

import xyz.eulix.space.BuildConfig;

public class DeploymentModeUtil {
    private DeploymentModeUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean isNoPlatformMode() {
        return BuildConfig.NO_PLATFORM_MODE;
    }
}
