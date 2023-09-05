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
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.bind.FindDevicePagerAdapter;
import xyz.eulix.space.bean.EulixBoxExtraInfo;
import xyz.eulix.space.bridge.FindBoxBridge;
import xyz.eulix.space.presenter.FindBoxPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 10:46
 */
public class FindBoxActivity extends AbsActivity<FindBoxPresenter.IFindBox, FindBoxPresenter> implements FindBoxPresenter.IFindBox, View.OnClickListener, FindDevicePagerAdapter.OnItemClickListener, FindBoxBridge.FindBoxSinkCallback {
    private ViewPager findDevicePager;
    private View findDevicePagerLeft, findDevicePagerRight;
    private ImageButton exit;
    private List<BluetoothDevice> bluetoothDevices;
    private FindDevicePagerAdapter adapter;
    private boolean isBusy;
    private String mBoxName;
    private String mProductId;
    private int mPaired;
    private int mDeviceModelNumber;
    private FindBoxHandler mHandler;
    private FindBoxBridge findBoxBridge;

    static class FindBoxHandler extends Handler {
        private WeakReference<FindBoxActivity> findBoxActivityWeakReference;

        public FindBoxHandler(FindBoxActivity activity) {
            findBoxActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            FindBoxActivity activity = findBoxActivityWeakReference.get();
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
        setContentView(R.layout.find_device_main);
        findDevicePager = findViewById(R.id.find_device_pager);
        findDevicePagerLeft = findViewById(R.id.find_device_pager_left);
        findDevicePagerRight = findViewById(R.id.find_device_pager_right);
        exit = findViewById(R.id.exit);
    }

    @Override
    public void initData() {
        mHandler = new FindBoxHandler(this);
        findBoxBridge = FindBoxBridge.getInstance();
        findBoxBridge.registerSinkCallback(this);
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

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public FindBoxPresenter createPresenter() {
        return new FindBoxPresenter();
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
            if (bluetoothDevices == null) {
                bluetoothDevices = new ArrayList<>();
            } else {
                bluetoothDevices.clear();
            }
            if (intent.hasExtra(ConstantField.EULIX_DEVICE)) {
                Parcelable[] eulixDeviceData = intent.getParcelableArrayExtra(ConstantField.EULIX_DEVICE);
                if (eulixDeviceData != null) {
                    for (Parcelable data : eulixDeviceData) {
                        if (data instanceof BluetoothDevice) {
                            bluetoothDevices.add((BluetoothDevice) data);
                        }
                    }
                } else {
                    bluetoothDevices = null;
                }
            }
            if (intent.hasExtra(ConstantField.BOX_NAME)) {
                mBoxName = intent.getStringExtra(ConstantField.BOX_NAME);
            }
            if (intent.hasExtra(ConstantField.PRODUCT_ID)) {
                mProductId = intent.getStringExtra(ConstantField.PRODUCT_ID);
            }
            mPaired = intent.getIntExtra(ConstantField.BOUND, 1);
            mDeviceModelNumber = intent.getIntExtra(ConstantField.DEVICE_MODEL_NUMBER, 0);
            if (bluetoothDevices == null || bluetoothDevices.size() <= 0) {
                handleResult();
            } else {
                if (bluetoothDevices.size() > 1) {
                    mBoxName = null;
                    mProductId = null;
                    mPaired = 1;
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
        EulixBoxExtraInfo eulixBoxExtraInfo = new EulixBoxExtraInfo();
        eulixBoxExtraInfo.setBoxName(mBoxName);
        eulixBoxExtraInfo.setProductId(mProductId);
        eulixBoxExtraInfo.setBind(mPaired == 0);
        eulixBoxExtraInfo.setDeviceModelNumber(mDeviceModelNumber);
        adapter = new FindDevicePagerAdapter(this, bluetoothDevices, eulixBoxExtraInfo);
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

    private void handleResult() {
        handleResult(false);
    }

    private void handleResult(boolean isOk) {
        if (adapter != null) {
            adapter.updateView(-1, false);
        }
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
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
        if (findBoxBridge != null) {
            findBoxBridge.unregisterSinkCallback();
            findBoxBridge = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.exit:
                    handleResult();
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
                        if (bluetoothDevices != null && position >= 0 && bluetoothDevices.size() > position) {
                            isBusy = true;
                            if (findBoxBridge != null) {
                                findBoxBridge.selectBox(bluetoothDevices.get(position), mPaired);
                            }
                        }
                    }
                    break;
                case R.id.item_exit:
                    handleResult();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void handleBindResult(boolean isSuccess, boolean isFinish) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isBusy) {
                    isBusy = false;
                    if (adapter != null) {
                        adapter.updateView(-1, false);
                    }
                }
                if (isSuccess) {
                    handleResult(true);
                } else {
                    if (isFinish) {
                        handleResult(false);
                    } else {
                        showImageTextToast(R.drawable.toast_refuse, R.string.bluetooth_connect_fail_hint);
                    }
                }
            });
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isBusy) {
                    isBusy = false;
                }
                if (adapter != null) {
                    adapter.updateView(-1, false);
                    handleResult(false);
                }
            });
        }
    }
}
