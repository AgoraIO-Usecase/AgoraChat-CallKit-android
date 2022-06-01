package io.agora.chat.callkit.bean;

import org.json.JSONObject;

import io.agora.chat.callkit.general.EaseCallType;


public class EaseCallInfo {
    private String channelName;
    private String fromUser;
    private boolean isComming;
    private String callerDevId;
    private String callId;
    private EaseCallType callKitType;
    private JSONObject ext;

    public EaseCallInfo(){

    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public boolean isComming() {
        return isComming;
    }

    public void setComming(boolean comming) {
        isComming = comming;
    }

    public String getCallerDevId() {
        return callerDevId;
    }

    public void setCallerDevId(String callerDevId) {
        this.callerDevId = callerDevId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public EaseCallType getCallKitType() {
        return callKitType;
    }

    public void setCallKitType(EaseCallType callKitType) {
        this.callKitType = callKitType;
    }

    public JSONObject getExt() { return ext; }

    public void setExt(JSONObject ext) { this.ext = ext; }
}
