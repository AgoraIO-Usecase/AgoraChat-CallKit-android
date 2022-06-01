package io.agora.chat.callkit.utils;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import io.agora.chat.callkit.EaseCallKit;
import io.agora.chat.callkit.general.EaseCallKitConfig;
import io.agora.chat.callkit.listener.EaseCallKitListener;
import io.agora.chat.callkit.listener.EaseCallKitTokenCallback;
import io.agora.chat.callkit.bean.EaseCallUserInfo;
import io.agora.util.EMLog;


public class EaseCallKitUtils {
    public final static String TAG = "EaseCallKitUtils";
    public final static String UPDATE_USERINFO = "updateUserInfo";
    public final static String UPDATE_CALLINFO = "updateCallInfo";

    /**
     * length用户要求产生字符串的长度，随机生成会议密码
     * @param length
     * @return
     */
    static public String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyz";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(26);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }


    /**
     * 获取手机唯一标识符
     * @return
     */
    public static String getPhoneSign(){
        StringBuilder deviceId = new StringBuilder();
        // 渠道标志
        deviceId.append("a");
        try {
            // 则生成一个id：随机码
            String uuid = getUUID();
            if(!TextUtils.isEmpty(uuid)){
                deviceId.append("id");
                deviceId.append(uuid);
                return deviceId.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            deviceId.append("id").append(getUUID());
        }
        return deviceId.toString();
    }

    private static String uuid;
    public static String getUUID(){
          SharedPreferences mShare = EaseCallKit.getInstance().getContext().getSharedPreferences("uuid",MODE_PRIVATE);
          if(mShare != null){
              uuid = mShare.getString("uuid", "");
          }
          if(TextUtils.isEmpty(uuid)){
              uuid = UUID.randomUUID().toString();
              mShare.edit().putString("uuid",uuid).commit();
          }
          return uuid;
    }

    /**
     * 获取用户头像
     * @param uersId
     * @return
     */
    public static String getUserHeadImage(String uersId){
        EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
        if(callKitConfig != null){
            Map<String, EaseCallUserInfo> userInfoMap = callKitConfig.getUserInfoMap();
            if(userInfoMap != null){
                EaseCallUserInfo userInfo = userInfoMap.get(uersId);
                if(userInfo != null){
                    if(userInfo.getHeadImage() != null && userInfo.getHeadImage().length() > 0){
                        return userInfo.getHeadImage();
                    }
                }
            }
            return callKitConfig.getDefaultHeadImage();
        }
        return  null;
    }

    /**
     * 获取用户昵称
     * @param uersId
     * @return
     */
    public static String  getUserNickName(String uersId){
        EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
        if(callKitConfig != null){
            Map<String, EaseCallUserInfo> userInfoMap = callKitConfig.getUserInfoMap();
            if(userInfoMap != null){
                EaseCallUserInfo userInfo = userInfoMap.get(uersId);
                if(userInfo != null){
                    if(userInfo.getNickName() != null && userInfo.getNickName().length() > 0){
                        return userInfo.getNickName();
                    }
                }
            }
            return uersId;
        }
        return  uersId;
    }


    /**
     * 获取用户振铃文件
     * @return
     */
    public static String getRingFile(){
        EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
        if(callKitConfig != null){
            if(callKitConfig.getRingFile() != null){
                return callKitConfig.getRingFile();
            }
        }
        return  null;
    }

    public static boolean isAppRunningForeground(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
            if (runningProcesses == null) {
                return false;
            }
            final String packageName = ctx.getPackageName();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        } else {
            try {
                List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
                if (tasks == null || tasks.size() < 1) {
                    return false;
                }
                boolean b = ctx.getPackageName().equalsIgnoreCase(tasks.get(0).baseActivity.getPackageName());
                EMLog.d("utils", "app running in foregroud：" + (b ? true : false));
                return b;
            } catch (SecurityException e) {
                EMLog.d(TAG, "Apk doesn't hold GET_TASKS permission");
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将本应用置顶到最前端
     * 当本应用位于后台时，则将它切换到最前端
     *
     * @param context
     */
    public static void bring2Front(Context context) {
        /**获取ActivityManager*/
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.AppTask> list = activityManager.getAppTasks();
            for (ActivityManager.AppTask appTask : list){
                appTask.moveToFront();
                break;
            }
        }else {
            /**获得当前运行的task(任务)*/
            List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
            for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                /**找到本应用的 task，并将它切换到前台*/
                if (taskInfo.topActivity.getPackageName().equals(context.getPackageName())) {
                    activityManager.moveTaskToFront(taskInfo.id, 0);
                    break;
                }
            }
        }
    }


    public static JSONObject convertMapToJSONObject(Map<String, Object> map) {
        JSONObject obj = new JSONObject();
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Object result;
            Object value = entry.getValue();
            if (value instanceof Map) { // is a JSONObject
                result = convertMapToJSONObject((Map<String, Object>) value);
            } else if (value instanceof List) { // is a JSONArray
                result = new JSONArray();
                for (Object item : (List) value) {
                    ((JSONArray)result).put(item);
                }
            } else if (value instanceof Object[]) {
                result = new JSONArray();
                for (Object item : (Object[]) value) {
                    ((JSONArray)result).put(item);
                }
            } else { // is common value
                result = value;
            }
            try {
                obj.put(entry.getKey(), result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    public static int getSupportedWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
    }


    /**
     * 判断用户是否重写EaseCallKitListener中的onGenerateToken方法
     * @param listener
     * @return
     */
    public static boolean realizeGetToken(EaseCallKitListener listener) {
        if(listener != null){
            Method cMethod = null;
            try {
                cMethod = listener.getClass().getDeclaredMethod("onGenerateRTCToken", String.class,String.class,EaseCallKitTokenCallback.class);
                EMLog.d(TAG,"realizeGetToken result:"+cMethod.toString());
                if(cMethod != null){
                    return  true;
                }
            } catch (NoSuchMethodException e) {
                EMLog.e(TAG,"realizeGetToken result:"+e.getLocalizedMessage());
                return false;
            }
        }
        return false;
    }

    public static int dp2px(Context context,int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
