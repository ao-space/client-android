package com.google.zxing.client.android.util;

import android.graphics.Bitmap;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.aztec.AztecReader;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.pdf417.PDF417Reader;
import com.google.zxing.qrcode.QRCodeReader;

public class DecodeUtil {
    private static Result readerDecode(BinaryBitmap binaryBitmap, String scanMode) {
        Result result = null;
        Reader reader = null;
        if (Intents.Scan.QR_CODE_MODE.equals(scanMode)) {
            reader = new QRCodeReader();
        } else if (Intents.Scan.DATA_MATRIX_MODE.equals(scanMode)) {
            reader = new DataMatrixReader();
        } else if (Intents.Scan.AZTEC_MODE.equals(scanMode)) {
            reader = new AztecReader();
        } else if (Intents.Scan.PDF417_MODE.equals(scanMode)) {
            reader = new PDF417Reader();
        } else {
            reader = new MultiFormatReader();
        }
        try {
            if (reader instanceof MultiFormatReader) {
                result = ((MultiFormatReader) reader).decodeWithState(binaryBitmap);
            } else {
                result = reader.decode(binaryBitmap);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } finally {
            reader.reset();
        }

        return result;
    }

    private static byte[] encodeRGBtoYUV(int[] argb, int width, int height) {
        byte[] yuv = null;
        int argbLength = argb.length;
        if (argbLength >= (width * height)) {
            yuv = new byte[(width * height / 2 * 3)];
            int yuvLength = yuv.length;
            int yIndex = 0;
            int uvIndex = (width * height);
            int rgbIndex = 0;
            int r, g, b;
            int y, u, v;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    r = ((argb[rgbIndex] & 0xff0000) >> 16);
                    g = ((argb[rgbIndex] & 0xff00) >> 8);
                    b = (argb[rgbIndex] & 0xff);
                    rgbIndex++;
                    y = Math.max(Math.min((((66 * r + 129 * g + 25 * b + 128) >> 8) + 16), 255), 0);
                    u = Math.max(Math.min((((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128), 255), 0);
                    v = Math.max(Math.min((((112 * r - 94 * g - 18 * b + 128) >> 8) + 128), 255), 0);
                    yuv[yIndex++] = (byte) y;
                    if (uvIndex < yuvLength && (j % 2 == 0) && ((j + 1) < height) && (i % 2 == 0) && ((i + 1) < width)) {
                        yuv[uvIndex++] = (byte) v;
                        if (uvIndex < yuvLength) {
                            yuv[uvIndex++] = (byte) u;
                        }
                    }
                }
            }
        }
        return yuv;
    }

    private static Result decodeYUV(byte[] yuv, int width, int height, String scanMode) {
        Result result = null;
        if (yuv != null && width > 0 && height > 0) {
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(yuv, width, height, 0, 0, width, height, false);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            result = readerDecode(binaryBitmap, scanMode);
        }
        return result;
    }

    public static Result bitmapDecodeYUV(Bitmap bitmap, String scanMode) {
        Result result = null;
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            result = decodeYUV(encodeRGBtoYUV(pixels, width, height), width, height, scanMode);
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }

    private static Result decodeRGB(int width, int height, int[] pixels, String scanMode) {
        Result result = null;
        if (width > 0 && height > 0 && pixels != null) {
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            result = readerDecode(binaryBitmap, scanMode);
        }
        return result;
    }

    public static Result bitmapDecodeRGB(Bitmap bitmap, String scanMode) {
        Result result = null;
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            result = decodeRGB(width, height, pixels, scanMode);
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }

    public static Result decodeBitmap(Bitmap bitmap, String scanMode) {
        Result result = null;
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            result = decodeRGB(width, height, pixels, scanMode);
            if (result == null) {
                result = decodeYUV(encodeRGBtoYUV(pixels, width, height), width, height, scanMode);
            }
            bitmap.recycle();
            bitmap = null;
        }
        return result;
    }
}
