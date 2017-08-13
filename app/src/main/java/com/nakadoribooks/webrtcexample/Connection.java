package com.nakadoribooks.webrtcexample;

import org.json.JSONObject;
import org.webrtc.MediaStream;

/**
 * Created by kawase on 2017/08/11.
 */

public class Connection implements ConnectionInterface {

    private ConnectionCallbacks callbacks;
    private WebRTCInterface webRTC;
    private final String myId;
    private final String _targetId;
    private WampInterface wamp;

    Connection(final String myId, final String targetId, final WampInterface wamp, final ConnectionCallbacks callbacks){
        this.myId = myId;
        this._targetId = targetId;
        this.wamp = wamp;
        this.callbacks = callbacks;

        this.webRTC = new WebRTC(new WebRTCCallbacks(){

            @Override
            public void onCreateOffer(String sdp) {

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
            public void onAddedStream(MediaStream mediaStream) {
                callbacks.onAddedStream(mediaStream);
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {

                final JSONObject json = new JSONObject();
                try{
                    json.put("type", "candidate");
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

    public String targetId(){
        return _targetId;
    }

    public void publishOffer(){
        webRTC.createOffer();
    }

    public void receiveOffer(String sdp){
        webRTC.receiveOffer(sdp);
    }

    public void receiveAnswer(String sdp){
        webRTC.receiveAnswer(sdp);
    }

    public void receiveCandidate(String candidate, String sdpMid, int sdpMLineIndex){
        webRTC.receiveCandidate(candidate, sdpMid, sdpMLineIndex);
    }

    public void close(){
        webRTC.close();
    }

}


