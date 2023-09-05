package com.google.zxing.client.android.local;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.bean.LocalMediaUpItem;
import com.google.zxing.client.android.bean.PhotoUpImageBucket;
import com.google.zxing.client.android.util.ConstantField;
import com.google.zxing.client.android.util.StatusBarUtil;
import com.google.zxing.client.android.util.ViewUtils;
import com.google.zxing.client.android.widget.FolderPopWindow;

import java.util.List;

public class LocalGalleryActivity extends Activity implements View.OnClickListener {
    private String mScanMode;
    private String mFunctionBeanJson = null;
    private String mImageUnit = null;
    private String mLocalAllImageText = null;
    private String mLocalGalleryEmptyHint = null;
    private RelativeLayout titleContainer;
    private RelativeLayout layoutTitle;
    private ImageButton back;
    private TextView tvTitleName;
    private ImageView imgTitleArrow;
    private RecyclerView recyclerView;
    private LocalMediaSelectAdapter adapter;
    private RelativeLayout layoutNoData;
    private TextView emptyFile;
    private List<PhotoUpImageBucket> mAlbumList;
    private FolderPopWindow folderWindow;

    private void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.SCAN_MODE)) {
                mScanMode = intent.getStringExtra(ConstantField.SCAN_MODE);
            }
            if (intent.hasExtra(ConstantField.FUNCTION_JSON)) {
                mFunctionBeanJson = intent.getStringExtra(ConstantField.FUNCTION_JSON);
            }
            if (intent.hasExtra(ConstantField.LOCAL_GALLERY_JSON)) {
                String localGalleryBeanJson = intent.getStringExtra(ConstantField.LOCAL_GALLERY_JSON);
                CaptureActivity.LocalGalleryBean localGalleryBean = null;
                if (localGalleryBeanJson != null) {
                    try {
                        localGalleryBean = new Gson().fromJson(localGalleryBeanJson, CaptureActivity.LocalGalleryBean.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                if (localGalleryBean != null) {
                    mImageUnit = localGalleryBean.getImageUnit();
                    mLocalAllImageText = localGalleryBean.getLocalAllImageText();
                    mLocalGalleryEmptyHint = localGalleryBean.getLocalGalleryEmptyHint();
                }
            }
        }
    }

    private void initView() {
        titleContainer = findViewById(R.id.title_container);
        layoutTitle = findViewById(R.id.layout_title);
        back = findViewById(R.id.back);
        tvTitleName = findViewById(R.id.tv_title);
        imgTitleArrow = findViewById(R.id.img_title_arrow);
        layoutNoData = findViewById(R.id.layout_no_data);
        emptyFile = findViewById(R.id.empty_file);

        if (mLocalGalleryEmptyHint != null) {
            emptyFile.setText(mLocalGalleryEmptyHint);
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new LocalMediaSelectAdapter(this, ConstantField.MediaType.MEDIA_IMAGE);
        adapter.setCheckStateVisible(false);
        adapter.setSelectListener(new LocalMediaSelectAdapter.ImageSelectListener() {
            @Override
            public void onSelectStateChange(LocalMediaUpItem item, int position, boolean isSelect) {
                // todo 修改
//                String path = item.getMediaPath();
//                Intent intent = new Intent();
//                intent.putExtra("path", path);
//                setResult(Activity.RESULT_OK, intent);
//                finish();
                Intent intent = LocalImageActivity.prepare()
                        .aspectX(ViewUtils.getScreenWidth(LocalGalleryActivity.this))
                        .aspectY(ViewUtils.getScreenHeight(LocalGalleryActivity.this))
                        .inputPath(item.getMediaPath())
                        .getSinkIntent(LocalGalleryActivity.this);
                if (intent != null) {
                    if (mScanMode != null) {
                        intent.putExtra(ConstantField.SCAN_MODE, mScanMode);
                    }
                    if (mFunctionBeanJson != null) {
                        intent.putExtra(ConstantField.FUNCTION_JSON, mFunctionBeanJson);
                    }
                    startActivityForResult(intent, ConstantField.RequestCode.LOCAL_IMAGE_CODE);
                }
            }

            @Override
            public void onItemLongClick(LocalMediaUpItem item, int position) {

            }
        });
        recyclerView.setAdapter(adapter);


        folderWindow = new FolderPopWindow(this, mImageUnit);
        folderWindow.setOnItemClickListener(new PictureAlbumDirectoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String folderName, List<LocalMediaUpItem> images) {
                tvTitleName.setText(folderName);
                folderWindow.dismiss();
                adapter.dataList.clear();
                if (images != null) {
                    adapter.dataList.addAll(images);
                }
                adapter.notifyDataSetChanged();
            }
        });

        //获取相簿列表
        LocalMediaUpSelectHelper localMediaUpSelectHelper = LocalMediaUpSelectHelper.getHelper();
        localMediaUpSelectHelper.init(LocalGalleryActivity.this, ConstantField.MediaType.MEDIA_IMAGE, mLocalAllImageText);
        localMediaUpSelectHelper.setCreateAll(true);
        localMediaUpSelectHelper.setIncludeGif(false);
        localMediaUpSelectHelper.setGetAlbumListListener(new LocalMediaUpSelectHelper.GetAlbumListListener() {
            @Override
            public void onGetAlbumList(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> totalFilesList) {
//                closeLoading();
                if (list.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    layoutNoData.setVisibility(View.VISIBLE);
                } else {
                    layoutNoData.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    mAlbumList = list;
                    adapter.dataList.clear();
                    adapter.dataList.addAll(totalFilesList);
                    if (mLocalAllImageText == null || TextUtils.isEmpty(mLocalAllImageText)) {
                        tvTitleName.setText(getResources().getString(R.string.all_pictures));
                    } else {
                        tvTitleName.setText(mLocalAllImageText);
                    }
                    imgTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_close);
                    mAlbumList.get(0).setChecked(true);
                    folderWindow.bindFolder(mAlbumList);
                }
            }
        });
//        showLoading(null);
        localMediaUpSelectHelper.execute(true);
    }

    private void initEvent() {
        back.setOnClickListener(this);
        layoutTitle.setOnClickListener(this);
        folderWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                imgTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_close);
            }
        });
    }

    private void handleResult(boolean isOk, String dataUuid) {
        Intent intent = new Intent();
        if (dataUuid != null) {
            intent.putExtra(ConstantField.DATA_UUID, dataUuid);
        }
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setStatusBarColor(Color.WHITE, this);
        initData();
        setContentView(R.layout.activity_local_gallery);
        initView();
        initEvent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ConstantField.RequestCode.LOCAL_IMAGE_CODE) {
            String dataUuid = null;
            if (data != null && data.hasExtra(ConstantField.DATA_UUID)) {
                dataUuid = data.getStringExtra(ConstantField.DATA_UUID);
            }
            if (resultCode == RESULT_OK) {
                handleResult(true, dataUuid);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            if (v.getId() == R.id.back) {
                finish();
            } else if (v.getId() == R.id.layout_title) {//切换相册
                if (folderWindow.isShowing()) {
                    folderWindow.dismiss();
                } else {
                    if (mAlbumList != null && mAlbumList.size() > 0) {
                        folderWindow.showAsDropDown(titleContainer);
                        imgTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_open);
                    }
                }
            }
        }
    }
}
