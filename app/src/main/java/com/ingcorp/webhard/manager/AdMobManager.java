package com.ingcorp.webhard.manager;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.ingcorp.webhard.R;

import androidx.annotation.NonNull;

public class AdMobManager {
    private static final String TAG = "AdMobManager";
    private static AdMobManager instance;
    private static boolean isInitialized = false;
    private static boolean isInitializing = false;

    private Context context;
    private AdView mBannerAdView;

    private AdMobManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AdMobManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdMobManager(context);
        }
        return instance;
    }

    /**
     * AdMob 초기화
     */
    public void initialize(OnAdMobInitializedListener listener) {
        if (isInitialized) {
            Log.d(TAG, "AdMob이 이미 초기화됨");
            if (listener != null) {
                listener.onInitialized(true);
            }
            return;
        }

        if (isInitializing) {
            Log.d(TAG, "AdMob 초기화가 이미 진행 중");
            return;
        }

        isInitializing = true;
        Log.d(TAG, "AdMob 초기화 시작");

        try {
            MobileAds.initialize(context, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    isInitialized = true;
                    isInitializing = false;
                    Log.d(TAG, "AdMob 초기화 성공!");
                    Log.d(TAG, "초기화 상태: " + initializationStatus.toString());

                    if (listener != null) {
                        listener.onInitialized(true);
                    }
                }
            });

            // 타임아웃 처리 (10초)
            new android.os.Handler().postDelayed(() -> {
                if (!isInitialized && isInitializing) {
                    isInitializing = false;
                    Log.e(TAG, "AdMob 초기화 타임아웃");
                    Log.e(TAG, "AndroidManifest.xml 설정을 확인하세요:");
                    Log.e(TAG, "1. INTERNET 권한");
                    Log.e(TAG, "2. APPLICATION_ID 메타데이터");

                    if (listener != null) {
                        listener.onInitialized(false);
                    }
                }
            }, 10000);

        } catch (Exception e) {
            isInitializing = false;
            Log.e(TAG, "AdMob 초기화 중 예외 발생: " + e.getMessage(), e);
            if (listener != null) {
                listener.onInitialized(false);
            }
        }
    }

    /**
     * AdMob 초기화 상태 확인
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 네이티브 광고 로드
     */
    public void loadNativeAd(OnNativeAdLoadedListener listener) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob이 초기화되지 않음. 광고 로드 실패");
            if (listener != null) {
                listener.onAdLoadFailed("AdMob이 초기화되지 않음");
            }
            return;
        }

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

    /**
     * 접는 배너 광고 로드
     */
    public void loadCollapsibleBannerAd(AdView adView, OnBannerAdLoadedListener listener) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob이 초기화되지 않음. 배너 광고 로드 실패");
            if (listener != null) {
                listener.onAdLoadFailed("AdMob이 초기화되지 않음");
            }
            return;
        }

        this.mBannerAdView = adView;

        // AdUnitId가 설정되어 있지 않으면 설정
        if (mBannerAdView.getAdUnitId() == null || mBannerAdView.getAdUnitId().isEmpty()) {
            String bannerAdUnitId = context.getString(R.string.admob_id_banner);
            mBannerAdView.setAdUnitId(bannerAdUnitId);
        }

        Log.d(TAG, "접는 배너 광고 로드 시작 - Ad Unit ID: " + mBannerAdView.getAdUnitId());

        // AdListener 먼저 설정
        mBannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "접는 배너 광고 로드 완료");
                if (listener != null) {
                    listener.onAdLoaded();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                String errorReason = getErrorReason(adError.getCode());
                Log.e(TAG, "접는 배너 광고 로드 실패: " + errorReason);
                if (listener != null) {
                    listener.onAdLoadFailed(errorReason);
                }
            }

            @Override
            public void onAdClicked() {
                Log.d(TAG, "접는 배너 광고 클릭");
                if (listener != null) {
                    listener.onAdClicked();
                }
            }

            @Override
            public void onAdOpened() {
                Log.d(TAG, "접는 배너 광고 열림");
            }

            @Override
            public void onAdClosed() {
                Log.d(TAG, "접는 배너 광고 닫힘");
            }
        });

        // ViewTreeObserver를 사용하여 뷰가 완전히 레이아웃된 후 AdSize 설정 및 광고 로드
        mBannerAdView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 리스너 제거 (한 번만 실행되도록)
                mBannerAdView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                try {
                    // AdSize가 설정되어 있지 않은 경우에만 설정
                    if (mBannerAdView.getAdSize() == null) {
                        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                        float density = displayMetrics.density;
                        float adWidthPixels = mBannerAdView.getWidth();

                        if (adWidthPixels == 0) {
                            adWidthPixels = displayMetrics.widthPixels;
                        }

                        int adWidth = (int) (adWidthPixels / density);
                        AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
                        mBannerAdView.setAdSize(adSize);
                        Log.d(TAG, "AdSize 동적 설정: " + adSize.toString());
                    } else {
                        Log.d(TAG, "AdSize 이미 설정됨: " + mBannerAdView.getAdSize().toString());
                    }

                    // AdRequest 생성 및 광고 로드
                    AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

                    // 접는 배너를 위한 추가 파라미터 설정
                    Bundle extras = new Bundle();
                    extras.putString("collapsible", "bottom"); // 하단 접는 배너
                    adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);

                    AdRequest adRequest = adRequestBuilder.build();

                    // 광고 로드
                    mBannerAdView.loadAd(adRequest);

                } catch (Exception e) {
                    Log.e(TAG, "접는 배너 광고 로드 중 오류: " + e.getMessage());
                    if (listener != null) {
                        listener.onAdLoadFailed(e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 배너 광고 일시정지
     */
    public void pauseBannerAd() {
        if (mBannerAdView != null) {
            mBannerAdView.pause();
            Log.d(TAG, "배너 광고 일시정지");
        }
    }

    /**
     * 배너 광고 재시작
     */
    public void resumeBannerAd() {
        if (mBannerAdView != null) {
            mBannerAdView.resume();
            Log.d(TAG, "배너 광고 재시작");
        }
    }

    /**
     * 배너 광고 제거
     */
    public void destroyBannerAd() {
        if (mBannerAdView != null) {
            mBannerAdView.destroy();
            mBannerAdView = null;
            Log.d(TAG, "배너 광고 제거");
        }
    }

    /**
     * 접는 배너 지원 여부 확인
     */
    public boolean isCollapsibleBannerSupported() {
        // Android 10 (API 29) 이상에서만 접는 배너 지원
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    }

    /**
     * 디버깅을 위한 기기 정보 로그
     */
    public void logDeviceInfo() {
        Log.d(TAG, "=== 기기 정보 ===");
        Log.d(TAG, "모델: " + android.os.Build.MODEL);
        Log.d(TAG, "제조사: " + android.os.Build.MANUFACTURER);
        Log.d(TAG, "Android 버전: " + android.os.Build.VERSION.RELEASE);
        Log.d(TAG, "API 레벨: " + android.os.Build.VERSION.SDK_INT);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Log.d(TAG, "화면 밀도: " + metrics.density);
        Log.d(TAG, "화면 크기: " + metrics.widthPixels + "x" + metrics.heightPixels);
        Log.d(TAG, "접는 배너 지원: " + (isCollapsibleBannerSupported() ? "지원" : "미지원"));
        Log.d(TAG, "==================");
    }

    /**
     * 전체 정리 메서드
     */
    public void cleanup() {
        destroyBannerAd();
        Log.d(TAG, "AdMobManager 정리 완료");
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

    /**
     * AdMob 초기화 완료 리스너
     */
    public interface OnAdMobInitializedListener {
        void onInitialized(boolean success);
    }

    /**
     * 네이티브 광고 로드 리스너
     */
    public interface OnNativeAdLoadedListener {
        void onAdLoaded(NativeAd nativeAd);
        void onAdLoadFailed(String error);
    }

    /**
     * 배너 광고 로드 리스너
     */
    public interface OnBannerAdLoadedListener {
        void onAdLoaded();
        void onAdLoadFailed(String error);
        void onAdClicked();
    }
}