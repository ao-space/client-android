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

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.DockerServiceAdapter;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.presenter.BoxSystemDetailPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.ToastUtil;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 盒子系统规格详情页
 * History:     2022/7/19
 */
public class BoxSystemDetailActivity extends AbsActivity<BoxSystemDetailPresenter.IBoxSystemDetail, BoxSystemDetailPresenter> implements BoxSystemDetailPresenter.IBoxSystemDetail {
    private TitleBarWithSelect titleBar;
    private TextView tvVersion;
    private TextView tvOsVersion;
    private RecyclerView recyclerView;
    private LinearLayout btnCheckUpdate;
    private LottieAnimationView loadingAnim;
    private TextView tvBtnText;

    private DockerServiceAdapter adapter;

    private DeviceVersionInfoBean mDeviceVersionInfoBean;

    public static void startActivity(Context context, DeviceVersionInfoBean deviceVersionInfoBean) {
        Intent intent = new Intent(context, BoxSystemDetailActivity.class);
        intent.putExtra("deviceInfoBean", deviceVersionInfoBean);
        context.startActivity(intent);
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_box_system_detail);

        titleBar = findViewById(R.id.title_bar);
        tvVersion = findViewById(R.id.tv_version);
        tvOsVersion = findViewById(R.id.tv_os_version);
        btnCheckUpdate = findViewById(R.id.btn_check_update);
        loadingAnim = findViewById(R.id.loading_animation);
        tvBtnText = findViewById(R.id.tv_btn_text);
        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DockerServiceAdapter(this);
        recyclerView.setAdapter(adapter);

        EventBusUtil.register(this);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        if (getIntent().getSerializableExtra("deviceInfoBean") != null) {
            mDeviceVersionInfoBean = (DeviceVersionInfoBean) getIntent().getSerializableExtra("deviceInfoBean");
        }
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(getResources().getString(R.string.system_specifications));
        if (presenter.isActiveUserAdmin()) {
            btnCheckUpdate.setVisibility(View.VISIBLE);
        } else {
            btnCheckUpdate.setVisibility(View.GONE);
        }
        if (mDeviceVersionInfoBean != null) {
            refreshViewInfo(mDeviceVersionInfoBean);
        } else {
            showLoading("");
        }
        presenter.getDeviceVersionDetailInfo();
    }

    private void refreshViewInfo(DeviceVersionInfoBean deviceVersionInfoBean) {
        if (deviceVersionInfoBean == null) {
            return;
        }
        tvVersion.setText(getResources().getString(R.string.app_name) + " " + deviceVersionInfoBean.spaceVersion);
        tvOsVersion.setText(deviceVersionInfoBean.osVersion);
        if (deviceVersionInfoBean.serviceDetail != null && !deviceVersionInfoBean.serviceDetail.isEmpty()) {
            adapter.dataList.clear();
            adapter.dataList.addAll(deviceVersionInfoBean.serviceVersion);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void initEvent() {
        btnCheckUpdate.setOnClickListener(v -> {
            Logger.d("zfy", "click btnCheckUpdate");
            changeBtnState(true);
            presenter.checkBoxVersion();
        });
    }

    @NonNull
    @Override
    public BoxSystemDetailPresenter createPresenter() {
        return new BoxSystemDetailPresenter();
    }

    private void changeBtnState(boolean isChecking) {
        if (isChecking) {
            btnCheckUpdate.setClickable(false);
            loadingAnim.setVisibility(View.VISIBLE);
            LottieUtil.loop(loadingAnim, "loading_button.json");
            tvBtnText.setText(getResources().getString(R.string.check_doing));
        } else {
            btnCheckUpdate.setClickable(true);
            LottieUtil.stop(loadingAnim);
            loadingAnim.setVisibility(View.GONE);
            tvBtnText.setText(getResources().getString(R.string.check_update));
        }
    }

    @Override
    public void onCheckCallback(boolean hasUpdate) {
        changeBtnState(false);
        if (!hasUpdate) {
            ToastUtil.showToast(getResources().getString(R.string.already_latest_version));
        } else {
            //进入系统升级页面
            Intent intent = new Intent(BoxSystemDetailActivity.this, SystemUpdateActivity.class);
            if (ConstantField.hasClickSystemUpgradeInstallLater) {
                intent.putExtra(SystemUpdateActivity.KEY_DIALOG_OPERATE_TYPE, SystemUpdateActivity.DIALOG_OPERATE_TYPE_INSTALL_LATER);
            }
            startActivity(intent);
        }
    }

    @Override
    public void onCheckError(String msg) {
        changeBtnState(false);
        if (!TextUtils.isEmpty(msg) && msg.contains(String.valueOf(ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR))){
            showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
        } else {
            ToastUtil.showToast(getResources().getString(R.string.network_exception_hint));
        }
    }

    @Override
    public void onGetDeviceVersionInfo(boolean result, DeviceVersionInfoBean deviceVersionInfoBean) {
        closeLoading();
        if (result && deviceVersionInfoBean != null) {
            refreshViewInfo(deviceVersionInfoBean);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxVersionDetailInfoEvent event) {
        String deviceCacheStr = PreferenceUtil.getDeviceVersionDetailInfo(this);
        if (!TextUtils.isEmpty(deviceCacheStr)) {
            DeviceVersionInfoBean deviceVersionInfoBean = new Gson().fromJson(deviceCacheStr, DeviceVersionInfoBean.class);
            refreshViewInfo(deviceVersionInfoBean);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBusUtil.unRegister(this);
    }
}