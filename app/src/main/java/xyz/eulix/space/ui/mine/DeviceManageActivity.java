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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.EulixDeviceListActivity;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.mine.TerminalAdapter;
import xyz.eulix.space.bean.BoxGenerationShowBean;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixDeviceManageInfo;
import xyz.eulix.space.bean.EulixTerminal;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.event.BoxNetworkRequestEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.BoxVersionCheckEvent;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.event.DeviceAbilityResponseEvent;
import xyz.eulix.space.event.DeviceNetworkEvent;
import xyz.eulix.space.event.DiskManagementListRequestEvent;
import xyz.eulix.space.event.DiskManagementListResponseEvent;
import xyz.eulix.space.event.StorageInfoRequestEvent;
import xyz.eulix.space.event.TerminalListEvent;
import xyz.eulix.space.event.TerminalResultEvent;
import xyz.eulix.space.presenter.DeviceManagePresenter;
import xyz.eulix.space.ui.bind.UnbindDeviceActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.view.RingProgressBar;
import xyz.eulix.space.view.TitleBarWithSelect;
import xyz.eulix.space.view.dialog.EulixDialogUtil;

public class DeviceManageActivity extends AbsActivity<DeviceManagePresenter.IDeviceManage, DeviceManagePresenter> implements DeviceManagePresenter.IDeviceManage, TerminalAdapter.OnItemClickListener {
    private TitleBarWithSelect titleBar;
    private TextView tvBoxName;
    private EulixDeviceManageInfo manageInfo;
    private LinearLayout layoutSystemUpdate;
    private TextView bindDeviceTitle;
    private LinearLayout bindDeviceContainer;
    private ImageView bindDeviceImage;
    private TextView bindDeviceName;
    private TextView bindDeviceTypePlace;
    private TextView bindDeviceTime;
    private Button btnUnbind;
    private TextView tvDeviceUpdateFlag;
    private RingProgressBar ringProgressBar;
    private TextView tvTotalStorage;
    private TextView tvUsedStorage;
    private TextView tvUnusedStorage;
    private TextView tvSnNumber;
    private TextView tvSystemVersion;
    private LinearLayout layoutMoreDeviceInfo;
    private ImageView imgDeviceLogo;
    private View noFunctionSplit;
    private TextView loginTerminalText;
    private RecyclerView loginTerminalList;

    private DeviceManageHandler mHandler;
    private ContentObserver boxObserver;
    private boolean isBoxObserve;
    private String diskManagementRequestUuid;

    private Dialog offlineDialog;
    private TextView dialogTitle;
    private TextView dialogContent;
    private Button dialogCancel;
    private Button dialogConfirm;

    private TerminalAdapter mAdapter;
    private List<EulixTerminal> mEulixTerminals;
    private EulixTerminal mSelectTerminal;

