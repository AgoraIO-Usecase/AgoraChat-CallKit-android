package io.agora.chat.callkit.ui;

import static io.agora.chat.callkit.utils.EaseCallImageUtil.setBgRadius;
import static io.agora.chat.callkit.utils.EaseCallImageUtil.setImage;
import static io.agora.chat.callkit.utils.EaseCallImageUtil.setViewGaussianBlur;
import static io.agora.chat.callkit.utils.EaseCallKitUtils.dp2px;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.CALL_INVITE_EXT;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_MAKE_SIGNAL_VIDEO;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_MAKE_SIGNAL_VOICE;
import static io.agora.chat.callkit.utils.EaseCallMsgUtils.MSG_RELEASE_HANDLER;
import static io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
import static io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import io.agora.CallBack;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.chat.Conversation;
import io.agora.chat.callkit.EaseCallKit;
import io.agora.chat.callkit.R;
import io.agora.chat.callkit.bean.EaseCallUserInfo;
import io.agora.chat.callkit.bean.EaseUserAccount;
import io.agora.chat.callkit.databinding.EaseCallActivitySingleBinding;
import io.agora.chat.callkit.event.EaseCallAlertEvent;
import io.agora.chat.callkit.event.EaseCallAnswerEvent;
import io.agora.chat.callkit.event.EaseCallBaseEvent;
import io.agora.chat.callkit.event.EaseCallCallCancelEvent;
import io.agora.chat.callkit.event.EaseCallConfirmCallEvent;
import io.agora.chat.callkit.event.EaseCallConfirmRingEvent;
import io.agora.chat.callkit.event.EaseCallInviteEventEase;
import io.agora.chat.callkit.event.EaseCallVideoToVoiceeEvent;
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
import io.agora.chat.callkit.widget.EaseCallChronometer;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.util.EMLog;


public class EaseCallSingleBaseActivity extends EaseCallBaseActivity implements View.OnClickListener {

    private static final String TAG = EaseCallSingleBaseActivity.class.getSimpleName();
    private EaseCallActivitySingleBinding mBinding;
    private Bundle savedInstanceState;

    private static final int PERMISSION_REQ_ID = 22;
    private final String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
    List<String> mPermissionList = new ArrayList<>();


    private View rootView;
    private boolean isMuteVoice = false;
    private boolean isSpeakerOn;
    // Determine whether to initiate or to be invited
    protected boolean isInComingCall;
    // Judge whether is ongoing call
    protected boolean isOngoingCall;
    protected String username;
    protected String channelName;

