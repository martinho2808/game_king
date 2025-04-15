package com.example.gema_king;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.widget.Toast;
import android.os.CountDownTimer;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.SeekBar;
import android.content.SharedPreferences;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Random;
import java.util.Locale;

public class Game9Activity extends MenuActivity {
    private static final String TAG = "Game9Activity";
    private static final int GAME_ID = 90;
    private static final int QUESTIONS_PER_GAME = 15;
    private static final long QUESTION_TIME_LIMIT = 15000; // 15 seconds
    private static final int BASE_SCORE = 100;
    private static final String PREF_NAME = "GameKing";
    private static final String KEY_LANGUAGE = "isEnglish";
    
    private SoundManager soundManager;
    
    private View startOverlay;
    private View gameContainer;
    private View endOverlay;
    private TextView questionText;
    private TextView timerText;
    private TextView scoreText;
    private TextView endMessage;
    private TextView endStats;
    private Button endActionButton;
    private MaterialButton[] answerButtons;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Vibrator vibrator;
    
    private int currentQuestion = 0;
    private int score = 0;
    private int correctAnswers = 0;
    private int recordId;
    private long questionStartTime;
    private boolean isGameRunning = false;
    private ArrayList<MathRiddle> riddles;
    private long gameStartTime; // 添加遊戲開始時間變量
    private CountDownTimer timer;
    private Toast currentToast; // 添加成員變量來追蹤當前顯示的 Toast
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 讀取語言設定，預設為英文
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isEnglish = prefs.getBoolean(KEY_LANGUAGE, true); // 預設為英文
        
        // 初始化音效管理器
        soundManager = SoundManager.getInstance(this);
        
