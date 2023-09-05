package xyz.eulix.space.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.event.AccessTokenResultEvent;
import xyz.eulix.space.event.MemberListEvent;
import xyz.eulix.space.event.MemberResultEvent;
import xyz.eulix.space.event.SpecificBoxOnlineRequestEvent;
import xyz.eulix.space.event.SpecificBoxOnlineResponseEvent;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.presenter.AOCompletePresenter;
import xyz.eulix.space.ui.bind.DiskInitializationActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.gesture.LeftFlingGestureDetector;

public class AOCompleteActivity extends AbsActivity<AOCompletePresenter.IAOComplete, AOCompletePresenter> implements AOCompletePresenter.IAOComplete {
    private static final String BOX_UUID = "box_uuid";
    private static final String BOX_BIND = "box_bind";
    private static final String AVATAR_URL = "avatar_url";
    private static final int MESSAGE_ONLINE_REQUEST = 1;
    private static final int MESSAGE_ACCESS_TOKEN_REQUEST = MESSAGE_ONLINE_REQUEST + 1;
    private static final int MESSAGE_MEMBER_LIST_REQUEST = MESSAGE_ACCESS_TOKEN_REQUEST + 1;
    private ImageView aoAvatar;
    private TextView aoNickname;
    private TextView aoDomain;
    private LinearLayout startContainer;
    private LeftFlingGestureDetector leftFlingGestureDetector;
    private String mBoxUuid;
    private String mBoxBind;
    private String mSpecificOnlineRequestId;
    private String mAvatarUrl;
    private boolean isReady;
    private AOCompleteHandler mHandler;

    static class AOCompleteHandler extends Handler {
        private WeakReference<AOCompleteActivity> aoCompleteActivityWeakReference;

        public AOCompleteHandler(AOCompleteActivity activity) {
            aoCompleteActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AOCompleteActivity activity = aoCompleteActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case MESSAGE_ONLINE_REQUEST:
                        activity.requestOnline();
                        break;
                    case MESSAGE_ACCESS_TOKEN_REQUEST:
                        activity.obtainAccessToken();
                        break;
                    case MESSAGE_MEMBER_LIST_REQUEST:
                        activity.memberListRequest();
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_ao_complete);
        aoAvatar = findViewById(R.id.ao_avatar);
        aoNickname = findViewById(R.id.ao_nickname);
        aoDomain = findViewById(R.id.ao_domain);
        startContainer = findViewById(R.id.start_container);
    }

    @Override
    public void initData() {
        mHandler = new AOCompleteHandler(this);
        handleIntent(getIntent());
        isReady = false;
        mSpecificOnlineRequestId = null;
        float minDistance = (ViewUtils.getScreenWidth(this) / 3.0f);
        leftFlingGestureDetector = new LeftFlingGestureDetector(this, minDistance, minDistance, minDistance, new LeftFlingGestureDetector.DetectorListener() {
            @Override
            public void onSingleClick() {
                // Do nothing
            }

            @Override
            public void onScroll(float dx, float dy, float vx, float vy) {
                startMain();
            }
        });
    }

    @Override
    public void initViewData() {
        if (presenter != null) {
            updateAODomainPattern();
            EulixUser eulixUser = null;
            if ("1".equals(mBoxBind) || "-1".equals(mBoxBind)) {
                eulixUser = presenter.generateEulixUser(DataUtil.getClientUuid(getApplicationContext()), mBoxUuid, mBoxBind);
            } else {
                eulixUser = presenter.generateEulixUserWithLoginIdentity(mBoxBind, mBoxUuid, mBoxBind);
            }
            if (eulixUser != null) {
                aoNickname.setText(StringUtil.nullToEmpty(eulixUser.getNickName()));
            }
        }
        boolean isAvatarHandle = false;
        if (StringUtil.isNonBlankString(mAvatarUrl)) {
            String avatarHttpUrl = FormatUtil.generateHttpImageUrlString(mAvatarUrl);
            if (FormatUtil.isHttpUrlString(avatarHttpUrl)) {
                isAvatarHandle = true;
                GlideUtil.loadUserCircleFromUrl(avatarHttpUrl, aoAvatar);
            }
        }
        if (!isAvatarHandle) {
            aoAvatar.setImageResource(R.drawable.avatar_default);
        }
    }

