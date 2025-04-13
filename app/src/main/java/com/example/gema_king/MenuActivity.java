package com.example.gema_king;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gema_king.model.UserSession;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class MenuActivity extends AppCompatActivity {

    private SoundManager soundManager;
    private static final String KEY_LANGUAGE = "isEnglish";
    private static final String PREF_NAME = "GameKing";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        soundManager = SoundManager.getInstance(this);
        soundManager.switchBGM(R.raw.bgm_menu);
        soundManager.startBGM();

        super.onCreate(savedInstanceState);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_language) {
            showLanguageDialog();
            return true;
        } else if (id == R.id.action_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_settings);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 設置對話框寬度為螢幕寬度的 90%
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
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

        // 設置進度條顏色
        updateSeekBarColor(bgmSeekBar, soundManager.isBGMEnabled());
        updateSeekBarColor(soundSeekBar, soundManager.isSoundEnabled());

        // 背景音樂開關監聽器
        bgmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.setBGMEnabled(isChecked);
            bgmSeekBar.setEnabled(isChecked);
            updateSeekBarColor(bgmSeekBar, isChecked);
        });

        // 音效開關監聽器
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.setSoundEnabled(isChecked);
            soundSeekBar.setEnabled(isChecked);
            updateSeekBarColor(soundSeekBar, isChecked);
        });

        // 背景音樂音量監聽器
        bgmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && soundManager.isBGMEnabled()) {
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
                if (fromUser && soundManager.isSoundEnabled()) {
                    soundManager.setSFXVolume(progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 確定按鈕監聽器
        okButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSeekBarColor(SeekBar seekBar, boolean enabled) {
        int color = enabled ? getResources().getColor(R.color.purple_500)
                : getResources().getColor(R.color.disabled_color);
        seekBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        seekBar.getThumb().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
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
                updateLocale(newIsEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE);

                // 更新UI語言
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
    private void updateButtonTextColor(Button button, boolean isSelected) {
        int textColor = isSelected ?
                getResources().getColor(R.color.purple_500) :
                getResources().getColor(R.color.text_primary);
        button.setTextColor(textColor);
    }

    private void updateUILanguage() {
        // 更新所有文字為當前語言
        TextView tvPlayerName = findViewById(R.id.tv_player_name);
        TextView tvPlayerAge = findViewById(R.id.tv_player_age);
        TextView tvLevel = findViewById(R.id.tvLevel);
        TextView tvGamesCount = findViewById(R.id.tv_games_count);
        MaterialButton btnStartGame = findViewById(R.id.btn_start_game);

        // 更新文字
        tvPlayerName.setText(getString(R.string.player_name));
        tvPlayerAge.setText(getString(R.string.age));
        tvLevel.setText(getString(R.string.level));
        btnStartGame.setText(getString(R.string.start_game));
    }

    private void updateLocale(Locale locale) {
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        recreate();
    }

    private void handleLogout() {
        // 保存當前的音樂設置
        SharedPreferences prefs = getSharedPreferences("GameKing", MODE_PRIVATE);
        boolean isBGMEnabled = soundManager.isBGMEnabled();
        boolean isSoundEnabled = soundManager.isSoundEnabled();

        // 停止當前背景音樂並切換回主頁面音樂
        soundManager.stopBGM();
        soundManager.switchBGM(R.raw.bgm_main);

        // 清除登入狀態但保留音樂設置
        prefs.edit()
                .putBoolean("isLoggedIn", false)
                .putString("username", "")
                .putBoolean("isBGMEnabled", isBGMEnabled)
                .putBoolean("isSoundEnabled", isSoundEnabled)
                .apply();
        
        // 清除用戶會話
        UserSession.getInstance().clearUserSession(this);
        
        // 顯示登出成功提示
        Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
        
        // 返回主頁面
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
