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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.wxiwei.office.IOffice;
import com.wxiwei.office.system.beans.pagelist.APageListView;
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.ImgPreviewAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.event.DeleteFileEvent;
import xyz.eulix.space.event.MoveFileEvent;
import xyz.eulix.space.event.RenameFileEvent;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.presenter.FilePreviewPresenter;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.event.TransferSizeEvent;
import xyz.eulix.space.transfer.event.TransferStateEvent;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.SystemMediaUtils;
import xyz.eulix.space.util.share.ShareUtil;
import xyz.eulix.space.view.SectorProgressView;
import xyz.eulix.space.view.TitleBarWithSelect;
import xyz.eulix.space.view.dialog.EulixLoadingDialog;
import xyz.eulix.space.view.dialog.file.FileEditView;
import xyz.eulix.space.view.photoview.PhotoView;

/**
 * Author:      Zhu Fuyu
 * Description: 文件预览页面
 * History:     2021/9/2
 */
public class FilePreviewActivity extends AbsActivity<FilePreviewPresenter.IFilePreview, FilePreviewPresenter> implements FilePreviewPresenter.IFilePreview, View.OnClickListener, FileEditView.FileEditCallback {
    public static final String KEY_FILE_NAME = "name";
    public static final String KEY_FILE_PATH = "path";
    public static final String KEY_FILE_UUID = "uuid";
    public static final String KEY_FILE_SIZE = "size";
    public static final String KEY_FILE_MD5 = "md5";
    public static final String KEY_FILE_TIME = "time";
    public static final String KEY_SHOW_BOTTOM_EDIT = "bottom_edit_switch";
    public static final String KEY_IS_LOCAL = "is_local";
    public static final String KEY_FROM = "from";
    public static final String KEY_ALBUM_ID = "album_id";
    public static final String KEY_ALBUM_TYPE = "album_type";

    private String fileName;
    private String filePath;
    private String fileUuid;
    private long fileSize;
    private String fileMd5;
    private long fileTime;
    private boolean isShowBottomEdit = true;
    private boolean isLocal = false;

    private RelativeLayout layoutTitle;
    private TitleBarWithSelect titleBar;
    private LinearLayout layoutCache;
    private ImageView imgFileType;
    private PhotoView imgPicture;
    private TextView tvFileName, tvFileSize;
    private ProgressBar progressBar;
    private RelativeLayout layoutImage;
    private TextView tvShowOriginalImage;
    private TextView tvNotSupport;
    private RelativeLayout layoutRoot;

    private ViewPager2 imgViewPager;

    private String suffix;
    private String mimeType;

    private boolean hasCached = false;
    private boolean titlesShowFlag = true;  //是否显示标题栏、底部编辑栏

    private String mFileLocalAbsolutPath;

    private String mNewFileName;
    private String mNewFilePath;

    private ScrollView scrollViewTxt;
    private TextView tvTxt;

    private PDFView pdfView;
    private RelativeLayout layoutPreviewError;

    private SectorProgressView sectorProgressView;

    private LinearLayout layoutThumbDefalut;

    private RelativeLayout layoutOffice;


    private LinearLayout fileEditContainer;
    private FileEditView fileEditView;


    //是否支持预览
    private boolean isSupport = false;

    private EulixLoadingDialog shareLoadingDialog;

    private int mCurrentProgress = 0;

    private IOffice iOffice;

    private boolean isDestroyed = false;

    //图片列表数据
    private List<CustomizeFile> mImageListData = new ArrayList<>();

    public static List<CustomizeFile> tmpImageList;

    private int mSelectImagePosition = -1;

    private CustomizeFile mSelectedImageItem;

    private ImgPreviewAdapter imgPreviewAdapter;

    //来源
    private String from = "";

    private int mAlbumId;
    private int mAlbumType;

