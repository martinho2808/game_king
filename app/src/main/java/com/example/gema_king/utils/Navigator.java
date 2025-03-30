package com.example.gema_king.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Navigator {
    private static final String TAG = "Navigator";
    public static void navigateTo(Context context, Class<?> targetActivity) {
        Log.i(TAG, "Navigating to " + targetActivity.getSimpleName() );
        Intent intent = new Intent(context, targetActivity);
        context.startActivity(intent);
    }
}