    volatile private boolean mConfirmRing = false;
    private int remoteUId = 0;
    private boolean changeFlag = true;
    private String headUrl = null;
    private int idInLocalSurfaceLayout = -1;
    private int idInOppositeSurfaceLayout = -1;
    protected EaseCallType callType;
    private TimeHandler timehandler;
    private DateFormat dateFormat = null;
    private RtcEngine mRtcEngine;
    private boolean isLocalVideoMuted = false;
    private boolean isRemoteVideoMuted = false;
    private String agoraAppId = null;
    // Camera direction: front or back
    private boolean isCameraFront;
    // To prevent opening the request hover page multiple times
    private boolean requestOverlayPermission;
    private boolean isAgreedInHeadDialog;
    //in channel EaseUserAccounts
    private Map<Integer, EaseUserAccount> inChannelAccounts = new HashMap<>();
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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (EaseCallKit.getInstance().getCallType() == EaseCallType.SINGLE_VOICE_CALL) {
                        setSpeakerMode(false);
                    } else {
                        setSpeakerMode(true);
                    }
                    if (!isInComingCall) {
                        //send invite message
                        if (EaseCallKit.getInstance().getCallType() == EaseCallType.SINGLE_VIDEO_CALL) {
                            handler.sendEmptyMessage(MSG_MAKE_SIGNAL_VIDEO);
                        } else {
                            handler.sendEmptyMessage(MSG_MAKE_SIGNAL_VOICE);
                        }
                        //start invite time record
                        timehandler.startTime();
                    }
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EaseCallAudioControl.getInstance().stopPlayRing();
                    // the remote come in
                    makeOngoingStatus();
                    setUserJoinChannelInfo(null, uid);
                }
            });
        }
        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //the remote exit ,the exit self too
                    exitChannel();
                    if (inChannelAccounts != null) {
                        inChannelAccounts.remove(uid);
                    }
                    if (listener != null) {
                        //remote hangup
                        long time = getChronometerSeconds(mBinding.chronometer);
                        listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonHangup, time * 1000);
                    }

                }
            });
        }


        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            isRemoteVideoMuted = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remoteUId = uid;
                    if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
                        updateViewWithCameraStatus();
                    }
                }
            });
        }

        @Deprecated
        public void onFirstRemoteAudioFrame(int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remoteUId = uid;
                    startCount();
                    if (EaseCallKit.getInstance().getCallType() == EaseCallType.SINGLE_VOICE_CALL) {
                        mBinding.llVoiceCallingControl.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public void onUserMuteVideo(int uid, boolean muted) {
            super.onUserMuteVideo(uid, muted);
            isRemoteVideoMuted = muted;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(EaseCallKit.getInstance().getCallType()==EaseCallType.SINGLE_VIDEO_CALL) {
                        //Stop the video remotely
                        //Opens by onFirstRemoteVideoDecoded callback to update the view, avoid rebuild streaming video produced by the black screen time
                        //They'll probably have their cameras turned off by the time they join
                        if(muted) {
                            updateViewWithCameraStatus();
                        }
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        mBinding = EaseCallActivitySingleBinding.inflate(getLayoutInflater());
        setContentView(mBinding.rootLayout);

        if (Build.VERSION.SDK_INT >= 23) {
            initPermission();
        } else {
            init();
        }
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


    private void init() {
        if (savedInstanceState == null) {
            initParams(getIntent().getExtras());
        } else {
            initParams(savedInstanceState);
        }
        initEngine();
        //Init View
        initView();
        checkFloatIntent(getIntent());
        addLiveDataObserver();
        timehandler = new TimeHandler();
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        EaseCallKit.getInstance().getNotifier().reset();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;
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

    private void initParams(Bundle bundle) {
        if (bundle != null) {
            isInComingCall = bundle.getBoolean("isComingCall", false);
            username = bundle.getString("username");
            channelName = bundle.getString("channelName");
            isAgreedInHeadDialog = bundle.getBoolean("isAgreedInHeadDialog");
            int uId = bundle.getInt("uId", -1);
            callType = EaseCallKit.getInstance().getCallType();
            if (uId == -1) {
                EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
            } else {
                isOngoingCall = true;
            }
        } else {
            isInComingCall = EaseCallKit.getInstance().getIsComingCall();
            username = EaseCallKit.getInstance().getFromUserId();
            channelName = EaseCallKit.getInstance().getChannelName();
            EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
        }
    }

    public void initView() {
        rootView = ((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);
        if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
            mBinding.llVideoCallingHead.setVisibility(View.VISIBLE);
            mBinding.llVoiceCallingHead.setVisibility(View.GONE);
            mBinding.btnSwitchCamera.setVisibility(View.VISIBLE);
            if (isInComingCall) {
                mBinding.btnVideoTranse.setVisibility(View.GONE);
                mBinding.btnVideoTranseComming.setVisibility(View.GONE);
            } else {
                mBinding.btnVideoTranse.setVisibility(View.GONE);
                mBinding.btnVideoTranseComming.setVisibility(View.GONE);
            }
            mBinding.ivAvatar.setVisibility(View.GONE);
        } else {
            //voice call
            mBinding.llVideoCallingHead.setVisibility(View.GONE);
            mBinding.btnVideoTranse.setVisibility(View.GONE);
            mBinding.btnVideoTranseComming.setVisibility(View.GONE);
            mBinding.llVoiceCallingHead.setVisibility(View.VISIBLE);
            mBinding.btnHangupCall.setVisibility(View.GONE);
            mBinding.tvNickVoice.setText(EaseCallKitUtils.getUserNickName(username));
            mBinding.btnSwitchCamera.setVisibility(View.GONE);

            //Set gaussian blur background
            String userHeadImage = EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser());
            setViewGaussianBlur(mBinding.rootLayout, userHeadImage);
            //surfaceView gone
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
            //voice ui visitable
            mBinding.ivAvatar.setVisibility(View.VISIBLE);
        }
        mBinding.llVideoCalled.setVisibility(View.GONE);
        mBinding.llVoiceCallingControl.setVisibility(View.GONE);
        mBinding.btnVideoTranse.setOnClickListener(this);
        mBinding.btnVideoTranseComming.setOnClickListener(this);
        mBinding.btnVoiceTrans.setOnClickListener(this);
        mBinding.btnRefuseCall.setOnClickListener(this);
        mBinding.btnAnswerCall.setOnClickListener(this);
        mBinding.btnHangupCall.setOnClickListener(this);
        mBinding.btnVideoHangupCall.setOnClickListener(this);
        mBinding.ivMute.setOnClickListener(this);
        mBinding.ivSpeaker.setOnClickListener(this);
        mBinding.btnSwitchCamera.setOnClickListener(this);
        mBinding.btnRefuseVideoCalled.setOnClickListener(this);
        mBinding.btnAnswerVideoCalled.setOnClickListener(this);
        mBinding.ivVidiconVideoCalled.setOnClickListener(this);
        mBinding.ivVidiconVideoCalling.setOnClickListener(this);
        mBinding.ivVideoMute.setOnClickListener(this);
        mBinding.btnCallFloat.setOnClickListener(this);
        mBinding.ivCallClose.setOnClickListener(this);
        mBinding.ivCallRedial.setOnClickListener(this);
        mBinding.localSurfaceLayout.setOnClickListener(this);

        mBinding.tvNick.setText(EaseCallKitUtils.getUserNickName(username));
        setBgRadius(mBinding.localSurfaceLayout, dp2px(this, 12));
        headUrl = EaseCallKitUtils.getUserHeadImage(username);

        loadHeadImage();
        //incoming (来电状态)
        if (isInComingCall) {
            //agreed in dialog outside 点击外面弹框中的同意按钮进来的
            if (isAgreedInHeadDialog) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Agree to answer directly, because click on the outside agree(直接同意接听，因为在外边点击同意了)
                        sendAgreeMessage();
                    }
                });
            } else {
                //incoming status (被呼叫状态)
                makeComingStatus();
            }
        } else {
            makeCallStatus();
            EaseCallAudioControl.getInstance().playRing();
            //Calling party joins channel(主叫加入频道)
            joinChannel();
        }
        if (isOngoingCall) {
            makeOngoingStatus();
        }
    }


    private void initEngine() {
        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
    }

    /**
     * incoming ui status
     */
    private void makeComingStatus() {
        mBinding.groupUseInfo.setVisibility(View.VISIBLE);
        if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
            mBinding.llVideoCalledControl.setVisibility(View.VISIBLE);
            mBinding.groupOngoingSettings.setVisibility(View.GONE);
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            mBinding.tvCallState.setText(getString(R.string.ease_call_video_call));
        } else {
            mBinding.llComingCallVoice.setVisibility(View.VISIBLE);
            mBinding.ivAvatar.setVisibility(View.VISIBLE);
            mBinding.tvNick.setVisibility(View.VISIBLE);
            mBinding.tvCallStateVoice.setVisibility(View.VISIBLE);
            mBinding.tvCallStateVoice.setText(getString(R.string.ease_call_voice_call));
        }
        mBinding.llVoiceCallingControl.setVisibility(View.GONE);
    }


    /**
     * on going ui state
     */
    private void makeOngoingStatus() {
        isOngoingCall = true;
        mBinding.llComingCallVoice.setVisibility(View.GONE);
        mBinding.groupUseInfo.setVisibility(View.GONE);
        mBinding.llVideoCalledControl.setVisibility(View.GONE);
        callType = EaseCallKit.getInstance().getCallType();
        EaseCallFloatWindow.getInstance().setCallType(callType);
        if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
            mBinding.groupOngoingSettings.setVisibility(View.GONE);
            mBinding.localSurfaceLayout.setVisibility(View.VISIBLE);
            mBinding.llVideoCalled.setVisibility(View.VISIBLE);
            mBinding.llVideoCallingOutAndOngoingControl.setVisibility(View.VISIBLE);
            mBinding.llVoiceCallingControl.setVisibility(View.GONE);
            mBinding.btnHangupCall.setVisibility(View.GONE);
            mBinding.llVideoCallingHead.setVisibility(View.GONE);
            mBinding.llVoiceCallingHead.setVisibility(View.GONE);
        } else {
            mBinding.llVideoCallingOutAndOngoingControl.setVisibility(View.GONE);
            mBinding.groupOngoingSettings.setVisibility(View.VISIBLE);
            mBinding.ivAvatar.setVisibility(View.VISIBLE);
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
            mBinding.tvNick.setVisibility(View.VISIBLE);
            mBinding.llVideoCalled.setVisibility(View.GONE);
            mBinding.llVoiceCallingControl.setVisibility(View.VISIBLE);
            mBinding.btnHangupCall.setVisibility(View.VISIBLE);

            mBinding.llVideoCallingHead.setVisibility(View.GONE);
            mBinding.llVoiceCallingHead.setVisibility(View.VISIBLE);
            mBinding.tvNickVoice.setText(EaseCallKitUtils.getUserNickName(username));
            mBinding.tvCallStateVoice.setVisibility(View.GONE);
        }

        mBinding.btnVideoTranse.setVisibility(View.GONE);
        mBinding.btnVideoTranseComming.setVisibility(View.GONE);
    }

    /**
     * calling ui state
     */
    public void makeCallStatus() {
        if (!isInComingCall && callType == EaseCallType.SINGLE_VOICE_CALL) {
            mBinding.llVoiceCallingControl.setVisibility(View.VISIBLE);
            mBinding.llVideoCallingHead.setVisibility(View.GONE);
            mBinding.llVoiceCallingHead.setVisibility(View.VISIBLE);
            mBinding.tvCallStateVoice.setVisibility(View.VISIBLE);
            mBinding.tvCallStateVoice.setText(getApplicationContext().getString(R.string.ease_call_calling));
        } else {
            mBinding.llVoiceCallingControl.setVisibility(View.GONE);
            mBinding.llVideoCallingOutAndOngoingControl.setVisibility(View.VISIBLE);
            mBinding.llVideoCallingHead.setVisibility(View.VISIBLE);
            mBinding.llVoiceCallingHead.setVisibility(View.GONE);
            mBinding.tvCallState.setText(getApplicationContext().getString(R.string.ease_call_calling));
            //oppositeSurface_layout.setVisibility(View.GONE);
        }
        mBinding.llComingCallVoice.setVisibility(View.GONE);
        mBinding.groupOngoingSettings.setVisibility(View.GONE);
        mBinding.localSurfaceLayout.setVisibility(View.GONE);
        mBinding.llVideoCloseControl.setVisibility(View.GONE);
        mBinding.btnCallFloat.setVisibility(View.VISIBLE);
        mBinding.btnSwitchCamera.setVisibility(View.VISIBLE);
    }

    private void initializeEngine() {
        try {
            EaseCallKitConfig config = EaseCallKit.getInstance().getCallKitConfig();
            if (config != null) {
                agoraAppId = config.getAgoraAppId();
            }
            mRtcEngine = RtcEngine.create(getBaseContext(), agoraAppId, mRtcEventHandler);
            //// Because there is a applet set to live mode, the role is set to master
            mRtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setClientRole(CLIENT_ROLE_BROADCASTER);
            EaseCallFloatWindow.getInstance().setRtcEngine(getApplicationContext(), mRtcEngine);
        } catch (Exception e) {
            EMLog.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        if (EaseCallKit.getInstance().getCallType() == EaseCallType.SINGLE_VIDEO_CALL) {
            mRtcEngine.enableVideo();
            mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_1280x720,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
            isCameraFront = true;
        } else {
            mRtcEngine.disableVideo();
        }
    }

    private void setupLocalVideo() {
        if (isFloatWindowShowing()) {
            return;
        }
        updateOppositeSurfaceLayoutUid(0);
        if (isInComingCall) {
            if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
                mRtcEngine.startPreview();
            }
        }
    }

    private void joinChannel() {
        EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
        if (listener != null && callKitConfig != null && callKitConfig.isEnableRTCToken()) {
            listener.onGenerateRTCToken(ChatClient.getInstance().getCurrentUser(), channelName, new EaseCallKitTokenCallback() {
                @Override
                public void onSetToken(String token, int uId) {
                    EMLog.d(TAG, "onSetToken token:" + token + " uid: " + uId);
                    //gets agora RTC token ,then join channel (获取到RTC Token uid加入频道)
                    mRtcEngine.joinChannel(token, channelName, null, uId);
                    //add uid to inChannelAccounts
                    inChannelAccounts.put(uId, new EaseUserAccount(uId, ChatClient.getInstance().getCurrentUser()));
                }

                @Override
                public void onGetTokenError(int error, String errorMsg) {
                    EMLog.e(TAG, "onGenerateToken error :" + error + " errorMsg:" + errorMsg);
                    // Failed to obtain the RTC Token(获取RTC Token失败,退出呼叫)
                    exitChannel();
                }
            });
        } else {
            //Don't checkout token
            mRtcEngine.joinChannel(null, channelName, null, 0);
            //add uid to inChannelAccounts
            inChannelAccounts.put(0, new EaseUserAccount(0, ChatClient.getInstance().getCurrentUser()));
        }
    }

    private void changeCameraDirection(boolean isFront) {
        if (isCameraFront != isFront) {
            if (mRtcEngine != null) {
                mRtcEngine.switchCamera();
            }
            isCameraFront = isFront;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_refuse_call || id == R.id.btn_refuse_video_called) {
            if (isInComingCall) {
                sendRefuseMessage();
            }
            exitChannel();
        } else if (id == R.id.btn_answer_call || id == R.id.btn_answer_video_called) {
            if (isInComingCall) {
                EaseCallAudioControl.getInstance().stopPlayRing();
                sendAgreeMessage();
            }
        } else if (id == R.id.btn_hangup_call || id == R.id.btn_video_hangup_call) {
            stopCount();
            if (remoteUId == 0) {
                EaseCallCallCancelEvent cancelEvent = new EaseCallCallCancelEvent();
                cancelEvent.callId = EaseCallKit.getInstance().getCallID();
                sendCmdMsg(cancelEvent, username);
            } else {
                if (listener != null) {
                    //normal hangup
                    long time = getChronometerSeconds(mBinding.chronometer);
                    listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonHangup, time * 1000);
                }
            }
            exitChannel();
        } else if (id == R.id.local_surface_layout) {
            changeSurface();
        } else if (id == R.id.btn_call_float) {
            showFloatWindow();
        } else if (id == R.id.iv_mute || id == R.id.iv_video_mute) { // mute
            if (isMuteVoice) {
                // resume voice transfer
                mBinding.ivMute.setImageResource(R.drawable.call_mute_normal);
                mBinding.ivVideoMute.setImageResource(R.drawable.call_mute_normal);
                mRtcEngine.muteLocalAudioStream(false);
                isMuteVoice = false;
            } else {
                // pause voice transfer
                mBinding.ivMute.setImageResource(R.drawable.call_mute_on);
                mBinding.ivVideoMute.setImageResource(R.drawable.call_mute_on);
                mRtcEngine.muteLocalAudioStream(true);
                isMuteVoice = true;
            }
        } else if (id == R.id.iv_speaker) {
            setSpeakerMode(!isSpeakerOn);
        } else if (id == R.id.btn_switch_camera) {
            changeCameraDirection(!isCameraFront);
        } else if (id == R.id.btn_voice_trans) {
            if (callType == EaseCallType.SINGLE_VOICE_CALL) {
                callType = EaseCallType.SINGLE_VIDEO_CALL;
                EaseCallKit.getInstance().setCallType(EaseCallType.SINGLE_VIDEO_CALL);
                EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
                changeVideoVoiceState();
                if (mRtcEngine != null) {
                    mRtcEngine.muteLocalVideoStream(false);
                }
            } else {
                callType = EaseCallType.SINGLE_VOICE_CALL;
                EaseCallKit.getInstance().setCallType(EaseCallType.SINGLE_VOICE_CALL);
                EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
                setSpeakerMode(true);
                mBinding.ivSpeaker.setImageResource(R.drawable.em_icon_speaker_on);
                changeVideoVoiceState();
                if (mRtcEngine != null) {
                    mRtcEngine.muteLocalVideoStream(true);
                }
            }
        } else if (id == R.id.btn_video_transe_comming || id == R.id.btn_video_transe) {
            //Switch to audio before entering a call
            callType = EaseCallType.SINGLE_VOICE_CALL;
            EaseCallKit.getInstance().setCallType(EaseCallType.SINGLE_VOICE_CALL);
            EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
            if (mRtcEngine != null) {
                mRtcEngine.disableVideo();
                mRtcEngine.muteLocalVideoStream(true);
            }
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
            //Set gaussian blur background
            setViewGaussianBlur(rootView, EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser()));

            loadHeadImage();

            mBinding.llVideoCallingHead.setVisibility(View.GONE);
            mBinding.btnVideoTranse.setVisibility(View.GONE);
            mBinding.btnVideoTranseComming.setVisibility(View.GONE);
            mBinding.llVoiceCallingHead.setVisibility(View.VISIBLE);
            mBinding.tvNickVoice.setText(EaseCallKitUtils.getUserNickName(username));
