package io.agora.chat.callkit.listener;


import io.agora.chat.callkit.bean.EaseUserAccount;

/**
 * \~chinese
 * 用户返回EaseUserAccount(加入频道的uId及对应的AgoraChat userId)
 *
 * \~english
 * The user returns EaseUserAccount(join channel uId and corresponding AgoraChat userId)
 */
public interface EaseCallGetUserAccountCallback {
    /**
     *  \~chinese
     * 获取到channel里面的用户信息
     * @param userAccount 用户信息
     *
     *  \~english
     * Gets the userAccount in channel
     * @param userAccount  user's account info
     */
    void onUserAccount(EaseUserAccount userAccount);

    /**
     * \~chinese
     * 获取uid失败的错误回调
     *
     * @param error   获取uid失败的错误码
     * @param errorMsg  获取uid失败的错误信息描述
     *
     * \~english
     * Error callback for failed to get uid
     * @param error   errorcode when get uid fail
     * @param errorMsg  errorDes when get uid fail
     */
    void onSetUserAccountError(int error, final String errorMsg);
}
