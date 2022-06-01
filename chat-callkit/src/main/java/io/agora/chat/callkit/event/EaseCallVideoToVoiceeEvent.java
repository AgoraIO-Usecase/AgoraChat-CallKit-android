package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;


public class EaseCallVideoToVoiceeEvent extends EaseCallBaseEvent {
    public EaseCallVideoToVoiceeEvent(){
        callAction = EaseCallAction.CALL_VIDEO_TO_VOICE;
    }
}
