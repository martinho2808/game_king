package com.example.gema_king;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.gema_king.database.dao.UserDao;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout usernameLayout;
    private TextInputLayout ageLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout passwordConfirmLayout;
    private TextInputLayout emailLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText ageInput;
    private TextInputEditText passwordInput;
    private TextInputEditText passwordConfirmInput;
    private TextInputEditText emailInput;
    private Button registerButton;
    private Button cancelButton;
    private TextView loginLink;
    private SoundManager soundManager;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 設置窗口標誌
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        getWindow().setNavigationBarColor(getResources().getColor(android.R.color.transparent));
        
        setContentView(R.layout.activity_register);

        
        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this);

        // 初始化視圖
        initializeViews();
        // 設置點擊監聽器
        setupClickListeners();

        Log.i(TAG, "Access Register Page");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soundManager == null) {
            soundManager = SoundManager.getInstance(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null && isFinishing()) {
            soundManager = null;
        }
    }

    private void initializeViews() {
        // 初始化輸入框布局
        usernameLayout = findViewById(R.id.username_layout);
        ageLayout = findViewById(R.id.age_layout);
        passwordLayout = findViewById(R.id.password_layout);
        passwordConfirmLayout = findViewById(R.id.confirm_password_layout);
        emailLayout = findViewById(R.id.email_layout);

        // 初始化輸入框
        usernameInput = findViewById(R.id.username_input);
        ageInput = findViewById(R.id.age_input);
        passwordInput = findViewById(R.id.password_input);
        passwordConfirmInput = findViewById(R.id.confirm_password_input);
        emailInput = findViewById(R.id.email_input);

        // 初始化按鈕
        registerButton = findViewById(R.id.register_button);
        cancelButton = findViewById(R.id.cancel_button);
        loginLink = findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            validateAndRegister();
        });
        
        cancelButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            finish();
        });

        loginLink.setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void clearAllErrors() {
        // 清除錯誤提示
        usernameLayout.setError(null);
        ageLayout.setError(null);
        passwordLayout.setError(null);
        passwordConfirmLayout.setError(null);
        emailLayout.setError(null);

        // 重置輸入框狀態
        usernameLayout.setErrorEnabled(false);
        ageLayout.setErrorEnabled(false);
        passwordLayout.setErrorEnabled(false);
        passwordConfirmLayout.setErrorEnabled(false);
        emailLayout.setErrorEnabled(false);
    }

    private void setError(TextInputLayout layout, String errorMessage) {
        layout.setErrorEnabled(true);
        layout.setError(errorMessage);
    }

    private void validateAndRegister() {
        Log.i(TAG, "Starting registration validation");
        
        // 先清除所有錯誤提示
        clearAllErrors();
        
        String username = usernameInput.getText().toString().trim();
        String ageStr = ageInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String passwordConfirm = passwordConfirmInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        
        boolean isValid = true;

        // 驗證用戶名
        if (username.isEmpty()) {
            setError(usernameLayout, getString(R.string.username_required));
            isValid = false;
        } else {
            // 檢查用戶名是否已存在
            try {
                if (UserDao.isUsernameExists(this, username)) {
                    setError(usernameLayout, getString(R.string.username_exists));
                    isValid = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking username: " + e.getMessage());
                setError(usernameLayout, getString(R.string.username_exists));
                isValid = false;
            }
        }

        // 驗證年齡
        int age = 0;
        if (ageStr.isEmpty()) {
            setError(ageLayout, getString(R.string.age_required));
            isValid = false;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age < 1 || age > 120) {
                    setError(ageLayout, getString(R.string.invalid_age));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                setError(ageLayout, getString(R.string.invalid_age));
                isValid = false;
            }
        }

        // 驗證密碼
        if (password.isEmpty()) {
            setError(passwordLayout, getString(R.string.password_required));
            isValid = false;
        } else if (password.length() < 6) {
            setError(passwordLayout, getString(R.string.password_too_short));
            isValid = false;
        }

        // 驗證確認密碼
        if (passwordConfirm.isEmpty()) {
            setError(passwordConfirmLayout, getString(R.string.confirm_password_required));
            isValid = false;
        } else if (!password.equals(passwordConfirm)) {
            setError(passwordConfirmLayout, getString(R.string.passwords_not_match));
            isValid = false;
        }

        // 驗證電子郵件
        if (email.isEmpty()) {
            setError(emailLayout, getString(R.string.email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setError(emailLayout, getString(R.string.invalid_email));
            isValid = false;
        }

        // 如果所有驗證都通過，執行註冊
        if (isValid) {
            Log.i(TAG, "Validation passed, proceeding with registration");
            try {
                UserDao.insertData(this, username, age, password, email);
                Log.i(TAG, "Registration successful");
                handleRegistrationSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Registration failed", e);
                String errorMessage = String.format(getString(R.string.registration_failed), e.getMessage());
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleRegistrationSuccess() {
        // 清空所有輸入框
        usernameInput.setText("");
        ageInput.setText("");
        passwordInput.setText("");
        passwordConfirmInput.setText("");
        emailInput.setText("");
        
        // 顯示成功訊息
        Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
        
        // 返回登入頁面
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}