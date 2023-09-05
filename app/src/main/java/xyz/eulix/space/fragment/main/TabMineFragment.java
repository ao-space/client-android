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

package xyz.eulix.space.fragment.main;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsFragment;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.EulixNotificationEvent;
import xyz.eulix.space.event.StorageInfoResponseEvent;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.presenter.TabMinePresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.ui.EulixWebViewActivity;
import xyz.eulix.space.ui.mine.AboutUsActivity;
import xyz.eulix.space.ui.mine.DeviceManageActivity;
import xyz.eulix.space.ui.mine.EulixSettingsActivity;
import xyz.eulix.space.ui.mine.MessageCenterActivity;
import xyz.eulix.space.ui.mine.UserInfoActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.RingProgressBar;

/**
 * Author:      Zhu Fuyu
 * Description: 主页-我的
 * History:     2021/7/16
 */
public class TabMineFragment extends AbsFragment<TabMinePresenter.ITabMine, TabMinePresenter> implements TabMinePresenter.ITabMine, View.OnClickListener {
    private static final int SECOND_UNIT = 1000;
    private static final int UPDATE_AVATAR = 1;
    private static final int QUERY_MESSAGE_READ = UPDATE_AVATAR + 1;
    private EulixMainActivity parentActivity;
    private FrameLayout statusBarContainer;
    private RelativeLayout layoutUserInfo;
    private RelativeLayout messageCenterEntrance;
    private View messageCenterIndicator;
    private LinearLayout copyUserDomainContainer;
    private TextView userDomain;
    private RelativeLayout layoutDeviceManage;
    private LinearLayout layoutAbout;
    private LinearLayout layoutSettings;
    private LinearLayout layoutHelpCenter;
    private LinearLayout layoutMail;
    private TextView tvNickName;
    private TextView tvSignature;
    private ImageView imageHeader;
    private RingProgressBar ringProgressBar;
    private TextView tvTotalStorage;
    private TextView tvUsedStorage;
    private TextView tvUnusedStorage;


    private String mNickName = null;
    private String mSignature = null;
    private String mAvatarPath = null;
    private long mAvatarSize = 0L;
    private boolean isCreated = false;
    private boolean isReadingMessage;
    private boolean isNeedReadMessage;
    private TabMineHandler mHandler;

    static class TabMineHandler extends Handler {
        private WeakReference<TabMineFragment> tabMineFragmentWeakReference;

        public TabMineHandler(TabMineFragment fragment) {
            tabMineFragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            TabMineFragment fragment = tabMineFragmentWeakReference.get();
            if (fragment == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case UPDATE_AVATAR:
                        fragment.updateAvatar(null);
                        sendEmptyMessageDelayed(UPDATE_AVATAR, SECOND_UNIT);
                        break;
                    case QUERY_MESSAGE_READ:
                        if (fragment.presenter != null) {
                            if (fragment.isReadingMessage) {
                                fragment.isNeedReadMessage = true;
                            } else {
                                fragment.isNeedReadMessage = false;
                                fragment.isReadingMessage = true;
                                fragment.presenter.queryMessageAllRead();
                            }
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCreated = true;
    }

    @Override
    public void initRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.tab_mine_fragment_layout, container, false);
    }

    @NotNull
    @Override
    public TabMinePresenter createPresenter() {
        return new TabMinePresenter();
    }

    @Override
    public void initData() {
        obtainParentActivity();
        mHandler = new TabMineHandler(this);
    }

    @Override
    public void initView(@Nullable View root) {
        if (root == null) {
            return;
        }
        statusBarContainer = root.findViewById(R.id.status_bar_container);
        layoutUserInfo = root.findViewById(R.id.layout_user_info);
        messageCenterEntrance = root.findViewById(R.id.message_center_entrance);
        messageCenterIndicator = root.findViewById(R.id.message_center_indicator);
        copyUserDomainContainer = root.findViewById(R.id.copy_user_domain_container);
        userDomain = root.findViewById(R.id.user_domain);
        layoutDeviceManage = root.findViewById(R.id.layout_device_manage);
        layoutAbout = root.findViewById(R.id.layout_about);
        layoutSettings = root.findViewById(R.id.layout_settings);
        layoutHelpCenter = root.findViewById(R.id.layout_help_center);
        layoutMail = root.findViewById(R.id.layout_mail);
        ringProgressBar = root.findViewById(R.id.ring_progress);
        tvTotalStorage = root.findViewById(R.id.tv_total_size);
        tvUsedStorage = root.findViewById(R.id.tv_used_storage);
        tvUnusedStorage = root.findViewById(R.id.tv_unused_storage);
        tvNickName = root.findViewById(R.id.tv_nick_name);
        tvSignature = root.findViewById(R.id.tv_signature);
        imageHeader = root.findViewById(R.id.img_header);
        EventBusUtil.register(this);
    }

