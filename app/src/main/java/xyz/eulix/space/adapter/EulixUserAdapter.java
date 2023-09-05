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

package xyz.eulix.space.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.widgets.LeftSwipeScrollView;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/29 11:50
 */
public class EulixUserAdapter extends RecyclerView.Adapter<EulixUserAdapter.ViewHolder> implements View.OnClickListener/*, View.OnLongClickListener*/ {
    private static final String TAG = EulixUserAdapter.class.getSimpleName();
    private Context mContext;
    private boolean isAdmin;
    private int mViewType;
    private List<EulixUser> mEulixUserList;
    private ColorMatrix grayColorMatrix;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onLeftSwipeCallback(String viewUuid);
        void onMenuClick(int menuFunction, int position);
    }

    public EulixUserAdapter(Context context, int viewType, List<EulixUser> eulixUserList) {
        mContext = context;
        mViewType = viewType;
        mEulixUserList = eulixUserList;
        grayColorMatrix = new ColorMatrix();
        grayColorMatrix.setSaturation(0.0f);
    }

    public EulixUserAdapter(Context context, int viewType, List<EulixUser> eulixUserList, boolean isAdmin) {
        mContext = context;
        mViewType = viewType;
        mEulixUserList = eulixUserList;
        this.isAdmin = isAdmin;
        grayColorMatrix = new ColorMatrix();
        grayColorMatrix.setSaturation(0.0f);
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mEulixUserList != null && position >= 0 && mEulixUserList.size() > position) {
            EulixUser eulixUser = mEulixUserList.get(position);
            if (eulixUser != null) {
                Boolean isInternetAccess = eulixUser.getInternetAccess();
                holder.userNickname.setText(StringUtil.nullToEmpty(eulixUser.getNickName()));
                if (isInternetAccess == null || isInternetAccess) {
                    holder.userDescription.setVisibility(View.VISIBLE);
                    holder.userDescription.setText(generateBaseUrl(eulixUser.getUserDomain()));
                } else {
                    holder.userDescription.setText("");
                    holder.userDescription.setVisibility(View.GONE);
                }
                switch (getItemViewType(position)) {
                    case ConstantField.ViewType.BOX_SPACE_VIEW:
                        holder.administrator.setVisibility(View.GONE);
                        holder.myselfText.setVisibility(View.GONE);
                        holder.userNickname.setMaxWidth(Math.max((ViewUtils.getScreenWidth(mContext)
                                - mContext.getResources().getDimensionPixelSize(R.dimen.dp_181)), 0));
                        holder.userDescription.setMaxWidth(Math.max((ViewUtils.getScreenWidth(mContext)
                                - mContext.getResources().getDimensionPixelSize(R.dimen.dp_181)), 0));
                        holder.memberIndicator.setVisibility(View.GONE);
                        Boolean isGranter = null;
                        String isBind = eulixUser.getBind();
                        if (isBind != null) {
                            isGranter = ("1".equals(isBind) || "-1".equals(isBind));
                        }
                        holder.spaceGrantLabel.setVisibility(isGranter == null ? View.INVISIBLE : View.VISIBLE);
                        boolean isActive = false;
                        boolean isOnline = false;
                        boolean isProgress = false;
                        boolean isDiskUninitialized = false;
                        boolean isValid = true;
                        switch (eulixUser.getSpaceState()) {
                            case ConstantField.EulixDeviceStatus.INVALID:
                                isValid = false;
                                break;
                            case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                                isActive = true;
                                break;
                            case ConstantField.EulixDeviceStatus.ACTIVE:
                                isActive = true;
                                isOnline = true;
                                break;
                            case ConstantField.EulixDeviceStatus.REQUEST_USE:
                            case ConstantField.EulixDeviceStatus.REQUEST_LOGIN:
                                isOnline = true;
                                break;
                            case ConstantField.EulixDeviceStatus.PROGRESS:
                                isOnline = true;
                                isProgress = true;
                                break;
                            case ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED:
                                isOnline = true;
                                isDiskUninitialized = true;
                                break;
                            case ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED:
                                isDiskUninitialized = true;
                                break;
                            default:
                                break;
                        }
                        holder.diskUninitializedIndicator.setVisibility(isDiskUninitialized ? View.VISIBLE : View.GONE);
                        setGrantLabelOnline(holder.spaceGrantLabel, isGranter, isOnline);
                        if (isProgress) {
                            setSpaceLoadingAnimation(holder.spaceLoadingAnimation, true);
                            holder.spaceContainer.setVisibility(View.GONE);
                        } else {
                            setSpaceLoadingAnimation(holder.spaceLoadingAnimation, false);
                            setStateOnline(holder.spaceIndicator, holder.spaceState, isActive, isValid);
                            holder.spaceContainer.setVisibility((!isValid || isActive || !isOnline) ? View.VISIBLE : View.GONE);
                        }
                        setBackgroundOnline(holder.userContainer, isOnline);
                        setAvatarOnline(holder.userAvatar, eulixUser.getAvatarPath(), isOnline);
                        setUserInfoOnline(holder.userNickname, isOnline);
                        setUserInfoOnline(holder.userDescription, isOnline);
                        holder.eulixUserContainer.registerCallback(viewUuid -> {
                            if (mOnItemClickListener != null) {
                                mOnItemClickListener.onLeftSwipeCallback(viewUuid);
                            }
                        });
                        holder.menuClear.setVisibility(View.VISIBLE);
                        int[] menuClearSize = ViewUtils.measureTextView(holder.menuClear);
                        int[] menuWidth = null;
                        if (menuClearSize != null && menuClearSize.length > 1) {
                            menuWidth = new int[] {menuClearSize[0]};
                        }
                        holder.eulixUserContainer.setMenuInfo(menuWidth, mContext.getResources().getDimensionPixelSize(R.dimen.dp_10), true);
                        holder.eulixUserContainer.resetScroll();
                        holder.menuClear.setTag(position);
                        holder.menuClear.setOnClickListener(this);
                        break;
                    case ConstantField.ViewType.CLIENT_MEMBER_VIEW:
                        holder.administrator.setVisibility(eulixUser.isAdmin() ? View.VISIBLE : View.GONE);
                        boolean isMyself = eulixUser.isMyself();
                        holder.myselfText.setVisibility(isMyself ? View.VISIBLE : View.GONE);
                        if (isMyself) {
                            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                            holder.myselfText.measure(measureSpec, measureSpec);
                            holder.userNickname.setMaxWidth(Math.max((ViewUtils.getScreenWidth(mContext)
                                    - mContext.getResources().getDimensionPixelSize(R.dimen.dp_159)
                                    - holder.myselfText.getMeasuredWidth()), 0));
                        } else {
                            holder.userNickname.setMaxWidth(Math.max((ViewUtils.getScreenWidth(mContext)
                                    - mContext.getResources().getDimensionPixelSize(R.dimen.dp_153)), 0));
                        }
                        holder.memberIndicator.setVisibility((isAdmin || eulixUser.isMyself())
                                ? View.VISIBLE : View.GONE);
                        holder.spaceGrantLabel.setVisibility(View.GONE);
                        holder.spaceContainer.setVisibility(View.GONE);
                        setBackgroundOnline(holder.userContainer, true);
                        setAvatarOnline(holder.userAvatar, eulixUser.getAvatarPath(), true);
                        setUserInfoOnline(holder.userNickname, true);
                        setUserInfoOnline(holder.userDescription, true);
                        holder.menuClear.setVisibility(View.GONE);
                        break;
                    case ConstantField.ViewType.MENU_LOGIN_MORE_SPACE_VIEW:
                        holder.userNickname.setText(R.string.login_more_space);
                        holder.userDescription.setVisibility(View.GONE);
                        holder.userAvatar.setImageResource(R.drawable.icon_add_oval_2x);
                        setUserInfoOnline(holder.userNickname, true);
                        setBackgroundOnline(holder.userContainer, false);
                        holder.menuClear.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void setBackgroundOnline(RelativeLayout container, boolean isOnline) {
        if (container != null) {
            container.setBackgroundResource(isOnline ? R.drawable.background_ffedf3ff_rectangle_10
                    : R.drawable.background_fff5f6fa_rectangle_10);
        }
    }

    private void setAvatarOnline(ImageView imageView, String avatarPath, boolean isOnline) {
        if (imageView != null && avatarPath != null) {
            if (FileUtil.existFile(avatarPath)) {
                GlideUtil.loadUserCircleFromPath(avatarPath, imageView);
            } else {
                imageView.setImageResource(R.drawable.avatar_default);
            }
            imageView.setColorFilter(isOnline ? null : new ColorMatrixColorFilter(grayColorMatrix));
        }
    }

    private void setUserInfoOnline(TextView textView, boolean isOnline) {
        if (textView != null) {
            textView.setTextColor(mContext.getResources().getColor(isOnline
                    ? R.color.black_ff333333 : R.color.gray_ffbcbfcd));
        }
    }

    private void setGrantLabelOnline(ImageView imageView, Boolean isGranter, boolean isOnline) {
        if (imageView != null && isGranter != null) {
            imageView.setImageResource(isGranter ? (isOnline ? R.drawable.granter_label_online_2x : R.drawable.granter_label_offline_2x)
                    : (isOnline ? R.drawable.grantee_label_online_2x : R.drawable.grantee_label_offline_2x));
        }
    }

    private void setStateOnline(View view, TextView textView, boolean isActive, boolean isValid) {
        if (view != null && textView != null) {
            view.setBackgroundResource(isActive ? R.drawable.background_ff43d9af_oval
                    : R.drawable.background_ffdfe0e5_oval);
            textView.setText(isValid ? (isActive ? R.string.using : R.string.offline) : R.string.expired);
        }
    }

    private void setSpaceLoadingAnimation(LottieAnimationView lottieAnimationView, boolean isProgress) {
        if (lottieAnimationView != null) {
            if (isProgress) {
                lottieAnimationView.setVisibility(View.VISIBLE);
                LottieUtil.loop(lottieAnimationView, "loading_button_blue.json");
            } else {
                LottieUtil.stop(lottieAnimationView);
                lottieAnimationView.setVisibility(View.GONE);
            }
        }
    }

    private boolean updateItem(int position) {
        boolean result = false;
        if (mEulixUserList != null && position >= 0 && mEulixUserList.size() > position) {
            EulixUser eulixUser = mEulixUserList.get(position);
            if (eulixUser != null) {
                switch (eulixUser.getSpaceState()) {
                    case ConstantField.EulixDeviceStatus.OFFLINE:
                    case ConstantField.EulixDeviceStatus.REQUEST_USE:
                    case ConstantField.EulixDeviceStatus.REQUEST_LOGIN:
                    case ConstantField.EulixDeviceStatus.ACTIVE:
                    case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                    case ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED:
                    case ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED:
                    case ConstantField.EulixDeviceStatus.INVALID:
                        result = true;
                        break;
                    default:
                        break;
                }
            }
        }
        return result;
    }

    private String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = "";
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (!TextUtils.isEmpty(baseUrl)) {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    public void updateState(int position) {
        if (getItemViewType(position) == ConstantField.ViewType.BOX_SPACE_VIEW && mEulixUserList != null && position >= 0) {
            int size = mEulixUserList.size();
            if (size > position) {
                for (int i = 0; i < size; i++) {
                    EulixUser eulixUser = mEulixUserList.get(i);
                    if (eulixUser != null) {
                        int state = eulixUser.getSpaceState();
                        if (position == i) {
                            if (state == ConstantField.EulixDeviceStatus.REQUEST_LOGIN || state == ConstantField.EulixDeviceStatus.REQUEST_USE) {
                                eulixUser.setSpaceState(ConstantField.EulixDeviceStatus.PROGRESS);
                            }
                        } else {
                            if (state == ConstantField.EulixDeviceStatus.PROGRESS) {
                                eulixUser.setSpaceState(eulixUser.isAdmin() ? ConstantField.EulixDeviceStatus.REQUEST_USE : ConstantField.EulixDeviceStatus.REQUEST_LOGIN);
                            }
                        }
                    }
                }
                notifyDataSetChanged();
            }
        }
    }

    public void updateStateAndRefresh(String boxUuid, String boxBind) {
        if (mViewType == ConstantField.ViewType.BOX_SPACE_VIEW && mEulixUserList != null && boxUuid != null && boxBind != null) {
            for (EulixUser eulixUser : mEulixUserList) {
                if (eulixUser != null) {
                    int state = eulixUser.getSpaceState();
                    if (boxUuid.equals(eulixUser.getUuid()) && boxBind.equals(eulixUser.getBind())) {
                        if (state == ConstantField.EulixDeviceStatus.REQUEST_LOGIN || state == ConstantField.EulixDeviceStatus.REQUEST_USE) {
                            eulixUser.setSpaceState(ConstantField.EulixDeviceStatus.PROGRESS);
                        }
                    } else {
                        if (state == ConstantField.EulixDeviceStatus.PROGRESS) {
                            eulixUser.setSpaceState(eulixUser.isAdmin() ? ConstantField.EulixDeviceStatus.REQUEST_USE : ConstantField.EulixDeviceStatus.REQUEST_LOGIN);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public void updateItem(View view, int position) {
        if (getItemViewType(position) == ConstantField.ViewType.BOX_SPACE_VIEW && view != null && mEulixUserList != null && position >= 0 && mEulixUserList.size() > position) {
            EulixUser eulixUser = mEulixUserList.get(position);
            if (eulixUser != null) {
                boolean isActive = false;
                boolean isOnline = false;
                boolean isProgress = false;
                boolean isValid = true;
                switch (eulixUser.getSpaceState()) {
                    case ConstantField.EulixDeviceStatus.INVALID:
                        isValid = false;
                        break;
                    case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                        isActive = true;
                        break;
                    case ConstantField.EulixDeviceStatus.ACTIVE:
                        isActive = true;
                        isOnline = true;
                        break;
                    case ConstantField.EulixDeviceStatus.REQUEST_USE:
                    case ConstantField.EulixDeviceStatus.REQUEST_LOGIN:
                        isOnline = true;
                        break;
                    case ConstantField.EulixDeviceStatus.PROGRESS:
                        isOnline = true;
                        isProgress = true;
                        break;
                    default:
                        break;
                }
                LottieAnimationView spaceLoadingAnimation = view.findViewById(R.id.space_loading_animation);
                LinearLayout spaceContainer = view.findViewById(R.id.space_container);
                View spaceIndicator = view.findViewById(R.id.space_indicator);
                TextView spaceState = view.findViewById(R.id.space_state);
                if (isProgress) {
                    setSpaceLoadingAnimation(spaceLoadingAnimation, true);
                    spaceContainer.setVisibility(View.GONE);
                } else {
                    setSpaceLoadingAnimation(spaceLoadingAnimation, false);
                    setStateOnline(spaceIndicator, spaceState, isActive, isValid);
                    spaceContainer.setVisibility((!isValid || isActive || !isOnline) ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateData(List<EulixUser> eulixUsers) {
        if (eulixUsers == null) {
            eulixUsers = new ArrayList<>();
        }
        mEulixUserList = eulixUsers;
        notifyDataSetChanged();
    }

    public void updateData(List<EulixUser> eulixUsers, boolean isAdmin) {
        this.isAdmin = isAdmin;
        updateData(eulixUsers);
    }

    public void resetMenuScroll(View view, String viewUuid) {
        if (view != null) {
            switch (mViewType) {
                case ConstantField.ViewType.BOX_SPACE_VIEW:
                    LeftSwipeScrollView eulixUserContainer = view.findViewById(R.id.eulix_user_container);
                    if (eulixUserContainer != null) {
                        eulixUserContainer.resetScroll(viewUuid);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Logger.d(TAG, "on create view holder");
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.eulix_user_item, parent, false), mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Logger.d(TAG, "on bind view holder, position: " + position);
        holder.itemView.setTag(position);
        holder.userContainer.setTag(position);
        holder.userContainer.setOnClickListener(this);
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mEulixUserList == null ? 0 : mEulixUserList.size());
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = mViewType;
        if (position >= 0 && mEulixUserList != null && mEulixUserList.size() > position) {
            EulixUser eulixUser = mEulixUserList.get(position);
            if (eulixUser != null) {
                switch (eulixUser.getMenuType()) {
                    case EulixUser.MENU_TYPE_LOGIN_MORE_SPACE:
                        viewType = ConstantField.ViewType.MENU_LOGIN_MORE_SPACE_VIEW;
                        break;
                    default:
                        break;
                }
            }
        }
        return viewType;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Object positionTag = holder.itemView.getTag();
        if (positionTag instanceof Integer) {
            int position = (int) positionTag;
            generateViewHolderData(holder, position);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                if (mOnItemClickListener != null) {
                    boolean isHandle = true;
                    switch (getItemViewType(position)) {
                        case ConstantField.ViewType.BOX_SPACE_VIEW:
                            isHandle = updateItem(position);
                            break;
                        default:
                            break;
                    }
                    if (isHandle) {
                        switch (v.getId()) {
                            case R.id.menu_clear:
                                mOnItemClickListener.onMenuClick(EulixUser.MENU_TYPE_LOGIN_MORE_SPACE, position);
                                break;
                            default:
                                mOnItemClickListener.onItemClick(v, position);
                                break;
                        }
                    }
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private LeftSwipeScrollView eulixUserContainer;
        private TextView spaceState, userNickname, userDescription;
        private ImageView userAvatar, administrator, memberIndicator, myself, spaceGrantLabel;
        private TextView diskUninitializedIndicator;
        private TextView myselfText;
        private LottieAnimationView spaceLoadingAnimation;
        private RelativeLayout userContainer;
        private LinearLayout spaceContainer;
        private View spaceIndicator;
        private TextView menuClear;

        public ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            eulixUserContainer = itemView.findViewById(R.id.eulix_user_container);
            diskUninitializedIndicator = itemView.findViewById(R.id.disk_uninitialized_indicator);
            userContainer = itemView.findViewById(R.id.user_container);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            administrator = itemView.findViewById(R.id.administrator);
            userNickname = itemView.findViewById(R.id.user_nickname);
            myself = itemView.findViewById(R.id.myself);
            myselfText = itemView.findViewById(R.id.myself_text);
            userDescription = itemView.findViewById(R.id.user_description);
            spaceContainer = itemView.findViewById(R.id.space_container);
            spaceIndicator = itemView.findViewById(R.id.space_indicator);
            spaceState = itemView.findViewById(R.id.space_state);
            memberIndicator = itemView.findViewById(R.id.member_indicator);
            spaceGrantLabel = itemView.findViewById(R.id.space_grant_label);
            spaceLoadingAnimation = itemView.findViewById(R.id.space_loading_animation);
            menuClear = itemView.findViewById(R.id.menu_clear);
            userContainer.setLayoutParams(new FrameLayout.LayoutParams(Math.max((ViewUtils.getScreenWidth(context)
                    - 2 * context.getResources().getDimensionPixelSize(R.dimen.dp_10)), 0)
                    , FrameLayout.LayoutParams.MATCH_PARENT));
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
