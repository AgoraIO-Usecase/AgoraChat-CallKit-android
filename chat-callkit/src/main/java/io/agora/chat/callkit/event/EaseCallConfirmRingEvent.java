package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;


public class EaseCallConfirmRingEvent extends EaseCallBaseEvent {
    public EaseCallConfirmRingEvent(){
        callAction = EaseCallAction.CALL_CONFIRM_RING;
    }
    public Boolean valid;
}
