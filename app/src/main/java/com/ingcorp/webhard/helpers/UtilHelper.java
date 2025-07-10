package com.ingcorp.webhard.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import com.ingcorp.webhard.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UtilHelper {

    private static final String TAG = "UtilHelper";
    private static final String PREF_NAME = "WebHardPrefs";

    private Context context;
    private static UtilHelper instance;

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

    // 네트워크 연결 상태 확인
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // WiFi 연결 상태 확인
    public boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetwork != null && wifiNetwork.isConnected();
    }

    // 모바일 데이터 연결 상태 확인
    public boolean isMobileConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobileNetwork != null && mobileNetwork.isConnected();
    }

    // SharedPreferences 관련 메서드들
    public void saveStringPreference(String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public String getStringPreference(String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    public void saveBooleanPreference(String key, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBooleanPreference(String key, boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }

    public void saveIntPreference(String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

    public int getIntPreference(String key, int defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(key, defaultValue);
    }

    // 토스트 메시지 표시
    public void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void showLongToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // 현재 날짜/시간 포맷팅
    public String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // 날짜 포맷팅
    public String formatDate(Date date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(date);
    }

    // 문자열 유틸리티 메서드들
    public boolean isStringEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public boolean isStringNotEmpty(String str) {
        return !isStringEmpty(str);
    }

    public String trimString(String str) {
        return str != null ? str.trim() : "";
    }

    // 화면 크기 관련 메서드들
    public int getScreenWidth() {
        return getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return getDisplayMetrics().heightPixels;
    }

    public float getScreenDensity() {
        return getDisplayMetrics().density;
    }

    // dp를 px로 변환
    public int dpToPx(int dp) {
        return Math.round(dp * getScreenDensity());
    }

    // px를 dp로 변환
    public int pxToDp(int px) {
        return Math.round(px / getScreenDensity());
    }

    // 앱 버전 정보
    public String getAppVersionName() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "앱 버전 정보를 가져올 수 없습니다.", e);
            return "Unknown";
        }
    }

    public int getAppVersionCode() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            Log.e(TAG, "앱 버전 코드를 가져올 수 없습니다.", e);
            return -1;
        }
    }

    // 디버그 모드 확인
    public boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }

    // 로그 메서드들
    public void logDebug(String message) {
        if (isDebugMode()) {
            Log.d(TAG, message);
        }
    }

    public void logInfo(String message) {
        Log.i(TAG, message);
    }

    public void logWarning(String message) {
        Log.w(TAG, message);
    }

    public void logError(String message) {
        Log.e(TAG, message);
    }

    public void logError(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }

    // 메모리 사용량 확인
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Log.d(TAG, "=== 메모리 사용량 ===");
        Log.d(TAG, "총 메모리: " + formatBytes(totalMemory));
        Log.d(TAG, "사용중 메모리: " + formatBytes(usedMemory));
        Log.d(TAG, "여유 메모리: " + formatBytes(freeMemory));
        Log.d(TAG, "최대 메모리: " + formatBytes(maxMemory));
        Log.d(TAG, "===================");
    }

    // 바이트를 읽기 쉬운 형태로 변환
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

}