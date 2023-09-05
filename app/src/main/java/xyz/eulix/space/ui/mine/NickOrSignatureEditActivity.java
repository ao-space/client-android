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

package xyz.eulix.space.ui.mine;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.presenter.NickOrSignatureEditPresenter;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 昵称/个性签名编辑页面
 * History:     2021/9/15
 */
public class NickOrSignatureEditActivity extends AbsActivity<NickOrSignatureEditPresenter.INickOrSignatureEdit, NickOrSignatureEditPresenter> implements NickOrSignatureEditPresenter.INickOrSignatureEdit {
    private TitleBarWithSelect titleBar;
    private EditText eTContent;
    private TextView btnComplete;
    private TextView etHint;
    private String userUuid;
    private String clientUuid;

    public static final int TYPE_NICK = 1;
    public static final int TYPE_SIGNATURE = 2;
    private int mType;
    private int mMaxCount;
    private String mCurrentContent;

    //页面埋点名称
    private String mLogUpPageName;

    private NickOrSignatureEditHandler mHandler;

    static class NickOrSignatureEditHandler extends Handler {
        private WeakReference<NickOrSignatureEditActivity> nickOrSignatureEditActivityWeakReference;

        public NickOrSignatureEditHandler(NickOrSignatureEditActivity activity) {
            nickOrSignatureEditActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            NickOrSignatureEditActivity activity = nickOrSignatureEditActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_nick_or_singature_edit);

        titleBar = findViewById(R.id.title_bar);
        eTContent = findViewById(R.id.et_content);
        btnComplete = findViewById(R.id.btn_done);
        etHint = findViewById(R.id.et_hint);
        eTContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > mMaxCount) {
                    btnComplete.setVisibility(View.VISIBLE);
                    eTContent.setText(s.subSequence(0, mMaxCount));
                    Selection.setSelection(eTContent.getText(), mMaxCount);
                    showImageTextToast(R.drawable.toast_refuse, R.string.over_maxcount);
                }
            }
        });
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        mHandler = new NickOrSignatureEditHandler(this);
        clientUuid = DataUtil.getClientUuid(getApplicationContext());
        userUuid = null;
        Intent intent = getIntent();
        if (intent != null) {
            mType = intent.getIntExtra("type", TYPE_NICK);
            if (intent.hasExtra("user_uuid")) {
                userUuid = intent.getStringExtra("user_uuid");
            }
        }
    }

    @Override
    public void initViewData() {
        UserInfo userInfo;
        if (userUuid == null) {
            userInfo = presenter.getActiveUserInfo();
        } else {
            userInfo = presenter.getActiveUserInfo(userUuid);
        }
        if (mType == TYPE_NICK) {
            if (userInfo != null) {
                mCurrentContent = userInfo.getNickName();
            }
            titleBar.setTitle(getResources().getString(R.string.space_name));
            eTContent.setHint(getResources().getString(R.string.nick_hint));
            etHint.setVisibility(View.VISIBLE);
            mMaxCount = 24;
        } else {
            if (userInfo != null) {
                mCurrentContent = userInfo.getSignature();
            }
            titleBar.setTitle(getResources().getString(R.string.personal_signature));
            eTContent.setHint(getResources().getString(R.string.signature_hint));
            etHint.setVisibility(View.GONE);
            mMaxCount = 120;
        }
        if (!TextUtils.isEmpty(mCurrentContent)) {
            eTContent.setText(mCurrentContent);
        }
    }

    @Override
    public void initEvent() {
        btnComplete.setOnClickListener(v -> {
            String newContent = eTContent.getText().toString();
            //调用接口更新数据
            if (mType == TYPE_NICK && TextUtils.isEmpty(newContent)) {
                showImageTextToast(R.drawable.toast_wrong, R.string.empty_input_warning);
            } else {
                showLoading("");
                if (userUuid == null || userUuid.equals(clientUuid)) {
                    presenter.updateUserInfo(this, newContent, mType);
                } else {
                    presenter.updateMemberNickname(userUuid, newContent, mType);
                }
            }
        });
    }

    @NotNull
    @Override
    public NickOrSignatureEditPresenter createPresenter() {
        return new NickOrSignatureEditPresenter();
    }

    @Override
    public void onUpdateResult(Boolean result, String errorMsg) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (result != null && !result && UserInfoUtil.NICK_FORMAT_ERROR_CODE.equals(errorMsg)) {
                    //修改失败，空间标识不合法
                    showImageTextToast(R.drawable.toast_wrong, R.string.modify_nick_failed_format);
                    return;
                } else if (result != null && !result && UserInfoUtil.NICK_REPEAT_ERROR_CODE.equals(errorMsg)){
                    //修改失败，空间标识重复
                    showImageTextToast(R.drawable.toast_refuse, R.string.modify_nick_failed_repeat);
                    return;
                }
                if (result == null) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(result ? R.drawable.toast_right : R.drawable.toast_wrong, result ? R.string.modify_success : R.string.modify_userinfo_failed);
                }
                if (result != null && result) {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }
}