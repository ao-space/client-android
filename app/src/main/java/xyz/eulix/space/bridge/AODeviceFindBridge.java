package xyz.eulix.space.bridge;

import xyz.eulix.space.abs.AbsBridge;

public class AODeviceFindBridge extends AbsBridge {
    private static final AODeviceFindBridge INSTANCE = new AODeviceFindBridge();

    public interface AODeviceFindSourceCallback extends SourceCallback {
        void aoDeviceFindSelectDevice(int index);
    }

    public interface AODeviceFindSinkCallback extends SinkCallback {
        void unbindResult(boolean isSuccess, String password);
    }

    private AODeviceFindBridge() {}

    public static AODeviceFindBridge getInstance() {
        return INSTANCE;
    }

    public void selectDevice(int index) {
        if (mSourceCallback != null && mSourceCallback instanceof AODeviceFindSourceCallback) {
            ((AODeviceFindSourceCallback) mSourceCallback).aoDeviceFindSelectDevice(index);
        }
    }

    public void unbindResult(boolean isSuccess, String password) {
        if (mSinkCallback != null && mSinkCallback instanceof AODeviceFindSinkCallback) {
            ((AODeviceFindSinkCallback) mSinkCallback).unbindResult(isSuccess, password);
        }
    }
}
