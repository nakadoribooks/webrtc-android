package com.nakadoribooks.webrtcexample;

import android.app.Activity;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import rx.android.app.AppObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;


/**
 * Created by kawase on 2017/04/15.
 */

public class Wamp implements WampInterface {

    private static final String HandshakeEndpint = "wss://nakadoribooks-webrtc.herokuapp.com";

    private String roomTopic(String base){
        return base.replace("[roomId]", roomId);
    }

    String answerTopic(String userId){
        return roomTopic(Topic.Answer.getString().replace("[userId]", userId));
    }

    String offerTopic(String userId) {
        return roomTopic(Topic.Offer.getString().replace("[userId]", userId));
    }

    String candidateTopic(String userId) {
        return roomTopic(Topic.Candidate.getString().replace("[userId]", userId));
    }

    String callmeTopic() {
        return roomTopic(Topic.Callme.getString());
    }

    String closeTopic(String userId) {
        return roomTopic(Topic.Callme.getString());
    }

    private final Activity activity;
    private WampClient client;
    private WampCallbacks callbacks;
    private String roomId;
    private String userId;

    Wamp(Activity activity, String roomId, String userId, WampCallbacks callbacks){
        this.activity = activity;
        this.roomId = roomId;
        this.userId = userId;
        this.callbacks = callbacks;
    }

    // interface -------

    public void connect(){
        WampClientBuilder builder = new WampClientBuilder();

        try {
            builder
                    .withUri(HandshakeEndpint)
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

    public void publishCallme(){
        String callmeTopic = callmeTopic();
        final ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode();
        args.put("targetId", userId);
        client.publish(callmeTopic, userId);
    }

    public void publishOffer(String targetId, String sdp){
        String topic = offerTopic(targetId);
        client.publish(topic, this.userId, sdp);
    }

    public void publishAnswer(String targetId, String sdp){
        String topic = answerTopic(targetId);
        client.publish(topic, this.userId, sdp);
    }

    public void publishCandidate(String targetId, String candidate){
        String topic = candidateTopic(targetId);
        client.publish(topic, this.userId, candidate);
    }

    // implements --------

    private void onConnectWamp(){

        // ▼ subscribe -----

        // offer
        String offerTopic = offerTopic(userId);
        client.makeSubscription(offerTopic).subscribe(new Action1<PubSubData>(){
            @Override
            public void call(PubSubData arg0) {

                /*       ここから ------------ */
                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                JsonNode node = args.get(1);

                String sdpString = node.asText();
                try {
                    JSONObject obj = new JSONObject(sdpString);
                    String s = obj.getString("sdp");
                    callbacks.onReceiveOffer(targetId, s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        // answer
        String answerTopic = answerTopic(userId);
        client.makeSubscription(answerTopic).subscribe(new Action1<PubSubData>(){
            @Override
            public void call(PubSubData arg0) {

                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                JsonNode node = args.get(1);
                String sdpString = node.asText();

                try {
                    JSONObject obj = new JSONObject(sdpString);
                    String s = obj.getString("sdp");
                    callbacks.onReceiveAnswer(targetId, s);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("receiveAnswer 5", "");
                }
            }

        }, new Action1<Throwable>(){
            @Override
            public void call(Throwable arg0) {
                arg0.printStackTrace();
            }
        });

        // candidate
        String candidateTopic = candidateTopic(userId);
        client.makeSubscription(candidateTopic).subscribe(new Action1<PubSubData>(){
            @Override
            public void call(PubSubData arg0) {
                String targetId = arg0.arguments().get(0).asText();
                String jsonString = arg0.arguments().get(1).asText();
                try{
                    JSONObject json = new JSONObject(jsonString);
                    String sdp = null;
                    if(!json.has("candidate")){
                        return;
                    }
                    sdp = json.getString("candidate");
                    String sdpMid = json.getString("sdpMid");
                    int sdpMLineIndex = json.getInt("sdpMLineIndex");

                    callbacks.onIceCandidate(targetId, sdp, sdpMid, sdpMLineIndex);
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
        String callmeTopic = callmeTopic();
        client.makeSubscription(callmeTopic).subscribe(new Action1<PubSubData>() {
            @Override
            public void call(PubSubData arg0) {
                JsonNode json = arg0.arguments().get(0);
                ArrayNode args = arg0.arguments();
                String targetId = args.get(0).asText();

                if(targetId.equals(userId)){
                    Log.d("onCallme", "cancel");
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
        String closeTopic = closeTopic(userId);
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


}
