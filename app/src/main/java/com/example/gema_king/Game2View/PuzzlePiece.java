package com.example.gema_king.Game2View;

import android.graphics.Bitmap;
public class PuzzlePiece {
    private Bitmap bitmap;
    private int position; // 在拼图中的位置

    public PuzzlePiece(Bitmap bitmap, int position) {
        this.bitmap = bitmap;
        this.position = position;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}