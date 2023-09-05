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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.files.FileAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bridge.FileSearchBridge;
import xyz.eulix.space.event.DeleteFileEvent;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.presenter.FileSearchPresenter;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.event.TransferStateEvent;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.OperationUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.dialog.file.FileEditView;
import xyz.eulix.space.view.rv.FooterView;
import xyz.eulix.space.view.rv.HeaderFooterWrapper;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/17 13:45
 */
public class FileSearchActivity extends AbsActivity<FileSearchPresenter.IFileSearch, FileSearchPresenter> implements FileSearchPresenter.IFileSearch
        , FileAdapter.OnItemClickListener, FileEditView.FileEditCallback, FileEditView.FileEditPluralCallback, View.OnClickListener, FileSearchBridge.FileSearchSinkCallback {
    private static final String TAG = FileSearchActivity.class.getSimpleName();
    private Button cancel, select;
    private Button refreshNow;
    private ImageButton back, fileSearch, fileSearchClear;
    private TextView title, fileSelect, fileSearchResult;
    private EditText fileSearchInput;
    private FrameLayout fileSubViewContainer;
    private RecyclerView fileList;
    private ImageView fileAllImage;
    private ImageView fileImageImage;
    private ImageView fileVideoImage;
    private ImageView fileDocumentImage;
    private ImageView fileOtherImage;
    private TextView fileAllText;
    private TextView fileImageText;
    private TextView fileVideoText;
    private TextView fileDocumentText;
    private TextView fileOtherText;
    private Button fileAllButton;
    private Button fileImageButton;
    private Button fileVideoButton;
    private Button fileDocumentButton;
    private Button fileOtherButton;
    private HorizontalScrollView fileSearchCategoryTab;
    private LinearLayout fileSearchCategoryContainer;
    private LinearLayout fileAllContainer;
    private LinearLayout fileImageContainer;
    private LinearLayout fileVideoContainer;
    private LinearLayout fileDocumentContainer;
    private LinearLayout fileOtherContainer;
    private LinearLayout fileSearchContentContainer;
    private LinearLayout selectContainer, searchContainer, inputContainer, fileEditContainer;
    private LinearLayout networkExceptionContainer, status404Container, emptySearchContainer, emptyFileContainer;
    private RelativeLayout exceptionContainer;
    private SwipeRefreshLayout swipeRefreshContainer;
    private FileEditView fileEditView;
    private FileAdapter mAdapter;
    private HeaderFooterWrapper headerFooterWrapper;
    private FooterView footer;
    private List<CustomizeFile> customizeFiles;
    private List<String> selectIds;
    private FileSearchHandler mHandler;
    private int mPage = 1;
    private int mTotalPage = 1;
    private int maxChildCount = 7;
    private String mSearchContent;
    private String mCategory;
    private int mCategoryIndex = 0;
    private boolean mSearched;
    private int searchResultNumber;
    // 上拉加载使能，本地加载时失效，网络加载到来之后生效
    private boolean isLoadingEnable = false;
    // 本地加载是否存在，不存在则展示网络错误，刷新时强制为true
    private boolean isLocalEmpty = true;
    private List<UUID> newFolderUUIDList;
    private InputMethodManager inputMethodManager;
    private FileSearchBridge mBridge;
    private Boolean isWindowFocus = null;

    private HeaderFooterWrapper.ILoadMore loadMore = new HeaderFooterWrapper.ILoadMore() {
        @Override
        public void loadMore() {
            if (getFileDepth() < 0) {
                if (presenter != null && mPage < mTotalPage) {
                    presenter.searchFile(mSearchContent, (mPage + 1), mCategory);
                }
            } else if (isLoadingEnable && fileEditView != null && mPage < mTotalPage) {
                fileEditView.getEulixSpaceStorage((mPage + 1));
            }
        }
    };

    private static class FileSearchHandler extends Handler {
        private WeakReference<FileSearchActivity> fileSearchActivityWeakReference;

        public FileSearchHandler(FileSearchActivity activity) {
            fileSearchActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            FileSearchActivity activity = fileSearchActivityWeakReference.get();
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
        setContentView(R.layout.activity_file_search);

        title = findViewById(R.id.title);
        selectContainer = findViewById(R.id.select_container);
        cancel = findViewById(R.id.cancel);
        fileSelect = findViewById(R.id.file_select);
        select = findViewById(R.id.select);

        searchContainer = findViewById(R.id.search_container);
        back = findViewById(R.id.back);
        inputContainer = findViewById(R.id.input_container);
        fileSearch = findViewById(R.id.file_search);
        fileSearchInput = findViewById(R.id.file_search_input);
        fileSearchClear = findViewById(R.id.file_search_clear);

        fileSearchCategoryContainer = findViewById(R.id.file_search_category_container);
        fileAllContainer = findViewById(R.id.file_all_container);
        fileAllImage = findViewById(R.id.file_all_image);
        fileAllText = findViewById(R.id.file_all_text);
        fileImageContainer = findViewById(R.id.file_image_container);
        fileImageImage = findViewById(R.id.file_image_image);
        fileImageText = findViewById(R.id.file_image_text);
        fileVideoContainer = findViewById(R.id.file_video_container);
        fileVideoImage = findViewById(R.id.file_video_image);
        fileVideoText = findViewById(R.id.file_video_text);
        fileDocumentContainer = findViewById(R.id.file_document_container);
        fileDocumentImage = findViewById(R.id.file_document_image);
        fileDocumentText = findViewById(R.id.file_document_text);
        fileOtherContainer = findViewById(R.id.file_other_container);
        fileOtherImage = findViewById(R.id.file_other_image);
        fileOtherText = findViewById(R.id.file_other_text);

        fileSearchContentContainer = findViewById(R.id.file_search_content_container);
        fileSearchCategoryTab = findViewById(R.id.file_search_category_tab);
        fileAllButton = findViewById(R.id.file_all_button);
        fileImageButton = findViewById(R.id.file_image_button);
        fileVideoButton = findViewById(R.id.file_video_button);
        fileDocumentButton = findViewById(R.id.file_document_button);
        fileOtherButton = findViewById(R.id.file_other_button);

        fileSearchResult = findViewById(R.id.file_search_result);
        swipeRefreshContainer = findViewById(R.id.swipe_refresh_container);
        exceptionContainer = findViewById(R.id.exception_container);
        networkExceptionContainer = findViewById(R.id.network_exception_container);
        refreshNow = findViewById(R.id.refresh_now);
        status404Container = findViewById(R.id.status_404_container);
        emptySearchContainer = findViewById(R.id.empty_search_container);
        emptyFileContainer = findViewById(R.id.empty_file_container);
        fileSubViewContainer = findViewById(R.id.file_sub_view_container);
        fileList = findViewById(R.id.file_list);
        fileEditContainer = findViewById(R.id.file_edit_container);

        EventBusUtil.register(this);
    }

    @Override
    public void initData() {
        StatusBarUtil.setStatusBarColor(Color.WHITE, this);
        mHandler = new FileSearchHandler(this);
        customizeFiles = new ArrayList<>();
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mBridge = FileSearchBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        setCategory(ConstantField.FragmentIndex.FILE_ALL);
        switchSearchVisibility(false);
        maxChildCount = Math.max((int) (Math.ceil((ViewUtils.getScreenHeight(this) - ViewUtils.getStatusBarHeight(this))
                * 1.0 / getResources().getDimensionPixelSize(R.dimen.dp_80))), maxChildCount);
        fileEditView = new FileEditView(this, null);
    }

    @Override
    public void initEvent() {
        UUID fileUuid = null;
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ConstantField.FILE_UUID)) {
            String fileUuidValue = intent.getStringExtra(ConstantField.FILE_UUID);
            if (fileUuidValue != null) {
                try {
                    fileUuid = UUID.fromString(fileUuidValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (presenter != null) {
            presenter.setmUuid(fileUuid);
        }
        fileEditView.registerCallback(this);
        fileEditView.registerPluralCallback(this);
        cancel.setOnClickListener(this);
        select.setOnClickListener(this);
        back.setOnClickListener(this);
        fileSearch.setOnClickListener(this);
        fileSearchClear.setOnClickListener(this);
        fileAllContainer.setOnClickListener(this);
        fileImageContainer.setOnClickListener(this);
        fileVideoContainer.setOnClickListener(this);
        fileDocumentContainer.setOnClickListener(this);
        fileOtherContainer.setOnClickListener(this);
        fileAllButton.setOnClickListener(this);
        fileImageButton.setOnClickListener(this);
        fileVideoButton.setOnClickListener(this);
        fileDocumentButton.setOnClickListener(this);
        fileOtherButton.setOnClickListener(this);
        refreshNow.setOnClickListener(this);

        fileSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null || s.length() <= 0) {
                    fileSearchClear.setVisibility(View.GONE);
                } else {
                    fileSearchClear.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
        fileSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                forceHideSoftInput();
                searchFile();
            }
            return false;
        });
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setOnRefreshListener(this::refresh);
        }
        fileSearchInput.requestFocus();
        if (isWindowFocus == null) {
            isWindowFocus = false;
        } else {
            isWindowFocus = true;
            if (mHandler == null) {
                showSoftInput(fileSearchInput, true);
            } else {
                mHandler.postDelayed(() -> showSoftInput(fileSearchInput, false), 200);
            }
        }
    }

    private void switchSearchVisibility(boolean isSearched) {
        mSearched = isSearched;
        fileSearchCategoryContainer.setVisibility(isSearched ? View.GONE : View.VISIBLE);
        fileSearchContentContainer.setVisibility(isSearched ? View.VISIBLE : View.GONE);
    }

    private void setCategory(int category) {
        if (mCategoryIndex != category) {
            mCategoryIndex = category;
            switch (category) {
                case ConstantField.FragmentIndex.FILE_ALL:
                    mCategory = null;
                    fileSearchInput.setHint(R.string.search_all_files);
                    setCategoryPattern(fileAllContainer, fileAllImage, fileAllText, ConstantField.FragmentIndex.FILE_ALL, true);
                    setCategoryPattern(fileImageContainer, fileImageImage, fileImageText, ConstantField.FragmentIndex.FILE_IMAGE, false);
                    setCategoryPattern(fileVideoContainer, fileVideoImage, fileVideoText, ConstantField.FragmentIndex.FILE_VIDEO, false);
                    setCategoryPattern(fileDocumentContainer, fileDocumentImage, fileDocumentText, ConstantField.FragmentIndex.FILE_DOCUMENT, false);
                    setCategoryPattern(fileOtherContainer, fileOtherImage, fileOtherText, ConstantField.FragmentIndex.FILE_OTHER, false);
                    break;
                case ConstantField.FragmentIndex.FILE_IMAGE:
                    mCategory = ConstantField.Category.PICTURE;
                    fileSearchInput.setHint(R.string.search_image);
                    setCategoryPattern(fileAllContainer, fileAllImage, fileAllText, ConstantField.FragmentIndex.FILE_ALL, false);
                    setCategoryPattern(fileImageContainer, fileImageImage, fileImageText, ConstantField.FragmentIndex.FILE_IMAGE, true);
                    setCategoryPattern(fileVideoContainer, fileVideoImage, fileVideoText, ConstantField.FragmentIndex.FILE_VIDEO, false);
                    setCategoryPattern(fileDocumentContainer, fileDocumentImage, fileDocumentText, ConstantField.FragmentIndex.FILE_DOCUMENT, false);
                    setCategoryPattern(fileOtherContainer, fileOtherImage, fileOtherText, ConstantField.FragmentIndex.FILE_OTHER, false);
                    break;
                case ConstantField.FragmentIndex.FILE_VIDEO:
                    mCategory = ConstantField.Category.VIDEO;
                    fileSearchInput.setHint(R.string.search_video);
                    setCategoryPattern(fileAllContainer, fileAllImage, fileAllText, ConstantField.FragmentIndex.FILE_ALL, false);
                    setCategoryPattern(fileImageContainer, fileImageImage, fileImageText, ConstantField.FragmentIndex.FILE_IMAGE, false);
                    setCategoryPattern(fileVideoContainer, fileVideoImage, fileVideoText, ConstantField.FragmentIndex.FILE_VIDEO, true);
                    setCategoryPattern(fileDocumentContainer, fileDocumentImage, fileDocumentText, ConstantField.FragmentIndex.FILE_DOCUMENT, false);
                    setCategoryPattern(fileOtherContainer, fileOtherImage, fileOtherText, ConstantField.FragmentIndex.FILE_OTHER, false);
                    break;
                case ConstantField.FragmentIndex.FILE_DOCUMENT:
                    mCategory = ConstantField.Category.DOCUMENT;
                    fileSearchInput.setHint(R.string.search_document);
                    setCategoryPattern(fileAllContainer, fileAllImage, fileAllText, ConstantField.FragmentIndex.FILE_ALL, false);
                    setCategoryPattern(fileImageContainer, fileImageImage, fileImageText, ConstantField.FragmentIndex.FILE_IMAGE, false);
                    setCategoryPattern(fileVideoContainer, fileVideoImage, fileVideoText, ConstantField.FragmentIndex.FILE_VIDEO, false);
                    setCategoryPattern(fileDocumentContainer, fileDocumentImage, fileDocumentText, ConstantField.FragmentIndex.FILE_DOCUMENT, true);
                    setCategoryPattern(fileOtherContainer, fileOtherImage, fileOtherText, ConstantField.FragmentIndex.FILE_OTHER, false);
                    break;
                case ConstantField.FragmentIndex.FILE_OTHER:
                    mCategory = ConstantField.Category.OTHER;
                    fileSearchInput.setHint(R.string.search_other);
                    setCategoryPattern(fileAllContainer, fileAllImage, fileAllText, ConstantField.FragmentIndex.FILE_ALL, false);
                    setCategoryPattern(fileImageContainer, fileImageImage, fileImageText, ConstantField.FragmentIndex.FILE_IMAGE, false);
                    setCategoryPattern(fileVideoContainer, fileVideoImage, fileVideoText, ConstantField.FragmentIndex.FILE_VIDEO, false);
                    setCategoryPattern(fileDocumentContainer, fileDocumentImage, fileDocumentText, ConstantField.FragmentIndex.FILE_DOCUMENT, false);
                    setCategoryPattern(fileOtherContainer, fileOtherImage, fileOtherText, ConstantField.FragmentIndex.FILE_OTHER, true);
                    break;
                case ConstantField.FragmentIndex.TAB_CIRCLE:
                    mCategory = ConstantField.Category.PICTURE_AND_VIDEO;
                    fileSearchInput.setHint(R.string.search_image_and_vide);
                    break;
                default:
                    break;
            }
        }
        setCategoryButtonPattern(selectIds == null);
    }

    private void setCategoryPattern(LinearLayout categoryContainer, ImageView categoryImage, TextView categoryText, int category, boolean isSelect) {
        if (categoryContainer != null && categoryImage != null && categoryText != null) {
            categoryContainer.setBackgroundResource(isSelect ? R.drawable.background_ffedf3ff_rectangle_10 : R.drawable.background_fff5f6fa_rectangle_10);
            switch (category) {
                case ConstantField.FragmentIndex.FILE_ALL:
                    categoryImage.setImageResource(isSelect ? R.drawable.ic_category_all_on : R.drawable.ic_category_all_off);
                    break;
                case ConstantField.FragmentIndex.FILE_IMAGE:
                    categoryImage.setImageResource(isSelect ? R.drawable.ic_category_image_on : R.drawable.ic_category_image_off);
                    break;
                case ConstantField.FragmentIndex.FILE_VIDEO:
                    categoryImage.setImageResource(isSelect ? R.drawable.ic_category_video_on : R.drawable.ic_category_video_off);
                    break;
                case ConstantField.FragmentIndex.FILE_DOCUMENT:
                    categoryImage.setImageResource(isSelect ? R.drawable.ic_category_document_on : R.drawable.ic_category_document_off);
                    break;
                case ConstantField.FragmentIndex.FILE_OTHER:
                    categoryImage.setImageResource(isSelect ? R.drawable.ic_category_other_on : R.drawable.ic_category_other_off);
                    break;
                default:
                    break;
            }
            categoryText.setTextColor(getResources().getColor(isSelect ? R.color.blue_ff337aff : R.color.gray_ff85899c));
        }
    }

    private void setCategoryButtonPattern(boolean isWork) {
        setCategoryButtonPattern(fileAllButton, (mCategoryIndex == ConstantField.FragmentIndex.FILE_ALL), isWork);
        setCategoryButtonPattern(fileImageButton, (mCategoryIndex == ConstantField.FragmentIndex.FILE_IMAGE), isWork);
        setCategoryButtonPattern(fileVideoButton, (mCategoryIndex == ConstantField.FragmentIndex.FILE_VIDEO), isWork);
        setCategoryButtonPattern(fileDocumentButton, (mCategoryIndex == ConstantField.FragmentIndex.FILE_DOCUMENT), isWork);
        setCategoryButtonPattern(fileOtherButton, (mCategoryIndex == ConstantField.FragmentIndex.FILE_OTHER), isWork);
    }

    private void setCategoryButtonPattern(Button categoryButton, boolean isSelect, boolean isWork) {
        if (categoryButton != null) {
            categoryButton.setBackgroundResource(isWork ? (isSelect ? R.drawable.background_ffedf3ff_rectangle_6
                    : R.drawable.background_fff5f6fa_rectangle_6) : (isSelect
                    ? R.drawable.background_fff2f7ff_rectangle_6 : R.drawable.background_fff5f6fa_rectangle_6));
            categoryButton.setTextColor(getResources().getColor(isWork ? (isSelect ? R.color.blue_ff337aff
                    : R.color.gray_ff85899c) : (isSelect ? R.color.blue_ffb6cfff : R.color.gray_ffd3d5df)));
            categoryButton.setTypeface(Typeface.defaultFromStyle(isSelect ? Typeface.BOLD : Typeface.NORMAL));
            categoryButton.setClickable(isWork);
        }
    }

    private void addFileSubView() {
        View view = LayoutInflater.from(this).inflate(R.layout.file_sub_view, null);
        fileList = view.findViewById(R.id.file_list);
        customizeFiles = new ArrayList<>();
        mAdapter = new FileAdapter(this, customizeFiles, ConstantField.ViewType.BOX_FILE_LINEAR_VIEW, false, false);
        mAdapter.setOnItemClickListener(this);
        fileList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        fileList.addItemDecoration(new FileAdapter.ItemDecoration(RecyclerView.VERTICAL, Math.round(getResources().getDimension(R.dimen.dp_1))
                , getResources().getColor(R.color.white_fff7f7f9)));
        headerFooterWrapper = new HeaderFooterWrapper(mAdapter);
        fileList.setAdapter(headerFooterWrapper);
        footer = new FooterView(this);
        mAdapter.setFooterView(footer);
        if (fileSubViewContainer != null) {
            int childCount = fileSubViewContainer.getChildCount();
            if (childCount > 0) {
                View childView = fileSubViewContainer.getChildAt((childCount - 1));
                if (childView != null) {
                    childView.setVisibility(View.GONE);
                }
            }
            view.setVisibility(View.VISIBLE);
            fileSubViewContainer.addView(view, childCount);
        }
    }

    private void removeFileSubView() {
        if (fileSubViewContainer != null) {
            int childCount = fileSubViewContainer.getChildCount();
            if (childCount > 1) {
                View childView = fileSubViewContainer.getChildAt((childCount - 2));
                if (childView != null) {
                    fileList = childView.findViewById(R.id.file_list);
                    if (fileList != null) {
                        if (fileList.getAdapter() != null && fileList.getAdapter() instanceof HeaderFooterWrapper) {
                            headerFooterWrapper = (HeaderFooterWrapper) fileList.getAdapter();
                            if (headerFooterWrapper != null && headerFooterWrapper.dataAdapter != null && headerFooterWrapper.dataAdapter instanceof FileAdapter) {
                                mAdapter = (FileAdapter) headerFooterWrapper.dataAdapter;
                                customizeFiles = mAdapter.getmCustomizeFileList();
                                footer = mAdapter.getFooterView();
                            }
                        }
                    }
                    childView.setVisibility(View.VISIBLE);
                }
                fileSubViewContainer.removeViewAt((childCount - 1));
            }
        }
    }

    private void resetFileSubView() {
        if (fileSubViewContainer != null) {
            fileSubViewContainer.removeAllViews();
        }
    }

    private void refresh() {
        ThumbManager.getInstance().cancelCache();
        isLocalEmpty = true;
        refreshEulixSpaceStorage();
    }

    private void setFooter(boolean isAdd, boolean isForce) {
        if (isAdd) {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() <= 0 && fileList != null && footer != null
                    && (isForce || (fileList.canScrollVertically(-1) || fileList.canScrollVertically(1)))) {
                // 只添加一次footer
                headerFooterWrapper.addFooterView(footer, loadMore);
            }
        } else {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() > 0) {
                headerFooterWrapper.removeAllFooters();
            }
        }
    }

    private void setFooterVisible(boolean isVisible) {
        if (footer != null) {
            ViewGroup.LayoutParams param = footer.getLayoutParams();

            if (isVisible) {
                param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                param.height = getResources().getDimensionPixelSize(R.dimen.dp_33);
            } else {
                param.width = 0;
                param.height = 0;
            }
            footer.setLayoutParams(param);
        }
    }

    /**
     * 展示软键盘
     *
     * @param editText
     * @param isForce
     */
    private void showSoftInput(EditText editText, boolean isForce) {
        if (inputMethodManager != null && editText != null) {
            try {
                inputMethodManager.showSoftInput(editText, (isForce ? InputMethodManager.SHOW_FORCED : InputMethodManager.SHOW_IMPLICIT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 收起软键盘
     */
    private void forceHideSoftInput() {
        if (inputMethodManager != null) {
            View view = getCurrentFocus();
            if (view != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void searchFile() {
        switchSearchVisibility(true);
        String content = fileSearchInput.getText().toString();
        mSearchContent = content;
        if (content.length() <= 0) {
            resetFileSubView();
            mTotalPage = 1;
            addFileSubView();
            openSearchDirectory(new ArrayList<>(), 0);
            swipeRefreshContainer.setRefreshing(false);
        } else if (presenter != null) {
            setSearchResult(-1);
            resetFileSubView();
            addFileSubView();
            presenter.searchFile(content, 1, mCategory);
        }
    }

    private void setSearchResult(int number) {
        if (number < 0) {
            fileSearchResult.setText(R.string.search_result);
            fileSearchResult.setVisibility(View.GONE);
        } else {
            fileSearchResult.setVisibility(View.VISIBLE);
            String result = getString(R.string.search_result) + getString(R.string.left_bracket)
                    + number + getString(R.string.right_bracket);
            fileSearchResult.setText(result);
        }
    }

    private void handleDataResult(int statusCode) {
        switch (statusCode) {
            case -5:
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.INVISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                emptySearchContainer.setVisibility(View.INVISIBLE);
                setSearchResult(-1);
                break;
            case -4:
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                emptySearchContainer.setVisibility(View.VISIBLE);
                setSearchResult(0);
                break;
            case -3:
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                emptySearchContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.VISIBLE);
                break;
            case -2:
                fileSubViewContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptySearchContainer.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.VISIBLE);
                break;
            case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                obtainAccessToken();
                fileSubViewContainer.setVisibility(View.GONE);
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult((getFileDepth() >= 0) ? -2 : ConstantField.NETWORK_ERROR_CODE);
                }
                break;
            case ConstantField.FILE_DISCONNECT_CODE:
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint);
                break;
            case ConstantField.SERVER_EXCEPTION_CODE:
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                break;
            case ConstantField.NETWORK_ERROR_CODE:
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.VISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                emptySearchContainer.setVisibility(View.INVISIBLE);
                break;
            default:
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.VISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                emptySearchContainer.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void changeEditAdapterView(int showType) {
        if (mAdapter != null && fileList != null) {
            mAdapter.changeEditStatus(showType);
            int visibleCount = fileList.getChildCount();
            for (int i = 0; i < visibleCount; i++) {
                View child = fileList.getChildAt(i);
                if (child != null) {
                    switch (mAdapter.getItemViewType(0)) {
                        case ConstantField.ViewType.BOX_FILE_LINEAR_VIEW:
                            mAdapter.setMarkPattern(child, showType);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * 搜索文件
     *
     * @param customizeFileList
     */
    private void openSearchDirectory(List<CustomizeFile> customizeFileList, Integer fileCount) {
        List<Integer> selectionPositionList = null;
        if (customizeFileList != null) {
            mPage = 1;
            customizeFiles = customizeFileList;
            if (mAdapter != null && headerFooterWrapper != null) {
                if (selectIds == null) {
                    mAdapter.updateData(customizeFiles, false, mSearchContent);
                } else {
                    selectionPositionList = new ArrayList<>();
                    int size = customizeFileList.size();
                    for (int i = 0; i < size; i++) {
                        CustomizeFile customizeFile = customizeFileList.get(i);
                        if (customizeFile != null) {
                            String id = customizeFile.getId();
                            if (id != null && selectIds.contains(id)) {
                                selectionPositionList.add(i);
                            }
                        }
                    }
                    mAdapter.updateData(customizeFiles, selectionPositionList, mSearchContent);
                }
                headerFooterWrapper.notifyDataSetChanged();
            }
            searchResultNumber = (fileCount == null ? customizeFileList.size() : fileCount);
            setSearchResult(searchResultNumber);
            if (selectionPositionList == null) {
                handleTitleVisibility(-1, 0);
            } else {
                handleTitleVisibility(selectionPositionList.size(), customizeFileList.size());
            }
            handleDataResult(customizeFileList.size() <= 0 ? -4 : -3);
        } else {
            handleTitleVisibility(-1, 0);
            handleDataResult(-5);
        }
    }

    /**
     * 打开在线文件夹
     *
     * @param customizeFileList
     */
    private void openDirectory(String currentDirectory, List<CustomizeFile> customizeFileList, boolean isLocal, boolean isBack) {
        if (!isBack) {
            setFooter(false, true);
        }
        List<Integer> selectionPositionList = null;
        if (customizeFileList != null) {
            if (!isLocal) {
                mPage = 1;
            }
            handleDataResult(customizeFileList.size() <= 0 ? -2 : -3);
        }
        if (customizeFileList == null) {
            customizeFileList = new ArrayList<>();
        }
        customizeFiles = customizeFileList;
        if (mAdapter != null && headerFooterWrapper != null) {
            if (selectIds == null) {
                mAdapter.updateData(customizeFiles, false, null);
            } else {
                selectionPositionList = new ArrayList<>();
                int size = customizeFileList.size();
                for (int i = 0; i < size; i++) {
                    CustomizeFile customizeFile = customizeFileList.get(i);
                    if (customizeFile != null) {
                        String id = customizeFile.getId();
                        if (id != null && selectIds.contains(id)) {
                            selectionPositionList.add(i);
                        }
                    }
                }
                mAdapter.updateData(customizeFiles, selectionPositionList, null);
            }
            headerFooterWrapper.notifyDataSetChanged();
        }
        String title = getUuidTitle(currentDirectory);
        if (selectionPositionList == null) {
            handleTitleVisibility(title == null ? "" : title);
        } else {
            handleTitleVisibility(selectionPositionList.size(), customizeFileList.size(), title == null ? "" : title);
        }
    }

    /**
     * 在线文件夹增加一页
     *
     * @param customizeFileList
     * @param pageInfo
     */
    private void addSearchDirectory(List<CustomizeFile> customizeFileList, PageInfo pageInfo, Integer fileCount) {
        if (pageInfo != null && pageInfo.getPage() != null) {
            List<Integer> selectionPositionList = null;
            mPage = Math.max(mPage, pageInfo.getPage());
            if (customizeFiles != null) {
                if (customizeFileList != null) {
                    customizeFiles.addAll(customizeFileList);
                }
                if (mAdapter != null && headerFooterWrapper != null) {
                    if (selectIds == null) {
                        mAdapter.updateData(customizeFiles, false, mSearchContent);
                    } else {
                        selectionPositionList = new ArrayList<>();
                        int size = customizeFiles.size();
                        for (int i = 0; i < size; i++) {
                            CustomizeFile customizeFile = customizeFiles.get(i);
                            if (customizeFile != null) {
                                String id = customizeFile.getId();
                                if (id != null && selectIds.contains(id)) {
                                    selectionPositionList.add(i);
                                }
                            }
                        }
                        mAdapter.updateData(customizeFiles, selectionPositionList, mSearchContent);
                    }
                    headerFooterWrapper.notifyDataSetChanged();
                }
                searchResultNumber = (fileCount == null ? customizeFiles.size() : fileCount);
                setSearchResult(searchResultNumber);
            }
            if (selectionPositionList == null) {
                if (mAdapter != null && customizeFiles != null) {
                    List<Integer> selectPosition = mAdapter.getSelectPosition();
                    handleTitleVisibility((selectPosition == null ? -1 : selectPosition.size()), customizeFiles.size());
                }
            } else {
                handleTitleVisibility(selectionPositionList.size(), customizeFiles.size());
            }
        }
    }

    /**
     * 在线文件夹增加一页
     *
     * @param customizeFileList
     * @param pageInfo
     */
    private void addDirectory(List<CustomizeFile> customizeFileList, PageInfo pageInfo) {
        if (pageInfo != null && pageInfo.getPage() != null) {
            List<Integer> selectionPositionList = null;
            mPage = Math.max(mPage, pageInfo.getPage());
            List<String> currentIds = new ArrayList<>();
            if (customizeFiles != null) {
                for (CustomizeFile customizeFile : customizeFiles) {
                    if (customizeFile != null) {
                        currentIds.add(customizeFile.getId());
                    }
                }
                if (customizeFileList != null) {
                    for (CustomizeFile customizeFile : customizeFileList) {
                        if (customizeFile != null && !currentIds.contains(customizeFile.getId())) {
                            customizeFiles.add(customizeFile);
                        }
                    }
                }
                if (mAdapter != null && headerFooterWrapper != null) {
                    if (selectIds == null) {
                        mAdapter.updateData(customizeFiles, false, null);
                    } else {
                        selectionPositionList = new ArrayList<>();
                        int size = customizeFiles.size();
                        for (int i = 0; i < size; i++) {
                            CustomizeFile customizeFile = customizeFiles.get(i);
                            if (customizeFile != null) {
                                String id = customizeFile.getId();
                                if (id != null && selectIds.contains(id)) {
                                    selectionPositionList.add(i);
                                }
                            }
                        }
                        mAdapter.updateData(customizeFiles, selectionPositionList, null);
                    }
                    headerFooterWrapper.notifyDataSetChanged();
                }
            }
            if (selectionPositionList == null) {
                if (mAdapter != null && customizeFiles != null) {
                    List<Integer> selectPosition = mAdapter.getSelectPosition();
                    handleTitleVisibility((selectPosition == null ? -1 : selectPosition.size()), customizeFiles.size());
                }
            } else {
                handleTitleVisibility(selectionPositionList.size(), customizeFiles.size());
            }
        }
    }

    private void handleSwipeEnable() {
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setEnabled(selectIds == null);
        }
        setCategoryButtonPattern(selectIds == null);
    }

    private int getFileDepth() {
        return ((fileEditView == null ? 0 : fileEditView.getDepth()) - 1);
    }

    private int getSelectNumber() {
        int number = -1;
        if (mAdapter != null) {
            List<Integer> selectPosition = mAdapter.getSelectPosition();
            if (selectPosition != null) {
                number = selectPosition.size();
            }
        }
        return number;
    }

    private void handleFileEditView(int selectNumber) {
        if (fileEditView != null) {
            if (selectNumber > 0) {
                fileEditView.showFileDialog(selectNumber);
            } else {
                fileEditView.dismissFileDialog();
            }
        }
    }

    private void handleSelectTotal() {
        if (mAdapter != null && customizeFiles != null) {
            int selectNumber = -1;
            List<Integer> selectPosition = mAdapter.getSelectPosition();
            if (selectPosition != null) {
                selectNumber = selectPosition.size();
            }
            int totalNumber = customizeFiles.size();
            if (selectNumber < totalNumber) {
                if (selectIds == null) {
                    selectIds = new ArrayList<>();
                }
                for (CustomizeFile customizeFile : customizeFiles) {
                    if (customizeFile != null) {
                        selectIds.add(customizeFile.getId());
                    }
                }
                changeEditAdapterView(ConstantField.ShowType.SELECT);
                mAdapter.changeEditStatus(ConstantField.ShowType.SELECT);
                handleTitleVisibility(totalNumber, totalNumber);
            } else {
                if (selectIds != null) {
                    selectIds.clear();
                }
                changeEditAdapterView(ConstantField.ShowType.EDIT);
                mAdapter.changeEditStatus(ConstantField.ShowType.EDIT);
                handleTitleVisibility(0);
            }
            handleSwipeEnable();
        }
    }

    private void folderChange(List<UUID> newFolderList) {
        if (fileEditView != null && newFolderList != null) {
            UUID uuid = fileEditView.getUUID();
            String folderUUIDValue = ConstantField.UUID.FILE_ROOT_UUID;
            if (uuid != null) {
                folderUUIDValue = uuid.toString();
            }
            for (UUID newFolderUUID : newFolderList) {
                if (newFolderUUID != null && newFolderUUID.toString().equals(folderUUIDValue)) {
                    refreshEulixSpaceStorage();
                    break;
                }
            }
        }
    }

    private String getUuidTitle(String uuid) {
        String title = null;
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        if (uuid != null && uuidTitleMap != null && uuidTitleMap.containsKey(uuid)) {
            title = uuidTitleMap.get(uuid);
        }
        return title;
    }

    private void handleTitleVisibility(int selectNumber) {
        handleTitleVisibility(selectNumber, (selectNumber + 1));
    }

    private void handleTitleVisibility(String titleText) {
        handleTitleVisibility(-1, 0, titleText);
    }

    private void handleTitleVisibility(int selectNumber, int totalNumber) {
        handleTitleVisibility(selectNumber, totalNumber, null);
    }

    private void handleTitleVisibility(int selectNumber, int totalNumber, String titleText) {
        int fileDepth = getFileDepth();
        searchContainer.setVisibility(selectNumber >= 0 ? View.GONE : View.VISIBLE);
        inputContainer.setVisibility((selectNumber >= 0 || fileDepth >= 0) ? View.INVISIBLE : View.VISIBLE);
        title.setVisibility((selectNumber < 0 && fileDepth >= 0) ? View.VISIBLE : View.GONE);
        if (titleText != null) {
            title.setText(titleText);
        }
        selectContainer.setVisibility(selectNumber >= 0 ? View.VISIBLE : View.GONE);
        if (fileSelect != null) {
            String fileSelectNumber = getString(R.string.choose_file_part_1) + selectNumber +
                    getString((Math.abs(selectNumber) == 1) ? R.string.choose_file_part_2_singular : R.string.choose_file_part_2_plural);
            fileSelect.setText(fileSelectNumber);
        }
        if (select != null) {
            select.setText(selectNumber < totalNumber ? R.string.select_all : R.string.select_none);
        }
        handleFileEditView(selectNumber);
    }

    private UUID getCurrentFolderUUID() {
        UUID uuid = null;
        if (fileEditView != null) {
            uuid = fileEditView.getUUID();
        }
        return uuid;
    }

    private ArrayStack<UUID> getCurrentUUIDStack() {
        ArrayStack<UUID> uuids = null;
        if (fileEditView != null) {
            uuids = fileEditView.getUUIDStack();
        }
        return uuids;
    }

    private CustomizeFile getSelectFile() {
        CustomizeFile customizeFile = null;
        List<CustomizeFile> customizeFiles = getSelectFiles();
        if (customizeFiles != null && customizeFiles.size() == 1) {
            customizeFile = customizeFiles.get(0);
        }
        return customizeFile;
    }

    private void handleSearchPageInfo(PageInfo pageInfo, List<CustomizeFile> customizeFileList, Integer fileCount) {
        Integer pageValue = pageInfo.getPage();
        Integer totalPageValue = pageInfo.getTotal();
        if (totalPageValue != null) {
            mTotalPage = totalPageValue;
        }
        if (pageValue != null && pageValue > 1) {
            addSearchDirectory(customizeFileList, pageInfo, fileCount);
        } else {
            openSearchDirectory(customizeFileList, fileCount);
        }
        if (mTotalPage > 1) {
            Integer pageSizeValue = pageInfo.getPageSize();
            if (pageSizeValue != null) {
                int pageSize = pageSizeValue;
                if (pageSize <= 0) {
                    pageSize = customizeFileList.size();
                }
                pageSize = Math.max(pageSize, 1);
                if (presenter != null && (pageSize * mPage) <= maxChildCount && mPage < mTotalPage) {
                    presenter.searchFile(mSearchContent, (mPage + 1), mCategory);
                }
            }
        }
        if (mPage == mTotalPage) {
            searchResultNumber = (customizeFiles != null ? customizeFiles.size() : (fileCount == null ? 0 : fileCount));
            setSearchResult(searchResultNumber);
            if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))) {
                setFooterVisible(false);
            } else if (footer != null) {
                setFooterVisible(true);
                footer.showBottom(getString(R.string.home_bottom_flag));
            }
        } else {
            setFooterVisible(true);
            if (footer != null) {
                footer.showLoading();
            }
        }
    }

    private void handlePageInfo(PageInfo pageInfo, String currentDirectory, List<CustomizeFile> customizeFiles) {
        Integer pageValue = pageInfo.getPage();
        Integer totalPageValue = pageInfo.getTotal();
        if (totalPageValue != null) {
            mTotalPage = totalPageValue;
        }
        if (pageValue != null && pageValue > 1 && fileEditView.isUUIDSame()) {
            addDirectory(customizeFiles, pageInfo);
        } else {
            openDirectory(currentDirectory, customizeFiles, false, false);
        }
        if (mTotalPage > 1) {
            Integer pageSizeValue = pageInfo.getPageSize();
            if (pageSizeValue != null) {
                int pageSize = pageSizeValue;
                if (pageSize <= 0) {
                    pageSize = customizeFiles.size();
                }
                pageSize = Math.max(pageSize, 1);
                if (fileEditView != null && (pageSize * mPage) <= maxChildCount && mPage < mTotalPage) {
                    fileEditView.getEulixSpaceStorage((mPage + 1));
                }
            }
        }
        if (mPage == mTotalPage) {
            if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))) {
                setFooterVisible(false);
            } else if (footer != null) {
                setFooterVisible(true);
                footer.showBottom(getString(R.string.home_bottom_flag));
            }
        } else {
            setFooterVisible(true);
            if (footer != null) {
                footer.showLoading();
            }
        }
    }

    public boolean isShareDialogShow() {
        return (fileEditView != null && fileEditView.isShareDialogShowing());
    }

    public void dismissShareDialog(String absolutePath, boolean isShare) {
        if (fileEditView != null) {
            fileEditView.dismissShareDialog(absolutePath, isShare);
        }
    }

    private void obtainAccessToken() {
        Intent serviceIntent = new Intent(FileSearchActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        startService(serviceIntent);
    }

    public void getLocalEulixSpaceStorage(String currentId, boolean isNext, boolean isBack) {
        boolean tempLoadingEnable = isLoadingEnable;
        isLoadingEnable = false;
        if (selectIds != null) {
            selectIds.clear();
            selectIds = null;
        }
        handleSwipeEnable();
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setRefreshing(false);
        }
        if (presenter != null) {
            List<CustomizeFile> customizeFiles = presenter.getLocalEulixSpaceStorage(currentId);
            isLocalEmpty = (customizeFiles == null);
            if (ConstantField.Category.FILE_ROOT.equals(currentId) || ConstantField.Category.FILE_IMAGE.equals(currentId)
                    || ConstantField.Category.FILE_VIDEO.equals(currentId) || ConstantField.Category.FILE_DOCUMENT.equals(currentId)
                    || ConstantField.Category.FILE_OTHER.equals(currentId)) {
                currentId = ConstantField.UUID.FILE_ROOT_UUID;
            }
            if (fileEditView != null && isNext) {
                if (mAdapter != null) {
                    mAdapter.setLoadingEnable(tempLoadingEnable);
                    mAdapter.setCurrentPage(mPage);
                    mAdapter.setTotalPage(mTotalPage);
                }
                addFileSubView();
                fileEditView.handleNext(currentId, true);
            }
            openDirectory(currentId, customizeFiles, true, isBack);
        }
        if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))
                && customizeFiles != null && customizeFiles.size() < maxChildCount) {
            setFooter(true, false);
            setFooterVisible(false);
        } else if (footer != null) {
            setFooter(true, true);
            setFooterVisible(true);
            footer.showBottom(getString(R.string.home_bottom_flag));
        }
    }

    private void refreshEulixSpaceStorage() {
        if (getFileDepth() >= 0) {
            if (fileEditView != null) {
                fileEditView.getEulixSpaceStorage(1);
            }
        } else {
            searchFile();
        }

    }

    private boolean checkSearchValid(String currentSearchId) {
        boolean result = false;
        if (currentSearchId != null) {
            int currentSearchIndex = -1;
            switch (currentSearchId) {
                case ConstantField.Category.FILE_ROOT:
                    currentSearchIndex = ConstantField.FragmentIndex.FILE_ALL;
                    break;
                case ConstantField.Category.FILE_IMAGE:
                    currentSearchIndex = ConstantField.FragmentIndex.FILE_IMAGE;
                    break;
                case ConstantField.Category.FILE_VIDEO:
                    currentSearchIndex = ConstantField.FragmentIndex.FILE_VIDEO;
                    break;
                case ConstantField.Category.FILE_DOCUMENT:
                    currentSearchIndex = ConstantField.FragmentIndex.FILE_DOCUMENT;
                    break;
                case ConstantField.Category.FILE_OTHER:
                    currentSearchIndex = ConstantField.FragmentIndex.FILE_OTHER;
                    break;
                default:
                    break;
            }
            result = (currentSearchIndex == mCategoryIndex);
        }
        return result;
    }

    private void startPreviewFile(CustomizeFile customizeFile) {
        String mimeType = FileUtil.getMimeTypeByPath(customizeFile.getName());
        if (mimeType.contains("image")) {
            List<CustomizeFile> imageList = new ArrayList<>();
            int selectPosition = 0;
            int imageIndex = 0;
            for (int i = 0; i < customizeFiles.size(); i++) {
                if (FileUtil.getMimeTypeByPath(customizeFiles.get(i).getName()).contains("image")) {
                    imageList.add(customizeFiles.get(i));
                    if (customizeFile == customizeFiles.get(i)) {
                        selectPosition = imageIndex;
                    }
                    imageIndex++;
                }
            }
            FilePreviewActivity.openImgList(this, imageList, selectPosition, TransferHelper.FROM_FILE, -1, -1);
        } else {
            Intent intent = new Intent(FileSearchActivity.this, FilePreviewActivity.class);
            intent.putExtra(FilePreviewActivity.KEY_FILE_NAME, customizeFile.getName());
            intent.putExtra(FilePreviewActivity.KEY_FILE_PATH, customizeFile.getPath());
            intent.putExtra(FilePreviewActivity.KEY_FILE_UUID, customizeFile.getId());
            intent.putExtra(FilePreviewActivity.KEY_FILE_SIZE, customizeFile.getSize());
            intent.putExtra(FilePreviewActivity.KEY_FILE_MD5, customizeFile.getMd5());
            intent.putExtra(FilePreviewActivity.KEY_FILE_TIME, customizeFile.getTimestamp());
            startActivity(intent);
        }
    }


    @NotNull
    @Override
    public FileSearchPresenter createPresenter() {
        return new FileSearchPresenter();
    }

    @Override
    public void onBackPressed() {
        if (!handleBackEvent()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return ((keyCode == KeyEvent.KEYCODE_BACK && handleBackEvent()) || super.onKeyDown(keyCode, event));
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        EventBusUtil.unRegister(this);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        ThumbManager.getInstance().cancelCache();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (isWindowFocus == null) {
                isWindowFocus = false;
            } else if (!isWindowFocus && fileSearchInput != null) {
                isWindowFocus = true;
                if (mHandler == null) {
                    showSoftInput(fileSearchInput, true);
                } else {
                    mHandler.postDelayed(() -> showSoftInput(fileSearchInput, false), 200);
                }
            }
        }
    }

    //预览页删除文件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeleteFileEvent event) {
        Logger.d("zfy", "receive DeleteEvent");
        showImageTextToast(R.drawable.toast_right, R.string.delete_success);
        refreshEulixSpaceStorage();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferStateEvent event) {
        Logger.d("zfy", "receive TransferStateEvent:" + event.state);
    }

    @Override
    public void searchEulixSpaceFileResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFileList, PageInfo pageInfo, Integer fileCount) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (swipeRefreshContainer != null) {
                    swipeRefreshContainer.setRefreshing(false);
                }
                if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                }
                if (mSearched && getFileDepth() < 0 && (checkSearchValid(currentDirectory))) {
                    if (customizeFileList == null) {
                        closeLoading();
                        if (customizeFiles == null || customizeFiles.size() <= 0) {
                            handleDataResult(code);
                        } else {
                            setSearchResult(customizeFiles.size());
                        }
                        searchResultNumber = (customizeFiles != null ? customizeFiles.size() : (fileCount == null ? 0 : fileCount));
                        setSearchResult(searchResultNumber);
                        if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))) {
                            setFooterVisible(false);
                        } else if (footer != null) {
                            setFooterVisible(true);
                            footer.showBottom(getString(R.string.home_bottom_flag));
                        }
                    } else {
                        if (pageInfo != null) {
                            handleSearchPageInfo(pageInfo, customizeFileList, fileCount);
                        } else {
                            mTotalPage = 1;
                            closeLoading();
                            openSearchDirectory(customizeFileList, fileCount);
                        }
                    }
                    setFooter(true, false);
                } else {
                    Logger.d(TAG, "search result expired");
                }
            });
        }
    }

    @Override
    public void onItemClick(View view, int position, boolean isEnable) {
        if (mAdapter != null && customizeFiles != null) {
            List<Integer> selectPosition = mAdapter.getSelectPosition();
            int selectNumber = -1;
            if (selectPosition != null) {
                selectNumber = selectPosition.size();
            }
            handleTitleVisibility(selectNumber, customizeFiles.size());
            if (selectNumber >= 0) {
                if (customizeFiles != null) {
                    if (selectIds == null) {
                        selectIds = new ArrayList<>();
                    } else {
                        selectIds.clear();
                    }
                    for (int selectP : selectPosition) {
                        if (selectP >= 0 && customizeFiles.size() > selectP) {
                            CustomizeFile customizeFile = customizeFiles.get(selectP);
                            if (customizeFile != null) {
                                selectIds.add(customizeFile.getId());
                            }
                        }
                    }
                }
            } else {
                if (selectIds != null) {
                    selectIds.clear();
                    selectIds = null;
                }
                if (isEnable && presenter != null && customizeFiles != null && position >= 0 && customizeFiles.size() > position) {
                    CustomizeFile customizeFile = customizeFiles.get(position);
                    if (customizeFile != null) {
                        String mime = customizeFile.getMime();
                        if (mime != null && mime.equalsIgnoreCase(ConstantField.MimeType.FOLDER)) {
                            String titleContent = customizeFile.getName();
                            String id = customizeFile.getId();
                            if (id != null) {
                                DataUtil.setUuidTitleMap(id, titleContent);
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(customizeFile.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (uuid != null && mBridge != null) {
                                    inputContainer.setVisibility(View.INVISIBLE);
                                    fileSearchCategoryTab.setVisibility(View.GONE);
                                    setSearchResult(-1);
                                    getLocalEulixSpaceStorage(uuid.toString(), true, false);
                                    fileEditView.getEulixSpaceStorage(uuid, 1, null, null);
                                }
                            }
                        } else {
                            //预览文件
                            startPreviewFile(customizeFile);
                        }
                    }
                }
            }
            handleSwipeEnable();
        }
    }

    @Override
    public boolean handleBackEvent() {
        if (mAdapter == null) {
            return false;
        }
        if (mAdapter.getSelectPosition() != null) {
            if (mHandler != null) {
                mHandler.post(() -> {
                    changeEditAdapterView(ConstantField.ShowType.NORMAL);
                    mAdapter.changeEditStatus(ConstantField.ShowType.NORMAL);
                    handleTitleVisibility(-1);
                });
            }
            if (selectIds != null) {
                selectIds.clear();
                selectIds = null;
            }
            handleSwipeEnable();
            return true;
        }
        // 在文件搜索页展示文件
        int fileDepth = getFileDepth();
        if (fileDepth >= 0) {
            removeFileSubView();
            if (fileDepth == 0) {
//                openSearchDirectory(null, null);
//                fileSearchInput.setText("");
                exceptionContainer.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.VISIBLE);
                fileSearchCategoryTab.setVisibility(View.VISIBLE);
                setSearchResult(searchResultNumber);
                inputContainer.setVisibility(View.VISIBLE);
                fileEditView.reset();
            } else {
                UUID uuid = fileEditView.handleBack();
                if (uuid != null) {
                    if (ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
                        getLocalEulixSpaceStorage(ConstantField.Category.FILE_ROOT, false, true);
                    } else {
                        getLocalEulixSpaceStorage(uuid.toString(), false, true);
                    }
                    if (mAdapter != null) {
                        mPage = mAdapter.getCurrentPage();
                        mTotalPage = mAdapter.getTotalPage();
                        isLoadingEnable = mAdapter.isLoadingEnable();
                    }
                    if (mPage == mTotalPage) {
                        if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))) {
                            setFooterVisible(false);
                        } else if (footer != null) {
                            setFooterVisible(true);
                            footer.showBottom(getString(R.string.home_bottom_flag));
                        }
                    } else {
                        setFooterVisible(true);
                        if (footer != null) {
                            footer.showLoading();
                        }
                    }
                    setFooter(true, false);
//                    fileEditView.getEulixSpaceStorage(uuid, 1, null, null);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void handleRefresh(boolean isSuccess, String serviceFunction) {
        if (isSuccess && mHandler != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.MODIFY_FILE:
                case ConstantField.ServiceFunction.DELETE_FILE:
                case ConstantField.ServiceFunction.MOVE_FILE:
                    mHandler.post(this::refreshEulixSpaceStorage);
                    OperationUtil.setAllFileRefresh(true);
                    break;
                case ConstantField.ServiceFunction.COPY_FILE:
                    OperationUtil.setAllFileRefresh(true);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public List<CustomizeFile> getSelectFiles() {
        List<CustomizeFile> files = new ArrayList<>();
        if (mAdapter != null && customizeFiles != null) {
            List<Integer> selectIndex = mAdapter.getSelectPosition();
            if (selectIndex != null) {
                for (int i : selectIndex) {
                    if (i >= 0 && customizeFiles.size() > i) {
                        files.add(customizeFiles.get(i));
                    }
                }
            }
        }
        return files;
    }

    @Override
    public void fileDialog(View view, boolean isShow) {
        if (fileEditContainer != null) {
            fileEditContainer.removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            fileEditContainer.addView(view, layoutParams);
            fileEditContainer.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void handleGetEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (swipeRefreshContainer != null) {
                    swipeRefreshContainer.setRefreshing(false);
                }
                if (currentDirectory == null && customizeFiles == null) {
                    isLoadingEnable = true;
                    if (isLocalEmpty) {
                        handleDataResult(code);
                    }
                } else {
                    UUID uuid = fileEditView.getUUID();
                    if (uuid != null && uuid.toString().equals(currentDirectory)) {
                        setSearchResult(-1);
                        isLoadingEnable = true;
                        if (customizeFiles == null) {
                            if (isLocalEmpty) {
                                handleDataResult(code);
                            }
                        } else {
                            if (pageInfo != null) {
                                handlePageInfo(pageInfo, currentDirectory, customizeFiles);
                            } else {
                                mTotalPage = 1;
                                openDirectory(currentDirectory, customizeFiles, false, false);
                            }
                        }
                    } else {
                        Logger.d(TAG, "result expired");
                    }
                }
                setFooter(true, false);
            });
        }
    }

    @Override
    public ArrayStack<UUID> handleCurrentUUIDStack() {
        return DataUtil.cloneUUIDStack(getCurrentUUIDStack());
    }

    @Override
    public void handleRefreshEulixSpaceStorage(UUID parentUUID) {
        if (parentUUID != null) {
            newFolderUUIDList = new ArrayList<>();
            newFolderUUIDList.add(parentUUID);
            folderChange(newFolderUUIDList);
            newFolderUUIDList = null;
        }
    }

    @Override
    public void handleDismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs) {
        newFolderUUIDList = newFolderUUIDs;
        if (newFolderUUIDs != null) {
            folderChange(newFolderUUIDList);
            newFolderUUIDList = null;
        }
        if (isConfirm) {
            if (isCopy == null) {
                DataUtil.setUuidStack(uuids);
            } else {
                handleBackEvent();
            }
        }
    }

    @Override
    public void handleShowOrDismissFileSearch(boolean isShow) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isShow) {
//                    if (mBridge != null) {
//                        // todo 放入原uuid
//                        mBridge.startSelf(null);
//                    }
                } else {
//                    finish();
                }
            });
        }
    }

    @Override
    public void handleFolderDetail(String folderUuid, String name, Long operationAt, String path, Long size) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (customizeFiles != null && folderUuid != null) {
                    for (CustomizeFile customizeFile : customizeFiles) {
                        if (customizeFile != null && folderUuid.equals(customizeFile.getId()) && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                            if (size != null) {
                                customizeFile.setSize(size);
                            }
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    if (!handleBackEvent()) {
                        finish();
                    }
                    break;
                case R.id.cancel:
                    handleBackEvent();
                    break;
                case R.id.select:
                    handleSelectTotal();
                    break;
                case R.id.file_search:
//                    searchFile();
                    break;
                case R.id.file_search_clear:
                    openSearchDirectory(null, null);
                    fileSearchInput.setText("");
                    inputContainer.setVisibility(View.VISIBLE);
                    fileEditView.reset();
                    switchSearchVisibility(false);
                    break;
                case R.id.file_all_container:
                    setCategory(ConstantField.FragmentIndex.FILE_ALL);
                    break;
                case R.id.file_all_button:
                    setCategory(ConstantField.FragmentIndex.FILE_ALL);
                    refresh();
                    break;
                case R.id.file_image_container:
                    setCategory(ConstantField.FragmentIndex.FILE_IMAGE);
                    break;
                case R.id.file_image_button:
                    setCategory(ConstantField.FragmentIndex.FILE_IMAGE);
                    refresh();
                    break;
                case R.id.file_video_container:
                    setCategory(ConstantField.FragmentIndex.FILE_VIDEO);
                    break;
                case R.id.file_video_button:
                    setCategory(ConstantField.FragmentIndex.FILE_VIDEO);
                    refresh();
                    break;
                case R.id.file_document_container:
                    setCategory(ConstantField.FragmentIndex.FILE_DOCUMENT);
                    break;
                case R.id.file_document_button:
                    setCategory(ConstantField.FragmentIndex.FILE_DOCUMENT);
                    refresh();
                    break;
                case R.id.file_other_container:
                    setCategory(ConstantField.FragmentIndex.FILE_OTHER);
                    break;
                case R.id.file_other_button:
                    setCategory(ConstantField.FragmentIndex.FILE_OTHER);
                    refresh();
                    break;
                case R.id.refresh_now:
                    isLocalEmpty = true;
                    refreshEulixSpaceStorage();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void finishSearch() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }
}