    @Override
    public void initViewData() {
        if (parentActivity != null) {
            statusBarContainer.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ViewUtils.getStatusBarHeight(parentActivity)));
            userDomain.setMaxWidth(Math.max((ViewUtils.getScreenWidth(parentActivity)
                    - parentActivity.getResources().getDimensionPixelSize(R.dimen.dp_126)), 0));
        }

        presenter.updateStorageInfo();
    }

    @Override
    public void initEvent() {
        layoutUserInfo.setOnClickListener(this);
        messageCenterEntrance.setOnClickListener(this);
        layoutDeviceManage.setOnClickListener(this);
        layoutAbout.setOnClickListener(this);
        layoutSettings.setOnClickListener(this);
        layoutHelpCenter.setOnClickListener(this);
        layoutMail.setOnClickListener(this);
        copyUserDomainContainer.setOnClickListener(this);
    }

    private void updateSpaceProgress(long usedSize, long totalSize) {
        usedSize = Math.max(usedSize, 0);
        totalSize = Math.max(totalSize, usedSize);
        if (totalSize == 0) {
            tvTotalStorage.setText("--");
            tvUsedStorage.setText("--");
            tvUnusedStorage.setText("--");
        } else {
            String totalSizeStr = FormatUtil.formatSimpleSize(totalSize, ConstantField.SizeUnit.FORMAT_1F);
            String usedSizeStr = FormatUtil.formatSimpleSize(usedSize, ConstantField.SizeUnit.FORMAT_1F);
            String unusedSizeStr = FormatUtil.formatSimpleSize((totalSize - usedSize), ConstantField.SizeUnit.FORMAT_1F);

            tvTotalStorage.setText(totalSizeStr);
            tvUsedStorage.setText(usedSizeStr);
            tvUnusedStorage.setText(unusedSizeStr);
            int usedProgress = (int) (usedSize * 100 / totalSize);
            ringProgressBar.setProgress(usedProgress);
        }
    }

    private void updateAvatar(String avatarPath) {
        if (avatarPath == null) {
            if (presenter != null) {
                UserInfo userInfo = presenter.getActiveUserInfo();
                if (userInfo != null) {
                    avatarPath = userInfo.getAvatarPath();
                }
            }
        }
        long fileSize = FileUtil.getFileSize(avatarPath);
        if (mAvatarPath == null || !mAvatarPath.equals(avatarPath) || mAvatarSize != fileSize) {
            mAvatarPath = avatarPath;
            mAvatarSize = fileSize;
            if (avatarPath != null && !TextUtils.isEmpty(avatarPath)) {
                if (FileUtil.existFile(avatarPath)) {
                    GlideUtil.loadCircleFromPath(avatarPath, imageHeader);
                } else if (parentActivity != null) {
                    UserInfoUtil.getHeaderImage(parentActivity);
                }
            } else {
                imageHeader.setImageResource(R.drawable.icon_user_header_default);
            }
        }
    }

    public void handleMimeInfo(boolean isChange) {
        if (presenter != null) {
            EulixBoxInfo eulixBoxInfo = presenter.getBoxInfo();
            if (presenter.isPhysicalDevice()) {
                if (eulixBoxInfo != null) {
                    updateSpaceProgress(eulixBoxInfo.getUsedSize(), eulixBoxInfo.getTotalSize());
                } else {
                    updateSpaceProgress(0, 0);
                }
            } else {
                UserInfo userInfo = presenter.getUserInfo();
                if (userInfo != null) {
                    updateSpaceProgress(userInfo.getUsedSize(), userInfo.getTotalSize());
                } else {
                    updateSpaceProgress(0, 0);
                }
            }
            String nickName = null;
            String signature = null;
            String avatarPath = null;
            String domain = presenter.getActiveBoxDomain();
            UserInfo userInfo = presenter.getActiveUserInfo();
            AOSpaceAccessBean aoSpaceAccessBean = presenter.getActiveAOSpaceAccessBean();
            if (userInfo != null) {
                nickName = userInfo.getNickName();
                signature = userInfo.getSignature();
                avatarPath = userInfo.getAvatarPath();
            }
            if (mNickName == null || !mNickName.equals(nickName)) {
                mNickName = nickName;
                tvNickName.setText(nickName == null ? "" : nickName);
            }
            if (mSignature == null || !mSignature.equals(signature)) {
                mSignature = signature;
                tvSignature.setText(signature == null ? "" : signature);
                if (!TextUtils.isEmpty(mSignature)) {
                    tvSignature.setVisibility(View.VISIBLE);
                } else {
                    tvSignature.setVisibility(View.GONE);
                }
            }
            if (domain != null) {
                while ((domain.startsWith(":") || domain.startsWith("/")) && domain.length() > 1) {
                    domain = domain.substring(1);
                }
                if (TextUtils.isEmpty(domain)) {
                    domain = "https://ao.space/";
                } else {
                    if (!(domain.startsWith("http://") || domain.startsWith("https://"))) {
                        domain = "https://" + domain;
                    }
                    if (!domain.endsWith("/")) {
                        domain = domain + "/";
                    }
                }
            }
            userDomain.setText(StringUtil.nullToEmpty(domain));
            boolean isShowCopyUserDomain = true;
            if (aoSpaceAccessBean != null) {
                Boolean isInternetAccess = aoSpaceAccessBean.getInternetAccess();
                isShowCopyUserDomain = (isInternetAccess == null || isInternetAccess);
            }
            copyUserDomainContainer.setVisibility(isShowCopyUserDomain ? View.VISIBLE : View.GONE);
            updateAvatar(avatarPath);

            if (mHandler != null) {
                mHandler.sendEmptyMessage(QUERY_MESSAGE_READ);
            }
        }
    }

    @Override
    public void onRefreshMessageAllRead(boolean isAllRead) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (messageCenterIndicator != null) {
                    messageCenterIndicator.setVisibility(isAllRead ? View.GONE : View.VISIBLE);
                }
                isReadingMessage = false;
                if (isNeedReadMessage) {
                    mHandler.sendEmptyMessage(QUERY_MESSAGE_READ);
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(UserInfoEvent event) {
        Logger.d("zfy", "receive UserInfoEvent type = " + event.type);
        if (event.type == UserInfoEvent.TYPE_NAME) {
            //更新昵称
            tvNickName.setText(StringUtil.nullToEmpty(event.nickName));
        } else if (event.type == UserInfoEvent.TYPE_SIGN) {
            //更新签名
            tvSignature.setText(StringUtil.nullToEmpty(event.signature));
            if (!TextUtils.isEmpty(event.signature)) {
                tvSignature.setVisibility(View.VISIBLE);
            } else {
                tvSignature.setVisibility(View.GONE);
            }
        } else if (event.type == UserInfoEvent.TYPE_HEADER) {
            //更新头像
            if (FileUtil.existFile(event.headerPath)) {
                GlideUtil.loadCircleFromPath(event.headerPath, imageHeader);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StorageInfoResponseEvent event) {
        if (event != null && parentActivity != null) {
            String boxUuid = event.getBoxUuid();
            if (presenter != null && presenter.isPhysicalDevice() && boxUuid != null && boxUuid.equals(EulixSpaceDBUtil.queryAvailableBoxUuid(parentActivity.getApplicationContext()))) {
                updateSpaceProgress(event.getUsedSize(), event.getTotalSize());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EulixNotificationEvent event) {
        if (event != null && mHandler != null) {
            mHandler.sendEmptyMessage(QUERY_MESSAGE_READ);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_user_info:
                Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                startActivity(intent);
                break;
            case R.id.message_center_entrance:
                Intent messageCenterIntent = new Intent(getActivity(), MessageCenterActivity.class);
                startActivity(messageCenterIntent);
                break;
            case R.id.layout_device_manage:
                Intent intentDeviceManage = new Intent(getActivity(), DeviceManageActivity.class);
                startActivity(intentDeviceManage);
                break;
            case R.id.layout_about:
                Intent intentAboutUs = new Intent(getActivity(), AboutUsActivity.class);
                startActivity(intentAboutUs);
                break;
            case R.id.layout_settings:
                Intent intentSettings = new Intent(getActivity(), EulixSettingsActivity.class);
                startActivity(intentSettings);
                break;
            case R.id.copy_user_domain_container:
                if (presenter != null && presenter.copyWebUrl(userDomain.getText().toString())) {
                    showPureTextToast(R.string.copy_to_clipboard_success_2);
                } else {
                    showPureTextToast(R.string.copy_to_clipboard_failed);
                }
                break;
            case R.id.layout_help_center:
                String url = DebugUtil.getOfficialEnvironmentWeb() + (FormatUtil.isChinese(FormatUtil.getLocale(parentActivity)
                        , false) ? ConstantField.URL.SUPPORT_HELP_API : ConstantField.URL.EN_SUPPORT_HELP_API);
                EulixWebViewActivity.startWeb(getActivity(), getResources().getString(R.string.help_center), url);
                break;
            case R.id.layout_mail:
                //跳转到发送邮件
                String mailUrl = "mailto:service@ao.space";
                Uri uri = Uri.parse(mailUrl);
                Intent data = new Intent(Intent.ACTION_SENDTO, uri);
                startActivity(data);
                break;
            default:
                break;
        }
    }

    private void obtainParentActivity() {
        if (parentActivity == null) {
            FragmentActivity activity = getActivity();
            if (activity instanceof EulixMainActivity) {
                parentActivity = (EulixMainActivity) activity;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        obtainParentActivity();
        handleMimeInfo(false);
        presenter.checkCacheSize(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isCreated) {
            return;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBusUtil.unRegister(this);
    }

    @Override
    public void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }
}