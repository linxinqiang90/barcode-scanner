package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRCodeScanner {

    private static final String TAG = "QRCodeScanner";

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageAnalysis imageAnalysis;
    private QRCodeCallback callback;
    private volatile boolean scanning = false;
    private final PreviewView previewView;
    private Size targetResolution = new Size(1280, 720);
    private CameraSelector cameraSelector;
    private Set<String> codes = new HashSet<>();
    private boolean isCameraInitialized = false;

    public interface QRCodeCallback {
        void onQRCodeDetected(String qrCode);
        void onError(String error);
    }

    public QRCodeScanner(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        this.previewView = previewView;

        this.barcodeScanner = BarcodeScanning.getClient();
        initCameraOnce();
    }

    public void setFormats(int... moreFormats) {
        if (moreFormats == null || moreFormats.length == 0) {
            throw new IllegalArgumentException("至少要传入一个条码格式");
        }

        BarcodeScannerOptions.Builder builder = new BarcodeScannerOptions.Builder();
        if (moreFormats.length > 1) {
            builder.setBarcodeFormats(moreFormats[0], Arrays.copyOfRange(moreFormats, 1, moreFormats.length));
        } else {
            builder.setBarcodeFormats(moreFormats[0]);
        }
        this.barcodeScanner = BarcodeScanning.getClient(builder.build());
    }

    public void setTargetResolution(Size targetResolution) {
        this.targetResolution = targetResolution;
    }

    public void setCallback(QRCodeCallback callback) {
        this.callback = callback;
    }

    /**
     * 按下按钮开始扫码 + 开灯
     **/
    public void startScanning() {
        if (!isCameraInitialized) {
            postError("相机初始化中，请稍候...");
            return;
        }

        if (!scanning) {
            scanning = true;
            codes.clear();

            if (imageAnalysis != null) {
                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);
            }

            if (camera != null) {
                camera.getCameraControl().enableTorch(true);
            }
        }
    }

    /**
     * 松开按钮停止扫码 + 关灯
     **/
    public void stopScanning() {
        scanning = false;

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer(); // 停止分析
        }

        if (camera != null) {
            camera.getCameraControl().enableTorch(false);
        }
        // 不解绑 CameraProvider，保持相机 ready
    }

    /**
     * 只初始化一次相机
     **/
    private void initCameraOnce() {
        if (isCameraInitialized) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(this.targetResolution)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                Preview preview = null;
                if (previewView != null) {
                    preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                }

                if (preview != null) {
                    camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                    );
                } else {
                    camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageAnalysis
                    );
                }

                isCameraInitialized = true;

            } catch (ExecutionException | InterruptedException e) {
                postError("初始化相机失败: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * 图像处理
     **/
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (!scanning) {
            imageProxy.close();
            return;
        }

        try {
            if (imageProxy.getImage() == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String value = barcode.getRawValue();
                            if (value != null && callback != null && !codes.contains(value)) {
                                codes.add(value);
                                callback.onQRCodeDetected(value);
                            }
                        }
                    })
                    .addOnFailureListener(e -> postError("扫码失败: " + e.getMessage()))
                    .addOnCompleteListener(task -> imageProxy.close());

        } catch (Exception e) {
            postError("图像处理异常: " + e.getMessage());
            imageProxy.close();
        }
    }

    private void postError(String error) {
        Log.e(TAG, error);
        if (callback != null) {
            callback.onError(error);
        }
    }

    /**
     * 释放资源
     **/
    public void release() {
        stopScanning();
        cameraExecutor.shutdown();
    }
}
