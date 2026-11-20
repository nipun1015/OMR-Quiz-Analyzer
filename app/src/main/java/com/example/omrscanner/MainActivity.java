package com.example.omrscanner;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    // Camera & Views
    private PreviewView previewView;
    private ImageView capturedImageView;
    private AlignmentOverlayView alignmentOverlayView;
    private ImageCapture imageCapture;

    // Header & Control Panels
    private LinearLayout topHeaderLayout;
    private LinearLayout bottomActionBar;
    private LinearLayout calibrationControlsPanel;
    private LinearLayout llProcessingOverlay;
    private TextView tvCountdown;

    // Headers & Labels
    private TextView tvActiveExamHeader;
    private TextView calibrationStatusText;

    // Buttons
    private Button captureButton;
    private Button btnBackToHome;
    private Button btnToggleDebug;
    private Button btnToggleControls;
    private Button btnLock;
    private Button btnMoveUp;
    private Button btnMoveDown;
    private Button btnMoveLeft;
    private Button btnMoveRight;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private Button btnResetCalibration;

    // State
    private ExamManager examManager;
    private Exam activeExam;
    private boolean isDebugMode = true;
    private int timerSeconds = 0; // kept for future use
    private boolean isCapturing = false;
    private final ExecutorService processingExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        examManager = new ExamManager(this);
        loadActiveExam();

        // 1. Initialize Views
        previewView = findViewById(R.id.previewView);
        capturedImageView = findViewById(R.id.capturedImageView);
        alignmentOverlayView = findViewById(R.id.alignmentOverlayView);
        topHeaderLayout = findViewById(R.id.topHeaderLayout);
        bottomActionBar = findViewById(R.id.bottomActionBar);
        calibrationControlsPanel = findViewById(R.id.calibrationControlsPanel);
        llProcessingOverlay = findViewById(R.id.llProcessingOverlay);
        tvCountdown = findViewById(R.id.tvCountdown);

        tvActiveExamHeader = findViewById(R.id.tvActiveExamHeader);
        calibrationStatusText = findViewById(R.id.calibrationStatusText);

        captureButton = findViewById(R.id.captureButton);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        btnToggleDebug = findViewById(R.id.btnToggleDebug);
        btnToggleControls = findViewById(R.id.btnToggleControls);
        btnLock = findViewById(R.id.btnLock);

        btnMoveUp = findViewById(R.id.btnMoveUp);
        btnMoveDown = findViewById(R.id.btnMoveDown);
        btnMoveLeft = findViewById(R.id.btnMoveLeft);
        btnMoveRight = findViewById(R.id.btnMoveRight);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnResetCalibration = findViewById(R.id.btnResetCalibration);

        updateExamHeader();

        // 2. Initialize OpenCV
        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
        }

        // 3. Listen for PreviewView layout changes to update content area
        previewView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            updatePreviewContentArea();
        });

        // 4. Setup Listeners
        setupControlListeners();

        // 5. Camera Permission Check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveExam();
        updateExamHeader();
        unfreezeCameraPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        processingExecutor.shutdown();
    }

    private void loadActiveExam() {
        String examId = getIntent().getStringExtra("exam_id");
        if (examId != null) {
            activeExam = examManager.getExamById(examId);
            getIntent().removeExtra("exam_id");
        }
        if (activeExam == null) {
            activeExam = examManager.getActiveExam();
        }
    }

    private void updateExamHeader() {
        if (activeExam != null) {
            tvActiveExamHeader.setText("Exam: " + activeExam.getName() + " ▾");
        } else {
            tvActiveExamHeader.setText("Exam: General ▾");
        }
    }

    private void showSelectExamDialog() {
        List<Exam> exams = examManager.getAllExams();
        if (exams.isEmpty()) {
            Toast.makeText(this, "No exams available.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] examNames = new String[exams.size()];
        int selectedIndex = 0;
        Exam currentActive = (activeExam != null) ? activeExam : examManager.getActiveExam();
        for (int i = 0; i < exams.size(); i++) {
            examNames[i] = exams.get(i).getName();
            if (currentActive != null && currentActive.getId().equals(exams.get(i).getId())) {
                selectedIndex = i;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Switch Examination Key")
                .setSingleChoiceItems(examNames, selectedIndex, (dialog, which) -> {
                    Exam selected = exams.get(which);
                    examManager.setActiveExamId(selected.getId());
                    activeExam = selected;
                    updateExamHeader();
                    dialog.dismiss();
                    Toast.makeText(this, "Active Exam switched to: " + selected.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePreviewContentArea() {
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0)
            return;

        float streamAspect = 3.0f / 4.0f;
        float viewAspect = (float) viewWidth / (float) viewHeight;

        float renderedWidth;
        float renderedHeight;
        float left;
        float top;

        if (viewAspect > streamAspect) {
            renderedHeight = viewHeight;
            renderedWidth = viewHeight * streamAspect;
            left = (viewWidth - renderedWidth) / 2f;
            top = 0f;
        } else {
            renderedWidth = viewWidth;
            renderedHeight = viewWidth / streamAspect;
            left = 0f;
            top = (viewHeight - renderedHeight) / 2f;
        }

        alignmentOverlayView.setCameraContentArea(left, top, renderedWidth, renderedHeight);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics());
    }

    private void setupControlListeners() {
        btnBackToHome.setOnClickListener(v -> finish());
        tvActiveExamHeader.setOnClickListener(v -> showSelectExamDialog());

        // Toggle Visual Debug Mode
        btnToggleDebug.setOnClickListener(v -> {
            isDebugMode = !isDebugMode;
            btnToggleDebug.setText(isDebugMode ? "Debug: ON" : "Debug: OFF");
            btnToggleDebug.setBackgroundColor(isDebugMode ? 0xFF1976D2 : 0xFF424242);
            Toast.makeText(this, "Visual Debug Mode: " + (isDebugMode ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        // Lock / unlock overlay position
        btnLock.setOnClickListener(v -> {
            boolean nowLocked = !alignmentOverlayView.isLocked();
            alignmentOverlayView.setLocked(nowLocked);
            if (nowLocked) {
                btnLock.setText("🔒 Locked");
                btnLock.setBackgroundColor(0xFFD32F2F);
                Toast.makeText(this, "Overlay locked", Toast.LENGTH_SHORT).show();
            } else {
                btnLock.setText("🔓 Lock");
                btnLock.setBackgroundColor(0xFF424242);
                Toast.makeText(this, "Overlay unlocked", Toast.LENGTH_SHORT).show();
            }
        });

        // Capture button with instant freeze
        captureButton.setOnClickListener(v -> onCaptureClicked());

        // Toggle adjustment panel visibility
        btnToggleControls.setOnClickListener(v -> {
            if (calibrationControlsPanel.getVisibility() == View.VISIBLE) {
                calibrationControlsPanel.setVisibility(View.GONE);
                btnToggleControls.setText("Adjust ⚙");
            } else {
                calibrationControlsPanel.setVisibility(View.VISIBLE);
                btnToggleControls.setText("Close ✕");
            }
        });

        // Fine navigation buttons
        final float moveStepPx = dpToPx(8);
        btnMoveUp.setOnClickListener(v -> alignmentOverlayView.moveOverlay(0, -moveStepPx));
        btnMoveDown.setOnClickListener(v -> alignmentOverlayView.moveOverlay(0, moveStepPx));
        btnMoveLeft.setOnClickListener(v -> alignmentOverlayView.moveOverlay(-moveStepPx, 0));
        btnMoveRight.setOnClickListener(v -> alignmentOverlayView.moveOverlay(moveStepPx, 0));

        // Fine size buttons
        btnZoomIn.setOnClickListener(v -> alignmentOverlayView.scaleOverlay(0.04f));
        btnZoomOut.setOnClickListener(v -> alignmentOverlayView.scaleOverlay(-0.04f));

        // Reset calibration
        btnResetCalibration.setOnClickListener(v -> {
            alignmentOverlayView.resetCalibration();
            Toast.makeText(this, "Overlay position reset", Toast.LENGTH_SHORT).show();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture);

                previewView.post(this::updatePreviewContentArea);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera initialization failed!", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Handles capture button click with INSTANT preview freeze.
     */
    private void onCaptureClicked() {
        if (isCapturing)
            return;
        freezeAndProcessInstantFrame();
    }

    /**
     * Instantly grabs the current preview frame, freezes the display on screen to
     * prevent
     * any post-click hand-shake movement, and processes the OMR sheet.
     */
    private void freezeAndProcessInstantFrame() {
        isCapturing = true;
        captureButton.setEnabled(false);

        // 1. Instantly grab the frame at the microsecond of the click
        Bitmap frozenBitmap = previewView.getBitmap();

        if (frozenBitmap == null) {
            // Fallback to ImageCapture if preview bitmap is not available
            captureWithCameraXFallback();
            return;
        }

        // 2. Visually freeze the screen immediately:
        // Show the frozen bitmap on capturedImageView and hide the moving live preview!
        capturedImageView.setImageBitmap(frozenBitmap);
        capturedImageView.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.GONE);
        alignmentOverlayView.setVisibility(View.GONE);
        llProcessingOverlay.setVisibility(View.VISIBLE);

        // 3. Process the frozen frame asynchronously with zero hand shake
        processingExecutor.execute(() -> {
            try {
                // Get dots matching preview bitmap coordinates 1:1
                float[][] rollDots = alignmentOverlayView.getRollDotsInPreviewCoords();
                float[][] answerDots = alignmentOverlayView.getAnswerDotsInPreviewCoords();

                // Run OpenCV adaptive evaluation
                BubbleDetectionResult detectionResult = OMRProcessor.processSheet(
                        frozenBitmap, rollDots, answerDots, isDebugMode);

                // Save frozen photo copy to Gallery in background
                saveBitmapToGallery(frozenBitmap);

                // Evaluate against active exam
                OMRResult evaluationResult = EvaluationEngine.evaluate(activeExam, detectionResult);

                // Set in-memory session for ResultActivity
                ScanSession.setSession(evaluationResult, detectionResult.getDebugBitmap());

                mainHandler.post(() -> {
                    isCapturing = false;
                    llProcessingOverlay.setVisibility(View.GONE);

                    // Launch ResultActivity
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    unfreezeCameraPreview();
                    Toast.makeText(MainActivity.this, "Processing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Unfreezes the camera preview and restores the live camera when returning from
     * results.
     */
    private void unfreezeCameraPreview() {
        isCapturing = false;
        if (capturedImageView != null) {
            capturedImageView.setVisibility(View.GONE);
            capturedImageView.setImageDrawable(null);
        }
        if (previewView != null) {
            previewView.setVisibility(View.VISIBLE);
        }
        if (alignmentOverlayView != null) {
            alignmentOverlayView.setVisibility(View.VISIBLE);
        }
        if (llProcessingOverlay != null) {
            llProcessingOverlay.setVisibility(View.GONE);
        }
        if (tvCountdown != null) {
            tvCountdown.setVisibility(View.GONE);
        }
        if (captureButton != null) {
            captureButton.setEnabled(true);
        }
        updatePreviewContentArea();
    }

    private void captureWithCameraXFallback() {
        if (imageCapture == null) {
            unfreezeCameraPreview();
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        File picturesDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picturesDirectory == null)
            picturesDirectory = getFilesDir();
        File photoFile = new File(picturesDirectory, "OMR_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        if (previewView.getDisplay() != null) {
            imageCapture.setTargetRotation(previewView.getDisplay().getRotation());
        }

        llProcessingOverlay.setVisibility(View.VISIBLE);

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        processingExecutor.execute(() -> {
                            try {
                                Bitmap rawBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                                Bitmap orientedBitmap = rotateBitmapIfNeeded(rawBitmap, photoFile);
                                int imgW = orientedBitmap.getWidth();
                                int imgH = orientedBitmap.getHeight();

                                float[][] rollDots = alignmentOverlayView.getRollDotsInImageCoords(imgW, imgH);
                                float[][] answerDots = alignmentOverlayView.getAnswerDotsInImageCoords(imgW, imgH);

                                BubbleDetectionResult detectionResult = OMRProcessor.processSheet(
                                        orientedBitmap, rollDots, answerDots, isDebugMode);

                                OMRResult evaluationResult = EvaluationEngine.evaluate(activeExam, detectionResult);
                                ScanSession.setSession(evaluationResult, detectionResult.getDebugBitmap());

                                mainHandler.post(() -> {
                                    unfreezeCameraPreview();
                                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                                    startActivity(intent);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                mainHandler.post(() -> {
                                    unfreezeCameraPreview();
                                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG)
                                            .show();
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        unfreezeCameraPreview();
                        Toast.makeText(MainActivity.this, "Capture failed: " + exception.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        if (bitmap == null)
            return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "OMR_" + System.currentTimeMillis() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OMRScanner");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    try (OutputStream output = getContentResolver().openOutputStream(imageUri)) {
                        if (output != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output);
                        }
                    }
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(imageUri, values, null, null);
                }
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (dir != null) {
                    File file = new File(dir, "OMR_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, File photoFile) {
        try {
            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
            int rotationDegrees = exif.getRotationDegrees();
            if (rotationDegrees == 0)
                return bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            return Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
