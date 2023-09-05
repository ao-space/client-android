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

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.EulixDeviceListActivity;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.presenter.EulixSettingsPresenter;
import xyz.eulix.space.ui.mine.developer.DeveloperOptionsActivity;
import xyz.eulix.space.ui.mine.general.EulixGeneralActivity;
import xyz.eulix.space.ui.mine.privacy.EulixPrivacyActivity;
import xyz.eulix.space.ui.mine.security.EulixSecuritySettingsActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/7 15:03
 */
public class EulixSettingsActivity extends AbsActivity<EulixSettingsPresenter.IEulixSettings, EulixSettingsPresenter> implements EulixSettingsPresenter.IEulixSettings, View.OnClickListener {
    private ImageButton back;
    private TextView title;
    private LinearLayout securitySettingContainer;
    private View securitySettingSplit;
    private LinearLayout eulixDeviceContainer;
    private LinearLayout messageNotificationContainer;
    private LinearLayout eulixPrivacyContainer;
    private LinearLayout eulixGeneralContainer;
    private LinearLayout menu2Container;
    private LinearLayout developerOptionsContainer;
    private Button changeAccount;

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_settings);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        securitySettingContainer = findViewById(R.id.security_setting_container);
        securitySettingSplit = findViewById(R.id.security_setting_split);
        eulixDeviceContainer = findViewById(R.id.eulix_device_container);
        messageNotificationContainer = findViewById(R.id.message_notification_container);
        eulixPrivacyContainer = findViewById(R.id.eulix_privacy_container);
        eulixGeneralContainer = findViewById(R.id.eulix_general_container);
        menu2Container = findViewById(R.id.menu_2_container);
        developerOptionsContainer = findViewById(R.id.developer_options_container);
        changeAccount = findViewById(R.id.change_account);
    }

    @Override
    public void initData() {
        // Do nothing
    }

    @Override
    public void initViewData() {
        title.setText(R.string.setting);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        securitySettingContainer.setOnClickListener(this);
        eulixDeviceContainer.setOnClickListener(this);
        messageNotificationContainer.setOnClickListener(this);
        eulixPrivacyContainer.setOnClickListener(this);
        eulixGeneralContainer.setOnClickListener(this);
        developerOptionsContainer.setOnClickListener(this);
        changeAccount.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        int identity = ConstantField.UserIdentity.NO_IDENTITY;
        DeviceAbility deviceAbility = null;
        if (presenter != null) {
            identity = presenter.getIdentity();
            deviceAbility = presenter.getActiveDeviceAbility();
        }
        boolean isAdministrator = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE);
        boolean isGranter = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.MEMBER_IDENTITY);
//        securitySettingContainer.setVisibility((isAdministrator || isGranter) ? View.VISIBLE : View.GONE);
//        securitySettingSplit.setVisibility((isAdministrator || isGranter) ? View.VISIBLE : View.GONE);

        // 空间账号开通
        securitySettingContainer.setVisibility(View.VISIBLE);
        securitySettingSplit.setVisibility(View.VISIBLE);

        boolean isDevOptionSupport = true;
        if (deviceAbility != null) {
            Boolean isDevOptionSupportValue = deviceAbility.getAospaceDevOptionSupport();
            if (isDevOptionSupportValue != null) {
                isDevOptionSupport = isDevOptionSupportValue;
            }
        }
        boolean isAdminBind = identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY;
        boolean isDeveloperOptionsEnable = (isAdminBind && isDevOptionSupport);
        menu2Container.setVisibility(isDeveloperOptionsEnable ? View.VISIBLE : View.GONE);
        developerOptionsContainer.setVisibility(isDeveloperOptionsEnable ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @NotNull
    @Override
    public EulixSettingsPresenter createPresenter() {
        return new EulixSettingsPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.security_setting_container:
                    Intent securitySettingsIntent = new Intent(EulixSettingsActivity.this, EulixSecuritySettingsActivity.class);
                    startActivity(securitySettingsIntent);
                    break;
                case R.id.eulix_device_container:
                    Intent deviceManageIntent = new Intent(EulixSettingsActivity.this, DeviceManageActivity.class);
                    startActivity(deviceManageIntent);
                    break;
                case R.id.message_notification_container:
                    Intent messageSettingIntent = new Intent(EulixSettingsActivity.this, MessageSettingsActivity.class);
                    startActivity(messageSettingIntent);
                    break;
                case R.id.eulix_privacy_container:
                    Intent eulixPrivacyIntent = new Intent(EulixSettingsActivity.this, EulixPrivacyActivity.class);
                    startActivity(eulixPrivacyIntent);
                    break;
                case R.id.eulix_general_container:
                    Intent eulixGeneralIntent = new Intent(EulixSettingsActivity.this, EulixGeneralActivity.class);
                    startActivity(eulixGeneralIntent);
                    break;
                case R.id.developer_options_container:
                    Intent developerOptionsIntent = new Intent(EulixSettingsActivity.this, DeveloperOptionsActivity.class);
                    startActivity(developerOptionsIntent);
                    break;
                case R.id.change_account:
                    EventBusUtil.post(new BoxOnlineRequestEvent(false));
                    Intent eulixDeviceListIntent = new Intent(EulixSettingsActivity.this, EulixDeviceListActivity.class);
                    startActivity(eulixDeviceListIntent);
                    break;
                default:
                    break;
            }
        }
    }
}
