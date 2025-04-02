package com.example.gema_king;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.media.AudioAttributes;
import android.util.Log;
import java.io.IOException;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private static SoundManager instance;
    private final Context context;
    private SoundPool soundPool;
    private MediaPlayer bgmPlayer;
    private MediaPlayer loginSuccessPlayer;
    private int buttonClickSoundId;
    private int loginSuccessSoundId;
    private float bgmVolume = 0.5f;
    private float sfxVolume = 1.0f;
    private boolean isBGMPlaying = false;
    private int currentBGM = R.raw.bgm_main; // 默認主菜單音樂
    private boolean isBGMEnabled = true;
    private boolean isSoundEnabled = true;
    private boolean isInitialized = false;
    private int bgmStreamId;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        initSoundPool();
        loadSounds();
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    private void initSoundPool() {
        if (soundPool != null) {
            soundPool.release();
        }
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
                
        isInitialized = true;
    }

    private void loadSounds() {
        if (soundPool != null) {
            buttonClickSoundId = soundPool.load(context, R.raw.button_click, 1);
            loginSuccessSoundId = soundPool.load(context, R.raw.login_success, 1);
        }
    }

    public void startBGM() {
        if (!isBGMEnabled || !isInitialized) return;

        try {
            if (bgmPlayer != null) {
                bgmPlayer.release();
            }
            bgmPlayer = MediaPlayer.create(context, currentBGM);
            if (bgmPlayer != null) {
                bgmPlayer.setLooping(true);
                bgmPlayer.setVolume(bgmVolume, bgmVolume);
                bgmPlayer.start();
                isBGMPlaying = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting BGM", e);
        }
    }

    public void switchBGM(int newBGM) {
        if (currentBGM != newBGM) {
            currentBGM = newBGM;
            startBGM();
        }
    }

    public void playGameSound(int soundId) {
        if (isSoundEnabled && soundPool != null) {
            soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void playLoginSuccess() {
        if (isSoundEnabled && soundPool != null) {
            soundPool.play(loginSuccessSoundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void playButtonClick() {
        if (isSoundEnabled && soundPool != null) {
            soundPool.play(buttonClickSoundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void pauseBGM() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
            isBGMPlaying = false;
        }
    }

    public void resumeBGM() {
        if (isBGMEnabled && bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
            isBGMPlaying = true;
        }
    }

    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
            bgmPlayer = null;
            isBGMPlaying = false;
        }
    }

    public void setBGMVolume(float volume) {
        bgmVolume = volume;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(volume, volume);
        }
    }

    public void setSFXVolume(float volume) {
        sfxVolume = volume;
    }

    public void setBGMEnabled(boolean enabled) {
        isBGMEnabled = enabled;
        if (enabled) {
            startBGM();
        } else {
            stopBGM();
        }
    }

    public void setSoundEnabled(boolean enabled) {
        isSoundEnabled = enabled;
    }

    public boolean isBGMEnabled() {
        return isBGMEnabled;
    }

    public boolean isSoundEnabled() {
        return isSoundEnabled;
    }

    public float getBGMVolume() {
        return bgmVolume;
    }

    public float getSFXVolume() {
        return sfxVolume;
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        stopBGM();
        isInitialized = false;
        instance = null;
    }

    public void reinitialize() {
        initSoundPool();
        loadSounds();
        if (isBGMEnabled) {
            startBGM();
        }
    }
} 