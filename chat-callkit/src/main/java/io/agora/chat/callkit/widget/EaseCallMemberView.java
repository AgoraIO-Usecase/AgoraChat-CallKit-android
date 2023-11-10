package io.agora.chat.callkit.widget;

import static io.agora.chat.callkit.utils.EaseCallImageUtil.setImage;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import io.agora.chat.callkit.R;
import io.agora.chat.callkit.bean.EaseUserAccount;
import io.agora.chat.callkit.general.EaseCallType;
import io.agora.chat.callkit.utils.EaseCallKitUtils;


public class EaseCallMemberView extends RelativeLayout {

    private Context context;
    private RelativeLayout surfaceViewLayout;
    private EaseCallImageView avatarView;
    private ImageView audioOffInVideo;
    private ImageView audioOffInVoice;
    private ImageView talkingView;
    private TextView nameView;
    private SurfaceView surfaceView;
    private ValueAnimator animator;

    private EaseUserAccount userInfo;

    private boolean isShowVideo = false;
    private boolean isAudioOff = false;
    private boolean isDesktop = false;
    private boolean isFullScreenMode = false;
    private String streamId;
    private Bitmap headBitMap;
    private String headUrl;
    private EaseCallMemberView memberView;
    private LinearLayout loading_dialog;
    private boolean speakActivated;
    private boolean isCameraFront;
    private ImageView ivVidicon;
    private EaseCallType callType;


    public EaseCallMemberView(Context context) {
        this(context, null);
    }

