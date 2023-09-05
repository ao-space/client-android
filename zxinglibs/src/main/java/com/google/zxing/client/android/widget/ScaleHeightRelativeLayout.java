package com.google.zxing.client.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.google.zxing.client.android.R;

public class ScaleHeightRelativeLayout extends RelativeLayout {
    private int widthRatio = 0;
    private int heightRatio = 0;
    private Context context;

    public ScaleHeightRelativeLayout(Context context) {
        this(context, null);
    }

    public ScaleHeightRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScaleHeightRelativeLayout);
        try {
            widthRatio = ta.getInt(R.styleable.ScaleHeightRelativeLayout_width_ratio, 0);
            heightRatio = ta.getInt(R.styleable.ScaleHeightRelativeLayout_height_ratio, 0);
        } finally {
            ta.recycle();
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (widthRatio > 0 && heightRatio > 0) {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = widthSize * heightRatio / widthRatio;
            super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
