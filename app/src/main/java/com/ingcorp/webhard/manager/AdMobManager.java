package com.ingcorp.webhard.manager;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.ingcorp.webhard.R;

public class AdMobManager {
    private static final String TAG = "AdMobManager";
    private static AdMobManager instance;
    private static boolean isInitialized = false;
    private static boolean isInitializing = false;

    private Context context;

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
}