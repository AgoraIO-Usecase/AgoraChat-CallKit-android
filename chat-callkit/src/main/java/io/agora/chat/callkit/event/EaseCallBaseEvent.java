package io.agora.chat.callkit.event;

import io.agora.chat.callkit.general.EaseCallAction;

public class EaseCallBaseEvent {
    public EaseCallBaseEvent(){}

    public EaseCallAction callAction;
    public String callerDevId;
    public String calleeDevId;
    public long timeStramp;
    public String callId;
    public String msgType;
    public String userId;
}
