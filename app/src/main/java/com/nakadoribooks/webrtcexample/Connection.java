package com.nakadoribooks.webrtcexample;

import org.json.JSONObject;
import org.webrtc.MediaStream;

/**
 * Created by kawase on 2017/08/11.
 */

public class Connection {

    public static interface ConnectionCallbacks{
        void onAddedStream(MediaStream mediaStream);
    }

    private ConnectionCallbacks callbacks;
    private WebRTC webRTC;
    private final String myId;
    final String targetId;
    private Wamp wamp;
    MediaStream mediaStream;

    Connection(final String myId, final String targetId, final Wamp wamp, final ConnectionCallbacks callbacks){
        this.myId = myId;
        this.targetId = targetId;
        this.wamp = wamp;
        this.callbacks = callbacks;

        this.webRTC = new WebRTC(new WebRTC.WebRTCCallbacks(){

            @Override
            public void onCreateOffer(String sdp) {
                String offerTopic = wamp.offerTopic(targetId);

                try{
                    JSONObject json = new JSONObject();
                    json.put("sdp", sdp);
                    json.put("type", "offer");
                    wamp.publishOffer(targetId, json.toString());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCreateAnswer(String sdp) {

                try{
                    JSONObject json = new JSONObject();
                    json.put("sdp", sdp);
                    json.put("type", "answer");
                    wamp.publishAnswer(targetId, json.toString());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void didReceiveRemoteStream(MediaStream mediaStream) {
                callbacks.onAddedStream(mediaStream);
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {

                final JSONObject json = new JSONObject();
                try{
                    json.put("candidate", sdp);
                    json.put("sdpMid", sdpMid);
                    json.put("sdpMLineIndex", sdpMLineIndex);

                    wamp.publishCandidate(targetId, json.toString());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    // ▼ interface

    void publishOffer(){
        webRTC.createOffer();
    }

    void publishAnswer(String remoteSdp){
        webRTC.receiveOffer(remoteSdp);
    }

    void receiveAnswer(String sdp){
        webRTC.receiveAnswer(sdp);
    }

    void receiveCandidate(String sdp, String sdpMid, int sdpMLineIndex){
        webRTC.addIceCandidate(sdp, sdpMid, sdpMLineIndex);
    }

    void close(){
        webRTC.close();
    }

}


