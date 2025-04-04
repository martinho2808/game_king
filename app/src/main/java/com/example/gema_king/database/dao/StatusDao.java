package com.example.gema_king.database.dao;

import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_COLUMN_PLAY_TIME;
import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_COLUMN_GAME_ID;
import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_COLUMN_PLAY_TIME;
import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_COLUMN_SCORE;
import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_COLUMN_STATUS;
import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_COLUMN_USER_ID;
import static com.example.gema_king.database.DatabaseHelper.GAME_STATUS_TABLE_NAME;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_ID;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gema_king.database.DatabaseHelper;

public class StatusDao {

    private static SQLiteDatabase getDatabase(Context context, boolean writable) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        return writable ? dbHelper.getWritableDatabase() : dbHelper.getReadableDatabase();
    }

    private static final String TAG = "StatusDao";

    // 添加遊戲記錄
    public long addGameRecord(Context context, String username, int gameId, int score, int playTime, String status) {
        SQLiteDatabase db = getDatabase(context, true);
        ContentValues values = new ContentValues();

        // 獲取用戶ID
        Cursor cursor = UserDao.getUserDataByUsername(context, username);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int userId = cursor.getInt(cursor.getColumnIndex(USER_COLUMN_ID));
            cursor.close();

            values.put(GAME_STATUS_COLUMN_USER_ID, userId);
            values.put(GAME_STATUS_COLUMN_GAME_ID, gameId);
            values.put(GAME_STATUS_COLUMN_SCORE, score);
            values.put(GAME_STATUS_COLUMN_PLAY_TIME, playTime);
            values.put(GAME_STATUS_COLUMN_STATUS, status);

            return db.insert(GAME_STATUS_TABLE_NAME, null, values);
        }
        return -1;
    }

    // 獲取用戶最近的遊戲記錄
    public Cursor getRecentGames(Context context, String username, int limit) {
        SQLiteDatabase db = getDatabase(context, false);

        // 獲取用戶ID
        Cursor userCursor = UserDao.getUserDataByUsername(context, username);
        if (userCursor.moveToFirst()) {
            @SuppressLint("Range") int userId = userCursor.getInt(userCursor.getColumnIndex(USER_COLUMN_ID));
            userCursor.close();

            // 查詢最近的遊戲記錄
            String query = "SELECT * FROM " + GAME_STATUS_TABLE_NAME +
                    " WHERE " + GAME_STATUS_COLUMN_USER_ID + " = ?" +
                    " ORDER BY " + GAME_STATUS_COLUMN_PLAY_TIME + " DESC" +
                    " LIMIT " + limit;

            return db.rawQuery(query, new String[]{String.valueOf(userId)});
        }
        return null;
    }
    @SuppressLint("Range")
    // 獲取用戶最高分數
    public int getHighestScore(Context context, String username) {
        SQLiteDatabase db = getDatabase(context, false);

        // 獲取用戶ID
        Cursor userCursor = UserDao.getUserDataByUsername(context, username);
        if (userCursor.moveToFirst()) {
            int userId = userCursor.getInt(userCursor.getColumnIndex(USER_COLUMN_ID));
            userCursor.close();

            // 查詢最高分數
            String query = "SELECT MAX(" + GAME_STATUS_COLUMN_SCORE + ") as max_score" +
                    " FROM " + GAME_STATUS_TABLE_NAME +
                    " WHERE " + GAME_STATUS_COLUMN_USER_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
            if (cursor.moveToFirst()) {
                int highestScore = cursor.getInt(cursor.getColumnIndex("max_score"));
                cursor.close();
                return highestScore;
            }
        }
        return 0;
    }


}
