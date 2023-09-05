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

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.EulixDeviceListActivity;
import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bridge.FileSearchBridge;
import xyz.eulix.space.callback.EulixSpaceCallback;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.AppCheckRequestEvent;
import xyz.eulix.space.event.AppInstallEvent;
import xyz.eulix.space.event.AppUpdateEvent;
import xyz.eulix.space.event.BoxNetworkRequestEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.DiskManagementListRequestEvent;
import xyz.eulix.space.fragment.main.TabFileFragment;
import xyz.eulix.space.fragment.main.TabMineFragment;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.manager.AlreadyUploadedManager;
import xyz.eulix.space.presenter.MainPresenter;
import xyz.eulix.space.ui.authorization.GranterLoginActivity;
import xyz.eulix.space.ui.mine.AboutUsActivity;
import xyz.eulix.space.ui.mine.DeviceManageActivity;
import xyz.eulix.space.ui.mine.SystemUpdateActivity;
import xyz.eulix.space.ui.mine.security.EulixAuthenticationActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PermissionUtils;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.TabImageView;
import xyz.eulix.space.view.TabImageViewHolder;
import xyz.eulix.space.view.dialog.folder.FolderListView;

/**
 * Author:      Zhu Fuyu
 * Description: 主页
 * History:     2021/7/16
 */
