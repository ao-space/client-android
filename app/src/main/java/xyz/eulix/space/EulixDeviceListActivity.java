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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.EulixUserAdapter;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.ApplicationLockEventInfo;
import xyz.eulix.space.bean.ApplicationLockInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.EulixDeviceManageInfo;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.AccessTokenCreateEvent;
import xyz.eulix.space.event.AuthAutoLoginEvent;
import xyz.eulix.space.event.BoxAllCheckedEvent;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.BoxStatusEvent;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.event.SpaceValidEvent;
import xyz.eulix.space.manager.AlreadyUploadedManager;
import xyz.eulix.space.manager.EulixBiometricManager;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.presenter.EulixDeviceListPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.ui.authorization.GranteeLoginActivity;
import xyz.eulix.space.ui.bind.AODeviceBindActivity;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PermissionUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.Utils;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.BottomDialog;

/**
 * @author: chenjiawei
 * date: 2021/7/14 10:44
 */
public class EulixDeviceListActivity extends AbsActivity<EulixDeviceListPresenter.IEulixDeviceList, EulixDeviceListPresenter> implements EulixDeviceListPresenter.IEulixDeviceList, View.OnClickListener, EulixUserAdapter.OnItemClickListener {
    private static final String TAG = EulixDeviceListActivity.class.getSimpleName();
    private static final int SECOND_UNIT = 1000;
    private static final int QUERY_BOX_STATUS = 1;
    private static final int AUTH_AUTO_LOGIN_PULL = QUERY_BOX_STATUS + 1;
    private ImageButton back;
    private TextView title;
    private Button functionText;
    private SwipeRefreshLayout swipeRefreshContainer;
    private LinearLayout eulixDeviceEmptyContainer;
    private RecyclerView eulixDeviceList;
    private Button bindDevice/*, loginMoreDevices*/;
    private List<EulixUser> eulixUsers;
    private EulixUserAdapter adapter;

    private Button offlineUseButton;
    private ImageView offlineImage;
    private ImageButton offlineExit;
    private TextView offlineTitle;
    private TextView offlineContent;
    private TextView offlineHint;
    private Dialog offlineDialog;

    private Button loginExpireConfirm;
    private ImageView loginExpireImage;
    private TextView loginExpireTitle;
    private TextView loginExpireContent;
    private Dialog loginExpireDialog;

    private Button granteeRequestLoginCancel;
    private ImageView granteeRequestLoginAvatar;
    private TextView granteeRequestLoginNickname;
    private TextView granteeRequestLoginDomain;
    private Dialog granteeRequestLoginDialog;

    private TextView spacePlatformErrorDialogTitle;
    private TextView spacePlatformErrorDialogContent;
    private Button spacePlatformErrorDialogCancel;
    private Button spacePlatformErrorDialogConfirm;
    private Dialog spacePlatformErrorDialog;

    private TextView spaceDeleteDialogTitle;
    private TextView spaceDeleteDialogContent;
    private TextView spaceDeleteDialogHint;
    private Button spaceDeleteDialogConfirm;
    private Button spaceDeleteDialogCancel;
    private Dialog spaceDeleteDialog;

    private String clientUuid;
    private String mOfflineBoxUuid;
    private String mOfflineBoxBind;
    private String mBoxUuid;
    private String mBoxBind;
    private String spacePlatformErrorBoxUuid;
    private String spacePlatformErrorBoxBind;
    private String spaceDeleteBoxUuid;
    private String spaceDeleteBoxBind;
    private EulixDeviceListHandler mHandler;
    private boolean isChooseBox = false;
    private boolean isActive = false;
    private boolean isClickBind = false;
    private boolean isClickLogin = false;
    private boolean isClickUnlock = false;
    private long mExitTime = 0L;

    // TODO 后门：切换环境
    private int clickCount = 0;
    private long lastClickTime;
    private BottomDialog testEnvironmentDialog;

    // 应用锁使用，控制列表更新
    private List<String> authenticateRequestIds;
    private Map<String, EulixSpaceInfo> authenticateEulixSpaceInfoMap;

    private Runnable resetClickBindRunnable = () -> isClickBind = false;
    private Runnable resetClickLoginRunnable = () -> isClickLogin = false;
    private Runnable resetClickUnlockRunnable = () -> isClickUnlock = false;
    private Runnable resetSwipeRefreshRunnable = this::resetSwipeRefreshEvent;

