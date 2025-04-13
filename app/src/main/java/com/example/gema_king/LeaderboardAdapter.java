package com.example.gema_king;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class LeaderboardAdapter extends BaseAdapter {
    private final Context context;
    private final List<HashMap<String, String>> data;

    public LeaderboardAdapter(Context context, List<HashMap<String, String>> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView rankText, usernameText, scoreText, timeText;
        Button detailButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false); // ← 修正這裡
            holder = new ViewHolder();
            holder.rankText = convertView.findViewById(R.id.rank_text);
            holder.usernameText = convertView.findViewById(R.id.username_text);
            holder.scoreText = convertView.findViewById(R.id.ranking_score);
            holder.timeText = convertView.findViewById(R.id.ranking_time);
            holder.detailButton = convertView.findViewById(R.id.btn_details);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HashMap<String, String> item = data.get(position);
        holder.rankText.setText(item.get("rank"));
        holder.usernameText.setText(item.get("username"));
        holder.scoreText.setText(item.get("score"));
        holder.timeText.setText(item.get("time"));

        holder.detailButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlayerDetailActivity.class);
            intent.putExtra("userId", Integer.parseInt(Objects.requireNonNull(item.get("userId"))));
            //intent.putExtra("username", item.get("username")); // 如果 PlayerDetailActivity 有需要用到
            context.startActivity(intent);
        });

        return convertView;
    }
}
