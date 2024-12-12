package io.agora.chat.callkit;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static io.agora.chat.callkit.general.EaseCallEndReason.EaseCallEndReasonHandleOnOtherDeviceAgreed;
import static io.agora.chat.callkit.general.EaseCallEndReason.EaseCallEndReasonHandleOnOtherDeviceRefused;
import static io.agora.chat.callkit.general.EaseCallError.PROCESS_ERROR;
import static io.agora.chat.callkit.general.EaseCallProcessError.CALL_PARAM_ERROR;
import static io.agora.chat.callkit.general.EaseCallType.CONFERENCE_VOICE_CALL;
import static io.agora.chat.callkit.general.EaseCallType.SINGLE_VOICE_CALL;
import static io.agora.chat.callkit.utils.EaseCallKitUtils.bring2Front;
import static io.agora.chat.callkit.utils.EaseCallKitUtils.isAppRunningForeground;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.CALL_INVITE_EXT;
import static io.agora.chat.callkit.utils.EaseCallStatusBarCompat.getStatusBarHeight;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.CallBack;
import io.agora.MessageListener;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.chat.CmdMessageBody;
import io.agora.chat.Conversation;
import io.agora.chat.GroupReadAck;
import io.agora.chat.MessageReactionChange;
import io.agora.chat.callkit.bean.EaseCallInfo;
import io.agora.chat.callkit.event.EaseCallAlertEvent;
import io.agora.chat.callkit.event.EaseCallAnswerEvent;
import io.agora.chat.callkit.event.EaseCallBaseEvent;
import io.agora.chat.callkit.event.EaseCallCallCancelEvent;
import io.agora.chat.callkit.event.EaseCallConfirmCallEvent;
import io.agora.chat.callkit.event.EaseCallConfirmRingEvent;
import io.agora.chat.callkit.event.EaseCallInviteEventEase;
import io.agora.chat.callkit.general.EaseCallAction;
import io.agora.chat.callkit.general.EaseCallEndReason;
import io.agora.chat.callkit.general.EaseCallError;
import io.agora.chat.callkit.general.EaseCallFloatWindow;
import io.agora.chat.callkit.general.EaseCallKitConfig;
import io.agora.chat.callkit.general.EaseCallProcessError;
import io.agora.chat.callkit.general.EaseCallState;
import io.agora.chat.callkit.general.EaseCallType;
import io.agora.chat.callkit.listener.EaseCallKitListener;
import io.agora.chat.callkit.livedatas.EaseCallLiveDataBus;
import io.agora.chat.callkit.ui.EaseCallBaseActivity;
import io.agora.chat.callkit.ui.EaseCallMultipleBaseActivity;
import io.agora.chat.callkit.ui.EaseCallSingleBaseActivity;
import io.agora.chat.callkit.utils.EaseCallAudioControl;
import io.agora.chat.callkit.utils.EaseCallKitNotifier;
import io.agora.chat.callkit.utils.EaseCallKitUtils;
import io.agora.chat.callkit.utils.EaseCallMsgUtils;
import io.agora.exceptions.ChatException;
import io.agora.util.EMLog;
import io.agora.util.EasyUtils;


/**
 * The kit is a help class to help developers use CallKit, it provides methods to launch audio and video
 */
public class EaseCallKit {
    private static final String TAG = EaseCallKit.class.getSimpleName();
    private static EaseCallKit instance = null;
    private boolean callKitInit = false;
    private Context mContext = null;
    private MessageListener messageListener = null;
    private EaseCallType callType = EaseCallType.SINGLE_VIDEO_CALL;
    private EaseCallState callState = EaseCallState.CALL_IDLE;
    private String channelName;
    private String fromUserId;
    public static String deviceId = "android_";
    public String clallee_devId;
    private String callID = null;
    private JSONObject inviteExt = null;
    private EaseCallInfo callInfo = new EaseCallInfo();
    private TimeHandler timeHandler;
    private Map<String, EaseCallInfo> callInfoMap = new HashMap<>();
    private EaseCallKitListener callListener;
    private static boolean isComingCall = true;
    private ArrayList<String> inviteeUsers = new ArrayList<>();
    private EaseCallKitConfig callKitConfig;
    private EaseCallKitNotifier notifier;
    private Class<? extends EaseCallBaseActivity> curCallCls;
    private Handler handler;
    private View headview;
    private int largestNumInChannel = 16;
    private boolean isAgreedInHeadDialog = false;
    /**
     * If use the default class, you should register it to AndroidManifest
     */
    private Class<? extends EaseCallSingleBaseActivity> defaultVideoCallCls = EaseCallSingleBaseActivity.class;

    /**
     * If use the default class, you should register it to AndroidManifest
     */
    private Class<? extends EaseCallMultipleBaseActivity> defaultMultiVideoCls = EaseCallMultipleBaseActivity.class;

    private EaseCallKit() {
    }

    public static EaseCallKit getInstance() {
        if (instance == null) {
            synchronized (EaseCallKit.class) {
                if (instance == null) {
                    instance = new EaseCallKit();
                }
            }
        }
        return instance;
    }

    public boolean isAgreedInHeadDialog() {
        return isAgreedInHeadDialog;
    }

