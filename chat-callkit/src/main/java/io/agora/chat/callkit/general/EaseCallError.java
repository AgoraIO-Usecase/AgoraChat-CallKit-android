package io.agora.chat.callkit.general;


/**
 * Call error type
 */
public enum EaseCallError {
    PROCESS_ERROR, //Service logic exception
    RTC_ERROR, //Audio and video exception
    IM_ERROR  //IM exception
}
