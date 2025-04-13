package com.example.gema_king;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.MenuItem;

import com.example.gema_king.model.UserSession;
import com.example.gema_king.utils.Navigator;

import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button loginBtnSignup, loginBtnLogin;
    private TextView mainPageTitle, btnLanguageText;
    private ImageButton btnSettings;
    private boolean isUserLoggedIn = false;
    private boolean isEnglish = true;  // 預設為英文
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_LANGUAGE = "isEnglish";
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        // 設置窗口標誌
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        getWindow().setNavigationBarColor(getResources().getColor(android.R.color.transparent));
        
        // 讀取語言設置
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isEnglish = prefs.getBoolean(KEY_LANGUAGE, true);  // 預設為英文
        
        // 設置語言
        Locale locale = isEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this);

        // 讀取音樂設置
        boolean isBGMEnabled = prefs.getBoolean("isBGMEnabled", true);
        boolean isSoundEnabled = prefs.getBoolean("isSoundEnabled", true);

        // 設置音樂狀態
        soundManager.setBGMEnabled(isBGMEnabled);
        soundManager.setSoundEnabled(isSoundEnabled);

        // 如果背景音樂開啟，則播放
        if (isBGMEnabled) {
            soundManager.startBGM();
        }


        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 設置動畫背景
        try {
            ConstraintLayout mainLayout = findViewById(R.id.main);
            AnimationDrawable animationDrawable = (AnimationDrawable) mainLayout.getBackground();
            if (animationDrawable != null) {
                animationDrawable.setEnterFadeDuration(2000);
                animationDrawable.setExitFadeDuration(4000);
                animationDrawable.start();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error setting animated background", e);
        }



        // 初始化視圖
        initializeViews();
        // 設置點擊事件
        setupClickListeners();
        // 更新UI語言
        updateUILanguage();
        //checkUserSession();
    }

    private void initializeViews() {
        mainPageTitle = findViewById(R.id.title);
        loginBtnSignup = findViewById(R.id.login_btn_signup);
        loginBtnLogin = findViewById(R.id.login_btn_login);
        btnLanguageText = findViewById(R.id.btn_language_text);
        btnSettings = findViewById(R.id.btn_settings);
    }

    private void setupClickListeners() {
        loginBtnSignup.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginBtnLogin.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnLanguageText.setOnClickListener(v -> {
            soundManager.playButtonClick();
            toggleLanguage();
        });

        btnSettings.setOnClickListener(v -> {
            soundManager.playButtonClick();
            showSettingsDialog();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 只有在用戶登入後才顯示選單
        if (isUserLoggedIn) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        }
        return true;
    }

    // 當用戶登入成功後調用
    public void onUserLoggedIn() {
        isUserLoggedIn = true;
        invalidateOptionsMenu(); // 刷新選單顯示
        // 隱藏登入和註冊按鈕
        loginBtnSignup.setVisibility(View.GONE);
        loginBtnLogin.setVisibility(View.GONE);
    }

    // 當用戶登出後調用
    public void onUserLoggedOut() {
        isUserLoggedIn = false;
        invalidateOptionsMenu(); // 刷新選單隱藏
        // 顯示登入和註冊按鈕
        loginBtnSignup.setVisibility(View.VISIBLE);
        loginBtnLogin.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soundManager == null) {
            soundManager = SoundManager.getInstance(this);
            soundManager.startBGM();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 只有在應用即將退出時才暫停音樂
        if (isFinishing()) {
            soundManager.pauseBGM();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null && isFinishing()) {
            soundManager.release();
            soundManager = null;
        }
    }

    private void toggleLanguage() {
        isEnglish = !isEnglish;
        
        // 保存語言設置
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_LANGUAGE, isEnglish).apply();
        
        // 設置新的語言
        Locale locale = isEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        
        // 更新UI語言
        updateUILanguage();
        
        // 顯示切換提示
        Toast.makeText(this, 
            isEnglish ? R.string.switch_to_english : R.string.switch_to_chinese, 
            Toast.LENGTH_SHORT).show();
    }

    private void updateUILanguage() {
        mainPageTitle.setText(R.string.main_page);
        loginBtnSignup.setText(R.string.login_txt_signup);
        loginBtnLogin.setText(R.string.login_txt_login);
        btnLanguageText.setText(isEnglish ? "中" : "EN");
    }

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


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_leaderboard) {
            Log.d("Toolbar", "📌 點擊了排行榜選項，準備跳轉 LeaderboardActivity");
            startActivity(new Intent(this, LeaderboardActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onClick(View v) {
        // 由於我們已經在 setupClickListeners() 中使用 lambda 表達式設置了所有點擊事件
        // 這個方法可以保持空白
    }

    private void checkUserSession(){
        //Get session
        UserSession.getInstance();
        JSONObject userSession = UserSession.getUserSession(this);
        if (userSession != null) {
            try {
                String username = userSession.getString("username");
                Toast.makeText(this, "Welcome to Game King, " + username, Toast.LENGTH_LONG).show();
                Navigator.navigateTo(MainActivity.this, MainMenuActivity.class);
                soundManager = SoundManager.getInstance(this);
                soundManager.stopBGM();  // 停止當前背景音樂
                soundManager.switchBGM(R.raw.bgm_menu);  // 切換到主選單的背景音樂

                finish(); // End the current page to prevent the user from returning to the login page

            } catch (Exception e) {
                UserSession.getInstance().clearUserSession(this);
                Log.e(TAG, "Error retrieving user session data: " + e.getMessage());
            }
        }

    }
}