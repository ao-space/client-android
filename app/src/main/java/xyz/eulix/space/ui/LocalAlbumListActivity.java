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
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.LocalAlbumListAdapter;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.bean.PhotoUpImageBucket;
import xyz.eulix.space.manager.LocalMediaCacheManager;
import xyz.eulix.space.presenter.LocalAlbumListPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.LineItemDecoration;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 本地相册列表页
 * History:     2021/7/21
 */
public class LocalAlbumListActivity extends AbsActivity<LocalAlbumListPresenter.ILocalAlbumList, LocalAlbumListPresenter> implements LocalAlbumListPresenter.ILocalAlbumList {
    public static final String KEY_PATH = "path";
    public static final String KEY_MEDIA_TYPE = "mediaType";
    public static final String KEY_ALBUM_ID = "key_album_id";

    private TitleBarWithSelect titleBar;
    private RecyclerView recyclerView;
    private LocalAlbumListAdapter adapter;
    private RelativeLayout layoutNoData;
    private List<PhotoUpImageBucket> dataList = new ArrayList<>();
    private int mediaType;
    private String mPath;
    private int mAlbumId = -1;

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_local_album_list);
        titleBar = findViewById(R.id.title_bar);
        recyclerView = findViewById(R.id.rv_local_albums);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        mediaType = intent.getIntExtra(KEY_MEDIA_TYPE, ConstantField.MediaType.MEDIA_IMAGE);
        mAlbumId = intent.getIntExtra(KEY_ALBUM_ID, -1);
        if (intent.hasExtra(KEY_PATH)) {
            mPath = intent.getStringExtra(KEY_PATH);
        }
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(R.string.select_album);

        layoutNoData = findViewById(R.id.layout_no_data);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        adapter = new LocalAlbumListAdapter(this, mediaType, mPath);
        adapter.setOnItemClickListener(new LocalAlbumListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String bucketName) {
                Intent intent = new Intent(LocalAlbumListActivity.this, LocalMediaSelectActivity.class);
                intent.putExtra("bucketName", bucketName);
                intent.putExtra("mediaType", mediaType);
                if (mPath != null) {
                    intent.putExtra("path", mPath);
                }
                intent.putExtra("key_album_id", mAlbumId);
                startActivityForResult(intent, 0);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new LineItemDecoration(RecyclerView.VERTICAL, Math.round(getResources().getDimension(R.dimen.dp_1)),
                getResources().getColor(R.color.white_fff7f7f9)));

        //获取相簿列表
        showLoading(null);
        LocalMediaCacheManager.getAllMediaListByType(mediaType, new LocalMediaCacheManager.GetAlbumListListener() {
            @Override
            public void onGetAlbumList(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> totalFilesList) {
                closeLoading();
                adapter.dataList.addAll(list);
                adapter.notifyDataSetChanged();
                if (list.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    layoutNoData.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        if (resultCode == LocalMediaSelectActivity.RESULT_CODE) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void initEvent() {

    }

    @NotNull
    @Override
    public LocalAlbumListPresenter createPresenter() {
        return new LocalAlbumListPresenter();
    }

}