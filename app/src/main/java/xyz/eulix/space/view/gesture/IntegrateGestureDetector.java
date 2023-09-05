/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.view.gesture;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/13 15:41
 */
public class IntegrateGestureDetector extends GestureDetector {
    private static final String TAG = IntegrateGestureDetector.class.getSimpleName();
    private GestureListener mGestureListener;

    public interface DetectorListener {
        void onSingleClick();
        void onLongClick();
        void onDoubleClick();
        void onScroll(float dx, float dy, float vx, float vy);
    }

    public IntegrateGestureDetector(Context context, GestureListener listener, DetectorListener detectorListener) {
        this(context, listener, null, detectorListener);
    }

    public IntegrateGestureDetector(Context context, GestureListener listener, Handler handler, DetectorListener detectorListener) {
        super(context, listener, handler);
        if (listener != null) {
            init(listener, detectorListener);
        }
    }

    private void init(@NonNull GestureListener gestureListener, DetectorListener detectorListener) {
        mGestureListener = gestureListener;
        mGestureListener.registerListener(detectorListener);
    }

    public static class GestureListener extends SimpleOnGestureListener {
        private DetectorListener detectorListener;
        private float dx, dy;
        private boolean supportDoubleClick;

        public GestureListener() {
            this(false);
        }

        public GestureListener(boolean supportDoubleClick) {
            this.supportDoubleClick = supportDoubleClick;
        }

        void registerListener(DetectorListener listener) {
            detectorListener = listener;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Logger.d(TAG, "onDown: " + (e == null ? "null" : e.getAction()));
            dx = 0;
            dy = 0;
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Logger.d(TAG, "onShowPress: " + (e == null ? "null" : e.getAction()));
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Logger.d(TAG, "onSingleTapUp: " + (e == null ? "null" : e.getAction()));
            if (e != null && MotionEvent.ACTION_UP == e.getAction() && !supportDoubleClick && detectorListener != null) {
                detectorListener.onSingleClick();
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Logger.d(TAG, "onScroll");
            dx += distanceX;
            dy += distanceY;
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Logger.d(TAG, "onLongPress: " + (e == null ? "null" : e.getAction()));
            if (detectorListener != null) {
                detectorListener.onLongClick();
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Logger.d(TAG, "onFling");
            if (detectorListener != null) {
                if (e1 != null && e2 != null) {
                    detectorListener.onScroll((e1.getX() - e2.getX()), (e1.getY() - e2.getY()), velocityX, velocityY);
                } else {
                    detectorListener.onScroll(dx, dy, velocityX, velocityY);
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Logger.d(TAG, "onDoubleTap: " + (e == null ? "null" : e.getAction()));
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Logger.d(TAG, "onDoubleTapEvent: " + (e == null ? "null" : e.getAction()));
            if (e != null && MotionEvent.ACTION_UP == e.getAction() && detectorListener != null) {
                if (supportDoubleClick) {
                    detectorListener.onDoubleClick();
                } else {
                    detectorListener.onSingleClick();
                }
            }
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Logger.d(TAG, "onSingleTapConfirmed: " + (e == null ? "null" : e.getAction()));
            if (e != null && MotionEvent.ACTION_UP == e.getAction() && supportDoubleClick && detectorListener != null) {
                detectorListener.onSingleClick();
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            Logger.d(TAG, "onContextClick: " + (e == null ? "null" : e.getAction()));
            return super.onContextClick(e);
        }
    }
}
