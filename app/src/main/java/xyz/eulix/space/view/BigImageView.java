package xyz.eulix.space.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class BigImageView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener {
    private Scroller mScroller;
    private Rect mRect;
    private BitmapFactory.Options mOptions;
    private GestureDetector mGestureDetector;
    private int mImageWidth;
    private int mImageHeight;
    private BitmapRegionDecoder mDecoder;
    private int mViewWidth;
    private int mViewHeight;
    private float mScale;
    private Bitmap mBitmap;
    private Matrix mMatrix;

    public BigImageView(Context context) {
        this(context, null);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mRect = new Rect();
        mOptions = new BitmapFactory.Options();
        mGestureDetector = new GestureDetector(context, this);
        setOnTouchListener(this);
        mScroller = new Scroller(context);
        mMatrix = new Matrix();
    }

    public void setImage(InputStream inputStream) {
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;

        mOptions.inMutable = true;
        mOptions.inJustDecodeBounds = false;

        try {
            mDecoder = BitmapRegionDecoder.newInstance(inputStream, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();
        if (mDecoder == null) {
            return;
        }
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = mImageWidth;
        mScale = mViewWidth * 1.0f / mImageWidth;
        mRect.bottom = (int) (mViewHeight / mScale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDecoder == null) {
            return;
        }
        mOptions.inBitmap = mBitmap;
        mBitmap = mDecoder.decodeRegion(mRect, mOptions);
        if (mMatrix != null) {
            mMatrix.setScale(mScale, mScale);
            canvas.drawBitmap(mBitmap, mMatrix, null);
        }
    }

    @Override
    public void computeScroll() {
//        super.computeScroll();
        if (mScroller == null || mScroller.isFinished()) {
            return;
        }
        if (mScroller.computeScrollOffset()) {
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + ((int) (mViewHeight/mScale));
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mScroller != null) {
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mRect != null) {
            mRect.offset(0, (int) distanceY);
            if (mRect.bottom > mImageHeight) {
                mRect.bottom = mImageHeight;
                mRect.top = mImageHeight - ((int) (mViewHeight / mScale));
            }
            if (mRect.top < 0) {
                mRect.top = 0;
                mRect.bottom = (int) (mViewHeight / mScale);
            }
            invalidate();
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mScroller != null) {
            mScroller.fling(0, mRect.top, 0, (int) -velocityY, 0, 0
                    , 0, (mImageHeight - ((int) (mViewHeight / mScale))));
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return (mGestureDetector != null && mGestureDetector.onTouchEvent(event));
    }
}
