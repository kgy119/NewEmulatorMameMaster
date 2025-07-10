package com.ingcorp.webhard;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.manager.GameListManager;
import com.ingcorp.webhard.model.VersionResponse;
import com.ingcorp.webhard.network.NetworkClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends Activity {

    private static final String TAG = "DEBUGMAME";
    private static final int SPLASH_DURATION = 3000; // 3초

    private boolean isVersionCheckCompleted = false;
    private boolean isSplashTimeCompleted = false;
    private boolean isGameListUpdateCompleted = false;
    private GameListManager gameListManager;
    private UtilHelper utilHelper;

    // 뷰 참조
    private ImageView appIcon;
    private TextView copyrightText;
    private View dot1, dot2, dot3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 레이아웃 설정
        setContentView(R.layout.activity_splash);

        // 유틸리티 헬퍼 초기화
        utilHelper = UtilHelper.getInstance(this);

        // 뷰 초기화
        initViews();

        // 인터넷 연결 상태 확인
        if (!utilHelper.checkNetworkConnectionWithDialog(this)) {
            return; // 연결이 없으면 여기서 중단
        }

        // GameListManager 초기화
        gameListManager = new GameListManager(this);

        // 애니메이션 시작
        startAnimations();

        // 버전 체크 시작
        checkVersion();

        // 스플래시 타이머 시작
        startSplashTimer();
    }

    private boolean checkNetworkConnection() {
        if (!utilHelper.isNetworkConnected()) {
            Log.w(TAG, "No internet connection detected");
            showNetworkErrorDialog();
            return false;
        }

        Log.d(TAG, "Internet connection verified");
        return true;
    }

    private void showNetworkErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("This app requires an internet connection to function properly. Please check your network settings and try again.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d(TAG, "Network error dialog dismissed - closing app");
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    private void initViews() {
        appIcon = findViewById(R.id.app_icon);
        copyrightText = findViewById(R.id.copyright_text);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        // 저작권 텍스트 설정
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String copyright = "© " + currentYear + " RETROMASTER. All rights reserved.";
        copyrightText.setText(copyright);
    }

    private void startAnimations() {
        // 전체 화면 페이드인
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(500);
            rootView.startAnimation(fadeIn);
        }

        // 아이콘 애니메이션
        startIconAnimation();

        // 도트 스피너 애니메이션
        startDotSpinnerAnimation();
    }

    private void startIconAnimation() {
        // 페이드인 + 스케일 애니메이션
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);

        ScaleAnimation scaleIn = new ScaleAnimation(
                0.5f, 1.0f, 0.5f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleIn.setDuration(1000);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(scaleIn);

        appIcon.startAnimation(animationSet);
    }

    private void startDotSpinnerAnimation() {
        // 각 도트를 120도씩 차이나게 회전 애니메이션
        startDotRotation(dot1, 0);
        startDotRotation(dot2, 120);
        startDotRotation(dot3, 240);
    }

    private void startDotRotation(View dot, int startAngle) {
        final float radius = 15 * getResources().getDisplayMetrics().density; // 15dp to px

        ValueAnimator animator = ValueAnimator.ofFloat(0, 360);
        animator.setDuration(1200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            float angle = (Float) animation.getAnimatedValue() + startAngle;
            double radians = Math.toRadians(angle);

            float x = (float) (radius * Math.cos(radians));
            float y = (float) (radius * Math.sin(radians));

            dot.setTranslationX(x);
            dot.setTranslationY(y);
        });

        animator.start();
    }

    /**
     * 버전 체크 수행
     */
    private void checkVersion() {
        NetworkClient.getApiService().getVersionInfo().enqueue(new Callback<VersionResponse>() {
            @Override
            public void onResponse(Call<VersionResponse> call, Response<VersionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.e(TAG, "Version check success: " + response.body());
                    handleVersionResponse(response.body());
                } else {
                    Log.e(TAG, "Version check failed: " + response.code());
                    // 네트워크 에러 시 재확인
                    if (!utilHelper.isNetworkConnected()) {
                        utilHelper.showNetworkErrorDialog(SplashActivity.this);
                        return;
                    }
                    onVersionCheckCompleted();
                }
            }

            @Override
            public void onFailure(Call<VersionResponse> call, Throwable t) {
                Log.e(TAG, "Version check error", t);
                // 네트워크 에러 시 재확인
                if (!utilHelper.isNetworkConnected()) {
                    utilHelper.showNetworkErrorDialog(SplashActivity.this);
                    return;
                }
                onVersionCheckCompleted();
            }
        });
    }

    private void handleVersionResponse(VersionResponse versionResponse) {
        VersionResponse.Root root = versionResponse.getRoot();

        if (root.isCheck()) {
            String currentPackageName = getPackageName();
            String serverPackageName = root.getPackageName();

            if (!currentPackageName.equals(serverPackageName)) {
                showPackageUpdateDialog(serverPackageName);
                return;
            }

            int currentVersionCode = getCurrentVersionCode();
            int serverVersionCode = root.getNowVersionCode();

            if (currentVersionCode < serverVersionCode) {
                showVersionUpdateDialog();
                return;
            }

            // 광고 설정 저장
            saveAdSettingsFromServer(root);

            checkGameListVersion(root.getGameListVersion());
        } else {
            onVersionCheckCompleted();
            checkGameListVersionForced();
        }
    }

    private void saveAdSettingsFromServer(VersionResponse.Root root) {
        try {
            // 서버에서 받은 광고 설정 값들을 가져와서 저장
            boolean adBannerUse = root.isAdBannerUse(); // 배너 광고 사용 여부
            int adFullCnt = root.getAdFullCnt(); // 전면 광고 주기
            int adFullCoinCnt = root.getAdFullCoinCnt(); // 전면 광고 코인 개수

            // UtilHelper를 통해 광고 설정 저장
            utilHelper.saveAdSettings(adBannerUse, adFullCnt, adFullCoinCnt);

            Log.d(TAG, "서버에서 받은 광고 설정 저장 완료");
            utilHelper.logAdSettings(); // 디버그용 로깅

        } catch (Exception e) {
            Log.e(TAG, "광고 설정 저장 중 오류 발생: " + e.getMessage(), e);
            // 오류 발생 시 기본값으로 저장
            utilHelper.saveAdSettings(true, 1, 5);
        }
    }

    private void checkGameListVersion(int serverGameListVersion) {
        if (gameListManager.needsUpdate(serverGameListVersion)) {
            Log.d(TAG, "Game list update needed. Server version: " + serverGameListVersion +
                    ", Current version: " + gameListManager.getCurrentGameListVersion());
            updateGameList(serverGameListVersion);
        } else {
            Log.d(TAG, "Game list is up to date. Version: " + serverGameListVersion);
            onGameListUpdateCompleted();
        }
        onVersionCheckCompleted();
    }

    private void checkGameListVersionForced() {
        int currentVersion = gameListManager.getCurrentGameListVersion();
        if (currentVersion == 0) {
            Log.d(TAG, "First install detected. Downloading game list...");
            updateGameList(1);
        } else {
            onGameListUpdateCompleted();
        }
    }

    private void updateGameList(int newVersion) {
        gameListManager.updateGameList(newVersion, new GameListManager.GameListUpdateListener() {
            @Override
            public void onUpdateStarted() {
                Log.d(TAG, "Game list update started");
            }

            @Override
            public void onUpdateCompleted() {
                Log.d(TAG, "Game list update completed successfully");
                onGameListUpdateCompleted();
            }

            @Override
            public void onUpdateFailed(String error) {
                Log.e(TAG, "Game list update failed: " + error);
                // 네트워크 에러로 인한 실패인지 확인
                if (!utilHelper.isNetworkConnected()) {
                    utilHelper.showNetworkErrorDialog(SplashActivity.this);
                    return;
                }
                onGameListUpdateCompleted();
            }
        });
    }

    private void onGameListUpdateCompleted() {
        isGameListUpdateCompleted = true;
        checkAndProceedToMain();
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode();
            } else {
                return packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get version code", e);
            return 0;
        }
    }

    private void showPackageUpdateDialog(String packageName) {
        new AlertDialog.Builder(this)
                .setTitle("App Update Required")
                .setMessage("A new version of the app is available. Please update from the Play Store.")
                .setPositiveButton("Update", (dialog, which) -> openPlayStore(packageName))
                .setNegativeButton("Later", (dialog, which) -> {
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    private void showVersionUpdateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("App Update")
                .setMessage("A new version has been released. Would you like to update?")
                .setPositiveButton("Update", (dialog, which) -> openPlayStore(getPackageName()))
                .setNegativeButton("Later", (dialog, which) -> {
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
        finish();
    }

    private void onVersionCheckCompleted() {
        isVersionCheckCompleted = true;
        checkAndProceedToMain();
    }

    private void startSplashTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isSplashTimeCompleted = true;
            checkAndProceedToMain();
        }, SPLASH_DURATION);
    }

    private void checkAndProceedToMain() {
        if (isVersionCheckCompleted && isSplashTimeCompleted && isGameListUpdateCompleted) {
            startMainActivity();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // 스플래시 화면에서 뒤로가기 버튼 비활성화
    }
}