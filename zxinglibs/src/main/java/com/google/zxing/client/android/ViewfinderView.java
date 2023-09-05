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

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 10L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    private Bitmap scanBitmap;
    /**
     * 扫描线开始位置
     */
    public int scannerStart = 0;
    /**
     * 扫描线结束位置
     */
    public int scannerEnd = 0;
    private int scannerLineHeight = 0;
    private int scannerLineWidth = 0;
    private int scannerLineMoveDistance = 0;
    private int drawBitmapHeight = 0;

    private int scannerBitmapShowWidth = 0;
    private int scannerBitmapShowHeight = 0;
    private int scannerSidePadding = 0;


    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;

        setBackgroundColor(Color.parseColor("#4D000000"));
        scanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_scan_line);
        scannerLineHeight = getResources().getDimensionPixelSize(R.dimen.dp_72);
        scannerLineMoveDistance = getResources().getDimensionPixelOffset(R.dimen.dp_3);
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }

        //计算一次扫描图片相关参数
        if (scannerEnd <= 0) {
            //根据缩放计算扫描图片宽高
            scannerBitmapShowWidth = frame.width() - getResources().getDimensionPixelSize(R.dimen.dp_13);
            float scale = (float) scannerBitmapShowWidth / (float) scanBitmap.getWidth();
            scannerBitmapShowHeight = (int) (scanBitmap.getHeight() * scale);

            scannerSidePadding = getResources().getDimensionPixelSize(R.dimen.dp_13);

            scannerEnd = frame.height() - getResources().getDimensionPixelSize(R.dimen.dp_72)
                    - 2 * scannerBitmapShowHeight;
        }

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, previewFrame, paint);
        } else {

            drawLineScanner(canvas, frame);
            postInvalidateDelayed(ANIMATION_DELAY);
        }
    }

    //绘制扫描图片
    private void drawLineScanner(Canvas canvas, Rect frame) {
        if (scannerStart <= scannerEnd) {
            Rect src = new Rect(0, 0, scanBitmap.getWidth(), scanBitmap.getHeight());
            Rect dst = new Rect(frame.left + scannerSidePadding, scannerStart, frame.right - scannerSidePadding, scannerStart + scannerBitmapShowHeight);
            canvas.drawBitmap(scanBitmap, src, dst, null);
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }

    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scanBitmap != null){
            scanBitmap.recycle();
            scanBitmap = null;
        }
    }
}
