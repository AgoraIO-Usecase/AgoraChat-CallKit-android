package io.agora.chat.callkit.general;


public enum EaseCallEndReason {
    /**
     * \~chinese
     * 正常挂断
     *
     * \~english
     * Normal hang up
     */
    EaseCallEndReasonHangup(0),
    /**
     * \~chinese
     * 自己取消通话
     *
     * \~english
     * cancel by self
     */
    EaseCallEndReasonCancel(1),
    /**
     * \~chinese
     * 对方取消通话
     *
     * \~english
     * cancel by remote
     */
    EaseCallEndReasonRemoteCancel(2),
    /**
     * \~chinese
     * 拒绝接听
     *
     * \~english
     * Refuse to answer
     */
    EaseCallEndReasonRefuse(3),
    /**
     * \~chinese
     * 忙线中
     *
     * \~english
     * The line is busy
     */
    EaseCallEndReasonBusy(4),
    /**
     * \~chinese
     * 自己无响应
     *
     * \~english
     * No response from self
     */
    EaseCallEndReasonNoResponse(5),
    /**
     * \~chinese
     * 对端无响应
     *
     * \~english
     * no response from remote
     */
    EaseCallEndReasonRemoteNoResponse(6),
    /**
     * \~chinese
     * 在其他设备拒绝
     *
     * \~english
     * refused in another device
     */
    EaseCallEndReasonHandleOnOtherDeviceRefused(7),
    /**
     * \~chinese
     * 在其他设备同意
     *
     * \~english
     * agreed in another device
     */
    EaseCallEndReasonHandleOnOtherDeviceAgreed(8);


    /**
     * \~chinese
     * 状态码
     *
     * \~english
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
