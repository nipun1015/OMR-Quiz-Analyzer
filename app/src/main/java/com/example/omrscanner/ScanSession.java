package com.example.omrscanner;

import android.graphics.Bitmap;

/**
 * In-memory holder for the active scan evaluation result and debug bitmap,
 * avoiding Android Binder TransactionTooLargeException with large Bitmaps.
 */


public class ScanSession {

    private static OMRResult currentResult;
    private static Bitmap currentDebugBitmap;

    public static synchronized void setSession(OMRResult result, Bitmap debugBitmap) {
        currentResult = result;
        currentDebugBitmap = debugBitmap;
    }

    public static synchronized OMRResult getCurrentResult() {
        return currentResult;
    }

    public static synchronized Bitmap getCurrentDebugBitmap() {
        return currentDebugBitmap;
    }

    public static synchronized void clear() {
        currentResult = null;
        if (currentDebugBitmap != null && !currentDebugBitmap.isRecycled()) {
            currentDebugBitmap.recycle();
        }
        currentDebugBitmap = null;
    }
}

