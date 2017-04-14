package com.nakadoribooks.webrtcexample;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import rx.android.app.AppObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.WampError;

import org.webrtc.*;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1;
    private WampClient wamp;
    private WebRTC webRTC;

    private static final String AnswerTopic = "com.nakadoribook.webrtc.answer";
    private static final String OfferTopic = "com.nakadoribook.webrtc.offer";
    private static final String CandidateTopic = "com.nakadoribook.webrtc.candidate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // checkPermission → onRequestPermissionsResult → setup → start
        checkPermission();
    }

    private void checkPermission(){
        String[] permissioins = new String[]{ Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissioins, REQUEST_CODE_CAMERA_PERMISSION);
    }

    private void setup(){
        setupWamp();
        setupWebRTC();
    }

    private void setupWebRTC(){
        webRTC = new WebRTC(this);
    }

    private void setupWamp() {

        WampClientBuilder builder = new WampClientBuilder();

        // Build two clients
        try {

            builder.withUri("ws://192.168.1.2:8000")
                    .withRealm("realm1")
                    .withInfiniteReconnects()
                    .withReconnectInterval(3, TimeUnit.SECONDS);
            wamp = builder.build();
        } catch (WampError e) {
            return;
        }

        AppObservable.bindActivity(this, wamp.statusChanged())
                .subscribe(new Action1<WampClient.Status>() {
                    @Override
                    public void call(final WampClient.Status status) {

                        Log.d("kawa", "Status changed to " + status);
                        if (status == WampClient.Status.Connected) {

                            Log.d("kawa", "connected");
                            wamp.makeSubscription(OfferTopic).subscribe(new Action1<PubSubData>(){
                                @Override
                                public void call(PubSubData arg0) {
                                    Log.d("kawa", "called " + arg0.toString());
                                }

                            }, new Action1<Throwable>(){
                                @Override
                                public void call(Throwable arg0) {
                                    Log.d("kawa", "Throwabled " + arg0.toString());
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable t) {
                        Log.d("kawa", "Session ended with error " + t);
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        Log.d("kawa", "Session ended normally");
                    }
                });

        wamp.open();
    }

    private void start(){
        webRTC.startCapture();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Logging.d("TAG", "onActivityResult 1");

        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION)
            return;

        setup();
        start();
    }

}
