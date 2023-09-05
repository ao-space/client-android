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

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/11/12
 */

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;


/**
 * Author:      Zhu Fuyu
 * Description: 支持点击跳转span
 * History:     2021/11/12
 */
public class LinkClickSpan extends ClickableSpan {
    @Override
    public void onClick(View widget) {

    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(EulixSpaceApplication.getContext().getResources().getColor(R.color.c_ff337aff));
        ds.setUnderlineText(true);
    }


}

