package com.example.gema_king;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.view.Gravity;
import android.graphics.Typeface;
import android.util.TypedValue;

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
        
        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this);
        
        // 讀取設置
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isEnglish = prefs.getBoolean(KEY_LANGUAGE, true);
        
        // 立即應用語言設置
        Locale locale = isEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        
        boolean isBGMEnabled = prefs.getBoolean("isBGMEnabled", true);
        
        // 設置音樂狀態
        soundManager.setBGMEnabled(isBGMEnabled);
        
        // 檢查用戶會話
        checkUserSession();
        
        // 如果沒有背景音樂在播放，則播放主畫面音樂
        if (isBGMEnabled && !soundManager.isBGMPlaying()) {
            soundManager.switchBGM(R.raw.bgm_main);
            soundManager.startBGM();  // 確保音樂開始播放
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

    @Override
    protected void onResume() {
        super.onResume();
        // 檢查並切換背景音樂
        if (SoundManager.getInstance(this).getCurrentBGM() != R.raw.bgm_main) {
            SoundManager.getInstance(this).switchBGM(R.raw.bgm_main);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 不要在這裡停止背景音樂，讓它繼續播放
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 只有在完全退出應用時才停止背景音樂
        if (isFinishing()) {
            SoundManager.getInstance(this).stopBGM();
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

        // 從 SoundManager 讀取當前設置
        bgmSwitch.setChecked(soundManager.isBGMEnabled());
        soundSwitch.setChecked(soundManager.isSoundEnabled());
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

    private void checkUserSession() {
        UserSession.getInstance();
        JSONObject userSession = UserSession.getUserSession(this);
        if (userSession != null) {
            try {
                String username = userSession.getString("username");
                // 顯示歡迎訊息
                Toast.makeText(this, 
                    String.format(getString(R.string.welcome_back), username), 
                    Toast.LENGTH_LONG).show();
                
                // 導航到主選單
                Intent intent = new Intent(this, MainMenuActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                UserSession.getInstance().clearUserSession(this);
                Log.e(TAG, "Error retrieving user session data: " + e.getMessage());
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
                positiveButton.setTextColor(getResources().getColor(R.color.dialog_confirm)); // 綠色
                // 取消按鈕設為紅色
                negativeButton.setTextColor(getResources().getColor(R.color.dialog_cancel)); // 紅色
            }
        });
        
        dialog.show();
    }
}