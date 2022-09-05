package io.agora.chat.callkit.general;


public enum EaseCallEndReason {
    /**
     * Normal hang up
     */
    EaseCallEndReasonHangup(0),
    /**
     * cancel by self
     */
    EaseCallEndReasonCancel(1),
    /**
     * cancel by remote
     */
    EaseCallEndReasonRemoteCancel(2),
    /**
     * Refuse to answer
     */
    EaseCallEndReasonRefuse(3),
    /**
     * The line is busy
     */
    EaseCallEndReasonBusy(4),
    /**
     * No response from self
     */
    EaseCallEndReasonNoResponse(5),
    /**
     * no response from remote
     */
    EaseCallEndReasonRemoteNoResponse(6),
    /**
     * refused in another device
     */
    EaseCallEndReasonHandleOnOtherDeviceRefused(7),
    /**
     * agreed in another device
     */
    EaseCallEndReasonHandleOnOtherDeviceAgreed(8);


    /**
     * state code
     */
    public int code;

    EaseCallEndReason(int code) {
        this.code = code;
    }

    public static EaseCallEndReason getfrom(int code) {
        switch (code) {
            case 0:
                return EaseCallEndReasonHangup;
            case 1:
                return EaseCallEndReasonCancel;
            case 2:
                return EaseCallEndReasonRemoteCancel;
            case 3:
                return EaseCallEndReasonRefuse;
            case 4:
                return EaseCallEndReasonBusy;
            case 5:
                return EaseCallEndReasonNoResponse;
            case 6:
                return EaseCallEndReasonRemoteNoResponse;
            case 7:
                return EaseCallEndReasonHandleOnOtherDeviceRefused;
            case 8:
                return EaseCallEndReasonHandleOnOtherDeviceAgreed;
            default:
                return EaseCallEndReasonHangup;
        }
    }
}
