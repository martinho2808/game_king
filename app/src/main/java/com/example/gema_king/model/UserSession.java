package com.example.gema_king.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;

public class UserSession {
    private static UserSession instance;
    private static final String PREFS_NAME = "UserSessionPrefs";
    private static final String KEY_USER_DATA = "currentUserData";

    private static String userDataJson = null;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void saveUserSession(Context context, long id, String username, int age, String email) {
        try {
            JSONObject userJson = new JSONObject();
            userJson.put("id", id);
            userJson.put("username", username);
            userJson.put("age", age);
            userJson.put("email", email);

            userDataJson = userJson.toString();

            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_USER_DATA, userDataJson);
            editor.apply();
        } catch (Exception e) {
            Log.e("UserSession", "Error saving user session: " + e.getMessage());
        }
    }


    public static JSONObject getUserSession(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userDataJson = preferences.getString(KEY_USER_DATA, null);

        if (userDataJson != null) {
            try {
                return new JSONObject(userDataJson);
            } catch (Exception e) {
                Log.e("UserSession", "Error parsing user session: " + e.getMessage());
            }
        }
        return null;
    }

    public void clearUserSession(Context context) {
        userDataJson = null;
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_USER_DATA);
        editor.apply();
    }

    public static long getUserId(Context context) {
        JSONObject user = getUserSession(context);
        if (user != null) {
            return user.optLong("id", -1);
        }
        return 0;
    }

    public String getUsername(Context context) {
        JSONObject user = getUserSession(context);
        if (user != null) {
            return user.optString("username", null);
        }
        return null;
    }

    public int getAge(Context context) {
        JSONObject user = getUserSession(context);
        if (user != null) {
            return user.optInt("age", -1);
        }
        return -1;
    }

    public String getEmail(Context context) {
        JSONObject user = getUserSession(context);
        if (user != null) {
            return user.optString("email", null);
        }
        return null;
    }
}