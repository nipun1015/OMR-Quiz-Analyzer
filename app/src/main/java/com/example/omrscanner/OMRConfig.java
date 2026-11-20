
package com.example.omrscanner;

/**
 * OMR sheet configuration derived from OMR2_ultra_hd.pdf.
 *
 * All coordinates are expressed as fractions (0..1) of the full
 * sheet image dimensions (2382 × 3368 px in the reference PDF).
 *
 * The fractions were extracted by rendering the PDF at native resolution,
 * detecting every bubble circle with OpenCV HoughCircles, and dividing
 * each pixel coordinate by the image width or height.
 *
 * ---- Roll-number section ----
 *   10 columns  ×  10 rows (digits 0-9)  =  100 bubbles.
 *
 * ---- Answer section ----
 *   3 groups of 5 questions, 4 options (A B C D) each  =  60 bubbles.
 *     Group 1 : Q 1-5      Group 2 : Q 6-10     Group 3 : Q 11-15
 *
 * The class is intentionally stateless and holds only constants.
 */
public final class OMRConfig {

    private OMRConfig() { /* utility class */ }

    // ------------------------------------------------------------------ //
    //  Reference image used for measurement
    // ------------------------------------------------------------------ //
    public static final int REF_IMAGE_WIDTH  = 2382;
    public static final int REF_IMAGE_HEIGHT = 3368;

    // ------------------------------------------------------------------ //
    //  Roll-number bubble grid (normalised 0..1)
    // ------------------------------------------------------------------ //

    /** X fraction for each of the 10 roll-number columns. */
    public static final float[] ROLL_X = {
        0.602015f, 0.629723f, 0.656591f, 0.684299f, 0.711167f,
        0.738875f, 0.765743f, 0.793451f, 0.820319f, 0.848027f
    };

    /** Y fraction for each of the 10 digit rows (0 through 9). */
    public static final float[] ROLL_Y = {
        0.214371f, 0.232779f, 0.251188f, 0.269596f, 0.288005f,
        0.306413f, 0.324822f, 0.343230f, 0.361639f, 0.380048f
    };

    public static final int ROLL_COLS = ROLL_X.length; // 10
    public static final int ROLL_ROWS = ROLL_Y.length; // 10

    // ------------------------------------------------------------------ //
    //  Answer bubble grid (normalised 0..1)
    // ------------------------------------------------------------------ //

    /** X fractions for group 1 options A-D (questions 1-5). */
    public static final float[] ANS_G1_X = {
        0.089001f, 0.151134f, 0.213266f, 0.275399f
    };

    /** X fractions for group 2 options A-D (questions 6-10). */
    public static final float[] ANS_G2_X = {
        0.411419f, 0.473552f, 0.535684f, 0.597817f
    };

    /** X fractions for group 3 options A-D (questions 11-15). */
    public static final float[] ANS_G3_X = {
        0.732997f, 0.795970f, 0.859782f, 0.922754f
    };

    /** Combined X-fraction arrays for all three groups. */
    public static final float[][] ANS_GROUP_X = {
        ANS_G1_X, ANS_G2_X, ANS_G3_X
    };

    /** Y fractions for the 5 question rows within each group. */
    public static final float[] ANS_Y = {
        0.443884f, 0.465261f, 0.486045f, 0.507126f, 0.529394f
    };

    public static final int QUESTIONS_PER_GROUP = ANS_Y.length;   // 5
    public static final int NUM_GROUPS          = ANS_GROUP_X.length; // 3
    public static final int OPTIONS_PER_Q       = ANS_G1_X.length;   // 4
    public static final int TOTAL_QUESTIONS     = QUESTIONS_PER_GROUP * NUM_GROUPS; // 15

    // ------------------------------------------------------------------ //
    //  Sheet aspect ratio (width / height)
    // ------------------------------------------------------------------ //
    public static final float SHEET_ASPECT =
        (float) REF_IMAGE_WIDTH / REF_IMAGE_HEIGHT;   // ~0.7074
}

