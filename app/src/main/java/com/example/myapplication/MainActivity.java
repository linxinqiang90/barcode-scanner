package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Size;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private QRCodeScanner scanner;
    private QRCodeAdapter adapter;
    private boolean readState = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreviewView previewView = findViewById(R.id.previewView);

        RecyclerView recyclerView = findViewById(R.id.qrCodeList);
        adapter = new QRCodeAdapter();
        recyclerView.setAdapter(adapter);

        // 让最新的数据显示在最上面
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                this, RecyclerView.VERTICAL, false
        ));


        scanner = new QRCodeScanner(this, this, previewView);
        scanner.setTargetResolution(new Size(640, 480));
        scanner.setFormats(Barcode.FORMAT_QR_CODE);
//        scanner.setTargetResolution(new Size(1280,720));


        scanner.setCallback(new QRCodeScanner.QRCodeCallback() {
            @Override
            public void onQRCodeDetected(Set<String> qrCodes) {
                runOnUiThread(() -> {
                    adapter.addQRCode(qrCodes);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });

        KeyReceiver keyReceiver = new KeyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.rfid.FUN_KEY");
        filter.addAction("android.intent.action.FUN_KEY");
        this.registerReceiver(keyReceiver, filter);
    }

    //
    private class KeyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int keyCode = intent.getIntExtra("keyCode", 0);
            if (keyCode == 0) {
                keyCode = intent.getIntExtra("keycode", 0);
            }
            boolean keyDown = intent.getBooleanExtra("keydown", false);
            if (keyDown) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_F7://H3100

                        scanner.startScanning();

                        readState = true;
                        break;
                }
            } else {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_F1:
                        break;
                    case KeyEvent.KEYCODE_F2:
                        break;
                    case KeyEvent.KEYCODE_F5:
                        break;
                    case KeyEvent.KEYCODE_F3://C510x
                    case KeyEvent.KEYCODE_F4://6100
                    case KeyEvent.KEYCODE_F7://H3100
                        scanner.stopScanning();
                        runOnUiThread(() -> adapter.clear());
                        readState = false;

                        break;
                }
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) {
            scanner.release();
        }
    }

}