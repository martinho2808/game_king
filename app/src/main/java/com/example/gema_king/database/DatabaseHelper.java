package com.example.gema_king.database;

import android.annotation.SuppressLint;
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
    public static final String USER_COLUMN_PASSWORD = "password";
    public static final String USER_COLUMN_AGE = "age";
    public static final String USER_COLUMN_EXPERIENCE = "experience";
    public static final String USER_COLUMN_EMAIL = "email";
    public static final String USER_COLUMN_LEVEL = "level";
    public static final String USER_COLUMN_GAMES_PLAYED = "games_played";
    public static final String GAME_STATUS_TABLE_NAME = "game_status";

    public static final String GAME_STATUS_COLUMN_ID = "id";
    public static final String GAME_STATUS_COLUMN_USER_ID = "user_id";
    public static final String GAME_STATUS_COLUMN_GAME_ID = "game_id";
    public static final String GAME_STATUS_COLUMN_SCORE = "score";
    public static final String GAME_STATUS_COLUMN_PLAY_TIME = "play_time";
    public static final String GAME_STATUS_COLUMN_STATUS = "status";
    //    SQL query to create table

    private static final String CREATE_TABLE = "CREATE TABLE " + USER_TABLE_NAME + "("
            + USER_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + USER_COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, "
            + USER_COLUMN_PASSWORD + " TEXT NOT NULL, "
            + USER_COLUMN_EMAIL + " TEXT, "
            + USER_COLUMN_AGE + " INTEGER NOT NULL, "
            + USER_COLUMN_EXPERIENCE + " INTEGER DEFAULT 0, "
            + USER_COLUMN_LEVEL + " INTEGER DEFAULT 1, "
            + USER_COLUMN_GAMES_PLAYED + " INTEGER DEFAULT 0, "
            + "total_play_time INTEGER DEFAULT 0, "
            + "highest_score INTEGER DEFAULT 0"
            + ")";
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

    @SuppressLint("Range")
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cursor cursor = db.query(USER_TABLE_NAME, null, null, null, null, null, null);
        ContentValues[] oldData = null;
        if (cursor.moveToFirst()) {
            oldData = new ContentValues[cursor.getCount()];
            int i = 0;
            do {
                ContentValues values = new ContentValues();
                values.put(USER_COLUMN_USERNAME, cursor.getString(cursor.getColumnIndex(USER_COLUMN_USERNAME)));
                values.put(USER_COLUMN_PASSWORD, cursor.getString(cursor.getColumnIndex(USER_COLUMN_PASSWORD)));
                values.put(USER_COLUMN_EMAIL, cursor.getString(cursor.getColumnIndex(USER_COLUMN_EMAIL)));
                values.put(USER_COLUMN_AGE, cursor.getInt(cursor.getColumnIndex(USER_COLUMN_AGE)));
                values.put(USER_COLUMN_EXPERIENCE, cursor.getString(cursor.getColumnIndex(USER_COLUMN_EXPERIENCE)));
                values.put(USER_COLUMN_LEVEL, cursor.getInt(cursor.getColumnIndex(USER_COLUMN_LEVEL)));
                values.put(USER_COLUMN_GAMES_PLAYED, cursor.getInt(cursor.getColumnIndex(USER_COLUMN_GAMES_PLAYED)));
                oldData[i++] = values;
            } while (cursor.moveToNext());
            cursor.close();
        }

        // 刪除舊表
        db.execSQL("DROP TABLE IF EXISTS " + GAME_STATUS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE_NAME);

        // 創建新表
        onCreate(db);

        // 恢復數據
        if (oldData != null) {
            for (ContentValues values : oldData) {
                db.insert(USER_TABLE_NAME, null, values);
            }
        }

        Log.i(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
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
