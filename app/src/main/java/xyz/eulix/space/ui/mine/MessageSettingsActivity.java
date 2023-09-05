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

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.MessageSettingsPresenter;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.SystemUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/20 17:56
 */
public class MessageSettingsActivity extends AbsActivity<MessageSettingsPresenter.IMessageSetting, MessageSettingsPresenter> implements MessageSettingsPresenter.IMessageSetting, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private ImageButton back;
    private TextView title;
    private CheckBox systemMessageSwitch;
    private CheckBox businessMessageSwitch;

    @Override
    public void initView() {
        setContentView(R.layout.activity_message_settings);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        systemMessageSwitch = findViewById(R.id.system_message_switch);
        businessMessageSwitch = findViewById(R.id.business_message_switch);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initViewData() {
        title.setText(R.string.notification);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
    }

    private void setCheckBoxPattern(CheckBox checkBox, boolean isEnable, boolean isCheck) {
        if (checkBox != null) {
            if (isEnable) {
                checkBox.setEnabled(true);
                checkBox.setChecked(isCheck);
            } else {
                checkBox.setChecked(isCheck);
                checkBox.setEnabled(false);
            }
        }
    }

    private void setCheckBoxStatus(CheckBox checkBox, boolean isCheck) {
        if (checkBox != null && checkBox.isEnabled()) {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(isCheck);
            checkBox.setOnCheckedChangeListener(this);
        }
    }

    private void setCheckedChangeListener(boolean isSet) {
        systemMessageSwitch.setOnCheckedChangeListener(isSet ? this : null);
        businessMessageSwitch.setOnCheckedChangeListener(isSet ? this : null);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean isNotificationEnable = SystemUtil.requestNotification(getApplicationContext(), false);
        setCheckBoxPattern(systemMessageSwitch, isNotificationEnable, (presenter == null || presenter.isSystemMessageEnable()));
        setCheckBoxPattern(businessMessageSwitch, isNotificationEnable, (presenter == null || presenter.isBusinessMessageEnable()));
        setCheckedChangeListener(true);
    }

    @Override
    protected void onStop() {
        setCheckedChangeListener(false);
        super.onStop();
    }

    @NotNull
    @Override
    public MessageSettingsPresenter createPresenter() {
        return new MessageSettingsPresenter();
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView != null) {
            switch (buttonView.getId()) {
                case R.id.system_message_switch:
                    if (presenter != null && !presenter.setSystemMessageEnable(isChecked)) {
                        setCheckBoxStatus(systemMessageSwitch, presenter.isSystemMessageEnable());
                    }
                    break;
                case R.id.business_message_switch:
                    if (presenter != null && !presenter.setBusinessMessageEnable(isChecked)) {
                        setCheckBoxStatus(businessMessageSwitch, presenter.isBusinessMessageEnable());
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
