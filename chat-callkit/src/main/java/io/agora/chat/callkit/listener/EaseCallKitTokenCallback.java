package io.agora.chat.callkit.listener;


/**
 * Callback for user to get RTC token
 */
public interface EaseCallKitTokenCallback {
    /**
     * Set the obtained RTC token to the CallKit
     * @param token  RTC token
     * @param uid
     */
    void onSetToken(String token,int uid);

    /**
     * Error callback for failed to get RTC token
     * @param error   errorcode when get uid fail
     * @param errorMsg  errorDes when get uid fail
     */
    void onGetTokenError(int error, final String errorMsg);
}
