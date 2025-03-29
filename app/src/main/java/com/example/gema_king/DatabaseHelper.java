package com.example.gema_king;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MyDatabase.db";
    private static final String TABLE_NAME = "MyTable";

    //    Table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_AGE = "age";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_EMAIL = "email";

    //    SQL query to create table
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
    " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_USERNAME + " TEXT, "
            + COLUMN_AGE + " INTEGER, "
            + COLUMN_PASSWORD + " INTEGER, "
            + COLUMN_EMAIL + " TEXT);";

    private static final String TAG = "MyApp";
    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertData(String username, int age, String password, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        long newRowId;

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_AGE, age);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_EMAIL, email);

        try {
            newRowId = db.insert(TABLE_NAME, null, values);
            Log.d(TAG, String.valueOf(newRowId));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        db.close();
    }

    public boolean readData(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = String.format("SELECT COUNT(*) FROM %s WHERE username = '%s' AND password = '%s';", TABLE_NAME, username, password);
        Cursor cursor = db.rawQuery(query, null);

        int count;

        cursor.moveToFirst();
        count = cursor.getInt(0);
        if (count >= 1) {
            cursor.close();
            db.close();
            return true;
        }

        Log.d("Count", "Number of rows: " + count);

        cursor.close();
        db.close();

        return false;
    }
}
