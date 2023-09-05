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

import android.app.Dialog;
import android.content.Intent;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.BuildConfig;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.event.AppCheckRequestEvent;
import xyz.eulix.space.event.AppCheckResponseEvent;
import xyz.eulix.space.event.AppInstallEvent;
import xyz.eulix.space.event.AppUpdateEvent;
import xyz.eulix.space.event.BoxVersionCheckEvent;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.presenter.AboutUsPresenter;
import xyz.eulix.space.ui.EulixWebViewActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ToastUtil;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 关于我们页面
 * History:     2021/8/23
 */
public class AboutUsActivity extends AbsActivity<AboutUsPresenter.IAboutUs, AboutUsPresenter> implements AboutUsPresenter.IAboutUs, View.OnClickListener {
    private TitleBarWithSelect titleBar;
    private Button cancel, update;
    private ImageView versionUpdateImage;
    private TextView tvCurrentVersionName;
    private TextView versionUpdateContent;
    private LinearLayout checkUpdateContainer;
    private LinearLayout layoutSystemVersion;
    private TextView tvSystemVersion;
    private ImageView systemVersionIndicator;
    private View newSystemVersionReminder;
    private View newVersionReminder;
    private View versionUpdateDialogView;
    private Dialog versionUpdateDialog;
    private LinearLayout layoutPrivacy;
    private LinearLayout layoutAgreement;
    private ImageView imgLogo;

    private int mClickCount = 0;

    @Override
    public void initView() {
        setContentView(R.layout.activity_about_us);
        titleBar = findViewById(R.id.title_bar);
        tvCurrentVersionName = findViewById(R.id.tv_version_name);
        checkUpdateContainer = findViewById(R.id.layout_check_update);
        newVersionReminder = findViewById(R.id.new_version_reminder);
        layoutSystemVersion = findViewById(R.id.layout_system_version);
        tvSystemVersion = findViewById(R.id.tv_system_version);
        newSystemVersionReminder = findViewById(R.id.new_system_version_reminder);
        systemVersionIndicator = findViewById(R.id.system_version_indicator);
        imgLogo = findViewById(R.id.img_logo);

        versionUpdateDialogView = LayoutInflater.from(this).inflate(R.layout.version_update_dialog, null);
        versionUpdateImage = versionUpdateDialogView.findViewById(R.id.version_update_image);
        versionUpdateContent = versionUpdateDialogView.findViewById(R.id.version_update_content);
        cancel = versionUpdateDialogView.findViewById(R.id.cancel);
        update = versionUpdateDialogView.findViewById(R.id.update);
        versionUpdateDialog = new Dialog(this, R.style.EulixDialog);
        versionUpdateDialog.setCancelable(false);
        versionUpdateDialog.setContentView(versionUpdateDialogView);

        layoutPrivacy = findViewById(R.id.layout_privacy);
        layoutAgreement = findViewById(R.id.layout_agreement);

        EventBusUtil.register(this);
    }

    @Override
    public void initData() {
        mClickCount = 0;
    }

