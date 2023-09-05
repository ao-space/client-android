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

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.NetworkConfigDNSInfo;
import xyz.eulix.space.bridge.NetworkConfigurationInnerBridge;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.presenter.IPConfigurationPresenter;
import xyz.eulix.space.util.BooleanUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/2 16:17
 */
public class IPConfigurationActivity extends AbsActivity<IPConfigurationPresenter.IIPConfiguration, IPConfigurationPresenter> implements IPConfigurationPresenter.IIPConfiguration
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, NetworkConfigurationInnerBridge.NetworkConfigurationInnerSinkCallback {
    private String activityId;
    private ImageButton back;
    private TextView title;
    private Button functionText;
    private TextView ipTitle;
    private LinearLayout ipDhcpAutomaticContainer;
    private ImageView ipDhcpAutomaticIndicator;
    private LinearLayout ipDhcpManualContainer;
    private ImageView ipDhcpManualIndicator;
    private LinearLayout ipConfigContainer;
    private EditText ipAddressInput;
    private EditText subnetMaskInput;
    private EditText defaultGatewayInput;
    private LinearLayout dnsConfigContainer;
    private EditText dns1Input;
    private EditText dns2Input;
    private boolean isConnect;
    private boolean isIpv6;
    private NetworkConfigDNSInfo networkConfigDNSInfo;
    private boolean isDistributeNetworkOnly;
    private NetworkAdapter networkAdapter;
    @NonNull
    private IPConfigurationBean oldIpConfigurationBean = new IPConfigurationBean();
    @NonNull
    private IPConfigurationBean newIpConfigurationBean = new IPConfigurationBean();
    private NetworkConfigurationInnerBridge mBridge;
    private AODeviceDiscoveryManager mManager;
    private IPConfigurationHandler mHandler;

    private static class IPConfigurationHandler extends Handler {
        private WeakReference<IPConfigurationActivity> ipConfigurationActivityWeakReference;

        public IPConfigurationHandler(IPConfigurationActivity activity) {
            ipConfigurationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            IPConfigurationActivity activity = ipConfigurationActivityWeakReference.get();
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
        setContentView(R.layout.activity_ip_configuration);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        ipTitle = findViewById(R.id.ip_title);
        ipDhcpAutomaticContainer = findViewById(R.id.ip_dhcp_automatic_container);
        ipDhcpAutomaticIndicator = findViewById(R.id.ip_dhcp_automatic_indicator);
        ipDhcpManualContainer = findViewById(R.id.ip_dhcp_manual_container);
        ipDhcpManualIndicator = findViewById(R.id.ip_dhcp_manual_indicator);
        ipConfigContainer = findViewById(R.id.ip_config_container);
        ipAddressInput = findViewById(R.id.ip_address_input);
        subnetMaskInput = findViewById(R.id.subnet_mask_input);
        defaultGatewayInput = findViewById(R.id.default_gateway_input);
        dnsConfigContainer = findViewById(R.id.dns_config_container);
        dns1Input = findViewById(R.id.dns1_input);
        dns2Input = findViewById(R.id.dns2_input);
    }

    @Override
    public void initData() {
        mHandler = new IPConfigurationHandler(this);
        isConnect = true;
        Intent intent = getIntent();
        if (intent != null) {
            isDistributeNetworkOnly = intent.getBooleanExtra(ConstantField.DISTRIBUTE_NETWORK, false);
            isConnect = intent.getBooleanExtra(ConstantField.IS_CONNECT, true);
            isIpv6 = intent.getBooleanExtra(ConstantField.IS_IPV6, false);
            if (intent.hasExtra(ConstantField.NETWORK_CONFIG_DNS)) {
                String networkConfigDnsValue = intent.getStringExtra(ConstantField.NETWORK_CONFIG_DNS);
                if (networkConfigDnsValue != null) {
                    try {
                        networkConfigDNSInfo = new Gson().fromJson(networkConfigDnsValue, NetworkConfigDNSInfo.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (intent.hasExtra(ConstantField.DATA_UUID)) {
                String dataUuid = intent.getStringExtra(ConstantField.DATA_UUID);
                if (dataUuid != null) {
                    String data = DataUtil.getData(dataUuid);
                    if (data != null) {
                        try {
                            networkAdapter = new Gson().fromJson(data, NetworkAdapter.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        if (networkConfigDNSInfo != null) {
            String dns1 = (isIpv6 ? networkConfigDNSInfo.getIpv6Dns1() : networkConfigDNSInfo.getIpv4Dns1());
            String dns2 = (isIpv6 ? networkConfigDNSInfo.getIpv6Dns2() : networkConfigDNSInfo.getIpv4Dns2());
            oldIpConfigurationBean.setDns1Address(StringUtil.nullToEmpty(dns1));
            oldIpConfigurationBean.setDns2Address(StringUtil.nullToEmpty(dns2));
            newIpConfigurationBean.setDns1Address(StringUtil.nullToEmpty(dns1));
            newIpConfigurationBean.setDns2Address(StringUtil.nullToEmpty(dns2));
        }
        if (networkAdapter != null) {
            boolean isDhcp = (isIpv6 ? networkAdapter.isIpv6UseDhcp() : networkAdapter.isIpv4UseDhcp());
            String ipAddress = (isIpv6 ? networkAdapter.getIpv6() : networkAdapter.getIpv4());
            String subnetMask = networkAdapter.getSubNetMask();
            String defaultGateway = (isIpv6 ? networkAdapter.getIpv6DefaultGateway() : networkAdapter.getDefaultGateway());
            oldIpConfigurationBean.setDhcp(isDhcp);
            oldIpConfigurationBean.setIpAddress(ipAddress);
            oldIpConfigurationBean.setSubnetMask(subnetMask);
            oldIpConfigurationBean.setDefaultGateway(defaultGateway);
            newIpConfigurationBean.setDhcp(isDhcp);
            newIpConfigurationBean.setIpAddress(ipAddress);
            newIpConfigurationBean.setSubnetMask(subnetMask);
            newIpConfigurationBean.setDefaultGateway(defaultGateway);
        }
        if (!isDistributeNetworkOnly) {
            activityId = UUID.randomUUID().toString();
            mManager = AODeviceDiscoveryManager.getInstance();
            mManager.registerCallback(activityId, this);
        }
        mBridge = NetworkConfigurationInnerBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        functionText.setVisibility(View.VISIBLE);
        functionText.setText(R.string.done);
        ipTitle.setText(isIpv6 ? R.string.ipv6 : R.string.ipv4);
        Boolean isDhcp = oldIpConfigurationBean.getDhcp();
        if (isDhcp != null) {
            updateDhcpContainer(isDhcp);
        }
        setInputEnabled(true);
        String networkName = null;
        if (networkAdapter != null) {
            networkName = (networkAdapter.isWired() ? networkAdapter.getAdapterName() : networkAdapter.getwIFIName());
        }
        title.setText(StringUtil.nullToEmpty(networkName));
        updateIpInformationContainer(oldIpConfigurationBean.getIpAddress(), oldIpConfigurationBean.getSubnetMask(), oldIpConfigurationBean.getDefaultGateway());
        updateDnsContainer(oldIpConfigurationBean.getDns1Address(), oldIpConfigurationBean.getDns2Address());
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        functionText.setOnClickListener(this);
        ipDhcpAutomaticContainer.setOnClickListener(this);
        ipDhcpManualContainer.setOnClickListener(this);
        ipAddressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    newIpConfigurationBean.setIpAddress(s.toString());
                    updateDoneButton(checkLegal());
                }
            }
        });
        subnetMaskInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    newIpConfigurationBean.setSubnetMask(s.toString());
                    updateDoneButton(checkLegal());
                }
            }
        });
        defaultGatewayInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    newIpConfigurationBean.setDefaultGateway(s.toString());
                    updateDoneButton(checkLegal());
                }
            }
        });
        dns1Input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    newIpConfigurationBean.setDns1Address(s.toString());
                    updateDoneButton(checkLegal());
                }
            }
        });
        dns2Input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null) {
                    newIpConfigurationBean.setDns2Address(s.toString());
                    updateDoneButton(checkLegal());
                }
            }
        });
        Boolean isDhcp = oldIpConfigurationBean.getDhcp();
        if (isDhcp != null) {
            setInputEnabled(!(isDhcp));
        }
        updateDoneButton(checkLegal());
    }

    private void updateDoneButton(boolean isClickable) {
        if (functionText != null) {
            functionText.setTextColor(getResources().getColor(isClickable ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
            functionText.setClickable(isClickable);
        }
    }

    private void updateDhcpContainer(boolean isDhcp) {
        if (ipDhcpAutomaticIndicator != null) {
            if (isDhcp) {
                ipDhcpAutomaticIndicator.setImageResource(R.drawable.icon_radio_button_select_2x);
            } else {
                ipDhcpAutomaticIndicator.setImageDrawable(null);
            }
        }
        if (ipDhcpManualIndicator != null) {
            if (isDhcp) {
                ipDhcpManualIndicator.setImageDrawable(null);
            } else {
                ipDhcpManualIndicator.setImageResource(R.drawable.icon_radio_button_select_2x);
            }
        }
        if (ipConfigContainer != null) {
            ipConfigContainer.setVisibility(isDhcp ? View.GONE : View.VISIBLE);
        }
        if (dnsConfigContainer != null) {
            dnsConfigContainer.setVisibility(isDhcp ? View.GONE : View.VISIBLE);
        }
    }

    private void updateIpInformationContainer(String ipAddress, String subnetMask, String defaultGateway) {
        if (ipAddressInput != null) {
            ipAddressInput.setText(StringUtil.nullToEmpty(ipAddress));
        }
        if (subnetMaskInput != null) {
            subnetMaskInput.setText(StringUtil.nullToEmpty(subnetMask));
        }
        if (defaultGatewayInput != null) {
            defaultGatewayInput.setText(StringUtil.nullToEmpty(defaultGateway));
        }
    }

    private void updateDnsContainer(String dns1Address, String dns2Address) {
        if (dns1Input != null) {
            dns1Input.setText(StringUtil.nullToEmpty(dns1Address));
        }
        if (dns2Input != null) {
            dns2Input.setText(StringUtil.nullToEmpty(dns2Address));
        }
    }

    private void setInputEnabled(boolean isEnable) {
        if (ipAddressInput != null) {
            ipAddressInput.setEnabled(isEnable);
        }
        if (subnetMaskInput != null) {
            subnetMaskInput.setEnabled(isEnable);
        }
        if (defaultGatewayInput != null) {
            defaultGatewayInput.setEnabled(isEnable);
        }
        if (dns1Input != null) {
            dns1Input.setEnabled(isEnable);
        }
        if (dns2Input != null) {
            dns2Input.setEnabled(isEnable);
        }
    }

    private boolean checkLegal() {
        boolean isLegal = false;
        if (newIpConfigurationBean != null) {
            Boolean isDhcp = newIpConfigurationBean.getDhcp();
            if (isDhcp != null) {
                isLegal = (isDhcp || (StringUtil.isNonBlankString(newIpConfigurationBean.ipAddress)
                        && StringUtil.isNonBlankString(newIpConfigurationBean.subnetMask)
                        && StringUtil.isNonBlankString(newIpConfigurationBean.defaultGateway)
                        && StringUtil.isNonBlankString(newIpConfigurationBean.dns1Address)
                        && StringUtil.isNonBlankString(newIpConfigurationBean.dns2Address)));
            }
        }
        return isLegal;
    }

    private void prepareFinish() {
        if (mBridge != null) {
            mBridge.handleDestroy();
        }
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
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
    public void onBackPressed() {
        prepareFinish();
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            prepareFinish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public IPConfigurationPresenter createPresenter() {
        return new IPConfigurationPresenter();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    prepareFinish();
                    finish();
                    break;
                case R.id.function_text:
                    if (ipAddressInput != null && ipAddressInput.isEnabled()) {
                        Editable editable = ipAddressInput.getText();
                        if (editable != null) {
                            newIpConfigurationBean.setIpAddress(editable.toString());
                        }
                    }
                    if (subnetMaskInput != null && subnetMaskInput.isEnabled()) {
                        Editable editable = subnetMaskInput.getText();
                        if (editable != null) {
                            newIpConfigurationBean.setSubnetMask(editable.toString());
                        }
                    }
                    if (defaultGatewayInput != null && defaultGatewayInput.isEnabled()) {
                        Editable editable = defaultGatewayInput.getText();
                        if (editable != null) {
                            newIpConfigurationBean.setDefaultGateway(editable.toString());
                        }
                    }
                    if (dns1Input != null && dns1Input.isEnabled()) {
                        Editable editable = dns1Input.getText();
                        if (editable != null) {
                            newIpConfigurationBean.setDns1Address(editable.toString());
                        }
                    }
                    if (dns2Input != null && dns2Input.isEnabled()) {
                        Editable editable = dns2Input.getText();
                        if (editable != null) {
                            newIpConfigurationBean.setDns2Address(editable.toString());
                        }
                    }
                    updateDoneButton(checkLegal());
                    if (checkLegal()) {
                        boolean isSame = IPConfigurationBean.compare(oldIpConfigurationBean, newIpConfigurationBean);
                        if (!isSame) {
                            if (networkAdapter != null) {
                                Boolean isDhcp = newIpConfigurationBean.getDhcp();
                                if (isIpv6) {
                                    if (isDhcp != null) {
                                        networkAdapter.setIpv6UseDhcp(isDhcp);
                                    }
                                    networkAdapter.setIpv6(newIpConfigurationBean.getIpAddress());
                                    networkAdapter.setIpv6DefaultGateway(newIpConfigurationBean.getDefaultGateway());
                                } else {
                                    if (isDhcp != null) {
                                        networkAdapter.setIpv4UseDhcp(isDhcp);
                                    }
                                    networkAdapter.setIpv4(newIpConfigurationBean.getIpAddress());
                                    networkAdapter.setDefaultGateway(newIpConfigurationBean.getDefaultGateway());
                                }
                                networkAdapter.setSubNetMask(newIpConfigurationBean.getSubnetMask());
                            }
                            if (networkConfigDNSInfo != null) {
                                if (isIpv6) {
                                    networkConfigDNSInfo.setIpv6Dns1(newIpConfigurationBean.getDns1Address());
                                    networkConfigDNSInfo.setIpv6Dns2(newIpConfigurationBean.getDns2Address());
                                } else {
                                    networkConfigDNSInfo.setIpv4Dns1(newIpConfigurationBean.getDns1Address());
                                    networkConfigDNSInfo.setIpv4Dns2(newIpConfigurationBean.getDns2Address());
                                }
                            }
                        }
                        if (!isConnect || isSame) {
                            if (!isSame && mBridge != null) {
                                mBridge.requestIpConfiguration(false, networkConfigDNSInfo, networkAdapter);
                            }
                            prepareFinish();
                            finish();
                        } else {
                            oldIpConfigurationBean.reset();
                            if (mBridge != null) {
                                showLoading("");
                                mBridge.requestIpConfiguration(true, networkConfigDNSInfo, networkAdapter);
                            }
                        }
                    } else {
                        // do nothing
                    }
                    break;
                case R.id.ip_dhcp_automatic_container:
                    updateDhcpContainer(true);
                    newIpConfigurationBean.setDhcp(true);
                    setInputEnabled(false);
                    updateDoneButton(checkLegal());
                    break;
                case R.id.ip_dhcp_manual_container:
                    updateDhcpContainer(false);
                    newIpConfigurationBean.setDhcp(false);
                    setInputEnabled(true);
                    updateDoneButton(checkLegal());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFinish() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void onResponse(int code, String source, int step, String bodyJson) {
        // Do nothing
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(() -> {
                prepareFinish();
                finish();
            });
        }
    }

    @Override
    public void handleNetworkConfigurationSetWifi(int code, String source) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400) {
                    showImageTextToast(R.drawable.toast_right, R.string.modify_success);
                    prepareFinish();
                    finish();
                } else if (code >= 100 && code < 200) {
                    showPureTextToast(R.string.modify_pending);
                    prepareFinish();
                    finish();
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.modify_failed_pure);
                }
            });
        }
    }

    static class IPConfigurationBean {
        private Boolean isDhcp;
        private String ipAddress;
        private String subnetMask;
        private String defaultGateway;
        private String dns1Address;
        private String dns2Address;

        static boolean compare(IPConfigurationBean bean1, IPConfigurationBean bean2) {
            boolean isEqual = false;
            if (bean1 == null && bean2 == null) {
                isEqual = true;
            } else if (bean1 != null && bean2 != null) {
                isEqual = (BooleanUtil.compare(bean1.isDhcp, bean2.isDhcp) && StringUtil.compare(bean1.ipAddress, bean2.ipAddress)
                        && StringUtil.compare(bean1.subnetMask, bean2.subnetMask) && StringUtil.compare(bean1.defaultGateway, bean2.defaultGateway)
                        && StringUtil.compare(bean1.dns1Address, bean2.dns1Address) && StringUtil.compare(bean1.dns2Address, bean2.dns2Address));
            }
            return isEqual;
        }

        void reset() {
            isDhcp = null;
            ipAddress = null;
            subnetMask = null;
            defaultGateway = null;
            dns1Address = null;
            dns2Address = null;
        }

        public Boolean getDhcp() {
            return isDhcp;
        }

        public void setDhcp(Boolean dhcp) {
            isDhcp = dhcp;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getSubnetMask() {
            return subnetMask;
        }

        public void setSubnetMask(String subnetMask) {
            this.subnetMask = subnetMask;
        }

        public String getDefaultGateway() {
            return defaultGateway;
        }

        public void setDefaultGateway(String defaultGateway) {
            this.defaultGateway = defaultGateway;
        }

        public String getDns1Address() {
            return dns1Address;
        }

        public void setDns1Address(String dns1Address) {
            this.dns1Address = dns1Address;
        }

        public String getDns2Address() {
            return dns2Address;
        }

        public void setDns2Address(String dns2Address) {
            this.dns2Address = dns2Address;
        }

        @Override
        public String toString() {
            return "IPConfigurationBean{" +
                    "isDhcp=" + isDhcp +
                    ", ipAddress='" + ipAddress + '\'' +
                    ", subnetMask='" + subnetMask + '\'' +
                    ", defaultGateway='" + defaultGateway + '\'' +
                    ", dns1Address='" + dns1Address + '\'' +
                    ", dns2Address='" + dns2Address + '\'' +
                    '}';
        }
    }
}
