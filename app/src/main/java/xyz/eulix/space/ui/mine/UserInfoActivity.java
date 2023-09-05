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
import android.app.Dialog;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.event.AccessInfoRequestEvent;
import xyz.eulix.space.event.AccessInfoResponseEvent;
import xyz.eulix.space.event.DomainEditEvent;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.presenter.UserInfoPresenter;
import xyz.eulix.space.ui.AOSpaceAccessActivity;
import xyz.eulix.space.util.CameraUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PermissionUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 个人信息页面
 * History:     2021/8/23
 */
public class UserInfoActivity extends AbsActivity<UserInfoPresenter.IUserInfo, UserInfoPresenter> implements UserInfoPresenter.IUserInfo, View.OnClickListener {
    private TitleBarWithSelect titleBar;
    private LinearLayout layoutHeader;
    private Dialog pictureFromDialog;
    private ImageView imgHeader;
    private RelativeLayout layoutNickname;
    private RelativeLayout layoutSignature;
    private View spaceAccessSplit;
    private RelativeLayout layoutSpaceAccess;
    private ImageView ivSpaceAccess;
    private LinearLayout spaceAccessContainer;
    private TextView labelLan;
    private TextView labelInternet;
    private ImageView domainSplit;
    private RelativeLayout layoutDomain;
    private TextView tvNickName;
    private TextView tvSignature;
    private TextView tvDomain;
    private boolean isBoxObserve;
    private String mNickName = null;
    private String mSignature = null;
    private ContentObserver boxObserver;
    private UserInfoHandler mHandler;
    private String accessInfoRequestUuid = null;

    static class UserInfoHandler extends Handler {
        private WeakReference<UserInfoActivity> userInfoActivityWeakReference;

        public UserInfoHandler(UserInfoActivity activity) {
            userInfoActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            UserInfoActivity activity = userInfoActivityWeakReference.get();
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
        setContentView(R.layout.activity_user_info);

        titleBar = findViewById(R.id.title_bar);
        layoutHeader = findViewById(R.id.layout_header);
        imgHeader = findViewById(R.id.img_header);
        layoutNickname = findViewById(R.id.layout_nick_name);
        layoutSignature = findViewById(R.id.layout_signature);
        spaceAccessSplit = findViewById(R.id.space_access_split);
        layoutSpaceAccess = findViewById(R.id.layout_space_access);
        ivSpaceAccess = findViewById(R.id.iv_space_access);
        spaceAccessContainer = findViewById(R.id.space_access_container);
        labelLan = findViewById(R.id.label_lan);
        labelInternet = findViewById(R.id.label_internet);
        domainSplit = findViewById(R.id.domain_split);
        layoutDomain = findViewById(R.id.layout_domain);
        tvNickName = findViewById(R.id.tv_nick_name);
        tvSignature = findViewById(R.id.tv_signature);
        tvDomain = findViewById(R.id.tv_domain);

        EventBusUtil.register(this);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initData() {
        accessInfoRequestUuid = null;
        mHandler = new UserInfoHandler(this);
        boxObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                handleUserInfo(true);
            }
        };
    }

    @Override
    public void initViewData() {
        titleBar.setTitle(R.string.space_information);
        domainSplit.setVisibility(View.VISIBLE);
        layoutDomain.setVisibility(View.VISIBLE);
    }

    @Override
    public void initEvent() {
        layoutHeader.setOnClickListener(this);
        layoutNickname.setOnClickListener(this);
        layoutSignature.setOnClickListener(this);
        if (presenter != null && presenter.isActiveGranter()) {
            layoutDomain.setOnClickListener(this);
        } else {
            layoutDomain.setClickable(false);
        }
        if (presenter != null && presenter.isActiveAdminGranter()) {
            layoutSpaceAccess.setOnClickListener(this);
        } else {
            layoutSpaceAccess.setClickable(false);
        }
    }

