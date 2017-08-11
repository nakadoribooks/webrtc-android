package com.nakadoribooks.webrtcexample;

import android.Manifest;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;

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

    private ArrayList<Connection> connectionList = new ArrayList<Connection>();

    private boolean typeOffer = false;
    private String userId = UUID.randomUUID().toString().substring(0, 8);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // wamp
        wamp = new Wamp(this);

        // checkPermission → onRequestPermissionsResult → startWebRTC
        checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION) {
            connect();
            return;
        }

        setupStream();
        connect();
    }

    private void checkPermission(){
        String[] permissioins = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, permissioins, REQUEST_CODE_CAMERA_PERMISSION);
    }

    private void connect(){

//        String roomKey = "-Kr-JqhdoZ1YtdeO0-9r"; // とりあえず固定
        String roomKey = "abcdef"; // とりあえず固定

        wamp.connect(roomKey, userId, new Wamp.WampCallbacks() {
            @Override
            public void onOpen() {
                Log.d("MainActivity", "onOpen");
                String callmeTopic = wamp.endpointCallme(userId);

                final ObjectMapper mapper = new ObjectMapper();
                ObjectNode args = mapper.createObjectNode();
                args.put("targetId", userId);
//                wamp.client.publish(callmeTopic, args);
                wamp.client.publish(callmeTopic, userId);

                Log.d("publish", callmeTopic);
            }

            @Override
            public void onReceiveAnswer(String targetId, String sdp) {
                Connection connection = findConnection(targetId);
                if(connection == null){
                    Log.d("onReceiveAnswer", "not found connection");
                    return;
                }

                connection.publishAnswer(sdp);
            }

            @Override
            public void onReceiveOffer(String targetId, String sdp) {
                Log.d("MainActivity", "onReceiveOffer:" + targetId);
                Connection connection = createConnection(targetId);
                connection.publishAnswer(sdp);
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {

            }

            @Override
            public void onReceiveCallme(String targetId) {
                Log.d("MainActivity", "onReceiveCallme");
                Connection connection = createConnection(targetId);
                connection.publishOffer();
            }

            @Override
            public void onCloseConnection(String targetId) {

            }
        });
    }

    private Connection createConnection(String targetId){
        Connection connection = new Connection(userId, targetId, wamp, new ConnectionCallbacks() {
            @Override
            public void onAddedStream() {

            }
        });

        connectionList.add(connection);
        return connection;
    }

    private Connection findConnection(String targetId){

        for(int i=0,max=connectionList.size();i<max;i++){
            Connection connection = connectionList.get(i);
            if (connection.targetId == targetId){
                return connection;
            }
        }
        return null;
    }


    private void setupStream(){

        WebRTC.setup(this);
//        webRTC = new WebRTC(this);
//        webRTC.connect(new WebRTC.WebRTCCallbacks() {
//            @Override
//            public void onCreateLocalSdp(String sdp) {
//                if(typeOffer){
//                    wamp.publishOffer(sdp);
//                }else{
//                    wamp.publishAnswer(sdp);
//                }
//            }
//
//            @Override
//            public void didReceiveRemoteStream() {
//            }
//
//            @Override
//            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {
//                wamp.publishIceCandidate(sdp, sdpMid, sdpMLineIndex);
//            }
//        });
//        webRTC.startCapture();
    }

}
