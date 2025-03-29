package com.example.gema_king;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.w3c.dom.Text;


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


        boolean check = dbHelper.readData(verify_username, verify_password);

        Log.i(TAG, "Checked");

        if (check) {
            Intent i = new Intent(LoginActivity.this, GameActivity.class);
            startActivity(i);
        } else {
            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
        }
    }
}
