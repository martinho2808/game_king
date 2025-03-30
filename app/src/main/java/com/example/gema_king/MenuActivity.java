package com.example.gema_king;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.gema_king.model.UserSession;
import com.example.gema_king.utils.Navigator;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu); // 替換為你的菜單文件名
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_leaderboard) {
            // handle LeaderBoard
            return true;
        } else if (item.getItemId() == R.id.action_login) {
            //Temp use for logout
            UserSession.getInstance().clearUserSession(this);
            Navigator.navigateTo(this, MainActivity.class);


            // handle Login
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}