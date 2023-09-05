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

package xyz.eulix.space.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.files.FilePagerAdapter;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.event.SpaceOnlineCallbackEvent;
import xyz.eulix.space.fragment.TransferListFragment;
import xyz.eulix.space.manager.BoxNetworkCheckManager;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.presenter.TransferListPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.Utils;
import xyz.eulix.space.view.TitleBarWithSelect;
import xyz.eulix.space.widgets.FixableViewPager;

/**
 * Author:      Zhu Fuyu
 * Description: 传输列表页
 * History:     2021/8/13
 */
public class TransferListActivity extends AbsActivity<TransferListPresenter.ITransferList, TransferListPresenter> implements TransferListPresenter.ITransferList {
    private TitleBarWithSelect titleBar;
    private TabLayout tabLayout;
    private FixableViewPager viewPager;
    private PagerAdapter adapter;
    List<Fragment> fragments = new ArrayList<>();
    private int currentPageIndex = 0;
    private TextView tvStorageTitle;
    private TextView tvStorageSize;
    private TextView tvNetworkType;

    List<String> tabTitles = new ArrayList<>();

    private boolean isSelectMode = false;

    @Override
    public void initView() {
        setContentView(R.layout.activity_transfer_list_layout);
        titleBar = findViewById(R.id.title_bar);
        tabLayout = findViewById(R.id.transfer_tab);
        viewPager = findViewById(R.id.transfer_view_pager);
        tvStorageTitle = findViewById(R.id.tv_storage_title);
        tvStorageSize = findViewById(R.id.tv_storage_size);
        tvNetworkType = findViewById(R.id.tv_network_type);

        EventBusUtil.register(this);
    }

    @Override
    public void initData() {
        Logger.d("zfy", "initData");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBusUtil.unRegister(this);
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(R.string.transfer_list);
        titleBar.setDefaultShowAllSelect(false);

        tabTitles.add(getString(R.string.download_list));
        tabTitles.add(getString(R.string.upload_list));


        fragments.add(new TransferListFragment(ConstantField.TransferType.TYPE_DOWNLOAD));
        fragments.add(new TransferListFragment(ConstantField.TransferType.TYPE_UPLOAD));

        adapter = new FilePagerAdapter(getSupportFragmentManager(), fragments, tabTitles);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Logger.d("zfy", "onPageScrolled");
            }

