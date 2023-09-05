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

package xyz.eulix.space.view.rv;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;

import xyz.eulix.space.R;
import xyz.eulix.space.util.LottieUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 分页加载底部提示
 * History:     2021/9/29
 */
public class FooterView extends LinearLayout {
    private Context context;
    private TextView footer_tv;
    private LottieAnimationView footer_iv;
    private ImageView footer_complete_icon;

    public FooterView(Context context) {
        this(context, null);
    }

    public FooterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FooterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(context).inflate(R.layout.item_adapter_footer, this);
        // 给rootview(即this)设置LayoutParams，不然item_multi_adapter_footer没有LayoutParams参数，其根布局layout_width的match_parent不生效
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setVisibility(View.VISIBLE);
        footer_tv = view.findViewById(R.id.footer_tv);
        footer_iv = view.findViewById(R.id.footer_loading);
        footer_complete_icon = view.findViewById(R.id.footer_complete_icon);
        startLoading();
    }

    private void startLoading() {
        LottieUtil.loop(footer_iv, "loading_button_dark.json");
    }

    public void showLoading() {
        footer_tv.setText(context.getResources().getString(R.string.loading_more));
        footer_iv.setVisibility(View.VISIBLE);
        footer_complete_icon.setVisibility(GONE);
        startLoading();
    }


    public void showLoading(String text) {
        footer_tv.setText(text);
        footer_iv.setVisibility(View.VISIBLE);
        footer_complete_icon.setVisibility(GONE);
        startLoading();
    }

    public void showBottom() {
        LottieUtil.stop(footer_iv);
        footer_iv.setVisibility(View.GONE);
        footer_complete_icon.setVisibility(VISIBLE);
        footer_tv.setText(context.getResources().getString(R.string.home_bottom_flag));
    }

    public void showBottom(String text) {
        if (TextUtils.isEmpty(text)){
            showBottom();
        } else {
            LottieUtil.stop(footer_iv);
            footer_iv.setVisibility(View.GONE);
            footer_complete_icon.setVisibility(VISIBLE);
            footer_tv.setText(text);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //防止列表刷新后动画停止
        if (footer_iv != null) {
            startLoading();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LottieUtil.stop(footer_iv);
    }
}
