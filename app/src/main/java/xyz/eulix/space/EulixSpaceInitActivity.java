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

package xyz.eulix.space;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bridge.InitializeBridge;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.InitialResults;
import xyz.eulix.space.network.agent.PairingBoxInfo;
import xyz.eulix.space.network.agent.PasswordInfo;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.presenter.InitSpacePresenter;
import xyz.eulix.space.ui.AOCompleteActivity;
import xyz.eulix.space.ui.bind.DiskInitializationActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 18:31
 */
public class EulixSpaceInitActivity extends AbsActivity<InitSpacePresenter.IInitSpace, InitSpacePresenter> implements InitSpacePresenter.IInitSpace, View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, InitializeBridge.InitializeSinkCallback {
    private static final int STEP_INITIAL = 0;
    private static final int STEP_SPACE_READY_CHECK = STEP_INITIAL + 1;
    private static final int STEP_DISK_RECOGNITION = STEP_SPACE_READY_CHECK + 1;
    private static final int STEP_DISK_MANAGEMENT_LIST = STEP_DISK_RECOGNITION + 1;
    private String activityId;
    private ImageButton back;
    private TextView title, initDomain;
    private TextView enterSpace;
    private LottieAnimationView enterSpaceLoading;
    private LinearLayout enterSpaceContainer;
    private EulixSpaceInitHandler mHandler;
    private AODeviceDiscoveryManager mManager;
    private long mExitTime = 0L;
    private boolean isInitSuccess;
    private int mStep;
    private ReadyCheckResult mReadyCheckResult;

    static class EulixSpaceInitHandler extends Handler {
        private WeakReference<EulixSpaceInitActivity> eulixSpaceInitActivityWeakReference;

        public EulixSpaceInitHandler(EulixSpaceInitActivity activity) {
            eulixSpaceInitActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixSpaceInitActivity activity = eulixSpaceInitActivityWeakReference.get();
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
        setContentView(R.layout.init_space_main);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        initDomain = findViewById(R.id.init_domain);
        enterSpaceContainer = findViewById(R.id.loading_button_container);
        enterSpaceLoading = findViewById(R.id.loading_animation);
        enterSpace = findViewById(R.id.loading_content);
    }

    @Override
    public void initData() {
        StatusBarUtil.setStatusBarColor(Color.WHITE, this);
        isInitSuccess = false;
        mStep = STEP_INITIAL;
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.space_initialization);
        back.setVisibility(View.GONE);
        if (mManager != null) {
            PairingBoxInfo pairingBoxInfo = mManager.getPairingBoxInfo();
            if (pairingBoxInfo != null) {
                initDomain.setText(StringUtil.nullToEmpty(pairingBoxInfo.getUserDomain()));
            }
        }
    }