    @Override
    public void initViewData() {
        if (presenter != null) {
            presenter.initAppUpdate();
        }
        titleBar.setTitle(R.string.about);
        String currentVersionName = "v" + BuildConfig.VERSION_NAME;
        tvCurrentVersionName.setText(currentVersionName);
        boolean isActiveUserAdmin = false;
        boolean isSupportSystemUpdate = true;
        if (presenter != null) {
            newVersionReminder.setVisibility((presenter.isUpdate() ? View.VISIBLE : View.INVISIBLE));
            isActiveUserAdmin = presenter.isActiveUserAdmin();
            isSupportSystemUpdate = presenter.isSupportSystemUpdate();
        }
        systemVersionIndicator.setVisibility((isActiveUserAdmin) ? View.VISIBLE : View.INVISIBLE);

        String deviceCacheStr = PreferenceUtil.getDeviceVersionDetailInfo(this);
        if (!TextUtils.isEmpty(deviceCacheStr)) {
            DeviceVersionInfoBean deviceVersionInfoBean = new Gson().fromJson(deviceCacheStr, DeviceVersionInfoBean.class);
            if (presenter != null) {
                presenter.deviceVersionInfoBean = deviceVersionInfoBean;
            }
            if (deviceVersionInfoBean != null) {
                refreshDeviceInfoViews(deviceVersionInfoBean);
            }
        }
        if (presenter != null) {
            presenter.getDeviceVersionDetailInfo();
        }
        if (isActiveUserAdmin && ConstantField.boxVersionCheckBody != null) {
            newSystemVersionReminder.setVisibility(View.VISIBLE);
        } else {
            newSystemVersionReminder.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void initEvent() {
        checkUpdateContainer.setOnClickListener(this);
        if (presenter != null && presenter.isActiveUserAdmin()) {
            layoutSystemVersion.setOnClickListener(this);
        } else {
            layoutSystemVersion.setClickable(false);
        }
        cancel.setOnClickListener(this);
        update.setOnClickListener(this);
        versionUpdateContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        layoutPrivacy.setOnClickListener(v -> {
            String url = FormatUtil.isChinese(FormatUtil.getLocale(AboutUsActivity.this)
                    , false) ? ConstantField.URL.PRIVACY_API
                    : ConstantField.URL.EN_PRIVACY_API;
            EulixWebViewActivity.startWeb(this, getResources().getString(R.string.privacy_policy), url);
        });

        layoutAgreement.setOnClickListener(v -> {
            String url = FormatUtil.isChinese(FormatUtil.getLocale(AboutUsActivity.this)
                    , false) ? ConstantField.URL.AGREEMENT_API
                    : ConstantField.URL.EN_AGREEMENT_API;
            EulixWebViewActivity.startWeb(this, getResources().getString(R.string.user_agreement), url);
        });

        imgLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mClickCount++;
                if (mClickCount > 1) {
                    boolean newLoggerSwitch = !PreferenceUtil.getLoggerSwitch(AboutUsActivity.this);
                    PreferenceUtil.saveLoggerSwitch(AboutUsActivity.this, newLoggerSwitch);
                    Logger.setDebuggable(BuildConfig.LOG_SWITCH || newLoggerSwitch);
                    Log.d("eulix", "change log switch:" + newLoggerSwitch + ",isDebug:" + BuildConfig.DEBUG);
                    ToastUtil.showToast("Debug Mode: " + (Logger.isDebuggable() ? "Open" : "Close"));
                    mClickCount = 0;
                }
                return false;
            }
        });

