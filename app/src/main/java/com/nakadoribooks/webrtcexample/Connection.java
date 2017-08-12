package com.nakadoribooks.webrtcexample;

import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;

/**
 * Created by kawase on 2017/08/11.
 */




public class Connection {

    public static interface ConnectionCallbacks{
        void onAddedStream(MediaStream mediaStream);
    }

    public static class RemoteStream{

        final MediaStream mediaStream;
        final String targetId;

        RemoteStream(MediaStream mediaStream, String targetId){
            this.mediaStream = mediaStream;
            this.targetId = targetId;
        }

    }

    private ConnectionCallbacks callbacks;
    private WebRTC webRTC;
    private final String myId;
    final String targetId;
    private Wamp wamp;
    private RemoteStream remoteStream;

    Connection(final String myId, final String targetId, final Wamp wamp, final ConnectionCallbacks callbacks){
        this.myId = myId;
        this.targetId = targetId;
        this.wamp = wamp;
        this.callbacks = callbacks;

        subscribeCandidate();

        this.webRTC = new WebRTC(new WebRTC.WebRTCCallbacks(){

            @Override
            public void onCreateOffer(String sdp) {
                String offerTopic = wamp.endpointOffer(targetId);

                try{
                    JSONObject json = new JSONObject();
                    json.put("sdp", sdp);
                    json.put("type", "offer");
                    wamp.client.publish(offerTopic, myId, json.toString());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCreateAnswer(String sdp) {

                String answerTopic = wamp.endpointAnswer(targetId);

                try{
                    JSONObject json = new JSONObject();
                    json.put("sdp", sdp);
                    json.put("type", "answer");

                    wamp.client.publish(answerTopic, myId, json.toString());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void didReceiveRemoteStream(MediaStream mediaStream) {
                RemoteStream remoteStream = new RemoteStream(mediaStream, targetId);
                Connection.this.remoteStream = remoteStream;

                callbacks.onAddedStream(mediaStream);
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {

                final JSONObject json = new JSONObject();
                try{
                    json.put("candidate", sdp);
                    json.put("sdpMid", sdpMid);
                    json.put("sdpMLineIndex", sdpMLineIndex);

                    final String candidateTopic = wamp.endpointCandidate(targetId);
                    wamp.client.publish(candidateTopic, json.toString());

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void subscribeCandidate(){
        String candidateTopic = wamp.endpointCandidate(myId);
        wamp.client.makeSubscription(candidateTopic).subscribe(new Action1<PubSubData>() {
            @Override
            public void call(PubSubData pubSubData) {
                String jsonString = pubSubData.arguments().get(0).asText();

                try{
                    JSONObject json = new JSONObject(jsonString);
                    String sdp = json.getString("candidate");
                    String sdpMid = json.getString("sdpMid");
                    int sdpMLineIndex = json.getInt("sdpMLineIndex");
                    webRTC.addIceCandidate(sdp, sdpMid, sdpMLineIndex);
                }catch(Exception e){
                    Log.e("---fail----", "candidate");
                    e.printStackTrace();
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });
    }

    void publishOffer(){
        webRTC.createOffer();
    }

    void publishAnswer(String remoteSdp){
        webRTC.receiveOffer(remoteSdp);
    }

    void receiveAnswer(String sdp){
        webRTC.receiveAnswer(sdp);
    }

}


