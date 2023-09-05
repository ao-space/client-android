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

package xyz.eulix.space.ui.mine.security;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.mine.security.SecuritySettingAdapter;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.SecuritySettingBean;
import xyz.eulix.space.did.ui.SpaceAccountActivity;
import xyz.eulix.space.event.SecurityMessagePollRequestEvent;
import xyz.eulix.space.event.SecurityMessagePollResponseEvent;
import xyz.eulix.space.network.security.SecurityMessagePollResult;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.presenter.EulixSecuritySettingsPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.dialog.security.GranteeSecurityRequestDialog;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/7 15:39
 */
public class EulixSecuritySettingsActivity extends AbsActivity<EulixSecuritySettingsPresenter.IEulixSecuritySettings, EulixSecuritySettingsPresenter> implements EulixSecuritySettingsPresenter.IEulixSecuritySettings
        , View.OnClickListener, SecuritySettingAdapter.OnItemClickListener {
    private static final int GRANTEE_APPLY = 1;
    private ImageButton back;
    private TextView title;
    private RecyclerView securitySettingList;
    private SecuritySettingAdapter mAdapter;
    private List<SecuritySettingBean> mSecuritySettingBeanList;
    private GranteeSecurityRequestDialog granteeSecurityRequestDialog;
    private GranteeSecurityRequestDialog.GranteeSecurityRequestCallback granteeSecurityRequestCallback = null;
    private String granteeApplyId;
    private EulixSecuritySettingsHandler mHandler;

    static class EulixSecuritySettingsHandler extends Handler {
        private WeakReference<EulixSecuritySettingsActivity> eulixSecuritySettingsActivityWeakReference;

        public EulixSecuritySettingsHandler(EulixSecuritySettingsActivity activity) {
            eulixSecuritySettingsActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixSecuritySettingsActivity activity = eulixSecuritySettingsActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case GRANTEE_APPLY:
                        activity.granteeApplyId = null;
                        if (activity.granteeSecurityRequestDialog != null) {
                            activity.granteeSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.REQUEST_EXPIRE);
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
        setContentView(R.layout.activity_eulix_security_settings);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        securitySettingList = findViewById(R.id.security_setting_list);
    }

    @Override
    public void initData() {
        mHandler = new EulixSecuritySettingsHandler(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.security);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        mSecuritySettingBeanList = new ArrayList<>();
        SecuritySettingBean bean = new SecuritySettingBean(SecuritySettingBean.FUNCTION_SPACE_ACCOUNT);
        bean.setClick(true);
        mSecuritySettingBeanList.add(bean);
        mAdapter = new SecuritySettingAdapter(EulixSecuritySettingsActivity.this, mSecuritySettingBeanList);
        mAdapter.setOnItemClickListener(this);
        securitySettingList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        securitySettingList.addItemDecoration(new SecuritySettingAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimensionPixelSize(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        securitySettingList.setAdapter(mAdapter);
        if (presenter != null) {
            int identity = presenter.getIdentity();
            if (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE || identity == ConstantField.UserIdentity.MEMBER_IDENTITY) {
                if (identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE) {
                    granteeSecurityRequestCallback = new GranteeSecurityRequestDialog.GranteeSecurityRequestCallback() {
                        @Override
                        public void cancelRequest() {
                            handleResetApplyId();
                        }

                        @Override
                        public void handleRequestCode(int requestCode) {
                            // Do nothing
                        }
                    };
                    granteeSecurityRequestDialog = new GranteeSecurityRequestDialog(this, granteeSecurityRequestCallback, false);
                }
            }
        }
    }

    private void refreshSecuritySettingList(boolean isRefresh) {
        int identity = ConstantField.UserIdentity.NO_IDENTITY;
        if (presenter != null) {
            identity = presenter.getIdentity();
        }
        boolean isAdministrator = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE);
        boolean isGranter = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.MEMBER_IDENTITY);
        if (mSecuritySettingBeanList == null) {
            mSecuritySettingBeanList = new ArrayList<>();
        } else {
            mSecuritySettingBeanList.clear();
        }
        SecuritySettingBean spaceAccountBean = new SecuritySettingBean(SecuritySettingBean.FUNCTION_SPACE_ACCOUNT);
        spaceAccountBean.setClick(true);
        mSecuritySettingBeanList.add(spaceAccountBean);
        if (isAdministrator) {
            SecuritySettingBean securityPasswordBean = new SecuritySettingBean(SecuritySettingBean.FUNCTION_SECURITY_PASSWORD);
            securityPasswordBean.setClick(true);
            mSecuritySettingBeanList.add(securityPasswordBean);
        }
        if (isGranter) {
            SecuritySettingBean applicationLockBean = new SecuritySettingBean(SecuritySettingBean.FUNCTION_APPLICATION_LOCK);
            applicationLockBean.setClick(true);
            mSecuritySettingBeanList.add(applicationLockBean);
        }
        if (mAdapter != null) {
            mAdapter.updateData(mSecuritySettingBeanList);
        }
    }

    private void pollSecurityMessage() {
        boolean isHandle = false;
        if (presenter != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.getBaseInfo();
            if (eulixBoxBaseInfo != null) {
                String boxUuid = eulixBoxBaseInfo.getBoxUuid();
                String boxBind = eulixBoxBaseInfo.getBoxBind();
                if (boxUuid != null && boxBind != null && granteeApplyId != null) {
                    isHandle = true;
                    EventBusUtil.post(new SecurityMessagePollRequestEvent(boxUuid, boxBind, granteeApplyId));
                }
            }
        }
        if (!isHandle) {
            handleResetApplyId();
            if (granteeSecurityRequestDialog != null) {
                granteeSecurityRequestDialog.dismissGranteeRequestDialog();
            }
        }
    }

    private void handleResetApplyId() {
        if (mHandler != null) {
            while (mHandler.hasMessages(GRANTEE_APPLY)) {
                mHandler.removeMessages(GRANTEE_APPLY);
            }
        }
        granteeApplyId = null;
    }

    private void startModifySecurityPasswordActivity(String granterDataUuid) {
        Intent modifySecurityPasswordIntent = new Intent(EulixSecuritySettingsActivity.this, ModifySecurityPasswordActivity.class);
        if (granterDataUuid != null) {
            modifySecurityPasswordIntent.putExtra(ConstantField.GRANTER_DATA_UUID, granterDataUuid);
        }
        startActivity(modifySecurityPasswordIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshSecuritySettingList(true);
    }

    @Override
    protected void onDestroy() {
        granteeApplyId = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public EulixSecuritySettingsPresenter createPresenter() {
        return new EulixSecuritySettingsPresenter();
    }

    @Override
    public void granteeApplyResult(String source, int code) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400) {
                    while (mHandler.hasMessages(GRANTEE_APPLY)) {
                        mHandler.removeMessages(GRANTEE_APPLY);
                    }
                    mHandler.sendEmptyMessageDelayed(GRANTEE_APPLY, (10 * ConstantField.TimeUnit.MINUTE_UNIT));
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.showGranteeRequestDialog();
                    }
                    pollSecurityMessage();
                } else if (code == ConstantField.KnownError.AccountCommonError.ACCOUNT_410 && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.REQUEST_TOO_MANY);
                    }
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.apply_authorization_fail);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (position >= 0 && mSecuritySettingBeanList != null && mSecuritySettingBeanList.size() > position) {
            SecuritySettingBean bean = mSecuritySettingBeanList.get(position);
            if (bean != null) {
                switch (bean.getSecuritySettingFunction()) {
                    case SecuritySettingBean.FUNCTION_SPACE_ACCOUNT:
                        Intent spaceAccountIntent = new Intent(EulixSecuritySettingsActivity.this, SpaceAccountActivity.class);
                        startActivity(spaceAccountIntent);
                        break;
                    case SecuritySettingBean.FUNCTION_SECURITY_PASSWORD:
                        if (presenter != null) {
                            int identity = presenter.getIdentity();
                            switch (identity) {
                                case ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY:
                                    startModifySecurityPasswordActivity(null);
                                    break;
                                case ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE:
                                    showLoading("");
                                    granteeApplyId = UUID.randomUUID().toString();
                                    presenter.granteeApplyModifySecurityPassword(granteeApplyId);
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    case SecuritySettingBean.FUNCTION_APPLICATION_LOCK:
                        if (presenter != null) {
                            if (presenter.hasBiometricFeature()) {
                                Intent intent = new Intent(EulixSecuritySettingsActivity.this, ApplicationLockSettingActivity.class);
                                startActivity(intent);
                            } else {
                                showImageTextToast(R.drawable.toast_refuse, R.string.application_lock_unsupported_hint);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SecurityMessagePollResponseEvent event) {
        if (event != null) {
            int code = event.getCode();
            String requestId = event.getRequestUuid();
            SecurityMessagePollResult result = event.getResult();
            if (granteeApplyId != null && granteeApplyId.equals(requestId)) {
                if (code >= 200 && code < 400 && result != null) {
                    handleResetApplyId();
                    boolean isAccept = result.isAccept();
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.handleGranteeRequestResult(isAccept
                                ? GranteeSecurityRequestDialog.REQUEST_ACCEPT : GranteeSecurityRequestDialog.REQUEST_DENY);
                    }
                    if (isAccept) {
                        String granterDataUuid = null;
                        SecurityTokenResult securityTokenResult = result.getSecurityTokenResult();
                        if (securityTokenResult != null) {
                            granterDataUuid = DataUtil.setData(new Gson().toJson(securityTokenResult, SecurityTokenResult.class));
                        }
                        startModifySecurityPasswordActivity(granterDataUuid);
                    }
                } else {
                    switch (code) {
                        case -3:
                            handleResetApplyId();
                            if (granteeSecurityRequestDialog != null) {
                                granteeSecurityRequestDialog.dismissGranteeRequestDialog();
                            }
                            break;
                        default:
                            pollSecurityMessage();
                            break;
                    }
                }
            }
        }
    }
}