public class EulixMainActivity extends AbsActivity<MainPresenter.IMain, MainPresenter> implements MainPresenter.IMain
        , EulixSpaceCallback, FolderListView.FolderListCallback, FolderListView.FolderNewCallback, FileSearchBridge.FileSearchSourceCallback {
    public static final String TAG = EulixMainActivity.class.getSimpleName();
    private static final String SPLIT_1 = "/";
    private static final long WEEK_UNIT = 604800000L;
    private static final int MINUTE_UNIT = 60000;
    private static final int BIND_EULIX_SPACE_SERVICE_DELAY = 2000;
    private static final int GET_EULIX_SPACE_BINDER_DELAY = 1000;
    private static final int INIT_DELAY = 3500;
    private static final int BIND_EULIX_SPACE_SERVICE = 1;
    private static final int GET_EULIX_SPACE_BINDER = BIND_EULIX_SPACE_SERVICE + 1;
    private static final int INSTALL_APP = GET_EULIX_SPACE_BINDER + 1;
    private static final int INIT_NETWORK = INSTALL_APP + 1;
    private static final int INIT_MEMBER_LIST = INIT_NETWORK + 1;
    private static final int INIT_DEVICE_ABILITY = INIT_MEMBER_LIST + 1;
    private static final int DEVICE_TOKEN_REGISTER = INIT_DEVICE_ABILITY + 1;
    private TabImageViewHolder tabImageViewHolder;
    private FragmentManager mFragmentManager;
    private String mCurrentSelectedFragment = ConstantField.FRAG_FILE;
    private int deviceNumber = 0;
    private String activeDeviceUUID = null;
    private String activeDeviceBind = null;
    private TabImageView tabViewFile;
    private TabImageView tabViewMine;
    private RelativeLayout layoutNavigation;
    private int childFragmentIndex = 0;
    private String tabFileTarget = null;
    private LinearLayout layoutUpload;
    private TextView tvLocation;
    private ImageButton notificationReminderExit;
    private Button notificationReminderButton;
    private Dialog notificationReminderDialog;
    private Button appVersionUpdateCancel;
    private Button appVersionUpdateConfirm;
    private ImageView appVersionUpdateImage;
    private TextView appVersionUpdateContent;
    private ImageView imgDialogMask;
    private RelativeLayout layoutUploadDialog;
    private ImageView imgUploadAdd;

    private Animation transitionInAnim;
    private Animation transitionOutAnim;

    private Dialog appVersionUpdateDialog;
    private FolderListView folderListView;
    private TabFileFragment fileFragment;
    private TabMineFragment mineFragment;
    private EulixMainHandler mHandler;
    private List<UUID> newFolderUUIDList;

    private LinearLayout layoutPhoto;
    private LinearLayout layoutVideo;
    private LinearLayout layoutDocument;
    private LinearLayout layoutMkFile;
    private LinearLayout layoutScan;

    private Dialog securityMailboxOnlineReminderDialog;
    private Button securityMailboxOnlineReminderDialogConfirm;
    private ImageButton securityMailboxOnlineReminderDialogExit;

    private Animation animOut, animIn;
    private String MEDIA_TYPE_ = "mediaType";
    private static final String PATH = "path";
    private ContentObserver boxObserver;
    private boolean isBoxObserve;
    private boolean isInitMemberList;
    private boolean isInitDeviceAbility;
    private boolean isMemberCreated;
    private boolean isRequestAvatar;
    private String mPushType;
    private String mFragmentTag;
    private String mFragmentTarget;
    private String apkDownloadPath;
    private String appVersionName;
    private boolean isBindEulixSpaceService;
    private EulixSpaceService.EulixSpaceBinder eulixSpaceBinder;
    private FileSearchBridge fileSearchBridge;

    private long mExitTime = 0L;
    private boolean isClickCamera = false;

    private String securityEmailRequestUuid;
    private String deviceHardwareInfoRequestUuid;

    private Runnable resetClickCameraRunnable = () -> isClickCamera = false;

    private boolean isUploadDialogShowing = false;

    static class EulixMainHandler extends Handler {
        private WeakReference<EulixMainActivity> eulixMainActivityWeakReference;

        public EulixMainHandler(EulixMainActivity activity) {
            eulixMainActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixMainActivity activity = eulixMainActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                Object obj = msg.obj;
                switch (msg.what) {
                    case BIND_EULIX_SPACE_SERVICE:
                        if (activity.bindEulixSpaceService()) {
                            sendEmptyMessage(GET_EULIX_SPACE_BINDER);
                        } else {
                            sendEmptyMessageDelayed(BIND_EULIX_SPACE_SERVICE, BIND_EULIX_SPACE_SERVICE_DELAY);
                        }
                        break;
                    case GET_EULIX_SPACE_BINDER:
                        if (activity.eulixSpaceBinder == null) {
                            sendEmptyMessageDelayed(GET_EULIX_SPACE_BINDER, GET_EULIX_SPACE_BINDER_DELAY);
                        } else {
                            activity.eulixSpaceBinder.registerCallback(activity);
                        }
                        break;
                    case INSTALL_APP:
                        if (obj instanceof String) {
                            activity.installApp((String) obj);
                        }
                        break;
                    case INIT_NETWORK:
                        if (activity.presenter == null || !activity.presenter.updateNetwork()) {
                            sendEmptyMessageDelayed(INIT_NETWORK, INIT_DELAY);
                        } else {
                            sendEmptyMessageDelayed(INIT_NETWORK, MINUTE_UNIT);
                        }
                        break;
                    case INIT_MEMBER_LIST:
                        if (activity.presenter == null || !activity.presenter.updateMemberList(!activity.isInitMemberList)) {
                            activity.isInitMemberList = false;
                            sendEmptyMessageDelayed(INIT_MEMBER_LIST, INIT_DELAY);
                        } else {
                            activity.isInitMemberList = true;
                        }
                        break;
                    case INIT_DEVICE_ABILITY:
                        if (activity.presenter == null || !activity.presenter.updateDeviceAbility(!activity.isInitDeviceAbility)) {
                            activity.isInitDeviceAbility = false;
                            sendEmptyMessageDelayed(INIT_DEVICE_ABILITY, INIT_DELAY);
                        } else {
                            activity.isInitDeviceAbility = true;
                        }
                        break;
                    case DEVICE_TOKEN_REGISTER:
                        if (activity.presenter == null || !activity.presenter.deviceRegister()) {
                            sendEmptyMessageDelayed(DEVICE_TOKEN_REGISTER, INIT_DELAY);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private ServiceConnection eulixSpaceServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof EulixSpaceService.EulixSpaceBinder) {
                eulixSpaceBinder = (EulixSpaceService.EulixSpaceBinder) service;
                isBindEulixSpaceService = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBindEulixSpaceService = false;
            eulixSpaceBinder = null;
            if (mHandler != null) {
                mHandler.sendEmptyMessageDelayed(BIND_EULIX_SPACE_SERVICE, BIND_EULIX_SPACE_SERVICE_DELAY);
            }
        }
    };

    @Override
    protected boolean interceptorShow() {
        initDevice();
        if (activeDeviceUUID == null || activeDeviceBind == null) {
            Intent intent = new Intent(EulixMainActivity.this, EulixDeviceListActivity.class);
            startActivity(intent);
            return false;
        } else {
            DataUtil.setLastEulixSpace(getApplicationContext(), activeDeviceUUID, activeDeviceBind);
            return super.interceptorShow();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EulixSpaceApplication.popAllOldActivity(this);
        handleIntent(getIntent());


        EulixSpaceDBUtil.offlineTemperateBox(getApplicationContext(), WEEK_UNIT);
        EulixSpaceDBUtil.handleTemperateBox(getApplicationContext());
        EulixSpaceDBUtil.handleGranteeBox(getApplicationContext());

        List<String> permissionList = new ArrayList<>();
        permissionList.add(ConstantField.Permission.READ_EXTERNAL_STORAGE);
        permissionList.add(ConstantField.Permission.WRITE_EXTERNAL_STORAGE);
        SystemUtil.requestPermission(this, permissionList.toArray(new String[0]), ConstantField.RequestCode.EXTERNAL_STORAGE_PERMISSION);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        jumpToSpecificFragment();
        jumpToSpecificActivity();
    }

    @Override
    public boolean isStatusBarImmersion() {
        return true;
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_main_layout);

        layoutNavigation = findViewById(R.id.layout_navigation);
        tabViewFile = findViewById(R.id.tab_image_file);

        tabViewMine = findViewById(R.id.tab_image_mine);
        layoutUpload = findViewById(R.id.layout_upload);

        imgDialogMask = findViewById(R.id.img_dialog_mask);
        layoutUploadDialog = findViewById(R.id.layout_upload_dialog);
        imgUploadAdd = findViewById(R.id.img_upload_add);

        layoutPhoto = findViewById(R.id.upload_layout_gallery);
        layoutVideo = findViewById(R.id.upload_layout_video);
        layoutDocument = findViewById(R.id.upload_layout_document);
        layoutMkFile = findViewById(R.id.upload_layout_mkdir);
        layoutScan = findViewById(R.id.upload_layout_scan);
        LinearLayout layoutLocation = findViewById(R.id.upload_layout_location);
        tvLocation = findViewById(R.id.tv_location);

        layoutPhoto.setOnClickListener(mUploadDialogClickListener);
        layoutVideo.setOnClickListener(mUploadDialogClickListener);
        layoutDocument.setOnClickListener(mUploadDialogClickListener);
        layoutMkFile.setOnClickListener(mUploadDialogClickListener);
        layoutScan.setOnClickListener(mUploadDialogClickListener);
        layoutLocation.setOnClickListener(mUploadDialogClickListener);

        tabImageViewHolder = new TabImageViewHolder();
        tabImageViewHolder.addTabImageView(tabViewFile, R.drawable.icon_file_selected, R.drawable.icon_file_normal, getString(R.string.home_file));
        tabImageViewHolder.addTabImageView(tabViewMine, R.drawable.icon_mine_selected_v2, R.drawable.icon_mine_normal_v2, getString(R.string.home_mine));
        tabImageViewHolder.setOnTabSelectedListener(viewId -> changeTabById(viewId, null));
        tabImageViewHolder.setSelected(tabViewFile.getId());

        animIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        animOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

        folderListView = new FolderListView(this);
        folderListView.registerCallback(this);
        folderListView.registerFolderNewCallback(this);

        View notificationReminderDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_error_one_button_dialog_style_2, null);
        notificationReminderExit = notificationReminderDialogView.findViewById(R.id.dialog_exit);
        ImageView notificationReminderImage = notificationReminderDialogView.findViewById(R.id.dialog_image);
        TextView notificationReminderTitle = notificationReminderDialogView.findViewById(R.id.dialog_title);
        TextView notificationReminderContent = notificationReminderDialogView.findViewById(R.id.dialog_content);
        notificationReminderButton = notificationReminderDialogView.findViewById(R.id.dialog_button);
        notificationReminderExit.setVisibility(View.VISIBLE);
        notificationReminderImage.setImageResource(R.drawable.notification_reminder_2x);
        notificationReminderTitle.setText(R.string.notification_reminder_title);
        notificationReminderContent.setText(R.string.notification_reminder_content);
        notificationReminderButton.setText(R.string.to_open);
        notificationReminderDialog = new Dialog(this, R.style.EulixDialog);
        notificationReminderDialog.setCancelable(false);
        notificationReminderDialog.setContentView(notificationReminderDialogView);

        View versionUpdateDialogView = LayoutInflater.from(this).inflate(R.layout.version_update_dialog, null);
        appVersionUpdateImage = versionUpdateDialogView.findViewById(R.id.version_update_image);
        appVersionUpdateContent = versionUpdateDialogView.findViewById(R.id.version_update_content);
        appVersionUpdateCancel = versionUpdateDialogView.findViewById(R.id.cancel);
        appVersionUpdateConfirm = versionUpdateDialogView.findViewById(R.id.update);
        appVersionUpdateDialog = new Dialog(this, R.style.EulixDialog);
        appVersionUpdateDialog.setCancelable(false);
        appVersionUpdateDialog.setContentView(versionUpdateDialogView);

        View securityMailboxOnlineReminderDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_picture_dialog, null);
        ImageView securityMailboxOnlineReminderDialogImage = securityMailboxOnlineReminderDialogView.findViewById(R.id.dialog_image);
        TextView securityMailboxOnlineReminderDialogTitle = securityMailboxOnlineReminderDialogView.findViewById(R.id.dialog_title);
        TextView securityMailboxOnlineReminderDialogContent = securityMailboxOnlineReminderDialogView.findViewById(R.id.dialog_content);
        securityMailboxOnlineReminderDialogConfirm = securityMailboxOnlineReminderDialogView.findViewById(R.id.dialog_confirm);
        securityMailboxOnlineReminderDialogExit = securityMailboxOnlineReminderDialogView.findViewById(R.id.dialog_exit);
        securityMailboxOnlineReminderDialogImage.setImageResource(R.drawable.security_mailbox_image_2x);
        securityMailboxOnlineReminderDialogTitle.setText(R.string.security_mailbox_online);
        securityMailboxOnlineReminderDialogContent.setText(R.string.security_mailbox_online_content);
        securityMailboxOnlineReminderDialogConfirm.setText(R.string.to_set);
        securityMailboxOnlineReminderDialog = new Dialog(this, R.style.EulixDialog);
        securityMailboxOnlineReminderDialog.setCancelable(false);
        securityMailboxOnlineReminderDialog.setContentView(securityMailboxOnlineReminderDialogView);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(ConstantField.FRAGMENT_TAG)) {
                mFragmentTag = intent.getStringExtra(ConstantField.FRAGMENT_TAG);
            }
            if (intent.hasExtra(ConstantField.FRAGMENT_TARGET)) {
                mFragmentTarget = intent.getStringExtra(ConstantField.FRAGMENT_TARGET);
            }
            if (intent.hasExtra(ConstantField.PushExtraKey.OPT_TYPE)) {
                mPushType = intent.getStringExtra(ConstantField.PushExtraKey.OPT_TYPE);
            }
        }
    }

    private void jumpToSpecificFragment() {
        if (mFragmentTag != null) {
            startSpecificFragment(mFragmentTag, mFragmentTarget);
            mFragmentTarget = null;
            mFragmentTag = null;
        }
    }

    private void jumpToSpecificActivity() {
        if (mPushType != null) {
            Intent intent = null;
            switch (mPushType) {
                case ConstantField.PushType.LOGIN:
                    intent = new Intent(EulixMainActivity.this, DeviceManageActivity.class);
                    break;
                case ConstantField.PushType.BOX_UPGRADE:
                case ConstantField.PushType.UPGRADE_SUCCESS:
                case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                    intent = new Intent(EulixMainActivity.this, SystemUpdateActivity.class);
                    break;
                case ConstantField.PushType.APP_UPGRADE:
                    intent = new Intent(EulixMainActivity.this, AboutUsActivity.class);
                    break;
                default:
                    break;
            }
            mPushType = null;
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    private void changeTabById(int tabId, String target) {
        if (tabId == tabViewFile.getId()) {
            if (fileFragment != null) {
                fileFragment.refreshTransferringCount();
            }
            showFragment(ConstantField.FRAG_FILE, tabFileTarget);
            mCurrentSelectedFragment = ConstantField.FRAG_FILE;
            setChildFragmentIndex(ConstantField.FragmentIndex.TAB_FILE);
        } else if (tabId == tabViewMine.getId()) {
            if (mineFragment != null) {
                mineFragment.handleMimeInfo(false);
            }
            showFragment(ConstantField.FRAG_MINE, null);
            mCurrentSelectedFragment = ConstantField.FRAG_MINE;
            setChildFragmentIndex(ConstantField.FragmentIndex.TAB_MINE);
        }
    }

    private void showFragment(String tag, String target) {
        if (mFragmentManager == null) {
            mFragmentManager = getSupportFragmentManager();
        }

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        List<Fragment> fragments = mFragmentManager.getFragments();
        for (Fragment it : fragments) {
            fragmentTransaction.hide(it);
        }

        try {
            Fragment fragment = mFragmentManager.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = getFragmentByTag(tag);
            }
            Bundle bundle = new Bundle();
            bundle.putString("from", target);
            if (fragment != null) {
                if (!fragment.isStateSaved()) {
                    fragment.setArguments(bundle);
                }
                if (fragment.isAdded()) {
                    fragmentTransaction.show(fragment);
                    setTarget(tag, target);
                } else {
                    fragmentTransaction.add(R.id.fg_container, fragment, tag);
                }
                fragmentTransaction.commitNowAllowingStateLoss();
            }
            if (ConstantField.FRAG_FILE.equals(tag)) {
                tabFileTarget = null;
            }
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }
    }

    private Fragment getFragmentByTag(String tag) {
        switch (tag) {
            case ConstantField.FRAG_FILE:
                fileFragment = new TabFileFragment();
                return fileFragment;
            case ConstantField.FRAG_MINE:
                mineFragment = new TabMineFragment();
                return mineFragment;
            default:
                return null;
        }
    }

    private void setTarget(String tag, String target) {
        if (tag != null) {
            switch (tag) {
                case ConstantField.FRAG_FILE:
                    if (fileFragment != null) {
                        fileFragment.setFileTarget(target);
                        fileFragment.selectTab();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void initData() {
        EventBusUtil.post(new BoxOnlineRequestEvent(false));
        AlreadyUploadedManager.getInstance().init(getApplicationContext());
        isInitMemberList = false;
        isInitDeviceAbility = false;
        isMemberCreated = false;
        mHandler = new EulixMainHandler(this);
        EventBusUtil.register(this);
        isBindEulixSpaceService = false;
        startSpaceService();
        mHandler.sendEmptyMessage(BIND_EULIX_SPACE_SERVICE);
        boxObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                boolean isActiveExist = initDevice();
                if (activeDeviceUUID == null || activeDeviceBind == null) {
                    if (!isActiveExist) {
                        Boolean lock = getStrongPushLock();
                        if (lock != null && !lock) {
                            Intent intent = new Intent(EulixMainActivity.this, EulixDeviceListActivity.class);
                            startActivity(intent);
                        }
                    }
                } else {
                    DataUtil.setLastEulixSpace(getApplicationContext(), activeDeviceUUID, activeDeviceBind);
                }
                if (mineFragment != null) {
                    mineFragment.handleMimeInfo(true);
                }
            }
        };
    }

    @Override
    public void initViewData() {
        presenter.getCurrentBoxVersion();
        presenter.checkBoxVersion();
        if (!PreferenceUtil.getHasUpgradeDbAccountValue(this)) {
            presenter.upgradeDbAccountValue();
        }
    }

    @Override
    public void initEvent() {
        imgDialogMask.setOnClickListener(v -> {
            if (isUploadDialogShowing) {
                showUploadDialog(false);
            }
        });
        layoutUploadDialog.setOnClickListener(v -> {
            //do nothing
        });
        layoutUpload.setOnClickListener(v -> {
            showUploadDialog(!isUploadDialogShowing);
        });
        if (presenter != null) {
            presenter.checkAppUpdate();
            DeviceAbility deviceAbility = presenter.getActiveDeviceAbility();
            int identity = presenter.getIdentity();
            if (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity || ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE == identity) {
                boolean isDiskManagementEnable = false;
                if (deviceAbility != null) {
                    Boolean isInnerDiskSupportValue = deviceAbility.getInnerDiskSupport();
                    if (isInnerDiskSupportValue != null) {
                        isDiskManagementEnable = isInnerDiskSupportValue;
                    }
                }
                if (isDiskManagementEnable) {
                    String boxUuid = null;
                    String boxBind = null;
                    EulixBoxBaseInfo eulixBoxBaseInfo = null;
                    if (presenter != null) {
                        eulixBoxBaseInfo = presenter.getActiveBoxBaseInfo();
                    }
                    if (eulixBoxBaseInfo != null) {
                        boxUuid = eulixBoxBaseInfo.getBoxUuid();
                        boxBind = eulixBoxBaseInfo.getBoxBind();
                    }
                    if (boxUuid != null && boxBind != null) {
                        EventBusUtil.post(new DiskManagementListRequestEvent(boxUuid, boxBind, UUID.randomUUID().toString()));
                    }
                }
            }
        }

        // 2.0后取消主页面询问问卷
//        if (!QuestionnaireUtil.isRequestQuestionnaire() && presenter != null) {
//            presenter.getQuestionnaireList(1, false);
//        }

        notificationReminderExit.setOnClickListener(v -> {
            dismissNotificationReminderDialog();
            handleNotificationReminder();
        });
        notificationReminderButton.setOnClickListener(v -> {
            dismissNotificationReminderDialog();
            handleNotificationReminder();
            SystemUtil.requestNotification(EulixMainActivity.this, true);
        });

        appVersionUpdateCancel.setOnClickListener(v -> {
            dismissAppVersionUpdateDialog();
            handleAppVersionUpdate();
        });

        appVersionUpdateConfirm.setOnClickListener(v -> {
            updateApp();
            dismissAppVersionUpdateDialog();
            handleAppVersionUpdate();
        });

        securityMailboxOnlineReminderDialogConfirm.setOnClickListener(v -> {
            dismissSecurityMailboxOnlineReminderDialog();
            Intent intent = new Intent(EulixMainActivity.this, EulixAuthenticationActivity.class);
            intent.putExtra(ConstantField.HARDWARE_FUNCTION, ConstantField.HardwareFunction.SECURITY_VERIFICATION);
            intent.putExtra(ConstantField.SECURITY_FUNCTION, ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX);
            startActivity(intent);
        });

        securityMailboxOnlineReminderDialogExit.setOnClickListener(v -> dismissSecurityMailboxOnlineReminderDialog());
    }

    private boolean initDevice() {
        boolean isActiveExist = true;
        Logger.d("zfy", "initDevice()");
        isRequestAvatar = false;
        deviceNumber = EulixSpaceDBUtil.getDeviceNumber(getApplicationContext(), false);
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext()
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null && boxValues.size() > 0) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null) {
                    String nActiveDeviceUUID = (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            ? boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID) : "");
                    String nActiveDeviceBind = (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            ? boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND) : "");
                    if (nActiveDeviceUUID != null && nActiveDeviceBind != null
                            && (activeDeviceUUID == null || !activeDeviceUUID.equals(nActiveDeviceUUID)
                            || activeDeviceBind == null || !activeDeviceBind.equals(nActiveDeviceBind))) {
                        if (verifySecurityMailboxIdentity() && presenter != null) {
                            securityEmailRequestUuid = UUID.randomUUID().toString();
                            deviceHardwareInfoRequestUuid = UUID.randomUUID().toString();
                            presenter.getBluetoothId(deviceHardwareInfoRequestUuid);
                        }
                    }
                    if ((activeDeviceUUID != null && !activeDeviceUUID.equals(nActiveDeviceUUID))
                            || (activeDeviceBind != null && !activeDeviceBind.equals(nActiveDeviceBind))) {
                        Logger.d(TAG, "device change, old uuid: " + activeDeviceUUID + ", bind: " + activeDeviceBind
                                + "; new uuid: " + nActiveDeviceUUID + ", bind: " + nActiveDeviceBind);
                        isRequestAvatar = true;

                        if (fileFragment != null) {
                            fileFragment.resetFileFragment(ConstantField.FragmentIndex.TAB_FILE);
                        }
                    }
                    activeDeviceUUID = nActiveDeviceUUID;
                    activeDeviceBind = nActiveDeviceBind;
                    break;
                }
            }
        } else {
            if (activeDeviceUUID != null && activeDeviceBind != null) {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, activeDeviceUUID);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, activeDeviceBind);
                List<Map<String, String>> nBoxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
                isActiveExist = (nBoxValues != null && nBoxValues.size() > 0);
            }
            activeDeviceUUID = null;
            activeDeviceBind = null;
        }
        return isActiveExist;
    }

    private void handleNotificationReminder() {
        if (presenter != null) {
            presenter.setNotificationReminderVersion();
        }
    }

    private void handleAppVersionUpdate() {
        if (presenter != null) {
            presenter.setAppVersionUpdate(appVersionName);
            appVersionName = null;
        }
    }

    private void updateApp() {
        if (presenter != null) {
            AppUpdateEvent appUpdateEvent = new AppUpdateEvent(presenter.getApkSize(), presenter.getDownloadUrl()
                    , presenter.getMd5(), presenter.getNewestVersion(), false);
            showPureTextToast(R.string.download_newest_version);
            EventBusUtil.post(appUpdateEvent);
        }
    }

    //弹出上传文件弹框
    public void showUploadDialog(boolean isShow) {
        if (tvLocation != null) {
            tvLocation.setText(generatePath(getString(R.string.my_space), SPLIT_1, true));
        }

        if (layoutUploadDialog.getAnimation() != null) {
            layoutUploadDialog.clearAnimation();
        }
        if (imgUploadAdd.getAnimation() != null) {
            imgUploadAdd.clearAnimation();
        }
        if (isShow) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.icon_vertical_translate);
            if (layoutPhoto != null) {
                layoutPhoto.startAnimation(animation);
            }
            if (layoutVideo != null) {
                layoutVideo.startAnimation(animation);
            }
            if (layoutDocument != null) {
                layoutDocument.startAnimation(animation);
            }
            if (layoutMkFile != null) {
                layoutMkFile.startAnimation(animation);
            }
            if (layoutScan != null) {
                layoutScan.startAnimation(animation);
            }

            DataUtil.setUuidStack(getCurrentFileUUIDStack());
            transitionInAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_in_anim);

            ObjectAnimator animator = ObjectAnimator.ofFloat(imgUploadAdd, "rotation", 0, 45);
            animator.setDuration(200);
            animator.start();

            imgDialogMask.setVisibility(View.VISIBLE);
            layoutUploadDialog.setAnimation(transitionInAnim);
            layoutUploadDialog.setVisibility(View.VISIBLE);
        } else {
            DataUtil.setUuidStack(null);
            transitionOutAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_out_anim);

            ObjectAnimator animator = ObjectAnimator.ofFloat(imgUploadAdd, "rotation", 45, 0);
            animator.setDuration(200);
            animator.start();

            layoutUploadDialog.setAnimation(transitionOutAnim);
            layoutUploadDialog.setVisibility(View.GONE);
            imgDialogMask.setVisibility(View.GONE);
        }

        isUploadDialogShowing = isShow;

    }

    private void setBackgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = this.getWindow()
                .getAttributes();
        lp.alpha = bgAlpha;
        this.getWindow().setAttributes(lp);
    }

    //上传弹框选项点击事件处理
    private View.OnClickListener mUploadDialogClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ArrayStack<UUID> uuids = null;
            if (presenter != null) {
                uuids = presenter.getUuids();
            }
            String path = null;
            if (uuids != null) {
                path = new Gson().toJson(uuids, new TypeToken<ArrayStack<UUID>>() {
                }.getType());
            }
            if (path == null) {
                path = "";
            }
            final String pathFinal = path;
            switch (v.getId()) {
                case R.id.upload_layout_gallery:
                    if (PermissionUtils.isPermissionGranted(EulixMainActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE)) {
                        jumpImageSelectAfterGetPermission(pathFinal);
                    } else {
                        PermissionUtils.requestPermissionWithNotice(EulixMainActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                if (result) {
                                    jumpImageSelectAfterGetPermission(pathFinal);
                                }
                            }
                        });
                    }
                    break;
                case R.id.upload_layout_video:
                    if (PermissionUtils.isPermissionGranted(EulixMainActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE)) {
                        jumpVideoSelectAfterGetPermission(pathFinal);
                    } else {
                        PermissionUtils.requestPermissionWithNotice(EulixMainActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                if (result) {
                                    jumpVideoSelectAfterGetPermission(pathFinal);
                                }
                            }
                        });
                    }
                    break;
                case R.id.upload_layout_document:
                    if (PermissionUtils.isPermissionGranted(EulixMainActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE)) {
                        jumpDocumentSelectAfterGetPermission(pathFinal);
                    } else {
                        PermissionUtils.requestPermissionWithNotice(EulixMainActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                if (result) {
                                    jumpDocumentSelectAfterGetPermission(pathFinal);
                                }
                            }
                        });
                    }
                    break;
                case R.id.upload_layout_mkdir:
                    if (uuids == null || uuids.size() < FolderListView.FOLDER_DEPTH_MAX_VALUE) {
                        handleNewFolder();
                    } else {
                        showPureTextToast(R.string.new_folder_exceed_layers);
                    }
                    break;
                case R.id.upload_layout_scan:
                    startScan();
                    showUploadDialog(false);
                    break;
                case R.id.upload_layout_location:
                    folderListView.showFolderListDialog(null);
                    break;
                default:
                    break;
            }
        }
    };

    //跳转上传图片选择
    private void jumpImageSelectAfterGetPermission(String path) {
        Intent intent = new Intent(EulixMainActivity.this, LocalAlbumListActivity.class);
        intent.putExtra(MEDIA_TYPE_, ConstantField.MediaType.MEDIA_IMAGE);
        intent.putExtra(PATH, path);
        startActivity(intent);
        showUploadDialog(false);
    }

    //跳转上传视频选择
    private void jumpVideoSelectAfterGetPermission(String path) {
        Intent intent1 = new Intent(EulixMainActivity.this, LocalMediaSelectActivity.class);
        intent1.putExtra(MEDIA_TYPE_, ConstantField.MediaType.MEDIA_VIDEO);
        intent1.putExtra(PATH, path);
        startActivity(intent1);
        showUploadDialog(false);
    }

    //跳转上传文件选择
    private void jumpDocumentSelectAfterGetPermission(String path) {
        Intent intent2 = new Intent(EulixMainActivity.this, LocalMediaSelectActivity.class);
        intent2.putExtra(MEDIA_TYPE_, ConstantField.MediaType.MEDIA_FILE);
        intent2.putExtra(PATH, path);
        startActivity(intent2);
        showUploadDialog(false);
    }

    private String generatePath(String rootName, String split, boolean isUploadShow) {
        StringBuilder locationBuilder = new StringBuilder(rootName);
        ArrayStack<UUID> uuidStack = (isUploadShow ? getCurrentFileUUIDStack() : getCurrentUUIDStack());
        if (presenter != null) {
            presenter.setUuids(uuidStack);
        }
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        if (uuidTitleMap != null && uuidStack != null && uuidStack.size() > 1) {
            for (UUID uuid : uuidStack) {
                if (uuid != null) {
                    if (uuidTitleMap.containsKey(uuid.toString())) {
                        locationBuilder.append(split);
                        locationBuilder.append(uuidTitleMap.get(uuid.toString()));
                    } else {
                        if (!ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
                            locationBuilder.append(split);
                        }
                    }
                }
            }
        }
        return locationBuilder.toString();
    }

    /**
     * 展示新建文件夹
     */
    private void handleNewFolder() {
        folderListView.showNewFolderDialog(false);
    }

    public String getUuidTitle(String uuid) {
        String title = null;
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        if (uuid != null && uuidTitleMap != null && uuidTitleMap.containsKey(uuid)) {
            title = uuidTitleMap.get(uuid);
        }
        return title;
    }

    public void setUuidTitle(String uuid, String title) {
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        if (uuidTitleMap == null) {
            uuidTitleMap = new HashMap<>();
        }
        uuidTitleMap.put(uuid, title);
    }

    public void startSpecificFragment(@NonNull String fragmentTag, @Nullable String fragmentTarget) {
        switch (fragmentTag) {
            case ConstantField.FRAG_FILE:
                tabFileTarget = fragmentTarget;
                tabImageViewHolder.setSelected(tabViewFile.getId());
                break;
            case ConstantField.FRAG_MINE:
                tabImageViewHolder.setSelected(tabViewMine.getId());
                break;
            default:
                break;
        }
    }

    //扫描二维码
    public void startScan() {
        if (PermissionUtils.isPermissionGranted(EulixMainActivity.this, PermissionUtils.PERMISSION_CAMERA)) {
            startScanAfterGetPermission();
        } else {
            PermissionUtils.requestPermissionWithNotice(EulixMainActivity.this, PermissionUtils.PERMISSION_CAMERA, new ResultCallback() {
                @Override
                public void onResult(boolean result, String extraMsg) {
                    if (result) {
                        startScanAfterGetPermission();
                    }
                }
            });
        }
    }

    private void startScanAfterGetPermission() {
        if (!isClickCamera) {
            isClickCamera = true;
            Intent intent = new Intent(EulixMainActivity.this, CaptureActivity.class);
            intent.setAction(Intents.Scan.ACTION);
            //全屏扫描
            intent.putExtra(Intents.Scan.WIDTH, ViewUtils.getScreenWidth(getApplicationContext()));
            intent.putExtra(Intents.Scan.HEIGHT, ViewUtils.getScreenHeight(getApplicationContext()));
            //只扫描二维码
            intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
            intent.putExtra(ConstantField.ZxingCommunication.FUNCTION_EXTRA_KEY, ConstantField.ZxingCommunication.LOGIN_EXTRA_VALUE);
            intent.putExtra(ConstantField.ZxingCommunication.IMMEDIATE_EXTRA_KEY, false);
            intent.putExtra(ConstantField.ZxingCommunication.DEFAULT_STATUS, getString(R.string.scan_qr_code_status_login));
            startActivityForResult(intent, ConstantField.RequestCode.REQUEST_CODE_SCAN);
            if (mHandler != null) {
                mHandler.postDelayed(resetClickCameraRunnable, 2000);
            } else {
                isClickCamera = false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.REQUEST_CODE_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || data.getExtras() == null) {
                        return;
                    }
                    String result = data.getExtras().getString(Intents.Scan.RESULT);
                    Logger.d(TAG, "qr code result: " + result);
                    if (result != null) {
                        //解析二维码数据，并兼容旧格式 eg: p=aospace&bt=box-login&v=4e3022fd-156d-459f-8369-bbabe579b2b3
                        boolean isPlatformQrCode;
                        String value;
                        if (!result.contains("=")) {
                            isPlatformQrCode = true;
                            value = result;
                        } else {
                            Map<String, String> paramMap = new HashMap<>();
                            String[] params = result.split("&");
                            for (int i = 0; i < params.length; i++) {
                                String[] paramItem = params[i].split("=");
                                if (paramItem.length > 1) {
                                    paramMap.put(paramItem[0], paramItem[1]);
                                }
                            }
                            if (paramMap.containsKey("isApp")) {
                                if ("1".equals(paramMap.get("isApp"))) {
                                    //移动端判断版本类型
                                    if (!paramMap.containsKey("isOpensource") || !"1".equals(paramMap.get("isOpensource"))) {
                                        showImageTextToast(R.drawable.toast_refuse, R.string.app_server_not_match);
                                        return;
                                    }
                                }
                            }

                            isPlatformQrCode = ConstantField.QrScanParamType.BT_PLATFORM_LOGIN.equals(paramMap.get("bt"));
                            value = paramMap.get("v");
                        }

                        Intent intent = new Intent(EulixMainActivity.this, GranterLoginActivity.class);
                        if (!isPlatformQrCode) {
                            intent.putExtra(GranterLoginActivity.KEY_IS_BOX_LOGIN, true);
                        }
                        intent.putExtra(ConstantField.PLATFORM_KEY, value);
                        startActivity(intent);
                    }
                }
                break;
            case ConstantField.RequestCode.REQUEST_INSTALL_PACKAGES:
                if (SystemUtil.requestInstallPackages(this, false)) {
                    if (apkDownloadPath == null && presenter != null) {
                        apkDownloadPath = presenter.getApkDownloadPath();
                    }
                    SystemUtil.installPackage(this, apkDownloadPath);
                }
                break;
            default:
                break;
        }
    }

    private boolean handleBackEvent() {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.TAB_FILE:
                return (fileFragment != null && fileFragment.handleBackEvent());
            default:
                return false;
        }
    }

    public void startFileSearch(String fileUuid) {
        if (fileSearchBridge == null) {
            fileSearchBridge = FileSearchBridge.getInstance();
            fileSearchBridge.registerSourceCallback(this);
        }
        Intent intent = new Intent(EulixMainActivity.this, FileSearchActivity.class);
        if (fileUuid != null) {
            intent.putExtra(ConstantField.FILE_UUID, fileUuid);
        }
        startActivity(intent);
    }


    public void setLayoutNavigationVisibility(boolean isShow) {
        if (layoutNavigation != null) {
            layoutNavigation.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
            layoutUpload.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    public void setBtnUploadVisibility(boolean isShow) {
//        if (btnUpload != null) {
//            btnUpload.setVisibility(isShow ? View.VISIBLE : View.GONE);
//        }
    }

    private void showNotificationReminderDialog() {
        if (notificationReminderDialog != null && !notificationReminderDialog.isShowing()) {
            notificationReminderDialog.show();
            Window window = notificationReminderDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissNotificationReminderDialog() {
        if (notificationReminderDialog != null && notificationReminderDialog.isShowing()) {
            notificationReminderDialog.dismiss();
        }
    }

    private void prepareAppUpdateShow() {
        appVersionUpdateContent.scrollTo(0, 0);
        String newestVersion = null;
        Long apkSize = null;
        String updateDescription = null;
        if (presenter != null) {
            newestVersion = presenter.getNewestVersion();
            apkSize = presenter.getApkSize();
            updateDescription = presenter.getUpdateDescription();
        }
        StringBuilder versionBuilder = new StringBuilder();
        versionBuilder.append(getString(R.string.newest_version));
        versionBuilder.append(getString(R.string.colon));
        versionBuilder.append("V");
        versionBuilder.append(newestVersion == null ? "" : newestVersion);
        versionBuilder.append("\n");
        versionBuilder.append(getString(R.string.new_version_size));
        versionBuilder.append(getString(R.string.colon));
        if (apkSize == null) {
            versionBuilder.append(getString(R.string.unknown));
        } else {
            versionBuilder.append(FormatUtil.formatSize(apkSize, ConstantField.SizeUnit.FORMAT_2F));
        }
        versionBuilder.append("\n");
        versionBuilder.append(getString(R.string.update_content));
        versionBuilder.append(getString(R.string.colon));
        versionBuilder.append("\n");
        versionBuilder.append(updateDescription == null ? "" : updateDescription);
        appVersionUpdateContent.setText(versionBuilder.toString());
    }

    private void showAppVersionUpdateDialog() {
        if (appVersionUpdateDialog != null && !appVersionUpdateDialog.isShowing()) {
            appVersionUpdateDialog.show();
            RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE);
            Glide.with(this).load(R.drawable.version_update).apply(options).into(appVersionUpdateImage);
            Window window = appVersionUpdateDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259)
                        , getResources().getDimensionPixelSize(R.dimen.dp_365));
            }
        }
    }

    private void dismissAppVersionUpdateDialog() {
        if (appVersionUpdateDialog != null && appVersionUpdateDialog.isShowing()) {
            appVersionUpdateImage.setImageDrawable(null);
            appVersionUpdateDialog.dismiss();
        }
    }

    private void showSecurityMailboxOnlineReminderDialog() {
        if (securityMailboxOnlineReminderDialog != null && !securityMailboxOnlineReminderDialog.isShowing()) {
            securityMailboxOnlineReminderDialog.show();
            Window window = securityMailboxOnlineReminderDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_288), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissSecurityMailboxOnlineReminderDialog() {
        if (securityMailboxOnlineReminderDialog != null && securityMailboxOnlineReminderDialog.isShowing()) {
            securityMailboxOnlineReminderDialog.dismiss();
        }
    }


    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter();
    }

    @Override
    public void appVersionUpdateCallback(String versionName) {
        if (mHandler != null) {
            mHandler.post(() -> {
                appVersionName = versionName;
                prepareAppUpdateShow();
                showAppVersionUpdateDialog();
            });
        }
    }

    private void startSpaceService() {
        Intent intent = new Intent(EulixMainActivity.this, EulixSpaceService.class);
        intent.setAction(ConstantField.Action.LAUNCH_ACTION);
        startService(intent);
    }

    /**
     * 绑定主服务
     *
     * @return
     */
    private boolean bindEulixSpaceService() {
        Intent intent = new Intent(EulixMainActivity.this, EulixSpaceService.class);
        return bindService(intent, eulixSpaceServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void installApp(String filePath) {
        apkDownloadPath = filePath;
        if (presenter != null) {
            presenter.setApkDownloadPath(filePath);
        }
        if (SystemUtil.requestInstallPackages(this, true)) {
            SystemUtil.installPackage(this, filePath);
        }
    }

    private boolean verifySecurityMailboxIdentity() {
        boolean isVerify = false;
        if (presenter != null) {
            int identity = presenter.getIdentity();
            isVerify = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE);
        }
        return isVerify;
    }

    public ArrayStack<UUID> getCurrentFileUUIDStack() {
        ArrayStack<UUID> uuids = null;
        if (fileFragment != null) {
            uuids = DataUtil.cloneUUIDStack(fileFragment.getCurrentUUIDStack());
        }
        return uuids;
    }

    @Override
    public void receiveMessage(String message) {
        // Do nothing
    }


    @Override
    public void obtainAccessToken() {
        Intent serviceIntent = new Intent(EulixMainActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        startService(serviceIntent);
    }

    @Override
    public int getSelectFilesSize() {
        int size = -1;
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.TAB_FILE:
                if (fileFragment != null) {
                    size = fileFragment.getSelectFilesSize();
                }
                break;
            default:
                break;
        }
        return size;
    }

    @Override
    public UUID getCurrentFolderUUID() {
        UUID uuid = null;
        if (fileFragment != null) {
            uuid = fileFragment.getCurrentFolderUUID();
        }
        return uuid;
    }

    @Override
    public ArrayStack<UUID> getCurrentUUIDStack() {
        ArrayStack<UUID> uuids = null;
        ArrayStack<UUID> stack = DataUtil.getUuidStack();
        if (stack != null) {
            uuids = DataUtil.cloneUUIDStack(stack);
        } else {
            if (fileFragment != null) {
                uuids = DataUtil.cloneUUIDStack(fileFragment.getCurrentUUIDStack());
            }
        }
        return uuids;
    }

    @Override
    public void refreshEulixSpaceStorage(UUID parentUUID) {
        if (parentUUID != null) {
            newFolderUUIDList = new ArrayList<>();
            newFolderUUIDList.add(parentUUID);
            if (fileFragment != null) {
                fileFragment.folderChange();
            }
        }
    }

    @Override
    public void dismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs) {
        newFolderUUIDList = newFolderUUIDs;
        if (newFolderUUIDs != null && fileFragment != null) {
            fileFragment.folderChange();
        }
        if (isConfirm) {
            if (isCopy == null) {
                DataUtil.setUuidStack(uuids);
                if (tvLocation != null) {
                    tvLocation.setText(generatePath(getString(R.string.my_space), SPLIT_1, false));
                }
            } else {
                handleBackEvent();
            }
        }
    }

    @Override
    public void dismissConfirmOuterNewFolderCallback(ArrayStack<UUID> uuids) {
        showUploadDialog(false);
        startSpecificFragment(ConstantField.FRAG_FILE, ConstantField.FRAG_FILE_ALL);
        if (fileFragment != null) {
            fileFragment.folderChange(uuids);
        }
    }

    @Override
    public void refreshEulixSpaceStorage(UUID parentUUID, String folderName, String folderUuid) {
        if (parentUUID != null) {
            newFolderUUIDList = new ArrayList<>();
            newFolderUUIDList.add(parentUUID);
            if (fileFragment != null) {
                fileFragment.folderChange(folderName, folderUuid);
            }
        }
    }

    @Override
    public void setFolder(UUID folderUuid) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (folderUuid != null) {
                    if (fileFragment == null) {
                        DataUtil.setFileSearchUuid(folderUuid);
                        if (childFragmentIndex != ConstantField.FragmentIndex.FILE_ALL) {
                            startSpecificFragment(ConstantField.FRAG_FILE, ConstantField.FRAG_FILE_ALL);
                        }
                    } else {
                        if (childFragmentIndex != ConstantField.FragmentIndex.FILE_ALL) {
                            startSpecificFragment(ConstantField.FRAG_FILE, ConstantField.FRAG_FILE_ALL);
                        }
                        fileFragment.folderChange(folderUuid, true);
                    }
                }
            });
        }
    }

    @Override
    public void startSelf(String fileUuid) {
        if (mHandler != null) {
            mHandler.post(() -> startFileSearch(fileUuid));
        }
    }

    public List<UUID> getNewFolderUUIDList() {
        return newFolderUUIDList;
    }

    public void resetNewFolderUUIDList() {
        newFolderUUIDList = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                while (mHandler.hasCallbacks(resetClickCameraRunnable)) {
                    mHandler.removeCallbacks(resetClickCameraRunnable);
                }
            } else {
                try {
                    mHandler.removeCallbacks(resetClickCameraRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        isClickCamera = false;
        EventBusUtil.post(new BoxNetworkRequestEvent());
        if (activeDeviceUUID != null && activeDeviceBind != null) {
//            if (mHandler != null) {
//                while (mHandler.hasMessages(PUSH_DEVICE_TOKEN)) {
//                    mHandler.removeMessages(PUSH_DEVICE_TOKEN);
//                }
//                mHandler.sendEmptyMessage(PUSH_DEVICE_TOKEN);
//            } else if (presenter != null) {
//                presenter.pushDevice();
//            }

            if (mHandler != null) {
                while (mHandler.hasMessages(DEVICE_TOKEN_REGISTER)) {
                    mHandler.removeMessages(DEVICE_TOKEN_REGISTER);
                }
                mHandler.sendEmptyMessage(DEVICE_TOKEN_REGISTER);
            } else if (presenter != null) {
                presenter.deviceRegister();
            }

            if (tvLocation != null) {
                tvLocation.setText(generatePath(getString(R.string.my_space), SPLIT_1, false));
            }
            switch (childFragmentIndex) {
                case ConstantField.FragmentIndex.TAB_MINE:
                    //StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_fff5f6fa), this);
                    break;
                case ConstantField.FragmentIndex.TAB_FILE:
                    if (fileFragment == null) {
                        //StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_fff5f6fa), this);
                    } else {
                        fileFragment.refreshTransferringCount();
                        //fileFragment.setStatusBarColor();
                    }
                    break;
                default:
                    break;
            }
            if (boxObserver != null && !isBoxObserve) {
                isBoxObserve = true;
                getContentResolver().registerContentObserver(EulixSpaceDBManager.BOX_URI, true, boxObserver);
            }
            if (mHandler != null) {
                while (mHandler.hasMessages(INIT_NETWORK)) {
                    mHandler.removeMessages(INIT_NETWORK);
                }
                mHandler.sendEmptyMessage(INIT_NETWORK);
                while (mHandler.hasMessages(INIT_MEMBER_LIST)) {
                    mHandler.removeMessages(INIT_MEMBER_LIST);
                }
                mHandler.sendEmptyMessage(INIT_MEMBER_LIST);
                while (mHandler.hasMessages(INIT_DEVICE_ABILITY)) {
                    mHandler.removeMessages(INIT_DEVICE_ABILITY);
                }
                mHandler.sendEmptyMessage(INIT_DEVICE_ABILITY);
            }
            if (presenter != null) {
                presenter.updateCommonInfo();
                if (presenter.getNotificationReminderVersion()) {
                    if (SystemUtil.requestNotification(this, false)) {
                        handleNotificationReminder();
                    } else {
                        showNotificationReminderDialog();
                    }
                }
            }
        }
        jumpToSpecificFragment();
        jumpToSpecificActivity();
    }

    @Override
    public void onBackPressed() {
        if (!handleBackEvent()) {
            //采用super.onBackPressed会在Android.Q造成内存泄漏
            finishAfterTransition();
        }
    }

    @Override
    protected void onStop() {
        if (isBoxObserve && boxObserver != null) {
            getContentResolver().unregisterContentObserver(boxObserver);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (fileSearchBridge != null) {
            fileSearchBridge.unregisterSourceCallback();
            fileSearchBridge = null;
        }
        EulixSpaceApplication.popAllOldActivity(this);
        if (folderListView != null) {
            folderListView.unregisterFolderNewCallback();
            folderListView.unregisterCallback();
            folderListView = null;
        }
        if (isBindEulixSpaceService && eulixSpaceBinder != null) {
            eulixSpaceBinder.unregisterCallback();
            try {
                unbindService(eulixSpaceServiceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                eulixSpaceBinder = null;
            }
            isBindEulixSpaceService = false;
        }
        EventBusUtil.unRegister(this);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isUploadDialogShowing) {
                showUploadDialog(false);
                return true;
            }
            if (handleBackEvent()) {
                return true;
            } else {
                if (System.currentTimeMillis() - mExitTime > 2000) {
                    showDefaultPureTextToast(R.string.app_exit_hint);
                    mExitTime = System.currentTimeMillis();
                    return true;
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public int getChildFragmentIndex() {
        return childFragmentIndex;
    }

    public void setChildFragmentIndex(int childFragmentIndex) {
        this.childFragmentIndex = childFragmentIndex;
        if (childFragmentIndex != ConstantField.FragmentIndex.TAB_FILE && fileFragment != null) {
            fileFragment.onLogUpPage(-1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ConstantField.RequestCode.EXTERNAL_STORAGE_PERMISSION) {
            boolean result = true;
            int length = Math.min(permissions.length, grantResults.length);
            List<String> denyPermissions = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                String permission = permissions[i];
                boolean isGrant = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                if (permission != null) {
                    PreferenceUtil.saveBaseKeyBoolean(EulixSpaceApplication.getContext(), permission, isGrant);
                }
                if (permission != null && !isGrant) {
                    switch (permission) {
                        case ConstantField.Permission.READ_EXTERNAL_STORAGE:
                        case ConstantField.Permission.WRITE_EXTERNAL_STORAGE:
//                            if (DataUtil.getClientUuid(getApplicationContext()) == null) {
//                                result = false;
//                                denyPermissions.add(permission);
//                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            if (!result) {
                boolean isRationale = false;
                for (String denyPermission : denyPermissions) {
                    if (denyPermission != null && !ActivityCompat.shouldShowRequestPermissionRationale(this, denyPermission)) {
                        isRationale = true;
                        break;
                    }
                }
                if (isRationale) {
                    Toast.makeText(this, R.string.permission_deny_setting_hint, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.permission_deny_open_failed, Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppCheckRequestEvent event) {
        if (event != null && presenter != null) {
            presenter.checkAppUpdate(event.isForce());
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppInstallEvent event) {
        if (event != null && !event.isForce()) {
            String filePath = event.getFilePath();
            if (filePath != null && event.isSuccess()) {
                if (mHandler == null) {
                    installApp(filePath);
                } else {
                    Message message = mHandler.obtainMessage(INSTALL_APP, filePath);
                    mHandler.sendMessage(message);
                }
            } else {
                showPureTextToast(R.string.download_failed);
            }
        }
    }

}