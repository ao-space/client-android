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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.event.BoxSystemRestartEvent;
import xyz.eulix.space.event.BoxVersionCheckEvent;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.BoxNetworkCheckManager;
import xyz.eulix.space.presenter.SystemUpdatePresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.view.TitleBarWithSelect;
import xyz.eulix.space.view.dialog.EulixDialogUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 系统升级页面
 * History:     2021/10/26
 */
public class SystemUpdateActivity extends AbsActivity<SystemUpdatePresenter.ISystemUpdate, SystemUpdatePresenter> implements SystemUpdatePresenter.ISystemUpdate {
    private TitleBarWithSelect titleBar;
    private LinearLayout layoutAutoSetting;
    private TextView tvAutoState;
    private TextView tvVersionName;
    private TextView tvVersionSubtitle;
    private TextView tvChangeLog;
    private LinearLayout btnConfirm;
    private ImageView imageSplit;
    private LottieAnimationView loadingAnim;
    private TextView tvBtnText;
    private TextView tvUpgradingNotice;
    private ImageView imageLogo;
    private ImageView imgAutoSettingSplit;
    private TextView tvViewDetail;

    private boolean isFromLaunch = false;
    //对话框选择的操作方式
    private int mDialogOperateType = 0;

    public static final String KEY_DIALOG_OPERATE_TYPE = "key_dialog_operate_type";

    //立即安装
    public static final int DIALOG_OPERATE_TYPE_INSTALL_NOW = 1;
    //稍后安装
    public static final int DIALOG_OPERATE_TYPE_INSTALL_LATER = 2;

    //是否退出页面
    private boolean isPageOut = false;

    @Override
    public void initView() {
        setContentView(R.layout.activity_system_update);
        titleBar = findViewById(R.id.title_bar);
        layoutAutoSetting = findViewById(R.id.layout_auto_setting);
        tvAutoState = findViewById(R.id.tv_auto_update_state);
        tvVersionName = findViewById(R.id.tv_version_name);
        tvVersionSubtitle = findViewById(R.id.tv_version_subtitle);
        tvChangeLog = findViewById(R.id.tv_changelog);
        btnConfirm = findViewById(R.id.btn_confirm);
        loadingAnim = findViewById(R.id.loading_animation);
        tvBtnText = findViewById(R.id.tv_btn_text);
        imageSplit = findViewById(R.id.img_split);
        tvUpgradingNotice = findViewById(R.id.tv_upgrading_notice);
        imageLogo = findViewById(R.id.image_logo);
        imgAutoSettingSplit = findViewById(R.id.img_auto_setting_split);
        tvViewDetail = findViewById(R.id.tv_view_detail);

        EventBusUtil.register(this);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        isFromLaunch = getIntent().getBooleanExtra("isFromLaunch", false);
        mDialogOperateType = getIntent().getIntExtra(KEY_DIALOG_OPERATE_TYPE, 0);
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(getResources().getString(R.string.system_update));

        presenter.getAutoUpgradeConfig();
        refreshConfigState(PreferenceUtil.getUpgradeAutoInstall(this)
                || PreferenceUtil.getUpgradeAutoDownload(this));

        if (isFromLaunch) {
            titleBar.hideBackButton();
            layoutAutoSetting.setVisibility(View.GONE);
            imgAutoSettingSplit.setVisibility(View.GONE);
        }
        changeBtnState(false, false);

        if (ConstantField.boxVersionCheckBody != null) {
            //有新版本数据
            refreshViewsState(true);
            showLoading("");
            showStatueByType();
        } else {
            //检查是否有新版本
            showLoading("");
            presenter.checkBoxVersion(new ResultCallbackObj() {
                @Override
                public void onResult(boolean result, Object extraObj) {
                    if (ConstantField.boxVersionCheckBody != null) {
                        //有新版本
                        refreshViewsState(true);
                        showStatueByType();
                        closeLoading();
                    } else {
                        boolean isRefreshBoxVersion = true;
                        //已是最新版
                        if (TextUtils.isEmpty(PreferenceUtil.getCurrentBoxVersion(SystemUpdateActivity.this))) {
                            presenter.getCurrentBoxVersion();
                            isRefreshBoxVersion = false;
                        }
                        refreshViewsState(false);
                        showStatueByType(isRefreshBoxVersion);
                    }
                }

                @Override
                public void onError(String msg) {
                    showStatueByType();
                    closeLoading();
                    if (!TextUtils.isEmpty(msg) && msg.contains(String.valueOf(ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR))) {
                        showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                    }
                }
            });
        }

//        //退出后恢复查询
//        if (SystemUpdatePresenter.sUpgradeState > SystemUpdatePresenter.STATE_NORMAL) {
//            changeBtnState(true);
//            presenter.starPollingCheckTask();
//        } else {
//            changeBtnState(false);
//        }
    }

