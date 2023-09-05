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

package xyz.eulix.space.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 圆环进度条
 * History:     2022/7/19
 */
public class RingProgressBar extends View {

    /**
     * 画笔对象的引用
     */
    private Paint ringPaint;
    private Paint ringProgressPaint;
    private Paint txtPaint;

    /**
     * 圆环的颜色
     */
    private int ringColor;

    /**
     * 圆环进度的颜色
     */
    private int ringProgressColor;

    /**
     * 中间进度百分比的字符串的颜色
     */
    private int textColor;

    /**
     * 中间进度百分比的字符串的字体
     */
    private float textSize;

    /**
     * 圆环的宽度
     */
    private float ringWidth;

    /**
     * 最大进度
     */
    private int max;

    /**
     * 当前进度
     */
    private int progress;
    /**
     * 是否显示中间的进度
     */
    private boolean textIsDisplayable;

    private Context mContext;
    /**
     * 进度的风格，实心或者空心
     */
    private int style;

    public static final int STROKE = 0;
    public static final int FILL = 1;

    public RingProgressBar(Context context) {
        this(context, null);
    }

    public RingProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RingProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;
        // 获取自定义的属性
        initAttrs(context, attrs);
        initPaint();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RingProgressBar);

        //获取自定义属性和默认值
        ringColor = mTypedArray.getColor(R.styleable.RingProgressBar_ringNormalColor, getResources().getColor(R.color.c_ffe3eaf8));
        ringProgressColor = mTypedArray.getColor(R.styleable.RingProgressBar_ringProgressColor, getResources().getColor(R.color.c_ff337aff));
        textColor = mTypedArray.getColor(R.styleable.RingProgressBar_textColor, getResources().getColor(R.color.c_ff337aff));
        textSize = mTypedArray.getDimension(R.styleable.RingProgressBar_textSize, 16);
        ringWidth = mTypedArray.getDimension(R.styleable.RingProgressBar_ringWidth, getResources().getDimensionPixelSize(R.dimen.dp_6));
        max = mTypedArray.getInteger(R.styleable.RingProgressBar_max, 100);
        textIsDisplayable = mTypedArray.getBoolean(R.styleable.RingProgressBar_textIsDisplayable, false);
        style = mTypedArray.getInt(R.styleable.RingProgressBar_style, 0);

        //资源回收
        mTypedArray.recycle();
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        //圆环画笔
        ringPaint = new Paint();
        ringPaint.setColor(ringColor); //设置圆环的颜色
        ringPaint.setStyle(Paint.Style.STROKE); //设置空心
        ringPaint.setStrokeWidth(ringWidth); //设置圆环的宽度
        ringPaint.setAntiAlias(true);  //消除锯齿

        //圆环进度画笔
        ringProgressPaint = new Paint();
        ringProgressPaint.setColor(ringProgressColor); //设置圆环的颜色
        ringProgressPaint.setStrokeWidth(ringWidth); //设置圆环的宽度
        ringProgressPaint.setAntiAlias(true);  //消除锯齿
        switch (style) {
            case STROKE:
                ringProgressPaint.setStyle(Paint.Style.STROKE);
                break;
            case FILL:
                ringProgressPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
        }

        //百分比文字画笔
        txtPaint = new Paint();
        txtPaint.setColor(textColor);
        txtPaint.setTextSize(textSize);
        txtPaint.setTypeface(Typeface.DEFAULT_BOLD); //设置字体
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //圆心坐标
        int mXCenter = getWidth() / 2;
        int mYCenter = getHeight() / 2;
        int radius = (int) (mXCenter - ringWidth / 2); //圆环的半径
        //绘制圆环
        canvas.drawCircle(mXCenter, mYCenter, radius, ringPaint);

        //绘制圆环进度
        RectF oval = new RectF(mXCenter - radius, mYCenter - radius, mXCenter
                + radius, mYCenter + radius);  //用于定义的圆弧的形状和大小的界限
        switch (style) {
            case STROKE:
                canvas.drawArc(oval, -90, 360 * progress / max, false, ringProgressPaint);  //根据进度画圆弧
                break;
            case FILL:
                if (progress != 0)
                    canvas.drawArc(oval, -90, 360 * progress / max, true, ringProgressPaint);  //根据进度画圆弧
                break;
        }

        //绘制百分比数字
        //文字绘制
        if (textIsDisplayable && progress != 0 && style == STROKE) {
            String txt = progress + "%";
            //文字的长度
            float mTxtWidth = txtPaint.measureText(txt, 0, txt.length());
            Log.e("tag", textIsDisplayable + "," + progress + (style == STROKE));
            canvas.drawText(txt, mXCenter - mTxtWidth / 2, mYCenter + textSize / 2, txtPaint);
        }


    }

    public synchronized int getMax() {
        return max;
    }

    /**
     * 设置进度的最大值
     *
     * @param max
     */
    public synchronized void setMax(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max not less than 0");
        }
        this.max = max;
    }

    /**
     * 获取进度.需要同步
     *
     * @return
     */
    public synchronized int getProgress() {
        return progress;
    }

    /**
     * 设置进度，此为线程安全控件，由于考虑多线程的问题，需要同步
     * 刷新界面调用postInvalidate()能在非UI线程刷新
     *
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if (progress < 0) {
            throw new IllegalArgumentException("progress not less than 0");
        }
        if (progress > max) {
            progress = max;
        }
        if (progress <= max) {
            this.progress = progress;
            postInvalidate();
        }

    }

    public int getRingColor() {
        return ringColor;
    }

    public void setRingColor(int ringColor) {
        this.ringColor = ringColor;
    }

    public int getRingProgressColor() {
        return ringProgressColor;
    }

    public void setRingProgressColor(int ringProgressColor) {
        this.ringProgressColor = ringProgressColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getRingWidth() {
        return ringWidth;
    }

    public void setRingWidth(float roundWidth) {
        this.ringWidth = roundWidth;
    }
}
