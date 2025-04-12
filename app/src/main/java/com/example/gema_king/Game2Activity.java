package com.example.gema_king;

import static java.security.AccessController.getContext;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.gema_king.Game2View.PuzzleView;

public class Game2Activity extends MenuActivity implements PuzzleView.GameEventListener {
    private PuzzleView puzzleView;
    private View startOverlay;
    private View endOverlay;
    private TextView endMessage;
    private Button endActionButton;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game2); // Use the XML layout
        startOverlay = findViewById(R.id.start_overlay);
        setupToolbar();


        // Find the PuzzleView in the layout

        Button btnStartGame = findViewById(R.id.btn_start_game);
        btnStartGame.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
            // Initialize the puzzle after the layout is loaded
            puzzleView = findViewById(R.id.puzzle_view);
            puzzleView.post(() -> {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.puzzle);
                puzzleView.initializePuzzle(bitmap);
                puzzleView.setGameEventListener((PuzzleView.GameEventListener) this);
            });
        });

        endOverlay = findViewById(R.id.end_overlay);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);

    }


    @SuppressLint("SetTextI18n")
    public void onGameCompleted(int time, int score) {
        runOnUiThread(() -> {
            //startOverlay.setVisibility(View.VISIBLE);
            String secondText = getString(R.string.game2_second);
            String timeTakeText = getString(R.string.game2_time_take);
            String scoreText = getString(R.string.game2_score);
            endMessage.setText(getString(R.string.end_success_g5) + "\n" + timeTakeText + time + secondText + "\n" + scoreText + score);
            endActionButton.setText(getString(R.string.next_stage));
            endActionButton.setOnClickListener(view -> {
                Intent intent = new Intent(Game2Activity.this, Game5Activity.class);
                startActivity(intent);
                finish();
            });
            endOverlay.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onGameOver() {
        runOnUiThread(() -> {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.puzzle);
            puzzleView.initializePuzzle(bitmap);
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
}
