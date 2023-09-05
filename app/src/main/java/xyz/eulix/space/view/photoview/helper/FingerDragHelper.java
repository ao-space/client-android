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

package xyz.eulix.space.view.photoview.helper;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import xyz.eulix.space.R;


/**
 * description:辅助下拉关闭图片
 */
public class FingerDragHelper extends LinearLayout {

    private static final String TAG = FingerDragHelper.class.getSimpleName();
    private final static int MAX_EXIT_Y = 500;
    private final static long DURATION = 200;
    private static int MAX_TRANSLATE_Y = 500;
    private View childView;
    private float mDownY;
    private float mTranslationY;
    private float mLastTranslationY;
    private boolean isAnimate = false;
    private int fadeIn = R.anim.fade_in_150;
    private int fadeOut = R.anim.fade_out_150;
    private int mTouchslop;
    private onAlphaChangedListener mOnAlphaChangedListener;

    public FingerDragHelper(Context context) {
        this(context, null);
    }

    public FingerDragHelper(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FingerDragHelper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    private void initViews() {
        mTouchslop = ViewConfiguration.getTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        childView =  getChildAt(0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean isIntercept = false;
        int action = ev.getAction() & ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getRawY();
            case MotionEvent.ACTION_MOVE:
                    if (childView != null && childView.getVisibility() == View.VISIBLE) {
//                        isIntercept = (childView.getScale() <= (childView.getMinimumScale() + 0.001F))
//                                && (childView.getMaxTouchCount() == 0 || childView.getMaxTouchCount() == 1)
//                                && Math.abs(ev.getRawY() - mDownY) > 2 * mTouchslop;
                        isIntercept = (childView.getScaleX() <= 1)
                                && (ev.getPointerCount() == 0 || ev.getPointerCount() == 1)
                                && Math.abs(ev.getRawY() - mDownY) > 2 * mTouchslop;
                    }
                break;
            default:
                break;
        }
        return isIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getRawY();
            case MotionEvent.ACTION_MOVE:
                    if (childView != null && childView.getVisibility() == View.VISIBLE) {
                        onOneFingerPanActionMove(event);
                    }
                break;
            case MotionEvent.ACTION_UP:
                onActionUp();
                break;
            default:
                break;
        }
        return true;
    }

    private void onOneFingerPanActionMove(MotionEvent event) {
        float moveY = event.getRawY();
        mTranslationY = moveY - mDownY + mLastTranslationY;
        //触发回调 根据距离处理其他控件的透明度 显示或者隐藏角标，文字信息等
        if (null != mOnAlphaChangedListener) {
            mOnAlphaChangedListener.onTranslationYChanged(event, mTranslationY);
        }
        setScrollY(this, -(int) mTranslationY);
    }

    private void onActionUp() {
        // 是否启用上拉关闭
        boolean enableUpDragClose = true;
        if (enableUpDragClose) {
            if (Math.abs(mTranslationY) > MAX_EXIT_Y) {
                exitWithTranslation(mTranslationY);
            } else {
                resetCallBackAnimation();
            }
        } else {
            if (mTranslationY > MAX_EXIT_Y) {
                exitWithTranslation(mTranslationY);
            } else {
                resetCallBackAnimation();
            }
        }
    }

