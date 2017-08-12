package com.nakadoribooks.webrtcexample;

import org.webrtc.MediaStream;

/**
 * Created by kawase on 2017/08/12.
 */

interface ConnectionCallbacks{

    void onAddedStream(MediaStream mediaStream);

}

public interface ConnectionInterface {

    String targetId();
    void publishOffer();
    void receiveOffer(String sdp);
    void receiveAnswer(String sdp);
    void receiveCandidate(String candidate, String sdpMid, int sdpMLineIndex);
    void close();

}