    private Comparator<EulixTerminal> eulixTerminalComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else {
                return -1;
            }
        } else {
            Long timestamp1 = o1.getTerminalTimestamp();
            Long timestamp2 = o2.getTerminalTimestamp();
            return timestamp2.compareTo(timestamp1);
        }
    };

    static class DeviceManageHandler extends Handler {
        private WeakReference<DeviceManageActivity> deviceManageActivityWeakReference;

        public DeviceManageHandler(DeviceManageActivity activity) {
            deviceManageActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            DeviceManageActivity activity = deviceManageActivityWeakReference.get();
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
        setContentView(R.layout.activity_device_manage);
        titleBar = findViewById(R.id.title_bar);
        tvBoxName = findViewById(R.id.tv_box_name);
        layoutSystemUpdate = findViewById(R.id.layout_system_update);
        bindDeviceTitle = findViewById(R.id.bind_device_title);
        bindDeviceContainer = findViewById(R.id.bind_device_container);
        bindDeviceImage = findViewById(R.id.bind_device_image);
        bindDeviceName = findViewById(R.id.bind_device_name);
        bindDeviceTypePlace = findViewById(R.id.bind_device_type_place);
        bindDeviceTime = findViewById(R.id.bind_device_time);
        btnUnbind = findViewById(R.id.btn_unbind);
        tvDeviceUpdateFlag = findViewById(R.id.tv_device_update_flag);
        ringProgressBar = findViewById(R.id.ring_progress);
        tvTotalStorage = findViewById(R.id.tv_total_size);
        tvUsedStorage = findViewById(R.id.tv_used_storage);
        tvUnusedStorage = findViewById(R.id.tv_unused_storage);
        tvSnNumber = findViewById(R.id.tv_sn_number);
        tvSystemVersion = findViewById(R.id.tv_system_version);
        layoutMoreDeviceInfo = findViewById(R.id.layout_more_device_info);
        tvSnNumber = findViewById(R.id.tv_sn_number);
        tvSystemVersion = findViewById(R.id.tv_system_version);
        imgDeviceLogo = findViewById(R.id.img_space);
        noFunctionSplit = findViewById(R.id.no_function_split);
        loginTerminalText = findViewById(R.id.login_terminal_text);
        loginTerminalList = findViewById(R.id.login_terminal_list);

        View offlineDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        dialogTitle = offlineDialogView.findViewById(R.id.dialog_title);
        dialogContent = offlineDialogView.findViewById(R.id.dialog_content);
        dialogCancel = offlineDialogView.findViewById(R.id.dialog_cancel);
        dialogConfirm = offlineDialogView.findViewById(R.id.dialog_confirm);
        offlineDialog = new Dialog(this, R.style.EulixDialog);
        offlineDialog.setCancelable(false);
        offlineDialog.setContentView(offlineDialogView);

        EventBusUtil.register(this);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        mHandler = new DeviceManageHandler(this);
        mEulixTerminals = new ArrayList<>();
        boxObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                handleStorageInfo();
            }
        };
        EventBusUtil.post(new BoxNetworkRequestEvent());
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(R.string.device);
        String deviceCacheStr = PreferenceUtil.getDeviceVersionDetailInfo(this);
        if (!TextUtils.isEmpty(deviceCacheStr)) {
            DeviceVersionInfoBean deviceVersionInfoBean = new Gson().fromJson(deviceCacheStr, DeviceVersionInfoBean.class);
            presenter.deviceVersionInfoBean = deviceVersionInfoBean;
            if (deviceVersionInfoBean != null) {
                refreshDeviceInfoViews(deviceVersionInfoBean);
            }
        } else {
            showLoading("");
        }
        presenter.getDeviceVersionDetailInfo();

//        tvBoxName.setText(R.string.box_name);

