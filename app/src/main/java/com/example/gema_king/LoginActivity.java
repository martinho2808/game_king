package com.example.gema_king;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.example.gema_king.model.StatusManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gema_king.model.UserSession;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private Button cancelButton;
    private TextView registerLink;
    private DatabaseHelper dbHelper;
    private SoundManager soundManager;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 設置窗口標誌
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        getWindow().setNavigationBarColor(getResources().getColor(android.R.color.transparent));
        
        setContentView(R.layout.activity_login);

        // 初始化數據庫
        dbHelper = new DatabaseHelper(this);
        
        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this);

        // 初始化視圖
        initializeViews();
        // 設置點擊監聽器
        setupClickListeners();

        Log.i(TAG, "Access Login Page");
    }

    private void initializeViews() {
        // 初始化輸入框布局
        usernameLayout = findViewById(R.id.username_layout);
        passwordLayout = findViewById(R.id.password_layout);

        // 初始化輸入框
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);

        // 初始化按鈕
        loginButton = findViewById(R.id.login_button);
        cancelButton = findViewById(R.id.cancel_button);
        registerLink = findViewById(R.id.register_link);
    }

    private void setupClickListeners() {
        // 登入按鈕點擊事件
        loginButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            validateAndLogin();
        });

        // 取消按鈕點擊事件
        cancelButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            finish();
        });

        // 註冊鏈接點擊事件
        registerLink.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        // 設置輸入框文字變化監聽器
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 當用戶開始輸入時，清除錯誤提示
                if (usernameInput.getText().hashCode() == s.hashCode()) {
                    usernameLayout.setError(null);
                } else if (passwordInput.getText().hashCode() == s.hashCode()) {
                    passwordLayout.setError(null);
                }
            }
        };

        usernameInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);
    }

    private void validateAndLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        boolean isValid = true;

        Log.i(TAG, "Validating login credentials");

        // 驗證用戶名
        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.username_required));
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

        // 驗證密碼
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.password_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.password_too_short));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        // 如果基本驗證通過，檢查數據庫中的用戶憑證
        if (isValid) {
            Log.i(TAG, "Basic validation passed, checking database");
            checkCredentials(username, password);
        }
    }

    private void checkCredentials(String username, String password) {
        // 檢查數據庫中的用戶憑證
        boolean isValid = dbHelper.readData(username, password);
        Log.i(TAG, "Database check completed");

        if (isValid) {
            // 登入成功
            Log.i(TAG, "Login successful");
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
            // 跳轉到遊戲頁面
            handleLoginSuccess(username);
        } else {
            // 登入失敗
            Log.i(TAG, "Login failed");
            Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show();
        }
    }

    private void handleLoginSuccess(String username) {
        // 播放登入成功音效
        soundManager.playLoginSuccess();
        
        // 保存登入狀態
        storeUserSession(username);
        SharedPreferences prefs = getSharedPreferences("GameKing", MODE_PRIVATE);
        prefs.edit()
            .putBoolean("isLoggedIn", true)
            .putString("username", username)
            .apply();

        // 切換到主選單並切換背景音樂
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
        soundManager.stopBGM();  // 停止當前背景音樂
        soundManager.switchBGM(R.raw.bgm_menu);  // 切換到主選單的背景音樂
        finish();
    }

    private void storeUserSession(String username) {
        Cursor cursor = dbHelper.getUserData(username);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                long id = cursor.getInt(cursor.getColumnIndex("id"));
                int age = cursor.getInt(cursor.getColumnIndex("age"));
                String email = cursor.getString(cursor.getColumnIndex("email"));

                UserSession.getInstance().saveUserSession(this, id, username, age, email);
                long id_v2 = UserSession.getUserId(this);
                Log.i(TAG, String.valueOf(id_v2));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
    }
}
