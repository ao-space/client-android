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

package xyz.eulix.space;

import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.FindDevicePagerAdapter;
import xyz.eulix.space.bean.EulixDevice;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.presenter.FindDevicePresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * date: 2021/7/15 11:44
 */
public class EulixFindDeviceActivity extends AbsActivity<FindDevicePresenter.IFindDevice, FindDevicePresenter> implements FindDevicePresenter.IFindDevice, View.OnClickListener, FindDevicePagerAdapter.OnItemClickListener {
    private ViewPager findDevicePager;
    private View findDevicePagerLeft, findDevicePagerRight;
    private ImageButton exit;
    private List<EulixDevice> eulixDevices;
    private Integer mCode;
    private FindDevicePagerAdapter adapter;
    private boolean isBusy;
    private EulixFindDeviceHandler mHandler;
    private ContentObserver boxObserver;
    private boolean isBoxObserve;

    static class EulixFindDeviceHandler extends Handler {
        private WeakReference<EulixFindDeviceActivity> eulixFindDeviceActivityWeakReference;

        public EulixFindDeviceHandler(EulixFindDeviceActivity activity) {
            eulixFindDeviceActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixFindDeviceActivity activity = eulixFindDeviceActivityWeakReference.get();
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
    public void initData() {
        mHandler = new EulixFindDeviceHandler(this);
        boxObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                handleEulixSpaceDBChange();
            }
        };
    }

    @Override
    public void initView() {
        setContentView(R.layout.find_device_main);
        findDevicePager = findViewById(R.id.find_device_pager);
        findDevicePagerLeft = findViewById(R.id.find_device_pager_left);
        findDevicePagerRight = findViewById(R.id.find_device_pager_right);
        exit = findViewById(R.id.exit);
    }

    @Override
    public void initViewData() {
        // Do nothing
    }

    @Override
    public void initEvent() {
        exit.setOnClickListener(this);
        findDevicePagerLeft.setOnClickListener(this);
        findDevicePagerRight.setOnClickListener(this);
    }

    @NotNull
    @Override
    public FindDevicePresenter createPresenter() {
        return new FindDevicePresenter();
    }

