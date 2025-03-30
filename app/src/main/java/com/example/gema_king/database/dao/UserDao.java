package com.example.gema_king.database.dao;

import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_AGE;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_EMAIL;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_PASSWORD;
import static com.example.gema_king.database.DatabaseHelper.USER_COLUMN_USERNAME;
import static com.example.gema_king.database.DatabaseHelper.USER_TABLE_NAME;

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

    public static Map<String, Object> getUserData(Context context, long userId) {
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
}
