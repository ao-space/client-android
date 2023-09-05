/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.google.zxing.client.android.history.HistoryActivity;
import com.google.zxing.client.android.history.HistoryItem;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.android.local.LocalGalleryActivity;
import com.google.zxing.client.android.result.ResultButtonListener;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.android.result.supplement.SupplementalInfoRetriever;
import com.google.zxing.client.android.share.ShareActivity;
import com.google.zxing.client.android.util.ConstantField;
import com.google.zxing.client.android.util.DataUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class CaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private static final String[] ZXING_URLS = {"http://zxing.appspot.com/scan", "zxing://scan/"};

    private static final int HISTORY_REQUEST_CODE = 0x0000bacc;
    private static final int LOCAL_GALLERY_CODE = HISTORY_REQUEST_CODE + 1;

    private static final Collection<ResultMetadataType> DISPLAYABLE_METADATA_TYPES =
            EnumSet.of(ResultMetadataType.ISSUE_NUMBER,
                    ResultMetadataType.SUGGESTED_PRICE,
                    ResultMetadataType.ERROR_CORRECTION_LEVEL,
                    ResultMetadataType.POSSIBLE_COUNTRY);

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private TextView statusView;
    private TextView localGallery;
    private LinearLayout customizeScanQRCodeContainer;
    private TextView customizeScanDeviceQRCodeStatusView;
    private TextView customizeScanDeviceQRCodeHint;
    private View resultView;
    private Result lastResult;
    private boolean hasSurface;
    private boolean copyToClipboard;
    private IntentSource source;
    private String sourceUrl;
    private ScanFromWebPageManager scanFromWebPageManager;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private HistoryManager historyManager;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private ImageView btnBack;
    private String mScanMode;
    private int mFunction;
    private String mRequestId;
    private Result mRawResult;
    private long mResultDurationMS;
    private CaptureHandler mHandler;
    private CaptureReceiver mReceiver;
    private boolean isRegisterReceiver;
    private boolean isImmediate = true;
    private String mDefaultStatus = null;
    private String mLocalGalleryText = null;
    private String mFunctionBeanJson = null;
    private String mLocalGalleryBeanJson = null;
    private String customizeMatchReg = null;
    private int mCustomizePattern;
    private String mCustomizePatternHint = null;
    private String extraContent = null;
    private ImageView imgQrTip;
    private int mQrTipImgResId = -1;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    public static class ZxingCommunication {
        public static final String ZXING_CAPTURE_SEND_ACTION = "com.google.zxing.client.android.action.CAPTURE_SEND";
        public static final String ZXING_CAPTURE_RECV_ACTION = "com.google.zxing.client.android.action.CAPTURE_RECV";

        public static final String KEY_TYPE = "key_type";
        public static final int SEND_BASE = 10000;
        public static final int RECV_BASE = 20000;
        public static final String REQUEST_ID = "request_id";

        public static final int CUSTOMIZE_PATTERN_SCAN_DEVICE_QR_CODE = 1;

        // 表示二维码信息用作校验，值为String
        public static final String CONTENT = "content";

        // 表示扫码功能，值为int
        public static final String FUNCTION_EXTRA_KEY = "function";

        // 表示扫码匹配规则，FUNCTION_EXTRA_KEY值为0使用
        public static final String CUSTOMIZE_MATCH_REG = "customize_match_reg";

        // 表示是否立即响应，值为boolean
        public static final String IMMEDIATE_EXTRA_KEY = "immediate";

        public static final String DEFAULT_STATUS = "default_status";

        public static final String LOCAL_GALLERY_TEXT = "local_gallery_text";

        public static final String CUSTOMIZE_PATTERN = "customize_pattern";

        public static final String CUSTOMIZE_PATTERN_HINT = "customize_pattern_hint";

        public static final String CUSTOMIZE_QR_TIP_RES_ID = "customize_qr_tip_res_id";

        public static final String EXTRA_CONTENT = "extra_content";

        // 表示处理结果，值为boolean
        public static final String RESULT = "result";
    }

    static class CaptureHandler extends Handler {
        private WeakReference<CaptureActivity> captureActivityWeakReference;

        public CaptureHandler(CaptureActivity activity) {
            captureActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CaptureActivity activity = captureActivityWeakReference.get();
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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.capture);

        imgQrTip = findViewById(R.id.img_qr_tip);

        //设置页面全屏显示
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            //设置页面延伸到刘海区显示
            getWindow().setAttributes(lp);
        }

        mHandler = new CaptureHandler(this);
        mFunction = 0;
        mReceiver = new CaptureReceiver();

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isRegisterReceiver && mReceiver != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ZxingCommunication.ZXING_CAPTURE_RECV_ACTION);
            isRegisterReceiver = true;
            try {
                registerReceiver(mReceiver, intentFilter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // historyManager must be initialized here to update the history preference
        historyManager = new HistoryManager(this);
        historyManager.trimHistory();

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);
        localGallery = findViewById(R.id.local_gallery);

        customizeScanQRCodeContainer = findViewById(R.id.customize_scan_device_qr_code_container);
        customizeScanDeviceQRCodeStatusView = findViewById(R.id.customize_scan_device_qr_code_status_view);
        customizeScanDeviceQRCodeHint = findViewById(R.id.customize_scan_device_qr_code_hint);

        handler = null;
        lastResult = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

//    if (prefs.getBoolean(PreferencesActivity.KEY_DISABLE_AUTO_ORIENTATION, true)) {
//      setRequestedOrientation(getCurrentOrientation());
//    } else {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//    }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        resetStatusView();


        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        Intent intent = getIntent();

        copyToClipboard = prefs.getBoolean(PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true)
                && (intent == null || intent.getBooleanExtra(Intents.Scan.SAVE_HISTORY, true));

        source = IntentSource.NONE;
        sourceUrl = null;
        scanFromWebPageManager = null;
        decodeFormats = null;
        characterSet = null;

        if (intent != null) {

            String action = intent.getAction();
            String dataString = intent.getDataString();
            mFunction = intent.getIntExtra(ZxingCommunication.FUNCTION_EXTRA_KEY, 0);
            isImmediate = intent.getBooleanExtra(ZxingCommunication.IMMEDIATE_EXTRA_KEY, true);
            if (intent.hasExtra(ZxingCommunication.DEFAULT_STATUS)) {
                mDefaultStatus = intent.getStringExtra(ZxingCommunication.DEFAULT_STATUS);
            }

            if (intent.hasExtra(ZxingCommunication.LOCAL_GALLERY_TEXT)) {
                mLocalGalleryText = intent.getStringExtra(ZxingCommunication.LOCAL_GALLERY_TEXT);
            }

            mCustomizePattern = intent.getIntExtra(ZxingCommunication.CUSTOMIZE_PATTERN, 0);
            if (intent.hasExtra(ZxingCommunication.CUSTOMIZE_PATTERN_HINT)) {
                mCustomizePatternHint = intent.getStringExtra(ZxingCommunication.CUSTOMIZE_PATTERN_HINT);
            }

            mQrTipImgResId = intent.getIntExtra(ZxingCommunication.CUSTOMIZE_QR_TIP_RES_ID, -1);

            resetStatusContent();

            if (intent.hasExtra(ConstantField.FUNCTION_JSON)) {
                mFunctionBeanJson = intent.getStringExtra(ConstantField.FUNCTION_JSON);
            }

            if (intent.hasExtra(ConstantField.LOCAL_GALLERY_JSON)) {
                mLocalGalleryBeanJson = intent.getStringExtra(ConstantField.LOCAL_GALLERY_JSON);
            }

            if (Intents.Scan.ACTION.equals(action)) {

                // Scan the formats the intent requested, and return the result to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
                decodeHints = DecodeHintManager.parseDecodeHints(intent);

                if (intent.hasExtra(Intents.Scan.MODE)) {
                    mScanMode = intent.getStringExtra(Intents.Scan.MODE);
                }

                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
                    int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height);
                    }
                }

                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
                    if (cameraId >= 0) {
                        cameraManager.setManualCameraId(cameraId);
                    }
                }

                String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
                if (customPromptMessage != null) {
                    statusView.setText(customPromptMessage);
                }

            } else if (dataString != null &&
                    dataString.contains("http://www.google") &&
                    dataString.contains("/m/products/scan")) {

                // Scan only products and send the result to mobile Product Search.
                source = IntentSource.PRODUCT_SEARCH_LINK;
                sourceUrl = dataString;
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

            } else if (isZXingURL(dataString)) {

                // Scan formats requested in query string (all formats if none specified).
                // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
                source = IntentSource.ZXING_LINK;
                sourceUrl = dataString;
                Uri inputUri = Uri.parse(dataString);
                scanFromWebPageManager = new ScanFromWebPageManager(inputUri);
                decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);
                // Allow a sub-set of the hints to be specified by the caller.
                decodeHints = DecodeHintManager.parseDecodeHints(inputUri);

            }

            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

            if (intent.hasExtra(ZxingCommunication.CUSTOMIZE_MATCH_REG)) {
                customizeMatchReg = intent.getStringExtra(ZxingCommunication.CUSTOMIZE_MATCH_REG);
            }
            if (intent.hasExtra(ZxingCommunication.EXTRA_CONTENT)) {
                extraContent = intent.getStringExtra(ZxingCommunication.EXTRA_CONTENT);
            }

        }

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }

    }

    private int getCurrentOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
    }

    private static boolean isZXingURL(String dataString) {
        if (dataString == null) {
            return false;
        }
        for (String url : ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (isRegisterReceiver && mReceiver != null) {
            isRegisterReceiver = false;
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        mReceiver = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (source == IntentSource.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK) && lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        int itemId = item.getItemId();
        if (itemId == R.id.menu_share) {
            intent.setClassName(this, ShareActivity.class.getName());
            startActivity(intent);
        } else if (itemId == R.id.menu_history) {
            intent.setClassName(this, HistoryActivity.class.getName());
            startActivityForResult(intent, HISTORY_REQUEST_CODE);
        } else if (itemId == R.id.menu_settings) {
            intent.setClassName(this, PreferencesActivity.class.getName());
            startActivity(intent);
        } else if (itemId == R.id.menu_help) {
            intent.setClassName(this, HelpActivity.class.getName());
            startActivity(intent);
        } else {
            return super.onOptionsItemSelected(item);
        }
//    switch (item.getItemId()) {
//      case R.id.menu_share:
//        intent.setClassName(this, ShareActivity.class.getName());
//        startActivity(intent);
//        break;
//      case R.id.menu_history:
//        intent.setClassName(this, HistoryActivity.class.getName());
//        startActivityForResult(intent, HISTORY_REQUEST_CODE);
//        break;
//      case R.id.menu_settings:
//        intent.setClassName(this, PreferencesActivity.class.getName());
//        startActivity(intent);
//        break;
//      case R.id.menu_help:
//        intent.setClassName(this, HelpActivity.class.getName());
//        startActivity(intent);
//        break;
//      default:
//        return super.onOptionsItemSelected(item);
//    }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == HISTORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && historyManager != null) {
                int itemNumber = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);
                if (itemNumber >= 0) {
                    HistoryItem historyItem = historyManager.buildHistoryItem(itemNumber);
                    decodeOrStoreSavedBitmap(null, historyItem.getResult());
                }
            }
        } else if (requestCode == LOCAL_GALLERY_CODE) {
            Result result = null;
            String dataUuid = null;
            if (intent != null && intent.hasExtra(ConstantField.DATA_UUID)) {
                dataUuid = intent.getStringExtra(ConstantField.DATA_UUID);
            }
            if (dataUuid != null) {
                result = DataUtil.getResult(dataUuid);
            }
            if (resultCode == RESULT_OK) {
                if (result == null) {
                    sendCaptureBroadcast(null);
                } else {
                    decodeOrStoreSavedBitmap(null, result);
                }
            }
        }
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        lastResult = rawResult;
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            historyManager.addHistoryItem(rawResult, resultHandler);
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        switch (source) {
            case NATIVE_APP_INTENT:
            case PRODUCT_SEARCH_LINK:
                handleDecodeExternally(rawResult, resultHandler, barcode);
                break;
            case ZXING_LINK:
                if (scanFromWebPageManager == null || !scanFromWebPageManager.isScanFromWebPage()) {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                } else {
                    handleDecodeExternally(rawResult, resultHandler, barcode);
                }
                break;
            case NONE:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (fromLiveScan && prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE, false)) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.msg_bulk_mode_scanned) + " (" + rawResult.getText() + ')',
                            Toast.LENGTH_SHORT).show();
                    // Wait a moment or else it will scan the same barcode continuously about 3 times
                    restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
                } else {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                }
                break;
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {

        CharSequence displayContents = resultHandler.getDisplayContents();

        if (copyToClipboard && !resultHandler.areContentsSecure()) {
            ClipboardInterface.setText(displayContents, this);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (resultHandler.getDefaultButtonID() != null && prefs.getBoolean(PreferencesActivity.KEY_AUTO_OPEN_WEB, false)) {
            resultHandler.handleButtonPress(resultHandler.getDefaultButtonID());
            return;
        }

        statusView.setVisibility(View.GONE);
        if (mLocalGalleryText != null && !TextUtils.isEmpty(mLocalGalleryText)) {
            localGallery.setVisibility(View.GONE);
            localGallery.setClickable(false);
        }
        customizeScanQRCodeContainer.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                    R.drawable.launcher_icon));
        } else {
            barcodeImageView.setImageBitmap(barcode);
        }

        TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());

        TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
        timeTextView.setText(formatter.format(new Date(rawResult.getTimestamp())));


        TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
        View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
        metaTextView.setVisibility(View.GONE);
        metaTextViewLabel.setVisibility(View.GONE);
        Map<ResultMetadataType, Object> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            StringBuilder metadataText = new StringBuilder(20);
            for (Map.Entry<ResultMetadataType, Object> entry : metadata.entrySet()) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
                    metadataText.append(entry.getValue()).append('\n');
                }
            }
            if (metadataText.length() > 0) {
                metadataText.setLength(metadataText.length() - 1);
                metaTextView.setText(metadataText);
                metaTextView.setVisibility(View.VISIBLE);
                metaTextViewLabel.setVisibility(View.VISIBLE);
            }
        }

        TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
        contentsTextView.setText(displayContents);
        int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
        contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

        TextView supplementTextView = (TextView) findViewById(R.id.contents_supplement_text_view);
        supplementTextView.setText("");
        supplementTextView.setOnClickListener(null);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_SUPPLEMENTAL, true)) {
            SupplementalInfoRetriever.maybeInvokeRetrieval(supplementTextView,
                    resultHandler.getResult(),
                    historyManager,
                    this);
        }

        int buttonCount = resultHandler.getButtonCount();
        ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
        buttonView.requestFocus();
        for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
            TextView button = (TextView) buttonView.getChildAt(x);
            if (x < buttonCount) {
                button.setVisibility(View.VISIBLE);
                button.setText(resultHandler.getButtonText(x));
                button.setOnClickListener(new ResultButtonListener(resultHandler, x));
            } else {
                button.setVisibility(View.GONE);
            }
        }

    }

    // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
    private void handleDecodeExternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {

        if (barcode != null) {
//            viewfinderView.drawResultBitmap(barcode);
        }

        //立即返回扫码结果
        long resultDurationMS = 0;
//        if (getIntent() == null) {
//            resultDurationMS = DEFAULT_INTENT_RESULT_DURATION_MS;
//        } else {
//            resultDurationMS = getIntent().getLongExtra(Intents.Scan.RESULT_DISPLAY_DURATION_MS,
//                    DEFAULT_INTENT_RESULT_DURATION_MS);
//        }
//
//        if (resultDurationMS > 0) {
//            String rawResultString = String.valueOf(rawResult);
//            if (rawResultString.length() > 32) {
//                rawResultString = rawResultString.substring(0, 32) + " ...";
//            }
////            statusView.setText(getString(resultHandler.getDisplayTitle()) + " : " + rawResultString);
//        }

        if (copyToClipboard && !resultHandler.areContentsSecure()) {
            CharSequence text = resultHandler.getDisplayContents();
            ClipboardInterface.setText(text, this);
        }

        if (source == IntentSource.NATIVE_APP_INTENT) {

            if (isImmediate) {
                handleAppNative(rawResult, resultDurationMS);
            } else if (mFunction == 0) {
                if (customizeMatchReg == null) {
                    handleAppNative(rawResult, resultDurationMS);
                } else {
                    // 先归属回传
                    handleAppNative(rawResult, resultDurationMS);
                }
            } else {
                mRawResult = rawResult;
                mResultDurationMS = resultDurationMS;
                sendCaptureBroadcast(rawResult == null ? null : rawResult.toString());
            }

        } else if (source == IntentSource.PRODUCT_SEARCH_LINK) {

            // Reformulate the URL which triggered us into a query, so that the request goes to the same
            // TLD as the scan URL.
            int end = sourceUrl.lastIndexOf("/scan");
            String replyURL = sourceUrl.substring(0, end) + "?q=" + resultHandler.getDisplayContents() + "&source=zxing";
            sendReplyMessage(R.id.launch_product_query, replyURL, resultDurationMS);

        } else if (source == IntentSource.ZXING_LINK) {

            if (scanFromWebPageManager != null && scanFromWebPageManager.isScanFromWebPage()) {
                String replyURL = scanFromWebPageManager.buildReplyURL(rawResult, resultHandler);
                scanFromWebPageManager = null;
                sendReplyMessage(R.id.launch_product_query, replyURL, resultDurationMS);
            }

        }
    }

    private void sendReplyMessage(int id, Object arg, long delayMS) {
        if (handler != null) {
            Message message = Message.obtain(handler, id, arg);
            if (delayMS > 0L) {
                handler.sendMessageDelayed(message, delayMS);
            } else {
                handler.sendMessage(message);
            }
        }
    }

    private void handleAppNative(Result rawResult, long resultDurationMS) {
        // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
        // the deprecated intent is retired.
        Intent intent = new Intent(getIntent().getAction());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
        intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
        if (extraContent != null) {
            intent.putExtra(ZxingCommunication.EXTRA_CONTENT, extraContent);
        }
        byte[] rawBytes = rawResult.getRawBytes();
        if (rawBytes != null && rawBytes.length > 0) {
            intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
        }
        Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                        metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
            }
            Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
            if (orientation != null) {
                intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
            }
            String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
            if (ecLevel != null) {
                intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
            }
            @SuppressWarnings("unchecked")
            Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
            if (byteSegments != null) {
                int i = 0;
                for (byte[] byteSegment : byteSegments) {
                    intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
                    i++;
                }
            }
        }
        sendReplyMessage(R.id.return_scan_result, intent, resultDurationMS);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
        resetStatusContent();
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        localGallery.setText(R.string.msg_local_gallery);
        localGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CaptureActivity.this, LocalGalleryActivity.class);
                if (mScanMode != null) {
                    intent.putExtra(ConstantField.SCAN_MODE, mScanMode);
                }
                if (mFunctionBeanJson != null) {
                    intent.putExtra(ConstantField.FUNCTION_JSON, mFunctionBeanJson);
                }
                if (mLocalGalleryBeanJson != null) {
                    intent.putExtra(ConstantField.LOCAL_GALLERY_JSON, mLocalGalleryBeanJson);
                }
                startActivityForResult(intent, LOCAL_GALLERY_CODE);
            }
        });
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    private void resetStatusContent() {
        switch (mCustomizePattern) {
            case ZxingCommunication.CUSTOMIZE_PATTERN_SCAN_DEVICE_QR_CODE:
                statusView.setVisibility(View.GONE);
                customizeScanQRCodeContainer.setVisibility(View.VISIBLE);
                if (mDefaultStatus != null) {
                    customizeScanDeviceQRCodeStatusView.setText(mDefaultStatus);
                }
                if (mCustomizePatternHint != null && !TextUtils.isEmpty(mCustomizePatternHint)) {
                    customizeScanDeviceQRCodeHint.setVisibility(View.VISIBLE);
                    customizeScanDeviceQRCodeHint.setText(mCustomizePatternHint);
                } else {
                    customizeScanDeviceQRCodeHint.setText("");
                    customizeScanDeviceQRCodeHint.setVisibility(View.GONE);
                }
                if (mQrTipImgResId != -1) {
                    imgQrTip.setImageResource(mQrTipImgResId);
                } else {
                    imgQrTip.setImageResource(R.drawable.customize_scan_device_qr_code_image_2x);
                }
                break;
            default:
                customizeScanQRCodeContainer.setVisibility(View.GONE);
                statusView.setVisibility(View.VISIBLE);
                if (mDefaultStatus != null) {
                    statusView.setText(mDefaultStatus);
                }
                if (mLocalGalleryText != null && !TextUtils.isEmpty(mLocalGalleryText)) {
                    localGallery.setText(mLocalGalleryText);
                    localGallery.setVisibility(View.VISIBLE);
                    localGallery.setClickable(true);
                } else {
                    localGallery.setClickable(false);
                    localGallery.setVisibility(View.GONE);
                }
                break;
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    private void sendCaptureBroadcast(String content) {
        mRequestId = UUID.randomUUID().toString();
        Intent intent = new Intent();
        intent.setAction(ZxingCommunication.ZXING_CAPTURE_SEND_ACTION);
        intent.putExtra(ZxingCommunication.KEY_TYPE, (ZxingCommunication.SEND_BASE + mFunction));
        intent.putExtra(ZxingCommunication.REQUEST_ID, mRequestId);
        if (content != null) {
            intent.putExtra(ZxingCommunication.CONTENT, content);
        }
        sendBroadcast(intent);
    }

    class CaptureReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean isSuccess = null;
            if (intent != null) {
                String action = intent.getAction();
                if (ZxingCommunication.ZXING_CAPTURE_RECV_ACTION.equals(action)) {
                    if (intent.hasExtra(ZxingCommunication.KEY_TYPE) && intent.hasExtra(ZxingCommunication.REQUEST_ID)
                            && intent.hasExtra(ZxingCommunication.RESULT)) {
                        String requestId = intent.getStringExtra(ZxingCommunication.REQUEST_ID);
                        if ((requestId != null && requestId.equals(mRequestId))
                                && (mFunction == 0 || mFunction == (intent.getIntExtra(ZxingCommunication.KEY_TYPE
                                , ZxingCommunication.RECV_BASE) - ZxingCommunication.RECV_BASE))) {
                            isSuccess = intent.getBooleanExtra(ZxingCommunication.RESULT, false);
                        }
                    }
                }
            }
            if (isSuccess != null) {
                if (isSuccess) {
                    if (mHandler == null) {
                        handleAppNative(mRawResult, mResultDurationMS);
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleAppNative(mRawResult, mResultDurationMS);
                            }
                        });
                    }
                } else {
                    if (mHandler == null) {
                        restartPreviewAfterDelay(2000L);
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                restartPreviewAfterDelay(2000L);
                            }
                        });
                    }
                }
            }
        }
    }

    public static class FunctionBean {
        private String cancelText;
        private String confirmText;

        public String getCancelText() {
            return cancelText;
        }

        public void setCancelText(String cancelText) {
            this.cancelText = cancelText;
        }

        public String getConfirmText() {
            return confirmText;
        }

        public void setConfirmText(String confirmText) {
            this.confirmText = confirmText;
        }

        @Override
        public String toString() {
            return "FunctionBean{" +
                    "cancelText='" + cancelText + '\'' +
                    ", confirmText='" + confirmText + '\'' +
                    '}';
        }
    }

    public static class LocalGalleryBean {
        private String imageUnit;
        private String localAllImageText;
        private String localGalleryEmptyHint;

        public String getImageUnit() {
            return imageUnit;
        }

        public void setImageUnit(String imageUnit) {
            this.imageUnit = imageUnit;
        }

        public String getLocalAllImageText() {
            return localAllImageText;
        }

        public void setLocalAllImageText(String localAllImageText) {
            this.localAllImageText = localAllImageText;
        }

        public String getLocalGalleryEmptyHint() {
            return localGalleryEmptyHint;
        }

        public void setLocalGalleryEmptyHint(String localGalleryEmptyHint) {
            this.localGalleryEmptyHint = localGalleryEmptyHint;
        }

        @Override
        public String toString() {
            return "LocalGalleryBean{" +
                    "imageUnit='" + imageUnit + '\'' +
                    ", localAllImageText='" + localAllImageText + '\'' +
                    ", localGalleryEmptyHint='" + localGalleryEmptyHint + '\'' +
                    '}';
        }
    }
}
