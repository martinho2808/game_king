package com.example.gema_king;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class Game3Activity extends MenuActivity {
    public boolean flagging = false;
    public boolean isGameOver = false;
    private GridLayout MineField;
    private TextView txtMineCount;
    private Button openbtn, flagbtn;
    private String time = "00:00:00";
    private TextView txtTimer;
    private String Level = "Easy";
    private Handler timer = new Handler();
    private int Seconds = 0;
    public boolean isTimerStarted = false;
    public int numberOfRowsInMineField = 6;
    public int numberOfColumnsInMineField = 6;
    public int totalNumberOfMines = 5;
    private int Mines = 5;//Initial Mines
    private int Opened = 0;//The number of opened
    private int Flagged = 0;//The current number of marked
    private View startOverlay;
    private View endOverlay;
    private TextView endMessage;
    private Button endActionButton;
    private Block[][] blocks;
    private Handler mines;
    private View minesView;
    private final int gameId = 30;
    private int recordId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game3);
        startOverlay = findViewById(R.id.start_overlay);
        MineField = (GridLayout) findViewById(R.id.mines);
        txtMineCount = (TextView) findViewById(R.id.minecount);
        txtTimer = (TextView) findViewById(R.id.time1);
        openbtn = (Button) findViewById(R.id.open);
        flagbtn = (Button) findViewById(R.id.flag);
        setupToolbar();
        init();

        //Handle status
        UserSession.getUserId(this);
        StatusManager.init(this);
        recordId =  StatusManager.initGameStatus(UserSession.getUserId(this),gameId);
        StatusManager.updateGamePlayed(UserSession.getUserId(this));



        Button btnStartGame = findViewById(R.id.btn_start_game);
        btnStartGame.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
            minesView = findViewById(R.id.mines);
            minesView.setVisibility(View.VISIBLE);
            init();
        });
        endOverlay = findViewById(R.id.end_overlay);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);
    }

    @Override
    //Receive the parameters returned by the menu interface for processing
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Click the button to initialize
        if (resultCode == 1) {
            if (data.getStringExtra("result").equals("newgame")) {
                StatusManager.updateGameStatusToProgress(recordId);
                init();
            }
            //恢复暂停的计时器
            else {
                if (!isGameOver && isTimerStarted) {
                    startTimer();
                }
            }
        }
    }

    //Game initialize
    public void init() {
        MineField.removeAllViews();
        Seconds = 0;
        Mines = totalNumberOfMines;
        isGameOver = false;
        Flagged = 0;
        isTimerStarted = false;
        flagging = false;
        time = "00:00:00";
        Opened = 0;
        stopTimer();
        txtMineCount.setText(Integer.toString(totalNumberOfMines));
        txtTimer.setText(time);
        openbtn.setEnabled(false);
        flagbtn.setEnabled(true);
        openbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flagging = false;
                flagbtn.setEnabled(true);
                openbtn.setEnabled(false);
            }
        });
        flagbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flagging = true;
                flagbtn.setEnabled(false);
                openbtn.setEnabled(true);
            }
        });
        createMineField();
        showMineField();
    }

    //Creating a minefield
    private void createMineField() {
        blocks = new Block[numberOfRowsInMineField][numberOfColumnsInMineField];
        for (int row = 0; row < numberOfRowsInMineField; row++) {
            for (int column = 0; column < numberOfColumnsInMineField; column++) {
                blocks[row][column] = new Block(this);
                blocks[row][column].setDefaults();
                final int currentRow = row;
                final int currentColumn = column;
                final Block temp = blocks[currentRow][currentColumn];
                blocks[row][column].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //If the timer is not started, start the timer
                        if (!isTimerStarted) {
                            startTimer();
                            isTimerStarted = true;
                        }
                        //Open
                        if (!flagging) {
                            if (!temp.isFlagged()) {
                                //Open the minefields around you and yourself
                                rippleUncover(currentRow, currentColumn);
                                //If you hit a mine, the game ends
                                if (temp.hasMine()) {
                                    finishGame();
                                }
                            }
                        }
                        //Tag
                        else {
                            if (temp.isCovered()) {
                                if (temp.isFlagged()) {
                                    blocks[currentRow][currentColumn].clearAllIcons();
                                    blocks[currentRow][currentColumn].setFlagged(false);
                                    UpdateMinecount(true);
                                } else {
                                    blocks[currentRow][currentColumn].setFlagIcon(false);
                                    blocks[currentRow][currentColumn].setFlagged(true);
                                    UpdateMinecount(false);
                                }
                            }
                        }
                        if (checkGameWin()) {
                            winGame();
                        }

                    }

                });
            }
        }
        setMines();
    }

    //Win the game
    @SuppressLint("SetTextI18n")
    private void winGame() {
        stopTimer();
        isTimerStarted = false;
        isGameOver = true;
        Mines = 0;
        for (int row = 0; row < numberOfRowsInMineField; row++) {
            for (int column = 0; column < numberOfColumnsInMineField; column++) {
                blocks[row][column].setEnabled(false);
                if (blocks[row][column].hasMine()) {
                    blocks[row][column].setFlagIcon(true);
                }
            }
        }
        int score = calculateScore();
        StatusManager.updateGameStatusToFinish(recordId, score, Seconds);
        runOnUiThread(() -> {
            String secondText = getString(R.string.game3_second);
            String timeTakeText = getString(R.string.game3_time_take);
            String scoreText = getString(R.string.game3_score);
            endMessage.setText(getString(R.string.end_success_g5) + "\n" + timeTakeText + Seconds + secondText + "\n" + scoreText + score);
            endActionButton.setText(getString(R.string.next_stage));
            endActionButton.setOnClickListener(view -> {
                Intent intent = new Intent(Game3Activity.this, Game4Activity.class);
                startActivity(intent);
                finish();
            });
            endOverlay.setVisibility(View.VISIBLE);
        });
    }

    private int calculateScore() {
        int baseScore = 200;
        int timePenalty = Seconds - 1; //1 points deducted per second
        int score = Math.max(0, baseScore - timePenalty);
        return score;
    }

    //Loss the game
    private void finishGame() {
        stopTimer();
        isGameOver = true;
        isTimerStarted = false;
        for (int row = 0; row < numberOfRowsInMineField; row++) {
            for (int column = 0; column < numberOfColumnsInMineField; column++) {
                blocks[row][column].setEnabled(false);
                if (blocks[row][column].hasMine() && blocks[row][column].isFlagged()) continue;
                else {
                    if (blocks[row][column].hasMine()) {
                        blocks[row][column].setMineIcon();
                    } else {
                        blocks[row][column].OpenBlock();
                    }
                }
            }
        }

        runOnUiThread(() -> {
            endMessage.setText(getString(R.string.game3_retry));
            endActionButton.setText(getString(R.string.retry));
            endActionButton.setOnClickListener(v -> {
                endOverlay.setVisibility(View.GONE);
                startOverlay.setVisibility(View.VISIBLE); // 顯示開始提示
            });
            endOverlay.setVisibility(View.VISIBLE);
        });
    }

    //Updated mine counter
    public void UpdateMinecount(boolean flag) {
        if (flag) {
            Flagged--;
            Mines++;
        } else {
            Flagged++;
            Mines--;
        }
        if (Mines < 0) Mines = 0;
        if (Mines < 10) {
            txtMineCount.setText("0" + Integer.toString(Mines));
        } else {
            txtMineCount.setText(Integer.toString(Mines));
        }
    }

    //Start Timer
    public void startTimer() {
        if (Seconds == 0) {
            timer.removeCallbacks(updateTimeElasped);
            timer.postDelayed(updateTimeElasped, 1000);
        } else {
            timer.postDelayed(updateTimeElasped, 1000);
        }
    }

    public void stopTimer() {
        timer.removeCallbacks(updateTimeElasped);
    }

    //Timer child thread
    private Runnable updateTimeElasped = new Runnable() {
        public void run() {
            ++Seconds;
            String hh = new DecimalFormat("00").format(Seconds / 3600);
            String mm = new DecimalFormat("00").format(Seconds % 3600 / 60);
            String ss = new DecimalFormat("00").format(Seconds % 60);
            time = hh + ":" + mm + ":" + ss;
            txtTimer.setText(time);
            timer.postDelayed(updateTimeElasped, 1000);
        }
    };

    //Show MineField
    private void showMineField() {
        for (int i = 0; i < numberOfRowsInMineField; i++) {
            for (int j = 0; j < numberOfColumnsInMineField; j++) {
                GridLayout.Spec rowSpec = GridLayout.spec(i); // Setup the row of btn
                GridLayout.Spec columnSpec = GridLayout.spec(j);// Setup the column of btn
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                Resources resources = this.getResources();
                DisplayMetrics dm = resources.getDisplayMetrics();
                int t = dm.widthPixels / (numberOfColumnsInMineField + 1);
                params.width = t;
                params.height = t;
                if (Level.equals("Easy")) {
                    params.setMargins(13, 13, 13, 13);
                }
                MineField.addView(blocks[i][j], params);
            }
        }
    }

    //Lay mines and count the number of mines around each square
    private void setMines() {
        Random random = new Random();
        for (int i = 0; i < totalNumberOfMines; i++) {
            while (true) {
                Block randomBlock = blocks[random.nextInt(numberOfRowsInMineField)][random.nextInt(numberOfColumnsInMineField)];
                if (!randomBlock.hasMine()) {
                    randomBlock.plantMine();
                    break;
                }
            }
        }
        //Calculate the number of mines around each mine-free block
        for (int i = 0; i < numberOfRowsInMineField; i++) {
            for (int j = 0; j < numberOfColumnsInMineField; j++) {
                if (blocks[i][j].hasMine())
                    continue;
                int mineCount = 0;
                mineCount = getMineCount(j, i);
                blocks[i][j].setNumberOfMinesInSurrounding(mineCount);
            }
        }
    }

    private int getMineCount(int x, int y) {
        int mineCount = 0;
        for (int i = (y - 1); i <= (y + 1); i++) {
            for (int j = (x - 1); j <= (x + 1); j++) {
                if (i == y && j == x)
                    continue;
                try {
                    if (blocks[i][j].hasMine())
                        mineCount++;
                } catch (IndexOutOfBoundsException ex) {
                    continue;
                }

            }
        }
        return mineCount;
    }

    //Turn over your own and surrounding blocks
    private void rippleUncover(int rowClicked, int columnClicked) {
        if (blocks[rowClicked][columnClicked].hasMine() || blocks[rowClicked][columnClicked].isFlagged())
            return;

        blocks[rowClicked][columnClicked].OpenBlock();
        Opened++;
        if (blocks[rowClicked][columnClicked].getNumberOfMinesInSorrounding() != 0) {
            return;
        }
        for (int i = (rowClicked - 1); i <= (rowClicked + 1); i++) {
            for (int j = (columnClicked - 1); j <= (columnClicked + 1); j++) {
                try {
                    if (i == rowClicked && j == columnClicked) continue;
                    if (!blocks[i][j].isCovered()) continue;
                    rippleUncover(i, j);
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }
            }
        }
        return;
    }

    //Check whether the game is won
    private boolean checkGameWin() {
        if (isGameOver) return false;
        if (Flagged + Opened == numberOfColumnsInMineField * numberOfRowsInMineField
                && totalNumberOfMines == Flagged) {
            return true;
        }
        return false;
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
}