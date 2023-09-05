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
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 高度根据比例计算
 * History:     2021/9/30
 */
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
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScaleHeightLayout);
        try {
            widthRatio = ta.getInt(R.styleable.ScaleHeightLayout_width_ratio, 0);
            heightRatio = ta.getInt(R.styleable.ScaleHeightLayout_height_ratio, 0);
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