            @Override
            public void onPageSelected(int position) {
                currentPageIndex = position;
                TransferListFragment fragment = (TransferListFragment) fragments.get(currentPageIndex);
                fragment.getSelectDetail((selectedCount, hasSelectedAll, isSelectMode)
                        -> refreshTitleBar(selectedCount, hasSelectedAll, isSelectMode));
                refreshStorageShow(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Logger.d("zfy", "onPageScrollStateChanged");
            }
        });

        refreshStorageShow(0);

        refreshNetChannelStateView();

        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setCustomView(getTabView(i));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TextView tvTitle = tab.getCustomView().findViewById(R.id.tv_tab_title);
                tvTitle.setTextColor(getResources().getColor(R.color.black_ff333333));
                ImageView dot = tab.getCustomView().findViewById(R.id.img_dot);
                if (tvTitle.getText().toString().equals(getString(R.string.upload_list)) && dot.getVisibility() == View.VISIBLE) {
                    dot.setVisibility(View.INVISIBLE);
                    PreferenceUtil.saveHasNewUploadTask(TransferListActivity.this, false);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                TextView tvTitle = tab.getCustomView().findViewById(R.id.tv_tab_title);
                tvTitle.setTextColor(getResources().getColor(R.color.c_ff85899c));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //do nothing
            }
        });

        TransferTaskManager.getInstance().refreshDoingCountFromDB();

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LanStatusEvent event) {
        Logger.d("zfy", "receive LanStatusEvent " + event.isLanEnable);
        refreshNetChannelStateView();
        super.onEvent(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpaceOnlineCallbackEvent event) {
        Logger.d("zfy", "receive SpaceOnlineCallbackEvent " + event.isOnline());
        refreshNetChannelStateView();
    }

    //更新当前网络网络通道状态
    private void refreshNetChannelStateView() {
        String networkTypeStr;
        if (LanManager.getInstance().isLanEnable()) {
            //局域网在线
            networkTypeStr = getResources().getString(R.string.lan);
        } else if (!BoxNetworkCheckManager.getActiveDeviceOnlineStrict()) {
            //离线
            networkTypeStr = getResources().getString(R.string.offline);
        } else {
            //互联网
            networkTypeStr = getResources().getString(R.string.internet);
        }
        if (tvNetworkType != null) {
            tvNetworkType.setText(networkTypeStr);
        }
    }

    //刷新空间大小显示
    private void refreshStorageShow(int position) {
        if (position == 0) {
            String usedStorage = FormatUtil.formatSimpleSize(Utils.getLocalStorageUsedSize(), ConstantField.SizeUnit.FORMAT_1F, 1000);
            String totalStorage = FormatUtil.formatSimpleSize(Utils.getLocalStorageTotalSize(), ConstantField.SizeUnit.FORMAT_1F, 1000);
            tvStorageTitle.setText(R.string.phone_storage);
            tvStorageSize.setText(usedStorage + "/" + totalStorage);
        } else {
            tvStorageTitle.setText(R.string.space_storage);
            tvStorageSize.setText((presenter == null ? "--/--" : presenter.getSpaceStorageSizeContent()));
        }
    }

    private View getTabView(int index) {

        View inflate = LayoutInflater.from(this).inflate(R.layout.tansfer_tab_layout, null);
        TextView tabTV = inflate.findViewById(R.id.tv_tab_title);
        tabTV.setText(tabTitles.get(index));
        if (index == 0) {
            tabTV.setTextColor(getResources().getColor(R.color.black_ff333333));
        } else if (index == 1 && PreferenceUtil.getHasNewUploadTask(this)) {
            ImageView dot = inflate.findViewById(R.id.img_dot);
            dot.setVisibility(View.VISIBLE);
        }
        return inflate;
    }

    @Override
    public void initEvent() {
        titleBar.setClickListener(clickEvent -> {
            if (fragments.size() <= 0) {
                return;
            }
            TransferListFragment fragment = (TransferListFragment) fragments.get(currentPageIndex);
            fragment.onTitleBarStateChange(clickEvent);
        });

        titleBar.setOnLongClickListener(v -> {
            if (Logger.isDebuggable()) {
                Intent intent = new Intent(TransferListActivity.this, TransferLogShowActivity.class);
                startActivity(intent);
            }
            return false;
        });
    }

    @NotNull
    @Override
    public TransferListPresenter createPresenter() {
        return new TransferListPresenter();
    }

    public void refreshTitleBar(int selectedCount, boolean hasSelectedAll, boolean isSelectMode) {
        this.isSelectMode = isSelectMode;
        if (isSelectMode) {
            viewPager.setScrollable(false);
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                Objects.requireNonNull(tabLayout.getTabAt(i)).view.setClickable(false);
            }
            StringBuilder titleBarTextBuilder = new StringBuilder();
            titleBarTextBuilder.append(getString(R.string.choose_file_part_1));
            if (selectedCount > 999) {
                titleBarTextBuilder.append(getString(R.string.left_bracket));
                titleBarTextBuilder.append("999+");
                titleBarTextBuilder.append(getString(R.string.right_bracket));
            } else {
                titleBarTextBuilder.append(selectedCount);
            }
            titleBarTextBuilder.append(getString((Math.abs(selectedCount) == 1)
                    ? R.string.choose_file_part_2_singular : R.string.choose_file_part_2_plural));
            titleBar.setTitle(titleBarTextBuilder.toString());
            titleBar.setSelectState(true);
            titleBar.setHasSelectedAll(hasSelectedAll);
        } else {
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                Objects.requireNonNull(tabLayout.getTabAt(i)).view.setClickable(true);
            }
            viewPager.setScrollable(true);
            titleBar.setTitle(R.string.transfer_list);
            titleBar.setSelectState(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (isSelectMode) {
            TransferListFragment fragment = (TransferListFragment) fragments.get(currentPageIndex);
            fragment.clearSelectedItem();
            return;
        }
        super.onBackPressed();
    }
}