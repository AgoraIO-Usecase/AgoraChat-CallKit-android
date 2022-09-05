package io.agora.chat.callkit.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import io.agora.chat.callkit.EaseCallKit;
import io.agora.chat.callkit.general.EaseCallFloatWindow;
import io.agora.chat.callkit.general.EaseCallState;


public class EaseCallBaseActivity extends AppCompatActivity {
    protected final int REQUEST_CODE_OVERLAY_PERMISSION = 1002;
    // To prevent opening the request hover page multiple times
    protected boolean requestOverlayPermission;

    /**
     * Check whether float window is showing
     * @return
     */
    public boolean isFloatWindowShowing() {
        return EaseCallFloatWindow.getInstance().isShowing();
    }

    /**
     * Check permission and show float window
     */
    public void showFloatWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                doShowFloatWindow();
            } else { // To reqire the window permission.
                if(!requestOverlayPermission) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        // Add this to open the management GUI specific to this app.
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
                        requestOverlayPermission = true;
                        // Handle the permission require result in #onActivityResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            doShowFloatWindow();
        }
    }

    public void doShowFloatWindow() {}

    @Override
    protected void onStop() {
        super.onStop();
        if (EaseCallKit.getInstance().getCallState() != EaseCallState.CALL_IDLE
                && TextUtils.equals(EaseCallFloatWindow.getInstance().getCurrentInstance(),this.toString())) {
            showFloatWindow();
        }
    }
}
