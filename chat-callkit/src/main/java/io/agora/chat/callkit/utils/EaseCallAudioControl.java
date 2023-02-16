package io.agora.chat.callkit.utils;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

import io.agora.util.EMLog;

public class EaseCallAudioControl {

    private String TAG = getClass().getSimpleName();
    private static volatile EaseCallAudioControl mInstance;
    private Context mContext;
    private MediaPlayer mediaPlayer;
    private String ringFile;
    protected AudioManager audioManager;
    protected Ringtone ringtone;
    private boolean isPlaying;

    public void init(Context context) {
        this.mContext = context.getApplicationContext();
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        audioManager.setMode(AudioManager.MODE_RINGTONE);
        if (ringUri != null) {
            ringtone = RingtoneManager.getRingtone(mContext, ringUri);
        }
        ringFile = EaseCallKitUtils.getRingFile();
    }

    public static EaseCallAudioControl getInstance() {
        if (mInstance == null) {
            synchronized (EaseCallAudioControl.class) {
                if (mInstance == null) {
                    mInstance = new EaseCallAudioControl();
                }
            }
        }
        return mInstance;
    }

    private EaseCallAudioControl() {
    }

    public void playRing() {
        if (!isPlaying) {
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {

                doRing();
                EMLog.e(TAG, "playRing start");
            }
        }
    }

    private void doRing() {
        isPlaying = true;
        if (ringFile != null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(ringFile);
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Log.e(TAG, "playRing play file");
                }
            } catch (IOException e) {
                mediaPlayer = null;
            }
        } else {
            EMLog.d(TAG, "playRing start play");
            if (ringtone != null) {
                ringtone.play();
                Log.e(TAG, "playRing play ringtone");
            }
            EMLog.d(TAG, "playRing start play end");
        }
    }

    public void stopPlayRing() {
        isPlaying = false;
        if (ringFile != null) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer = null;
            }
        } else {
            if (ringtone != null) {
                ringtone.stop();
            }
        }
    }

    /**
     * Turn on the speaker
     */
    public void openSpeakerOn() {
        try {
            if (!audioManager.isSpeakerphoneOn())
                audioManager.setSpeakerphoneOn(true);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * turn off the speakers
     */
    public void closeSpeakerOn() {
        try {
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn())
                    audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
