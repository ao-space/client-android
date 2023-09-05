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

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.LocalMediaSelectAdapter;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.bean.PhotoUpImageBucket;
import xyz.eulix.space.presenter.GalleryPictureSelectPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.LocalMediaUpSelectHelper;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.FolderPopWindow;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 相册图片选择页面
 * History:     2021/9/15
 */
public class GalleryPictureSelectActivity extends AbsActivity<GalleryPictureSelectPresenter.IGalleryPictureSelect, GalleryPictureSelectPresenter> implements GalleryPictureSelectPresenter.IGalleryPictureSelect, View.OnClickListener {
    private TitleBarWithSelect titleBar;
    private RelativeLayout layoutTitle;
    private TextView tvTitleName;
    private ImageView imgTitleArrow;
    private RecyclerView recyclerView;
    private LocalMediaSelectAdapter adapter;
    private RelativeLayout layoutNoData;
    private List<PhotoUpImageBucket> mAlbumList;
    private FolderPopWindow folderWindow;

    @Override
    public void initView() {
        setContentView(R.layout.activity_gallery_pictiure_select);
        titleBar = findViewById(R.id.title_bar);
        layoutTitle = findViewById(R.id.layout_title);
        tvTitleName = findViewById(R.id.tv_title);
        imgTitleArrow = findViewById(R.id.img_title_arrow);
        layoutNoData = findViewById(R.id.layout_no_data);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new LocalMediaSelectAdapter(this, ConstantField.MediaType.MEDIA_IMAGE);
        adapter.setCheckStateVisible(false);
        adapter.setSelectListener(new LocalMediaSelectAdapter.ImageSelectListener() {
            @Override
            public void onSelectStateChange(LocalMediaUpItem item, int position, boolean isSelect) {
                String path = item.getMediaPath();
                Intent intent = new Intent();
                intent.putExtra("path", path);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

            @Override
            public void onItemLongClick(LocalMediaUpItem item, int position) {

            }
        });
        recyclerView.setAdapter(adapter);


        folderWindow = new FolderPopWindow(this);
        folderWindow.setOnItemClickListener((folderName, images) -> {
            tvTitleName.setText(folderName);
            folderWindow.dismiss();
            adapter.dataList.clear();
            adapter.dataList.addAll(images);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initViewData() {
        //获取相簿列表
        LocalMediaUpSelectHelper localMediaUpSelectHelper = LocalMediaUpSelectHelper.getHelper();
        localMediaUpSelectHelper.init(GalleryPictureSelectActivity.this, ConstantField.MediaType.MEDIA_IMAGE);
        localMediaUpSelectHelper.setCreateAll(true);
        localMediaUpSelectHelper.setIncludeGif(false);
        localMediaUpSelectHelper.setGetAlbumListListener((list, totalFilesList) -> {
            closeLoading();
            if (list.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutNoData.setVisibility(View.VISIBLE);
            } else {
                mAlbumList = list;
                adapter.dataList.clear();
                adapter.dataList.addAll(totalFilesList);
                tvTitleName.setText(getResources().getString(R.string.all_pictures));
                imgTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_close);
                mAlbumList.get(0).setChecked(true);
                folderWindow.bindFolder(mAlbumList);
            }
        });
        showLoading(null);
        localMediaUpSelectHelper.execute(true);
    }

    @Override
    public void initEvent() {
        layoutTitle.setOnClickListener(this);
        folderWindow.setOnDismissListener(() -> {
            imgTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_close);
        });
    }

    @NotNull
    @Override
    public GalleryPictureSelectPresenter createPresenter() {
        return new GalleryPictureSelectPresenter();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_title:
                //切换相册
                if (folderWindow.isShowing()) {
                    folderWindow.dismiss();
                } else {
                    if (mAlbumList != null && mAlbumList.size() > 0) {
                        folderWindow.showAsDropDown(titleBar);
                        imgTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_open);
                    }
                }
                break;
            default:
                break;
        }
    }
}