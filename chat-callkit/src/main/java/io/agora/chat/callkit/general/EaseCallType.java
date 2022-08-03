package io.agora.chat.callkit.general;

public enum EaseCallType {
    SINGLE_VOICE_CALL(0), //1v1 voice call
    SINGLE_VIDEO_CALL(1), //1v1 video call
    CONFERENCE_VIDEO_CALL(2),//multiplayer video
    CONFERENCE_VOICE_CALL(3);//multiplayer voice

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