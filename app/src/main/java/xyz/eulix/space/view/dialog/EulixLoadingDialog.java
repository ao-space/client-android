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

package xyz.eulix.space.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;

import xyz.eulix.space.R;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.LottieUtil;

/**
 * Author:      Zhu Fuyu
 * Description: loading对话框
 * History:     2021/8/25
 */
public class EulixLoadingDialog extends Dialog {
    private TextView mTextView;
    //    private ImageView mImage;
    private LottieAnimationView mLottie;

    public EulixLoadingDialog(Context context) {
        super(context, R.style.EulixDialog);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public static class Builder {
        private Context mContext;
        private String mText;
        private boolean mCancelable = false;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setText(String text) {
            this.mText = text;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.mCancelable = cancelable;
            return this;
        }

        public EulixLoadingDialog createDialog() {
            EulixLoadingDialog dialog = new EulixLoadingDialog(mContext);

            View view = View.inflate(mContext, R.layout.loading_dialog_layout, null);

            dialog.mTextView = view.findViewById(R.id.logout_dialog_tv);
//            dialog.mImage = view.findViewById(R.id.loading_dialog_image);
            dialog.mLottie = view.findViewById(R.id.loading_dialog_lottie);
//            RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE);
//            Glide.with(mContext).load(R.drawable.waiting).apply(options).into(dialog.mImage);
//            dialog.setAni(mContext, dialog.mImage);

            if (!TextUtils.isEmpty(mText)) {
                dialog.mTextView.setText(mText);
            } else {
                dialog.mTextView.setText(mContext.getString(R.string.waiting));
            }

            dialog.setContentView(view);
            dialog.setCancelable(mCancelable);
            return dialog;

        }
    }

    public void setText(String text) {
        mTextView.setText(text);
    }

    @Override
    public void show() {
        try {
            setLottie(mLottie);
            super.show();
//        setAni(getContext(), mImage);
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }
    }

    @Override
    public void dismiss() {
        if (isShowing()) {
            try {
                super.dismiss();
//            mImage.clearAnimation();
                LottieUtil.stop(mLottie);
            } catch (Exception e) {

            }
        }
    }

    public void setAni(Context context, ImageView image) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.anim_loading_rotate);
        LinearInterpolator interPolator = new LinearInterpolator();
        animation.setInterpolator(interPolator);
        image.setAnimation(animation);
    }

    public void setLottie(LottieAnimationView lottieAnimationView) {
        LottieUtil.loop(lottieAnimationView, "default_loading.json");
    }
}
