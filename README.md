# 实现一对一音视频通话

AgoraChatCallKit 是一套基于声网音视频服务，使用 Agora Chat 作为信令通道的开源音视频 UI 库。该库提供一对一及多人音视频通话的功能接口。同时，通过信令的交互确认，还可以保证用户在多个设备登录时，能同步处理呼叫振铃，即当用户在一台设备上处理振铃后，其他设备自动停止振铃。

本文展示如何使用 AgoraChatCallKit 快速构建实时音视频场景。

## 实现原理

使用 AgoraChatCallKit 实现音视频通话的基本流程如下：

1. 调用 `init` 初始化 AgoraChatCallKit，并调用 `setCallKitListener` 设置 AgoraChatCallKit 监听器。
2. 根据想要实现的音视频通话场景，主叫调用 `startSingleCall` 或 `startInviteMultipleCall` 发起一对一通话或多人通话的呼叫邀请。
3. 被叫在 `onReceivedCall` 中收到呼叫邀请，选择接听或拒绝呼叫。成功接听后，进入通话。
4. 结束通话时，SDK 触发 `onEndCallWithReason` 回调。

## 前提条件

开始前，请确保你的项目满足如下条件：

- Android Studio 3.5 及以上版本；
- Gradle 4.6 及以上版本；
- targetSdkVersion 30；
- minSdkVersion 21；
- Java JDK 1.8 及以上版本;
- 创建 [声网应用](https://console.agora.io/projects)
- 有效的 [Agora 账号](https://docs.agora.io/cn/Agora%20Platform/get_appid_token?platform=All%20Platforms#%E5%88%9B%E5%BB%BA-agora-%E8%B4%A6%E5%8F%B7)。
- 已[开通 Agora Chat 服务](./enable_agora_chat?platform=iOS)的 Agora 项目。
- 有一个实现了基础的实时消息功能的项目，包含用户登录登出、添加好友、创建群组等。

## 项目设置

参考如下步骤，将 AgoraChatCallKit 集成到你的项目中，并完成相关配置。

### 集成依赖库

AgoraChatCallKit 主要依赖于 `io.agora.rtc:chat-sdk:x.x.x` (1.0.5 及以上版本) 和 `io.agora.rtc:full-rtc-basic:x.x.x` (3.6.2 及以上版本)库。

AgoraChatCallKit 库可通过 Gradle 方式和源码两种方式集成。

#### Gradle 方式集成

- 在 `build.gradle` 中添加以下代码，重新 build 你的项目即可。

```java
implementation 'io.agora.rtc:chat-callkit:1.0.1'
```

#### 源码集成

- 下载 [AgoraChatCallKit 源码](https://github.com/AgoraIO-Usecase/AgoraChat-CallKit-android)；

- 引入项目：在 AndroidStudio 菜单栏选择 **File > New > Import Module > Source directory**，选择刚下载的源码目录AgoraChat-CallKit-android/chat-callkit，点击 **Finish**。

- 在 `build.gradle` 中增加以下内容，重新 build 你的项目即可。

```java
 implementation project(path: ':chat-callkit')
```

AgoraChatCallKit 中如果要修改 `chat-sdk` 和 `agora.rtc` 中版本号，可修改以下依赖:

```java
//Agora Chat SDK
implementation 'io.agora.rtc:chat-sdk:1.0.5'
//声网 RTC SDK
implementation 'io.agora.rtc:full-rtc-basic:3.6.2'
```

### 添加项目权限

根据场景需要，本库需要增加联网、麦克风、相机和悬浮窗等权限:

```java
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.hyphenate.easeim">

    <!-- 添加悬浮窗权限 -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <!-- 添加访问网络权限.-->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- 添加麦克风权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <!-- 添加相机权限 -->
    <uses-permission android:name="android.permission.CAMERA" />
    <!-- 添加任务栈权限 -->
    <uses-permission android:name="android.permission.REORDER_TASKS" />
    ...

</manifest>
```

### 添加 AgoraChatCallKit Activity

在 Manifest 文件中分别为一对一音视频通话和多人音视频通话添加 Activity，如 `CallSingleBaseActivity` (需继承自 `EaseCallSingleBaseActivity`) 和 `CallMultipleBaseActivity`（需继承自 `EaseCallMultipleBaseActivity`）。

```java
<activity
  android:name=".av.CallSingleBaseActivity"
  android:configChanges="orientation|keyboardHidden|screenSize"
  android:excludeFromRecents="true"
  android:label="@string/demo_activity_label_video_call"
  android:launchMode="singleInstance"
  android:screenOrientation="portrait" />
 <activity
  android:name=".av.CallMultipleBaseActivity"
  android:configChanges="orientation|keyboardHidden|screenSize"
  android:excludeFromRecents="true"
  android:label="@string/demo_activity_label_multi_call"
  android:launchMode="singleInstance"
  android:screenOrientation="portrait" />
```

## 实现音视频通话

参考如下步骤在项目中实现一对一实时音视频通话。

### 初始化

在 Agora Chat SDK 初始化完成后，可以开始初始化 AgoraChatCallKit，同时增加监听回调，设置常用配置项。示例代码如下：

```java
//构建配置信息类。
EaseCallKitConfig callKitConfig = new EaseCallKitConfig();
//设置呼叫超时时间，单位为秒。
callKitConfig.setCallTimeOut(30 * 1000);
//设置声网的 App ID。
callKitConfig.setAgoraAppId("******");
//设置是否开启 RTC token 验证。
callKitConfig.setEnableRTCToken(true);
//设置默认头像。    
callKitConfig.setDefaultHeadImage(getUsersManager().getCurrentUserInfo().getAvatar());
//设置振铃文件。   
String ringFile = EaseFileUtils.getModelFilePath(context,"huahai.mp3");
callKitConfig.setRingFile(ringFile);
//设置用户信息。
Map<String, EaseCallUserInfo> userInfoMap = new HashMap<>();
userInfoMap.put("***",new EaseCallUserInfo("****",null));
userInfoMap.put("***",new EaseCallUserInfo("****",null));
callKitConfig.setUserInfoMap(userInfoMap);
//初始化 `EaseCallKit` 类。
EaseCallKit.getInstance().init(context, callKitConfig);
//注册在 Manifest 中注册的 Activity。
EaseCallKit.getInstance().registerVideoCallClass(CallSingleBaseActivity.class);
EaseCallKit.getInstance().registerMultipleVideoClass(CallMultipleBaseActivity.class);
//添加 AgoraChatCallKit 通话过程中的事件监听器。
addCallkitListener();
```

可设置的配置项包括以下内容：

```java
/**
 * `EaseCallKitConfig` 相关的用户配置选项： 
 * @param defaultHeadImage 用户默认头像。该参数的值为本地文件绝对路径或者 URL。 
 * @param userInfoMap      用户相关信息。该信息为 key-value 格式，key 为用户的 Agora Chat 用户 ID, value 为 `EaseCallUserInfo`。
 * @param callTimeOut      呼叫超时时间，单位为秒，默认为 30 秒。 
 * @param agoraAppId       声网 App ID。 
 * @param ringFile         振铃文件。该参数的值为本地文件的绝对路径。  
 * @param enableRTCToken   是否开启 RTC 验证。该功能通过声网后台控制，默认为关闭。 
  */
  public class EaseCallKitConfig {
    private String defaultHeadImage;
    private Map<String, EaseCallUserInfo> userInfoMap = new HashMap<>();
    private String ringFile;
    private String agoraAppId;
    private long callTimeOut = 30 * 1000;
    private boolean enableRTCToken = false;
   	public EaseCallKitConfig(){
  ...
  }
  
```

### 主叫发起通话邀请

调用 `startSingleCall` 或 `startInviteMultipleCall` 方法，发起通话邀请。你需要在该方法中指定通话类型。

#### 一对一音视频通话

一对一通话可分为视频通话和语音通话，示例代码如下：

```java
/**
     * \~chinese
     * 加入一对一通话。
     * 
     * @param type 通话类型：音频通话为 {@link EaseCallType#SINGLE_VOICE_CALL}，视频通话为 {@link EaseCallType#SINGLE_VIDEO_CALL}。
     * @param user 被叫方的用户 ID，即 Agora Chat 用户 ID。该参数必填。
     * @param ext  通话邀请中的扩展信息。若不需要，可传入 `null`。
     */
    public void startSingleCall(final EaseCallType type, final String user, final Map<String, Object> ext){}
  
```
下图展示发起一对一语音通话后的 UI 界面：

![image](./images/agorachatcallkit_android_ outgoing.png)

#### 多人音视频通话

你可以从群组成员列表或者好友列表中选择，发起多人音视频邀请，具体实现可参考 demo 中的 `ConferenceInviteActivity`。

```java
/**
     * \~chinese
     * 邀请用户加入多人通话。
     * 
     * @param type 通话类型：视频通话为 {@link EaseCallType#CONFERENCE_VIDEO_CALL}，音频通话为 {@link EaseCallType#CONFERENCE_VOICE_CALL}。
     * @param users 受邀用户的用户 ID 列表，即 Agora Chat 用户 ID 列表。
     * @param ext  通话邀请中的扩展信息。若不需要，可传入 `null`。
     */
    public void startInviteMultipleCall(final EaseCallType type, final String[] users, final Map<String, Object> ext) {}
```

### 被叫收到通话邀请

主叫方发起邀请后，如果被叫方在线且当前不在通话中，会弹出邀请通话界面，被叫可以选择接听或者拒绝。

被叫振铃的同时会触发 `EaseCallKitListener` 中的 `onReceivedCall` 回调：

```java
 /**
     * \~chinese
     * 接收到通话邀请。
     * @param callType  通话类型。
     * @param fromUserId  邀请人的用户 ID，即 Agora Chat 用户 ID。
     * @param ext   呼叫邀请中的扩展信息，JSONObject 类型。
     */
    void onReceivedCall(EaseCallType callType, String fromUserId, JSONObject ext);
   
```

被叫振铃的界面如下：

![image](./images/agorachatcallkit_android_ incoming.png)

### 多人通话过程中发起邀请

多人通话中，当前用户可以点击通话界面右上角的邀请按钮向其他用户发起邀请。发出邀请后，SDK 会在主叫客户端触发 `EaseCallKitListener` 中的 `onInviteUsers` 回调：

```java
/**
     * \~chinese
     * 多人通话过程中邀请新用户加入通话时触发的回调。
     * @param callType     通话类型。
     * @param existMembers 通话中的现有用户，不包括当前用户。
     * @param ext          呼叫邀请中的扩展信息，JSONObject 类型。
     */
    void onInviteUsers(EaseCallType callType, String[] existMembers, JSONObject ext);
```

通话邀请界面的实现，可以参考 Demo 中的 `io.agora.chatdemo.group.fragments.MultiplyVideoSelectMemberChildFragment` 实现。

### 加入频道成功回调

对端用户加入通话后，当前用户及通话中其他用户会收到 `EaseCallKitListener` 中的 `onRemoteUserJoinChannel` 回调。

用户应先通过传入的参数在自己的 App Server 中查询声网 UID 对应的 AgoraChat userId：

- 若查询成功，则将 AgoraChat userId 封装成 {@link EaseUserAccount} 对象，回调给通过参数传递的 callback 对象，调用callback 对象的 {@link EaseCallGetUserAccountCallback#onUserAccount(io.agora.chat.callkit.bean.EaseUserAccount) } 方法，将结果传递过去。
- 若查询失败，则调用 {@link EaseCallGetUserAccountCallback#onSetUserAccountError(int, java.lang.String) }，将错误码和错误描述传递过去。

```java
/**
     * \~chinese
     * 对端用户成功加入频道时的回调。
     * 
     * @param channelName 频道名称。
     * @param userName Agora Chat 用户 ID。
     * @param uid 声网 UID。
     * @param callback 回调对象。
     */
    void onRemoteUserJoinChannel(String channelName, String userName, int uid, EaseCallGetUserAccountCallback callback);
```

### 通话结束

在一对一音视频通话中，若其中一方挂断，双方的通话会自动结束。多人音视频通话场景中，用户主动挂断才能结束通话。通话结束后，SDK 会在本地触发`onEndCallWithReason` 回调：

```java
/**
     * \~chinese
     * 通话结束回调。
     * @param callType    通话类型。
     * @param channelName 频道名称。
     * @param reason      通话结束原因。
     * @param callTime    通话时长。
     */
    void onEndCallWithReason(EaseCallType callType, String channelName, EaseCallEndReason reason, long callTime);


//通话结束原因如下：  
public enum EaseCallEndReason {
    EaseCallEndReasonHangup(0), //通话中的一方挂断。
    EaseCallEndReasonCancel(1), //您已取消通话。 
    EaseCallEndReasonRemoteCancel(2), //对方取消通话。
    EaseCallEndReasonRefuse(3),//对方拒绝接听。
    EaseCallEndReasonBusy(4), //忙线中。 
    EaseCallEndReasonNoResponse(5), //您未接听。
    EaseCallEndReasonRemoteNoResponse(6), //对方无响应。
    EaseCallEndReasonHandleOnOtherDeviceRefused(7),//在其他设备拒绝接听。
    EaseCallEndReasonHandleOnOtherDeviceAgreed; //在其他设备上接听。
   ....
}
```

## 更多操作

实现基础的音视频通话后，你还可以参考本节内容，在项目中实现更为进阶的功能。

### 通话异常回调

通话过程中如果有异常或者错误发生，会触发 `EaseCallKitListener` 中的 `onCallError` 回调。你可以通过返回的 `AgoraChatCallError` 了解具体报错的原因。常见的异常包括业务逻辑异常、音视频异常以及 AgoraChat IM 异常。

```java
/**
     * \~chinese
     * 通话异常回调。
     * @param type        错误类型，详见 {@link EaseCallError}。
     * @param errorCode   错误码，详见 {@link io.agora.chat.callkit.general.EaseCallProcessError}
     * @param description 错误描述。
     */
    void onCallError(EaseCallError type, int errorCode, String description);
    
```

`EaseCallError` 异常包括业务逻辑异常、音视频异常以及 AgoraChat IM 异常。

```java
/**
 * 通话异常。
 */
public enum EaseCallError {
    PROCESS_ERROR, //业务逻辑异常。
    RTC_ERROR, //音视频异常。
    IM_ERROR  //AgoraChat IM 异常。
}
```

### 修改配置

AgoraChatCallKit 库初始化之后，可修改有关配置，示例代码如下:

```java
/**
     * \~chinese
     * 获取当前 AgoraChatCallKit 的配置。
     *
     * @return 当前 AgoraChatCallKit 配置，详见 {@link EaseCallKitConfig}。
     */
    public EaseCallKitConfig getCallKitConfig();

//设置默认头像。
 EaseCallKitConfig config = EaseCallKit.getInstance().getCallKitConfig();
	if(config != null){
     String Image = EaseFileUtils.getModelFilePath(context,"bryant.png"……);
     callKitConfig.setDefaultHeadImage(Image);
}
```

### 修改头像或昵称

当 AgoraChatCallKit 内部发生 UI 变化或者收到频道中的变化事件时，例如，有新用户加入频道时，会触发 `onUserInfoUpdate` 回调通知用户可以更新对应用户信息，使得 UI 页面最终展示用户更新后的图像或昵称。用户若没有修改需求，无需实现该方法。

用户信息修改完毕，需调用 `io.agora.chat.callkit.general.EaseCallKitConfig#setUserInfo` 将修改的用户信息进行设置。注意更新过程放在同步方法里，才能实现及时刷新页面。

```java
/**
     * \~chinese
     * 通知用户更新用户信息。

     * @param userName 用户的 Agora Chat 用户 ID。
     */
     void onUserInfoUpdate(String userName){
    	//示例
    	/**
    	 EaseUser user = mUsersManager.getUserInfo(userName);
        EaseCallUserInfo userInfo = new EaseCallUserInfo();
        if (user != null) {
            userInfo.setNickName(user.getNickname());
            userInfo.setHeadImage(user.getAvatar());
        }
        EaseCallKit.getInstance().getCallKitConfig().setUserInfo(userName, userInfo);
        */
    }
```

### 使用 Token 鉴权

为保证通信安全，我们建议你使用声网 RTC Token 对加入音视频通话的用户进行鉴权。你需要在声网控制台[启用主要证书](./manage_projects?platform=All%20Platforms#启用主要证书)，并将并 AgoraChatCallKit 中的 `setEnableRTCToken(boolean)` 设置为 `true` 启用 RTC token 验证。

下面是启用 token 验证的示例代码：

```java
EaseCallKitConfig callKitConfig = new EaseCallKitConfig();
 ……
 callKitConfig.setEnableRTCToken(true);
 ……
 EaseCallKit.getInstance().init(context, callKitConfig);

```

开启 Token 鉴权后，SDK 会触发 `onGenerateRTCToken` 回调，你需要在该回调中获取 Token，具体过程如下：

用户应首先从自己的 App Server 中获取声网 RTC token 和 UID，若获取成功则触发 {@link EaseCallKitTokenCallback#onSetToken(java.lang.String, int)}方法将该 token 和 UID 回调给 callback 对象，这样 AgoraChatCallKit 内部能获得 RTC token 和 UID，加入对应的频道。

Token 需要在你自己的服务端部署生成，并在客户端实现获取及更新 Token 的逻辑。详见[使用 User Token 鉴权](https://docs.agora.io/cn/Video/token_server?platform=Android)。

下面是 `onGenerateRTCToken` 回调的示例代码：

```java
/**
     * \~chinese
     * 启用 RTC Token 验证的回调。
     * 
     * @param userId       当前用户的 Agora Chat 用户 ID。
     * @param channelName  频道名称。
     * @param callback     回调对象。
     */
    default void onGenerateRTCToken(String userId, String channelName, EaseCallKitTokenCallback callback) {
     //获取到 token 及 用户 ID 后，将其回调给 callback 即可。 
      //callback.onSetToken(token, uid);
    }
```
### 离线推送

为保证被叫用户的 App 在后台运行或离线时也能收到通话请求，用户需开启离线推送。关于如何开启离线推送，请参见 [开启 Android Push](https://docs-im.easemob.com/ccim/android/push)。开启离线推送后，用户在离线情况下收到呼叫请求时，其手机通知页面会弹出一条通知消息，用户点击该消息可唤醒 App 并弹出邀请弹窗。

关于离线推送场景方案，请参见[安卓端保障新消息及时通知的常见实践](https://docs-im.easemob.com/im/other/integrationcases/appimnotifi)。

## 参考

本节提供在实现音视频通话过程中，可以参考的其他内容。

### API 列表

AgoraChatCallKit 提供的 API 列表如下：

| 方法                       | 说明                           |
| :------------------------- | :----------------------------- |
| init                       | 初始化 AgoraChatCallKit。        |
| setCallKitListener         | 设置监听器。                     |
| startSingleCall            | 发起一对一通话。                 |
| startInviteMultipleCall    | 邀请用户加入多人通话。         |
| getCallKitConfig           | 获取 AgoraChatCallKit 相关配置。 |
| registerVideoCallClass     | 注册一对一音视频通话实现类。   |
| registerMultipleVideoClass | 注册多人音视频通话实现类。    |

回调模块 `EaseCallKitListener` 的 API 列表如下：

| 事件                    | 说明                                                         |
| :---------------------- | :----------------------------------------------------------- |
| onEndCallWithReason     | 通话结束时触发该回调。                                       |
| onInviteUsers           | 多人通话中点击邀请按钮触发该回调。                           |
| onReceivedCall          | 振铃时触发该回调。                                           |
| onGenerateToken         | 获取声网 token 回调。用户将获取到的 token 回调到 AgoraChatCallKit。 |
| onCallError             | 通话异常时触发该回调。                                       |
| onInViteCallMessageSent | 发送通话邀请后触发该回调。                                           |
| onRemoteUserJoinChannel | 用户加入频道时触发该回调。                                         |
| onUserInfoUpdate        | 通知用户可更新用户信息，当 AgoraChatCallKit 内部发生 UI 变化或者接收到频道中的一些变化事件时触发该回调。|

`EaseCallGetUserAccountCallback` 的 API 列表如下：

| 事件                  | 说明                                        |
| :-------------------- | :------------------------------------------ |
| onUserAccount         | 通过声网 UID 获得对应的 Agora Chat 用户 ID 后的回调。 |
| onSetUserAccountError | 获取用户信息失败的回调。                      |

`EaseCallKitTokenCallback` 的 API 列表如下：

| 事件            | 说明                               |
| :-------------- | :--------------------------------- |
| onSetToken      | 设置获取到的 RTC token 到 AgoraChatCallKit 时触发该回调。 |
| onGetTokenError | 获取 RTC token 失败的错误回调。        |

### 示例项目

为方便快速体验，我们在 GitHub 上提供了一个开源的 [Agora Chat](https://github.com/AgoraIO-Usecase/AgoraChat-android) 示例项目，你可以下载体验，或查看源代码。

AgoraChatCallKit 在通话过程中，使用 AgoraChat UserId 加入频道，方便音视频视图中显示用户 ID。如果你直接调用声网 API 实现音视频通话功能，也可以直接使用 Int 型 UID 加入频道。

**注意**

Demo 中 AgoraChatCallKit 使用的 RTC token 和 UID 及对应的音视频服务，需通过声网申请。



