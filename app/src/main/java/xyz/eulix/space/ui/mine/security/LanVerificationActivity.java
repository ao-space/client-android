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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;

import xyz.eulix.space.EulixSpaceLanActivity;
import xyz.eulix.space.R;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bridge.SpacePlatformEnvironmentBridge;
import xyz.eulix.space.event.ForgetPasswordResultEvent;
import xyz.eulix.space.event.SecurityOperationResultEvent;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.network.agent.platform.SwitchStatusResult;
import xyz.eulix.space.ui.mine.developer.SpacePlatformEnvironmentActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.PermissionUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.Utils;
import xyz.eulix.space.util.ViewUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/13 16:22
 */
public class LanVerificationActivity extends EulixSpaceLanActivity implements SpacePlatformEnvironmentBridge.SpacePlatformEnvironmentSourceCallback {
    private ImageButton back;
    private TextView title;
    private LottieAnimationView boxSearching;
    private TextView hardwareSearchHint;
    private TextView hardwareVerificationHint;
    private TextView hardwareVerificationHintSecond;
    private Button startSearch;
    private Boolean isUIBoxSearching = null;
    private boolean isScan = false;
    private String forgetPasswordUuid;
    private boolean isPCHost;
    private SpacePlatformEnvironmentBridge spacePlatformEnvironmentBridge;

    @Override
    public void initView() {
        setContentView(R.layout.activity_hardware_verification);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        boxSearching = findViewById(R.id.box_searching);
        hardwareSearchHint = findViewById(R.id.hardware_search_hint);
        hardwareVerificationHint = findViewById(R.id.hardware_verification_hint);
        hardwareVerificationHintSecond = findViewById(R.id.hardware_verification_hint_second);
        startSearch = findViewById(R.id.start_search);
    }

    @Override
    public void initViewData() {
        switch (mHardwareFunction) {
            case ConstantField.HardwareFunction.SECURITY_VERIFICATION:
            case ConstantField.HardwareFunction.SWITCH_SPACE_PLATFORM:
                title.setText(R.string.hardware_device_verify);
                break;
            default:
                title.setText(R.string.device_connect);
                break;
        }
        hardwareVerificationHint.setText(R.string.lan_verify_hint);
        hardwareVerificationHint.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        startSearch.setOnClickListener(this);
        int deviceModelNumber = 0;
        if (presenter != null) {
            DeviceAbility deviceAbility = presenter.getActiveDeviceAbility();
            if (deviceAbility != null) {
                Integer deviceModelNumberValue = deviceAbility.getDeviceModelNumber();
                if (deviceModelNumberValue != null) {
                    deviceModelNumber = deviceModelNumberValue;
                }
            }
        }
        isPCHost = false;
        switch ((deviceModelNumber / 100)) {
            case -1:
            case -3:
                isPCHost = true;
                break;
            default:
                break;
        }
        if (isPCHost) {
            bindDeviceStatusChange(ConstantField.BindDeviceStatus.PC_HOST_PREPARE);
        } else {
            if (btid != null) {
                bindDeviceStatusChange(ConstantField.BindDeviceStatus.SEARCHING);
                if (mIpBean == null && presenter != null) {
                    presenter.startCountdown(SCAN_COUNTDOWN_SECOND);
                }
            } else {
                bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
            }
            super.initEvent();
        }
    }

    @Override
    protected void handleIntent(Intent intent) {
        super.handleIntent(intent);
        if (intent != null) {
            if (intent.hasExtra(ConstantField.FORGET_PASSWORD_UUID)) {
                forgetPasswordUuid = intent.getStringExtra(ConstantField.FORGET_PASSWORD_UUID);
            }
        }
    }

    private void setBoxSearching(boolean isWork) {
        boolean isUpdate = true;
        if (isUIBoxSearching != null && isWork == isUIBoxSearching) {
            isUpdate = false;
        }
        if (isUpdate) {
            if (isWork) {
                isUIBoxSearching = true;
                LottieUtil.loop(boxSearching, "search_box.json");
            } else {
                isUIBoxSearching = false;
                LottieUtil.stop(boxSearching, "search_box.json");
            }
        }
    }