    private void showStatueByType() {
        showStatueByType(true);
    }

    private void showStatueByType(boolean isRefreshCurrentBoxVersion) {
        if (mDialogOperateType == DIALOG_OPERATE_TYPE_INSTALL_NOW) {
            //对话框-立即安装
            closeLoading();
            if (ConstantField.boxVersionCheckBody != null) {
                if (!isLoadingShowing()) {
                    showLoading(getResources().getString(R.string.installing_system_upgrade));
                }
                presenter.startUpgrade(false);
                changeBtnState(true, true);
            }
        } else if (mDialogOperateType == DIALOG_OPERATE_TYPE_INSTALL_LATER) {
            //对话框-详细信息、稍后安装
            closeLoading();
            changeBtnState(false, true);
        } else {
            //默认，检查一次升级状态，若已下载，则提示“现在安装”
            presenter.checkUpgradeStatusOnce((result, extraMsg) -> {
                closeLoading();
                if (result && !TextUtils.isEmpty(extraMsg)) {
                    switch (extraMsg) {
                        case ConstantField.UpgradeStatus.STATUS_PULLED:
                            //已下载
                            changeBtnState(false, true);
                            break;
                        case ConstantField.UpgradeStatus.STATUS_PULLING:
                            //下载中
                            changeBtnState(true, false);
                            break;
                        case ConstantField.UpgradeStatus.STATUS_UPPING:
                            //安装中 - 不进行状态恢复
                            break;
                        case ConstantField.UpgradeStatus.STATUS_UPPED:
                            changeBtnState(false, false);
                            if (isRefreshCurrentBoxVersion && presenter != null) {
                                presenter.getCurrentBoxVersion();
                            }
                            break;
                        default:
                            changeBtnState(false, false);
                            break;
                    }
                } else {
                    changeBtnState(false, false);
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isFromLaunch && SystemUpdatePresenter.sUpgradeState > SystemUpdatePresenter.STATE_NORMAL) {
            EulixDialogUtil.showNoticeAlertDialog(this, getResources().getString(R.string.program_incompatibility), "请完成系统升级",
                    getResources().getString(R.string.i_know), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent(BoxVersionCheckEvent event) {
//        closeLoading();
//        if (ConstantField.boxVersionCheckBody != null) {
//            refreshViewsState(true);
//        } else {
//            refreshViewsState(false);
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxSystemRestartEvent event) {
        //接受到系统重启
        Logger.d("zfy", "receive BoxSystemRestartEvent");
        if (SystemUpdatePresenter.sUpgradeState > SystemUpdatePresenter.STATE_NORMAL) {
            Logger.d("zfy", "system upgrade task is progress");
            closeLoading();
            showLoading(getResources().getString(R.string.restarting_device));
        } else {
            Logger.d("zfy", "system upgrade task is finish");
        }
    }

    @Override
    public void onCheckVersionError() {
        closeLoading();
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            showServerExceptionToast();
        } else {
            new Handler(Looper.getMainLooper()).post(this::showServerExceptionToast);
        }
    }


    @Override
    public void onGetCurrentVersion(String currentVersion) {
        String currentBoxVersion = PreferenceUtil.getCurrentBoxVersion(this);
        if ((TextUtils.isEmpty(currentBoxVersion) || (currentVersion != null && !currentVersion.equals(currentBoxVersion)))) {
            PreferenceUtil.saveCurrentBoxVersion(this, currentVersion);
            tvVersionName.setText(getResources().getString(R.string.app_name) + " " + StringUtil.nullToEmpty(currentVersion));
        }
    }

    private void refreshViewsState(boolean hasUpdate) {
        closeLoading();
        if (hasUpdate && ConstantField.boxVersionCheckBody != null && ConstantField.boxVersionCheckBody.latestBoxPkg != null) {
            tvVersionName.setText(getResources().getString(R.string.app_name) + " " + ConstantField.boxVersionCheckBody.latestBoxPkg.pkgVersion);
            tvVersionSubtitle.setText(getResources().getString(R.string.update_size) + FormatUtil.formatSize(ConstantField.boxVersionCheckBody.latestBoxPkg.pkgSize, ConstantField.SizeUnit.FORMAT_1F));
            tvChangeLog.setText(ConstantField.boxVersionCheckBody.latestBoxPkg.updateDesc);
            btnConfirm.setVisibility(View.VISIBLE);
            tvViewDetail.setVisibility(View.GONE);
        } else {
            tvVersionName.setText(getResources().getString(R.string.app_name) + " " + PreferenceUtil.getCurrentBoxVersion(this));
            tvVersionSubtitle.setText(getResources().getString(R.string.already_latest_version));
            tvChangeLog.setText("");
            btnConfirm.setVisibility(View.GONE);
            imageSplit.setVisibility(View.GONE);
            tvViewDetail.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void refreshConfigState(boolean isOpen) {
        tvAutoState.setText(getResources().getString(isOpen ? R.string.upgrade_open : R.string.upgrade_close));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (presenter != null && !presenter.isSupportSystemUpdate()) {
//            showDefaultPureTextToast(R.string.system_upgrade_unsupported);
//            finish();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPageOut = false;
        refreshConfigState(PreferenceUtil.getUpgradeAutoInstall(this)
                || PreferenceUtil.getUpgradeAutoDownload(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void initEvent() {
        layoutAutoSetting.setOnClickListener(v -> {
            Intent intent = new Intent(SystemUpdateActivity.this, AutoUpdateSettingActivity.class);
            startActivity(intent);
        });
        btnConfirm.setOnClickListener(v -> {
            if (tvBtnText.getText().equals(getResources().getString(R.string.download_and_install))
                    || tvBtnText.getText().equals(getResources().getString(R.string.install_now))) {
                String confirmTextStr = getResources().getString(R.string.update_now);
                if (ConstantField.boxVersionCheckBody != null && ConstantField.boxVersionCheckBody.latestBoxPkg != null && ConstantField.boxVersionCheckBody.latestBoxPkg.restart) {
                    confirmTextStr = getResources().getString(R.string.update_and_restart);
                }
                EulixDialogUtil.showChooseAlertDialog(this, getResources().getString(R.string.system_update),
                        getResources().getString(R.string.system_update_alert), confirmTextStr,
                        (dialog, which) -> {
                            boolean isPull = tvBtnText.getText().equals(getResources().getString(R.string.download_and_install));
                            if (!isPull && !isLoadingShowing()) {
                                showLoading(getResources().getString(R.string.installing_system_upgrade));
                            }
                            presenter.startUpgrade(isPull);
                        }, null);
            }
        });

        tvViewDetail.setOnClickListener(v -> {
            //跳转到系统规格
            DeviceVersionInfoBean deviceVersionInfoBean = null;
            String deviceCacheStr = PreferenceUtil.getDeviceVersionDetailInfo(this);
            if (!TextUtils.isEmpty(deviceCacheStr)) {
                deviceVersionInfoBean = new Gson().fromJson(deviceCacheStr, DeviceVersionInfoBean.class);
            }
            BoxSystemDetailActivity.startActivity(this, deviceVersionInfoBean);
        });
    }

    @Override
    protected int getActivityIndex() {
        return ConstantField.ActivityIndex.SYSTEM_UPDATE_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public SystemUpdatePresenter createPresenter() {
        return new SystemUpdatePresenter();
    }

    @Override
    public void onCheckInfoResult(Boolean result, String status) {
        if (result != null && result) {
            switch (status) {
                case ConstantField.UpgradeStatus.STATUS_PULLING:
                    //正在下拉镜像
                    changeBtnState(true, false);
                    break;
                case ConstantField.UpgradeStatus.STATUS_UPPING:
                    //正在升级
                    changeBtnState(true, true);
                    break;
                case ConstantField.UpgradeStatus.STATUS_PULLED:
                    //下拉镜像完成
                    changeBtnState(false, true);
                    //取消轮询
                    presenter.cancelPollingCheck();
                    //如果不是外部跳入，则自动安装
                    if (mDialogOperateType != DIALOG_OPERATE_TYPE_INSTALL_LATER && !isPageOut) {
                        if (!isLoadingShowing()) {
                            showLoading(getResources().getString(R.string.installing_system_upgrade));
                        }
                        presenter.startUpgrade(false);
                    } else {
                        changeBtnState(false, true);
                    }
                    break;
                case ConstantField.UpgradeStatus.STATUS_UPPED:
                    //升级完成
                    closeLoading();
                    btnConfirm.setVisibility(View.GONE);
                    tvUpgradingNotice.setVisibility(View.GONE);
                    imageSplit.setVisibility(View.GONE);
                    tvViewDetail.setVisibility(View.VISIBLE);
                    tvChangeLog.setVisibility(View.GONE);
                    PreferenceUtil.saveCurrentBoxVersion(this, ConstantField.boxVersionCheckBody.latestBoxPkg.pkgVersion);
                    ConstantField.boxVersionCheckBody = null;
                    updateBoxVersionInfo();
                    EventBusUtil.post(new BoxVersionCheckEvent());
                    if (isFromLaunch) {
                        EulixDialogUtil.showNoticeAlertDialog(this, getResources().getString(R.string.upgrade_complete), getResources().getString(R.string.upgrade_complete_desc),
                                getResources().getString(R.string.i_know), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                    } else {
                        showImageTextToast(R.drawable.toast_right, R.string.update_success);
                        obtainAccessToken();
                    }
                    break;
                case ConstantField.UpgradeStatus.STATUS_PULL_ERR:
                    //下载错误
                case ConstantField.UpgradeStatus.STATUS_UP_ERR:
                    closeLoading();
                    //升级错误
                    showImageTextToast(R.drawable.toast_wrong, R.string.update_failed);
                    changeBtnState(false, false);
                    break;
                default:
                    break;
            }
        } else {
            if (result == null) {
                showServerExceptionToast();
            } else {
                showImageTextToast(R.drawable.toast_wrong, R.string.update_failed);
            }
            changeBtnState(false, false);
        }
    }

    //更新设备版本信息
    private void updateBoxVersionInfo() {
        String boxVersionStr = PreferenceUtil.getDeviceVersionDetailInfo(this);
        if (!TextUtils.isEmpty(boxVersionStr)) {
            DeviceVersionInfoBean deviceVersionInfoBean = new Gson().fromJson(boxVersionStr, DeviceVersionInfoBean.class);
            deviceVersionInfoBean.spaceVersion = PreferenceUtil.getCurrentBoxVersion(this);
            Gson gson = new Gson();
            String newJsonStr = gson.toJson(deviceVersionInfoBean);
            Logger.d("jsonStr=" + newJsonStr);
            if (!TextUtils.isEmpty(newJsonStr)) {
                PreferenceUtil.saveDeviceVersionDetailInfo(this, newJsonStr);
                EventBusUtil.post(new BoxVersionDetailInfoEvent());
            }
        }
    }

    private void changeBtnState(boolean isUpdating, boolean hasPulled) {
        if (isUpdating) {
            loadingAnim.setVisibility(View.VISIBLE);
            LottieUtil.loop(loadingAnim, "loading_button.json");
            tvBtnText.setText(getResources().getString(hasPulled ? R.string.state_updating : R.string.state_downing));
            btnConfirm.setBackground(getResources().getDrawable(R.drawable.background_ffc3d8ff_ffbaebff_rectangle_10));
            BoxNetworkCheckManager.setShowOffline(false);
            tvUpgradingNotice.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams splitLP = (RelativeLayout.LayoutParams) imageSplit.getLayoutParams();
            splitLP.removeRule(RelativeLayout.BELOW);
            splitLP.addRule(RelativeLayout.BELOW, tvUpgradingNotice.getId());
            splitLP.topMargin = getResources().getDimensionPixelOffset(R.dimen.dp_18);
            imageSplit.setLayoutParams(splitLP);
        } else {
            LottieUtil.stop(loadingAnim);
            loadingAnim.setVisibility(View.GONE);
            tvBtnText.setText(getResources().getString(hasPulled ? R.string.install_now : R.string.download_and_install));
            btnConfirm.setBackground(getResources().getDrawable(R.drawable.background_ff337aff_ff16b9ff_rectangle_10));
            BoxNetworkCheckManager.setShowOffline(true);
            tvUpgradingNotice.setVisibility(View.GONE);
            RelativeLayout.LayoutParams splitLP = (RelativeLayout.LayoutParams) imageSplit.getLayoutParams();
            splitLP.removeRule(RelativeLayout.BELOW);
            splitLP.addRule(RelativeLayout.BELOW, imageLogo.getId());
            splitLP.topMargin = getResources().getDimensionPixelOffset(R.dimen.dp_24);
            imageSplit.setLayoutParams(splitLP);
        }
    }

    @Override
    protected void onStop() {
        isPageOut = true;
        super.onStop();
    }

    //获取accessToken
    public void obtainAccessToken() {
        Intent serviceIntent = new Intent(SystemUpdateActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.cancelPollingCheck();
        EventBusUtil.unRegister(this);
    }
}