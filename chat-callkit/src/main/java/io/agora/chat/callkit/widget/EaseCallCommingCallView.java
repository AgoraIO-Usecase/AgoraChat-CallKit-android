package io.agora.chat.callkit.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.chat.callkit.R;
import io.agora.chat.callkit.general.EaseCallType;
import io.agora.chat.callkit.utils.EaseCallImageUtil;
import io.agora.chat.callkit.utils.EaseCallKitUtils;

public class EaseCallCommingCallView extends FrameLayout {

    private static final String TAG = EaseCallCommingCallView.class.getSimpleName();

    private ImageButton mBtnReject;
    private ImageButton mBtnPickup;
    private TextView mInviterName;
    private TextView mCallState;
    private OnActionListener mOnActionListener;
    private EaseCallImageView avatar_view;
    private ImageView ivVidicon;
    private Bitmap headBitMap;
    private String headUrl;
    private RelativeLayout mSurfaceLayout;
    private ImageButton mSwitchCamera;

    public EaseCallCommingCallView(@NonNull Context context) {
        this(context, null);
    }

    public EaseCallCommingCallView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EaseCallCommingCallView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.ease_call_activity_comming_call, this);
        mBtnReject = findViewById(R.id.btn_refuse_video_called);
        mBtnPickup = findViewById(R.id.btn_answer_video_called);
        mInviterName = findViewById(R.id.tv_nick);
        mCallState = findViewById(R.id.tv_call_state);
        mSwitchCamera = findViewById(R.id.btn_switch_camera);
        avatar_view = findViewById(R.id.iv_avatar);
        ivVidicon = findViewById(R.id.iv_vidicon_video_called);
        mSurfaceLayout = findViewById(R.id.opposite_surface_layout);
        mBtnReject.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnActionListener != null) {
                    mOnActionListener.onRejectClick(v);
                }
            }
        });

        mBtnPickup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnActionListener != null) {
                    mOnActionListener.onPickupClick(v);
                }
            }
        });
        mSwitchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnActionListener != null) {
                    mOnActionListener.onSwitchCamerClick(v);
                }
            }
        });
        ivVidicon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnActionListener != null) {
                    mOnActionListener.onMuteVideoClick(v);
                }
            }
        });
    }

    public void setInviteInfo(String username, String groupId, EaseCallType callType) {
        if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
            mCallState.setText(getContext().getString(R.string.ease_call_video_call));
            mSwitchCamera.setVisibility(VISIBLE);
        } else if (callType == EaseCallType.CONFERENCE_VOICE_CALL) {
            mCallState.setText(getContext().getString(R.string.ease_call_voice_call));
            mSwitchCamera.setVisibility(GONE);
        }
        if (!TextUtils.isEmpty(groupId)) {
            mInviterName.setText(groupId);
        }
        headUrl = EaseCallKitUtils.getUserHeadImage(username);
        //加载头像图片
        loadHeadImage();
    }

    /**
     * 加载用户配置头像
     *
     * @return
     */
    private void loadHeadImage() {
        post(new Runnable() {
            @Override
            public void run() {
                EaseCallImageUtil.setImage(getContext(), avatar_view, headUrl);
            }
        });
    }

    public void setVideoView(View view) {
        mSurfaceLayout.removeAllViews();
        mSurfaceLayout.addView(view);
    }

    public void removeVideoView(View view){
        mSurfaceLayout.removeAllViews();
    }

    public void setOnActionListener(OnActionListener listener) {
        this.mOnActionListener = listener;
    }

    public interface OnActionListener {
        void onRejectClick(View v);

        void onPickupClick(View v);

        void onMuteVideoClick(View v);

        void onSwitchCamerClick(View v);
    }
}

