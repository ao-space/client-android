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

package xyz.eulix.space.ui.mine.developer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.SwitchPlatformTaskBean;
import xyz.eulix.space.bean.developer.SpacePlatformInfo;
import xyz.eulix.space.bridge.SpacePlatformEnvironmentBridge;
import xyz.eulix.space.bridge.SpacePlatformEnvironmentInnerBridge;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.network.agent.platform.SwitchStatusResult;
import xyz.eulix.space.presenter.SpacePlatformEnvironmentPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/17 17:01
 */
public class SpacePlatformEnvironmentActivity extends AbsActivity<SpacePlatformEnvironmentPresenter.ISpacePlatformEnvironment, SpacePlatformEnvironmentPresenter> implements SpacePlatformEnvironmentPresenter.ISpacePlatformEnvironment
        , View.OnClickListener, SpacePlatformEnvironmentBridge.SpacePlatformEnvironmentSinkCallback, SpacePlatformEnvironmentInnerBridge.SpacePlatformEnvironmentInnerSourceCallback {
    private static final int MESSAGE_QUERY_SWITCH_STATUS = 1;
    private TextView title;
    private ImageButton back;
    private TextView spacePlatformEnvironment;
    private TextView spacePlatformServerAddress;
    private TextView spacePlatformWhatBenefitTitle;
    private TextView spacePlatformWhatBenefitContent;
    private TextView spacePlatformHowTitle;
    private TextView spacePlatformHowContent;
    private TextView spacePlatformOtherPrecautionsTitle;
    private TextView spacePlatformOtherPrecautionsContent;
    private Button switchSpacePlatform;
    private TextView switchSuccessDialogTitle;
    private TextView switchSuccessDialogContent;
    private Button switchSuccessDialogConfirm;
    private Dialog switchSuccessDialog;
    // 记录真实环境
    private Boolean mIsPrivateSpacePlatform;
    // 记录逻辑流程环境
    private Boolean nIsPrivateSpacePlatform;
    private String mServerUrl;
    private String mTaskId;
    private SpacePlatformEnvironmentBridge mBridge;
    private SpacePlatformEnvironmentInnerBridge spacePlatformEnvironmentInnerBridge;
    private SpacePlatformEnvironmentHandler mHandler;
    private List<String> exceptionTaskIds;

    static class SpacePlatformEnvironmentHandler extends Handler {
        private WeakReference<SpacePlatformEnvironmentActivity> spacePlatformEnvironmentActivityWeakReference;

        public SpacePlatformEnvironmentHandler(SpacePlatformEnvironmentActivity activity) {
            spacePlatformEnvironmentActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            SpacePlatformEnvironmentActivity activity = spacePlatformEnvironmentActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                Object obj = msg.obj;
                switch (msg.what) {
                    case MESSAGE_QUERY_SWITCH_STATUS:
                        if (activity.mBridge != null && obj instanceof String) {
                            activity.mBridge.handleRequestSwitchStatus((String) obj);
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
        setContentView(R.layout.activity_space_platform_environment);
        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
        spacePlatformEnvironment = findViewById(R.id.space_platform_environment);
        spacePlatformServerAddress = findViewById(R.id.space_platform_server_address);
        spacePlatformWhatBenefitTitle = findViewById(R.id.space_platform_what_benefit_title);
        spacePlatformWhatBenefitContent = findViewById(R.id.space_platform_what_benefit_content);
        spacePlatformHowTitle = findViewById(R.id.space_platform_how_title);
        spacePlatformHowContent = findViewById(R.id.space_platform_how_content);
        spacePlatformOtherPrecautionsTitle = findViewById(R.id.space_platform_other_precautions_title);
        spacePlatformOtherPrecautionsContent = findViewById(R.id.space_platform_other_precautions_content);
        switchSpacePlatform = findViewById(R.id.switch_space_platform);

        View switchSuccessDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        switchSuccessDialogTitle = switchSuccessDialogView.findViewById(R.id.dialog_title);
        switchSuccessDialogContent = switchSuccessDialogView.findViewById(R.id.dialog_content);
        switchSuccessDialogConfirm = switchSuccessDialogView.findViewById(R.id.dialog_confirm);
        switchSuccessDialog = new Dialog(this, R.style.EulixDialog);
        switchSuccessDialog.setCancelable(false);
        switchSuccessDialog.setContentView(switchSuccessDialogView);
    }

    @Override
    public void initData() {
        mHandler = new SpacePlatformEnvironmentHandler(this);
        mIsPrivateSpacePlatform = null;
        mServerUrl = null;
        mBridge = SpacePlatformEnvironmentBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.switch_space_platform_environment);

        switchSuccessDialogTitle.setText(R.string.switch_success);
        switchSuccessDialogConfirm.setText(R.string.confirm_ok);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        switchSpacePlatform.setOnClickListener(this);

        switchSuccessDialogConfirm.setOnClickListener(v -> {
            dismissSwitchSuccessDialog();
            popAllAndGoMain();
        });

        if (presenter != null) {
            presenter.generateBoxUuidAndBind();
            queryTask();
        }
    }

    private boolean queryTask() {
        boolean isProgress = false;
        if (presenter != null) {
            List<SwitchPlatformTaskBean> taskIds = presenter.getTaskIds();
            List<String> expireTaskIds = new ArrayList<>();
            String nTaskId = null;
            if (taskIds != null && !taskIds.isEmpty()) {
                for (SwitchPlatformTaskBean switchPlatformTaskBean : taskIds) {
                    long taskTimestamp = switchPlatformTaskBean.getTaskTimestamp();
                    String taskId = switchPlatformTaskBean.getTaskId();
                    // 先不加超时
                    if (taskTimestamp < 0/* || (taskTimestamp + 10 * ConstantField.TimeUnit.MINUTE_UNIT) < System.currentTimeMillis()*/) {
                        expireTaskIds.add(taskId);
                    } else if (nTaskId == null && taskId != null && (exceptionTaskIds == null || !exceptionTaskIds.contains(taskId))) {
                        nTaskId = taskId;
                    }
                }
            }
            if (nTaskId != null) {
                showLoading(getString(R.string.switch_space_platform_hint));
                handleRefreshSwitchStatusEvent(nTaskId, 0);
                isProgress = true;
            }
            if (!expireTaskIds.isEmpty()) {
                for (String id : expireTaskIds) {
                    if (presenter != null) {
                        presenter.removeTaskId(id, false);
                    }
                }
            }
        }
        return isProgress;
    }

    private void handleRefreshSwitchStatusEvent(@NonNull String taskId, long delayMillis) {
        mTaskId = taskId;
        if (mHandler != null) {
            while (mHandler.hasMessages(MESSAGE_QUERY_SWITCH_STATUS)) {
                mHandler.removeMessages(MESSAGE_QUERY_SWITCH_STATUS);
            }
            Message message = mHandler.obtainMessage(MESSAGE_QUERY_SWITCH_STATUS, taskId);
            if (delayMillis > 0) {
                mHandler.sendMessageDelayed(message, delayMillis);
            } else {
                mHandler.sendMessage(message);
            }
        } else if (mBridge != null) {
            mBridge.handleRequestSwitchStatus(taskId);
        }
    }

    private void updateSpacePlatformView() {
        if (presenter != null) {
            SpacePlatformInfo spacePlatformInfo = presenter.getSpacePlatformInfo();
            if (spacePlatformInfo != null) {
                updateSpacePlatformView(spacePlatformInfo.isPrivateSpacePlatform(), StringUtil.nullToEmpty(spacePlatformInfo.getPlatformServerUrl()));
            }
        }
    }

    private void updateSpacePlatformView(boolean isPrivateSpacePlatform, @NonNull String platformServerUrl) {
        if (mIsPrivateSpacePlatform == null || mIsPrivateSpacePlatform != isPrivateSpacePlatform || !platformServerUrl.equals(mServerUrl)) {
            mIsPrivateSpacePlatform = isPrivateSpacePlatform;
            mServerUrl = platformServerUrl;
            if (spacePlatformEnvironment != null) {
                spacePlatformEnvironment.setText(isPrivateSpacePlatform
                        ? R.string.current_environment_private_space_platform : R.string.current_environment_official_space_platform);
            }
            if (spacePlatformServerAddress != null) {
                String content = getString(R.string.server_url_is) + platformServerUrl;
                spacePlatformServerAddress.setText(content);
            }
            if (spacePlatformWhatBenefitTitle != null) {
                spacePlatformWhatBenefitTitle.setText(isPrivateSpacePlatform
                        ? R.string.switch_official_space_platform_what_benefit : R.string.switch_private_space_platform_what_benefit);
            }
            if (spacePlatformWhatBenefitContent != null) {
                spacePlatformWhatBenefitContent.setText(isPrivateSpacePlatform
                        ? R.string.switch_official_space_platform_what_benefit_content : R.string.switch_private_space_platform_what_benefit_content);
            }
            if (spacePlatformHowTitle != null) {
                spacePlatformHowTitle.setText(isPrivateSpacePlatform
                        ? R.string.switch_official_space_platform_how : R.string.switch_private_space_platform_how);
            }
            if (spacePlatformOtherPrecautionsTitle != null) {
                spacePlatformOtherPrecautionsTitle.setText(isPrivateSpacePlatform ? R.string.precautions : R.string.other_precautions);
            }
            if (spacePlatformHowContent != null) {
                if (isPrivateSpacePlatform) {
                    spacePlatformHowContent.setText(R.string.switch_official_space_platform_how_content);
                } else {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                    String contentPart1 = getString(R.string.switch_private_space_platform_how_content_part_1);
                    final String downloadUrl = platformServerUrl + (platformServerUrl.endsWith("/") ? "" : "/") + "download/platform";
                    String spaceContent = " ";
                    String clickToCopyContent = getString(R.string.click_to_copy);
                    String newLineContent = "\n";
                    String contentPart2 = getString(R.string.switch_private_space_platform_how_content_part_2);
                    spannableStringBuilder.append(contentPart1);
                    spannableStringBuilder.append(downloadUrl);
                    spannableStringBuilder.append(spaceContent);
                    spannableStringBuilder.append(clickToCopyContent);
                    spannableStringBuilder.append(newLineContent);
                    spannableStringBuilder.append(contentPart2);
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            if (presenter != null && presenter.copyWebUrl(downloadUrl)) {
                                showImageTextToast(R.drawable.toast_right, R.string.copy_to_clipboard_success_3);
                            } else {
                                showImageTextToast(R.drawable.toast_refuse, R.string.copy_to_clipboard_failed);
                            }
                        }
                    };
                    ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
                    int highlightStart = (contentPart1.length() + downloadUrl.length() + spaceContent.length());
                    int highlightEnd = (highlightStart + clickToCopyContent.length());
                    spannableStringBuilder.setSpan(clickableSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableStringBuilder.setSpan(foregroundColorSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spacePlatformHowContent.setMovementMethod(LinkMovementMethod.getInstance());
                    spacePlatformHowContent.setText(spannableStringBuilder);
                }
            }
            if (spacePlatformOtherPrecautionsContent != null) {
                spacePlatformOtherPrecautionsContent.setText(isPrivateSpacePlatform
                        ? R.string.other_precautions_official_space_platform_content : R.string.other_precautions_private_space_platform_content);
            }
            if (switchSpacePlatform != null) {
                switchSpacePlatform.setText(isPrivateSpacePlatform ? R.string.switch_official_space_platform : R.string.switch_private_space_platform);
            }
        }
    }

    private void showSwitchSuccessDialog() {
        if (switchSuccessDialog != null && !switchSuccessDialog.isShowing()) {
            switchSuccessDialog.show();
            Window window = switchSuccessDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissSwitchSuccessDialog() {
        if (switchSuccessDialog != null && switchSuccessDialog.isShowing()) {
            switchSuccessDialog.dismiss();
        }
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    private void popAllAndGoMain() {
        EulixSpaceApplication.popAllOldActivity(this);
        Intent intent = new Intent(SpacePlatformEnvironmentActivity.this, EulixMainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateSpacePlatformView();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        handleResult(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleResult(false);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (spacePlatformEnvironmentInnerBridge != null) {
            spacePlatformEnvironmentInnerBridge.unregisterSourceCallback();
            spacePlatformEnvironmentInnerBridge = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected int getActivityIndex() {
        return DEVELOPER_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public SpacePlatformEnvironmentPresenter createPresenter() {
        return new SpacePlatformEnvironmentPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleResult(false);
                    break;
                case R.id.switch_space_platform:
                    nIsPrivateSpacePlatform = mIsPrivateSpacePlatform;
                    if (mIsPrivateSpacePlatform != null) {
                        if (mIsPrivateSpacePlatform) {
                            if (mBridge != null) {
                                showLoading(getString(R.string.switch_space_platform_hint));
                                String transId = String.valueOf(System.currentTimeMillis());
                                String url = DebugUtil.getOfficialEnvironmentServices();
                                if (presenter != null) {
                                    transId = presenter.addTaskId(false, url);
                                }
                                mTaskId = transId;
                                mBridge.handleRequestSwitchPlatform(transId, StringUtil.urlToHost(url));
                            }
                        } else {
                            spacePlatformEnvironmentInnerBridge = SpacePlatformEnvironmentInnerBridge.getInstance();
                            spacePlatformEnvironmentInnerBridge.registerSourceCallback(this);
                            Intent inputIntent = new Intent(SpacePlatformEnvironmentActivity.this, SpacePlatformInputActivity.class);
                            startActivity(inputIntent);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result, boolean isError) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (nIsPrivateSpacePlatform != null) {
                    boolean isSuccess = (code >= 200 && code < 400 && result != null);
                    String transId = mTaskId;
                    if (isSuccess) {
                        transId = result.getTransId();
                        EventBusUtil.post(new BoxOnlineRequestEvent(true, true));
                        if (presenter != null) {
                            presenter.updateBoxDomain(result.getUserDomain());
                        }
                    }
                    if (presenter != null && !isError) {
                        presenter.removeTaskId(transId, false);
                    }
                    if (transId != null && transId.equals(mTaskId)) {
                        mTaskId = null;
                    }
                    if (nIsPrivateSpacePlatform) {
                        closeLoading();
                        if (isSuccess) {
                            String content = getString(R.string.switch_official_space_platform_hint) + "\n" + DebugUtil.getOfficialEnvironmentServices();
                            switchSuccessDialogContent.setText(content);
                            showSwitchSuccessDialog();
                        } else {
                            boolean isHandleError = false;
                            if (ConstantField.KnownSource.AGENT.equals(source)) {
                                switch (code) {
                                    case ConstantField.KnownError.SwitchPlatformError.RESOURCE_BUSY_ERROR:
                                        isHandleError = true;
                                        showImageTextToast(R.drawable.toast_refuse, R.string.switch_space_platform_resource_busy_error_content);
                                        break;
                                    case ConstantField.KnownError.SwitchPlatformError.CONNECT_ERROR:
                                        isHandleError = true;
                                        showImageTextToast(R.drawable.toast_refuse, R.string.switch_space_platform_connect_error_content);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            if (!isHandleError) {
                                String errContent = getString(R.string.switch_fail_hint_part_1) + code + getString(R.string.switch_fail_hint_part_2);
                                showImageTextToast(R.drawable.toast_wrong, errContent);
                            }
                        }
                    } else if (spacePlatformEnvironmentInnerBridge != null) {
                        spacePlatformEnvironmentInnerBridge.handleSwitchPlatformResponse(code, source, result);
                    }
                    if (isSuccess) {
                        nIsPrivateSpacePlatform = null;
                    }
                }
            });
        }
    }

    @Override
    public void handleSwitchStatusResponse(int code, String source, SwitchStatusResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                String taskId = mTaskId;
                Boolean isTaskFinish = null;
                boolean isTaskSuccess = false;
                if (code >= 200 && code < 400 && result != null) {
                    String transId = result.getTransId();
                    if (transId != null) {
                        taskId = transId;
                    }
                    Integer status = result.getStatus();
                    if (status != null) {
                        switch (status) {
                            case SwitchStatusResult.STATUS_INIT:
                            case SwitchStatusResult.STATUS_START:
                            case SwitchStatusResult.STATUS_UPDATE_GATEWAY:
                            case SwitchStatusResult.STATUS_UPDATE_BOX_INFO:
                                isTaskFinish = false;
                                break;
                            case SwitchStatusResult.STATUS_ABORT:
                                isTaskFinish = true;
                                break;
                            case SwitchStatusResult.STATUS_OK:
                                isTaskFinish = true;
                                isTaskSuccess = true;
                                break;
                            default:
                                break;
                        }
                    }
                } else if (code == ConstantField.KnownError.SwitchPlatformError.TASK_NOT_FOUND_ERROR && ConstantField.KnownSource.AGENT.equals(source)){
                    isTaskFinish = true;
                }
                if (isTaskFinish == null || isTaskFinish) {
                    List<SwitchPlatformTaskBean> switchPlatformTaskBeanList = null;
                    if (isTaskFinish == null) {
                        if (exceptionTaskIds == null) {
                            exceptionTaskIds = new ArrayList<>();
                        }
                        if (taskId != null) {
                            exceptionTaskIds.add(taskId);
                        }
                    } else if (presenter != null) {
                        switchPlatformTaskBeanList = presenter.removeTaskId(taskId, true);
                    }
                    if (taskId != null && taskId.equals(mTaskId)) {
                        mTaskId = null;
                    }
                    if (!queryTask()) {
                        closeLoading();
                        if (isTaskSuccess) {
                            EventBusUtil.post(new BoxOnlineRequestEvent(true, true));
                            if (presenter != null) {
                                presenter.updateBoxDomain(result.getUserDomain());
                            }
                            String content = null;
                            if (switchPlatformTaskBeanList != null) {
                                for (SwitchPlatformTaskBean bean : switchPlatformTaskBeanList) {
                                    if (bean != null) {
                                        boolean isPrivatePlatform = bean.isPrivatePlatform();
                                        String platformUrl = bean.getPlatformUrl();
                                        if (platformUrl != null) {
                                            content = getString(isPrivatePlatform ? R.string.switch_private_space_platform_hint
                                                    : R.string.switch_official_space_platform_hint)
                                                    + "\n" + platformUrl;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (content != null) {
                                switchSuccessDialogContent.setText(content);
                                showSwitchSuccessDialog();
                            }
                        } else {
                            String errContent = getString(R.string.switch_fail_hint_part_1) + (code < 300 ? 500 : code) + getString(R.string.switch_fail_hint_part_2);
                            showImageTextToast(R.drawable.toast_wrong, errContent);
                            updateSpacePlatformView();
                        }
                    }
                } else if (taskId != null) {
                    handleRefreshSwitchStatusEvent(taskId, 5000);
                }
            });
        }
    }

    @Override
    public void handleDisconnect() {
        if (spacePlatformEnvironmentInnerBridge != null) {
            spacePlatformEnvironmentInnerBridge.handleDisconnect();
        }
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void handleRequestSwitchPlatform(String url) {
        if (mBridge != null) {
            String transId = String.valueOf(System.currentTimeMillis());
            if (presenter != null) {
                transId = presenter.addTaskId(true, url);
            }
            mTaskId = transId;
            mBridge.handleRequestSwitchPlatform(transId, StringUtil.urlToHost(url));
        }
    }
}
