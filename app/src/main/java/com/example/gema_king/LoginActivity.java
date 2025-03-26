package com.example.gema_king;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class LoginActivity extends AppCompatActivity {
    private Button btnLogin;
    private static final String TAG = "LoginActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        Log.i(TAG, "Access Login Page");
        btnLogin = findViewById(R.id.login_btn_signup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Click Sign Up");
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                Toast.makeText(LoginActivity.this, "Welcome to Register", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
