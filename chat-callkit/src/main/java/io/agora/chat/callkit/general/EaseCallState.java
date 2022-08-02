package io.agora.chat.callkit.general;

public enum EaseCallState {
    CALL_IDLE(0), //initial state
    CALL_OUTGOING(1), //Outgoing state
    CALL_ALERTING(2), //Ringing state
    CALL_ANSWERED(3); //Answered call state

    public int code;

    EaseCallState(int code) {
        this.code = code;
    }

    public static EaseCallState getfrom(int code) {
        switch (code) {
            case 0:
                return CALL_IDLE;
            case 1:
                return CALL_OUTGOING;
            case 2:
                return CALL_ALERTING;
            case 3:
                return CALL_ANSWERED;
            default:
                return CALL_IDLE;
        }
    }
}