//        if (presenter.isActiveUserAdmin()) {
//            layoutNetworkSetting.setVisibility(View.VISIBLE);
//            layoutSystemUpdate.setVisibility(View.VISIBLE);
//        } else {
//            layoutNetworkSetting.setVisibility(View.GONE);
//            layoutSystemUpdate.setVisibility(View.GONE);
//        }
//        updateFunctionSpilt();
//        updateNoFunctionSplit();

        if (ConstantField.boxVersionCheckBody != null) {
            tvDeviceUpdateFlag.setVisibility(View.VISIBLE);
        } else {
            tvDeviceUpdateFlag.setVisibility(View.GONE);
        }

        ringProgressBar.setProgress(0);

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String hintPart1 = getString(R.string.login_terminal_hint);
        String hintPart2 = getString(R.string.login_terminal_hint_highlight);
        spannableStringBuilder.append(hintPart1);
        int highlightStart = spannableStringBuilder.length();
        spannableStringBuilder.append(hintPart2);
        int highlightEnd = spannableStringBuilder.length();
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
        spannableStringBuilder.setSpan(foregroundColorSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginTerminalText.setText(spannableStringBuilder);

        dialogTitle.setText(R.string.offline_terminal);
        dialogConfirm.setText(R.string.confirm);
    }

    private void updateShowDeviceAbility() {
        boolean isShowSystemUpdate = true;
        boolean isShowDiskManagement = false;
        BoxGenerationShowBean boxGenerationShowBean = new BoxGenerationShowBean(getString(R.string.device_server_name), R.drawable.eulix_box_device_v1);
        if (presenter != null) {
            DeviceAbility deviceAbility = presenter.getActiveDeviceAbility();
            if (deviceAbility != null) {
                Boolean isUpgradeApiSupportValue = deviceAbility.getUpgradeApiSupport();
                Boolean isInnerDiskSupportValue = deviceAbility.getInnerDiskSupport();
                if (isUpgradeApiSupportValue != null) {
                    isShowSystemUpdate = isUpgradeApiSupportValue;
                }
                if (isInnerDiskSupportValue != null) {
                    isShowDiskManagement = isInnerDiskSupportValue;
                }
                if (deviceAbility.getOpenSource()) {
                    boxGenerationShowBean.setBoxName(getString(R.string.open_source_version));
                    boxGenerationShowBean.setBoxResId(R.drawable.eulix_device_computer_2x);
                } else {
                    boxGenerationShowBean = DataUtil.generationBoxGenerationShowBean(this, deviceAbility.getDeviceModelNumber(), boxGenerationShowBean);
                }
            }
        }
        tvBoxName.setText(boxGenerationShowBean.getBoxName());
        imgDeviceLogo.setImageResource(boxGenerationShowBean.getBoxResId());
        if (presenter != null) {
            if (presenter.isActiveUserAdmin()) {
                if (isShowSystemUpdate) {
                    layoutSystemUpdate.setVisibility(View.VISIBLE);
                } else {
                    layoutSystemUpdate.setVisibility(View.GONE);
                }
            }
            if (presenter.isActiveAdministrator() && isShowDiskManagement) {
                boolean isDiskExpand = presenter.isDiskExpand();
                if (isDiskExpand) {
                    EulixBoxBaseInfo eulixBoxBaseInfo = null;
                    if (presenter != null) {
                        eulixBoxBaseInfo = presenter.getActiveBoxBaseInfo();
                    }
                    if (eulixBoxBaseInfo != null) {
                        diskManagementRequestUuid = UUID.randomUUID().toString();
                        EventBusUtil.post(new DiskManagementListRequestEvent(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), diskManagementRequestUuid));
                    }
                }
            }
        }
        updateNoFunctionSplit();
    }

    @Override
    public void refreshDeviceInfoViews(DeviceVersionInfoBean deviceVersionInfoBean) {
        closeLoading();
        if (deviceVersionInfoBean != null) {
            tvSnNumber.setText(TextUtils.isEmpty(deviceVersionInfoBean.snNumber) ? "--" : deviceVersionInfoBean.snNumber);
            tvSystemVersion.setText(deviceVersionInfoBean.spaceVersion);
        }
    }

    @Override
    public void handleTerminalOffline(int code, String customizeSource) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if ((code >= 200 && code < 400) || (code == ConstantField.KnownError.TerminalError.TERMINAL_OFFLINE_DUPLICATE_CODE && customizeSource != null && customizeSource.trim().toUpperCase().startsWith(ConstantField.KnownSource.ACCOUNT))) {
                    requestTerminalList();
                    showImageTextToast(R.drawable.toast_right, R.string.offline_success);
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.offline_fail);
                }
            });
        }
    }

    @Override
    public void initEvent() {
        layoutSystemUpdate.setOnClickListener(v -> {
            //进入系统升级页面
            Intent intent = new Intent(DeviceManageActivity.this, SystemUpdateActivity.class);
            if (ConstantField.hasClickSystemUpgradeInstallLater) {
                intent.putExtra(SystemUpdateActivity.KEY_DIALOG_OPERATE_TYPE, SystemUpdateActivity.DIALOG_OPERATE_TYPE_INSTALL_LATER);
            }
            startActivity(intent);
        });

        btnUnbind.setOnClickListener(v -> {
            //解绑设备
            EulixDialogUtil.showChooseAlertDialog(this, getResources().getString(R.string.unbind_device_dialog_title),
                    getResources().getString(R.string.unbind_content), getResources().getString(R.string.unbind_device),
                    (dialog, which) -> {
                        if (presenter.isActiveUserAdmin()) {
                            long permitTimestamp = -1L;
                            if (presenter != null) {
                                permitTimestamp = presenter.getSecurityPasswordPermitTimestamp();
                            }
                            long currentTimestamp = System.currentTimeMillis();
                            if (permitTimestamp > currentTimestamp) {
                                long minute = (long) Math.ceil((permitTimestamp - currentTimestamp) * 1.0 / ConstantField.TimeUnit.MINUTE_UNIT);
                                if (minute > 0) {
                                    String stringBuilder = getString(R.string.common_retry_hint_minute_part_1) +
                                            minute +
                                            getString((Math.abs(minute) == 1L
                                                    ? R.string.common_retry_hint_minute_part_2_singular : R.string.common_retry_hint_minute_part_2_plural));
                                    showImageTextToast(R.drawable.toast_refuse, stringBuilder);
                                }
                            } else {
                                if (presenter != null) {
                                    presenter.clearSecurityPasswordRevokeDenyTimestamp();
                                }
                                Intent intent = new Intent(DeviceManageActivity.this, UnbindDeviceActivity.class);
                                startActivity(intent);
                            }
                        } else {
                            presenter.revokeDevice();
                        }
                    }, null);
        });

        layoutMoreDeviceInfo.setOnClickListener(v -> {
            //跳转系统规格页面
            BoxSystemDetailActivity.startActivity(DeviceManageActivity.this, presenter.deviceVersionInfoBean);
        });

        dialogCancel.setOnClickListener(v -> {
            mSelectTerminal = null;
            dismissOfflineDialog();
        });
        dialogConfirm.setOnClickListener(v -> {
            if (mSelectTerminal != null && presenter != null) {
                presenter.offlineTerminal(mSelectTerminal.getTerminalUuid());
            }
            showLoading("");
            mSelectTerminal = null;
            dismissOfflineDialog();
        });

        mAdapter = new TerminalAdapter(this, mEulixTerminals);
        mAdapter.setOnItemClickListener(this);
        loginTerminalList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        loginTerminalList.addItemDecoration(new TerminalAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        loginTerminalList.setAdapter(mAdapter);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxVersionCheckEvent event) {
        Logger.d("zfy", "device manager BoxVersionCheckEvent");
        if (ConstantField.boxVersionCheckBody != null) {
            tvDeviceUpdateFlag.setVisibility(View.VISIBLE);
        } else {
            tvDeviceUpdateFlag.setVisibility(View.GONE);
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
    public void onRevokeResult(Boolean result, String msg) {
        if (result != null && result) {
            showImageTextToast(R.drawable.toast_right, R.string.unbind_toast_success);
            EventBusUtil.post(new BoxOnlineRequestEvent(false));
            Intent loginIntent = new Intent(DeviceManageActivity.this, EulixDeviceListActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish();
        } else {
            if (!TextUtils.isEmpty(msg) && msg.contains(String.valueOf(ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR))) {
                showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
            } else if (result == null) {
                showServerExceptionToast();
            } else {
                showImageTextToast(R.drawable.toast_wrong, R.string.unbind_toast_failed);
            }
        }
    }

    private void updateSpaceProgress(long usedSize, long totalSize) {
        usedSize = Math.max(usedSize, 0);
        totalSize = Math.max(totalSize, usedSize);
        if (totalSize == 0) {
            tvTotalStorage.setText("--");
            tvUsedStorage.setText("--");
            tvUnusedStorage.setText("--");
        } else {
            String totalSizeStr = FormatUtil.formatSimpleSize(totalSize, ConstantField.SizeUnit.FORMAT_1F);
            String usedSizeStr = FormatUtil.formatSimpleSize(usedSize, ConstantField.SizeUnit.FORMAT_1F);
            String unusedSizeStr = FormatUtil.formatSimpleSize((totalSize - usedSize), ConstantField.SizeUnit.FORMAT_1F);

            tvTotalStorage.setText(totalSizeStr);
            tvUsedStorage.setText(usedSizeStr);
            tvUnusedStorage.setText(unusedSizeStr);
            int usedProgress = (int) (usedSize * 100 / totalSize);
            ringProgressBar.setProgress(usedProgress);
        }
    }

    private void updateStorageInfo() {
        if (presenter != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.getActiveBoxUuid();
            if (eulixBoxBaseInfo != null) {
                String boxUuid = eulixBoxBaseInfo.getBoxUuid();
                String boxBind = eulixBoxBaseInfo.getBoxBind();
                String boxDomain = eulixBoxBaseInfo.getBoxDomain();
                if (boxUuid != null && boxDomain != null) {
                    StorageInfoRequestEvent requestEvent = new StorageInfoRequestEvent(boxUuid, boxBind, boxDomain);
                    EventBusUtil.post(requestEvent);
                    if (NetUtils.isNetAvailable(getApplicationContext())) {
                        DeviceNetworkEvent deviceNetworkEvent = new DeviceNetworkEvent(boxUuid, boxBind, boxDomain);
                        EventBusUtil.post(deviceNetworkEvent);
                    }
                }
            }
        }
    }

    private void handleStorageInfo() {
        if (presenter != null) {
            manageInfo = presenter.getActiveManageInfo();
            if (manageInfo != null) {
                updateSpaceProgress(manageInfo.getUsedSize(), manageInfo.getTotalSize());
            }
        }
    }

    private void showOfflineDialog() {
        if (offlineDialog != null && !offlineDialog.isShowing()) {
            offlineDialog.show();
            Window window = offlineDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissOfflineDialog() {
        if (offlineDialog != null && offlineDialog.isShowing()) {
            offlineDialog.dismiss();
        }
    }

    private void requestTerminalList() {
        if (presenter != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.getActiveBoxUuid();
            if (eulixBoxBaseInfo != null) {
                TerminalListEvent terminalListEvent = new TerminalListEvent(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), true);
                EventBusUtil.post(terminalListEvent);
            }
        }
    }

    private void updateTerminalList() {
        if (presenter != null) {
            mEulixTerminals = presenter.getEulixTerminalList();
        }
        Collections.sort(mEulixTerminals, eulixTerminalComparator);
        mAdapter.updateData(mEulixTerminals);
    }

    private void updateBindDevice() {
        boolean isGranter = false;
        boolean isFromTerminalList = true;
        if (presenter != null) {
            isGranter = presenter.isActiveGranter();
        }
        btnUnbind.setVisibility(isGranter ? View.VISIBLE : View.GONE);
        EulixTerminal eulixTerminal = null;
        if (mEulixTerminals != null) {
            for (EulixTerminal terminal : mEulixTerminals) {
                if (terminal != null && terminal.isGranter()) {
                    eulixTerminal = terminal;
                    break;
                }
            }
        }
        if (eulixTerminal == null && presenter != null) {
            eulixTerminal = presenter.getGranterTerminal();
            isFromTerminalList = false;
        }
        if (eulixTerminal != null) {
            if (bindDeviceName != null) {
                bindDeviceName.setText(StringUtil.nullToEmpty(eulixTerminal.getTerminalName()));
            }
            StringBuilder typePlaceBuilder = new StringBuilder();
            String type = (isFromTerminalList ? getString(R.string.unknown_terminal) : "");
            @DrawableRes int terminalResId = R.drawable.unknown_terminal_2x;
            String terminalType = eulixTerminal.getTerminalType();
            if (terminalType != null) {
                switch (terminalType.toLowerCase()) {
                    case "android":
                        type = getString(R.string.android_client);
                        terminalResId = R.drawable.android_terminal_2x;
                        break;
                    case "ios":
                        type = getString(R.string.ios_client);
                        terminalResId = R.drawable.ios_terminal_2x;
                        break;
                    case "web":
                        type = getString(R.string.web_browser);
                        terminalResId = R.drawable.browser_terminal_2x;
                        break;
                    default:
                        break;
                }
            }
            typePlaceBuilder.append(StringUtil.nullToEmpty(type));
            String place = eulixTerminal.getTerminalPlace();
            if (place != null && !TextUtils.isEmpty(place)) {
                String[] places = place.split("\\|");
                String nPlace = null;
                if (places.length >= 4) {
                    nPlace = places[3];
                    if (nPlace == null || TextUtils.isEmpty(nPlace) || nPlace.equals("0")) {
                        nPlace = places[1];
                    }
                    if (nPlace == null || TextUtils.isEmpty(nPlace) || nPlace.equals("0")) {
                        nPlace = places[0];
                    }
                    if (nPlace == null || TextUtils.isEmpty(nPlace) || nPlace.equals("0")) {
                        nPlace = null;
                    }
                }
                if (nPlace != null && !TextUtils.isEmpty(nPlace)) {
                    typePlaceBuilder.append("·");
                    typePlaceBuilder.append(nPlace);
                }
            }
            if (bindDeviceTypePlace != null) {
                bindDeviceTypePlace.setText(typePlaceBuilder.toString());
            }
            if (bindDeviceImage != null) {
                bindDeviceImage.setImageResource(terminalResId);
            }
            String timeLogin = FormatUtil.formatTime(eulixTerminal.getTerminalTimestamp(), ConstantField.TimeStampFormat.FILE_API_MINUTE_FORMAT)
                    + " " + getString(R.string.login);
            if (bindDeviceTime != null) {
                bindDeviceTime.setText(timeLogin);
            }
        }
        updateNoFunctionSplit();
    }

    private void updateNoFunctionSplit() {
        if (noFunctionSplit != null) {
            noFunctionSplit.setVisibility((isViewGone(layoutSystemUpdate)) ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected int getActivityIndex() {
        return ConstantField.ActivityIndex.LOGIN_TERMINAL_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public DeviceManagePresenter createPresenter() {
        return new DeviceManagePresenter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (boxObserver != null && !isBoxObserve) {
            isBoxObserve = true;
            getContentResolver().registerContentObserver(EulixSpaceDBManager.BOX_URI, true, boxObserver);
        }
        handleStorageInfo();
        if (presenter == null || presenter.isPhysicalDevice()) {
            updateStorageInfo();
        }
        updateTerminalList();
        updateBindDevice();
        requestTerminalList();
        updateShowDeviceAbility();
        if (presenter != null) {
            presenter.updateDeviceAbility();
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
        super.onDestroy();
        EventBusUtil.unRegister(this);
    }

    @Override
    public void onItemClick(View view, int position) {
        if (view != null) {
            switch (view.getId()) {
                case R.id.terminal_go_offline:
                    if (mEulixTerminals != null && position >= 0 && mEulixTerminals.size() > position) {
                        mSelectTerminal = mEulixTerminals.get(position);
                        String content = getString(R.string.offline_terminal_content_part_1)
                                + StringUtil.nullToEmpty(mSelectTerminal.getTerminalName())
                                + getString(R.string.offline_terminal_content_part_2);
                        dialogContent.setText(content);
                        showOfflineDialog();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TerminalResultEvent event) {
        updateTerminalList();
        updateBindDevice();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceAbilityResponseEvent event) {
        if (event != null && presenter != null) {
            updateShowDeviceAbility();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DiskManagementListResponseEvent event) {
        String requestUuid = null;
        if (event != null) {
            requestUuid = event.getRequestUuid();
        }
        if (StringUtil.compare(requestUuid, diskManagementRequestUuid)) {
            diskManagementRequestUuid = null;
            boolean isDiskExpand = presenter.isDiskExpand();
        }
    }
}