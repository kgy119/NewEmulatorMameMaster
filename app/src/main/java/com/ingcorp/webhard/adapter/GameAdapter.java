package com.ingcorp.webhard.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.ingcorp.webhard.R;
import com.ingcorp.webhard.database.entity.Game;
import com.ingcorp.webhard.manager.AdMobManager;

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "mame00";
    private static final String BASE_IMAGE_URL = "http://retrogamemaster.net";
    private static final int TYPE_GAME = 0;
    private static final int TYPE_AD = 1;
    private static final int AD_FREQUENCY = 6; // 6개마다 광고 삽입

    private List<Game> gameList;
    private Context context;
    private OnGameClickListener onGameClickListener;
    private AdMobManager adMobManager;

    public interface OnGameClickListener {
        void onGameClick(Game game);
        void onGameLongClick(Game game);
    }

    public GameAdapter(List<Game> gameList, Context context) {
        this.gameList = gameList;
        this.context = context;
        this.adMobManager = AdMobManager.getInstance(context);
    }

    public void setOnGameClickListener(OnGameClickListener listener) {
        this.onGameClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // 첫 번째 아이템은 항상 광고
        if (position == 0) {
            return TYPE_AD;
        }
        // 그 이후부터는 AD_FREQUENCY 간격으로 광고 삽입
        else if ((position) % (AD_FREQUENCY + 1) == 0) {
            return TYPE_AD;
        }
        return TYPE_GAME;
    }

    @Override
    public int getItemCount() {
        if (gameList == null || gameList.isEmpty()) return 0;
        // 게임 수 + 광고 수 계산
        int adCount = gameList.size() / AD_FREQUENCY;
        return gameList.size() + adCount;
    }

    private int getGamePosition(int adapterPosition) {
        // 첫 번째는 광고이므로 게임 위치 계산 시 1을 빼고 시작
        if (adapterPosition == 0) {
            return -1; // 첫 번째는 광고이므로 게임이 아님
        }

        // 광고를 제외한 실제 게임 위치 계산
        int adjustedPosition = adapterPosition - 1; // 첫 번째 광고 제외
        int adCount = adjustedPosition / AD_FREQUENCY; // 추가 광고 개수
        return adjustedPosition - adCount;
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
            if (gamePosition >= 0 && gamePosition < gameList.size()) {
                ((GameViewHolder) holder).bind(gameList.get(gamePosition));
            }
        }
    }

    // 게임 ViewHolder
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
            if (game == null) return;

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

    // 네이티브 광고 ViewHolder (게임 포맷과 유사하게)
    public class AdViewHolder extends RecyclerView.ViewHolder {
        private NativeAdView nativeAdView;
        private MediaView mediaView;
        private TextView adHeadline;
        // 숨겨진 필수 요소들 (AdMob 요구사항)
        private TextView adBody;
        private TextView adAdvertiser;
        private RatingBar adStars;
        private TextView adPrice;
        private Button adCallToAction;
        private ImageView adIcon;
        private NativeAd currentNativeAd;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            nativeAdView = (NativeAdView) itemView;

            // 주요 표시 요소들
            mediaView = nativeAdView.findViewById(R.id.ad_media);
            adHeadline = nativeAdView.findViewById(R.id.ad_headline);

            // 숨겨진 필수 요소들 (AdMob 정책 준수)
            adBody = nativeAdView.findViewById(R.id.ad_body);
            adAdvertiser = nativeAdView.findViewById(R.id.ad_advertiser);
            adStars = nativeAdView.findViewById(R.id.ad_stars);
            adPrice = nativeAdView.findViewById(R.id.ad_price);
            adCallToAction = nativeAdView.findViewById(R.id.ad_call_to_action);
            adIcon = nativeAdView.findViewById(R.id.ad_app_icon);
        }

        public void loadAd() {
            Log.d(TAG, "광고 로드 시작 - 위치: " + getAdapterPosition());

            // 기존 광고가 있다면 정리
            if (currentNativeAd != null) {
                Log.d(TAG, "기존 광고 정리");
                currentNativeAd.destroy();
                currentNativeAd = null;
            }

            // AdMob 매니저를 통해 광고 로드
            adMobManager.loadNativeAd(new AdMobManager.OnNativeAdLoadedListener() {
                @Override
                public void onAdLoaded(NativeAd nativeAd) {
                    currentNativeAd = nativeAd;
                    populateNativeAdView(nativeAd);
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(TAG, "광고 로드 실패: " + error);
                    showDefaultAd();
                }
            });
        }

        private void loadAdInternal() {
            // 이 메서드는 더 이상 필요하지 않음 (AdMobManager로 이동)
        }

        private void populateNativeAdView(NativeAd nativeAd) {
            Log.d(TAG, "네이티브 광고 뷰 설정 시작");

            try {
                // 광고 제목 설정 (게임 이름처럼 표시)
                if (nativeAd.getHeadline() != null) {
                    Log.d(TAG, "광고 제목 설정: " + nativeAd.getHeadline());
                    adHeadline.setText(nativeAd.getHeadline());
                    nativeAdView.setHeadlineView(adHeadline);
                } else {
                    Log.d(TAG, "광고 제목이 없어 기본값 설정");
//                    adHeadline.setText("광고");
                    nativeAdView.setHeadlineView(adHeadline);
                }

                // 미디어 콘텐츠 설정 (게임 이미지처럼 표시)
                if (nativeAd.getMediaContent() != null) {
                    Log.d(TAG, "미디어 콘텐츠 설정");
                    mediaView.setMediaContent(nativeAd.getMediaContent());
                    nativeAdView.setMediaView(mediaView);
                } else {
                    Log.d(TAG, "미디어 콘텐츠 없음");
                }

                // 필수 요소들을 NativeAdView에 등록 (숨겨진 상태로)
                if (nativeAd.getBody() != null && adBody != null) {
                    Log.d(TAG, "광고 본문 설정: " + nativeAd.getBody());
                    adBody.setText(nativeAd.getBody());
                    nativeAdView.setBodyView(adBody);
                }

                if (nativeAd.getAdvertiser() != null && adAdvertiser != null) {
                    Log.d(TAG, "광고주 설정: " + nativeAd.getAdvertiser());
                    adAdvertiser.setText(nativeAd.getAdvertiser());
                    nativeAdView.setAdvertiserView(adAdvertiser);
                }

                if (nativeAd.getStarRating() != null && adStars != null) {
                    Log.d(TAG, "별점 설정: " + nativeAd.getStarRating());
                    adStars.setRating(nativeAd.getStarRating().floatValue());
                    nativeAdView.setStarRatingView(adStars);
                }

                if (nativeAd.getPrice() != null && adPrice != null) {
                    Log.d(TAG, "가격 설정: " + nativeAd.getPrice());
                    adPrice.setText(nativeAd.getPrice());
                    nativeAdView.setPriceView(adPrice);
                }

                if (nativeAd.getCallToAction() != null && adCallToAction != null) {
                    Log.d(TAG, "행동 유도 버튼 설정: " + nativeAd.getCallToAction());
                    adCallToAction.setText(nativeAd.getCallToAction());
                    nativeAdView.setCallToActionView(adCallToAction);
                }

                if (nativeAd.getIcon() != null && adIcon != null) {
                    Log.d(TAG, "아이콘 설정");
                    adIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
                    nativeAdView.setIconView(adIcon);
                }

                // NativeAdView에 NativeAd 객체 등록 (중요!)
                Log.d(TAG, "NativeAdView에 NativeAd 등록");
                nativeAdView.setNativeAd(nativeAd);

                Log.d(TAG, "네이티브 광고 뷰 설정 완료");

            } catch (Exception e) {
                Log.e(TAG, "광고 뷰 설정 중 오류 발생: " + e.getMessage(), e);
                showDefaultAd();
            }
        }

        private void showDefaultAd() {
            Log.d(TAG, "기본 광고 표시");

            // 광고 로드 실패 시 기본 표시 (게임 아이템과 유사하게)
            if (adHeadline != null) {
                adHeadline.setText("광고");
                Log.d(TAG, "기본 광고 제목 설정 완료");
            }

            // 미디어뷰에 기본 이미지 표시는 불가능하므로
            // 광고 로드 실패 시에는 제목만 표시
        }
    }

    // 데이터 업데이트 메서드들
    public void updateGameList(List<Game> newGameList) {
        if (newGameList != null) {
            this.gameList.clear();
            this.gameList.addAll(newGameList);
            notifyDataSetChanged();
        }
    }

    public void addGame(Game game) {
        if (game != null && gameList != null) {
            gameList.add(game);
            notifyDataSetChanged(); // 광고 위치가 변경될 수 있으므로 전체 갱신
        }
    }

    public void removeGame(int position) {
        if (gameList != null) {
            int gamePosition = getGamePosition(position);
            if (gamePosition >= 0 && gamePosition < gameList.size()) {
                gameList.remove(gamePosition);
                notifyDataSetChanged(); // 광고 위치가 변경될 수 있으므로 전체 갱신
            }
        }
    }

    public int findGamePosition(String gameId) {
        if (gameId != null && gameList != null) {
            for (int i = 0; i < gameList.size(); i++) {
                Game game = gameList.get(i);
                if (game != null && gameId.equals(game.getGameId())) {
                    return i;
                }
            }
        }
        return -1;
    }

    // 메모리 누수 방지를 위한 정리 메서드
    public void cleanup() {
        if (gameList != null) {
            gameList.clear();
        }
        onGameClickListener = null;
    }
}