    private RecyclerView.OnScrollListener eulixDeviceListScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            resetMenuScroll(null);
        }
    };

    private Comparator<EulixUser> spaceStateComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else {
                return -1;
            }
        } else {
            return Integer.compare(o2.getSpaceState(), o1.getSpaceState());
        }
    };

    static class EulixDeviceListHandler extends Handler {
        private WeakReference<EulixDeviceListActivity> eulixDeviceListActivityWeakReference;

        public EulixDeviceListHandler(EulixDeviceListActivity activity) {
            eulixDeviceListActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixDeviceListActivity activity = eulixDeviceListActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                Bundle data = msg.peekData();
                switch (msg.what) {
                    case QUERY_BOX_STATUS:
                        BoxStatusEvent boxStatusEvent = new BoxStatusEvent(false);
                        EventBusUtil.post(boxStatusEvent);
                        break;
                    case AUTH_AUTO_LOGIN_PULL:
                        if (data != null) {
                            String boxUuid = null;
                            String boxBind = null;
                            if (data.containsKey(ConstantField.BOX_UUID) && data.containsKey(ConstantField.BOX_BIND)) {
                                boxUuid = data.getString(ConstantField.BOX_UUID, null);
                                boxBind = data.getString(ConstantField.BOX_BIND, null);
                            }
                            if (boxUuid != null && boxBind != null && activity.presenter != null) {
                                activity.presenter.authAutoLoginPoll(boxUuid, boxBind);
                            }
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
        setContentView(R.layout.device_list_main);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        swipeRefreshContainer = findViewById(R.id.swipe_refresh_container);
        eulixDeviceEmptyContainer = findViewById(R.id.eulix_device_empty_container);
        eulixDeviceList = findViewById(R.id.eulix_device_list);
        bindDevice = findViewById(R.id.bind_device);
//        loginMoreDevices = findViewById(R.id.login_more_devices);
//        deviceListEmptyHint = findViewById(R.id.device_list_empty_hint);

        View offlineDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_error_one_button_hint_dialog_style_2, null);
        offlineImage = offlineDialogView.findViewById(R.id.dialog_image);
        offlineExit = offlineDialogView.findViewById(R.id.dialog_exit);
        offlineTitle = offlineDialogView.findViewById(R.id.dialog_title);
        offlineContent = offlineDialogView.findViewById(R.id.dialog_content);
        offlineUseButton = offlineDialogView.findViewById(R.id.dialog_button);
        offlineHint = offlineDialogView.findViewById(R.id.dialog_hint);
        offlineDialog = new Dialog(this, R.style.EulixDialog);
        offlineDialog.setCancelable(false);
        offlineDialog.setContentView(offlineDialogView);

        View loginExpireDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_error_one_button_dialog_style_2, null);
        loginExpireImage = loginExpireDialogView.findViewById(R.id.dialog_image);
        loginExpireTitle = loginExpireDialogView.findViewById(R.id.dialog_title);
        loginExpireContent = loginExpireDialogView.findViewById(R.id.dialog_content);
        loginExpireConfirm = loginExpireDialogView.findViewById(R.id.dialog_button);
        loginExpireDialog = new Dialog(this, R.style.EulixDialog);
        loginExpireDialog.setCancelable(false);
        loginExpireDialog.setContentView(loginExpireDialogView);

        View granteeRequestLoginDialogView = LayoutInflater.from(this).inflate(R.layout.grantee_request_login_dialog, null);
        granteeRequestLoginAvatar = granteeRequestLoginDialogView.findViewById(R.id.granter_avatar);
        granteeRequestLoginNickname = granteeRequestLoginDialogView.findViewById(R.id.granter_nickname);
        granteeRequestLoginDomain = granteeRequestLoginDialogView.findViewById(R.id.granter_domain);
        granteeRequestLoginCancel = granteeRequestLoginDialogView.findViewById(R.id.grantee_cancel_request);
        granteeRequestLoginDialog = new Dialog(this, R.style.EulixDialog);
        granteeRequestLoginDialog.setCancelable(false);
        granteeRequestLoginDialog.setContentView(granteeRequestLoginDialogView);

        View spacePlatformErrorDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        spacePlatformErrorDialogTitle = spacePlatformErrorDialogView.findViewById(R.id.dialog_title);
        spacePlatformErrorDialogContent = spacePlatformErrorDialogView.findViewById(R.id.dialog_content);
        spacePlatformErrorDialogCancel = spacePlatformErrorDialogView.findViewById(R.id.dialog_cancel);
        spacePlatformErrorDialogConfirm = spacePlatformErrorDialogView.findViewById(R.id.dialog_confirm);
        spacePlatformErrorDialog = new Dialog(this, R.style.EulixDialog);
        spacePlatformErrorDialog.setCancelable(false);
        spacePlatformErrorDialog.setContentView(spacePlatformErrorDialogView);

        View spaceDeleteDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ao_space_delete, null);
        spaceDeleteDialogTitle = spaceDeleteDialogView.findViewById(R.id.dialog_title);
        spaceDeleteDialogContent = spaceDeleteDialogView.findViewById(R.id.dialog_content);
        spaceDeleteDialogHint = spaceDeleteDialogView.findViewById(R.id.dialog_hint);
        spaceDeleteDialogConfirm = spaceDeleteDialogView.findViewById(R.id.dialog_confirm);
        spaceDeleteDialogCancel = spaceDeleteDialogView.findViewById(R.id.dialog_cancel);
        spaceDeleteDialog = new BottomDialog(this);
        spaceDeleteDialog.setCancelable(false);
        spaceDeleteDialog.setContentView(spaceDeleteDialogView);
    }

    @Override
    public void initData() {
        mHandler = new EulixDeviceListHandler(this);
        isChooseBox = false;
        EventBusUtil.register(this);
        clientUuid = DataUtil.getClientUuid(getApplicationContext());
//        mHandler.sendEmptyMessage(QUERY_BOX_STATUS);
    }

    @Override
    public void initViewData() {
        offlineImage.setImageResource(R.drawable.offline_error_2x);
        offlineTitle.setText(R.string.device_offline_title);
        offlineContent.setText(R.string.device_offline_content);
        offlineUseButton.setText(R.string.offline_use);
        offlineHint.setText(R.string.device_offline_hint);
        offlineExit.setVisibility(View.VISIBLE);

        loginExpireImage.setImageResource(R.drawable.login_expired_2x);
        loginExpireTitle.setText(R.string.login_expire);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String loginExpireContentPart1 = getString(R.string.login_expire_content_part_1);
        String loginMoreSpaceContent = getString(R.string.login_more_space);
        String loginExpireContentPart2 = getString(R.string.login_expire_content_part_2);
        spannableStringBuilder.append(loginExpireContentPart1);
        spannableStringBuilder.append(loginMoreSpaceContent);
        spannableStringBuilder.append(loginExpireContentPart2);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                dismissLoginExpireDialog();
                performClickLoginMoreSpace();
            }
        };
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
        int highlightStart = loginExpireContentPart1.length();
        int highlightEnd = (loginExpireContentPart1.length() + loginMoreSpaceContent.length());
        spannableStringBuilder.setSpan(clickableSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.setSpan(foregroundColorSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginExpireContent.setMovementMethod(LinkMovementMethod.getInstance());
        loginExpireContent.setText(spannableStringBuilder);
        loginExpireConfirm.setText(R.string.ok);

        title.setText(R.string.login);
        functionText.setVisibility(View.GONE);

//        SpannableStringBuilder emptyHintStringBuilder = new SpannableStringBuilder();
//        String emptyHintPart1 = getString(R.string.join_beta_description_part_1);
//        String trialFreeText = getString(R.string.join_beta);
//        String emptyHintPart2 = getString(R.string.join_beta_description_part_2);
//        emptyHintStringBuilder.append(emptyHintPart1);
//        emptyHintStringBuilder.append(trialFreeText);
//        emptyHintStringBuilder.append(emptyHintPart2);
//        ClickableSpan emptyClickableHighlightSpan = new ClickableSpan() {
//            @Override
//            public void onClick(@NonNull View widget) {
//                performClickTrialFree();
//            }
//        };
//        ForegroundColorSpan emptyForegroundHighlightSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
//        int emptyHighlightStart = emptyHintPart1.length();
//        int emptyHighlightEnd = emptyHighlightStart + trialFreeText.length();
//        emptyHintStringBuilder.setSpan(emptyClickableHighlightSpan, emptyHighlightStart, emptyHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        emptyHintStringBuilder.setSpan(emptyForegroundHighlightSpan, emptyHighlightStart, emptyHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        deviceListEmptyHint.setMovementMethod(LinkMovementMethod.getInstance());
//        deviceListEmptyHint.setText(emptyHintStringBuilder);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        functionText.setOnClickListener(this);
        bindDevice.setOnClickListener(this);
//        loginMoreDevices.setOnClickListener(this);

        swipeRefreshContainer.setOnRefreshListener(() -> {
            if (mHandler != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    while (mHandler.hasCallbacks(resetSwipeRefreshRunnable)) {
                        mHandler.removeCallbacks(resetSwipeRefreshRunnable);
                    }
                } else {
                    try {
                        mHandler.removeCallbacks(resetSwipeRefreshRunnable);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (eulixUsers != null && !eulixUsers.isEmpty()) {
                EventBusUtil.post(new BoxOnlineRequestEvent(false));
                if (mHandler != null) {
                    mHandler.postDelayed(resetSwipeRefreshRunnable, 15000);
                }
            } else {
                swipeRefreshContainer.setRefreshing(false);
            }
        });

        offlineUseButton.setOnClickListener(v -> {
            dismissOfflineDialog();
            isChooseBox = true;
            boolean isOnline = false;
            int status = EulixSpaceDBUtil.getDeviceStatus(getApplicationContext(), mOfflineBoxUuid, mOfflineBoxBind);
            if (status >= 0) {
                isOnline = DataUtil.isSpaceStatusOnline(status, false);
            }
            updateDevice(mOfflineBoxUuid, mOfflineBoxBind, true, isOnline);
        });

        offlineExit.setOnClickListener(v -> dismissOfflineDialog());

        loginExpireConfirm.setOnClickListener(v -> dismissLoginExpireDialog());

        granteeRequestLoginCancel.setOnClickListener(v -> {
            resetDevice(false, null, null);
            dismissGranteeRequestLoginDialog();
        });

        spacePlatformErrorDialogCancel.setOnClickListener(v -> {
            dismissSpacePlatformErrorDialog();
            spacePlatformErrorDialogTitle.setText("");
            spacePlatformErrorDialogContent.setText("");
            spacePlatformErrorDialogConfirm.setText("");
            spacePlatformErrorBoxUuid = null;
            spacePlatformErrorBoxBind = null;
        });
        spacePlatformErrorDialogConfirm.setOnClickListener(v -> {
            if (presenter != null) {
                presenter.deleteBox(spacePlatformErrorBoxUuid, spacePlatformErrorBoxBind);
            }
            dismissSpacePlatformErrorDialog();
            spacePlatformErrorDialogTitle.setText("");
            spacePlatformErrorDialogContent.setText("");
            spacePlatformErrorDialogConfirm.setText("");
            spacePlatformErrorBoxUuid = null;
            spacePlatformErrorBoxBind = null;
            updateDeviceList();
        });
        spaceDeleteDialogConfirm.setOnClickListener(v -> {
            if (presenter != null) {
                presenter.deleteBox(spaceDeleteBoxUuid, spaceDeleteBoxBind);
            }
            dismissSpaceDeleteDialog();
            resetSpaceDeleteDialog();
            spaceDeleteBoxUuid = null;
            spaceDeleteBoxBind = null;
            updateDeviceList();
        });
        spaceDeleteDialogCancel.setOnClickListener(v -> {
            dismissSpaceDeleteDialog();
            resetSpaceDeleteDialog();
            spaceDeleteBoxUuid = null;
            spaceDeleteBoxBind = null;
        });

        initListener();
        initAdapter();
    }

    private void resetSwipeRefreshEvent() {
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setRefreshing(false);
        }
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected int getActivityIndex() {
        return ConstantField.ActivityIndex.EULIX_DEVICE_LIST_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public EulixDeviceListPresenter createPresenter() {
        return new EulixDeviceListPresenter();
    }

    private void initListener() {
        // todo 留后门，盒子需要将/etc/bp/system-agent.yml的qamode置为true，正式版本剔除
//        bindDevice.setOnLongClickListener(v -> {
//            Intent bindIntent = new Intent(EulixDeviceListActivity.this, LanBindBoxActivity.class);
//            bindIntent.putExtra(ConstantField.BLUETOOTH_ID, "4bba8f9e44338bf7");
//            startActivityForResult(bindIntent, ConstantField.RequestCode.BIND_DEVICE_CODE);
//            return true;
//        });

        // TODO 后门：切换环境

        View testView = LayoutInflater.from(this).inflate(R.layout.test_environment_switch_dialog, null);
        Button en1 = testView.findViewById(R.id.environment_rc_xyz);
        Button en2 = testView.findViewById(R.id.environment_rc_top);
        Button en3 = testView.findViewById(R.id.environment_dev);
        Button en4 = testView.findViewById(R.id.environment_test);
        Button en5 = testView.findViewById(R.id.environment_qa);
        Button en6 = testView.findViewById(R.id.environment_sit);
        Button en7 = testView.findViewById(R.id.environment_prod);
        en1.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(-1);
        });
        en2.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(0);
        });
        en3.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(1);
        });
        en4.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(2);
        });
        en5.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(3);
        });
        en6.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(4);
        });
        en7.setOnClickListener(v -> {
            dismissTestEnvironmentDialog();
            DebugUtil.setEnvironmentIndex(5);
        });
        testEnvironmentDialog = new BottomDialog(this);
        testEnvironmentDialog.setCancelable(false);
        testEnvironmentDialog.setContentView(testView);
        title.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 1000) {
                clickCount += 1;
            } else {
                clickCount = 1;
            }
            lastClickTime = currentTime;
            if (clickCount >= 5) {
                clickCount = 0;
                int enIndex = DebugUtil.getEnvironmentIndex();
                en1.setTextColor(getResources().getColor(enIndex == -1 ? R.color.blue_ff337aff : R.color.black_ff333333));
                en2.setTextColor(getResources().getColor(enIndex == 0 ? R.color.blue_ff337aff : R.color.black_ff333333));
                en3.setTextColor(getResources().getColor(enIndex == 1 ? R.color.blue_ff337aff : R.color.black_ff333333));
                en4.setTextColor(getResources().getColor(enIndex == 2 ? R.color.blue_ff337aff : R.color.black_ff333333));
                en5.setTextColor(getResources().getColor(enIndex == 3 ? R.color.blue_ff337aff : R.color.black_ff333333));
                en6.setTextColor(getResources().getColor(enIndex == 4 ? R.color.blue_ff337aff : R.color.black_ff333333));
                en7.setTextColor(getResources().getColor(enIndex == 5 ? R.color.blue_ff337aff : R.color.black_ff333333));
                showTestEnvironmentDialog();
            }
        });
    }

    private void showTestEnvironmentDialog() {
        if (testEnvironmentDialog != null && !testEnvironmentDialog.isShowing()) {
            testEnvironmentDialog.show();
            Window window = testEnvironmentDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.dp_360));
            }
        }
    }

    private void dismissTestEnvironmentDialog() {
        if (testEnvironmentDialog != null && testEnvironmentDialog.isShowing()) {
            testEnvironmentDialog.dismiss();
        }
    }

    private void initAdapter() {
        adapter = new EulixUserAdapter(this, ConstantField.ViewType.BOX_SPACE_VIEW, eulixUsers);
        adapter.setOnItemClickListener(this);
        eulixDeviceList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        eulixDeviceList.addItemDecoration(new EulixUserAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_10)), Color.TRANSPARENT));
        eulixDeviceList.setAdapter(adapter);
    }

    private void updateDeviceList() {
        eulixUsers = new ArrayList<>();
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext());
        boolean isDeviceActive = false;
        List<EulixSpaceInfo> activeEulixSpaceInfoList = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                    EulixSpaceInfo eulixSpaceInfo = handleBoxValue(boxValue);
                    if (eulixSpaceInfo != null) {
                        if (activeEulixSpaceInfoList == null) {
                            activeEulixSpaceInfoList = new ArrayList<>();
                        }
                        String boxUuid = eulixSpaceInfo.getBoxUuid();
                        String boxBind = eulixSpaceInfo.getBoxBind();
                        if (boxUuid != null && boxBind != null) {
                            activeEulixSpaceInfoList.add(eulixSpaceInfo);
                            isDeviceActive = true;
                        }
                    }
                }
            }
        }
        if (eulixUsers != null && !eulixUsers.isEmpty()) {
            Collections.sort(eulixUsers, spaceStateComparator);
        }
        if (eulixUsers == null) {
            eulixUsers = new ArrayList<>();
        }
        int spaceSize = eulixUsers.size();
        EulixUser eulixUser = new EulixUser();
        eulixUser.setMenuType(EulixUser.MENU_TYPE_LOGIN_MORE_SPACE);
        eulixUsers.add(eulixUser);
        if (eulixUsers == null || eulixUsers.isEmpty()) {
            eulixDeviceList.setVisibility(View.INVISIBLE);
            eulixDeviceList.removeOnScrollListener(eulixDeviceListScrollListener);
            eulixDeviceEmptyContainer.setVisibility(View.VISIBLE);
        } else {
            eulixDeviceEmptyContainer.setVisibility(View.INVISIBLE);
            eulixDeviceList.setVisibility(View.VISIBLE);
            eulixDeviceList.addOnScrollListener(eulixDeviceListScrollListener);
            if (adapter != null) {
                adapter.updateData(eulixUsers);
            }
        }
        swipeRefreshContainer.setEnabled(spaceSize > 0);
        if (isDeviceActive && !activeEulixSpaceInfoList.isEmpty()) {
            for (EulixSpaceInfo info : activeEulixSpaceInfoList) {
                if (info != null) {
                    ApplicationLockEventInfo applicationLockEventInfo = DataUtil.getApplicationLockEventInfo(info.getBoxUuid(), info.getBoxBind());
                    if (applicationLockEventInfo != null) {
                        isDeviceActive = false;
                        break;
                    }
                }
            }
        }
        isActive = isDeviceActive;
        back.setVisibility(isDeviceActive ? View.VISIBLE : View.GONE);
        back.setClickable(isDeviceActive);
    }

    private EulixSpaceInfo handleBoxValue(Map<String, String> boxValue) {
        EulixSpaceInfo eulixSpaceInfo = null;
        boolean deviceActive = false;
        String statusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
        if (statusValue != null) {
            int status = -1;
            try {
                status = Integer.parseInt(statusValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if ((status >= ConstantField.EulixDeviceStatus.OFFLINE && status <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) || status == ConstantField.EulixDeviceStatus.INVALID) {
                EulixUser user = new EulixUser();
                String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                AOSpaceAccessBean aoSpaceAccessBean = null;
                if (presenter != null) {
                    aoSpaceAccessBean = presenter.getSpecificAOSpaceAccessBean(boxUuid, boxBind);
                }
                Boolean isInternetAccess = null;
                if (aoSpaceAccessBean != null) {
                    isInternetAccess = aoSpaceAccessBean.getInternetAccess();
                }
                user.setUuid(boxUuid);
                user.setBind(boxBind);
                user.setUserDomain(boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN));
                user.setSpaceState(status);
                user.setInternetAccess(isInternetAccess);
                String userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                if (userInfoValue != null) {
                    Map<String, UserInfo> userInfoMap = null;
                    try {
                        userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>() {
                        }.getType());
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    if (userInfoMap != null) {
                        Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                        boolean findClient = false;
                        boolean findAdmin = false;
                        String bind = user.getBind();
                        for (Map.Entry<String, UserInfo> entry : entrySet) {
                            if (entry != null) {
                                String uuid = entry.getKey();
                                UserInfo userInfo = entry.getValue();
                                if (uuid != null && userInfo != null) {
                                    boolean isClient;
                                    if (bind != null && !"1".equals(bind) && !"-1".equals(bind) && !"0".equals(bind)) {
                                        String aoId = userInfo.getUserId();
                                        isClient = (bind.equals(aoId));
                                    } else {
                                        isClient = uuid.equals(clientUuid);
                                    }
                                    if (isClient) {
                                        user.setNickName(userInfo.getNickName());
                                        user.setAvatarPath(userInfo.getAvatarPath());
                                        user.setUserId(userInfo.getUserId());
                                        findClient = true;
                                    }
                                    if (userInfo.isAdmin()) {
                                        user.setAdminNickname(userInfo.getNickName());
                                        findAdmin = true;
                                        user.setAdmin(uuid.equals(clientUuid));
                                    }
                                }
                            }
                            if (findClient && findAdmin) {
                                break;
                            }
                        }
                    }
                }
                eulixUsers.add(user);
                if (status == ConstantField.EulixDeviceStatus.ACTIVE || status == ConstantField.EulixDeviceStatus.OFFLINE_USE) {
                    eulixSpaceInfo = new EulixSpaceInfo();
                    eulixSpaceInfo.setBoxUuid(boxUuid);
                    eulixSpaceInfo.setBoxBind(boxBind);
                    deviceActive = true;
                }
            }
        }
        return eulixSpaceInfo;
    }

    private void updateDevice(String boxUuid, String boxBind, boolean isInit, boolean isOnline) {
        if (boxUuid != null && boxBind != null) {
            long currentTimestamp = System.currentTimeMillis();
            Long expireTimestamp = null;
            String loginValid = null;
            boolean isExpire = true;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
            if (boxValues != null && !isInit) {
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        String boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        if (boxTokenValue != null) {
                            EulixBoxToken eulixBoxToken = null;
                            try {
                                eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (eulixBoxToken != null) {
                                expireTimestamp = eulixBoxToken.getTokenExpire();
                                loginValid = eulixBoxToken.getLoginValid();
                            }
                            isExpire = (expireTimestamp == null || expireTimestamp < (currentTimestamp + 10 * 1000));
                        }
                        break;
                    }
                }
            }
            boolean isActiveOrObtainAccessToken = true;
            if (!"1".equals(boxBind) && !"-1".equals(boxBind) && loginValid != null) {
                long loginValidValue = -1L;
                try {
                    loginValidValue = Long.parseLong(loginValid);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (loginValidValue >= 0) {
                    isExpire = true;
                    isActiveOrObtainAccessToken = (loginValidValue > currentTimestamp);
                }
            }
            if (isActiveOrObtainAccessToken) {
                if (isOnline) {
                    if (!isExpire) {
                        activeDevice(boxUuid, boxBind, currentTimestamp, expireTimestamp, true, false);
                    } else if (isInit) {
                        obtainAccessToken(boxUuid, boxBind, false);
                    }
                } else {
                    activeDevice(boxUuid, boxBind, currentTimestamp, (expireTimestamp == null ? -1 : expireTimestamp), false, false);
                }
            } else {
                showLoading("");
                obtainAccessToken(boxUuid, boxBind, true);
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

    private void showLoginExpireDialog() {
        if (loginExpireDialog != null && !loginExpireDialog.isShowing()) {
            loginExpireDialog.show();
            Window window = loginExpireDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissLoginExpireDialog() {
        if (loginExpireDialog != null && loginExpireDialog.isShowing()) {
            loginExpireDialog.dismiss();
        }
    }

    private void showGranteeRequestLoginDialog() {
        if (granteeRequestLoginDialog != null && !granteeRequestLoginDialog.isShowing()) {
            granteeRequestLoginDialog.show();
            Window window = granteeRequestLoginDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_307), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private boolean isGranteeRequestLoginDialogShowing() {
        return (granteeRequestLoginDialog != null && granteeRequestLoginDialog.isShowing());
    }

    private void dismissGranteeRequestLoginDialog() {
        if (granteeRequestLoginDialog != null && granteeRequestLoginDialog.isShowing()) {
            granteeRequestLoginDialog.dismiss();
        }
    }

    private void prepareShowSpacePlatformErrorDialog(int code) {
        if (spacePlatformErrorDialogTitle != null) {
            switch (code) {
                case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                    spacePlatformErrorDialogTitle.setText(R.string.login_fail);
                    break;
                case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                    spacePlatformErrorDialogTitle.setText(R.string.domain_non_exist_title);
                    break;
                default:
                    break;
            }
        }
        if (spacePlatformErrorDialogContent != null) {
            switch (code) {
                case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                    spacePlatformErrorDialogContent.setText(R.string.redirect_invalid_content);
                    break;
                case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                    spacePlatformErrorDialogContent.setText(R.string.domain_non_exist_content);
                    break;
                default:
                    break;
            }
        }
        if (spacePlatformErrorDialogConfirm != null) {
            spacePlatformErrorDialogConfirm.setText(R.string.delete_login_record);
        }
    }

    private void prepareShowSpacePlatformErrorDialogInvalid() {
        if (spacePlatformErrorDialogTitle != null) {
            spacePlatformErrorDialogTitle.setText(R.string.trial_invalid_title);
        }
        if (spacePlatformErrorDialogContent != null) {
            spacePlatformErrorDialogContent.setText(R.string.trial_invalid_content_v2);
        }
        if (spacePlatformErrorDialogConfirm != null) {
            spacePlatformErrorDialogConfirm.setText(R.string.delete_record);
        }
    }

    private void showSpacePlatformErrorDialog() {
        if (spacePlatformErrorDialog != null && !spacePlatformErrorDialog.isShowing()) {
            spacePlatformErrorDialog.show();
            Window window = spacePlatformErrorDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissSpacePlatformErrorDialog() {
        if (spacePlatformErrorDialog != null && spacePlatformErrorDialog.isShowing()) {
            spacePlatformErrorDialog.dismiss();
        }
    }

    private void prepareShowSpaceDeleteDialog(String nickName, String domain, Boolean isInternetAccess) {
        if (spaceDeleteDialogTitle != null) {
            String nickNamePart = StringUtil.nullToEmpty(nickName);
            if (StringUtil.isNonBlankString(nickNamePart)) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                String contentPart1 = getString(R.string.binding_sure_to_clear_part_1);
                String contentPart2 = getString(R.string.binding_sure_to_clear_part_2);
                spannableStringBuilder.append(contentPart1);
                spannableStringBuilder.append(nickNamePart);
                spannableStringBuilder.append(contentPart2);
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
                int highlightStart = contentPart1.length();
                int highlightEnd = (highlightStart + nickNamePart.length());
                spannableStringBuilder.setSpan(foregroundColorSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spaceDeleteDialogTitle.setText(spannableStringBuilder);
            } else {
                spaceDeleteDialogTitle.setText(R.string.binding_sure_to_clear);
            }
        }
        if (spaceDeleteDialogContent != null) {
            if (isInternetAccess == null || isInternetAccess) {
                spaceDeleteDialogContent.setVisibility(View.VISIBLE);
                String content = (getString(R.string.domain) + getString(R.string.colon)
                        + getString(R.string.common_language_space) + StringUtil.nullToEmpty(domain));
                spaceDeleteDialogContent.setText(content);
            } else {
                spaceDeleteDialogContent.setText("");
                spaceDeleteDialogContent.setVisibility(View.GONE);
            }
        }
    }

    private void showSpaceDeleteDialog() {
        if (spaceDeleteDialog != null && !spaceDeleteDialog.isShowing()) {
            spaceDeleteDialog.show();
            Window window = spaceDeleteDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void resetSpaceDeleteDialog() {
        if (spaceDeleteDialogTitle != null) {
            spaceDeleteDialogTitle.setText("");
        }
        if (spaceDeleteDialogContent != null) {
            spaceDeleteDialogContent.setText("");
        }
    }

    private void dismissSpaceDeleteDialog() {
        if (spaceDeleteDialog != null && spaceDeleteDialog.isShowing()) {
            spaceDeleteDialog.dismiss();
        }
    }

    private void handleGranteeRequestLogin(EulixUser eulixUser) {
        String avatarPath = null;
        String nickname = null;
        String userDomain = null;
        Boolean isInternetAccess = null;
        if (eulixUser != null) {
            avatarPath = eulixUser.getAvatarPath();
            nickname = eulixUser.getNickName();
            userDomain = eulixUser.getUserDomain();
            isInternetAccess = eulixUser.getInternetAccess();
        }
        if (avatarPath == null) {
            granteeRequestLoginAvatar.setImageResource(R.drawable.icon_user_header_default);
        } else {
            GlideUtil.loadCircleFromPath(avatarPath, granteeRequestLoginAvatar);
        }
        if (nickname == null) {
            granteeRequestLoginNickname.setText("");
        } else {
            granteeRequestLoginNickname.setText(Html.fromHtml(("<font color='#337aff'>"
                    + nickname + " " + "</font><font color='#333333'>"
                    + getString(R.string.affiliate_eulix_space) + "</font>")));
        }
        if (isInternetAccess == null || isInternetAccess) {
            granteeRequestLoginDomain.setVisibility(View.VISIBLE);
            granteeRequestLoginDomain.setText(generateBaseUrl(userDomain));
        } else {
            granteeRequestLoginDomain.setText("");
            granteeRequestLoginDomain.setVisibility(View.GONE);
        }
    }

    private String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = "";
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = DebugUtil.getEnvironmentServices();
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    private void resetChooseSpace() {
        isChooseBox = false;
        mBoxUuid = null;
        mBoxBind = null;
    }

    private void resetDevice(boolean isStopLastSpace, String boxUuid, String boxBind) {
        mBoxUuid = null;
        mBoxBind = null;
        if (isStopLastSpace && boxUuid != null && boxBind != null) {
            String activeBoxUuid = null;
            String activeBoxBind = null;
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getActiveOrLastEulixSpace(getApplicationContext());
            if (eulixSpaceInfo != null) {
                activeBoxUuid = eulixSpaceInfo.getBoxUuid();
                activeBoxBind = eulixSpaceInfo.getBoxBind();
            }
            isStopLastSpace = (boxUuid.equals(activeBoxUuid) && boxBind.equals(activeBoxBind));
        }
        updateDeviceList();
        if (isStopLastSpace) {
            DataUtil.setLastBoxToken(null);
            DataUtil.setLastEulixSpace(getApplicationContext(), null, null);
            Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, boxBind);
            if (boxAlarmId != null) {
                AlarmUtil.cancelAlarm(getApplicationContext(), boxAlarmId);
            }
        }
    }

    private void activeDevice(String boxUuid, String boxBind, long currentTimestamp, long expireTimestamp, boolean isOnline, boolean isUpdateTimestamp) {
        if (isOnline) {
            startPollPush();
        }
        mBoxUuid = null;
        mBoxBind = null;
        EulixSpaceDBUtil.readAppointPush(getApplicationContext(), boxUuid, boxBind, true);
        List<Map<String, String>> activeBoxValues = EulixSpaceDBUtil.queryBox(getApplicationContext()
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (activeBoxValues != null) {
            for (Map<String, String> activeBoxValue : activeBoxValues) {
                if (activeBoxValue != null && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String activeBoxUuid = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String activeBoxBind = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    Integer activeAlarmId = DataUtil.getTokenAlarmId(activeBoxUuid, activeBoxBind);
                    if (activeAlarmId != null) {
                        AlarmUtil.cancelAlarm(getApplicationContext(), activeAlarmId);
                    }
                    Map<String, String> requestUseBoxValue = new HashMap<>();
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, activeBoxUuid);
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, activeBoxBind);
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS
                            , String.valueOf((("1".equals(activeBoxBind) || "-1".equals(activeBoxBind))
                                    ? ConstantField.EulixDeviceStatus.REQUEST_USE
                                    : ConstantField.EulixDeviceStatus.REQUEST_LOGIN)));
                    EulixSpaceDBUtil.updateBox(getApplicationContext(), requestUseBoxValue);
                }
            }
        }
        List<Map<String, String>> offlineUseBoxValues = EulixSpaceDBUtil.queryBox(getApplicationContext()
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        if (offlineUseBoxValues != null) {
            for (Map<String, String> offlineUseBox : offlineUseBoxValues) {
                if (offlineUseBox != null && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String offlineUseBoxUuid = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String offlineUseBoxBind = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    Map<String, String> offlineBoxValue = new HashMap<>();
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, offlineUseBoxUuid);
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, offlineUseBoxBind);
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE));
                    EulixSpaceDBUtil.updateBox(getApplicationContext(), offlineBoxValue);
                }
            }
        }
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isOnline ? ConstantField.EulixDeviceStatus.ACTIVE : ConstantField.EulixDeviceStatus.OFFLINE_USE));
        if (isUpdateTimestamp) {
            boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
        }
        EulixSpaceDBUtil.updateBox(getApplicationContext(), boxValue);
        updateDeviceList();
        EulixBoxTokenDetail eulixBoxTokenDetail = new EulixBoxTokenDetail();
        eulixBoxTokenDetail.setBoxUuid(boxUuid);
        eulixBoxTokenDetail.setBoxBind(boxBind);
        eulixBoxTokenDetail.setTokenExpire(expireTimestamp);
        DataUtil.setLastBoxToken(eulixBoxTokenDetail);
        DataUtil.setLastEulixSpace(getApplicationContext(), boxUuid, boxBind);
        Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, boxBind);
        if (boxAlarmId != null) {
            AlarmUtil.cancelAlarm(getApplicationContext(), boxAlarmId);
        }
        int alarmId = AlarmUtil.getAlarmId();
        DataUtil.setTokenAlarmId(boxUuid, boxBind, alarmId);
        long diffTimestamp = 60 * 1000L;
        if (expireTimestamp > currentTimestamp) {
            diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
            AlarmUtil.setAlarm(getApplicationContext(), (expireTimestamp - diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
        } else {
            AlarmUtil.setAlarm(getApplicationContext(), (currentTimestamp + diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
        }
        EulixSpaceDBUtil.offlineTemperateBox(getApplicationContext(), boxUuid, boxBind);
        if (mHandler != null) {
            mHandler.post(() -> {
                //切换盒子
                EventBusUtil.post(new BoxOnlineRequestEvent(true));
                Logger.d("zfy", "change box");
                AlreadyUploadedManager.getInstance().init(getApplicationContext());
                TransferTaskManager.getInstance().resetManagerData();
                LanManager.getInstance().setLanEnable(false);
                EventBusUtil.post(new LanStatusEvent(false));
                LanManager.getInstance().startPollCheckTask();
                EventBusUtil.post(new BoxStatusEvent(true));
                EventBusUtil.post(new SpaceChangeEvent(true));
            });
        }
        DataUtil.resetApplicationLockEventInfo();
        EulixSpaceApplication.popAllOldActivity(this);
        if (isChooseBox) {
            isChooseBox = false;
            goMain();
//            finish();
        }
    }

    private void goMain() {
        Intent intent = new Intent(EulixDeviceListActivity.this, EulixMainActivity.class);
        startActivity(intent);
        finish();
    }

    public void obtainAccessToken(String boxUuid, String boxBind, boolean isForce) {
        if (!isForce) {
            stopPollPush();
        }
        Intent serviceIntent = new Intent(EulixDeviceListActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        serviceIntent.putExtra(ConstantField.BOX_UUID, boxUuid);
        serviceIntent.putExtra(ConstantField.BOX_BIND, boxBind);
        serviceIntent.putExtra(ConstantField.FORCE, true);
        startService(serviceIntent);
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
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

    private void performClickBindSpace() {
        if (!isClickBind) {
            if (mHandler != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    while (mHandler.hasCallbacks(resetClickBindRunnable)) {
                        mHandler.removeCallbacks(resetClickBindRunnable);
                    }
                } else {
                    try {
                        mHandler.removeCallbacks(resetClickBindRunnable);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            isClickBind = true;
//                        Intent bindIntent = new Intent(EulixDeviceListActivity.this, BindBoxActivity.class);
//                        startActivityForResult(bindIntent, ConstantField.RequestCode.BIND_DEVICE_CODE);
            Intent intent = new Intent(EulixDeviceListActivity.this, CaptureActivity.class);
            intent.setAction(Intents.Scan.ACTION);
            //全屏扫描
            intent.putExtra(Intents.Scan.WIDTH, ViewUtils.getScreenWidth(getApplicationContext()));
            intent.putExtra(Intents.Scan.HEIGHT, ViewUtils.getScreenHeight(getApplicationContext()));
            //只扫描二维码
            intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
            intent.putExtra(ConstantField.ZxingCommunication.FUNCTION_EXTRA_KEY, ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE);
            intent.putExtra(ConstantField.ZxingCommunication.IMMEDIATE_EXTRA_KEY, false);
            intent.putExtra(ConstantField.ZxingCommunication.DEFAULT_STATUS, "");
            intent.putExtra(ConstantField.ZxingCommunication.CUSTOMIZE_PATTERN, ConstantField.ZxingCommunication.CUSTOMIZE_PATTERN_SCAN_DEVICE_QR_CODE);
            intent.putExtra(ConstantField.ZxingCommunication.CUSTOMIZE_PATTERN_HINT, getString(R.string.scan_qr_code_on_browser));
            boolean isChinese = Utils.isChineseLanguage(this);
            intent.putExtra(ConstantField.ZxingCommunication.CUSTOMIZE_QR_TIP_RES_ID, isChinese ? R.drawable.icon_customize_scan_device_qr_tip : R.drawable.icon_customize_scan_device_qr_tip_en);
            startActivityForResult(intent, ConstantField.RequestCode.REQUEST_CODE_SCAN);
            if (mHandler != null) {
                mHandler.postDelayed(resetClickBindRunnable, 2000);
            } else {
                isClickBind = false;
            }
        }
    }

    private void performClickLoginMoreSpace() {
        if (!isClickLogin) {
            resetChooseSpace();
            if (mHandler != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    while (mHandler.hasCallbacks(resetClickLoginRunnable)) {
                        mHandler.removeCallbacks(resetClickLoginRunnable);
                    }
                } else {
                    try {
                        mHandler.removeCallbacks(resetClickLoginRunnable);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            isClickLogin = true;
            Intent loginIntent = new Intent(EulixDeviceListActivity.this, GranteeLoginActivity.class);
            startActivityForResult(loginIntent, ConstantField.RequestCode.LOGIN_DEVICE_CODE);
            if (mHandler != null) {
                mHandler.postDelayed(resetClickLoginRunnable, 2000);
            } else {
                isClickLogin = false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.BIND_DEVICE_CODE:
            case ConstantField.RequestCode.LOGIN_DEVICE_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    //刷新已上传记录对应文件
                    AlreadyUploadedManager.getInstance().init(getApplicationContext());
                    TransferTaskManager.getInstance().resetManagerData();
                    LanManager.getInstance().setLanEnable(false);
                    EventBusUtil.post(new LanStatusEvent(false));
                    LanManager.getInstance().startPollCheckTask();
                    EventBusUtil.post(new BoxStatusEvent(true));
                    finish();
                }
                break;
            case ConstantField.RequestCode.REQUEST_CODE_SCAN:
                String result = null;
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        if (bundle != null) {
                            result = bundle.getString(Intents.Scan.RESULT);
                            Logger.d(TAG, "qr code result: " + result);
                        }
                    }
                }
                if (result == null) {
                    if (mHandler != null) {
                        mHandler.post(() -> showImageTextToast(R.drawable.toast_refuse, R.string.qr_code_unrecognized));
                    }
                } else {
                    if (presenter.checkIsBindQrValid(result)) {
                        if (mHandler != null) {
                            String finalResult = result;
                            mHandler.post(() -> {
                                Intent intent = new Intent(EulixDeviceListActivity.this, AODeviceBindActivity.class);
                                intent.putExtra(ConstantField.QR_CODE_RESULT, finalResult);
                                startActivity(intent);
                            });
                        }
                    } else {
                        showImageTextToast(R.drawable.toast_refuse, R.string.app_server_not_match);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                while (mHandler.hasCallbacks(resetClickBindRunnable)) {
                    mHandler.removeCallbacks(resetClickBindRunnable);
                }
                while (mHandler.hasCallbacks(resetClickLoginRunnable)) {
                    mHandler.removeCallbacks(resetClickLoginRunnable);
                }
                while (mHandler.hasCallbacks(resetClickUnlockRunnable)) {
                    mHandler.removeCallbacks(resetClickUnlockRunnable);
                }
            } else {
                try {
                    mHandler.removeCallbacks(resetClickBindRunnable);
                    mHandler.removeCallbacks(resetClickLoginRunnable);
                    mHandler.removeCallbacks(resetClickUnlockRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        isClickBind = false;
        isClickLogin = false;
        isClickUnlock = false;
        updateDeviceList();
    }

    private boolean isApplicationLockEnable(String boxUuid, String boxBind) {
        boolean isLock = false;
        if (boxUuid != null && boxBind != null && presenter != null) {
            int biometricFeature = presenter.getBiometricFeature();
            ApplicationLockInfo applicationLockInfo = EulixBiometricManager.getInstance().getSpaceApplicationLock(boxUuid, boxBind);
            if (applicationLockInfo != null) {
                boolean isFingerprintUnlock = applicationLockInfo.isFingerprintUnlock();
                boolean isFaceUnlock = applicationLockInfo.isFaceUnlock();
                isLock = ((isFingerprintUnlock && (biometricFeature == 1 || biometricFeature >= 3))
                        || (isFaceUnlock && (biometricFeature == 2 || biometricFeature >= 3)));
            }
        }
        return isLock;
    }

    private String generateAuthenticateRequestId(String boxUuid, String boxBind) {
        if (mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                while (mHandler.hasCallbacks(resetClickUnlockRunnable)) {
                    mHandler.removeCallbacks(resetClickUnlockRunnable);
                }
            } else {
                try {
                    mHandler.removeCallbacks(resetClickUnlockRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        isClickUnlock = true;
        String authenticateRequestId = UUID.randomUUID().toString();
        if (authenticateRequestIds == null) {
            authenticateRequestIds = new ArrayList<>();
        }
        authenticateRequestIds.add(authenticateRequestId);
        if (authenticateEulixSpaceInfoMap == null) {
            authenticateEulixSpaceInfoMap = new HashMap<>();
        }
        EulixSpaceInfo eulixSpaceInfo = new EulixSpaceInfo();
        eulixSpaceInfo.setBoxUuid(boxUuid);
        eulixSpaceInfo.setBoxBind(boxBind);
        authenticateEulixSpaceInfoMap.put(authenticateRequestId, eulixSpaceInfo);
        return authenticateRequestId;
    }

    private EulixSpaceInfo resetAuthenticateRequestId(String requestId) {
        EulixSpaceInfo eulixSpaceInfo = null;
        if (requestId != null) {
            if (authenticateEulixSpaceInfoMap != null && authenticateEulixSpaceInfoMap.containsKey(requestId)) {
                eulixSpaceInfo = authenticateEulixSpaceInfoMap.remove(requestId);
            }
            if (authenticateRequestIds != null) {
                Iterator<String> iterator = authenticateRequestIds.iterator();
                while (iterator.hasNext()) {
                    String id = iterator.next();
                    if (requestId.equals(id)) {
                        iterator.remove();
                    }
                }
            }
        }
        return eulixSpaceInfo;
    }

    private void performOfflineUse(String boxUuid, String boxBind) {
        mOfflineBoxUuid = boxUuid;
        mOfflineBoxBind = boxBind;
        showOfflineDialog();
    }

    private void performOnlineUse(String boxUuid, String boxBind) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(getApplicationContext(), true);
        String activeBoxUuid = null;
        String activeBoxBind = null;
        if (eulixBoxBaseInfo != null) {
            activeBoxUuid = eulixBoxBaseInfo.getBoxUuid();
            activeBoxBind = eulixBoxBaseInfo.getBoxBind();
        }
        if (((mBoxUuid == null && !boxUuid.equals(activeBoxUuid)) || (mBoxUuid != null && !boxUuid.equals(mBoxUuid)))
                || ((mBoxBind == null && !boxBind.equals(activeBoxBind)) || (mBoxBind != null && !boxBind.equals(mBoxBind)))) {
            isChooseBox = true;
            mBoxUuid = boxUuid;
            mBoxBind = boxBind;
            if (adapter != null) {
                adapter.updateStateAndRefresh(boxUuid, boxBind);
            }
            updateDevice(boxUuid, boxBind, true, true);
        }
    }

    @Override
    protected void handleApplicationLock(String boxUuid, String boxBind, boolean isDisposable) {
        super.handleApplicationLock(boxUuid, boxBind, isDisposable);
        if (mHandler != null) {
            mHandler.postDelayed(resetClickUnlockRunnable, 2000);
        } else {
            isClickUnlock = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (isActive) {
            super.onBackPressed();
        } else {
            confirmForceExit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isActive || keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        } else {
            confirmForceExit();
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        EventBusUtil.unRegister(this);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void authAutoLoginResult(String boxUuid, String boxBind, int code, long expireTimestamp) {
        if (mHandler != null) {
            if (code >= 200 && code < 400) {
                if (mBoxUuid != null && mBoxBind != null) {
                    mHandler.post(() -> {
                        dismissGranteeRequestLoginDialog();
                        activeDevice(boxUuid, boxBind, System.currentTimeMillis(), expireTimestamp, true, true);
                    });
                }
            } else if (code == ConstantField.KnownError.AutoLoginError.AUTO_LOGIN_INVALID) {
                mHandler.post(() -> {
                    if (mBoxUuid != null && mBoxBind != null) {
                        if (presenter != null) {
                            presenter.deleteLoginInvalidSpace(boxUuid, boxBind);
                        }
                        resetDevice(false, null, null);
                        dismissGranteeRequestLoginDialog();
                        showLoginExpireDialog();
                    }
                });
            } else if (code == ConstantField.KnownError.AutoLoginError.LOGIN_REFUSE) {
                mHandler.post(() -> {
                    if (mBoxUuid != null && mBoxBind != null) {
                        resetDevice(false, null, null);
                        dismissGranteeRequestLoginDialog();
                        performClickLoginMoreSpace();
                    }
                });
            } else {
                if (code == ConstantField.KnownError.AutoLoginError.CONTINUE_WAITING) {
                    mHandler.post(() -> {
                        if (!isGranteeRequestLoginDialogShowing() && mBoxUuid != null && mBoxBind != null) {
                            EulixUser eulixUser = null;
                            if (presenter != null) {
                                eulixUser = presenter.getEulixUser(boxUuid, boxBind);
                            }
                            handleGranteeRequestLogin(eulixUser);
                            showGranteeRequestLoginDialog();
                        }
                    });
                }
                while (mHandler.hasMessages(AUTH_AUTO_LOGIN_PULL)) {
                    mHandler.removeMessages(AUTH_AUTO_LOGIN_PULL);
                }
                if (mBoxUuid != null && mBoxBind != null) {
                    Message message = mHandler.obtainMessage(AUTH_AUTO_LOGIN_PULL);
                    Bundle data = new Bundle();
                    data.putString(ConstantField.BOX_UUID, boxUuid);
                    data.putString(ConstantField.BOX_BIND, boxBind);
                    message.setData(data);
                    mHandler.sendMessageDelayed(message, SECOND_UNIT);
                }
            }
        }
    }

    private void authenticateCallback(boolean isSuccess, Integer code, String message, String responseId) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isSuccess) {
                    updateDeviceList();
                    String boxUuid = null;
                    String boxBind = null;
                    EulixSpaceInfo eulixSpaceInfo = resetAuthenticateRequestId(responseId);
                    if (eulixSpaceInfo != null) {
                        boxUuid = eulixSpaceInfo.getBoxUuid();
                        boxBind = eulixSpaceInfo.getBoxBind();
                    }
                    int status = -2;
                    if (presenter != null) {
                        status = presenter.getSpaceStatus(boxUuid, boxBind);
                    }
                    switch (status) {
                        case ConstantField.EulixDeviceStatus.OFFLINE:
                            performOfflineUse(boxUuid, boxBind);
                            break;
                        case ConstantField.EulixDeviceStatus.ACTIVE:
                        case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                            finish();
                            break;
                        case ConstantField.EulixDeviceStatus.REQUEST_LOGIN:
                        case ConstantField.EulixDeviceStatus.REQUEST_USE:
                            performOnlineUse(boxUuid, boxBind);
                            break;
                        default:
                            break;
                    }
                } else if (code != null) {
                    String boxUuid = null;
                    String boxBind = null;
                    EulixSpaceInfo eulixSpaceInfo = resetAuthenticateRequestId(responseId);
                    if (eulixSpaceInfo != null) {
                        boxUuid = eulixSpaceInfo.getBoxUuid();
                        boxBind = eulixSpaceInfo.getBoxBind();
                    }
                    if (code == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                        DataUtil.resetApplicationLockEventInfo();
                        int authenticateFeature = 0;
                        if (presenter != null) {
                            authenticateFeature = presenter.getBiometricFeature();
                            if (authenticateFeature > 0) {
                                switch (authenticateFeature) {
                                    case 1:
                                        if (boxUuid != null && boxBind != null) {
                                            presenter.setFingerprintLockInfo(boxUuid, boxBind, false);
                                        } else {
                                            presenter.setFingerprintLockInfo(false);
                                        }
                                        break;
                                    case 2:
                                        if (boxUuid != null && boxBind != null) {
                                            presenter.setFaceLockInfo(boxUuid, boxBind, false);
                                        } else {
                                            presenter.setFaceLockInfo(false);
                                        }
                                        break;
                                    default:
                                        if (boxUuid != null && boxBind != null) {
                                            presenter.setApplicationLockInfo(boxUuid, boxBind, false, false);
                                        } else {
                                            presenter.setApplicationLockInfo(false, false);
                                        }
                                        break;
                                }
                            }
                        }
                    } else if (code == BiometricPrompt.ERROR_CANCELED || code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        handlePositiveCancelError(responseId);
                    }
                    if (message != null && !TextUtils.isEmpty(message)) {
                        showPureTextToast(message);
                    }
                    updateDeviceList();
                }
            });
        }
    }

    private boolean handleAuthenticateEvent(String requestId, String responseId) {
        return (requestId != null && requestId.equals(responseId) && authenticateRequestIds != null && authenticateRequestIds.contains(requestId));
    }

    private boolean isRedirectExpire(Integer code) {
        boolean isExpire = false;
        if (code != null) {
            switch (code) {
                case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                    isExpire = true;
                    break;
                default:
                    break;
            }
        }
        return isExpire;
    }

    private void performClickTrialFree() {
    }

    private void resetMenuScroll(String viewUuid) {
        if (eulixDeviceList != null) {
            int visibleCount = eulixDeviceList.getChildCount();
            for (int i = 0; i < visibleCount; i++) {
                View child = eulixDeviceList.getChildAt(i);
                if (child != null) {
                    adapter.resetMenuScroll(child, viewUuid);
                }
            }
        }
    }

    @Override
    public void authenticateResult(boolean isSuccess, String responseId, String requestId) {
        if (handleAuthenticateEvent(requestId, responseId)) {
            authenticateCallback(isSuccess, null, null, responseId);
        } else {
            super.authenticateResult(isSuccess, responseId, requestId);
        }
    }

    @Override
    public void authenticateError(int code, CharSequence errMsg, String responseId, String requestId) {
        if (handleAuthenticateEvent(requestId, responseId)) {
            DataUtil.resetApplicationLockEventInfo(requestId);
            authenticateCallback(false, code, (errMsg == null ? null : errMsg.toString().trim()), responseId);
        } else {
            super.authenticateError(code, errMsg, responseId, requestId);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleResult(isActive);
                    break;
                case R.id.function_text:
                    performClickTrialFree();
                    break;
                case R.id.bind_device:
                    resetChooseSpace();
                    String[] unGetPerList = PermissionUtils.unGetPermissions(this, PermissionUtils.PERMISSION_CAMERA);
                    if (unGetPerList.length == 0) {
                        performClickBindSpace();
                    } else {
                        PermissionUtils.requestPermissionGroupWithNotice(this, unGetPerList, (result, extraMsg) -> {
                            if (result) {
                                performClickBindSpace();
                            }
                        });
                    }
                    break;
//                case R.id.login_more_devices:
//                    performClickLoginMoreSpace();
//                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (adapter != null && eulixUsers != null && position >= 0 && eulixUsers.size() > position) {
            EulixUser eulixUser = eulixUsers.get(position);
            if (eulixUser != null) {
                int menuType = eulixUser.getMenuType();
                switch (menuType) {
                    case EulixUser.MENU_TYPE_LOGIN_MORE_SPACE:
                        performClickLoginMoreSpace();
                        break;
                    default:
                        String boxUuid = eulixUser.getUuid();
                        String boxBind = eulixUser.getBind();
                        if (boxUuid != null && boxBind != null) {
                            mBoxUuid = boxUuid;
                            mBoxBind = boxBind;
                            int code = -1;
                            if (presenter != null) {
                                code = presenter.getSpaceStatusResponseCode(boxUuid, boxBind);
                            }
                            if (isRedirectExpire(code)) {
                                spacePlatformErrorBoxUuid = boxUuid;
                                spacePlatformErrorBoxBind = boxBind;
                                prepareShowSpacePlatformErrorDialog(code);
                                showSpacePlatformErrorDialog();
                            } else {
                                Map<String, String> queryMap = new HashMap<>();
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
                                if (boxValues != null && boxValues.size() == 1) {
                                    int spaceState = eulixUser.getSpaceState();
                                    if (spaceState == ConstantField.EulixDeviceStatus.INVALID) {
                                        spacePlatformErrorBoxUuid = boxUuid;
                                        spacePlatformErrorBoxBind = boxBind;
                                        prepareShowSpacePlatformErrorDialogInvalid();
                                        showSpacePlatformErrorDialog();
                                    } else {
                                        if (spaceState == ConstantField.EulixDeviceStatus.OFFLINE) {
                                            if (isApplicationLockEnable(boxUuid, boxBind) && presenter != null) {
                                                if (!isClickUnlock) {
                                                    String authenticateRequestId = generateAuthenticateRequestId(boxUuid, boxBind);
                                                    presenter.authenticate(boxUuid, boxBind, authenticateRequestId);
                                                    handleApplicationLock(boxUuid, boxBind, true);
                                                }
                                            } else {
                                                DataUtil.resetApplicationLockEventInfo();
                                                performOfflineUse(boxUuid, boxBind);
                                            }
                                        } else if (spaceState == ConstantField.EulixDeviceStatus.ACTIVE || spaceState == ConstantField.EulixDeviceStatus.OFFLINE_USE) {
                                            finish();
                                        } else if (spaceState == ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED || spaceState == ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED) {
                                            if (!isClickBind) {
                                                if (mHandler != null) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        while (mHandler.hasCallbacks(resetClickBindRunnable)) {
                                                            mHandler.removeCallbacks(resetClickBindRunnable);
                                                        }
                                                    } else {
                                                        try {
                                                            mHandler.removeCallbacks(resetClickBindRunnable);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }
                                                isClickBind = true;
                                                String bluetoothAddress = null;
                                                String bluetoothId = null;
                                                String bluetoothDeviceName = null;
                                                if (presenter != null) {
                                                    EulixDeviceManageInfo eulixDeviceManageInfo = presenter.getManageInfo(boxUuid, boxBind);
                                                    if (eulixDeviceManageInfo != null) {
                                                        bluetoothAddress = eulixDeviceManageInfo.getBluetoothAddress();
                                                        bluetoothId = eulixDeviceManageInfo.getBluetoothId();
                                                        bluetoothDeviceName = eulixDeviceManageInfo.getBluetoothDeviceName();
                                                    }
                                                }
                                                Intent bindIntent = new Intent(EulixDeviceListActivity.this, AODeviceBindActivity.class);
                                                if (bluetoothAddress != null) {
                                                    bindIntent.putExtra(ConstantField.BLUETOOTH_ADDRESS, bluetoothAddress);
                                                }
                                                if (bluetoothId != null) {
                                                    bindIntent.putExtra(ConstantField.BLUETOOTH_ID, bluetoothId);
                                                }
                                                if (bluetoothDeviceName != null) {
                                                    bindIntent.putExtra(ConstantField.DEVICE_NAME, bluetoothDeviceName);
                                                }
                                                startActivityForResult(bindIntent, ConstantField.RequestCode.BIND_DEVICE_CODE);
                                                if (mHandler != null) {
                                                    mHandler.postDelayed(resetClickBindRunnable, 2000);
                                                } else {
                                                    isClickBind = false;
                                                }
                                            }
                                        } else {
                                            if (isApplicationLockEnable(boxUuid, boxBind) && presenter != null) {
                                                if (!isClickUnlock) {
                                                    String authenticateRequestId = generateAuthenticateRequestId(boxUuid, boxBind);
                                                    presenter.authenticate(boxUuid, boxBind, authenticateRequestId);
                                                    handleApplicationLock(boxUuid, boxBind, true);
                                                }
                                            } else {
                                                DataUtil.resetApplicationLockEventInfo();
                                                isChooseBox = true;
                                                adapter.updateState(position);
                                                updateDevice(boxUuid, boxBind, true, true);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

//    @Override
//    public boolean onItemLongClick(View view, int position) {
//        boolean isHandle = false;
//        if (adapter != null && eulixUsers != null && position >= 0 && eulixUsers.size() > position) {
//            EulixUser eulixUser = eulixUsers.get(position);
//            if (eulixUser != null) {
//                String boxUuid = eulixUser.getUuid();
//                String boxBind = eulixUser.getBind();
//                if (boxUuid != null && boxBind != null) {
//                    isHandle = true;
//                    spaceDeleteBoxUuid = boxUuid;
//                    spaceDeleteBoxBind = boxBind;
//                    AOSpaceAccessBean aoSpaceAccessBean = null;
//                    if (presenter != null) {
//                        aoSpaceAccessBean = presenter.getSpecificAOSpaceAccessBean(boxUuid, boxBind);
//                    }
//                    Boolean isInternetAccess = null;
//                    if (aoSpaceAccessBean != null) {
//                        isInternetAccess = aoSpaceAccessBean.getInternetAccess();
//                    }
//                    prepareShowSpaceDeleteDialog(eulixUser.getNickName(), generateBaseUrl(eulixUser.getUserDomain()), isInternetAccess);
//                    showSpaceDeleteDialog();
//                }
//            }
//        }
//        return isHandle;
//    }

    @Override
    public void onLeftSwipeCallback(String viewUuid) {
        resetMenuScroll(viewUuid);
    }

    @Override
    public void onMenuClick(int menuFunction, int position) {
        switch (menuFunction) {
            case EulixUser.MENU_TYPE_LOGIN_MORE_SPACE:
                if (adapter != null && eulixUsers != null && position >= 0 && eulixUsers.size() > position) {
                    EulixUser eulixUser = eulixUsers.get(position);
                    if (eulixUser != null) {
                        String boxUuid = eulixUser.getUuid();
                        String boxBind = eulixUser.getBind();
                        if (boxUuid != null && boxBind != null) {
                            spaceDeleteBoxUuid = boxUuid;
                            spaceDeleteBoxBind = boxBind;
                            AOSpaceAccessBean aoSpaceAccessBean = null;
                            if (presenter != null) {
                                aoSpaceAccessBean = presenter.getSpecificAOSpaceAccessBean(boxUuid, boxBind);
                            }
                            Boolean isInternetAccess = null;
                            if (aoSpaceAccessBean != null) {
                                isInternetAccess = aoSpaceAccessBean.getInternetAccess();
                            }
                            prepareShowSpaceDeleteDialog(eulixUser.getNickName(), generateBaseUrl(eulixUser.getUserDomain()), isInternetAccess);
                            showSpaceDeleteDialog();
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AccessTokenCreateEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            boolean isRetry = event.isRetry();
            if (mBoxUuid != null && mBoxBind != null && mBoxUuid.equals(boxUuid) && mBoxBind.equals(boxBind)) {
                if (!isRetry) {
                    Boolean isOnline = null;
                    int status = EulixSpaceDBUtil.getDeviceStatus(getApplicationContext(), mBoxUuid, mBoxBind);
                    if (status >= 0) {
                        isOnline = DataUtil.isSpaceStatusOnline(status, false);
                    } else if (status == -2) {
                        // 该设备消失
                        resetDevice(true, mBoxUuid, mBoxBind);
                    }
                    if (isOnline != null) {
                        updateDevice(mBoxUuid, mBoxBind, false, isOnline);
                    }
                } else if (mHandler != null) {
                    mHandler.postDelayed(() -> obtainAccessToken(mBoxUuid, mBoxBind, event.isForce())
                            , (10 * ConstantField.TimeUnit.SECOND_UNIT));
                } else {
                    obtainAccessToken(mBoxUuid, mBoxBind, event.isForce());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxInsertDeleteEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null && ((boxUuid.equals(mBoxUuid) && boxBind.equals(mBoxBind)))) {
                resetDevice(!event.isInsert(), boxUuid, boxBind);
            } else {
                updateDeviceList();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpaceValidEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null && ((boxUuid.equals(mBoxUuid) && boxBind.equals(mBoxBind)))) {
                resetDevice(!event.isValid(), boxUuid, boxBind);
            } else {
                updateDeviceList();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxOnlineEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            int spaceStatus = event.getStatus();
            if (eulixUsers != null && boxUuid != null && boxBind != null) {
                for (EulixUser user : eulixUsers) {
                    if (user != null && boxUuid.equals(user.getUuid()) && boxBind.equals(user.getBind()) && spaceStatus != user.getSpaceState()) {
                        updateDeviceList();
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxAllCheckedEvent event) {
        if (event != null) {
            if (mHandler != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    while (mHandler.hasCallbacks(resetSwipeRefreshRunnable)) {
                        mHandler.removeCallbacks(resetSwipeRefreshRunnable);
                    }
                } else {
                    try {
                        mHandler.removeCallbacks(resetSwipeRefreshRunnable);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (event.isAllCheckRedirect()) {
                    mHandler.post(() -> {
                        resetSwipeRefreshEvent();
                        updateDeviceList();
                    });
                } else {
                    mHandler.post(resetSwipeRefreshRunnable);
                }
            } else if (swipeRefreshContainer != null) {
                swipeRefreshContainer.setRefreshing(false);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AuthAutoLoginEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null && boxUuid.equals(mBoxUuid) && boxBind.equals(mBoxBind)) {
                closeLoading();
                int code = event.getCode();
                long expireTimestamp = event.getExpireTimestamp();
                boolean isHandle = false;
                if ((code >= 200 && code < 400)) {
                    isHandle = (expireTimestamp >= 0);
                } else {
                    switch (code) {
                        case ConstantField.KnownError.AutoLoginError.CONTINUE_WAITING:
                        case ConstantField.KnownError.AutoLoginError.AUTO_LOGIN_INVALID:
                        case ConstantField.KnownError.AutoLoginError.LOGIN_REFUSE:
                            isHandle = true;
                            break;
                        default:
                            break;
                    }
                }
                if (isHandle) {
                    authAutoLoginResult(boxUuid, boxBind, code, expireTimestamp);
                } else {
                    resetDevice(false, null, null);
                    showPureTextToast(R.string.login_exception);
                }
            }
        }
    }
}
