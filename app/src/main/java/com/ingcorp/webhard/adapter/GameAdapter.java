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
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.model.BaseItem;
import com.ingcorp.webhard.model.GameItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "GameAdapter";
    private static final String BASE_IMAGE_URL = "http://retrogamemaster.net";

    private List<BaseItem> itemList; // 통합 리스트
    private Context context;
    private OnGameClickListener onGameClickListener;
    private AdMobManager adMobManager;
    private UtilHelper utilHelper;

    public interface OnGameClickListener {
        void onGameClick(Game game);
        void onGameLongClick(Game game);
    }

    public GameAdapter(List<BaseItem> itemList, Context context) {
        this.itemList = itemList != null ? itemList : new ArrayList<>();
        this.context = context;
        this.adMobManager = AdMobManager.getInstance(context);
        this.utilHelper = UtilHelper.getInstance(context);

        Log.d(TAG, "GameAdapter 생성 - 아이템 수: " + this.itemList.size());

        // 전면광고 미리 로드
        loadInterstitialAdIfNeeded();
    }

    public void setOnGameClickListener(OnGameClickListener listener) {
        this.onGameClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < itemList.size()) {
            return itemList.get(position).getItemType();
        }
        return BaseItem.TYPE_GAME; // 기본값
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == BaseItem.TYPE_AD) {
            Log.d(TAG, "네이티브 광고 ViewHolder 생성");
            View view = LayoutInflater.from(context).inflate(R.layout.item_ad_native, parent, false);
            return new AdViewHolder(view);
        } else {
            Log.v(TAG, "게임 ViewHolder 생성");
            View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
            return new GameViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= itemList.size()) {
            Log.e(TAG, "잘못된 위치: " + position + " (최대: " + (itemList.size() - 1) + ")");
            return;
        }

        BaseItem item = itemList.get(position);

        if (item.getItemType() == BaseItem.TYPE_AD) {
            Log.d(TAG, "위치 " + position + "에 네이티브 광고 바인딩");
            ((AdViewHolder) holder).loadAd();
        } else if (item.getItemType() == BaseItem.TYPE_GAME) {
            GameItem gameItem = (GameItem) item;
            Log.v(TAG, "위치 " + position + "에 게임 바인딩: " + gameItem.getGame().getGameName());
            ((GameViewHolder) holder).bind(gameItem.getGame());
        }
    }

    // 게임 ViewHolder - 기존과 동일
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
                Log.d(TAG, "Game clicked: " + game.getGameName());

                // Check internet connection
                if (!utilHelper.isNetworkConnected()) {
                    Log.w(TAG, "No internet connection - showing warning dialog");
                    utilHelper.showGameNetworkErrorDialog((android.app.Activity) context);
                    return;
                }

                // Show confirmation dialog
                utilHelper.showConfirmDialog(
                        (android.app.Activity) context,
                        "Launch Game",
                        "Do you want to start " + game.getGameName() + "?",
                        () -> {
                            // User confirmed - proceed with game launch
                            proceedWithGameLaunch(game);
                        }
                );
            });

            itemContainer.setOnLongClickListener(v -> {
                if (onGameClickListener != null) {
                    onGameClickListener.onGameLongClick(game);
                }
                return true;
            });
        }

        private void proceedWithGameLaunch(Game game) {
            // Check if should show interstitial ad
            boolean shouldShowAd = utilHelper.shouldShowInterstitialAd();

            if (shouldShowAd && adMobManager != null && adMobManager.isInterstitialAdReady()) {
                Log.d(TAG, "Showing interstitial ad before game launch");
                adMobManager.showInterstitialAd((android.app.Activity) context, new AdMobManager.OnInterstitialAdShownListener() {
                    @Override
                    public void onAdShown() {
                        Log.d(TAG, "Interstitial ad shown");
                    }

                    @Override
                    public void onAdClosed() {
                        Log.d(TAG, "Interstitial ad closed - launching game");
                        if (onGameClickListener != null) {
                            onGameClickListener.onGameClick(game);
                        }
                    }

                    @Override
                    public void onAdShowFailed(String error) {
                        Log.e(TAG, "Interstitial ad show failed: " + error + " - launching game directly");
                        if (onGameClickListener != null) {
                            onGameClickListener.onGameClick(game);
                        }
                    }

                    @Override
                    public void onAdNotReady() {
                        Log.w(TAG, "Interstitial ad not ready - launching game directly");
                        if (onGameClickListener != null) {
                            onGameClickListener.onGameClick(game);
                        }
                    }
                });
            } else {
                Log.d(TAG, "No ad to show or ad not ready - launching game directly");
                if (onGameClickListener != null) {
                    onGameClickListener.onGameClick(game);
                }
                loadInterstitialAdIfNeeded();
            }
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

    // 네이티브 광고 ViewHolder - 기존과 동일
    public class AdViewHolder extends RecyclerView.ViewHolder {
        private NativeAdView nativeAdView;
        private MediaView mediaView;
        private TextView adHeadline;
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

            mediaView = nativeAdView.findViewById(R.id.ad_media);
            adHeadline = nativeAdView.findViewById(R.id.ad_headline);
            adBody = nativeAdView.findViewById(R.id.ad_body);
            adAdvertiser = nativeAdView.findViewById(R.id.ad_advertiser);
            adStars = nativeAdView.findViewById(R.id.ad_stars);
            adPrice = nativeAdView.findViewById(R.id.ad_price);
            adCallToAction = nativeAdView.findViewById(R.id.ad_call_to_action);
            adIcon = nativeAdView.findViewById(R.id.ad_app_icon);
        }

        public void loadAd() {
            Log.d(TAG, "광고 로드 시작 - 위치: " + getAdapterPosition());

            if (currentNativeAd != null) {
                Log.d(TAG, "기존 광고 정리");
                currentNativeAd.destroy();
                currentNativeAd = null;
            }

            adMobManager.loadNativeAd(new AdMobManager.OnNativeAdLoadedListener() {
                @Override
                public void onAdLoaded(NativeAd nativeAd) {
                    Log.d(TAG, "네이티브 광고 로드 성공!");
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

        private void populateNativeAdView(NativeAd nativeAd) {
            Log.d(TAG, "네이티브 광고 뷰 설정 시작");

            try {
                if (nativeAd.getHeadline() != null) {
                    adHeadline.setText(nativeAd.getHeadline());
                    nativeAdView.setHeadlineView(adHeadline);
                } else {
                    nativeAdView.setHeadlineView(adHeadline);
                }

                if (nativeAd.getMediaContent() != null) {
                    mediaView.setMediaContent(nativeAd.getMediaContent());
                    nativeAdView.setMediaView(mediaView);
                }

                if (nativeAd.getBody() != null && adBody != null) {
                    adBody.setText(nativeAd.getBody());
                    nativeAdView.setBodyView(adBody);
                }

                if (nativeAd.getAdvertiser() != null && adAdvertiser != null) {
                    adAdvertiser.setText(nativeAd.getAdvertiser());
                    nativeAdView.setAdvertiserView(adAdvertiser);
                }

                if (nativeAd.getStarRating() != null && adStars != null) {
                    adStars.setRating(nativeAd.getStarRating().floatValue());
                    nativeAdView.setStarRatingView(adStars);
                }

                if (nativeAd.getPrice() != null && adPrice != null) {
                    adPrice.setText(nativeAd.getPrice());
                    nativeAdView.setPriceView(adPrice);
                }

                if (nativeAd.getCallToAction() != null && adCallToAction != null) {
                    adCallToAction.setText(nativeAd.getCallToAction());
                    nativeAdView.setCallToActionView(adCallToAction);
                }

                if (nativeAd.getIcon() != null && adIcon != null) {
                    adIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
                    nativeAdView.setIconView(adIcon);
                }

                nativeAdView.setNativeAd(nativeAd);
                Log.d(TAG, "네이티브 광고 뷰 설정 완료");

            } catch (Exception e) {
                Log.e(TAG, "광고 뷰 설정 중 오류 발생: " + e.getMessage(), e);
                showDefaultAd();
            }
        }

        private void showDefaultAd() {
            Log.d(TAG, "기본 광고 표시");
            if (adHeadline != null) {
                adHeadline.setText("광고");
            }
        }
    }

    private void loadInterstitialAdIfNeeded() {
        if (adMobManager != null && !adMobManager.isInterstitialAdReady()) {
            Log.d(TAG, "전면광고 미리 로드 시작");
            adMobManager.loadInterstitialAd(new AdMobManager.OnInterstitialAdLoadedListener() {
                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "전면광고 미리 로드 완료");
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(TAG, "전면광고 미리 로드 실패: " + error);
                }

                @Override
                public void onAdClosed() {
                    // 광고 닫힘 후 자동으로 다음 광고 로드됨
                }

                @Override
                public void onAdShown() {
                    // 광고 표시됨
                }

                @Override
                public void onAdShowFailed(String error) {
                    Log.e(TAG, "전면광고 표시 실패: " + error);
                }
            });
        }
    }

    // 데이터 업데이트 메서드들 (단순화됨)
    public void updateItemList(List<BaseItem> newItemList) {
        if (newItemList != null) {
            this.itemList.clear();
            this.itemList.addAll(newItemList);
            notifyDataSetChanged();
            Log.d(TAG, "아이템 리스트 업데이트 완료 - 새로운 크기: " + itemList.size());
        }
    }
}