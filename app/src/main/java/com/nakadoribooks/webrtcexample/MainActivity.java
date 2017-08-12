package com.nakadoribooks.webrtcexample;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridLayout;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA_PERMISSION = 1;
    private WampInterface wamp;

    private ArrayList<Connection> connectionList = new ArrayList<Connection>();

    private String userId = UUID.randomUUID().toString().substring(0, 8);
    private EglBase eglBase = EglBase.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupWamp();

        // checkPermission → onRequestPermissionsResult → startWebRTC
        checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION) {
            return;
        }

        WebRTC.setup(this, eglBase);

        // show localView
        MediaStream localStream = WebRTC.localStream;
        SurfaceViewRenderer renderer = (SurfaceViewRenderer) findViewById(R.id.local_render_view);
        VideoRenderer localRenderer = setupRenderer(renderer);
        localStream.videoTracks.getFirst().addRenderer(localRenderer);

        wamp.connect();
    }

    private void checkPermission(){
        String[] permissioins = new String[]{ Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, permissioins, REQUEST_CODE_CAMERA_PERMISSION);
    }

    private void setupWamp(){
        // wamp
        String roomId = "abcdef"; // とりあえず固定
        wamp = new Wamp(this, roomId, userId, new WampCallbacks() {
            @Override
            public void onOpen() {
                wamp.publishCallme();
            }

            @Override
            public void onReceiveAnswer(String targetId, String sdp) {

                Connection connection = findConnection(targetId);
                if(connection == null){
                    Log.d("onReceiveAnswer", "not found connection");
                    return;
                }

                connection.receiveAnswer(sdp);
            }

            @Override
            public void onReceiveOffer(String targetId, String sdp) {
                Connection connection = createConnection(targetId);
                connection.receiveOffer(sdp);
            }

            @Override
            public void onIceCandidate(String targetId, String candidate, String sdpMid, int sdpMLineIndex) {
                Connection connection = createConnection(targetId);
                connection.receiveCandidate(candidate, sdpMid, sdpMLineIndex);
            }

            @Override
            public void onReceiveCallme(String targetId) {
                Connection connection = createConnection(targetId);
                connection.publishOffer();
            }

            @Override
            public void onCloseConnection(String targetId) {

            }
        });
    }

    private int remoteIndex = 0;

    private Connection createConnection(String targetId){
        Connection connection = new Connection(userId, targetId, wamp, new ConnectionCallbacks() {
            @Override
            public void onAddedStream(MediaStream mediaStream) {
                if (mediaStream.videoTracks.size() == 0){
                    Log.e("createConnection", "noVideoTracks");
                    return;
                }

                final VideoTrack remoteVideoTrack = mediaStream.videoTracks.getFirst();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        SurfaceViewRenderer remoteRenderer = new SurfaceViewRenderer(MainActivity.this);

                        int row = remoteIndex / 2;
                        int col = remoteIndex % 2;

                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.columnSpec = GridLayout.spec(col, 1);
                        params.rowSpec = GridLayout.spec(row, 1);
                        params.width = 500;
                        params.height = 500;
                        params.leftMargin = 10;
                        params.rightMargin = 10;
                        params.topMargin = 10;

                        remoteRenderer.setLayoutParams(params);

                        VideoRenderer videoRenderer = setupRenderer(remoteRenderer);
                        remoteVideoTrack.addRenderer(videoRenderer);

                        GridLayout remoteViewContainer = (GridLayout) MainActivity.this.findViewById(R.id.remote_view_container);
                        remoteViewContainer.addView(remoteRenderer);

                        remoteIndex = remoteIndex + 1;
                    }
                });
            }
        });

        connectionList.add(connection);
        return connection;
    }

    private Connection findConnection(String targetId){

        for(int i=0,max=connectionList.size();i<max;i++){
            Connection connection = connectionList.get(i);
            if (connection.targetId().equals(targetId)){
                return connection;
            }
        }

        Log.d("not found", connectionList.toString());
        return null;
    }

    private VideoRenderer setupRenderer(SurfaceViewRenderer renderer){

        renderer.init(eglBase.getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        renderer.setZOrderMediaOverlay(true);
        renderer.setEnableHardwareScaler(true);

        return new VideoRenderer(renderer);
    }

}
