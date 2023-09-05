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
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.mine.TerminalAdapter;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixTerminal;
import xyz.eulix.space.event.TerminalListEvent;
import xyz.eulix.space.event.TerminalResultEvent;
import xyz.eulix.space.presenter.LoginTerminalPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/21 17:27
 */
public class LoginTerminalActivity extends AbsActivity<LoginTerminalPresenter.ILoginTerminal, LoginTerminalPresenter> implements LoginTerminalPresenter.ILoginTerminal, View.OnClickListener, TerminalAdapter.OnItemClickListener {
    private ImageButton back;
    private TextView title;
    private SwipeRefreshLayout swipeRefreshContainer;
    private RecyclerView loginTerminalList;
    private Dialog offlineDialog;
    private TextView dialogTitle;
    private TextView dialogContent;
    private Button dialogCancel;
    private Button dialogConfirm;
    private LoginTerminalHandler mHandler;
    private TerminalAdapter mAdapter;
    private List<EulixTerminal> mEulixTerminals;
    private EulixTerminal mSelectTerminal;

    private Comparator<EulixTerminal> eulixTerminalComparator = new Comparator<EulixTerminal>() {
        @Override
        public int compare(EulixTerminal o1, EulixTerminal o2) {
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
        }
    };

    static class LoginTerminalHandler extends Handler {
        private WeakReference<LoginTerminalActivity> loginTerminalActivityWeakReference;

        public LoginTerminalHandler(LoginTerminalActivity activity) {
            loginTerminalActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            LoginTerminalActivity activity = loginTerminalActivityWeakReference.get();
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
        setContentView(R.layout.activity_login_terminal);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        swipeRefreshContainer = findViewById(R.id.swipe_refresh_container);
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
    public void initData() {
        mHandler = new LoginTerminalHandler(this);
        mEulixTerminals = new ArrayList<>();
    }

    @Override
    public void initViewData() {
        title.setText(R.string.login_terminal);
        dialogTitle.setText(R.string.offline_terminal);
        dialogConfirm.setText(R.string.confirm);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        dialogCancel.setOnClickListener(this);
        dialogConfirm.setOnClickListener(this);
        swipeRefreshContainer.setOnRefreshListener(this::requestTerminalList);
        mAdapter = new TerminalAdapter(this, mEulixTerminals);
        mAdapter.setOnItemClickListener(this);
        loginTerminalList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        loginTerminalList.addItemDecoration(new TerminalAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        loginTerminalList.setAdapter(mAdapter);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
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
                TerminalListEvent terminalListEvent = new TerminalListEvent(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind());
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

    @Override
    protected void onStart() {
        super.onStart();
//        mEulixTerminals = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            EulixTerminal eulixTerminal = new EulixTerminal();
//            eulixTerminal.setGranter(Math.random() < 0.5);
//            eulixTerminal.setMyself(Math.random() < 0.5);
//            double randomType = Math.random();
//            eulixTerminal.setTerminalType(randomType > 0.6 ? (randomType > 0.9 ? "unknown" : "android") : (randomType < 0.3 ? "ios" : "web"));
//            eulixTerminal.setTerminalName(randomType > 0.9 ? getString(R.string.unknown_terminal): String.valueOf(i));
//            eulixTerminal.setTerminalPlace("南京市");
//            eulixTerminal.setTerminalTimestamp(System.currentTimeMillis());
//            mEulixTerminals.add(eulixTerminal);
//        }
        updateTerminalList();
        requestTerminalList();
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
    protected int getActivityIndex() {
        return ConstantField.ActivityIndex.LOGIN_TERMINAL_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public LoginTerminalPresenter createPresenter() {
        return new LoginTerminalPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.dialog_confirm:
                    if (mSelectTerminal != null && presenter != null) {
                        presenter.offlineTerminal(mSelectTerminal.getTerminalUuid());
                    }
                    showLoading("");
                    mSelectTerminal = null;
                    dismissOfflineDialog();
                    break;
                case R.id.dialog_cancel:
                    mSelectTerminal = null;
                    dismissOfflineDialog();
                    break;
                default:
                    break;
            }
        }
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

    @Override
    public void handleTerminalOffline(int code, String customizeSource) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if ((code >= 200 && code < 400) || (code == ConstantField.KnownError.TerminalError.TERMINAL_OFFLINE_DUPLICATE_CODE && customizeSource != null && customizeSource.trim().toUpperCase().startsWith(ConstantField.KnownSource.ACCOUNT))) {
                    requestTerminalList();
                    showImageTextToast(R.drawable.toast_right, R.string.offline_success);
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.offline_fail);
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TerminalResultEvent event) {
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setRefreshing(false);
        }
        updateTerminalList();
    }
}
