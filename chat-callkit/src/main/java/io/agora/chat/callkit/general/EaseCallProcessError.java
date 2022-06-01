package io.agora.chat.callkit.general;


/**
 * error code detail
 */
public enum EaseCallProcessError {
    CALL_STATE_ERROR(0),//call state error
    CALL_TYPE_ERROR(1),//call type error
    CALL_PARAM_ERROR(2),//call param error
    CALL_RECEIVE_ERROR(3);//call receive error

    public int code;

    EaseCallProcessError(int code) {
        this.code = code;
    }
}
