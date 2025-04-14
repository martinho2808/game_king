package com.example.gema_king.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;

public class TiltMazeView extends View implements SensorEventListener {
    private static final String TAG = "TiltMazeView";
    private static final float BALL_RADIUS = 20f;
    private static final float WALL_THICKNESS = 15f;  // 增加牆壁厚度
    private static final float GOAL_RADIUS = 30f;
    private static final float OBSTACLE_RADIUS = 20f;  // 減小障礙物半徑
    private static final int MAZE_SIZE = 6;  // 增加到 6x6 網格
    private static final float SENSITIVITY = 0.5f;
    private static final float FRICTION = 0.98f;
    private static final float BOUNCE_FACTOR = 0.7f;
    private static final float MIN_VELOCITY = 0.1f;
    private static final float MAX_VELOCITY = 15.0f;
    private static final float SAFE_DISTANCE = 100f;  // 增加安全距離
    private static final int MAX_WALLS = 20;  // 增加最大牆壁數量
    private static final float START_SAFE_DISTANCE = 120f;  // 增加起點安全距離

    // 添加新的迷宮模式
    private static final int CROSS = 0;
    private static final int SPIRAL = 1;
    private static final int ZIGZAG = 2;
    private static final int MAZE = 3;  // 新增迷宮模式

    private Paint ballPaint;
    private Paint wallPaint;
    private Paint goalPaint;
    private Paint obstaclePaint;
    private Paint gridPaint;

    private float ballX;
    private float ballY;
    private float ballVelocityX = 0;
    private float ballVelocityY = 0;

    private float cellWidth;
    private float cellHeight;
    private float mazeWidth;
    private float mazeHeight;

    private List<RectF> walls; // 改用 RectF 來表示牆壁，便於碰撞檢測
    private List<float[]> obstacles;
    private float[] goal;
    private float[] start;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private AtomicBoolean isGameActive = new AtomicBoolean(false);
    private OnGameEventListener gameEventListener;
    private int attempts = 0;
    private Random random;
    private int mazePattern = 0; // 迷宮模式
    
    private Handler mainHandler;
    private ExecutorService executorService;
    private boolean isGeneratingMaze = false;
    
    // 添加讀寫鎖來保護共享資源
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // 添加一個標誌來防止重複觸發遊戲完成事件
    private boolean isGameCompleted = false;

    public interface OnGameEventListener {
        void onGameComplete(long time);
        void onGameOver();
    }

    public TiltMazeView(Context context) {
        super(context);
        init();
    }

    public TiltMazeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Log.d(TAG, "初始化 TiltMazeView");
        ballPaint = new Paint();
        ballPaint.setColor(Color.RED);
        ballPaint.setStyle(Paint.Style.FILL);

        wallPaint = new Paint();
        wallPaint.setColor(Color.WHITE);
        wallPaint.setStyle(Paint.Style.FILL);

        goalPaint = new Paint();
        goalPaint.setColor(Color.GREEN);
        goalPaint.setStyle(Paint.Style.FILL);

        obstaclePaint = new Paint();
        obstaclePaint.setColor(Color.YELLOW);
        obstaclePaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setColor(Color.GRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);

