package com.example.gema_king;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView detailListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_detail);

        dbHelper = new DatabaseHelper(this);
        detailListView = findViewById(R.id.detail_list);
        TextView playerDetailTitle = findViewById(R.id.detail_title);

        int userId = getIntent().getIntExtra("userId", -1);

        if (userId != -1) {
            playerDetailTitle.setText(getString(R.string.detail_title));
            loadPlayerGameDetails(userId);
        }
        Log.d("PlayerDetail", "接收到 userId: " + userId);
    }

    private void loadPlayerGameDetails(int userId) {
        List<HashMap<String, String>> data = new ArrayList<>();

        // 對應顯示名稱：Game 1 ~ Game 10
        int[] gameIds = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

        // 初始化每個遊戲為 -
        for (int i = 0; i < gameIds.length; i++) {
            HashMap<String, String> map = new HashMap<>();
            map.put("game", getString(R.string.signal_game_id, i + 1));
            map.put("score", getString(R.string.signal_score, 0));
            map.put("time", getString(R.string.signal_time, 0));
            data.add(map);
        }

        // 查詢資料庫紀錄
        Log.d("PlayerDetail", "開始查詢 userId=" + userId);

        String query = "SELECT game_id, score, play_time FROM game_status WHERE user_id = ?";
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, new String[]{String.valueOf(userId)});
        Log.d("PlayerDetail", "資料筆數：" + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int gameId = cursor.getInt(cursor.getColumnIndex("game_id"));
                @SuppressLint("Range") int score = cursor.getInt(cursor.getColumnIndex("score"));
                @SuppressLint("Range") int time = cursor.getInt(cursor.getColumnIndex("play_time"));

                // Find the position of gameId in the array
                for (int i = 0; i < gameIds.length; i++) {
                    if (gameIds[i] == gameId) {
                        HashMap<String, String> map = data.get(i);
                        map.put("score", getString(R.string.signal_score, score));
                        map.put("time", getString(R.string.signal_time, time));
                        break;
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                R.layout.item_player_detail,
                new String[]{"game", "score", "time"},
                new int[]{R.id.signal_game_id_text, R.id.signal_score_text, R.id.signal_time_text}
        );

        detailListView.setAdapter(adapter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
