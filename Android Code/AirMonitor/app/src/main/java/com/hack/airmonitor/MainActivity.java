package com.hack.airmonitor;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;

import io.rapid.Rapid;

public class MainActivity extends BlunoLibrary {
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    final private int REQUEST_ASK_ALERT = 124;
    WebView webView;
    Button btnStart;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        Rapid.getInstance().collection("airdata", ServiceFloating.AirData.class).newDocument()
                .mutate(new ServiceFloating.AirData(new ServiceFloating.Location(0,0), 0, 0, 0, 0))
                .onSuccess(() -> {
                    Log.d("foobar", "Success");
                })
                .onError(error -> {
                    switch(error.getType()){
                        case TIMEOUT:
                            Log.e("foobar", "timeout");
                            break; // mutation timed out
                        case PERMISSION_DENIED:
                            Log.e("foobar", "PERMISSION_DENIED");
                            break; // access control related error
                    }
                });
*/
        Toolbar toolbar = (Toolbar)findViewById(R.id.my_toolbar);
        this.setActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        this.getActionBar().setTitle(R.string.app_name);
        webView = (WebView)findViewById(R.id.webView);
        btnStart = (Button)findViewById(R.id.btnStart);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("https://nyceane.github.io/");
        btnStart.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
            }
        });
        checkForCourseLocation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void onResume(){
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!hasPermissions())
            {
                return;
            }
        }
        System.out.println("BlUNOActivity onResume");
        onResumeProcess();														//onResume Process by BlunoLibrary
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!hasPermissions())
            {
                return;
            }
        }
        onPauseProcess();														//onPause Process by BlunoLibrary
    }

    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!hasPermissions())
            {
                return;
            }
        }

        onStopProcess();														//onStop Process by BlunoLibrary
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!hasPermissions())
            {
                return;
            }
        }
        onDestroyProcess();														//onDestroy Process by BlunoLibrary
    }


    @Override
    public void onConectionStateChange(connectionStateEnum theConnectionState) {
        switch (theConnectionState) {											//Four connection state
            case isConnected:
                Log.e("Foobar", "Connected");
                //buttonScan.setText("Connected");
                break;
            case isConnecting:
                Log.e("Fobar", "Connecting");
                break;
            case isToScan:
                Log.e("Fobar", "Scan");
                break;
            case isScanning:
                Log.e("Fobar", "Scanning");
                break;
            case isDisconnecting:
                Log.e("Fobar", "isDisconnecting");
                break;
            default:
                break;
        }
    }

    @Override
    public void onSerialReceived(String theString) {
        Log.e("Foobar", theString);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkForCourseLocation(){
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        if (!Settings.canDrawOverlays(MainActivity.this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_ASK_ALERT);
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        bleScan();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    checkForCourseLocation();
                }  else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "ACCESS_COARSE_LOCATION Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case REQUEST_ASK_ALERT:
                checkForCourseLocation();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void bleScan()
    {
        onCreateProcess();                                                        //onCreate Process by BlunoLibrary
        serialBegin(9600);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermissions()
    {
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }
        if(checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }
        return true;
    }

}
