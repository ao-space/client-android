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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 任务进度对话框
 * History:     2023/4/17
 */
public class TaskProgressDialog extends Dialog {
    private TextView mTvTaskName;
    private TextView mTvPercent;
    private ProgressBar mProgressBar;

    public TaskProgressDialog(Context context) {
        super(context, R.style.EulixDialog);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public static class Builder {
        private Context mContext;
        private String mTaskNameText;
        private int mPercent = 0;
        private boolean mCancelable = false;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setTaskName(String text) {
            this.mTaskNameText = text;
            return this;
        }

        public TaskProgressDialog create() {
            TaskProgressDialog dialog = new TaskProgressDialog(mContext);

            Window window = dialog.getWindow();
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = mContext.getResources().getDimensionPixelSize(R.dimen.dp_259);
            window.setAttributes(layoutParams);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.eulix_task_progress_dialog, null);

            dialog.mTvTaskName = view.findViewById(R.id.tv_task_name);
            dialog.mTvPercent = view.findViewById(R.id.tv_percent);
            dialog.mProgressBar = view.findViewById(R.id.progress_bar);

            dialog.mTvTaskName.setText(mTaskNameText + "…");
            dialog.mTvPercent.setText(mPercent + "%");

            dialog.setContentView(view);
            dialog.setCancelable(mCancelable);

            return dialog;
        }
    }

    public void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            return;
        }
        mTvPercent.setText(progress + "%");
        if (progress < 3) {
            //圆角进度进度太少时会变形，需要切割
            mProgressBar.setProgressDrawable(getContext().getDrawable(R.drawable.transfer_progress_dialog_bg_less));
        } else {
            mProgressBar.setProgressDrawable(getContext().getDrawable(R.drawable.transfer_progress_dialog_bg));
        }
        mProgressBar.setProgress(progress);
    }

}
