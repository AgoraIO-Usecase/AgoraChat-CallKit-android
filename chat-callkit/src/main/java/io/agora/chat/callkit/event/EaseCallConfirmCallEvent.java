package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;


public class EaseCallConfirmCallEvent extends EaseCallBaseEvent {
    public EaseCallConfirmCallEvent(){
        callAction = EaseCallAction.CALL_CONFIRM_CALLEE;
    }
    public String result;
}
