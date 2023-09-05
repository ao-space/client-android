package xyz.eulix.space.did.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.DeviceInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.did.DIDUtils;
import xyz.eulix.space.did.adapter.CredentialInformationAdapter;
import xyz.eulix.space.did.bean.CredentialInformationBean;
import xyz.eulix.space.did.bean.VerificationMethod;
import xyz.eulix.space.did.presenter.CredentialInformationPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;

public class CredentialInformationActivity extends AbsActivity<CredentialInformationPresenter.ICredentialInformation, CredentialInformationPresenter> implements CredentialInformationPresenter.ICredentialInformation, View.OnClickListener {
    private static final String CREDENTIAL_TYPE = "credential_type";
    private TextView title;
    private ImageButton back;
    private RecyclerView credentialInformationList;
    private CredentialInformationAdapter mAdapter;
    private List<CredentialInformationBean> mCredentialInformationBeanList;
    private int mCredentialTypeValue;
    private String mCredentialType;

    @Override
    public void initView() {
        setContentView(R.layout.activity_credential_information);
        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
        credentialInformationList = findViewById(R.id.credential_information_list);
    }

    @Override
    public void initData() {
        handleIntent(getIntent());
    }

    @Override
    public void initViewData() {
        title.setText(R.string.credential_information);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        generateDataList();
        mAdapter = new CredentialInformationAdapter(this, mCredentialInformationBeanList);
        credentialInformationList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        credentialInformationList.addItemDecoration(new CredentialInformationAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_10)), Color.TRANSPARENT));
        credentialInformationList.setAdapter(mAdapter);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        boolean isValid = false;
        if (intent != null) {
            mCredentialTypeValue = intent.getIntExtra(CREDENTIAL_TYPE, 0);
            switch (mCredentialTypeValue) {
                case CredentialInformationBean.CREDENTIAL_TYPE_AO_SPACE_SERVER:
                    mCredentialType = VerificationMethod.CREDENTIAL_TYPE_DEVICE;
                    isValid = true;
                    break;
                case CredentialInformationBean.CREDENTIAL_TYPE_BIND_PHONE:
                    mCredentialType = VerificationMethod.CREDENTIAL_TYPE_BINDER;
                    isValid = true;
                    break;
                case CredentialInformationBean.CREDENTIAL_TYPE_SECURITY_PASSWORD:
                    mCredentialType = VerificationMethod.CREDENTIAL_TYPE_PASSWORD;
                    isValid = true;
                    break;
                case CredentialInformationBean.CREDENTIAL_TYPE_AUTHORIZE_PHONE:
                    isValid = true;
                    break;
                case CredentialInformationBean.CREDENTIAL_TYPE_FRIEND:
                    isValid = true;
                    break;
                default:
                    break;
            }
        }
        if (!isValid) {
            finish();
        }
    }

    private void afterHandleCredentialInformationBeanList() {
        if (mCredentialInformationBeanList != null && !mCredentialInformationBeanList.isEmpty()) {
            switch (mCredentialTypeValue) {
                case CredentialInformationBean.CREDENTIAL_TYPE_SECURITY_PASSWORD:
                    String passwordEncryptPrivateKey = null;
                    if (presenter != null) {
                        passwordEncryptPrivateKey = presenter.getPasswordEncryptPrivateKey();
                    }
                    if (StringUtil.isNonBlankString(passwordEncryptPrivateKey)) {
                        CredentialInformationBean onDeviceBean = null;
                        CredentialInformationBean onBinderBean = null;
                        for (CredentialInformationBean bean : mCredentialInformationBeanList) {
                            if (bean != null) {
                                Integer locationIndex = null;
                                CredentialInformationBean.StorageLocation storageLocation = bean.getStorageLocation();
                                if (storageLocation != null) {
                                    locationIndex = storageLocation.getLocationIndex();
                                }
                                if (locationIndex != null) {
                                    switch (locationIndex) {
                                        case CredentialInformationBean.StorageLocation.LOCATION_SERVER:
                                            onDeviceBean = bean;
                                            break;
                                        case CredentialInformationBean.StorageLocation.LOCATION_CLIENT_BINDER:
                                            onBinderBean = bean;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                        if (onBinderBean == null && onDeviceBean != null) {
                            onBinderBean = onDeviceBean.cloneSelf();
                            CredentialInformationBean.StorageLocation nStorageLocation = new CredentialInformationBean.StorageLocation();
                            nStorageLocation.setLocationIndex(CredentialInformationBean.StorageLocation.LOCATION_CLIENT_BINDER);
                            if (presenter != null) {
                                UserInfo userInfo = presenter.getGranterUserInfo();
                                String phoneModel = null;
                                if (userInfo != null) {
                                    String deviceInfoValue = userInfo.getDeviceInfo();
                                    DeviceInfo deviceInfo = null;
                                    if (deviceInfoValue != null) {
                                        try {
                                            deviceInfo = new Gson().fromJson(deviceInfoValue, DeviceInfo.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (deviceInfo != null) {
                                        phoneModel = deviceInfo.getPhoneModel();
                                    }
                                }
                                nStorageLocation.setPhoneModel(phoneModel);
                            }
                            onBinderBean.setStorageLocation(nStorageLocation);
                            mCredentialInformationBeanList.add(onBinderBean);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private boolean checkCredentialType(String credentialType) {
        boolean isCheck = false;
        switch (mCredentialTypeValue) {
            case CredentialInformationBean.CREDENTIAL_TYPE_AO_SPACE_SERVER:
                isCheck = (VerificationMethod.CREDENTIAL_TYPE_DEVICE.equals(credentialType));
                break;
            case CredentialInformationBean.CREDENTIAL_TYPE_BIND_PHONE:
                isCheck = (VerificationMethod.CREDENTIAL_TYPE_BINDER.equals(credentialType));
                break;
            case CredentialInformationBean.CREDENTIAL_TYPE_SECURITY_PASSWORD:
                isCheck = (VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_DEVICE.equals(credentialType)
                        || VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_BINDER.equals(credentialType));
                break;
//            case CredentialInformationBean.CREDENTIAL_TYPE_AUTHORIZE_PHONE:
//                break;
//            case CredentialInformationBean.CREDENTIAL_TYPE_FRIEND:
//                break;
            default:
                break;
        }
        return isCheck;
    }

    private void generateDataList() {
        if (mCredentialInformationBeanList == null) {
            mCredentialInformationBeanList = new ArrayList<>();
        } else {
            mCredentialInformationBeanList.clear();
        }
        List<VerificationMethod> verificationMethodList = null;
        if (presenter != null) {
            verificationMethodList = presenter.getVerificationMethods();
        }
        if (mCredentialType != null && verificationMethodList != null) {
            for (VerificationMethod verificationMethod : verificationMethodList) {
                if (verificationMethod != null) {
                    String id = verificationMethod.getId();
                    Map<String, String> queryMap = DIDUtils.getDIDQueryMap(id);
                    if (queryMap != null && queryMap.containsKey(VerificationMethod.QUERY_CREDENTIAL_TYPE)) {
                        String credentialType = queryMap.get(VerificationMethod.QUERY_CREDENTIAL_TYPE);
                        if (checkCredentialType(credentialType)) {
                            String publicKeyValue = id;
                            if (publicKeyValue != null) {
                                int splitNumber = 2;
                                for (int i = 0; i < splitNumber; i++) {
                                    int publicKeyValueLength = publicKeyValue.length();
                                    int splitIndex = publicKeyValue.indexOf(":");
                                    if (splitIndex >= 0 && splitIndex < publicKeyValueLength) {
                                        if ((splitIndex + 1) >= publicKeyValueLength) {
                                            publicKeyValue = "";
                                            break;
                                        } else {
                                            publicKeyValue = publicKeyValue.substring((splitIndex + 1));
                                        }
                                    } else {
                                        break;
                                    }
                                }
                                if (StringUtil.isNonBlankString(publicKeyValue)) {
                                    int fragmentIndex = publicKeyValue.lastIndexOf("#");
                                    if (fragmentIndex >= 0 && fragmentIndex < publicKeyValue.length()) {
                                        publicKeyValue = publicKeyValue.substring(0, fragmentIndex);
                                    }
                                    int queryIndex = publicKeyValue.indexOf("?");
                                    if (queryIndex >= 0 && queryIndex < publicKeyValue.length()) {
                                        publicKeyValue = publicKeyValue.substring(0, queryIndex);
                                    }
                                }
                            }
                            Long lastUpdateTimestamp = null;
                            if (queryMap.containsKey(VerificationMethod.QUERY_VERSION_TIME)) {
                                String versionTime = queryMap.get(VerificationMethod.QUERY_VERSION_TIME);
                                if (versionTime != null) {
                                    // 默认类有时区，先注释
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Instant instant = null;
                                        try {
                                            instant = Instant.parse(versionTime);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (instant != null) {
                                            lastUpdateTimestamp = (instant.getEpochSecond() * ConstantField.TimeUnit.SECOND_UNIT
                                                    + instant.getNano() / 1000000);
                                        }
                                    }
                                    if (lastUpdateTimestamp == null) {
                                        lastUpdateTimestamp = FormatUtil.parseFileApiTimestamp(versionTime
                                                , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT
                                                , ConstantField.TimeStampFormat.FILE_API_SPLIT);
                                    }
                                }
                            }
                            CredentialInformationBean bean = new CredentialInformationBean();
                            bean.setPublicKey(publicKeyValue);
                            bean.setCredentialType(mCredentialTypeValue);
                            bean.setLastUpdateTimestamp(lastUpdateTimestamp);
                            CredentialInformationBean.StorageLocation storageLocation = new CredentialInformationBean.StorageLocation();
                            switch (mCredentialTypeValue) {
                                case CredentialInformationBean.CREDENTIAL_TYPE_AO_SPACE_SERVER:
                                    storageLocation.setLocationIndex(CredentialInformationBean.StorageLocation.LOCATION_SERVER);
                                    break;
                                case CredentialInformationBean.CREDENTIAL_TYPE_BIND_PHONE:
                                    storageLocation.setLocationIndex(CredentialInformationBean.StorageLocation.LOCATION_CLIENT_BINDER);
                                    if (presenter != null) {
                                        UserInfo userInfo = presenter.getGranterUserInfo();
                                        String phoneModel = null;
                                        if (userInfo != null) {
                                            String deviceInfoValue = userInfo.getDeviceInfo();
                                            DeviceInfo deviceInfo = null;
                                            if (deviceInfoValue != null) {
                                                try {
                                                    deviceInfo = new Gson().fromJson(deviceInfoValue, DeviceInfo.class);
                                                } catch (JsonSyntaxException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (deviceInfo != null) {
                                                phoneModel = deviceInfo.getPhoneModel();
                                            }
                                        }
                                        storageLocation.setPhoneModel(phoneModel);
                                    }
                                    break;
                                case CredentialInformationBean.CREDENTIAL_TYPE_SECURITY_PASSWORD:
                                    if (VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_DEVICE.equals(credentialType)) {
                                        storageLocation.setLocationIndex(CredentialInformationBean.StorageLocation.LOCATION_SERVER);
                                    } else if (VerificationMethod.CREDENTIAL_TYPE_PASSWORD_ON_BINDER.equals(credentialType)){
                                        storageLocation.setLocationIndex(CredentialInformationBean.StorageLocation.LOCATION_CLIENT_BINDER);
                                        if (presenter != null) {
                                            UserInfo userInfo = presenter.getGranterUserInfo();
                                            String phoneModel = null;
                                            if (userInfo != null) {
                                                String deviceInfoValue = userInfo.getDeviceInfo();
                                                DeviceInfo deviceInfo = null;
                                                if (deviceInfoValue != null) {
                                                    try {
                                                        deviceInfo = new Gson().fromJson(deviceInfoValue, DeviceInfo.class);
                                                    } catch (JsonSyntaxException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                if (deviceInfo != null) {
                                                    phoneModel = deviceInfo.getPhoneModel();
                                                }
                                            }
                                            storageLocation.setPhoneModel(phoneModel);
                                        }
                                    }
                                    break;
                                // todo 二期开发
//                                case CredentialInformationBean.CREDENTIAL_TYPE_AUTHORIZE_PHONE:
//                                    break;
//                                case CredentialInformationBean.CREDENTIAL_TYPE_FRIEND:
//                                    break;
                                default:
                                    break;
                            }
                            bean.setStorageLocation(storageLocation);
                            mCredentialInformationBeanList.add(bean);
                        }
                    }
                }
            }
            afterHandleCredentialInformationBeanList();
        }
    }

    @NotNull
    @Override
    public CredentialInformationPresenter createPresenter() {
        return new CredentialInformationPresenter();
    }

    public static void startThisActivity(@NonNull Context context, int credentialType) {
        Intent intent = new Intent(context, CredentialInformationActivity.class);
        intent.putExtra(CREDENTIAL_TYPE, credentialType);
        context.startActivity(intent);
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
}
