package com.example.gema_king;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.app.Dialog;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.content.Intent;
import android.content.Context;
import android.os.Vibrator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;
import com.example.gema_king.view.TiltMazeView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Game10Activity extends MenuActivity {
    private static final String TAG = "Game10Activity";
    private static final int GAME_ID = 100;
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_LANGUAGE = "isEnglish";
    private static final String PREF_HIGH_SCORE = "highScore";
    
    private TiltMazeView tiltMazeView;
    private MaterialButton btnStartGame;
    private MaterialButton endActionButton;
    private TextView timerText;
    private TextView attemptsText;
    private TextView endMessage;
    private Handler handler;
    private long startTime;
    private boolean isGameRunning = false;
    private int recordId;
    private int highScore = 0;
    
    private ConstraintLayout startOverlay;
    private ConstraintLayout gameContainer;
    private ConstraintLayout endOverlay;
    
    private SoundManager soundManager;
    private Runnable timerRunnable;
    private ExecutorService executorService;
    private boolean isStartingGame = false;

    private Handler toastHandler = new Handler(Looper.getMainLooper());
    private boolean isShowingToast = false;
    private Runnable toastRunnable = new Runnable() {
        @Override
        public void run() {
            isShowingToast = false;
        }
    };

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate 開始");
        // 讀取語言設定，預設為英文
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isEnglish = prefs.getBoolean(KEY_LANGUAGE, true); // 預設為英文
        
        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this);
        
        // 設置語言
        Locale locale = isEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game10);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // 設置背景顏色
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundColor(getResources().getColor(R.color.game_background));
        
        // 初始化振動器
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // 初始化狀態管理器
        StatusManager.init(this);
        recordId = StatusManager.initGameStatus(UserSession.getUserId(this), GAME_ID);
        StatusManager.updateGamePlayed(UserSession.getUserId(this));
        
        // 設置工具欄
        setupToolbar();
        
        // 初始化視圖
        initViews();
        setupGameListeners();
        
        // 檢查是否是從語言切換返回
        boolean returnToStart = getIntent().getBooleanExtra("returnToStart", false);
        if (returnToStart) {
            // 確保顯示開始遊戲畫面
            startOverlay.setVisibility(View.VISIBLE);
            gameContainer.setVisibility(View.GONE);
            endOverlay.setVisibility(View.GONE);
            
            // 設置開始遊戲按鈕
            btnStartGame.setOnClickListener(v -> {
                soundManager.playButtonClick();
                startOverlay.setVisibility(View.GONE);
                gameContainer.setVisibility(View.VISIBLE);
                startGame();
            });
        }
        
        // 初始化執行器
        executorService = Executors.newSingleThreadExecutor();
        
        Log.d(TAG, "onCreate 完成");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // 初始時不顯示標題
            TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
            if (toolbarTitle != null) {
                toolbarTitle.setVisibility(View.GONE);
            }
        }
    }

    private void initViews() {
        Log.d(TAG, "初始化視圖");
        startOverlay = findViewById(R.id.start_overlay);
        gameContainer = findViewById(R.id.game_container);
        endOverlay = findViewById(R.id.end_overlay);
        
        tiltMazeView = findViewById(R.id.tilt_maze_view);
        btnStartGame = findViewById(R.id.btn_start_game);
        MaterialButton btnNextGame = findViewById(R.id.btn_next_game);
        MaterialButton btnPlayAgain = findViewById(R.id.btn_play_again);
        timerText = findViewById(R.id.timer_text);
        attemptsText = findViewById(R.id.attempts_text);
        endMessage = findViewById(R.id.end_message);

        // 初始化計時器
        handler = new Handler(Looper.getMainLooper());
        startTime = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isGameRunning) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    updateTimerText(elapsedTime);
                    handler.postDelayed(this, 1000);
                }
            }
        };

        // 設置開始遊戲按鈕點擊事件
        btnStartGame.setOnClickListener(v -> {
            if (!isStartingGame) {
                isStartingGame = true;
                startOverlay.setVisibility(View.GONE);
                gameContainer.setVisibility(View.VISIBLE);
                
                // 確保 TiltMazeView 已經完成初始化
                tiltMazeView.post(() -> {
                    startGame();
                });
            }
        });
        
        // 設置下一關按鈕點擊事件
        btnNextGame.setOnClickListener(v -> {
            soundManager.playButtonClick();
            endGame();
        });
        
        // 設置再玩一次按鈕點擊事件
        btnPlayAgain.setOnClickListener(v -> {
            soundManager.playButtonClick();
            restartGame();
        });
        
        Log.d(TAG, "視圖初始化完成");
    }

    private void setupGameListeners() {
        tiltMazeView.setOnGameEventListener(new TiltMazeView.OnGameEventListener() {
            @Override
            public void onGameComplete(long time) {
                long endTime = System.currentTimeMillis();
                long playTime = (endTime - startTime) / 1000; // 轉換為秒
                Log.d(TAG, "遊戲完成，耗時: " + playTime + "秒");
                
                // 播放正確音效
                soundManager.playGameSound(R.raw.correct);
                
                // 計算新分數（基於嘗試次數和時間）
                int newScore = calculateScore(playTime);
                
                // 顯示結束畫面，並傳入新分數和時間
                showEndScreen(playTime, newScore);
            }
            
            @Override
            public void onGameOver() {
                Log.d(TAG, "遊戲失敗，嘗試次數: " + tiltMazeView.getAttempts());
                attemptsText.setText(getString(R.string.game10_attempts, tiltMazeView.getAttempts()));
                soundManager.playGameSound(R.raw.wrong);

                if (tiltMazeView.getAttempts() >= 10) {
                    Toast.makeText(Game10Activity.this, R.string.game_10_game_over, Toast.LENGTH_SHORT).show();
                    restartGame();
                }
            }
        });
    }

    private int calculateScore(long playTimeSeconds) {
        // 基礎分數
        int baseScore = 1000;
        
        // 根據時間扣分（每秒扣 2 分）
        int timePenalty = (int) (playTimeSeconds * 2);
        
        // 根據嘗試次數扣分（每次嘗試扣 50 分）
        int attemptsPenalty = tiltMazeView.getAttempts() * 50;
        
        // 計算最終分數
        int finalScore = baseScore - timePenalty - attemptsPenalty;
        
        // 確保分數不會小於 0
        return Math.max(0, finalScore);
    }

    private void startGame() {
        Log.d(TAG, "開始遊戲");
        if (isStartingGame) {
            Log.d(TAG, "遊戲正在啟動中，忽略此次調用");
            return;
        }
        
        isStartingGame = true;
        isGameRunning = true;
        startTime = System.currentTimeMillis();
        
        // 遊戲開始時顯示標題
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setText(R.string.game10_title);
        }
        
        // 確保 TiltMazeView 已經完成初始化
        if (tiltMazeView != null) {
            tiltMazeView.post(() -> {
                // 重置遊戲狀態
                tiltMazeView.startGame();
                attemptsText.setText(getString(R.string.game10_attempts, 0));
                timerText.setText(getString(R.string.game10_time_format, 0));
                
                // 開始計時
                startTimer();
                
                // 更新遊戲狀態
                StatusManager.updateGameStatusToProgress(recordId);
                
                isStartingGame = false;
            });
        } else {
            Log.e(TAG, "TiltMazeView 未初始化");
            isStartingGame = false;
            isGameRunning = false;
        }
    }

    private void restartGame() {
        Log.d(TAG, "重新開始遊戲");
        
        // 重置遊戲狀態
        isGameRunning = false;
        handler.removeCallbacksAndMessages(null);
        
        // 重置計時器和嘗試次數
        timerText.setText(getString(R.string.game10_time_format, 0));
        attemptsText.setText(getString(R.string.game10_attempts, 0));
        
        // 重置迷宮視圖
        tiltMazeView.resetGame();
        
        // 隱藏工具欄標題
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setVisibility(View.GONE);
        }
        
        // 切換回準備界面
        gameContainer.setVisibility(View.GONE);
        endOverlay.setVisibility(View.GONE);
        startOverlay.setVisibility(View.VISIBLE);
        
        // 重置開始遊戲按鈕狀態
        isStartingGame = false;
        
        // 更新遊戲狀態
        StatusManager.updateGameStatusToStop(recordId);
        
        Log.d(TAG, "遊戲重置完成");
    }

    private void showEndScreen(long time, int newScore) {
        isGameRunning = false;
        gameContainer.setVisibility(View.GONE);
        endOverlay.setVisibility(View.VISIBLE);
        
        // 停止計時器
        handler.removeCallbacksAndMessages(null);
        
        // 計算實際遊戲時間（秒）
        int totalPlayTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
        
        // 更新遊戲狀態和分數到數據庫
        StatusManager.updateGameStatusToFinish(recordId, newScore, totalPlayTime);
        
        // 更新結束訊息，使用本地化的字符串資源
        String completeMessage = getString(R.string.game10_complete_message, newScore, totalPlayTime);
        endMessage.setText(completeMessage);
        
        // 更新再玩一次按鈕文字
        MaterialButton playAgainButton = findViewById(R.id.btn_play_again);
        if (playAgainButton != null) {
            playAgainButton.setText(R.string.play_again);
            playAgainButton.setOnClickListener(v -> {
                soundManager.playButtonClick();
                restartGame();
            });
        }
        
        // 更新下一關按鈕文字
        MaterialButton nextGameButton = findViewById(R.id.btn_next_game);
        if (nextGameButton != null) {
            nextGameButton.setText(R.string.return_to_main_menu);
            nextGameButton.setOnClickListener(v -> {
                soundManager.playButtonClick();
                // 禁用按鈕防止重複點擊
                nextGameButton.setEnabled(false);
                // 切換到主選單音樂
                soundManager.switchBGM(R.raw.bgm_menu);
                // 直接返回主選單
                Intent intent = new Intent(this, MainMenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void startTimer() {
        Log.d(TAG, "開始計時器");
        handler.removeCallbacksAndMessages(null);
        
        // 重置開始時間
        startTime = System.currentTimeMillis();
        
        handler.post(timerRunnable);
    }

    private void updateTimerText(long elapsedTime) {
        // 將毫秒轉換為秒
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            timerText.setText(getString(R.string.game10_time_format_minutes, minutes, seconds));
        } else {
            timerText.setText(getString(R.string.game10_time_format, seconds));
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (isGameRunning) {
            tiltMazeView.stopGame();
            handler.removeCallbacksAndMessages(null);
            StatusManager.updateGameStatusToStop(recordId);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (isGameRunning) {
            // 只恢復感應器和計時器，不重新生成迷宮
            tiltMazeView.resumeGame();  // 使用新方法代替startGame
            startTimer();
            StatusManager.updateGameStatusToProgress(recordId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            soundManager.playButtonClick();
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_language) {
            soundManager.playButtonClick();
            showLanguageDialog();
            return true;
        } else if (id == R.id.action_leaderboard) {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, LeaderboardActivity.class);
            intent.putExtra("gameId", GAME_ID);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        toastHandler.removeCallbacksAndMessages(null);
        // 釋放音效資源
        if (soundManager != null) {
            soundManager.release();
        }
        // 關閉執行器
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void updateUILanguage() {
        // 更新開始遊戲界面的所有文字
        if (startOverlay != null) {
            // 更新遊戲標題
            TextView titleText = startOverlay.findViewById(R.id.game10_title);
            if (titleText != null) {
                titleText.setText(R.string.game10_title);
            }

            // 更新開始遊戲訊息
            TextView messageText = startOverlay.findViewById(R.id.game10_start_message);
            if (messageText != null) {
                messageText.setText(R.string.game10_start_message);
            }

            // 更新遊戲規則
            TextView rulesText = startOverlay.findViewById(R.id.game10_rules);
            if (rulesText != null) {
                rulesText.setText(R.string.game10_rules);
            }

            // 更新開始遊戲按鈕
            if (btnStartGame != null) {
                btnStartGame.setText(R.string.game10_start);
            }
        }

        // 如果遊戲正在進行，更新遊戲相關文字
        if (isGameRunning) {
            // 更新工具欄標題
            Toolbar toolbar = findViewById(R.id.toolbar);
            TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
            if (toolbarTitle != null) {
                toolbarTitle.setVisibility(View.VISIBLE);
                toolbarTitle.setText(R.string.game10_title);
            }
            
            // 更新計時器文字
            if (timerText != null) {
                timerText.setText(getString(R.string.game10_time_format, 0));
            }
            
            // 更新嘗試次數文字
            if (attemptsText != null) {
                attemptsText.setText(getString(R.string.game10_attempts, tiltMazeView.getAttempts()));
            }
        }

        // 更新結束遊戲界面
        if (endOverlay != null && endOverlay.getVisibility() == View.VISIBLE) {
            if (endMessage != null) {
                endMessage.setText(R.string.game10_complete);
            }
            
            // 更新再玩一次按鈕
            MaterialButton playAgainButton = findViewById(R.id.btn_play_again);
            if (playAgainButton != null) {
                playAgainButton.setText(R.string.play_again);
            }
            
            // 更新下一關按鈕
            MaterialButton nextGameButton = findViewById(R.id.btn_next_game);
            if (nextGameButton != null) {
                nextGameButton.setText(R.string.return_to_main_menu);
            }
        }
    }
    
    private void showLanguageDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_language);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 設置對話框寬度為螢幕寬度的 90%
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(layoutParams);

        Button btnEnglish = dialog.findViewById(R.id.btn_english);
        Button btnChinese = dialog.findViewById(R.id.btn_chinese);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        // 讀取當前語言設置
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isEnglish = prefs.getBoolean(KEY_LANGUAGE, true);

        // 設置按鈕的初始狀態
        btnEnglish.setSelected(isEnglish);
        btnChinese.setSelected(!isEnglish);

        // 設置按鈕文字顏色
        updateButtonTextColor(btnEnglish, isEnglish);
        updateButtonTextColor(btnChinese, !isEnglish);

        // 英文按鈕點擊事件
        btnEnglish.setOnClickListener(v -> {
            soundManager.playButtonClick();
            btnEnglish.setSelected(true);
            btnChinese.setSelected(false);
            updateButtonTextColor(btnEnglish, true);
            updateButtonTextColor(btnChinese, false);
        });

        // 中文按鈕點擊事件
        btnChinese.setOnClickListener(v -> {
            soundManager.playButtonClick();
            btnEnglish.setSelected(false);
            btnChinese.setSelected(true);
            updateButtonTextColor(btnEnglish, false);
            updateButtonTextColor(btnChinese, true);
        });

        // 取消按鈕點擊事件
        btnCancel.setOnClickListener(v -> {
            soundManager.playButtonClick();
            dialog.dismiss();
        });

        // 確定按鈕點擊事件
        dialog.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            soundManager.playButtonClick();
            boolean newIsEnglish = btnEnglish.isSelected();

            // 如果語言設置有變化，則更新
            if (newIsEnglish != isEnglish) {
                // 保存新的語言設置
                prefs.edit().putBoolean(KEY_LANGUAGE, newIsEnglish).apply();

                // 設置新的語言
                Locale locale = newIsEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
                Locale.setDefault(locale);
                Resources res = getResources();
                Configuration config = res.getConfiguration();
                config.setLocale(locale);
                res.updateConfiguration(config, res.getDisplayMetrics());

                // 如果遊戲正在進行，停止遊戲
                if (isGameRunning) {
                    handler.removeCallbacksAndMessages(null);
                }

                // 重置遊戲狀態
                isGameRunning = false;

                // 更新UI
                startOverlay.setVisibility(View.VISIBLE);
                gameContainer.setVisibility(View.GONE);
                endOverlay.setVisibility(View.GONE);

                // 隱藏工具列標題
                Toolbar toolbar = findViewById(R.id.toolbar);
                TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
                if (toolbarTitle != null) {
                    toolbarTitle.setVisibility(View.GONE);
                }

                // 更新所有文字
                updateUILanguage();

                // 顯示切換提示
                Toast.makeText(this,
                        newIsEnglish ? R.string.switch_to_english : R.string.switch_to_chinese,
                        Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }
    
    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_settings);
        
        // 設置對話框背景為透明
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // 設置對話框寬度為螢幕寬度的 90%
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = android.view.Gravity.CENTER;
        
        // 設置背景變暗
        layoutParams.dimAmount = 0.6f;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        
        dialog.getWindow().setAttributes(layoutParams);

        Switch bgmSwitch = dialog.findViewById(R.id.switch_background_music);
        Switch soundSwitch = dialog.findViewById(R.id.switch_game_sound);
        SeekBar bgmSeekBar = dialog.findViewById(R.id.seekbar_background_music);
        SeekBar soundSeekBar = dialog.findViewById(R.id.seekbar_game_sound);
        Button okButton = dialog.findViewById(R.id.btn_ok);

        // 設置開關的初始狀態
        bgmSwitch.setChecked(soundManager.isBGMEnabled());
        soundSwitch.setChecked(soundManager.isSoundEnabled());

        // 設置音量條的初始值和狀態
        bgmSeekBar.setProgress((int)(soundManager.getBGMVolume() * 100));
        soundSeekBar.setProgress((int)(soundManager.getSFXVolume() * 100));
        bgmSeekBar.setEnabled(soundManager.isBGMEnabled());
        soundSeekBar.setEnabled(soundManager.isSoundEnabled());

        // 背景音樂開關監聽器
        bgmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.playButtonClick();
            soundManager.setBGMEnabled(isChecked);
            bgmSeekBar.setEnabled(isChecked);
        });

        // 音效開關監聽器
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.playButtonClick();
            soundManager.setSoundEnabled(isChecked);
            soundSeekBar.setEnabled(isChecked);
        });

        // 背景音樂音量監聽器
        bgmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    soundManager.setBGMVolume(progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 音效音量監聽器
        soundSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    soundManager.setSFXVolume(progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        okButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            dialog.dismiss();
        });
        dialog.show();
    }
    
    private void updateButtonTextColor(Button button, boolean isSelected) {
        if (button != null) {
            button.setTextColor(isSelected ? getResources().getColor(R.color.purple_500) : getResources().getColor(R.color.white));
        }
    }

    private void endGame() {
        Log.d(TAG, "遊戲結束");
        isGameRunning = false;
        
        // 停止計時器
        handler.removeCallbacksAndMessages(null);
        tiltMazeView.stopGame();
        
        gameContainer.setVisibility(View.GONE);
        endOverlay.setVisibility(View.VISIBLE);

        // 計算總遊戲時間（秒）
        int totalPlayTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
        
        // 計算分數（基於嘗試次數和時間）
        int score = calculateScore(totalPlayTime);
        
        // 更新遊戲狀態和分數到數據庫
        StatusManager.updateGameStatusToFinish(recordId, score, totalPlayTime);
        
        // 更新結束訊息
        endMessage.setText(getString(R.string.game10_complete, totalPlayTime, tiltMazeView.getAttempts()));
        
        // 設置按鈕文字和行為
        MaterialButton btnNextGame = findViewById(R.id.btn_next_game);
        btnNextGame.setText(R.string.return_to_main_menu);
        btnNextGame.setOnClickListener(v -> {
            soundManager.playButtonClick();
            // 禁用按鈕防止重複點擊
            btnNextGame.setEnabled(false);
            // 切換到主選單音樂
            soundManager.switchBGM(R.raw.bgm_menu);
            // 直接返回主選單
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // 創建對話框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        // 創建自定義標題視圖
        TextView titleView = new TextView(this);
        titleView.setText(R.string.exit_title);
        titleView.setGravity(Gravity.START);  // 左對齊
        titleView.setPadding(40, 30, 40, 10); // 減少標題和內容之間的間距
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTextColor(Color.WHITE); // 白色標題
        titleView.setTypeface(null, Typeface.BOLD);

        // 創建自定義消息視圖
        TextView messageView = new TextView(this);
        messageView.setText(R.string.exit_message);
        messageView.setGravity(Gravity.START);  // 左對齊
        messageView.setPadding(40, 10, 40, 30); // 減少與標題的間距
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        messageView.setTextColor(Color.WHITE); // 白色內容

        builder.setCustomTitle(titleView)
                .setView(messageView)
                .setPositiveButton(R.string.exit_confirm, (dialog, which) -> {
                    // 如果用戶確認要退出
                    if (soundManager != null) {
                        soundManager.stopBGM();
                        soundManager.release();
                    }
                    // 完全退出應用程式
                    finishAffinity();  // 結束所有活動
                    System.exit(0);    // 確保完全退出
                })
                .setNegativeButton(R.string.exit_cancel, (dialog, which) -> {
                    // 如果用戶取消，什麼都不做
                    dialog.dismiss();
                })
                .setCancelable(false);  // 防止點擊對話框外部關閉

        // 創建並顯示對話框
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // 設置對話框背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        // 設置按鈕文字顏色
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null && negativeButton != null) {
                // 確定按鈕設為綠色
                positiveButton.setTextColor(getResources().getColor(R.color.dialog_confirm)); // 綠色
                // 取消按鈕設為紅色
                negativeButton.setTextColor(getResources().getColor(R.color.dialog_cancel)); // 紅色
            }
        });

        dialog.show();
    }
} 