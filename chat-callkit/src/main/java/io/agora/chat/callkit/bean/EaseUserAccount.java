package io.agora.chat.callkit.bean;

/**
 * User account information
 * uid      Join the uId of the AgoraRTC channel
 * userName  agoraChat Id corresponding to uid
 */
public class EaseUserAccount {
    private int uid;
    private String userName;

    public EaseUserAccount(){
    }

    public EaseUserAccount(int uid,String userName){
        this.uid = uid;
        this.userName = userName;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
