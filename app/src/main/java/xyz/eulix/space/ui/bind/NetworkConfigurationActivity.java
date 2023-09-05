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
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.NetworkConfigDNSInfo;
import xyz.eulix.space.bridge.NetworkConfigurationBridge;
import xyz.eulix.space.bridge.NetworkConfigurationInnerBridge;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.presenter.NetworkConfigurationPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/2 9:05
 */
public class NetworkConfigurationActivity extends AbsActivity<NetworkConfigurationPresenter.INetworkConfiguration, NetworkConfigurationPresenter> implements NetworkConfigurationPresenter.INetworkConfiguration
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, NetworkConfigurationBridge.NetworkConfigurationSinkCallback, NetworkConfigurationInnerBridge.NetworkConfigurationInnerSourceCallback {
    private String activityId;
    private ImageButton back;
    private TextView title;
    private TextView macAddressText;
    private LinearLayout networkDetailContainer;
    private TextView ipv4AddressText;
    private TextView ipv6AddressText;
    private TextView dns1AddressText;
    private TextView dns2AddressText;
    private Button accessNetwork;
    private LinearLayout ipv4ConfigContainer;
    private TextView ipv4ConfigDhcpText;
    private LinearLayout ipv6ConfigContainer;
    private TextView ipv6ConfigTitle;
    private Button ignoreNetwork;
    private Dialog distributeNetworkDialog;
    private View distributeNetworkDialogView;
    private TextView dialogTitle;
    private EditText dialogInput;
    private ImageButton dialogPrivate;
    private Button dialogCancel;
    private Button dialogConfirm;
    private InputMethodManager inputMethodManager;
    private String mSsid;
    private String mAddress;
    private boolean mPrivate;
    private boolean isDistributeNetworkOnly;
    private NetworkConfigurationHandler mHandler;
    private NetworkConfigDNSInfo networkConfigDNSInfo;
    private NetworkAdapter networkAdapter;
    private NetworkConfigDNSInfo temperateNetworkConfigDNSInfo;
    private NetworkAdapter temperateNetworkAdapter;
    private NetworkConfigurationBridge mBridge;
    private AODeviceDiscoveryManager mManager;
    private NetworkConfigurationInnerBridge networkConfigurationInnerBridge;

    private TextWatcher inputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s != null) {
                setDialogButtonPattern(dialogConfirm, !s.toString().isEmpty());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Do nothing
        }
    };

    private static class NetworkConfigurationHandler extends Handler {
        private WeakReference<NetworkConfigurationActivity> networkConfigurationActivityWeakReference;

        public NetworkConfigurationHandler(NetworkConfigurationActivity activity) {
            networkConfigurationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            NetworkConfigurationActivity activity = networkConfigurationActivityWeakReference.get();
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
        setContentView(R.layout.activity_network_configuration);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        macAddressText = findViewById(R.id.mac_address);
        networkDetailContainer = findViewById(R.id.network_detail_container);
        ipv4AddressText = findViewById(R.id.ipv4_address);
        ipv6AddressText = findViewById(R.id.ipv6_address);
        dns1AddressText = findViewById(R.id.dns1_address);
        dns2AddressText = findViewById(R.id.dns2_address);
        accessNetwork = findViewById(R.id.access_network);
        ipv4ConfigContainer = findViewById(R.id.ipv4_config_container);
        ipv4ConfigDhcpText = findViewById(R.id.ipv4_config_dhcp);
        ipv6ConfigContainer = findViewById(R.id.ipv6_config_container);
        ipv6ConfigTitle = findViewById(R.id.ipv6_config_title);
        ignoreNetwork = findViewById(R.id.ignore_network);

        distributeNetworkDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_edit_private_dialog, null);
        dialogTitle = distributeNetworkDialogView.findViewById(R.id.dialog_title);
        dialogInput = distributeNetworkDialogView.findViewById(R.id.dialog_input);
        dialogPrivate = distributeNetworkDialogView.findViewById(R.id.dialog_private);
        dialogCancel = distributeNetworkDialogView.findViewById(R.id.dialog_cancel);
        dialogConfirm = distributeNetworkDialogView.findViewById(R.id.dialog_confirm);
        distributeNetworkDialog = new Dialog(this, R.style.EulixDialog);
        distributeNetworkDialog.setCancelable(false);
        distributeNetworkDialog.setContentView(distributeNetworkDialogView);
    }

    @Override
    public void initData() {
        mHandler = new NetworkConfigurationHandler(this);
        Intent intent = getIntent();
        if (intent != null) {
            isDistributeNetworkOnly = intent.getBooleanExtra(ConstantField.DISTRIBUTE_NETWORK, false);
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
        mSsid = null;
        mAddress = null;
        if (networkAdapter != null) {
            mSsid = networkAdapter.getwIFIName();
            mAddress = networkAdapter.getwIFIAddress();
        }
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!isDistributeNetworkOnly) {
            activityId = UUID.randomUUID().toString();
            mManager = AODeviceDiscoveryManager.getInstance();
            mManager.registerCallback(activityId, this);
        }
        mBridge = NetworkConfigurationBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        updateNetworkConfigurationView();
        dialogInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        dialogInput.setHint(R.string.please_input_password);
        dialogConfirm.setText(R.string.join);
        dialogInput.setOnEditorActionListener((v, actionId, event) -> {
            if (!dialogInput.getText().toString().isEmpty() && actionId == EditorInfo.IME_ACTION_DONE) {
                handleDialogConfirm();
            }
            return false;
        });
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        ipv4ConfigContainer.setOnClickListener(this);
        accessNetwork.setOnClickListener(this);
        ignoreNetwork.setOnClickListener(this);

        dialogPrivate.setOnClickListener(v -> setDialogPrivate(!mPrivate));
        dialogCancel.setOnClickListener(v -> dismissDistributeNetworkDialog());
        dialogConfirm.setOnClickListener(v -> handleDialogConfirm());
    }

    private void updateNetworkConfigurationView() {
        String networkName = null;
        if (networkAdapter != null) {
            String ipv4 = networkAdapter.getIpv4();
            String ipv6 = networkAdapter.getIpv6();
            String dns1 = null;
            String dns2 = null;
            if (networkConfigDNSInfo != null) {
                String ipv4Dns1 = networkConfigDNSInfo.getIpv4Dns1();
                String ipv4Dns2 = networkConfigDNSInfo.getIpv4Dns2();
                String ipv6Dns1 = networkConfigDNSInfo.getIpv6Dns1();
                String ipv6Dns2 = networkConfigDNSInfo.getIpv6Dns2();
                if (ipv4 == null && ipv6 != null) {
                    dns1 = ipv6Dns1;
                    dns2 = ipv6Dns2;
                } else {
                    dns1 = ipv4Dns1;
                    dns2 = ipv4Dns2;
                }
            }
            boolean isWired = networkAdapter.isWired();
            boolean isConnect = networkAdapter.isConnected();
            updateNetworkConfigurationView(isWired, isConnect);
            updateMacAddressContainer(isWired ? networkAdapter.getmACAddress() : networkAdapter.getwIFIAddress());
            updateDetailContainer(ipv4, ipv6, dns1, dns2);
            updateDhcpContainer(networkAdapter.isIpv4UseDhcp(), networkAdapter.isIpv6UseDhcp());
            networkName = (isWired ? networkAdapter.getAdapterName() : networkAdapter.getwIFIName());
        }
        title.setText(StringUtil.nullToEmpty(networkName));
    }

    private void updateNetworkConfigurationView(boolean isWired, boolean isConnect) {
        if (networkDetailContainer != null) {
            networkDetailContainer.setVisibility(isConnect ? View.VISIBLE : View.GONE);
        }
        if (accessNetwork != null) {
            accessNetwork.setVisibility((!isWired && !isConnect) ? View.VISIBLE : View.GONE);
        }
        if (ignoreNetwork != null) {
            ignoreNetwork.setVisibility((!isWired && isConnect) ? View.VISIBLE : View.GONE);
        }
    }

    private void updateMacAddressContainer(String macAddress) {
        if (macAddressText != null) {
            macAddressText.setText(StringUtil.isNonBlankString(macAddress) ? macAddress : ConstantField.DEFAULT_MAC_ADDRESS);
        }
    }

    private void updateDetailContainer(String ipv4Address, String ipv6Address, String dns1Address, String dns2Address) {
        if (ipv4AddressText != null) {
            ipv4AddressText.setText(StringUtil.nullToEmpty(ipv4Address));
        }
        if (ipv6AddressText != null) {
            ipv6AddressText.setText(StringUtil.nullToEmpty(ipv6Address));
        }
        if (dns1AddressText != null) {
            dns1AddressText.setText(StringUtil.nullToEmpty(dns1Address));
        }
        if (dns2AddressText != null) {
            dns2AddressText.setText(StringUtil.nullToEmpty(dns2Address));
        }
    }

    private void updateDhcpContainer(boolean isIpv4Dhcp, boolean isIpv6Dhcp) {
        if (ipv4ConfigDhcpText != null) {
            ipv4ConfigDhcpText.setText(getString(isIpv4Dhcp ? R.string.automatic : R.string.manual));
        }
        if (ipv6ConfigTitle != null) {
//            ipv6ConfigTitle.setText(getString(isIpv6Dhcp ? R.string.automatic : R.string.manual));
            // ipv6显示为自动
            ipv6ConfigTitle.setText(getString(R.string.automatic));
        }
    }

    private void showDistributeNetworkDialog() {
        if (distributeNetworkDialog != null && !distributeNetworkDialog.isShowing()) {
            dialogInput.setEnabled(true);
            String address = null;
            if (networkAdapter != null) {
                address = networkAdapter.getwIFIAddress();
            }
            String passwordContent = StringUtil.nullToEmpty(DataUtil.getNetworkPassword(getApplicationContext(), address));
            dialogInput.setText(passwordContent);
            setDialogPrivate(true);
            setDialogButtonPattern(dialogCancel, true);
            setDialogButtonPattern(dialogConfirm, !passwordContent.isEmpty());
            distributeNetworkDialog.show();
            Window window = distributeNetworkDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), getResources().getDimensionPixelSize(R.dimen.dp_133));
            }
        }
    }

    private void dismissDistributeNetworkDialog() {
        if (distributeNetworkDialog != null && distributeNetworkDialog.isShowing()) {
            dialogInput.removeTextChangedListener(inputWatcher);
            distributeNetworkDialog.dismiss();
        }
    }

    private void setDialogPrivate(boolean nPrivate) {
        mPrivate = nPrivate;
        dialogPrivate.setImageResource(mPrivate ? R.drawable.icon_private_2x : R.drawable.icon_public_2x);
        dialogInput.removeTextChangedListener(inputWatcher);
        dialogInput.setInputType(mPrivate ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        dialogInput.setSelection(dialogInput.getText().length());
        dialogInput.addTextChangedListener(inputWatcher);
    }

    private void setDialogButtonPattern(Button button, boolean isWork) {
        if (button != null) {
            button.setClickable(isWork);
            button.setTextColor(getResources().getColor(isWork ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
        }
    }

    private void handleDialogConfirm() {
        forceHideSoftInput(distributeNetworkDialogView.getWindowToken());
        String password = dialogInput.getText().toString();
        DataUtil.setNetworkPassword(getApplicationContext(), mAddress, password);
        if (mBridge != null) {
            dialogInput.setEnabled(false);
            setDialogButtonPattern(dialogCancel, false);
            setDialogButtonPattern(dialogConfirm, false);
            mBridge.networkConfigurationSetWifi(mSsid, mAddress, password, temperateNetworkConfigDNSInfo, temperateNetworkAdapter);
            showLoading("");
        }
    }

    /**
     * 收起指定窗口软键盘
     * @param windowToken
     */
    private void forceHideSoftInput(IBinder windowToken) {
        if (inputMethodManager != null && windowToken != null) {
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    private void prepareFinish() {
        if (mBridge != null) {
            mBridge.handleDestroy();
        }
    }

    @Override
    protected void onDestroy() {
        if (networkConfigurationInnerBridge != null) {
            networkConfigurationInnerBridge.unregisterSourceCallback();
            networkConfigurationInnerBridge = null;
        }
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
    public NetworkConfigurationPresenter createPresenter() {
        return new NetworkConfigurationPresenter();
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
                case R.id.ipv4_config_container:
                    String dataUuid = null;
                    boolean isConnect = false;
                    if (networkAdapter != null) {
                        isConnect = networkAdapter.isConnected();
                        dataUuid = DataUtil.setData(new Gson().toJson((temperateNetworkAdapter == null
                                ? networkAdapter : temperateNetworkAdapter), NetworkAdapter.class));
                    }
                    networkConfigurationInnerBridge = NetworkConfigurationInnerBridge.getInstance();
                    networkConfigurationInnerBridge.registerSourceCallback(this);
                    Intent intent = new Intent(NetworkConfigurationActivity.this, IPConfigurationActivity.class);
                    intent.putExtra(ConstantField.IS_IPV6, false);
                    if (networkConfigDNSInfo != null) {
                        intent.putExtra(ConstantField.NETWORK_CONFIG_DNS, new Gson().toJson((temperateNetworkConfigDNSInfo == null
                                ? networkConfigDNSInfo : temperateNetworkConfigDNSInfo), NetworkConfigDNSInfo.class));
                    }
                    if (dataUuid != null) {
                        intent.putExtra(ConstantField.DATA_UUID, dataUuid);
                    }
                    intent.putExtra(ConstantField.IS_CONNECT, isConnect);
                    intent.putExtra(ConstantField.DISTRIBUTE_NETWORK, isDistributeNetworkOnly);
                    startActivity(intent);
                    break;
                case R.id.access_network:
                    String title = getString(R.string.input_password_title_part_1) + StringUtil.nullToEmpty(mSsid) + getString(R.string.input_password_title_part_2);
                    dialogTitle.setText(title);
                    showDistributeNetworkDialog();
                    break;
                case R.id.ignore_network:
                    if (mBridge != null) {
                        showLoading("");
                        mBridge.networkConfigurationIgnoreWifi(mSsid, mAddress);
                    }
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

    }

    @Override
    public NetworkAdapter getCurrentNetworkAdapter() {
        return networkAdapter;
    }

    @Override
    public void handleRefreshAccessNetwork(NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                this.networkConfigDNSInfo = networkConfigDNSInfo;
                this.networkAdapter = networkAdapter;
                updateNetworkConfigurationView();
            });
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (networkConfigurationInnerBridge != null) {
                    networkConfigurationInnerBridge.handleDisconnect();
                }
                prepareFinish();
                finish();
            });
        }
    }

    @Override
    public void handleNetworkConfigurationSetWifi(int code, String source) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (networkConfigurationInnerBridge != null) {
                    if (code >= 200 && code < 400) {
                        showLoading("");
                    }
                    networkConfigurationInnerBridge.handleNetworkConfigurationSetWifi(code, source);
                } else {
                    closeLoading();
                    if (code >= 100 && code < 400) {
                        dismissDistributeNetworkDialog();
                        if (code < 200) {
                            showPureTextToast(R.string.modify_pending);
                        } else {
                            showImageTextToast(R.drawable.toast_right, R.string.connect_success);
                        }
                        prepareFinish();
                        finish();
                    } else {
                        if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                            showServerExceptionToast();
                        } else if (code >= 600 && code < 700) {
                            showPureTextToast(R.string.connection_time_out);
                        } else {
                            showImageTextToast(R.drawable.toast_wrong, (code == 561 ? R.string.connect_fail_wrong_password : R.string.connect_fail));
                        }
                        dialogInput.setEnabled(true);
                        setDialogButtonPattern(dialogCancel, true);
                        setDialogButtonPattern(dialogConfirm, true);
                    }
                }
            });
        }
    }

    @Override
    public void handleNetworkConfigurationIgnoreWifi(int code, String source) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400) {
                    showImageTextToast(R.drawable.toast_right, R.string.ignore_success);
                    prepareFinish();
                    finish();
                } else if (code >= 600 && code < 700) {
                    showPureTextToast(R.string.ignore_expire);
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.ignore_fail);
                }
            });
        }
    }

    @Override
    public void handleDestroy() {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (networkConfigurationInnerBridge != null) {
                    networkConfigurationInnerBridge.unregisterSourceCallback();
                    networkConfigurationInnerBridge = null;
                }
            });
        }
    }

    @Override
    public void requestIpConfiguration(boolean isConnect, NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (!isConnect) {
                    temperateNetworkConfigDNSInfo = networkConfigDNSInfo;
                    temperateNetworkAdapter = networkAdapter;
                    if (networkAdapter != null) {
                        updateDhcpContainer(networkAdapter.isIpv4UseDhcp(), networkAdapter.isIpv6UseDhcp());
                    }
                } else if (mBridge != null) {
                    mBridge.networkConfigurationSetWifi(null, null, null, networkConfigDNSInfo, networkAdapter);
                }
            });
        }
    }
}