    private void handleUserInfo(boolean isChange) {
        if (presenter != null) {
            String nickName = null;
            String signature = null;
            UserInfo userInfo = presenter.getActiveUserInfo();
            if (userInfo != null) {
                nickName = userInfo.getNickName();
                signature = userInfo.getSignature();
                if (!isChange) {
                    String avatarPath = userInfo.getAvatarPath();
                    if (!TextUtils.isEmpty(avatarPath) && FileUtil.existFile(avatarPath)) {
                        GlideUtil.loadCircleFromPath(avatarPath, imgHeader);
                    } else {
                        UserInfoUtil.getHeaderImage(this);
                    }
                }
            }
            if (mNickName == null || !mNickName.equals(nickName)) {
                mNickName = nickName;
                if (StringUtil.isNonBlankString(nickName)) {
                    tvNickName.setText(nickName);
                } else {
                    tvNickName.setText(R.string.none);
                }
            }
            if (mSignature == null || !mSignature.equals(signature)) {
                mSignature = signature;
                if (StringUtil.isNonBlankString(signature)) {
                    tvSignature.setText(signature);
                } else {
                    tvSignature.setText(R.string.none);
                }
            }
            String userDomain = presenter.getUserDomain();
            if (userDomain != null) {
                if (!(userDomain.startsWith("http://") || userDomain.startsWith("https://"))) {
                    userDomain = "https://" + userDomain;
                }
                tvDomain.setText(userDomain);
            }
        }
    }

