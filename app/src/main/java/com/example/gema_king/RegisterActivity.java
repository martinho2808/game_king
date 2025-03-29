package com.example.gema_king;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.gema_king.DatabaseHelper;

import org.w3c.dom.Text;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    DatabaseHelper dbHelper;
    Button btnregister;
    TextView linkTextView;

    String username;
    int age;
    String password;
    String passwordConfirm;
    String email;

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
        Intent i = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(i);
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

        username = (String) textview_username.getText();
        age = Integer.parseInt((String) textview_age.getText());
        password = (String) textview_password.getText();
        passwordConfirm = (String) textview_password_confirm.getText();
        email = (String) textview_email.getText();

        //Insert data
        dbHelper.insertData(username, age, password, email);
    }
}