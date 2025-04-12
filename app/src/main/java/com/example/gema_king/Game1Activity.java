package com.example.gema_king;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;

import java.util.ArrayList;
import java.util.Collections;

public class Game1Activity extends MenuActivity {
    private GridLayout gridLayout; // Grid layout, display color.
    private GridLayout gridLayout_answer; // Grid layout, displaying color buttons
    private TextView timerTextView; // Display a text view for the countdown timer.
    private View startOverlay;
    private Button startButton; // Start game button
    private View endOverlay;
    private TextView endMessage;
    private Button endActionButton;
    private ArrayList<Integer> colorPattern; // Store color mode.
    private ArrayList<Integer> randomColorPattern; // Store random color
    private ArrayList<Integer> buttonIds = new ArrayList<>(); // The ID for the save button
    private ArrayList<Integer> selectedColorPattern;
    private ArrayList<View> buttons; // Store color button.
    private final int totalColors = 6; // Total color count
    private static final String TAG = "Game1Activity";
    private final int gameId = 10;
    private int recordId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game1);
        setupToolbar();

        //Handle status
        UserSession.getUserId(this);
        StatusManager.init(this);
        recordId =  StatusManager.initGameStatus(UserSession.getUserId(this),gameId);

        // 初始化视图组件
        startOverlay = findViewById(R.id.start_overlay);
        gridLayout = findViewById(R.id.gridLayout);
        gridLayout_answer = findViewById(R.id.gridLayout_answer);
        timerTextView = findViewById(R.id.timerTextView);
        startButton = findViewById(R.id.btn_start_game);
        gridLayout.setVisibility(View.INVISIBLE);
        timerTextView.setVisibility(View.GONE);

        // Set the click event for the start game button.
        startButton.setOnClickListener(v -> {
            startOverlay.setVisibility(View.GONE);
            startGame();
        });
        endOverlay = findViewById(R.id.end_overlay);
        endMessage = findViewById(R.id.end_message);
        endActionButton = findViewById(R.id.end_action_button);
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            Log.d(TAG, "setupToolbar: " +getSupportActionBar());
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    private void startGame() {
        StatusManager.updateGameStatusToProgress(recordId);
        resetGame(); // reset game
        startCountdown(); // start countdown
    }

    private void resetGame() {
        buttonIds.clear();
        gridLayout.removeAllViews();
        gridLayout_answer.removeAllViews();
        selectedColorPattern = new ArrayList<>();
        buttons = new ArrayList<>();

        for (int i = 0; i < totalColors; i++) {
            View button = new View(this);
            button.setLayoutParams(new GridLayout.LayoutParams());
            button.setBackgroundColor(Color.WHITE); // init background color
            buttons.add(button); // Add the button to the list.
            gridLayout.addView(button); // Add the button to the layout
        }

    }

    private void startCountdown() {
        Handler handler = new Handler(Looper.getMainLooper());
        timerTextView.setVisibility(View.VISIBLE);
        for (int i = 5; i >= 0; i--) {
            Log.i(TAG, "Access Game Page" + i);
            final int count = i;
            // Set up the countdown display.
            handler.postDelayed(() -> timerTextView.setText(String.valueOf(count)), (3 - i) * 1000);
        }
        // To generate a color mode after 6 seconds
        handler.postDelayed(()-> {
            gridLayout.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.GONE);
            generateColorPattern();
        }, 3000);
    }

    private void generateColorPattern() {
        colorPattern = new ArrayList<>(); // To initialize a color mode list
        for (int i = 0; i < totalColors; i++) {
            Log.i(TAG, "Access Color"  );
            int color = getRandomColor(); // get random color
            Log.i(TAG, "Access Color: "+ color  );
            colorPattern.add(color); // add color to the list
            buttons.get(i).setTag(color); //
        }
        randomColorPattern = new ArrayList<>(colorPattern);;
        Collections.shuffle(randomColorPattern);
        displayPattern(); // display color
    }

    private int getRandomColor() {
        // 随机生成颜色
        return Color.rgb((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
    }

    private void displayPattern() {
        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < colorPattern.size(); i++) {
            final int index = i;
            // setup every color will display 1 second
            handler.postDelayed(() -> {
                Log.d("ColorChange", "Changing to color: " + colorPattern.get(index));
                buttons.get(0).setBackgroundColor(colorPattern.get(index)); // display color
                buttons.get(0).invalidate();
            }, i * 1000);
        }

        // To restore the original color and enable a button after displaying all colors
        handler.postDelayed(() -> {
            gridLayout.setVisibility(View.INVISIBLE);
            //startButton.setVisibility(View.VISIBLE);
            createColorButtons();
            for (View button : buttons) {
                button.setBackgroundColor(Color.WHITE);
            }

        }, colorPattern.size() * 1000); // after all color display
    }


    @SuppressLint("SetTextI18n")
    private void createColorButtons() {
        //startButton.setVisibility(View.INVISIBLE);
        // get GridLayout
        GridLayout gridLayout = findViewById(R.id.gridLayout);

        // init GridLayout
        gridLayout.removeAllViews();


        // To create a button dynamically and add it to GridLayout
        for (int i = 0; i < totalColors; i++) {
            Button newButton = new Button(this);
            newButton.setText(" ");

            // setup display color
            newButton.setBackgroundColor(randomColorPattern.get(i));

            // To set the layout parameters for buttons
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.setMargins(10, 10, 10, 10); // martin
            params.columnSpec = GridLayout.spec(i % 2); // col
            params.rowSpec = GridLayout.spec(i / 2);    // row
            newButton.setLayoutParams(params);

            // To generate unique IDs and save them to a list
            int buttonId = View.generateViewId();
            newButton.setId(buttonId);
            buttonIds.add(buttonId); // save ID

            // To set up a click event listener
            newButton.setOnClickListener(v -> {
                int index = buttonIds.indexOf(v.getId());
                v.setVisibility(View.INVISIBLE);
                if (index != -1) {
                    //Toast.makeText(this, "Button " + (index + 1) + " clicked!", Toast.LENGTH_SHORT).show();
                }
                int color = ((ColorDrawable) v.getBackground()).getColor();
                selectedColorPattern.add(color);
                Log.d("ColorChoose: ", String.valueOf(color));

                if(colorPattern.size() == selectedColorPattern.size()){
                    boolean result = validColor();
                    if(result) {
                        StatusManager.updateGameStatusToFinish(recordId, 100,0);
                        endMessage.setText(getString(R.string.end_success_g5));
                        endActionButton.setText(getString(R.string.next_stage));
                        endActionButton.setOnClickListener(view -> {
                            Intent intent = new Intent(Game1Activity.this, Game5Activity.class);
                            startActivity(intent);
                            finish();
                        });
                        Log.d("Result: ", "success");
                    } else {
                        endMessage.setText(getString(R.string.end_fail));
                        endActionButton.setText(getString(R.string.retry));
                        endActionButton.setOnClickListener(view -> {
                            endOverlay.setVisibility(View.GONE);
                            startOverlay.setVisibility(View.VISIBLE); // 顯示開始提示
                        });
                    }
                    endOverlay.setVisibility(View.VISIBLE);
                }
            });

            // To add buttons to a GridLayout
            gridLayout_answer.addView(newButton);
        }
    }

    private boolean validColor() {
        for (int i = 0; i < colorPattern.size(); i++) {
            if (!colorPattern.get(i).equals(selectedColorPattern.get(i))) {
                return false;
            }
        }
        return true;
    }

}