    public void setAgreedInHeadDialog(boolean agreedInHeadDialog) {
        isAgreedInHeadDialog = agreedInHeadDialog;
    }

    /**
     * Initializes the EaseCallkit.
     * Make sure to initialize the SDK in the main thread.
     * @param context Make sure to set the param.
     * @param config  The configurations. Make sure to set the param, see {@link EaseCallKitConfig}.
     */
    public synchronized boolean init(Context context, EaseCallKitConfig config) {
        if (callKitInit) {
            return true;
        }
        removeMessageListener();
        mContext = context;
        if (!isMainProcess(mContext)) {
            Log.e(TAG, "enter the service process!");
            return false;
        }

        // Obtain the device serial number
        deviceId += EaseCallKitUtils.getPhoneSign();
        timeHandler = new TimeHandler();

        // Set the CallKit configuration item
        callKitConfig = new EaseCallKitConfig();
        callKitConfig.setAgoraAppId(config.getAgoraAppId());
        callKitConfig.setUserInfoMap(config.getUserInfoMap());
        callKitConfig.setDefaultHeadImage(config.getDefaultHeadImage());
        callKitConfig.setCallTimeOut(config.getCallTimeOut());
        callKitConfig.setRingFile(config.getRingFile());
        callKitConfig.setEnableRTCToken(config.isEnableRTCToken());

        //init notifier
        initNotifier();

        // Add the received message callback
        addMessageListener();
        callKitInit = true;

        handler = new Handler(mContext.getMainLooper());

        EaseCallAudioControl.getInstance().init(context);
        registerActivityLifecycleCallbacks(mContext);
        return true;
    }

    private WeakReference<Activity> currentActivity;

