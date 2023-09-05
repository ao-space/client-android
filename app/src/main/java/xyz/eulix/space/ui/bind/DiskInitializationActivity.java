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

package xyz.eulix.space.ui.bind;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bridge.DiskInitializeBridge;
import xyz.eulix.space.event.AccessTokenResultEvent;
import xyz.eulix.space.event.DiskManagementListRequestEvent;
import xyz.eulix.space.event.DiskManagementListResponseEvent;
import xyz.eulix.space.event.DiskManagementRaidInfoRequestEvent;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.PairingBoxInfo;
import xyz.eulix.space.network.agent.disk.DiskInfo;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskInitializeRequest;
import xyz.eulix.space.network.agent.disk.DiskManageInfo;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.disk.DiskExpandProgressResult;
import xyz.eulix.space.presenter.DiskInitializationPresenter;
import xyz.eulix.space.ui.AOCompleteActivity;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/25 9:52
 */
public class DiskInitializationActivity extends AbsActivity<DiskInitializationPresenter.IDiskInitialization, DiskInitializationPresenter> implements DiskInitializationPresenter.IDiskInitialization
        , View.OnClickListener, CompoundButton.OnCheckedChangeListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, DiskInitializeBridge.DiskInitializeSinkCallback {
    private static final String TAG = DiskInitializationActivity.class.getSimpleName();
    private static final int MESSAGE_PROGRESS_TEST = 0;
    private static final int MESSAGE_PROGRESS_REQUEST = 1;
    private static final int MESSAGE_DISK_INITIALIZATION_SUCCESS = MESSAGE_PROGRESS_REQUEST + 1;
    private static final int MESSAGE_ACCESS_TOKEN_REQUEST = MESSAGE_DISK_INITIALIZATION_SUCCESS + 1;
    private static final int MESSAGE_EXPAND_PROGRESS_REQUEST = MESSAGE_ACCESS_TOKEN_REQUEST + 1;
    private static final int STEP_NO_MAIN_STORAGE = 0;
    private static final int STEP_NO_DISK = STEP_NO_MAIN_STORAGE + 1;
    private static final int STEP_FAST_DISK_INITIALIZATION = STEP_NO_DISK + 1;
    private static final int STEP_DISK_INFORMATION = STEP_FAST_DISK_INITIALIZATION + 1;
    private static final int STEP_DISK_MODE = STEP_DISK_INFORMATION + 1;
    private static final int STEP_DISK_MAIN_STORAGE = STEP_DISK_MODE + 1;
    private static final int STEP_DISK_ENCRYPTION = STEP_DISK_MAIN_STORAGE + 1;
    private static final int STEP_DISK_FORMAT = STEP_DISK_ENCRYPTION + 1;
    private static final int STEP_DISK_INITIALIZATION_PROGRESS = STEP_DISK_FORMAT + 1;
    private static final int STEP_DISK_INITIALIZATION_FAIL = STEP_DISK_INITIALIZATION_PROGRESS + 1;
    private static final int STEP_DISK_INITIALIZATION_SUCCESS = STEP_DISK_INITIALIZATION_FAIL + 1;
    private static final int STEP_DISK_EXPAND = STEP_DISK_INITIALIZATION_SUCCESS + 1;
    private static final int MODE_MAXIMUM_CAPACITY = 0;
    private static final int MODE_DUAL_DISK_MUTUAL_BACKUP = MODE_MAXIMUM_CAPACITY + 1;
    private static final int MAIN_STORAGE_M2_HIGH_SPEED = 0;
    private static final int MAIN_STORAGE_DISK_1 = MAIN_STORAGE_M2_HIGH_SPEED + 1;
    private static final int MAIN_STORAGE_DISK_2 = MAIN_STORAGE_DISK_1 + 1;
    private static final int MAIN_STORAGE_DISK_1_AND_2 = MAIN_STORAGE_DISK_2 + 1;
    private static final int EXPAND_DISK_1 = 0;
    private static final int EXPAND_DISK_2 = EXPAND_DISK_1 + 1;
    private static final int EXPAND_DISK_SSD = EXPAND_DISK_2 + 1;
    private String activityId;
    private int mDiskFunction;
    private int mStepIndex;
    // 磁盘初始化的值，以及磁盘扩容前的值
    private int mDiskIndex;
    // 磁盘扩容后的值
    private int mExpandDiskIndex;
    private int mModeIndex = -1;
    private int mMainStorageIndex = -1;
    // 仅用于是否重绘视图
    private int vDiskExpandIndex = -1;
    private boolean mEncryption = true;
    private RelativeLayout titleContainer;
    private TextView title;
    private ImageButton back;
    private Button functionText;
    private ScrollView diskInitializationScroll;
    private FrameLayout titleBlankContainer;
    private LinearLayout titleScrollContainer;
    private ImageView titleHeaderImage;
    private TextView titleHeaderText;
    private TextView titleHeaderIntroduction;
    private LinearLayout disk1And2Container;
    private RelativeLayout disk1Container;
    private ImageView disk1Image;
    private ImageView disk1Exception;
    private RelativeLayout disk2Container;
    private ImageView disk2Image;
    private ImageView disk2Exception;
    private RelativeLayout diskSsdContainer;
    private ImageView diskSsdException;
    private LinearLayout diskExpand1And2Container;
    private ImageView diskExpandBefore1Image;
    private ImageView diskExpandBefore2Image;
    private TextView diskExpandBefore1Text;
    private TextView diskExpandBefore2Text;
    private ImageView diskExpandAfter1Image;
    private ImageView diskExpandAfter2Image;
    private TextView diskExpandAfter1Text;
    private TextView diskExpandAfter2Text;
    private LinearLayout diskExpandSsdContainer;
    private LinearLayout diskExpandSsdBeforeContainer;
    private ImageView diskExpandSsdBeforeImage;
    private TextView diskExpandSsdBeforeText;
    private LinearLayout diskExpandSsdAfterContainer;
    private ImageView diskExpandSsdAfterImage;
    private TextView diskExpandSsdAfterText;
    private LinearLayout diskNoMainStorageContainer;
    private TextView noDiskText;
    private LinearLayout fastDiskInitializationContainer;
    private TextView fastStorageMode;
    private TextView fastMainStorage;
    private TextView fastDiskEncryption;
    private LinearLayout diskInformationContainer;
    private TextView disk1Information;
    private TextView disk2Information;
    private View diskSsdSplit;
    private LinearLayout diskSsdInformationContainer;
    private TextView diskSsdInformation;
    private LinearLayout diskModeContainer;
    private LinearLayout diskMainStorageChooseContainer;
    private LinearLayout mainStorageContainer;
    private LinearLayout diskEncryptionContainer;
    private CheckBox diskEncryptionSwitch;
    private TextView diskFormatText;
    private LinearLayout diskInitializationProgressContainer;
    private TextView diskInitializationProgressNumber;
    private ProgressBar diskInitializationProgress;
    private TextView diskInitializationProgressHint;
    private LinearLayout diskInitializationFailContainer;
    private TextView diskInitializationFailTitle;
    private TextView diskInitializationFailContent;
    private RelativeLayout diskInitializationSuccessContainer;
    private TextView diskInitializationSuccessTitle;
    private ImageView diskInitializationSuccessEncryptionIndicator;
    private TextView diskInitializationSuccess1Title;
    private ImageView diskInitializationSuccess1Main;
    private TextView diskInitializationSuccess2Title;
    private ImageView diskInitializationSuccess2Main;
    private TextView diskInitializationSuccess1Content;
    private TextView diskInitializationSuccess2Content;
    private TextView diskInitializationSuccessModeTitle;
    private TextView diskInitializationSuccessM2Title;
    private ImageView diskInitializationSuccessM2Main;
    private TextView diskInitializationSuccessModeContent;
    private TextView diskInitializationSuccessM2Content;
    private TextView diskInitializationSuccessModeHint;
    private LinearLayout diskExpandContainer;
    private LinearLayout oneButtonContainer;
    private TextView oneButtonText;
    private LottieAnimationView oneButtonLoading;
    private LinearLayout oneButton;
    private LinearLayout twoButtonsContainer;
    private Button twoButtons1;
    private Button twoButtons2;
    private LinearLayout twoButtonsVerticalContainer;
    private Button twoButtonsVertical1;
    private Button twoButtonsVertical2;
    private View maximumCapacityModeItem;
    private ImageView maximumCapacityModeItemSelectIndicator;
    private View dualDiskMutualBackupModeItem;
    private ImageView dualDiskMutualBackupModeItemSelectIndicator;
    private View dualDiskMutualBackupModeConfiguredItem;
    private View m2HighSpeedStorageItem;
    private ImageView m2HighSpeedStorageItemSelectIndicator;
    private View disk1Item;
    private ImageView disk1ItemSelectIndicator;
    private View disk2Item;
    private ImageView disk2ItemSelectIndicator;
    private View disk1And2Item;
    private ImageView disk1And2ItemSelectIndicator;
    private View diskExpand1Item;
    private ImageView diskExpand1ItemSelectIndicator;
    private View diskExpand2Item;
    private ImageView diskExpand2ItemSelectIndicator;
    private View diskExpandSsdItem;
    private ImageView diskExpandSsdItemSelectIndicator;
    private TextView diskExpandSsdItemDescription;
    private Dialog formatReminderDialog;
    private TextView formatReminderDialogTitle;
    private TextView formatReminderDialogContent;
    private Button formatReminderDialogCancel;
    private Button formatReminderDialogConfirm;
    private Dialog shutdownReminderDialog;
    private TextView shutdownReminderDialogTitle;
    private TextView shutdownReminderDialogContent;
    private Button shutdownReminderDialogConfirm;
    private Button shutdownReminderDialogCancel;
    private DiskInitializationHandler mHandler;
    private DiskRecognitionResult mDiskRecognitionResult;
    private Integer mDiskInitialize = null;
    private Integer mDiskExpand = null;
    private boolean isNoMainStorage;
    private String mBoxUuid;
    private Map<Integer, String> ssdInformationMap;
    private Map<Integer, String> diskInformationMap;
    private Map<Integer, String> ssdHardwareIdMap;
    private Map<Integer, String> diskHardwareIdMap;
//    private DiskInitializeBridge mBridge;
    private AODeviceDiscoveryManager mManager;
    private long mExitTime = 0L;
    private boolean spaceTokenReady = false;
    private boolean waitingTokenReady = false;

    // 可扩容的磁盘
    private int expandDiskTotalIndex = 0;
    // 已选择的扩容磁盘
    private int expandDiskIndex = 0;
    private Integer expandRaidType = null;
    private String mRequestUuid;

    static class DiskInitializationHandler extends Handler {
        private WeakReference<DiskInitializationActivity> diskInitializationActivityWeakReference;

        public DiskInitializationHandler(DiskInitializationActivity activity) {
            diskInitializationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            DiskInitializationActivity activity = diskInitializationActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case MESSAGE_PROGRESS_TEST:
                        if (activity.diskInitializationProgress != null) {
                            int progress = activity.diskInitializationProgress.getProgress();
                            int nowProgress = progress + (int) Math.round(50 * Math.random());
                            if (nowProgress < 100) {
                                activity.handleDiskInitializationProgressCallback(nowProgress);
                                sendEmptyMessageDelayed(MESSAGE_PROGRESS_TEST, ConstantField.TimeUnit.SECOND_UNIT);
                            } else {
                                activity.diskStep(STEP_DISK_INITIALIZATION_SUCCESS);
                                activity.handleDiskInitializationSuccess("test1", "test2", "test3");
                            }
                        }
                        break;
                    case MESSAGE_PROGRESS_REQUEST:
//                        if (activity.mBridge != null) {
//                            activity.mBridge.requestDiskInitializeProgress();
//                        }
                        if (activity.mManager != null) {
                            activity.mManager.request(activity.activityId, UUID.randomUUID().toString()
                                    , AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS, null);
                        }
                        break;
                    case MESSAGE_DISK_INITIALIZATION_SUCCESS:
                        switch (activity.mDiskFunction) {
                            case ConstantField.DiskFunction.DISK_EXPAND:
                                activity.requestDiskManagementList();
                                break;
                            default:
//                                if (activity.mBridge != null) {
//                                    activity.mBridge.requestDiskManagementList();
//                                }
                                if (activity.mManager != null) {
                                    activity.mManager.request(activity.activityId, UUID.randomUUID().toString()
                                            , AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST, null);
                                }
                                break;
                        }
                        break;
                    case MESSAGE_ACCESS_TOKEN_REQUEST:
                        activity.obtainAccessToken();
                        break;
                    case MESSAGE_EXPAND_PROGRESS_REQUEST:
                        if (activity.presenter != null) {
                            activity.presenter.getDiskExpandProgress();
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_disk_initialization);
        // 标题栏
        titleContainer = findViewById(R.id.title_container);
        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
        functionText = findViewById(R.id.function_text);
        diskInitializationScroll = findViewById(R.id.disk_initialization_scroll);
        titleBlankContainer = findViewById(R.id.title_blank_container);
        titleScrollContainer = findViewById(R.id.title_scroll_container);
        titleHeaderImage = findViewById(R.id.title_header_image);
        titleHeaderText = findViewById(R.id.title_header_text);
        titleHeaderIntroduction = findViewById(R.id.title_header_introduction);
        // 磁盘公共区域
        disk1And2Container = findViewById(R.id.disk_1_and_2_container);
        disk1Container = findViewById(R.id.disk_1_container);
        disk1Image = findViewById(R.id.disk_1_image);
        disk1Exception = findViewById(R.id.disk_1_exception);
        disk2Container = findViewById(R.id.disk_2_container);
        disk2Image = findViewById(R.id.disk_2_image);
        disk2Exception = findViewById(R.id.disk_2_exception);
        diskSsdContainer = findViewById(R.id.disk_ssd_container);
        diskSsdException = findViewById(R.id.disk_ssd_exception);
        // 磁盘扩容公共区域
        diskExpand1And2Container = findViewById(R.id.disk_expand_1_and_2_container);
        diskExpandBefore1Image = findViewById(R.id.disk_expand_before_1_image);
        diskExpandBefore2Image = findViewById(R.id.disk_expand_before_2_image);
        diskExpandBefore1Text = findViewById(R.id.disk_expand_before_1_text);
        diskExpandBefore2Text = findViewById(R.id.disk_expand_before_2_text);
        diskExpandAfter1Image = findViewById(R.id.disk_expand_after_1_image);
        diskExpandAfter2Image = findViewById(R.id.disk_expand_after_2_image);
        diskExpandAfter1Text = findViewById(R.id.disk_expand_after_1_text);
        diskExpandAfter2Text = findViewById(R.id.disk_expand_after_2_text);
        diskExpandSsdContainer = findViewById(R.id.disk_expand_ssd_container);
        diskExpandSsdBeforeContainer = findViewById(R.id.disk_expand_ssd_before_container);
        diskExpandSsdBeforeImage = findViewById(R.id.disk_expand_ssd_before_image);
        diskExpandSsdBeforeText = findViewById(R.id.disk_expand_ssd_before_text);
        diskExpandSsdAfterContainer = findViewById(R.id.disk_expand_ssd_after_container);
        diskExpandSsdAfterImage = findViewById(R.id.disk_expand_ssd_after_image);
        diskExpandSsdAfterText = findViewById(R.id.disk_expand_ssd_after_text);
        // 无主存储
        diskNoMainStorageContainer = findViewById(R.id.disk_no_main_storage_container);
        // 无磁盘
        noDiskText = findViewById(R.id.no_disk_text);
        // 快速设置
        fastDiskInitializationContainer = findViewById(R.id.fast_disk_initialization_container);
        fastStorageMode = findViewById(R.id.fast_storage_mode);
        fastMainStorage = findViewById(R.id.fast_main_storage);
        fastDiskEncryption = findViewById(R.id.fast_disk_encryption);
        // 磁盘信息
        diskInformationContainer = findViewById(R.id.disk_information_container);
        disk1Information = findViewById(R.id.disk_1_information);
        disk2Information = findViewById(R.id.disk_2_information);
        diskSsdSplit = findViewById(R.id.disk_ssd_split);
        diskSsdInformationContainer = findViewById(R.id.disk_ssd_information_container);
        diskSsdInformation = findViewById(R.id.disk_ssd_information);
        // 存储模式
        diskModeContainer = findViewById(R.id.disk_mode_container);
        // 主存储选择
        diskMainStorageChooseContainer = findViewById(R.id.disk_main_storage_choose_container);
        mainStorageContainer = findViewById(R.id.main_storage_container);
        // 磁盘加密
        diskEncryptionContainer = findViewById(R.id.disk_encryption_container);
        diskEncryptionSwitch = findViewById(R.id.disk_encryption_switch);
        // 格式化
        diskFormatText = findViewById(R.id.disk_format_text);
        // 初始化过程
        diskInitializationProgressContainer = findViewById(R.id.disk_initialization_progress_container);
        diskInitializationProgressNumber = findViewById(R.id.disk_initialization_progress_number);
        diskInitializationProgress = findViewById(R.id.disk_initialization_progress);
        diskInitializationProgressHint = findViewById(R.id.disk_initialization_progress_hint);
        // 初始化失败
        diskInitializationFailContainer = findViewById(R.id.disk_initialization_fail_container);
        diskInitializationFailTitle = findViewById(R.id.disk_initialization_fail_title);
        diskInitializationFailContent = findViewById(R.id.disk_initialization_fail_content);
        // 初始化成功
        diskInitializationSuccessContainer = findViewById(R.id.disk_initialization_success_container);
        diskInitializationSuccessTitle = findViewById(R.id.disk_initialization_success_title);
        diskInitializationSuccessEncryptionIndicator = findViewById(R.id.disk_initialization_success_encryption_indicator);
        diskInitializationSuccess1Title = findViewById(R.id.disk_initialization_success_1_title);
        diskInitializationSuccess1Main = findViewById(R.id.disk_initialization_success_1_main);
        diskInitializationSuccess2Title = findViewById(R.id.disk_initialization_success_2_title);
        diskInitializationSuccess2Main = findViewById(R.id.disk_initialization_success_2_main);
        diskInitializationSuccess1Content = findViewById(R.id.disk_initialization_success_1_content);
        diskInitializationSuccess2Content = findViewById(R.id.disk_initialization_success_2_content);
        diskInitializationSuccessModeTitle = findViewById(R.id.disk_initialization_success_mode_title);
        diskInitializationSuccessM2Title = findViewById(R.id.disk_initialization_success_m2_title);
        diskInitializationSuccessM2Main = findViewById(R.id.disk_initialization_success_m2_main);
        diskInitializationSuccessModeContent = findViewById(R.id.disk_initialization_success_mode_content);
        diskInitializationSuccessM2Content = findViewById(R.id.disk_initialization_success_m2_content);
        diskInitializationSuccessModeHint = findViewById(R.id.disk_initialization_success_mode_hint);
        // 选择扩容
        diskExpandContainer = findViewById(R.id.disk_expand_container);
        // 按钮区域
        oneButtonContainer = findViewById(R.id.one_button_container);
        oneButton = findViewById(R.id.loading_button_container);
        oneButtonLoading = findViewById(R.id.loading_animation);
        oneButtonText = findViewById(R.id.loading_content);
        twoButtonsContainer = findViewById(R.id.two_buttons_container);
        twoButtons1 = findViewById(R.id.two_buttons_1);
        twoButtons2 = findViewById(R.id.two_buttons_2);
        twoButtonsVerticalContainer = findViewById(R.id.two_buttons_vertical_container);
        twoButtonsVertical1 = findViewById(R.id.two_buttons_vertical_1);
        twoButtonsVertical2 = findViewById(R.id.two_buttons_vertical_2);

        View formatReminderDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        formatReminderDialogTitle = formatReminderDialogView.findViewById(R.id.dialog_title);
        formatReminderDialogContent = formatReminderDialogView.findViewById(R.id.dialog_content);
        formatReminderDialogCancel = formatReminderDialogView.findViewById(R.id.dialog_cancel);
        formatReminderDialogConfirm = formatReminderDialogView.findViewById(R.id.dialog_confirm);
        formatReminderDialog = new Dialog(this, R.style.EulixDialog);
        formatReminderDialog.setCancelable(false);
        formatReminderDialog.setContentView(formatReminderDialogView);

        View shutdownReminderDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        shutdownReminderDialogTitle = shutdownReminderDialogView.findViewById(R.id.dialog_title);
        shutdownReminderDialogContent = shutdownReminderDialogView.findViewById(R.id.dialog_content);
        shutdownReminderDialogCancel = shutdownReminderDialogView.findViewById(R.id.dialog_cancel);
        shutdownReminderDialogConfirm = shutdownReminderDialogView.findViewById(R.id.dialog_confirm);
        shutdownReminderDialog = new Dialog(this, R.style.EulixDialog);
        shutdownReminderDialog.setCancelable(false);
        shutdownReminderDialog.setContentView(shutdownReminderDialogView);
    }

    @Override
    public void initData() {
        mHandler = new DiskInitializationHandler(this);
        Intent intent = getIntent();
        if (intent != null) {
            mDiskFunction = intent.getIntExtra(ConstantField.DISK_FUNCTION, ConstantField.DiskFunction.DISK_INITIALIZE);
            isNoMainStorage = intent.getBooleanExtra(ConstantField.DISK_INITIALIZE_NO_MAIN_STORAGE, false);
            if (intent.hasExtra(ConstantField.DATA_UUID)) {
                String dataUuid = intent.getStringExtra(ConstantField.DATA_UUID);
                if (dataUuid != null) {
                    String data = DataUtil.getData(dataUuid);
                    if (data != null) {
                        try {
                            mDiskRecognitionResult = new Gson().fromJson(data, DiskRecognitionResult.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            mDiskInitialize = intent.getIntExtra(ConstantField.DISK_INITIALIZE, ReadyCheckResult.DISK_INITIALIZE_ERROR);
            if (intent.hasExtra(ConstantField.BOX_UUID)) {
                mBoxUuid = intent.getStringExtra(ConstantField.BOX_UUID);
            }
            int chooseDiskIndex = intent.getIntExtra(ConstantField.DISK_INDEX, 0);
            if (chooseDiskIndex != 0) {
                expandDiskIndex = 1;
                while (chooseDiskIndex > 0) {
                    expandDiskIndex = (expandDiskIndex << 1);
                    chooseDiskIndex -= 1;
                }
            }
            mDiskExpand = intent.getIntExtra(ConstantField.DISK_EXPAND, DiskExpandProgressResult.CODE_EXPAND_ERROR);
        }
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
//        mBridge = DiskInitializeBridge.getInstance();
//        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        functionText.setText(R.string.run_background);
        titleContainer.setBackgroundColor(Color.WHITE);
        setTitleVisibility(false);
        setTitlePattern(false);
        switch (mDiskFunction) {
            case ConstantField.DiskFunction.DISK_EXPAND:
                back.setVisibility(View.VISIBLE);
                title.setText(R.string.disk_expand);
                diskFormatText.setText(R.string.disk_expand_format_hint);
                diskInitializationProgressHint.setTextColor(getResources().getColor(R.color.black_ff333333));
                diskInitializationSuccessTitle.setText(R.string.disk_expand_complete);
                break;
            default:
                back.setVisibility(View.GONE);
                title.setText(R.string.disk_initialization);
                diskFormatText.setText(R.string.disk_format_hint);
                diskInitializationProgressHint.setTextColor(getResources().getColor(R.color.red_fff6222d));
                diskInitializationSuccessTitle.setText(R.string.disk_initialization_complete);
                break;
        }
        int maxDiskInitializationSuccessTitleWidth = Math.max(((ViewUtils.getScreenWidth(this)
                - getResources().getDimensionPixelSize(R.dimen.dp_50)) / 2) - getResources().getDimensionPixelSize(R.dimen.dp_77), 0);
        diskInitializationSuccess1Title.setMaxWidth(maxDiskInitializationSuccessTitleWidth);
        diskInitializationSuccess2Title.setMaxWidth(maxDiskInitializationSuccessTitleWidth);
        diskInitializationSuccessM2Title.setMaxWidth(maxDiskInitializationSuccessTitleWidth);

        formatReminderDialogTitle.setText(R.string.format_reminder);
        formatReminderDialogContent.setText(R.string.format_reminder_content);
        formatReminderDialogConfirm.setText(R.string.format);
        formatReminderDialogConfirm.setTypeface(Typeface.DEFAULT_BOLD);

        shutdownReminderDialogTitle.setText(R.string.shutdown_prompt);
        shutdownReminderDialogContent.setText(R.string.shutdown_prompt_content);
        shutdownReminderDialogCancel.setText(R.string.return_back);
        shutdownReminderDialogConfirm.setText(R.string.shutdown);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        functionText.setOnClickListener(this);
        oneButton.setOnClickListener(this);
        twoButtons1.setOnClickListener(this);
        twoButtons2.setOnClickListener(this);
        twoButtonsVertical1.setOnClickListener(this);
        twoButtonsVertical2.setOnClickListener(this);
        diskEncryptionSwitch.setOnCheckedChangeListener(this);
        switch (mDiskFunction) {
            case ConstantField.DiskFunction.DISK_EXPAND:
                if (generateDiskExpandIndexAndInfo()) {
                    setDiskExpandView();
                    diskStep(STEP_DISK_INITIALIZATION_PROGRESS);
                    handleStepDiskInitializationProgress();
                    handleRefreshExpandProgressEvent(0);
                } else {
                    setDiskExpandView();
                    diskStep(STEP_DISK_EXPAND);
                    handleStepDiskExpand();
                    setOneButtonLoadingPattern((expandDiskIndex > 0), false, null);
                }
                break;
            default:
                generateDiskIndexAndInfo();
                setDiskView();
                if (isNoMainStorage) {
                    diskStep(STEP_NO_MAIN_STORAGE);
                } else if (mDiskIndex == 0) {
                    diskStep(STEP_NO_DISK);
                } else if (mDiskInitialize != null) {
                    switch (mDiskInitialize) {
                        case ReadyCheckResult.DISK_NORMAL:
                            diskStep(STEP_DISK_INITIALIZATION_PROGRESS);
                            handleDiskInitializationProgressCallback(100);
                            handleDiskInitializationSuccessEvent(0);
                            break;
                        case ReadyCheckResult.DISK_UNINITIALIZED:
                            diskStep(STEP_FAST_DISK_INITIALIZATION);
                            handleStepFastDiskInitialization();
                            break;
                        case ReadyCheckResult.DISK_INITIALIZING:
                        case ReadyCheckResult.DISK_DATA_SYNCHRONIZATION:
                            diskStep(STEP_DISK_INITIALIZATION_PROGRESS);
                            handleStepDiskInitializationProgress();
                            handleRefreshInitializeProgressEvent(0);
                            break;
                        default:
                            break;
                    }
                } else {
                    mDiskInitialize = ReadyCheckResult.DISK_INITIALIZE_ERROR;
                    diskStep(STEP_DISK_INITIALIZATION_FAIL);
                    handleDiskInitializationFail();
                }
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            diskInitializationScroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> setTitleVisibility(Math.abs(scrollY) < getResources().getDimensionPixelSize(R.dimen.dp_121)));
        }

        formatReminderDialogCancel.setOnClickListener(v -> dismissFormatReminderDialog());
        formatReminderDialogConfirm.setOnClickListener(v -> {
            dismissFormatReminderDialog();
            diskStep(STEP_DISK_INITIALIZATION_PROGRESS);
            handleDiskInitializationEvent();
        });

        shutdownReminderDialogCancel.setOnClickListener(v -> dismissShutdownReminderDialog());
        shutdownReminderDialogConfirm.setOnClickListener(v -> {
            dismissShutdownReminderDialog();
            handleShutdownEvent();
            if (mManager != null) {
                mManager.finishSource();
            }
            finish();
        });
    }

    private void setTitleVisibility(boolean isScrollToTop) {
        if (mStepIndex == STEP_FAST_DISK_INITIALIZATION) {
            titleContainer.setVisibility(isScrollToTop ? View.GONE : View.VISIBLE);
        } else {
            titleContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setTitlePattern(boolean isTitleHeader) {
        if (isTitleHeader) {
            titleBlankContainer.setVisibility(View.GONE);
            titleScrollContainer.setVisibility(View.VISIBLE);
            titleHeaderImage.setImageResource(R.drawable.image_disk_initialization_2x);
            titleHeaderText.setText(R.string.disk_initialization);
            titleHeaderIntroduction.setVisibility(View.VISIBLE);
            titleHeaderIntroduction.setText(R.string.disk_initialization_fast_introduction);
        } else {
            titleHeaderIntroduction.setText("");
            titleHeaderIntroduction.setVisibility(View.GONE);
            titleScrollContainer.setVisibility(View.GONE);
            titleBlankContainer.setVisibility(View.VISIBLE);
        }
    }

    private int generateDiskType(Integer busNumber, Integer transportType) {
        int diskType = 0;
        if (busNumber != null) {
            switch (busNumber) {
                case DiskInfo.BUS_NUMBER_SATA_4:
                    diskType = 1;
                    break;
                case DiskInfo.BUS_NUMBER_SATA_8:
                    diskType = 2;
                    break;
                case DiskInfo.BUS_NUMBER_M2:
                    diskType = -1;
                    break;
                default:
                    break;
            }
        } else if (transportType != null) {
            switch (transportType) {
                case 1:
                    diskType = 1;
                    break;
                case 2:
                    diskType = 2;
                    break;
                case 3:
                    diskType = -1;
                    break;
                default:
                    break;
            }
        }
        return diskType;
    }

    private void generateDiskIndexAndInfo() {
        mDiskIndex = 0;
        if (mDiskRecognitionResult != null) {
            List<DiskInfo> diskInfoList = mDiskRecognitionResult.getDiskInfos();
            if (diskInfoList != null) {
                for (DiskInfo diskInfo : diskInfoList) {
                    if (diskInfo != null) {
                        String displayName = diskInfo.getDisplayName();
                        String deviceModel = diskInfo.getDeviceModel();
                        String modelNumber = diskInfo.getModelNumber();
                        String hardwareId = diskInfo.getHwId();
                        int diskType = generateDiskType(diskInfo.getBusNumber(), diskInfo.getTransportType());
                        switch (diskType) {
                            case 1:
                                if (((mDiskIndex % 4) >> 1) == 0) {
                                    mDiskIndex += 2;
                                }
                                if (diskInformationMap == null) {
                                    diskInformationMap = new HashMap<>();
                                }
                                if (diskHardwareIdMap == null) {
                                    diskHardwareIdMap = new HashMap<>();
                                }
                                if (StringUtil.isNonBlankString(displayName)) {
                                    diskInformationMap.put(1, displayName);
                                } else if (StringUtil.isNonBlankString(deviceModel)) {
                                    diskInformationMap.put(1, deviceModel);
                                } else if (modelNumber != null) {
                                    diskInformationMap.put(1, modelNumber);
                                }
                                if (hardwareId != null) {
                                    diskHardwareIdMap.put(1, hardwareId);
                                }
                                break;
                            case 2:
                                if ((mDiskIndex >> 2) == 0) {
                                    mDiskIndex += 4;
                                }
                                if (diskInformationMap == null) {
                                    diskInformationMap = new HashMap<>();
                                }
                                if (diskHardwareIdMap == null) {
                                    diskHardwareIdMap = new HashMap<>();
                                }
                                if (StringUtil.isNonBlankString(displayName)) {
                                    diskInformationMap.put(2, displayName);
                                } else if (StringUtil.isNonBlankString(deviceModel)) {
                                    diskInformationMap.put(2, deviceModel);
                                } else if (modelNumber != null) {
                                    diskInformationMap.put(2, modelNumber);
                                }
                                if (hardwareId != null) {
                                    diskHardwareIdMap.put(2, hardwareId);
                                }
                                break;
                            case -1:
                                if ((mDiskIndex % 2) == 0) {
                                    mDiskIndex += 1;
                                }
                                if (ssdInformationMap == null) {
                                    ssdInformationMap = new HashMap<>();
                                }
                                if (ssdHardwareIdMap == null) {
                                    ssdHardwareIdMap = new HashMap<>();
                                }
                                if (StringUtil.isNonBlankString(displayName)) {
                                    ssdInformationMap.put(1, displayName);
                                } else if (StringUtil.isNonBlankString(deviceModel)) {
                                    ssdInformationMap.put(1, deviceModel);
                                } else if (modelNumber != null) {
                                    ssdInformationMap.put(1, modelNumber);
                                }
                                if (hardwareId != null) {
                                    ssdHardwareIdMap.put(1, hardwareId);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    private boolean generateDiskExpandIndexAndInfo() {
        boolean isExpanding = false;
        mDiskIndex = 0;
        mExpandDiskIndex = expandDiskIndex;
        DiskManageListResult result = null;
        if (presenter != null) {
            result = presenter.getActiveDiskManageListResult();
        }
        if (result != null) {
            List<DiskManageInfo> diskManageInfoList = result.getDiskManageInfos();
            expandRaidType = result.getRaidType();
            if (diskManageInfoList != null) {
                for (DiskManageInfo diskManageInfo : diskManageInfoList) {
                    if (diskManageInfo != null) {
                        Integer diskExceptionValue = diskManageInfo.getDiskException();
                        String hardwareId = diskManageInfo.getHwId();
                        Boolean isDiskExpand = null;
                        if (diskExceptionValue != null) {
                            switch (diskExceptionValue) {
                                case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_NOT_EXPAND:
                                case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_EXPAND_ERROR:
                                    isDiskExpand = false;
                                    break;
                                case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_EXPANDING:
                                    isDiskExpand = true;
                                    break;
                                default:
                                    break;
                            }
                        }
                        int diskType = generateDiskType(diskManageInfo.getBusNumber(), diskManageInfo.getTransportType());
                        switch (diskType) {
                            case 1:
                                if (isDiskExpand == null) {
                                    if (((mDiskIndex % 4) >> 1) == 0) {
                                        mDiskIndex += 2;
                                    }
                                    if (((mExpandDiskIndex % 4) >> 1) == 0) {
                                        mExpandDiskIndex += 2;
                                    }
                                } else {
                                    if (((expandDiskTotalIndex % 4) >> 1) == 0) {
                                        expandDiskTotalIndex += 2;
                                    }
                                    if (isDiskExpand) {
                                        if (((expandDiskIndex % 4) >> 1) == 0) {
                                            expandDiskIndex += 2;
                                        }
                                        isExpanding = true;
                                    }
                                }
                                if (diskHardwareIdMap == null) {
                                    diskHardwareIdMap = new HashMap<>();
                                }
                                if (hardwareId != null) {
                                    diskHardwareIdMap.put(1, hardwareId);
                                }
                                break;
                            case 2:
                                if (isDiskExpand == null) {
                                    if ((mDiskIndex >> 2) == 0) {
                                        mDiskIndex += 4;
                                    }
                                    if ((mExpandDiskIndex >> 2) == 0) {
                                        mExpandDiskIndex += 4;
                                    }
                                } else {
                                    if ((expandDiskTotalIndex >> 2) == 0) {
                                        expandDiskTotalIndex += 4;
                                    }
                                    if (isDiskExpand) {
                                        if ((expandDiskIndex >> 2) == 0) {
                                            expandDiskIndex += 4;
                                        }
                                        isExpanding = true;
                                    }
                                }
                                if (diskHardwareIdMap == null) {
                                    diskHardwareIdMap = new HashMap<>();
                                }
                                if (hardwareId != null) {
                                    diskHardwareIdMap.put(2, hardwareId);
                                }
                                break;
                            case -1:
                                if (isDiskExpand == null) {
                                    if ((mDiskIndex % 2) == 0) {
                                        mDiskIndex += 1;
                                    }
                                    if ((mExpandDiskIndex % 2) == 0) {
                                        mExpandDiskIndex += 1;
                                    }
                                } else {
                                    if ((expandDiskTotalIndex % 2) == 0) {
                                        expandDiskTotalIndex += 1;
                                    }
                                    if (isDiskExpand) {
                                        if ((expandDiskIndex % 2) == 0) {
                                            expandDiskIndex += 1;
                                        }
                                        isExpanding = true;
                                    }
                                }
                                if (ssdHardwareIdMap == null) {
                                    ssdHardwareIdMap = new HashMap<>();
                                }
                                if (hardwareId != null) {
                                    ssdHardwareIdMap.put(1, hardwareId);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        return isExpanding;
    }

    private void generateMaximumCapacityModeItem() {
        if (maximumCapacityModeItem == null || maximumCapacityModeItemSelectIndicator == null) {
            maximumCapacityModeItem = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            maximumCapacityModeItemSelectIndicator = maximumCapacityModeItem.findViewById(R.id.item_select_indicator);
            TextView maximumCapacityModeItemTitle = maximumCapacityModeItem.findViewById(R.id.item_title);
            TextView maximumCapacityModeItemDescription = maximumCapacityModeItem.findViewById(R.id.item_description);
            maximumCapacityModeItemDescription.setVisibility(View.VISIBLE);
            maximumCapacityModeItemTitle.setText(R.string.maximum_capacity_mode);
            maximumCapacityModeItemDescription.setText(R.string.maximum_capacity_mode_content);
        }
    }

    private void generateDualDiskMutualBackupModeItem() {
        if (dualDiskMutualBackupModeItem == null || dualDiskMutualBackupModeItemSelectIndicator == null) {
            dualDiskMutualBackupModeItem = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            dualDiskMutualBackupModeItemSelectIndicator = dualDiskMutualBackupModeItem.findViewById(R.id.item_select_indicator);
            TextView dualDiskMutualBackupModeItemTitle = dualDiskMutualBackupModeItem.findViewById(R.id.item_title);
            TextView dualDiskMutualBackupModeItemDescription = dualDiskMutualBackupModeItem.findViewById(R.id.item_description);
            dualDiskMutualBackupModeItemDescription.setVisibility(View.VISIBLE);
            dualDiskMutualBackupModeItemTitle.setText(R.string.dual_disk_mutual_backup_mode);
            dualDiskMutualBackupModeItemDescription.setText(R.string.dual_disk_mutual_backup_mode_content);
        }
    }

    private void generateDualDiskMutualBackupModeConfiguredItem() {
        if (dualDiskMutualBackupModeConfiguredItem == null) {
            dualDiskMutualBackupModeConfiguredItem = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            ImageView dualDiskMutualBackupModeConfiguredItemSelectIndicator = dualDiskMutualBackupModeConfiguredItem.findViewById(R.id.item_select_indicator);
            TextView dualDiskMutualBackupModeConfiguredItemTitle = dualDiskMutualBackupModeConfiguredItem.findViewById(R.id.item_title);
            TextView dualDiskMutualBackupModeConfiguredItemDescription = dualDiskMutualBackupModeConfiguredItem.findViewById(R.id.item_description);
            dualDiskMutualBackupModeConfiguredItemDescription.setVisibility(View.VISIBLE);
            dualDiskMutualBackupModeConfiguredItemSelectIndicator.setImageResource(R.drawable.icon_radio_button_on_disable_2x);
            dualDiskMutualBackupModeConfiguredItemTitle.setText(R.string.dual_disk_mutual_backup_mode);
            dualDiskMutualBackupModeConfiguredItemDescription.setText(R.string.dual_disk_mutual_backup_mode_configured_content);
        }
    }

    private void generateM2HighSpeedStorageItem() {
        if (m2HighSpeedStorageItem == null || m2HighSpeedStorageItemSelectIndicator == null) {
            m2HighSpeedStorageItem = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            m2HighSpeedStorageItemSelectIndicator = m2HighSpeedStorageItem.findViewById(R.id.item_select_indicator);
            TextView m2HighSpeedStorageItemTitle = m2HighSpeedStorageItem.findViewById(R.id.item_title);
            TextView m2HighSpeedStorageItemRecommend = m2HighSpeedStorageItem.findViewById(R.id.item_recommend);
            m2HighSpeedStorageItemTitle.setText(R.string.m2_high_speed_storage);
            m2HighSpeedStorageItemRecommend.setVisibility(View.VISIBLE);
            int[] sizeArray = ViewUtils.measureTextView(m2HighSpeedStorageItemRecommend);
            int xOffset = 0;
            if (sizeArray != null && sizeArray.length > 1) {
                xOffset = sizeArray[0];
            }
            m2HighSpeedStorageItemTitle.setMaxWidth(Math.max((ViewUtils.getScreenWidth(this)
                    - getResources().getDimensionPixelSize(R.dimen.dp_99) - xOffset), 0));
        }
    }

    private void generateDisk1Item() {
        if (disk1Item == null || disk1ItemSelectIndicator == null) {
            disk1Item = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            disk1ItemSelectIndicator = disk1Item.findViewById(R.id.item_select_indicator);
            TextView disk1ItemTitle = disk1Item.findViewById(R.id.item_title);
            TextView disk1ItemRecommend = disk1Item.findViewById(R.id.item_recommend);
            disk1ItemTitle.setText(R.string.disk_1);
            disk1ItemRecommend.setVisibility(View.GONE);
        }
    }

    private void generateDisk2Item() {
        if (disk2Item == null || disk2ItemSelectIndicator == null) {
            disk2Item = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            disk2ItemSelectIndicator = disk2Item.findViewById(R.id.item_select_indicator);
            TextView disk2ItemTitle = disk2Item.findViewById(R.id.item_title);
            TextView disk2ItemRecommend = disk2Item.findViewById(R.id.item_recommend);
            disk2ItemTitle.setText(R.string.disk_2);
            disk2ItemRecommend.setVisibility(View.GONE);
        }
    }

    private void generateDisk1And2Item() {
        if (disk1And2Item == null || disk1And2ItemSelectIndicator == null) {
            disk1And2Item = LayoutInflater.from(this).inflate(R.layout.item_disk_initialization, null);
            disk1And2ItemSelectIndicator = disk1And2Item.findViewById(R.id.item_select_indicator);
            TextView disk1And2ItemTitle = disk1And2Item.findViewById(R.id.item_title);
            TextView disk1And2ItemRecommend = disk1And2Item.findViewById(R.id.item_recommend);
            disk1And2ItemTitle.setText(R.string.disk_1_and_2);
            disk1And2ItemRecommend.setVisibility(View.GONE);
        }
    }

    private void generateDiskExpand1Item() {
        if (diskExpand1Item == null || diskExpand1ItemSelectIndicator == null) {
            diskExpand1Item = LayoutInflater.from(this).inflate(R.layout.item_disk_expand, null);
            diskExpand1ItemSelectIndicator = diskExpand1Item.findViewById(R.id.item_select_indicator);
            TextView diskExpand1ItemTitle = diskExpand1Item.findViewById(R.id.item_title);
            TextView diskExpand1ItemDescription = diskExpand1Item.findViewById(R.id.item_description);
            diskExpand1ItemTitle.setText(R.string.disk_1_expand);
            diskExpand1ItemDescription.setVisibility(View.GONE);
            diskExpand1ItemTitle.setMaxWidth(Math.max((ViewUtils.getScreenWidth(this)
                    - getResources().getDimensionPixelSize(R.dimen.dp_90)), 0));
        }
    }

    private void generateDiskExpand2Item() {
        if (diskExpand2Item == null || diskExpand2ItemSelectIndicator == null) {
            diskExpand2Item = LayoutInflater.from(this).inflate(R.layout.item_disk_expand, null);
            diskExpand2ItemSelectIndicator = diskExpand2Item.findViewById(R.id.item_select_indicator);
            TextView diskExpand2ItemTitle = diskExpand2Item.findViewById(R.id.item_title);
            TextView diskExpand2ItemDescription = diskExpand2Item.findViewById(R.id.item_description);
            diskExpand2ItemTitle.setText(R.string.disk_2_expand);
            diskExpand2ItemDescription.setVisibility(View.GONE);
            diskExpand2ItemTitle.setMaxWidth(Math.max((ViewUtils.getScreenWidth(this)
                    - getResources().getDimensionPixelSize(R.dimen.dp_90)), 0));
        }
    }

    private void generateDiskExpandSsdItem() {
        if (diskExpandSsdItem == null || diskExpandSsdItemSelectIndicator == null || diskExpandSsdItemDescription == null) {
            diskExpandSsdItem = LayoutInflater.from(this).inflate(R.layout.item_disk_expand, null);
            diskExpandSsdItemSelectIndicator = diskExpandSsdItem.findViewById(R.id.item_select_indicator);
            TextView diskExpandSsdItemTitle = diskExpandSsdItem.findViewById(R.id.item_title);
            diskExpandSsdItemDescription = diskExpandSsdItem.findViewById(R.id.item_description);
            diskExpandSsdItemTitle.setText(R.string.m2_expand);
            diskExpandSsdItemDescription.setText(R.string.m2_expand_hint);
            diskExpandSsdItemDescription.setVisibility(((expandDiskIndex % 2) == 0 ? View.GONE : View.VISIBLE));
            diskExpandSsdItemTitle.setMaxWidth(Math.max((ViewUtils.getScreenWidth(this)
                    - getResources().getDimensionPixelSize(R.dimen.dp_90)), 0));
            diskExpandSsdItemDescription.setMaxWidth(Math.max((ViewUtils.getScreenWidth(this)
                    - getResources().getDimensionPixelSize(R.dimen.dp_90)), 0));
        }
    }

    @NonNull
    private View generateSplit() {
        View split = new View(this);
        split.setBackgroundColor(getResources().getColor(R.color.white_fff7f7f9));
        return split;
    }

    @NonNull
    private ViewGroup.LayoutParams generateSplitLinearLayoutParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.dp_1));
    }

    @NonNull
    private ViewGroup.LayoutParams generateItemLinearLayoutParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void setSelectIndicator(@Nullable ImageView imageView, boolean isSelect) {
        if (imageView != null) {
            imageView.setImageResource(isSelect ? R.drawable.icon_radio_button_on_2x : R.drawable.icon_radio_button_off_2x);
        }
    }

    private void setMultiSelectIndicator(@Nullable ImageView imageView, boolean isSelect) {
        if (imageView != null) {
            imageView.setImageResource(isSelect ? R.drawable.icon_radio_button_multi_on_2x : R.drawable.icon_radio_button_off_2x);
        }
    }

    /**
     * @param isClickable 是否可点击
     * @param isLoading 是否在加载
     * @param text 按钮文案，如果是null则不变动
     */
    private void setOneButtonLoadingPattern(boolean isClickable, boolean isLoading, @Nullable String text) {
        if (oneButton != null) {
            oneButton.setClickable(isClickable);
            oneButton.setBackgroundResource((isClickable || isLoading) ? R.drawable.background_ff337aff_ff16b9ff_rectangle_10
                    : R.drawable.background_ffdfe0e5_rectangle_10);
        }
        if (oneButtonLoading != null) {
            if (isLoading) {
                oneButtonLoading.setVisibility(View.VISIBLE);
                LottieUtil.loop(oneButtonLoading, "loading_button.json");
            } else {
                LottieUtil.stop(oneButtonLoading);
                oneButtonLoading.setVisibility(View.GONE);
            }
        }
        if (text != null && oneButtonText != null) {
            oneButtonText.setText(text);
        }
    }

    private void setDiskView() {
        setDiskView(mDiskIndex);
    }

    private void setDiskView(int diskIndex) {
        boolean isFormatError = false;
        switch (mDiskFunction) {
            case ConstantField.DiskFunction.DISK_EXPAND:
                isFormatError = (mDiskExpand != null && mDiskExpand == DiskExpandProgressResult.CODE_EXPAND_ERROR);
                break;
            default:
                isFormatError = (mDiskInitialize != null && mDiskInitialize == ReadyCheckResult.DISK_FORMAT_ERROR);
                break;
        }
        boolean isDiskSsdExist = ((diskIndex % 2) != 0);
        boolean isDisk1Exist = (((diskIndex % 4) >> 1) != 0);
        boolean isDisk2Exist = ((diskIndex >> 2) != 0);
        if (diskExpand1And2Container != null) {
            diskExpand1And2Container.setVisibility(View.GONE);
        }
        if (diskExpandSsdContainer != null) {
            diskExpandSsdContainer.setVisibility(View.GONE);
        }
        if (disk1And2Container != null) {
            disk1And2Container.setVisibility(View.VISIBLE);
        }
        if (disk1Container != null && disk1Image != null) {
            disk1Container.setBackgroundResource(isDisk1Exist ? (isFormatError ? R.drawable.background_ffffe3e4_rectangle_10
                    : R.drawable.background_ffedf3ff_rectangle_10) : R.drawable.background_fff5f6fa_rectangle_10);
            disk1Image.setImageResource(isDisk1Exist ? R.drawable.disk_recognize_on_2x : R.drawable.disk_recognize_off_2x);
            disk1Exception.setVisibility(isFormatError ? View.VISIBLE : View.GONE);
        }
        if (disk2Container != null && disk2Image != null) {
            disk2Container.setBackgroundResource(isDisk2Exist ? (isFormatError ? R.drawable.background_ffffe3e4_rectangle_10
                    : R.drawable.background_ffedf3ff_rectangle_10) : R.drawable.background_fff5f6fa_rectangle_10);
            disk2Image.setImageResource(isDisk2Exist ? R.drawable.disk_recognize_on_2x : R.drawable.disk_recognize_off_2x);
            disk2Exception.setVisibility(isFormatError ? View.VISIBLE : View.GONE);
        }
        if (diskSsdContainer != null) {
            diskSsdContainer.setVisibility(isDiskSsdExist ? View.VISIBLE : View.GONE);
            if (isDiskSsdExist) {
                diskSsdContainer.setBackgroundResource(isFormatError ? R.drawable.background_ffffe3e4_rectangle_10
                        : R.drawable.background_ffedf3ff_rectangle_10);
                diskSsdException.setVisibility(isFormatError ? View.VISIBLE : View.GONE);
            }
        }
        mModeIndex = -1;
        mMainStorageIndex = -1;
    }

    private void setDiskExpandView() {
        boolean isDiskSsdBeforeExist = ((mDiskIndex % 2) != 0);
        boolean isDisk1BeforeExist = (((mDiskIndex % 4) >> 1) != 0);
        boolean isDisk2BeforeExist = ((mDiskIndex >> 2) != 0);
        boolean isDiskSsdAfterExist = ((mExpandDiskIndex % 2) != 0);
        boolean isDisk1AfterExist = (((mExpandDiskIndex % 4) >> 1) != 0);
        boolean isDisk2AfterExist = ((mExpandDiskIndex >> 2) != 0);
        boolean isDiskSsdTotalExist = (isDiskSsdBeforeExist || ((expandDiskTotalIndex % 2) != 0));
        if (disk1And2Container != null) {
            disk1And2Container.setVisibility(View.GONE);
        }
        if (diskSsdContainer != null) {
            diskSsdContainer.setVisibility(View.GONE);
        }
        if (diskExpand1And2Container != null) {
            diskExpand1And2Container.setVisibility(View.VISIBLE);
        }
        if (diskExpandBefore1Image != null && diskExpandBefore1Text != null) {
            diskExpandBefore1Image.setImageResource(isDisk1BeforeExist ? R.drawable.disk_recognize_on_2x : R.drawable.disk_recognize_off_2x);
            diskExpandBefore1Text.setTextColor(getResources().getColor(isDisk1BeforeExist ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
        }
        if (diskExpandBefore2Image != null && diskExpandBefore2Text != null) {
            diskExpandBefore2Image.setImageResource(isDisk2BeforeExist ? R.drawable.disk_recognize_on_2x : R.drawable.disk_recognize_off_2x);
            diskExpandBefore2Text.setTextColor(getResources().getColor(isDisk2BeforeExist ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
        }
        if (diskExpandAfter1Image != null && diskExpandAfter1Text != null) {
            diskExpandAfter1Image.setImageResource(isDisk1AfterExist ? R.drawable.disk_recognize_on_2x : R.drawable.disk_recognize_off_2x);
            diskExpandAfter1Text.setTextColor(getResources().getColor(isDisk1AfterExist ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
        }
        if (diskExpandAfter2Image != null && diskExpandAfter2Text != null) {
            diskExpandAfter2Image.setImageResource(isDisk2AfterExist ? R.drawable.disk_recognize_on_2x : R.drawable.disk_recognize_off_2x);
            diskExpandAfter2Text.setTextColor(getResources().getColor(isDisk2AfterExist ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
        }
        if (diskExpandSsdContainer != null) {
            diskExpandSsdContainer.setVisibility(isDiskSsdTotalExist ? View.VISIBLE : View.GONE);
            if (isDiskSsdTotalExist) {
                if (diskExpandSsdBeforeContainer != null && diskExpandSsdBeforeImage != null && diskExpandSsdBeforeText != null) {
                    diskExpandSsdBeforeContainer.setBackgroundResource(isDiskSsdBeforeExist ? R.drawable.background_fff5f6fa_rectangle_10 : R.drawable.background_ffffffff_rectangle_10_stroke_1_ffdfe0e5);
                    diskExpandSsdBeforeImage.setImageResource(isDiskSsdBeforeExist ? R.drawable.m2_ssd_2x : R.drawable.m2_ssd_recognize_off_2x);
                    diskExpandSsdBeforeText.setTextColor(getResources().getColor(isDiskSsdBeforeExist ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
                }
                if (diskExpandSsdAfterImage != null && diskExpandSsdAfterText != null) {
                    diskExpandSsdAfterImage.setImageResource(isDiskSsdAfterExist ? R.drawable.m2_ssd_2x : R.drawable.m2_ssd_recognize_off_2x);
                    diskExpandSsdAfterText.setTextColor(getResources().getColor(isDiskSsdAfterExist ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
                }
            }
        }
        mModeIndex = -1;
    }

    private void diskStep(int stepIndex) {
        mStepIndex = stepIndex;
        setTitleVisibility(diskInitializationScroll != null && Math.abs(diskInitializationScroll.getScrollY()) < getResources().getDimensionPixelSize(R.dimen.dp_121));
        setTitlePattern(stepIndex == STEP_FAST_DISK_INITIALIZATION);
        functionText.setVisibility((ConstantField.DiskFunction.DISK_INITIALIZE == mDiskFunction
                && stepIndex == STEP_DISK_INITIALIZATION_PROGRESS) ? View.VISIBLE : View.GONE);
        diskNoMainStorageContainer.setVisibility(stepIndex == STEP_NO_MAIN_STORAGE ? View.VISIBLE : View.GONE);
        noDiskText.setVisibility(stepIndex == STEP_NO_DISK ? View.VISIBLE : View.GONE);
        fastDiskInitializationContainer.setVisibility(stepIndex == STEP_FAST_DISK_INITIALIZATION ? View.VISIBLE : View.GONE);
        diskInformationContainer.setVisibility(stepIndex == STEP_DISK_INFORMATION ? View.VISIBLE : View.GONE);
        diskModeContainer.setVisibility(stepIndex == STEP_DISK_MODE ? View.VISIBLE : View.GONE);
        diskMainStorageChooseContainer.setVisibility(stepIndex == STEP_DISK_MAIN_STORAGE ? View.VISIBLE : View.GONE);
        diskEncryptionContainer.setVisibility(stepIndex == STEP_DISK_ENCRYPTION ? View.VISIBLE : View.GONE);
        diskFormatText.setVisibility(stepIndex == STEP_DISK_FORMAT ? View.VISIBLE : View.GONE);
        diskInitializationProgressContainer.setVisibility(stepIndex == STEP_DISK_INITIALIZATION_PROGRESS ? View.VISIBLE : View.GONE);
        diskInitializationFailContainer.setVisibility(stepIndex == STEP_DISK_INITIALIZATION_FAIL ? View.VISIBLE : View.GONE);
        diskInitializationSuccessContainer.setVisibility(stepIndex == STEP_DISK_INITIALIZATION_SUCCESS ? View.VISIBLE : View.GONE);
        diskExpandContainer.setVisibility(stepIndex == STEP_DISK_EXPAND ? View.VISIBLE : View.GONE);
        switch (stepIndex) {
            case STEP_NO_MAIN_STORAGE:
            case STEP_NO_DISK:
                twoButtonsContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                oneButtonContainer.setVisibility(View.VISIBLE);
                setOneButtonLoadingPattern(true, false, getString(R.string.shutdown));
                break;
            case STEP_FAST_DISK_INITIALIZATION:
                oneButtonContainer.setVisibility(View.GONE);
                twoButtonsContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.VISIBLE);
                twoButtonsVertical1.setText(R.string.string_continue);
                twoButtonsVertical2.setText(R.string.custom_settings);
                break;
            case STEP_DISK_INITIALIZATION_FAIL:
                twoButtonsContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                oneButtonContainer.setVisibility(View.VISIBLE);
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        setOneButtonLoadingPattern(true, false, getString(R.string.exit_expand));
                        break;
                    default:
                        setOneButtonLoadingPattern(true, false, getString(R.string.shutdown));
                        break;
                }
                break;
            case STEP_DISK_INFORMATION:
                oneButtonContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                twoButtonsContainer.setVisibility(View.VISIBLE);
                twoButtons1.setText(R.string.shutdown);
                twoButtons2.setText(R.string.next_step);
                break;
            case STEP_DISK_MODE:
            case STEP_DISK_MAIN_STORAGE:
            case STEP_DISK_ENCRYPTION:
                oneButtonContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                twoButtonsContainer.setVisibility(View.VISIBLE);
                twoButtons1.setText(R.string.previous_step);
                twoButtons2.setText(R.string.next_step);
                break;
            case STEP_DISK_FORMAT:
                oneButtonContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                twoButtonsContainer.setVisibility(View.VISIBLE);
                twoButtons1.setText(R.string.previous_step);
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        twoButtons2.setText(R.string.format);
                        break;
                    default:
                        twoButtons2.setText(R.string.initialization);
                        break;
                }
                break;
            case STEP_DISK_INITIALIZATION_PROGRESS:
                twoButtonsContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                oneButtonContainer.setVisibility(View.GONE);
                break;
            case STEP_DISK_INITIALIZATION_SUCCESS:
                twoButtonsContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                oneButtonContainer.setVisibility(View.VISIBLE);
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        setOneButtonLoadingPattern(true, false, getString(R.string.done));
                        break;
                    default:
                        setOneButtonLoadingPattern(true, false, getString(R.string.enter_ao_space));
                        break;
                }
                break;
            case STEP_DISK_EXPAND:
                twoButtonsContainer.setVisibility(View.GONE);
                twoButtonsVerticalContainer.setVisibility(View.GONE);
                oneButtonContainer.setVisibility(View.VISIBLE);
                setOneButtonLoadingPattern(true, false, getString(R.string.next_step));
                break;
            default:
                break;
        }
    }

    private void handleStepFastDiskInitialization() {
        boolean isDiskSsdExist = ((mDiskIndex % 2) != 0);
        boolean isDisk1Exist = (((mDiskIndex % 4) >> 1) != 0);
        boolean isDisk2Exist = ((mDiskIndex >> 2) != 0);
        if (isDiskSsdExist || isDisk1Exist || isDisk2Exist) {
            boolean isDualDiskExist = (isDisk1Exist && isDisk2Exist);
            mModeIndex = (isDualDiskExist ? MODE_DUAL_DISK_MUTUAL_BACKUP : MODE_MAXIMUM_CAPACITY);
            if (isDiskSsdExist) {
                mMainStorageIndex = MAIN_STORAGE_M2_HIGH_SPEED;
            } else if (isDualDiskExist) {
                mMainStorageIndex = MAIN_STORAGE_DISK_1_AND_2;
            } else if (isDisk1Exist) {
                mMainStorageIndex = MAIN_STORAGE_DISK_1;
            } else if (isDisk2Exist) {
                mMainStorageIndex = MAIN_STORAGE_DISK_2;
            }
            mEncryption = true;
        }
        if (fastStorageMode != null) {
            switch (mModeIndex) {
                case MODE_MAXIMUM_CAPACITY:
                    fastStorageMode.setText(R.string.maximum_capacity_mode);
                    break;
                case MODE_DUAL_DISK_MUTUAL_BACKUP:
                    fastStorageMode.setText(R.string.dual_disk_mutual_backup_mode);
                    break;
                default:
                    break;
            }
        }
        if (fastMainStorage != null) {
            switch (mMainStorageIndex) {
                case MAIN_STORAGE_M2_HIGH_SPEED:
                    fastMainStorage.setText(R.string.m2_high_speed_storage);
                    break;
                case MAIN_STORAGE_DISK_1:
                    fastMainStorage.setText(R.string.disk_1);
                    break;
                case MAIN_STORAGE_DISK_2:
                    fastMainStorage.setText(R.string.disk_2);
                    break;
                case MAIN_STORAGE_DISK_1_AND_2:
                    fastMainStorage.setText(R.string.disk_1_and_2);
                    break;
                default:
                    break;
            }
        }
        if (fastDiskEncryption != null) {
            fastDiskEncryption.setText((mEncryption ? R.string.yes : R.string.no));
        }
    }

    private void handleStepDiskInformation() {
        String disk1Info = null;
        String disk2Info = null;
        String diskSsdInfo = null;
        if (ssdInformationMap != null && ssdInformationMap.containsKey(1)) {
            diskSsdInfo = ssdInformationMap.get(1);
        }
        if (diskInformationMap != null) {
            if (diskInformationMap.containsKey(1)) {
                disk1Info = diskInformationMap.get(1);
            }
            if (diskInformationMap.containsKey(2)) {
                disk2Info = diskInformationMap.get(2);
            }
        }
        boolean isDiskSsdExist = ((mDiskIndex % 2) != 0);
        boolean isDisk1Exist = (((mDiskIndex % 4) >> 1) != 0);
        boolean isDisk2Exist = ((mDiskIndex >> 2) != 0);
        if (disk1Information != null) {
            disk1Information.setText((isDisk1Exist && disk1Info != null) ? disk1Info : ConstantField.NOT_APPLICABLE);
        }
        if (disk2Information != null) {
            disk2Information.setText((isDisk2Exist && disk2Info != null) ? disk2Info : ConstantField.NOT_APPLICABLE);
        }
        if (diskSsdSplit != null) {
            diskSsdSplit.setVisibility(isDiskSsdExist ? View.VISIBLE : View.GONE);
        }
        if (diskSsdInformationContainer != null) {
            diskSsdInformationContainer.setVisibility(isDiskSsdExist ? View.VISIBLE : View.GONE);
        }
        if (diskSsdInformation != null) {
            diskSsdInformation.setText((isDiskSsdExist && diskSsdInfo != null) ? diskSsdInfo : ConstantField.NOT_APPLICABLE);
        }
    }

    private void handleStepDiskExpand() {
        if (vDiskExpandIndex != expandDiskIndex) {
            vDiskExpandIndex = expandDiskIndex;
            boolean isDiskSsdExpand = ((expandDiskTotalIndex % 2) != 0);
            boolean isDisk1Expand = (((expandDiskTotalIndex % 4) >> 1) != 0);
            boolean isDisk2Expand = ((expandDiskTotalIndex >> 2) != 0);
            boolean isAddItem = false;
            if (diskExpandContainer != null) {
                diskExpandContainer.removeAllViews();
            }
            if (isDisk1Expand) {
                generateDiskExpand1Item();
                if (diskExpandContainer != null) {
                    diskExpandContainer.addView(diskExpand1Item, generateItemLinearLayoutParams());
                    isAddItem = true;
                }
            }
            if (isDisk2Expand) {
                generateDiskExpand2Item();
                if (diskExpandContainer != null) {
                    if (isAddItem) {
                        diskExpandContainer.addView(generateSplit(), generateSplitLinearLayoutParams());
                    }
                    diskExpandContainer.addView(diskExpand2Item, generateItemLinearLayoutParams());
                    isAddItem = true;
                }
            }
            if (isDiskSsdExpand) {
                generateDiskExpandSsdItem();
                if (diskExpandContainer != null) {
                    if (isAddItem) {
                        diskExpandContainer.addView(generateSplit(), generateSplitLinearLayoutParams());
                    }
                    diskExpandContainer.addView(diskExpandSsdItem, generateItemLinearLayoutParams());
                }
            }
            setMultiSelectIndicator(diskExpand1ItemSelectIndicator, (((expandDiskIndex % 4) >> 1) != 0));
            setMultiSelectIndicator(diskExpand2ItemSelectIndicator, ((expandDiskIndex >> 2) != 0));
            setMultiSelectIndicator(diskExpandSsdItemSelectIndicator, ((expandDiskIndex % 2) != 0));
            if (diskExpand1Item != null) {
                diskExpand1Item.setOnClickListener(v -> {
                    if (((expandDiskIndex % 4) >> 1) == 0) {
                        expandDiskIndex += 2;
                        if (((mExpandDiskIndex % 4) >> 1) == 0) {
                            mExpandDiskIndex += 2;
                            setDiskExpandView();
                        }
                        setMultiSelectIndicator(diskExpand1ItemSelectIndicator, true);
                    } else {
                        expandDiskIndex -= 2;
                        if (((mExpandDiskIndex % 4) >> 1) != 0) {
                            mExpandDiskIndex -= 2;
                            setDiskExpandView();
                        }
                        setMultiSelectIndicator(diskExpand1ItemSelectIndicator, false);
                    }
                    setOneButtonLoadingPattern((expandDiskIndex > 0), false, null);
                    vDiskExpandIndex = expandDiskIndex;
                    mModeIndex = -1;
                });
            }
            if (diskExpand2Item != null) {
                diskExpand2Item.setOnClickListener(v -> {
                    if ((expandDiskIndex >> 2) == 0) {
                        expandDiskIndex += 4;
                        if ((mExpandDiskIndex >> 2) == 0) {
                            mExpandDiskIndex += 4;
                            setDiskExpandView();
                        }
                        setMultiSelectIndicator(diskExpand2ItemSelectIndicator, true);
                    } else {
                        expandDiskIndex -= 4;
                        if ((mExpandDiskIndex >> 2) != 0) {
                            mExpandDiskIndex -= 4;
                            setDiskExpandView();
                        }
                        setMultiSelectIndicator(diskExpand2ItemSelectIndicator, false);
                    }
                    setOneButtonLoadingPattern((expandDiskIndex > 0), false, null);
                    vDiskExpandIndex = expandDiskIndex;
                    mModeIndex = -1;
                });
            }
            if (diskExpandSsdItem != null) {
                diskExpandSsdItem.setOnClickListener(v -> {
                    if ((expandDiskIndex % 2) == 0) {
                        expandDiskIndex += 1;
                        if ((mExpandDiskIndex % 2) == 0) {
                            mExpandDiskIndex += 1;
                            setDiskExpandView();
                        }
                        setMultiSelectIndicator(diskExpandSsdItemSelectIndicator, true);
                        if (diskExpandSsdItemDescription != null) {
                            diskExpandSsdItemDescription.setVisibility(View.VISIBLE);
                        }
                    } else {
                        expandDiskIndex -= 1;
                        if ((mExpandDiskIndex % 2) != 0) {
                            mExpandDiskIndex -= 1;
                            setDiskExpandView();
                        }
                        setMultiSelectIndicator(diskExpandSsdItemSelectIndicator, false);
                        if (diskExpandSsdItemDescription != null) {
                            diskExpandSsdItemDescription.setVisibility(View.GONE);
                        }
                    }
                    setOneButtonLoadingPattern((expandDiskIndex > 0), false, null);
                    vDiskExpandIndex = expandDiskIndex;
                    mModeIndex = -1;
                });
            }
        }
    }

    private void handleStepDiskMode() {
        // 设置默认选择
        if (mModeIndex != MODE_MAXIMUM_CAPACITY && mModeIndex != MODE_DUAL_DISK_MUTUAL_BACKUP) {
            boolean isDisk1Exist = (((mDiskIndex % 4) >> 1) != 0);
            boolean isDisk2Exist = ((mDiskIndex >> 2) != 0);
            boolean isDualDiskMutualBackupModeConfigured = false;
            boolean isDualDiskMutualBackupModeEnable = false;
            switch (mDiskFunction) {
                case ConstantField.DiskFunction.DISK_EXPAND:
                    isDualDiskMutualBackupModeConfigured = (expandRaidType != null && expandRaidType == 2);
                    isDualDiskMutualBackupModeEnable = ((!isDisk1Exist && !isDisk2Exist)
                            && (((mExpandDiskIndex % 4) >> 1) != 0) && ((mExpandDiskIndex >> 2) != 0));
                    break;
                default:
                    isDualDiskMutualBackupModeEnable = (isDisk1Exist && isDisk2Exist);
                    break;
            }
            if (diskModeContainer != null) {
                diskModeContainer.removeAllViews();
            }
            if (isDualDiskMutualBackupModeConfigured) {
                generateDualDiskMutualBackupModeConfiguredItem();
                if (diskModeContainer != null) {
                    diskModeContainer.addView(dualDiskMutualBackupModeConfiguredItem, generateItemLinearLayoutParams());
                }
                // todo 暂时保持接口不变
//                mModeIndex = MODE_DUAL_DISK_MUTUAL_BACKUP;
                mModeIndex = MODE_MAXIMUM_CAPACITY;
                mMainStorageIndex = -1;
            } else {
                generateMaximumCapacityModeItem();
                if (diskModeContainer != null) {
                    diskModeContainer.addView(maximumCapacityModeItem, generateItemLinearLayoutParams());
                }
                if (isDualDiskMutualBackupModeEnable) {
                    generateDualDiskMutualBackupModeItem();
                    if (diskModeContainer != null) {
                        diskModeContainer.addView(generateSplit(), generateSplitLinearLayoutParams());
                        diskModeContainer.addView(dualDiskMutualBackupModeItem, generateItemLinearLayoutParams());
                    }
                }
                setSelectIndicator(maximumCapacityModeItemSelectIndicator, true);
                setSelectIndicator(dualDiskMutualBackupModeItemSelectIndicator, false);
                mModeIndex = MODE_MAXIMUM_CAPACITY;
                mMainStorageIndex = -1;
                maximumCapacityModeItem.setOnClickListener(v -> {
                    setSelectIndicator(dualDiskMutualBackupModeItemSelectIndicator, false);
                    setSelectIndicator(maximumCapacityModeItemSelectIndicator, true);
                    mModeIndex = MODE_MAXIMUM_CAPACITY;
                    mMainStorageIndex = -1;
                });
                if (dualDiskMutualBackupModeItem != null) {
                    dualDiskMutualBackupModeItem.setOnClickListener(v -> {
                        setSelectIndicator(maximumCapacityModeItemSelectIndicator, false);
                        setSelectIndicator(dualDiskMutualBackupModeItemSelectIndicator, true);
                        mModeIndex = MODE_DUAL_DISK_MUTUAL_BACKUP;
                        mMainStorageIndex = -1;
                    });
                }
            }
        }
    }

    private void handleStepDiskMainStorage() {
        // 设置默认选择
        if (mMainStorageIndex < 0) {
            boolean isDiskSsdExist = ((mDiskIndex % 2) != 0);
            boolean isDisk1Exist = (((mDiskIndex % 4) >> 1) != 0);
            boolean isDisk2Exist = ((mDiskIndex >> 2) != 0);
            boolean isAddItem = false;
            int index = -1;
            if (mainStorageContainer != null) {
                mainStorageContainer.removeAllViews();
            }
            if (isDiskSsdExist) {
                generateM2HighSpeedStorageItem();
                if (mainStorageContainer != null) {
                    mainStorageContainer.addView(m2HighSpeedStorageItem, generateItemLinearLayoutParams());
                    isAddItem = true;
                }
                index = MAIN_STORAGE_M2_HIGH_SPEED;
            }
            if (isDisk1Exist && isDisk2Exist && mModeIndex == MODE_DUAL_DISK_MUTUAL_BACKUP) {
                generateDisk1And2Item();
                if (mainStorageContainer != null) {
                    if (isAddItem) {
                        mainStorageContainer.addView(generateSplit(), generateSplitLinearLayoutParams());
                    }
                    mainStorageContainer.addView(disk1And2Item, generateItemLinearLayoutParams());
                }
                if (index < 0) {
                    index = MAIN_STORAGE_DISK_1_AND_2;
                }
            } else {
                if (isDisk1Exist) {
                    generateDisk1Item();
                    if (mainStorageContainer != null) {
                        if (isAddItem) {
                            mainStorageContainer.addView(generateSplit(), generateSplitLinearLayoutParams());
                        }
                        mainStorageContainer.addView(disk1Item, generateItemLinearLayoutParams());
                        isAddItem = true;
                    }
                    if (index < 0) {
                        index = MAIN_STORAGE_DISK_1;
                    }
                }
                if (isDisk2Exist) {
                    generateDisk2Item();
                    if (mainStorageContainer != null) {
                        if (isAddItem) {
                            mainStorageContainer.addView(generateSplit(), generateSplitLinearLayoutParams());
                        }
                        mainStorageContainer.addView(disk2Item, generateItemLinearLayoutParams());
                    }
                    if (index < 0) {
                        index = MAIN_STORAGE_DISK_2;
                    }
                }
            }
            setSelectIndicator(m2HighSpeedStorageItemSelectIndicator, (index == MAIN_STORAGE_M2_HIGH_SPEED));
            setSelectIndicator(disk1And2ItemSelectIndicator, (index == MAIN_STORAGE_DISK_1_AND_2));
            setSelectIndicator(disk1ItemSelectIndicator, (index == MAIN_STORAGE_DISK_1));
            setSelectIndicator(disk2ItemSelectIndicator, (index == MAIN_STORAGE_DISK_2));
            mMainStorageIndex = index;
            if (m2HighSpeedStorageItem != null) {
                m2HighSpeedStorageItem.setOnClickListener(v -> {
                    setSelectIndicator(disk1And2ItemSelectIndicator, false);
                    setSelectIndicator(disk1ItemSelectIndicator, false);
                    setSelectIndicator(disk2ItemSelectIndicator, false);
                    setSelectIndicator(m2HighSpeedStorageItemSelectIndicator, true);
                    mMainStorageIndex = MAIN_STORAGE_M2_HIGH_SPEED;
                });
            }
            if (disk1And2Item != null) {
                disk1And2Item.setOnClickListener(v -> {
                    setSelectIndicator(m2HighSpeedStorageItemSelectIndicator, false);
                    setSelectIndicator(disk1ItemSelectIndicator, false);
                    setSelectIndicator(disk2ItemSelectIndicator, false);
                    setSelectIndicator(disk1And2ItemSelectIndicator, true);
                    mMainStorageIndex = MAIN_STORAGE_DISK_1_AND_2;
                });
            }
            if (disk1Item != null) {
                disk1Item.setOnClickListener(v -> {
                    setSelectIndicator(m2HighSpeedStorageItemSelectIndicator, false);
                    setSelectIndicator(disk2ItemSelectIndicator, false);
                    setSelectIndicator(disk1And2ItemSelectIndicator, false);
                    setSelectIndicator(disk1ItemSelectIndicator, true);
                    mMainStorageIndex = MAIN_STORAGE_DISK_1;
                });
            }
            if (disk2Item != null) {
                disk2Item.setOnClickListener(v -> {
                    setSelectIndicator(m2HighSpeedStorageItemSelectIndicator, false);
                    setSelectIndicator(disk1ItemSelectIndicator, false);
                    setSelectIndicator(disk1And2ItemSelectIndicator, false);
                    setSelectIndicator(disk2ItemSelectIndicator, true);
                    mMainStorageIndex = MAIN_STORAGE_DISK_2;
                });
            }
        }
    }

    private void handleStepDiskEncryption() {
        if (diskEncryptionSwitch != null) {
            diskEncryptionSwitch.setOnCheckedChangeListener(null);
            diskEncryptionSwitch.setChecked(mEncryption);
            diskEncryptionSwitch.setOnCheckedChangeListener(this);
        }
    }

    private void handleStepDiskInitializationProgress() {
        handleDiskInitializationProgressCallback(0);
    }

    private void handleDiskInitializationProgressCallback(int progress) {
        progress = Math.max(Math.min(progress, 100), 0);
        if (diskInitializationProgressNumber != null) {
            String progressContent = progress + "%";
            diskInitializationProgressNumber.setText(progressContent);
        }
        if (diskInitializationProgress != null) {
            diskInitializationProgress.setProgress(progress);
        }
    }

    private void handleDiskInitializationFail() {
        switch (mDiskFunction) {
            case ConstantField.DiskFunction.DISK_EXPAND:
                back.setVisibility(View.GONE);
                diskInitializationFailTitle.setText(R.string.disk_expand_fail_title);
                diskInitializationFailContent.setText(R.string.disk_expand_fail_content);
                break;
            default:
                boolean isHandle = false;
                if (mDiskInitialize != null) {
                    isHandle = true;
                    if (mDiskInitialize == ReadyCheckResult.DISK_FORMAT_ERROR) {
                        diskInitializationFailTitle.setText(R.string.disk_initialization_fail_fault_title);
                        diskInitializationFailContent.setText(R.string.disk_initialization_fail_fault_content);
                    } else if (mDiskInitialize >= ReadyCheckResult.DISK_INITIALIZE_ERROR) {
                        diskInitializationFailTitle.setText(R.string.disk_initialization_fail_progress_error_title);
                        String errContent = getString(R.string.disk_initialization_fail_progress_error_content) + mDiskInitialize;
                        diskInitializationFailContent.setText(errContent);
                    } else {
                        isHandle = false;
                    }
                }
                if (!isHandle) {
                    diskInitializationFailTitle.setText(R.string.disk_initialization_fail_progress_unknown_error_title);
                    diskInitializationFailContent.setText("");
                }
                break;
        }
    }

    private void handleDiskInitializationSuccess(String disk1Info, String disk2Info, String diskSsdInfo) {
        handleDiskInitializationSuccess(disk1Info, disk2Info, diskSsdInfo, mEncryption, mModeIndex, mMainStorageIndex);
    }

    private void handleDiskInitializationSuccess(String disk1Info, String disk2Info, String diskSsdInfo, boolean isDiskEncrypt, int modeIndex, int mainStorageIndex) {
        boolean isDiskSsdExist = ((mDiskIndex % 2) != 0);
        boolean isDisk1Exist = (((mDiskIndex % 4) >> 1) != 0);
        boolean isDisk2Exist = ((mDiskIndex >> 2) != 0);
        if (diskInitializationSuccessEncryptionIndicator != null) {
            diskInitializationSuccessEncryptionIndicator.setVisibility(isDiskEncrypt ? View.VISIBLE : View.GONE);
        }
        if (diskInitializationSuccessM2Title != null) {
            diskInitializationSuccessM2Title.setVisibility(isDiskSsdExist ? View.VISIBLE : View.GONE);
        }
        if (diskInitializationSuccess1Main != null) {
            diskInitializationSuccess1Main.setVisibility(((mainStorageIndex == MAIN_STORAGE_DISK_1
                    || mainStorageIndex == MAIN_STORAGE_DISK_1_AND_2) && isDisk1Exist) ? View.VISIBLE : View.GONE);
        }
        if (diskInitializationSuccess2Main != null) {
            diskInitializationSuccess2Main.setVisibility(((mainStorageIndex == MAIN_STORAGE_DISK_2
                    || mainStorageIndex == MAIN_STORAGE_DISK_1_AND_2) && isDisk2Exist) ? View.VISIBLE : View.GONE);
        }
        if (diskInitializationSuccessM2Main != null) {
            diskInitializationSuccessM2Main.setVisibility((mainStorageIndex == MAIN_STORAGE_M2_HIGH_SPEED
                    && isDiskSsdExist) ? View.VISIBLE : View.GONE);
        }
        if (diskInitializationSuccess1Content != null) {
            diskInitializationSuccess1Content.setText((disk1Info != null && isDisk1Exist) ? disk1Info : "--");
        }
        if (diskInitializationSuccess2Content != null) {
            diskInitializationSuccess2Content.setText((disk2Info != null && isDisk2Exist) ? disk2Info : "--");
        }
        if (diskInitializationSuccessM2Content != null) {
            diskInitializationSuccessM2Content.setText((diskSsdInfo != null && isDiskSsdExist) ? diskSsdInfo : "--");
            diskInitializationSuccessM2Content.setVisibility(isDiskSsdExist ? View.VISIBLE : View.GONE);
        }
        switch (modeIndex) {
            case MODE_MAXIMUM_CAPACITY:
                if (diskInitializationSuccessModeContent != null) {
                    diskInitializationSuccessModeContent.setText(R.string.maximum_capacity);
                }
                if (diskInitializationSuccessModeHint != null) {
                    diskInitializationSuccessModeHint.setText("");
                    diskInitializationSuccessModeHint.setVisibility(View.GONE);
                }
                break;
            case MODE_DUAL_DISK_MUTUAL_BACKUP:
                if (diskInitializationSuccessModeContent != null) {
                    diskInitializationSuccessModeContent.setText(R.string.dual_disk_mutual_backup);
                }
                if (diskInitializationSuccessModeHint != null) {
                    diskInitializationSuccessModeHint.setVisibility(View.VISIBLE);
                    diskInitializationSuccessModeHint.setText(R.string.disk_1_and_2);
                }
                break;
            default:
                break;
        }
    }

    private void handleOneButtonClick() {
        switch (mStepIndex) {
            case STEP_NO_MAIN_STORAGE:
            case STEP_NO_DISK:
                showShutdownReminderDialog();
                break;
            case STEP_DISK_INITIALIZATION_FAIL:
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        finish();
                        break;
                    default:
                        showShutdownReminderDialog();
                        break;
                }
                break;
            case STEP_DISK_INITIALIZATION_SUCCESS:
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        finish();
                        break;
                    default:
                        handleEnterEulixSpaceEvent();
                        break;
                }
                break;
            case STEP_DISK_EXPAND:
                diskStep(STEP_DISK_MODE);
                handleStepDiskMode();
                break;
            default:
                break;
        }
    }

    private void handleTwoButtonPreviousClick() {
        switch (mStepIndex) {
            case STEP_FAST_DISK_INITIALIZATION:
                showFormatReminderDialog();
                break;
            case STEP_DISK_INFORMATION:
                showShutdownReminderDialog();
                break;
            case STEP_DISK_MODE:
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        diskStep(STEP_DISK_EXPAND);
                        handleStepDiskExpand();
                        setOneButtonLoadingPattern((expandDiskIndex > 0), false, null);
                        break;
                    default:
                        diskStep(STEP_DISK_INFORMATION);
                        break;
                }
                break;
            case STEP_DISK_MAIN_STORAGE:
                diskStep(STEP_DISK_MODE);
                handleStepDiskMode();
                break;
            case STEP_DISK_ENCRYPTION:
                diskStep(STEP_DISK_MAIN_STORAGE);
                handleStepDiskMainStorage();
                break;
            case STEP_DISK_FORMAT:
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        diskStep(STEP_DISK_MODE);
                        handleStepDiskMode();
                        break;
                    default:
                        diskStep(STEP_DISK_ENCRYPTION);
                        handleStepDiskEncryption();
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void handleTwoButtonNextClick() {
        switch (mStepIndex) {
            case STEP_FAST_DISK_INITIALIZATION:
                mModeIndex = -1;
                mMainStorageIndex = -1;
                mEncryption = true;
                diskStep(STEP_DISK_INFORMATION);
                handleStepDiskInformation();
                break;
            case STEP_DISK_INFORMATION:
                diskStep(STEP_DISK_MODE);
                handleStepDiskMode();
                break;
            case STEP_DISK_MODE:
                switch (mDiskFunction) {
                    case ConstantField.DiskFunction.DISK_EXPAND:
                        diskStep(STEP_DISK_FORMAT);
                        break;
                    default:
                        diskStep(STEP_DISK_MAIN_STORAGE);
                        handleStepDiskMainStorage();
                        break;
                }
                break;
            case STEP_DISK_MAIN_STORAGE:
                diskStep(STEP_DISK_ENCRYPTION);
                handleStepDiskEncryption();
                break;
            case STEP_DISK_ENCRYPTION:
                diskStep(STEP_DISK_FORMAT);
                break;
            case STEP_DISK_FORMAT:
                diskStep(STEP_DISK_INITIALIZATION_PROGRESS);
                handleDiskInitializationEvent();
                break;
            default:
                break;
        }
    }

    private void handleRefreshInitializeProgressEvent(long delayMillis) {
        if (mHandler != null) {
            while (mHandler.hasMessages(MESSAGE_PROGRESS_REQUEST)) {
                mHandler.removeMessages(MESSAGE_PROGRESS_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_PROGRESS_REQUEST, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_PROGRESS_REQUEST);
            }
        } else if (mManager != null) {
            mManager.request(activityId, UUID.randomUUID().toString()
                    , AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS, null);
        }
    }

    private void handleDiskInitializationSuccessEvent(long delayMillis) {
        if (mHandler != null) {
            while (mHandler.hasMessages(MESSAGE_DISK_INITIALIZATION_SUCCESS)) {
                mHandler.removeMessages(MESSAGE_DISK_INITIALIZATION_SUCCESS);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_DISK_INITIALIZATION_SUCCESS, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_DISK_INITIALIZATION_SUCCESS);
            }
        } else {
            switch (mDiskFunction) {
                case ConstantField.DiskFunction.DISK_EXPAND:
                    requestDiskManagementList();
                    break;
                default:
                    if (mManager != null) {
                        mManager.request(activityId, UUID.randomUUID().toString()
                                , AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST, null);
                    }
                    break;
            }
        }
    }

    private void handleRefreshExpandProgressEvent(long delayMillis) {
        if (mHandler != null) {
            while (mHandler.hasMessages(MESSAGE_EXPAND_PROGRESS_REQUEST)) {
                mHandler.removeMessages(MESSAGE_EXPAND_PROGRESS_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_EXPAND_PROGRESS_REQUEST, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_EXPAND_PROGRESS_REQUEST);
            }
        } else if (presenter != null) {
            presenter.getDiskExpandProgress();
        }
    }

    private void requestDiskManagementList() {
        EulixBoxBaseInfo eulixBoxBaseInfo = null;
        if (presenter != null) {
            eulixBoxBaseInfo = presenter.getActiveBoxBaseInfo();
        }
        if (eulixBoxBaseInfo != null) {
            mRequestUuid = UUID.randomUUID().toString();
            EventBusUtil.post(new DiskManagementListRequestEvent(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), mRequestUuid, true));
        } else {
            handleDiskInitializationSuccessEvent(5000);
        }
    }

    private void checkDiskManageListResult(DiskManageListResult result) {
        List<DiskManageInfo> diskManageInfoList = result.getDiskManageInfos();
        if (diskManageInfoList != null) {
            for (DiskManageInfo diskManageInfo : diskManageInfoList) {
                if (diskManageInfo != null) {
                    Integer diskExceptionValue = diskManageInfo.getDiskException();
                    boolean isDiskExpand = false;
                    if (diskExceptionValue != null) {
                        switch (diskExceptionValue) {
                            case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_NOT_EXPAND:
                            case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_EXPAND_ERROR:
                            case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_EXPANDING:
                                isDiskExpand = true;
                                break;
                            default:
                                break;
                        }
                    }
                    int diskType = generateDiskType(diskManageInfo.getBusNumber(), diskManageInfo.getTransportType());
                    switch (diskType) {
                        case 1:
                            if (isDiskExpand) {
                                if (((mDiskIndex % 4) >> 1) != 0) {
                                    mDiskIndex -= 2;
                                }
                            } else if (((mDiskIndex % 4) >> 1) == 0){
                                mDiskIndex += 2;
                            }
                            break;
                        case 2:
                            if (isDiskExpand) {
                                if ((mDiskIndex >> 2) != 0) {
                                    mDiskIndex -= 4;
                                }
                            } else if ((mDiskIndex >> 2) == 0) {
                                mDiskIndex += 4;
                            }
                            break;
                        case -1:
                            if (isDiskExpand) {
                                if ((mDiskIndex % 2) != 0) {
                                    mDiskIndex -= 1;
                                }
                            } else if ((mDiskIndex % 2) == 0) {
                                mDiskIndex += 1;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void handleDiskManagementList(DiskManageListResult result) {
        checkDiskManageListResult(result);
        String disk1Info = null;
        String disk2Info = null;
        String ssdInfo = null;
        boolean isDiskEncrypt = mEncryption;
        int modeIndex = mModeIndex;
        int mainStorageIndex = mMainStorageIndex;
        String disk1HardwareId = null;
        String disk2HardwareId = null;
        String ssdHardwareId = null;
        if (diskHardwareIdMap != null) {
            if (diskHardwareIdMap.containsKey(1)) {
                disk1HardwareId = diskHardwareIdMap.get(1);
            }
            if (diskHardwareIdMap.containsKey(2)) {
                disk2HardwareId = diskHardwareIdMap.get(2);
            }
        }
        if (ssdHardwareIdMap != null && ssdHardwareIdMap.containsKey(1)) {
            ssdHardwareId = ssdHardwareIdMap.get(1);
        }
        List<DiskManageInfo> diskManageInfoList = result.getDiskManageInfos();
        if (diskManageInfoList != null) {
            for (DiskManageInfo diskManageInfo : diskManageInfoList) {
                if (diskManageInfo != null) {
                    String hardwareId = diskManageInfo.getHwId();
                    if (hardwareId != null) {
                        Long spaceTotal = diskManageInfo.getSpaceTotal();
                        Long spaceUsage = diskManageInfo.getSpaceUsage();
                        StringBuilder infoBuilder = new StringBuilder();
                        infoBuilder.append((spaceUsage == null ? "-" : FormatUtil.formatSimpleSize(spaceUsage, ConstantField.SizeUnit.FORMAT_1F)));
                        infoBuilder.append(" / ");
                        infoBuilder.append((spaceTotal == null ? "-" : FormatUtil.formatSimpleSize(spaceTotal, ConstantField.SizeUnit.FORMAT_1F)));
                        if (hardwareId.equals(ssdHardwareId)) {
                            ssdInfo = infoBuilder.toString();
                        }
                        if (hardwareId.equals(disk1HardwareId)) {
                            disk1Info = infoBuilder.toString();
                        }
                        if (hardwareId.equals(disk2HardwareId)) {
                            disk2Info = infoBuilder.toString();
                        }
                    }
                }
            }
        }
        Integer diskEncrypt = result.getDiskEncrypt();
        if (diskEncrypt != null) {
            switch (diskEncrypt) {
                case 1:
                    isDiskEncrypt = true;
                    break;
                case 2:
                    isDiskEncrypt = false;
                    break;
                default:
                    break;
            }
        }
        Integer raidType = result.getRaidType();
        if (raidType != null) {
            switch (raidType) {
                case 1:
                    modeIndex = MODE_MAXIMUM_CAPACITY;
                    break;
                case 2:
                    modeIndex = MODE_DUAL_DISK_MUTUAL_BACKUP;
                    break;
                default:
                    break;
            }
        }
        List<String> primaryStorageHwIds = result.getPrimaryStorageHwIds();
        if (primaryStorageHwIds != null && !primaryStorageHwIds.isEmpty()) {
            int primaryIndex = 0;
            if (ssdHardwareId != null && primaryStorageHwIds.contains(ssdHardwareId)) {
                primaryIndex += 1;
            }
            if (disk1HardwareId != null && primaryStorageHwIds.contains(disk1HardwareId)) {
                primaryIndex += 2;
            }
            if (disk2HardwareId != null && primaryStorageHwIds.contains(disk2HardwareId)) {
                primaryIndex += 4;
            }
            switch (primaryIndex) {
                case 1:
                    mainStorageIndex = MAIN_STORAGE_M2_HIGH_SPEED;
                    break;
                case 2:
                    mainStorageIndex = MAIN_STORAGE_DISK_1;
                    break;
                case 4:
                    mainStorageIndex = MAIN_STORAGE_DISK_2;
                    break;
                case 6:
                    mainStorageIndex = MAIN_STORAGE_DISK_1_AND_2;
                    break;
                default:
                    break;
            }
        }
        diskStep(STEP_DISK_INITIALIZATION_SUCCESS);
        handleDiskInitializationSuccess(disk1Info, disk2Info, ssdInfo, isDiskEncrypt, modeIndex, mainStorageIndex);
    }

    private void showFormatReminderDialog() {
        if (formatReminderDialog != null && !formatReminderDialog.isShowing()) {
            formatReminderDialog.show();
            Window window = formatReminderDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissFormatReminderDialog() {
        if (formatReminderDialog != null && formatReminderDialog.isShowing()) {
            formatReminderDialog.dismiss();
        }
    }

    private void handleDiskInitializationEvent() {
        handleStepDiskInitializationProgress();
        List<String> raidDiskHardwareIds = null;
        String disk1HardwareId = null;
        String disk2HardwareId = null;
        String ssdHardwareId = null;
        if (diskHardwareIdMap != null) {
            if (diskHardwareIdMap.containsKey(1)) {
                disk1HardwareId = diskHardwareIdMap.get(1);
            }
            if (diskHardwareIdMap.containsKey(2)) {
                disk2HardwareId = diskHardwareIdMap.get(2);
            }
        }
        if (ssdHardwareIdMap != null && ssdHardwareIdMap.containsKey(1)) {
            ssdHardwareId = ssdHardwareIdMap.get(1);
        }
        if (mModeIndex == MODE_DUAL_DISK_MUTUAL_BACKUP) {
            raidDiskHardwareIds = new ArrayList<>();
            if (disk1HardwareId != null) {
                raidDiskHardwareIds.add(disk1HardwareId);
            }
            if (disk2HardwareId != null) {
                raidDiskHardwareIds.add(disk2HardwareId);
            }
        }
        switch (mDiskFunction) {
            case ConstantField.DiskFunction.DISK_EXPAND:
                if (presenter != null) {
                    List<String> storageHardwareIds = new ArrayList<>();
                    boolean isDiskSsdExist = ((expandDiskIndex % 2) != 0);
                    boolean isDisk1Exist = (((expandDiskIndex % 4) >> 1) != 0);
                    boolean isDisk2Exist = ((expandDiskIndex >> 2) != 0);
                    if (isDisk1Exist && disk1HardwareId != null) {
                        storageHardwareIds.add(disk1HardwareId);
                    }
                    if (isDisk2Exist && disk2HardwareId != null) {
                        storageHardwareIds.add(disk2HardwareId);
                    }
                    if (isDiskSsdExist && ssdHardwareId != null) {
                        storageHardwareIds.add(ssdHardwareId);
                    }
                    presenter.diskExpand(storageHardwareIds, (mModeIndex == MODE_DUAL_DISK_MUTUAL_BACKUP), raidDiskHardwareIds);
                }
                break;
            default:
                if (mManager != null) {
                    List<String> primaryStorageHardwareIds = null;
                    List<String> secondaryStorageHardwareIds = null;
                    switch (mMainStorageIndex) {
                        case MAIN_STORAGE_M2_HIGH_SPEED:
                            primaryStorageHardwareIds = new ArrayList<>();
                            if (ssdHardwareId != null) {
                                primaryStorageHardwareIds.add(ssdHardwareId);
                            }
                            if (disk1HardwareId != null || disk2HardwareId != null) {
                                secondaryStorageHardwareIds = new ArrayList<>();
                                if (disk1HardwareId != null) {
                                    secondaryStorageHardwareIds.add(disk1HardwareId);
                                }
                                if (disk2HardwareId != null) {
                                    secondaryStorageHardwareIds.add(disk2HardwareId);
                                }
                            }
                            break;
                        case MAIN_STORAGE_DISK_1:
                            primaryStorageHardwareIds = new ArrayList<>();
                            if (disk1HardwareId != null) {
                                primaryStorageHardwareIds.add(disk1HardwareId);
                            }
                            if (disk2HardwareId != null || ssdHardwareId != null) {
                                secondaryStorageHardwareIds = new ArrayList<>();
                                if (disk2HardwareId != null) {
                                    secondaryStorageHardwareIds.add(disk2HardwareId);
                                }
                                if (ssdHardwareId != null) {
                                    secondaryStorageHardwareIds.add(ssdHardwareId);
                                }
                            }
                            break;
                        case MAIN_STORAGE_DISK_2:
                            primaryStorageHardwareIds = new ArrayList<>();
                            if (disk2HardwareId != null) {
                                primaryStorageHardwareIds.add(disk2HardwareId);
                            }
                            if (disk1HardwareId != null || ssdHardwareId != null) {
                                secondaryStorageHardwareIds = new ArrayList<>();
                                if (disk1HardwareId != null) {
                                    secondaryStorageHardwareIds.add(disk1HardwareId);
                                }
                                if (ssdHardwareId != null) {
                                    secondaryStorageHardwareIds.add(ssdHardwareId);
                                }
                            }
                            break;
                        case MAIN_STORAGE_DISK_1_AND_2:
                            primaryStorageHardwareIds = new ArrayList<>();
                            if (disk1HardwareId != null) {
                                primaryStorageHardwareIds.add(disk1HardwareId);
                            }
                            if (disk2HardwareId != null) {
                                primaryStorageHardwareIds.add(disk2HardwareId);
                            }
                            if (ssdHardwareId != null) {
                                secondaryStorageHardwareIds = new ArrayList<>();
                                secondaryStorageHardwareIds.add(ssdHardwareId);
                            }
                            break;
                        default:
                            break;
                    }
                    DiskInitializeRequest diskInitializeRequest = new DiskInitializeRequest();
                    diskInitializeRequest.setDiskEncrypt(mEncryption ? 1 : 2);
                    diskInitializeRequest.setRaidType(((mModeIndex == MODE_DUAL_DISK_MUTUAL_BACKUP) ? 2 : 1));
                    diskInitializeRequest.setPrimaryStorageHwIds(primaryStorageHardwareIds);
                    diskInitializeRequest.setSecondaryStorageHwIds(secondaryStorageHardwareIds);
                    diskInitializeRequest.setRaidDiskHwIds(raidDiskHardwareIds);
                    mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_INITIALIZE
                            , new Gson().toJson(diskInitializeRequest, DiskInitializeRequest.class));
//                    mBridge.requestDiskInitialize(mEncryption, (mModeIndex == MODE_DUAL_DISK_MUTUAL_BACKUP)
//                            , primaryStorageHardwareIds, secondaryStorageHardwareIds, raidDiskHardwareIds);
                }
                break;
        }
    }

    private void showShutdownReminderDialog() {
        if (shutdownReminderDialog != null && !shutdownReminderDialog.isShowing()) {
            shutdownReminderDialog.show();
            Window window = shutdownReminderDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissShutdownReminderDialog() {
        if (shutdownReminderDialog != null && shutdownReminderDialog.isShowing()) {
            shutdownReminderDialog.dismiss();
        }
    }

    private void handleShutdownEvent() {
//        if (mBridge != null) {
//            mBridge.requestEulixSystemShutdown();
//        }
        if (mManager != null) {
            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_AO_SYSTEM_SHUTDOWN, null);
        }
    }

    private void handleEnterEulixSpaceEvent() {
        if (mManager != null) {
            String avatarUrl = null;
            PairingBoxInfo pairingBoxInfo = mManager.getPairingBoxInfo();
            if (pairingBoxInfo != null) {
                avatarUrl = pairingBoxInfo.getAvatarUrl();
            }
            AOCompleteActivity.startThisActivity(DiskInitializationActivity.this, mBoxUuid, "1", avatarUrl);
            mManager.finishSource();
        } else if (spaceTokenReady) {
            goMain();
        } else {
            setOneButtonLoadingPattern(false, true, getString(R.string.entering));
            waitingTokenReady = true;
        }
    }

    private void prepareObtainAccessToken(long delayMillis) {
        if (mHandler == null) {
            obtainAccessToken();
        } else {
            while (mHandler.hasMessages(MESSAGE_ACCESS_TOKEN_REQUEST)) {
                mHandler.removeMessages(MESSAGE_ACCESS_TOKEN_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_ACCESS_TOKEN_REQUEST, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_ACCESS_TOKEN_REQUEST);
            }
        }
    }

    /**
     * 请求token
     */
    private void obtainAccessToken() {
        Intent serviceIntent = new Intent(DiskInitializationActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        if (mBoxUuid != null) {
            serviceIntent.putExtra(ConstantField.BOX_UUID, mBoxUuid);
        }
        serviceIntent.putExtra(ConstantField.BOX_BIND, "1");
        serviceIntent.putExtra(ConstantField.FORCE, true);
        startService(serviceIntent);
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    private void goMain() {
        EulixSpaceApplication.popAllOldActivity(this);
        Intent intent = new Intent(DiskInitializationActivity.this, EulixMainActivity.class);
        startActivity(intent);
        handleFinish(false);
    }

    private void handleFinish(boolean isOnlyFinish) {
//        if (mBridge != null) {
//            mBridge.diskInitializeFinish();
//        }
        if (mManager != null) {
            mManager.finishSource();
            finish();
        } else if (isOnlyFinish) {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void onBackPressed() {
        if (back != null && (View.VISIBLE != back.getVisibility())) {
            if (ConstantField.DiskFunction.DISK_INITIALIZE == mDiskFunction && mManager != null) {
                mManager.finishSource();
            } else {
                confirmForceExit();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (back != null && (View.VISIBLE != back.getVisibility()) && keyCode == KeyEvent.KEYCODE_BACK) {
            if (ConstantField.DiskFunction.DISK_INITIALIZE == mDiskFunction && mManager != null) {
                mManager.finishSource();
            } else {
                confirmForceExit();
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
//        if (mBridge != null) {
//            mBridge.unregisterSinkCallback();
//            mBridge = null;
//        }
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public DiskInitializationPresenter createPresenter() {
        return new DiskInitializationPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.function_text:
                    showImageTextToast(R.drawable.toast_tip, R.string.binding_background_hint);
                    handleFinish(true);
                    break;
                case R.id.loading_button_container:
                    handleOneButtonClick();
                    break;
                case R.id.two_buttons_1:
                case R.id.two_buttons_vertical_1:
                    handleTwoButtonPreviousClick();
                    break;
                case R.id.two_buttons_2:
                case R.id.two_buttons_vertical_2:
                    handleTwoButtonNextClick();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView != null) {
            switch (buttonView.getId()) {
                case R.id.disk_encryption_switch:
                    mEncryption = isChecked;
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void completeStorageDiskManageList() {
        // todo
    }

    @Override
    public void diskExpandResponse(int code, String source) {
        if (code >= 200 && code < 400) {
            handleRefreshExpandProgressEvent(0);
        } else if (mHandler != null) {
            mHandler.post(() -> {
                if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.disk_format_fail_hint);
                }
                diskStep(STEP_DISK_FORMAT);
            });
        }
    }

    @Override
    public void diskExpandProgressResponse(int code, String source, DiskExpandProgressResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (result != null) {
                    String expandMessage = result.getExpandMessage();
                    if (expandMessage != null) {
                        Logger.d(TAG, "expand message: " + expandMessage);
                    }
                }
                long continueMillis = 2000L;
                Integer expandCode = null;
                Integer expandProgress = null;
                if (code >= 200 && code < 400 && result != null) {
                    expandCode = result.getExpandCode();
                    expandProgress = result.getExpandProgress();
                }
                if (expandCode != null || expandProgress != null) {
                    if (expandProgress != null) {
                        handleDiskInitializationProgressCallback(expandProgress);
                    }
                    if (expandCode != null) {
                        switch (expandCode) {
                            case DiskExpandProgressResult.CODE_EXPANDING:
                                continueMillis = 3500L;
                                if (diskInitializationProgressHint != null) {
                                    diskInitializationProgressHint.setText(R.string.disk_format_progress_hint);
                                }
                                break;
                            case DiskExpandProgressResult.CODE_EXPAND_COMPLETE:
                                continueMillis = -1L;
                                mDiskExpand = expandCode;
                                handleDiskInitializationSuccessEvent(0);
                                break;
                            default:
                                continueMillis = -1L;
                                mDiskExpand = expandCode;
                                setDiskView(mExpandDiskIndex);
                                diskStep(STEP_DISK_INITIALIZATION_FAIL);
                                handleDiskInitializationFail();
                                break;
                        }
                    }
                }
                if (continueMillis >= 0) {
                    handleRefreshExpandProgressEvent(continueMillis);
                }
            });
        }
    }

    @Override
    public void onFinish() {
        if (mHandler != null) {
            mHandler.post(() -> {
                dismissShutdownReminderDialog();
                finish();
            });
        }
    }

    @Override
    public void onResponse(int code, String source, int step, String bodyJson) {
        switch (step) {
            case AODeviceDiscoveryManager.STEP_AO_SYSTEM_SHUTDOWN:
//                if (code >= 200 && code < 400 && mManager != null) {
//                    mManager.finishSource();
//                    finish();
//                }
                break;
            case AODeviceDiscoveryManager.STEP_DISK_INITIALIZE:
                handleDiskInitializeResponse(code, source);
                break;
            case AODeviceDiscoveryManager.STEP_DISK_INITIALIZE_PROGRESS:
                DiskInitializeProgressResult diskInitializeProgressResult = null;
                if (bodyJson != null) {
                    try {
                        diskInitializeProgressResult = new Gson().fromJson(bodyJson, DiskInitializeProgressResult.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                handleDiskInitializeProgressResponse(code, source, diskInitializeProgressResult);
                break;
            case AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST:
                DiskManageListResult diskManageListResult = null;
                if (bodyJson != null) {
                    try {
                        diskManageListResult = new Gson().fromJson(bodyJson, DiskManageListResult.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                handleDiskManagementListResponse(code, source, diskManageListResult);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(() -> {
                dismissShutdownReminderDialog();
                finish();
            });
        }
    }

    @Override
    public void handleDiskInitializeResponse(int code, String source) {
        if (code >= 200 && code < 400) {
            handleRefreshInitializeProgressEvent(0);
        } else if (mHandler != null) {
            mHandler.post(() -> {
                showImageTextToast(R.drawable.toast_wrong, R.string.disk_initialize_fail_hint);
                if (code == ConstantField.KnownError.DiskInitializationError.DISK_NOT_PAIR && ConstantField.KnownSource.AGENT.equals(source)) {
                    if (presenter != null) {
                        presenter.deleteBox(mBoxUuid);
                    }
                    goMain();
                } else {
                    diskStep(STEP_DISK_FORMAT);
                }
            });
        }
    }

    @Override
    public void handleDiskInitializeProgressResponse(int code, String source, DiskInitializeProgressResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (result != null) {
                    String initialMessage = result.getInitialMessage();
                    if (initialMessage != null) {
                        Logger.d(TAG, "initial message: " + initialMessage);
                    }
                }
                long continueMillis = 2000L;
                Integer initialCode = null;
                Integer initialProgress = null;
                if (code >= 200 && code < 400 && result != null) {
                    initialCode = result.getInitialCode();
                    initialProgress = result.getInitialProgress();
                }
                if (initialCode != null || initialProgress != null) {
                    if (initialProgress != null) {
                        handleDiskInitializationProgressCallback(initialProgress);
                    }
                    if (initialCode != null) {
                        switch (initialCode) {
                            case ReadyCheckResult.DISK_INITIALIZING:
                                continueMillis = 3500L;
                                if (diskInitializationProgressHint != null) {
                                    diskInitializationProgressHint.setText(R.string.disk_initialization_hint);
                                }
                                break;
                            case ReadyCheckResult.DISK_DATA_SYNCHRONIZATION:
                                continueMillis = 3500L;
                                if (diskInitializationProgressHint != null) {
                                    diskInitializationProgressHint.setText(R.string.disk_data_synchronization_hint);
                                }
                                break;
                            case ReadyCheckResult.DISK_NORMAL:
                                continueMillis = -1L;
                                handleDiskInitializationSuccessEvent(0);
                                break;
                            default:
                                continueMillis = -1L;
                                mDiskInitialize = initialCode;
                                setDiskView();
                                diskStep(STEP_DISK_INITIALIZATION_FAIL);
                                handleDiskInitializationFail();
                                break;
                        }
                    }
                }
                if (continueMillis >= 0) {
                    handleRefreshInitializeProgressEvent(continueMillis);
                }
            });
        }
    }

    @Override
    public void handleDiskManagementListResponse(int code, String source, DiskManageListResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (code >= 200 && code < 400 && result != null) {
//                    if (presenter != null) {
//                        presenter.requestUseBox(mBoxUuid, result);
//                    }
                    AOSpaceUtil.requestUseBox(getApplicationContext(), mBoxUuid, "1", result);
                    Boolean isMissingMainStorage = result.getMissingMainStorage();
                    if (isMissingMainStorage != null && isMissingMainStorage) {
                        diskStep(STEP_NO_MAIN_STORAGE);
                    } else {
                        handleDiskManagementList(result);
                        setDiskView();
//                        if (mBridge != null) {
//                            mBridge.diskInitializeHardwareFinish();
//                        }
//                        if (mManager == null || !mManager.isNewBindProcessSupport()) {
//                            prepareObtainAccessToken(0);
//                        }
                    }
                } else {
                    handleDiskInitializationSuccessEvent(5000);
                }
            });
        }
    }

    @Override
    public void handleEulixSystemShutdownResponse(int code, String source) {
        // do nothing
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AccessTokenResultEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            Boolean result = event.getResult();
            long expireTimestamp = event.getExpireTimestamp();
            if (mBoxUuid != null && mBoxUuid.equals(boxUuid) && "1".equals(boxBind)) {
                if (result == null) {
                    prepareObtainAccessToken(10 * ConstantField.TimeUnit.SECOND_UNIT);
                } else if (result) {
                    if (expireTimestamp <= System.currentTimeMillis()) {
                        prepareObtainAccessToken(0);
                    } else if (presenter != null) {
                        presenter.changeActiveBox(boxUuid, expireTimestamp);
                        if (waitingTokenReady) {
                            setOneButtonLoadingPattern(true, false, getString(R.string.enter_ao_space));
                            waitingTokenReady = false;
                            goMain();
                        }
                        spaceTokenReady = true;
                    }
                } else {
                    if (waitingTokenReady) {
                        setOneButtonLoadingPattern(true, false, getString(R.string.enter_ao_space));
                        waitingTokenReady = false;
                        goMain();
                    }
                    spaceTokenReady = true;
                }
            } else {
                prepareObtainAccessToken(0L);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DiskManagementListResponseEvent event) {
        if (event != null) {
            int code = event.getCode();
            String source = event.getSource();
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            String requestUuid = event.getRequestUuid();
            if (StringUtil.compare(requestUuid, mRequestUuid)) {
                mRequestUuid = null;
                DiskManageListResult result = null;
                if (presenter != null) {
                    result = presenter.getDiskManageListResult(boxUuid, boxBind);
                }
                if (code >= 200 && code < 400 && result != null) {
                    mDiskIndex = mExpandDiskIndex;
                    handleDiskManagementList(result);
                    setDiskView();
                } else {
                    handleDiskInitializationSuccessEvent(5000);
                }
            }
        }
    }
}
