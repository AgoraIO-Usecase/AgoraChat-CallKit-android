package io.agora.chat.callkit.ui;

import static io.agora.chat.callkit.general.EaseCallError.PROCESS_ERROR;
import static io.agora.chat.callkit.general.EaseCallProcessError.CALL_PARAM_ERROR;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.CALL_INVITE_EXT;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.CALL_TIMER_CALL_TIME;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.CALL_TIMER_TIMEOUT;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_MAKE_CONFERENCE_VIDEO;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_MAKE_SIGNAL_VIDEO;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_MAKE_SIGNAL_VOICE;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_RELEASE_HANDLER;
import static io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
import static io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER;
import static io.agora.rtc.Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED;
import static io.agora.rtc.Constants.REMOTE_AUDIO_REASON_REMOTE_UNMUTED;
import static io.agora.rtc.Constants.REMOTE_AUDIO_STATE_DECODING;
import static io.agora.rtc.Constants.REMOTE_AUDIO_STATE_STARTING;
import static io.agora.rtc.Constants.REMOTE_AUDIO_STATE_STOPPED;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_DECODING;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_STOPPED;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import io.agora.CallBack;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.chat.CmdMessageBody;
import io.agora.chat.Conversation;
import io.agora.chat.callkit.EaseCallKit;
import io.agora.chat.callkit.R;
import io.agora.chat.callkit.bean.EaseCallUserInfo;
import io.agora.chat.callkit.bean.EaseUserAccount;
import io.agora.chat.callkit.databinding.EaseCallActivityMultipleBinding;
import io.agora.chat.callkit.event.EaseCallAlertEvent;
import io.agora.chat.callkit.event.EaseCallAnswerEvent;
import io.agora.chat.callkit.event.EaseCallBaseEvent;
import io.agora.chat.callkit.event.EaseCallCallCancelEvent;
import io.agora.chat.callkit.event.EaseCallConfirmCallEvent;
import io.agora.chat.callkit.event.EaseCallConfirmRingEvent;
import io.agora.chat.callkit.general.EaseCallAction;
import io.agora.chat.callkit.general.EaseCallEndReason;
import io.agora.chat.callkit.general.EaseCallError;
import io.agora.chat.callkit.general.EaseCallFloatWindow;
import io.agora.chat.callkit.general.EaseCallKitConfig;
import io.agora.chat.callkit.general.EaseCallState;
import io.agora.chat.callkit.general.EaseCallType;
import io.agora.chat.callkit.listener.EaseCallGetUserAccountCallback;
import io.agora.chat.callkit.listener.EaseCallKitListener;
import io.agora.chat.callkit.listener.EaseCallKitTokenCallback;
import io.agora.chat.callkit.livedatas.EaseCallLiveDataBus;
import io.agora.chat.callkit.utils.EaseCallAudioControl;
import io.agora.chat.callkit.utils.EaseCallKitUtils;
import io.agora.chat.callkit.utils.EaseCallMsgUtils;
import io.agora.chat.callkit.widget.EaseCallCommingCallView;
import io.agora.chat.callkit.widget.EaseCallMemberView;
import io.agora.chat.callkit.widget.EaseCallMemberViewGroup;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.UserInfo;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.util.EMLog;


public class EaseCallMultipleBaseActivity extends EaseCallBaseActivity implements View.OnClickListener {

