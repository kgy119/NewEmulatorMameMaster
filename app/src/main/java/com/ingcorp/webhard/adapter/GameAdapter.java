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
import com.ingcorp.webhard.base.Constants;
import com.ingcorp.webhard.database.entity.Game;
import com.ingcorp.webhard.manager.AdMobManager;
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.model.BaseItem;
import com.ingcorp.webhard.model.GameItem;

import java.util.ArrayList;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "GameAdapter";

    private List<BaseItem> itemList;
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
        return BaseItem.TYPE_GAME;
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == BaseItem.TYPE_AD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_ad_native, parent, false);
            return new AdViewHolder(view);
        } else {
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
            ((AdViewHolder) holder).loadAd();
        } else if (item.getItemType() == BaseItem.TYPE_GAME) {
            GameItem gameItem = (GameItem) item;
            ((GameViewHolder) holder).bind(gameItem.getGame());
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
                // 인터넷 연결 확인
                if (!utilHelper.isNetworkConnected()) {
                    Log.w(TAG, "인터넷 연결 없음 - 경고 다이얼로그 표시");
                    utilHelper.showGameNetworkErrorDialog((android.app.Activity) context);
                    return;
                }

                // 확인 다이얼로그 표시
                utilHelper.showConfirmDialog(
                        (android.app.Activity) context,
                        "Launch Game",
                        "Do you want to start " + game.getGameName() + "?",
                        () -> proceedWithGameLaunch(game)
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
            // 전면광고 표시 여부 확인
            boolean shouldShowAd = utilHelper.shouldShowInterstitialAd();

            if (shouldShowAd && adMobManager != null && adMobManager.isInterstitialAdReady()) {
                adMobManager.showInterstitialAd((android.app.Activity) context, new AdMobManager.OnInterstitialAdShownListener() {
                    @Override
                    public void onAdShown() {
                        // 광고 표시됨
                    }

                    @Override
                    public void onAdClosed() {
                        if (onGameClickListener != null) {
                            onGameClickListener.onGameClick(game);
                        }
                    }

                    @Override
                    public void onAdShowFailed(String error) {
                        Log.e(TAG, "전면광고 표시 실패: " + error);
                        if (onGameClickListener != null) {
                            onGameClickListener.onGameClick(game);
                        }
                    }

                    @Override
                    public void onAdNotReady() {
                        if (onGameClickListener != null) {
                            onGameClickListener.onGameClick(game);
                        }
                    }
                });
            } else {
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
                return Constants.BASE_IMAGE_URL + gameImg;
            } else {
                return Constants.BASE_IMAGE_URL + "/" + gameImg;
            }
        }

        private int getDefaultImageForCategory(String category) {
            return R.drawable.ic_game_default;
        }
    }

    // 네이티브 광고 ViewHolder
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
            if (currentNativeAd != null) {
                currentNativeAd.destroy();
                currentNativeAd = null;
            }

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

        private void populateNativeAdView(NativeAd nativeAd) {
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

            } catch (Exception e) {
                Log.e(TAG, "광고 뷰 설정 중 오류 발생: " + e.getMessage(), e);
                showDefaultAd();
            }
        }

        private void showDefaultAd() {
            if (adHeadline != null) {
                adHeadline.setText("광고");
            }
        }
    }

    private void loadInterstitialAdIfNeeded() {
        if (adMobManager != null && !adMobManager.isInterstitialAdReady()) {
            adMobManager.loadInterstitialAd(new AdMobManager.OnInterstitialAdLoadedListener() {
                @Override
                public void onAdLoaded() {
                    // 전면광고 로드 완료
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(TAG, "전면광고 로드 실패: " + error);
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

    // 데이터 업데이트 메서드
    public void updateItemList(List<BaseItem> newItemList) {
        if (newItemList != null) {
            this.itemList.clear();
            this.itemList.addAll(newItemList);
            notifyDataSetChanged();
        }
    }
}