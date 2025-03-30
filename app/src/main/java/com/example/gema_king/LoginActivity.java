package com.example.gema_king;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gema_king.database.DatabaseHelper;
import com.example.gema_king.database.dao.LoginDao;
import com.example.gema_king.database.dao.UserDao;
import com.example.gema_king.model.UserSession;
import com.example.gema_king.utils.Navigator;

import java.util.Map;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    Button login;
    TextView username;
    TextView password;
    DatabaseHelper dbHelper;

    private static final String TAG = "LoginActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.i(TAG, "Access Login Page");
        Toast.makeText(LoginActivity.this, "Welcome to Login", Toast.LENGTH_SHORT).show();

        login = findViewById(R.id.btn_login);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        dbHelper = new DatabaseHelper(this);

        Log.i(TAG, "Started database");

        username = (TextView) findViewById(R.id.usernameInput);
        password = (TextView) findViewById(R.id.passwordInput);

        Log.i(TAG, "Received username and password");

        String verify_username = (String) username.getText().toString();
        String verify_password = (String) password.getText().toString();

        Log.i(TAG, "Converted username and password");


        //boolean check = dbHelper.readData(verify_username, verify_password);
        long userId = LoginDao.loginValidate(this, verify_username, verify_password);
        Map<String, Object> userData = UserDao.getUserData(this, userId);
        Log.i(TAG, "Checked");

        if (userId != -1) {
            UserDao.getUserData(this, userId);
            if (userData != null) {
                long id = (long) userData.get("id");
                String username = (String) userData.get("username");
                int age = (int) userData.get("age");
                String email = (String) userData.get("email");

                //Save User Session
                UserSession.getInstance().saveUserSession(this, id, username, age, email);

            } else {
                Log.d(TAG, "User not found.");
            }
            Navigator.navigateTo(LoginActivity.this, GameActivity.class);
        } else {
            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
        }
    }
}
