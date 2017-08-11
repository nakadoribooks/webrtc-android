package com.nakadoribooks.webrtcexample;

import android.app.Activity;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import rx.android.app.AppObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.WampError;
//import ws.wamp.jawampa.connection.IWampConnectorProvider;
//import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;
//import ws.wamp.jawampa.transport.netty.NettyWampConnectionConfig;


/**
 * Created by kawase on 2017/04/15.
 */

public class Wamp {

    public static interface WampCallbacks {
        void onOpen();

        void onReceiveAnswer(String targetId, String sdp);

        void onReceiveOffer(String taretId, String sdp);

        void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex);

        void onReceiveCallme(String targetId);

        void onCloseConnection(String targetId);
    }

    public enum Topic {

        Callme("com.nakadoribook.webrtc.[roomId].callme"),
        Close("com.nakadoribook.webrtc.[roomId].close"),
        Answer("com.nakadoribook.webrtc.[roomId].[id].answer"),
        Offer("com.nakadoribook.webrtc.[roomId].[id].offer"),
        Candidate("com.nakadoribook.webrtc.[roomId].[id].candidate");

        private final String text;

        private Topic(final String text) {
            this.text = text;
        }

        public String getString() {
            return this.text;
        }
    }

    private static final String TAG = "Wamp";
    private static final String AnswerTopic = "com.nakadoribook.webrtc.answer";
    private static final String OfferTopic = "com.nakadoribook.webrtc.offer";
    private static final String CandidateTopic = "com.nakadoribook.webrtc.candidate";

    private static final String HandshakeEndpint = "wss://nakadoribooks-webrtc.herokuapp.com";

    private String roomTopic(String base){
        return base.replace("[roomId]", roomKey);
    }

    String endpointAnswer(String targetId){
        return roomTopic(Topic.Answer.getString().replace("[id]", targetId));
    }

    String endpointOffer(String targetId) {
        return roomTopic(Topic.Offer.getString().replace("[id]", targetId));
    }

    String endpointCandidate(String targetId) {
        return roomTopic(Topic.Candidate.getString().replace("[id]", targetId));
    }

    String endpointCallme(String targetId) {
        return roomTopic(Topic.Callme.getString());
    }

    String endpointClose(String targetId) {
        return roomTopic(Topic.Callme.getString());
    }


    private final Activity activity;
    WampClient client;
    private WampCallbacks callbacks;
    private String roomKey;
    private String userId;

    Wamp(Activity activity){
        this.activity = activity;
    }

    // interface -------

    public void connect(String roomKey, String userId, WampCallbacks callbacks){
        this.roomKey = roomKey;
        this.userId = userId;
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
//            IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
//            NettyWampConnectionConfig connectionConfiguration = new NettyWampConnectionConfig();
            builder
//                    .withConnectorProvider(connectorProvider)
//                    .withConnectionConfiguration(connectionConfiguration)
//                    .withUri(HandshakeEndpint)
                    .withUri("wss://nakadoribooks-webrtc.herokuapp.com")
                    .withRealm("realm1")
                    .withInfiniteReconnects()
                    .withReconnectInterval(3, TimeUnit.SECONDS);
            client = builder.build();
        } catch (Exception e) {
            return;
        }

        AppObservable.bindActivity(activity, client.statusChanged())
                .subscribe(new Action1<WampClient.Status>() {
                    @Override
                    public void call(final WampClient.Status status) {

                        if (status == WampClient.Status.Connected) {
                            onConnectWamp();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable t) {}
                }, new Action0() {
                    @Override
                    public void call() {}
                });

        client.open();

    }

    private void onConnectWamp(){

        // ▼ subscribe -----

        // offer
        String offerTopic = endpointOffer(userId);
        client.makeSubscription(offerTopic).subscribe(new Action1<PubSubData>(){
            @Override
            public void call(PubSubData arg0) {

                /*       ここから ------------ */
                Log.d("Wamp", "-----------------------------");
                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                JsonNode node = args.get(1);
                if(node == null){
                    Log.d("Wamp", "node is null");
                }else {
                    Log.d("Wamp", "node is not null");
                    String sdpString = node.asText();
                    try {
                        JSONObject obj = new JSONObject(sdpString);
                        Log.d("Wamp", "createdJson");
                        Log.d("Wamp", obj.toString());
                        String s = obj.getString("sdp");
                        Log.d("Wamp sdp:", s);
                        callbacks.onReceiveOffer(targetId, s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        // answer
        String answerTopic = endpointAnswer(userId);
        client.makeSubscription(answerTopic).subscribe(new Action1<PubSubData>(){
            @Override
            public void call(PubSubData arg0) {
                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                JsonNode json = args.get(1);
                String sdp = json.get("sdp").asText();

                callbacks.onReceiveAnswer(targetId, sdp);
            }

        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        // candidate
        String candidateTopic = endpointCandidate(userId);
        client.makeSubscription(candidateTopic).subscribe(new Action1<PubSubData>(){
            @Override
            public void call(PubSubData arg0) {
                String jsonString = arg0.arguments().get(0).asText();
//                Log.d("jsonString", jsonString);
                try{
                    JSONObject json = new JSONObject(jsonString);
//                    Log.d("json", json.toString());
                    String sdp = null;
                    if(json.has("sdp")){
                        sdp = json.getString("sdp");
                    }
                    String sdpMid = json.getString("sdpMid");
                    int sdpMLineIndex = json.getInt("sdpMLineIndex");

//                    Log.d("sdp", sdp);
//                    Log.d("sdpMLineIndex", "" + sdpMLineIndex);
//                    Log.d("sdpMid", sdpMid);

                    callbacks.onIceCandidate(sdp, sdpMid, sdpMLineIndex);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        // callme

        String callmeTopic = endpointCallme(userId);
        Log.d("callmeTopic", callmeTopic);
        client.makeSubscription(callmeTopic).subscribe(new Action1<PubSubData>() {
            @Override
            public void call(PubSubData arg0) {
                JsonNode json = arg0.arguments().get(0);
                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                if(targetId == userId){
                    return;
                }

                callbacks.onReceiveCallme(targetId);
            }
        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        // close
        String closeTopic = endpointClose(userId);
        client.makeSubscription(closeTopic).subscribe(new Action1<PubSubData>() {
            @Override
            public void call(PubSubData arg0) {
                JsonNode json = arg0.arguments().get(0);
                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                callbacks.onCloseConnection(targetId);
            }
        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        callbacks.onOpen();
    }

    public void _publishOffer(String sdp){

        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "offer");
        node.put("sdp", sdp);
        client.publish(OfferTopic, node);
    }

    public void _publishAnswer(String sdp){
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "answer");
        node.put("sdp", sdp);

        client.publish(AnswerTopic, node);
    }

    public void _publishIceCandidate(String sdp, String sdpMid, int sdpMLineIndex){
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "candidate");
        node.put("candidate", sdp);
        node.put("id", sdpMid);
        node.put("kRTCICECandidateMLineIndexKey", sdpMLineIndex);

        client.publish(CandidateTopic, node);
    }

}
