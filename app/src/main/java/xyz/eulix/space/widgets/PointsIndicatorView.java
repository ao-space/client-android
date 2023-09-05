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

package xyz.eulix.space.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;

import java.util.ArrayList;

import xyz.eulix.space.R;

/**
 * Author: 		Zhufy
 * Description: 引导页底部指示器
 * History:		2021/7/13
 */
public class PointsIndicatorView extends LinearLayout {
    private int mPointSelectedWidth;
    private int mPointUnselectedWidth;
    private int mPointHeight;
    private View lastSelectedView;
    private Animation selectedAnimation;
    private ArrayList<View> pointList = new ArrayList<>();

    public PointsIndicatorView(Context context) {
        super(context);
        initView();
    }

    public PointsIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }


    private void initView() {
        mPointHeight = getResources().getDimensionPixelSize(R.dimen.dp_10);
        mPointSelectedWidth = getResources().getDimensionPixelSize(R.dimen.dp_20);
        mPointUnselectedWidth = getResources().getDimensionPixelSize(R.dimen.dp_10);

        selectedAnimation = new ScaleAnimation(0.25f, 1.0f, 1.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        selectedAnimation.setDuration(200L);
    }

    public void addPoints(int size) {
        if (size <= 1) {
            this.setVisibility(View.GONE);
            return;
        }
        this.removeAllViews();
        pointList.clear();
        for (int index = 0; index < size; index++) {
            LinearLayout v = new LinearLayout(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            if (index != 0) {
                lp.leftMargin = getResources().getDimensionPixelSize(R.dimen.dp_12);
            }
            pointList.add(v);
            this.addView(v, lp);
        }
        setPointPosition(0);
    }

    public void setPointPosition(int position) {
        int pointSize = pointList.size();
        if (pointSize == 0) {
            return;
        }
        if (lastSelectedView != null){
            lastSelectedView.setVisibility(View.VISIBLE);
        }
        int pointPosition = position % pointSize;
        if (pointSize > 0 && pointPosition < pointSize) {
            for (View v : pointList) {
                ViewGroup.LayoutParams unSelectedLayoutParams = v.getLayoutParams();
                unSelectedLayoutParams.width = mPointUnselectedWidth;
                unSelectedLayoutParams.height = mPointHeight;
                v.setLayoutParams(unSelectedLayoutParams);
                v.setBackgroundResource(R.drawable.background_4d337aff_oval);
            }
            View selectedView = pointList.get(pointPosition);
            selectedView.setVisibility(View.INVISIBLE);
            ViewGroup.LayoutParams selectedLayoutParams = selectedView.getLayoutParams();
            selectedLayoutParams.width = mPointSelectedWidth;
            selectedLayoutParams.height = mPointHeight;
            selectedView.setLayoutParams(selectedLayoutParams);
            selectedView.setBackgroundResource(R.drawable.background_ff337aff_rectangle_5);
            selectedAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    selectedView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            selectedView.startAnimation(selectedAnimation);
            lastSelectedView = selectedView;
        }

    }
}