    public EaseCallMemberView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EaseCallMemberView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.ease_call_avtivity_call_member, this);
        init();
    }

    private void init() {
        surfaceViewLayout = findViewById(R.id.item_surface_layout);
        avatarView = (EaseCallImageView) findViewById(R.id.img_call_avatar);
        audioOffInVideo = (ImageView) findViewById(R.id.mic_mute_in_video);
        audioOffInVoice = (ImageView) findViewById(R.id.mic_mute_in_voice);
        talkingView = (ImageView) findViewById(R.id.icon_talking);
        nameView = (TextView) findViewById(R.id.text_name);
        loading_dialog = findViewById(R.id.member_loading);
        ivVidicon = findViewById(R.id.iv_vidicon);

    }

    public void setLoading(Boolean loading) {
        if (loading) {
            loading_dialog.setVisibility(VISIBLE);
        } else {
            loading_dialog.setVisibility(GONE);
        }
    }

    public void addSurfaceView(SurfaceView surfaceView) {
        surfaceViewLayout.removeAllViews();
        surfaceViewLayout.addView(surfaceView);
        this.surfaceView = surfaceView;
    }

    public void setUserInfo(EaseUserAccount info) {
        userInfo = info;
        updateUserInfo();
    }

    public void updateUserInfo() {
        if (userInfo != null) {
            nameView.setVisibility(VISIBLE);
            nameView.setText(EaseCallKitUtils.getUserNickName(userInfo.getUserName()));
            headUrl = EaseCallKitUtils.getUserHeadImage(userInfo.getUserName());
            if (headUrl != null) {
                loadHeadImage();
            } else {
                avatarView.setImageResource(R.drawable.call_memberview_background);
            }
        }
    }

    public EaseUserAccount getUserInfo() {
        return userInfo;
    }

    public String getUserAccount() {
        if (userInfo != null) {
            return userInfo.getUserName();
        }
        return null;
    }

    public int getUserId() {
        if (userInfo != null) {
            return userInfo.getUid();
        }
        return 0;
    }

    public SurfaceView getSurfaceView() {
        return this.surfaceView;
    }

    /**
     * Update mute status
     */
    public void setAudioOff(boolean state) {
        if (!state) {
            setVoiceOnlineImageState(false);
        }
        isAudioOff = state;
        if (isFullScreenMode) {
            return;
        }
        if (isAudioOff) {
            if (callType == EaseCallType.CONFERENCE_VOICE_CALL) {
                audioOffInVoice.setVisibility(VISIBLE);
                audioOffInVideo.setVisibility(GONE);
                audioOffInVoice.setImageResource(R.drawable.ease_call_mic_off);
            } else {
                audioOffInVoice.setVisibility(GONE);
                audioOffInVideo.setVisibility(VISIBLE);
                audioOffInVideo.setImageResource(R.drawable.ease_call_mic_off_small);
            }
        } else {
            if (callType == EaseCallType.CONFERENCE_VOICE_CALL) {
                audioOffInVoice.setVisibility(GONE);
                audioOffInVideo.setVisibility(GONE);
            } else {
                audioOffInVoice.setVisibility(GONE);
                audioOffInVideo.setVisibility(GONE);
            }
        }
    }

    public boolean getAudioOff() {
        return isAudioOff;
    }

    public void setSpeak(boolean speak, int volume) {
        if (speak && volume > 3) {
            setVoiceOnlineImageState(true);
        } else {
            setVoiceOnlineImageState(false);
        }

    }

    public boolean isAudioOff() {
        return isAudioOff;
    }


    /**
     * Update video display status
     */
    public void showVideo(boolean show) {
        isShowVideo = show;
        if (isShowVideo) {
            avatarView.setVisibility(View.GONE);
            surfaceViewLayout.setVisibility(VISIBLE);
            ivVidicon.setVisibility(GONE);
        } else {
            avatarView.setVisibility(View.VISIBLE);
            surfaceViewLayout.setVisibility(GONE);
            ivVidicon.setVisibility(VISIBLE);
        }
        if(callType==EaseCallType.CONFERENCE_VOICE_CALL||callType==EaseCallType.SINGLE_VOICE_CALL) {
            ivVidicon.setVisibility(GONE);
        }
    }

    public void setNameVisiable(int visiable) {
        nameView.setVisibility(visiable);
    }

    public void setVidiconVisiable(int visiable) {
        ivVidicon.setVisibility(visiable);
    }

    public boolean isShowVideo() {
        return isShowVideo;
    }

    public void setDesktop(boolean desktop) {
        isDesktop = desktop;
        if (isDesktop) {
            avatarView.setVisibility(View.GONE);
        }
    }


    /**
     * Set the user of the stream corresponding to the current view, mainly used to display the other party's avatar during a voice call
     */
    public void setUsername(String username) {
        headUrl = EaseCallKitUtils.getUserHeadImage(username);
        if (headUrl != null) {
            avatarView.setImageResource(R.drawable.call_memberview_background);
        } else {
            loadHeadImage();
        }
        nameView.setText(EaseCallKitUtils.getUserNickName(username));
    }

    /**
     * Set the Stream Id displayed by the current control
     */
    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setFullScreen(boolean fullScreen) {
        isFullScreenMode = fullScreen;

        if (fullScreen) {
            talkingView.setVisibility(GONE);
            nameView.setVisibility(GONE);
            audioOffInVideo.setVisibility(GONE);
        } else {
            nameView.setVisibility(VISIBLE);
            if (isAudioOff) {
                audioOffInVideo.setVisibility(VISIBLE);
            }
        }
    }

    public boolean isFullScreen() {
        return isFullScreenMode;
    }


    /**
     * Load user profile avatar
     */
    private void loadHeadImage() {
        setImage(getContext(), avatarView, headUrl);
    }

    public void setSpeakActivated(boolean activated) {
        this.speakActivated = activated;
    }

    public boolean isSpeakActivated() {
        return speakActivated;
    }

    public void setCameraDirectionFront(boolean isFront) {
        this.isCameraFront = isFront;
    }

    public boolean isCameraDirectionFront() {
        return this.isCameraFront;
    }

    public void setVoiceOnlineImageState(boolean show) {
        if (show) {
            avatarView.setBorderWidth(dp2px(3));
            avatarView.setBorderColor(Color.GREEN);
        } else {
            avatarView.setBorderWidth(0);
            avatarView.setBorderColor(Color.TRANSPARENT);
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void setCallType(EaseCallType callType) {
        this.callType = callType;
        if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
            setVoiceOnlineImageState(false);
            //left
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) nameView.getParent());
            constraintSet.connect(nameView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, dp2px(10));
            constraintSet.applyTo((ConstraintLayout) nameView.getParent());
        } else {
            ivVidicon.setVisibility(View.GONE);
            setVoiceOnlineImageState(false);
            //center
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) nameView.getParent());
            constraintSet.connect(nameView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            constraintSet.connect(nameView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            constraintSet.connect(nameView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            constraintSet.applyTo((ConstraintLayout) nameView.getParent());
        }

    }
}

