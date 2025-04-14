package com.example.gema_king;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gema_king.model.UserSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class LeaderboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView leaderboardList;
    private TextView currentScoreText, currentTimeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        dbHelper = new DatabaseHelper(this);

        leaderboardList = findViewById(R.id.leaderboard_list);
        currentScoreText = findViewById(R.id.current_player_score);
        currentTimeText = findViewById(R.id.current_player_use_time);

        logAllGameStatus();
        loadLeaderboard();
        loadCurrentPlayerRecord();

        Button detailButton = findViewById(R.id.btn_details);
        detailButton.setOnClickListener(v -> {
            long userId = UserSession.getUserId(this);
            Intent intent = new Intent(LeaderboardActivity.this, PlayerDetailActivity.class);
            intent.putExtra("userId", (int) userId);
            startActivity(intent);
        });
    }

    private void logAllGameStatus() {
        Cursor c = dbHelper.getReadableDatabase()
                .rawQuery("SELECT user_id, game_id, score, play_time, status FROM game_status", null);

        Log.d("DB-DUMP", "=== 所有 game_status 記錄 ===");
        if (c.moveToFirst()) {
            do {
                int userId = c.getInt(0);
                int gameId = c.getInt(1);
                int score = c.getInt(2);
                int time = c.getInt(3);
                String status = c.getString(4);
                Log.d("DB-DUMP", "user=" + userId + " game=" + gameId + " score=" + score + " time=" + time + " status=" + status);
            } while (c.moveToNext());
        } else {
            Log.d("DB-DUMP", "❌ 沒有任何 game_status 記錄");
        }
        c.close();
    }

    private void loadLeaderboard() {
        List<HashMap<String, String>> leaderboardData = new ArrayList<>();

        String query = "SELECT users.id as user_id, users.username, SUM(g.score) as total_score, SUM(g.play_time) as total_time " +
                "FROM game_status g INNER JOIN users ON users.id = g.user_id " +
                "GROUP BY g.user_id " +
                "ORDER BY total_score DESC, total_time ASC " +
                "LIMIT 5";

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, null);

        if (cursor.moveToFirst()) {
            int rank = 1;
            do {
                @SuppressLint("Range") int userId = cursor.getInt(cursor.getColumnIndex("user_id"));
                @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex("username"));
                @SuppressLint("Range") int score = cursor.getInt(cursor.getColumnIndex("total_score"));
                @SuppressLint("Range") int time = cursor.getInt(cursor.getColumnIndex("total_time"));

                HashMap<String, String> map = new HashMap<>();
                map.put("rank", rank + ".");
                map.put("username", username);
                map.put("score", getString(R.string.ranking_score, score));
                map.put("time", getString(R.string.ranking_time, time));
                map.put("userId", String.valueOf(userId));
                leaderboardData.add(map);
                rank++;
            } while (cursor.moveToNext());
            cursor.close();
        }

        LeaderboardAdapter adapter = new LeaderboardAdapter(this, leaderboardData);
        leaderboardList.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void loadCurrentPlayerRecord() {
        long userId = UserSession.getUserId(this);
        String query = "SELECT SUM(score) as total_score, SUM(play_time) as total_time " +
                "FROM game_status WHERE user_id = ?";

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int score = cursor.getInt(cursor.getColumnIndex("total_score"));
            @SuppressLint("Range") int time = cursor.getInt(cursor.getColumnIndex("total_time"));

            currentScoreText.setText(getString(R.string.current_player_score, score));
            currentTimeText.setText(getString(R.string.current_player_use_time, time));
            cursor.close();
        } else {
            currentScoreText.setText(getString(R.string.current_player_score, 0));
            currentTimeText.setText(getString(R.string.current_player_use_time, 0));
        }

        Log.d("Leaderboard", "✅ 玩家 userId=" + userId + " 的排行榜資料已載入");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
