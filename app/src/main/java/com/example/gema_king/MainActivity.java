package com.example.gema_king;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gema_king.model.UserSession;
import com.example.gema_king.utils.Navigator;

import org.json.JSONObject;

public class MainActivity extends MenuActivity implements View.OnClickListener{
    Button sign_up;
    Button login;
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //Get session
        JSONObject userSession = UserSession.getInstance().getUserSession(this);
        if (userSession != null) {
            try {
                String username = userSession.getString("username");
                Toast.makeText(this, "Welcome to Game King, " + username, Toast.LENGTH_LONG).show();
                Navigator.navigateTo(MainActivity.this, GameActivity.class);
                finish(); // End the current page to prevent the user from returning to the login page

            } catch (Exception e) {
                UserSession.getInstance().clearUserSession(this);
                Log.e(TAG, "Error retrieving user session data: " + e.getMessage());
            }
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            Toast.makeText(this, "Welcome to Game King", Toast.LENGTH_LONG).show();

            sign_up = (Button) findViewById(R.id.login_btn_signup);;
            login = (Button) findViewById(R.id.login_btn_login);

            sign_up.setOnClickListener(this);
            login.setOnClickListener(this);
        }


    }

    @Override
    public void onClick(View v) {
        Intent i;
        if(v.getId() == sign_up.getId()) {
            i = new Intent(MainActivity.this, RegisterActivity.class);
        } else {
            i = new Intent(MainActivity.this, LoginActivity.class);
        }
        startActivity(i);
    }
}