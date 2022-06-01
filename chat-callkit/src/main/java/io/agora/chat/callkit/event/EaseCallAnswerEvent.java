package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;


public class EaseCallAnswerEvent extends EaseCallBaseEvent {
    public EaseCallAnswerEvent(){
        callAction = EaseCallAction.CALL_ANSWER;
    }
    public String result;
    public boolean transVoice;
}