    @Override
    public void initEvent() {
        prepareRequestOnline(0L);
        prepareObtainAccessToken(0L);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            if (intent.hasExtra(BOX_UUID)) {
                mBoxUuid = intent.getStringExtra(BOX_UUID);
            }
            if (intent.hasExtra(BOX_BIND)) {
                mBoxBind = intent.getStringExtra(BOX_BIND);
            }
            if (intent.hasExtra(AVATAR_URL)) {
                mAvatarUrl = intent.getStringExtra(AVATAR_URL);
            }
        }
    }

    private void completeReady(boolean isSuccess) {
        if (startContainer != null) {
            startContainer.setVisibility(View.VISIBLE);
        }
        registerGesture();
        if (isSuccess) {
            prepareMemberListRequest(0L);
        }
    }

    private void registerGesture() {
        boolean isHandle = false;
        Window window = getWindow();
        if (window != null) {
            View decorView = window.getDecorView();
            if (decorView != null) {
                View rootView = decorView.getRootView();
                if (rootView != null) {
                    isHandle = (leftFlingGestureDetector != null);
                    rootView.setOnTouchListener((v, event) -> (leftFlingGestureDetector != null && leftFlingGestureDetector.onTouchEvent(event)));
                }
            }
        }
        if (!isHandle && startContainer != null) {
            startContainer.setOnClickListener(v -> startMain());
        }
    }

    private void prepareRequestOnline(long delayMillis) {
        if (mHandler == null) {
            requestOnline();
        } else {
            while (mHandler.hasMessages(MESSAGE_ONLINE_REQUEST)) {
                mHandler.removeMessages(MESSAGE_ONLINE_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_ONLINE_REQUEST, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_ONLINE_REQUEST);
            }
        }
    }

    private void requestOnline() {
        mSpecificOnlineRequestId = UUID.randomUUID().toString();
        EventBusUtil.post(new SpecificBoxOnlineRequestEvent(mBoxUuid, mBoxBind, mSpecificOnlineRequestId));
    }

    private void prepareObtainAccessToken(long delayMillis) {
        if (mHandler == null) {
            obtainAccessToken();
        } else {
            while (mHandler.hasMessages(MESSAGE_ACCESS_TOKEN_REQUEST)) {
                mHandler.removeMessages(MESSAGE_ACCESS_TOKEN_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_ACCESS_TOKEN_REQUEST, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_ACCESS_TOKEN_REQUEST);
            }
        }
    }

    /**
     * 请求token
     */
    private void obtainAccessToken() {
        Intent serviceIntent = new Intent(AOCompleteActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        if (mBoxUuid != null) {
            serviceIntent.putExtra(ConstantField.BOX_UUID, mBoxUuid);
        }
        if (mBoxBind != null) {
            serviceIntent.putExtra(ConstantField.BOX_BIND, mBoxBind);
        }
        serviceIntent.putExtra(ConstantField.FORCE, true);
        startService(serviceIntent);
    }

    private void prepareMemberListRequest(long delayMillis) {
        if (mHandler == null) {
            memberListRequest();
        } else {
            while (mHandler.hasMessages(MESSAGE_MEMBER_LIST_REQUEST)) {
                mHandler.removeMessages(MESSAGE_MEMBER_LIST_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_MEMBER_LIST_REQUEST, delayMillis);
            } else {
                mHandler.sendEmptyMessage(MESSAGE_MEMBER_LIST_REQUEST);
            }
        }
    }

    private void memberListRequest() {
        if (presenter != null) {
            EventBusUtil.post(new MemberListEvent(mBoxUuid, mBoxBind, presenter.getBoxDomain(mBoxUuid, mBoxBind), true));
        }
    }

    private void updateUserInformation() {
        EulixUser eulixUser = null;
        if (presenter != null) {
            if ("1".equals(mBoxBind) || "-1".equals(mBoxBind)) {
                eulixUser = presenter.generateEulixUser(DataUtil.getClientUuid(getApplicationContext()));
            } else {
                eulixUser = presenter.generateEulixUserWithLoginIdentity(mBoxBind);
            }
            updateUserInformation(eulixUser);
        }
    }

    private void updateUserInformation(EulixUser eulixUser) {
        if (eulixUser == null) {
            prepareMemberListRequest(10L);
        } else {
            if (aoAvatar != null) {
                String avatarPath = eulixUser.getAvatarPath();
                if (FileUtil.existFile(avatarPath)) {
                    GlideUtil.loadUserCircleFromPath(avatarPath, aoAvatar);
                } else {
                    aoAvatar.setImageResource(R.drawable.avatar_default);
                }
            }
            if (aoNickname != null) {
                aoNickname.setText(StringUtil.nullToEmpty(eulixUser.getNickName()));
            }
            updateAODomainPattern();
        }
    }

    private void updateAODomainPattern() {
        if (aoDomain != null && presenter != null) {
            AOSpaceAccessBean aoSpaceAccessBean = presenter.getSpecificAOSpaceAccessBean(mBoxUuid, mBoxBind);
            Boolean isInternetAccess = null;
            if (aoSpaceAccessBean != null) {
                isInternetAccess = aoSpaceAccessBean.getInternetAccess();
            }
            if (isInternetAccess == null || isInternetAccess) {
                aoDomain.setText(generateBaseUrl(presenter.getBoxDomain(mBoxUuid, mBoxBind)));
            } else {
                aoDomain.setText("");
            }
        }
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

    private void startMain() {
        AOSpaceUtil.prepareGoMain(getApplicationContext());
        EulixSpaceApplication.popAllOldActivity(this);
        Intent intent = new Intent(AOCompleteActivity.this, EulixMainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public AOCompletePresenter createPresenter() {
        return new AOCompletePresenter();
    }

    public static void startThisActivity(Context context, String boxUuid, String boxBind) {
        startThisActivity(context, boxUuid, boxBind, null);
    }

    public static void startThisActivity(Context context, String boxUuid, String boxBind, String avatarUrl) {
        if (context != null) {
            Intent intent = new Intent(context, AOCompleteActivity.class);
            if (boxUuid != null) {
                intent.putExtra(BOX_UUID, boxUuid);
            }
            if (boxBind != null) {
                intent.putExtra(BOX_BIND, boxBind);
            }
            if (avatarUrl != null) {
                intent.putExtra(AVATAR_URL, avatarUrl);
            }
            context.startActivity(intent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpecificBoxOnlineResponseEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            String requestId = event.getRequestId();
            Boolean isOnline = event.getOnline();
            if (mBoxUuid != null && mBoxUuid.equals(boxUuid) && mBoxBind != null && mBoxBind.equals(boxBind)
                    && (mSpecificOnlineRequestId == null || mSpecificOnlineRequestId.equals(requestId))) {
                mSpecificOnlineRequestId = null;
                if (!isReady) {
                    if (isOnline == null || isOnline) {
                        prepareRequestOnline(10 * ConstantField.TimeUnit.SECOND_UNIT);
                    } else {
                        isReady = true;
                        AOSpaceUtil.changeActiveBox(getApplicationContext(), boxUuid, boxBind, -1L, false);
                        completeReady(false);
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AccessTokenResultEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            Boolean result = event.getResult();
            long expireTimestamp = event.getExpireTimestamp();
            if (!isReady) {
                if (mBoxUuid != null && mBoxUuid.equals(boxUuid) && mBoxBind != null && mBoxBind.equals(boxBind)) {
                    if (result == null) {
                        prepareObtainAccessToken(10 * ConstantField.TimeUnit.SECOND_UNIT);
                    } else if (result) {
                        if (expireTimestamp <= System.currentTimeMillis()) {
                            prepareObtainAccessToken(0);
                        } else {
                            boolean isOnline = true;
                            if (presenter != null) {
                                isOnline = presenter.getBoxOnline(boxUuid, boxBind, true);
                            }
                            isReady = true;
                            AOSpaceUtil.changeActiveBox(getApplicationContext(), boxUuid, boxBind, expireTimestamp, isOnline);
                            completeReady(true);
                        }
                    } else {
                        isReady = true;
                        completeReady(false);
                    }
                } else {
                    prepareObtainAccessToken(0L);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MemberResultEvent event) {
        if (event != null) {
            updateUserInformation();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(UserInfoEvent event) {
        if (event.type == UserInfoEvent.TYPE_HEADER) {
            updateUserInformation();
        }
    }
}
