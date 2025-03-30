package com.example.gema_king.database.dao;


import static com.example.gema_king.database.DatabaseHelper.USER_TABLE_NAME;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.example.gema_king.database.DatabaseHelper;

public class LoginDao {

    private static SQLiteDatabase getDatabase(Context context, boolean writable) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        return writable ? dbHelper.getWritableDatabase() : dbHelper.getReadableDatabase();
    }

    private static final String TAG = "LoginDao";

    public static long loginValidate(Context context, String username, String password) {
        SQLiteDatabase db = getDatabase(context, false);
        long userId = -1;  // 預設為 -1 表示登入失敗

        Cursor cursor = null;
        try {
            // 查詢 ID
            String query = "SELECT id FROM " + USER_TABLE_NAME + " WHERE username = ? AND password = ?";
            cursor = db.rawQuery(query, new String[]{username, password});

            if (cursor.moveToFirst()) {
                userId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during login validation: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return userId; // 返回用戶 ID，失敗則返回 -1
    }


}
