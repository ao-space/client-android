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

package xyz.eulix.space;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.GuideViewPagerAdapter;
import xyz.eulix.space.event.AppInstallEvent;
import xyz.eulix.space.event.AppUpdateEvent;
import xyz.eulix.space.network.gateway.VersionCompatibleResponseBody;
import xyz.eulix.space.presenter.LaunchPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.ui.mine.SystemUpdateActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.ImportantThreadPool;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.widgets.PointsIndicatorView;

/**
 * Author: 		Zhufy
 * Description: 启动引导页
 * History:		2021/7/13
 */
public class EulixSpaceLaunchActivity extends AbsActivity<LaunchPresenter.ILauncher, LaunchPresenter> implements LaunchPresenter.ILauncher {
    private static final String TAG = EulixSpaceLaunchActivity.class.getSimpleName();
    private boolean isHandleGuideView = false;
    private boolean isInitPermission = false;
    private boolean isInitManageExternalStorage = false;
    private boolean isPrepareFinish = false;
    private boolean isExternalStorageEnable = true;
    private ViewPager mViewPager;
    private PointsIndicatorView mIndicator;
    private RelativeLayout layoutLaunch;
    private RelativeLayout layoutGuide;

    private Button updateNow;
    private ImageView versionUpdateImage;
    private TextView versionUpdateContent;
    private View versionUpdateDialogView;
    private Dialog versionUpdateDialog;

    private Button repairCancel;
    private Button repairNow;
    private TextView repairAdminTitle;
    private TextView repairAdminContent;
    private View repairAdminDialogView;
    private Dialog repairAdminDialog;

    private Button repairIKnow;
    private TextView repairMemberTitle;
    private TextView repairMemberContent;
    private View repairMemberDialogView;
    private Dialog repairMemberDialog;

