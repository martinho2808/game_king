package com.example.gema_king;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gema_king.model.UserSession;
import com.example.gema_king.utils.Navigator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

public class MainMenuActivity extends MenuActivity {
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_LANGUAGE = "isEnglish";
    private DatabaseHelper dbHelper;
    //private SoundManager soundManager;
    private TextView tvPlayerName, tvPlayerAge;
    private TextView tvGamesCount;
    private MaterialButton btnStartGame;
    private TextView tvLevel;
    private ProgressBar progressBarExperience;
    private TextView tvExperience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // 初始化數據庫
        dbHelper = new DatabaseHelper(this);
        
        // 初始化音效管理器
        //soundManager = SoundManager.getInstance(this);
        //soundManager.startBGM();

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

            Cursor cursor = dbHelper.getReadableDatabase().query(
                "users",
                new String[]{"username", "age", "level", "games_played", "total_play_time", "highest_score"},
                "username = ?",
                new String[]{username},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String playerName = cursor.getString(cursor.getColumnIndex("username"));
                int age = cursor.getInt(cursor.getColumnIndex("age"));
                int level = cursor.getInt(cursor.getColumnIndex("level"));
                int gamesPlayed = cursor.getInt(cursor.getColumnIndex("games_played"));
                int totalPlayTime = cursor.getInt(cursor.getColumnIndex("total_play_time"));
                int highestScore = cursor.getInt(cursor.getColumnIndex("highest_score"));

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
                    tvGameState.setText(R.string.state_ready);
                }

                // 更新遊戲進度詳細信息
                TextView tvTotalPlayTime = findViewById(R.id.tv_total_play_time);
                TextView tvHighestScore = findViewById(R.id.tv_highest_score);

                if (tvTotalPlayTime != null && tvHighestScore != null) {
                    // 將總遊戲時間轉換為小時和分鐘
                    int hours = totalPlayTime / 60;
                    int minutes = totalPlayTime % 60;
                    String playTimeText = "";
                    if (hours > 0) {
                        playTimeText = getString(R.string.total_play_time_format, hours, minutes);
                    } else {
                        playTimeText = getString(R.string.total_play_time_minutes, minutes);
                    }

                    tvTotalPlayTime.setText(playTimeText);
                    tvHighestScore.setText(String.valueOf(highestScore));
                }

