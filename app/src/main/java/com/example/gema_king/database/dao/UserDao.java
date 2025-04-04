package com.example.gema_king.database.dao;

import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_AGE;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_EMAIL;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_EXPERIENCE;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_GAMES_PLAYED;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_LEVEL;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_PASSWORD;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_USERNAME;
import static com.example.gema_king.database.DatabaseHelper.USER_TABLE_NAME;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.gema_king.database.DatabaseHelper;
import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

public class UserDao {


    private static SQLiteDatabase getDatabase(Context context, boolean writable) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        return writable ? dbHelper.getWritableDatabase() : dbHelper.getReadableDatabase();
    }

    private static final String TAG = "UserDao";
    public static long insertUserData(Context context, String username, int age, String password, String email) {
        SQLiteDatabase db = getDatabase(context, true);

        ContentValues values = new ContentValues();
        values.put(USER_COLUMN_USERNAME, username);
        values.put(USER_COLUMN_AGE, age);
        values.put(USER_COLUMN_PASSWORD, password);
        values.put(USER_COLUMN_EMAIL, email);
        long newRowId;
        try {
            newRowId = db.insert(USER_TABLE_NAME, null, values);
            db.close();
            Log.d(TAG, String.valueOf(newRowId));

        } catch (Exception e) {
            //Log.e(TAG, e.getMessage());
            newRowId = -1;
            return newRowId;
        }

        return newRowId;
    }
    // 插入新用戶數據
    public static void insertData(Context context, String username, int age, String password, String email) throws Exception {
        // 首先檢查用戶名是否已存在
        if (isUsernameExists(context, username)) {
            throw new Exception("Username already exists");
        }

        SQLiteDatabase db = getDatabase(context, true);
        ContentValues values = new ContentValues();

        try {
            values.put(USER_COLUMN_USERNAME, username);
            values.put(USER_COLUMN_AGE, age);
            values.put(USER_COLUMN_PASSWORD, password);
            values.put(USER_COLUMN_EMAIL, email);
            values.put(USER_COLUMN_EXPERIENCE, 0);

            long result = db.insert(USER_TABLE_NAME, null, values);
            if (result == -1) {
                throw new Exception("Failed to insert data");
            }
            Log.i(TAG, "Data inserted successfully for username: " + username);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting data: " + e.getMessage());
            throw e;
        }
    }
    public static Map<String, Object> getUserDataById(Context context, long userId) {
        SQLiteDatabase db = getDatabase(context, false);
        Map<String, Object> userData = null;

        Cursor cursor = null;
        try {
            // 查詢用戶數據
            String[] columns = {USER_COLUMN_USERNAME, USER_COLUMN_AGE, USER_COLUMN_EMAIL};
            cursor = db.query(USER_TABLE_NAME, columns, "id = ?", new String[]{String.valueOf(userId)}, null, null, null);

            if (cursor.moveToFirst()) {
                userData = new HashMap<>();
                userData.put("id", userId);
                userData.put("username", cursor.getString(cursor.getColumnIndexOrThrow(USER_COLUMN_USERNAME)));
                userData.put("age", cursor.getInt(cursor.getColumnIndexOrThrow(USER_COLUMN_AGE)));
                userData.put("email", cursor.getString(cursor.getColumnIndexOrThrow(USER_COLUMN_EMAIL)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user data: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return userData;
    }

    public static boolean isUsernameExists(Context context, String username) {
        SQLiteDatabase db = getDatabase(context, true);
        Cursor cursor = null;
        try {
            cursor = db.query(USER_TABLE_NAME, new String[]{USER_COLUMN_USERNAME},
                    USER_COLUMN_USERNAME + " = ?", new String[]{username},
                    null, null, null);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    // 更新用戶遊戲數據
    @SuppressLint("Range")
    public void updateUserGameStats(Context context, String username, boolean isWin) {
        SQLiteDatabase db = getDatabase(context, true);
        ContentValues values = new ContentValues();

        // 先獲取當前數據
        Cursor cursor = getUserDataByUsername(context, username);
        if (cursor.moveToFirst()) {
            int currentGamesPlayed = cursor.getInt(cursor.getColumnIndex(USER_COLUMN_GAMES_PLAYED));
            int currentLevel = cursor.getInt(cursor.getColumnIndex(USER_COLUMN_LEVEL));
            int currentExperience = cursor.getInt(cursor.getColumnIndex(USER_COLUMN_EXPERIENCE));

            // 更新數據
            values.put(USER_COLUMN_GAMES_PLAYED, currentGamesPlayed + 1);
            if (isWin) {
                // 每贏一場加10點經驗值
                values.put(USER_COLUMN_EXPERIENCE, currentExperience + 10);
                // 每100點經驗值升一級
                if ((currentExperience + 10) >= currentLevel * 100) {
                    values.put(USER_COLUMN_LEVEL, currentLevel + 1);
                }
            }

            // 執行更新
            db.update(
                    USER_TABLE_NAME,
                    values,
                    USER_COLUMN_USERNAME + " = ?",
                    new String[]{username}
            );

            cursor.close();
        }
    }

    public void updateUserStats(Context context, String username, int level, int gamesPlayed, int totalPlayTime, int highestScore) {
        SQLiteDatabase db = getDatabase(context, true);
        ContentValues values = new ContentValues();
        values.put("level", level);
        values.put("games_played", gamesPlayed);
        values.put("total_play_time", totalPlayTime);
        values.put("highest_score", highestScore);
        db.update("user", values, "username = ?", new String[]{username});
    }
    // 獲取用戶完整資料

    public void updateUserExperience(Context context, String username, int experience) {
        SQLiteDatabase db = getDatabase(context, true);
        ContentValues values = new ContentValues();
        values.put(USER_COLUMN_EXPERIENCE, experience);
        db.update(USER_TABLE_NAME, values, USER_COLUMN_USERNAME + "=?", new String[]{username});
    }

    @SuppressLint("Range")
    public int getUserExperience(Context context, String username) {
        SQLiteDatabase db = getDatabase(context, false);
        Cursor cursor = db.query(USER_TABLE_NAME, new String[]{USER_COLUMN_EXPERIENCE},
                USER_COLUMN_USERNAME + "=?", new String[]{username},
                null, null, null);
        int experience = 0;
        if (cursor.moveToFirst()) {
            experience = cursor.getInt(cursor.getColumnIndex(USER_COLUMN_EXPERIENCE));
        }
        cursor.close();
        return experience;
    }
    public static Cursor getUserDataByUsername(Context context, String username) {
        SQLiteDatabase db = getDatabase(context, false);
        return db.query(
                USER_TABLE_NAME,
                null,  // 獲取所有列
                USER_COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );
    }
}
