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
     * The constructor
     */
    public EaseCallKitConfig() {
    }

    /**
     * Gets the defaultHeadImage path (Is the absolute path  of the local file or URL)
     * @return the defaultHeadImage path (Is the absolute path  of the local file or URL)
     */
    public String getDefaultHeadImage() {
        return defaultHeadImage;
    }

    /**
     * Sets the defaultHeadImage (Is the absolute path  of the local file or URL)
     * @param defaultHeadImage the defaultHeadImage path
     */
    public void setDefaultHeadImage(String defaultHeadImage) {
        this.defaultHeadImage = defaultHeadImage;
    }

    /**
     * Gets the userInfo
     * @return userInfo, type of Map<String, EaseCallUserInfo>
     * key:userId(AgoraChat userId)
     * value:userInfo see{@link EaseCallUserInfo}
     */
    public Map<String, EaseCallUserInfo> getUserInfoMap() {
        return userInfoMap;
    }

    /**
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
     * Gets the ring file path
     * @return the ring file path (Local file absolute path)
     */
    public String getRingFile() {
        return ringFile;
    }

    /**
     * Sets the ring file path
     * @param ringFile ring file path (Local file absolute path)
     */
    public void setRingFile(String ringFile) {
        this.ringFile = ringFile;
    }

    /**
     * Gets the call timeout
     * @return the call timeout
     */
    public long getCallTimeOut() {
        return callTimeOut;
    }

    /**
     * Sets the call timeout (unit: MS , Default 30s)
     * @param callTimeOut the call timeout
     */
    public void setCallTimeOut(long callTimeOut) {
        this.callTimeOut = callTimeOut;
    }

    /**
     * Gets the agora appId
     * @return the agora appId
     */
    public String getAgoraAppId() {
        return agoraAppId;
    }

    /**
     * Sets agora appId
     * @param agoraAppId agora appId
     */
    public void setAgoraAppId(String agoraAppId) {
        this.agoraAppId = agoraAppId;
    }

    /**
     * Whether to enable the RTC Token
     * @return Whether to enable the RTC Token
     * (default) false:disable
     * true:enable
     */
    public boolean isEnableRTCToken() {
        return enableRTCToken;
    }

    /**
     * Set Whether to enable the RTC Token (need the agora control to control true or false, the default is off)
     * @param enableRTCToken Whether to enable the RTC Token
     *                       (default) false:disable
     *                       true:enable
     */
    public void setEnableRTCToken(boolean enableRTCToken) {
        this.enableRTCToken = enableRTCToken;
    }

}
