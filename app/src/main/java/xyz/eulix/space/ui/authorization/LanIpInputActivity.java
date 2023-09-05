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

package xyz.eulix.space.ui.authorization;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.LanIpInputPresenter;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.Utils;
import xyz.eulix.space.view.NormalEditInputView;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 局域网IP、域名输入页
 * History:     2023/3/24
 */
public class LanIpInputActivity extends AbsActivity<LanIpInputPresenter.ILanIpInput, LanIpInputPresenter> implements LanIpInputPresenter.ILanIpInput {
    private NormalEditInputView normalEditInputView;
    private TitleBarWithSelect titleBar;
    private LinearLayout layoutConfirm;
    private LottieAnimationView loadingAnim;

    private final static int STATE_NORMAL = 1;
    private final static int STATE_CLICKABLE = 2;
    private final static int STATE_LOADING = 3;
    private int mConfirmState = STATE_NORMAL;

    @Override
    public void initView() {
        setContentView(R.layout.activity_lan_ip_input);
        normalEditInputView = findViewById(R.id.input_view);
        layoutConfirm = findViewById(R.id.layout_confirm);
        loadingAnim = findViewById(R.id.loading_animation);
        titleBar = findViewById(R.id.title_bar);

        normalEditInputView.setInputChangeListener(new NormalEditInputView.InputChangeListener() {
            @Override
            public void onInputChange(String inputText) {
                if (TextUtils.isEmpty(inputText)) {
                    mConfirmState = STATE_NORMAL;
                } else {
                    mConfirmState = STATE_CLICKABLE;
                }
                refreshConfirmState();
            }
        });
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initViewData() {
        titleBar.setTitle(getResources().getString(R.string.login));
        refreshConfirmState();
    }

    @Override
    public void initEvent() {
        layoutConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConfirmState == STATE_CLICKABLE) {
                    Utils.forceHideSoftInput(LanIpInputActivity.this);
                    mConfirmState = STATE_LOADING;
                    refreshConfirmState();
                    normalEditInputView.setViewEnable(false);
                    //查询可用性
                    presenter.checkDomainAvailable(normalEditInputView.getInputText());
                }
            }
        });
    }

    @Override
    public void onCheckDomainResult(boolean isAvailable) {
        normalEditInputView.setViewEnable(true);
        if (isAvailable) {
            //打开局域网二维码页面
            String boxDomain = normalEditInputView.getInputText();
            String realDomain;
            if (NetUtils.isIpAddress(normalEditInputView.getInputText())) {
                realDomain = "http://" + boxDomain;
            } else {
                realDomain = "https://" + boxDomain;
            }
            Intent intent = new Intent(LanIpInputActivity.this, GranteeLoginActivity.class);
            intent.putExtra(GranteeLoginActivity.KEY_LAN_BOX_DOMAIN, realDomain);
            startActivity(intent);
        } else {
            showImageTextToast(R.drawable.toast_refuse, R.string.lan_access_failed);
        }
        mConfirmState = STATE_CLICKABLE;
        refreshConfirmState();
    }

    private void refreshConfirmState() {
        switch (mConfirmState) {
            case STATE_CLICKABLE:
                LottieUtil.stop(loadingAnim);
                loadingAnim.setVisibility(View.GONE);
                layoutConfirm.setBackground(getResources().getDrawable(R.drawable.background_ff337aff_ff16b9ff_rectangle_10));
                break;
            case STATE_LOADING:
                loadingAnim.setVisibility(View.VISIBLE);
                LottieUtil.loop(loadingAnim, "loading_button.json");
                layoutConfirm.setBackground(getResources().getDrawable(R.drawable.background_b2337aff_b216b9ff_rectangle_10));
                break;
            default:
                LottieUtil.stop(loadingAnim);
                loadingAnim.setVisibility(View.GONE);
                layoutConfirm.setBackground(getResources().getDrawable(R.drawable.background_ffdfe0e5_rectangle_10));
        }
    }

    @NonNull
    @Override
    public LanIpInputPresenter createPresenter() {
        return new LanIpInputPresenter();
    }
}