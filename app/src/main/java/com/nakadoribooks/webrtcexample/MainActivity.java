package com.nakadoribooks.webrtcexample;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1;
    private WebRTC webRTC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // checkPermission → onRequestPermissionsResult → setupWebRTC
        checkPermission();
    }

    private void checkPermission(){
        String[] permissioins = new String[]{ Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissioins, REQUEST_CODE_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION)
            return;

        webRTC = new WebRTC(this);
        webRTC.startCapture();
    }

}
