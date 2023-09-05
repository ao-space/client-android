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

package xyz.eulix.space.view;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import xyz.eulix.space.R;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.ViewUtils;

/**
 * Author:      Zhu Fuyu
 * Description: 带选择标签的标题栏
 * History:     2021/7/21
 */
public class TitleBarWithSelect extends RelativeLayout {
    private TextView tvTitle, tvLeft, tvRight;
    private ImageView btnBack;
    private boolean isSelectState = false;  //是否为选择状态（隐藏返回按键，显示“取消”、“全选”）
    private boolean hasSelectedAll = false; //是否已经全选
    private TitleBarClickListener listener;
    private boolean isDefaultShowAllSelect = false;   //是否默认显示“全选”按键
    private boolean showBackBtn = true;
    private OnClickBackListener mBackListener;
    private int nameMaxWidthFile = -1;
    private TextView tvSubTitle;

    public static final int LOCATION_TITLE_ELLIPSIZE_START = 1;
    public static final int LOCATION_TITLE_ELLIPSIZE_MIDDLE = LOCATION_TITLE_ELLIPSIZE_START + 1;
    public static final int LOCATION_TITLE_ELLIPSIZE_END = LOCATION_TITLE_ELLIPSIZE_MIDDLE + 1;
    public static final int LOCATION_TITLE_ELLIPSIZE_MARQUEE = LOCATION_TITLE_ELLIPSIZE_END + 1;

    public static final int CLICK_EVENT_CANCEL_SELECT = 0; //点击取消选择
    public static final int CLICK_EVENT_SELECT_ALL = 1; //点击全选
    public static final int CLICK_EVENT_SELECT_NULL = 2;    //点击全不选

    public static final int TITLE_TYPE_FILENAME = 1;

    public TitleBarWithSelect(Context context) {
        this(context, null);
    }

    public TitleBarWithSelect(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitleBarWithSelect(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.layout_title_view, this);
        tvTitle = viewGroup.findViewById(R.id.title_tv);
        tvSubTitle = viewGroup.findViewById(R.id.title_tv_sub);
        tvLeft = viewGroup.findViewById(R.id.title_text_left);
        tvRight = viewGroup.findViewById(R.id.title_text_right);
        btnBack = viewGroup.findViewById(R.id.title_btn_back);

        btnBack.setOnClickListener(v -> {
            if (mBackListener != null) {
                mBackListener.onClick();
            } else if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        });

        tvLeft.setOnClickListener(v -> {
            setSelectState(false);
            if (listener != null) {
                listener.onClickEvent(CLICK_EVENT_CANCEL_SELECT);
            }
        });

        tvRight.setOnClickListener(v -> {
            if (hasSelectedAll) {
                tvRight.setText(R.string.select_all);
                hasSelectedAll = !hasSelectedAll;
                if (listener != null) {
                    listener.onClickEvent(CLICK_EVENT_SELECT_NULL);
                }
            } else {
                tvRight.setText(R.string.select_none);
                hasSelectedAll = !hasSelectedAll;
                if (listener != null) {
                    listener.onClickEvent(CLICK_EVENT_SELECT_ALL);
                }
            }

        });
        nameMaxWidthFile = (ViewUtils.getScreenWidth(context) - context.getResources().getDimensionPixelSize(R.dimen.dp_100)
                - context.getResources().getDimensionPixelSize(R.dimen.dp_100));
        tvTitle.post(() -> {
            int width = tvTitle.getWidth();
            if (width > 0) {
                nameMaxWidthFile = width;
            }
        });
    }

    //设置否默认显示“全选”按键
    public void setDefaultShowAllSelect(boolean flag) {
        isDefaultShowAllSelect = flag;
        tvRight.setVisibility(flag ? VISIBLE : GONE);
    }

    //设置是否为已“全选”
    public void setHasSelectedAll(boolean hasSelectedAll) {
        this.hasSelectedAll = hasSelectedAll;
        if (hasSelectedAll) {
            tvRight.setText(R.string.select_none);
        } else {
            tvRight.setText(R.string.select_all);
        }
    }

    //设置选择状态（显示“取消”按钮）
    public void setSelectState(boolean isSelectState) {
        if (isSelectState) {
            btnBack.setVisibility(GONE);
            tvLeft.setVisibility(VISIBLE);
            tvRight.setVisibility(VISIBLE);
        } else {
            if (showBackBtn) {
                btnBack.setVisibility(VISIBLE);
            }
            tvLeft.setVisibility(GONE);
            if (!isDefaultShowAllSelect) {
                tvRight.setVisibility(GONE);
            }
        }
        this.isSelectState = isSelectState;
    }

    //设置点击监听
    public void setClickListener(TitleBarClickListener listener) {
        this.listener = listener;
    }

    /**
     * 设置标题
     *
     * @param title
     * @param titleType
     */
    public void setTitle(String title, int titleType) {
        String nTitle = title;
        switch (titleType) {
            case TITLE_TYPE_FILENAME:
                if (nameMaxWidthFile > 0 && title != null) {
                    nTitle = FormatUtil.customizeFileEllipsize(title, nameMaxWidthFile, tvTitle.getPaint());
                }
                break;
            default:
                break;
        }
        setTitle(nTitle);
    }

    //设置标题
    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    /**
     * @param location LOCATION_TITLE_ELLIPSIZE_*
     */
    public void setTitleEllipsize(int location) {
        if (tvTitle != null) {
            switch (location) {
                case LOCATION_TITLE_ELLIPSIZE_START:
                    tvTitle.setEllipsize(TextUtils.TruncateAt.START);
                    break;
                case LOCATION_TITLE_ELLIPSIZE_MIDDLE:
                    tvTitle.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                    break;
                case LOCATION_TITLE_ELLIPSIZE_END:
                    tvTitle.setEllipsize(TextUtils.TruncateAt.END);
                    break;
                case LOCATION_TITLE_ELLIPSIZE_MARQUEE:
                    tvTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    break;
                default:
                    break;
            }
        }
    }

    //设置标题
    public void setTitle(int stringId) {
        tvTitle.setText(getResources().getString(stringId));
    }

    //设置副标题
    public void setSubTitle(String subTitle) {
        if (TextUtils.isEmpty(subTitle)) {
            tvSubTitle.setText("");
            tvSubTitle.setVisibility(GONE);
            return;
        } else {
            tvSubTitle.setText(subTitle);
            tvSubTitle.setVisibility(VISIBLE);
        }
    }

    //隐藏返回按钮
    public void hideBackButton() {
        showBackBtn = false;
        if (btnBack != null) {
            btnBack.setVisibility(GONE);
        }
    }

    public void setOnClickBackListener(OnClickBackListener listener) {
        this.mBackListener = listener;
    }

    public interface TitleBarClickListener {
        void onClickEvent(int clickEvent);
    }

    public interface OnClickBackListener {
        void onClick();
    }
}
