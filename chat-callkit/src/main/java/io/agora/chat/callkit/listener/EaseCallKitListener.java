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
     * \~chinese
     * 多人聊天过程中邀请新用户加入语音或者视频
     * @param callType     通话类型
     * @param existMembers 当前房间已经存在的用户，不包括自己
     * @param ext          扩展信息,JSONObject 类型
     *
     * \~english
     * Invite new users to join a audio or video call during a multi-party chat
     * @param callType     call type
     * @param existMembers Users who already exist in the channel, excluding the inviter self
     * @param ext          the ext information ,type of JSONObject
     */
    void onInviteUsers(EaseCallType callType, String[] existMembers, JSONObject ext);


    /**
     * \~chinese
     * 结束通话
     * @param callType    通话类型
     * @param channelName 房间名称
     * @param reason      结束通话原因
     * @param callTime    总计通话时间
     *
     * \~english
     * Ending a Call
     * @param callType    call type
     * @param channelName channel name
     * @param reason      Reason for Ending a Call
     * @param callTime    total call time
     */
    void onEndCallWithReason(EaseCallType callType, String channelName, EaseCallEndReason reason, long callTime);


    /**
     * \~chinese
     * 接收到通话邀请
     * @param callType  通话类型
     * @param fromUserId  邀请者的userId(AgoraChat userId)
     * @param ext   邀请扩展信息,JSONObject 类型
     *
     * \~english
     * receive a invite call
     * @param callType  call type
     * @param fromUserId inviter's userId(AgoraChat userId)
     * @param ext  the ext information of invite ,type of JSONObject
     */
    void onReceivedCall(EaseCallType callType, String fromUserId, JSONObject ext);

    /**
     * \~chinese
     * 构建 RTC token
     * 用户可以在{@link EaseCallKitConfig#setEnableRTCToken(boolean)}中配置是否启用RTC token校验，如果不启用，则这个方法不会被回调。反之。
     * 在这个方法里面，用户应该先去自己的服务器获取声网 RTC token和声网uid,然后把RTC token和声网uid回调给参数中传递进来的callback对象，使得callkit内部能拿到 RTC token及uid去加入对应的房间。
     * 获取RTC Token成功：则调用callback回调对象的{@link EaseCallKitTokenCallback#onSetToken(java.lang.String, int)}方法
     * 失败：则调用callback回调对象的{@link EaseCallKitTokenCallback#onGetTokenError(int, java.lang.String)}方法,传递过去返回错误码和错误消息。
     * @param userId   用户自己的userId(AgoraChat userId)
     * @param channelName 房间名称
     * @param callback  回调对象
     *
     * \~english
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
     * \~chinese
     * 通话错误的回调
     * @param type        错误类型 参考{@link EaseCallError}
     * @param errorCode   错误码 参考{@link io.agora.chat.callkit.general.EaseCallProcessError}
     * @param description 错误描述
     *
     * \~english
     * Call error callback
     * @param type  call error type ,see {@link EaseCallError}
     * @param errorCode errorCode ,see {@link io.agora.chat.callkit.general.EaseCallProcessError}
     * @param description  the error des
     */
    void onCallError(EaseCallError type, int errorCode, String description);


    /**
     * \~chinese
     * 用户发送通话邀请消息时的回调
     *
     * \~english
     * Callback when a user sends a call invitation message
     */
    void onInViteCallMessageSent();


    /**
     * \~chinese
     * 远端用户加入房间时回调
     * 在这里用户应该先通过传递进来的参数去自己的app服务器中去查询uid对应的AgoraChat userId,封装成一个{@link EaseUserAccount}对象，再回调给通过参数传递进来的callback对象，
     * 成功则调用其{@link EaseCallGetUserAccountCallback#onUserAccount(io.agora.chat.callkit.bean.EaseUserAccount) } 将{@link EaseUserAccount}对象传递过去。
     * 失败则调用其{@link EaseCallGetUserAccountCallback#onSetUserAccountError(int, java.lang.String) } 将错误码和错误描述传递过去。
     * @param channelName 房间名称
     * @param userName 用户userId(AgoraChat userId)
     * @param uid 声网uid
     * @param callback 回调对象
     *
     * \~english
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
     * \~chinese
     * 通知用户更新用户信息，当callkit内部在发生UI变化或者接收到channel中的一些变化事件时触发。
     * 用户可在这里自定义用户昵称，图像等。注意更新过程放在同步方法里，才能实现及时刷新页面。用户没这个需求可不用实现。
     * @param userName 用户userId(AgoraChat userId)
     *
     * \~english
     * Notifies the user to update the user information，Fired when a UI change occurs within the Callkit or some change event is received in a channel。
     * Users can customize user nicknames, images and so on here.
     * Note that the update process is placed in the synchronous method to achieve timely page refresh.Users do not need to achieve this requirement.
     * @param userName userId(AgoraChat userId)
     */
    default void onUserInfoUpdate(String userName){}
}