    private String apkDownloadPath;
    private boolean isMemberAccept;
    private Uri acceptMemberData;
    private String pushType;

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_space_launch);
        layoutLaunch = findViewById(R.id.layout_lunch);
        layoutGuide = findViewById(R.id.layout_guide);

        versionUpdateDialogView = LayoutInflater.from(this).inflate(R.layout.version_force_update_dialog, null);
        versionUpdateImage = versionUpdateDialogView.findViewById(R.id.version_update_image);
        versionUpdateContent = versionUpdateDialogView.findViewById(R.id.version_update_content);
        updateNow = versionUpdateDialogView.findViewById(R.id.update);
        versionUpdateDialog = new Dialog(this, R.style.EulixDialog);
        versionUpdateDialog.setCancelable(false);
        versionUpdateDialog.setContentView(versionUpdateDialogView);

        repairAdminDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        repairAdminTitle = repairAdminDialogView.findViewById(R.id.dialog_title);
        repairAdminContent = repairAdminDialogView.findViewById(R.id.dialog_content);
        repairCancel = repairAdminDialogView.findViewById(R.id.dialog_cancel);
        repairNow = repairAdminDialogView.findViewById(R.id.dialog_confirm);
        repairAdminDialog = new Dialog(this, R.style.EulixDialog);
        repairAdminDialog.setCancelable(false);
        repairAdminDialog.setContentView(repairAdminDialogView);

        repairMemberDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        repairMemberTitle = repairMemberDialogView.findViewById(R.id.dialog_title);
        repairMemberContent = repairMemberDialogView.findViewById(R.id.dialog_content);
        repairIKnow = repairMemberDialogView.findViewById(R.id.dialog_confirm);
        repairMemberDialog = new Dialog(this, R.style.EulixDialog);
        repairMemberDialog.setCancelable(false);
        repairMemberDialog.setContentView(repairMemberDialogView);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        isHandleGuideView = false;
        isPrepareFinish = false;
        acceptMemberData = null;
        Intent intent = getIntent();
        if (intent != null) {
            isMemberAccept = intent.getBooleanExtra("accept", false);
            if (isMemberAccept) {
                acceptMemberData = intent.getData();
            } else if (intent.hasExtra(ConstantField.PushExtraKey.OPT_TYPE)) {
                pushType = intent.getStringExtra(ConstantField.PushExtraKey.OPT_TYPE);
            }
        }
        DataUtil.setStartLauncher(true);
    }

    @Override
    public void initViewData() {
        showPrivacyAgreement();
        repairAdminTitle.setText(R.string.program_incompatibility);
        repairAdminContent.setText(R.string.program_incompatibility_content_admin);
        repairNow.setText(R.string.repair_now);

        repairMemberTitle.setText(R.string.program_incompatibility);
        repairMemberContent.setText(R.string.program_incompatibility_content_member);
        repairIKnow.setText(R.string.i_know);
    }

    @Override
    public void initEvent() {
        updateNow.setOnClickListener(v -> {
            updateNow.setClickable(false);
            updateNow.setTextColor(getResources().getColor(R.color.gray_ff85899c));
            updateApp();
        });
        repairCancel.setOnClickListener(v -> {
            dismissRepairAdminDialog();
            EulixSpaceApplication.popAllOldActivity(null);
        });
        repairNow.setOnClickListener(v -> {
            dismissRepairAdminDialog();
            Intent intent = new Intent(EulixSpaceLaunchActivity.this, SystemUpdateActivity.class);
            intent.putExtra("isFromLaunch", true);
            startActivity(intent);
            EulixSpaceApplication.popAllOldActivity(null);
        });
        repairIKnow.setOnClickListener(v -> {
            dismissRepairMemberDialog();
            EulixSpaceApplication.popAllOldActivity(null);
        });
    }

    //展示隐私协议对话框
    private void showPrivacyAgreement(){
        if (!PreferenceUtil.getPrivacyAgreed(this)){
            EulixDialogUtil.showPrivacyAgreementDialog(this, (dialog, which) -> {
                checkPermission();
                PreferenceUtil.savePrivacyAgreed(getApplicationContext(), true);
            }, (dialog, which) -> EulixSpaceApplication.popAllOldActivity(null));
        } else {
            checkPermission();
        }

    }

    @Override
    protected int getActivityIndex() {
        return EULIX_SPACE_LAUNCH_ACTIVITY_INDEX;
    }

    @Override
    public LaunchPresenter createPresenter() {
        return new LaunchPresenter();
    }

    protected void checkPermission() {
        if (!isPrepareFinish) {
            List<String> permissionList = new ArrayList<>();
            permissionList.add(ConstantField.Permission.CAMERA);
            permissionList.add(ConstantField.Permission.ACCESS_COARSE_LOCATION);
            permissionList.add(ConstantField.Permission.ACCESS_FINE_LOCATION);
            permissionList.add(ConstantField.Permission.READ_EXTERNAL_STORAGE);
            permissionList.add(ConstantField.Permission.WRITE_EXTERNAL_STORAGE);

            permissionList.add(ConstantField.Permission.INTERNET);
            permissionList.add(ConstantField.Permission.ACCESS_NETWORK_STATE);
            permissionList.add(ConstantField.Permission.BLUETOOTH);
            permissionList.add(ConstantField.Permission.BLUETOOTH_ADMIN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionList.add(ConstantField.Permission_23.ACCESS_NOTIFICATION_POLICY);
                permissionList.add(ConstantField.Permission_23.REQUEST_INSTALL_PACKAGES);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permissionList.add(ConstantField.Permission_28.FOREGROUND_SERVICE);
                permissionList.add(ConstantField.Permission_28.USE_BIOMETRIC);
            }
            if (SystemUtil.requestPermission(this, permissionList.toArray(new String[0])
                    , ConstantField.RequestCode.ALL_PERMISSION) && manageExternalStoragePermission()) {
                isExternalStorageEnable = true;
                isInitPermission = true;
                isInitManageExternalStorage = true;
                handleLaunch();
            }
        }
    }

    private boolean manageExternalStoragePermission() {
        boolean isPermit = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                isPermit = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ConstantField.RequestCode.MANAGE_EXTERNAL_STORAGE_PERMISSION);
            }
        }
        return isPermit;
    }

    private void handleLaunch() {
        if (isInitPermission && isInitManageExternalStorage && !isHandleGuideView) {
            isHandleGuideView = true;
            layoutLaunch.setVisibility(View.VISIBLE);
            presenter.checkUpdate();
        }
    }

    private void initGuideViews() {
        layoutLaunch.setVisibility(View.GONE);
        layoutGuide.setVisibility(View.VISIBLE);
        mViewPager = findViewById(R.id.guide_view_pager);
        mIndicator = findViewById(R.id.points_indicator);

        //实例化各个界面的布局对象
        View view1 = View.inflate(this, R.layout.guide_view_layout, null);
        View view2 = View.inflate(this, R.layout.guide_view_layout, null);
        View view3 = View.inflate(this, R.layout.guide_view_layout, null);

        ((ImageView) view1.findViewById(R.id.img_guide)).setImageResource(R.drawable.guide_page_1_2x);
        ((ImageView) view2.findViewById(R.id.img_guide)).setImageResource(R.drawable.guide_page_2_2x);
        ((ImageView) view3.findViewById(R.id.img_guide)).setImageResource(R.drawable.guide_page_3_2x);
        TextView btnStart = view3.findViewById(R.id.btn_guide_start);
        btnStart.setVisibility(View.VISIBLE);
        btnStart.setOnClickListener(v -> {
            jumpActivity();
        });

        ArrayList<View> views = new ArrayList<>();
        views.add(view1);
        views.add(view2);
        views.add(view3);

        GuideViewPagerAdapter vpAdapter = new GuideViewPagerAdapter(views);
        mViewPager.setAdapter(vpAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            boolean isScrolled ;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Do nothing
            }

            @Override
            public void onPageSelected(int position) {
                mIndicator.setPointPosition(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        isScrolled = false;
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        isScrolled = true;
                        break;
                    case ViewPager.SCROLL_STATE_IDLE:
                        if (mViewPager.getCurrentItem() == mViewPager.getAdapter().getCount() - 1 && !isScrolled) {
                            //滑到最后一个页面之后的继续滑动操作
//                            jumpActivity();
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        mIndicator.addPoints(views.size());
    }

    private void jumpActivity(){
        if (isMemberAccept) {
            isMemberAccept = false;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(acceptMemberData);
            startActivity(intent);
        } else {
            goMain();
        }
    }
    //跳转主页
    private void goMain() {
        Intent intent = new Intent(EulixSpaceLaunchActivity.this, EulixMainActivity.class);
        if (pushType != null) {
            intent.putExtra(ConstantField.PushExtraKey.OPT_TYPE, pushType);
        }
        startActivity(intent);
        finish();
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

    private void showVersionCheckDialog() {
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

    private void dismissVersionCheckDialog() {
        if (versionUpdateDialog != null && versionUpdateDialog.isShowing()) {
            versionUpdateImage.setImageDrawable(null);
            versionUpdateDialog.dismiss();
        }
    }

    private void showRepairAdminDialog() {
        if (repairAdminDialog != null && !repairAdminDialog.isShowing()) {
            repairAdminDialog.show();
            Window window = repairAdminDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissRepairAdminDialog() {
        if (repairAdminDialog != null && repairAdminDialog.isShowing()) {
            repairAdminDialog.dismiss();
        }
    }

    private void showRepairMemberDialog() {
        if (repairMemberDialog != null && !repairMemberDialog.isShowing()) {
            repairMemberDialog.show();
            Window window = repairMemberDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissRepairMemberDialog() {
        if (repairMemberDialog != null && repairMemberDialog.isShowing()) {
            repairMemberDialog.dismiss();
        }
    }

    private void updateApp() {
        if (presenter != null) {
            String downloadUrl = presenter.getDownloadUrl();
            if (downloadUrl == null || TextUtils.isEmpty(downloadUrl)) {
                showPureTextToast(R.string.download_failed);
                updateNow.setTextColor(getResources().getColor(R.color.blue_ff337aff));
                updateNow.setClickable(true);
            } else {
                AppUpdateEvent appUpdateEvent = new AppUpdateEvent(presenter.getApkSize(), presenter.getDownloadUrl()
                        , presenter.getMd5(), presenter.getNewestVersion(), true);
                showPureTextToast(R.string.download_newest_version);
                EventBusUtil.post(appUpdateEvent);
            }
        }
    }

    private void installApp(String filePath) {
        apkDownloadPath = filePath;
        if (presenter != null) {
            presenter.setApkDownloadPath(filePath);
        }
        if (SystemUtil.requestInstallPackages(this, true)) {
            SystemUtil.installPackage(this, filePath);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ConstantField.RequestCode.ALL_PERMISSION) {
            boolean result = true;
            int length = Math.min(permissions.length, grantResults.length);
            List<String> denyPermissions = new ArrayList<>();
            for (int i = 0; i < length ; i++) {
                String permission = permissions[i];
                boolean isGrant = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                if (permission != null) {
                    PreferenceUtil.saveBaseKeyBoolean(EulixSpaceApplication.getContext(), permission, isGrant);
                }
                if (permission != null && !isGrant) {
                    switch (permission) {
                        case ConstantField.Permission.READ_EXTERNAL_STORAGE:
                        case ConstantField.Permission.WRITE_EXTERNAL_STORAGE:
                            isExternalStorageEnable = false;
//                            if (DataUtil.getClientUuid(getApplicationContext()) == null) {
//                                result = false;
//                                denyPermissions.add(permission);
//                            }
                            break;
                        case ConstantField.Permission.INTERNET:
                        case ConstantField.Permission.ACCESS_NETWORK_STATE:
                            break;
                        case ConstantField.Permission.CAMERA:
                        case ConstantField.Permission.ACCESS_COARSE_LOCATION:
                        case ConstantField.Permission.ACCESS_FINE_LOCATION:
                        case ConstantField.Permission.BLUETOOTH:
                        case ConstantField.Permission.BLUETOOTH_ADMIN:
                            break;
                        default:
                            break;
                    }
                }
            }
            if (result) {
                isInitPermission = true;
                if (manageExternalStoragePermission()) {
                    isExternalStorageEnable = true;
                    isInitManageExternalStorage = true;
                    handleLaunch();
                }
            } else {
                isPrepareFinish = true;
                boolean isRationale = false;
                for (String denyPermission : denyPermissions) {
                    if (denyPermission != null && !ActivityCompat.shouldShowRequestPermissionRationale(this, denyPermission)) {
                        isRationale = true;
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.MANAGE_EXTERNAL_STORAGE_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        isExternalStorageEnable = true;
                    } else {
                        isExternalStorageEnable = false;
                    }
                }
                isInitManageExternalStorage = true;
                handleLaunch();
                break;
            case ConstantField.RequestCode.REQUEST_INSTALL_PACKAGES:
                if (SystemUtil.requestInstallPackages(this, false)) {
                    if (apkDownloadPath == null && presenter != null) {
                        apkDownloadPath = presenter.getApkDownloadPath();
                    }
                    SystemUtil.installPackage(this, apkDownloadPath);
                }
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void fileInitialized() {
        new Handler(Looper.getMainLooper()).post(() -> {
            boolean hasShowedGuide = PreferenceUtil.getGuideShowed(EulixSpaceLaunchActivity.this);
            if (hasShowedGuide) {
                jumpActivity();
            } else {
                initGuideViews();
                PreferenceUtil.saveGuideShowed(getApplication(), true);
            }
        });
    }

    @Override
    public void eulixSpaceInitialized(Integer result) {
        // Do nothing
    }

    @Override
    public void versionCheckResultCallback(boolean isAppForceUpdate, boolean isBoxForceUpdate, VersionCompatibleResponseBody.Results.LatestPkg latestAppPkg, VersionCompatibleResponseBody.Results.LatestPkg latestBoxPkg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            boolean isUpdate = false;
            if (isAppForceUpdate) {
                isUpdate = true;
                prepareAppUpdateShow();
                showVersionCheckDialog();
            } else if (isBoxForceUpdate && presenter != null) {
                Boolean isAdmin = presenter.isAdmin();
                if (isAdmin != null) {
                    isUpdate = true;
                    if (isAdmin) {
                        showRepairAdminDialog();
                    } else {
                        showRepairMemberDialog();
                    }
                }
            }
            if (!isUpdate && presenter != null) {
                ImportantThreadPool.getInstance().execute(() -> {
                    presenter.initFile(isExternalStorageEnable);
                });
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppInstallEvent event) {
        if (event != null && event.isForce()) {
            String filePath = event.getFilePath();
            if (filePath != null && event.isSuccess()) {
                dismissVersionCheckDialog();
                installApp(filePath);
            } else {
                showPureTextToast(R.string.download_failed);
                updateNow.setTextColor(getResources().getColor(R.color.blue_ff337aff));
                updateNow.setClickable(true);
            }
        }
    }
}