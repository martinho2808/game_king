package com.example.gema_king;

import android.graphics.Color;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Game7Activity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "GameActivity";

    private ImageView cryingGirl, happyGirl, bone, goodDog, badDog, bucket, shovel;

    private View parentView;

    private float dX, dY;
    private int lastAction;

    private boolean isColliding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game7);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cryingGirl = findViewById(R.id.cryingGirl);
        happyGirl = findViewById(R.id.happyGirl);

        bone = findViewById(R.id.bone);
        goodDog = findViewById(R.id.goodDog);
        badDog = findViewById(R.id.badDog);
        bucket = findViewById(R.id.bucket);
        shovel = findViewById(R.id.shovel);
    }

    @Override
    protected void onResume() {
        super.onResume();

        shovel.setVisibility(View.INVISIBLE);
        bone.setVisibility(View.INVISIBLE);

        bucket.setOnClickListener(this);

        parentView = (View) shovel.getParent();
        parentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                parentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int parentWidth = parentView.getWidth();
                int parentHeight = parentView.getHeight();

                shovel.setOnTouchListener(createDragListener(parentWidth, parentHeight));
                bone.setOnTouchListener(createDragListener(parentWidth, parentHeight));
            }
        });
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "Start clicking");
        if (v.getId() == bucket.getId()) {
            shovel.setVisibility(View.VISIBLE);
            shovel.setClickable(true);
        }
    }
    private View.OnTouchListener createDragListener(final int parentWidth, final int parentHeight) {
        return new View.OnTouchListener() {
            private float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Log.i(TAG, "In touch mode");
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "Action down");
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        if(isColliding) {
                            Log.i(TAG, "changing the visibility of bone");
                            bone.setVisibility(View.VISIBLE);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Apply constraints
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX > parentWidth - view.getWidth())
                            newX = parentWidth - view.getWidth();
                        if (newY > parentHeight - view.getHeight())
                            newY = parentHeight - view.getHeight();

                        view.setX(newX);
                        view.setY(newY);

                        // Check collision
                        isColliding = isCollision(view, bone);
                        isColliding = isCollision(view, badDog);
                        Log.i(TAG, "Collide: " + isColliding);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        };
    }

    private boolean isCollision(View view1, View view2) {
        Rect rect1 = new Rect();
        view1.getHitRect(rect1);

        Rect rect2 = new Rect();
        view2.getHitRect(rect2);

        return Rect.intersects(rect1, rect2);
    }
}