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

package xyz.eulix.space.adapter.files;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.HighlightString;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.rv.FooterView;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/20 17:23
 */
public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = FileAdapter.class.getSimpleName();
    private Context mContext;
    public List<CustomizeFile> mCustomizeFileList;
    private int mViewType;
    private int nameMaxWidthLinear = -1;
    private boolean isNameMaxWidthLinearInit;
    private boolean isDialog;
    private boolean isEditOnly;
    private Map<Integer, Integer> showTypeMap;
    private FileAdapter.OnItemClickListener mOnItemClickListener;
    private FooterView footerView;
    private int currentPage;
    private int totalPage;
    private boolean loadingEnable;
    private String searchContent;

    public interface OnItemClickListener {
        void onItemClick(View view, int position, boolean isEnable);
    }

    public FileAdapter(Context context, List<CustomizeFile> customizeFileList, int viewType, boolean dialog, boolean editOnly) {
        mContext = context;
        mCustomizeFileList = customizeFileList;
        mViewType = viewType;
        isDialog = dialog;
        isEditOnly = editOnly;
        if (customizeFileList != null) {
            int size = customizeFileList.size();
            showTypeMap = new HashMap<>();
            for (int i = 0; i < size; i++) {
                showTypeMap.put(i, ConstantField.ShowType.NORMAL);
            }
        }
        nameMaxWidthLinear = (ViewUtils.getScreenWidth(mContext) - mContext.getResources().getDimensionPixelSize(R.dimen.dp_38)
                - mContext.getResources().getDimensionPixelSize(R.dimen.dp_29) - mContext.getResources().getDimensionPixelSize(R.dimen.dp_9)
                - mContext.getResources().getDimensionPixelSize(R.dimen.dp_25) - mContext.getResources().getDimensionPixelSize(R.dimen.dp_25));
        isNameMaxWidthLinearInit = false;
    }

    public void setMarkPattern(View view, int showType) {
        if (view != null) {
            ImageView mark = view.findViewById(R.id.eulix_item_mark);
            Object positionTag = view.getTag();
            boolean isFolder = false;
            if (positionTag instanceof Integer) {
                int position = (Integer) positionTag;
                if (mCustomizeFileList != null && mCustomizeFileList.size() > position) {
                    CustomizeFile customizeFile = mCustomizeFileList.get(position);
                    if (customizeFile != null) {
                        String mime = customizeFile.getMime();
                        isFolder = (!TextUtils.isEmpty(mime) && mime.equalsIgnoreCase(ConstantField.MimeType.FOLDER));
                    }
                }
            }
            setMarkPattern(mark, showType, isFolder);
        }
    }

    private void setMarkPattern(ImageView mark, int showType, boolean isFolder) {
        if (mark != null) {
            switch (showType) {
                case ConstantField.ShowType.NORMAL:
                case ConstantField.ShowType.EDIT:
                    mark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
                    break;
                case ConstantField.ShowType.SELECT:
                    mark.setImageResource(R.drawable.file_selected);
                    break;
                default:
                    break;
            }
        }
    }

    private void changeSelectStatus(boolean isSelect, int position) {
        int showType = (isSelect ? ConstantField.ShowType.SELECT : ConstantField.ShowType.EDIT);
        boolean isChange = false;
        if (showTypeMap != null) {
            if (showTypeMap.containsKey(position)) {
                Integer value = showTypeMap.get(position);
                isChange = (value == null || (value != ConstantField.ShowType.NORMAL && value != showType));
            } else {
                isChange = true;
            }
        }
        if (isChange) {
            showTypeMap.put(position, showType);
        }
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mCustomizeFileList != null && mCustomizeFileList.size() > position) {
            CustomizeFile customizeFile = mCustomizeFileList.get(position);
            if (customizeFile != null) {
                String mime;
                if (customizeFile.getMime().equals(ConstantField.MimeType.FOLDER)){
                    mime = ConstantField.MimeType.FOLDER;
                } else {
                    mime = FileUtil.getMimeTypeByPath(customizeFile.getName());
                }
                holder.icon.setImageResource(FileUtil.getMimeIcon(mime));
                String name = customizeFile.getName();
                if (holder.name != null) {
                    Map<Integer, Integer> searchIndexMap = null;
                    if (name != null && !TextUtils.isEmpty(name) && searchContent != null
                            && !TextUtils.isEmpty(searchContent) && name.toLowerCase().contains(searchContent)) {
                        searchIndexMap = new HashMap<>();
                        String searchName = name.toLowerCase();
                        int diffIndex = 0;
                        while (searchName.contains(searchContent)) {
                            int index = searchName.indexOf(searchContent);
                            if (index >= 0 && (index + searchContent.length() - 1) < searchName.length()) {
                                int nextSearchIndex = (index + searchContent.length());
                                searchIndexMap.put((index + diffIndex), (nextSearchIndex - 1 + diffIndex));
                                if (nextSearchIndex < searchName.length()) {
                                    diffIndex += nextSearchIndex;
                                    searchName = searchName.substring(nextSearchIndex);
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    String showName = name;
                    switch (mViewType) {
                        case ConstantField.ViewType.BOX_FILE_LINEAR_VIEW:
                            if (nameMaxWidthLinear > 0 && name != null) {
                                HighlightString highlightName = FormatUtil.customizeFileEllipsize(name, nameMaxWidthLinear, holder.name.getPaint(), searchIndexMap);
                                showName = highlightName.getContent();
                                searchIndexMap = highlightName.getHighlightIndexMap();
                            }
                            break;
                        default:
                            break;
                    }
                    if (showName == null || TextUtils.isEmpty(showName) || searchIndexMap == null || searchIndexMap.size() <= 0) {
                        holder.name.setText(showName == null ? "" : showName);
                    } else {
                        StringBuilder htmlContentBuilder = new StringBuilder();
                        String normalHtmlHead = "<font color='#333333'>";
                        String highlightHtmlHead = "<font color='#337aff'>";
                        String htmlTail = "</font>";
                        int showNameLength = showName.length();
                        boolean[] highLightArray = new boolean[showNameLength];
                        for (int i = 0; i < showNameLength; i++) {
                            highLightArray[i] = false;
                        }
                        Set<Map.Entry<Integer, Integer>> entrySet = searchIndexMap.entrySet();
                        for (Map.Entry<Integer, Integer> entry : entrySet) {
                            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                                int firstIndex = entry.getKey();
                                int lastIndex = entry.getValue();
                                if (firstIndex >= 0 && firstIndex <= lastIndex && lastIndex < showNameLength) {
                                    for (int i = firstIndex; i <= lastIndex; i++) {
                                        highLightArray[i] = true;
                                    }
                                }
                            }
                        }
                        boolean initState = highLightArray[0];
                        int index = 0;
                        for (int i = 1; i < showNameLength; i++) {
                            if (initState != highLightArray[i]) {
                                htmlContentBuilder.append(initState ? highlightHtmlHead : normalHtmlHead);
                                htmlContentBuilder.append(showName.substring(index, i));
                                htmlContentBuilder.append(htmlTail);
                                index = i;
                                initState = highLightArray[i];
                            }
                        }
                        htmlContentBuilder.append(initState ? highlightHtmlHead : normalHtmlHead);
                        htmlContentBuilder.append(showName.substring(index));
                        htmlContentBuilder.append(htmlTail);
                        holder.name.setText(Html.fromHtml(htmlContentBuilder.toString()));
                    }
                }
                if (holder.sizeDate != null) {
                    String content = "";
                    switch (mViewType) {
                        case ConstantField.ViewType.BOX_FILE_LINEAR_VIEW:
                            StringBuilder sizeAndDate = new StringBuilder();
                            String date = FormatUtil.formatTime(customizeFile.getTimestamp(), ConstantField.TimeStampFormat.FILE_API_MINUTE_FORMAT);
                            if (!TextUtils.isEmpty(date)) {
                                sizeAndDate.append(date);
                            }
                            long fileSize = customizeFile.getSize();
                            if (/*fileSize > 0 || */!ConstantField.MimeType.FOLDER.equalsIgnoreCase(mime)) {
                                String size = FormatUtil.formatSimpleSize(fileSize, ConstantField.SizeUnit.FORMAT_1F);
                                if (!TextUtils.isEmpty(size)) {
                                    sizeAndDate.append("  ");
                                    sizeAndDate.append(size);
                                }
                            }
                            content = sizeAndDate.toString();
                            break;
                        default:
                            break;
                    }
                    holder.sizeDate.setText(content);
                }
                if (showTypeMap != null && showTypeMap.containsKey(position)) {
                    Integer showType = showTypeMap.get(position);
                    if (showType != null) {
                        if (isDialog) {
                            if (holder.mark != null) {
                                holder.mark.setVisibility(View.GONE);
                            }
                            if (holder.markDialog != null) {
                                holder.markDialog.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (holder.markDialog != null) {
                                holder.markDialog.setVisibility(View.GONE);
                            }
                            if (holder.mark != null) {
                                setMarkPattern(holder.mark, showType, (!TextUtils.isEmpty(mime)
                                        && mime.equalsIgnoreCase(ConstantField.MimeType.FOLDER)));
                            }
                        }
                    }
                }

                String mimeType = FileUtil.getMimeTypeByPath(name);
                if (mimeType.contains("image") || mimeType.contains("video")){
                    String thumbPath = FileListUtil.getThumbPath(mContext,customizeFile.getId());
                    if (!TextUtils.isEmpty(thumbPath)){
                        GlideUtil.load(thumbPath, holder.icon);
                    } else {
                        //获取缩略图
                        ThumbManager.getInstance().insertItem(customizeFile.getId(), TransferHelper.FROM_FILE);
                        ThumbManager.getInstance().start();
                    }
                }

                if (holder.markContainer != null) {
                    holder.markContainer.setTag(position);
                    holder.markContainer.setOnClickListener(this);
                }
            }
        }
    }

    public void setOnItemClickListener(FileAdapter.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void changeEditStatus(int showType) {
        if (!(showType == ConstantField.ShowType.NORMAL || showType == ConstantField.ShowType.EDIT || showType == ConstantField.ShowType.SELECT)) {
            return;
        }
        if (showTypeMap != null) {
            for (Map.Entry<Integer, Integer> showTypeEntry : showTypeMap.entrySet()) {
                if (showTypeEntry != null) {
                    Integer key = showTypeEntry.getKey();
                    Integer value = showTypeEntry.getValue();
                    if (key != null && (value == null || value != showType)) {
                        showTypeMap.put(key, showType);
                    }
                }
            }
        }
    }

    public void updateData(List<CustomizeFile> customizeFiles, boolean isEdit) {
        mCustomizeFileList = customizeFiles;
        List<Integer> selectIndex = null;
        if (showTypeMap != null) {
            selectIndex = getSelectPosition();
            try {
                showTypeMap.clear();
            } catch (Exception e) {
                e.printStackTrace();
                showTypeMap = null;
            }
        }
        if (showTypeMap == null) {
            showTypeMap = new HashMap<>();
        }
        if (customizeFiles != null) {
            int size = customizeFiles.size();
            for (int i = 0; i < size; i++) {
                showTypeMap.put(i, (isEdit ? ((selectIndex != null && selectIndex.contains(i))
                        ? ConstantField.ShowType.SELECT : ConstantField.ShowType.EDIT) : ConstantField.ShowType.NORMAL));
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<CustomizeFile> customizeFiles, boolean isEdit, String searchContent) {
        this.searchContent = searchContent;
        if (searchContent != null) {
            this.searchContent = searchContent.toLowerCase();
        }
        updateData(customizeFiles, isEdit);
    }

    public void updateData(List<CustomizeFile> customizeFiles, List<Integer> selectPositionList) {
        if (selectPositionList == null) {
            updateData(customizeFiles, false);
        } else {
            mCustomizeFileList = customizeFiles;
            if (showTypeMap != null) {
                try {
                    showTypeMap.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                    showTypeMap = null;
                }
            }
            if (showTypeMap == null) {
                showTypeMap = new HashMap<>();
            }
            if (customizeFiles != null) {
                int size = customizeFiles.size();
                for (int i = 0; i < size; i++) {
                    showTypeMap.put(i, (selectPositionList.contains(i) ? ConstantField.ShowType.SELECT : ConstantField.ShowType.EDIT));
                }
            }
            notifyDataSetChanged();
        }
    }

    public void updateData(List<CustomizeFile> customizeFiles, List<Integer> selectPositionList, String searchContent) {
        this.searchContent = searchContent;
        if (searchContent != null) {
            this.searchContent = searchContent.toLowerCase();
        }
        updateData(customizeFiles, selectPositionList);
    }

    @Nullable
    public List<Integer> getSelectPosition() {
        List<Integer> positions = null;
        if (showTypeMap != null) {
            Set<Map.Entry<Integer, Integer>> entrySet = showTypeMap.entrySet();
            for (Map.Entry<Integer, Integer> entry : entrySet) {
                if (entry != null && entry.getKey() != null) {
                    int showType = entry.getValue();
                    switch (showType) {
                        case ConstantField.ShowType.EDIT:
                            if (positions == null) {
                                positions = new ArrayList<>();
                            }
                            break;
                        case ConstantField.ShowType.SELECT:
                            if (positions == null) {
                                positions = new ArrayList<>();
                            }
                            positions.add(entry.getKey());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return positions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Logger.d(TAG, "on create view holder");
        View view = null;
        ViewHolder holder = null;
        switch (mViewType) {
            case ConstantField.ViewType.BOX_FILE_LINEAR_VIEW:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_list_item, parent, false);
                holder = new ViewHolder(view);
                ViewHolder finalHolder = holder;
                if (!isNameMaxWidthLinearInit && holder.name != null) {
                    isNameMaxWidthLinearInit = true;
                    holder.name.post(() -> {
                        int width = finalHolder.name.getWidth();
                        if (width > 0) {
                            nameMaxWidthLinear = width;
                        }
                    });
                }
                break;
            default:
                view = new View(mContext);
                holder = new ViewHolder(view);
                break;
        }
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Logger.d(TAG, "on bind view holder, position: " + position);
        if (holder instanceof ViewHolder) {
            holder.itemView.setTag(position);
            generateViewHolderData((ViewHolder) holder, position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull @NotNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else if (((String)payloads.get(0)).equals("refresh_thumb")){
            //刷新缩略图
            CustomizeFile customizeFile = mCustomizeFileList.get(position);
            String thumbPath = FileListUtil.getThumbPath(mContext,customizeFile.getId());
            if (!TextUtils.isEmpty(thumbPath) && holder instanceof ViewHolder){
                GlideUtil.loadNoCache(thumbPath, ((ViewHolder) holder).icon);
            }
        }
    }

    @Override
    public int getItemCount() {
        return (mCustomizeFileList == null ? 0 : mCustomizeFileList.size());
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof ViewHolder) {
            Object positionTag = holder.itemView.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                generateViewHolderData((ViewHolder) holder, position);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return mViewType;
    }

    @Override
    public void onClick(View v) {
        Logger.d("zfy","files/adapter onClick");
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                boolean isEnable = true;
                if (!isDialog && showTypeMap != null && showTypeMap.containsKey(position)) {
                    Integer showType = showTypeMap.get(position);
                    if (showType != null) {
                        switch (showType) {
                            case ConstantField.ShowType.NORMAL:
                                if (isEditOnly || v.getId() == R.id.eulix_item_mark_container) {
                                    changeEditStatus(ConstantField.ShowType.EDIT);
                                } else {
                                    break;
                                }
                                changeSelectStatus(true, position);
                                Integer currentShowTypeNormal = showTypeMap.get(position);
                                if (currentShowTypeNormal != null) {
                                    setMarkPattern(v, currentShowTypeNormal);
                                }
                                break;
                            case ConstantField.ShowType.EDIT:
                                changeSelectStatus(true, position);
                                Integer currentShowTypeEdit = showTypeMap.get(position);
                                if (currentShowTypeEdit != null) {
                                    setMarkPattern(v, currentShowTypeEdit);
                                }
                                break;
                            case ConstantField.ShowType.SELECT:
                                changeSelectStatus(false, position);
                                Integer currentShowTypeSelect = showTypeMap.get(position);
                                if (currentShowTypeSelect != null) {
                                    setMarkPattern(v, currentShowTypeSelect);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position, isEnable);
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        boolean isHandle = false;
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                boolean isEnable = true;
                if (!isDialog && showTypeMap != null && showTypeMap.containsKey(position)) {
                    Integer showType = showTypeMap.get(position);
                    if (showType != null) {
                        switch (showType) {
                            case ConstantField.ShowType.NORMAL:
                                changeEditStatus(ConstantField.ShowType.EDIT);
                                changeSelectStatus(true, position);
                                Integer currentShowTypeNormal = showTypeMap.get(position);
                                if (currentShowTypeNormal != null) {
                                    setMarkPattern(v, currentShowTypeNormal);
                                }
                                break;
                            case ConstantField.ShowType.EDIT:
                                changeSelectStatus(true, position);
                                Integer currentShowTypeEdit = showTypeMap.get(position);
                                if (currentShowTypeEdit != null) {
                                    setMarkPattern(v, currentShowTypeEdit);
                                }
                                break;
                            case ConstantField.ShowType.SELECT:
                                changeSelectStatus(false, position);
                                Integer currentShowTypeSelect = showTypeMap.get(position);
                                if (currentShowTypeSelect != null) {
                                    setMarkPattern(v, currentShowTypeSelect);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                isHandle = true;
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position, isEnable);
                }
            }
        }
        return isHandle;
    }

    public List<CustomizeFile> getmCustomizeFileList() {
        return mCustomizeFileList;
    }

    public FooterView getFooterView() {
        return footerView;
    }

    public void setFooterView(FooterView footerView) {
        this.footerView = footerView;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public boolean isLoadingEnable() {
        return loadingEnable;
    }

    public void setLoadingEnable(boolean loadingEnable) {
        this.loadingEnable = loadingEnable;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon, mark, markDialog;
        private TextView name, sizeDate;
        private FrameLayout markContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.eulix_item_icon);
            name = itemView.findViewById(R.id.eulix_item_name);
            sizeDate = itemView.findViewById(R.id.eulix_item_size_date);
            mark = itemView.findViewById(R.id.eulix_item_mark);
            markDialog = itemView.findViewById(R.id.eulix_item_mark_dialog);
            markContainer = itemView.findViewById(R.id.eulix_item_mark_container);
        }
    }

    public static class ItemDecoration extends RecyclerView.ItemDecoration {
        private Paint paint;
        private int orientation;
        private int dividerWidth;

        public ItemDecoration(int orientation, int dividerWidth, @ColorInt int dividerColor) {
            this.orientation = orientation;
            this.dividerWidth = dividerWidth;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(dividerColor);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawHorizontal(Canvas canvas, RecyclerView parent) {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getMeasuredHeight() - parent.getPaddingBottom();
            final int childSize = parent.getChildCount();
            for (int i = 0; i < (childSize - 1); i++) {
                final View child = parent.getChildAt(i);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                final int left = child.getRight() + layoutParams.rightMargin;
                final int right = left + dividerWidth;
                if (paint != null) {
                    canvas.drawRect(left, top, right, bottom, paint);
                }
            }
        }

        private void drawVertical(Canvas canvas, RecyclerView parent) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getMeasuredWidth() - parent.getPaddingRight();
            final int childSize = parent.getChildCount();
            for (int i = 0; i < (childSize - 1); i++) {
                final View child = parent.getChildAt(i);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                final int top = child.getBottom() + layoutParams.bottomMargin;
                final int bottom = top + dividerWidth;
                if (paint != null) {
                    canvas.drawRect(left, top, right, bottom, paint);
                }
            }
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);
            if (orientation == RecyclerView.HORIZONTAL) {
                drawHorizontal(c, parent);
            } else {
                drawVertical(c, parent);
            }
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (orientation == RecyclerView.HORIZONTAL) {
                outRect.set(0, 0, dividerWidth, 0);
            } else {
                outRect.set(0, 0, 0, dividerWidth);
            }
        }
    }
}
