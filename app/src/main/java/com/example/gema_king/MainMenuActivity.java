package com.example.gema_king;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;

import com.example.gema_king.model.UserSession;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;

public class MainMenuActivity extends MenuActivity {
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_LANGUAGE = "isEnglish";
    private DatabaseHelper dbHelper;
    private SoundManager soundManager;
    private TextView tvPlayerName, tvPlayerAge;
    private TextView tvGamesCount;
    private MaterialButton btnStartGame;
    private TextView tvLevel;
    private ProgressBar progressBarExperience;
    private TextView tvExperience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        soundManager = SoundManager.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // 初始化數據庫
        dbHelper = new DatabaseHelper(this);
        
        // 確保背景音樂正在播放
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isBGMEnabled = prefs.getBoolean("isBGMEnabled", true);
        
        if (isBGMEnabled) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!soundManager.isBGMPlaying() || soundManager.getCurrentBGM() != R.raw.bgm_menu) {
                    soundManager.switchBGM(R.raw.bgm_menu);
                }
            }, 300);
        }

        // 初始化視圖
        initViews();
        setupToolbar();
        
        // 載入玩家資料
        loadPlayerData();
        
        // 設置按鈕點擊事件
        setupClickListeners();
    }

    private void initViews() {
        tvPlayerName = findViewById(R.id.tv_player_name);
        tvPlayerAge = findViewById(R.id.tv_player_age);
        tvLevel = findViewById(R.id.tvLevel);
        tvGamesCount = findViewById(R.id.tv_games_count);
        btnStartGame = findViewById(R.id.btn_start_game);
        progressBarExperience = findViewById(R.id.progressBarExperience);
        tvExperience = findViewById(R.id.tvExperience);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    @SuppressLint("Range")
    private void loadPlayerData() {
        try {
            SharedPreferences prefs = getSharedPreferences("GameKing", MODE_PRIVATE);
            String username = prefs.getString("username", "");
            Log.d("MainMenuActivity", "Loading data for username: " + username);

            if (username.isEmpty()) {
                Log.e("MainMenuActivity", "Username is empty");
                Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }

            HashMap<String, Object> stats = dbHelper.getUserGameStatsSimple(UserSession.getUserId(this));
            if (stats != null && !stats.isEmpty()) {
                String playerName = (String) stats.get("username");
                int age = (int) stats.get("age");
                int level = (int) stats.get("level");
                int gamesPlayed = (int) stats.get("games_played");
                int totalPlayTime = (int) stats.get("total_play_time");
                int highestScore = (int) stats.get("total_score");
                int total_finished_game = (int) stats.get("total_finished_game");
                int total_progress_game = (int) stats.get("total_progress_game");

                Log.d("MainMenuActivity", "Data loaded: " +
                    "playerName=" + playerName +
                    ", age=" + age +
                    ", level=" + level +
                    ", gamesPlayed=" + gamesPlayed +
                    ", totalPlayTime=" + totalPlayTime +
                    ", highestScore=" + highestScore);
                // 更新玩家基本信息

                tvPlayerName.setText(playerName);
                tvPlayerAge.setText(getString(R.string.age_format, age));
                tvLevel.setText("(Lv. " + level + ")");
                tvGamesCount.setText(String.valueOf(gamesPlayed));

                // 更新遊戲狀態顯示
                TextView tvGameState = findViewById(R.id.tv_game_state);
                if (tvGameState != null) {
                    if(total_finished_game + total_progress_game == 0) {
                        tvGameState.setText(R.string.not_start);
                    } else if (total_finished_game < 10) {
                        tvGameState.setText(R.string.in_progress);
                    } else {
                        tvGameState.setText(R.string.progress_finish);
                    }
                }

                // 更新遊戲進度詳細信息
                TextView tvTotalPlayTime = findViewById(R.id.tv_total_play_time);
                TextView tvHighestScore = findViewById(R.id.tv_highest_score);

                if (tvTotalPlayTime != null && tvHighestScore != null) {
                    // 將總遊戲時間轉換為小時和分鐘
                    int hours = totalPlayTime / 3600;
                    int minutes = (totalPlayTime % 3600) / 60;
                    int seconds = totalPlayTime % 60;
                    String playTimeText = "";
                    if (hours > 0) {
                        playTimeText = getString(R.string.total_play_time_format, hours, minutes);  
                    } else if (minutes > 0) {
                        playTimeText = getString(R.string.total_play_time_minutes, minutes);
                    } else {
                        playTimeText = getString(R.string.total_play_time_seconds, seconds);
                    }

                    tvTotalPlayTime.setText(playTimeText);
                    tvHighestScore.setText(String.valueOf(highestScore));
                }


            } else {
                Log.e("MainMenuActivity", "No data found for username: " + username);
                Toast.makeText(this, "無法載入用戶數據", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {
            Log.e("MainMenuActivity", "Error loading player data: " + e.getMessage());
            Toast.makeText(this, "載入數據時發生錯誤", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void setupClickListeners() {
        btnStartGame.setOnClickListener(v -> {
            // TODO: 實現開始遊戲的邏輯
            Log.e("MainMenuActivity", "Start Game");
            Intent intent = new Intent(this, Game5Activity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_leaderboard) {
            startActivity(new Intent(this, LeaderboardActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 確保音樂管理器存在
        if (soundManager == null) {
            soundManager = SoundManager.getInstance(this);
        }
        
        // 檢查並確保背景音樂正確播放
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isBGMEnabled = prefs.getBoolean("isBGMEnabled", true);
        
        if (isBGMEnabled) {
            if (!soundManager.isBGMPlaying() || soundManager.getCurrentBGM() != R.raw.bgm_menu) {
                soundManager.switchBGM(R.raw.bgm_menu);
                soundManager.startBGM();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            dbHelper.close();
        }
    }


    private void updateUserInfo() {
        SharedPreferences prefs = getSharedPreferences("GameKing", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        tvPlayerName.setText(username);

        // 獲取用戶等級和經驗值
        Cursor cursor = dbHelper.getUserData(username);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                @SuppressLint("Range") int level = cursor.getInt(cursor.getColumnIndex("level"));
                @SuppressLint("Range") int experience = cursor.getInt(cursor.getColumnIndex("experience"));
                int nextLevelExp = level * 100;  // 下一級所需經驗值
                int currentLevelExp = (level - 1) * 100;  // 當前等級所需經驗值
                int progress = ((experience - currentLevelExp) * 100) / (nextLevelExp - currentLevelExp);  // 計算進度百分比

                // 更新等級顯示
                tvLevel.setText(String.format("Lv.%d", level));

                // 更新進度條
                progressBarExperience.setProgress(progress);

                // 更新經驗值文字
                int remainingExp = nextLevelExp - experience;
                tvExperience.setText(String.format("還需 %d", remainingExp));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 創建對話框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
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
        AlertDialog dialog = builder.create();
        
        // 設置對話框背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        
        // 設置按鈕文字顏色
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            
            if (positiveButton != null && negativeButton != null) {
                // 確定按鈕設為綠色
                positiveButton.setTextColor(Color.parseColor("#4CAF50")); // Material Green
                // 取消按鈕設為紅色
                negativeButton.setTextColor(Color.parseColor("#F44336")); // Material Red
                
                // 設置按鈕字體大小
                positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        });
        
        dialog.show();
    }
} 