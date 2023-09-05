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

package xyz.eulix.space.view.smartrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshKernel;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;
import com.scwang.smart.refresh.layout.constant.SpinnerStyle;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.R;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: SmartRefreshLayout 自定义Header
 * History:     2021/12/13
 */
public class CustomRefreshHeader extends LinearLayout implements RefreshHeader {
    private Context mContext;
    private LottieAnimationView mAnimationView;
    private TextView tv_refresh;
    private int refreshTime = 0;

    public CustomRefreshHeader(Context context) {
        this(context, null);
    }

    public CustomRefreshHeader(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView();
    }

    private void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.layout_pull_to_refresh, this);
        mAnimationView = view.findViewById(R.id.iv_triangle);
        tv_refresh = view.findViewById(R.id.tv_refresh);
        mAnimationView.setAnimation("refresh_header.json");
        mAnimationView.setProgress(100);
        tv_refresh.setText(getResources().getString(R.string.refresh_header));
    }


    public void setAnimationViewJson(String animName) {
        mAnimationView.setAnimation(animName);
    }


    @NonNull
    @NotNull
    @Override
    public View getView() {
        return this;
    }

    @NonNull
    @NotNull
    @Override
    public SpinnerStyle getSpinnerStyle() {
        return SpinnerStyle.FixedBehind;
    }

    @Override
    public void setPrimaryColors(int... colors) {

    }

    @Override
    public void onInitialized(@NonNull @NotNull RefreshKernel kernel, int height, int maxDragHeight) {

    }

    @Override
    public void onMoving(boolean isDragging, float percent, int offset, int height, int maxDragHeight) {

    }

    @Override
    public void onReleased(@NonNull @NotNull RefreshLayout refreshLayout, int height, int maxDragHeight) {

    }

    @Override
    public void onStartAnimator(@NonNull @NotNull RefreshLayout refreshLayout, int height, int maxDragHeight) {
        Logger.d("zfy", "onStartAnimator");
        changeViewsVisible(true);
        mAnimationView.setProgress(0);
        mAnimationView.setRepeatCount(-1);
        mAnimationView.playAnimation();
    }

    @Override
    public int onFinish(@NonNull @NotNull RefreshLayout refreshLayout, boolean success) {
        Logger.d("zfy", "onAnimatorFinish");
        mAnimationView.cancelAnimation();
        changeViewsVisible(false);
        //重置动画，恢复位置
        mAnimationView.setProgress(100);
        return 0;
    }

    @Override
    public void onHorizontalDrag(float percentX, int offsetX, int offsetMax) {

    }

    @Override
    public boolean isSupportHorizontalDrag() {
        return false;
    }

    @Override
    public void onStateChanged(@NonNull @NotNull RefreshLayout refreshLayout, @NonNull @NotNull RefreshState oldState, @NonNull @NotNull RefreshState newState) {
        switch (newState) {
            case PullDownToRefresh:
                changeViewsVisible(true);
                break;
            case PullDownCanceled:
                changeViewsVisible(false);
                break;
            default:
                break;
        }
    }

    private void changeViewsVisible(boolean isVisible) {
        mAnimationView.setVisibility(isVisible ? VISIBLE : GONE);
        tv_refresh.setVisibility(isVisible ? VISIBLE : GONE);
    }
}
