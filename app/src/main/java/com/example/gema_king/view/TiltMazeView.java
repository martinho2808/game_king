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
    private static final float WALL_THICKNESS = 20f;  // 增加牆壁厚度
    private static final float GOAL_RADIUS = 30f;
    private static final float OBSTACLE_RADIUS = 20f;
    private static final int MAZE_SIZE = 6;
    private static final float SENSITIVITY = 0.5f;
    private static final float FRICTION = 0.98f;
    private static final float BOUNCE_FACTOR = 0.7f;
    private static final float MIN_VELOCITY = 0.1f;
    private static final float MAX_VELOCITY = 15.0f;
    private static final float SAFE_DISTANCE = 80f;  // 減少安全距離
    private static final int MAX_WALLS = 20;  // 增加最大牆壁數量
    private static final float START_SAFE_DISTANCE = 100f;
    private static final float TWO_PI = (float)(2 * Math.PI);  // 添加 TWO_PI 常量

    // 添加新的迷宮模式
    private static final int CROSS = 0;
    private static final int ZIGZAG = 1;
    private static final int MAZE = 2;  // 新增迷宮模式

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
        
        // 初始化起點和終點
        start = new float[2];
        goal = new float[2];

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
        }
    }

    private void generateMaze() {
        if (isGeneratingMaze || mazeWidth <= 0 || mazeHeight <= 0) {
            Log.e(TAG, "無法生成迷宮：尺寸無效或正在生成中");
            return;
        }
        isGeneratingMaze = true;
        
        executorService.execute(() -> {
            int maxRetries = 5;
            int currentRetry = 0;
            AtomicBoolean success = new AtomicBoolean(false);
            
            while (!success.get() && currentRetry < maxRetries) {
                try {
                    lock.writeLock().lock();
                    try {
                        walls.clear();
                        obstacles.clear();
                        
                        // 隨機選擇迷宮模式
                        mazePattern = random.nextInt(3);  // 改為3種模式
                        Log.d(TAG, "選擇迷宮模式: " + mazePattern);
                        
                        float margin = Math.max(BALL_RADIUS * 3, GOAL_RADIUS * 3);
                        margin = Math.min(margin, mazeWidth / 4);
                        margin = Math.min(margin, mazeHeight / 4);
                        
                        // 設置起點和終點位置
                        setupStartAndGoalPositions(margin);
                        
                        // 生成基本迷宮模式
                        boolean generated = false;
                        switch (mazePattern) {
                            case CROSS:
                                generated = generateCrossPattern();
                                break;
                            case ZIGZAG:
                                generated = generateZigzagPattern();
                                break;
                            case MAZE:
                                generated = generateMazePattern();
                                break;
                        }
                        
                        // 檢查是否成功生成了足夠的牆壁
                        if (!generated || walls.size() < 5) {
                            Log.d(TAG, "牆壁生成失敗或數量不足，重試");
                            currentRetry++;
                            continue;
                        }
                        
                        // 確保有路徑到達終點
                        if (!ensurePathToGoal()) {
                            currentRetry++;
                            continue;
                        }
                        
                        // 添加動態障礙物
                        addObstacles();
                        
                        // 檢查起點和終點是否與障礙物重疊
                        if (checkObstacleOverlap()) {
                            currentRetry++;
                            continue;
                        }
                        
                        // 重置球的位置
                        resetBallPosition();
                        
                        success.set(true);
                        Log.d(TAG, "成功生成迷宮，牆壁數量: " + walls.size());
                    } finally {
                        lock.writeLock().unlock();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "生成迷宮時發生錯誤", e);
                    currentRetry++;
                }
            }
            
            mainHandler.post(() -> {
                isGeneratingMaze = false;
                if (success.get()) {
                    invalidate();
                } else {
                    Log.d(TAG, "生成失敗，使用簡單迷宮");
                    generateSimpleMaze();
                }
            });
        });
    }

    private void setupStartAndGoalPositions(float margin) {
        // 隨機選擇起點位置（四個角落之一）
        int startCorner = random.nextInt(4);
        switch (startCorner) {
            case 0: // 左上
                start = new float[]{margin, margin};
                break;
            case 1: // 右上
                start = new float[]{mazeWidth - margin, margin};
                break;
            case 2: // 左下
                start = new float[]{margin, mazeHeight - margin};
                break;
            case 3: // 右下
                start = new float[]{mazeWidth - margin, mazeHeight - margin};
                break;
        }
        
        // 設置終點位置（對角）
        switch (startCorner) {
            case 0: // 左上 -> 右下
                goal = new float[]{mazeWidth - margin, mazeHeight - margin};
                break;
            case 1: // 右上 -> 左下
                goal = new float[]{margin, mazeHeight - margin};
                break;
            case 2: // 左下 -> 右上
                goal = new float[]{mazeWidth - margin, margin};
                break;
            case 3: // 右下 -> 左上
                goal = new float[]{margin, margin};
                break;
        }
    }

    private boolean generateCrossPattern() {
        float centerX = mazeWidth / 2f;
        float centerY = mazeHeight / 2f;
        float maxLength = Math.min(mazeWidth, mazeHeight) * 0.4f;
        List<RectF> tempWalls = new ArrayList<>();
        
        // 在四個象限添加小十字
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            float angle = quadrant * (float)Math.PI/2;
            float crossOffset = maxLength * 0.6f;  // 增加偏移距離
            float smallArmLength = maxLength * 0.3f;  // 減少小十字的臂長
            
            // 計算小十字的中心點
            float smallCrossX = centerX + crossOffset * (float)Math.cos(angle);
            float smallCrossY = centerY + crossOffset * (float)Math.sin(angle);
            
            // 小十字的水平臂
            RectF smallHorizontal = new RectF(
                smallCrossX - smallArmLength,
                smallCrossY - WALL_THICKNESS/2,
                smallCrossX + smallArmLength,
                smallCrossY + WALL_THICKNESS/2
            );
            
            // 小十字的垂直臂
            RectF smallVertical = new RectF(
                smallCrossX - WALL_THICKNESS/2,
                smallCrossY - smallArmLength,
                smallCrossX + WALL_THICKNESS/2,
                smallCrossY + smallArmLength
            );
            
            // 添加小十字
            tempWalls.add(smallHorizontal);
            tempWalls.add(smallVertical);
        }
        
        // 檢查路徑是否可行
        if (!ensurePathToGoal()) {
            return false;
        }
        
        walls.addAll(tempWalls);
        return true;
    }

    private boolean generateZigzagPattern() {
        List<RectF> tempWalls = new ArrayList<>();
        float verticalSpacing = mazeHeight / 8f;
        float horizontalSpacing = mazeWidth / 6f;
        float wallLength = horizontalSpacing * 1.5f;
        
        // 生成水平之字形牆
        for (int row = 1; row < 8; row++) {
            float y = row * verticalSpacing;
            boolean isRightToLeft = (row % 2 == 0);
            
            // 每行生成多段水平牆
            for (int col = 0; col < 5; col++) {
                float x = isRightToLeft ? 
                    mazeWidth - (col * horizontalSpacing + horizontalSpacing) :
                    col * horizontalSpacing + horizontalSpacing;
                
                RectF wall = new RectF(
                    x - wallLength/2,
                    y - WALL_THICKNESS/2,
                    x + wallLength/2,
                    y + WALL_THICKNESS/2
                );
                
                tempWalls.add(wall);
            }
            
            // 添加垂直連接
            if (row < 7) {
                float nextY = (row + 1) * verticalSpacing;
                float x = isRightToLeft ? horizontalSpacing : mazeWidth - horizontalSpacing;
                
                RectF connector = new RectF(
                    x - WALL_THICKNESS/2,
                    y,
                    x + WALL_THICKNESS/2,
                    nextY
                );
                
                tempWalls.add(connector);
            }
        }
        
        walls.addAll(tempWalls);
        return true;
    }

    private boolean generateMazePattern() {
        float margin = WALL_THICKNESS * 1.5f;  // 增加牆壁間距
        float effectiveWidth = mazeWidth - margin * 2;
        float effectiveHeight = mazeHeight - margin * 2;
        float cellSize = Math.min(effectiveWidth, effectiveHeight) / 6f;
        List<RectF> tempWalls = new ArrayList<>();
        
        // 計算起始位置，使網格居中
        float startX = (mazeWidth - effectiveWidth) / 2;
        float startY = (mazeHeight - effectiveHeight) / 2;
        
        // 生成外框
        RectF leftBorder = new RectF(startX, startY, startX + WALL_THICKNESS, startY + effectiveHeight);
        RectF rightBorder = new RectF(startX + effectiveWidth - WALL_THICKNESS, startY, startX + effectiveWidth, startY + effectiveHeight);
        RectF topBorder = new RectF(startX, startY, startX + effectiveWidth, startY + WALL_THICKNESS);
        RectF bottomBorder = new RectF(startX, startY + effectiveHeight - WALL_THICKNESS, startX + effectiveWidth, startY + effectiveHeight);
        
        tempWalls.add(leftBorder);
        tempWalls.add(rightBorder);
        tempWalls.add(topBorder);
        tempWalls.add(bottomBorder);
        
        // 生成內部垂直牆
        for (int i = 1; i < 6; i++) {
            float x = startX + i * cellSize;
            for (int j = 0; j < 5; j++) {
                if (random.nextFloat() < 0.7f) {  // 降低牆壁生成機率
                    float y = startY + j * cellSize;
                    float length = cellSize * 0.8f;  // 縮短牆壁長度
                    
                    RectF wall = new RectF(
                        x - WALL_THICKNESS/2,
                        y + cellSize * 0.1f,  // 留出間距
                        x + WALL_THICKNESS/2,
                        y + length
                    );
                    
                    if (!isWallBlockingPath(wall, start, goal) && !isWallOverlapping(wall)) {
                        tempWalls.add(wall);
                    }
                }
            }
        }
        
        // 生成內部水平牆
        for (int j = 1; j < 6; j++) {
            float y = startY + j * cellSize;
            for (int i = 0; i < 5; i++) {
                if (random.nextFloat() < 0.7f) {  // 降低牆壁生成機率
                    float x = startX + i * cellSize;
                    float length = cellSize * 0.8f;  // 縮短牆壁長度
                    
                    RectF wall = new RectF(
                        x + cellSize * 0.1f,  // 留出間距
                        y - WALL_THICKNESS/2,
                        x + length,
                        y + WALL_THICKNESS/2
                    );
                    
                    if (!isWallBlockingPath(wall, start, goal) && !isWallOverlapping(wall)) {
                        tempWalls.add(wall);
                    }
                }
            }
        }
        
        // 確保路徑可達
        if (!ensurePathExists()) {
            return false;
        }
        
        if (tempWalls.size() >= 15) {
            walls.addAll(tempWalls);
            return true;
        }
        return false;
    }

    private boolean ensurePathExists() {
        // 使用簡單的路徑檢查：確保從起點到終點有一條大致的通路
        float dx = goal[0] - start[0];
        float dy = goal[1] - start[1];
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        
        // 檢查路徑上的障礙物密度
        int numChecks = 10;
        float stepX = dx / numChecks;
        float stepY = dy / numChecks;
        int blockedPoints = 0;
        
        for (int i = 1; i < numChecks; i++) {
            float checkX = start[0] + stepX * i;
            float checkY = start[1] + stepY * i;
            float checkRadius = BALL_RADIUS * 2;
            
            // 檢查點周圍是否有太多牆壁
            int wallsNearby = 0;
            for (RectF wall : walls) {
                if (distanceToWall(checkX, checkY, wall) < checkRadius) {
                    wallsNearby++;
                }
            }
            
            if (wallsNearby > 2) { // 如果一個檢查點周圍有超過2個牆壁，認為該點被阻塞
                blockedPoints++;
            }
        }
        
        // 如果被阻塞的點太多，認為路徑不可達
        return blockedPoints < numChecks / 2;
    }

    // 新增計算兩個牆之間最小距離的方法
    private float getMinWallDistance(RectF wall1, RectF wall2) {
        float[] points1 = {
            wall1.left, wall1.top,
            wall1.right, wall1.top,
            wall1.right, wall1.bottom,
            wall1.left, wall1.bottom
        };
        
        float[] points2 = {
            wall2.left, wall2.top,
            wall2.right, wall2.top,
            wall2.right, wall2.bottom,
            wall2.left, wall2.bottom
        };
        
        float minDistance = Float.MAX_VALUE;
        
        // 計算所有頂點之間的最小距離
        for (int i = 0; i < points1.length; i += 2) {
            for (int j = 0; j < points2.length; j += 2) {
                float distance = (float) Math.sqrt(
                    Math.pow(points1[i] - points2[j], 2) +
                    Math.pow(points1[i+1] - points2[j+1], 2)
                );
                minDistance = Math.min(minDistance, distance);
            }
        }
        
        return minDistance;
    }

    private boolean checkObstacleOverlap() {
        for (float[] obstacle : obstacles) {
            if (distance(start[0], start[1], obstacle[0], obstacle[1]) < SAFE_DISTANCE ||
                distance(goal[0], goal[1], obstacle[0], obstacle[1]) < SAFE_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private boolean ensurePathToGoal() {
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
            
            // 再次檢查路徑是否可行
            for (RectF wall : walls) {
                if (rectIntersectsWithLine(wall, start[0], start[1], goal[0], goal[1])) {
                    return false;
                }
            }
        }
        
        return true;
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
        int numObstacles;
        if (mazePattern == CROSS) {
            numObstacles = 12 + random.nextInt(4);  // 十字型迷宮增加障礙物數量到12-15個
        } else {
            numObstacles = 6 + random.nextInt(3);  // 其他模式保持6-8個
        }
        
        float margin = 100f;
        
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
            } while (!validPosition && attempts < 30);
            
            if (validPosition) {
                obstacles.add(new float[]{x, y});
            }
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
        if (mazeWidth <= 0 || mazeHeight <= 0) {
            Log.e(TAG, "無法開始遊戲：迷宮尺寸無效");
            return;
        }
        
        // 確保所有必要的組件都已初始化
        if (walls == null || obstacles == null || start == null || goal == null) {
            Log.e(TAG, "無法開始遊戲：組件未初始化");
            init();
        }
        
        isGameActive.set(true);
        attempts = 0;
        isGameCompleted = false;
        
        // 清除現有的牆壁和障礙物
        walls.clear();
        obstacles.clear();
        
        // 生成新的迷宮
        generateMaze();
        
        // 註冊感應器
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

    private boolean addLWall(float x, float y, float length, float rotationDegrees) {
        float rad = (float) Math.toRadians(rotationDegrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        
        // 水平部分
        float x1 = x - length/2 * cos;
        float y1 = y - length/2 * sin;
        float x2 = x + length/2 * cos;
        float y2 = y + length/2 * sin;
        
        RectF horizontalWall = new RectF(
            Math.min(x1, x2) - WALL_THICKNESS/2,
            Math.min(y1, y2) - WALL_THICKNESS/2,
            Math.max(x1, x2) + WALL_THICKNESS/2,
            Math.max(y1, y2) + WALL_THICKNESS/2
        );
        
        // 垂直部分
        float perpRad = rad + (float) Math.PI/2;
        float x3 = x2 + length/2 * (float)Math.cos(perpRad);
        float y3 = y2 + length/2 * (float)Math.sin(perpRad);
        
        RectF verticalWall = new RectF(
            Math.min(x2, x3) - WALL_THICKNESS/2,
            Math.min(y2, y3) - WALL_THICKNESS/2,
            Math.max(x2, x3) + WALL_THICKNESS/2,
            Math.max(y2, y3) + WALL_THICKNESS/2
        );
        
        if (!isWallCompletelyBlockingPath(horizontalWall)) {
            walls.add(horizontalWall);
        }
        if (!isWallCompletelyBlockingPath(verticalWall)) {
            walls.add(verticalWall);
        }
        
        return walls.size() >= 6; // 至少有主十字和4個L形牆
    }

    private boolean isWallCompletelyBlockingPath(RectF wall) {
        // 檢查牆是否完全阻擋了起點到終點的路徑
        float margin = BALL_RADIUS * 2;
        
        // 計算牆的中心點
        float wallCenterX = (wall.left + wall.right) / 2;
        float wallCenterY = (wall.top + wall.bottom) / 2;
        
        // 計算起點和終點到牆中心的向量
        float startToWallX = wallCenterX - start[0];
        float startToWallY = wallCenterY - start[1];
        float goalToWallX = wallCenterX - goal[0];
        float goalToWallY = wallCenterY - goal[1];
        
        // 計算向量的點積
        float dotProduct = startToWallX * goalToWallX + startToWallY * goalToWallY;
        float startDist = (float) Math.sqrt(startToWallX * startToWallX + startToWallY * startToWallY);
        float goalDist = (float) Math.sqrt(goalToWallX * goalToWallX + goalToWallY * goalToWallY);
        
        // 如果點積為負，表示起點和終點在牆的不同側
        if (dotProduct < 0) {
            // 檢查牆是否太靠近起點或終點
            float distToStart = distanceToWall(start[0], start[1], wall);
            float distToGoal = distanceToWall(goal[0], goal[1], wall);
            
            return distToStart < margin || distToGoal < margin;
        }
        
        return false;
    }

    private float distanceToWall(float x, float y, RectF wall) {
        float closestX = Math.max(wall.left, Math.min(x, wall.right));
        float closestY = Math.max(wall.top, Math.min(y, wall.bottom));
        float dx = x - closestX;
        float dy = y - closestY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    private void generateSimpleMaze() {
        // 生成一個簡單的迷宮，確保遊戲可以進行
        walls.clear();
        obstacles.clear();
        
        // 在中心位置添加一個簡單的十字形牆
        float centerX = mazeWidth / 2;
        float centerY = mazeHeight / 2;
        float wallLength = Math.min(mazeWidth, mazeHeight) * 0.3f;
        
        // 水平牆
        RectF horizontalWall = new RectF(
            centerX - wallLength/2,
            centerY - WALL_THICKNESS,
            centerX + wallLength/2,
            centerY + WALL_THICKNESS
        );
        
        if (!isWallOverlapping(horizontalWall)) {
            walls.add(horizontalWall);
        }
        
        // 垂直牆
        RectF verticalWall = new RectF(
            centerX - WALL_THICKNESS,
            centerY - wallLength/2,
            centerX + WALL_THICKNESS,
            centerY + wallLength/2
        );
        
        if (!isWallOverlapping(verticalWall)) {
            walls.add(verticalWall);
        }
        
        // 重置球的位置
        resetBallPosition();
    }

    // 添加檢查牆壁是否重疊的方法
    private boolean isWallOverlapping(RectF newWall) {
        // 檢查與現有牆壁的重疊
        for (RectF wall : walls) {
            if (RectF.intersects(newWall, wall)) {
                return true;
            }
        }
        
        // 檢查與起點和終點的距離
        float[] points = {start[0], start[1], goal[0], goal[1]};
        for (int i = 0; i < points.length; i += 2) {
            if (newWall.contains(points[i], points[i + 1])) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isWallBlockingPath(RectF wall, float[] start, float[] goal) {
        float margin = BALL_RADIUS * 3;
        
        // 檢查牆是否太靠近起點或終點
        if (distanceToWall(start[0], start[1], wall) < margin ||
            distanceToWall(goal[0], goal[1], wall) < margin) {
            return true;
        }
        
        // 檢查牆是否直接阻擋路徑
        float wallCenterX = (wall.left + wall.right) / 2;
        float wallCenterY = (wall.top + wall.bottom) / 2;
        
        // 計算向量
        float pathX = goal[0] - start[0];
        float pathY = goal[1] - start[1];
        float wallX = wallCenterX - start[0];
        float wallY = wallCenterY - start[1];
        
        // 計算投影
        float pathLength = (float) Math.sqrt(pathX * pathX + pathY * pathY);
        float projection = (pathX * wallX + pathY * wallY) / pathLength;
        
        // 如果投影在路徑上且距離小於安全距離，則視為阻擋
        return projection > 0 && projection < pathLength && 
               Math.abs(pathX * wallY - pathY * wallX) / pathLength < margin;
    }

    // 更新牆壁重疊檢查方法
    private boolean isWallsOverlapping(RectF wall1, RectF wall2) {
        if (!RectF.intersects(wall1, wall2)) {
            return false;
        }
        
        // 計算重疊區域
        float overlapLeft = Math.max(wall1.left, wall2.left);
        float overlapRight = Math.min(wall1.right, wall2.right);
        float overlapTop = Math.max(wall1.top, wall2.top);
        float overlapBottom = Math.min(wall1.bottom, wall2.bottom);
        
        float overlapArea = (overlapRight - overlapLeft) * (overlapBottom - overlapTop);
        float wall1Area = (wall1.right - wall1.left) * (wall1.bottom - wall1.top);
        float wall2Area = (wall2.right - wall2.left) * (wall2.bottom - wall2.top);
        float minArea = Math.min(wall1Area, wall2Area);
        
        return overlapArea > (minArea * 0.1f);  // 允許10%的重疊
    }

    public void resumeGame() {
        Log.d(TAG, "恢復遊戲");
        if (mazeWidth <= 0 || mazeHeight <= 0) {
            Log.e(TAG, "無法恢復遊戲：迷宮尺寸無效");
            return;
        }
        
        isGameActive.set(true);
        isGameCompleted = false;
        
        // 註冊感應器
        if (sensorManager != null && accelerometer != null) {
            boolean registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "註冊加速度計: " + registered);
        } else {
            Log.e(TAG, "無法註冊加速度計: sensorManager=" + (sensorManager != null) + ", accelerometer=" + (accelerometer != null));
        }
        
        invalidate();
    }
} 