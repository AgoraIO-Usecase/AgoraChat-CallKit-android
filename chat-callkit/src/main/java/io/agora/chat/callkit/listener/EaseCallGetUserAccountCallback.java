package io.agora.chat.callkit.listener;


import io.agora.chat.callkit.bean.EaseUserAccount;

/**
 * The user returns EaseUserAccount(join channel uId and corresponding AgoraChat userId)
 */
public interface EaseCallGetUserAccountCallback {
    /**
     * Gets the userAccount in channel
     * @param userAccount  user's account info
     */
    void onUserAccount(EaseUserAccount userAccount);

    /**
     * Error callback for failed to get uid
     * @param error   errorcode when get uid fail
     * @param errorMsg  errorDes when get uid fail
     */
    void onSetUserAccountError(int error, final String errorMsg);
}