        walls = new ArrayList<>();
        obstacles = new ArrayList<>();
        random = new Random();

        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        if (accelerometer == null) {
            Log.e(TAG, "設備沒有加速度計感應器");
        }
        
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: w=" + w + ", h=" + h);
        if (w > 0 && h > 0) {  // 確保 View 有有效的尺寸
            cellWidth = w / (float) MAZE_SIZE;
            cellHeight = h / (float) MAZE_SIZE;
            mazeWidth = w;
            mazeHeight = h;
            
            // 只在遊戲開始時生成迷宮
            if (isGameActive.get()) {
                generateMaze();
            }
        }
    }

    private void generateMaze() {
        if (isGeneratingMaze || mazeWidth <= 0 || mazeHeight <= 0) {
            return;
        }
        isGeneratingMaze = true;
        
        executorService.execute(() -> {
            try {
                lock.writeLock().lock();
                try {
                    walls.clear();
                    obstacles.clear();
                    
                    // 確保起點和終點在有效範圍內
                    generateRandomStartAndGoal();
                    
                    // 隨機選擇迷宮模式
                    mazePattern = random.nextInt(4);
                    
                    // 生成基本迷宮模式
                    switch (mazePattern) {
                        case CROSS:
                            generateCrossPattern();
                            break;
                        case SPIRAL:
                            generateSpiralPattern();
                            break;
                        case ZIGZAG:
                            generateZigzagPattern();
                            break;
                        case MAZE:
                            generateMazePattern();
                            break;
                    }
                    
                    // 確保有路徑到達終點
                    ensurePathToGoal();
                    
                    // 添加動態障礙物
                    addDynamicObstacles();
                    
                    // 重置球的位置
                    resetBallPosition();
                    
                } finally {
                    lock.writeLock().unlock();
                }
                
                mainHandler.post(() -> {
                    isGeneratingMaze = false;
                    invalidate();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "生成迷宮時發生錯誤", e);
                isGeneratingMaze = false;
            }
        });
    }

    private void generateRandomStartAndGoal() {
        // 定義邊緣安全距離
        float margin = Math.max(BALL_RADIUS * 3, GOAL_RADIUS * 3);
        
        // 確保 margin 不會超過 View 尺寸的一半
        margin = Math.min(margin, mazeWidth / 4);
        margin = Math.min(margin, mazeHeight / 4);
        
        // 隨機選擇起點和終點的位置類型
        int startPos = random.nextInt(4);
        int goalPos;
        do {
            goalPos = random.nextInt(4);
        } while (goalPos == startPos);
        
        // 設置起點
        switch (startPos) {
            case 0:  // 左上
                start = new float[]{
                    margin + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    margin + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
            case 1:  // 右上
                start = new float[]{
                    mazeWidth * 2/3 + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    margin + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
            case 2:  // 左下
                start = new float[]{
                    margin + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    mazeHeight * 2/3 + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
            case 3:  // 右下
                start = new float[]{
                    mazeWidth * 2/3 + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    mazeHeight * 2/3 + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
        }
        
        // 設置終點
        switch (goalPos) {
            case 0:  // 左上
                goal = new float[]{
                    margin + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    margin + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
            case 1:  // 右上
                goal = new float[]{
                    mazeWidth * 2/3 + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    margin + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
            case 2:  // 左下
                goal = new float[]{
                    margin + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    mazeHeight * 2/3 + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
            case 3:  // 右下
                goal = new float[]{
                    mazeWidth * 2/3 + random.nextFloat() * (mazeWidth/3 - 2 * margin),
                    mazeHeight * 2/3 + random.nextFloat() * (mazeHeight/3 - 2 * margin)
                };
                break;
        }
        
        // 確保起點和終點不會太靠近
        float minDistance = Math.min(mazeWidth, mazeHeight) / 3;
        if (distance(start[0], start[1], goal[0], goal[1]) < minDistance) {
            // 如果太靠近，重新生成終點
            generateRandomStartAndGoal();
        }
    }

    private void ensurePathToGoal() {
        // 檢查起點到終點是否有直接路徑
        boolean hasDirectPath = true;
        for (RectF wall : walls) {
            if (rectIntersectsWithLine(wall, start[0], start[1], goal[0], goal[1])) {
                hasDirectPath = false;
                break;
            }
        }
        
        // 如果沒有直接路徑，創建一個
        if (!hasDirectPath) {
            // 在起點和終點之間創建一個通道
            float midX = (start[0] + goal[0]) / 2;
            float midY = (start[1] + goal[1]) / 2;
            
            // 清除可能阻擋路徑的牆
            walls.removeIf(wall -> 
                rectIntersectsWithLine(wall, start[0], start[1], midX, midY) ||
                rectIntersectsWithLine(wall, midX, midY, goal[0], goal[1])
            );
        }
    }

    // 添加檢查矩形是否與線段相交的方法
    private boolean rectIntersectsWithLine(RectF rect, float x1, float y1, float x2, float y2) {
        // 檢查線段的端點是否在矩形內
        if (rect.contains(x1, y1) || rect.contains(x2, y2)) {
            return true;
        }

        // 檢查線段是否與矩形的邊相交
        float[] lines = {
            rect.left, rect.top, rect.right, rect.top,  // 上邊
            rect.right, rect.top, rect.right, rect.bottom,  // 右邊
            rect.right, rect.bottom, rect.left, rect.bottom,  // 下邊
            rect.left, rect.bottom, rect.left, rect.top   // 左邊
        };

        for (int i = 0; i < lines.length; i += 4) {
            if (lineSegmentsIntersect(x1, y1, x2, y2, 
                                    lines[i], lines[i+1], 
                                    lines[i+2], lines[i+3])) {
                return true;
            }
        }

        return false;
    }

    // 添加檢查兩條線段是否相交的方法
    private boolean lineSegmentsIntersect(float x1, float y1, float x2, float y2,
                                        float x3, float y3, float x4, float y4) {
        float denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (denominator == 0) {
            return false;
        }

        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator;
        float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator;

        return t >= 0 && t <= 1 && u >= 0 && u <= 1;
    }

    private void addObstacles() {
        int maxObstacles = 5;
        int attempts = 0;
        int maxAttempts = 50;
        
        while (obstacles.size() < maxObstacles && attempts < maxAttempts) {
            float x = random.nextFloat() * (getWidth() - 200f) + 100f;
            float y = random.nextFloat() * (getHeight() - 200f) + 100f;
            
            // 確保障礙物不會太靠近起點和終點
            if (Math.abs(x - ballX) < SAFE_DISTANCE * 2 || 
                Math.abs(y - ballY) < SAFE_DISTANCE * 2 ||
                Math.abs(x - goal[0]) < SAFE_DISTANCE * 2 || 
                Math.abs(y - goal[1]) < SAFE_DISTANCE * 2) {
                attempts++;
                continue;
            }
            
            // 確保障礙物不會擋住主要路徑
            if (!isObstacleBlockingPath(x, y)) {
                obstacles.add(new float[]{x, y});
            }
            attempts++;
        }
    }

    private boolean isObstacleBlockingPath(float x, float y) {
        // 檢查障礙物是否會擋住起點到終點的路徑
        float pathWidth = 100f;
        float startX = ballX;
        float startY = ballY;
        float endX = goal[0];
        float endY = goal[1];
        
        // 計算障礙物到路徑的最短距離
        float distance = pointToLineDistance(x, y, startX, startY, endX, endY);
        return distance < pathWidth + OBSTACLE_RADIUS;
    }

    private float pointToLineDistance(float px, float py, float x1, float y1, float x2, float y2) {
        float A = px - x1;
        float B = py - y1;
        float C = x2 - x1;
        float D = y2 - y1;
        
        float dot = A * C + B * D;
        float lenSq = C * C + D * D;
        float param = -1;
        
        if (lenSq != 0) {
            param = dot / lenSq;
        }
        
        float xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        float dx = px - xx;
        float dy = py - yy;
        
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void addRandomWalls() {
        int wallCount = 0;
        int maxAttempts = 50;
        
        while (wallCount < MAX_WALLS && maxAttempts > 0) {
            float x = random.nextFloat() * mazeWidth;
            float y = random.nextFloat() * mazeHeight;
            float width = random.nextFloat() * cellWidth * 2;
            float height = random.nextFloat() * cellHeight * 2;
            
            RectF newWall = new RectF(x, y, x + width, y + height);
            
            // 檢查是否與起點或終點重疊
            if (isTooCloseToPoint(newWall, start[0], start[1]) || 
                isTooCloseToPoint(newWall, goal[0], goal[1])) {
                maxAttempts--;
                continue;
            }
            
            // 檢查是否與現有牆壁重疊過多
            boolean tooMuchOverlap = false;
            for (RectF wall : walls) {
                if (calculateOverlap(newWall, wall) > 0.3f) {
                    tooMuchOverlap = true;
                    break;
                }
            }
            
            if (!tooMuchOverlap) {
                walls.add(newWall);
                wallCount++;
            }
            
            maxAttempts--;
        }
    }

    private boolean isTooCloseToPoint(RectF wall, float x, float y) {
        float centerX = (wall.left + wall.right) / 2;
        float centerY = (wall.top + wall.bottom) / 2;
        float distance = (float) Math.sqrt(
            Math.pow(centerX - x, 2) + Math.pow(centerY - y, 2)
        );
        return distance < SAFE_DISTANCE;
    }

    private float calculateOverlap(RectF rect1, RectF rect2) {
        float overlapWidth = Math.max(0, Math.min(rect1.right, rect2.right) - Math.max(rect1.left, rect2.left));
        float overlapHeight = Math.max(0, Math.min(rect1.bottom, rect2.bottom) - Math.max(rect1.top, rect2.top));
        float overlapArea = overlapWidth * overlapHeight;
        float rect1Area = (rect1.right - rect1.left) * (rect1.bottom - rect1.top);
        return overlapArea / rect1Area;
    }

    private void resetBallPosition() {
        ballX = start[0];
        ballY = start[1];
        ballVelocityX = 0;
        ballVelocityY = 0;
        Log.d(TAG, "重置球位置: (" + ballX + ", " + ballY + ")");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 獲取讀鎖
        lock.readLock().lock();
        try {
            // 繪製網格
            for (int i = 0; i <= MAZE_SIZE; i++) {
                canvas.drawLine(i * cellWidth, 0, i * cellWidth, mazeHeight, gridPaint);
                canvas.drawLine(0, i * cellHeight, mazeWidth, i * cellHeight, gridPaint);
            }

            // 繪製牆壁
            for (RectF wall : walls) {
                canvas.drawRect(wall, wallPaint);
            }

            // 繪製障礙物
            for (float[] obstacle : obstacles) {
                canvas.drawCircle(obstacle[0], obstacle[1], OBSTACLE_RADIUS, obstaclePaint);
            }

            // 繪製終點
            canvas.drawCircle(goal[0], goal[1], GOAL_RADIUS, goalPaint);

            // 繪製球
            canvas.drawCircle(ballX, ballY, BALL_RADIUS, ballPaint);
        } finally {
            // 釋放讀鎖
            lock.readLock().unlock();
        }
    }

    public void startGame() {
        Log.d(TAG, "開始遊戲");
        isGameActive.set(true);
        attempts = 0;
        isGameCompleted = false;
        generateMaze();
        if (sensorManager != null && accelerometer != null) {
            boolean registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "註冊加速度計: " + registered);
        } else {
            Log.e(TAG, "無法註冊加速度計: sensorManager=" + (sensorManager != null) + ", accelerometer=" + (accelerometer != null));
        }
        invalidate();
    }

    public void stopGame() {
        Log.d(TAG, "停止遊戲");
        isGameActive.set(false);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isGameActive.get() || isGameCompleted) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

            // 應用靈敏度，但限制最大加速度
            float maxAccel = 10.0f;
            x = Math.max(-maxAccel, Math.min(maxAccel, x));
            y = Math.max(-maxAccel, Math.min(maxAccel, y));

            ballVelocityX += -x * SENSITIVITY;
            ballVelocityY += y * SENSITIVITY;

            // 限制最大速度
            float currentSpeed = (float) Math.sqrt(ballVelocityX * ballVelocityX + ballVelocityY * ballVelocityY);
            if (currentSpeed > MAX_VELOCITY) {
                float scale = MAX_VELOCITY / currentSpeed;
                ballVelocityX *= scale;
                ballVelocityY *= scale;
            }

            // 應用摩擦力
            ballVelocityX *= FRICTION;
            ballVelocityY *= FRICTION;

            // 如果速度太小，設為0
            if (Math.abs(ballVelocityX) < MIN_VELOCITY) {
                ballVelocityX = 0;
            }
            if (Math.abs(ballVelocityY) < MIN_VELOCITY) {
                ballVelocityY = 0;
            }

            // 更新球的位置
            float newBallX = ballX + ballVelocityX;
            float newBallY = ballY + ballVelocityY;

            // 檢查邊界碰撞
            boolean collision = false;
            
            if (newBallX < BALL_RADIUS) {
                newBallX = BALL_RADIUS;
                ballVelocityX *= -BOUNCE_FACTOR;
                collision = true;
            } else if (newBallX > mazeWidth - BALL_RADIUS) {
                newBallX = mazeWidth - BALL_RADIUS;
                ballVelocityX *= -BOUNCE_FACTOR;
                collision = true;
            }

            if (newBallY < BALL_RADIUS) {
                newBallY = BALL_RADIUS;
                ballVelocityY *= -BOUNCE_FACTOR;
                collision = true;
            } else if (newBallY > mazeHeight - BALL_RADIUS) {
                newBallY = mazeHeight - BALL_RADIUS;
                ballVelocityY *= -BOUNCE_FACTOR;
                collision = true;
            }

            // 獲取讀鎖
            lock.readLock().lock();
            try {
                // 檢查牆壁碰撞
                for (RectF wall : walls) {
                    if (checkWallCollision(newBallX, newBallY, wall)) {
                        // 計算反彈方向
                        float centerX = (wall.left + wall.right) / 2;
                        float centerY = (wall.top + wall.bottom) / 2;
                        
                        // 計算球到牆壁中心的方向向量
                        float dx = ballX - centerX;
                        float dy = ballY - centerY;
                        
                        // 標準化方向向量
                        float length = (float) Math.sqrt(dx * dx + dy * dy);
                        if (length > 0) {
                            dx /= length;
                            dy /= length;
                        }
                        
                        // 應用反彈
                        ballVelocityX = dx * Math.abs(ballVelocityX) * BOUNCE_FACTOR;
                        ballVelocityY = dy * Math.abs(ballVelocityY) * BOUNCE_FACTOR;
                        
                        // 將球移出牆壁
                        newBallX = ballX;
                        newBallY = ballY;
                        collision = true;
                        break;
                    }
                }

                // 更新球的位置
                ballX = newBallX;
                ballY = newBallY;

                // 檢查障礙物碰撞
                for (float[] obstacle : obstacles) {
                    float dx = ballX - obstacle[0];
                    float dy = ballY - obstacle[1];
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance < BALL_RADIUS + OBSTACLE_RADIUS) {
                        Log.d(TAG, "碰到障礙物，重置球位置");
                        resetBallPosition();
                        attempts++;
                        if (gameEventListener != null) {
                            gameEventListener.onGameOver();
                        }
                        return;
                    }
                }

                // 檢查是否到達終點
                float dx = ballX - goal[0];
                float dy = ballY - goal[1];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < BALL_RADIUS + GOAL_RADIUS && !isGameCompleted) {
                    Log.d(TAG, "到達終點，遊戲完成");
                    isGameCompleted = true;
                    if (gameEventListener != null) {
                        gameEventListener.onGameComplete(System.currentTimeMillis());
                    }
                    stopGame();
                }
            } finally {
                // 釋放讀鎖
                lock.readLock().unlock();
            }

            invalidate();
        }
    }

    // 檢查球是否與牆壁碰撞
    private boolean checkWallCollision(float ballX, float ballY, RectF wall) {
        // 計算球到牆壁的最短距離
        float closestX = Math.max(wall.left, Math.min(ballX, wall.right));
        float closestY = Math.max(wall.top, Math.min(ballY, wall.bottom));
        
        float distanceX = ballX - closestX;
        float distanceY = ballY - closestY;
        
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        return distanceSquared < BALL_RADIUS * BALL_RADIUS;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 不需要實現
    }

    public void setOnGameEventListener(OnGameEventListener listener) {
        this.gameEventListener = listener;
    }

    public int getAttempts() {
        return attempts;
    }

    public void resetGame() {
        Log.d(TAG, "重置遊戲");
        attempts = 0;
        isGameActive.set(false);
        isGameCompleted = false;
        ballX = start[0];
        ballY = start[1];
        ballVelocityX = 0;
        ballVelocityY = 0;
        invalidate();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void generateCrossPattern() {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float wallLength = Math.min(getWidth(), getHeight()) * 0.7f;
        float gapSize = 80f;
        
        // 創建十字形狀，但有多個缺口
        // 水平牆
        for (int i = 0; i < 3; i++) {
            float segmentLength = wallLength / 3;
            float startX = centerX - wallLength/2 + i * segmentLength;
            walls.add(new RectF(startX + gapSize/2, centerY - WALL_THICKNESS,
                              startX + segmentLength - gapSize/2, centerY + WALL_THICKNESS));
        }
        
        // 垂直牆
        for (int i = 0; i < 3; i++) {
            float segmentLength = wallLength / 3;
            float startY = centerY - wallLength/2 + i * segmentLength;
            walls.add(new RectF(centerX - WALL_THICKNESS, startY + gapSize/2,
                              centerX + WALL_THICKNESS, startY + segmentLength - gapSize/2));
        }
        
        // 添加一些額外的小牆段增加難度
        for (int i = 0; i < 4; i++) {
            float x = random.nextFloat() * (getWidth() - 200) + 100;
            float y = random.nextFloat() * (getHeight() - 200) + 100;
            float length = 100f + random.nextFloat() * 100f;
            
            if (random.nextBoolean()) {
                // 水平牆
                walls.add(new RectF(x, y, x + length, y + WALL_THICKNESS));
            } else {
                // 垂直牆
                walls.add(new RectF(x, y, x + WALL_THICKNESS, y + length));
            }
        }
    }

    private void generateSpiralPattern() {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float maxRadius = Math.min(getWidth(), getHeight()) * 0.4f;
        float spacing = maxRadius / 4;
        
        // 生成螺旋形狀的牆，每圈都有缺口
        for (int i = 1; i <= 4; i++) {
            float radius = spacing * i;
            float gapSize = 70f;
            float gapRotation = (float) (Math.PI / 2 * i); // 每層缺口旋轉90度
            
            for (int angle = 0; angle < 360; angle += 30) {
                float rad = (float) Math.toRadians(angle);
                float nextRad = (float) Math.toRadians(angle + 30);
                
                // 檢查是否需要在此處創建缺口
                if (Math.abs(rad - gapRotation) % (Math.PI * 2) > 0.5) {
                    float x1 = centerX + (float) (radius * Math.cos(rad));
                    float y1 = centerY + (float) (radius * Math.sin(rad));
                    float x2 = centerX + (float) (radius * Math.cos(nextRad));
                    float y2 = centerY + (float) (radius * Math.sin(nextRad));
                    
                    walls.add(new RectF(
                        Math.min(x1, x2), Math.min(y1, y2),
                        Math.max(x1, x2), Math.max(y1, y2)
                    ));
                }
            }
        }
    }

    private void generateZigzagPattern() {
        float spacing = getHeight() / 7f;
        float wallThickness = WALL_THICKNESS;
        float gapSize = 90f;
        
        // 生成不規則的之字形牆
        for (int i = 1; i < 6; i++) {
            float y = spacing * i;
            float offset = (i % 2 == 0) ? 50f : -50f; // 交替偏移
            
            // 分段生成牆，確保有多個通道
            float[] segments = {0f, getWidth() * 0.33f, getWidth() * 0.66f, getWidth()};
            for (int j = 0; j < segments.length - 1; j++) {
                float startX = segments[j];
                float endX = segments[j + 1];
                
                if (random.nextFloat() > 0.3f) { // 70%的機率生成牆
                    walls.add(new RectF(
                        startX + gapSize + offset,
                        y - wallThickness,
                        endX - gapSize + offset,
                        y + wallThickness
                    ));
                }
            }
            
            // 添加一些垂直連接
            if (i < 5 && random.nextFloat() > 0.5f) {
                float x = segments[random.nextInt(segments.length - 1)] + offset;
                walls.add(new RectF(
                    x - wallThickness,
                    y,
                    x + wallThickness,
                    y + spacing
                ));
            }
        }
    }

    private void generateMazePattern() {
        float cellSize = Math.min(getWidth(), getHeight()) / MAZE_SIZE;
        float wallThickness = WALL_THICKNESS;
        
        // 創建網格式迷宮
        for (int i = 0; i < MAZE_SIZE; i++) {
            for (int j = 0; j < MAZE_SIZE; j++) {
                if (random.nextFloat() > 0.7f) { // 30%的機率生成牆
                    // 隨機選擇水平或垂直牆
                    if (random.nextBoolean()) {
                        // 水平牆
                        walls.add(new RectF(
                            i * cellSize,
                            j * cellSize,
                            (i + 1) * cellSize - 40f,
                            j * cellSize + wallThickness
                        ));
                    } else {
                        // 垂直牆
                        walls.add(new RectF(
                            i * cellSize,
                            j * cellSize,
                            i * cellSize + wallThickness,
                            (j + 1) * cellSize - 40f
                        ));
                    }
                }
            }
        }
    }

    private void addDynamicObstacles() {
        int numObstacles = 4 + random.nextInt(3); // 4-6個障礙物
        float margin = 80f;
        
        for (int i = 0; i < numObstacles; i++) {
            float x, y;
            boolean validPosition;
            int attempts = 0;
            
            do {
                validPosition = true;
                x = margin + random.nextFloat() * (getWidth() - 2 * margin);
                y = margin + random.nextFloat() * (getHeight() - 2 * margin);
                
                // 檢查與起點和終點的距離
                if (distance(x, y, start[0], start[1]) < START_SAFE_DISTANCE ||
                    distance(x, y, goal[0], goal[1]) < START_SAFE_DISTANCE) {
                    validPosition = false;
                    continue;
                }
                
                // 檢查與其他障礙物的距離
                for (float[] obstacle : obstacles) {
                    if (distance(x, y, obstacle[0], obstacle[1]) < SAFE_DISTANCE) {
                        validPosition = false;
                        break;
                    }
                }
                
                // 檢查與牆壁的距離
                for (RectF wall : walls) {
                    if (distanceToWall(x, y, wall) < SAFE_DISTANCE) {
                        validPosition = false;
                        break;
                    }
                }
                
                attempts++;
            } while (!validPosition && attempts < 10);
            
            if (validPosition) {
                obstacles.add(new float[]{x, y});
            }
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    private float distanceToWall(float x, float y, RectF wall) {
        float dx = Math.max(wall.left - x, Math.max(0, x - wall.right));
        float dy = Math.max(wall.top - y, Math.max(0, y - wall.bottom));
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
} 