package xyz.eulix.space.view.gesture;

import android.content.Context;
import android.view.MotionEvent;

import xyz.eulix.space.util.Logger;

public class LeftFlingGestureDetector {
    private static final String TAG = LeftFlingGestureDetector.class.getSimpleName();
    private IntegrateGestureDetector integrateGestureDetector;
    private float minFlingDistance;
    private float minFlingVelocity;
    private float maxClickDistance;
    private LeftFlingGestureDetector.DetectorListener detectorListener;
    private IntegrateGestureDetector.DetectorListener integrateListener = new IntegrateGestureDetector.DetectorListener() {
        @Override
        public void onSingleClick() {
            if (detectorListener != null) {
                detectorListener.onSingleClick();
            }
        }

        @Override
        public void onLongClick() {
            if (detectorListener != null) {
                detectorListener.onSingleClick();
            }
        }

        @Override
        public void onDoubleClick() {
            // Do nothing
        }

        @Override
        public void onScroll(float dx, float dy, float vx, float vy) {
            Logger.d(TAG, "scroll dx: " + dx + ", dy: " + dy + ", vx: " + vx + ", vy: " + vy);
            if (dx > minFlingDistance) {
                Logger.d(TAG, "prepare scroll");
                if (Math.abs(vx) > minFlingVelocity && detectorListener != null) {
                    Logger.d(TAG, "ready scroll");
                    detectorListener.onScroll(dx, dy, vx, vy);
                }
            } else if (((dx * dx + dy * dy) < (maxClickDistance * maxClickDistance)) && detectorListener != null) {
                Logger.d(TAG, "ready click");
                detectorListener.onSingleClick();
            }
        }
    };

    public interface DetectorListener {
        void onSingleClick();
        void onScroll(float dx, float dy, float vx, float vy);
    }

    public LeftFlingGestureDetector(Context context, float minFlingDistance, float minFlingVelocity, float maxClickDistance, LeftFlingGestureDetector.DetectorListener listener) {
        detectorListener = listener;
        this.minFlingDistance = Math.max(minFlingDistance, 0);
        this.minFlingVelocity = Math.max(minFlingVelocity, 0);
        this.maxClickDistance = Math.max(maxClickDistance, 0);
        integrateGestureDetector = new IntegrateGestureDetector(context, new IntegrateGestureDetector.GestureListener(), integrateListener);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return (integrateGestureDetector != null && integrateGestureDetector.onTouchEvent(motionEvent));
    }
}
