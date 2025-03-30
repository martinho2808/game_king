package com.example.gema_king.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper instance;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "game-app.db";
    public static final String USER_TABLE_NAME = "user";

    //    Table columns
    public static final String USER_COLUMN_ID = "id";
    public static final String USER_COLUMN_USERNAME = "username";
    public static final String USER_COLUMN_AGE = "age";
    public static final String USER_COLUMN_PASSWORD = "password";
    public static final String USER_COLUMN_EMAIL = "email";
    public static final String GAME_STATUS_TABLE_NAME = "game_status";

    // Table columns
    private static final String GAME_STATUS_COLUMN_ID = "id";
    private static final String GAME_STATUS_COLUMN_USER_ID = "user_id"; // 關聯到用戶表
    private static final String GAME_STATUS_COLUMN_GAME_ID = "game_id"; // 遊戲名稱
    private static final String GAME_STATUS_COLUMN_SCORE = "score"; // 分數
    private static final String GAME_STATUS_COLUMN_PLAY_TIME = "play_time"; // 遊玩時間
    private static final String GAME_STATUS_COLUMN_STATUS = "status"; // 遊戲狀態
    //    SQL query to create table
    private static final String CREATE_TABLE = "CREATE TABLE " + USER_TABLE_NAME +
    " ("
            + USER_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + USER_COLUMN_USERNAME + " TEXT, "
            + USER_COLUMN_AGE + " INTEGER, "
            + USER_COLUMN_PASSWORD + " INTEGER, "
            + USER_COLUMN_EMAIL + " TEXT);";

    private static final String CREATE_GAME_STATUS_TABLE = "CREATE TABLE " + GAME_STATUS_TABLE_NAME +
            " ("
            + GAME_STATUS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + GAME_STATUS_COLUMN_USER_ID + " INTEGER, "
            + GAME_STATUS_COLUMN_GAME_ID + " INTEGER, "
            + GAME_STATUS_COLUMN_SCORE + " INTEGER, "
            + GAME_STATUS_COLUMN_PLAY_TIME + " INTEGER, "
            + GAME_STATUS_COLUMN_STATUS + " TEXT, "
            + "FOREIGN KEY(" + GAME_STATUS_COLUMN_USER_ID + ") REFERENCES " + USER_TABLE_NAME + "(" + USER_COLUMN_ID + ") ON DELETE CASCADE);";
    private static final String TAG = "DatabaseHelper";
    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_GAME_STATUS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE_NAME);
        onCreate(db);
    }
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

//    public void insertData(String username, int age, String password, String email) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        long newRowId;
//
//        ContentValues values = new ContentValues();
//        values.put(USER_COLUMN_USERNAME, username);
//        values.put(USER_COLUMN_AGE, age);
//        values.put(USER_COLUMN_PASSWORD, password);
//        values.put(USER_COLUMN_EMAIL, email);
//
//        try {
//            newRowId = db.insert(USER_TABLE_NAME, null, values);
//            Log.d(TAG, String.valueOf(newRowId));
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage());
//        }
//
//        db.close();
//    }

//    public boolean readData(String username, String password) {
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        String query = String.format("SELECT COUNT(*) FROM %s WHERE username = '%s' AND password = '%s';", USER_TABLE_NAME, username, password);
//        Cursor cursor = db.rawQuery(query, null);
//
//        int count;
//
//        cursor.moveToFirst();
//        count = cursor.getInt(0);
//        if (count >= 1) {
//            cursor.close();
//            db.close();
//            return true;
//        }
//
//        Log.d("Count", "Number of rows: " + count);
//
//        cursor.close();
//        db.close();
//
//        return false;
//    }
}