    private static final String TAG = EaseCallMultipleBaseActivity.class.getSimpleName();
    private TimeHandler timeHandler;
    private TimeHandler timeUpdataTimer;
    private RtcEngine mRtcEngine;
    // Determine whether to initiate or to be invited
    protected boolean isInComingCall;
    protected String username;
    protected String channelName;
    private volatile boolean mConfirRing = false;
    private EaseCallType callType;
    private boolean isMuteState = false;
    private boolean isVideoMute = false;
    private boolean isCameraFront = true;
    private EaseCallMemberView localMemberView;
    private String agoraAppId = null;
    private boolean isAgreedInHeadDialog;
    private static final int PERMISSION_REQ_ID = 22;
    private final String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
    };
    List<String> mPermissionList = new ArrayList<>();
    private EaseCallActivityMultipleBinding mBinding;
    private String groupId;
    private boolean isPreview;
    private Bundle savedInstanceState;
    // Record The users invited time（用户定时map存储,记录超时）
    private Map<String, Long> invitedUsersTime = new HashMap<>();
    //Record views stored in surfaceViewGroup（记录surfaceViewGroup中存放的views）
    private final Map<Integer, EaseCallMemberView> inChannelViews = new HashMap<>();
    //in channel EaseUserAccounts(加入频道的EaseUserAccounts)
    private Map<Integer, EaseUserAccount> inChannelAccounts = new HashMap<>();
    //Record the corresponding AgoraChat userId and agora uid, which is used when users update local information(记录对应AgoraChat userId和声网uid,用户更新本地信息时用)
    private final Map<String, Integer> userIdAndUidMap = new HashMap<>();
    //Record placeholder views(记录占位views)
    private final Map<String, EaseCallMemberView> placeholders = new HashMap<>();
    //Record agora uids without speaking(记录没有说话的声网uids)
    private List<Integer> uidsNotSpeak = new ArrayList<>();
    //Invitation + has entered
    private Set<String> effectiveUsers = new HashSet<>();
    EaseCallKitListener listener = EaseCallKit.getInstance().getCallListener();

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onError(int err) {
            super.onError(err);
            EMLog.d(TAG, "IRtcEngineEventHandler onError:" + err);
            if (listener != null) {
                listener.onCallError(EaseCallError.RTC_ERROR, err, "rtc error");
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            EMLog.d(TAG, "onJoinChannelSuccess channel:" + channel + " uid" + uid);
            // Add channel start timer
            timeUpdataTimer.startTime(CALL_TIMER_CALL_TIME);
            if (!isInComingCall) {
                ArrayList<String> userList = EaseCallKit.getInstance().getInviteeUsers();
                if (userList != null && userList.size() > 0) {
                    handler.sendEmptyMessage(MSG_MAKE_CONFERENCE_VIDEO);
                    // The inviter becomes the caller
                    isInComingCall = false;
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBinding.chronometer.start();
                }
            });
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
        }

        @Override
        public void onClientRoleChanged(int oldRole, int newRole) {
            super.onClientRoleChanged(oldRole, newRole);
        }

        @Override
        public void onLocalUserRegistered(int uid, String userAccount) {
            super.onLocalUserRegistered(uid, userAccount);
        }

        @Override
        public void onUserInfoUpdated(int uid, UserInfo userInfo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EMLog.d(TAG, "onUserInfoUpdated onUserOffline: " + uid + ", account:" + userInfo.userAccount);
                    EaseUserAccount account = new EaseUserAccount(userInfo.uid, userInfo.userAccount);
                    inChannelAccounts.put(uid, account);
                    if (!userIdAndUidMap.containsValue(uid)) {
                        userIdAndUidMap.put(userInfo.userAccount, uid);
                    }
                    //Delete placeholders (删除占位符)
                    EaseCallMemberView placeView = placeholders.remove(userInfo.userAccount);
                    if (placeView != null) {
                        mBinding.surfaceViewGroup.removeView(placeView);
                    }
                    if (inChannelViews.containsKey(uid)) {
                        EaseCallMemberView memberView = inChannelViews.get(uid);
                        if (memberView != null) {
                            memberView.setUserInfo(account);
                        }
                    } else {
                        notifyUserToUpdateUserInfo(userInfo.userAccount,uid);
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        memberView.setUserInfo(account);
                        mBinding.surfaceViewGroup.addView(memberView);
                        inChannelViews.put(uid, memberView);
                    }
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            EaseCallAudioControl.getInstance().stopPlayRing();
            setUserJoinChannelInfo(null, uid);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EMLog.d(TAG, "onUserOffline: " + uid + ",reason:" + reason);

                    if (isFinishing()) {
                        return;
                    }
                    EaseUserAccount account = inChannelAccounts.get(uid);
                    if (account != null) {
                        effectiveUsers.remove(account.getUserName());
                    }
                    EaseCallMemberView memberView = inChannelViews.remove(uid);
                    if (memberView == null) {
                        return;
                    }
                    mBinding.surfaceViewGroup.removeView(memberView);
                    if (userIdAndUidMap.containsValue(uid) && account != null) {
                        userIdAndUidMap.remove(account.getUserName());
                    }

                    int tempUid = 0;
                    if (inChannelViews.size() > 0) { // If there are other members in the room, the first member is displayed（如果会议中有其他成员,则显示第一个成员）
                        Set<Integer> uidSet = inChannelViews.keySet();
                        for (int id : uidSet) {
                            tempUid = id;
                        }
                        updateFloatWindow(inChannelViews.get(tempUid));
                    }

                    if (inChannelAccounts != null) {
                        inChannelAccounts.remove(uid);
                    }
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) {
                        return;
                    }
                    if (inChannelViews.containsKey(uid)) {
                        EaseCallMemberView memberView = inChannelViews.get(uid);
                        if (inChannelAccounts.containsKey(uid)) {
                            memberView.setUserInfo(inChannelAccounts.get(uid));
                            if (!userIdAndUidMap.containsValue(uid)) {
                                userIdAndUidMap.put(inChannelAccounts.get(uid).getUserName(), uid);
                            }
                        }
                        if (memberView != null) {
                            //Delete placeholders (删除占位符)
                            EaseCallMemberView placeView = placeholders.remove(memberView.getUserAccount());
                            if (placeView != null) {
                                mBinding.surfaceViewGroup.removeView(placeView);
                            }

                            if (memberView.getSurfaceView() == null) {
                                SurfaceView surfaceView =
                                        RtcEngine.CreateRendererView(getApplicationContext());
                                memberView.addSurfaceView(surfaceView);
                                surfaceView.setZOrderOnTop(false);
                                memberView.showVideo(false);
                                surfaceView.setZOrderMediaOverlay(false);
                                mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                            } else {
                                memberView.showVideo(false);
                            }
                        }
                    } else {
                        EaseCallMemberView memberView = createCallMemberView();
                        if (inChannelAccounts.containsKey(uid)) {
                            memberView.setUserInfo(inChannelAccounts.get(uid));
                        }

                        //Delete placeholders (删除占位符)
                        EaseCallMemberView placeView = placeholders.remove(memberView.getUserAccount());
                        if (placeView != null) {
                            mBinding.surfaceViewGroup.removeView(placeView);
                        }

                        mBinding.surfaceViewGroup.addView(memberView);

                        memberView.showVideo(false);
                        inChannelViews.put(uid, memberView);
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uid));

                        if(inChannelAccounts.containsKey(uid)) {
                            EaseUserAccount account = inChannelAccounts.get(uid);
                            if (account != null) {
                                notifyUserToUpdateUserInfo(account.getUserName(),uid);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EaseCallMemberView memberView = inChannelViews.get(uid);
                    if (memberView != null) {
                        if (state == REMOTE_VIDEO_STATE_STOPPED || state == REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED) {
                            memberView.showVideo(true);
                        } else if (state == REMOTE_VIDEO_STATE_DECODING || state == REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED) {
                            memberView.showVideo(false);
                        }

                        if (state == REMOTE_VIDEO_STATE_STOPPED || state == REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED || state == REMOTE_VIDEO_STATE_DECODING || state == REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED) {
                            // Determine the video is the current hover window update hover window（判断视频是当前悬浮窗 更新悬浮窗）
                            EaseCallMemberView floatView = EaseCallFloatWindow.getInstance().getCallMemberView();
                            if (floatView != null && floatView.getUserId() == uid) {
                                updateFloatWindow(inChannelViews.get(uid));
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (state == REMOTE_AUDIO_STATE_STARTING) {
                        //first frame
                        EMLog.d(TAG, "onRemoteAudioStateChanged:" +uid + ",elapsed:" + elapsed);
                        if (isFinishing()) {
                            return;
                        }
                        if (inChannelViews.containsKey(uid)) {
                            EaseCallMemberView memberView = inChannelViews.get(uid);
                            if (memberView != null) {
                                memberView.setAudioOff(false);
                            }
                            if (inChannelAccounts.containsKey(uid)) {
                                memberView.setUserInfo(inChannelAccounts.get(uid));
                            }
                            if (!userIdAndUidMap.containsValue(uid)) {
                                if (inChannelAccounts.get(uid) != null && inChannelAccounts.get(uid).getUserName() != null) {
                                    userIdAndUidMap.put(inChannelAccounts.get(uid).getUserName(), uid);
                                }
                            }
                        } else {
                            final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                            if (inChannelAccounts.containsKey(uid)) {
                                memberView.setUserInfo(inChannelAccounts.get(uid));
                            }

                            //Delete placeholders (删除占位符)
                            EaseCallMemberView placeView = placeholders.remove(memberView.getUserAccount());
                            if (placeView != null) {
                                mBinding.surfaceViewGroup.removeView(placeView);
                            }

                            memberView.setAudioOff(false);
                            mBinding.surfaceViewGroup.addView(memberView);
                            inChannelViews.put(uid, memberView);

                            //notify user to update userinfo (通知更新)
                            if(inChannelAccounts.containsKey(uid)) {
                                EaseUserAccount account = inChannelAccounts.get(uid);
                                if (account != null) {
                                    notifyUserToUpdateUserInfo(account.getUserName(),uid);
                                }
                            }
                        }
                    } else {
                        EaseCallMemberView memberView = inChannelViews.get(uid);
                        if (memberView != null) {
                            if (state == REMOTE_AUDIO_REASON_REMOTE_MUTED || state == REMOTE_AUDIO_STATE_STOPPED) {
                                memberView.setAudioOff(true);
                            } else if (state == REMOTE_AUDIO_STATE_DECODING || state == REMOTE_AUDIO_REASON_REMOTE_UNMUTED) {
                                memberView.setAudioOff(false);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (speakers != null && speakers.length > 0) {
                        uidsNotSpeak.clear();
                        uidsNotSpeak.addAll(inChannelViews.keySet());
                        for (AudioVolumeInfo info : speakers) {
                            Integer uId = info.uid;
                            int volume = info.volume;
                            EMLog.d(TAG, "onAudioVolumeIndication:" +uId + ",volume: " + volume);
                            if (uidsNotSpeak.contains(uId)) {
                                EaseCallMemberView memberView = inChannelViews.get(uId);
                                if (memberView != null && !memberView.getAudioOff()) {
                                    memberView.setSpeak(true, volume);
                                    uidsNotSpeak.remove(uId);
                                }
                            }
                        }
                        if (uidsNotSpeak.size() > 0) {
                            for (int uid : uidsNotSpeak) {
                                EaseCallMemberView memberView = inChannelViews.get(uid);
                                if (memberView != null && !memberView.getAudioOff()) {
                                    memberView.setSpeak(false, 0);
                                }
                            }
                        }
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = EaseCallActivityMultipleBinding.inflate(getLayoutInflater());
        this.savedInstanceState = savedInstanceState;
        setContentView(mBinding.layoutRoot);

        if (Build.VERSION.SDK_INT >= 23) {
            initPermission();
        } else {
            init();
        }
    }

    private void init() {
        if (savedInstanceState == null) {
            initParams(getIntent().getExtras());
        } else {
            initParams(savedInstanceState);
        }
        initEngine();
        //init View
        initView();
        addLiveDataObserver();
        timeHandler = new TimeHandler();
        timeUpdataTimer = new TimeHandler();
        checkConference(true);
        EaseCallKit.getInstance().getNotifier().reset();
    }

    private void initPermission() {
        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQ_ID);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;// Permission failed(有权限没有通过)
        if (PERMISSION_REQ_ID == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                }
            }
            if (hasPermissionDismiss) {
                exitChannel();
            } else {
                init();
            }
        }
    }


    public void initView() {
        mBinding.incomingCallView.setOnActionListener(onActionListener);
        mBinding.surfaceViewGroup.setOnItemClickListener(onItemClickListener);
        mBinding.surfaceViewGroup.setOnScreenModeChangeListener(onScreenModeChangeListener);
        mBinding.btnInvite.setOnClickListener(this);
        mBinding.btnMicSwitchVoice.setOnClickListener(this);
        mBinding.btnSpeakerSwitchVoice.setOnClickListener(this);
        mBinding.btnVidicon.setOnClickListener(this);
        mBinding.btnChangeCameraSwitch.setOnClickListener(this);
        mBinding.btnHangupVoice.setOnClickListener(this);
        mBinding.btnFloat.setOnClickListener(this);

        mBinding.btnHangupVideo.setOnClickListener(this);
        mBinding.btnVidicon.setOnClickListener(this);
        mBinding.btnMicSwitchVideo.setOnClickListener(this);

        mBinding.btnMicSwitchVoice.setActivated(false);
        mBinding.btnVidicon.setActivated(true);
        mBinding.btnSpeakerSwitchVoice.setActivated(true);
        EaseCallAudioControl.getInstance().openSpeakerOn();
        mBinding.surfaceViewGroup.setCallType(callType);

        //If you are invited, an invitation window is displayed(被邀请的话弹出邀请界面)
        if (isInComingCall) {
            //Click the agree button in the box outside to enter(点击外面弹框中的同意按钮进来的)
            if (isAgreedInHeadDialog) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Agree to answer directly, because click on the outside agree(直接同意接听，因为在外边点击同意了)
                        addLocalViewToConferenceViewsGroup();
                        sendAgreeMessage();
                    }
                });
            } else {
                EaseCallAudioControl.getInstance().playRing();
                mBinding.incomingCallView.setInviteInfo(username, groupId, callType);
                mBinding.incomingCallView.setVisibility(View.VISIBLE);
                // Update the nickname avatar(更新昵称头像)
                notifyUserToUpdateUserInfo(ChatClient.getInstance().getCurrentUser(),0);
            }
        } else {
            mBinding.incomingCallView.setVisibility(View.GONE);
            ArrayList<String> inviteeUsers = EaseCallKit.getInstance().getInviteeUsers();
            if (inviteeUsers != null && inviteeUsers.size() > 0) {
                EaseCallAudioControl.getInstance().playRing();
            }
            //Calling party joins channel(主叫加入频道)
            joinChannel();
        }
        if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
            mBinding.rlVideoControl.setVisibility(View.VISIBLE);
            mBinding.btnChangeCameraSwitch.setVisibility(View.VISIBLE);
        } else {
            mBinding.btnChangeCameraSwitch.setVisibility(View.GONE);
            mBinding.rlVoiceControl.setVisibility(View.VISIBLE);
        }
    }

    private void sendAgreeMessage() {
        //Sending an Answer Message(发送接听消息)
        EaseCallAnswerEvent event = new EaseCallAnswerEvent();
        event.result = EaseCallMsgUtils.CALL_ANSWER_ACCEPT;
        event.callId = EaseCallKit.getInstance().getCallID();
        event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
        event.calleeDevId = EaseCallKit.deviceId;
        sendCmdMsg(event, username);
    }

    private void initParams(Bundle bundle) {
        if (bundle != null) {
            isInComingCall = bundle.getBoolean("isComingCall", false);
            username = bundle.getString("username");
            channelName = bundle.getString("channelName");
            isAgreedInHeadDialog = bundle.getBoolean("isAgreedInHeadDialog");
        } else {
            isInComingCall = EaseCallKit.getInstance().getIsComingCall();
            username = EaseCallKit.getInstance().getFromUserId();
            channelName = EaseCallKit.getInstance().getChannelName();
        }
        callType = EaseCallKit.getInstance().getCallType();
        try {
            JSONObject inviteExt = EaseCallKit.getInstance().getInviteExt();
            groupId = inviteExt.getString("groupId");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initEngine() {
        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
    }

    private void initializeEngine() {
        try {
            EaseCallKitConfig config = EaseCallKit.getInstance().getCallKitConfig();
            if (config != null) {
                agoraAppId = config.getAgoraAppId();
            }
            mRtcEngine = RtcEngine.create(getBaseContext(), agoraAppId, mRtcEventHandler);

            //Because there is a applet set to live mode, the role is set to master(因为有小程序 设置为直播模式 角色设置为主播)
            mRtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setClientRole(CLIENT_ROLE_BROADCASTER);

            EaseCallFloatWindow.getInstance().setRtcEngine(getApplicationContext(), mRtcEngine);
            // Set the small window hover type(设置小窗口悬浮类型)
            EaseCallFloatWindow.getInstance().setCallType(callType);
        } catch (Exception e) {
            EMLog.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        mRtcEngine.enableVideo();
        if (callType == EaseCallType.CONFERENCE_VOICE_CALL) {
            mRtcEngine.muteLocalVideoStream(true);
        }
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));

        //Enable detection of who is talking(启动谁在说话检测)
        mRtcEngine.enableAudioVolumeIndication(500, 3, false);
    }

    /**
     * If float window is showing, use the old view
     */
    private void setupLocalVideo() {
        if (isFloatWindowShowing()) {
            return;
        }
        EaseUserAccount account = new EaseUserAccount(0, ChatClient.getInstance().getCurrentUser());
        localMemberView = createCallMemberView();
        localMemberView.setUserInfo(account);
        localMemberView.setNameVisiable(View.GONE);
        localMemberView.setVidiconVisiable(View.GONE);
        localMemberView.setCameraDirectionFront(isCameraFront);
        mRtcEngine.setupLocalVideo(new VideoCanvas(localMemberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, 0));
        if (isInComingCall) {
            if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
                mBinding.incomingCallView.setVideoView(localMemberView);
                mRtcEngine.startPreview();
                isPreview = true;
            }
        } else {
            addLocalViewToConferenceViewsGroup();
        }

    }

    public EaseCallMemberView createCallMemberView() {
        EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
        surfaceView.setZOrderOnTop(false);
        surfaceView.setZOrderMediaOverlay(false);
        memberView.addSurfaceView(surfaceView);
        if (callType == EaseCallType.CONFERENCE_VOICE_CALL) {
            memberView.setVoiceOnlineImageState(false);
            memberView.showVideo(true);
        } else {
            memberView.showVideo(false);
            memberView.setVoiceOnlineImageState(true);
        }
        return memberView;
    }


    private void joinChannel() {
        EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
        if (listener != null && callKitConfig != null && callKitConfig.isEnableRTCToken()) {
            listener.onGenerateRTCToken(ChatClient.getInstance().getCurrentUser(), channelName, new EaseCallKitTokenCallback() {
                @Override
                public void onSetToken(String token, int uId) {
                    //gets agora RTC token ,then join channel (获取到RTC Token uid加入频道)
                    mRtcEngine.joinChannel(token, channelName, null, uId);
                    //add uid to inChannelAccounts
                    inChannelAccounts.put(uId, new EaseUserAccount(uId, ChatClient.getInstance().getCurrentUser()));
                }

                @Override
                public void onGetTokenError(int error, String errorMsg) {
                    EMLog.e(TAG, "onGenerateToken error :" + ChatClient.getInstance().getAccessToken());
                    // Failed to obtain the RTC Token(获取RTC Token失败,退出呼叫)
                    exitChannel();
                }
            });
        } else {
            //Don't checkout token(不校验token)
            mRtcEngine.joinChannel(null, channelName, null, 0);
            //add uid to inChannelAccounts
            inChannelAccounts.put(0, new EaseUserAccount(0, ChatClient.getInstance().getCurrentUser()));

        }
    }

    /**
     * Change whether mute
     *
     * @param isMute
     */
    private void changeMuteState(boolean isMute) {
        localMemberView.setAudioOff(isMute);
        mRtcEngine.muteLocalAudioStream(isMute);
        isMuteState = isMute;
        mBinding.btnMicSwitchVoice.setBackground(isMute ? getResources().getDrawable(R.drawable.call_mute_on) : getResources().getDrawable(R.drawable.call_mute_normal));
        mBinding.btnMicSwitchVideo.setBackground(isMute ? getResources().getDrawable(R.drawable.call_mute_on) : getResources().getDrawable(R.drawable.call_mute_normal));
    }

    private void changeSpeakerState(boolean isActive) {
        localMemberView.setSpeakActivated(isActive);
        mBinding.btnSpeakerSwitchVoice.setActivated(isActive);
        mBinding.btnSpeakerSwitchVoice.setBackground(isActive ? getResources().getDrawable(R.drawable.ease_call_voice_on) : getResources().getDrawable(R.drawable.ease_call_voice_off));
        if (isActive) {
            EaseCallAudioControl.getInstance().openSpeakerOn();
        } else {
            EaseCallAudioControl.getInstance().closeSpeakerOn();
        }
    }

    private void changeVideoState(boolean videoOff) {
        localMemberView.showVideo(videoOff);
        mRtcEngine.muteLocalVideoStream(videoOff);
        isVideoMute = videoOff;
        mBinding.btnVidicon.setBackground(videoOff ? getResources().getDrawable(R.drawable.call_video_off) : getResources().getDrawable(R.drawable.call_video_on));
    }

    private void changeCameraDirect(boolean isFront) {
        if (this.isCameraFront != isFront) {
            if (mRtcEngine != null) {
                mRtcEngine.switchCamera();
            }
            this.isCameraFront = isFront;
            localMemberView.setCameraDirectionFront(isFront);
        }
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btn_mic_switch_voice || viewId == R.id.btn_mic_switch_video) {
            changeMuteState(!isMuteState);
        } else if (viewId == R.id.btn_speaker_switch_voice) {
            changeSpeakerState(!mBinding.btnSpeakerSwitchVoice.isActivated());
        } else if (viewId == R.id.btn_vidicon) {
            changeVideoState(!isVideoMute);
        } else if (viewId == R.id.btn_change_camera_switch) {
            changeCameraDirect(!isCameraFront);
        } else if (viewId == R.id.btn_hangup_voice || viewId == R.id.btn_hangup_video) {
            if (listener != null) {
                listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonHangup, timeUpdataTimer.timePassed * 1000);
            }
            exitChannel();
        } else if (viewId == R.id.btn_float) {
            showFloatWindow();
        } else if (viewId == R.id.btn_invite) {
            if (listener != null) {
                int size = effectiveUsers.size();
                JSONObject object = EaseCallKit.getInstance().getInviteExt();
                if (size >= EaseCallKit.getInstance().getLargestNumInChannel() - 1) {
                    listener.onCallError(PROCESS_ERROR, CALL_PARAM_ERROR.code, getString(R.string.ease_call_max_people_in_channel));
                } else if (size > 0) {
                    String users[] = new String[size];
                    int i = 0;
                    for (String user : effectiveUsers) {
                        users[i++] = user;
                    }
                    listener.onInviteUsers(callType, users, object);
                } else {
                    listener.onInviteUsers(callType, null, object);
                }
            }
        }
    }


    /**
     * add livedate listener (增加LiveData监听)
     */
    protected void addLiveDataObserver() {
        EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString(), EaseCallBaseEvent.class).observe(this, event -> {
            if (event != null) {
                switch (event.callAction) {
                    case CALL_ALERT:
                        EaseCallAlertEvent alertEvent = (EaseCallAlertEvent) event;
                        //Determine whether the session is valid(判断会话是否有效)
                        EaseCallConfirmRingEvent ringEvent = new EaseCallConfirmRingEvent();
                        String user = alertEvent.userId;
                        if (TextUtils.equals(alertEvent.callId, EaseCallKit.getInstance().getCallID())
                                && invitedUsersTime.containsKey(user)) {
                            //Send a valid session message(发送会话有效消息)
                            ringEvent.calleeDevId = alertEvent.calleeDevId;
                            ringEvent.valid = true;
                            ringEvent.userId = alertEvent.userId;
                            sendCmdMsg(ringEvent, alertEvent.userId);
                        } else {
                            //Invalid session message was sent(发送会话无效消息)
                            ringEvent.calleeDevId = alertEvent.calleeDevId;
                            ringEvent.valid = false;
                            sendCmdMsg(ringEvent, alertEvent.userId);
                        }
                        //A session confirmation message has been sent.(已经发送过会话确认消息)
                        mConfirRing = true;
                        break;
                    case CALL_CANCEL:
                        if (userIdAndUidMap.get(event.userId) == null && !TextUtils.equals(event.userId, ChatClient.getInstance().getCurrentUser())) {
                            //An event sent by a strange third party(陌生的第三者发的event)
                            break;
                        }
                        if (!isInComingCall) {
                            //Stop quorum timer(停止仲裁定时器)
                            timeHandler.stopTime();
                        }
                        //cancel call (取消通话)
                        exitChannel();
                        break;
                    case CALL_ANSWER:
                        EaseCallAnswerEvent answerEvent = (EaseCallAnswerEvent) event;
                        EaseCallConfirmCallEvent callEvent = new EaseCallConfirmCallEvent();
                        callEvent.calleeDevId = answerEvent.calleeDevId;
                        callEvent.result = answerEvent.result;
                        //remove form time recorder (删除超时机制)
                        String userId = answerEvent.userId;
                        invitedUsersTime.remove(userId);

                        if (TextUtils.equals(answerEvent.result, EaseCallMsgUtils.CALL_ANSWER_BUSY)) {
                            if (!mConfirRing) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //The other party is busy(对方正在忙碌中)
                                        effectiveUsers.remove(userId);
                                        //Delete placeholders (删除占位符)
                                        EaseCallMemberView placeView = placeholders.remove(userId);
                                        if (placeView != null) {
                                            mBinding.surfaceViewGroup.removeView(placeView);
                                        }
                                        if (listener != null) {
                                            listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonBusy, timeUpdataTimer.timePassed * 1000);
                                        }
                                        // check placeholders state (检查placeholders状态)
                                        if (placeholders.size() == 0) {
                                            EaseCallAudioControl.getInstance().stopPlayRing();
                                        }
                                    }
                                });
                            } else {
                                sendCmdMsg(callEvent, username);
                            }
                        } else if (TextUtils.equals(answerEvent.result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                            //set call answered state (设置为接听)
                            EaseCallKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                            sendCmdMsg(callEvent, answerEvent.userId);
                        } else if (TextUtils.equals(answerEvent.result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                            sendCmdMsg(callEvent, answerEvent.userId);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    effectiveUsers.remove(userId);
                                    //Delete placeholders (删除占位符)
                                    EaseCallMemberView placeView = placeholders.remove(userId);
                                    if (placeView != null) {
                                        mBinding.surfaceViewGroup.removeView(placeView);
                                    }
                                }
                            });
                            if (listener != null) {
                                listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRefuse, 0);
                            }
                            //  check placeholders state (检查placeholders状态)
                            if (placeholders.size() == 0) {
                                EaseCallAudioControl.getInstance().stopPlayRing();
                            }
                        }
                        break;
                    case CALL_CONFIRM_RING:
                        break;
                    case CALL_CONFIRM_CALLEE:
                        EaseCallConfirmCallEvent confirmEvent = (EaseCallConfirmCallEvent) event;
                        String deviceId = confirmEvent.calleeDevId;
                        String result = confirmEvent.result;
                        timeHandler.stopTime();
                        //is self (收到的仲裁为自己设备)
                        if (TextUtils.equals(deviceId, EaseCallKit.deviceId)) {
                            //answer accept(收到的仲裁为接听)
                            if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                                joinChannel();
                            } else if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                                //exit call
                                exitChannel();
                            }
                        } else {
                            //exit call(退出通话)
                            exitChannel();
                        }
                        break;
                }
            }
        });

        EaseCallLiveDataBus.get().with(EaseCallKitUtils.UPDATE_USERINFO, EaseCallUserInfo.class).observe(this, userInfo -> {
            if (userInfo != null) {
                //Update local avatar nicknames
                EaseCallKit.getInstance().getCallKitConfig().setUserInfo(userInfo.getUserId(), userInfo);
                if (userInfo.getUserId() != null) {
                    if (userIdAndUidMap.containsKey(userInfo.getUserId())) {
                        int uid = userIdAndUidMap.get(userInfo.getUserId());
                        updateUserInfo(uid);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (TextUtils.equals(username, userInfo.getUserId()) && mBinding.incomingCallView != null) {
                                    mBinding.incomingCallView.setInviteInfo(username, groupId, callType);
                                }
                            }
                        });
                    }
                }
            }
        });
    }


    private EaseCallCommingCallView.OnActionListener onActionListener = new EaseCallCommingCallView.OnActionListener() {
        @Override
        public void onPickupClick(View v) {
            //stop ring(停止震铃)
            EaseCallAudioControl.getInstance().stopPlayRing();
            addLocalViewToConferenceViewsGroup();
            mBinding.incomingCallView.setVisibility(View.GONE);
            if (isInComingCall) {
                sendAgreeMessage();
            }
        }

        @Override
        public void onMuteVideoClick(View v) {
            //stop preview(禁止图像)
            if (isPreview) {
                mRtcEngine.stopPreview();
                localMemberView.setVisibility(View.GONE);
                isPreview = false;
            } else {
                mRtcEngine.startPreview();
                localMemberView.setVisibility(View.VISIBLE);
                isPreview = true;
            }
            ((ImageView) v).setImageResource(isPreview ? R.drawable.call_video_off : R.drawable.call_video_on);
        }

        @Override
        public void onSwitchCamerClick(View v) {
            if (isCameraFront) {
                changeCameraDirect(false);
            } else {
                changeCameraDirect(true);
            }
        }

        @Override
        public void onRejectClick(View v) {
            //stop ring(停止震铃)
            if (isInComingCall) {
                EaseCallAudioControl.getInstance().stopPlayRing();
                //send refused message(发送拒绝消息)
                EaseCallAnswerEvent event = new EaseCallAnswerEvent();
                event.result = EaseCallMsgUtils.CALL_ANSWER_REFUSE;
                event.callId = EaseCallKit.getInstance().getCallID();
                event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallKit.deviceId;
                sendCmdMsg(event, username);
            }
        }
    };

    private void addLocalViewToConferenceViewsGroup() {
        mBinding.incomingCallView.removeVideoView(localMemberView);
        mBinding.surfaceViewGroup.addView(localMemberView);
        notifyUserToUpdateUserInfo(ChatClient.getInstance().getCurrentUser(),0);
        inChannelViews.put(0, localMemberView);
    }


    private EaseCallMemberViewGroup.OnScreenModeChangeListener onScreenModeChangeListener = new EaseCallMemberViewGroup.OnScreenModeChangeListener() {
        @Override
        public void onScreenModeChange(boolean isFullScreenMode, @Nullable View fullScreenView) {
            if (isFullScreenMode) { // (fullScreen)全屏模式
                mBinding.rlVideoControl.setVisibility(View.GONE);
            } else { // (not fullscreen)非全屏模式
                mBinding.rlVideoControl.setVisibility(View.VISIBLE);
            }
        }
    };

    private EaseCallMemberViewGroup.OnItemClickListener onItemClickListener = new EaseCallMemberViewGroup.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
        }
    };


    private void leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
    }

    //update time (更新时间)
    private void updateConferenceTime(String time) {
        Log.e(TAG, "time: " + time);
        mBinding.tvCallTime.setText(time);
    }

    private class TimeHandler extends Handler {
        private DateFormat dateFormat = null;
        private int timePassed = 0;
        private String passedTime;

        public TimeHandler() {
            dateFormat = new SimpleDateFormat("mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void startTime(int timeType) {
            Log.e(TAG, "start timer");
            timePassed = 0;
            removeMessages(timeType);
            sendEmptyMessageDelayed(timeType, 1000);
        }

        public String getPassedTime() {
            passedTime = dateFormat.format(timePassed * 1000);
            return passedTime;
        }

        public void stopTime() {
            Log.e(TAG, "stopTime");
            removeMessages(CALL_TIMER_CALL_TIME);
            removeMessages(EaseCallMsgUtils.CALL_TIMER_TIMEOUT);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CALL_TIMER_TIMEOUT) {
                timePassed++;
                if (!isInComingCall) { //如果是主叫
                    long totalMilliSeconds = System.currentTimeMillis();
                    Iterator<String> itUser = invitedUsersTime.keySet().iterator();
                    while (itUser.hasNext()) {
                        String userName = itUser.next();
                        //Check whether the current time times out(判断当前时间是否超时)
                        if (totalMilliSeconds >= invitedUsersTime.get(userName)) {
                            //send cancel event (发送取消事件)
                            EaseCallCallCancelEvent cancelEvent = new EaseCallCallCancelEvent();
                            cancelEvent.callId = EaseCallKit.getInstance().getCallID();
                            sendCmdMsg(cancelEvent, userName);
                            itUser.remove();
                            effectiveUsers.remove(userName);
                            EaseCallMemberView memberView = placeholders.remove(userName);
                            if (memberView != null) {
                                mBinding.surfaceViewGroup.removeView(memberView);
                            }
                        }
                    }
                    if (invitedUsersTime.size() == 0) {
                        timeHandler.stopTime();
                    } else {
                        sendEmptyMessageDelayed(CALL_TIMER_TIMEOUT, 1000);
                    }
                } else {
                    long intervalTime;
                    EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
                    if (callKitConfig != null) {
                        intervalTime = callKitConfig.getCallTimeOut()*1000;
                    } else {
                        intervalTime = EaseCallMsgUtils.CALL_INVITE_INTERVAL;
                    }
                    sendEmptyMessageDelayed(CALL_TIMER_TIMEOUT, 1000);
                    if (timePassed * 1000 == intervalTime) {
                        timeHandler.stopTime();
                        //The called party waiting for the arbitration message timed out(被叫等待仲裁消息超时)
                        exitChannel();
                        if (listener != null) {
                            //Reply timed out(对方回复超时)
                            listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse, 0);
                        }
                    }
                }


            } else if (msg.what == CALL_TIMER_CALL_TIME) {
                timePassed++;
                updateTime(this);
            }
            super.handleMessage(msg);
        }
    }

    private void updateTime(TimeHandler handler) {
        String time = handler.dateFormat.format(handler.timePassed * 1000);
        updateConferenceTime(time);
        handler.removeMessages(CALL_TIMER_CALL_TIME);
        handler.sendEmptyMessageDelayed(CALL_TIMER_CALL_TIME, 1000);
    }

    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_MAKE_SIGNAL_VOICE: // 1V1 voice call (语音通话)
                    break;
                case MSG_MAKE_SIGNAL_VIDEO: // 1V1 video call (视频通话)
                    break;
                case MSG_MAKE_CONFERENCE_VIDEO: // multiply video or audio call (多人音视频通话)
                    ArrayList<String> sendInviteeMsg = EaseCallKit.getInstance().getInviteeUsers();
                    sendInviteeMsg(sendInviteeMsg);
                    break;
                case MSG_RELEASE_HANDLER: // remote loop message(停止事件循环)
                    //Preventing memory leaks(防止内存泄漏)
                    handler.removeMessages(MSG_MAKE_SIGNAL_VOICE);
                    handler.removeMessages(MSG_MAKE_SIGNAL_VIDEO);
                    handler.removeMessages(MSG_MAKE_CONFERENCE_VIDEO);
                    break;
                default:
                    break;
            }
        }
    };


    /**
     * Send a call invitation
     * @param userArray
     */
    private void sendInviteeMsg(ArrayList<String> userArray) {
        //start invite timer (开始定时器)
        isInComingCall = false;
        timeHandler.startTime(CALL_TIMER_TIMEOUT);
        for (String username : userArray) {
            if (!placeholders.containsKey(username) && !userIdAndUidMap.containsKey(username)) {

                //update user nickname and image (更新头像昵称)
                notifyUserToUpdateUserInfo(username,0);

                long totalMilliSeconds = System.currentTimeMillis();
                long intervalTime;
                EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
                if (callKitConfig != null) {
                    intervalTime = callKitConfig.getCallTimeOut()*1000;
                } else {
                    intervalTime = EaseCallMsgUtils.CALL_INVITE_INTERVAL;
                }
                totalMilliSeconds += intervalTime;

                //put total time in invitedUsersTime (放入超时时间)
                invitedUsersTime.put(username, totalMilliSeconds);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //show placeholder (显示占位符)
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        memberView.setUserInfo(new EaseUserAccount(0, username));
//                        memberView.setLoading(true);
                        memberView.showVideo(true);
                        mBinding.surfaceViewGroup.addView(memberView);
                        placeholders.put(username, memberView);
                    }
                });

                final ChatMessage message ;
                if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
                    message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invite_you_for_video_call), username);
                } else {
                    message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invite_you_for_audio_call), username);
                }
                setInviteeMessageAttr(message);

                ChatClient.getInstance().chatManager().sendMessage(message);
            }
        }

        //send a message to a group (给群里发送一条消息)
        final ChatMessage message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invited_to_make_multi_party_call), groupId);
        message.setChatType(ChatMessage.ChatType.GroupChat);
        setInviteeMessageAttr(message);
        ChatClient.getInstance().chatManager().sendMessage(message);
        //init inviteeUsers(初始化邀请列表)
        EaseCallKit.getInstance().InitInviteeUsers();
    }

    private void setInviteeMessageAttr(ChatMessage message) {
        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, EaseCallAction.CALL_INVITE.state);
        message.setAttribute(EaseCallMsgUtils.CALL_CHANNELNAME, channelName);
        message.setAttribute(EaseCallMsgUtils.CALL_TYPE, callType.code);
        message.setAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, EaseCallKit.deviceId);
        JSONObject object = EaseCallKit.getInstance().getInviteExt();
        if (object != null) {
            message.setAttribute(CALL_INVITE_EXT, object);
        } else {
            try {
                JSONObject obj = new JSONObject();
                message.setAttribute(CALL_INVITE_EXT, obj);
            } catch (Exception e) {
                e.getStackTrace();
            }
        }
        if (EaseCallKit.getInstance().getCallID() == null) {
            EaseCallKit.getInstance().setCallID(EaseCallKitUtils.getRandomString(10));
        }
        message.setAttribute(EaseCallMsgUtils.CLL_ID, EaseCallKit.getInstance().getCallID());
        message.setAttribute(EaseCallMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
        message.setAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, EaseCallMsgUtils.CALL_MSG_INFO);

        //add push ext (增加推送字段)
        JSONObject extObject = new JSONObject();
        try {
            String info = getApplication().getString(R.string.ease_call_alert_request_multiple_video, ChatClient.getInstance().getCurrentUser());
            extObject.putOpt("em_push_title", info);
            extObject.putOpt("em_push_content", info);
            extObject.putOpt("isRtcCall", true);
            extObject.putOpt("callType", EaseCallType.CONFERENCE_VIDEO_CALL.code);

            if(message.getChatType()== ChatMessage.ChatType.Chat) {
                extObject.putOpt("em_push_type", "voip");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        message.setAttribute("em_apns_ext", extObject);

        if(message.getChatType()== ChatMessage.ChatType.Chat) {
            try {
                JSONObject pushExtObject = new JSONObject();
                pushExtObject.putOpt("type","call");

                JSONObject customObject = new JSONObject();
                customObject.putOpt("callId",EaseCallKit.getInstance().getCallID());
                pushExtObject.putOpt("custom",customObject);

                message.setAttribute("em_push_ext",pushExtObject);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        final Conversation conversation = ChatClient.getInstance().chatManager().getConversation(username, Conversation.ConversationType.Chat, true);
        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success username:" + username);
                if (listener != null) {
                    listener.onInViteCallMessageSent();
                }
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error + " username:" + username);

                if (listener != null) {
                    listener.onCallError(EaseCallError.IM_ERROR, code, error);
                    listener.onInViteCallMessageSent();
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
    }

    /**
     * send cmd message (发送CMD回复信息)
     *
     * @param username
     */
    private void sendCmdMsg(EaseCallBaseEvent event, String username) {
        final ChatMessage message = ChatMessage.createSendMessage(ChatMessage.Type.CMD);

        String action = "rtcCall";
        CmdMessageBody cmdBody = new CmdMessageBody(action);
        message.setTo(username);
        message.addBody(cmdBody);
        if (event.callAction.equals(EaseCallAction.CALL_CANCEL)) {
            cmdBody.deliverOnlineOnly(false);
        } else {
            cmdBody.deliverOnlineOnly(true);
        }

        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, event.callAction.state);
        message.setAttribute(EaseCallMsgUtils.CALL_DEVICE_ID, EaseCallKit.deviceId);
        message.setAttribute(EaseCallMsgUtils.CLL_ID, EaseCallKit.getInstance().getCallID());
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
        }
        final Conversation conversation = ChatClient.getInstance().chatManager().getConversation(username, Conversation.ConversationType.Chat, true);
        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
                if (event.callAction == EaseCallAction.CALL_CANCEL) {
                    //exitChannel();
                } else if (event.callAction == EaseCallAction.CALL_ANSWER) {
                    //After the reply, start the timer and wait for the arbitration timeout(回复以后启动定时器，等待仲裁超时)
                    timeHandler.startTime(CALL_TIMER_TIMEOUT);
                }
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
                conversation.removeMessage(message.getMsgId());
                if (listener != null) {
                    listener.onCallError(EaseCallError.IM_ERROR, code, error);
                }
                if (event.callAction == EaseCallAction.CALL_CANCEL) {
                    exitChannel();
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        ChatClient.getInstance().chatManager().sendMessage(message);
    }

    /**
     * Set the user information callback
     *
     * @param userName
     * @param uid
     */
    private void setUserJoinChannelInfo(String userName, int uid) {
        if (TextUtils.isEmpty(userName)) {
            if (listener != null) {
                listener.onRemoteUserJoinChannel(channelName, userName, uid, new EaseCallGetUserAccountCallback() {
                    @Override
                    public void onUserAccount(EaseUserAccount account) {
                        processOnUserAccount(account);
                    }

                    @Override
                    public void onSetUserAccountError(int error, String errorMsg) {
                        EMLog.e(TAG, "onRemoteUserJoinChannel error:" + error + "  errorMsg:" + errorMsg);
                    }
                });
            }
        } else {
            EaseUserAccount account = new EaseUserAccount(uid, userName);
            processOnUserAccount(account);
        }
    }

    private void processOnUserAccount(EaseUserAccount account) {
        if (account != null) {
            if (account.getUid() != 0) {
                inChannelAccounts.put(account.getUid(), account);
                effectiveUsers.add(account.getUserName());
            }
            notifyUserToUpdateUserInfo(account.getUserName(),account.getUid());
        }
    }

    private void updateView(EaseUserAccount account) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.equals(account.getUserName(), ChatClient.getInstance().getCurrentUser())) {
                    updateUserInfo(account.getUid());
                } else {
                    localMemberView.updateUserInfo();
                }
                //Delete placeholders (删除占位符)
                EaseCallMemberView placeView = placeholders.remove(account.getUserName());
                if (placeView != null) {
                    mBinding.surfaceViewGroup.removeView(placeView);
                }
                //update user nickname and image(通知更新昵称头像)
                if (TextUtils.equals(account.getUserName(), username)) {
                    if (mBinding.incomingCallView != null) {
                        mBinding.incomingCallView.setInviteInfo(account.getUserName(), groupId, callType);
                    }
                }
            }
        });
    }

    private void notifyUserToUpdateUserInfo(String username,int uid) {
        if (listener != null && !TextUtils.isEmpty(username)) {
            listener.onUserInfoUpdate(username);
        }
        if(inChannelAccounts.containsKey(uid)) {
            updateView(inChannelAccounts.get(uid));
        }
    }

    private void resetVideoView() {
        if (inChannelViews != null && !inChannelViews.isEmpty()) {
            Iterator<Map.Entry<Integer, EaseCallMemberView>> iterator = inChannelViews.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, EaseCallMemberView> entry = iterator.next();
                Integer uid = entry.getKey();
                EaseCallMemberView memberView = entry.getValue();
                if (inChannelAccounts.containsKey(uid)) {
                    memberView.setUserInfo(inChannelAccounts.get(uid));
                }
                if (uid != 0) {
                    mBinding.surfaceViewGroup.addView(memberView);
                    mRtcEngine.setupRemoteVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uid));
                } else {
                    localMemberView = memberView;
                    mBinding.surfaceViewGroup.addView(memberView, 0);
                    mRtcEngine.setupLocalVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }
            }
        }
        if (localMemberView != null) {
            changeCameraDirect(localMemberView.isCameraDirectionFront());
            changeVideoState(localMemberView.isShowVideo());
            changeSpeakerState(localMemberView.isSpeakActivated());
            changeMuteState(localMemberView.isAudioOff());
        }
    }

    private void updateUserInfo(int uid) {
        //update userinfo in local view (更新本地头像昵称)
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (inChannelViews != null) {
                    EaseUserAccount account = inChannelAccounts.get(uid);
                    EaseCallMemberView memberView = inChannelViews.get(uid);
                    if (memberView != null) {
                        if (memberView.getUserInfo() != null) {
                            memberView.updateUserInfo();
                        } else {
                            memberView.setUserInfo(account);
                        }
                    } else {
                        final EaseCallMemberView newMemberView = new EaseCallMemberView(getApplicationContext());
                        newMemberView.setUserInfo(account);
                        mBinding.surfaceViewGroup.addView(newMemberView);
                        inChannelViews.put(uid, newMemberView);
                    }
                }
            }
        });
    }



    void exitChannel() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EMLog.i(TAG, "exit channel channelName: " + channelName);
                EaseCallAudioControl.getInstance().stopPlayRing();
                mBinding.chronometer.stop();
                leaveChannel();
                if (!isInComingCall) {
                    if (invitedUsersTime.size() > 0) {
                        if (timeHandler != null) {
                            timeHandler.stopTime();
                        }

                        Iterator<String> it_user = invitedUsersTime.keySet().iterator();
                        while (it_user.hasNext()) {
                            String userName = it_user.next();
                            //send cancel envent (发送取消事件)
                            EaseCallCallCancelEvent cancelEvent = new EaseCallCallCancelEvent();
                            cancelEvent.callId = EaseCallKit.getInstance().getCallID();
                            sendCmdMsg(cancelEvent, userName);
                            it_user.remove();
                            effectiveUsers.remove(userName);
                            EaseCallMemberView memberView = placeholders.remove(userName);
                            if (memberView != null) {
                                mBinding.surfaceViewGroup.removeView(memberView);
                            }
                        }
                    }
                }
                if (isFloatWindowShowing()) {
                    EaseCallFloatWindow.getInstance().dismiss();
                }else{
                    EaseCallFloatWindow.getInstance().resetCurrentInstance();
                }
                //insert a hangup message to local when in group chat (群聊消息时，本地插入一条挂断消息)
                insertCancelMessageToLocal();
                //reset state (重置状态)
                releaseHandler();
                EaseCallKit.getInstance().setCallState(EaseCallState.CALL_IDLE);
                EaseCallKit.getInstance().setCallID(null);
                EaseCallKit.getInstance().releaseCall();
                RtcEngine.destroy();
                finish();
            }
        });
    }

    private void insertCancelMessageToLocal() {
        final ChatMessage message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invited_to_make_multi_party_call), groupId);
        message.setUnread(false);
        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, EaseCallAction.CALL_CANCEL.state);
        message.setAttribute(EaseCallMsgUtils.CALL_TYPE, callType.code);
        message.setAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, EaseCallMsgUtils.CALL_MSG_INFO);
        message.setAttribute(EaseCallMsgUtils.CALL_CHANNELNAME, channelName);
        message.setAttribute(EaseCallMsgUtils.CALL_COST_TIME, timeUpdataTimer.getPassedTime());
        Conversation conversation = ChatClient.getInstance().chatManager().getConversation(groupId);
        if (conversation != null) {
            conversation.insertMessage(message);
        }
    }

    /**
     * show float window
     */
    @Override
    public void doShowFloatWindow() {
        super.doShowFloatWindow();
        if (timeUpdataTimer != null) {
            Log.e(TAG, "timeUpdataTimer cost seconds: " + timeUpdataTimer.timePassed);
            EaseCallFloatWindow.getInstance().setCostSeconds(timeUpdataTimer.timePassed);
        }
        EaseCallFloatWindow.getInstance().show();
        setConferenceInfoAfterShowFloat();
        int uid = 0;
        if (inChannelViews.size() > 0) { // show first when channel has others
            Set<Integer> uidSet = inChannelViews.keySet();
            for (int id : uidSet) {
                uid = id;
            }
            EaseCallMemberView memberView = inChannelViews.get(uid);
            EaseCallFloatWindow.getInstance().update(memberView);
        }
        moveTaskToBack(false);
    }

    private void setConferenceInfoAfterShowFloat() {
        EaseCallFloatWindow.ConferenceInfo info = new EaseCallFloatWindow.ConferenceInfo();
        info.uidToUserAccountMap = inChannelAccounts;
        info.uidToViewList = getViewStateMap();
        info.userAccountToUidMap = userIdAndUidMap;
        EaseCallFloatWindow.getInstance().setConferenceInfo(info);
    }

    private Map<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> getViewStateMap() {
        if (inChannelViews == null || inChannelViews.isEmpty()) {
            return new HashMap<>();
        }
        Map<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> viewStateMap = new HashMap<>();
        Iterator<Map.Entry<Integer, EaseCallMemberView>> iterator = inChannelViews.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EaseCallMemberView> entry = iterator.next();
            Integer key = entry.getKey();
            EaseCallMemberView value = entry.getValue();
            EaseCallFloatWindow.ConferenceInfo.ViewState viewState = new EaseCallFloatWindow.ConferenceInfo.ViewState();
            if (value != null) {
                viewState.isAudioOff = value.getAudioOff();
                viewState.isCameraFront = value.isCameraDirectionFront();
                viewState.isFullScreenMode = value.isFullScreen();
                viewState.isVideoOff = value.isShowVideo();
                viewState.speakActivated = value.isSpeakActivated();
            }
            viewStateMap.put(key, viewState);
        }
        return viewStateMap;
    }

    /**
     * update float window
     *
     * @param memberView
     */
    private void updateFloatWindow(EaseCallMemberView memberView) {
        if (memberView != null) {
            EaseCallFloatWindow.getInstance().update(memberView);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkConference(false);
    }

    private void checkConference(boolean isNew) {
        if (isFloatWindowShowing()) {
            int uId = EaseCallFloatWindow.getInstance().getUid();
            if (uId != -1) {
                EaseCallMemberView memberView = inChannelViews.get(uId);
                if (memberView != null) {
                    if (uId == 0) {
                        mRtcEngine.setupLocalVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uId));
                    } else {
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uId));
                    }
                }
            }
            if (isNew) {
                EaseCallFloatWindow.ConferenceInfo info = EaseCallFloatWindow.getInstance().getConferenceInfo();
                if (info != null) {
                    resetConferenceData(info);
                    resetVideoView();
                }
            }
            // Prevent the activity from being started in the background to the foreground, causing the Window to still exist(防止activity在后台被start至前台导致window还存在)
            long costSeconds = EaseCallFloatWindow.getInstance().getTotalCostSeconds();
            Log.e(TAG, "costSeconds: " + costSeconds);
            if (timeUpdataTimer != null) {
                timeUpdataTimer.timePassed = (int) costSeconds;
                updateTime(timeUpdataTimer);
            }
            EaseCallFloatWindow.getInstance().dismiss();
        }
        //After processing the data synchronization in the hover window(处理完悬浮窗中的数据同步之后)
        ArrayList<String> users = EaseCallKit.getInstance().getInviteeUsers();
        if (isNew) {
            //is new in (新activity进来)
            effectiveUsers.clear();
            effectiveUsers.addAll(EaseCallKit.getInstance().getInviteeUsers());
            for (Map.Entry<Integer, EaseUserAccount> entry : inChannelAccounts.entrySet()) {
                effectiveUsers.add(entry.getValue().getUserName());
            }
        } else {
            //will be added if have deference (有差异的会加进来)
            effectiveUsers.addAll(users);
            if (users != null && users.size() > 0) {
                handler.sendEmptyMessage(MSG_MAKE_CONFERENCE_VIDEO);
            }
        }
    }

    private void resetConferenceData(EaseCallFloatWindow.ConferenceInfo info) {
        if (info != null) {
            if (info.uidToUserAccountMap != null) {
                this.inChannelAccounts.putAll(info.uidToUserAccountMap);
            }
            if (info.userAccountToUidMap != null) {
                this.userIdAndUidMap.putAll(info.userAccountToUidMap);
            }
            if (info.uidToViewList != null) {
                Map<Integer, EaseCallMemberView> callViewMap = createCallViewMap(info.uidToViewList);
                this.inChannelViews.putAll(callViewMap);
                info.uidToViewList.clear();
            }
        }
    }

    private Map<Integer, EaseCallMemberView> createCallViewMap(Map<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> viewStateMap) {
        Map<Integer, EaseCallMemberView> memberViewMap = new HashMap<>();
        if (viewStateMap == null || viewStateMap.isEmpty()) {
            return memberViewMap;
        }
        Iterator<Map.Entry<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState>> iterator = viewStateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> entry = iterator.next();
            Integer uid = entry.getKey();
            EaseCallFloatWindow.ConferenceInfo.ViewState state = entry.getValue();
            EaseCallMemberView memberView = createCallMemberView();
            memberView.setCameraDirectionFront(state.isCameraFront);
            memberView.setAudioOff(state.isAudioOff);
            memberView.showVideo(state.isVideoOff);
            memberView.setSpeakActivated(state.speakActivated);
            memberView.setFullScreen(state.isFullScreenMode);
            memberViewMap.put(uid, memberView);
        }
        return memberViewMap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        EMLog.i(TAG, "onActivityResult: " + requestCode + ", result code: " + resultCode);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestOverlayPermission = false;
            // Result of window permission request, resultCode = RESULT_CANCELED
            if (Settings.canDrawOverlays(this)) {
                doShowFloatWindow();
            } else {
                Toast.makeText(this, getString(R.string.ease_call_alert_window_permission_denied), Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }


    protected void releaseHandler() {
        if(handler!=null) {
            handler.sendEmptyMessage(MSG_RELEASE_HANDLER);
        }
        if (timeHandler != null) {
            timeHandler.stopTime();
        }
        if (timeUpdataTimer != null) {
            timeUpdataTimer.stopTime();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding.surfaceViewGroup.removeAllViews();
        releaseHandler();
        if (inChannelViews != null) {
            inChannelViews.clear();
        }
        if (!isFloatWindowShowing()) {
            if (userIdAndUidMap != null) {
                userIdAndUidMap.clear();
            }
            if (inChannelAccounts != null) {
                inChannelAccounts.clear();
            }
            leaveChannel();
            //Avoid the effect of last instance delay release on this instance（避免上个实例延迟释放对本实例产生影响）
            if(TextUtils.equals(EaseCallFloatWindow.getInstance().getCurrentInstance(),this.toString())) {
                EaseCallKit.getInstance().releaseCall();
                RtcEngine.destroy();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        Log.d(TAG, "onUserLeaveHint");
        super.onUserLeaveHint();
    }


    @Override
    public void onBackPressed() {
        exitChannelDisplay();
    }


    /**
     * whether to exit current call dialog hint (是否退出当前通话提示框)
     */
    public void exitChannelDisplay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EaseCallMultipleBaseActivity.this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(EaseCallMultipleBaseActivity.this, R.layout.ease_call_activity_exit_channel, null);
        dialog.setView(dialogView);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.CENTER | Gravity.CENTER;
        dialog.show();

        final Button btn_ok = dialogView.findViewById(R.id.btn_ok);
        final Button btn_cancel = dialogView.findViewById(R.id.btn_cancel);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                EMLog.e(TAG, "exitChannelDisplay  exit channel:");
                exitChannel();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                EMLog.e(TAG, "exitChannelDisplay not exit channel");
            }
        });
    }
};