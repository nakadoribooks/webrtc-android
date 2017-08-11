package com.nakadoribooks.webrtcexample;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.webrtc.*;

import java.util.Arrays;
import java.util.List;

/**
 * Created by kawase on 2017/04/13.
 */

public class WebRTC implements PeerConnection.Observer {

    public static interface WebRTCCallbacks{
        void onCreateOffer(String sdp);
        void onCreateAnswer(String sdp);
        void didReceiveRemoteStream();
        void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex);
    }

    

    private static abstract class SkeletalSdpObserver implements SdpObserver{

        private static final String TAG = "SkeletalSdpObserver";

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override
        public void onSetSuccess() {}
        @Override
        public void onCreateFailure(String s) {}
        @Override
        public void onSetFailure(String s) {}
    }

    private static final String TAG = "WebRTC";

    private static Activity activity;
    private final WebRTCCallbacks callbacks;
    private static PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private static MediaStream localStream;
    private static VideoCapturer videoCapturer;
    private static EglBase eglBase;

    private static VideoTrack localVideoTrack;
    private static VideoRenderer localRenderer;

    private VideoRenderer remoteRenderer;

    WebRTC(WebRTCCallbacks callbacks){
        this.callbacks = callbacks;

        // create PeerConnection
        List<PeerConnection.IceServer> iceServers = Arrays.asList(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        peerConnection = factory.createPeerConnection(iceServers, WebRTCUtil.peerConnectionConstraints(), this);
        peerConnection.addStream(localStream);
    }

    // interface -----------------

    static void setup(Activity activity){
        WebRTC.activity = activity;
        eglBase = EglBase.create();

        // initialize Factory
        PeerConnectionFactory.initializeAndroidGlobals(activity.getApplicationContext(), true);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = new PeerConnectionFactory(options);
        factory.setVideoHwAccelerationOptions(eglBase.getEglBaseContext(), eglBase.getEglBaseContext());

        localStream = factory.createLocalMediaStream("android_local_stream");

        // videoTrack
        videoCapturer = createCameraCapturer(new Camera2Enumerator(activity));
        VideoSource localVideoSource = factory.createVideoSource(videoCapturer);
        localVideoTrack = factory.createVideoTrack("android_local_videotrack", localVideoSource);
        localStream.addTrack(localVideoTrack);

        // render
        localRenderer = setupRenderer(R.id.local_render_view, activity);
        localVideoTrack.addRenderer(localRenderer);

        // audioTrack
        AudioSource audioSource = factory.createAudioSource(WebRTCUtil.mediaStreamConstraints());
        AudioTrack audioTrack = factory.createAudioTrack("android_local_audiotrack", audioSource);
        localStream.addTrack(audioTrack);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        int videoWidth = displayMetrics.widthPixels;
        int videoHeight = displayMetrics.heightPixels;

        videoCapturer.startCapture(videoWidth, videoHeight, 30);
    }

    public void createOffer(){
        _createOffer();
    }

    public void receiveOffer(String sdp){
        _receiveOffer(sdp);
    }

    public void receiveAnswer(String sdp){
        _receiveAnswer(sdp);
    }

    void addIceCandidate(String sdp, String sdpMid, int sdpMLineIndex){
        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
        peerConnection.addIceCandidate(iceCandidate);
    }

    // implements -------------

    private void _receiveAnswer(String sdp){
        SessionDescription remoteDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        peerConnection.setRemoteDescription(new SkeletalSdpObserver() {
            @Override
            public void onSetSuccess() {

            }
        }, remoteDescription);
    }

    private void _receiveOffer(String sdp){
        Log.d("WebRTC", "_receiveOffer 1");

        // setRemoteDescription
        SessionDescription remoteDescription = new SessionDescription(SessionDescription.Type.OFFER, sdp);
        peerConnection.setRemoteDescription(new SkeletalSdpObserver() {
            @Override
            public void onSetSuccess() {
                Log.d("WebRTC", "_receiveOffer 2");

                // createAnswer
                peerConnection.createAnswer(new SkeletalSdpObserver() {
                    @Override
                    public void onCreateSuccess(final SessionDescription sessionDescription) {
                        Log.d("WebRTC", "_receiveOffer 3");
                        peerConnection.setLocalDescription(new SkeletalSdpObserver() {

                            @Override
                            public void onSetSuccess() {
                                Log.d("WebRTC", "_receiveOffer 4");
                                callbacks.onCreateAnswer(sessionDescription.description);
                            }

                        }, sessionDescription);
                    }
                }, WebRTCUtil.answerConnectionConstraints());

            }

            @Override
            public void onSetFailure(String s) {
                Log.d("WebRTC", " ------------ onSetFailure ----------------");
                Log.d("WebRTC", s);
            }
        }, remoteDescription);
    }

    private void _createOffer(){
        Log.d("WebRtc", "_createOffer 1");
        peerConnection.createOffer(new SkeletalSdpObserver() {
            @Override
            public void onCreateSuccess(final SessionDescription sessionDescription) {
                Log.d("WebRtc", "_createOffer 2");
                peerConnection.setLocalDescription(new SkeletalSdpObserver() {

                    @Override
                    public void onSetSuccess() {
                        Log.d("WebRtc", "_createOffer 3");
                        callbacks.onCreateOffer(sessionDescription.description);
                    }

                }, sessionDescription);
            }
        }, WebRTCUtil.offerConnectionConstraints());
    }

    private static VideoRenderer setupRenderer(int viewId, Activity activity){

        SurfaceViewRenderer renderer = (SurfaceViewRenderer) activity.findViewById(viewId);
        renderer.init(eglBase.getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        renderer.setZOrderMediaOverlay(true);
        renderer.setEnableHardwareScaler(true);

        return new VideoRenderer(renderer);
    }

    private static VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        return createBackCameraCapturer(enumerator);
    }

    private static VideoCapturer createBackCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    // PeerConnection.Observer -----

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {}
    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {}
    @Override
    public void onIceConnectionReceivingChange(boolean b) {}
    @Override
    public void onRemoveStream(MediaStream mediaStream) {}
    @Override
    public void onDataChannel(DataChannel dataChannel) {}
    @Override
    public void onRenegotiationNeeded() {}
    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}
    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        callbacks.onIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex);
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

        if (mediaStream.videoTracks.size() == 0){
            return;
        }

        final VideoTrack remoteVideoTrack = mediaStream.videoTracks.getFirst();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                remoteRenderer = setupRenderer(R.id.remote_render_view, activity);
                remoteVideoTrack.addRenderer(remoteRenderer);

                callbacks.didReceiveRemoteStream();
            }
        });

    }
}



