package com.ingcorp.webhard.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.ingcorp.webhard.R;
import com.ingcorp.webhard.database.entity.Game;

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private static final String BASE_IMAGE_URL = "http://retrogamemaster.net";

    private List<Game> gameList;
    private Context context;
    private OnGameClickListener onGameClickListener;

    public interface OnGameClickListener {
        void onGameClick(Game game);
        void onGameLongClick(Game game);
    }

    public GameAdapter(List<Game> gameList, Context context) {
        this.gameList = gameList;
        this.context = context;
    }

    public void setOnGameClickListener(OnGameClickListener listener) {
        this.onGameClickListener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = gameList.get(position);
        holder.bind(game);
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    public class GameViewHolder extends RecyclerView.ViewHolder {
        private ImageView gameImage;
        private TextView gameNameText;
        private View itemContainer;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameImage = itemView.findViewById(R.id.game_image);
            gameNameText = itemView.findViewById(R.id.game_name_text);
            itemContainer = itemView.findViewById(R.id.item_container);
        }

        public void bind(Game game) {
            // 게임 이름 설정
            gameNameText.setText(game.getGameName());

            // 게임 이미지 로드
            loadGameImage(game);

            // 클릭 리스너 설정
            itemContainer.setOnClickListener(v -> {
                if (onGameClickListener != null) {
                    onGameClickListener.onGameClick(game);
                }
            });

            // 롱클릭 리스너 설정
            itemContainer.setOnLongClickListener(v -> {
                if (onGameClickListener != null) {
                    onGameClickListener.onGameLongClick(game);
                }
                return true;
            });
        }

        private void loadGameImage(Game game) {
            // 먼저 drawable에서 이미지 확인
            String gameId = game.getGameId();
            if (gameId != null) {
                int drawableId = getDrawableResourceId(gameId);
                if (drawableId != 0) {
                    // drawable에 이미지가 있으면 사용
                    gameImage.setImageResource(drawableId);
                    return;
                }
            }

            // drawable에 없으면 Glide로 웹에서 로드
            String imageUrl = buildImageUrl(game.getGameImg());

            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(getDefaultImageForCategory(game.getGameCate()))
                    .error(getDefaultImageForCategory(game.getGameCate()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();

            Glide.with(context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(gameImage);
        }

        private int getDrawableResourceId(String gameId) {
            // drawable 폴더에서 게임 ID로 이미지 찾기
            String resourceName = "game_" + gameId.toLowerCase().replaceAll("[^a-z0-9]", "_");
            return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        }

        private String buildImageUrl(String gameImg) {
            if (gameImg == null || gameImg.isEmpty()) {
                return "";
            }

            // 이미 전체 URL인 경우
            if (gameImg.startsWith("http")) {
                return gameImg;
            }

            // 상대 경로인 경우 BASE_URL과 결합
            if (gameImg.startsWith("/")) {
                return BASE_IMAGE_URL + gameImg;
            } else {
                return BASE_IMAGE_URL + "/" + gameImg;
            }
        }

        private int getDefaultImageForCategory(String category) {
            // 모든 카테고리에서 동일한 기본 이미지 사용
            return R.drawable.ic_game_default;
        }
    }

    // 데이터 업데이트 메서드
    public void updateGameList(List<Game> newGameList) {
        this.gameList.clear();
        this.gameList.addAll(newGameList);
        notifyDataSetChanged();
    }

    // 게임 추가
    public void addGame(Game game) {
        gameList.add(game);
        notifyItemInserted(gameList.size() - 1);
    }

    // 게임 제거
    public void removeGame(int position) {
        if (position >= 0 && position < gameList.size()) {
            gameList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // 특정 게임 찾기
    public int findGamePosition(String gameId) {
        for (int i = 0; i < gameList.size(); i++) {
            if (gameList.get(i).getGameId().equals(gameId)) {
                return i;
            }
        }
        return -1;
    }
}