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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 外部圆角矩形半透明，中间透明原型进度条（小程序安装进度使用）
 * History:     2022/4/24
 */
public class EraseProgressView extends View {

    private Paint mRectPaint;
    // 画圆环的画笔
    private Paint mRingPaint;
    // 外部矩形颜色
    private int mRectColor;
    // 圆环颜色
    private int mRingColor;
    // 外部矩形弧度
    private float mRadius;
    // 圆环半径
    private float mRingRadius;
    // 中间圆距离边框宽度
    private float mStrokeWidth;
    // 圆心x坐标
    private int mXCenter;
    // 圆心y坐标
    private int mYCenter;
    // 总进度
    private int mTotalProgress = 100;
    // 当前进度
    private int mProgress = 0;

    RectF rectOutside = new RectF();
    RectF ovalInside = new RectF();

    public EraseProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获取自定义的属性
        initAttrs(context, attrs);
        initVariable();
    }

    //属性
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.EraseProgressView, 0, 0);
        mRadius = typeArray.getDimension(R.styleable.EraseProgressView_radiusE, getResources().getDimensionPixelSize(R.dimen.dp_6));
        mStrokeWidth = typeArray.getDimension(R.styleable.EraseProgressView_strokeWidthE, getResources().getDimensionPixelSize(R.dimen.dp_10));
        typeArray.recycle();

    }

    //初始化画笔
    private void initVariable() {
        mRectColor = Color.parseColor("#80000000");
        mRingColor = Color.TRANSPARENT;

        //外部半透明圆角矩形
        mRectPaint = new Paint();
        mRectPaint.setAntiAlias(true);
        mRectPaint.setColor(mRectColor);
        mRectPaint.setStyle(Paint.Style.FILL);


        //进度圆弧
        mRingPaint = new Paint();
        mRingPaint.setAntiAlias(true);
        mRingPaint.setColor(mRingColor);
        PorterDuffXfermode mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mRingPaint.setXfermode(mPorterDuffXfermode);
//        mRingPaint.setStyle(Paint.Style.FILL);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    //画图
    @Override
    protected void onDraw(Canvas canvas) {
        mXCenter = getWidth() / 2;
        mYCenter = getHeight() / 2;

        mRingRadius = mXCenter - mStrokeWidth;

        rectOutside.left = 0;
        rectOutside.right = getWidth();
        rectOutside.top = 0;
        rectOutside.bottom = getWidth();

        //外部圆角矩形
        canvas.drawRoundRect(rectOutside, mRadius, mRadius, mRectPaint);

        //进度内圆弧
        if (mProgress > 0) {
            ovalInside.left = mXCenter - mRingRadius;
            ovalInside.top = mYCenter - mRingRadius;
            ovalInside.right = mXCenter + mRingRadius;
            ovalInside.bottom = mYCenter + mRingRadius;
            canvas.drawArc(ovalInside, -90, ((float) mProgress / mTotalProgress) * 360, true, mRingPaint); //圆弧所在的椭圆对象、圆弧的起始角度、圆弧的角度、是否显示半径连线
        }
    }

    //设置进度
    public void setProgress(int progress) {
        mProgress = progress;
        postInvalidate();//重绘
    }

    public int getCurrentProgress() {
        return mProgress;
    }
}

