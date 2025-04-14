package com.example.gema_king;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.example.gema_king.MenuActivity;
import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;

import java.util.ArrayList;
import java.util.Random;

public class Game4Activity extends MenuActivity {

    int score = 0;
    ArrayList<ImageView> imageArray;
    Handler handler;
    Runnable runnable;
    TextView timeText;
    TextView scoreText;
    TextView readyText; // New TextView for the ready countdown
    ImageView imageView;
    ImageView imageView2;
    ImageView imageView3;
    ImageView imageView4;
    ImageView imageView5;
    ImageView imageView6;
    ImageView imageView7;
    ImageView imageView8;
    ImageView imageView9;
    private boolean gameStarted = false;
    private View startOverlay;
    private View endOverlay;
    private TextView endMessage;
    private Button endActionButton;
    private View gridLayout;
    private Button btnStartGame;
    private final int gameId = 40;
    private int recordId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game4);
        setupToolbar();

        //Handle status
        UserSession.getUserId(this);
        StatusManager.init(this);
        recordId =  StatusManager.initGameStatus(UserSession.getUserId(this),gameId);
        StatusManager.updateGamePlayed(UserSession.getUserId(this));

        startOverlay = findViewById(R.id.start_overlay);
        btnStartGame = findViewById(R.id.btn_start_game);

        // Initialize views
        timeText = findViewById(R.id.timeText);
        scoreText = findViewById(R.id.scoreText);
        readyText = findViewById(R.id.readyText);
        imageView = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        imageView3 = findViewById(R.id.imageView3);
        imageView4 = findViewById(R.id.imageView4);
        imageView5 = findViewById(R.id.imageView5);
        imageView6 = findViewById(R.id.imageView6);
        imageView7 = findViewById(R.id.imageView7);
        imageView8 = findViewById(R.id.imageView8);
        imageView9 = findViewById(R.id.imageView9);

        // ImageArray
        imageArray = new ArrayList<>();
        imageArray.add(imageView);
        imageArray.add(imageView2);
        imageArray.add(imageView3);
        imageArray.add(imageView4);
        imageArray.add(imageView5);
        imageArray.add(imageView6);
        imageArray.add(imageView7);
        imageArray.add(imageView8);
        imageArray.add(imageView9);

        handler = new Handler(Looper.getMainLooper());

        hideImagesInitially(); // Hide images before the ready countdown

        Button btnStartGame = findViewById(R.id.btn_start_game);
        btnStartGame.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
            readyText.setVisibility(View.VISIBLE);
            gridLayout = findViewById(R.id.gridLayout3);
            gridLayout.setVisibility(View.VISIBLE);
            startReadyCountdown();
        });
        endOverlay = findViewById(R.id.end_overlay);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void startReadyCountdown() {
        readyText.setVisibility(View.VISIBLE);
        final int[] readyTime = {4}; // Countdown for 4 seconds

        new CountDownTimer(4000, 1000) {
            public void onTick(long millisUntilFinished) {
                readyText.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                readyText.setVisibility(View.GONE);
                startGame();
            }
        }.start();
    }

    @SuppressLint("SetTextI18n")
    private void startGame() {
        StatusManager.updateGameStatusToProgress(recordId);
        gameStarted = true;
        hideImages();

        // Main game CountDown Timer
        new CountDownTimer(15000, 1000) { // Adjusted to 15000 to account for the ready time
            @Override
            public void onTick(long millisUntilFinished) {
                timeText.setText("Time: " + millisUntilFinished / 1000);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                timeText.setText("Time: 0");
                gameStarted = false;
                handler.removeCallbacks(runnable);

                for (ImageView image : imageArray) {
                    image.setVisibility(View.INVISIBLE);
                }

                runOnUiThread(() -> {
                    StatusManager.updateGameStatusToFinish(recordId, score, 0);
                    String scoreText = getString(R.string.game4_score);
                    endMessage.setText(getString(R.string.end_success_g5) + "\n" + scoreText + score);
                    endActionButton.setText(getString(R.string.next_stage));
                    endActionButton.setOnClickListener(view -> {
                        Intent intent = new Intent(Game4Activity.this, Game5Activity.class);
                        startActivity(intent);
                        finish();
                    });
                    endOverlay.setVisibility(View.VISIBLE);
                });
            }
        }.start();
    }

    private void hideImagesInitially() {
        for (ImageView image : imageArray) {
            image.setVisibility(View.INVISIBLE);
        }
    }

    public void hideImages() {
        if (!gameStarted) {
            return; // Don't start hiding images until the game begins
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                for (ImageView image : imageArray) {
                    image.setVisibility(View.INVISIBLE);
                }
                Random random = new Random();
                int randomIndex = random.nextInt(9);
                imageArray.get(randomIndex).setVisibility(View.VISIBLE);

                handler.postDelayed(runnable, 400);
            }
        };

        handler.post(runnable);
    }

    public void increaseScore(View view) {
        if (gameStarted) {
            score = score + 10;
            scoreText.setText("Score: " + score);
        }
    }
}