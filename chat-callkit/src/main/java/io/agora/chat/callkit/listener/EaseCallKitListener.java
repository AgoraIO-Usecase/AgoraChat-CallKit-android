package io.agora.chat.callkit.listener;

import org.json.JSONObject;

import io.agora.chat.callkit.bean.EaseUserAccount;
import io.agora.chat.callkit.general.EaseCallEndReason;
import io.agora.chat.callkit.general.EaseCallError;
import io.agora.chat.callkit.general.EaseCallKitConfig;
import io.agora.chat.callkit.general.EaseCallType;


/**
 * the callkit events listener。
 * the user can implementation this to listen the events occured in the video or audio .
 */
public interface EaseCallKitListener {

    /**
     * Invite new users to join a audio or video call during a multi-party chat
     * @param callType     call type
     * @param existMembers Users who already exist in the channel, excluding the inviter self
     * @param ext          the ext information ,type of JSONObject
     */
    void onInviteUsers(EaseCallType callType, String[] existMembers, JSONObject ext);


    /**
     * Ending a Call
     * @param callType    call type
     * @param channelName channel name
     * @param reason      Reason for Ending a Call
     * @param callTime    total call time
     */
    void onEndCallWithReason(EaseCallType callType, String channelName, EaseCallEndReason reason, long callTime);


    /**
     * receive a invite call
     * @param callType  call type
     * @param fromUserId inviter's userId(AgoraChat userId)
     * @param ext  the ext information of invite ,type of JSONObject
     */
    void onReceivedCall(EaseCallType callType, String fromUserId, JSONObject ext);

    /**
     * Get the RTC token
     * Users can configure whether to enable RTC token verification in {@link EaseCallKitConfig#setEnableRTCToken(boolean)} (Boolean)}. If RTC token verification is not enabled, the method will not be called back. On the other hand.
     * In this method, the user should first obtain the RTC token and agora uid from the app server, and then callback the RTC token and agora uid to the callback object passed in the parameter, so that the callkit can get the RTC token and agora uid to join in the calling room.
     * if get RTC token success:{@link EaseCallKitTokenCallback#onSetToken(java.lang.String, int)} of the callback object is called
     * failure: the {@link EaseCallKitTokenCallback#onGetTokenError(int, java.lang.String)} method of the callback object is called, which returns the error code and error message.
     * *
     * @param userId     userId(AgoraChat userId)
     * @param channelName channel name
     * @param callback   a callback object
     */
    default void onGenerateRTCToken(String userId, String channelName, EaseCallKitTokenCallback callback) {
    }

    /**
     * Call error callback
     * @param type  call error type ,see {@link EaseCallError}
     * @param errorCode errorCode ,see {@link io.agora.chat.callkit.general.EaseCallProcessError}
     * @param description  the error des
     */
    void onCallError(EaseCallError type, int errorCode, String description);


    /**
     * Callback when a user sends a call invitation message
     */
    void onInViteCallMessageSent();


    /**
     * Callback when the remote user joins the channel
     * In this case, the user should query the AgoraChat userId corresponding to the uid in the app server through the passed parameter, encapsulate it into a {@link EaseUserAccount} object, and then callback to the callback object passed through the parameter.
     * Success is to call its {@link EaseCallGetUserAccountCallback# onUserAccount (IO. Agora. Chat. Callkit. Beans. EaseUserAccount)} to {@link  EaseUserAccount} object is passed.
     * Failure is to call its {@link EaseCallGetUserAccountCallback# onSetUserAccountError (int, Java. Lang. String)} transfer error code and error description .
     * @param channelName  channel name
     * @param userName userId(AgoraChat userId)
     * @param uid  agora uid
     * @param callback   a callback object
     */
    void onRemoteUserJoinChannel(String channelName, String userName, int uid, EaseCallGetUserAccountCallback callback);

    /**
     * Notifies the user to update the user information，Fired when a UI change occurs within the Callkit or some change event is received in a channel。
     * Users can customize user nicknames, images and so on here.
     * Note that the update process is placed in the synchronous method to achieve timely page refresh.Users do not need to achieve this requirement.
     * @param userName userId(AgoraChat userId)
     */
    default void onUserInfoUpdate(String userName){}
}