    private void updateAccessInfoPattern(AOSpaceAccessBean aoSpaceAccessBean) {
        Boolean isAtLeastOneOn = null;
        Boolean isLanAccess = null;
        Boolean isP2PAccess = null;
        Boolean isInternetAccess = null;
        if (aoSpaceAccessBean != null) {
            isLanAccess = aoSpaceAccessBean.getLanAccess();
            isP2PAccess = aoSpaceAccessBean.getP2PAccess();
            isInternetAccess = aoSpaceAccessBean.getInternetAccess();
            if (isLanAccess != null || isP2PAccess != null || isInternetAccess != null) {
                isAtLeastOneOn = ((isLanAccess != null && isLanAccess) || (isP2PAccess != null && isP2PAccess)
                        || (isInternetAccess != null && isInternetAccess));
            }
        } else {
            isAtLeastOneOn = false;
        }
        boolean isAdminGranter = false;
        if (presenter != null) {
            isAdminGranter = presenter.isActiveAdminGranter();
        }
        if (spaceAccessSplit != null) {
            spaceAccessSplit.setVisibility((aoSpaceAccessBean != null ? View.VISIBLE : View.GONE));
        }
        if (layoutSpaceAccess != null) {
            layoutSpaceAccess.setVisibility((aoSpaceAccessBean != null ? View.VISIBLE : View.GONE));
            layoutSpaceAccess.setClickable((aoSpaceAccessBean != null && isAdminGranter));
        }
        if (ivSpaceAccess != null) {
            ivSpaceAccess.setVisibility(isAdminGranter ? View.VISIBLE : View.INVISIBLE);
        }
        if (spaceAccessContainer != null && isAtLeastOneOn != null) {
            spaceAccessContainer.setVisibility(isAtLeastOneOn ? View.VISIBLE : View.GONE);
        }
        if (labelLan != null) {
            if (aoSpaceAccessBean == null) {
                labelLan.setVisibility(View.GONE);
            } else if (isLanAccess != null) {
                labelLan.setVisibility(isLanAccess ? View.VISIBLE : View.GONE);
            }
        }
        if (labelInternet != null) {
            if (aoSpaceAccessBean == null) {
                labelInternet.setVisibility(View.GONE);
            } else if (isInternetAccess != null) {
                labelInternet.setVisibility(isInternetAccess ? View.VISIBLE : View.GONE);
            }
        }
        if (domainSplit != null) {
            if (aoSpaceAccessBean == null) {
                domainSplit.setVisibility(View.VISIBLE);
            } else if (isInternetAccess != null) {
                domainSplit.setVisibility(isInternetAccess ? View.VISIBLE : View.GONE);
            }
        }
        if (layoutDomain != null) {
            if (aoSpaceAccessBean == null) {
                layoutDomain.setVisibility(View.VISIBLE);
            } else if (isInternetAccess != null) {
                layoutDomain.setVisibility(isInternetAccess ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(UserInfoEvent event) {
        Logger.d("zfy", "receive UserInfoEvent type = " + event.type);
        if (event.type == UserInfoEvent.TYPE_NAME) {
            //更新昵称
            String nickName = UserInfoUtil.getNickname(this);
            if (StringUtil.isNonBlankString(nickName)) {
                tvNickName.setText(nickName);
            } else {
                tvNickName.setText(R.string.none);
            }
        } else if (event.type == UserInfoEvent.TYPE_SIGN) {
            //更新签名
            String signature = UserInfoUtil.getSignature(this);
            if (StringUtil.isNonBlankString(signature)) {
                tvSignature.setText(signature);
            } else {
                tvSignature.setText(R.string.none);
            }
        } else if (event.type == UserInfoEvent.TYPE_HEADER) {
            //更新头像
            if (FileUtil.existFile(event.headerPath)) {
                GlideUtil.loadCircleFromPath(event.headerPath, imgHeader);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DomainEditEvent event) {
        if (event != null) {
            switch (event.getResponseCode()) {
                case ConstantField.KnownError.DomainError.DOMAIN_MODIFY_ONCE_YEAR_CODE:
                    showImageTextToast(R.drawable.toast_refuse, R.string.modify_once_year);
                    break;
                default:
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AccessInfoResponseEvent event) {
        if (event != null) {
            InternetServiceConfigResult internetServiceConfigResult = event.getInternetServiceConfigResult();
            String requestUuid = event.getRequestUuid();
            if (requestUuid != null && requestUuid.equals(accessInfoRequestUuid)) {
                accessInfoRequestUuid = null;
                if (internetServiceConfigResult != null) {
                    AOSpaceAccessBean aoSpaceAccessBean = new AOSpaceAccessBean();
                    aoSpaceAccessBean.setLanAccess(internetServiceConfigResult.getEnableLAN());
                    aoSpaceAccessBean.setP2PAccess(internetServiceConfigResult.getEnableP2P());
                    aoSpaceAccessBean.setInternetAccess(internetServiceConfigResult.getEnableInternetAccess());
                    updateAccessInfoPattern(aoSpaceAccessBean);
                }
            }
        }
    }

    @NotNull
    @Override
    public UserInfoPresenter createPresenter() {
        return new UserInfoPresenter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (boxObserver != null && !isBoxObserve) {
            isBoxObserve = true;
            getContentResolver().registerContentObserver(EulixSpaceDBManager.BOX_URI, true, boxObserver);
        }
        handleUserInfo(false);
        AOSpaceAccessBean aoSpaceAccessBean = null;
        EulixBoxBaseInfo eulixBoxBaseInfo = null;
        if (presenter != null) {
            aoSpaceAccessBean = presenter.getActiveAOSpaceAccessBean();
            eulixBoxBaseInfo = presenter.getActiveBoxBaseInfo();
        }
        updateAccessInfoPattern(aoSpaceAccessBean);
        UserInfoUtil.getUserInfo(this, true);
        if (eulixBoxBaseInfo != null) {
            accessInfoRequestUuid = UUID.randomUUID().toString();
            EventBusUtil.post(new AccessInfoRequestEvent(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), accessInfoRequestUuid));
        }
    }

    @Override
    protected void onStop() {
        if (isBoxObserve && boxObserver != null) {
            getContentResolver().unregisterContentObserver(boxObserver);
        }
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_header:
                showChangeHeaderDialog();
                break;
            case R.id.layout_nick_name:
                Intent intent = new Intent(UserInfoActivity.this, NickOrSignatureEditActivity.class);
                intent.putExtra("type", NickOrSignatureEditActivity.TYPE_NICK);
                startActivity(intent);
                break;
            case R.id.layout_signature:
                Intent intent1 = new Intent(UserInfoActivity.this, NickOrSignatureEditActivity.class);
                intent1.putExtra("type", NickOrSignatureEditActivity.TYPE_SIGNATURE);
                startActivity(intent1);
                break;
            case R.id.layout_space_access:
                AOSpaceAccessActivity.startThisActivity(UserInfoActivity.this, false);
                break;
            default:
                break;
        }
    }

    //弹出头像选择弹框
    private void showChangeHeaderDialog() {
        if (pictureFromDialog == null) {
            pictureFromDialog = new BottomDialog(this);
            View view = View.inflate(this, R.layout.picture_from_choose_dialog_layout, null);
            pictureFromDialog.setContentView(view);

            TextView btnFromCamera = view.findViewById(R.id.btn_camera);
            TextView btnFromAlbum = view.findViewById(R.id.btn_album);
            TextView btnCancel = view.findViewById(R.id.btn_cancel);
            btnFromCamera.setOnClickListener((v) -> {
                if (PermissionUtils.isPermissionGranted(UserInfoActivity.this, PermissionUtils.PERMISSION_CAMERA)) {
                    CameraUtil.openCamera(this);
                    pictureFromDialog.dismiss();
                } else {
                    PermissionUtils.requestPermissionWithNotice(UserInfoActivity.this, PermissionUtils.PERMISSION_CAMERA, new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            if (result) {
                                CameraUtil.openCamera(UserInfoActivity.this);
                                pictureFromDialog.dismiss();
                            }
                        }
                    });
                }
            });
            btnFromAlbum.setOnClickListener((v) -> {
                if (PermissionUtils.isPermissionGranted(UserInfoActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE)) {
                    CameraUtil.openGallery(this);
                    pictureFromDialog.dismiss();
                } else {
                    PermissionUtils.requestPermissionWithNotice(UserInfoActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE, new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            if (result) {
                                CameraUtil.openGallery(UserInfoActivity.this);
                                pictureFromDialog.dismiss();
                            }
                        }
                    });
                }
            });
            btnCancel.setOnClickListener((v) -> {
                pictureFromDialog.dismiss();
            });

        }
        pictureFromDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String path = "";
        String mOutputPath = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), "header_chosen.jpg").getPath();
        switch (requestCode) {
            case CameraUtil.REQUEST_GALLERY_CODE:
                if (data == null) {
                    return;
                }
                path = data.getStringExtra("path");
                Logger.d("zfy", "图片地址：" + path);
                callClipImage(path, mOutputPath);
                break;
            case CameraUtil.REQUEST_CAMERA_CODE:
                path = CameraUtil.getCameraBack();
                Logger.d("zfy", "照片地址：" + path);
                callClipImage(path, mOutputPath);
                break;
            default:
                break;
        }

        if (resultCode == Activity.RESULT_OK
                && data != null
                && requestCode == CameraUtil.REQUEST_CLIP_CODE) {
            //裁剪成功
            String pathClip = ClipImageActivity.ClipOptions.createFromBundle(data).getOutputPath();
            Logger.d("zfy", "pathClip=" + pathClip);
            showLoading(getString(R.string.uploading_avatar));
            presenter.updateHeader(this, pathClip);
        }

    }

    @Override
    public void onUpdateHeaderResult(Boolean result, String message) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                Logger.d("result = " + (result != null && result) + ",message = " + (message == null ? "null" : message));
                if (result == null) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(result ? R.drawable.toast_right : R.drawable.toast_wrong, result ? R.string.modify_success : R.string.modify_userinfo_failed);
                }
            });
        }
    }

    private void callClipImage(String inputPath, String outputPath) {
        if (presenter != null) {
            File inputFile = new File(inputPath);
            if (!TextUtils.isEmpty(inputPath) && inputFile.exists()) {
                ClipImageActivity.prepare()
                        .aspectX(1).aspectY(1)
                        .inputPath(inputPath).outputPath(outputPath)
                        .startForResult(this, CameraUtil.REQUEST_CLIP_CODE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        accessInfoRequestUuid = null;
        super.onDestroy();
        EventBusUtil.unRegister(this);
    }
}