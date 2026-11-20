package com.example.omrscanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Computer vision processor for OMR sheets using OpenCV.
 * Analyzes the 100 Roll Number bubbles and 60 Answer bubbles using
 * calibrated guide-dot coordinates without any neural network or cloud service.
 */
public class OMRProcessor {

    // Thresholds calibrated against actual printed Option 2 template
    // Roll number bubbles have digits '0'-'9' printed inside, so their empty darkness is ~0.15 - 0.26
    private static final float ROLL_FILL_THRESHOLD = 0.42f;
    private static final float ROLL_CONTRAST_MARGIN = 0.14f;

    // Answer bubbles are clean empty circles without digits inside, so empty darkness is < 0.10
    private static final float ANS_FILL_THRESHOLD = 0.32f;
    private static final float ANS_CONTRAST_MARGIN = 0.12f;

    // Dark pixel threshold in 8-bit grayscale (0=black, 255=white)
    private static final int PIXEL_DARK_THRESHOLD = 130;

    /**
     * Complete OMR evaluation from a captured photo and calibrated dot coordinates.
     *
     * @param photoBitmap          Captured Bitmap (rotated according to EXIF)
     * @param rollDots             100 dot coordinates [row*10 + col][2]
     * @param answerDots           60 dot coordinates [(g*5 + q)*4 + opt][2]
     * @param generateDebugVisual  Whether to create a visual debug bitmap with metrics
     * @return BubbleDetectionResult containing roll number, answers, fill matrices, and debug image
     */
    public static BubbleDetectionResult processSheet(
            Bitmap photoBitmap,
            float[][] rollDots,
            float[][] answerDots,
            boolean generateDebugVisual) {

        if (photoBitmap == null) {
            throw new IllegalArgumentException("Input Bitmap is null");
        }

        int imgW = photoBitmap.getWidth();
        int imgH = photoBitmap.getHeight();

        // 1. Convert Android Bitmap to OpenCV Mat
        Mat bgrMat = new Mat();
        Utils.bitmapToMat(photoBitmap, bgrMat);

        Mat grayMat = new Mat(imgH, imgW, CvType.CV_8UC1);
        Imgproc.cvtColor(bgrMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
        bgrMat.release();

        // 2. Compute dynamic bubble sampling radius from dot spacing
        float colSpacing = 60f;
        if (rollDots != null && rollDots.length >= 2) {
            float dx = rollDots[1][0] - rollDots[0][0];
            float dy = rollDots[1][1] - rollDots[0][1];
            colSpacing = (float) Math.sqrt(dx * dx + dy * dy);
        }
        int bubbleRadius = Math.max(6, Math.round(colSpacing * 0.22f));

        // 3. Measure Roll Number darkness: 10 columns x 10 rows
        float[][] rollDarkness = new float[OMRConfig.ROLL_COLS][OMRConfig.ROLL_ROWS];
        for (int row = 0; row < OMRConfig.ROLL_ROWS; row++) {
            for (int col = 0; col < OMRConfig.ROLL_COLS; col++) {
                int dotIndex = row * OMRConfig.ROLL_COLS + col;
                if (rollDots != null && dotIndex < rollDots.length) {
                    int cx = Math.round(rollDots[dotIndex][0]);
                    int cy = Math.round(rollDots[dotIndex][1]);
                    rollDarkness[col][row] = measureBubbleFill(grayMat, cx, cy, bubbleRadius);
                }
            }
        }

        // 4. Measure Answer darkness: 15 questions x 4 options
        float[][] answerDarkness = new float[OMRConfig.TOTAL_QUESTIONS][OMRConfig.OPTIONS_PER_Q];
        for (int g = 0; g < OMRConfig.NUM_GROUPS; g++) {
            for (int q = 0; q < OMRConfig.QUESTIONS_PER_GROUP; q++) {
                int questionIndex = g * OMRConfig.QUESTIONS_PER_GROUP + q;
                for (int opt = 0; opt < OMRConfig.OPTIONS_PER_Q; opt++) {
                    int dotIndex = questionIndex * OMRConfig.OPTIONS_PER_Q + opt;
                    if (answerDots != null && dotIndex < answerDots.length) {
                        int cx = Math.round(answerDots[dotIndex][0]);
                        int cy = Math.round(answerDots[dotIndex][1]);
                        answerDarkness[questionIndex][opt] = measureBubbleFill(grayMat, cx, cy, bubbleRadius);
                    }
                }
            }
        }

        // 5. Decode Roll Number digits
        StringBuilder rollBuilder = new StringBuilder();
        int[] detectedRollDigits = new int[OMRConfig.ROLL_COLS];

        for (int col = 0; col < OMRConfig.ROLL_COLS; col++) {
            float[] colValues = rollDarkness[col];
            int markedCount = 0;
            int bestRow = -1;
            float maxFill = -1f;
            float secondMaxFill = -1f;

            for (int row = 0; row < OMRConfig.ROLL_ROWS; row++) {
                float fill = colValues[row];
                if (fill > maxFill) {
                    secondMaxFill = maxFill;
                    maxFill = fill;
                    bestRow = row;
                } else if (fill > secondMaxFill) {
                    secondMaxFill = fill;
                }
                if (fill >= ROLL_FILL_THRESHOLD) {
                    markedCount++;
                }
            }

            // Decide digit
            if (markedCount == 1 && bestRow != -1 && (maxFill - secondMaxFill >= ROLL_CONTRAST_MARGIN || maxFill > 0.50f)) {
                rollBuilder.append(bestRow);
                detectedRollDigits[col] = bestRow;
            } else if (markedCount > 1) {
                rollBuilder.append('M'); // Multiple marked
                detectedRollDigits[col] = -2;
            } else {
                // If markedCount == 0, check if max fill is close to threshold and clearly stands out
                if (maxFill >= 0.38f && (maxFill - secondMaxFill >= ROLL_CONTRAST_MARGIN + 0.05f)) {
                    rollBuilder.append(bestRow);
                    detectedRollDigits[col] = bestRow;
                } else {
                    rollBuilder.append('?'); // Unanswered / missing
                    detectedRollDigits[col] = -1;
                }
            }
        }
        String detectedRoll = rollBuilder.toString();

        // 6. Decode Answers: 15 questions
        char[] detectedAnswers = new char[OMRConfig.TOTAL_QUESTIONS];
        char[] optionChars = {'A', 'B', 'C', 'D'};

        for (int q = 0; q < OMRConfig.TOTAL_QUESTIONS; q++) {
            float[] qValues = answerDarkness[q];
            int markedCount = 0;
            int bestOpt = -1;
            float maxFill = -1f;
            float secondMaxFill = -1f;

            for (int opt = 0; opt < OMRConfig.OPTIONS_PER_Q; opt++) {
                float fill = qValues[opt];
                if (fill > maxFill) {
                    secondMaxFill = maxFill;
                    maxFill = fill;
                    bestOpt = opt;
                } else if (fill > secondMaxFill) {
                    secondMaxFill = fill;
                }
                if (fill >= ANS_FILL_THRESHOLD) {
                    markedCount++;
                }
            }

            if (markedCount == 1 && bestOpt != -1 && (maxFill - secondMaxFill >= ANS_CONTRAST_MARGIN || maxFill > 0.45f)) {
                detectedAnswers[q] = optionChars[bestOpt];
            } else if (markedCount > 1) {
                detectedAnswers[q] = 'M'; // Multiple marked
            } else {
                if (maxFill >= 0.28f && (maxFill - secondMaxFill >= ANS_CONTRAST_MARGIN + 0.05f)) {
                    detectedAnswers[q] = optionChars[bestOpt];
                } else {
                    detectedAnswers[q] = '?'; // Unanswered
                }
            }
        }

        // 7. Visual Debugging Mode
        Bitmap debugBitmap = null;
        if (generateDebugVisual) {
            debugBitmap = generateVisualDebugBitmap(
                    photoBitmap, rollDots, answerDots,
                    rollDarkness, answerDarkness,
                    detectedRollDigits, detectedAnswers, bubbleRadius
            );
        }

        grayMat.release();

        return new BubbleDetectionResult(
                detectedRoll,
                detectedAnswers,
                rollDarkness,
                answerDarkness,
                debugBitmap
        );
    }

    /**
     * Measures the fraction of dark pixels (ink) inside a circular region around (cx, cy).
     */
    private static float measureBubbleFill(Mat grayMat, int cx, int cy, int radius) {
        int w = grayMat.cols();
        int h = grayMat.rows();

        int x1 = Math.max(0, cx - radius);
        int x2 = Math.min(w - 1, cx + radius);
        int y1 = Math.max(0, cy - radius);
        int y2 = Math.min(h - 1, cy + radius);

        if (x2 <= x1 || y2 <= y1) return 0f;

        int darkPixels = 0;
        int totalCirclePixels = 0;
        int radiusSq = radius * radius;

        byte[] rowData = new byte[x2 - x1 + 1];

        for (int y = y1; y <= y2; y++) {
            int dy = y - cy;
            int dySq = dy * dy;

            grayMat.get(y, x1, rowData);

            for (int x = x1; x <= x2; x++) {
                int dx = x - cx;
                if (dx * dx + dySq <= radiusSq) {
                    totalCirclePixels++;
                    int pixelVal = rowData[x - x1] & 0xFF;
                    if (pixelVal < PIXEL_DARK_THRESHOLD) {
                        darkPixels++;
                    }
                }
            }
        }

        if (totalCirclePixels == 0) return 0f;
        return (float) darkPixels / (float) totalCirclePixels;
    }

    /**
     * Generates an annotated visual debugging image showing sampled bubble regions,
     * measured fill percentages, and detection decisions.
     */
    private static Bitmap generateVisualDebugBitmap(
            Bitmap originalBitmap,
            float[][] rollDots,
            float[][] answerDots,
            float[][] rollDarkness,
            float[][] answerDarkness,
            int[] detectedRollDigits,
            char[] detectedAnswers,
            int radius) {

        Bitmap debugBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(debugBmp);

        Paint paintMarked = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarked.setColor(Color.GREEN);
        paintMarked.setStyle(Paint.Style.STROKE);
        paintMarked.setStrokeWidth(Math.max(3f, radius * 0.35f));

        Paint paintUnmarked = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintUnmarked.setColor(Color.argb(180, 200, 200, 200));
        paintUnmarked.setStyle(Paint.Style.STROKE);
        paintUnmarked.setStrokeWidth(Math.max(1.5f, radius * 0.15f));

        Paint paintConflict = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintConflict.setColor(Color.RED);
        paintConflict.setStyle(Paint.Style.STROKE);
        paintConflict.setStrokeWidth(Math.max(3.5f, radius * 0.4f));

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(Math.max(12f, radius * 0.9f));
        textPaint.setFakeBoldText(true);

        Paint bgTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgTextPaint.setColor(Color.argb(180, 0, 0, 0));
        bgTextPaint.setStyle(Paint.Style.FILL);

        // 1. Draw Roll Number bubbles
        for (int row = 0; row < OMRConfig.ROLL_ROWS; row++) {
            for (int col = 0; col < OMRConfig.ROLL_COLS; col++) {
                int dotIndex = row * OMRConfig.ROLL_COLS + col;
                if (rollDots != null && dotIndex < rollDots.length) {
                    float x = rollDots[dotIndex][0];
                    float y = rollDots[dotIndex][1];
                    float fill = rollDarkness[col][row];

                    int detectedDigit = detectedRollDigits[col];
                    if (detectedDigit == -2) { // Multiple marked
                        if (fill >= ROLL_FILL_THRESHOLD) {
                            canvas.drawCircle(x, y, radius, paintConflict);
                        } else {
                            canvas.drawCircle(x, y, radius, paintUnmarked);
                        }
                    } else if (detectedDigit == row) {
                        canvas.drawCircle(x, y, radius, paintMarked);
                    } else {
                        canvas.drawCircle(x, y, radius, paintUnmarked);
                    }

                    // Draw fill percentage text for high values
                    if (fill >= 0.30f) {
                        String txt = Math.round(fill * 100) + "%";
                        canvas.drawRect(x - radius, y - radius - 14, x + radius, y - radius, bgTextPaint);
                        canvas.drawText(txt, x - radius + 2, y - radius - 2, textPaint);
                    }
                }
            }
        }

        // 2. Draw Answer bubbles
        char[] optionChars = {'A', 'B', 'C', 'D'};
        for (int g = 0; g < OMRConfig.NUM_GROUPS; g++) {
            for (int q = 0; q < OMRConfig.QUESTIONS_PER_GROUP; q++) {
                int questionIndex = g * OMRConfig.QUESTIONS_PER_GROUP + q;
                char detectedChar = detectedAnswers[questionIndex];

                for (int opt = 0; opt < OMRConfig.OPTIONS_PER_Q; opt++) {
                    int dotIndex = questionIndex * OMRConfig.OPTIONS_PER_Q + opt;
                    if (answerDots != null && dotIndex < answerDots.length) {
                        float x = answerDots[dotIndex][0];
                        float y = answerDots[dotIndex][1];
                        float fill = answerDarkness[questionIndex][opt];
                        char optChar = optionChars[opt];

                        if (detectedChar == 'M') {
                            if (fill >= ANS_FILL_THRESHOLD) {
                                canvas.drawCircle(x, y, radius, paintConflict);
                            } else {
                                canvas.drawCircle(x, y, radius, paintUnmarked);
                            }
                        } else if (detectedChar == optChar) {
                            canvas.drawCircle(x, y, radius, paintMarked);
                        } else {
                            canvas.drawCircle(x, y, radius, paintUnmarked);
                        }

                        if (fill >= 0.25f) {
                            String txt = Math.round(fill * 100) + "%";
                            canvas.drawRect(x - radius, y - radius - 14, x + radius, y - radius, bgTextPaint);
                            canvas.drawText(txt, x - radius + 2, y - radius - 2, textPaint);
                        }
                    }
                }
            }
        }

        return debugBmp;
    }
}
