package com.example.gema_king;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;

public class Game8Activity extends MenuActivity {

    private TextView endMessage;
    private Button endActionButton;
    private Button btnStartGame;
    private View startOverlay;
    private View endOverlay;
    private final int gameId = 80;
    private int recordId;
    private static final String TAG = "Game8Activity";

    private ImageView cryingGirl, happyGirl, scissor, television, tragic;
    RadioButton tvbtn;
    private View parentView;

    private float dX, dY;
    private int lastAction;
    private boolean shovelIsColliding;
    private boolean boneIsColliding;

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            Log.d(TAG, "setupToolbar: " +getSupportActionBar());
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game8);
        setupToolbar();

        UserSession.getUserId(this);
        StatusManager.init(this);
        recordId =  StatusManager.initGameStatus(UserSession.getUserId(this),gameId);
        StatusManager.updateGamePlayed(UserSession.getUserId(this));

        btnStartGame = findViewById(R.id.btn_start_game);
        startOverlay = findViewById(R.id.start_overlay);
        endOverlay = findViewById(R.id.end_overlay);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);

        btnStartGame.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
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
            onGameCompleted(100);
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

    public void onGameCompleted(int score) {
        StatusManager.updateGameStatusToFinish(recordId, 100,0);
        String scoreText = getString(R.string.game2_score);
        endMessage.setText(getString(R.string.end_success_g5) + "\n" + scoreText + score);
        endActionButton.setText(getString(R.string.next_stage));
        endActionButton.setOnClickListener(view -> {
            Intent intent = new Intent(Game8Activity.this, Game6Activity.class);
            startActivity(intent);
            finish();
        });
        endOverlay.setVisibility(View.VISIBLE);
    }
}