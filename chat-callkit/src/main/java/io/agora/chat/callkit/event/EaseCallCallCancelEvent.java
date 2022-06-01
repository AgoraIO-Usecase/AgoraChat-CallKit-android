package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;


public class EaseCallCallCancelEvent extends EaseCallBaseEvent {
    public EaseCallCallCancelEvent(){
        callAction = EaseCallAction.CALL_CANCEL;
    }
    public boolean cancel = true;
    public boolean remoteTimeout = false;
}
