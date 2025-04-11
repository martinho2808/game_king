package com.example.gema_king;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game6Activity extends AppCompatActivity {

    private FrameLayout gameLayer;
    private View player;
    private TextView healthText, scoreText, countdownText;
    private View startOverlay, endOverlay;
    private Button endActionButton;
    private TextView endMessage;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private int playerHealth = 5;
    private int score = 0;
    private long startTime;
    private boolean isRunning = false;
    private final int GAME_DURATION = 30000;
    private final List<View> obstacles = new ArrayList<>();

    private final int[] speeds = {5, 8, 10, 6, 12};
    private final int[] colors = {
            Color.RED, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GREEN
    };

    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game6);

        gameLayer = findViewById(R.id.game_layer);
        player = findViewById(R.id.player);
        healthText = findViewById(R.id.health_text);
        scoreText = findViewById(R.id.score_text);
        countdownText = findViewById(R.id.countdown_text);

        startOverlay = findViewById(R.id.start_overlay);
        endOverlay = findViewById(R.id.end_overlay);
        Button btnStartGame = findViewById(R.id.btn_start_game);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);

        btnStartGame.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
            gameLayer.post(this::startGame);
        });

        endActionButton.setOnClickListener(v -> {
            endOverlay.setVisibility(View.GONE);
            startOverlay.setVisibility(View.VISIBLE);
        });
        // 觸控拖曳備用方式
        //gameLayer.setOnTouchListener((v, event) -> {
        //    if (event.getAction() == MotionEvent.ACTION_MOVE) {
        //        float x = event.getX() - player.getWidth() / 2f;
        //        player.setX(Math.max(0, Math.min(x, gameLayer.getWidth() - player.getWidth())));
        //    }
        //    return true;
        //});
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isRunning) return;
            float x = event.values[0];
            float moveX = x * 10f;
            float newX = player.getX() + moveX;
            newX = Math.max(0, Math.min(newX, gameLayer.getWidth() - player.getWidth()));
            player.setX(newX);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void startGame() {
        isRunning = true;
        playerHealth = 5;
        score = 0;
        startTime = System.currentTimeMillis();
        obstacles.clear();
        if (player.getParent() instanceof ViewGroup) {
            ((ViewGroup) player.getParent()).removeView(player);
        }
        gameLayer.removeAllViews();
        gameLayer.addView(player);
        updateScore();
        updateHealth();
        runTimer();
        spawnObstacle();
    }

    private void runTimer() {
        handler.postDelayed(() -> {
            if (!isRunning) return;
            long elapsed = System.currentTimeMillis() - startTime;
            int timeLeft = (int) ((GAME_DURATION - elapsed) / 1000);
            updateCountdown(timeLeft);

            if (elapsed >= GAME_DURATION) {
                endGame(true);
            } else {
                score++;
                if (elapsed % 5000 < 100) {
                    score += 10;
                }
                updateScore();
                runTimer();
            }
        }, 100);
    }

    private void updateScore() {
        scoreText.setText(getString(R.string.score_text, score));
    }

    private void updateHealth() {
        healthText.setText(getString(R.string.health_text, playerHealth));
    }

    private void updateCountdown(int seconds) {
        countdownText.setText(getString(R.string.countdown_text, seconds));
    }

    private void spawnObstacle() {
        if (!isRunning) return;
        int OBSTACLE_LIMIT = 8;
        if (obstacles.size() < OBSTACLE_LIMIT) {
            View obstacle = new View(this);
            int colorIndex = random.nextInt(colors.length);
            obstacle.setBackgroundColor(colors[colorIndex]);

            int speed = speeds[colorIndex % speeds.length];
            int size = 60 + (12 - speed) * 5;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.leftMargin = random.nextInt(Math.max(1, gameLayer.getWidth() - size));
            params.topMargin = 0;
            obstacle.setLayoutParams(params);

            obstacle.setTag(speed);
            gameLayer.addView(obstacle);
            obstacles.add(obstacle);

            moveObstacle(obstacle, speed);
        }

        handler.postDelayed(this::spawnObstacle, 1000);
    }

    private void moveObstacle(View obstacle, int speed) {
        obstacle.setTag("active");

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                obstacle.setY(obstacle.getY() + speed);

                if (obstacle.getY() > gameLayer.getHeight()) {
                    gameLayer.removeView(obstacle);
                    obstacles.remove(obstacle);
                    return;
                }

                if (Rect.intersects(getRect(obstacle), getRect(player)) &&
                        "active".equals(obstacle.getTag())) {

                    obstacle.setTag("hit");
                    gameLayer.removeView(obstacle);
                    obstacles.remove(obstacle);

                    int damage = (speed == 5 || speed == 6) ? 2 : 1;
                    playerHealth -= damage;
                    score = Math.max(0, score - damage * 10);

                    updateHealth();
                    updateScore();

                    if (playerHealth <= 0) {
                        endGame(false);
                        return;
                    }
                }

                handler.postDelayed(this, 20);
            }
        });
    }

    private Rect getRect(View view) {
        return new Rect((int) view.getX(), (int) view.getY(),
                (int) (view.getX() + view.getWidth()), (int) (view.getY() + view.getHeight()));
    }

    @SuppressLint("SetTextI18n")
    private void endGame(boolean survivedTime) {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);

        if (survivedTime) {
            endMessage.setText(getString(R.string.end_success_g6) + "\n" +
                    getString(R.string.score_text, score));
            endActionButton.setText(getString(R.string.next_stage));
            endActionButton.setOnClickListener(v -> {
                //Intent intent = new Intent(Game6Activity.this, Game7Activity.class);
                //startActivity(intent);
                //finish();
            });
        } else {
            endMessage.setText(getString(R.string.end_fail) + "\n" +
                    getString(R.string.score_text, score));
            endActionButton.setText(getString(R.string.retry));
            endActionButton.setOnClickListener(v -> {
                endOverlay.setVisibility(View.GONE);
                startOverlay.setVisibility(View.VISIBLE);
            });
        }

        endOverlay.setVisibility(View.VISIBLE);
    }
}
