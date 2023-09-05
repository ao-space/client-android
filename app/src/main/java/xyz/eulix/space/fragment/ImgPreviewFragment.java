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

package xyz.eulix.space.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsFragment;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.event.ThumbEvent;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.presenter.ImgPreviewFragmentPresenter;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.event.TransferSizeEvent;
import xyz.eulix.space.transfer.event.TransferStateEvent;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.ui.FilePreviewActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.VideoPlayUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.SectorProgressView;
import xyz.eulix.space.view.photoview.PhotoView;
import xyz.eulix.space.view.photoview.helper.FingerDragHelper;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2022/1/11
 */
public class ImgPreviewFragment extends AbsFragment<ImgPreviewFragmentPresenter.IImgPreviewFragment, ImgPreviewFragmentPresenter> implements ImgPreviewFragmentPresenter.IImgPreviewFragment {
    private CustomizeFile mData;

    private LinearLayout layoutCache;
    private PhotoView imgPicture;
    private TextView tvFileName;
    private TextView tvFileSize;
    private ProgressBar progressBar;
    private RelativeLayout layoutImage;
    private LinearLayout layoutThumbDefault;
    private SectorProgressView sectorProgressView;
    private TextView tvShowOriginalImage;
    private ImageView imgFileType;
    private FingerDragHelper fingerDragHelper;
    private TextView tvNotSupport;
    private RelativeLayout layoutVideo;
    private ImageView imgVideo;
    private ImageView imgVideoPlay;

    private boolean isShowOriginalImageVisible = false;

    private boolean isSupport = false;

    private String from;

    //是否需要重置缩略图
    private boolean needResetThumb = false;

