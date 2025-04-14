package com.example.gema_king;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Game8Activity extends AppCompatActivity {


    private static final String TAG = "Game8Activity";

    private ImageView cryingGirl, happyGirl, scissor, television, tragic;

    RadioButton tvbtn;

    private View parentView;

    private float dX, dY;
    private int lastAction;

    private boolean shovelIsColliding;
    private boolean boneIsColliding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game8);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cryingGirl = findViewById(R.id.cryingGirl);
        happyGirl = findViewById(R.id.happyGirl);

        scissor = findViewById(R.id.scissor);
        television = findViewById(R.id.television);
        tragic = findViewById(R.id.tragic);

        tvbtn = findViewById(R.id.radioButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

//      State the view image
        television.setVisibility(View.INVISIBLE);

        tvbtn.setOnClickListener(v -> {
            boolean newState = !tvbtn.isChecked();
            tvbtn.setChecked(newState); // Manually toggle

            television.setVisibility(View.VISIBLE);
            tragic.setVisibility(View.INVISIBLE);
            happyGirl.setVisibility(View.VISIBLE);
            cryingGirl.setVisibility(View.INVISIBLE);
        });

        parentView = (View) scissor.getParent();
        parentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                parentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int parentWidth = parentView.getWidth();
                int parentHeight = parentView.getHeight();

                scissor.setOnTouchListener(createDragListener(parentWidth, parentHeight));
            }
        });
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
                        break;
                    default:
                        return false;
                }
                return true;
            }
        };
    }

}