        checkAppUpdate(false);
    }

    private void prepareAppUpdateShow() {
        versionUpdateContent.scrollTo(0, 0);
        StringBuilder versionBuilder = new StringBuilder();
        versionBuilder.append(getString(R.string.newest_version));
        versionBuilder.append(getString(R.string.colon));
        versionBuilder.append("V");
        String newestVersion = presenter.getNewestVersion();
        versionBuilder.append(newestVersion == null ? "" : newestVersion);
        versionBuilder.append("\n");
        versionBuilder.append(getString(R.string.new_version_size));
        versionBuilder.append(getString(R.string.colon));
        Long apkSize = presenter.getApkSize();
        if (apkSize == null) {
            versionBuilder.append(getString(R.string.unknown));
        } else {
            versionBuilder.append(FormatUtil.formatSize(apkSize, ConstantField.SizeUnit.FORMAT_2F));
        }
        versionBuilder.append("\n");
        versionBuilder.append(getString(R.string.update_content));
        versionBuilder.append(getString(R.string.colon));
        versionBuilder.append("\n");
        String updateDescription = presenter.getUpdateDescription();
        versionBuilder.append(updateDescription == null ? "" : updateDescription);
        versionUpdateContent.setText(versionBuilder.toString());
    }

    private void showAppCheckDialog() {
        if (versionUpdateDialog != null && !versionUpdateDialog.isShowing()) {
            versionUpdateDialog.show();
            RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE);
            Glide.with(this).load(R.drawable.version_update).apply(options).into(versionUpdateImage);
            Window window = versionUpdateDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259)
                        , getResources().getDimensionPixelSize(R.dimen.dp_365));
            }
        }
    }

    private void dismissAppCheckDialog() {
        if (versionUpdateDialog != null && versionUpdateDialog.isShowing()) {
            versionUpdateImage.setImageDrawable(null);
            versionUpdateDialog.dismiss();
        }
    }

    private void checkAppUpdate(boolean isForce) {
        AppCheckRequestEvent requestEvent = new AppCheckRequestEvent(isForce);
        EventBusUtil.post(requestEvent);
    }

    private void updateApp() {
        if (presenter != null) {
            AppUpdateEvent appUpdateEvent = new AppUpdateEvent(presenter.getApkSize(), presenter.getDownloadUrl()
                    , presenter.getMd5(), presenter.getNewestVersion(), false);
            showPureTextToast(R.string.download_newest_version);
            EventBusUtil.post(appUpdateEvent);
        }
    }

    @Override
    protected void onDestroy() {
        EventBusUtil.unRegister(this);
        super.onDestroy();
    }

    @Override
    protected int getActivityIndex() {
        return ConstantField.ActivityIndex.ABOUT_US_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public AboutUsPresenter createPresenter() {
        return new AboutUsPresenter();
    }

    @Override
    public void refreshDeviceInfoViews(DeviceVersionInfoBean deviceVersionInfoBean) {
        if (deviceVersionInfoBean != null) {
            tvSystemVersion.setText(StringUtil.nullToEmpty(deviceVersionInfoBean.spaceVersion));
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.layout_check_update:
                    if (presenter != null && presenter.isUpdate()) {
                        prepareAppUpdateShow();
                        showAppCheckDialog();
                    } else {
                        checkAppUpdate(true);
                    }
                    break;
                case R.id.layout_system_version:
                    if (presenter != null && presenter.isActiveUserAdmin()) {
                        //进入系统升级页面
                        Intent intent = new Intent(AboutUsActivity.this, SystemUpdateActivity.class);
                        if (ConstantField.hasClickSystemUpgradeInstallLater) {
                            intent.putExtra(SystemUpdateActivity.KEY_DIALOG_OPERATE_TYPE, SystemUpdateActivity.DIALOG_OPERATE_TYPE_INSTALL_LATER);
                        }
                        startActivity(intent);
                    }
                    break;
                case R.id.cancel:
                    dismissAppCheckDialog();
                    break;
                case R.id.update:
                    updateApp();
                    dismissAppCheckDialog();
                    break;
                default:
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppCheckResponseEvent event) {
        if (event != null) {
            if (event.isPlatformConnectFail()) {
                showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                return;
            }
            Boolean isUpdate = event.getUpdate();
            if (isUpdate != null) {
                newVersionReminder.setVisibility((isUpdate ? View.VISIBLE : View.INVISIBLE));
                if (presenter != null) {
                    presenter.setUpdate(isUpdate);
                    presenter.setApkSize(event.getApkSize());
                    presenter.setDownloadUrl(StringUtil.nullToEmpty(event.getDownloadUrl()));
                    presenter.setMd5(StringUtil.nullToEmpty(event.getMd5()));
                    presenter.setNewestVersion(StringUtil.nullToEmpty(event.getNewestVersion()));
                    presenter.setUpdateDescription(StringUtil.nullToEmpty(event.getUpdateDescription()));
                }
                if (isUpdate) {
                    if (event.isRemindForce()) {
                        prepareAppUpdateShow();
                        showAppCheckDialog();
                    }
                } else if (event.isRemindForce()) {
                    showPureTextToast(R.string.latest_version_already);
                }
            } else if (event.isRemindForce()) {
                showServerExceptionToast();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppInstallEvent event) {
        if (event != null && !event.isForce()) {
            String filePath = event.getFilePath();
            if (filePath != null && event.isSuccess()) {
                finish();
            } else {
                showPureTextToast(R.string.download_failed);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxVersionCheckEvent event) {
        Logger.d("zfy", "device manager BoxVersionCheckEvent");
        if (ConstantField.boxVersionCheckBody != null) {
            newSystemVersionReminder.setVisibility(View.VISIBLE);
        } else {
            newSystemVersionReminder.setVisibility(View.INVISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxVersionDetailInfoEvent event) {
        String deviceCacheStr = PreferenceUtil.getDeviceVersionDetailInfo(this);
        if (!TextUtils.isEmpty(deviceCacheStr)) {
            DeviceVersionInfoBean deviceVersionInfoBean = new Gson().fromJson(deviceCacheStr, DeviceVersionInfoBean.class);
            presenter.deviceVersionInfoBean = deviceVersionInfoBean;
            if (deviceVersionInfoBean != null) {
                refreshDeviceInfoViews(deviceVersionInfoBean);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}