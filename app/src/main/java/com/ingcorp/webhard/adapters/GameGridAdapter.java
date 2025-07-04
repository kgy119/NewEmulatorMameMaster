package com.ingcorp.webhard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ingcorp.webhard.database.GameEntity;

import java.util.List;

public class GameGridAdapter extends RecyclerView.Adapter<GameGridAdapter.GameViewHolder> {

    private List<GameEntity> games;
    private OnGameClickListener listener;

    public interface OnGameClickListener {
        void onGameClick(GameEntity game);
    }

    public GameGridAdapter(List<GameEntity> games, OnGameClickListener listener) {
        this.games = games;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 기본 Android 레이아웃 사용 (안전하고 간단)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameEntity game = games.get(position);
        holder.bind(game);
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    public void updateGames(List<GameEntity> newGames) {
        this.games = newGames;
        notifyDataSetChanged();
    }

    public class GameViewHolder extends RecyclerView.ViewHolder {
        private TextView gameTitle;
        private TextView gameCategory;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            // 기본 Android 레이아웃의 ID 사용
            gameTitle = itemView.findViewById(android.R.id.text1);
            gameCategory = itemView.findViewById(android.R.id.text2);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGameClick(games.get(position));
                }
            });
        }

        public void bind(GameEntity game) {
            // 게임 제목 표시
            if (gameTitle != null) {
                gameTitle.setText(game.getGameName());
            }

            // 게임 카테고리만 표시 (실행수 제거)
            if (gameCategory != null) {
                String category = game.getGameCategory() != null ? game.getGameCategory() : "Unknown";
                gameCategory.setText("Category: " + category);
            }
        }
    }
}