    private void registerActivityLifecycleCallbacks(Context appContext) {
        EMLog.d(TAG, "registerActivityLifecycleCallbacks");
        ((Application) appContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = new WeakReference<>(activity);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (headview != null && headview.getParent() != null) {
                            ((ViewGroup) headview.getParent()).removeView(headview);
                            String fromUserId = (String) headview.getTag(R.id.tag_from_userid);
                            EaseCallType callType = (EaseCallType) headview.getTag(R.id.tag_call_type);
                            showCallingHeadDialog(fromUserId, callType);
                        }
                    }
                });
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }

    /**
     * Register the activity which you want to display video call or audio call and you have registered in AndroidManifest.xml
     * @param videoCallClass the activity'class which you want to display video call or audio call
     */
    public void registerVideoCallClass(Class<? extends EaseCallSingleBaseActivity> videoCallClass) {
        defaultVideoCallCls = videoCallClass;
    }

    /**
     * Register the activity which you want to display multiple video call or audio call and you have registered in AndroidManifest.xml
     * @param multipleVideoClass the activity's class which you want to display multiple video call or audio call
     */
    public void registerMultipleVideoClass(Class<? extends EaseCallMultipleBaseActivity> multipleVideoClass) {
        defaultMultiVideoCls = multipleVideoClass;
    }

    /**
     * Gets callkitconfig
     * @return the current callKit configuration, see {@link EaseCallKitConfig}
     */
    public EaseCallKitConfig getCallKitConfig() {
        return callKitConfig;
    }


    private void initNotifier() {
        notifier = new EaseCallKitNotifier(mContext);
    }


    /**
     * Join in the 1V1 call
     * @param type call type (only {@link EaseCallType#SINGLE_VOICE_CALL } or {@link EaseCallType#SINGLE_VIDEO_CALL }）
     * @param user Called userId(AgoraChat userId)，not null or empty
     * @param ext  Extended fields (user extended fields) ,you can pass null if not needed
     */
    public void startSingleCall(final EaseCallType type, final String user, final Map<String, Object> ext) {
        startSingleCall(type, user, ext, defaultVideoCallCls);
    }


    /**
     * Join in the 1V1 call
     * @param type call type (only {@link EaseCallType#SINGLE_VOICE_CALL } or {@link EaseCallType#SINGLE_VIDEO_CALL }）
     * @param user Called userId(AgoraChat userId)，not null or empty
     * @param ext  Extended fields (user extended fields), you can pass null if not needed
     * @param cls  Inherited from {@link EaseCallSingleBaseActivity} activity corresponding to the class
     */
    public void startSingleCall(final EaseCallType type, final String user, final Map<String, Object> ext, Class<? extends EaseCallSingleBaseActivity> cls) {
        if (callState != EaseCallState.CALL_IDLE) {
            if (callListener != null) {
                callListener.onCallError(PROCESS_ERROR, EaseCallProcessError.CALL_STATE_ERROR.code, "current state is busy");
            }
            return;
        }
        if (type == EaseCallType.CONFERENCE_VIDEO_CALL || type == CONFERENCE_VOICE_CALL) {
            if (callListener != null) {
                callListener.onCallError(PROCESS_ERROR, EaseCallProcessError.CALL_TYPE_ERROR.code, "call type is error");
            }
            return;
        }
        if (user != null && user.length() == 0) {
            if (callListener != null) {
                callListener.onCallError(PROCESS_ERROR, CALL_PARAM_ERROR.code, "user is null");
            }
            return;
        }
        callType = type;
        // Change the active call status
        callState = EaseCallState.CALL_OUTGOING;
        fromUserId = user;
        if (ext != null) {
            inviteExt = EaseCallKitUtils.convertMapToJSONObject(ext);
        }
        curCallCls = cls;
        // Start a 1V1 call
        Intent intent = new Intent(mContext, curCallCls).addFlags(FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        isComingCall = false;
        bundle.putBoolean("isComingCall", false);
        bundle.putString("username", user);
        channelName = EaseCallKitUtils.getRandomString(10);
        bundle.putString("channelName", channelName);
        intent.putExtras(bundle);
        mContext.startActivity(intent);
    }



    /**
     * Invite to join a multi-party call
     * @param type call type (only {@link EaseCallType#CONFERENCE_VIDEO_CALL } or {@link EaseCallType#CONFERENCE_VOICE_CALL }）
     * @param users List of invited user ids (AgoraChat userId)
     * @param ext  Extended fields (user extended fields), you can pass null if not needed
     */
    public void startInviteMultipleCall(final EaseCallType type, final String[] users, final Map<String, Object> ext) {
        startInviteMultipleCall(type, users, ext, defaultMultiVideoCls);
    }


    /**
     * Invite to join a multi-party call
     * @param type call type (only {@link EaseCallType#CONFERENCE_VIDEO_CALL } or {@link EaseCallType#CONFERENCE_VOICE_CALL }）
     * @param users List of invited user ids (AgoraChat userId)
     * @param ext  Extended fields (user extended fields), you can pass null if not needed
     * @param cls  Inherited from {@link EaseCallMultipleBaseActivity} activity corresponding to the class
     */
    public void startInviteMultipleCall(final EaseCallType type, final String[] users, final Map<String, Object> ext, Class<? extends EaseCallMultipleBaseActivity> cls) {
        if (users != null && users.length > largestNumInChannel) {
            callListener.onCallError(PROCESS_ERROR, CALL_PARAM_ERROR.code, mContext.getString(R.string.ease_call_max_people_in_channel));
            return;
        }
        if (type == SINGLE_VOICE_CALL || type == EaseCallType.SINGLE_VIDEO_CALL) {
            if (callListener != null) {
                callListener.onCallError(PROCESS_ERROR, EaseCallProcessError.CALL_TYPE_ERROR.code, "call type is error");
            }
            return;
        }
        if (users == null || users.length == 0) {
            // Click in from a group chat
            inviteeUsers.clear();
            callType = type;
            curCallCls = cls;
            callState = EaseCallState.CALL_OUTGOING;
            if (ext != null) {
                inviteExt = EaseCallKitUtils.convertMapToJSONObject(ext);
                try {
                    channelName = inviteExt.getString(EaseCallMsgUtils.CALL_CHANNELNAME);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Intent intent = new Intent(mContext, curCallCls);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            Bundle bundle = new Bundle();
            isComingCall = false;
            bundle.putBoolean("isComingCall", false);
            bundle.putString("channelName", channelName);
            intent.putExtras(bundle);
            mContext.startActivity(intent);
        } else {
            callType = type;
            inviteeUsers.clear();
            for (String user : users) {
                inviteeUsers.add(user);
            }
            // You have not joined a channel yet
            if (curCallCls == null) {
                if (users != null && users.length > 0) {
                    //改为主动呼叫状态
                    if (ext != null) {
                        inviteExt = EaseCallKitUtils.convertMapToJSONObject(ext);
                    }
                    callState = EaseCallState.CALL_OUTGOING;
                    curCallCls = cls;
                    Intent intent = new Intent(mContext, curCallCls);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    isComingCall = false;
                    bundle.putBoolean("isComingCall", false);
                    channelName = EaseCallKitUtils.getRandomString(10);
                    bundle.putString("channelName", channelName);
                    intent.putExtras(bundle);
                    mContext.startActivity(intent);
                }
            } else {
                // Invite members to join
                Intent intent = new Intent(mContext, curCallCls).addFlags(FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        }
    }

    /**
     * The method is used for {@link io.agora.chat.callkit.general.EaseCallFloatWindow}, other methods are not recommended
     *
     * @return Current call activity's class, maybe is null.
     */
    public Class<? extends EaseCallBaseActivity> getCurrentCallClass() {
        return curCallCls;
    }

    /**
     * If you call {@link #startSingleCall(EaseCallType, String, Map)}, {@link #startSingleCall(EaseCallType, String, Map, Class)}
     * or {@link #startInviteMultipleCall(EaseCallType, String[], Map)}, you should call the method of {@link #releaseCall()} when the {@link #curCallCls} is finishing.
     */
    public void releaseCall() {
        if (curCallCls != null) {
            curCallCls = null;
        }
    }

    public int getLargestNumInChannel() {
        return largestNumInChannel;
    }

    /**
     * Add MessageListener
     */
    private void addMessageListener() {
        this.messageListener = new MessageListener() {
            @Override
            public void onMessageReceived(List<ChatMessage> messages) {
                for (ChatMessage message : messages) {
                    String messageType = message.getStringAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, "");
                    EMLog.d(TAG, "Receive msg:" + message.getMsgId() + " from:" + message.getFrom() + "  messageType:" + messageType);
                    // About call control signaling
                    if (TextUtils.equals(messageType, EaseCallMsgUtils.CALL_MSG_INFO)
                            && !TextUtils.equals(message.getFrom(), ChatClient.getInstance().getCurrentUser())) {
                        String action = message.getStringAttribute(EaseCallMsgUtils.CALL_ACTION, "");
                        String callerDevId = message.getStringAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, "");
                        String fromCallId = message.getStringAttribute(EaseCallMsgUtils.CLL_ID, "");
                        String fromUser = message.getFrom();
                        String channel = message.getStringAttribute(EaseCallMsgUtils.CALL_CHANNELNAME, "");
                        JSONObject ext = null;
                        try {
                            ext = message.getJSONObjectAttribute(CALL_INVITE_EXT);
                        } catch (ChatException exception) {
                            exception.printStackTrace();
                        }

                        if (action == null || callerDevId == null || fromCallId == null || fromUser == null || channel == null) {
                            if (callListener != null) {
                                callListener.onCallError(PROCESS_ERROR, EaseCallProcessError.CALL_RECEIVE_ERROR.code, "receive message error");
                            }
                            continue;
                        }
                        EaseCallAction callAction = EaseCallAction.getfrom(action);
                        switch (callAction) {
                            case CALL_INVITE: // Received a call invitation
                                if(message.getChatType()== ChatMessage.ChatType.GroupChat) {
                                    return;
                                }
                                int calltype = message.getIntAttribute(EaseCallMsgUtils.CALL_TYPE, 0);
                                EaseCallType callkitType = EaseCallType.getfrom(calltype);
                                if (callState != EaseCallState.CALL_IDLE) {
                                    if (TextUtils.equals(fromCallId, callID) && TextUtils.equals(fromUser, fromUserId)
                                            && callkitType == SINGLE_VOICE_CALL && callType == EaseCallType.SINGLE_VIDEO_CALL) {
                                        EaseCallInviteEventEase inviteEvent = new EaseCallInviteEventEase();
                                        inviteEvent.callId = fromCallId;
                                        inviteEvent.type = callkitType;

                                        // Publish the message
                                        EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(inviteEvent);
                                    } else {
                                        // Send busy status
                                        EaseCallAnswerEvent callEvent = new EaseCallAnswerEvent();
                                        callEvent.result = EaseCallMsgUtils.CALL_ANSWER_BUSY;
                                        callEvent.callerDevId = callerDevId;
                                        callEvent.callId = fromCallId;
                                        sendCmdMsg(callEvent, fromUser);
                                    }
                                } else {
                                    callInfo.setCallerDevId(callerDevId);
                                    callInfo.setCallId(fromCallId);
                                    callInfo.setCallKitType(callkitType);
                                    callInfo.setChannelName(channel);
                                    callInfo.setComming(true);
                                    callInfo.setFromUser(fromUser);
                                    callInfo.setExt(ext);

                                    // Add the invitation information to the list
                                    callInfoMap.put(fromCallId, callInfo);

                                    // Send an alert message
                                    EaseCallAlertEvent callEvent = new EaseCallAlertEvent();
                                    callEvent.callerDevId = callerDevId;
                                    callEvent.callId = fromCallId;
                                    sendCmdMsg(callEvent, fromUser);

                                    // Start timer
                                    timeHandler.startTime();
                                }
                                break;
                            default:
                                break;
                        }

                    }
                }
            }
            @Override
            public void onCmdMessageReceived(List<ChatMessage> messages) {
                for (ChatMessage message : messages) {
                    String messageType = message.getStringAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, "");
                    EMLog.d(TAG, "Receive cmdmsg:" + message.getMsgId() + " from:" + message.getFrom() + "  messageType:" + messageType);
                    // About call control signaling
                    if (TextUtils.equals(messageType, EaseCallMsgUtils.CALL_MSG_INFO)
                            && !TextUtils.equals(message.getFrom(), ChatClient.getInstance().getCurrentUser())) {
                        String action = message.getStringAttribute(EaseCallMsgUtils.CALL_ACTION, "");
                        String callerDevId = message.getStringAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, "");
                        String fromCallId = message.getStringAttribute(EaseCallMsgUtils.CLL_ID, "");
                        String fromUser = message.getFrom();
                        String channel = message.getStringAttribute(EaseCallMsgUtils.CALL_CHANNELNAME, "");
                        EaseCallAction callAction = EaseCallAction.getfrom(action);

                        if (action == null || callerDevId == null || fromCallId == null || fromUser == null || channel == null) {
                            if (callListener != null) {
                                callListener.onCallError(PROCESS_ERROR, EaseCallProcessError.CALL_RECEIVE_ERROR.code, "receive message error");
                            }
                            continue;
                        }

                        switch (callAction) {
                            case CALL_CANCEL: // Cancel the call
                                if (callState == EaseCallState.CALL_IDLE) {
                                    timeHandler.stopTime();
                                    // Cancel calling
                                    callInfoMap.remove(fromCallId);
                                    hideCallingHeadDialog();
                                    resetState();
                                } else {
                                    EaseCallCallCancelEvent event = new EaseCallCallCancelEvent();
                                    event.callerDevId = callerDevId;
                                    event.callId = fromCallId;
                                    event.userId = fromUser;
                                    if (TextUtils.equals(callID, fromCallId)) {
                                        callState = EaseCallState.CALL_IDLE;
                                        hideCallingHeadDialog();
                                        resetState();
                                    }
                                    notifier.reset();
                                    //publish the message
                                    EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(event);
                                }

                                break;
                            case CALL_ALERT:
                                String calleedDeviceId = message.getStringAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, "");
                                EaseCallAlertEvent alertEvent = new EaseCallAlertEvent();
                                alertEvent.callId = fromCallId;
                                alertEvent.calleeDevId = calleedDeviceId;
                                alertEvent.userId = fromUser;
                                EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(alertEvent);
                                break;
                            case CALL_CONFIRM_RING: // Check whether the received callId is valid
                                String calledDvId = message.getStringAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, "");
                                boolean vaild = message.getBooleanAttribute(EaseCallMsgUtils.CALL_STATUS, false);
                                // This parameter is used to distinguish DrviceId from other devices. The called party processes the CALL_CONFIRM_RING of its own device Id
                                if (TextUtils.equals(calledDvId, deviceId)) {
                                    timeHandler.stopTime();
                                    if (!vaild) {
                                        // The call is invalid
                                        callInfoMap.remove(fromCallId);
                                    } else {
                                        //the received callId is vaild
                                        if (callState == EaseCallState.CALL_IDLE) {
                                            callState = EaseCallState.CALL_ALERTING;
                                            // Device information of the calling party
                                            clallee_devId = callerDevId;
                                            callID = fromCallId;
                                            EaseCallInfo info = callInfoMap.get(fromCallId);
                                            if (info != null) {
                                                channelName = info.getChannelName();
                                                callType = info.getCallKitType();
                                                fromUserId = info.getFromUser();
                                                inviteExt = info.getExt();
                                            }
                                            // A valid map invitation was received
                                            callInfoMap.clear();
                                            showCallingHeadDialog(fromUserId, callType);
                                        } else {
                                            //the call is invalid
                                            callInfoMap.remove(fromCallId);
                                            timeHandler.stopTime();
                                        }
                                    }
                                }

                                break;
                            case CALL_CONFIRM_CALLEE:
                                if(!TextUtils.equals(callID,fromCallId)) {
                                    break;
                                }
                                // Received the arbitration message
                                String result = message.getStringAttribute(EaseCallMsgUtils.CALL_RESULT, "");
                                String calledDevId = message.getStringAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, "");
                                EaseCallConfirmCallEvent event = new EaseCallConfirmCallEvent();
                                event.calleeDevId = calledDevId;
                                event.result = result;
                                event.callerDevId = callerDevId;
                                event.callId = fromCallId;
                                event.userId = fromUser;

                                //is self
                                if (TextUtils.equals(calledDevId, EaseCallKit.deviceId)) {
                                    if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                                        if (callListener != null) {
                                            callListener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRefuse, 0);
                                        }
                                    }
                                } else {
                                    hideCallingHeadDialog();
                                    resetState();
                                    //handled in another device
                                    EaseCallEndReason reason = null;
                                    if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                                        //agreed in another device
                                        reason = EaseCallEndReasonHandleOnOtherDeviceAgreed;
                                    } else if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                                        //refused in another device
                                        reason = EaseCallEndReasonHandleOnOtherDeviceRefused;
                                    }
                                    if (callListener != null) {
                                        //handled in another device
                                        callListener.onEndCallWithReason(callType, channelName, reason, 0);
                                    }
                                }
                                //publish the message
                                EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(event);
                                break;
                            case CALL_ANSWER: // Received a reply message from the called party
                                String result1 = message.getStringAttribute(EaseCallMsgUtils.CALL_RESULT, "");
                                String calledDevId1 = message.getStringAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, "");
                                boolean transVoice = message.getBooleanAttribute(EaseCallMsgUtils.CALLED_TRANSE_VOICE, false);
                                // Check that the roaming message is not received by another device of the called party or by the calling party
                                if (callType != EaseCallType.CONFERENCE_VIDEO_CALL && callType != CONFERENCE_VOICE_CALL) {
                                    if (!isComingCall || TextUtils.equals(calledDevId1, deviceId)) {
                                        EaseCallAnswerEvent answerEvent = new EaseCallAnswerEvent();
                                        answerEvent.result = result1;
                                        answerEvent.calleeDevId = calledDevId1;
                                        answerEvent.callerDevId = callerDevId;
                                        answerEvent.callId = fromCallId;
                                        answerEvent.userId = fromUser;
                                        answerEvent.transVoice = transVoice;

                                        EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(answerEvent);
                                    }
                                } else {
                                    if (!TextUtils.equals(fromUser, ChatClient.getInstance().getCurrentUser())) {
                                        EaseCallAnswerEvent answerEvent = new EaseCallAnswerEvent();
                                        answerEvent.result = result1;
                                        answerEvent.calleeDevId = calledDevId1;
                                        answerEvent.callerDevId = callerDevId;
                                        answerEvent.callId = fromCallId;
                                        answerEvent.userId = fromUser;
                                        answerEvent.transVoice = transVoice;

                                        EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(answerEvent);

                                    }
                                }
                                break;
                            case CALL_VIDEO_TO_VOICE:
                                if (callState != EaseCallState.CALL_IDLE) {
                                    if (TextUtils.equals(fromCallId, callID)
                                            && TextUtils.equals(fromUser, fromUserId)) {
                                        EaseCallInviteEventEase inviteEvent = new EaseCallInviteEventEase();
                                        inviteEvent.callId = fromCallId;
                                        inviteEvent.type = SINGLE_VOICE_CALL;

                                        EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(inviteEvent);
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            @Override
            public void onMessageRead(List<ChatMessage> messages) {}

            @Override
            public void onGroupMessageRead(List<GroupReadAck> groupReadAcks) {}

            @Override
            public void onMessageDelivered(List<ChatMessage> messages) {}

            @Override
            public void onMessageRecalled(List<ChatMessage> messages) {}

            @Override
            public void onMessageChanged(ChatMessage message, Object change) {}

            @Override
            public void onReadAckForGroupMessageUpdated() {}

            @Override
            public void onReactionChanged(List<MessageReactionChange> messageReactionChangeList) {}
        };
        // add message listening
        ChatClient.getInstance().chatManager().addMessageListener(this.messageListener);

    }

    private void showCallingHeadDialog(String fromUserId, EaseCallType callType) {
        if (!isAppRunningForeground(mContext)) {
            bring2Front(mContext);
        }
        this.callType = callType;
        if (currentActivity == null) {
            return;
        }
        Activity activity = currentActivity.get();
        EMLog.d(TAG, "showCallingHeadDialog activity=" + activity);
        EMLog.d(TAG, "fromUserId =" + fromUserId);
        if (activity != null) {
            FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
            decorView.post(new Runnable() {
                @Override
                public void run() {
                    // The phone starts ringing
                    if (isAppRunningForeground(mContext)) {
                        EaseCallAudioControl.getInstance().playRing();
                    }
                    headview = LayoutInflater.from(activity).inflate(R.layout.ease_call_head_view, decorView, false);
                    decorView.addView(headview);
                    headview.setTag(R.id.tag_from_userid, fromUserId);
                    headview.setTag(R.id.tag_call_type, callType);

                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) headview.getLayoutParams();
                    layoutParams.setMargins(layoutParams.leftMargin, getStatusBarHeight(activity.getApplicationContext()), layoutParams.rightMargin, layoutParams.topMargin);

                    TextView tvName = headview.findViewById(R.id.tv_name);
                    TextView subTitle = headview.findViewById(R.id.tv_subtitle);
                    ImageButton btnAgree = headview.findViewById(R.id.btn_call_head_agree);
                    ImageButton btnRefuse = headview.findViewById(R.id.btn_call_head_refuse);

                    tvName.setText(fromUserId);
                    switch (callType) {
                        case SINGLE_VOICE_CALL:
                            subTitle.setText(activity.getString(R.string.ease_call_head_view_single_audio_subtitle));
                            break;
                        case CONFERENCE_VOICE_CALL:
                            subTitle.setText(activity.getString(R.string.ease_call_head_view_multiply_audio_subtitle));
                            break;
                        case SINGLE_VIDEO_CALL:
                            subTitle.setText(activity.getString(R.string.ease_call_head_view_single_video_subtitle));
                            break;
                        case CONFERENCE_VIDEO_CALL:
                            subTitle.setText(activity.getString(R.string.ease_call_head_view_multiply_video_subtitle));
                            break;
                    }

                    headview.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideCallingHeadDialog();
                            timeHandler.startSendEvent(false);
                            EMLog.d(TAG, " click headview");
                        }
                    });
                    btnAgree.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            hideCallingHeadDialog();
                            timeHandler.startSendEvent(true);
                            EMLog.d(TAG, "btnAgree removeView(headview)");
                        }
                    });
                    btnRefuse.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refused();
                        }
                    });
                }
            });

        }
    }

    private void refused() {
        // Send a rejection message
        EaseCallAnswerEvent event = new EaseCallAnswerEvent();
        event.result = EaseCallMsgUtils.CALL_ANSWER_REFUSE;
        event.callId = EaseCallKit.getInstance().getCallID();
        event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
        event.calleeDevId = EaseCallKit.deviceId;
        sendCmdMsg(event, fromUserId, new CallBack() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(int code, String error) {
                if (callListener != null) {
                    callListener.onCallError(EaseCallError.IM_ERROR, code, error);
                }
            }
        });
        hideCallingHeadDialog();
        resetState();
        EMLog.d(TAG, "btnRefuse removeView(headview)");
    }

    private void hideCallingHeadDialog() {
        // Stop ringing
        EaseCallAudioControl.getInstance().stopPlayRing();
        if (headview != null) {
            ViewParent parent = headview.getParent();
            if (parent != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ((ViewGroup) parent).removeView(headview);
                        headview = null;
                    }
                });
            }
        }
    }

    private void resetState() {
        setCallState(EaseCallState.CALL_IDLE);
        setCallID(null);
    }

    /***
     * Set call Kit listening
     * @param listener
     * @return
     */
    public void setCallKitListener(EaseCallKitListener listener) {
        this.callListener = listener;
    }


    /***
     * Remove call Kit listening
     * @param listener
     * @return
     */
    public void removeCallKitListener(EaseCallKitListener listener) {
        this.callListener = null;
    }


    /**
     * Remote message listener
     */
    private void removeMessageListener() {
        ChatClient.getInstance().chatManager().removeMessageListener(messageListener);
        messageListener = null;
    }

    public EaseCallState getCallState() {
        return callState;
    }

    public EaseCallType getCallType() {
        return callType;
    }

    public void setCallType(EaseCallType callType) {
        this.callType = callType;
    }

    public void setCallState(EaseCallState callState) {
        this.callState = callState;
    }

    public String getCallID() {
        return callID;
    }

    public void setCallID(String callID) {
        this.callID = callID;
    }

    public String getClallee_devId() {
        return clallee_devId;
    }

    public EaseCallKitListener getCallListener() {
        return callListener;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public boolean getIsComingCall() {
        return isComingCall;
    }

    public EaseCallKitNotifier getNotifier() {
        return notifier;
    }

    private boolean isMainProcess(Context context) {
        String processName;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            processName = getProcessNameByApplication();
        }else {
            processName = getProcessNameByReflection();
        }
        return context.getApplicationInfo().packageName.equals(processName);
    }

    private String getProcessNameByReflection() {
        String processName = null;
        try {
            final Method declaredMethod = Class.forName("android.app.ActivityThread", false, Application.class.getClassLoader())
                    .getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
            declaredMethod.setAccessible(true);
            final Object invoke = declaredMethod.invoke(null, new Object[0]);
            if (invoke instanceof String) {
                processName = (String) invoke;
            }
        } catch (Throwable e) {
        }
        return processName;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private String getProcessNameByApplication() {
        return Application.getProcessName();
    }


    /**
     * Send CMD reply message
     * @param username
     * @param event
     */
    private void sendCmdMsg(EaseCallBaseEvent event, String username) {
        final ChatMessage message = ChatMessage.createSendMessage(ChatMessage.Type.CMD);
        message.setTo(username);
        String action = "rtcCall";
        CmdMessageBody cmdBody = new CmdMessageBody(action);
        message.addBody(cmdBody);

        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, event.callAction.state);
        message.setAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, event.callerDevId);
        message.setAttribute(EaseCallMsgUtils.CLL_ID, event.callId);
        message.setAttribute(EaseCallMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
        message.setAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, EaseCallMsgUtils.CALL_MSG_INFO);
        if (event.callAction == EaseCallAction.CALL_ANSWER) {
            message.setAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, deviceId);
            message.setAttribute(EaseCallMsgUtils.CALL_RESULT, ((EaseCallAnswerEvent) event).result);
        } else if (event.callAction == EaseCallAction.CALL_ALERT) {
            message.setAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, deviceId);
        }
        final Conversation conversation = ChatClient.getInstance().chatManager().getConversation(username, Conversation.ConversationType.Chat, true);
        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
                conversation.removeMessage(message.getMsgId());
                if (callListener != null) {
                    callListener.onCallError(EaseCallError.IM_ERROR, code, error);
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        ChatClient.getInstance().chatManager().sendMessage(message);
    }


    private class TimeHandler extends Handler {
        private final int MSG_TIMER = 0;
        private final int MSG_START_ACTIVITY = 1;
        private int timePassed = 0;

        public TimeHandler() {

        }

        public void startTime() {
            timePassed = 0;
            sendEmptyMessageDelayed(MSG_TIMER, 1000);
        }

        public void startSendEvent(boolean _isAgreedInHeadDialog) {
            isAgreedInHeadDialog = _isAgreedInHeadDialog;
            sendEmptyMessage(MSG_START_ACTIVITY);
        }

        public void stopTime() {
            removeMessages(MSG_START_ACTIVITY);
            removeMessages(MSG_TIMER);
        }


        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TIMER) {
                timePassed++;
                if (timePassed * 1000 == EaseCallMsgUtils.CALL_INVITED_INTERVAL) {

                    // Call timed out
                    timeHandler.stopTime();
                    callState = EaseCallState.CALL_IDLE;
                }
                sendEmptyMessageDelayed(MSG_TIMER, 1000);
            } else if (msg.what == MSG_START_ACTIVITY) {
                timeHandler.stopTime();
                String info = "";
                String userName = EaseCallKitUtils.getUserNickName(fromUserId);
                if (callType != EaseCallType.CONFERENCE_VIDEO_CALL && callType != CONFERENCE_VOICE_CALL) {

                    // start the activity
                    curCallCls = defaultVideoCallCls;
                    Intent intent = new Intent(mContext, curCallCls).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    isComingCall = true;
                    bundle.putBoolean("isComingCall", true);
                    bundle.putString("channelName", channelName);
                    bundle.putString("username", fromUserId);
                    bundle.putBoolean("isAgreedInHeadDialog", isAgreedInHeadDialog);
                    intent.putExtras(bundle);
                    mContext.startActivity(intent);
                    if (Build.VERSION.SDK_INT >= 29 && !EasyUtils.isAppRunningForeground(mContext)) {
                        EMLog.e(TAG, "notifier.notify:" + info);
                        if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
                            info = mContext.getString(R.string.ease_call_alert_request_video, userName);
                        } else {
                            info = mContext.getString(R.string.ease_call_alert_request_voice, userName);
                        }
                        notifier.notify(intent, mContext.getString(R.string.ease_call_agora), info);
                    }
                } else {
                    // Start the multi-party call screen
                    curCallCls = defaultMultiVideoCls;
                    Intent intent = new Intent(mContext, curCallCls).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    isComingCall = true;
                    bundle.putBoolean("isComingCall", true);
                    bundle.putString("channelName", channelName);
                    bundle.putString("username", fromUserId);
                    bundle.putBoolean("isAgreedInHeadDialog", isAgreedInHeadDialog);
                    intent.putExtras(bundle);
                    mContext.startActivity(intent);
                    if (Build.VERSION.SDK_INT >= 29 && isAppRunningForeground(mContext)) {
                        info = mContext.getString(R.string.ease_call_alert_request_multiple_video, userName);
                        notifier.notify(intent, mContext.getString(R.string.ease_call_agora), info);
                    }
                }

                // Call invitation callback
                if (callListener != null) {
                    callListener.onReceivedCall(callType, fromUserId, inviteExt);
                }
            }
            super.handleMessage(msg);
        }
    }


    private boolean isDestroy(Activity mActivity) {
        if (mActivity == null ||
                mActivity.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed())) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<String> getInviteeUsers() {
        return inviteeUsers;
    }

    public void InitInviteeUsers() {
        inviteeUsers.clear();
    }

    public JSONObject getInviteExt() {
        return inviteExt;
    }

    public Context getContext() {
        return mContext;
    }

    public void sendCmdMsg(EaseCallBaseEvent event, String username, CallBack callBack) {
        final ChatMessage message = ChatMessage.createSendMessage(ChatMessage.Type.CMD);
        String action = "rtcCall";
        CmdMessageBody cmdBody = new CmdMessageBody(action);
        message.setTo(username);
        message.addBody(cmdBody);
        if (event.callAction.equals(EaseCallAction.CALL_VIDEO_TO_VOICE) ||
                event.callAction.equals(EaseCallAction.CALL_CANCEL)) {
            cmdBody.deliverOnlineOnly(false);
        } else {
            cmdBody.deliverOnlineOnly(true);
        }

        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, event.callAction.state);
        message.setAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, EaseCallKit.deviceId);
        message.setAttribute(EaseCallMsgUtils.CLL_ID, event.callId);
        message.setAttribute(EaseCallMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
        message.setAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, EaseCallMsgUtils.CALL_MSG_INFO);
        if (event.callAction == EaseCallAction.CALL_CONFIRM_RING) {
            message.setAttribute(EaseCallMsgUtils.CALL_STATUS, ((EaseCallConfirmRingEvent) event).valid);
            message.setAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, ((EaseCallConfirmRingEvent) event).calleeDevId);
        } else if (event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE) {
            message.setAttribute(EaseCallMsgUtils.CALL_RESULT, ((EaseCallConfirmCallEvent) event).result);
            message.setAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, ((EaseCallConfirmCallEvent) event).calleeDevId);
        } else if (event.callAction == EaseCallAction.CALL_ANSWER) {
            message.setAttribute(EaseCallMsgUtils.CALL_RESULT, ((EaseCallAnswerEvent) event).result);
            message.setAttribute(EaseCallMsgUtils.CALLED_DEVICE_ID, ((EaseCallAnswerEvent) event).calleeDevId);
            message.setAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, ((EaseCallAnswerEvent) event).callerDevId);
            message.setAttribute(EaseCallMsgUtils.CALLED_TRANSE_VOICE, ((EaseCallAnswerEvent) event).transVoice);
        }
        final Conversation conversation = ChatClient.getInstance().chatManager().getConversation(username, Conversation.ConversationType.Chat, true);
        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
                if (event.callAction == EaseCallAction.CALL_CANCEL) {
                    exitCall();
                } else if (event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE) {
                    // Exit the channel without being connected
                    if (!TextUtils.equals(((EaseCallConfirmCallEvent) event).result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                        exitCall();
                    }
                }
                callBack.onSuccess();
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
                if (conversation != null) {
                    conversation.removeMessage(message.getMsgId());
                }
                if (event.callAction == EaseCallAction.CALL_CANCEL) {
                    // Exit channel
                    exitCall();
                } else if (event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE) {
                    // Exit the channel without being connected
                    if (!TextUtils.equals(((EaseCallConfirmCallEvent) event).result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                        exitCall();
                    }
                }
                callBack.onError(code, error);
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        ChatClient.getInstance().chatManager().sendMessage(message);
    }

    private void exitCall() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (EaseCallFloatWindow.getInstance().isShowing()) {
                    EaseCallFloatWindow.getInstance(mContext).dismiss();
                }
                //reset state
                setCallState(EaseCallState.CALL_IDLE);
                setCallID(null);
                isAgreedInHeadDialog=false;
                isComingCall=false;
                fromUserId=null;
                channelName=null;
            }
        });
    }
}
