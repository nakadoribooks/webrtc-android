package com.nakadoribooks.webrtcexample;

import org.webrtc.MediaConstraints;


/**
 * Created by kawase on 2017/04/15.
 */

public class WebRTCUtil {

    static final MediaConstraints peerConnectionConstraints(){
        return audioVideoConstraints();
    }

    static final MediaConstraints offerConnectionConstraints(){
        return audioVideoConstraints();
    }

    static final MediaConstraints answerConnectionConstraints(){
        return audioVideoConstraints();
    }

    static final MediaConstraints mediaStreamConstraints(){
        MediaConstraints constraints = new MediaConstraints();

        return constraints;
    }

    private static final MediaConstraints audioVideoConstraints(){
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        return constraints;
    }

}