//            if(!isInComingCall){
//                voiceCalledGroup.setVisibility(View.VISIBLE);
//            }
            if (isInComingCall) {
                EaseCallAudioControl.getInstance().stopPlayRing();
                //send answer message
                EaseCallAnswerEvent event = new EaseCallAnswerEvent();
                event.result = EaseCallMsgUtils.CALL_ANSWER_ACCEPT;
                event.callId = EaseCallKit.getInstance().getCallID();
                event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallKit.deviceId;
                event.transVoice = true;
                sendCmdMsg(event, username);
            } else {
                //send voice to voice event
                EaseCallVideoToVoiceeEvent event = new EaseCallVideoToVoiceeEvent();
                sendCmdMsg(event, username);
            }
        } else if (id == R.id.iv_vidicon_video_called || id == R.id.iv_vidicon_video_calling) {
            //close camera
            if (!isLocalVideoMuted) {
                mRtcEngine.muteLocalVideoStream(true);
                isLocalVideoMuted = true;
            } else {
                mRtcEngine.muteLocalVideoStream(false);
                isLocalVideoMuted = false;
            }
            if (isOngoingCall) {
                updateViewWithCameraStatus();
            } else {
                if (isInComingCall) {
                    if (isLocalVideoMuted) {
                        String userHeadImage = EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser());
                        setViewGaussianBlur(mBinding.rootLayout, userHeadImage);
                        mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
                    } else {
                        mBinding.oppositeSurfaceLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (isLocalVideoMuted) {
                        String userHeadImage = EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser());
                        setViewGaussianBlur(mBinding.rootLayout, userHeadImage);
                        mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
                    } else {
                        mBinding.oppositeSurfaceLayout.setVisibility(View.VISIBLE);
                    }
                    mBinding.ivVidiconVideoCalled.setImageResource(!isLocalVideoMuted ? R.drawable.call_video_on : R.drawable.call_video_off);
                }
            }
            mBinding.ivVidiconVideoCalling.setImageResource(!isLocalVideoMuted ? R.drawable.call_video_on : R.drawable.call_video_off);
        } else if (id == R.id.iv_call_redial) {
            makeCallStatus();
            EaseCallAudioControl.getInstance().playRing();
            if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
                mBinding.oppositeSurfaceLayout.setVisibility(View.VISIBLE);
            }
            joinChannel();
        } else if (id == R.id.iv_call_close) {
            resetState();
        }
    }

    private void setSpeakerMode(boolean isSpeakerOn) {
        this.isSpeakerOn = isSpeakerOn;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSpeakerOn) {
                    mBinding.ivSpeaker.setImageResource(R.drawable.em_icon_speaker_on);
                    EaseCallAudioControl.getInstance().openSpeakerOn();
                } else {
                    mBinding.ivSpeaker.setImageResource(R.drawable.em_icon_speaker_normal);
                    EaseCallAudioControl.getInstance().closeSpeakerOn();
                }
            }
        });
    }


    private void updateViewWithCameraStatus() {
        if (isLocalVideoMuted && isRemoteVideoMuted) {
            //Turn off the cameras on both sides
            mBinding.btnSwitchCamera.setVisibility(View.GONE);
            mBinding.cslMuteSmallview.setVisibility(View.GONE);
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
            String userHeadImage = EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser());
            setViewGaussianBlur(mBinding.rootLayout, userHeadImage);
            mBinding.llVoiceCallingHead.setVisibility(View.VISIBLE);
//            mBinding.tvCallStateVoice.setText(mBinding.chronometer.getContentDescription());
            mBinding.tvCallStateVoice.setText("");
            setImage(this, mBinding.ivAvatarVoice, headUrl);
        } else if (isLocalVideoMuted && !isRemoteVideoMuted) {
            //Local disabled The remote camera is not disabled
            mBinding.btnSwitchCamera.setVisibility(View.GONE);
            mBinding.rootLayout.setBackground(null);
            mBinding.cslMuteSmallview.setVisibility(View.VISIBLE);
            mBinding.llVoiceCallingHead.setVisibility(View.GONE);
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            updateOppositeSurfaceLayoutUid(remoteUId);
            setImage(this, mBinding.ivMuteSmall, EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser()));
        } else if (!isLocalVideoMuted && isRemoteVideoMuted) {
            //The local camera is disabled, and the remote camera is disabled
            mBinding.btnSwitchCamera.setVisibility(View.VISIBLE);
            mBinding.rootLayout.setBackground(null);
            mBinding.llVoiceCallingHead.setVisibility(View.GONE);
            mBinding.cslMuteSmallview.setVisibility(View.VISIBLE);
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            updateOppositeSurfaceLayoutUid(0);
            idInLocalSurfaceLayout = -1;
            setImage(this, mBinding.ivMuteSmall, headUrl);
        } else {
            //Neither side turned off their cameras
            mBinding.btnSwitchCamera.setVisibility(View.VISIBLE);
            mBinding.rootLayout.setBackground(null);
            mBinding.llVoiceCallingHead.setVisibility(View.GONE);
            mBinding.cslMuteSmallview.setVisibility(View.GONE);
            updateOppositeSurfaceLayoutUid(remoteUId);
            updateLocalSurfaceLayoutUid(0);
        }
        if (isFloatWindowShowing()) {
            EaseCallFloatWindow.getInstance().setRemoteVideoMuted(isRemoteVideoMuted);
            EaseCallFloatWindow.getInstance().update(!changeFlag, headUrl, 0, remoteUId, true);
        }
    }

    private void sendAgreeMessage() {
        EaseCallAnswerEvent event = new EaseCallAnswerEvent();
        event.result = EaseCallMsgUtils.CALL_ANSWER_ACCEPT;
        event.callId = EaseCallKit.getInstance().getCallID();
        event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
        event.calleeDevId = EaseCallKit.deviceId;
        if (TextUtils.isEmpty(username)) {
            username = EaseCallKit.getInstance().getFromUserId();
        }
        if (TextUtils.isEmpty(channelName)) {
            channelName = EaseCallKit.getInstance().getChannelName();
        }
        sendCmdMsg(event, username);
    }

    private void sendRefuseMessage() {
        stopCount();
        //send refused message
        EaseCallAnswerEvent event = new EaseCallAnswerEvent();
        event.result = EaseCallMsgUtils.CALL_ANSWER_REFUSE;
        event.callId = EaseCallKit.getInstance().getCallID();
        event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
        event.calleeDevId = EaseCallKit.deviceId;
        sendCmdMsg(event, username);
    }

    private void changeSurface() {
        if (changeFlag) {
            updateLocalSurfaceLayoutUid(remoteUId);
            updateOppositeSurfaceLayoutUid(0);
            changeFlag = !changeFlag;
        } else {
            updateLocalSurfaceLayoutUid(0);
            updateOppositeSurfaceLayoutUid(remoteUId);
            changeFlag = !changeFlag;
        }
    }

    private synchronized void updateOppositeSurfaceLayoutUid(int uid) {
        mBinding.oppositeSurfaceLayout.setVisibility(View.VISIBLE);
        if (idInOppositeSurfaceLayout != uid) {
            idInOppositeSurfaceLayout = uid;
            SurfaceView localview = RtcEngine.CreateRendererView(getBaseContext());
            VideoCanvas mLocalVideo = new VideoCanvas(localview, VideoCanvas.RENDER_MODE_HIDDEN, uid);
            if (uid == 0) {
                mRtcEngine.setupLocalVideo(mLocalVideo);
            } else {
                mRtcEngine.setupRemoteVideo(mLocalVideo);
            }
            if(mBinding.oppositeSurfaceLayout.getChildCount()>=10) {
                mBinding.oppositeSurfaceLayout.removeAllViews();
            }
            mBinding.oppositeSurfaceLayout.addView(localview);
        }
    }

    private synchronized void updateLocalSurfaceLayoutUid(int uid) {
        mBinding.localSurfaceLayout.setVisibility(View.VISIBLE);
        if (idInLocalSurfaceLayout != uid) {
            idInLocalSurfaceLayout = uid;
            TextureView remoteview = RtcEngine.CreateTextureView(getBaseContext());
            setBgRadius(remoteview, dp2px(EaseCallSingleBaseActivity.this, 12));
            VideoCanvas mRemoteVideo = new VideoCanvas(remoteview, VideoCanvas.RENDER_MODE_HIDDEN, uid);
            if (uid == 0) {
                mRtcEngine.setupLocalVideo(mRemoteVideo);
            } else {
                mRtcEngine.setupRemoteVideo(mRemoteVideo);
            }
            if(mBinding.localSurfaceLayout.getChildCount()>=10) {
                mBinding.localSurfaceLayout.removeAllViews();
            }
            mBinding.localSurfaceLayout.addView(remoteview);
        }
    }


    private void leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
    }

    void changeVideoVoiceState() {
        if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
            // change to voice  ui
            mBinding.ivAvatar.setVisibility(View.GONE);

            //surfaceView gone
            mBinding.localSurfaceLayout.setVisibility(View.VISIBLE);
            mBinding.oppositeSurfaceLayout.setVisibility(View.VISIBLE);

            makeOngoingStatus();
        } else { // change to voice  ui
            mBinding.localSurfaceLayout.setVisibility(View.GONE);
            mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
            //Set gaussian blur background
            setViewGaussianBlur(rootView, EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser()));

            //in calling
            if (EaseCallKit.getInstance().getCallState() == EaseCallState.CALL_ANSWERED) {
                //voice ui visitable
                mBinding.ivAvatar.setVisibility(View.VISIBLE);
                makeOngoingStatus();
            } else {
                mBinding.localSurfaceLayout.setVisibility(View.GONE);
                mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
                mBinding.tvCallStateVoice.setVisibility(View.VISIBLE);
                //Set gaussian blur background
                setViewGaussianBlur(rootView, EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser()));

                if (isInComingCall) {
                    mBinding.tvCallStateVoice.setText(getApplicationContext().getString(R.string.ease_call_voice_call));
                } else {
                    mBinding.tvCallStateVoice.setText(getApplicationContext().getString(R.string.ease_call_calling));
                }
                mBinding.llVideoCallingHead.setVisibility(View.GONE);
                mBinding.btnVideoTranse.setVisibility(View.GONE);
                mBinding.btnVideoTranseComming.setVisibility(View.GONE);
                mBinding.llVoiceCallingHead.setVisibility(View.VISIBLE);
                mBinding.tvNickVoice.setText(EaseCallKitUtils.getUserNickName(username));
            }
            loadHeadImage();
        }
    }


    protected void addLiveDataObserver() {
        EaseCallLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString(), EaseCallBaseEvent.class).observe(this, event -> {
            if (event != null) {
                switch (event.callAction) {
                    case CALL_ALERT:
                        EaseCallAlertEvent alertEvent = (EaseCallAlertEvent) event;
                        //Determine whether the session is valid
                        EaseCallConfirmRingEvent ringEvent = new EaseCallConfirmRingEvent();
                        if (TextUtils.equals(alertEvent.callId, EaseCallKit.getInstance().getCallID())
                                && EaseCallKit.getInstance().getCallState() != EaseCallState.CALL_ANSWERED) {
                            //Send a valid session message
                            ringEvent.calleeDevId = alertEvent.calleeDevId;
                            ringEvent.callId = alertEvent.callId;
                            ringEvent.valid = true;
                            sendCmdMsg(ringEvent, username);
                        } else {
                            //Send a invalid session message
                            ringEvent.calleeDevId = alertEvent.calleeDevId;
                            ringEvent.callId = alertEvent.callId;
                            ringEvent.valid = false;
                            sendCmdMsg(ringEvent, username);
                        }
                        //A session confirmation message has been sent. Procedure
                        mConfirmRing = true;
                        break;
                    case CALL_CANCEL:

                        if (!TextUtils.equals(event.userId, username) && !TextUtils.equals(event.userId, ChatClient.getInstance().getCurrentUser())) {
                            //An event sent by a strange third party
                            break;
                        }
                        if (!isInComingCall) {
                            //Stop quorum timer
                            timehandler.stopTime();
                        }
                        //cancel call
                        exitChannel();
                        if (listener != null) {
                            //the other party cancels
                            listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRemoteCancel, 0);
                        }
                        break;
                    case CALL_ANSWER:
                        EaseCallAnswerEvent answerEvent = (EaseCallAnswerEvent) event;
                        EaseCallConfirmCallEvent callEvent = new EaseCallConfirmCallEvent();
                        boolean transVoice = answerEvent.transVoice;
                        callEvent.calleeDevId = answerEvent.calleeDevId;
                        callEvent.callerDevId = answerEvent.callerDevId;
                        callEvent.result = answerEvent.result;
                        callEvent.callId = answerEvent.callId;
                        if (TextUtils.equals(answerEvent.result, EaseCallMsgUtils.CALL_ANSWER_BUSY)) {
                            if (!mConfirmRing) {
                                timehandler.stopTime();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        exitChannel();

                                        if (listener != null) {
                                            //The other party is busy
                                            listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonBusy, 0);
                                        }

                                    }
                                });
                            } else {
                                timehandler.stopTime();
                                sendCmdMsg(callEvent, username);
                            }
                        } else if (TextUtils.equals(answerEvent.result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                            //Set to answer
                            EaseCallKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                            timehandler.stopTime();
                            sendCmdMsg(callEvent, username);
                            if (transVoice) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        callType = EaseCallType.SINGLE_VOICE_CALL;
                                        EaseCallKit.getInstance().setCallType(EaseCallType.SINGLE_VOICE_CALL);
                                        EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
                                        changeVideoVoiceState();
                                    }

                                });
                            }
                        } else if (TextUtils.equals(answerEvent.result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                            timehandler.stopTime();
                            sendCmdMsg(callEvent, username);
                        }
                        break;
                    case CALL_INVITE:
                        //An audio transfer event was received.
                        EaseCallInviteEventEase inviteEvent = (EaseCallInviteEventEase) event;
                        if (inviteEvent.type == EaseCallType.SINGLE_VOICE_CALL) {
                            callType = EaseCallType.SINGLE_VOICE_CALL;
                            EaseCallKit.getInstance().setCallType(EaseCallType.SINGLE_VOICE_CALL);
                            EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(callType);
                            if (mRtcEngine != null) {
                                mRtcEngine.disableVideo();
                            }
                            changeVideoVoiceState();
                        }
                        break;
                    case CALL_CONFIRM_RING:
                        break;
                    case CALL_CONFIRM_CALLEE:
                        EaseCallConfirmCallEvent confirmEvent = (EaseCallConfirmCallEvent) event;
                        String deviceId = confirmEvent.calleeDevId;
                        String result = confirmEvent.result;
                        timehandler.stopTime();
                        //is self device
                        if (TextUtils.equals(deviceId, EaseCallKit.deviceId)) {
                            //The received arbitration is an answer
                            if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                                EaseCallKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                                joinChannel();
                                makeOngoingStatus();
                            } else if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                                exitChannel();
                            }
                        } else {
                            exitChannel();
                        }
                        break;
                }
            }
        });

        EaseCallLiveDataBus.get().with(EaseCallKitUtils.UPDATE_USERINFO, EaseCallUserInfo.class).observe(this, userInfo -> {
            if (userInfo != null) {
                if (TextUtils.equals(userInfo.getUserId(), username)) {
                    //Update local avatar nicknames
                    EaseCallKit.getInstance().getCallKitConfig().setUserInfo(username, userInfo);
                    updateUserInfo();
                }
            }
        });
    }


    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MAKE_SIGNAL_VOICE: // 1V1 voice call
                    sendInviteeMsg(username, EaseCallType.SINGLE_VOICE_CALL);
                    break;
                case MSG_MAKE_SIGNAL_VIDEO: // 1V1 video call
                    sendInviteeMsg(username, EaseCallType.SINGLE_VIDEO_CALL);
                    break;
                case MSG_RELEASE_HANDLER:
                    //stop event
                    handler.removeMessages(MSG_MAKE_SIGNAL_VOICE);
                    handler.removeMessages(MSG_MAKE_SIGNAL_VIDEO);
                    handler.removeMessages(MSG_RELEASE_HANDLER);
                    break;
                default:
                    break;
            }
        }
    };


    private void sendInviteeMsg(String username, EaseCallType callType) {
        //update nickname image
        notifyUserToUpdateUserInfo(username);

        mConfirmRing = false;
        final ChatMessage message;
        if (callType == EaseCallType.SINGLE_VIDEO_CALL) {
            message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invite_you_for_video_call), username);
        } else {
            message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invite_you_for_audio_call), username);
        }
        setMessageAttr(message, username, callType, EaseCallAction.CALL_INVITE);
        ChatClient.getInstance().chatManager().sendMessage(message);
    }

    private void setMessageAttr(ChatMessage message, String username, EaseCallType callType, EaseCallAction callInvite) {

        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, callInvite.state);
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

        //add push attr
        JSONObject extObject = new JSONObject();
        try {
            EaseCallType type = EaseCallKit.getInstance().getCallType();
            if (type == EaseCallType.SINGLE_VOICE_CALL) {
                String info = getApplication().getString(R.string.ease_call_alert_request_voice, ChatClient.getInstance().getCurrentUser());
                extObject.putOpt("em_push_title", info);
                extObject.putOpt("em_push_content", info);
            } else {
                String info = getApplication().getString(R.string.ease_call_alert_request_video, ChatClient.getInstance().getCurrentUser());
                extObject.putOpt("em_push_title", info);
                extObject.putOpt("em_push_content", info);
            }
            extObject.putOpt("isRtcCall", true);
            extObject.putOpt("callType", type.code);
            extObject.putOpt("em_push_type", "voip");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        message.setAttribute("em_apns_ext", extObject);
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

        if (EaseCallKit.getInstance().getCallID() == null) {
            EaseCallKit.getInstance().setCallID(EaseCallKitUtils.getRandomString(10));
        }
        message.setAttribute(EaseCallMsgUtils.CLL_ID, EaseCallKit.getInstance().getCallID());

        message.setAttribute(EaseCallMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
        message.setAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, EaseCallMsgUtils.CALL_MSG_INFO);
        message.setAttribute(EaseCallMsgUtils.CALL_COST_TIME, mBinding.chronometer.getText().toString());

        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                if (listener != null) {
                    listener.onInViteCallMessageSent();
                }
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
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


    private void sendCmdMsg(EaseCallBaseEvent event, String username) {
        EaseCallKit.getInstance().sendCmdMsg(event, username, new CallBack() {
            @Override
            public void onSuccess() {
                if (event.callAction == EaseCallAction.CALL_CANCEL) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boolean cancel = ((EaseCallCallCancelEvent) event).cancel;
                            if (cancel) {
                                resetState();
                                if (listener != null) {
                                    // cancel call
                                    listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonCancel, 0);
                                }
                            } else {
                                //show redial view
                                showRedialView();
                                if (listener != null) {
                                    // remote no response
                                    listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse, 0);
                                }

                            }
                        }
                    });
                } else if (event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE) {
                    //Exit channel without on state
                    if (!TextUtils.equals(((EaseCallConfirmCallEvent) event).result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                        String result = ((EaseCallConfirmCallEvent) event).result;
                        //remote refused call
                        if (TextUtils.equals(result, EaseCallMsgUtils.CALL_ANSWER_REFUSE)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null) {
                                        listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRefuse, 0);
                                    }
                                }
                            });
                            //show recall view
                            showRedialView();
                        } else {
                            resetState();
                        }
                    }
                } else if (event.callAction == EaseCallAction.CALL_ANSWER) {
                    //After the reply, start the timer and wait for the arbitration timeout
                    timehandler.startTime();
                }
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
                if (listener != null) {
                    listener.onCallError(EaseCallError.IM_ERROR, code, error);
                }
                if (event.callAction == EaseCallAction.CALL_CANCEL) {
                    resetState();
                } else if (event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE) {
                    //Exit channel without on state
                    if (!TextUtils.equals(((EaseCallConfirmCallEvent) event).result, EaseCallMsgUtils.CALL_ANSWER_ACCEPT)) {
                        resetState();
                    }
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
    }

    //recall view
    private void showRedialView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                leaveChannel();
                EaseCallAudioControl.getInstance().stopPlayRing();
                isOngoingCall = false;
                mBinding.btnCallFloat.setVisibility(View.GONE);
                mBinding.btnSwitchCamera.setVisibility(View.GONE);
                mBinding.cslMuteSmallview.setVisibility(View.GONE);
                mBinding.localSurfaceLayout.setVisibility(View.GONE);
                mBinding.oppositeSurfaceLayout.setVisibility(View.GONE);
                mBinding.llVoiceCallingControl.setVisibility(View.GONE);
                mBinding.llVoiceCallingHead.setVisibility(View.GONE);
                mBinding.llVideoCallingOutAndOngoingControl.setVisibility(View.GONE);
                setImage(EaseCallSingleBaseActivity.this, mBinding.ivAvatar, headUrl);
                mBinding.groupUseInfo.setVisibility(View.VISIBLE);
                mBinding.llVideoCloseControl.setVisibility(View.VISIBLE);
                String userHeadImage = EaseCallKitUtils.getUserHeadImage(ChatClient.getInstance().getCurrentUser());
                setViewGaussianBlur(mBinding.rootLayout, userHeadImage);
                mBinding.tvCallState.setText(getString(R.string.ease_call_no_answer));

                if (isFloatWindowShowing()) {
                    EaseCallFloatWindow.getInstance(getApplicationContext()).dismiss();
                }
                insertCancelMessageToLocal();
                //reset state
                EaseCallKit.getInstance().setCallState(EaseCallState.CALL_IDLE);
                EaseCallKit.getInstance().setCallID(null);
            }
        });
    }

    private class TimeHandler extends Handler {
        private final int MSG_TIMER = 0;
        private int timePassed = 0;
        private String passedTime;

        public TimeHandler() {
        }

        public void startTime() {
            timePassed = 0;
            sendEmptyMessageDelayed(MSG_TIMER, 1000);
        }

        public String getPassedTime() {
            passedTime = dateFormat.format(timePassed * 1000);
            return passedTime;
        }

        public void stopTime() {
            removeMessages(MSG_TIMER);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TIMER) {
                timePassed++;
                Log.e("TAG", "TimeHandler timePassed: " + timePassed);
                String time = dateFormat.format(timePassed * 1000);

                long intervalTime;
                EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
                if (callKitConfig != null) {
                    intervalTime = callKitConfig.getCallTimeOut() * 1000;
                } else {
                    intervalTime = EaseCallMsgUtils.CALL_INVITE_INTERVAL;
                }
                if (timePassed * 1000 == intervalTime) {
                    //invite call time out
                    timehandler.stopTime();
                    if (isInComingCall) {
                        //The called party waiting for the arbitration message timed out(被叫等待仲裁消息超时)
                        showRedialView();
                        if (listener != null) {
                            //remote no response
                            listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse, 0);
                        }
                    } else {
                        EaseCallCallCancelEvent cancelEvent = new EaseCallCallCancelEvent();
                        cancelEvent.cancel = false;
                        cancelEvent.remoteTimeout = true;
                        cancelEvent.callId = EaseCallKit.getInstance().getCallID();

                        //The sender fails to connect due to timeout
                        sendCmdMsg(cancelEvent, username);
                    }
                }
                sendEmptyMessageDelayed(MSG_TIMER, 1000);
                return;
            }
            super.handleMessage(msg);
        }
    }

    public long getChronometerSeconds(EaseCallChronometer cmt) {
        if (cmt == null) {
            EMLog.e(TAG, "MyChronometer is null, can not get the cost seconds!");
            return 0;
        }
        return cmt.getCostSeconds();
    }


    private void loadHeadImage() {
        if (EaseCallKit.getInstance().getCallType() == EaseCallType.SINGLE_VIDEO_CALL) {
            setImage(this, mBinding.ivAvatar, headUrl);
        } else {
            setImage(this, mBinding.ivAvatarVoice, headUrl);
        }
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
                    public void onUserAccount(EaseUserAccount userAccount) {
                        processOnUserAccount(userAccount);
                    }

                    @Override
                    public void onSetUserAccountError(int error, String errorMsg) {
                        EMLog.e(TAG, "onRemoteUserJoinChannel error:" + error + "  errorMsg:" + errorMsg);
                    }
                });
            }
        } else {
            EaseUserAccount userAccount = new EaseUserAccount(uid, userName);
            processOnUserAccount(userAccount);
        }
    }

    private void processOnUserAccount(EaseUserAccount userAccount) {
        if (userAccount != null) {
            inChannelAccounts.put(userAccount.getUid(), userAccount);
            notifyUserToUpdateUserInfo(userAccount.getUserName());
        }
        updateUserInfo();
    }

    private void notifyUserToUpdateUserInfo(String username) {
        if (listener != null && !TextUtils.isEmpty(username)) {
            listener.onUserInfoUpdate(username);
        }
    }

    private void updateUserInfo() {
        //update local user nickname and image
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                headUrl = EaseCallKitUtils.getUserHeadImage(username);
                loadHeadImage();
                mBinding.tvNickVoice.setText(EaseCallKitUtils.getUserNickName(username));
            }
        });
    }


    private void resetState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EaseCallAudioControl.getInstance().stopPlayRing();
                isOngoingCall = false;

                //Note that it cannot be placed directly in the life cycle function, so as to avoid the impact of the previous instance's delayed release on this instance
                //reset state
                releaseHandler();
                EaseCallKit.getInstance().setCallState(EaseCallState.CALL_IDLE);
                EaseCallKit.getInstance().setCallID(null);
                EaseCallKit.getInstance().releaseCall();
                RtcEngine.destroy();

                finish();
            }
        });
    }

    void exitChannel() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                leaveChannel();
                EMLog.i(TAG, "exit channel channelName: " + channelName);
                if (isFloatWindowShowing()) {
                    EaseCallFloatWindow.getInstance(getApplicationContext()).dismiss();
                }else{
                    EaseCallFloatWindow.getInstance().resetCurrentInstance();
                }
                insertCancelMessageToLocal();
                resetState();
            }
        });

    }

    private void insertCancelMessageToLocal() {
        final ChatMessage message = ChatMessage.createTxtSendMessage(getApplicationContext().getString(R.string.ease_call_invited_to_make_multi_party_call), username);
        message.setAttribute(EaseCallMsgUtils.CALL_ACTION, EaseCallAction.CALL_CANCEL.state);
        message.setAttribute(EaseCallMsgUtils.CALL_CHANNELNAME, channelName);
        message.setAttribute(EaseCallMsgUtils.CALL_TYPE, callType.code);
        message.setAttribute(EaseCallMsgUtils.CALL_MSG_TYPE, EaseCallMsgUtils.CALL_MSG_INFO);
        message.setAttribute(EaseCallMsgUtils.CALL_COST_TIME, dateFormat.format(getChronometerSeconds(mBinding.chronometer) * 1000));
        Conversation conversation = ChatClient.getInstance().chatManager().getConversation(username);
        if (conversation != null) {
            conversation.insertMessage(message);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkFloatIntent(intent);
    }

    private void checkFloatIntent(Intent intent) {
        //Prevent the activity from being started in the background to the foreground, causing the Window to still exist
        if (isFloatWindowShowing()) {
            EaseCallFloatWindow.SingleCallInfo callInfo = EaseCallFloatWindow.getInstance().getSingleCallInfo();
            if (callInfo != null) {
                remoteUId = callInfo.remoteUid;
                changeFlag = callInfo.changeFlag;
                isCameraFront = callInfo.isCameraFront;
                idInLocalSurfaceLayout = -1;
                idInOppositeSurfaceLayout = -1;
                if (EaseCallKit.getInstance().getCallState() == EaseCallState.CALL_ANSWERED) {
                    if (changeFlag && remoteUId != 0) {
                        updateOppositeSurfaceLayoutUid(remoteUId);
                        updateLocalSurfaceLayoutUid(0);
                    } else {
                        updateOppositeSurfaceLayoutUid(0);
                        updateLocalSurfaceLayoutUid(remoteUId);
                    }
                } else {
                    if (!isInComingCall) {
                        updateOppositeSurfaceLayoutUid(0);
                    }
                }
                changeCameraDirection(isCameraFront);
            }
            long totalCostSeconds = EaseCallFloatWindow.getInstance().getTotalCostSeconds();
            mBinding.chronometer.setBase(SystemClock.elapsedRealtime() - totalCostSeconds * 1000);
            mBinding.chronometer.start();
        }
        EaseCallFloatWindow.getInstance().dismiss();
    }

    /**
     * do show float window
     */
    @Override
    public void doShowFloatWindow() {
        super.doShowFloatWindow();
        if (mBinding.chronometer != null) {
            EaseCallFloatWindow.getInstance().setCostSeconds(mBinding.chronometer.getCostSeconds());
        }
        EaseCallFloatWindow.getInstance().show();
        boolean surface = true;
        if (isInComingCall && EaseCallKit.getInstance().getCallState() != EaseCallState.CALL_ANSWERED) {
            surface = false;
        }
        EaseCallFloatWindow.getInstance().update(!changeFlag, headUrl, 0, remoteUId, surface);
        EaseCallFloatWindow.getInstance().setCameraDirection(isCameraFront, changeFlag);
        moveTaskToBack(false);
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


    private void startCount() {
        if (mBinding.chronometer != null) {
            mBinding.chronometer.setBase(SystemClock.elapsedRealtime());
            mBinding.chronometer.start();
        }
    }

    private void stopCount() {
        if (mBinding.chronometer != null) {
            mBinding.chronometer.stop();
        }
    }

    /**
     * stop event loop
     */
    protected void releaseHandler() {
        if(handler!=null) {
            handler.sendEmptyMessage(MSG_RELEASE_HANDLER);
        }
        if (timehandler != null) {
            timehandler.stopTime();
        }
    }

    @Override
    protected void onDestroy() {
        EMLog.d(TAG, "onDestroy");
        super.onDestroy();
        releaseHandler();
        if (inChannelAccounts != null) {
            inChannelAccounts.clear();
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
    public void onBackPressed() {
        exitChannelDisplay();
    }


    /**
     * Whether to exit the call prompt box
     */
    public void exitChannelDisplay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EaseCallSingleBaseActivity.this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(EaseCallSingleBaseActivity.this, R.layout.ease_call_activity_exit_channel, null);
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
                stopCount();
                if (remoteUId == 0) {
                    EaseCallCallCancelEvent cancelEvent = new EaseCallCallCancelEvent();
                    cancelEvent.callId = EaseCallKit.getInstance().getCallID();
                    sendCmdMsg(cancelEvent, username);
                } else {
                    exitChannel();
                    if (listener != null) {
                        //The reason for the end of the call is to hang up
                        long time = getChronometerSeconds(mBinding.chronometer);
                        listener.onEndCallWithReason(callType, channelName, EaseCallEndReason.EaseCallEndReasonHangup, time * 1000);
                    }
                }
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
}