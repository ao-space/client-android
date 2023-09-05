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

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 底部弹出Dialog
 * History:     2021/8/10
 */
public class BottomDialog extends Dialog {
    private View contentView;
    private Animation transitionInAnim;

    public BottomDialog(@NonNull Context context, boolean isDim) {
        this(context, (isDim ? R.style.BottomDialogTheme : R.style.BottomDialogThemeNoDim));
    }

    public BottomDialog(@NonNull Context context) {
        this(context, R.style.BottomDialogTheme);
    }

    public BottomDialog(@NonNull Context context, int themeResId) {
        this(context, themeResId, false);
    }

    public BottomDialog(@NonNull Context context, int themeResId, boolean isTransparent) {
        super(context, themeResId);
        transitionInAnim = AnimationUtils.loadAnimation(getContext(),
                R.anim.dialog_in_anim);
        Window window = getWindow();
        //设置导航横条颜色
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (isTransparent) {
                window.setNavigationBarColor(android.R.color.transparent);
            } else {
                window.setNavigationBarColor(getContext().getResources().getColor(R.color.white_ffffffff));
            }
        }
        window.setWindowAnimations(R.style.bottom_dialog_anim_style);
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void setContentView(@NonNull View view) {
        super.setContentView(view);
        this.contentView = view;
    }

    @Override
    public void show() {
        super.show();
        contentView.startAnimation(transitionInAnim);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}
