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

package xyz.eulix.space.ui;

import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.event.NetworkStateEvent;
import xyz.eulix.space.event.TransferListNetworkEvent;
import xyz.eulix.space.presenter.ConfirmDialogThemePresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.NetUtils;

/**
 * Author:      Zhu Fuyu
 * Description: 全局确认弹框，暂只用于流量传输提示，后续可扩展
 * History:     2022/4/6
 */
public class ConfirmDialogThemeActivity extends AbsActivity<ConfirmDialogThemePresenter.IConfirmDialogTheme,ConfirmDialogThemePresenter> implements ConfirmDialogThemePresenter.IConfirmDialogTheme {
    private TextView mTvTitle;
    private TextView mTvContent;
    private Button btnConfirm;
    private Button btnCancel;

    public static void start(Context context) {
        Intent intent = new Intent(context, ConfirmDialogThemeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_confirm_dialog_theme);
        mTvTitle = findViewById(R.id.dialog_title);
        mTvContent = findViewById(R.id.dialog_content);
        btnConfirm = findViewById(R.id.dialog_confirm);
        btnCancel = findViewById(R.id.dialog_cancel);

        EventBusUtil.register(this);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initViewData() {
        mTvTitle.setText(getResources().getString(R.string.mobile_data_transfer));
        mTvContent.setText(getResources().getString(R.string.mobile_data_transfer_desc));
        btnConfirm.setText(getResources().getString(R.string.confirm));
        btnCancel.setText(getResources().getString(R.string.cancel));
    }

    @Override
    public void initEvent() {
        btnConfirm.setOnClickListener(v -> {
            ConstantField.sIAllowTransferWithMobileData = true;
            finish();
        });

        btnCancel.setOnClickListener(v -> {
            EventBusUtil.post(new TransferListNetworkEvent());
            finish();
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NetworkStateEvent event){
        if (NetUtils.isWifiConnected(this)){
            finish();
        }
    }

    @NonNull
    @Override
    public ConfirmDialogThemePresenter createPresenter() {
        return new ConfirmDialogThemePresenter();
    }

    @Override
    public void onBackPressed() {
        //屏蔽返回按键
//        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBusUtil.unRegister(this);
    }
}