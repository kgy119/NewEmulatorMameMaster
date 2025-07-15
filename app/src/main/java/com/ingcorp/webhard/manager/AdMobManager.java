package com.ingcorp.webhard.manager;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ingcorp.webhard.R;
import com.ingcorp.webhard.base.Constants;

import androidx.annotation.NonNull;

public class AdMobManager {
    private static final String TAG = Constants.LOG_TAG;
    private static AdMobManager instance;

    private Context context;
    private AdView mBannerAdView;
    private InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd; // 리워드 광고 추가
    private boolean isLoadingInterstitial = false;
    private boolean isLoadingRewarded = false; // 리워드 광고 로딩 상태

    private AdMobManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AdMobManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdMobManager(context);
        }
        return instance;
    }

    /// 접을 수 있는 배너 광고 로드
    public void loadCollapsibleBanner(Context activityContext, FrameLayout adContainerView, OnBannerAdLoadedListener listener) {
        Log.d(TAG, "접을 수 있는 배너 광고 로드 시작");

        try {
            // AdView 생성
            AdView adView = new AdView(activityContext);
            adView.setAdUnitId(activityContext.getString(R.string.admob_id_banner));

            // 적응형 배너 크기 설정
            AdSize adSize = getAdaptiveBannerSize(activityContext);
            adView.setAdSize(adSize);

            // 접을 수 있는 배너 매개변수 추가
            Bundle extras = new Bundle();
            extras.putString("collapsible", "bottom"); // 하단 배치용

            // 광고 요청 생성
            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();

            // 광고 리스너 설정
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    Log.d(TAG, String.format("배너 광고 로드 완료. 접을 수 있는 배너: %s",
                            isCollapsibleBannerSupported() ? "예" : "아니오"));

                    if (listener != null) {
                        listener.onAdLoaded(adView);
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    super.onAdFailedToLoad(adError);
                    Log.e(TAG, "배너 광고 로드 실패:");
                    Log.e(TAG, "Error Code: " + adError.getCode());
                    Log.e(TAG, "Error Message: " + adError.getMessage());
                    Log.e(TAG, "Error Domain: " + adError.getDomain());

                    String errorReason = getBannerErrorReason(adError.getCode());
                    Log.e(TAG, "에러 원인: " + errorReason);

                    if (listener != null) {
                        listener.onAdLoadFailed(errorReason);
                    }
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                    Log.d(TAG, "배너 광고 열림");
                }

                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    Log.d(TAG, "배너 광고 닫힘");
                }
            });

            // 컨테이너에 AdView 추가
            adContainerView.removeAllViews();
            adContainerView.addView(adView);

            // 광고 로드
            adView.loadAd(adRequest);

            // 멤버 변수에 저장
            mBannerAdView = adView;

            Log.d(TAG, "접을 수 있는 배너 광고 요청 완료");

        } catch (Exception e) {
            Log.e(TAG, "배너 광고 로드 중 예외 발생: " + e.getMessage(), e);
            if (listener != null) {
                listener.onAdLoadFailed("배너 광고 로드 중 예외 발생: " + e.getMessage());
            }
        }
    }

    /// 적응형 배너 크기 계산
    private AdSize getAdaptiveBannerSize(Context activityContext) {
        WindowManager windowManager = (WindowManager) activityContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activityContext, adWidth);
    }

    /// 접을 수 있는 배너 지원 여부 확인
    public boolean isCollapsibleBannerSupported() {
        // Android 10 (API 29) 이상에서만 접는 배너 지원
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    }

    /// 배너 광고 에러 원인 반환
    private String getBannerErrorReason(int errorCode) {
        switch (errorCode) {
            case 0: // ERROR_CODE_INTERNAL_ERROR
                return "내부 오류 - AdMob 서버 문제 또는 잘못된 설정";
            case 1: // ERROR_CODE_INVALID_REQUEST
                return "잘못된 요청 - 광고 단위 ID 확인 필요 또는 앱 ID 누락";
            case 2: // ERROR_CODE_NETWORK_ERROR
                return "네트워크 오류 - 인터넷 연결 확인";
            case 3: // ERROR_CODE_NO_FILL
                return "광고 없음 - 현재 사용 가능한 광고가 없음";
            case 8: // ERROR_CODE_APP_ID_MISSING
                return "앱 ID 누락 - AndroidManifest.xml에 APPLICATION_ID 설정 필요";
            default:
                return "알 수 없는 오류 (코드: " + errorCode + ")";
        }
    }

    /// 리워드 광고 로드
    public void loadRewardedAd(OnRewardedAdLoadedListener listener) {
        if (isLoadingRewarded) {
            Log.d(TAG, "리워드 광고가 이미 로딩 중입니다");
            if (listener != null) {
                listener.onAdLoadFailed("리워드 광고가 이미 로딩 중입니다");
            }
            return;
        }

        Log.d(TAG, "리워드 광고 로드 시작");
        isLoadingRewarded = true;

        try {
            String adUnitId = context.getString(R.string.admob_id_reward); // 리워드 광고 ID 추가 필요
            AdRequest adRequest = new AdRequest.Builder().build();

            RewardedAd.load(context, adUnitId, adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            mRewardedAd = rewardedAd;
                            isLoadingRewarded = false;
                            Log.d(TAG, "리워드 광고 로드 성공");

                            if (listener != null) {
                                listener.onAdLoaded();
                            }
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            isLoadingRewarded = false;
                            Log.e(TAG, "리워드 광고 로드 실패:");
                            Log.e(TAG, "Error Code: " + loadAdError.getCode());
                            Log.e(TAG, "Error Message: " + loadAdError.getMessage());
                            Log.e(TAG, "Error Domain: " + loadAdError.getDomain());

                            String errorReason = getRewardedErrorReason(loadAdError.getCode());
                            Log.e(TAG, "에러 원인: " + errorReason);

                            mRewardedAd = null;
                            if (listener != null) {
                                listener.onAdLoadFailed(errorReason);
                            }
                        }
                    });

        } catch (Exception e) {
            isLoadingRewarded = false;
            Log.e(TAG, "리워드 광고 로드 중 예외 발생: " + e.getMessage(), e);
            if (listener != null) {
                listener.onAdLoadFailed("리워드 광고 로드 중 예외 발생: " + e.getMessage());
            }
        }
    }

    /// 리워드 광고 표시
    public void showRewardedAd(Context activityContext, OnRewardedAdShownListener listener) {
        Log.d(TAG, "showRewardedAd 호출됨 - mRewardedAd: " + (mRewardedAd != null ? "not null" : "null"));

        if (mRewardedAd != null) {
            Log.d(TAG, "리워드 광고 표시 시작");

            try {
                // 리워드 광고 콜백 설정
                mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "리워드 광고 클릭됨");
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "리워드 광고 닫힘 (AdMob 콜백)");
                        mRewardedAd = null;

                        if (listener != null) {
                            listener.onAdClosed();
                        }

                        // 다음 광고를 위해 미리 로드
                        Log.d(TAG, "다음 리워드 광고 로드 시작");
                        loadRewardedAd(null);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        Log.e(TAG, "리워드 광고 표시 실패 (AdMob 콜백): " + adError.getMessage());
                        mRewardedAd = null;

                        if (listener != null) {
                            listener.onAdShowFailed(adError.getMessage());
                        }

                        // 실패 시에도 다음 광고를 위해 미리 로드
                        Log.d(TAG, "실패 후 리워드 광고 로드 시작");
                        loadRewardedAd(null);
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "리워드 광고 표시됨 (AdMob 콜백)");

                        if (listener != null) {
                            listener.onAdShown();
                        }
                    }
                });

                // 리워드 광고 표시 및 보상 처리
                mRewardedAd.show((android.app.Activity) activityContext, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        // 사용자가 리워드를 획득했을 때
                        Log.d(TAG, "리워드 획득 (AdMob 콜백): " + rewardItem.getAmount() + " " + rewardItem.getType());

                        if (listener != null) {
                            listener.onUserEarnedReward(rewardItem.getAmount(), rewardItem.getType());
                        }
                    }
                });

                Log.d(TAG, "리워드 광고 show() 메서드 호출 완료");

            } catch (Exception e) {
                Log.e(TAG, "리워드 광고 표시 중 예외 발생: " + e.getMessage(), e);
                mRewardedAd = null;
                if (listener != null) {
                    listener.onAdShowFailed("예외 발생: " + e.getMessage());
                }
            }

        } else {
            Log.w(TAG, "리워드 광고가 준비되지 않음 - 새로운 광고 로드 시도");

            // 광고가 없으면 즉시 로드 시도
            loadRewardedAd(new OnRewardedAdLoadedListener() {
                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "새 리워드 광고 로드 성공 - 다시 표시 시도");
                    // 로드 성공 시 다시 표시 시도
                    showRewardedAd(activityContext, listener);
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(TAG, "새 리워드 광고 로드 실패: " + error);
                    if (listener != null) {
                        listener.onAdNotReady();
                    }
                }
            });
        }
    }

    /// 리워드 광고 준비 상태 확인
    public boolean isRewardedAdReady() {
        boolean isReady = mRewardedAd != null;
        Log.d(TAG, "리워드 광고 준비 상태 확인: " + (isReady ? "준비됨" : "준비 안됨") +
                " (mRewardedAd: " + (mRewardedAd != null ? "not null" : "null") + ")");
        return isReady;
    }

    /// 리워드 광고 에러 원인 반환
    private String getRewardedErrorReason(int errorCode) {
        switch (errorCode) {
            case 0: // ERROR_CODE_INTERNAL_ERROR
                return "내부 오류 - AdMob 서버 문제 또는 잘못된 설정";
            case 1: // ERROR_CODE_INVALID_REQUEST
                return "잘못된 요청 - 광고 단위 ID 확인 필요 또는 앱 ID 누락";
            case 2: // ERROR_CODE_NETWORK_ERROR
                return "네트워크 오류 - 인터넷 연결 확인";
            case 3: // ERROR_CODE_NO_FILL
                return "리워드 광고 없음 - 현재 사용 가능한 리워드 광고가 없음";
            case 8: // ERROR_CODE_APP_ID_MISSING
                return "앱 ID 누락 - AndroidManifest.xml에 APPLICATION_ID 설정 필요";
            default:
                return "알 수 없는 오류 (코드: " + errorCode + ")";
        }
    }

    /// 전면광고 로드
    public void loadInterstitialAd(OnInterstitialAdLoadedListener listener) {
        if (isLoadingInterstitial) {
            Log.d(TAG, "전면광고가 이미 로딩 중입니다");
            return;
        }

        Log.d(TAG, "전면광고 로드 시작");
        isLoadingInterstitial = true;

        try {
            String adUnitId = context.getString(R.string.admob_id_full);
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(context, adUnitId, adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                            isLoadingInterstitial = false;
                            Log.d(TAG, "전면광고 로드 성공");

                            // 전면광고 콜백 설정
                            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "전면광고 닫힘");
                                    mInterstitialAd = null;
                                    if (listener != null) {
                                        listener.onAdClosed();
                                    }
                                    // 다음 광고를 위해 미리 로드
                                    loadInterstitialAd(null);
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                                    Log.e(TAG, "전면광고 표시 실패: " + adError.getMessage());
                                    mInterstitialAd = null;
                                    if (listener != null) {
                                        listener.onAdShowFailed(adError.getMessage());
                                    }
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    Log.d(TAG, "전면광고 표시됨");
                                    if (listener != null) {
                                        listener.onAdShown();
                                    }
                                }
                            });

                            if (listener != null) {
                                listener.onAdLoaded();
                            }
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            isLoadingInterstitial = false;
                            Log.e(TAG, "전면광고 로드 실패:");
                            Log.e(TAG, "Error Code: " + loadAdError.getCode());
                            Log.e(TAG, "Error Message: " + loadAdError.getMessage());
                            Log.e(TAG, "Error Domain: " + loadAdError.getDomain());

                            String errorReason = getInterstitialErrorReason(loadAdError.getCode());
                            Log.e(TAG, "에러 원인: " + errorReason);

                            mInterstitialAd = null;
                            if (listener != null) {
                                listener.onAdLoadFailed(errorReason);
                            }
                        }
                    });

        } catch (Exception e) {
            isLoadingInterstitial = false;
            Log.e(TAG, "전면광고 로드 중 예외 발생: " + e.getMessage(), e);
            if (listener != null) {
                listener.onAdLoadFailed("전면광고 로드 중 예외 발생: " + e.getMessage());
            }
        }
    }

    /// 전면광고 표시
    public void showInterstitialAd(Context activityContext, OnInterstitialAdShownListener listener) {
        if (mInterstitialAd != null) {
            Log.d(TAG, "전면광고 표시");

            // 표시 전 콜백 설정 업데이트
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "전면광고 닫힘");
                    mInterstitialAd = null;
                    if (listener != null) {
                        listener.onAdClosed();
                    }
                    // 다음 광고를 위해 미리 로드
                    loadInterstitialAd(null);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "전면광고 표시 실패: " + adError.getMessage());
                    mInterstitialAd = null;
                    if (listener != null) {
                        listener.onAdShowFailed(adError.getMessage());
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "전면광고 표시됨");
                    if (listener != null) {
                        listener.onAdShown();
                    }
                }
            });

            mInterstitialAd.show((android.app.Activity) activityContext);
        } else {
            Log.w(TAG, "전면광고가 준비되지 않음");
            if (listener != null) {
                listener.onAdNotReady();
            }
        }
    }

    /// 전면광고 준비 상태 확인
    public boolean isInterstitialAdReady() {
        return mInterstitialAd != null;
    }

    /// 전면광고 에러 원인 반환
    private String getInterstitialErrorReason(int errorCode) {
        switch (errorCode) {
            case 0: // ERROR_CODE_INTERNAL_ERROR
                return "내부 오류 - AdMob 서버 문제 또는 잘못된 설정";
            case 1: // ERROR_CODE_INVALID_REQUEST
                return "잘못된 요청 - 광고 단위 ID 확인 필요 또는 앱 ID 누락";
            case 2: // ERROR_CODE_NETWORK_ERROR
                return "네트워크 오류 - 인터넷 연결 확인";
            case 3: // ERROR_CODE_NO_FILL
                return "광고 없음 - 현재 사용 가능한 전면광고가 없음";
            case 8: // ERROR_CODE_APP_ID_MISSING
                return "앱 ID 누락 - AndroidManifest.xml에 APPLICATION_ID 설정 필요";
            default:
                return "알 수 없는 오류 (코드: " + errorCode + ")";
        }
    }

    public void destroyBannerAd() {
        if (mBannerAdView != null) {
            mBannerAdView.destroy();
            mBannerAdView = null;
            Log.d(TAG, "배너 광고 해제됨");
        }
    }

    /// 네이티브 광고 로드
    public void loadNativeAd(OnNativeAdLoadedListener listener) {
        Log.d(TAG, "네이티브 광고 로드 시작");

        try {
            String adUnitId = context.getString(R.string.admob_id_native);
            Log.d(TAG, "광고 단위 ID: " + adUnitId);

            AdRequest adRequest = new AdRequest.Builder().build();

            AdLoader adLoader = new AdLoader.Builder(context, adUnitId)
                    .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                        @Override
                        public void onNativeAdLoaded(NativeAd nativeAd) {
                            Log.d(TAG, "네이티브 광고 로드 성공!");
                            Log.d(TAG, "광고 제목: " + (nativeAd.getHeadline() != null ? nativeAd.getHeadline() : "없음"));
                            Log.d(TAG, "광고주: " + (nativeAd.getAdvertiser() != null ? nativeAd.getAdvertiser() : "없음"));
                            Log.d(TAG, "미디어 콘텐츠: " + (nativeAd.getMediaContent() != null ? "있음" : "없음"));

                            if (listener != null) {
                                listener.onAdLoaded(nativeAd);
                            }
                        }
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            Log.d(TAG, "광고 로드 완료 콜백");
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError adError) {
                            Log.e(TAG, "광고 로드 실패:");
                            Log.e(TAG, "Error Code: " + adError.getCode());
                            Log.e(TAG, "Error Message: " + adError.getMessage());
                            Log.e(TAG, "Error Domain: " + adError.getDomain());

                            String errorReason = getErrorReason(adError.getCode());
                            Log.e(TAG, "에러 원인: " + errorReason);

                            if (listener != null) {
                                listener.onAdLoadFailed(errorReason);
                            }
                        }

                        @Override
                        public void onAdOpened() {
                            Log.d(TAG, "광고 열림");
                        }

                        @Override
                        public void onAdClosed() {
                            Log.d(TAG, "광고 닫힘");
                        }
                    })
                    .withNativeAdOptions(new NativeAdOptions.Builder()
                            .setRequestMultipleImages(false)
                            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                            .setVideoOptions(new com.google.android.gms.ads.VideoOptions.Builder()
                                    .setStartMuted(true)
                                    .build())
                            .build())
                    .build();

            adLoader.loadAd(adRequest);

        } catch (Exception e) {
            Log.e(TAG, "광고 로드 중 예외 발생: " + e.getMessage(), e);
            if (listener != null) {
                listener.onAdLoadFailed("광고 로드 중 예외 발생: " + e.getMessage());
            }
        }
    }

    private String getErrorReason(int errorCode) {
        switch (errorCode) {
            case 0: // ERROR_CODE_INTERNAL_ERROR
                return "내부 오류 - AdMob 서버 문제 또는 잘못된 설정";
            case 1: // ERROR_CODE_INVALID_REQUEST
                return "잘못된 요청 - 광고 단위 ID 확인 필요 또는 앱 ID 누락";
            case 2: // ERROR_CODE_NETWORK_ERROR
                return "네트워크 오류 - 인터넷 연결 확인";
            case 3: // ERROR_CODE_NO_FILL
                return "광고 없음 - 현재 사용 가능한 광고가 없음";
            case 8: // ERROR_CODE_APP_ID_MISSING
                return "앱 ID 누락 - AndroidManifest.xml에 APPLICATION_ID 설정 필요";
            default:
                return "알 수 없는 오류 (코드: " + errorCode + ")";
        }
    }

    /// 배너 광고 로드 리스너
    public interface OnBannerAdLoadedListener {
        void onAdLoaded(AdView adView);
        void onAdLoadFailed(String error);
    }

    /// 네이티브 광고 로드 리스너
    public interface OnNativeAdLoadedListener {
        void onAdLoaded(NativeAd nativeAd);
        void onAdLoadFailed(String error);
    }

    /// 전면광고 로드 리스너
    public interface OnInterstitialAdLoadedListener {
        void onAdLoaded();
        void onAdLoadFailed(String error);
        void onAdClosed();
        void onAdShown();
        void onAdShowFailed(String error);
    }

    /// 전면광고 표시 리스너
    public interface OnInterstitialAdShownListener {
        void onAdShown();
        void onAdClosed();
        void onAdShowFailed(String error);
        void onAdNotReady();
    }

    /// 리워드 광고 로드 리스너
    public interface OnRewardedAdLoadedListener {
        void onAdLoaded();
        void onAdLoadFailed(String error);
    }

    /// 리워드 광고 표시 리스너
    public interface OnRewardedAdShownListener {
        void onAdShown();
        void onAdClosed();
        void onAdShowFailed(String error);
        void onAdNotReady();
        void onUserEarnedReward(int amount, String type);
    }
}