    public static void openLocal(Context context, String fileName, String localPath, String fileUuid, long fileSize) {
        Intent intent = new Intent(context, FilePreviewActivity.class);
        intent.putExtra(KEY_FILE_NAME, fileName);
        intent.putExtra(KEY_FILE_PATH, localPath);
        intent.putExtra(KEY_SHOW_BOTTOM_EDIT, false);
        intent.putExtra(KEY_IS_LOCAL, true);
        intent.putExtra(KEY_FILE_UUID, fileUuid);
        intent.putExtra(KEY_FILE_SIZE, fileSize);

        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(0, android.R.anim.fade_out);
        }
    }

    public static void openImgList(Context context, List<CustomizeFile> imageListData, int selectPosition, String from, int albumId, int albumType) {
        Intent intent = new Intent(context, FilePreviewActivity.class);
        intent.putExtra("hasImageList", true);
        intent.putExtra(KEY_FROM, from);
        intent.putExtra(KEY_ALBUM_ID, albumId);
        intent.putExtra(KEY_ALBUM_TYPE, albumType);
        tmpImageList = imageListData;
        intent.putExtra("selectPosition", selectPosition);

        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(0, android.R.anim.fade_out);
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_file_preview);

        layoutRoot = findViewById(R.id.layout_root);
        layoutTitle = findViewById(R.id.layout_title);
        titleBar = findViewById(R.id.title_bar);
        layoutCache = findViewById(R.id.layout_cache);
        imgFileType = findViewById(R.id.img_sync_icon);
        tvFileName = findViewById(R.id.tv_file_name);
        tvFileSize = findViewById(R.id.tv_file_size);
        progressBar = findViewById(R.id.progress_bar);
        imgPicture = findViewById(R.id.image_picture);
        layoutImage = findViewById(R.id.layout_image);
        layoutThumbDefalut = findViewById(R.id.layout_thumb_default);
        tvShowOriginalImage = findViewById(R.id.tv_show_original_image);
        tvNotSupport = findViewById(R.id.tv_not_support);
        scrollViewTxt = findViewById(R.id.scroll_view_txt);
        tvTxt = findViewById(R.id.tv_txt_content);
        pdfView = findViewById(R.id.pdf_view);
        layoutPreviewError = findViewById(R.id.layout_preview_error);
        fileEditContainer = findViewById(R.id.file_edit_container);

        sectorProgressView = findViewById(R.id.circle_progress_view);
        layoutOffice = findViewById(R.id.layout_office);
        imgViewPager = findViewById(R.id.img_view_pager);

        fileEditView = new FileEditView(this, null);
        fileEditView.registerCallback(this);

        if (mImageListData.size() > 0) {
            fileEditView.setFrom(from);
            //图片列表
            imgViewPager.setOffscreenPageLimit(1);
            imgPreviewAdapter = new ImgPreviewAdapter(this, from);
            imgViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                }

                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    mSelectedImageItem = imgPreviewAdapter.dataList.get(position);
                    mSelectImagePosition = position;
                    refreshImageListSelectedData();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged(state);
                }
            });
            imgViewPager.setAdapter(imgPreviewAdapter);
            imgPreviewAdapter.dataList.addAll(mImageListData);
            imgPreviewAdapter.notifyDataSetChanged();
            imgViewPager.setCurrentItem(mSelectImagePosition, false);
            imgViewPager.setVisibility(View.VISIBLE);
            imgViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        }
        EventBusUtil.register(this);
    }

    private void refreshImageListSelectedData() {
        if (mSelectedImageItem != null) {
            fileName = mSelectedImageItem.getName();
            fileUuid = mSelectedImageItem.getId();
            filePath = mSelectedImageItem.getPath();
            fileSize = mSelectedImageItem.getSize();
            fileMd5 = mSelectedImageItem.getMd5();
            fileTime = mSelectedImageItem.getTimestamp();
            if (fileName != null) {
                int typeIndex = fileName.lastIndexOf(".");
                suffix = fileName.substring(typeIndex + 1);
                mimeType = FileUtil.getMimeType(suffix);
            }
            titleBar.setTitle((mSelectImagePosition + 1) + " / " + mImageListData.size());
            if (titlesShowFlag && fileName != null) {
                fileEditContainer.setVisibility(View.VISIBLE);
            } else {
                layoutTitle.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        fileName = intent.getStringExtra(KEY_FILE_NAME);
        filePath = intent.getStringExtra(KEY_FILE_PATH);
        fileUuid = intent.getStringExtra(KEY_FILE_UUID);
        fileSize = intent.getLongExtra(KEY_FILE_SIZE, 0L);
        fileMd5 = intent.getStringExtra(KEY_FILE_MD5);
        fileTime = intent.getLongExtra(KEY_FILE_TIME, 0L);
        isShowBottomEdit = intent.getBooleanExtra(KEY_SHOW_BOTTOM_EDIT, true);
        isLocal = intent.getBooleanExtra(KEY_IS_LOCAL, false);
        mAlbumId = intent.getIntExtra(KEY_ALBUM_ID, -1);
        mAlbumType = intent.getIntExtra(KEY_ALBUM_TYPE, -1);

        from = intent.getStringExtra(KEY_FROM);
        if (TextUtils.isEmpty(from)) {
            from = TransferHelper.FROM_FILE;
        }

        boolean hasImageList = intent.getBooleanExtra("hasImageList", false);
        if (hasImageList && tmpImageList != null) {
            mImageListData.addAll(tmpImageList);
            tmpImageList.clear();
        } else {
            tmpImageList = null;
        }
        mSelectImagePosition = intent.getIntExtra("selectPosition", 0);

        if (mImageListData.size() > 0) {
            //图片列表
            isSupport = true;
        } else {
            //参数校验
            if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(filePath)) {
                finish();
                return;
            }
            int typeIndex = fileName.lastIndexOf(".");
            suffix = fileName.substring(typeIndex + 1);
            mimeType = FileUtil.getMimeType(suffix);
            if (FileUtil.checkIsSupportPreview(mimeType)) {
                isSupport = true;
            }
        }
    }

    @Override
    public void initViewData() {
        if (isSupport) {
            if (mImageListData.size() > 0) {
                //图片由fragment内部加载
            } else {
                if (FileUtil.isOfficeFile(mimeType)) {
                    iOffice = new IOffice() {
                        @Override
                        public Activity getActivity() {
                            if (isDestroyed) {
                                return null;
                            } else {
                                return FilePreviewActivity.this;
                            }
                        }

                        @Override
                        public String getAppName() {
                            return "傲空间";
                        }

                        @Override
                        public File getTemporaryDirectory() {
                            File file = new File(ConstantField.SDCARD_ROOT_PATH + File.separator + ConstantField.APP_PATH);
                            return file;
                        }

                        @Override
                        public void openFileFinish() {
                            //加载成功
                            closeLoading();
                            Logger.d("zfy", "openFileFinish");

                            if (isShowBottomEdit) {
                                fileEditContainer.setVisibility(View.VISIBLE);
                            }
                            if (isLocal) {
                                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) pdfView.getLayoutParams();
                                layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.dp_15);
                                pdfView.setLayoutParams(layoutParams);
                            }
                            layoutCache.setVisibility(View.GONE);
                            layoutOffice.setVisibility(View.VISIBLE);
                            layoutOffice.removeAllViews();

                            View view = getOfficeView();
                            layoutOffice.addView(view,
                                    new RelativeLayout.LayoutParams(
                                            RelativeLayout.LayoutParams.MATCH_PARENT,
                                            RelativeLayout.LayoutParams.MATCH_PARENT
                                    ));
                            if (view instanceof APageListView) {
                                //设置缩放
                                ((APageListView) view).setFitSize(1);
                                int currentPageNumber = ((APageListView) view).getCurrentPageNumber();
                                int totalPageCount = ((APageListView) view).getPageCount();
                                Logger.d("zfy", currentPageNumber + "/" + totalPageCount);
                            }
                        }

                        @Override
                        public void error(int errorCode) {
                            //加载失败
                            Logger.d("zfy", "load error:" + errorCode);
                            closeLoading();
                            showNotSupportPreview();
                        }

                        @Override
                        public boolean isShowProgressBar() {
                            //不显示加载等待框
                            return false;
                        }

                        @Override
                        public boolean onEventMethod(View v, MotionEvent e1, MotionEvent e2, float xValue, float yValue, byte eventMethodType) {
//                        Logger.d("zfy","onEventMethod eventMethodType:"+eventMethodType);
                            if (eventMethodType == IPageListViewListener.ON_FLING) {
                                View view = getOfficeView();

                                if (view instanceof APageListView) {
                                    int currentPageNumber = ((APageListView) view).getCurrentPageNumber();
                                    int totalPageCount = ((APageListView) view).getPageCount();
                                    Logger.d("zfy", currentPageNumber + "/" + totalPageCount);
                                }
                            }
                            return super.onEventMethod(v, e1, e2, xValue, yValue, eventMethodType);
                        }

                        @Override
                        public byte getPageListViewMovingPosition() {
                            //设置滑动方向
                            return IPageListViewListener.Moving_Vertical;
                        }
                    };
                }
                presenter.checkFileExist(this, fileName, fileUuid, filePath, fileSize, fileMd5, mimeType, isLocal, from);
            }
        } else {
            progressBar.setVisibility(View.GONE);
            tvNotSupport.setVisibility(View.VISIBLE);
            showCacheView();
        }

        fileEditView.showFileDialog(1);

    }

    @Override
    public void initEvent() {
        imgPicture.setOnClickListener(this);
        tvShowOriginalImage.setOnClickListener(this);
    }

    @Override
    public void showCacheView() {
        layoutImage.setVisibility(View.GONE);
        fileEditContainer.setVisibility(View.GONE);
        layoutCache.setVisibility(View.VISIBLE);
        imgFileType.setImageResource(FileUtil.getMimeIcon(mimeType));
        tvFileName.setText(fileName);
        tvFileSize.setText(FormatUtil.formatSize(fileSize, ConstantField.SizeUnit.FORMAT_1F));

    }

    @Override
    public void showThumbImage() {
        layoutCache.setVisibility(View.GONE);
        layoutImage.setVisibility(View.VISIBLE);
        sectorProgressView.setVisibility(View.VISIBLE);
        if (mImageListData.size() > 0) {
            titleBar.setTitle((mSelectImagePosition + 1) + " / " + mImageListData.size());
        } else {
            titleBar.setTitle(fileName, TitleBarWithSelect.TITLE_TYPE_FILENAME);
        }
        if (!isLocal) {
            fileEditContainer.setVisibility(View.VISIBLE);
        }
        String thumbPath = FileListUtil.getThumbPath(this, fileUuid);
        if (TextUtils.isEmpty(thumbPath)) {
            //不存在缩略图
            layoutThumbDefalut.setVisibility(View.VISIBLE);
        } else {
            GlideUtil.load(thumbPath, imgPicture);
        }
    }

    @Override
    public void showPreview(String absolutePath, boolean isOriginalImage) {
        if (mImageListData.size() > 0) {
            titleBar.setTitle((mSelectImagePosition + 1) + " / " + mImageListData.size());
        } else {
            titleBar.setTitle(fileName, TitleBarWithSelect.TITLE_TYPE_FILENAME);
        }
        hasCached = true;
        this.mFileLocalAbsolutPath = absolutePath;
        if (mimeType.contains("image")) {
            //图片直接加载
            layoutCache.setVisibility(View.GONE);
            layoutImage.setVisibility(View.VISIBLE);
            layoutTitle.setVisibility(View.VISIBLE);
            if (isShowBottomEdit) {
                fileEditContainer.setVisibility(View.VISIBLE);
            }
            layoutThumbDefalut.setVisibility(View.GONE);
            sectorProgressView.setVisibility(View.GONE);
            GlideUtil.loadReplace(absolutePath, imgPicture);
            if (isOriginalImage) {
                tvShowOriginalImage.setVisibility(View.GONE);
            } else {
                String size = FormatUtil.formatSize(fileSize, ConstantField.SizeUnit.FORMAT_1F);
                String showOriginalText = getString(R.string.file_origin_image) + " " + size;
                tvShowOriginalImage.setText(showOriginalText);
                tvShowOriginalImage.setVisibility(View.VISIBLE);
            }
        } else if (mimeType.contains("video")) {
            ExoPlayerActivity.startLocal(FilePreviewActivity.this, absolutePath, fileName);
            finish();
        } else if (mimeType.contains("text")) {
            //展示txt文本
            if (isShowBottomEdit) {
                fileEditContainer.setVisibility(View.VISIBLE);
            }
            if (isLocal) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) scrollViewTxt.getLayoutParams();
                layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.dp_15);
                scrollViewTxt.setLayoutParams(layoutParams);
            }
            presenter.parseTxtFile(absolutePath, new ResultCallbackObj() {
                @Override
                public void onResult(boolean result, Object extraObj) {
                    if (scrollViewTxt.getVisibility() != View.VISIBLE) {
                        layoutCache.setVisibility(View.GONE);
                        scrollViewTxt.setVisibility(View.VISIBLE);
                    }
                    if (tvTxt != null && extraObj != null) {
                        String txtContent = (String) extraObj;
                        tvTxt.append(txtContent);
                    }
                }

                @Override
                public void onError(String msg) {
                    layoutCache.setVisibility(View.GONE);
                    layoutPreviewError.setVisibility(View.VISIBLE);
                }
            });

        } else if (mimeType.contains("pdf")) {
            if (isShowBottomEdit) {
                fileEditContainer.setVisibility(View.VISIBLE);
            }
            if (isLocal) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) pdfView.getLayoutParams();
                layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.dp_15);
                pdfView.setLayoutParams(layoutParams);
            }
            layoutCache.setVisibility(View.GONE);
            pdfView.setVisibility(View.VISIBLE);
            File pdfFile = new File(absolutePath);
            try {
                showLoading("");
                pdfView.fromFile(pdfFile).onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Logger.d("zfy", "load pdf onError");
                        showNotSupportPreview();
                    }
                }).onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        Logger.d("zfy", "load pdf success.pages:" + nbPages);
                        closeLoading();
                    }
                }).load();
            } catch (Exception e) {
                downloadFilePreviewFailed();
            } catch (Error error) {
                downloadFilePreviewFailed();
            }
        } else if (FileUtil.isOfficeFile(mimeType)) {
            if (iOffice != null) {
                Logger.d("zfy", "open office file:" + absolutePath);
                showLoading("");
                try {
                    iOffice.openFile(absolutePath);
                } catch (Exception e) {
                    Logger.d("zfy", "open office exception");
                    e.printStackTrace();
                    closeLoading();
                    showNotSupportPreview();
                }
            }

        } else {
            //其他文件调用系统打开
            layoutImage.setVisibility(View.GONE);
            fileEditContainer.setVisibility(View.GONE);
            layoutCache.setVisibility(View.VISIBLE);
            imgFileType.setImageResource(FileUtil.getMimeIcon(mimeType));
            tvFileName.setText(fileName);
            tvFileSize.setText(FormatUtil.formatSize(fileSize, ConstantField.SizeUnit.FORMAT_1F));
            progressBar.setProgress(100);
            SystemMediaUtils.openMediaFile(getApplicationContext(), absolutePath);
        }
    }

    private void showNotSupportPreview() {
        closeLoading();
        progressBar.setVisibility(View.GONE);
        pdfView.setVisibility(View.GONE);
        layoutOffice.setVisibility(View.GONE);
        tvNotSupport.setVisibility(View.VISIBLE);
        showCacheView();
        titleBar.setTitle("");
        fileEditContainer.setVisibility(View.GONE);
    }

    //调用分享工具进行分享
    @Override
    public void callShareUtil(String fileAbsolutePath) {
        Logger.d("zfy", fileAbsolutePath);
        if (shareLoadingDialog != null && shareLoadingDialog.isShowing()) {
            shareLoadingDialog.dismiss();
        }
        ShareUtil.shareFile(this, fileAbsolutePath);
    }

    @Override
    public void downloadCompressedImageFailed() {
        showImageTextToast(R.drawable.toast_refuse, R.string.get_compressed_failed);
        new Handler(getMainLooper()).postDelayed(() -> finish(), 2 * 1000);
    }

    @Override
    public void onLikeStateChange(Boolean result, boolean isLike, String uuid) {
        if (result == null || !result) {
            if (result == null) {
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    showServerExceptionToast();
                } else {
                    new Handler(Looper.getMainLooper()).post(this::showServerExceptionToast);
                }
            }
            return;
        }
    }

    @Override
    public void downloadFilePreviewFailed() {
        closeLoading();
        if (mImageListData.size() > 0) {
            titleBar.setTitle((mSelectImagePosition + 1) + " / " + mImageListData.size());
        } else {
            titleBar.setTitle(fileName, TitleBarWithSelect.TITLE_TYPE_FILENAME);
        }
        layoutCache.setVisibility(View.GONE);
        if (isShowBottomEdit) {
            fileEditContainer.setVisibility(View.VISIBLE);
        }
        layoutPreviewError.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferSizeEvent event) {
        if ((event.transferType != TransferHelper.TYPE_CACHE && event.transferType != TransferHelper.TYPE_PREVIEW) || !event.keyName.equals(fileName)) {
            return;
        }
        Logger.d("receive TransferSizeEvent:" + event.keyName + ";size=" + event.currentSize + ";type=" + event.transferType);

        if (mImageListData.size() > 0) {
            return;
        }
        //刷新进度
        int progress = (int) (event.currentSize * 100 / fileSize);
        Logger.d("zfy", "set progress:" + progress);
        if (tvShowOriginalImage.getVisibility() == View.VISIBLE) {
            String showOriginalText = getString(R.string.file_downloading) + " " + progress + "%";
            tvShowOriginalImage.setText(showOriginalText);
        } else if (sectorProgressView.getVisibility() == View.VISIBLE) {
            sectorProgressView.setProgress(progress);
        } else {
            if (progress < 3) {
                //圆角进度进度太少时会变形，需要切割
                progressBar.setProgressDrawable(getDrawable(R.drawable.transfer_progress_dialog_bg_less));
            } else {
                progressBar.setProgressDrawable(getDrawable(R.drawable.transfer_progress_dialog_bg));
            }
            progressBar.setProgress(progress);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferStateEvent event) {
        if (!event.keyName.equals(fileName)) {
            return;
        }
        if (mImageListData.size() > 0 && (shareLoadingDialog == null || !shareLoadingDialog.isShowing())) {
            return;
        }
        if (event.transferType == TransferHelper.TYPE_CACHE) {
            if (event.state == TransferHelper.STATE_FINISH) {
                TransferItem item = TransferDBManager.getInstance(getApplicationContext()).queryByUniqueTag(event.uniqueTag, TransferHelper.TYPE_CACHE);
                if (progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setProgress(100);
                }
                String cacheFilePath = FileListUtil.getCacheFilePath(this, fileName);
                if (shareLoadingDialog != null && shareLoadingDialog.isShowing()) {
//                    callShareUtil(item.localPath + "/" + item.keyName);
                    callShareUtil(cacheFilePath);
                } else {
//                    showPreview(item.localPath + fileName, true);
                    showPreview(cacheFilePath, true);
                }
            } else if (event.state == TransferHelper.STATE_ERROR) {
                //缓存失败
                Logger.d("zfy", "文件缓存失败");
                showImageTextToast(R.drawable.toast_refuse, R.string.file_preview_failed);
                finish();
            }
        } else if (event.transferType == TransferHelper.TYPE_DOWNLOAD) {
            if (event.state == TransferHelper.STATE_FINISH) {
                TransferItem item = TransferDBManager.getInstance(getApplicationContext()).queryByUniqueTag(event.uniqueTag, TransferHelper.TYPE_DOWNLOAD);
                showImageTextToast(R.drawable.toast_right, R.string.save_success);

                showPreview(item.localPath + "/" + item.keyName, true);

            } else if (event.state == TransferHelper.STATE_ERROR) {
                showImageTextToast(R.drawable.toast_refuse, R.string.download_failed);
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeleteFileEvent event) {
        if (!event.uuidList.isEmpty() && event.uuidList.contains(fileUuid) && imgPreviewAdapter != null && mImageListData.size()>1){
            for (int i=0;i<imgPreviewAdapter.dataList.size();i++) {
                if (event.uuidList.contains(imgPreviewAdapter.dataList.get(i).getId())){
                    if (mSelectImagePosition<mImageListData.size()-1){
                        mImageListData.remove(i);
                        imgPreviewAdapter.dataList.clear();
                        imgPreviewAdapter.dataList.addAll(mImageListData);
                        imgViewPager.setAdapter(null);
                        imgViewPager.setAdapter(imgPreviewAdapter);
                        imgPreviewAdapter.notifyDataSetChanged();
                        imgViewPager.setCurrentItem(mSelectImagePosition,false);
                    } else {
                        mImageListData.remove(i);
                        imgPreviewAdapter.dataList.remove(i);
                        imgPreviewAdapter.notifyItemRemoved(i);
                    }
                    break;
                }
            }
        } else {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RenameFileEvent event) {
        Logger.d("zfy", "#FilePreviewActivity RenameFileEvent");
        //成功重命名，刷新文件
        if (!event.getUuid().equals(fileUuid)) {
            return;
        }
        mNewFileName = event.getFileName();
        if (mImageListData.size() > 0) {
            for (int i = 0; i < mImageListData.size(); i++) {
                if (mImageListData.get(i).getId().equals(event.getUuid())) {
                    mImageListData.get(i).setName(event.getFileName());
                    break;
                }
            }
            titleBar.setTitle((mSelectImagePosition + 1) + " / " + mImageListData.size());
        } else {
            titleBar.setTitle(mNewFileName, TitleBarWithSelect.TITLE_TYPE_FILENAME);
        }
        fileName = mNewFileName;
        mNewFileName = null;
    }

    @NotNull
    @Override
    public FilePreviewPresenter createPresenter() {
        return new FilePreviewPresenter();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_picture:
            case R.id.img_view_pager:
                changeTitlesShowState();
                break;
            case R.id.tv_show_original_image:
                presenter.getOriginalImage(getApplicationContext(), fileName, fileUuid, filePath, fileSize, fileMd5, from);
                String showOriginalText = getString(R.string.file_downloading) + " 0%";
                tvShowOriginalImage.setText(showOriginalText);
                break;
            default:
                break;
        }
    }

    @Override
    public void fileNotExist() {
        showImageTextToast(R.drawable.toast_refuse, R.string.file_not_exist);
        finish();
    }

    @Override
    public void exitPreview() {
        finish();
    }

    private void changeTitlesShowState() {

        titlesShowFlag = !titlesShowFlag;
        if (titlesShowFlag) {
            layoutTitle.setVisibility(View.VISIBLE);
            if (isShowBottomEdit) {
                fileEditContainer.setVisibility(View.VISIBLE);
            }
//            quitFullScreen();
        } else {
            layoutTitle.setVisibility(View.GONE);
            fileEditContainer.setVisibility(View.GONE);
//            setFullScreen();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDestroyed) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        if (iOffice != null) {
            iOffice.dispose();
        }
        presenter.setActivityDestroy();
        EventBusUtil.unRegister(this);
        tmpImageList = null;
    }

    /**
     * 上传文件所用的path
     *
     * @return
     */
    private String generatePath() {
        StringBuilder pathBuilder = new StringBuilder("/");
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        ArrayStack<UUID> uuidStack = DataUtil.getUuidStack();
        if (uuidStack != null) {
            int size = uuidStack.size();
            if (size > 1) {
                for (int i = 1; i < size; i++) {
                    UUID uuid = uuidStack.get(i);
                    String title = "";
                    if (uuid != null && uuidTitleMap != null && uuidTitleMap.containsKey(uuid.toString())) {
                        title = uuidTitleMap.get(uuid.toString());
                    }
                    pathBuilder.append(title);
                    pathBuilder.append("/");
                }
            }
        }
        return pathBuilder.toString();
    }

    //图片列表上下滑动回调
    public void onImageTranslationYChanged(float scale) {
        imgViewPager.setUserInputEnabled(scale >= 1);
        if (scale < 1) {
            layoutTitle.setVisibility(View.GONE);
            fileEditContainer.setVisibility(View.GONE);
        } else if (titlesShowFlag) {
            layoutTitle.setVisibility(View.VISIBLE);
            if (isShowBottomEdit) {
                fileEditContainer.setVisibility(View.VISIBLE);
            }

        }
        layoutRoot.setAlpha(scale);
    }

    public void onImageListClick() {
        changeTitlesShowState();
    }

    private void setFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void quitFullScreen() {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setAttributes(attrs);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public void handleRefresh(boolean isSuccess, String serviceFunction) {
        if (isSuccess) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.MOVE_FILE:
                    //移动文件，刷新路径名
                    filePath = mNewFilePath;
                    mNewFilePath = null;
                    EventBusUtil.post(new MoveFileEvent(fileUuid, fileName));
                    finish();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public List<CustomizeFile> getSelectFiles() {
        ArrayList<CustomizeFile> photoCustomFileList = new ArrayList<>();
        CustomizeFile customizeFile = new CustomizeFile();
        customizeFile.setId(fileUuid);
        customizeFile.setName(fileName);
        customizeFile.setPath(filePath);
        customizeFile.setSize(fileSize);
        customizeFile.setMd5(fileMd5);
        customizeFile.setMime(mimeType);
        customizeFile.setTimestamp(fileTime);
        photoCustomFileList.add(customizeFile);
        return photoCustomFileList;
    }

    @Override
    public void fileDialog(View view, boolean isShow) {
        if (fileEditContainer != null) {
            fileEditContainer.removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            fileEditContainer.addView(view, layoutParams);
        }
    }

    @Override
    public ArrayStack<UUID> handleCurrentUUIDStack() {
        return null;
    }
}