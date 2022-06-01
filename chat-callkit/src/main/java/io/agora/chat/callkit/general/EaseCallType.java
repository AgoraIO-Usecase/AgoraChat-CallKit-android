package io.agora.chat.callkit.general;

public enum EaseCallType {
    SINGLE_VOICE_CALL(0), //1v1语音通话
    SINGLE_VIDEO_CALL(1), //1v1视频通话
    CONFERENCE_VIDEO_CALL(2),   //多人视频
    CONFERENCE_VOICE_CALL(3);//多人音频

    public int code;

    EaseCallType(int code) {
        this.code = code;
    }

    public static EaseCallType getfrom(int code) {
        switch (code) {
            case 0:
                return SINGLE_VOICE_CALL;
            case 1:
                return SINGLE_VIDEO_CALL;
            case 2:
                return CONFERENCE_VIDEO_CALL;
            case 3:
                return CONFERENCE_VOICE_CALL;
            default:
                return SINGLE_VIDEO_CALL;
        }
    }
};