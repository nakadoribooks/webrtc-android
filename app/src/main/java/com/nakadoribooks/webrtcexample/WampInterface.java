package com.nakadoribooks.webrtcexample;

/**
 * Created by kawase on 2017/08/12.
 */

enum Topic {

    Callme("com.nakadoribook.webrtc.[roomId].callme"),
    Close("com.nakadoribook.webrtc.[roomId].close"),
    Answer("com.nakadoribook.webrtc.[roomId].[userId].answer"),
    Offer("com.nakadoribook.webrtc.[roomId].[userId].offer"),
    Candidate("com.nakadoribook.webrtc.[roomId].[userId].candidate");

    private final String text;

    private Topic(final String text) {
        this.text = text;
    }

    public String getString() {
        return this.text;
    }
}

interface WampCallbacks {

    void onOpen();

    void onReceiveAnswer(String targetId, String sdp);

    void onReceiveOffer(String taretId, String sdp);

    void onIceCandidate(String targetId, String sdp, String sdpMid, int sdpMLineIndex);

    void onReceiveCallme(String targetId);

    void onCloseConnection(String targetId);

}

public interface WampInterface {

    void connect();
    void publishCallme();
    void publishOffer(String targetId, String sdp);
    void publishAnswer(String targetId, String sdp);
    void publishCandidate(String targetId, String candidate);

}
