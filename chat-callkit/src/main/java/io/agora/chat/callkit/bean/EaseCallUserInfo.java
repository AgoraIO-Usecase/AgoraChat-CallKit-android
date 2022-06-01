package io.agora.chat.callkit.bean;

/**
 * userinfo bean
 */
public class EaseCallUserInfo {
    private String nickName;
    private String headImage;
    private String userId;
    /**
     * \~chinese
     * 构造函数
     * @param nickName 昵称
     * @param headImage 头像
     * <p>
     * \~english
     * The constructor
     * @param nickName user's nickname
     * @param headImage user's headImage
     */
    public  EaseCallUserInfo(String nickName,String headImage){
        this.nickName = nickName;
        this.headImage = headImage;
    }
    /**
     * \~chinese
     * 构造函数
     * <p>
     * \~english
     * The constructor
     */
    public  EaseCallUserInfo(){

    }

    /**
     * \~chinese
     * 获取用户昵称
     * @return 用户昵称
     * <p>
     * \~english
     * Gets the user's nickname
     * @return the user's nickname
     */
    public String getNickName() {
        return nickName;
    }
    /**
     * \~chinese
     * 设置用户昵称
     * @param nickName 用户昵称
     * <p>
     * \~english
     * Sets the user's nickname
     * @param nickName the user's nickname
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    /**
     * \~chinese
     * 获取用户图像路径(为本地文件绝对路径或者url)
     * @return 用户图像路径
     * <p>
     * \~english
     * Gets the headImage path (Is the absolute path  of the local file or URL)
     * @return the headImage path
     */
    public String getHeadImage() {
        return headImage;
    }
    /**
     * \~chinese
     * 设置用户图像
     * @param headImage 用户图像路径 (为本地文件绝对路径或者url)
     * <p>
     * \~english
     * Sets the headImage (Is the absolute path  of the local file or URL)
     * @param headImage the headImage path
     */
    public void setHeadImage(String headImage) {
        this.headImage = headImage;
    }
    /**
     * \~chinese
     * 获取用户userId
     * @return 用户useId
     * <p>
     * \~english
     * Gets the userId
     * @return user's id
     */
    public String getUserId() { return userId; }
    /**
     * \~chinese
     * 设置用户userId
     * @param userId 用户userId
     * <p>
     * \~english
     * Sets the userId
     * @param userId user's id
     */
    public void setUserId(String userId) { this.userId = userId; }
}
