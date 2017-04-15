package com.nakadoribooks.webrtcexample;

import android.app.Activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.TimeUnit;

import rx.android.app.AppObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.WampError;

/**
 * Created by kawase on 2017/04/15.
 */

public class Wamp {

    public static interface WampCallbacks{
        void onConnected();
        void onReceiveOffer(String sdp);
        void onReceiveAnswer(String sdp);
        void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex);
    }

    private static final String TAG = "Wamp";
    private static final String AnswerTopic = "com.nakadoribook.webrtc.answer";
    private static final String OfferTopic = "com.nakadoribook.webrtc.offer";
    private static final String CandidateTopic = "com.nakadoribook.webrtc.candidate";

    private final Activity activity;
    private WampClient wampClient;
    private WampCallbacks callbacks;

    Wamp(Activity activity){
        this.activity = activity;
    }

    // interface -------

    public void connect(WampCallbacks callbacks){
        this.callbacks = callbacks;
        _connect();
    }

    public void publishOffer(String sdp){
        _publishOffer(sdp);
    }

    public void publishAnswer(String sdp){
        _publishAnswer(sdp);
    }

    public void publishIceCandidate(String sdp, String sdpMid, int sdpMLineIndex){
        _publishIceCandidate(sdp, sdpMid, sdpMLineIndex);
    }

    // implements --------

    private void _connect(){
        WampClientBuilder builder = new WampClientBuilder();

        try {
//            builder.withUri("ws://192.168.1.2:8000")
            builder.withUri("wss://nakadoribooks-webrtc.herokuapp.com")
                    .withRealm("realm1")
                    .withInfiniteReconnects()
                    .withReconnectInterval(3, TimeUnit.SECONDS);
            wampClient = builder.build();
        } catch (WampError e) {
            return;
        }

        AppObservable.bindActivity(activity, wampClient.statusChanged())
                .subscribe(new Action1<WampClient.Status>() {
                    @Override
                    public void call(final WampClient.Status status) {

                        if (status == WampClient.Status.Connected) {

                            callbacks.onConnected();

                            wampClient.makeSubscription(OfferTopic).subscribe(new Action1<PubSubData>(){
                                @Override
                                public void call(PubSubData arg0) {

                                    JsonNode json = arg0.arguments().get(0);
                                    String sdp = json.get("sdp").asText();

                                    callbacks.onReceiveOffer(sdp);
                                }

                            }, new Action1<Throwable>(){
                                @Override
                                public void call(Throwable arg0) {}
                            });

                            wampClient.makeSubscription(AnswerTopic).subscribe(new Action1<PubSubData>(){
                                @Override
                                public void call(PubSubData arg0) {
                                    JsonNode json = arg0.arguments().get(0);
                                    String sdp = json.get("sdp").asText();

                                    callbacks.onReceiveAnswer(sdp);
                                }

                            }, new Action1<Throwable>(){
                                @Override
                                public void call(Throwable arg0) {}
                            });

                            wampClient.makeSubscription(CandidateTopic).subscribe(new Action1<PubSubData>(){
                                @Override
                                public void call(PubSubData arg0) {
                                    JsonNode json = arg0.arguments().get(0);
                                    String sdp = json.get("sdp").asText();
                                    String sdpMid = json.get("sdpMid").asText();
                                    int sdpMLineIndex = json.get("sdpMLineIndex").asInt();

                                    callbacks.onIceCandidate(sdp, sdpMid, sdpMLineIndex);
                                }

                            }, new Action1<Throwable>(){
                                @Override
                                public void call(Throwable arg0) {}
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable t) {}
                }, new Action0() {
                    @Override
                    public void call() {}
                });

        wampClient.open();
    }


    public void _publishOffer(String sdp){

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "offer");
        node.put("sdp", sdp);
        wampClient.publish(OfferTopic, node);
    }

    public void _publishAnswer(String sdp){
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "answer");
        node.put("sdp", sdp);

        wampClient.publish(AnswerTopic, node);
    }

    public void _publishIceCandidate(String sdp, String sdpMid, int sdpMLineIndex){
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "candidate");
        node.put("candidate", sdp);
        node.put("id", sdpMid);
        node.put("kRTCICECandidateMLineIndexKey", sdpMLineIndex);

        wampClient.publish(CandidateTopic, node);
    }

}
