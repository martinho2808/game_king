package com.example.gema_king;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.media.AudioAttributes;
import android.util.Log;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_BGM_ENABLED = "isBGMEnabled";
    private static final String KEY_SOUND_ENABLED = "isSoundEnabled";
    private static final String KEY_BGM_VOLUME = "bgmVolume";
    private static final String KEY_SFX_VOLUME = "sfxVolume";

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
        loadSettings(); // 加載保存的設置
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
        if (!isBGMEnabled) {
            Log.d(TAG, "BGM is disabled, not starting");
            return;
        }

        try {
            if (bgmPlayer != null) {
                try {
                    if (bgmPlayer.isPlaying()) {
                        Log.d(TAG, "BGM is already playing");
                        return;
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "MediaPlayer in illegal state, recreating");
                    bgmPlayer.release();
                    bgmPlayer = null;
                }
            }

            if (bgmPlayer == null) {
                bgmPlayer = new MediaPlayer();
                bgmPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                bgmPlayer.setDataSource(context.getResources().openRawResourceFd(currentBGM));
                bgmPlayer.setLooping(true);
                bgmPlayer.setVolume(bgmVolume, bgmVolume);
                bgmPlayer.prepare();
                
                // 設置錯誤監聽器
                bgmPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                    isBGMPlaying = false;
                    // 嘗試重新創建播放器
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        startBGM();
                    }, 1000);
                    return true;
                });
            }

            bgmPlayer.start();
            isBGMPlaying = true;
            Log.d(TAG, "Started playing BGM: " + currentBGM);

        } catch (Exception e) {
            Log.e(TAG, "Error starting BGM: " + e.getMessage());
            e.printStackTrace();
            // 延遲重試
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (bgmPlayer != null) {
                    bgmPlayer.release();
                    bgmPlayer = null;
                }
                startBGM();
            }, 1000);
        }
    }

    public void switchBGM(int newBGM) {
        Log.d(TAG, "Switching BGM from " + currentBGM + " to " + newBGM);
        if (currentBGM != newBGM) {
            int oldBGM = currentBGM;
            currentBGM = newBGM;
            
            if (isBGMEnabled) {
                try {
                    // 創建新的 MediaPlayer 實例
                    MediaPlayer newPlayer = new MediaPlayer();
                    newPlayer.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
                    newPlayer.setDataSource(context.getResources().openRawResourceFd(newBGM));
                    newPlayer.setLooping(true);
                    newPlayer.setVolume(bgmVolume, bgmVolume);
                    newPlayer.prepare();

                    // 設置監聽器
                    newPlayer.setOnPreparedListener(mp -> {
                        // 當新的音樂準備好後，停止舊的並開始播放新的
                        if (bgmPlayer != null) {
                            try {
                                if (bgmPlayer.isPlaying()) {
                                    bgmPlayer.stop();
                                }
                                bgmPlayer.release();
                            } catch (Exception e) {
                                Log.e(TAG, "Error stopping old BGM: " + e.getMessage());
                            }
                        }
                        bgmPlayer = newPlayer;
                        bgmPlayer.start();
                        isBGMPlaying = true;
                        Log.d(TAG, "Successfully switched BGM to: " + newBGM);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error switching BGM: " + e.getMessage());
                    currentBGM = oldBGM;  // 恢復原來的BGM
                    startBGM();  // 嘗試重新啟動原來的BGM
                }
            }
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
        try {
            if (bgmPlayer != null && bgmPlayer.isPlaying()) {
                bgmPlayer.pause();
                isBGMPlaying = false;
                Log.d(TAG, "Paused BGM");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing BGM: " + e.getMessage());
        }
    }

    public void resumeBGM() {
        if (!isBGMEnabled) return;
        
        try {
            if (bgmPlayer != null && !bgmPlayer.isPlaying()) {
                bgmPlayer.start();
                isBGMPlaying = true;
                Log.d(TAG, "Resumed BGM");
            } else {
                startBGM();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming BGM: " + e.getMessage());
            startBGM();
        }
    }

    public void stopBGM() {
        try {
            if (bgmPlayer != null) {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.stop();
                }
                bgmPlayer.release();
                bgmPlayer = null;
                isBGMPlaying = false;
                Log.d(TAG, "Stopped BGM");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping BGM: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setBGMVolume(float volume) {
        bgmVolume = volume;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(volume, volume);
        }
        saveSettings(); // 保存設置
    }

    public void setSFXVolume(float volume) {
        sfxVolume = volume;
        saveSettings(); // 保存設置
    }

    public void setBGMEnabled(boolean enabled) {
        isBGMEnabled = enabled;
        if (enabled) {
            startBGM();
        } else {
            stopBGM();
        }
        saveSettings(); // 保存設置
    }

    public void setSoundEnabled(boolean enabled) {
        isSoundEnabled = enabled;
        saveSettings(); // 保存設置
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

    public int getCurrentBGM() {
        return currentBGM;
    }

    public boolean isBGMPlaying() {
        return bgmPlayer != null && bgmPlayer.isPlaying();
    }

    // 加載保存的設置
    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        isBGMEnabled = prefs.getBoolean(KEY_BGM_ENABLED, true);
        isSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
        bgmVolume = prefs.getFloat(KEY_BGM_VOLUME, 0.5f);
        sfxVolume = prefs.getFloat(KEY_SFX_VOLUME, 1.0f);
    }

    // 保存設置
    private void saveSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_BGM_ENABLED, isBGMEnabled);
        editor.putBoolean(KEY_SOUND_ENABLED, isSoundEnabled);
        editor.putFloat(KEY_BGM_VOLUME, bgmVolume);
        editor.putFloat(KEY_SFX_VOLUME, sfxVolume);
        editor.apply();
    }
} 