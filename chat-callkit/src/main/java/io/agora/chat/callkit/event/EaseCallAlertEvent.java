package io.agora.chat.callkit.event;
import io.agora.chat.callkit.general.EaseCallAction;



public class EaseCallAlertEvent extends EaseCallBaseEvent {
   public EaseCallAlertEvent(){
        callAction = EaseCallAction.CALL_ALERT;
    }
}
