package com.google.zxing.client.android.local;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.util.ConstantField;
import com.google.zxing.client.android.util.DataUtil;
import com.google.zxing.client.android.util.DecodeUtil;
import com.google.zxing.client.android.util.StatusBarUtil;
import com.google.zxing.client.android.widget.ClipImageView;

import java.io.IOException;

public class LocalImageActivity extends Activity implements View.OnClickListener {
    private ClipImageView mClipImageView;
    private ImageButton back;
    private Button function;

    private String mScanMode;
    private String mConfirmText = null;
    private String mCancelText = null;
    private String mOutput;
    private String mInput;
    private int mMaxWidth;

    // 图片被旋转的角度
    private int mDegree;
    // 大图被设置之前的缩放比例
    private int mSampleSize;
    private int mSourceWidth;
    private int mSourceHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setStatusBarColor(Color.WHITE, this);
        initData();
        this.setContentView(R.layout.activity_local_image);
        mClipImageView = findViewById(R.id.clip_image_view);
        back = findViewById(R.id.back);
        function = findViewById(R.id.function_text);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.SCAN_MODE)) {
                mScanMode = intent.getStringExtra(ConstantField.SCAN_MODE);
            }
            if (intent.hasExtra(ConstantField.FUNCTION_JSON)) {
                String functionBeanJson = intent.getStringExtra(ConstantField.FUNCTION_JSON);
                CaptureActivity.FunctionBean functionBean = null;
                if (functionBeanJson != null) {
                    try {
                        functionBean = new Gson().fromJson(functionBeanJson, CaptureActivity.FunctionBean.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                if (functionBean != null) {
                    mConfirmText = functionBean.getConfirmText();
                    mCancelText = functionBean.getCancelText();
                }
            }
        }

        function.setVisibility(View.VISIBLE);
        if (mConfirmText == null || TextUtils.isEmpty(mConfirmText)) {
            function.setText(R.string.confirm);
        } else {
            function.setText(mConfirmText);
        }
        back.setOnClickListener(this);
        function.setOnClickListener(this);

        ClipOptions clipOptions = ClipOptions.createFromBundle(intent);
        mOutput = clipOptions.getOutputPath();
        mInput = clipOptions.getInputPath();
        mMaxWidth = clipOptions.getMaxWidth();
        mClipImageView.setAspect(clipOptions.getAspectX(), clipOptions.getAspectY());
        mClipImageView.setTip(clipOptions.getTip());
        mClipImageView.setMaxOutputWidth(mMaxWidth);

        setImageAndClipParams(); //大图裁剪
    }

    public void initData() {
//        LocaleBean localeBean = null;
//        String localeValue = DataUtil.getApplicationLocale(this);
//        if (localeValue != null) {
//            try {
//                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
//            } catch (JsonSyntaxException e) {
//                e.printStackTrace();
//            }
//        }
//        if (localeBean != null) {
//            Locale locale = localeBean.parseLocale();
//            Resources resources = getResources();
//            if (resources != null && locale != null) {
//                Configuration configuration = resources.getConfiguration();
//                if (configuration != null) {
//                    configuration.setLocale(locale);
//                    //createConfigurationContext(configuration);
//                    resources.updateConfiguration(configuration, resources.getDisplayMetrics());
//                }
//            }
//        }
    }

    private void setImageAndClipParams() {
        mClipImageView.post(new Runnable() {
            @Override
            public void run() {
                mClipImageView.setMaxOutputWidth(mMaxWidth);

                mDegree = readPictureDegree(mInput);

                final boolean isRotate = (mDegree == 90 || mDegree == 270);

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mInput, options);

                mSourceWidth = options.outWidth;
                mSourceHeight = options.outHeight;

                // 如果图片被旋转，则宽高度置换
                int w = isRotate ? options.outHeight : options.outWidth;

                // 裁剪是宽高比例3:2，只考虑宽度情况，这里按border宽度的两倍来计算缩放。
                mSampleSize = findBestSample(w, mClipImageView.getClipBorder().width());

                options.inJustDecodeBounds = false;
                options.inSampleSize = mSampleSize;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                final Bitmap source = BitmapFactory.decodeFile(mInput, options);

                // 解决图片被旋转的问题
                Bitmap target;
                if (mDegree == 0) {
                    target = source;
                } else {
                    final Matrix matrix = new Matrix();
                    matrix.postRotate(mDegree);
                    target = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
                    if (target != source && !source.isRecycled()) {
                        source.recycle();
                    }
                }
                mClipImageView.setImageBitmap(target);
            }
        });
    }

    /**
     * 计算最好的采样大小。
     *
     * @param origin 当前宽度
     * @param target 限定宽度
     * @return sampleSize
     */
    private static int findBestSample(int origin, int target) {
        int sample = 1;
        for (int out = origin / 2; out > target; out /= 2) {
            sample *= 2;
        }
        return sample;
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    private Bitmap createClippedBitmap() {
        if (mSampleSize <= 1) {
            return mClipImageView.clip();
        }

        // 获取缩放位移后的矩阵值
        final float[] matrixValues = mClipImageView.getClipMatrixValues();
        final float scale = matrixValues[Matrix.MSCALE_X];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        // 获取在显示的图片中裁剪的位置
        final Rect border = mClipImageView.getClipBorder();
        final float cropX = ((-transX + border.left) / scale) * mSampleSize;
        final float cropY = ((-transY + border.top) / scale) * mSampleSize;
        final float cropWidth = (border.width() / scale) * mSampleSize;
        final float cropHeight = (border.height() / scale) * mSampleSize;

        // 获取在旋转之前的裁剪位置
        final RectF srcRect = new RectF(cropX, cropY, cropX + cropWidth, cropY + cropHeight);
        final Rect clipRect = getRealRect(srcRect);

        final BitmapFactory.Options ops = new BitmapFactory.Options();
        final Matrix outputMatrix = new Matrix();

        outputMatrix.setRotate(mDegree);
        // 如果裁剪之后的图片宽高仍然太大,则进行缩小
        if (mMaxWidth > 0 && cropWidth > mMaxWidth) {
            ops.inSampleSize = findBestSample((int) cropWidth, mMaxWidth);

            final float outputScale = mMaxWidth / (cropWidth / ops.inSampleSize);
            outputMatrix.postScale(outputScale, outputScale);
        }

        // 裁剪
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(mInput, false);
            final Bitmap source = decoder.decodeRegion(clipRect, ops);
            recycleImageViewBitmap();
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), outputMatrix, false);
        } catch (Exception e) {
            return mClipImageView.clip();
        } finally {
            if (decoder != null && !decoder.isRecycled()) {
                decoder.recycle();
            }
        }
    }

    private Rect getRealRect(RectF srcRect) {
        switch (mDegree) {
            case 90:
                return new Rect((int) srcRect.top, (int) (mSourceHeight - srcRect.right),
                        (int) srcRect.bottom, (int) (mSourceHeight - srcRect.left));
            case 180:
                return new Rect((int) (mSourceWidth - srcRect.right), (int) (mSourceHeight - srcRect.bottom),
                        (int) (mSourceWidth - srcRect.left), (int) (mSourceHeight - srcRect.top));
            case 270:
                return new Rect((int) (mSourceWidth - srcRect.bottom), (int) srcRect.left,
                        (int) (mSourceWidth - srcRect.top), (int) srcRect.right);
            default:
                return new Rect((int) srcRect.left, (int) srcRect.top, (int) srcRect.right, (int) srcRect.bottom);
        }
    }

    private void recycleImageViewBitmap() {
        mClipImageView.post(new Runnable() {
            @Override
            public void run() {
                mClipImageView.setImageBitmap(null);
            }
        });
    }

    public static ClipOptions prepare() {
        return new ClipOptions();
    }

    private void handleResult(boolean isOk, String dataUuid) {
        Intent intent = new Intent();
        if (dataUuid != null) {
            intent.putExtra(ConstantField.DATA_UUID, dataUuid);
        }
        setResult(isOk ? RESULT_OK : RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            int id = v.getId();
            if (id == R.id.back) {
                handleResult(false, null);
            } else if (id == R.id.function_text) {
                Bitmap bitmap = createClippedBitmap();
                Result result = DecodeUtil.decodeBitmap(bitmap, mScanMode);
                String dataUuid = null;
                if (result != null) {
                    dataUuid = DataUtil.setResult(result);
                }
                handleResult(true, dataUuid);
            }
        }
    }

    public static class ClipOptions {
        private int aspectX;
        private int aspectY;
        private int maxWidth;
        private String tip;
        private String inputPath;
        private String outputPath;

        private ClipOptions() {
        }

        public ClipOptions aspectX(int aspectX) {
            this.aspectX = aspectX;
            return this;
        }

        public ClipOptions aspectY(int aspectY) {
            this.aspectY = aspectY;
            return this;
        }

        public ClipOptions maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public ClipOptions tip(String tip) {
            this.tip = tip;
            return this;
        }

        public ClipOptions inputPath(String path) {
            this.inputPath = path;
            return this;
        }

        public ClipOptions outputPath(String path) {
            this.outputPath = path;
            return this;
        }

        public int getAspectX() {
            return aspectX;
        }

        public int getAspectY() {
            return aspectY;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public String getTip() {
            return tip;
        }

        public String getInputPath() {
            return inputPath;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public Intent getSinkIntent(Activity activity) {
            Intent intent = null;
            if (checkValues()) {
                intent = new Intent(activity, LocalImageActivity.class);
                intent.putExtra("aspectX", aspectX);
                intent.putExtra("aspectY", aspectY);
                intent.putExtra("maxWidth", maxWidth);
                intent.putExtra("tip", tip);
                intent.putExtra("inputPath", inputPath);
                if (outputPath != null) {
                    intent.putExtra("outputPath", outputPath);
                }
            }
            return intent;
        }

        public void startForResult(Activity activity, int requestCode) {
            checkValues();
            Intent intent = new Intent(activity, LocalImageActivity.class);
            intent.putExtra("aspectX", aspectX);
            intent.putExtra("aspectY", aspectY);
            intent.putExtra("maxWidth", maxWidth);
            intent.putExtra("tip", tip);
            intent.putExtra("inputPath", inputPath);
            if (outputPath != null) {
                intent.putExtra("outputPath", outputPath);
            }
            activity.startActivityForResult(intent, requestCode);
        }

        private boolean checkValues() {
            boolean isChecked = true;
            if (TextUtils.isEmpty(inputPath)) {
                isChecked = false;
//                throw new IllegalArgumentException("The input path could not be empty");
            }
//            if (TextUtils.isEmpty(outputPath)) {
//                throw new IllegalArgumentException("The output path could not be empty");
//            }
            return isChecked;
        }

        public static ClipOptions createFromBundle(Intent intent) {
            if (intent == null) {
                return new ClipOptions();
            } else {
                return new ClipOptions()
                        .aspectX(intent.getIntExtra("aspectX", 1))
                        .aspectY(intent.getIntExtra("aspectY", 1))
                        .maxWidth(intent.getIntExtra("maxWidth", 0))
                        .tip(intent.getStringExtra("tip"))
                        .inputPath(intent.getStringExtra("inputPath"))
                        .outputPath(intent.getStringExtra("outputPath"));
            }
        }
    }
}
