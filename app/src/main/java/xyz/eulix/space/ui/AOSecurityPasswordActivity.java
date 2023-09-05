package xyz.eulix.space.ui;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.presenter.AOSecurityPasswordPresenter;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.EulixSixSecurityPasswordPointView;

public class AOSecurityPasswordActivity extends AbsActivity<AOSecurityPasswordPresenter.IAOSecurityPassword, AOSecurityPasswordPresenter> implements AOSecurityPasswordPresenter.IAOSecurityPassword
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, EulixSixSecurityPasswordPointView.IEulixSixSecurityPassword {
    private String activityId;
    private ImageButton backNoTitle;
    private ImageView titleHeaderImage;
    private TextView titleHeaderText;
    private TextView titleHeaderIntroduction;
    private FrameLayout securityPasswordContainer;
    private EulixSixSecurityPasswordPointView eulixSixSecurityPasswordPointView;
    private String mPasswordValue;
    private boolean isRepeat;
    private AODeviceDiscoveryManager mManager;
    private AOSecurityPasswordHandler mHandler;

    static class AOSecurityPasswordHandler extends Handler {
        private WeakReference<AOSecurityPasswordActivity> aoSecurityPasswordActivityWeakReference;

        public AOSecurityPasswordHandler(AOSecurityPasswordActivity activity) {
            aoSecurityPasswordActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AOSecurityPasswordActivity activity = aoSecurityPasswordActivityWeakReference.get();
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
        setContentView(R.layout.activity_ao_security_password);
        backNoTitle = findViewById(R.id.back_no_title);
        titleHeaderImage = findViewById(R.id.title_header_image);
        titleHeaderText = findViewById(R.id.title_header_text);
        titleHeaderIntroduction = findViewById(R.id.title_header_introduction);
        securityPasswordContainer = findViewById(R.id.security_password_container);

        eulixSixSecurityPasswordPointView = new EulixSixSecurityPasswordPointView(this, this);
    }

    @Override
    public void initData() {
        mHandler = new AOSecurityPasswordHandler(this);
        resetData();
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
    }

    @Override
    public void initViewData() {
        backNoTitle.setVisibility(View.VISIBLE);
        setTitleHeaderPattern();
    }

    @Override
    public void initEvent() {
        backNoTitle.setOnClickListener(this);
        eulixSixSecurityPasswordPointView.setFocus(true);
    }

    private void resetData() {
        mPasswordValue = null;
        isRepeat = false;
    }

    private void setTitleHeaderPattern() {
        if (titleHeaderImage != null) {
            titleHeaderImage.setImageResource(R.drawable.image_security_password_2x);
        }
        if (titleHeaderText != null) {
            titleHeaderText.setText(R.string.security_password);
        }
        if (titleHeaderIntroduction != null) {
            titleHeaderIntroduction.setVisibility(View.VISIBLE);
            titleHeaderIntroduction.setText(isRepeat ? R.string.security_password_repeat_introduction
                    : R.string.security_password_introduction);
        }
    }

    private void resetEulixSixSecurityPasswordPointPattern(boolean isGainFocus) {
        if (eulixSixSecurityPasswordPointView != null) {
            eulixSixSecurityPasswordPointView.setEnable(true);
            eulixSixSecurityPasswordPointView.resetPassword();
            if (isGainFocus) {
                eulixSixSecurityPasswordPointView.setFocus(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (eulixSixSecurityPasswordPointView != null) {
            eulixSixSecurityPasswordPointView.setFocus(false);
        }
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
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
    public AOSecurityPasswordPresenter createPresenter() {
        return new AOSecurityPasswordPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back_no_title:
                    finish();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFinish() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void onResponse(int code, String source, int step, String bodyJson) {
        // Do nothing
    }

    @Override
    public void onPrepared(View view) {
        if (securityPasswordContainer != null) {
            securityPasswordContainer.removeAllViews();
            if (view != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                        , getResources().getDimensionPixelSize(R.dimen.dp_55));
                securityPasswordContainer.addView(view, layoutParams);
            }
        }
    }

    @Override
    public void onComplete(@NotNull String passwordValue) {
        eulixSixSecurityPasswordPointView.setEnable(false);
        eulixSixSecurityPasswordPointView.setFocus(false);
        if (!isRepeat) {
            mPasswordValue = passwordValue;
            isRepeat = true;
            setTitleHeaderPattern();
            resetEulixSixSecurityPasswordPointPattern(true);
        } else {
            boolean isRepeatSame = (passwordValue.equals(mPasswordValue));
            resetData();
            setTitleHeaderPattern();
            if (isRepeatSame) {
                resetEulixSixSecurityPasswordPointPattern(false);
                if (mManager != null) {
                    mManager.setAdminPassword(passwordValue);
                }
                AOSpaceAccessActivity.startThisActivity(AOSecurityPasswordActivity.this, true);
            } else {
                showImageTextToast(R.drawable.toast_refuse, R.string.security_password_repeat_error);
                resetEulixSixSecurityPasswordPointPattern(true);
            }
        }
    }

    @Override
    public void onInserted(String currentValue) {
        // Do nothing
    }
}
