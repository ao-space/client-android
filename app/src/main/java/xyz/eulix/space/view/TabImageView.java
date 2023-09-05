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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 主页导航栏tab控件
 * History:     2021/7/16
 */
public class TabImageView extends LinearLayout {
    private Context context;
    private ImageView imageView;
    private TextView tabText;
    private int selectedImgId;
    private int normalImgId;

    public TabImageView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public TabImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    private void initView(){
        View view = LayoutInflater.from(context).inflate(R.layout.layout_tab_image_view,this);
        imageView =view.findViewById(R.id.tab_image_view);
        tabText = view.findViewById(R.id.tab_text);
    }

    public void initRes(int selectImgId, int normalImgId, String tabName) {
        this.selectedImgId = selectImgId;
        this.normalImgId = normalImgId;
        tabText.setText(tabName);
        imageView.setImageDrawable(getResources().getDrawable(normalImgId));
    }

    public void setChecked(Boolean checked) {
        if (checked) {
            imageView.setImageDrawable(getResources().getDrawable(selectedImgId));
            tabText.setTextColor(getResources().getColor(R.color.c_ff337aff));
            setSelected(true);

        } else {
            imageView.setImageDrawable(getResources().getDrawable(normalImgId));
            tabText.setTextColor(getResources().getColor(R.color.c_ff85899c));
            setSelected(false);
        }
    }

}
