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
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 扇形进度条(图片预览加载压缩图/原图时使用)
 * History:     2021/12/15
 */
public class SectorProgressView extends View {

    // 画实心圆的画笔
    private Paint mCirclePaint;
    // 画圆环的画笔
    private Paint mRingPaint;
    // 画圆环的画笔背景色
    private Paint mRingPaintBg;
    // 圆形颜色
    private int mCircleColor;
    // 圆环颜色
    private int mRingColor;
    // 圆环背景颜色
    private int mRingBgColor;
    // 半径
    private float mRadius;
    // 圆环半径
    private float mRingRadius;
    // 圆环宽度
    private float mStrokeWidth;
    // 圆心x坐标
    private int mXCenter;
    // 圆心y坐标
    private int mYCenter;
    // 总进度
    private int mTotalProgress = 100;
    // 当前进度
    private int mProgress;

    public SectorProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获取自定义的属性
        initAttrs(context, attrs);
        initVariable();
    }

    //属性
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CircleProgressView, 0, 0);
        mRadius = typeArray.getDimension(R.styleable.CircleProgressView_radiusCircle, getResources().getDimensionPixelSize(R.dimen.dp_10));
        mStrokeWidth = typeArray.getDimension(R.styleable.CircleProgressView_strokeWidthCircle, getResources().getDimensionPixelSize(R.dimen.dp_1));
        mCircleColor = typeArray.getColor(R.styleable.CircleProgressView_circleColorCircle, 0xFFFFFFFF);
        mRingColor = typeArray.getColor(R.styleable.CircleProgressView_ringColorCircle, 0xFFFFFFFF);
        mRingBgColor = typeArray.getColor(R.styleable.CircleProgressView_ringBgColorCircle, 0x33000000);
        typeArray.recycle();
    }

    //初始化画笔
    private void initVariable() {
        //外圆
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setStyle(Paint.Style.FILL);


//        //进度背景
        mRingPaintBg = new Paint();
        mRingPaintBg.setAntiAlias(true);
        mRingPaintBg.setColor(mRingBgColor);
        mRingPaintBg.setStyle(Paint.Style.FILL);


        //进度圆弧
        mRingPaint = new Paint();
        mRingPaint.setAntiAlias(true);
        mRingPaint.setColor(mRingColor);
        mRingPaint.setStyle(Paint.Style.FILL);

    }

    //画图
    @Override
    protected void onDraw(Canvas canvas) {
        mXCenter = getWidth() / 2;
        mYCenter = getHeight() / 2;

        mRadius = (float) getWidth()/2;
        mRingRadius = mRadius - mStrokeWidth;

        //外圆
        canvas.drawCircle(mXCenter, mYCenter, mRadius, mCirclePaint);

        //进度背景圆
        canvas.drawCircle(mXCenter, mYCenter, mRingRadius, mRingPaintBg);

        //进度内圆弧
        if (mProgress > 0) {
            RectF oval1 = new RectF();
            oval1.left = mXCenter - mRingRadius;
            oval1.top = mYCenter - mRingRadius;
            oval1.right = mXCenter + mRingRadius;
            oval1.bottom = mYCenter + mRingRadius;
//            float startAngle = 360*((float) mProgress / mTotalProgress) -90;
//            float sweepAngle = 270 -startAngle;
            canvas.drawArc(oval1, -90,60*((float) mProgress / mTotalProgress),  true, mRingPaint); //圆弧所在的椭圆对象、圆弧的起始角度、圆弧的角度、是否显示半径连线
        }
    }

    //设置进度
    public void setProgress(int progress) {
        mProgress = progress;
        postInvalidate();//重绘
    }
}

