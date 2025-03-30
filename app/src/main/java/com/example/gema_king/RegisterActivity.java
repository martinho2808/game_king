package com.example.gema_king;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.gema_king.database.DatabaseHelper;
import com.example.gema_king.database.dao.UserDao;
import com.example.gema_king.utils.Navigator;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    DatabaseHelper dbHelper;
    Button btnregister;
    TextView linkTextView;

    String username;
    int age;
    String password;
    String passwordConfirm;
    String email;

    private static final String TAG = "Registration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //  Cancel registration
        linkTextView = (TextView) findViewById(R.id.linkTextView);
        linkTextView.setOnClickListener(this);

        //  Registration
        btnregister = findViewById(R.id.btn_register);
        btnregister.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnregister.getId()) {
            insert();
        }
        Navigator.navigateTo(RegisterActivity.this, MainActivity.class);
    }

    public void insert() {
        //  Database Installation
        dbHelper = new DatabaseHelper(this);

        //  Retrieve data
        TextView textview_username = (TextView) findViewById(R.id.usernameInput);
        TextView textview_age = (TextView) findViewById(R.id.ageInput);
        TextView textview_password = (TextView) findViewById(R.id.passwordInput);
        TextView textview_password_confirm = (TextView) findViewById(R.id.passwordConfirmInput);
        TextView textview_email = (TextView) findViewById(R.id.emailInput);

        Log.i(TAG, "Received username and password");

        username = textview_username.getText().toString();
        age = Integer.parseInt(textview_age.getText().toString());
        password = textview_password.getText().toString();
        passwordConfirm = textview_password_confirm.getText().toString();
        email = textview_email.getText().toString();

        Log.i(TAG, "Converted username and password");

        //Insert data
        //dbHelper.insertData(username, age, password, email);

        UserDao.insertUserData(this, username, age, password, email);

        Log.i(TAG, "Converted username and password");
    }
}