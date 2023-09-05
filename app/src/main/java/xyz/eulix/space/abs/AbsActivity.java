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

package xyz.eulix.space.abs;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import xyz.eulix.space.EulixDeviceListActivity;
import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.bean.ApplicationLockEventInfo;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.bean.PushBean;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.ApplicationLockEvent;
import xyz.eulix.space.event.EulixPushEvent;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.manager.EulixBiometricManager;
import xyz.eulix.space.manager.EulixPushManager;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.network.push.LoginBean;
import xyz.eulix.space.network.push.LoginConfirmBean;
import xyz.eulix.space.network.push.SecurityApplyBean;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.ui.EulixImageActivity;
import xyz.eulix.space.ui.authorization.GranterAuthorizationActivity;
import xyz.eulix.space.ui.mine.DeviceManageActivity;
import xyz.eulix.space.ui.mine.SystemUpdateActivity;
import xyz.eulix.space.ui.mine.security.GranterSecurityPasswordAuthenticationActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ToastManager;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.view.dialog.EulixLoadingDialog;
import xyz.eulix.space.view.dialog.SystemUpgradeNoticeDialog;
import xyz.eulix.space.view.gesture.UpFlingGestureDetector;


/**
 * Author:      Zhu Fuyu
 * Description: 基础Activity类，主要承载UI、事件/消息接收、生命周期相关
 * History:     2021/7/16
 */
public abstract class AbsActivity<V extends IBaseView, P extends AbsPresenter<V>> extends AppCompatActivity implements IBaseView {
    public P presenter;
    protected static final int EULIX_SPACE_LAUNCH_ACTIVITY_INDEX = -1;
    protected static final int BIND_SERIES_ACTIVITY_INDEX = EULIX_SPACE_LAUNCH_ACTIVITY_INDEX - 1;
    protected static final int GRANT_SERIES_ACTIVITY_INDEX = BIND_SERIES_ACTIVITY_INDEX - 1;
    protected static final int SECURITY_SERIES_ACTIVITY_INDEX = GRANT_SERIES_ACTIVITY_INDEX - 1;
    protected static final int DEVELOPER_SERIES_ACTIVITY_INDEX = SECURITY_SERIES_ACTIVITY_INDEX - 1;
    private EulixLoadingDialog mLoadingDialog;
    private UpFlingGestureDetector topNotificationGestureDetector;
    private UpFlingGestureDetector lanDialogGestureDetector;
    private View lanDialogView;
    private View topNotificationDialogView;
    private LinearLayout topNotificationContainer;
    private RelativeLayout upgradeNotificationContainer;
    private ImageView imgTowLinesNotification;
    private ImageView topNotificationImage;
    private TextView topNotificationText;
    private TextView memberDeleteContent;
    private TextView topNotificationTwoTitle;
    private TextView topNotificationTwoDesc;
    private Dialog oneButtonDialog;
    private TextView oneButtonDialogTitle;
    private TextView oneButtonDialogContent;
    private Button oneButtonDialogConfirm;
    private Dialog lanDialog;
    private Dialog waitingDialog;
    private Dialog topNotificationDialog;
    private Dialog logoutDialog;
    private Dialog memberDeleteDialog;
    private List<Dialog> strongPushDialogList;
    private ToastManager toastManager;
    private AbsReceiver mReceiver;
    private boolean isRegisterBroadcast;
    private boolean isVisible = false;
    private boolean lastLanState = false;
    private PushBean mStrongPushBean;
    private PushBean mWeakPushBean;
    private Boolean strongPushLock = null;
    private Boolean weakPushLock = null;
    // 负数：不处理推送；0：正常页面；正数：特殊页面，处理跳转逻辑时使用
    private int activityIndex;
    private Context mContext;
    // false时自行处理应用锁
    protected boolean applicationLockImmediately = true;
    protected boolean isStart = false;
    protected boolean isImmersion = false;
    protected boolean isStatusBarImmersion = false;
    protected boolean isNavigationBarImmersion = false;

    private Runnable dismissTopNotificationRunnable = this::dismissTopNotificationDialog;

    private Runnable releaseWeakPushLockRunnable = () -> handleWeakPushLock(false);

    /**
     * 动态加载沉浸式布局
     */
    protected void immersion() {
        if (isStatusBarImmersion || isNavigationBarImmersion) {
            Window phoneWindow = getWindow();
            if (phoneWindow != null) {
                phoneWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                View decorView = phoneWindow.getDecorView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
                phoneWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                if (isStatusBarImmersion) {
                    phoneWindow.setStatusBarColor(Color.TRANSPARENT);
                }
                if (isNavigationBarImmersion) {
                    phoneWindow.setNavigationBarColor(Color.TRANSPARENT);
                }
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isImmersion = isImmersion();
        isStatusBarImmersion = isStatusBarImmersion();
        isNavigationBarImmersion = isNavigationBarImmersion();
        mReceiver = new AbsReceiver();
        if (!isImmersion && !isStatusBarImmersion) {
            if (!isDialogStyle()) {
                StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_fff5f6fa), this);
            }
            resetStatusBar();
        }
        if (!isImmersion) {
            immersion();
        }
        activityIndex = getActivityIndex();
        initLocale();
        initData();
        initView();
        //设置底部导航栏颜色
        if (!isImmersion && !isNavigationBarImmersion && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!isStatusBarImmersion) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            getWindow().setNavigationBarColor(getResources().getColor(R.color.white_ffffffff));
        }
        presenter = createPresenter();
        presenter.attachView((V) this);
        initViewData();
        initEvent();
        mContext = this;

