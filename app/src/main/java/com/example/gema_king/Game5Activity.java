package com.example.gema_king;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Game5Activity extends AppCompatActivity {

    private FrameLayout gameContainer;
    private View startOverlay;
    private TextView countdownText, targetColorText, scoreText, comboText, comboEffect;

    private static final int TARGET_SCORE = 300;
    private static final long GAME_DURATION = 30000;
    private static final int GAME_ID = 50;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private int score = 0;
    private int comboCount = 0;
    private boolean isGameRunning = false;
    private Runnable gameTimer;
    private int timeLeft;

    private View endOverlay;
    private TextView endMessage;
    private Button endActionButton;

    private final String[] colorNames = {"Red", "Green", "Blue", "Yellow", "Purple", "Black"};
    private final int[] colorValues = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.BLACK
    };
    private int currentTargetColorValue;
    private int recordId;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game5);

        StatusManager.init(this);

        startOverlay = findViewById(R.id.start_overlay);
        gameContainer = findViewById(R.id.game_container);
        countdownText = findViewById(R.id.countdown_text);
        targetColorText = findViewById(R.id.target_color_text);
        scoreText = findViewById(R.id.score_text);
        comboText = findViewById(R.id.combo_text);
        comboEffect = findViewById(R.id.combo_effect);

        Button btnStartGame = findViewById(R.id.btn_start_game);
        btnStartGame.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
            gameContainer.post(this::startGame);
        });

        endOverlay = findViewById(R.id.end_overlay);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);
    }

    private void startGame() {
        score = 0;
        comboCount = 0;
        timeLeft = (int) (GAME_DURATION / 1000);
        isGameRunning = true;
        gameContainer.removeAllViews();
        updateCountdown(timeLeft);
        updateScore();
        updateCombo(0);

        long userId = UserSession.getUserId(this);
        recordId = StatusManager.initGameStatus((int) userId, GAME_ID);
        StatusManager.updateGameStatusToProgress(recordId);

        startTime = System.currentTimeMillis();

        gameTimer = () -> endGame(score >= TARGET_SCORE);
        handler.postDelayed(gameTimer, GAME_DURATION);

        setNewTargetColor();
        generateShape();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGameRunning) return;
                timeLeft--;
                updateCountdown(timeLeft);
                if (timeLeft > 0) handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @SuppressLint("SetTextI18n")
    private void updateCountdown(int seconds) {
        countdownText.setText(getString(R.string.countdown_text, seconds));
    }

    @SuppressLint("SetTextI18n")
    private void updateScore() {
        scoreText.setText(getString(R.string.score_text, score));
    }

    @SuppressLint("SetTextI18n")
    private void updateCombo(int combo) {
        if (combo > 0) {
            comboText.setText(getString(R.string.combo_text, combo));
            comboText.setVisibility(View.VISIBLE);
            comboEffect.setText("+1");
            comboEffect.setVisibility(View.VISIBLE);

            Animation fade = new AlphaAnimation(1.0f, 0.0f);
            fade.setDuration(800);
            comboEffect.startAnimation(fade);
            handler.postDelayed(() -> comboEffect.setVisibility(View.INVISIBLE), 800);
        } else {
            comboText.setVisibility(View.INVISIBLE);
            comboEffect.setVisibility(View.INVISIBLE);
        }
    }

    private void setNewTargetColor() {
        int index = random.nextInt(colorNames.length);
        String currentTargetColorName = colorNames[index];
        currentTargetColorValue = colorValues[index];
        targetColorText.setText(getString(R.string.target_color_text, currentTargetColorName));
    }

    private void generateShape() {
        if (!isGameRunning) return;

        gameContainer.removeAllViews();
        int ballsToGenerate = 1 + (comboCount / 3);
        ballsToGenerate = Math.min(ballsToGenerate, colorValues.length);

        List<Integer> colors = new ArrayList<>();
        for (int color : colorValues) {
            if (color != currentTargetColorValue) colors.add(color);
        }
        Collections.shuffle(colors);
        int correctIndex = random.nextInt(ballsToGenerate);
        List<Rect> usedAreas = new ArrayList<>();

        for (int i = 0; i < ballsToGenerate; i++) {
            int shapeColor = (i == correctIndex) ? currentTargetColorValue :
                    (colors.isEmpty() ? currentTargetColorValue : colors.remove(0));

            int size = random.nextInt(120) + 80;
            int x, y = 0, attempt = 0;
            Rect newRect;
            do {
                x = random.nextInt(Math.max(1, gameContainer.getWidth() - size));
                y = random.nextInt(Math.max(1, gameContainer.getHeight() - size));
                newRect = new Rect(x, y, x + size, y + size);
                attempt++;
            } while (isOverlapping(newRect, usedAreas) && attempt < 30);
            usedAreas.add(newRect);

            View shape = new View(this);
            shape.setBackground(createColoredCircle(shapeColor));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.leftMargin = x;
            params.topMargin = y;
            shape.setLayoutParams(params);

            final int clickedColor = shapeColor;
            shape.setOnClickListener(v -> {
                if (!isGameRunning) return;
                gameContainer.removeAllViews();

                if (clickedColor == currentTargetColorValue) {
                    score += 10;
                    comboCount++;
                    if (comboCount >= 3) {
                        score += 5;
                        Toast.makeText(this, "Combo +5!", Toast.LENGTH_SHORT).show();
                    }
                    updateCombo(comboCount);
                } else {
                    score -= 10;
                    comboCount = 0;
                    updateCombo(0);
                    Toast.makeText(this, "Wrong Color!", Toast.LENGTH_SHORT).show();
                }
                score = Math.min(score, TARGET_SCORE);
                updateScore();

                if (score >= TARGET_SCORE) {
                    endGame(true);
                } else {
                    setNewTargetColor();
                    generateShape();
                }
            });

            gameContainer.addView(shape);
        }
    }

    private boolean isOverlapping(Rect r, @NonNull List<Rect> others) {
        for (Rect other : others) {
            if (Rect.intersects(r, other)) return true;
        }
        return false;
    }

    @NonNull
    private GradientDrawable createColoredCircle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private void endGame(boolean isPassed) {
        isGameRunning = false;
        handler.removeCallbacks(gameTimer);
        gameContainer.removeAllViews();

        int playTime = (int) ((System.currentTimeMillis() - startTime) / 1000);

        if (isPassed && recordId != -1) {
            StatusManager.updateGameStatusToFinish(recordId, score, playTime);
            long userId = UserSession.getUserId(this);
            Log.d("Game5Activity", "✅ 分數儲存成功 - userId: " + userId + ", score: " + score + ", playTime: " + playTime + ", gameId: " + GAME_ID);
        } else {
            Log.d("Game5Activity", "❌ 未通關，未儲存分數。score=" + score + ", playTime=" + playTime);
        }

        if (isPassed) {
            endMessage.setText(getString(R.string.end_success_g5));
            endActionButton.setText(getString(R.string.next_stage));
            endActionButton.setOnClickListener(v -> {
                Intent intent = new Intent(Game5Activity.this, Game6Activity.class);
                startActivity(intent);
                finish();
            });
        } else {
            endMessage.setText(getString(R.string.end_fail));
            endActionButton.setText(getString(R.string.retry));
            endActionButton.setOnClickListener(v -> {
                endOverlay.setVisibility(View.GONE);
                startOverlay.setVisibility(View.VISIBLE);
            });
        }

        endOverlay.setVisibility(View.VISIBLE);
    }
}