    @Override
    protected boolean isDialogStyle() {
        return true;
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            if (eulixDevices == null) {
                eulixDevices = new ArrayList<>();
            } else {
                eulixDevices.clear();
            }
            if (intent.hasExtra(ConstantField.EULIX_DEVICE)) {
                String eulixDeviceData = intent.getStringExtra(ConstantField.EULIX_DEVICE);
                if (!TextUtils.isEmpty(eulixDeviceData)) {
                    try {
                        eulixDevices = new Gson().fromJson(eulixDeviceData, new TypeToken<List<EulixDevice>>(){}.getType());
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (eulixDevices == null || eulixDevices.size() <= 0) {
                handleResult();
            } else {
                if (eulixDevices.size() > 1) {
                    exit.setClickable(true);
                    exit.setVisibility(View.VISIBLE);
                } else {
                    exit.setClickable(false);
                    exit.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void initAdapter() {
        adapter = new FindDevicePagerAdapter(this, eulixDevices);
        adapter.setOnItemClickListener(this);
        float pageMargin = getResources().getDimension(R.dimen.dp_10) - (getResources().getDimension(R.dimen.dp_307)
                - getResources().getDimension(R.dimen.dp_205)) * getResources().getDimension(R.dimen.dp_259)
                / (2 * getResources().getDimension(R.dimen.dp_307));
        findDevicePager.setPageMargin(pageMargin < 0 ? (-1 * Math.round(Math.abs(pageMargin))) : Math.round(pageMargin));
        findDevicePager.setOffscreenPageLimit(3);
        findDevicePager.setPageTransformer(false, new FindDevicePagerAdapter.ScaleTransformer(205.0f / 307));
        findDevicePager.setAdapter(adapter);
    }

    private void changePager(boolean isNext) {
        if (adapter != null) {
            int totalCount = adapter.getCount();
            int index = findDevicePager.getCurrentItem() + (isNext ? 1 : -1);
            if (index >= 0 && index < totalCount) {
                try {
                    findDevicePager.setCurrentItem(index, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void bindDevice(int position) {
        isBusy = true;
        EulixDevice eulixDevice = null;
        if (eulixDevices != null && position >= 0 && eulixDevices.size() > position) {
            eulixDevice = eulixDevices.get(position);
            if (!presenter.initDevice(eulixDevice)) {
                isBusy = false;
                handleResult();
            }
        }
    }

    private void closeDevice(int position) {
        if (eulixDevices != null && eulixDevices.size() > position && adapter != null) {
            try {
                eulixDevices.remove(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
            adapter.updateData(eulixDevices);
        }
    }

    private void handleResult() {
        handleResult(false, null, null);
    }

    private void handleResult(boolean isOk, EulixDevice device, Integer code) {
        if (adapter != null) {
            adapter.updateView(-1, false);
        }
        Intent intent = new Intent();
        if (device != null) {
            intent.putExtra(ConstantField.EULIX_DEVICE, new Gson().toJson(device));
        }
        if (code != null) {
            intent.putExtra(ConstantField.CODE, code.intValue());
        }
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    private void obtainAccessToken(String boxUuid) {
        Intent serviceIntent = new Intent(EulixFindDeviceActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        if (boxUuid != null) {
            serviceIntent.putExtra(ConstantField.BOX_UUID, boxUuid);
        }
        serviceIntent.putExtra(ConstantField.BOX_BIND, "1");
        startService(serviceIntent);
    }

    private void handleEulixSpaceDBChange() {
        if (presenter != null) {
            if (presenter.handleDBChange()) {
                if (isBoxObserve && boxObserver != null) {
                    getContentResolver().unregisterContentObserver(boxObserver);
                }
                presenter.handleDBResult(mCode);
            }
        }
    }

    private void handlePairing(EulixDevice device, Integer code, String boxUuid, String boxPubKey, String authKey, String regKey, String userDomain) {
        isBusy = false;
        if (device != null && !TextUtils.isEmpty(boxUuid) && !TextUtils.isEmpty(boxPubKey)
                && !TextUtils.isEmpty(authKey) && !TextUtils.isEmpty(regKey) && !TextUtils.isEmpty(userDomain)) {
            device.setUuid(boxUuid);
        }
        handleResult(true, device, code);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isBusy = false;
        handleIntent(getIntent());
        initAdapter();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void pairingCallback(EulixDevice device, Integer code, String boxUuid, String boxPubKey, String authKey, String regKey, String userDomain) {
        if (mHandler != null) {
            mHandler.post(() -> {
                handlePairing(device, code, boxUuid, boxPubKey, authKey, regKey, userDomain);
            });
        }
    }

    @Override
    public void handleAccessToken(EulixDevice device, Integer code, String boxUuid) {
        mCode = code;
        if (mHandler != null) {
            mHandler.post(() -> {
                if (boxObserver != null && !isBoxObserve) {
                    isBoxObserve = true;
                    getContentResolver().registerContentObserver(EulixSpaceDBManager.BOX_URI, true, boxObserver);
                }
                obtainAccessToken(boxUuid);
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.exit:
                    if (!isBusy) {
                        handleResult();
                    }
                    break;
                case R.id.find_device_pager_left:
                    if (!isBusy) {
                        changePager(false);
                    }
                    break;
                case R.id.find_device_pager_right:
                    if (!isBusy) {
                        changePager(true);
                    }
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
                case R.id.loading_button_container:
                    if (!isBusy) {
                        if (adapter != null) {
                            adapter.updateView(position, true);
                        }
                        bindDevice(position);
                    }
                    break;
                case R.id.item_exit:
                    if (!isBusy) {
                        handleResult();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
