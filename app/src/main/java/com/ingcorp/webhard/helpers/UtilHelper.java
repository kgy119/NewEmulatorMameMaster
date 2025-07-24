package com.ingcorp.webhard.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.ingcorp.webhard.MAME4droid;
import com.ingcorp.webhard.R;
import com.ingcorp.webhard.base.Constants;

import java.io.File;
import java.io.FileNotFoundException;

public class UtilHelper {

    private static final String TAG = Constants.LOG_TAG;
    private static final String PREF_NAME = "WebHardPrefs";

    // 광고 설정 관련 키들
    private static final String AD_BANNER_USE_KEY = "ad_banner_use";
    private static final String AD_FULL_CNT_KEY = "ad_full_cnt";
    private static final String AD_REWARD_COIN_CNT_KEY = "ad_reward_coin_cnt";
    private static final String GAME_CLICK_COUNT_KEY = "game_click_count";
    private static final String AD_NATIVE_CNT_KEY = "ad_native_cnt";
    private static final String KEY_GAME_LIST_VERSION = "game_list_version";
    private static final String BTN_COIN_CLICK_COUNT_KEY = "btn_coin_click_count";
    private static final String AD_IN_PROGRESS_KEY = "ad_in_progress";


    private Context context;
    private static UtilHelper instance;
    private static final String TEMP_FILE_EXTENSION = ".tmp";
    private static final String BACKUP_FILE_EXTENSION = ".backup";

    protected MAME4droid mm = null;

    public UtilHelper(MAME4droid value) {
        mm = value;
    }



    // 싱글톤 패턴 적용
    private UtilHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized UtilHelper getInstance(Context context) {
        if (instance == null) {
            instance = new UtilHelper(context);
        }
        return instance;
    }


    // 기기 정보 로깅 (static 메서드 - Context 매개변수 포함)
    public static void logDeviceInfo(Context context) {
        Log.d(TAG, "=== 기기 정보 ===");
        Log.d(TAG, "모델: " + Build.MODEL);
        Log.d(TAG, "제조사: " + Build.MANUFACTURER);
        Log.d(TAG, "Android 버전: " + Build.VERSION.RELEASE);
        Log.d(TAG, "API 레벨: " + Build.VERSION.SDK_INT);

        if (context != null) {
            DisplayMetrics metrics = getDisplayMetrics(context);
            Log.d(TAG, "화면 밀도: " + metrics.density);
            Log.d(TAG, "화면 크기: " + metrics.widthPixels + "x" + metrics.heightPixels);
        }
        Log.d(TAG, "접는 배너 지원: " + (isCollapsibleBannerSupported() ? "지원" : "미지원"));
        Log.d(TAG, "==================");
    }

    // 디스플레이 메트릭스 가져오기 (static 버전)
    private static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    // 디스플레이 메트릭스 가져오기 (인스턴스 버전)
    private DisplayMetrics getDisplayMetrics() {
        return getDisplayMetrics(context);
    }

    // 접는 배너 지원 여부 확인
    private static boolean isCollapsibleBannerSupported() {
        // Android 7.0 (API 24) 이상에서 지원한다고 가정
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    // 네트워크 연결 상태 확인 (최신 API 사용)
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.w(TAG, "ConnectivityManager가 null입니다");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 (API 23) 이상에서는 최신 API 사용
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) {
                Log.w(TAG, "활성 네트워크가 null입니다");
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                Log.w(TAG, "NetworkCapabilities가 null입니다");
                return false;
            }

            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            Log.d(TAG, "네트워크 상태 - 인터넷: " + hasInternet + ", 검증됨: " + isValidated);