        View waitingView = LayoutInflater.from(this).inflate(R.layout.waiting_dialog_layout, null);
        waitingDialog = new Dialog(this, R.style.EulixDialog);
        waitingDialog.setCancelable(false);
        waitingDialog.setContentView(waitingView);

        float minUpFlingDistance = (getResources().getDimensionPixelSize(R.dimen.dp_67) / 2.0f);

        lanDialogGestureDetector = new UpFlingGestureDetector(mContext, minUpFlingDistance, minUpFlingDistance
                , minUpFlingDistance, new UpFlingGestureDetector.DetectorListener() {
            @Override
            public void onSingleClick() {
                dismissLANDialog();
                startLanAccessImage();
            }

            @Override
            public void onScroll(float dx, float dy, float vx, float vy) {
                dismissLANDialog();
            }
        });
        lanDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_common_top_notification_dialog, null);
        ImageView lanImage = lanDialogView.findViewById(R.id.top_notification_image);
        TextView lanText = lanDialogView.findViewById(R.id.top_notification_text);
        lanImage.setImageResource(R.drawable.icon_transfer_network);
        lanText.setText(R.string.lan_access_hint);
        lanDialog = new Dialog(this, R.style.EulixLANDialog);
        lanDialog.setCancelable(true);
        lanDialog.setContentView(lanDialogView);
        lanDialogView.setClickable(true);
        lanDialogView.setFocusable(true);
        lanDialogView.setLongClickable(true);
        lanDialogView.setOnTouchListener((v, event) -> (lanDialogGestureDetector != null && lanDialogGestureDetector.onTouchEvent(event)));

        View oneButtonDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        oneButtonDialogTitle = oneButtonDialogView.findViewById(R.id.dialog_title);
        oneButtonDialogContent = oneButtonDialogView.findViewById(R.id.dialog_content);
        oneButtonDialogConfirm = oneButtonDialogView.findViewById(R.id.dialog_confirm);
        oneButtonDialog = new Dialog(this, R.style.EulixDialog);
        oneButtonDialog.setCancelable(false);
        oneButtonDialog.setContentView(oneButtonDialogView);

        if (activityIndex >= 0) {
            strongPushDialogList = new ArrayList<>();
            topNotificationGestureDetector = new UpFlingGestureDetector(mContext, minUpFlingDistance
                    , minUpFlingDistance, minUpFlingDistance, new UpFlingGestureDetector.DetectorListener() {
                @Override
                public void onSingleClick() {
                    handleTopNotification(true);
                }

                @Override
                public void onScroll(float dx, float dy, float vx, float vy) {
                    handleTopNotification(false);
                }
            });
            topNotificationDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_common_top_notification_dialog, null);
            topNotificationContainer = topNotificationDialogView.findViewById(R.id.top_notification_container);
            topNotificationImage = topNotificationDialogView.findViewById(R.id.top_notification_image);
            topNotificationText = topNotificationDialogView.findViewById(R.id.top_notification_text);
            upgradeNotificationContainer = topNotificationDialogView.findViewById(R.id.upgrade_top_notification_container);
            topNotificationTwoTitle = topNotificationDialogView.findViewById(R.id.tv_top_notification_two_title);
            topNotificationTwoDesc = topNotificationDialogView.findViewById(R.id.tv_top_notification_two_desc);
            imgTowLinesNotification = topNotificationDialogView.findViewById(R.id.img_top_notification_two);
            topNotificationDialog = new Dialog(this, R.style.EulixLANDialog);
            topNotificationDialog.setCancelable(true);
            topNotificationDialog.setContentView(topNotificationDialogView);
            topNotificationContainer.setClickable(true);
            topNotificationContainer.setFocusable(true);
            topNotificationContainer.setLongClickable(true);
            topNotificationContainer.setOnTouchListener((v, event) -> (topNotificationGestureDetector != null && topNotificationGestureDetector.onTouchEvent(event)));
            upgradeNotificationContainer.setOnTouchListener((v, event) -> (topNotificationGestureDetector != null && topNotificationGestureDetector.onTouchEvent(event)));

            View logoutDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
            TextView logoutTitle = logoutDialogView.findViewById(R.id.dialog_title);
            TextView logoutContent = logoutDialogView.findViewById(R.id.dialog_content);
            Button logoutConfirm = logoutDialogView.findViewById(R.id.dialog_confirm);
            logoutTitle.setText(R.string.logout_title);
            logoutContent.setText(R.string.logout_content);
            logoutConfirm.setText(R.string.ok);
            logoutDialog = new Dialog(this, R.style.EulixDialog);
            logoutDialog.setCancelable(false);
            logoutDialog.setContentView(logoutDialogView);
            logoutConfirm.setOnClickListener(v -> handleLogoutOrMemberDelete(true, true));

            View memberDeleteDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
            TextView memberDeleteTitle = memberDeleteDialogView.findViewById(R.id.dialog_title);
            memberDeleteContent = memberDeleteDialogView.findViewById(R.id.dialog_content);
            Button memberDeleteConfirm = memberDeleteDialogView.findViewById(R.id.dialog_confirm);
            memberDeleteTitle.setText(R.string.member_delete_title);
            memberDeleteConfirm.setText(R.string.ok);
            memberDeleteDialog = new Dialog(this, R.style.EulixDialog);
            memberDeleteDialog.setCancelable(false);
            memberDeleteDialog.setContentView(memberDeleteDialogView);
            memberDeleteConfirm.setOnClickListener(v -> handleLogoutOrMemberDelete(false, true));
        }

        EventBusUtil.register(this);

        lastLanState = LanManager.getInstance().isLanEnable();
    }

    private void startLanAccessImage() {
        EulixImageActivity.startImage(mContext, null, FormatUtil.isChinese(this, false)
                ? "detail_lan_access_zh-rCN.png" : "detail_lan_access.png", true);
    }

    protected void initLocale() {
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(this);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        if (localeBean != null) {
            Locale locale = localeBean.parseLocale();
            Resources resources = getResources();
            if (resources != null && locale != null) {
                Configuration configuration = resources.getConfiguration();
                if (configuration != null) {
                    configuration.setLocale(locale);
                    resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                }
            }
        }
    }

    protected void startPollPush() {
        if (activityIndex >= 0) {
            weakPushLock = false;
            strongPushLock = false;
            pollPush();
        }
    }

    protected void stopPollPush() {
        strongPushLock = null;
        weakPushLock = null;
    }

    /**
     * 强制消失弱提醒（onStop使用）
     */
    private void forceDismissWeakPushDialog() {
        handleTopNotification(false);
    }

    /**
     * 强制消失强提醒（onStop使用）
     */
    private void forceDismissStrongPushDialog() {
        prepareDismissOneButtonDialog();
        dismissOneButtonDialog();
        handleLogoutOrMemberDelete(true, false);
        handleLogoutOrMemberDelete(false, false);
        if (strongPushDialogList != null) {
            for (Dialog dialog : strongPushDialogList) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        DataUtil.setActivityIndex(activityIndex);
        boolean isShow = interceptorShow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConstantField.BroadcastCommunication.EULIX_SPACE_LAN);
        startPollPush();
        if (isShow && applicationLockImmediately && permitApplicationLock()) {
            handleApplicationLock(false);
        }
        isStart = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        dismissLANDialog();
        super.onPause();
    }

    @Override
    protected void onStop() {
        isStart = false;
        stopPollPush();
        forceDismissWeakPushDialog();
        forceDismissStrongPushDialog();
        if (isRegisterBroadcast) {
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isRegisterBroadcast = false;
        }
        super.onStop();
        isVisible = false;
    }

    @Override
    protected void onDestroy() {
        mReceiver = null;
        super.onDestroy();
        EventBusUtil.unRegister(this);
        if (lanDialog != null) {
            lanDialog.dismiss();
        }
    }

    protected boolean interceptorShow() {
        return true;
    }

    protected void resetStatusBar() {

    }

    protected boolean isDialogStyle() {
        return false;
    }

    protected int getActivityIndex() {
        return 0;
    }

    protected void setActivityIndex(int activityIndex) {
        this.activityIndex = activityIndex;
    }

    public boolean isImmersion() {
        return isImmersion;
    }

    public boolean isStatusBarImmersion() {
        return isStatusBarImmersion;
    }

    public boolean isNavigationBarImmersion() {
        return isNavigationBarImmersion;
    }

    protected void handleNoBiometrics() {

    }

    public abstract void initView();

    public abstract void initData();

    public abstract void initViewData();

    public abstract void initEvent();

    @NotNull
    public abstract P createPresenter();

    public final void toast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public final void toast(int strRes) {
        Toast.makeText(this, strRes, Toast.LENGTH_SHORT).show();
    }

    public boolean permitApplicationLock() {
        return (activityIndex == SECURITY_SERIES_ACTIVITY_INDEX || activityIndex == DEVELOPER_SERIES_ACTIVITY_INDEX
                || (activityIndex >= 0 && activityIndex != ConstantField.ActivityIndex.EULIX_DEVICE_LIST_ACTIVITY_INDEX));
    }

    protected boolean isViewVisible(View view) {
        return (view != null && View.VISIBLE == view.getVisibility());
    }

    protected boolean isViewGone(View view) {
        return (view == null || View.GONE == view.getVisibility());
    }

    public void showLoading(String text) {
        if (TextUtils.isEmpty(text)) {
            text = getResources().getString(R.string.waiting);
        }
        if (mLoadingDialog == null) {
            mLoadingDialog = EulixDialogUtil.createLoadingDialog(this, text, false);
        } else {
            mLoadingDialog.setText(text);
        }
        mLoadingDialog.show();
    }

    public void closeLoading() {
        runOnUiThread(() -> {
                    if (mLoadingDialog != null) {
                        mLoadingDialog.dismiss();
                        mLoadingDialog = null;
                    }
                }
        );
    }

    public boolean isLoadingShowing() {
        return mLoadingDialog != null && mLoadingDialog.isShowing();
    }

    private void prepareShowOneButtonDialog(String title, String content, String buttonContent, View.OnClickListener clickListener) {
        if (oneButtonDialogTitle != null) {
            oneButtonDialogTitle.setText(StringUtil.nullToEmpty(title));
        }
        if (oneButtonDialogContent != null) {
            oneButtonDialogContent.setText(StringUtil.nullToEmpty(content));
        }
        if (oneButtonDialogConfirm != null) {
            oneButtonDialogConfirm.setText(StringUtil.nullToEmpty(buttonContent));
            oneButtonDialogConfirm.setOnClickListener(clickListener);
        }
    }

    public boolean showOneButtonDialog() {
        boolean isShow = false;
        if (oneButtonDialog != null && !oneButtonDialog.isShowing()) {
            isShow = true;
            oneButtonDialog.show();
            Window window = oneButtonDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        return isShow;
    }

    private void prepareDismissOneButtonDialog() {
        if (oneButtonDialogTitle != null) {
            oneButtonDialogTitle.setText("");
        }
        if (oneButtonDialogContent != null) {
            oneButtonDialogContent.setText("");
        }
        if (oneButtonDialogConfirm != null) {
            oneButtonDialogConfirm.setOnClickListener(null);
            oneButtonDialogConfirm.setText("");
        }
    }

    public boolean dismissOneButtonDialog() {
        boolean isDismiss = false;
        if (oneButtonDialog != null && oneButtonDialog.isShowing()) {
            isDismiss = true;
            oneButtonDialog.dismiss();
        }
        return isDismiss;
    }

    public void showLANDialog() {
        if (lanDialog != null && !lanDialog.isShowing()) {
            lanDialog.show();
            Window window = lanDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.TOP);
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.dp_67));
                window.setWindowAnimations(R.style.top_dialog_anim_style);
            }
            if (lanDialogView != null) {
                lanDialogView.postDelayed(this::dismissLANDialog, 5000);
            }
        }
    }

    public void dismissLANDialog() {
        if (lanDialog != null && lanDialog.isShowing()) {
            lanDialog.dismiss();
        }
    }

    public void showTopNotificationDialog(boolean showTwoLinesStyle) {
        showTopNotificationDialogWithIcon(-1, showTwoLinesStyle);
    }

    public void showTopNotificationDialogWithIcon(int iconResId, boolean showTwoLinesStyle) {
        if (showTwoLinesStyle) {
            //系统升级
            upgradeNotificationContainer.setVisibility(View.VISIBLE);
            topNotificationContainer.setVisibility(View.GONE);
            if (iconResId != -1) {
                imgTowLinesNotification.setImageResource(iconResId);
            } else {
                imgTowLinesNotification.setImageResource(R.drawable.icon_notification_upgrade);
            }
        } else {
            //默认提示
            upgradeNotificationContainer.setVisibility(View.GONE);
            topNotificationContainer.setVisibility(View.VISIBLE);
            if (iconResId != -1) {
                topNotificationImage.setImageResource(iconResId);
            } else {
                topNotificationImage.setImageResource(R.drawable.login_notification_icon_2x);
            }
        }
        if (topNotificationDialogView != null) {
            topNotificationDialogView.removeCallbacks(dismissTopNotificationRunnable);
            topNotificationDialogView.removeCallbacks(releaseWeakPushLockRunnable);
        }
        handleWeakPushLock(true);
        if (topNotificationDialog != null && !topNotificationDialog.isShowing()) {
            topNotificationDialog.show();
            Window window = topNotificationDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.TOP);
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setWindowAnimations(R.style.top_dialog_anim_style);
            }
        }
        if (topNotificationDialogView != null) {
            topNotificationDialogView.postDelayed(releaseWeakPushLockRunnable, 3000);
            topNotificationDialogView.postDelayed(dismissTopNotificationRunnable, 3000);
        }
    }

    public void dismissTopNotificationDialog() {
        if (topNotificationDialog != null && topNotificationDialog.isShowing()) {
            topNotificationDialog.dismiss();
        }
        handleWeakPushLock(false);
    }

    private void showLogoutDialog() {
        if (logoutDialog != null && !logoutDialog.isShowing()) {
            handleStrongPushLock(true);
            logoutDialog.show();
            Window window = logoutDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissLogoutDialog() {
        if (logoutDialog != null && logoutDialog.isShowing()) {
            logoutDialog.dismiss();
            handleStrongPushLock(false);
        }
    }

    private void showMemberDeleteDialog() {
        if (memberDeleteDialog != null) {
            if (!memberDeleteDialog.isShowing()) {
                handleStrongPushLock(true);
                memberDeleteDialog.show();
            }
            Window window = memberDeleteDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissMemberDeleteDialog() {
        if (memberDeleteDialog != null && memberDeleteDialog.isShowing()) {
            memberDeleteDialog.dismiss();
            handleStrongPushLock(false);
        }
    }

    private void handleTopNotification(boolean isSwitch) {
        if (topNotificationDialogView != null) {
            topNotificationDialogView.removeCallbacks(releaseWeakPushLockRunnable);
            topNotificationDialogView.removeCallbacks(dismissTopNotificationRunnable);
            Intent intent = null;
            if (isSwitch && activityIndex >= 0 && mWeakPushBean != null) {
                String type = mWeakPushBean.getType();
                if (type != null) {
                    switch (type) {
                        case ConstantField.PushType.LOGIN:
                            if (activityIndex != ConstantField.ActivityIndex.LOGIN_TERMINAL_ACTIVITY_INDEX) {
                                intent = new Intent(this, DeviceManageActivity.class);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            if (intent == null) {
                dismissTopNotificationDialog();
            } else {
                weakPushLock = null;
                dismissTopNotificationDialog();
                startActivity(intent);
            }
        }
    }

    private void handleLogoutOrMemberDelete(boolean isLogout) {
        if (isLogout) {
            dismissLogoutDialog();
        } else {
            dismissMemberDeleteDialog();
        }
    }

    private void handleLogoutOrMemberDelete(boolean isLogout, boolean isSwitch) {
        if (isSwitch) {
            handleStrongPush(true);
        }
        Intent intent = null;
        if (isSwitch && activityIndex >= 0 && activityIndex != ConstantField.ActivityIndex.EULIX_DEVICE_LIST_ACTIVITY_INDEX) {
            intent = new Intent(this, EulixDeviceListActivity.class);
        }
        if (intent == null) {
            handleLogoutOrMemberDelete(isLogout);
        } else {
            strongPushLock = null;
            handleLogoutOrMemberDelete(isLogout);
            startActivity(intent);
        }
    }

    private void handleStrongPushLock(boolean isLock) {
        if (strongPushLock != null) {
            strongPushLock = isLock;
            if (!isLock) {
                pollStrongPush();
            }
        }
    }

    private void handleWeakPushLock(boolean isLock) {
        if (weakPushLock != null) {
            weakPushLock = isLock;
            if (!isLock) {
                pollWeakPush();
            }
        }
    }

    private void pollStrongPush() {
        if (presenter != null && strongPushLock != null && !strongPushLock && activityIndex >= 0) {
            mStrongPushBean = presenter.pollPush(EulixPushManager.STRONG);
            if (mStrongPushBean != null) {
                String type = mStrongPushBean.getType();
                String boxUuid = mStrongPushBean.getBoxUuid();
                String boxBind = mStrongPushBean.getBoxBind();
                String data = mStrongPushBean.getRawData();
                int source = mStrongPushBean.getSource();
                if (type != null) {
                    switch (type) {
                        case ConstantField.PushType.NativeType.TRIAL_INVALID:
                            handleStrongPush(false);
                            prepareShowOneButtonDialog(getString(R.string.trial_invalid_title)
                                    , getString(R.string.trial_invalid_content_simple_v2)
                                    , getString(R.string.get_it), v -> {
                                        handleStrongPush(true);
                                        Intent intent = null;
                                        if (activityIndex >= 0 && activityIndex != ConstantField.ActivityIndex.EULIX_DEVICE_LIST_ACTIVITY_INDEX) {
                                            intent = new Intent(this, EulixDeviceListActivity.class);
                                        }
                                        if (intent == null) {
                                            prepareDismissOneButtonDialog();
                                            if (dismissOneButtonDialog()) {
                                                handleStrongPushLock(false);
                                            }
                                        } else {
                                            strongPushLock = null;
                                            prepareDismissOneButtonDialog();
                                            dismissOneButtonDialog();
                                            startActivity(intent);
                                        }
                                    });
                            if (showOneButtonDialog()) {
                                handleStrongPushLock(true);
                            }
                            break;
                        case ConstantField.PushType.LOGOUT:
                        case ConstantField.PushType.REVOKE:
                            if (SystemUtil.requestNotification(getApplicationContext(), false) && DataUtil.getBusinessMessageEnable(getApplicationContext())) {
                                handleStrongPush(false);
                                showLogoutDialog();
                            } else {
                                handleStrongPush(true);
                                pollStrongPush();
                            }
                            break;
                        case ConstantField.PushType.MEMBER_SELF_DELETE:
                            if (SystemUtil.requestNotification(getApplicationContext(), false) && DataUtil.getBusinessMessageEnable(getApplicationContext())) {
                                handleStrongPush(false);
                                if (memberDeleteContent != null) {
                                    memberDeleteContent.setText(R.string.member_self_delete_content);
                                }
                                showMemberDeleteDialog();
                            } else {
                                handleStrongPush(true);
                                pollStrongPush();
                            }
                            break;
                        case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                            //系统下载完成，弹框
                            Logger.d("zfy", "receive BOX_UPGRADE_PACKAGE_PULLED");
                            if (SystemUtil.requestNotification(getApplicationContext(), false) && DataUtil.getBusinessMessageEnable(getApplicationContext())) {
                                if (activityIndex == ConstantField.ActivityIndex.SYSTEM_UPDATE_ACTIVITY_INDEX || this instanceof SystemUpdateActivity || ConstantField.boxVersionCheckBody == null) {
                                    //当前页面为升级页面，不提示
                                    Logger.d("zfy", "current activity is " + ConstantField.ActivityIndex.SYSTEM_UPDATE_ACTIVITY_INDEX);
                                    handleStrongPush(true);
                                    pollStrongPush();
                                } else {
                                    handleStrongPush(false);
                                    handleStrongPushLock(true);
                                    String versionStr = getResources().getString(R.string.app_name);
                                    Logger.d("zfy", "data:" + data);
                                    if (!TextUtils.isEmpty(data)) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(data);
                                            versionStr = versionStr + " " + jsonObject.optString("version");
                                            //删除换行符
                                            versionStr.replaceAll("\n", "");
                                        } catch (Exception e) {
                                            Logger.d("zfy", "data is not json");
                                        }
                                    } else if (ConstantField.boxVersionCheckBody != null && ConstantField.boxVersionCheckBody.latestAppPkg != null
                                            && !TextUtils.isEmpty(ConstantField.boxVersionCheckBody.latestAppPkg.pkgVersion)) {
                                        versionStr = versionStr + " " + ConstantField.boxVersionCheckBody.latestAppPkg.pkgVersion;
                                    }
                                    SystemUpgradeNoticeDialog systemUpgradeNoticeDialog = EulixDialogUtil.showSystemUpgradeNoticeDialog(this, versionStr, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            handleStrongPush(true);
                                            handleStrongPushLock(false);
                                        }
                                    });
                                    if (strongPushDialogList != null) {
                                        strongPushDialogList.add(systemUpgradeNoticeDialog);
                                    }
                                }
                            } else {
                                handleStrongPush(true);
                                pollStrongPush();
                            }
                            break;
                        case ConstantField.PushType.LOGIN_CONFIRM:
                            handleLoginConfirmPush(boxUuid, boxBind, data, source);
                            break;
                        case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                        case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                            handleSecurityPasswordApplyPush(boxUuid, boxBind, data, source, type);
                            break;
                        default:
                            handleStrongPush(true);
                            pollStrongPush();
                            break;
                    }
                }
            }
        }
    }

    private void handleLoginConfirmPush(String boxUuid, String boxBind, String data, int source) {
        handleStrongPush(true);
        String aoId = null;
        String terminalType = null;
        String terminalMode = null;
        String loginClientUuid = null;
        if (data != null) {
            LoginConfirmBean loginConfirmBean = null;
            try {
                loginConfirmBean = new Gson().fromJson(data, LoginConfirmBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (loginConfirmBean != null) {
                loginClientUuid = loginConfirmBean.getUuid();
                switch (source) {
                    case 1:
                    case 2:
                    case 3:
                        aoId = loginConfirmBean.getAoid();
                        terminalType = loginConfirmBean.getTerminalType();
                        terminalMode = loginConfirmBean.getTerminalMode();
                        break;
                    default:
                        break;
                }
            }
        }
        if (boxUuid == null || boxBind == null || loginClientUuid == null || aoId == null) {
            pollStrongPush();
        } else {
            strongPushLock = null;
            if (presenter != null) {
                presenter.setGranterAuthorization(boxUuid, boxBind, loginClientUuid);
                presenter.customLoginConfirmMessage(boxUuid, boxBind, loginClientUuid);
            }
            Intent intent = new Intent(this, GranterAuthorizationActivity.class);
            intent.putExtra(ConstantField.BOX_UUID, boxUuid);
            intent.putExtra(ConstantField.BOX_BIND, boxBind);
            intent.putExtra(ConstantField.CLIENT_UUID, loginClientUuid);
            intent.putExtra(ConstantField.AO_ID, aoId);
            if (terminalType != null) {
                intent.putExtra(ConstantField.TERMINAL_TYPE, terminalType);
            }
            if (terminalMode != null) {
                intent.putExtra(ConstantField.TERMINAL_MODE, terminalMode);
            }
            startActivity(intent);
        }
    }

    private void handleSecurityPasswordApplyPush(String boxUuid, String boxBind, String data, int source, @NonNull String messageType) {
        handleStrongPush(true);
        String terminalMode = null;
        String authClientUuid = null;
        String applyId = null;
        SecurityTokenResult securityTokenResult = null;
        if (data != null) {
            SecurityApplyBean securityApplyBean = null;
            try {
                securityApplyBean = new Gson().fromJson(data, SecurityApplyBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (securityApplyBean != null) {
                authClientUuid = securityApplyBean.getAuthClientUUid();
                switch (source) {
                    case 1:
                    case 2:
                    case 3:
                        terminalMode = securityApplyBean.getAuthDeviceInfo();
                        securityTokenResult = securityApplyBean.getSecurityTokenRes();
                        applyId = securityApplyBean.getApplyId();
                        break;
                    default:
                        break;
                }
            }
        }
        if (boxUuid == null || boxBind == null || authClientUuid == null || securityTokenResult == null) {
            pollStrongPush();
        } else {
            long securityTokenExpireTimestamp = -1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                securityTokenExpireTimestamp = FormatUtil.parseZonedDateTime(securityTokenResult.getExpiredAt());
            } else {
                securityTokenExpireTimestamp = FormatUtil.parseZonedTimestamp(securityTokenResult.getExpiredAt()
                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
            }
            if (securityTokenExpireTimestamp < 0 || securityTokenExpireTimestamp > System.currentTimeMillis()) {
                strongPushLock = null;
                if (presenter != null) {
                    presenter.setGranterSecurityAuthentication(boxUuid, boxBind, authClientUuid, messageType, applyId, securityTokenResult);
                    presenter.customSecurityAuthenticationMessage(boxUuid, boxBind, authClientUuid, messageType);
                }
                Intent intent = new Intent(this, GranterSecurityPasswordAuthenticationActivity.class);
                intent.putExtra(ConstantField.BOX_UUID, boxUuid);
                intent.putExtra(ConstantField.BOX_BIND, boxBind);
                intent.putExtra(ConstantField.MESSAGE_TYPE, messageType);
                intent.putExtra(ConstantField.CLIENT_UUID, authClientUuid);
                if (terminalMode != null) {
                    intent.putExtra(ConstantField.TERMINAL_MODE, terminalMode);
                }
                startActivity(intent);
            } else {
                pollStrongPush();
            }
        }
    }

    private void pollWeakPush() {
        if (presenter != null && weakPushLock != null && !weakPushLock && activityIndex >= 0) {
            mWeakPushBean = presenter.pollPush(EulixPushManager.WEAK);
            if (mWeakPushBean != null) {
                String type = mWeakPushBean.getType();
                String data = mWeakPushBean.getRawData();
                int source = mWeakPushBean.getSource();
                boolean isShow = true;
                if (type != null) {
                    switch (type) {
                        case ConstantField.PushType.LOGIN:
                            String terminalMode = "";
                            if (data != null) {
                                switch (source) {
                                    case 1:
                                        isShow = false;
                                        break;
                                    case 2:
                                        LoginBean loginBean = null;
                                        try {
                                            loginBean = new Gson().fromJson(data, LoginBean.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (loginBean != null) {
                                            terminalMode = StringUtil.nullToEmpty(loginBean.getTerminalMode());
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            if (topNotificationImage != null) {
                                topNotificationImage.setImageResource(R.drawable.login_notification_icon_2x);
                            }
                            if (topNotificationText != null) {
                                StringBuilder loginTextBuilder = new StringBuilder();
                                loginTextBuilder.append(getString(R.string.login_notification_part_1));
                                if (!TextUtils.isEmpty(terminalMode)) {
                                    loginTextBuilder.append(" ");
                                    loginTextBuilder.append(terminalMode);
                                    loginTextBuilder.append(" ");
                                }
                                loginTextBuilder.append(getString(R.string.login_notification_part_2));
                                topNotificationText.setText(loginTextBuilder.toString());
                            }
                            handleWeakPush(true);
                            if (isShow && SystemUtil.requestNotification(getApplicationContext(), false) && DataUtil.getBusinessMessageEnable(getApplicationContext())) {
                                showTopNotificationDialog(false);
                            } else {
                                pollWeakPush();
                            }
                            break;
                        case ConstantField.PushType.BOX_START_UPGRADE:
                            Logger.d("zfy", "receive BOX_START_UPGRADE");
                            if (topNotificationTwoTitle != null) {
                                topNotificationTwoTitle.setText(getString(R.string.box_start_upgarde_title));
                            }
                            if (topNotificationTwoDesc != null) {
                                topNotificationTwoDesc.setText(getString(R.string.box_start_upgarde_desc));
                            }
                            handleWeakPush(true);
                            if (SystemUtil.requestNotification(getApplicationContext(), false) && DataUtil.getBusinessMessageEnable(getApplicationContext())) {
                                // 展示开始安装提示
                                showTopNotificationDialog(true);
                            } else {
                                pollWeakPush();
                            }
                            break;
                        default:
                            handleWeakPush(true);
                            pollWeakPush();
                            break;
                    }
                }
            }
        }

    }

    private void pollPush() {
        if (strongPushLock != null && weakPushLock != null) {
            pollWeakPush();
            pollStrongPush();
        }
    }

    private void handleStrongPush(boolean isCustom) {
        if (mStrongPushBean != null && presenter != null) {
            presenter.handlePush(mStrongPushBean.getMessageId(), mStrongPushBean.getBoxUuid(), mStrongPushBean.getBoxBind(), isCustom);
        }
    }

    private void handleWeakPush(boolean isCustom) {
        if (mWeakPushBean != null && presenter != null) {
            presenter.handlePush(mWeakPushBean.getMessageId(), mWeakPushBean.getBoxUuid(), mWeakPushBean.getBoxBind(), isCustom);
        }
    }

    public void showServerExceptionToast() {
        showImageTextToast(R.drawable.toast_refuse, R.string.service_exception_hint);
    }

    public void showDefaultPureTextToast(@StringRes int resId) {
        if (toastManager == null) {
            toastManager = new ToastManager(this);
        }
        toastManager.showDefaultPureTextToast(resId);
    }

    public void showDefaultPureTextToast(String content) {
        if (toastManager == null) {
            toastManager = new ToastManager(this);
        }
        toastManager.showDefaultPureTextToast(content);
    }

    public void showPureTextToast(@StringRes int resId) {
        if (toastManager == null) {
            toastManager = new ToastManager(this);
        }
        toastManager.showPureTextToast(resId);
    }

    public void showPureTextToast(String content) {
        if (toastManager == null) {
            toastManager = new ToastManager(this);
        }
        toastManager.showPureTextToast(content);
    }

    public void showImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId) {
        if (toastManager == null) {
            toastManager = new ToastManager(EulixSpaceApplication.getContext());
        }
        toastManager.showImageTextToast(drawableResId, stringResId);
    }

    public void showImageTextToast(@DrawableRes int drawableResId, String content) {
        if (toastManager == null) {
            toastManager = new ToastManager(this);
        }
        toastManager.showImageTextToast(drawableResId, content);
    }

    protected Boolean getStrongPushLock() {
        return strongPushLock;
    }

    protected EulixBiometricManager.PromptInfoBean rapidGeneratePromptInfoBean() {
        EulixBiometricManager.PromptInfoBean promptInfoBean = null;
        int status = 0;
        if (presenter != null) {
            status = presenter.getBiometricFeature();
        }
        if (status > 0) {
            String title = "";
            String negativeButtonText = getString(R.string.cancel);
            String subtitle = null;
            String description = null;
            switch (status) {
                case 1:
                    title = getString(R.string.fingerprint_authenticate_title);
                    subtitle = getString(R.string.fingerprint_authenticate_subtitle);
                    description = "";
                    break;
                case 2:
                    title = getString(R.string.face_authenticate_title);
                    break;
                default:
                    title = getString(R.string.biometric_authenticate_title);
                    break;
            }
            promptInfoBean = new EulixBiometricManager.PromptInfoBean(title, negativeButtonText);
            promptInfoBean.setSubtitle(subtitle);
            promptInfoBean.setDescription(description);
        }
        return promptInfoBean;
    }

    protected void handleApplicationLock(boolean isDisposable) {
        EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(getApplicationContext());
        if (eulixSpaceInfo != null) {
            handleApplicationLock(eulixSpaceInfo.getBoxUuid(), eulixSpaceInfo.getBoxBind(), isDisposable);
        }
    }

    protected void handleApplicationLock(String boxUuid, String boxBind, boolean isDisposable) {
        ApplicationLockEventInfo applicationLockEventInfo = DataUtil.getApplicationLockEventInfo(boxUuid, boxBind);
        if (applicationLockEventInfo != null) {
            Logger.d("onAuth", "handle auth: " + applicationLockEventInfo.toString());
            if ((applicationLockEventInfo.isFingerprintUnlock() || applicationLockEventInfo.isFaceUnlock())) {
                EulixBiometricManager.PromptInfoBean promptInfoBean = rapidGeneratePromptInfoBean();
                if (promptInfoBean != null) {
                    String requestId = applicationLockEventInfo.getRequestId();
                    if (!isDisposable) {
                        showLoading("");
                    }
                    DataUtil.setApplicationLockEventInfoError(requestId, false);
                    if (!presenter.authenticate(requestId, this, promptInfoBean, isDisposable)) {
                        closeLoading();
                        DataUtil.resetApplicationLockEventInfo();
                    }
                }
            } else {
                DataUtil.resetApplicationLockEventInfo();
            }
        }
    }

    @Override
    public void authenticateResult(boolean isSuccess, String responseId, String requestId) {
        if ((requestId == null && responseId == null) || (responseId != null && responseId.equals(requestId))) {
            if (isSuccess) {
                closeLoading();
            }
        }
    }

    @Override
    public void authenticateError(int code, CharSequence errMsg, String responseId, String requestId) {
        if ((requestId == null && responseId == null) || (responseId != null && responseId.equals(requestId))) {
            if (errMsg != null) {
                String message = errMsg.toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    runOnUiThread(() -> showPureTextToast(message));
                }
            }
            if (code == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                runOnUiThread(this::handleNoBiometricsError);
            } else if (code == BiometricPrompt.ERROR_CANCELED || code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                runOnUiThread(() -> handleCanceledError(responseId));
            } else {
                runOnUiThread(this::handleAuthenticateError);
            }
            closeLoading();
        }
    }

    private void handleNoBiometricsError() {
        DataUtil.resetApplicationLockEventInfo();
        if (presenter != null) {
            int biometricFeature = presenter.getBiometricFeature();
            if (biometricFeature > 0) {
                switch (biometricFeature) {
                    case 1:
                        presenter.setFingerprintLockInfo(false);
                        break;
                    case 2:
                        presenter.setFaceLockInfo(false);
                        break;
                    default:
                        presenter.setApplicationLockInfo(false, false);
                        break;
                }
            }
        }
        handleNoBiometrics();
    }

    protected boolean handlePositiveCancelError(String responseId) {
        boolean result = DataUtil.containsCancelAuthentication(responseId);
        if (result) {
            DataUtil.removeCancelAuthentication(responseId);
        }
        return result;
    }

    private void handleCanceledError(String responseId) {
        if (!handlePositiveCancelError(responseId)) {
            handleAuthenticateError();
        }
    }

    private void handleAuthenticateError() {
        if (activityIndex != ConstantField.ActivityIndex.EULIX_DEVICE_LIST_ACTIVITY_INDEX) {
            Intent intent = new Intent(this, EulixDeviceListActivity.class);
            startActivity(intent);
        }
    }

    protected class AbsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case ConstantField.BroadcastCommunication.EULIX_SPACE_LAN:
                            if (intent.hasExtra(ConstantField.BOX_UUID) && intent.hasExtra(ConstantField.BroadcastCommunication.LAN_ENABLE)) {
                                String boxUuid = intent.getStringExtra(ConstantField.BOX_UUID);
                                boolean isLanEnable = intent.getBooleanExtra(ConstantField.BroadcastCommunication.LAN_ENABLE, false);
                                String currentBoxUuid = EulixSpaceDBUtil.queryAvailableBoxUuid(getApplicationContext());
                                if (boxUuid != null && (currentBoxUuid == null || boxUuid.equals(currentBoxUuid))) {
                                    if (isLanEnable) {
                                        showLANDialog();
                                    } else {
                                        dismissLANDialog();
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LanStatusEvent event) {
        Logger.d("zfy", "receive LanStatusEvent " + event.isLanEnable);
        if (event.isLanEnable) {
            if (isVisible && !lastLanState) {
                showLANDialog();
            }
            lastLanState = true;
        } else {
            dismissLANDialog();
            lastLanState = false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EulixPushEvent event) {
        if (event != null && activityIndex >= 0) {
            int priority = 32;
            PushBean pushBean = event.getPushBean();
            if (pushBean != null) {
                priority = pushBean.getPriority();
            }
            if (priority > 0 && priority < 9) {
                pollStrongPush();
            } else if (priority > 8 && priority < 17) {
                pollWeakPush();
            } else {
                pollPush();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ApplicationLockEvent event) {
        if (event != null && isStart && permitApplicationLock()) {
            handleApplicationLock(event.getBoxUuid(), event.getBoxBind(), false);
        }
    }
}
