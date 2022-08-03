package io.agora.chat.callkit.bean;

/**
 * userinfo bean
 */
public class EaseCallUserInfo {
    private String nickName;
    private String headImage;
    private String userId;
    /**
     * The constructor
     * @param nickName user's nickname
     * @param headImage user's headImage
     */
    public  EaseCallUserInfo(String nickName,String headImage){
        this.nickName = nickName;
        this.headImage = headImage;
    }
    /**
     * The constructor
     */
    public  EaseCallUserInfo(){

    }

    /**
     * Gets the user's nickname
     * @return the user's nickname
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Sets the user's nickname
     * @param nickName the user's nickname
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * Gets the headImage path (Is the absolute path  of the local file or URL)
     * @return the headImage path
     */
    public String getHeadImage() {
        return headImage;
    }
    /**
     * Sets the headImage (Is the absolute path  of the local file or URL)
     * @param headImage the headImage path
     */
    public void setHeadImage(String headImage) {
        this.headImage = headImage;
    }

    /**
     * Gets the userId
     * @return user's id
     */
    public String getUserId() { return userId; }

    /**
     * Sets the userId
     * @param userId user's id
     */
    public void setUserId(String userId) { this.userId = userId; }

}