    @Override
    public void initEvent() {
        enterSpaceContainer.setOnClickListener(this);
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public InitSpacePresenter createPresenter() {
        return new InitSpacePresenter();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
    }

    private void setLoadingPattern(boolean isClickable) {
        if (enterSpaceContainer != null) {
            enterSpaceContainer.setClickable(isClickable);
        }
        if (enterSpaceLoading != null && enterSpace != null) {
            if (isClickable) {
                LottieUtil.stop(enterSpaceLoading);
                enterSpaceLoading.setVisibility(View.GONE);
                enterSpace.setText(R.string.next_step);
            } else {
                enterSpace.setText(isInitSuccess ? R.string.finishing_initialize : R.string.initialize_space);
                enterSpaceLoading.setVisibility(View.VISIBLE);
                LottieUtil.loop(enterSpaceLoading, "loading_button.json");
            }
        }
    }


    private void finishInit(Integer diskInitializeCode, Boolean diskInitializeNoMainStorage) {
        setLoadingPattern(true);
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new EulixSpaceInitHandler(this);
        handleIntent(getIntent());
        setLoadingPattern(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleRequestEvent() {
        boolean isHandle = (mManager != null);
        if (isHandle) {
            switch (mStep) {
                case STEP_INITIAL:
                    PasswordInfo passwordInfo = new PasswordInfo();
                    passwordInfo.setPassword(mManager.getAdminPassword());
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_INITIAL
                            , new Gson().toJson(passwordInfo, PasswordInfo.class));
                    break;
                case STEP_SPACE_READY_CHECK:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK, null);
                    break;
                case STEP_DISK_RECOGNITION:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_RECOGNITION, null);
                    break;
                case STEP_DISK_MANAGEMENT_LIST:
                    isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST, null);
                    break;
                default:
                    isHandle = false;
                    break;
            }
        }
        if (!isHandle) {
            handleErrorEvent();
        }
    }

    private void handleErrorEvent() {
        setLoadingPattern(true);
        showServerExceptionToast();
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
        if (mManager != null) {
            mManager.finishSource();
        } else {
            confirmForceExit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mManager != null) {
                mManager.finishSource();
            } else {
                confirmForceExit();
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void initialResult(Integer result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (result == null || result != 200) {
                    setLoadingPattern(true);
                }
            });
        }
    }

    @Override
    public void handleSpaceReadyCheck(String source, int code, ReadyCheckResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (code >= 200 && code < 400 && result != null) {
                    Integer diskInitializeCode = result.getDiskInitialCode();
                    finishInit(diskInitializeCode, result.getMissingMainStorage());
                } else {
                    setLoadingPattern(true);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.loading_button_container:
                    setLoadingPattern(false);
                    handleRequestEvent();
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
                    case AODeviceDiscoveryManager.STEP_INITIAL:
                        InitialResults initialResults = null;
                        if (bodyJson != null) {
                            try {
                                initialResults = new Gson().fromJson(bodyJson, InitialResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        int resultCode = (initialResults == null ? 500 : DataUtil.stringCodeToInt(initialResults.getCode()));
                        if (resultCode == 200 && mManager != null) {
                            isInitSuccess = true;
                            if (enterSpace != null) {
                                enterSpace.setText(R.string.finishing_initialize);
                            }
                            boolean isInnerDiskSupport = mManager.isInnerDiskSupport();
                            AOSpaceUtil.requestAdministratorBindUseBox(getApplicationContext(), mManager.getBoxUuid(), isInnerDiskSupport);
                            if (isInnerDiskSupport) {
                                mStep = STEP_SPACE_READY_CHECK;
                                handleRequestEvent();
                            } else {
                                AOCompleteActivity.startThisActivity(EulixSpaceInitActivity.this, mManager.getBoxUuid(), "1");
                                mManager.finishSource();
                            }
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_SPACE_READY_CHECK:
                        ReadyCheckResult readyCheckResult = null;
                        if (bodyJson != null) {
                            try {
                                readyCheckResult = new Gson().fromJson(bodyJson, ReadyCheckResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        mReadyCheckResult = readyCheckResult;
                        Integer diskInitialCode = null;
                        if (readyCheckResult != null) {
                            diskInitialCode = readyCheckResult.getDiskInitialCode();
                        }
                        if (diskInitialCode != null) {
                            if (diskInitialCode == ReadyCheckResult.DISK_NORMAL) {
                                mStep = STEP_DISK_MANAGEMENT_LIST;
                            } else {
                                mStep = STEP_DISK_RECOGNITION;
                            }
                            handleRequestEvent();
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_DISK_RECOGNITION:
                        DiskRecognitionResult diskRecognitionResult = null;
                        if (bodyJson != null) {
                            try {
                                diskRecognitionResult = new Gson().fromJson(bodyJson, DiskRecognitionResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (diskRecognitionResult != null) {
                            String dataUuid = DataUtil.setData(new Gson().toJson(diskRecognitionResult, DiskRecognitionResult.class));
                            boolean isNoMainStorage = false;
                            Integer diskInitialCodeValue = null;
                            if (mReadyCheckResult != null) {
                                diskInitialCodeValue = mReadyCheckResult.getDiskInitialCode();
                                Boolean isNoMainStorageValue = mReadyCheckResult.getMissingMainStorage();
                                if (isNoMainStorageValue != null) {
                                    isNoMainStorage = isNoMainStorageValue;
                                }
                            }
                            Intent intent = new Intent(EulixSpaceInitActivity.this, DiskInitializationActivity.class);
                            intent.putExtra(ConstantField.DISK_INITIALIZE_NO_MAIN_STORAGE, isNoMainStorage);
                            String boxUuid = null;
                            if (mManager != null) {
                                boxUuid = mManager.getBoxUuid();
                            }
                            if (boxUuid != null) {
                                intent.putExtra(ConstantField.BOX_UUID, boxUuid);
                            }
                            if (dataUuid != null) {
                                intent.putExtra(ConstantField.DATA_UUID, dataUuid);
                            }
                            if (diskInitialCodeValue != null) {
                                intent.putExtra(ConstantField.DISK_INITIALIZE, diskInitialCodeValue.intValue());
                            }
                            startActivity(intent);
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    case AODeviceDiscoveryManager.STEP_DISK_MANAGEMENT_LIST:
                        DiskManageListResult diskManageListResult = null;
                        if (bodyJson != null) {
                            try {
                                diskManageListResult = new Gson().fromJson(bodyJson, DiskManageListResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        String boxUuid = null;
                        if (mManager != null) {
                            boxUuid = mManager.getBoxUuid();
                        }
                        if (boxUuid != null && diskManageListResult != null) {
                            AOSpaceUtil.requestUseBox(getApplicationContext(), boxUuid, "1", diskManageListResult);
                            AOCompleteActivity.startThisActivity(EulixSpaceInitActivity.this, boxUuid, "1");
                            mManager.finishSource();
                        } else {
                            handleErrorEvent();
                        }
                        break;
                    default:
                        break;
                }
            });
        }
    }

    @Override
    public void initializeResult(int code) {

    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void handleSpaceReadyCheckResponse(int code, String source, ReadyCheckResult result) {

    }
}
