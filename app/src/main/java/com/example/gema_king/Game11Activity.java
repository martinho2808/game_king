package com.example.gema_king;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.content.Intent;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class Game11Activity extends MenuActivity {
    private static final String TAG = "Game11Activity";
    private static final int GAME_ID = 11;
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_LANGUAGE = "isEnglish";
    
    private MaterialButton btnStartGame;
    private MaterialButton endActionButton;
    private TextView timerText;
    private TextView scoreText;
    private TextView endMessage;
    private Handler handler;
    private long startTime;
    private boolean isGameRunning = false;
    private int recordId;
    
    private ConstraintLayout startOverlay;
    private ConstraintLayout gameContainer;
    private ConstraintLayout endOverlay;
    
    private SoundManager soundManager;
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate 開始");
        // 讀取語言設定
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isEnglish = prefs.getBoolean(KEY_LANGUAGE, false);
        
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
        setContentView(R.layout.activity_game11);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 初始化狀態管理器
        StatusManager.init(this);
        recordId = StatusManager.initGameStatus(UserSession.getUserId(this), GAME_ID);
        StatusManager.updateGamePlayed(UserSession.getUserId(this));

        // 設置工具欄
        setupToolbar();

        // 初始化視圖
        initViews();
        
        Log.d(TAG, "onCreate 完成");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
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
        
        btnStartGame = findViewById(R.id.btn_start_game);
        endActionButton = findViewById(R.id.end_action_button);
        timerText = findViewById(R.id.timer_text);
        scoreText = findViewById(R.id.score_text);
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
            soundManager.playButtonClick();
            startOverlay.setVisibility(View.GONE);
            gameContainer.setVisibility(View.VISIBLE);
            startGame();
        });
        
        Log.d(TAG, "視圖初始化完成");
    }

    private void startGame() {
        Log.d(TAG, "開始遊戲");
        isGameRunning = true;
        startTime = System.currentTimeMillis();
        
        // 遊戲開始時顯示標題
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setText(R.string.game11_title);
        }
        
        // 開始計時
        startTimer();
        
        // 更新遊戲狀態
        StatusManager.updateGameStatusToProgress(recordId);
    }

    private void startTimer() {
        Log.d(TAG, "開始計時器");
        handler.removeCallbacksAndMessages(null);
        handler.post(timerRunnable);
    }

    private void updateTimerText(long elapsedTime) {
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        timerText.setText(String.format("⏱️ %02d:%02d", minutes, seconds));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (isGameRunning) {
            handler.removeCallbacksAndMessages(null);
            StatusManager.updateGameStatusToStop(recordId);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (isGameRunning) {
            startTimer();
            StatusManager.updateGameStatusToProgress(recordId);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (soundManager != null) {
            soundManager.release();
        }
    }
} 