package com.nakadoribooks.webrtcexample;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

/**
 * Created by kawase on 2017/04/13.
 */

public class WebRTC {

    private static final String TAG = "WebRTC";

    private PeerConnectionFactory factory;
    private VideoCapturer videoCapturer;
    private EglBase.Context renderEGLContext;
    private Activity activity;

    WebRTC(Activity activity){
        this.activity = activity;

        // rendereContext
        EglBase eglBase = EglBase.create();
        renderEGLContext = eglBase.getEglBaseContext();

        // initialize Factory
        PeerConnectionFactory.initializeAndroidGlobals(activity.getApplicationContext(), true);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = new PeerConnectionFactory(options);
        factory.setVideoHwAccelerationOptions(renderEGLContext, renderEGLContext);

        // setupLocalStream
        setupLocalStream();
    }

    // interface -----------------

    public void startCapture(){
        _startCapture();
    }

    // implements -------------

    private void setupLocalStream() {

        Log.d(TAG, "setupLocalStream 1");

        SurfaceViewRenderer localRenderer = setupRenderer();

        Log.d(TAG, "setupLocalStream 3");

        MediaStream localStream = factory.createLocalMediaStream("android_local_stream");
        videoCapturer = createCameraCapturer(new Camera2Enumerator(activity));
        VideoSource localVideoSource = factory.createVideoSource(videoCapturer);

        VideoTrack localVideoTrack = factory.createVideoTrack("android_local_videotrack", localVideoSource);
        localStream.addTrack(localVideoTrack);

        VideoRenderer videoRender = new VideoRenderer(localRenderer);
        localVideoTrack.addRenderer(videoRender);
    }


    private void _startCapture(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        int videoWidth = displayMetrics.widthPixels;
        int videoHeight = displayMetrics.heightPixels;
        Logging.d(TAG, "videoWidth:" + videoWidth + ", height:" + videoHeight);

        videoCapturer.startCapture(videoWidth, videoHeight, 30);
    }



    private SurfaceViewRenderer setupRenderer(){
        SurfaceViewRenderer localRenderer = (SurfaceViewRenderer) activity.findViewById(R.id.local_render_view);
        localRenderer.init(renderEGLContext, null);
        localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localRenderer.setZOrderMediaOverlay(true);
        localRenderer.setEnableHardwareScaler(true);

        return localRenderer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        return createBackCameraCapturer(enumerator);
    }

    private VideoCapturer createBackCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                Logging.d(TAG, "DeviceName: " + deviceName);
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    Logging.d(TAG, "foundVideoCapture: " + videoCapturer.toString());
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private VideoCapturer createFrontCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                Logging.d(TAG, "DeviceName: " + deviceName);
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    Logging.d(TAG, "foundVideoCapture: " + videoCapturer.toString());
                    return videoCapturer;
                }
            }
        }

        return null;
    }

}



