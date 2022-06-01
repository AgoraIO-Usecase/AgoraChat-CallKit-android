package io.agora.chat.callkit.listener;


/**
 *  \~chinese
 * 用户返回Token的回调
 *
 * \~english
 * Callback for user to get RTC token
 */
public interface EaseCallKitTokenCallback {
    /**
     *  \~chinese
     * 设置获取到的RTC token到callkit
     * @param token  token的值
     * @param uid  声网uid
     *
     * \~english
     * Set the obtained RTC token to the CallKit
     * @param token  RTC token
     * @param uid
     */
    void onSetToken(String token,int uid);

    /**
     * \~chinese
     * 获取RTC Token失败的错误回调
     * @param error   获取token失败的错误码
     * @param errorMsg  获取token失败的错误信息描述
     *
     * \~english
     * Error callback for failed to get RTC token
     * @param error   errorcode when get uid fail
     * @param errorMsg  errorDes when get uid fail
     */
    void onGetTokenError(int error, final String errorMsg);
}
