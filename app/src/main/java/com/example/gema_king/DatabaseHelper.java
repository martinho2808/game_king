package com.example.gema_king;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.text.SimpleDateFormat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.gema_king.model.GameStatus;


import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "gameking.db";
    private static final String TABLE_USERS = "users";

    //    Table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_AGE = "age";
    private static final String COLUMN_EXPERIENCE = "experience";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_LEVEL = "level";
    private static final String COLUMN_GAMES_PLAYED = "games_played";

    // Game Status table
    public static final String TABLE_GAME_STATUS = "game_status";
    public static final String GAME_STATUS_COLUMN_ID = "_id";
    public static final String GAME_STATUS_COLUMN_USER_ID = "user_id";
    public static final String GAME_STATUS_COLUMN_GAME_ID = "game_id";
    public static final String GAME_STATUS_COLUMN_SCORE = "score";
    public static final String GAME_STATUS_COLUMN_PLAY_TIME = "play_time";
    public static final String GAME_STATUS_COLUMN_STATUS = "status";
    public static final String GAME_STATUS_COLUMN_DATE = "play_date";

    //    SQL query to create table
    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, "
            + COLUMN_PASSWORD + " TEXT NOT NULL, "
            + COLUMN_EMAIL + " TEXT, "
            + COLUMN_AGE + " INTEGER NOT NULL, "
            + COLUMN_EXPERIENCE + " INTEGER DEFAULT 0, "
            + COLUMN_LEVEL + " INTEGER DEFAULT 1, "
            + COLUMN_GAMES_PLAYED + " INTEGER DEFAULT 0, "
            + "total_play_time INTEGER DEFAULT 0, "
            + "highest_score INTEGER DEFAULT 0"
            + ")";

    private static final String CREATE_GAME_STATUS_TABLE = "CREATE TABLE " + TABLE_GAME_STATUS + "("
            + GAME_STATUS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + GAME_STATUS_COLUMN_USER_ID + " INTEGER NOT NULL, "
            + GAME_STATUS_COLUMN_GAME_ID + " INTEGER NOT NULL, "
            + GAME_STATUS_COLUMN_SCORE + " INTEGER DEFAULT 0, "
            + GAME_STATUS_COLUMN_PLAY_TIME + " INTEGER DEFAULT 0, "
            + GAME_STATUS_COLUMN_STATUS + " TEXT NOT NULL, "
            + GAME_STATUS_COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY(" + GAME_STATUS_COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
            + ")";

    private static final String TAG = "DatabaseHelper";
    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_GAME_STATUS_TABLE);
        Log.i(TAG, "Database tables created");
    }

    @SuppressLint("Range")
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 保存舊的數據
        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, null);
        ContentValues[] oldData = null;
        if (cursor != null && cursor.moveToFirst()) {
            oldData = new ContentValues[cursor.getCount()];
            int i = 0;
            do {
                ContentValues values = new ContentValues();
                values.put(COLUMN_USERNAME, cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)));
                values.put(COLUMN_PASSWORD, cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD)));
                values.put(COLUMN_EMAIL, cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)));
                values.put(COLUMN_AGE, cursor.getInt(cursor.getColumnIndex(COLUMN_AGE)));
                values.put(COLUMN_LEVEL, cursor.getInt(cursor.getColumnIndex(COLUMN_LEVEL)));
                values.put(COLUMN_GAMES_PLAYED, cursor.getInt(cursor.getColumnIndex(COLUMN_GAMES_PLAYED)));
                oldData[i++] = values;
            } while (cursor.moveToNext());
            cursor.close();
        }

        // 刪除舊表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAME_STATUS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        
        // 創建新表
        onCreate(db);

        // 恢復數據
        if (oldData != null) {
            for (ContentValues values : oldData) {
                db.insert(TABLE_USERS, null, values);
            }
        }
        
        Log.i(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
    }

    // 檢查用戶名是否已存在
    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_USERNAME},
                    COLUMN_USERNAME + " = ?", new String[]{username},
                    null, null, null);
            return cursor != null && cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // 插入新用戶數據
    public void insertData(String username, int age, String password, String email) throws Exception {
        // 首先檢查用戶名是否已存在
        if (isUsernameExists(username)) {
            throw new Exception("Username already exists");
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        try {
            values.put(COLUMN_USERNAME, username);
            values.put(COLUMN_AGE, age);
            values.put(COLUMN_PASSWORD, password);
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_EXPERIENCE, 0);

            long result = db.insert(TABLE_USERS, null, values);
            if (result == -1) {
                throw new Exception("Failed to insert data");
            }
            Log.i(TAG, "Data inserted successfully for username: " + username);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting data: " + e.getMessage());
            throw e;
        }
    }

    // 驗證用戶登入
    public boolean readData(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_USERNAME},
                    COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?",
                    new String[]{username, password},
                    null, null, null);
            return cursor != null && cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // 獲取用戶完整資料
    public Cursor getUserData(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
            TABLE_USERS,
            null,  // 獲取所有列
            COLUMN_USERNAME + " = ?",
            new String[]{username},
            null, null, null
        );
    }

    // 更新用戶遊戲數據
    public void updateUserGameStats(String username, boolean isWin) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        // 先獲取當前數據
        Cursor cursor = getUserData(username);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") int currentGamesPlayed = cursor.getInt(cursor.getColumnIndex(COLUMN_GAMES_PLAYED));
            @SuppressLint("Range") int currentLevel = cursor.getInt(cursor.getColumnIndex(COLUMN_LEVEL));
            @SuppressLint("Range") int currentExperience = cursor.getInt(cursor.getColumnIndex(COLUMN_EXPERIENCE));
            
            // 更新數據
            values.put(COLUMN_GAMES_PLAYED, currentGamesPlayed + 1);
            if (isWin) {
                // 每贏一場加10點經驗值
                values.put(COLUMN_EXPERIENCE, currentExperience + 10);
                // 每100點經驗值升一級
                if ((currentExperience + 10) >= currentLevel * 100) {
                    values.put(COLUMN_LEVEL, currentLevel + 1);
                }
            }
            
            // 執行更新
            db.update(
                TABLE_USERS,
                values,
                COLUMN_USERNAME + " = ?",
                new String[]{username}
            );
            
            cursor.close();
        }
    }

    // 添加遊戲記錄
    public long addGameRecord(int userId, int gameId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(GAME_STATUS_COLUMN_USER_ID, userId);
        values.put(GAME_STATUS_COLUMN_GAME_ID, gameId);
        values.put(GAME_STATUS_COLUMN_STATUS, status);

        long id = db.insert(TABLE_GAME_STATUS, null, values);
        //db.close();
        return id; // 返回新插入记录的 ID
    }

    // 獲取用戶最近的遊戲記錄
    public Cursor getRecentGames(String username, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 獲取用戶ID
        Cursor userCursor = getUserData(username);
        if (userCursor != null && userCursor.moveToFirst()) {
            int userId = userCursor.getInt(userCursor.getColumnIndex(COLUMN_ID));
            userCursor.close();
            
            // 查詢最近的遊戲記錄
            String query = "SELECT * FROM " + TABLE_GAME_STATUS +
                    " WHERE " + GAME_STATUS_COLUMN_USER_ID + " = ?" +
                    " ORDER BY " + GAME_STATUS_COLUMN_DATE + " DESC" +
                    " LIMIT " + limit;
            
            return db.rawQuery(query, new String[]{String.valueOf(userId)});
        }
        return null;
    }

    // 獲取用戶最高分數
    public int getHighestScore(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 獲取用戶ID
        Cursor userCursor = getUserData(username);
        if (userCursor != null && userCursor.moveToFirst()) {
            int userId = userCursor.getInt(userCursor.getColumnIndex(COLUMN_ID));
            userCursor.close();
            
            // 查詢最高分數
            String query = "SELECT MAX(" + GAME_STATUS_COLUMN_SCORE + ") as max_score" +
                    " FROM " + TABLE_GAME_STATUS +
                    " WHERE " + GAME_STATUS_COLUMN_USER_ID + " = ?";
            
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
            if (cursor != null && cursor.moveToFirst()) {
                int highestScore = cursor.getInt(cursor.getColumnIndex("max_score"));
                cursor.close();
                return highestScore;
            }
        }
        return 0;
    }

    public void updateUserStats(String username, int level, int gamesPlayed, int totalPlayTime, int highestScore) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("level", level);
        values.put("games_played", gamesPlayed);
        values.put("total_play_time", totalPlayTime);
        values.put("highest_score", highestScore);
        db.update("users", values, "username = ?", new String[]{username});
    }

    public void updateUserExperience(String username, int experience) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPERIENCE, experience);
        db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
    }

    public int getUserExperience(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_EXPERIENCE},
                COLUMN_USERNAME + "=?", new String[]{username},
                null, null, null);
        int experience = 0;
        if (cursor.moveToFirst()) {
            experience = cursor.getInt(cursor.getColumnIndex(COLUMN_EXPERIENCE));
        }
        cursor.close();
        return experience;
    }
    // Check if a record exists for a specific user and game
    @SuppressLint("Range")
    public Integer recordExists(int userId, int gameId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + GAME_STATUS_COLUMN_ID + " FROM " + TABLE_GAME_STATUS +
                " WHERE " + GAME_STATUS_COLUMN_USER_ID + " = ? AND " +
                GAME_STATUS_COLUMN_GAME_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(gameId)});

        Integer recordId = null;  // default null
        if (cursor.moveToFirst()) {
            recordId = cursor.getInt(cursor.getColumnIndex(GAME_STATUS_COLUMN_ID)); // 获取记录 ID
        }
        cursor.close();
        //db.close();
        return recordId; // Returns the record ID, or null if it does not exist
    }

    public void updateStatusById(int id, String newStatus, int score, int newPlayTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(GAME_STATUS_COLUMN_STATUS, newStatus);
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put(GAME_STATUS_COLUMN_DATE, currentTime);
        values.put(GAME_STATUS_COLUMN_SCORE, score);
        values.put(GAME_STATUS_COLUMN_PLAY_TIME, newPlayTime);



        db.update(TABLE_GAME_STATUS, values, GAME_STATUS_COLUMN_ID + "=?", new String[]{String.valueOf(id)});



       // int rowsAffected = db.update(TABLE_GAME_STATUS, values, GAME_STATUS_COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        //db.close();
        //return rowsAffected; // Return the number of rows affected
    }
    public GameStatus getStatusById(int recordId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT g." + GAME_STATUS_COLUMN_STATUS + ", " +
                "g." + GAME_STATUS_COLUMN_SCORE + ", " +
                "g." + GAME_STATUS_COLUMN_PLAY_TIME + ", " +
                "u." + COLUMN_USERNAME + " FROM " +
                TABLE_GAME_STATUS + " g " +
                "JOIN " + TABLE_USERS + " u ON u." + COLUMN_ID + " = g." + GAME_STATUS_COLUMN_USER_ID + " " + // 使用正确的 JOIN 条件
                "WHERE g." + GAME_STATUS_COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(recordId)});

        GameStatus gameStatus = null; // 初始化为 null
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String status = cursor.getString(cursor.getColumnIndex(GAME_STATUS_COLUMN_STATUS));
            @SuppressLint("Range") int score = cursor.getInt(cursor.getColumnIndex(GAME_STATUS_COLUMN_SCORE));
            @SuppressLint("Range") int playTime = cursor.getInt(cursor.getColumnIndex(GAME_STATUS_COLUMN_PLAY_TIME));
            @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
            gameStatus = new GameStatus(username, status, score, playTime); // 创建 GameStatus 对象
        }
        cursor.close();
        //db.close();
        return gameStatus; // 返回 GameStatus 对象或 null
    }
    public void updateGamePlayedByUserId(int UserId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String updateQuery = "UPDATE " + TABLE_USERS +
                " SET " + COLUMN_GAMES_PLAYED + " = " + COLUMN_GAMES_PLAYED + " + 1" +
                " WHERE " + COLUMN_ID + " = ?";
        db.execSQL(updateQuery, new Object[]{UserId});
        db.close();

    }

    public HashMap<String, Object> getUserGameStatsSimple(int userId) {
        HashMap<String, Object> result = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " +
                "user.username, " +
                "user.games_played, " +
                "user.experience, " +
                "user.level, " +
                "user.age, " +
                "COALESCE(SUM(gs.play_time), 0) AS total_play_time, " +
                "COALESCE(SUM(gs.score), 0) AS total_score, " +
                "SUM(CASE WHEN gs.status = 'Finished' THEN 1 ELSE 0 END) as total_finished_game, " +
                "SUM(CASE WHEN gs.status IN ('In Progress', 'Stopped', 'Not Started') THEN 1 ELSE 0 END) as total_progress_game " +
                "FROM users user " +
                "LEFT JOIN game_status gs ON user.id = gs.user_id " +
                "WHERE user.id = ? " +
                "GROUP BY user.username, user.games_played";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                result.put("username", cursor.getString(0));
                result.put("games_played", cursor.getInt(1));
                result.put("experience", cursor.getInt(2));
                result.put("level", cursor.getInt(3));
                result.put("age", cursor.getInt(4));
                result.put("total_play_time", cursor.getInt(5));
                result.put("total_score", cursor.getInt(6));
                result.put("total_finished_game", cursor.getInt(7));
                result.put("total_progress_game", cursor.getInt(8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

