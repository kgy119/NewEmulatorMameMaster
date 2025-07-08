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
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ingcorp.webhard.model.VersionResponse;
import com.ingcorp.webhard.network.NetworkClient;
import com.ingcorp.webhard.manager.GameListManager;

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

    // 뷰 참조
    private ImageView appIcon;
    private TextView copyrightText;
    private View dot1, dot2, dot3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge 설정
        setupEdgeToEdge();

        // 레이아웃 설정
        setContentView(R.layout.activity_splash);

        // 뷰 초기화
        initViews();

        // GameListManager 초기화
        gameListManager = new GameListManager(this);

        // 애니메이션 시작
        startAnimations();

        // 버전 체크 시작
        checkVersion();

        // 스플래시 타이머 시작
        startSplashTimer();
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
                    onVersionCheckCompleted();
                }
            }

            @Override
            public void onFailure(Call<VersionResponse> call, Throwable t) {
                Log.e(TAG, "Version check error", t);
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

            checkGameListVersion(root.getGameListVersion());
        } else {
            onVersionCheckCompleted();
            checkGameListVersionForced();
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
                .setTitle("앱 업데이트 필요")
                .setMessage("새로운 버전의 앱이 있습니다. 플레이스토어에서 업데이트해주세요.")
                .setPositiveButton("업데이트", (dialog, which) -> openPlayStore(packageName))
                .setNegativeButton("나중에", (dialog, which) -> {
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    private void showVersionUpdateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("앱 업데이트")
                .setMessage("새로운 버전이 출시되었습니다. 업데이트하시겠습니까?")
                .setPositiveButton("업데이트", (dialog, which) -> openPlayStore(getPackageName()))
                .setNegativeButton("나중에", (dialog, which) -> {
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

    private void setupEdgeToEdge() {
        Window window = getWindow();
        View decorView = window.getDecorView();
        int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupEdgeToEdgeApi29(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupEdgeToEdgeApi26(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupEdgeToEdgeApi23(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupEdgeToEdgeApi21(window, decorView, colorPrimaryDark);
        }
    }

    private void setupEdgeToEdgeApi29(Window window, View decorView, int colorPrimaryDark) {
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);
        window.setStatusBarContrastEnforced(false);
        window.setNavigationBarContrastEnforced(false);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi26(Window window, View decorView, int colorPrimaryDark) {
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi23(Window window, View decorView, int colorPrimaryDark) {
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi21(Window window, View decorView, int colorPrimaryDark) {
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);
    }

    @Override
    public void onBackPressed() {
        // 스플래시 화면에서 뒤로가기 버튼 비활성화
    }
}