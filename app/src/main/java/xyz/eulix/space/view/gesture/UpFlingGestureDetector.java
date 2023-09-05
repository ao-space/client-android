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
import android.view.MotionEvent;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/13 17:35
 */
public class UpFlingGestureDetector {
    private static final String TAG = UpFlingGestureDetector.class.getSimpleName();
    private IntegrateGestureDetector integrateGestureDetector;
    private float minFlingDistance;
    private float minFlingVelocity;
    private float maxClickDistance;
    private DetectorListener detectorListener;
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
            if (dy > minFlingDistance) {
                Logger.d(TAG, "prepare scroll");
                if (Math.abs(vy) > minFlingVelocity && detectorListener != null) {
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

    public UpFlingGestureDetector(Context context, float minFlingDistance, float minFlingVelocity, float maxClickDistance, DetectorListener listener) {
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