    public static ImgPreviewFragment newInstance(CustomizeFile data, String from) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("data", data);
        bundle.putString("from", from);
        ImgPreviewFragment fragment = new ImgPreviewFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void initRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.img_preview_layout, container, false);

    }

    @NotNull
    @Override
    public ImgPreviewFragmentPresenter createPresenter() {
        return new ImgPreviewFragmentPresenter();
    }

    @Override
    public void initData() {
        if (getArguments() != null) {
            mData = (CustomizeFile) getArguments().getSerializable("data");
            from = getArguments().getString("from");
        }
        if (TextUtils.isEmpty(from)) {
            from = TransferHelper.FROM_FILE;
        }
    }

    @Override
    public void initView(@Nullable View root) {
        layoutCache = root.findViewById(R.id.layout_cache);
        imgFileType = root.findViewById(R.id.img_sync_icon);
        tvFileName = root.findViewById(R.id.tv_file_name);
        tvFileSize = root.findViewById(R.id.tv_file_size);
        progressBar = root.findViewById(R.id.progress_bar);
        tvNotSupport = root.findViewById(R.id.tv_not_support);
        fingerDragHelper = root.findViewById(R.id.finger_drag_helper);
        imgPicture = root.findViewById(R.id.image_picture);
        layoutImage = root.findViewById(R.id.layout_image);
        layoutThumbDefault = root.findViewById(R.id.layout_thumb_default);
        sectorProgressView = root.findViewById(R.id.circle_progress_view);
        tvShowOriginalImage = root.findViewById(R.id.tv_show_original_image);
        layoutVideo = root.findViewById(R.id.layout_video);
        imgVideo = root.findViewById(R.id.img_video);
        imgVideoPlay = root.findViewById(R.id.img_video_play);

        EventBusUtil.register(this);
    }

    @Override
    public void initViewData() {
        if (mData != null && mData.getName() != null && mData.getName().contains(".")) {
            int typeIndex = mData.getName().lastIndexOf(".");
            String suffix = mData.getName().substring(typeIndex + 1);
            String mimeType = FileUtil.getMimeType(suffix);
            if (mimeType.contains("video")) {
                layoutImage.setVisibility(View.GONE);
                layoutVideo.setVisibility(View.VISIBLE);
            } else {
                layoutVideo.setVisibility(View.GONE);
                layoutImage.setVisibility(View.VISIBLE);
            }

            if (FileUtil.isImageSupportView(mimeType) || FileUtil.isVideoSupportView(mimeType)) {
                isSupport = true;
                presenter.checkFileExist(getActivity(), mData.getName(), mData.getId(), mData.getPath(), mData.getSize(), mData.getMd5(), mData.getMime(), false, from);
            } else {
                isSupport = false;
                progressBar.setVisibility(View.GONE);
                tvNotSupport.setVisibility(View.VISIBLE);
                layoutVideo.setVisibility(View.GONE);
                showCacheView();
            }
        }
    }

    @Override
    public void initEvent() {
        tvShowOriginalImage.setOnClickListener((view) -> {
            presenter.getOriginalImage(EulixSpaceApplication.getContext(), mData.getName(), mData.getId(), mData.getPath(), mData.getSize(), mData.getMd5(), from);
            String showOriginalText = getString(R.string.file_downloading) + " 0%";
            tvShowOriginalImage.setText(showOriginalText);
        });
        fingerDragHelper.setOnAlphaChangeListener(new FingerDragHelper.onAlphaChangedListener() {
            @Override
            public void onTranslationYChanged(MotionEvent event, float translationY) {
                float yAbs = Math.abs(translationY);
                float percent = yAbs / ViewUtils.getScreenHeight(getActivity());
                float number = 1.0f - percent;

                if (getActivity() != null && getActivity() instanceof FilePreviewActivity) {
                    ((FilePreviewActivity) getActivity()).onImageTranslationYChanged(number);
                }

                if (number < 1) {
                    tvShowOriginalImage.setVisibility(View.GONE);
                } else if (isShowOriginalImageVisible) {
                    tvShowOriginalImage.setVisibility(View.VISIBLE);
                }

                imgPicture.setScaleY(number);
                imgPicture.setScaleX(number);
            }
        });

        imgPicture.setOnClickListener(v -> {
            if (getActivity() != null && getActivity() instanceof FilePreviewActivity) {
                ((FilePreviewActivity) getActivity()).onImageListClick();
            }
        });

        imgVideo.setOnClickListener(v -> {
            if (getActivity() != null && getActivity() instanceof FilePreviewActivity) {
                ((FilePreviewActivity) getActivity()).onImageListClick();
            }
        });

        imgVideoPlay.setOnClickListener(v -> {
            //播放视频
            VideoPlayUtil.play(getActivity(), mData.getId(), mData.getName(), mData.getPath(), mData.getSize(), mData.getTimestamp(), null);
        });
    }

    @Override
    public void showPreview(String absolutePath, boolean isOriginalImage) {
        if (!isAdded()) {
            return;
        }
        layoutCache.setVisibility(View.GONE);
        layoutImage.setVisibility(View.VISIBLE);
        layoutThumbDefault.setVisibility(View.GONE);
        sectorProgressView.setVisibility(View.GONE);
        GlideUtil.load(absolutePath, imgPicture);
        if (isOriginalImage) {
            tvShowOriginalImage.setVisibility(View.GONE);
            isShowOriginalImageVisible = false;
        } else {
            String size = FormatUtil.formatSize(mData.getSize(), ConstantField.SizeUnit.FORMAT_1F);
            String showOriginalText = getString(R.string.file_origin_image) + " " + size;
            tvShowOriginalImage.setText(showOriginalText);
            tvShowOriginalImage.setVisibility(View.VISIBLE);
            isShowOriginalImageVisible = true;
        }
    }

    @Override
    public void showThumbImage(String localPath) {
        layoutCache.setVisibility(View.GONE);
        layoutImage.setVisibility(View.VISIBLE);
        sectorProgressView.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(localPath)) {
            GlideUtil.load(localPath, imgPicture);
        } else {
            String thumbPath = FileListUtil.getThumbPath(getActivity(), mData.getId());
            if (TextUtils.isEmpty(thumbPath)) {
                //不存在缩略图
                layoutThumbDefault.setVisibility(View.VISIBLE);

                needResetThumb = true;

                ThumbManager.getInstance().insertItem(mData.getId(), from);
                ThumbManager.getInstance().start();
            } else {
                GlideUtil.load(thumbPath, imgPicture);
            }
        }
    }

    @Override
    public void showCacheView() {
        layoutImage.setVisibility(View.GONE);
        layoutCache.setVisibility(View.VISIBLE);
        imgFileType.setImageResource(FileUtil.getMimeIcon(mData.getMime()));
        tvFileName.setText(mData.getName());
        tvFileSize.setText(FormatUtil.formatSize(mData.getSize(), ConstantField.SizeUnit.FORMAT_1F));
    }

    @Override
    public void showVideoPreview(String localPath) {
        if (!TextUtils.isEmpty(localPath)) {
            GlideUtil.load(localPath, imgVideo);
        } else {
            String thumbPath = FileListUtil.getThumbPath(getActivity(), mData.getId());
            if (!TextUtils.isEmpty(thumbPath)) {
                GlideUtil.load(thumbPath, imgVideo);
            } else {
                needResetThumb = true;

                ThumbManager.getInstance().insertItem(mData.getId(), from);
                ThumbManager.getInstance().start();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferSizeEvent event) {
        if (!isSupport || (event.transferType != TransferHelper.TYPE_CACHE && event.transferType != TransferHelper.TYPE_PREVIEW) || !event.keyName.equals(mData.getName())) {
            return;
        }
        Logger.d("receive TransferSizeEvent:" + event.keyName + ";size=" + event.currentSize + ";type=" + event.transferType);

        //刷新进度
        int progress = (int) (event.currentSize * 100 / mData.getSize());
        Logger.d("zfy", "set progress:" + progress);
        if (tvShowOriginalImage.getVisibility() == View.VISIBLE) {
            String showOriginalText = getString(R.string.file_downloading) + " " + progress + "%";
            tvShowOriginalImage.setText(showOriginalText);
        } else if (sectorProgressView.getVisibility() == View.VISIBLE) {
            sectorProgressView.setProgress(progress);
        } else {
            if (progress < 3) {
                //圆角进度进度太少时会变形，需要切割
                progressBar.setProgressDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.transfer_progress_dialog_bg_less));
            } else {
                progressBar.setProgressDrawable(Objects.requireNonNull(getActivity()).getDrawable(R.drawable.transfer_progress_dialog_bg));
            }
            progressBar.setProgress(progress);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferStateEvent event) {
        if (!isSupport || !event.keyName.equals(mData.getName())) {
            return;
        }
        if (event.transferType == TransferHelper.TYPE_CACHE) {
            if (event.state == TransferHelper.STATE_FINISH) {
                TransferItem item = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryByUniqueTag(event.uniqueTag, TransferHelper.TYPE_CACHE);
                if (progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setProgress(100);
                }
                if (mData.getMime().contains("video")) {
                    showVideoPreview(item.localPath + mData.getName());
                } else {
                    showPreview(item.localPath + mData.getName(), true);
                }
            } else if (event.state == TransferHelper.STATE_ERROR) {
                //缓存失败
                Logger.d("zfy", "文件缓存失败");
                showImageTextToast(R.drawable.toast_refuse, R.string.file_preview_failed);
                Objects.requireNonNull(getActivity()).finish();
            }
        } else if (event.transferType == TransferHelper.TYPE_DOWNLOAD) {
            if (event.state == TransferHelper.STATE_FINISH) {
                TransferItem item = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryByUniqueTag(event.uniqueTag, TransferHelper.TYPE_DOWNLOAD);
                showImageTextToast(R.drawable.toast_right, R.string.save_success);

                showPreview(item.localPath + "/" + item.keyName, true);

            } else if (event.state == TransferHelper.STATE_ERROR) {
                showImageTextToast(R.drawable.toast_refuse, R.string.download_failed);
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ThumbEvent event) {
        Logger.d("zfy", "onReceive ThumbEvent:" + event.uuid);
        if (needResetThumb && event.uuid.equals(mData.getId())) {
            refreshThumb(event.thumbPath);
        }
    }

    //刷新缩略图
    private void refreshThumb(String thumbPath) {
        if (mData == null) {
            return;
        }
        if (mData.getMime().contains("image")) {
            //图片
            showThumbImage(thumbPath);
        } else {
            //视频
            showVideoPreview(thumbPath);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBusUtil.unRegister(this);
    }
}
