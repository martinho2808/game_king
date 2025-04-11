package com.example.gema_king.model;

public class GameStatus {
    private final String username;
    private final String status;
    private final int score;
    private final int playTime;

    public GameStatus(String username, String status, int score, int playTime) {
        this.username = username;
        this.status = status;
        this.score = score;
        this.playTime = playTime;
    }


    public String getUsername() {
        return username;
    }
    public String getStatus() {
        return status;
    }

    public int getScore() {
        return score;
    }

    public int getPlayTime() {
        return playTime;
    }
}