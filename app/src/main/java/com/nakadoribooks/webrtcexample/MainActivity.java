package com.nakadoribooks.webrtcexample;

import android.Manifest;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private enum State{
        Disconnected
        , Connecting
        , Connected
        , Offering
        , ReceivedOffer
        , CreatingAnswer
        , Done
    }

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1;
    private WebRTC webRTC;
    private Wamp wamp;
    private State state = State.Disconnected;

    private Button controlButton;
    private TextView statusText;
    private boolean typeOffer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // getView
        controlButton = (Button) findViewById(R.id.control_button);
        statusText = (TextView) findViewById(R.id.status_label);

        // registerEvent
        findViewById(R.id.control_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTapButton();
            }
        });

        // wamp
        wamp = new Wamp(this);

        // checkPermission → onRequestPermissionsResult → startWebRTC
        checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION)
            return;

        startWebRTC();
    }

    private void checkPermission(){
        String[] permissioins = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, permissioins, REQUEST_CODE_CAMERA_PERMISSION);
    }

    private void onTapButton(){
        if(state == State.Disconnected){
            changeState(State.Connecting);
            connect();
        }else if(state == State.Connected){
            typeOffer = true;
            changeState(State.Offering);
            stateOffering();
            webRTC.createOffer();
        }
    }

    private void connect(){
        wamp.connect(new Wamp.WampCallbacks() {

            @Override
            public void onConnected() {
                changeState(State.Connected);
            }

            @Override
            public void onReceiveOffer(String sdp) {
                if(typeOffer){
                    return;
                }
                changeState(State.CreatingAnswer);
                webRTC.receiveOffer(sdp);
            }

            @Override
            public void onReceiveAnswer(String sdp) {
                if(!typeOffer){
                    return;
                }

                webRTC.receiveAnswer(sdp);
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {
                webRTC.addIceCandidate(sdp, sdpMid, sdpMLineIndex);
            }
        });
    }

    private void startWebRTC(){

        webRTC = new WebRTC(this);
        webRTC.connect(new WebRTC.WebRTCCallbacks() {
            @Override
            public void onCreateLocalSdp(String sdp) {
                if(typeOffer){
                    wamp.publishOffer(sdp);
                }else{
                    wamp.publishAnswer(sdp);
                }
            }

            @Override
            public void didReceiveRemoteStream() {
                changeState(State.Done);
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {
                wamp.publishIceCandidate(sdp, sdpMid, sdpMLineIndex);
            }
        });
        webRTC.startCapture();
    }

    // view ---------------

    private void stateConnecting(){
        statusText.setText("Connecting");
        controlButton.setText("Connecting...");
        controlButton.setEnabled(false);
    }

    private void stateConnected(){
        statusText.setText("connected");
        statusText.setTextColor(Color.BLUE);
        controlButton.setText("Send Offer");
        controlButton.setEnabled(true);
    }

    private void stateOffering(){
        statusText.setText("Offering...");
        controlButton.setText("Offering...");
        controlButton.setEnabled(false);
    }

    private void stateReceivedOffer(){
        statusText.setText("ReceivedOffer");
        controlButton.setText("ReceivedOffer");
        controlButton.setEnabled(false);
    }

    private void stateCreatingAnswer(){
        statusText.setText("CreatingAnswer...");
        controlButton.setText("CreatingAnswer...");
        controlButton.setEnabled(false);
    }

    private void stateDone(){
        statusText.setText("OK!");
        controlButton.setText("OK!");
        controlButton.setEnabled(false);
    }

    private void changeState(final State state){
        this.state = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state){
                    case Connected:
                        stateConnected();
                        break;
                    case Connecting:
                        stateConnecting();
                        break;
                    case CreatingAnswer:
                        stateCreatingAnswer();
                        break;
                    case ReceivedOffer:
                        stateReceivedOffer();
                        break;
                    case Done:
                        stateDone();
                    default:
                        break;
                }
            }
        });
    }

}
