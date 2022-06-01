package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;
import io.agora.chat.callkit.general.EaseCallType;


public class EaseCallInviteEventEase extends EaseCallBaseEvent {
    public EaseCallInviteEventEase(){
        callAction = EaseCallAction.CALL_INVITE;
    }
    public EaseCallType type;
}