            return hasInternet && isValidated;
        } else {
            // Android 6.0 미만에서는 기존 API 사용
            @SuppressWarnings("deprecation")
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null) {
                Log.w(TAG, "활성 네트워크 정보가 null입니다");
                return false;
            }

            boolean isConnected = activeNetwork.isConnectedOrConnecting();
            Log.d(TAG, "네트워크 연결 상태 (레거시): " + isConnected);

            return isConnected;
        }
    }

    // WiFi 연결 상태 확인 (최신 API 사용)
    public boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 (API 23) 이상에서는 최신 API 사용
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            // Android 6.0 미만에서는 기존 API 사용
            @SuppressWarnings("deprecation")
            NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetwork != null && wifiNetwork.isConnected();
        }
    }

    // 모바일 데이터 연결 상태 확인 (최신 API 사용)
    public boolean isMobileConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 (API 23) 이상에서는 최신 API 사용
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            // Android 6.0 미만에서는 기존 API 사용
            @SuppressWarnings("deprecation")
            NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobileNetwork != null && mobileNetwork.isConnected();
        }
    }

    // 이더넷 연결 상태 확인 (추가 기능)
    public boolean isEthernetConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false; // 이더넷 감지는 API 23 이상에서만 지원
    }

    public String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Unknown";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return "No Connection";

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) return "Unknown";

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Mobile Data";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return "VPN";
            }
            return "Other";
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null) return "No Connection";

            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return "WiFi";
                case ConnectivityManager.TYPE_MOBILE:
                    return "Mobile Data";
                case ConnectivityManager.TYPE_ETHERNET:
                    return "Ethernet";
                default:
                    return "Other";
            }
        }
    }

    // 네트워크 상태 정보 로깅 (디버그용)
    public void logNetworkInfo() {
        Log.d(TAG, "=== 네트워크 정보 ===");
        Log.d(TAG, "연결 상태: " + (isNetworkConnected() ? "연결됨" : "연결 안됨"));
        Log.d(TAG, "WiFi 연결: " + (isWifiConnected() ? "연결됨" : "연결 안됨"));
        Log.d(TAG, "모바일 데이터: " + (isMobileConnected() ? "연결됨" : "연결 안됨"));
        Log.d(TAG, "이더넷: " + (isEthernetConnected() ? "연결됨" : "연결 안됨"));
        Log.d(TAG, "네트워크 타입: " + getNetworkType());
        Log.d(TAG, "===================");
    }

    // 네트워크 연결 상태를 확인하고 없으면 에러 다이얼로그 표시
    public boolean checkNetworkConnectionWithDialog(android.app.Activity activity) {
        if (!isNetworkConnected()) {
            Log.w(TAG, "No internet connection detected");
            showNetworkErrorDialog(activity);
            return false;
        }

        Log.d(TAG, "Internet connection verified");
        return true;
    }

    // 네트워크 에러 다이얼로그 표시 (Activity 종료 포함) - 테마 적용
    public void showNetworkErrorDialog(android.app.Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        new android.app.AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle("No Internet Connection")
                .setMessage("This app requires an internet connection to function properly. Please check your network settings and try again.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d(TAG, "Network error dialog dismissed - closing app");
                    activity.finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    // 게임 클릭용 네트워크 에러 다이얼로그 (앱 종료하지 않음) - 테마 적용
    public void showGameNetworkErrorDialog(android.app.Activity activity) {
        if (activity == null) {
            Log.e(TAG, "Activity가 null이므로 다이얼로그를 표시할 수 없습니다");
            return;
        }

        if (activity.isFinishing()) {
            Log.e(TAG, "Activity가 종료 중이므로 다이얼로그를 표시할 수 없습니다");
            return;
        }

        Log.d(TAG, "게임 네트워크 에러 다이얼로그 표시 시작");

        try {
            new android.app.AlertDialog.Builder(activity, R.style.DialogTheme)
                    .setTitle("No Internet Connection")
                    .setMessage("Internet connection is required to play games. Please check your network settings and try again.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        Log.d(TAG, "Game network error dialog dismissed - continuing app");
                        // 다이얼로그만 닫고 앱은 계속 실행
                    })
                    .setCancelable(false)
                    .show();

            Log.d(TAG, "게임 네트워크 에러 다이얼로그 표시 완료");

        } catch (Exception e) {
            Log.e(TAG, "게임 네트워크 에러 다이얼로그 표시 중 예외 발생: " + e.getMessage(), e);
        }
    }

    public void showConfirmDialog(android.app.Activity activity, String title, String message,
                                  Runnable onConfirmCallback) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        new android.app.AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (onConfirmCallback != null) {
                        onConfirmCallback.run();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void showCustomDialog(android.app.Activity activity, String title, String message,
                                 String positiveButtonText, String negativeButtonText,
                                 Runnable onPositiveCallback, Runnable onNegativeCallback) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle(title)
                .setMessage(message);

        if (positiveButtonText != null) {
            builder.setPositiveButton(positiveButtonText, (dialog, which) -> {
                if (onPositiveCallback != null) {
                    onPositiveCallback.run();
                }
            });
        }

        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText, (dialog, which) -> {
                if (onNegativeCallback != null) {
                    onNegativeCallback.run();
                }
            });
        }

        builder.show();
    }


    // 게임 리스트 버전 저장
    public void saveGameListVersion(int version) {
        saveIntPreference(KEY_GAME_LIST_VERSION, version);
    }

    // 저장된 게임 리스트 버전 가져오기
    public int getSavedGameListVersion() {
        return getIntPreference(KEY_GAME_LIST_VERSION, 0); // 기본값 0
    }

    // 광고 설정 저장
    public void saveAdSettings(boolean adBannerUse, int adFullCnt, int adFullCoinCnt, int adNativeCnt) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(AD_BANNER_USE_KEY, adBannerUse);
        editor.putInt(AD_FULL_CNT_KEY, adFullCnt);
        editor.putInt(AD_REWARD_COIN_CNT_KEY, adFullCoinCnt);
        editor.putInt(AD_NATIVE_CNT_KEY, adNativeCnt);
        editor.apply();

        Log.d(TAG, "광고 설정 저장됨 - 배너: " + adBannerUse + ", 전면: " + adFullCnt +
                ", 코인: " + adFullCoinCnt + ", 네이티브: " + adNativeCnt);
    }

    public int getAdNativeCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(AD_NATIVE_CNT_KEY, 10); // 기본값: 10
    }


    // 배너 광고 사용 여부 확인
    public boolean isAdBannerEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(AD_BANNER_USE_KEY, true); // 기본값: true
    }

    // 전면 광고 주기 가져오기
    public int getAdFullCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(AD_FULL_CNT_KEY, 1); // 기본값: 1 (매번 표시)
    }

    // 보상형 광고 코인 개수 가져오기
    public int getAdFullCoinCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(AD_REWARD_COIN_CNT_KEY, 5); // 기본값: 5
    }

    // 게임 클릭 수 증가 및 전면 광고 표시 여부 확인
    public boolean shouldShowInterstitialAd() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 현재 클릭 수 가져오기
        int currentClickCount = prefs.getInt(GAME_CLICK_COUNT_KEY, 0);

        // 클릭 수 증가
        currentClickCount++;

        // 증가된 클릭 수 저장
        prefs.edit().putInt(GAME_CLICK_COUNT_KEY, currentClickCount).apply();

        // 전면 광고 주기 가져오기
        int adFullCnt = getAdFullCount();

        Log.d(TAG, "게임 클릭 수: " + currentClickCount + ", 광고 주기: " + adFullCnt);

        // 주기로 나누어서 나머지가 0인지 확인
        boolean shouldShow = (currentClickCount % adFullCnt) == 0;

        Log.d(TAG, "전면 광고 표시 여부: " + shouldShow);

        return shouldShow;
    }

    // 게임 클릭 수 가져오기
    public int getGameClickCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(GAME_CLICK_COUNT_KEY, 0);
    }

    // 게임 클릭 수 초기화 (디버그용)
    public void resetGameClickCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(GAME_CLICK_COUNT_KEY, 0).apply();
        Log.d(TAG, "게임 클릭 수 초기화됨");
    }

    // 광고 설정 정보 로깅 (디버그용)
    public void logAdSettings() {
        Log.d(TAG, "=== 광고 설정 정보 ===");
        Log.d(TAG, "배너 광고 사용: " + (isAdBannerEnabled() ? "예" : "아니오"));
        Log.d(TAG, "전면 광고 주기: " + getAdFullCount());
        Log.d(TAG, "보상형 광고 코인: " + getAdFullCoinCount());
        Log.d(TAG, "네이티브 광고 주기: " + getAdNativeCount());
        Log.d(TAG, "현재 게임 클릭 수: " + getGameClickCount());
        Log.d(TAG, "==================");
    }
    public void saveStringPreference(String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public String getStringPreference(String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    public void saveIntPreference(String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

    public int getIntPreference(String key, int defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(key, defaultValue);
    }

    // 문자열 유틸리티 메서드들
    public boolean isStringEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public boolean isStringNotEmpty(String str) {
        return !isStringEmpty(str);
    }

    public float getScreenDensity() {
        return getDisplayMetrics().density;
    }

    // px를 dp로 변환
    public int pxToDp(int px) {
        return Math.round(px / getScreenDensity());
    }

    // 바이트를 읽기 쉬운 형태로 변환
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
            * 앱 시작시 모든 임시 파일들을 정리하는 메서드
    */
    public void cleanupAllTemporaryFiles(String romsPath) {
        try {
//            Log.d(TAG, "임시 파일 정리 시작: " + romsPath);

            File romsDir = new File(romsPath);
            if (!romsDir.exists() || !romsDir.isDirectory()) {
//                Log.w(TAG, "ROMs 디렉토리가 존재하지 않음: " + romsPath);
                return;
            }

            // .tmp 파일들 정리
            cleanupFilesByExtension(romsDir, TEMP_FILE_EXTENSION, "임시");

            // .backup 파일들 정리 (오래된 백업 파일들)
            cleanupFilesByExtension(romsDir, BACKUP_FILE_EXTENSION, "백업");

//            Log.d(TAG, "임시 파일 정리 완료");

        } catch (Exception e) {
            Log.e(TAG, "임시 파일 정리 중 오류 발생", e);
        }
    }

    /**
     * 특정 확장자의 파일들을 정리하는 헬퍼 메서드
     */
    private void cleanupFilesByExtension(File directory, String extension, String fileType) {
        try {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(extension));
            if (files != null && files.length > 0) {
                Log.d(TAG, fileType + " 파일 " + files.length + "개 발견");

                int deletedCount = 0;
                for (File file : files) {
                    if (file.delete()) {
                        deletedCount++;
                        Log.d(TAG, fileType + " 파일 삭제됨: " + file.getName());
                    } else {
                        Log.w(TAG, fileType + " 파일 삭제 실패: " + file.getName());
                    }
                }

                Log.d(TAG, fileType + " 파일 정리 완료: " + deletedCount + "/" + files.length);
            } else {
                Log.d(TAG, fileType + " 파일 없음");
            }
        } catch (Exception e) {
            Log.e(TAG, fileType + " 파일 정리 중 오류", e);
        }
    }

    /**
     * 다운로드 디스크 공간이 충분한지 확인하는 메서드
     */
    public boolean hasEnoughDiskSpace(String romsPath, long requiredBytes) {
        try {
            File romsDir = new File(romsPath);

            // 디렉토리가 없다면 생성 시도
            if (!romsDir.exists()) {
                if (!romsDir.mkdirs()) {
                    Log.e(TAG, "ROMs 디렉토리 생성 실패: " + romsPath);
                    return false;
                }
            }

            long availableBytes = romsDir.getUsableSpace();
            long requiredWithBuffer = requiredBytes * 2; // 버퍼로 2배 크기 요구

            Log.d(TAG, "디스크 공간 확인 - 필요: " + formatBytes(requiredWithBuffer) +
                    ", 사용가능: " + formatBytes(availableBytes));

            if (availableBytes < requiredWithBuffer) {
                Log.w(TAG, "디스크 공간 부족");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "디스크 공간 확인 중 오류", e);
            return false;
        }
    }

    /**
     * 다운로드 상태를 추적하는 메서드
     */
    public void saveDownloadState(String romFileName, String state) {
        try {
            String key = "download_state_" + romFileName;
            saveStringPreference(key, state + "_" + System.currentTimeMillis());
            Log.d(TAG, "다운로드 상태 저장: " + romFileName + " -> " + state);
        } catch (Exception e) {
            Log.e(TAG, "다운로드 상태 저장 중 오류", e);
        }
    }

    /**
     * 다운로드 상태를 가져오는 메서드
     */
    public String getDownloadState(String romFileName) {
        try {
            String key = "download_state_" + romFileName;
            String value = getStringPreference(key, "none");

            if (value.contains("_")) {
                return value.split("_")[0];
            }
            return value;
        } catch (Exception e) {
            Log.e(TAG, "다운로드 상태 확인 중 오류", e);
            return "none";
        }
    }

    /**
     * 다운로드 관련 오류 다이얼로그를 표시하는 메서드
     */
    public void showDownloadErrorDialog(android.app.Activity activity, String gameName, String error,
                                        Runnable onRetryCallback) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        String message = "Failed to download " + gameName + "\n\nError: " + error +
                "\n\nWould you like to try again?";

        new android.app.AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle("Download Failed")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (onRetryCallback != null) {
                        onRetryCallback.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * ROMs 디렉토리의 상태를 로깅하는 디버그 메서드
     */
    public void logRomsDirectoryStatus(String romsPath) {
        try {
//            Log.d(TAG, "=== ROMs 디렉토리 상태 ===");
//            Log.d(TAG, "경로: " + romsPath);

            File romsDir = new File(romsPath);
            if (!romsDir.exists()) {
//                Log.d(TAG, "디렉토리 존재하지 않음");
                return;
            }

//            Log.d(TAG, "디렉토리 존재: " + romsDir.isDirectory());
//            Log.d(TAG, "읽기 권한: " + romsDir.canRead());
//            Log.d(TAG, "쓰기 권한: " + romsDir.canWrite());
//            Log.d(TAG, "사용 가능 공간: " + formatBytes(romsDir.getUsableSpace()));

            File[] files = romsDir.listFiles();
            if (files != null) {
//                Log.d(TAG, "총 파일 수: " + files.length);

                int romCount = 0;
                int tempCount = 0;
                int backupCount = 0;

                for (File file : files) {
                    if (file.getName().endsWith(TEMP_FILE_EXTENSION)) {
                        tempCount++;
                    } else if (file.getName().endsWith(BACKUP_FILE_EXTENSION)) {
                        backupCount++;
                    } else if (file.getName().endsWith(".zip")) {
                        romCount++;
                    }
                }

//                Log.d(TAG, "ROM 파일: " + romCount);
//                Log.d(TAG, "임시 파일: " + tempCount);
//                Log.d(TAG, "백업 파일: " + backupCount);
            } else {
                Log.d(TAG, "파일 목록을 가져올 수 없음");
            }

//            Log.d(TAG, "========================");

        } catch (Exception e) {
            Log.e(TAG, "ROMs 디렉토리 상태 확인 중 오류", e);
        }
    }

    /**
     * BTN_COIN 클릭시 다음순서 광고인지 이면 광고이미지로 변경
     */
    public boolean shouldShowRewardAdImage() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 현재 BTN_COIN 클릭 수 가져오기
        int currentClickCount = prefs.getInt(BTN_COIN_CLICK_COUNT_KEY, 0);

        // 리워드 광고 주기 가져오기
        int adRewardCoinCnt = getAdFullCoinCount();

        Log.e(Constants.LOG_TAG, "코인클릭수 : "+prefs.getInt(BTN_COIN_CLICK_COUNT_KEY, 0)+"  리워드 광고 주기 : " + adRewardCoinCnt);

        // 주기로 나누어서 나머지가 0인지 확인 (광고 노출 순서인지)
        return ((currentClickCount + 2) % adRewardCoinCnt) == 0;
    }

    /**
     * BTN_COIN 클릭 수 증가 및 리워드 광고 노출 순서인지 체크
     */
    // 기존 shouldShowRewardAd() 메서드를 수정
    public boolean shouldShowRewardAd() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 현재 광고 진행 중인지 확인
        if (prefs.getBoolean(AD_IN_PROGRESS_KEY, false)) {
            return false; // 이미 광고 진행 중이면 false 반환
        }

        // 현재 BTN_COIN 클릭 수 가져오기
        int currentClickCount = prefs.getInt(BTN_COIN_CLICK_COUNT_KEY, 0);

        // 리워드 광고 주기 가져오기
        int adRewardCoinCnt = getAdFullCoinCount();

        Log.e(Constants.LOG_TAG, "코인클릭수 : "+prefs.getInt(BTN_COIN_CLICK_COUNT_KEY, 0)+"  리워드 광고 주기 : " + adRewardCoinCnt);

        // 주기로 나누어서 나머지가 0인지 확인 (광고 노출 순서인지)
        return ((currentClickCount + 1) % adRewardCoinCnt) == 0;
    }

    // 광고 진행 상태 설정 메서드 추가
    public void setAdInProgress(boolean inProgress) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(AD_IN_PROGRESS_KEY, inProgress).apply();
    }

    // 광고 시청 완료 후 클릭 수 증가 메서드 추가
    public void completeRewardAd() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 클릭 수 증가
        int currentClickCount = prefs.getInt(BTN_COIN_CLICK_COUNT_KEY, 0);
        prefs.edit().putInt(BTN_COIN_CLICK_COUNT_KEY, currentClickCount + 1).apply();

        // 광고 진행 상태 해제
        setAdInProgress(false);
    }

    // 광고 실패/취소 시 호출할 메서드 추가
    public void cancelRewardAd() {
        // 클릭 수는 증가시키지 않고 진행 상태만 해제
        setAdInProgress(false);
    }

    public void cleanupPreviousRomFiles(String romsPath) {
        try {
            Log.d(TAG, "ROMs 디렉토리 삭제시작 : " + romsPath);
            String roms_dir = mm.getMainHelper().getInstallationDIR();

            File fm = new File(roms_dir + File.separator + "saves/"
                    + "dont-delete-" + mm.getMainHelper().getVersion() + ".bin");
            if (fm.exists())
                return;

            // ✅ 앱 업데이트 기존 ROM폴더 삭제
            File romsDir = new File(romsPath);
            if (romsDir.exists()) {
                Log.d(TAG, "ROMs 디렉토리 삭제시도: " + romsPath);
                deleteRecursive(romsDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists())
            throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }

            Log.d(TAG, "ROMs 디렉토리 삭제: " + ret);
        }
        return ret && path.delete();
    }

}