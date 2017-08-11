package com.nakadoribooks.webrtcexample;

import android.util.Log;

import org.json.JSONObject;
import org.webrtc.SessionDescription;

import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;

/**
 * Created by kawase on 2017/08/11.
 */

interface ConnectionCallbacks{
    void onAddedStream();
}


public class Connection {

    private ConnectionCallbacks callbacks;
    private WebRTC webRTC;
    private final String myId;
    final String targetId;
    private Wamp wamp;

    Connection(final String myId, final String targetId, final Wamp wamp, ConnectionCallbacks callbacks){
        this.myId = myId;
        this.targetId = targetId;
        this.wamp = wamp;
        this.callbacks = callbacks;

        subscribeCandidate();

        this.webRTC = new WebRTC(new WebRTC.WebRTCCallbacks(){

            @Override
            public void onCreateOffer(String sdp) {
                String offerTopic = wamp.endpointOffer(targetId);

                Log.d("Connection", "onCreateOffer");
                wamp.client.publish(offerTopic, myId, sdp);
            }

            @Override
            public void onCreateAnswer(String sdp) {
                String answerTopic = wamp.endpointAnswer(targetId);

                Log.d("Connection", "onCreateAnswer");

                try{
                    JSONObject json = new JSONObject();
                    json.put("sdp", sdp);
                    json.put("type", "answer");
                    wamp.client.publish(answerTopic, myId, json.toString());
                    Log.d("answerTopic", answerTopic);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void didReceiveRemoteStream() {
                Log.d("Connection", "didReceiveRemoteStream");
            }

            @Override
            public void onIceCandidate(String sdp, String sdpMid, int sdpMLineIndex) {
                Log.d("Connection", "onIceCandidate");

                JSONObject json = new JSONObject();
                try{
                    json.put("candidate", sdp);
                    json.put("sdpMid", sdpMid);
                    json.put("sdpMLineIndex", sdpMLineIndex);

                    String candidateTopic = wamp.endpointCandidate(targetId);
                    wamp.client.publish(candidateTopic, json.toString());

                    Log.d("published candidate", json.toString());
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
                Log.d("jsonString", jsonString);

                try{
                    JSONObject json = new JSONObject(jsonString);
                    String sdp = json.getString("candidate");
                    String sdpMid = json.getString("sdpMid");
                    int sdpMLineIndex = json.getInt("sdpMLineIndex");
                    webRTC.addIceCandidate(sdp, sdpMid, sdpMLineIndex);
                }catch(Exception e){
                    e.printStackTrace();
                }

//                callbacks.onAddedStream();
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });
    }

    void publishOffer(){
        webRTC.createOffer();

//        func publishOffer(){
//            webRtc.createOffer { (offerSdp) in
//                let wamp = Wamp.sharedInstance
//                let topic = wamp.endpointOffer(targetId: self.targetId)
//
//                let jsonData = try! JSONSerialization.data(withJSONObject: offerSdp, options: [])
//                let jsonStr = String(bytes: jsonData, encoding: .utf8)!
//
//                        wamp.session.publish(topic, options: [:], args: [self.myId, jsonStr], kwargs: [:])
//            }
//        }
    }

    void publishAnswer(String remoteSdp){
        webRTC.receiveOffer(remoteSdp);
    }

//    private func subscribeCandidate(){
//        let wamp = Wamp.sharedInstance
//        let candidateTopic = wamp.endpointCandidate(targetId: myId)
//
//        Wamp.sharedInstance.session.subscribe(candidateTopic, onSuccess: { (subscription) in
//        }, onError: { (results, error) in
//        }) { (results, args, kwArgs) in
//
//            guard let candidateStr = args?.first as? String else{
//                print(args?.first)
//                print("no candidate")
//                return
//            }
//
//            let data = candidateStr.data(using: String.Encoding.utf8)!
//                    let candidate = try! JSONSerialization.jsonObject(with: data, options: JSONSerialization.ReadingOptions.allowFragments) as! NSDictionary
//
//            self.webRtc.receiveCandidate(candidate: candidate)
//        }
//    }





}


