package com.example.gema_king.Game2View;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.gema_king.R;
import com.example.gema_king.model.StatusManager;
import com.example.gema_king.model.UserSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class PuzzleView extends View {
    private List<PuzzlePiece> pieces; // List to store puzzle pieces
    private int numColumns = 3; // Number of columns in the puzzle (3x3)
    private int pieceSize; // Original size of a puzzle piece
    private int scaledPieceSize; // Scaled size of a puzzle piece
    private Paint paint; // Paint object for drawing puzzle pieces
    private Paint textPaint; // Paint object for drawing text
    private Integer firstSelectedIndex = null; // Index of the first selected puzzle piece
    private float scaleFactor = 1.5f; // Scale factor for puzzle pieces
    private int swapCount = 0; // Counter for the number of swaps
    private long startTime; // Start time for the timer
    private int timeLimit = 30;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false; // Indicates if the timer is running
    private final int gameId = 20;
    private int recordId;


    public interface GameEventListener {
        void onGameCompleted(int time, int score);
        void onGameOver();
    }

    private GameEventListener gameEventListener;

    // setup listener
    public void setGameEventListener(GameEventListener listener) {
        this.gameEventListener = listener;
    }

    // Runnable to periodically update the timer
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000; // Calculate elapsed time in seconds
                if (elapsedTime >= timeLimit) {
                    String retryText = getContext().getString(R.string.game2_retry);
                    String secondText = getContext().getString(R.string.game2_second);
                    String timeTakeText = getContext().getString(R.string.game2_time_take);
                    swapCount = 0;
                    long totalTime = (isRunning ? (System.currentTimeMillis() - startTime) / 1000 : 0);
                    Toast.makeText(getContext(), retryText + "\n" + timeTakeText + totalTime + secondText, Toast.LENGTH_SHORT).show(); // Display the end message
                    endGame(); // End the game if time exceeds 120 seconds
                    if (gameEventListener != null) {
                        gameEventListener.onGameOver();
                    }
                    return;
                }
                invalidate(); // Refresh the view to update the timer
                handler.postDelayed(this, 1000); // Update every second
            }
        }
    };

    public PuzzleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(); // Initialize the paint object for drawing
        textPaint = new Paint(); // Initialize the paint object for text
        textPaint.setColor(0xFFFFFFFF); // Set text color to black
        textPaint.setTextSize(50); // Set text size
        pieces = new ArrayList<>(); // Initialize the list of puzzle pieces

        UserSession.getUserId(context);
        StatusManager.init(context);
        recordId = StatusManager.initGameStatus(UserSession.getUserId(context), gameId);
    }

    // Initialize the puzzle by dividing the image into pieces
    public void initializePuzzle(Bitmap bitmap) {
        // Crop the image to a square (to avoid boundary errors)
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size);

        pieceSize = bitmap.getWidth() / numColumns; // Calculate the original size of a puzzle piece
        scaledPieceSize = (int) (pieceSize * scaleFactor); // Calculate the scaled size of a puzzle piece

        pieces.clear(); // Clear previous puzzle pieces (recommended)

        // Divide the image into puzzle pieces
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numColumns; j++) {
                Bitmap pieceBitmap = Bitmap.createBitmap(bitmap, j * pieceSize, i * pieceSize, pieceSize, pieceSize);
                pieceBitmap = Bitmap.createScaledBitmap(pieceBitmap, scaledPieceSize, scaledPieceSize, true);
                pieces.add(new PuzzlePiece(pieceBitmap, i * numColumns + j));
            }
        }

        StatusManager.updateGameStatusToProgress(recordId);
        shufflePuzzle(); // Shuffle the puzzle pieces
        invalidate(); // Refresh the view
    }

    // Shuffle the order of puzzle pieces
    private void shufflePuzzle() {
        Collections.shuffle(pieces); // Randomly shuffle the puzzle pieces
    }

    // Start the timer
    private void startTimer() {
        startTime = System.currentTimeMillis(); // Record the start time
        isRunning = true; // Set the timer to running state
        handler.postDelayed(timerRunnable, 1000); // Start the timer to update every second
    }

    // End the game and display a message
    private void endGame() {
        isRunning = false; // Stop the timer
        handler.removeCallbacks(timerRunnable); // Remove the timer callback


    }
    @SuppressLint("DefaultLocale")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Calculate the starting position for drawing the puzzle to center it
        int totalWidth = scaledPieceSize * numColumns;
        int totalHeight = scaledPieceSize * numColumns;
        int startX = (getWidth() - totalWidth) / 2; // Center horizontally
        int startY = (getHeight() - totalHeight) / 2; // Center vertically

        // Draw the puzzle pieces
        for (int i = 0; i < pieces.size(); i++) {
            PuzzlePiece piece = pieces.get(i);
            int x = startX + (i % numColumns) * scaledPieceSize; // Calculate horizontal position
            int y = startY + (i / numColumns) * scaledPieceSize; // Calculate vertical position
            canvas.drawBitmap(piece.getBitmap(), x, y, paint); // Draw the puzzle piece
        }

        String timeText = getContext().getString(R.string.game2_time);
        String swapText = getContext().getString(R.string.game2_swap);

        // Draw the swap count text
        canvas.drawText(swapText + swapCount, startX, startY - 20, textPaint);

        // Calculate elapsed time (in seconds)
        long elapsedTime = (isRunning ? (System.currentTimeMillis() - startTime) / 1000 : 0);

        // Format the time as "mm:ss"
        String timeFormatted = String.format("%02d:%02d", elapsedTime / 60, elapsedTime % 60);
        canvas.drawText(timeText + timeFormatted, startX, startY - 70, textPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Calculate the column and row of the touch
            int startX = (getWidth() - (scaledPieceSize * numColumns)) / 2; // Calculate the starting X coordinate of the puzzle
            int startY = (getHeight() - (scaledPieceSize * numColumns)) / 2; // Calculate the starting Y coordinate of the puzzle
            int x = (int) (event.getX() - startX) / scaledPieceSize; // Calculate the column of the touch
            int y = (int) (event.getY() - startY) / scaledPieceSize; // Calculate the row of the touch
            int clickedIndex = y * numColumns + x; // Calculate the index of the touched puzzle piece

            // Handle selection logic
            if (clickedIndex >= 0 && clickedIndex < pieces.size()) {
                if (!isRunning) {
                    startTimer(); // Start the timer if it's not running
                }
                handleSelection(clickedIndex); // Handle the selected puzzle piece
            }
            invalidate(); // Refresh the view
        }
        return true; // Event handled
    }

    private void handleSelection(int clickedIndex) {
        if (firstSelectedIndex == null) {
            // First click, record the index
            firstSelectedIndex = clickedIndex;
        } else {
            // Second click, check if the pieces can be swapped
            if (isAdjacent(firstSelectedIndex, clickedIndex)) {
                swapPieces(firstSelectedIndex, clickedIndex); // Swap the puzzle pieces
                swapCount++; // Increment the swap counter
            }
            firstSelectedIndex = null; // Reset the selection state
        }
    }

    // Swap two puzzle pieces
    private void swapPieces(int index1, int index2) {
        Collections.swap(pieces, index1, index2); // Swap the puzzle pieces
        checkCompletion(); // Check if the puzzle is completed
    }

    // Check if two puzzle pieces are adjacent
    private boolean isAdjacent(int index1, int index2) {
        int row1 = index1 / numColumns;
        int col1 = index1 % numColumns;
        int row2 = index2 / numColumns;
        int col2 = index2 % numColumns;

        // Check if they are adjacent (either vertically or horizontally)
        return (Math.abs(row1 - row2) + Math.abs(col1 - col2) == 1);
    }

    // Check if the puzzle is completed
    private void checkCompletion() {
        boolean completed = true;
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).getPosition() != i) {
                completed = false; // Puzzle is not completed
                break;
            }
        }
        if (completed) {
            int score = calculateScore(swapCount);
            long totalTime = (System.currentTimeMillis() - startTime) / 1000; // Calculate total time (in seconds)
            int intTotalTime = (int) totalTime;
            StatusManager.updateGameStatusToFinish(recordId, score, intTotalTime);
            endGame(); // End the game if the puzzle is completed
            //endGame("Puzzle completed! Total swaps: " + swapCount + ", Time taken: " + totalTime + " seconds"); // Display completion message
            if (gameEventListener != null) {
                gameEventListener.onGameCompleted(intTotalTime, score);
            }
        }
    }

    private int calculateScore(int swapCount){
        if (swapCount <= 10) {
            return 100;
        } else if (swapCount <= 15) {
            return 90;
        } else if (swapCount <= 20) {
            return 80;
        } else if (swapCount <= 25) {
            return 70;
        } else if (swapCount <= 30) {
            return 60;
        } else {
            return 50;
        }
    }
}
