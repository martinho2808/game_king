package com.example.gema_king.model;

import android.content.Context;

import com.example.gema_king.DatabaseHelper;

public class StatusManager {
    private static final String game_not_start = "Not Started";
    private static final String game_in_progress = "In Progress";
    private static final String game_stop = "Stopped";
    private static final String game_finished = "Finished";
    private static DatabaseHelper dbHelper;
    // 私有构造函数
    private StatusManager() {
        // 不允许实例化
    }

    // 静态方法进行初始化
    public static void init(Context context) {
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(context);
        }
    }
    public static Integer initGameStatus(int userId, int gameId) {
        Integer recordId;
        recordId = dbHelper.recordExists(userId, gameId);
        if(recordId == null) {
            dbHelper.addGameRecord(userId, gameId, game_not_start);
            recordId = dbHelper.recordExists(userId, gameId);
        }
        return recordId;
    }

    public static void updateGameStatusToProgress(int recordId){
        GameStatus gameStatus = dbHelper.getStatusById(recordId);
        if(!gameStatus.getStatus().equals(game_finished)) {
            dbHelper.updateStatusById(recordId, game_in_progress, 0,gameStatus.getPlayTime());
        }
    }

    public static void updateGameStatusToStop(int recordId){
        GameStatus gameStatus = dbHelper.getStatusById(recordId);
        if(!gameStatus.getStatus().equals(game_finished)) {
            dbHelper.updateStatusById(recordId, game_stop, 0,gameStatus.getPlayTime());
        }
    }

    public static void updateGameStatusToFinish(int recordId, int score, int playTime){
        GameStatus gameStatus = dbHelper.getStatusById(recordId);
        int updatePlayTime;
        int updateScore = Math.max(gameStatus.getScore(), score);
        if(gameStatus.getPlayTime() != 0){
            if(score < gameStatus.getScore()) {
                updatePlayTime = playTime;
            } else if (score == gameStatus.getScore()){
                updatePlayTime = Math.min(gameStatus.getPlayTime(), playTime);
            } else {
                updatePlayTime = gameStatus.getPlayTime();
            }
        } else {
            updatePlayTime = playTime;
        }

        dbHelper.updateStatusById(recordId, game_finished, updateScore, updatePlayTime);

    }
    public static void updateGamePlayed(int userId){
        dbHelper.updateGamePlayedByUserId(userId);
    }
}
