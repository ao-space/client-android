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

import android.view.View;
import android.widget.CheckBox;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.AutoUpdateSettingPresenter;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.TitleBarWithSelect;

public class AutoUpdateSettingActivity extends AbsActivity<AutoUpdateSettingPresenter.IAutoUpdateSetting, AutoUpdateSettingPresenter> implements AutoUpdateSettingPresenter.IAutoUpdateSetting {
    private TitleBarWithSelect titleBar;
    private CheckBox checkBoxDownload;
    private CheckBox checkBoxInstall;
    private View maskDownload;
    private View maskInstall;

    private boolean isChangeUpgrade = false;

    @Override
    public void initView() {
        setContentView(R.layout.activity_auto_update_setting);
        titleBar = findViewById(R.id.title_bar);
        checkBoxDownload = findViewById(R.id.checkbox_download);
        checkBoxInstall = findViewById(R.id.checkbox_install);
        maskDownload = findViewById(R.id.mask_check_download);
        maskInstall = findViewById(R.id.mask_check_install);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(getResources().getString(R.string.auto_update));
        checkBoxDownload.setChecked(PreferenceUtil.getUpgradeAutoDownload(this));
        checkBoxInstall.setChecked(PreferenceUtil.getUpgradeAutoInstall(this));
    }

    @Override
    public void initEvent() {
        checkBoxDownload.setEnabled(false);
        checkBoxInstall.setEnabled(false);
        maskDownload.setOnClickListener(v -> {
            isChangeUpgrade = false;
            showLoading("");
            boolean autoInstallSwitch = PreferenceUtil.getUpgradeAutoInstall(AutoUpdateSettingActivity.this);
            if (checkBoxDownload.isChecked()){
                //关闭自动下载，关联关闭自动安装
                autoInstallSwitch = false;
            }
            presenter.setUpgradeConfig(!checkBoxDownload.isChecked(), autoInstallSwitch);
        });
        maskInstall.setOnClickListener(v -> {
            isChangeUpgrade = true;
            showLoading("");
            boolean autoDownloadSwitch = PreferenceUtil.getUpgradeAutoDownload(AutoUpdateSettingActivity.this);
            if (!checkBoxInstall.isChecked()){
                //开启自动安装，关联开启自动下载
                autoDownloadSwitch = true;
            }
            presenter.setUpgradeConfig(autoDownloadSwitch, !checkBoxInstall.isChecked());
        });
    }

    @Override
    public void setResult(boolean autoDownload, boolean autoInstall) {
        closeLoading();
        checkBoxDownload.setChecked(autoDownload);
        checkBoxInstall.setChecked(autoInstall);
        if (isChangeUpgrade) {
            isChangeUpgrade = false;
        }
    }

    @Override
    public void setFailed(boolean isError) {
        closeLoading();
        if (isError) {
            showServerExceptionToast();
        } else {
            showImageTextToast(R.drawable.toast_refuse, R.string.setting_fail);
        }
    }

    @NotNull
    @Override
    public AutoUpdateSettingPresenter createPresenter() {
        return new AutoUpdateSettingPresenter();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}