        // 設置語言
        Locale locale = isEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game9);
        
        // 設置背景顏色
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundColor(getResources().getColor(R.color.game_background));
        
        // 初始化振動器
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // 初始化狀態管理器
        StatusManager.init(this);
        recordId = StatusManager.initGameStatus(UserSession.getUserId(this), GAME_ID);
        StatusManager.updateGamePlayed(UserSession.getUserId(this));
        
        // 設置工具欄
        setupToolbar();
        
        // 初始化視圖
        initializeViews();

        // 檢查是否是從語言切換返回
        boolean returnToStart = getIntent().getBooleanExtra("returnToStart", false);
        if (returnToStart) {
            // 確保顯示開始遊戲畫面
            startOverlay.setVisibility(View.VISIBLE);
            gameContainer.setVisibility(View.GONE);
            endOverlay.setVisibility(View.GONE);
            
            // 設置開始遊戲按鈕
            Button btnStartGame = findViewById(R.id.btn_start_game);
            btnStartGame.setOnClickListener(v -> {
                soundManager.playButtonClick();
                startOverlay.setVisibility(View.GONE);
                gameContainer.setVisibility(View.VISIBLE);
                startGame();
            });
        } else {
            // 檢查是否需要恢復遊戲狀態
            boolean isGameInProgress = prefs.getBoolean("isGameInProgress", false);
            if (isGameInProgress) {
                // 恢復遊戲狀態
                currentQuestion = prefs.getInt("currentQuestion", 0);
                score = prefs.getInt("currentScore", 0);
                correctAnswers = prefs.getInt("correctAnswers", 0);
                gameStartTime = prefs.getLong("gameStartTime", System.currentTimeMillis());
                
                // 清除保存的狀態
                prefs.edit()
                    .remove("isGameInProgress")
                    .remove("currentQuestion")
                    .remove("currentScore")
                    .remove("correctAnswers")
                    .remove("gameStartTime")
                    .apply();
                
                // 恢復遊戲
                startOverlay.setVisibility(View.GONE);
                gameContainer.setVisibility(View.VISIBLE);
                startGame();
            } else {
                // 設置開始遊戲按鈕
                Button btnStartGame = findViewById(R.id.btn_start_game);
                btnStartGame.setOnClickListener(v -> {
                    soundManager.playButtonClick();
                    startOverlay.setVisibility(View.GONE);
                    gameContainer.setVisibility(View.VISIBLE);
                    startGame();
                });
            }
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // 初始時不顯示標題
            TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
            if (toolbarTitle != null) {
                toolbarTitle.setVisibility(View.GONE);
            }
        }
    }
    
    private void initializeViews() {
        startOverlay = findViewById(R.id.start_overlay);
        gameContainer = findViewById(R.id.game_container);
        endOverlay = findViewById(R.id.end_overlay);
        questionText = findViewById(R.id.question_text);
        timerText = findViewById(R.id.timer_text);
        scoreText = findViewById(R.id.score_text);
        endMessage = findViewById(R.id.end_message);
        endStats = findViewById(R.id.end_stats);
        endActionButton = findViewById(R.id.end_action_button);
        
        // 初始化答案按鈕
        answerButtons = new MaterialButton[4];
        answerButtons[0] = findViewById(R.id.answer_button_1);
        answerButtons[1] = findViewById(R.id.answer_button_2);
        answerButtons[2] = findViewById(R.id.answer_button_3);
        answerButtons[3] = findViewById(R.id.answer_button_4);
        
        for (int i = 0; i < answerButtons.length; i++) {
            final int index = i;
            answerButtons[i].setOnClickListener(v -> {
                soundManager.playButtonClick();
                checkAnswer(index);
            });
        }
    }
    
    private void startGame() {
        isGameRunning = true;
        currentQuestion = 0;
        score = 0;
        correctAnswers = 0;
        gameStartTime = System.currentTimeMillis(); // 記錄遊戲開始時間
        riddles = generateRiddles();
        StatusManager.updateGameStatusToProgress(recordId);

        // 遊戲開始時顯示標題
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setText(R.string.game9_title);
        }

        // 初始化分數顯示
        scoreText.setText(String.format(getString(R.string.game9_score_format), score));
        scoreText.setTextSize(22);

        showNextQuestion();
    }
    
    private void showNextQuestion() {
        if (currentQuestion >= QUESTIONS_PER_GAME) {
            endGame();
            return;
        }
        
        MathRiddle riddle = riddles.get(currentQuestion);
        String questionText = String.format("%s", riddle.question);
        this.questionText.setText(questionText);
        
        // 設置題號
        TextView questionNumber = findViewById(R.id.question_number);
        questionNumber.setText(String.format(getString(R.string.game9_question_number), currentQuestion + 1, QUESTIONS_PER_GAME));
        
        // 重置並啟用所有答案按鈕
        for (int i = 0; i < answerButtons.length; i++) {
            MaterialButton button = answerButtons[i];
            button.setEnabled(true);
            button.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.purple_500)));
            button.setStrokeColor(ColorStateList.valueOf(getResources().getColor(android.R.color.black)));
            button.setStrokeWidth(2);
            button.setText(String.valueOf(riddle.options[i]));
        }
        
        questionStartTime = System.currentTimeMillis();
        startTimer();
    }
    
    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        
        final long endTime = questionStartTime + QUESTION_TIME_LIMIT;
        
        timer = new CountDownTimer(QUESTION_TIME_LIMIT, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isGameRunning) return;
                
                long currentTime = System.currentTimeMillis();
                long remainingTime = endTime - currentTime;
                
                if (remainingTime <= 0) {
                    timerText.setText(getString(R.string.game9_time_up));
                    timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    handleTimeout();
                } else {
                    // 美化計時器顯示
                    float seconds = remainingTime / 1000f;
                    String timeText;
                    if (seconds <= 5) {
                        // 最後5秒顯示紅色
                        timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                        timeText = String.format(getString(R.string.game9_time_warning), seconds);
                    } else if (seconds <= 10) {
                        // 最後10秒顯示黃色
                        timerText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                        timeText = String.format(getString(R.string.game9_time_format), seconds);
                    } else {
                        // 正常時間顯示白色
                        timerText.setTextColor(getResources().getColor(android.R.color.white));
                        timeText = String.format(getString(R.string.game9_time_format), seconds);
                    }
                    timerText.setText(timeText);
                    timerText.setTextSize(24); // 調整計時器文字大小
                }
            }

            @Override
            public void onFinish() {
                handleTimeout();
            }
        }.start();
    }
    
    private void checkAnswer(int selectedIndex) {
        if (!isGameRunning) return;
        
        // 禁用所有答案按鈕
        for (MaterialButton button : answerButtons) {
            button.setEnabled(false);
        }
        
        MathRiddle currentRiddle = riddles.get(currentQuestion);
        boolean isCorrect = currentRiddle.options[selectedIndex] == currentRiddle.correctAnswer;
        
        // 設置按鈕顏色以顯示正確/錯誤
        for (int i = 0; i < answerButtons.length; i++) {
            if (currentRiddle.options[i] == currentRiddle.correctAnswer) {
                answerButtons[i].setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_light)));
            } else if (i == selectedIndex && !isCorrect) {
                answerButtons[i].setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_light)));
            }
        }
        
        if (isCorrect) {
            correctAnswers++;
            // 計算得分（基於回答速度）
            long timeTaken = System.currentTimeMillis() - questionStartTime;
            int timeBonus = (int) (BASE_SCORE * (1 - timeTaken / (double) QUESTION_TIME_LIMIT));
            int scoreGained = BASE_SCORE + Math.max(0, timeBonus);
            score += scoreGained;
            
            // 播放正確音效
            soundManager.playGameSound(R.raw.correct);
            
            // 正確答案的振動反饋
            try {
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } catch (SecurityException e) {
                // 忽略振動權限錯誤
            }

            // 如果上一個 Toast 還在顯示，取消它
            if (currentToast != null) {
                currentToast.cancel();
            }
            // 顯示得分 Toast
            currentToast = Toast.makeText(this, getString(R.string.game9_correct, scoreGained), Toast.LENGTH_SHORT);
            currentToast.show();
        } else {
            // 播放錯誤音效
            soundManager.playGameSound(R.raw.wrong);
            
            // 錯誤答案的振動反饋
            try {
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 100, 100, 100}, -1));
                }
            } catch (SecurityException e) {
                // 忽略振動權限錯誤
            }

            // 如果上一個 Toast 還在顯示，取消它
            if (currentToast != null) {
                currentToast.cancel();
            }
            // 顯示錯誤 Toast
            currentToast = Toast.makeText(this, getString(R.string.game9_wrong), Toast.LENGTH_SHORT);
            currentToast.show();
        }
        
        // 美化分數顯示
        scoreText.setText(String.format(getString(R.string.game9_score_format), score));
        scoreText.setTextSize(22); // 調整分數文字大小

        // 根據答題速度調整下一題的延遲時間
        long timeTaken = System.currentTimeMillis() - questionStartTime;
        long delayTime = timeTaken < 1000 ? 500 : 1000; // 如果回答很快，縮短延遲

        currentQuestion++;
        handler.postDelayed(this::showNextQuestion, delayTime);
    }
    
    private void handleTimeout() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 100, 100, 100}, -1));
            }
        } catch (SecurityException e) {
            // 忽略振動權限錯誤
        }
        currentQuestion++;
        handler.postDelayed(this::showNextQuestion, 500);
    }

    @SuppressLint("SetTextI18n")
    private void endGame() {
        isGameRunning = false;
        handler.removeCallbacksAndMessages(null);

        gameContainer.setVisibility(View.GONE);
        endOverlay.setVisibility(View.VISIBLE);

        // 計算總遊戲時間（秒）
        int totalPlayTime = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);

        // 更新遊戲狀態和記錄
        StatusManager.updateGameStatusToFinish(recordId, score, totalPlayTime);

        endMessage.setText(getString(R.string.game9_end_message, score, (correctAnswers * 100 / QUESTIONS_PER_GAME)));
        endStats.setText(getString(R.string.game9_stats, QUESTIONS_PER_GAME, correctAnswers));
        endActionButton.setText(R.string.game9_next_game);
        endActionButton.setOnClickListener(v -> {
            soundManager.playButtonClick();
            
            // 停止當前遊戲的音效和計時器
            if (timer != null) {
                timer.cancel();
            }
            handler.removeCallbacksAndMessages(null);
            
            // 重置遊戲狀態
            isGameRunning = false;
            
            // 創建新的 Intent 並設置標誌
            Intent intent = new Intent(Game9Activity.this, Game10Activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("returnToStart", true);
            
            // 啟動 Game10Activity
            startActivity(intent);
        });
    }
    
    private ArrayList<MathRiddle> generateRiddles() {
        ArrayList<MathRiddle> riddles = new ArrayList<>();
        for (int i = 0; i < QUESTIONS_PER_GAME; i++) {
            riddles.add(generateRiddle());
        }
        return riddles;
    }
    
    private MathRiddle generateRiddle() {
        // 隨機選擇運算類型（加、減、乘）
        int operationType = random.nextInt(3);
        int num1, num2, correctAnswer;
        String question;
        
        switch (operationType) {
            case 0: // 加法
                num1 = random.nextInt(50) + 1;
                num2 = random.nextInt(50) + 1;
                question = String.format("%d + %d = ?", num1, num2);
                correctAnswer = num1 + num2;
                break;
            case 1: // 減法
                num1 = random.nextInt(50) + 51; // 確保結果為正數
                num2 = random.nextInt(50) + 1;
                question = String.format("%d - %d = ?", num1, num2);
                correctAnswer = num1 - num2;
                break;
            default: // 乘法
                num1 = random.nextInt(12) + 1;
                num2 = random.nextInt(12) + 1;
                question = String.format("%d × %d = ?", num1, num2);
                correctAnswer = num1 * num2;
                break;
        }
        
        // 生成錯誤選項
        int[] options = new int[4];
        int correctPosition = random.nextInt(4);
        options[correctPosition] = correctAnswer;
        
        for (int i = 0; i < 4; i++) {
            if (i != correctPosition) {
                int wrongAnswer;
                do {
                    // 生成接近正確答案的錯誤選項
                    wrongAnswer = correctAnswer + (random.nextInt(11) - 5);
                } while (wrongAnswer == correctAnswer || containsAnswer(options, wrongAnswer));
                options[i] = wrongAnswer;
            }
        }
        
        return new MathRiddle(question, options, correctAnswer);
    }
    
    private boolean containsAnswer(int[] options, int answer) {
        for (int option : options) {
            if (option == answer) return true;
        }
        return false;
    }
    
    private static class MathRiddle {
        final String question;
        final int[] options;
        final int correctAnswer;
        
        MathRiddle(String question, int[] options, int correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (timer != null) {
            timer.cancel();
        }
        // 釋放音效資源
        if (soundManager != null) {
            soundManager.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            soundManager.playButtonClick();
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_language) {
            soundManager.playButtonClick();
            showLanguageDialog();
            return true;
        } else if (id == R.id.action_leaderboard) {
            soundManager.playButtonClick();
            showLeaderboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUILanguage() {
        // 更新開始遊戲界面的所有文字
        View startOverlay = findViewById(R.id.start_overlay);
        if (startOverlay != null) {
            // 查找 LinearLayout 中的所有 TextView
            LinearLayout container = (LinearLayout) ((ConstraintLayout) startOverlay).getChildAt(0);
            if (container != null) {
                // 更新遊戲圖標
                ImageView gameIcon = (ImageView) container.getChildAt(0);
                if (gameIcon != null) {
                    gameIcon.setContentDescription(getString(R.string.game9_title));
                }

                // 更新標題
                TextView titleText = (TextView) container.getChildAt(1);
                if (titleText != null) {
                    titleText.setText(R.string.game9_title);
                }

                // 更新開始遊戲訊息
                TextView messageText = (TextView) container.getChildAt(2);
                if (messageText != null) {
                    messageText.setText(R.string.game9_start_message);
                }

                // 更新遊戲規則（在 CardView 中）
                CardView cardView = (CardView) container.getChildAt(3);
                if (cardView != null) {
                    TextView rulesText = (TextView) cardView.getChildAt(0);
                    if (rulesText != null) {
                        rulesText.setText(R.string.game9_rules);
                    }
                }

                // 更新開始遊戲按鈕
                MaterialButton startButton = (MaterialButton) container.getChildAt(4);
                if (startButton != null) {
                    startButton.setText(R.string.game9_start);
                }
            }
        }

        // 如果遊戲正在進行，更新遊戲相關文字
        if (isGameRunning) {
            TextView questionNumber = findViewById(R.id.question_number);
            TextView scoreText = findViewById(R.id.score_text);
            TextView timerText = findViewById(R.id.timer_text);
            
            if (questionNumber != null) {
                questionNumber.setText(String.format(getString(R.string.game9_question_number), currentQuestion + 1, QUESTIONS_PER_GAME));
            }
            if (scoreText != null) {
                scoreText.setText(String.format(getString(R.string.game9_score_format), score));
            }
            if (timerText != null && timer != null) {
                // 更新計時器文字格式
                float remainingTime = Float.parseFloat(timerText.getText().toString().replaceAll("[^0-9.]", ""));
                if (remainingTime <= 5) {
                    timerText.setText(String.format(getString(R.string.game9_time_warning), remainingTime));
                } else {
                    timerText.setText(String.format(getString(R.string.game9_time_format), remainingTime));
                }
            }

            // 更新問題文字
            if (questionText != null && currentQuestion < riddles.size()) {
                questionText.setText(riddles.get(currentQuestion).question);
            }
        }

        // 更新結束遊戲界面的文字
        View endOverlay = findViewById(R.id.end_overlay);
        if (endOverlay != null && endOverlay.getVisibility() == View.VISIBLE) {
            TextView endMessage = findViewById(R.id.end_message);
            TextView endStats = findViewById(R.id.end_stats);
            Button endActionButton = findViewById(R.id.end_action_button);

            if (endMessage != null) {
                endMessage.setText(getString(R.string.game9_end_message, score, (correctAnswers * 100 / QUESTIONS_PER_GAME)));
            }
            if (endStats != null) {
                endStats.setText(getString(R.string.game9_stats, QUESTIONS_PER_GAME, correctAnswers));
            }
            if (endActionButton != null) {
                endActionButton.setText(R.string.game9_next_game);
            }
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_settings);
        
        // 設置對話框背景為透明
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // 設置對話框寬度為螢幕寬度的 90%
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = android.view.Gravity.CENTER;
        
        // 設置背景變暗
        layoutParams.dimAmount = 0.6f;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        
        dialog.getWindow().setAttributes(layoutParams);

        Switch bgmSwitch = dialog.findViewById(R.id.switch_background_music);
        Switch soundSwitch = dialog.findViewById(R.id.switch_game_sound);
        SeekBar bgmSeekBar = dialog.findViewById(R.id.seekbar_background_music);
        SeekBar soundSeekBar = dialog.findViewById(R.id.seekbar_game_sound);
        Button okButton = dialog.findViewById(R.id.btn_ok);

        // 設置開關的初始狀態
        bgmSwitch.setChecked(soundManager.isBGMEnabled());
        soundSwitch.setChecked(soundManager.isSoundEnabled());

        // 設置音量條的初始值和狀態
        bgmSeekBar.setProgress((int)(soundManager.getBGMVolume() * 100));
        soundSeekBar.setProgress((int)(soundManager.getSFXVolume() * 100));
        bgmSeekBar.setEnabled(soundManager.isBGMEnabled());
        soundSeekBar.setEnabled(soundManager.isSoundEnabled());

        // 背景音樂開關監聽器
        bgmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.setBGMEnabled(isChecked);
            bgmSeekBar.setEnabled(isChecked);
        });

        // 音效開關監聽器
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.setSoundEnabled(isChecked);
            soundSeekBar.setEnabled(isChecked);
        });

        // 背景音樂音量監聽器
        bgmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    soundManager.setBGMVolume(progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 音效音量監聽器
        soundSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    soundManager.setSFXVolume(progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_language);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 設置對話框寬度為螢幕寬度的 90%
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(layoutParams);

        Button btnEnglish = dialog.findViewById(R.id.btn_english);
        Button btnChinese = dialog.findViewById(R.id.btn_chinese);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        // 讀取當前語言設置
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isEnglish = prefs.getBoolean(KEY_LANGUAGE, true);

        // 設置按鈕的初始狀態
        btnEnglish.setSelected(isEnglish);
        btnChinese.setSelected(!isEnglish);

        // 設置按鈕文字顏色
        updateButtonTextColor(btnEnglish, isEnglish);
        updateButtonTextColor(btnChinese, !isEnglish);

        // 英文按鈕點擊事件
        btnEnglish.setOnClickListener(v -> {
            soundManager.playButtonClick();
            btnEnglish.setSelected(true);
            btnChinese.setSelected(false);
            updateButtonTextColor(btnEnglish, true);
            updateButtonTextColor(btnChinese, false);
        });

        // 中文按鈕點擊事件
        btnChinese.setOnClickListener(v -> {
            soundManager.playButtonClick();
            btnEnglish.setSelected(false);
            btnChinese.setSelected(true);
            updateButtonTextColor(btnEnglish, false);
            updateButtonTextColor(btnChinese, true);
        });

        // 取消按鈕點擊事件
        btnCancel.setOnClickListener(v -> {
            soundManager.playButtonClick();
            dialog.dismiss();
        });

        // 確定按鈕點擊事件
        dialog.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            soundManager.playButtonClick();
            boolean newIsEnglish = btnEnglish.isSelected();

            // 如果語言設置有變化，則更新
            if (newIsEnglish != isEnglish) {
                // 保存新的語言設置
                prefs.edit().putBoolean(KEY_LANGUAGE, newIsEnglish).apply();

                // 設置新的語言
                Locale locale = newIsEnglish ? Locale.ENGLISH : Locale.TRADITIONAL_CHINESE;
                Locale.setDefault(locale);
                Resources res = getResources();
                Configuration config = res.getConfiguration();
                config.setLocale(locale);
                res.updateConfiguration(config, res.getDisplayMetrics());

                // 如果遊戲正在進行，停止遊戲
                if (isGameRunning) {
                    handler.removeCallbacksAndMessages(null);
                }

                // 重置遊戲狀態
                isGameRunning = false;

                // 更新UI
                startOverlay.setVisibility(View.VISIBLE);
                gameContainer.setVisibility(View.GONE);
                endOverlay.setVisibility(View.GONE);

                // 隱藏工具列標題
                Toolbar toolbar = findViewById(R.id.toolbar);
                TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
                if (toolbarTitle != null) {
                    toolbarTitle.setVisibility(View.GONE);
                }

                // 更新所有文字
                updateUILanguage();

                // 顯示切換提示
                Toast.makeText(this,
                        newIsEnglish ? R.string.switch_to_english : R.string.switch_to_chinese,
                        Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateButtonTextColor(Button button, boolean isSelected) {
        int textColor = isSelected ?
                getResources().getColor(R.color.purple_500) :
                getResources().getColor(R.color.text_primary);
        button.setTextColor(textColor);
    }

    private void showLeaderboard() {
        Intent intent = new Intent(this, LeaderboardActivity.class);
        intent.putExtra("gameId", GAME_ID);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        // 創建對話框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 創建自定義標題視圖
        TextView titleView = new TextView(this);
        titleView.setText(R.string.exit_title);
        titleView.setGravity(Gravity.START);  // 左對齊
        titleView.setPadding(40, 30, 40, 10); // 減少標題和內容之間的間距
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTextColor(Color.WHITE); // 白色標題
        titleView.setTypeface(null, Typeface.BOLD);

        // 創建自定義消息視圖
        TextView messageView = new TextView(this);
        messageView.setText(R.string.exit_message);
        messageView.setGravity(Gravity.START);  // 左對齊
        messageView.setPadding(40, 10, 40, 30); // 減少與標題的間距
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        messageView.setTextColor(Color.WHITE); // 白色內容

        builder.setCustomTitle(titleView)
                .setView(messageView)
                .setPositiveButton(R.string.exit_confirm, (dialog, which) -> {
                    // 如果用戶確認要退出
                    if (soundManager != null) {
                        soundManager.stopBGM();
                        soundManager.release();
                    }
                    // 完全退出應用程式
                    finishAffinity();  // 結束所有活動
                    System.exit(0);    // 確保完全退出
                })
                .setNegativeButton(R.string.exit_cancel, (dialog, which) -> {
                    // 如果用戶取消，什麼都不做
                    dialog.dismiss();
                })
                .setCancelable(false);  // 防止點擊對話框外部關閉

        // 創建並顯示對話框
        AlertDialog dialog = builder.create();

        // 設置對話框背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        // 設置按鈕文字顏色
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null && negativeButton != null) {
                // 確定按鈕設為綠色
                positiveButton.setTextColor(getResources().getColor(R.color.dialog_confirm)); // 綠色
                // 取消按鈕設為紅色
                negativeButton.setTextColor(getResources().getColor(R.color.dialog_cancel)); // 紅色
            }
        });

        dialog.show();
    }
} 