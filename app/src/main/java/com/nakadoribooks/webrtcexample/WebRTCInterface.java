package com.nakadoribooks.webrtcexample;

import org.webrtc.MediaStream;

/**
 * Created by kawase on 2017/08/12.
 */

interface WebRTCCallbacks{

    void onCreateOffer(String sdp);
    void onCreateAnswer(String sdp);
    void onAddedStream(MediaStream mediaStream);
    void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex);

}

public interface WebRTCInterface {

    void createOffer();
    void receiveOffer(String sdp);
    void receiveAnswer(String sdp);
    void receiveCandidate(String sdp, String sdpMid, int sdpMLineIndex);
    void close();

}
