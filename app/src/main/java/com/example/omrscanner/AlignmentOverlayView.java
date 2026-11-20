package com.example.omrscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Transparent overlay that renders exactly 160 coloured guide dots:
 * - 100 RED dots for Student Roll Number (10 columns x 10 rows for digits 0-9)
 * - 60 GREEN dots for Answers (3 groups of 5 questions x 4 options A, B, C, D)
 *
 * Bubble positions are derived directly from the reference template OMR2_ultra_hd.pdf.
 *
 * Supports:
 * - Drag gesture to pan horizontally/vertically.
 * - Pinch-to-zoom gesture to scale.
 * - On-screen button/control adjustments.
 * - Automatic persistence of calibration offsets & scale.
 * - Accurate mapping from preview overlay space to captured photo pixel coordinates.
 */
public class
AlignmentOverlayView extends View {

    // Paints
    private final Paint rollDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rollDotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint answerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint answerDotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint sheetBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Calibration offsets and scale
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float scale = 1.0f;

    // Camera preview content area inside PreviewView (FIT_CENTER letterboxing)
    private float contentLeft = 0f;
    private float contentTop = 0f;
    private float contentWidth = 0f;
    private float contentHeight = 0f;

    // Visual dimensions
    private float dotRadiusPx;
    private boolean touchGesturesEnabled = true;
    private boolean overlayLocked = false;

    // Gesture handling
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    // Preferences
    private static final String PREFS_NAME = "omr_calibration_prefs";
    private static final String KEY_OFFSET_X = "calib_offset_x";
    private static final String KEY_OFFSET_Y = "calib_offset_y";
    private static final String KEY_SCALE = "calib_scale";

    public AlignmentOverlayView(Context context) {
        super(context);
        init(context);
    }

    public AlignmentOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlignmentOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = getResources().getDisplayMetrics().density;
        dotRadiusPx = density * 4.0f; // 4dp radius dot

        // Red dots for Roll Number
        rollDotPaint.setColor(0xFFFF1744); // Vibrant Red
        rollDotPaint.setStyle(Paint.Style.FILL);

        rollDotStrokePaint.setColor(0xCC000000); // Dark border for contrast
        rollDotStrokePaint.setStyle(Paint.Style.STROKE);
        rollDotStrokePaint.setStrokeWidth(density * 1.0f);

        // Green dots for Answers
        answerDotPaint.setColor(0xFF00E676); // Vibrant Green
        answerDotPaint.setStyle(Paint.Style.FILL);

        answerDotStrokePaint.setColor(0xCC000000);
        answerDotStrokePaint.setStyle(Paint.Style.STROKE);
        answerDotStrokePaint.setStrokeWidth(density * 1.0f);

        // Faint sheet border outline
        sheetBorderPaint.setColor(0x66FFFFFF);
        sheetBorderPaint.setStyle(Paint.Style.STROKE);
        sheetBorderPaint.setStrokeWidth(density * 1.5f);

        // Corner alignment marker hints
        cornerMarkerPaint.setColor(0x44FFFFFF);
        cornerMarkerPaint.setStyle(Paint.Style.FILL);

        // Load saved calibration
        loadCalibration();

        // Setup scale gesture detector
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!touchGesturesEnabled) return false;
                float scaleFactor = detector.getScaleFactor();
                scale = Math.max(0.4f, Math.min(3.0f, scale * scaleFactor));
                saveCalibration();
                invalidate();
                return true;
            }
        });
    }

    /**
     * Set the actual rendered camera preview frame rectangle.
     * With FIT_CENTER, this accounts for letterbox or pillarbox bars.
     */
    public void setCameraContentArea(float left, float top, float width, float height) {
        this.contentLeft = left;
        this.contentTop = top;
        this.contentWidth = width;
        this.contentHeight = height;
        invalidate();
    }

    public void setTouchGesturesEnabled(boolean enabled) {
        this.touchGesturesEnabled = enabled;
    }

    public boolean isTouchGesturesEnabled() {
        return touchGesturesEnabled;
    }

    /** Lock or unlock the overlay. When locked, all movement (touch, buttons) is blocked. */
    public void setLocked(boolean locked) {
        this.overlayLocked = locked;
    }

    public boolean isLocked() {
        return overlayLocked;
    }

    // ------------------------------------------------------------------
    // Calibration methods
    // ------------------------------------------------------------------

    public void moveOverlay(float dx, float dy) {
        if (overlayLocked) return;
        offsetX += dx;
        offsetY += dy;
        saveCalibration();
        invalidate();
    }

    public void scaleOverlay(float delta) {
        if (overlayLocked) return;
        scale = Math.max(0.4f, Math.min(3.0f, scale + delta));
        saveCalibration();
        invalidate();
    }

    public void resetCalibration() {
        offsetX = 0f;
        offsetY = 0f;
        scale = 1.0f;
        saveCalibration();
        invalidate();
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float getScale() {
        return scale;
    }

    private void saveCalibration() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat(KEY_OFFSET_X, offsetX)
                .putFloat(KEY_OFFSET_Y, offsetY)
                .putFloat(KEY_SCALE, scale)
                .apply();
    }

    private void loadCalibration() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        offsetX = prefs.getFloat(KEY_OFFSET_X, 0f);
        offsetY = prefs.getFloat(KEY_OFFSET_Y, 0f);
        scale = prefs.getFloat(KEY_SCALE, 1.0f);
    }

    // ------------------------------------------------------------------
    // Touch handling for intuitive drag and pinch-to-zoom
    // ------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!touchGesturesEnabled || overlayLocked) {
            return super.onTouchEvent(event);
        }

        scaleDetector.onTouchEvent(event);

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    offsetX += dx;
                    offsetY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                saveCalibration();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                isDragging = false;
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    // ------------------------------------------------------------------
    // Drawing the virtual sheet and exactly 160 guide dots
    // ------------------------------------------------------------------

    /**
     * Computes the bounding rectangle of the virtual OMR sheet on screen.
     */
    public RectF computeSheetRect() {
        float effectiveW = contentWidth > 0 ? contentWidth : getWidth();
        float effectiveH = contentHeight > 0 ? contentHeight : getHeight();
        float cLeft = contentWidth > 0 ? contentLeft : 0f;
        float cTop = contentHeight > 0 ? contentTop : 0f;

        if (effectiveW <= 0 || effectiveH <= 0) {
            return new RectF(0, 0, 0, 0);
        }

        float sheetAspect = OMRConfig.SHEET_ASPECT; // ~0.7072 (A4 proportion)

        float baseSheetW, baseSheetH;
        if (effectiveW / effectiveH < sheetAspect) {
            // Limited by width
            baseSheetW = effectiveW * 0.92f;
            baseSheetH = baseSheetW / sheetAspect;
        } else {
            // Limited by height
            baseSheetH = effectiveH * 0.92f;
            baseSheetW = baseSheetH * sheetAspect;
        }

        float sheetW = baseSheetW * scale;
        float sheetH = baseSheetH * scale;

        float sheetLeft = cLeft + (effectiveW - sheetW) / 2f + offsetX;
        float sheetTop = cTop + (effectiveH - sheetH) / 2f + offsetY;

        return new RectF(sheetLeft, sheetTop, sheetLeft + sheetW, sheetTop + sheetH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        RectF sheet = computeSheetRect();
        if (sheet.width() <= 0 || sheet.height() <= 0) {
            return;
        }

        float sheetLeft = sheet.left;
        float sheetTop = sheet.top;
        float sheetW = sheet.width();
        float sheetH = sheet.height();

        // 1. Draw outer sheet boundary rectangle
        canvas.drawRect(sheet, sheetBorderPaint);

        // 2. Draw 4 corner marker alignment indicators (Option 2 corners)
        float cornerSize = sheetW * 0.05f;
        // Top-Left
        canvas.drawRect(sheetLeft, sheetTop + sheetH * 0.043f, sheetLeft + cornerSize, sheetTop + sheetH * 0.043f + cornerSize, cornerMarkerPaint);
        // Top-Right
        canvas.drawRect(sheetLeft + sheetW - cornerSize, sheetTop + sheetH * 0.043f, sheetLeft + sheetW, sheetTop + sheetH * 0.043f + cornerSize, cornerMarkerPaint);
        // Bottom-Left
        canvas.drawRect(sheetLeft, sheetTop + sheetH * 0.54f, sheetLeft + cornerSize, sheetTop + sheetH * 0.54f + cornerSize, cornerMarkerPaint);
        // Bottom-Right
        canvas.drawRect(sheetLeft + sheetW - cornerSize, sheetTop + sheetH * 0.54f, sheetLeft + sheetW, sheetTop + sheetH * 0.54f + cornerSize, cornerMarkerPaint);

        // 3. Draw 100 RED roll-number guide dots (10 cols x 10 rows)
        for (int col = 0; col < OMRConfig.ROLL_COLS; col++) {
            float sx = sheetLeft + OMRConfig.ROLL_X[col] * sheetW;
            for (int row = 0; row < OMRConfig.ROLL_ROWS; row++) {
                float sy = sheetTop + OMRConfig.ROLL_Y[row] * sheetH;
                canvas.drawCircle(sx, sy, dotRadiusPx, rollDotPaint);
                canvas.drawCircle(sx, sy, dotRadiusPx, rollDotStrokePaint);
            }
        }

        // 4. Draw 60 GREEN answer guide dots (3 groups x 5 questions x 4 options)
        for (int g = 0; g < OMRConfig.NUM_GROUPS; g++) {
            float[] groupX = OMRConfig.ANS_GROUP_X[g];
            for (int q = 0; q < OMRConfig.QUESTIONS_PER_GROUP; q++) {
                float sy = sheetTop + OMRConfig.ANS_Y[q] * sheetH;
                for (int opt = 0; opt < OMRConfig.OPTIONS_PER_Q; opt++) {
                    float sx = sheetLeft + groupX[opt] * sheetW;
                    canvas.drawCircle(sx, sy, dotRadiusPx, answerDotPaint);
                    canvas.drawCircle(sx, sy, dotRadiusPx, answerDotStrokePaint);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Coordinate mapping: preview overlay space -> captured image pixel space
    // ------------------------------------------------------------------

    /**
     * Calculates the exact pixel position of all 160 guide dots on the captured image bitmap
     * (after EXIF rotation has been applied).
     *
     * @param imageWidth  captured image width
     * @param imageHeight captured image height
     * @return float[160][2] with image (X, Y) coordinates
     */
    public float[][] getDotsInImageCoords(int imageWidth, int imageHeight) {
        RectF sheet = computeSheetRect();
        if (sheet.width() <= 0 || sheet.height() <= 0) {
            return new float[0][2];
        }

        float cLeft = contentWidth > 0 ? contentLeft : 0f;
        float cTop = contentHeight > 0 ? contentTop : 0f;
        float cWidth = contentWidth > 0 ? contentWidth : getWidth();
        float cHeight = contentHeight > 0 ? contentHeight : getHeight();

        // Scale factor from preview content area to full captured bitmap
        float scaleToImageX = (float) imageWidth / cWidth;
        float scaleToImageY = (float) imageHeight / cHeight;

        int totalDots = OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS
                + OMRConfig.NUM_GROUPS * OMRConfig.QUESTIONS_PER_GROUP * OMRConfig.OPTIONS_PER_Q;

        float[][] result = new float[totalDots][2];
        int idx = 0;

        float sheetLeft = sheet.left;
        float sheetTop = sheet.top;
        float sheetW = sheet.width();
        float sheetH = sheet.height();

        // 100 Roll number dots
        for (int row = 0; row < OMRConfig.ROLL_ROWS; row++) {
            float sy = sheetTop + OMRConfig.ROLL_Y[row] * sheetH;
            for (int col = 0; col < OMRConfig.ROLL_COLS; col++) {
                float sx = sheetLeft + OMRConfig.ROLL_X[col] * sheetW;
                result[idx][0] = (sx - cLeft) * scaleToImageX;
                result[idx][1] = (sy - cTop) * scaleToImageY;
                idx++;
            }
        }

        // 60 Answer dots
        for (int g = 0; g < OMRConfig.NUM_GROUPS; g++) {
            float[] groupX = OMRConfig.ANS_GROUP_X[g];
            for (int q = 0; q < OMRConfig.QUESTIONS_PER_GROUP; q++) {
                float sy = sheetTop + OMRConfig.ANS_Y[q] * sheetH;
                for (int opt = 0; opt < OMRConfig.OPTIONS_PER_Q; opt++) {
                    float sx = sheetLeft + groupX[opt] * sheetW;
                    result[idx][0] = (sx - cLeft) * scaleToImageX;
                    result[idx][1] = (sy - cTop) * scaleToImageY;
                    idx++;
                }
            }
        }

        return result;
    }

    /**
     * Roll dots in image coordinates (100 dots).
     */
    public float[][] getRollDotsInImageCoords(int imageWidth, int imageHeight) {
        float[][] all = getDotsInImageCoords(imageWidth, imageHeight);
        int count = OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS;
        float[][] roll = new float[count][2];
        System.arraycopy(all, 0, roll, 0, count);
        return roll;
    }

    /**
     * Answer dots in image coordinates (60 dots).
     */
    public float[][] getAnswerDotsInImageCoords(int imageWidth, int imageHeight) {
        float[][] all = getDotsInImageCoords(imageWidth, imageHeight);
        int rollCount = OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS;
        int ansCount = OMRConfig.NUM_GROUPS * OMRConfig.QUESTIONS_PER_GROUP * OMRConfig.OPTIONS_PER_Q;
        float[][] ans = new float[ansCount][2];
        System.arraycopy(all, rollCount, ans, 0, ansCount);
        return ans;
    }

    /**
     * Returns the exact (x, y) coordinates of all 160 guide dots in the PreviewView's
     * pixel space (matches previewView.getBitmap() 1:1 without letterbox offset).
     */
    public float[][] getDotsInPreviewCoords() {
        RectF sheet = computeSheetRect();
        int totalDots = OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS
                + OMRConfig.NUM_GROUPS * OMRConfig.QUESTIONS_PER_GROUP * OMRConfig.OPTIONS_PER_Q;

        float[][] result = new float[totalDots][2];
        if (sheet.width() <= 0 || sheet.height() <= 0) {
            return result;
        }

        float sheetLeft = sheet.left;
        float sheetTop = sheet.top;
        float sheetW = sheet.width();
        float sheetH = sheet.height();

        int idx = 0;
        // 100 Roll number dots
        for (int row = 0; row < OMRConfig.ROLL_ROWS; row++) {
            float sy = sheetTop + OMRConfig.ROLL_Y[row] * sheetH;
            for (int col = 0; col < OMRConfig.ROLL_COLS; col++) {
                float sx = sheetLeft + OMRConfig.ROLL_X[col] * sheetW;
                result[idx][0] = sx;
                result[idx][1] = sy;
                idx++;
            }
        }

        // 60 Answer dots
        for (int g = 0; g < OMRConfig.NUM_GROUPS; g++) {
            float[] groupX = OMRConfig.ANS_GROUP_X[g];
            for (int q = 0; q < OMRConfig.QUESTIONS_PER_GROUP; q++) {
                float sy = sheetTop + OMRConfig.ANS_Y[q] * sheetH;
                for (int opt = 0; opt < OMRConfig.OPTIONS_PER_Q; opt++) {
                    float sx = sheetLeft + groupX[opt] * sheetW;
                    result[idx][0] = sx;
                    result[idx][1] = sy;
                    idx++;
                }
            }
        }

        return result;
    }

    public float[][] getRollDotsInPreviewCoords() {
        float[][] all = getDotsInPreviewCoords();
        int count = OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS;
        float[][] roll = new float[count][2];
        System.arraycopy(all, 0, roll, 0, count);
        return roll;
    }

    public float[][] getAnswerDotsInPreviewCoords() {
        float[][] all = getDotsInPreviewCoords();
        int rollCount = OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS;
        int ansCount = OMRConfig.NUM_GROUPS * OMRConfig.QUESTIONS_PER_GROUP * OMRConfig.OPTIONS_PER_Q;
        float[][] ans = new float[ansCount][2];
        System.arraycopy(all, rollCount, ans, 0, ansCount);
        return ans;
    }
}
