package com.example.gema_king;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class SignupActivity extends AppCompatActivity {
    private TextInputLayout usernameLayout;
    private TextInputLayout ageLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputLayout emailLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText ageInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputEditText emailInput;
    private Button registerButton;
    private Button cancelButton;
    private TextView loginLink;
    private Button languageButton;
    private boolean isEnglish = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化視圖
        initializeViews();
        // 設置點擊監聽器
        setupClickListeners();
        // 更新UI語言
        updateUILanguage();
    }

    private void initializeViews() {
        // 初始化輸入框布局
        usernameLayout = findViewById(R.id.username_layout);
        ageLayout = findViewById(R.id.age_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        emailLayout = findViewById(R.id.email_layout);

        // 初始化輸入框
        usernameInput = findViewById(R.id.username_input);
        ageInput = findViewById(R.id.age_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        emailInput = findViewById(R.id.email_input);

        // 初始化按鈕
        registerButton = findViewById(R.id.register_button);
        cancelButton = findViewById(R.id.cancel_button);
        loginLink = findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        // 註冊按鈕點擊事件
        registerButton.setOnClickListener(v -> {
            if (validateInputs()) {
                // TODO: 實現註冊邏輯
                Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // 取消按鈕點擊事件
        cancelButton.setOnClickListener(v -> finish());

        // 登入鏈接點擊事件
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // 語言切換按鈕點擊事件
        languageButton.setOnClickListener(v -> {
            isEnglish = !isEnglish;
            updateUILanguage();
            saveLanguagePreference();
        });
    }

    private void updateUILanguage() {
        // 更新按鈕文字
        registerButton.setText(getString(R.string.register));
        cancelButton.setText(getString(R.string.cancel));
        loginLink.setText(getString(R.string.already_have_account));
        languageButton.setText(getString(isEnglish ? R.string.switch_to_chinese : R.string.switch_to_english));

        // 更新輸入框提示文字
        usernameLayout.setHint(getString(R.string.username));
        ageLayout.setHint(getString(R.string.age));
        passwordLayout.setHint(getString(R.string.password));
        confirmPasswordLayout.setHint(getString(R.string.confirm_password));
        emailLayout.setHint(getString(R.string.email));
    }

    private void saveLanguagePreference() {
        SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isEnglish", isEnglish);
        editor.apply();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // 驗證用戶名
        String username = usernameInput.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError(getString(R.string.username_required));
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

        // 驗證年齡
        String age = ageInput.getText().toString().trim();
        if (TextUtils.isEmpty(age)) {
            ageLayout.setError(getString(R.string.age_required));
            isValid = false;
        } else {
            try {
                int ageValue = Integer.parseInt(age);
                if (ageValue < 1 || ageValue > 120) {
                    ageLayout.setError(getString(R.string.invalid_age));
                    isValid = false;
                } else {
                    ageLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                ageLayout.setError(getString(R.string.invalid_age));
                isValid = false;
            }
        }

        // 驗證密碼
        String password = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.password_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.password_too_short));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        // 驗證確認密碼
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.confirm_password_required));
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError(getString(R.string.passwords_not_match));
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        // 驗證電子郵件
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.email_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.invalid_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        return isValid;
    }
} 