package com.ingcorp.webhard.adapter;

import static android.provider.Settings.System.getString;

import android.content.Context;
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
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.ingcorp.webhard.R;
import com.ingcorp.webhard.database.entity.Game;

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String BASE_IMAGE_URL = "http://retrogamemaster.net";
    private static final int TYPE_GAME = 0;
    private static final int TYPE_AD = 1;
    private static final int AD_FREQUENCY = 6; // 6개마다 광고 삽입

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

    @Override
    public int getItemViewType(int position) {
        // AD_FREQUENCY 간격으로 광고 삽입
        if ((position + 1) % (AD_FREQUENCY + 1) == 0) {
            return TYPE_AD;
        }
        return TYPE_GAME;
    }

    @Override
    public int getItemCount() {
        if (gameList.isEmpty()) return 0;
        // 게임 수 + 광고 수 계산
        int adCount = gameList.size() / AD_FREQUENCY;
        return gameList.size() + adCount;
    }

    private int getGamePosition(int adapterPosition) {
        // 광고를 제외한 실제 게임 위치 계산
        int adCount = adapterPosition / (AD_FREQUENCY + 1);
        return adapterPosition - adCount;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_ad_native, parent, false);
            return new AdViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
            return new GameViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_AD) {
            ((AdViewHolder) holder).loadAd();
        } else {
            int gamePosition = getGamePosition(position);
            if (gamePosition < gameList.size()) {
                ((GameViewHolder) holder).bind(gameList.get(gamePosition));
            }
        }
    }

    // 게임 ViewHolder (기존)
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
            gameNameText.setText(game.getGameName());
            loadGameImage(game);

            itemContainer.setOnClickListener(v -> {
                if (onGameClickListener != null) {
                    onGameClickListener.onGameClick(game);
                }
            });

            itemContainer.setOnLongClickListener(v -> {
                if (onGameClickListener != null) {
                    onGameClickListener.onGameLongClick(game);
                }
                return true;
            });
        }

        private void loadGameImage(Game game) {
            String gameId = game.getGameId();
            if (gameId != null) {
                int drawableId = getDrawableResourceId(gameId);
                if (drawableId != 0) {
                    gameImage.setImageResource(drawableId);
                    return;
                }
            }

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
            String resourceName = "game_" + gameId.toLowerCase().replaceAll("[^a-z0-9]", "_");
            return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        }

        private String buildImageUrl(String gameImg) {
            if (gameImg == null || gameImg.isEmpty()) {
                return "";
            }

            if (gameImg.startsWith("http")) {
                return gameImg;
            }

            if (gameImg.startsWith("/")) {
                return BASE_IMAGE_URL + gameImg;
            } else {
                return BASE_IMAGE_URL + "/" + gameImg;
            }
        }

        private int getDefaultImageForCategory(String category) {
            return R.drawable.ic_game_default;
        }
    }

    // 광고 ViewHolder
    public class AdViewHolder extends RecyclerView.ViewHolder {
        private ImageView adImage;
        private TextView adTitle;
        private TextView adLabel;
        private View adContainer;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adImage = itemView.findViewById(R.id.ad_image);
            adTitle = itemView.findViewById(R.id.ad_title);
            adLabel = itemView.findViewById(R.id.ad_label);
            adContainer = itemView.findViewById(R.id.ad_container);
        }

        public void loadAd() {
            AdLoader adLoader = new AdLoader.Builder(context, getString(R.string.admob_id_native)) // 실제 광고 단위 ID로 교체
                    .forNativeAd(nativeAd -> {
                        // 광고 로드 성공
                        populateNativeAdView(nativeAd);
                    })
                    .withAdListener(new com.google.android.gms.ads.AdListener() {
                        @Override
                        public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError adError) {
                            // 광고 로드 실패 시 기본 이미지 표시
                            showDefaultAd();
                        }
                    })
                    .withNativeAdOptions(new NativeAdOptions.Builder().build())
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        }

        private void populateNativeAdView(NativeAd nativeAd) {
            // 광고 제목 설정
            if (nativeAd.getHeadline() != null) {
                adTitle.setText(nativeAd.getHeadline());
            }

            // 광고 이미지 설정
            if (nativeAd.getImages() != null && !nativeAd.getImages().isEmpty()) {
                Glide.with(context)
                        .load(nativeAd.getImages().get(0).getUri())
                        .into(adImage);
            }

            // 클릭 이벤트 설정
            adContainer.setOnClickListener(v -> {
                if (nativeAd.getCallToAction() != null) {
                    // 광고 클릭 처리
                }
            });
        }

        private void showDefaultAd() {
            // 광고 로드 실패 시 기본 표시
            adImage.setImageResource(R.drawable.ic_game_default);
            adTitle.setText("게임 더 보기");
        }
    }

    // 데이터 업데이트 메서드들
    public void updateGameList(List<Game> newGameList) {
        this.gameList.clear();
        this.gameList.addAll(newGameList);
        notifyDataSetChanged();
    }

    public void addGame(Game game) {
        gameList.add(game);
        notifyDataSetChanged(); // 광고 위치가 변경될 수 있으므로 전체 갱신
    }

    public void removeGame(int position) {
        int gamePosition = getGamePosition(position);
        if (gamePosition >= 0 && gamePosition < gameList.size()) {
            gameList.remove(gamePosition);
            notifyDataSetChanged(); // 광고 위치가 변경될 수 있으므로 전체 갱신
        }
    }

    public int findGamePosition(String gameId) {
        for (int i = 0; i < gameList.size(); i++) {
            if (gameList.get(i).getGameId().equals(gameId)) {
                return i;
            }
        }
        return -1;
    }
}