package com.example.gema_king;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.example.gema_king.MenuActivity;

import java.util.ArrayList;
import java.util.Random;

public class Game4Activity extends MenuActivity {

    int score = 0;
    ArrayList<ImageView> imageArray;
    Handler handler;
    Runnable runnable;
    TextView timeText;
    TextView scoreText;
    ImageView imageView;
    ImageView imageView2;
    ImageView imageView3;
    ImageView imageView4;
    ImageView imageView5;
    ImageView imageView6;
    ImageView imageView7;
    ImageView imageView8;
    ImageView imageView9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game4);

        // Initialize views
        timeText = findViewById(R.id.timeText);
        scoreText = findViewById(R.id.scoreText);
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

        handler = new Handler();

        hideImages();

        // CountDown Timer
        new CountDownTimer(15500, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeText.setText("Time: " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                timeText.setText("Time: 0");

                handler.removeCallbacks(runnable);

                for (ImageView image : imageArray) {
                    image.setVisibility(View.INVISIBLE);
                }

                // Alert
                new AlertDialog.Builder(Game4Activity.this).setTitle("Game Over")
                        .setMessage("You scored: " + score + " points!")
                        .setNegativeButton("Menu", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Game4Activity.this, MainMenuActivity.class);
                                startActivity(intent);
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("Next game", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Game4Activity.this, Game1Activity.class);
                                startActivity(intent);
                                dialog.dismiss();
                            }
                        })
                        .create().show();
            }
        }.start();
    }

    public void hideImages() {
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
        score = score + 1;
        scoreText.setText("Score: " + score);
    }
}