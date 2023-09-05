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
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.bind.FindDevicePagerAdapter;
import xyz.eulix.space.adapter.bind.LanFindDevicePagerAdapter;
import xyz.eulix.space.bean.EulixBoxExtraInfo;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.LanServiceInfo;
import xyz.eulix.space.bridge.LanFindBoxBridge;
import xyz.eulix.space.presenter.LanFindBoxPresenter;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/2 18:21
 */
public class LanFindBoxActivity extends AbsActivity<LanFindBoxPresenter.ILanFindBox, LanFindBoxPresenter> implements LanFindBoxPresenter.ILanFindBox, View.OnClickListener, LanFindDevicePagerAdapter.OnItemClickListener, LanFindBoxBridge.LanFindBoxSinkCallback {
    private ViewPager findDevicePager;
    private View findDevicePagerLeft, findDevicePagerRight;
    private ImageButton exit;
    private List<LanServiceInfo> nsdServiceInfos;
    private LanFindDevicePagerAdapter adapter;
    private boolean isBusy;
    private String mBoxName;
    private String mProductId;
    private int mPaired;
    private int mDeviceModelNumber;
    private LanFindBoxHandler mHandler;
    private LanFindBoxBridge mBridge;

    static class LanFindBoxHandler extends Handler {
        private WeakReference<LanFindBoxActivity> lanFindBoxActivityWeakReference;

        public LanFindBoxHandler(LanFindBoxActivity activity) {
            lanFindBoxActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            LanFindBoxActivity activity = lanFindBoxActivityWeakReference.get();
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
        mHandler = new LanFindBoxHandler(this);
        mBridge = LanFindBoxBridge.getInstance();
        mBridge.registerSinkCallback(this);
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

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            if (nsdServiceInfos == null) {
                nsdServiceInfos = new ArrayList<>();
            } else {
                nsdServiceInfos.clear();
            }
            if (intent.hasExtra(ConstantField.EULIX_DEVICE)) {
                Parcelable[] eulixDeviceData = intent.getParcelableArrayExtra(ConstantField.EULIX_DEVICE);
                if (eulixDeviceData != null) {
                    for (Parcelable data : eulixDeviceData) {
                        if (data instanceof NsdServiceInfo) {
                            nsdServiceInfos.add(new LanServiceInfo((NsdServiceInfo) data));
                        }
                    }
                } else {
                    nsdServiceInfos = null;
                }
            } else if (intent.hasExtra(ConstantField.OTHER_DEVICE)) {
                String otherDeviceData = intent.getStringExtra(ConstantField.OTHER_DEVICE);
                if (otherDeviceData != null) {
                    IPBean ipBean = null;
                    try {
                        ipBean = new Gson().fromJson(otherDeviceData, IPBean.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    if (ipBean != null) {
                        nsdServiceInfos.add(new LanServiceInfo(ipBean));
                    }
                } else {
                    nsdServiceInfos = null;
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
            if (nsdServiceInfos == null || nsdServiceInfos.size() <= 0) {
                handleResult();
            } else {
                if (nsdServiceInfos.size() > 1) {
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
        adapter = new LanFindDevicePagerAdapter(this, nsdServiceInfos, eulixBoxExtraInfo);
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
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public LanFindBoxPresenter createPresenter() {
        return new LanFindBoxPresenter();
    }

    @Override
    protected boolean isDialogStyle() {
        return true;
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
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
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
                        if (nsdServiceInfos != null && position >= 0 && nsdServiceInfos.size() > position) {
                            isBusy = true;
                            if (mBridge != null) {
                                mBridge.selectBox(nsdServiceInfos.get(position), mPaired);
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
                    }
                }
            });
        }
    }
}
