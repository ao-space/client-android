package xyz.eulix.space.ui.bind;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.event.CloseBannerEvent;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.bind.ProgressResult;
import xyz.eulix.space.presenter.AODeviceInitialPresenter;
import xyz.eulix.space.ui.AOSpaceInformationActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.banner.AdContentBean;
import xyz.eulix.space.view.banner.AutoScrollBanner;

public class AODeviceInitialActivity extends AbsActivity<AODeviceInitialPresenter.IAODeviceInitial, AODeviceInitialPresenter> implements AODeviceInitialPresenter.IAODeviceInitial
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback {
    private static final String IS_CONTAINER_STARTED = "is_container_started";
    private static final int TEST = 0;
    private static final int MESSAGE_PROGRESS_REQUEST = 1;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private Button functionText;
    private LinearLayout initialFailContainer;
    private TextView initialRetry;
    private TextView initialReturn;
    private LinearLayout initialProgressContainer;
    private ImageView initialLoading;
    private ImageView initialStep1Image;
    private TextView initialStep1Text;
    private ImageView initialStep2Image;
    private TextView initialStep2Text;
    private ImageView initialStep3Image;
    private TextView initialStep3Text;
    private ImageView initialStep4Image;
    private TextView initialStep4Text;
    private AutoScrollBanner initialAdvertisement;
    private List<AdContentBean> advertisementList;
    private AODeviceInitialHandler mHandler;
    private AODeviceDiscoveryManager mManager;
    private boolean isContainerStarted;
    private int step = 0;
    private long mExitTime = 0L;

    static class AODeviceInitialHandler extends Handler {
        private WeakReference<AODeviceInitialActivity> aoDeviceInitialActivityWeakReference;

        public AODeviceInitialHandler(AODeviceInitialActivity activity) {
            aoDeviceInitialActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AODeviceInitialActivity activity = aoDeviceInitialActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case TEST:
                        if (activity.step > 4) {
                            activity.step = -1;
                        } else {
                            activity.step += 1;
                            sendEmptyMessageDelayed(TEST, 2000);
                        }
                        activity.setInitialProgressPattern(activity.step);
                        break;
                    case MESSAGE_PROGRESS_REQUEST:
                        activity.handleProgressRequest();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_ao_device_initial);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        initialFailContainer = findViewById(R.id.initial_fail_container);
        initialRetry = findViewById(R.id.initial_retry);
        initialReturn = findViewById(R.id.initial_return);
        initialProgressContainer = findViewById(R.id.initial_progress_container);
        initialLoading = findViewById(R.id.initial_loading);
        initialStep1Image = findViewById(R.id.initial_step_1_image);
        initialStep1Text = findViewById(R.id.initial_step_1_text);
        initialStep2Image = findViewById(R.id.initial_step_2_image);
        initialStep2Text = findViewById(R.id.initial_step_2_text);
        initialStep3Image = findViewById(R.id.initial_step_3_image);
        initialStep3Text = findViewById(R.id.initial_step_3_text);
        initialStep4Image = findViewById(R.id.initial_step_4_image);
        initialStep4Text = findViewById(R.id.initial_step_4_text);
        initialAdvertisement = findViewById(R.id.initial_advertisement);
    }

    @Override
    public void initData() {
        mHandler = new AODeviceInitialHandler(this);
        handleIntent(getIntent());
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
        advertisementList = new ArrayList<>();
    }

    @Override
    public void initViewData() {
        title.setText("");
        back.setVisibility(View.GONE);
        functionText.setText(R.string.run_background);

        SpannableStringBuilder retrySpannableStringBuilder = new SpannableStringBuilder();
        String retryPart1 = getString(R.string.system_error_retry_part_1);
        String retryPart2 = getString(R.string.system_error_retry_part_2);
        String retryPart3 = getString(R.string.system_error_retry_part_3);
        retrySpannableStringBuilder.append(retryPart1);
        retrySpannableStringBuilder.append(retryPart2);
        retrySpannableStringBuilder.append(retryPart3);
        ForegroundColorSpan retryForegroundHighlightSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
        int retryHighlightStart = retryPart1.length();
        int retryHighlightEnd = retryHighlightStart + retryPart2.length();
        retrySpannableStringBuilder.setSpan(retryForegroundHighlightSpan, retryHighlightStart, retryHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        initialRetry.setText(retrySpannableStringBuilder);

        SpannableStringBuilder returnSpannableStringBuilder = new SpannableStringBuilder();
        String returnContent = getString(R.string.return_back);
        returnSpannableStringBuilder.append(returnContent);
        ClickableSpan returnClickableHighlightSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                handleFinish();
            }
        };
        ForegroundColorSpan returnForegroundHighlightSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
        int returnHighlightStart = 0;
        int returnHighlightEnd = returnContent.length();
        returnSpannableStringBuilder.setSpan(returnClickableHighlightSpan, returnHighlightStart, returnHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        returnSpannableStringBuilder.setSpan(returnForegroundHighlightSpan, returnHighlightStart, returnHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        initialReturn.setMovementMethod(LinkMovementMethod.getInstance());
        initialReturn.setText(returnSpannableStringBuilder);

        initAdvertisement();
        initialAdvertisement.init(advertisementList);
    }

    @Override
    public void initEvent() {
        functionText.setOnClickListener(this);
        initialRetry.setOnClickListener(this);
        handleStartDeviceInitialEvent();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            isContainerStarted = intent.getBooleanExtra(IS_CONTAINER_STARTED, false);
        }
    }

    private void initAdvertisement() {
        AdContentBean yourDataAdContentBean = new AdContentBean();
        yourDataAdContentBean.setAdId(UUID.randomUUID().toString());
        yourDataAdContentBean.setImgResId(R.drawable.image_your_data_2x);
        advertisementList.add(yourDataAdContentBean);

        AdContentBean encryptionPlatformAdContentBean = new AdContentBean();
        encryptionPlatformAdContentBean.setAdId(UUID.randomUUID().toString());
        encryptionPlatformAdContentBean.setImgResId(R.drawable.image_encryption_platform_2x);
        advertisementList.add(encryptionPlatformAdContentBean);

        AdContentBean multiTerminalAdContentBean = new AdContentBean();
        multiTerminalAdContentBean.setAdId(UUID.randomUUID().toString());
        multiTerminalAdContentBean.setImgResId(R.drawable.image_multi_terminal_2x);
        advertisementList.add(multiTerminalAdContentBean);
    }

    private void setInitialProgressPattern(int step) {
        if (functionText != null) {
            functionText.setVisibility(((step < 0) ? View.GONE : View.VISIBLE));
        }
        if (initialProgressContainer != null && initialFailContainer != null) {
            initialProgressContainer.setVisibility(((step < 0) ? View.GONE : View.VISIBLE));
            initialFailContainer.setVisibility(((step < 0) ? View.VISIBLE : View.GONE));
        }
        setStepPattern(initialStep1Image, initialStep1Text, (step >= 1));
        setStepPattern(initialStep2Image, initialStep2Text, (step >= 2));
        setStepPattern(initialStep3Image, initialStep3Text, (step >= 3));
        setStepPattern(initialStep4Image, initialStep4Text, (step >= 4));
        setInitialLoadingAnimation(step >= 0);
    }

    private void setInitialLoadingAnimation(boolean isAnimation) {
        if (initialLoading != null) {
            boolean hasAnimation = ViewUtils.hasAnim(initialLoading);
            if (!isAnimation) {
                if (hasAnimation) {
                    ViewUtils.clearAnim(initialLoading);
                }
            } else if (!hasAnimation) {
                ViewUtils.setLoadingAnim(this, initialLoading);
            }
        }
    }

    private void setStepPattern(ImageView imageView, TextView textView, boolean stepEnable) {
        if (imageView != null && textView != null) {
            imageView.setImageResource(stepEnable ? R.drawable.icon_progress_on_2x : R.drawable.icon_progress_off_2x);
            textView.setTextColor(getResources().getColor(stepEnable ? R.color.black_ff333333 : R.color.gray_ff85899c));
            textView.setTypeface(stepEnable ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    private void handleStartDeviceInitialEvent() {
        setInitialProgressPattern(0);
        boolean isHandle = false;
        if (mManager != null) {
            isHandle = mManager.request(activityId, UUID.randomUUID().toString(), (isContainerStarted
                    ? AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS
                    : AODeviceDiscoveryManager.STEP_BIND_COM_START), null);
        }
        if (!isHandle) {
            setInitialProgressPattern(-1);
        }
    }

    private void handleFinishDeviceInitialEvent() {
        setInitialLoadingAnimation(false);
        AOSpaceInformationActivity.administratorStartThisActivity(this, null);
    }

    private void handleFinish() {
        if (mManager != null) {
            mManager.finishSource();
            finish();
        } else {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    @Override
    protected void onDestroy() {
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
    public void onBackPressed() {
        confirmForceExit();
//        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            confirmForceExit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            handleFinish();
        }
    }

    private void handleStartResult(int code, String source) {
        if (code == ConstantField.KnownError.BindError.BOUND_CODE && ConstantField.KnownSource.AGENT.equals(source)) {
            if (mManager != null) {
                showImageTextToast(R.drawable.toast_refuse, R.string.binding_initializing_space_change_phone_hint);
                mManager.finishSource();
            }
            finish();
        } else if (code == ConstantField.KnownError.BindError.CONTAINER_STARTED && ConstantField.KnownSource.AGENT.equals(source)) {
            handleFinishDeviceInitialEvent();
        } else if (code == ConstantField.KnownError.BindError.CONTAINER_STARTING && ConstantField.KnownSource.AGENT.equals(source)) {
            prepareProgressRequest(0L);
        } else if (code >= 200 && code < 400) {
            prepareProgressRequest(3500L);
        } else {
            setInitialProgressPattern(-1);
        }
    }

    private void prepareProgressRequest(long delayMillis) {
        if (mHandler != null) {
            while (mHandler.hasMessages(MESSAGE_PROGRESS_REQUEST)) {
                mHandler.removeMessages(MESSAGE_PROGRESS_REQUEST);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_PROGRESS_REQUEST, delayMillis);
            } else if (delayMillis == 0L) {
                mHandler.sendEmptyMessage(MESSAGE_PROGRESS_REQUEST);
            }
        } else if (delayMillis >= 0) {
            handleProgressRequest();
        }
    }

    private void handleProgressRequest() {
        boolean isHandle = false;
        if (mManager != null) {
            isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS, null);
        }
        if (!isHandle) {
            prepareProgressRequest(2 * ConstantField.TimeUnit.SECOND_UNIT);
        }
    }

    private void handleProgressResult(int code, String source, ProgressResult progressResult) {
        boolean isHandle = false;
        if (code == ConstantField.KnownError.BindError.BOUND_CODE && ConstantField.KnownSource.AGENT.equals(source)) {
            if (mManager != null) {
                isHandle = true;
                showImageTextToast(R.drawable.toast_refuse, R.string.binding_initializing_space_change_phone_hint);
                mManager.finishSource();
                finish();
            }
        } else if (progressResult != null) {
            isHandle = true;
            int comStatus = progressResult.getComStatus();
            if (comStatus < 0 || comStatus == ProgressResult.COM_STATUS_CONTAINERS_DOWNLOADED) {
                isContainerStarted = false;
                handleStartDeviceInitialEvent();
            } else {
                switch (comStatus) {
                    case ProgressResult.COM_STATUS_CONTAINERS_STARTED:
                        handleFinishDeviceInitialEvent();
                        break;
                    case ProgressResult.COM_STATUS_CONTAINERS_START_FAILED:
                        isContainerStarted = false;
                        setInitialProgressPattern(-1);
                        break;
                    default:
                        setInitialProgressPattern(calculateStep(Math.max(Math.min(progressResult.getProgress(), 100), 0)));
                        prepareProgressRequest(3500L);
                        break;
                }
            }
        }
        if (!isHandle) {
            prepareProgressRequest(2 * ConstantField.TimeUnit.SECOND_UNIT);
        }
    }

    private int calculateStep(int progress) {
        double sideProp = (1 - ConstantField.GOLD_RATIO);
        double unit = 100 / (sideProp * 2 + 3);
        double sideUnit = unit * sideProp;
        int step = 0;
        double level = sideUnit;
        while (progress > level) {
            step += 1;
            level += unit;
        }
        return step;
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
    public AODeviceInitialPresenter createPresenter() {
        return new AODeviceInitialPresenter();
    }

    public static void startThisActivity(Context context, boolean isContainerStarted) {
        if (context != null) {
            Intent intent = new Intent(context, AODeviceInitialActivity.class);
            intent.putExtra(IS_CONTAINER_STARTED, isContainerStarted);
            context.startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.function_text:
                    String toastContent = getString(R.string.bind_communication_progress_background_hint_part_1)
                            + getString(R.string.bind_device)
                            + getString(R.string.bind_communication_progress_background_hint_part_2);
                    showImageTextToast(R.drawable.toast_tip, toastContent);
                    handleFinish();
                    break;
                case R.id.initial_retry:
                    handleStartDeviceInitialEvent();
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
        if (mHandler != null) {
            mHandler.post(() -> {
                switch (step) {
                    case AODeviceDiscoveryManager.STEP_BIND_COM_START:
                        handleStartResult(code, source);
                        break;
                    case AODeviceDiscoveryManager.STEP_BIND_COM_PROGRESS:
                        ProgressResult progressResult = null;
                        if (bodyJson != null) {
                            progressResult = new Gson().fromJson(bodyJson, ProgressResult.class);
                        }
                        handleProgressResult(code, source, progressResult);
                        break;
                    default:
                        break;
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CloseBannerEvent event) {
        boolean isClose = false;
        if (event != null) {
            String bannerId = event.getBannerId();
            if (bannerId != null && advertisementList != null) {
                for (AdContentBean adContentBean : advertisementList) {
                    if (adContentBean != null && bannerId.equals(adContentBean.getAdId())) {
                        isClose = true;
                        break;
                    }
                }
            }
        }
        if (isClose) {
            initialAdvertisement.setVisibility(View.INVISIBLE);
        }
    }
}
