package xyz.eulix.space.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 圆环进度条
 * History:     2023/3/21
 */
public class CircleRingProgressBar extends View {
    private int mHeight = 0;
    private int mWidth = 0;

    // 画圆环的画笔
    private Paint mRingPaint;
    // 画圆环的画笔背景色
    private Paint mRingPaintBg;
    // 画字体的画笔
    private Paint mTextPaint;
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
    // 字的长度
    private float mTxtWidth;
    // 字的高度
    private float mTxtHeight;
    // 总进度
    private int max = 100;
    // 当前进度
    private int progress;
    private String text;
    //文字大小
    private float textSize;

    public CircleRingProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获取自定义的属性
        initAttrs(context, attrs);
        initVariable();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.TasksCompletedView, 0, 0);
        mStrokeWidth = typeArray.getDimension(R.styleable.CirclePgBarTasksCompletedView_circleWidth, getResources().getDimensionPixelSize(R.dimen.dp_2));
        mRingColor = typeArray.getColor(R.styleable.CirclePgBarTasksCompletedView_cPringColor, 0xFFCBDDFF);
        mRingBgColor = typeArray.getColor(R.styleable.CirclePgBarTasksCompletedView_cPringBgColor, 0xFF337AFF);
        text = typeArray.getString(R.styleable.CirclePgBarTasksCompletedView_text);
        max = typeArray.getInteger(R.styleable.CirclePgBarTasksCompletedView_cPmax, 100);
        progress = typeArray.getInteger(R.styleable.CirclePgBarTasksCompletedView_progress, 0);
        textSize = typeArray.getFloat(R.styleable.CirclePgBarTasksCompletedView_cPtextSize, getResources().getDimensionPixelSize(R.dimen.dp_10));
        typeArray.recycle();
    }


    private void initVariable() {
        //外圆弧背景
        mRingPaintBg = new Paint();
        mRingPaintBg.setAntiAlias(true);
        mRingPaintBg.setColor(mRingBgColor);
        mRingPaintBg.setStyle(Paint.Style.STROKE);
        mRingPaintBg.setStrokeWidth(mStrokeWidth);
        //外圆弧
        mRingPaint = new Paint();
        mRingPaint.setAntiAlias(true);
        mRingPaint.setColor(mRingColor);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(mStrokeWidth);
        //mRingPaint.setStrokeCap(Paint.Cap.ROUND);
        //中间字
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mRingBgColor);
        invalidate();
    }

    //测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //实际测量宽高
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
        if (mWidth > mHeight) {
            mRadius = mHeight / 2;
        } else {
            mRadius = mWidth / 2;
        }
        //半径
        mRingRadius = mRadius - mStrokeWidth / 2;
        //文字宽高测量
        mTextPaint.setTextSize(textSize);
        mTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTextPaint.setStrokeWidth(0.7f);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        mTxtHeight = (int) Math.ceil(fm.descent - fm.ascent);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mXCenter = mWidth / 2;
        mYCenter = mHeight / 2;
        //外圆弧背景
        RectF rectBg = new RectF(mXCenter - mRingRadius, mYCenter - mRingRadius, mXCenter + mRingRadius, mYCenter + mRingRadius);
        canvas.drawArc(rectBg, 0, 360, false, mRingPaintBg);
        //外圆弧//进度
        if (progress > 0) {
            RectF oval = new RectF(mXCenter - mRingRadius, mYCenter - mRingRadius, mXCenter + mRingRadius, mYCenter + mRingRadius);
            canvas.drawArc(oval, -90, ((float) progress / max) * 360, false, mRingPaint);
        }
        //字体
        if (!TextUtils.isEmpty(text)) {
            mTxtWidth = mTextPaint.measureText(text, 0, text.length());
            canvas.drawText(text, mXCenter - mTxtWidth / 2, mYCenter + mTxtHeight / 4, mTextPaint);
        }
    }

    /**
     * 设置最大值
     *
     * @param max
     */
    public void setMax(int max) {
        this.max = max;
        postInvalidate();
    }

    /**
     * 设置文字和进度
     *
     * @param text
     * @param progress
     */
    public void setTextAndProgress(String text, int progress) {
        this.text = text;
        this.progress = progress;
        postInvalidate();
    }
}