    @Override
    protected void setBoxSearingIndicator(int index) {
        super.setBoxSearingIndicator(index);
        if (index < 0) {
            hardwareSearchHint.setVisibility(View.INVISIBLE);
        } else {
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(getString(isScan ? R.string.lan_searching_indicator : R.string.binding_connecting_device));
//            while (index > 0) {
//                contentBuilder.append(".");
//                index -= 1;
//            }
            hardwareSearchHint.setVisibility(View.VISIBLE);
            hardwareSearchHint.setText(contentBuilder.toString());
        }
    }

    @Override
    protected void bindDeviceStatusChange(int status) {
        super.bindDeviceStatusChange(status);
        bindDeviceStatus = status;
        switch (bindDeviceStatus) {
            case ConstantField.BindDeviceStatus.SEARCHING:
                isScan = true;
                setBoxSearching(true);
                hardwareSearchHint.setTextColor(getResources().getColor(R.color.blue_ff337aff));
                hardwareSearchHint.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.dp_12));
                hardwareSearchHint.setTypeface(Typeface.DEFAULT);
                hardwareVerificationHint.setText(isPCHost ? R.string.bind_power_tip : R.string.lan_verify_hint);
                hardwareVerificationHint.setTextColor(getResources().getColor(R.color.gray_ff85899c));
                hardwareVerificationHintSecond.setVisibility(isPCHost ? View.VISIBLE : View.GONE);
                startSearch.setClickable(false);
                startSearch.setVisibility(View.INVISIBLE);
                break;
            case ConstantField.BindDeviceStatus.EMPTY:
                isScan = false;
                hardwareVerificationHintSecond.setVisibility(View.GONE);
                setBoxSearching(false);
                hardwareSearchHint.setText(R.string.no_device_found);
                hardwareSearchHint.setTextColor(getResources().getColor(R.color.black_ff333333));
                hardwareSearchHint.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.dp_17));
                hardwareSearchHint.setTypeface(Typeface.DEFAULT_BOLD);
                hardwareVerificationHint.setText(Html.fromHtml((getString(R.string.lan_verify_empty_hint_part_1)
                        + "<b><tt>" + getString(R.string.left_quotation_mark) + getString(R.string.rescan)
                        + getString(R.string.right_quotation_mark) + "</tt></b>"
                        + getString(R.string.lan_verify_empty_hint_part_2))));
                startSearch.setVisibility(View.VISIBLE);
                startSearch.setText(R.string.rescan);
                startSearch.setClickable(true);
                break;
            case ConstantField.BindDeviceStatus.BINDING:
                isScan = false;
                setBoxSearingIndicator(0);
                break;
            case ConstantField.BindDeviceStatus.BIND_FAILED:
                hardwareVerificationHintSecond.setVisibility(View.GONE);
                isScan = false;
                setBoxSearching(false);
                setBoxSearingIndicator(-1);
                break;
            case ConstantField.BindDeviceStatus.PC_HOST_PREPARE:
                hardwareVerificationHintSecond.setVisibility(View.GONE);
                isScan = false;
                setBoxSearching(false);
                hardwareVerificationHint.setVisibility(View.VISIBLE);
                hardwareVerificationHint.setText(R.string.pc_host_verify_hint);
                startSearch.setVisibility(View.VISIBLE);
                startSearch.setText(R.string.scan_ready);
                startSearch.setClickable(true);
                break;
            default:
                break;
        }
    }

    private void startScanQRCode() {
        Intent intent = new Intent(LanVerificationActivity.this, CaptureActivity.class);
        intent.setAction(Intents.Scan.ACTION);
        //全屏扫描
        intent.putExtra(Intents.Scan.WIDTH, ViewUtils.getScreenWidth(getApplicationContext()));
        intent.putExtra(Intents.Scan.HEIGHT, ViewUtils.getScreenHeight(getApplicationContext()));
        //只扫描二维码
        intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
        intent.putExtra(ConstantField.ZxingCommunication.FUNCTION_EXTRA_KEY, ConstantField.ZxingCommunication.PC_HOST_EXTRA_VALUE);
        intent.putExtra(ConstantField.ZxingCommunication.IMMEDIATE_EXTRA_KEY, false);
        intent.putExtra(ConstantField.ZxingCommunication.DEFAULT_STATUS, "");
        intent.putExtra(ConstantField.ZxingCommunication.CUSTOMIZE_PATTERN, ConstantField.ZxingCommunication.CUSTOMIZE_PATTERN_SCAN_DEVICE_QR_CODE);
        intent.putExtra(ConstantField.ZxingCommunication.CUSTOMIZE_PATTERN_HINT, getString(R.string.scan_qr_code_on_browser));
        boolean isChinese = Utils.isChineseLanguage(this);
        intent.putExtra(ConstantField.ZxingCommunication.CUSTOMIZE_QR_TIP_RES_ID, isChinese ? R.drawable.icon_customize_scan_device_qr_tip : R.drawable.icon_customize_scan_device_qr_tip_en);
        startActivityForResult(intent, ConstantField.RequestCode.REQUEST_CODE_SCAN);
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void spacePlatformEnvironment() {
        super.spacePlatformEnvironment();
        spacePlatformEnvironmentBridge = SpacePlatformEnvironmentBridge.getInstance();
        spacePlatformEnvironmentBridge.registerSourceCallback(this);
        Intent intent = new Intent(LanVerificationActivity.this, SpacePlatformEnvironmentActivity.class);
        startActivityForResult(intent, ConstantField.RequestCode.SPACE_PLATFORM_ENVIRONMENT_CODE);
    }

    @Override
    protected void handleSwitchPlatformCallback(String source, int code, SwitchPlatformResult result, boolean isError) {
        super.handleSwitchPlatformCallback(source, code, result, isError);
        if (spacePlatformEnvironmentBridge != null) {
            spacePlatformEnvironmentBridge.handleSwitchPlatformResponse(code, source, result, isError);
        }
    }

    @Override
    protected void handleSwitchStatusCallback(String source, int code, SwitchStatusResult result) {
        super.handleSwitchStatusCallback(source, code, result);
        if (spacePlatformEnvironmentBridge != null) {
            spacePlatformEnvironmentBridge.handleSwitchStatusResponse(code, source, result);
        }
    }

    @Override
    protected void onDestroy() {
        if (spacePlatformEnvironmentBridge != null) {
            spacePlatformEnvironmentBridge.unregisterSourceCallback();
            spacePlatformEnvironmentBridge = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.start_search:
                    if ((bindDeviceStatus == ConstantField.BindDeviceStatus.PC_HOST_PREPARE)
                            || (bindDeviceStatus == ConstantField.BindDeviceStatus.EMPTY && (isPCHost || btid != null))) {
                        String[] unGetPerList = PermissionUtils.unGetPermissions(this, PermissionUtils.PERMISSION_CAMERA);
                        if (unGetPerList.length == 0) {
                            startScanQRCode();
                        } else {
                            PermissionUtils.requestPermissionGroupWithNotice(this, unGetPerList, (result, extraMsg) -> {
                                if (result) {
                                    startScanQRCode();
                                }
                            });
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void handleRequestSwitchPlatform(String transId, String domain) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.switchPlatform(transId, domain);
            }
        });
    }

    @Override
    public void handleRequestSwitchStatus(String transId) {
        handlerPost(() -> {
            if (presenter != null) {
                presenter.getSwitchPlatformStatus(transId);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        boolean isResultOk = (resultCode == Activity.RESULT_OK);
        switch (requestCode) {
            case ConstantField.RequestCode.RESET_SECURITY_PASSWORD_CODE:
                boolean isGranteeSecurityTokenExpire = false;
                if (data != null && data.hasExtra(ConstantField.GRANTEE_SECURITY_TOKEN_EXPIRE)) {
                    isGranteeSecurityTokenExpire = data.getBooleanExtra(ConstantField.GRANTEE_SECURITY_TOKEN_EXPIRE, false);
                }
                EventBusUtil.post(new SecurityOperationResultEvent((isResultOk || isGranteeSecurityTokenExpire), authenticationUuid));
                if (forgetPasswordUuid != null) {
                    EventBusUtil.post(new ForgetPasswordResultEvent(forgetPasswordUuid, isResultOk));
                }
                finish();
                break;
            case ConstantField.RequestCode.SET_SECURITY_EMAIL_CODE:
                EventBusUtil.post(new SecurityOperationResultEvent(isResultOk, authenticationUuid));
                finish();
                break;
            case ConstantField.RequestCode.SPACE_PLATFORM_ENVIRONMENT_CODE:
                EventBusUtil.post(new SecurityOperationResultEvent(isResultOk, authenticationUuid));
                finish();
                break;
            case ConstantField.RequestCode.REQUEST_CODE_SCAN:
                boolean isRecognize = false;
                String result = null;
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        if (bundle != null) {
                            result = bundle.getString(Intents.Scan.RESULT);
                        }
                    }
                }
                if (result != null && (Patterns.WEB_URL.matcher(result).matches() || URLUtil.isValidUrl(result))) {
                    Uri uri = Uri.parse(result);
                    if (uri != null) {
                        String sn = null;
                        String nBtid = null;
                        String snHashHeader = null;
                        String ipAddress = null;
                        String portValue = null;
                        int port = -1;
                        Set<String> querySet = uri.getQueryParameterNames();
                        if (querySet != null) {
                            for (String query : querySet) {
                                if (ConstantField.SN.equals(query)) {
                                    sn = uri.getQueryParameter(ConstantField.SN);
                                }
                                if (ConstantField.BTID.equals(query)) {
                                    nBtid = uri.getQueryParameter(ConstantField.BTID);
                                }
                                if (ConstantField.IPADDR.equals(query)) {
                                    ipAddress = uri.getQueryParameter(ConstantField.IPADDR);
                                    String decodeIpAddress = null;
                                    if (ipAddress != null) {
                                        try {
                                            decodeIpAddress = URLDecoder.decode(ipAddress, "UTF-8");
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (decodeIpAddress != null) {
                                        ipAddress = decodeIpAddress;
                                    }
                                }
                                if (ConstantField.PORT.equals(query)) {
                                    portValue = uri.getQueryParameter(ConstantField.PORT);
                                }
                            }
                        }
                        if (sn != null) {
                            String snHash = FormatUtil.getSHA256String(sn);
                            if (snHash != null && snHash.length() > 16) {
                                snHashHeader = snHash.substring(0, 16);
                            } else {
                                snHashHeader = snHash;
                            }
                        }
                        if (portValue != null) {
                            try {
                                port = Integer.parseInt(portValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if ((StringUtil.isNonBlankString(snHashHeader) || nBtid != null) && ipAddress != null && port >= 0) {
                            isRecognize = true;
                            mIpBean = new IPBean();
                            mIpBean.setIPV4Address(ipAddress);
                            mIpBean.setPort(port);
                            if (StringUtil.isNonBlankString(snHashHeader)) {
                                btid = snHashHeader;
                            } else if (nBtid != null) {
                                btid = nBtid;
                            }
                        }
                    }
                }
                if (isRecognize) {
                    handlerPost(() -> {
                        bindDeviceStatusChange(ConstantField.BindDeviceStatus.SEARCHING);
                        super.initEvent();
                    });
                } else {
                    handlerPost(() -> showImageTextToast(R.drawable.toast_refuse, R.string.qr_code_unrecognized));
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void handleInitCallback(InitResponse initResponse) {
        super.handleInitCallback(initResponse);
        if (initResponse == null) {
            countDownFinish();
        }
    }
}