    public void exitWithTranslation(float currentY) {
        if (currentY > 0) {
            ValueAnimator animDown = ValueAnimator.ofFloat(mTranslationY, getHeight());
            animDown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = (float) animation.getAnimatedValue();
                    setScrollY(FingerDragHelper.this, -(int) fraction);
                }
            });
            animDown.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reset();
                    Activity activity = ((Activity) getContext());
                    activity.finish();
                    activity.overridePendingTransition(fadeIn, fadeOut);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animDown.setDuration(DURATION);
            animDown.setInterpolator(new LinearInterpolator());
            animDown.start();
        } else {
            ValueAnimator animUp = ValueAnimator.ofFloat(mTranslationY, -getHeight());
            animUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = (float) animation.getAnimatedValue();
                    setScrollY(FingerDragHelper.this, -(int) fraction);
                }
            });
            animUp.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reset();
                    ((Activity) getContext()).finish();
                    ((Activity) getContext()).overridePendingTransition(fadeIn, fadeOut);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animUp.setDuration(DURATION);
            animUp.setInterpolator(new LinearInterpolator());
            animUp.start();
        }
    }

    private void resetCallBackAnimation() {
        ValueAnimator animatorY = ValueAnimator.ofFloat(mTranslationY, 0);
        animatorY.setDuration(DURATION);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (isAnimate) {
                    mTranslationY = (float) valueAnimator.getAnimatedValue();
                    mLastTranslationY = mTranslationY;
                    setScrollY(FingerDragHelper.this, -(int) mTranslationY);
                }
            }
        });
        animatorY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimate = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimate) {
                    mTranslationY = 0;
                    invalidate();
                    reset();
                }
                isAnimate = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorY.start();
    }

    /**
     * 暴露的回调方法（可根据位移距离或者alpha来改变主UI控件的透明度等
     */
    public void setOnAlphaChangeListener(onAlphaChangedListener alphaChangeListener) {
        mOnAlphaChangedListener = alphaChangeListener;
    }

    private void reset() {
        if (null != mOnAlphaChangedListener) {
            mOnAlphaChangedListener.onTranslationYChanged(null, mTranslationY);
        }
    }

    public interface onAlphaChangedListener {

        void onTranslationYChanged(MotionEvent event, float translationY);
    }

    public static final boolean NEEDS_PROXY =
            Integer.valueOf(Build.VERSION.SDK_INT).intValue() < Build.VERSION_CODES.HONEYCOMB;
    public static void setScrollY(View view, int scrollY) {
        if (NEEDS_PROXY) {
            AnimatorProxy.wrap(view).setScrollY(scrollY);
        } else {
            Honeycomb.setScrollY(view, scrollY);
        }
    }

    private static final class Honeycomb {
        static float getAlpha(View view) {
            return view.getAlpha();
        }

        static void setAlpha(View view, float alpha) {
            view.setAlpha(alpha);
        }

        static float getPivotX(View view) {
            return view.getPivotX();
        }

        static void setPivotX(View view, float pivotX) {
            view.setPivotX(pivotX);
        }

        static float getPivotY(View view) {
            return view.getPivotY();
        }

        static void setPivotY(View view, float pivotY) {
            view.setPivotY(pivotY);
        }

        static float getRotation(View view) {
            return view.getRotation();
        }

        static void setRotation(View view, float rotation) {
            view.setRotation(rotation);
        }

        static float getRotationX(View view) {
            return view.getRotationX();
        }

        static void setRotationX(View view, float rotationX) {
            view.setRotationX(rotationX);
        }

        static float getRotationY(View view) {
            return view.getRotationY();
        }

        static void setRotationY(View view, float rotationY) {
            view.setRotationY(rotationY);
        }

        static float getScaleX(View view) {
            return view.getScaleX();
        }

        static void setScaleX(View view, float scaleX) {
            view.setScaleX(scaleX);
        }

        static float getScaleY(View view) {
            return view.getScaleY();
        }

        static void setScaleY(View view, float scaleY) {
            view.setScaleY(scaleY);
        }

        static float getScrollX(View view) {
            return view.getScrollX();
        }

        static void setScrollX(View view, int scrollX) {
            view.setScrollX(scrollX);
        }

        static float getScrollY(View view) {
            return view.getScrollY();
        }

        static void setScrollY(View view, int scrollY) {
            view.setScrollY(scrollY);
        }

        static float getTranslationX(View view) {
            return view.getTranslationX();
        }

        static void setTranslationX(View view, float translationX) {
            view.setTranslationX(translationX);
        }

        static float getTranslationY(View view) {
            return view.getTranslationY();
        }

        static void setTranslationY(View view, float translationY) {
            view.setTranslationY(translationY);
        }

        static float getX(View view) {
            return view.getX();
        }

        static void setX(View view, float x) {
            view.setX(x);
        }

        static float getY(View view) {
            return view.getY();
        }

        static void setY(View view, float y) {
            view.setY(y);
        }
    }
}