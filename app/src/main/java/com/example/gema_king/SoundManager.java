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
import java.util.HashMap;
import com.example.gema_king.model.UserSession;

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
    private final HashMap<Integer, Integer> soundMap;
    private MediaPlayer bgmPlayer;
    private MediaPlayer loginSuccessPlayer;
    private int buttonClickSound;
    private int loginSuccessSoundId;
    private float bgmVolume = 1.0f;
    private float sfxVolume = 1.0f;
    private boolean isBGMPlaying = false;
    private int currentBGM = -1;
    private boolean isBGMEnabled = true;
    private boolean isSoundEnabled = true;
    private boolean isInitialized = false;
    private int bgmStreamId;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        loadSettings(); // 加載保存的設置
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();
                
        soundMap = new HashMap<>();
        loadSounds();
        isInitialized = true;
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    private void loadSounds() {
        if (soundPool != null) {
            buttonClickSound = soundPool.load(context, R.raw.button_click, 1);
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
                    // 先停止當前的音樂
                    if (bgmPlayer != null) {
                        try {
                            if (bgmPlayer.isPlaying()) {
                                bgmPlayer.stop();
                            }
                            bgmPlayer.release();
                            bgmPlayer = null;
                        } catch (Exception e) {
                            Log.e(TAG, "Error stopping old BGM: " + e.getMessage());
                        }
                    }

                    // 創建新的 MediaPlayer 實例
                    bgmPlayer = new MediaPlayer();
                    bgmPlayer.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
                    bgmPlayer.setDataSource(context.getResources().openRawResourceFd(newBGM));
                    bgmPlayer.setLooping(true);
                    bgmPlayer.setVolume(bgmVolume, bgmVolume);
                    
                    // 設置準備完成的監聽器
                    bgmPlayer.setOnPreparedListener(mp -> {
                        Log.d(TAG, "BGM prepared, starting playback");
                        mp.start();
                        isBGMPlaying = true;
                    });
                    
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
                    
                    // 準備播放
                    bgmPlayer.prepareAsync();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error switching BGM: " + e.getMessage());
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
        }
    }

    public void playGameSound(int soundResourceId) {
        if (isSoundEnabled) {
            // 檢查是否已經載入過這個音效
            if (!soundMap.containsKey(soundResourceId)) {
                // 如果沒有載入過，就載入並保存
                int soundId = soundPool.load(context, soundResourceId, 1);
                soundMap.put(soundResourceId, soundId);
                // 等待載入完成後播放
                soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                    if (status == 0) { // 0 表示載入成功
                        soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
                    }
                });
            } else {
                // 如果已經載入過，直接播放
                int soundId = soundMap.get(soundResourceId);
                soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
            }
        }
    }

    public void playLoginSuccess() {
        if (isSoundEnabled && soundPool != null) {
            soundPool.play(loginSuccessSoundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void playButtonClick() {
        if (isSoundEnabled) {
            soundPool.play(buttonClickSound, sfxVolume, sfxVolume, 1, 0, 1.0f);
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
            // 檢查是否有用戶會話
            if (UserSession.getUserId(context) > 0) {
                // 如果有用戶會話，播放主選單音樂
                currentBGM = R.raw.bgm_menu;
            } else {
                // 如果沒有用戶會話，播放主畫面音樂
                currentBGM = R.raw.bgm_main;
            }
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
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }

    public void reinitialize() {
        // 重新初始化音效池
        if (soundPool != null) {
            soundPool.release();
        }
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();
                
        loadSounds();
        isInitialized = true;
        
        // 如果背景音樂已啟用，重新開始播放
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
        bgmVolume = prefs.getFloat(KEY_BGM_VOLUME, 1.0f);
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