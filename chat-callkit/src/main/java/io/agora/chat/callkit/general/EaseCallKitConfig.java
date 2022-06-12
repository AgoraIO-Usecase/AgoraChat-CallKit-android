package io.agora.chat.callkit.general;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.agora.chat.callkit.bean.EaseCallUserInfo;


/**
 * About callKit user configuration options
 */
public class EaseCallKitConfig {
    private String defaultHeadImage;
    private Map<String, EaseCallUserInfo> userInfoMap = new HashMap<>();
    private String ringFile;
    private String agoraAppId;
    private long callTimeOut = 30;
    private boolean enableRTCToken = false;

    /**
     * \~chinese
     * 构造函数
     * <p>
     * \~english
     * The constructor
     */
    public EaseCallKitConfig() {
    }

    /**
     * \~chinese
     * 获取默认用户图像路径(为本地文件绝对路径或者url)
     * @return 默认用户图像路径 (为本地文件绝对路径或者url)
     * <p>
     * \~english
     * Gets the defaultHeadImage path (Is the absolute path  of the local file or URL)
     * @return the defaultHeadImage path (Is the absolute path  of the local file or URL)
     */
    public String getDefaultHeadImage() {
        return defaultHeadImage;
    }

    /**
     * \~chinese
     * 设置默认用户图像
     *
     * @param defaultHeadImage 默认用户图像路径 (为本地文件绝对路径或者url)
     * <p>
     * \~english
     * Sets the defaultHeadImage (Is the absolute path  of the local file or URL)
     * @param defaultHeadImage the defaultHeadImage path
     */
    public void setDefaultHeadImage(String defaultHeadImage) {
        this.defaultHeadImage = defaultHeadImage;
    }

    /**
     * \~chinese
     * 获取用户信息
     *
     * @return 用户信息, Map<String, EaseCallUserInfo>类型
     * key:用户名(AgoraChat userid)
     * value:用户信息 参考{@link EaseCallUserInfo }
     * <p>
     * \~english
     * Gets the userInfo
     * @return userInfo, type of Map<String, EaseCallUserInfo>
     * key:userId(AgoraChat userId)
     * value:userInfo see{@link EaseCallUserInfo}
     */
    public Map<String, EaseCallUserInfo> getUserInfoMap() {
        return userInfoMap;
    }

    /**
     * \~chinese
     * 设置用户信息
     *
     * @param userMap 用户信息,Map<String, EaseCallUserInfo>类型
     *                key:用户名(AgoraChat userId)
     *                value:用户信息 参考{@link EaseCallUserInfo }
     * <p>
     * \~english
     * Sets the userInfo
     * @param userMap userInfo,type of Map<String, EaseCallUserInfo>
     *                key:userId(AgoraChat userId)
     *                value:userInfo see{@link EaseCallUserInfo}
     */
    public void setUserInfoMap(Map<String, EaseCallUserInfo> userMap) {
        userInfoMap.clear();
        if (userMap != null && userMap.size() > 0) {
            Set<String> userSet = userMap.keySet();
            for (String userId : userSet) {
                EaseCallUserInfo userInfo = userMap.get(userId);
                if (userInfo != null) {
                    EaseCallUserInfo newUserInfo = new EaseCallUserInfo(userInfo.getNickName(), userInfo.getHeadImage());
                    userInfoMap.put(userId, newUserInfo);
                } else {
                    userInfoMap.put(userId, null);
                }
            }
        }
    }

    /**
     * \~chinese
     * 设置用户信息
     * @param userName 用户名(AgoraChat userId)
     * @param userInfo 用户信息,EaseCallUserInfo类型，参考{@link EaseCallUserInfo }
     * <p>
     * \~english
     * Sets the userInfo
     * @param userName userId(AgoraChat userId)
     * @param userInfo userInfo,type of EaseCallUserInfo,see{@link EaseCallUserInfo}
     */
    public void setUserInfo(String userName, EaseCallUserInfo userInfo) {
        if (userInfoMap == null) {
            userInfoMap = new HashMap<>();
        }
        userInfoMap.put(userName, userInfo);
    }

    /**
     * \~chinese
     * 获取铃声文件路径
     * @return 铃声文件路径(本地文件绝对路径)
     * <p>
     * \~english
     * Gets the ring file path
     * @return the ring file path (Local file absolute path)
     */
    public String getRingFile() {
        return ringFile;
    }

    /**
     * \~chinese
     * 设置铃声文件路径
     * @param ringFile 铃声文件路径(本地文件绝对路径)
     * <p>
     * \~english
     * Sets the ring file path
     * @param ringFile ring file path (Local file absolute path)
     */
    public void setRingFile(String ringFile) {
        this.ringFile = ringFile;
    }

    /**
     * \~chinese
     * 获取呼叫超时时间
     * @return 呼叫超时时间
     * <p>
     * \~english
     * Gets the call timeout
     * @return the call timeout
     */
    public long getCallTimeOut() {
        return callTimeOut;
    }

    /**
     * \~chinese
     * 设置呼叫超时时间 (单位s 默认30s)
     * @param callTimeOut 呼叫超时时间
     * <p>
     * \~english
     * Sets the call timeout (unit: MS , Default 30s)
     * @param callTimeOut the call timeout
     */
    public void setCallTimeOut(long callTimeOut) {
        this.callTimeOut = callTimeOut;
    }

    /**
     * \~chinese
     * 获取声网appId
     * @return 声网appId
     * <p>
     * \~english
     * Gets the agora appId
     * @return the agora appId
     */
    public String getAgoraAppId() {
        return agoraAppId;
    }

    /**
     * \~chinese
     * 设置声网appId
     * @param agoraAppId 声网appId
     * <p>
     * \~english
     * Sets agora appId
     * @param agoraAppId agora appId
     */
    public void setAgoraAppId(String agoraAppId) {
        this.agoraAppId = agoraAppId;
    }

    /**
     * \~chinese
     * 是否启用RTC token
     * @return 是否启用RTC token
     * (默认) false:不启用
     * true:启用
     * <p>
     * \~english
     * Whether to enable the RTC Token
     * @return Whether to enable the RTC Token
     * (default) false:disable
     * true:enable
     */
    public boolean isEnableRTCToken() {
        return enableRTCToken;
    }

    /**
     * \~chinese
     * 设置是否启用RTC token校验。(需要声网控制台去控制开关,默认为关闭)
     *
     * @param enableRTCToken 是否启用RTC token校验
     *                       (默认) false:不启用
     *                       true:启用
     * <p>
     * \~english
     * Set Whether to enable the RTC Token (need the agora control to control true or false, the default is off)
     * @param enableRTCToken Whether to enable the RTC Token
     *                       (default) false:disable
     *                       true:enable
     */
    public void setEnableRTCToken(boolean enableRTCToken) {
        this.enableRTCToken = enableRTCToken;
    }

}