                cursor.close();
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
            Intent intent = new Intent(this, Game3Activity.class);
            startActivity(intent);
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main_menu, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            showSettingsDialog();
//            return true;
//        } else if (id == R.id.action_language) {
//            showLanguageDialog();
//            return true;
//        } else if (id == R.id.action_logout) {
//            handleLogout();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

//    private void handleLogout() {
//        // 保存當前的音樂設置
//        SharedPreferences prefs = getSharedPreferences("GameKing", MODE_PRIVATE);
//        boolean isBGMEnabled = soundManager.isBGMEnabled();
//        boolean isSoundEnabled = soundManager.isSoundEnabled();
//
//        // 停止當前背景音樂並切換回主頁面音樂
//        soundManager.stopBGM();
//        soundManager.switchBGM(R.raw.bgm_main);
//
//        // 清除登入狀態但保留音樂設置
//        prefs.edit()
//            .putBoolean("isLoggedIn", false)
//            .putString("username", "")
//            .putBoolean("isBGMEnabled", isBGMEnabled)
//            .putBoolean("isSoundEnabled", isSoundEnabled)
//            .apply();
//        UserSession.getInstance().clearUserSession(this);
//        // 返回主頁面
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//        finish();
//    }

//    private void showSettingsDialog() {
//        Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.dialog_settings);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//        // 設置對話框寬度為螢幕寬度的 90%
//        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
//        layoutParams.copyFrom(dialog.getWindow().getAttributes());
//        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
//        dialog.getWindow().setAttributes(layoutParams);
//
//        Switch bgmSwitch = dialog.findViewById(R.id.switch_background_music);
//        Switch soundSwitch = dialog.findViewById(R.id.switch_game_sound);
//        SeekBar bgmSeekBar = dialog.findViewById(R.id.seekbar_background_music);
//        SeekBar soundSeekBar = dialog.findViewById(R.id.seekbar_game_sound);
//        Button okButton = dialog.findViewById(R.id.btn_ok);
//
//        // 設置開關的初始狀態
//        bgmSwitch.setChecked(soundManager.isBGMEnabled());
//        soundSwitch.setChecked(soundManager.isSoundEnabled());
//
//        // 設置音量條的初始值和狀態
//        bgmSeekBar.setProgress((int)(soundManager.getBGMVolume() * 100));
//        soundSeekBar.setProgress((int)(soundManager.getSFXVolume() * 100));
//        bgmSeekBar.setEnabled(soundManager.isBGMEnabled());
//        soundSeekBar.setEnabled(soundManager.isSoundEnabled());
//
//        // 設置進度條顏色
//        updateSeekBarColor(bgmSeekBar, soundManager.isBGMEnabled());
//        updateSeekBarColor(soundSeekBar, soundManager.isSoundEnabled());
//
//        // 背景音樂開關監聽器
//        bgmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            soundManager.setBGMEnabled(isChecked);
//            bgmSeekBar.setEnabled(isChecked);
//            updateSeekBarColor(bgmSeekBar, isChecked);
//        });
//
//        // 音效開關監聽器
//        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            soundManager.setSoundEnabled(isChecked);
//            soundSeekBar.setEnabled(isChecked);
//            updateSeekBarColor(soundSeekBar, isChecked);
//        });
//
//        // 背景音樂音量監聽器
//        bgmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser && soundManager.isBGMEnabled()) {
//                    soundManager.setBGMVolume(progress / 100f);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
//
//        // 音效音量監聽器
//        soundSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser && soundManager.isSoundEnabled()) {
//                    soundManager.setSFXVolume(progress / 100f);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
//
//        // 確定按鈕監聽器
//        okButton.setOnClickListener(v -> {
//            soundManager.playButtonClick();
//            dialog.dismiss();
//        });
//
//        dialog.show();
//    }

//    private void updateSeekBarColor(SeekBar seekBar, boolean enabled) {
//        int color = enabled ? getResources().getColor(R.color.purple_500)
//                          : getResources().getColor(R.color.disabled_color);
//        seekBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
//        seekBar.getThumb().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
//    }

//    private void showLanguageDialog() {
//        Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.dialog_language);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//        // 設置對話框寬度為螢幕寬度的 90%
//        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
//        layoutParams.copyFrom(dialog.getWindow().getAttributes());
//        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
//        dialog.getWindow().setAttributes(layoutParams);
//
//        Button btnEnglish = dialog.findViewById(R.id.btn_english);
//        Button btnChinese = dialog.findViewById(R.id.btn_chinese);
//        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
//
//        // 讀取當前語言設置
//        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
//        boolean isEnglish = prefs.getBoolean(KEY_LANGUAGE, true);
//
//        // 設置按鈕的初始狀態
//        btnEnglish.setSelected(isEnglish);
//        btnChinese.setSelected(!isEnglish);
//
//        // 設置按鈕文字顏色
//        updateButtonTextColor(btnEnglish, isEnglish);
//        updateButtonTextColor(btnChinese, !isEnglish);
//
//        // 英文按鈕點擊事件
//        btnEnglish.setOnClickListener(v -> {
//            soundManager.playButtonClick();
//            btnEnglish.setSelected(true);
//            btnChinese.setSelected(false);
//            updateButtonTextColor(btnEnglish, true);
//            updateButtonTextColor(btnChinese, false);
//        });
//
//        // 中文按鈕點擊事件
//        btnChinese.setOnClickListener(v -> {
//            soundManager.playButtonClick();
//            btnEnglish.setSelected(false);
//            btnChinese.setSelected(true);
//            updateButtonTextColor(btnEnglish, false);
//            updateButtonTextColor(btnChinese, true);
//        });
//
//        // 取消按鈕點擊事件
//        btnCancel.setOnClickListener(v -> {
//            soundManager.playButtonClick();
//            dialog.dismiss();
//        });
//
//        // 確定按鈕點擊事件
//        dialog.findViewById(R.id.btn_ok).setOnClickListener(v -> {
//            soundManager.playButtonClick();
//            boolean newIsEnglish = btnEnglish.isSelected();
//
//            // 如果語言設置有變化，則更新
//            if (newIsEnglish != isEnglish) {
//                // 保存新的語言設置
//                prefs.edit().putBoolean(KEY_LANGUAGE, newIsEnglish).apply();
//
//                // 設置新的語言
//                updateLocale(newIsEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE);
//
//                // 更新UI語言
//                updateUILanguage();
//
//                // 顯示切換提示
//                Toast.makeText(this,
//                    newIsEnglish ? R.string.switch_to_english : R.string.switch_to_chinese,
//                    Toast.LENGTH_SHORT).show();
//            }
//
//            dialog.dismiss();
//        });
//
//        dialog.show();
//    }

//    private void updateButtonTextColor(Button button, boolean isSelected) {
//        int textColor = isSelected ?
//            getResources().getColor(R.color.purple_500) :
//            getResources().getColor(R.color.text_primary);
//        button.setTextColor(textColor);
//    }
//
//    private void updateUILanguage() {
//        // 更新所有文字為當前語言
//        TextView tvPlayerName = findViewById(R.id.tv_player_name);
//        TextView tvPlayerAge = findViewById(R.id.tv_player_age);
//        TextView tvLevel = findViewById(R.id.tvLevel);
//        TextView tvGamesCount = findViewById(R.id.tv_games_count);
//        MaterialButton btnStartGame = findViewById(R.id.btn_start_game);
//
//        // 更新文字
//        tvPlayerName.setText(getString(R.string.player_name));
//        tvPlayerAge.setText(getString(R.string.age));
//        tvLevel.setText(getString(R.string.level));
//        btnStartGame.setText(getString(R.string.start_game));
//    }
//
//    private void updateLocale(Locale locale) {
//        Locale.setDefault(locale);
//        Resources res = getResources();
//        Configuration config = res.getConfiguration();
//        config.setLocale(locale);
//        res.updateConfiguration(config, res.getDisplayMetrics());
//        recreate();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            dbHelper.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInfo();
    }

    private void updateUserInfo() {
        SharedPreferences prefs = getSharedPreferences("GameKing", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        tvPlayerName.setText(username);

        // 獲取用戶等級和經驗值
        Cursor cursor = dbHelper.getUserData(username);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                int level = cursor.getInt(cursor.getColumnIndex("level"));
                int experience = cursor.getInt(cursor.getColumnIndex("experience"));
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

//    @Override
//    public void onBackPressed() {
//        // 創建退出確認對話框
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("退出遊戲")
//               .setMessage("確定要退出遊戲嗎？")
//               .setPositiveButton("確定", (dialog, which) -> {
//                    // 停止背景音樂
//                    soundManager.stopBGM();
//                    // 結束活動
//                    finish();
//               })
//               .setNegativeButton("取消", (dialog, which) -> {
//                    dialog.dismiss();
//               })
//               .setCancelable(false)
//               .show();
//    }
} 