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
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ingcorp.webhard.helpers.PrefsHelper;
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.manager.GameListManager;
import com.ingcorp.webhard.model.VersionResponse;
import com.ingcorp.webhard.network.NetworkClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends Activity {

    private static final String TAG = "mame00";
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

        // 설정 타입 호환성 체크 및 수정
        PrefsHelper.validateAndFixPreferenceTypes(this);

        // 레이아웃 설정
        setContentView(R.layout.activity_splash);

        // ★★★ setContentView 후 전체화면 설정 ★★★
        enableFullScreen();

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

    // ★★★ 전체화면 모드 설정 ★★★
    private void enableFullScreen() {
        // Window와 DecorView가 준비되었는지 확인
        if (getWindow() == null || getWindow().getDecorView() == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30 이상 (Android 11+) - 상단바와 네비게이션 바 모두 숨김
            try {
                getWindow().setDecorFitsSystemWindows(false);
                if (getWindow().getInsetsController() != null) {
                    getWindow().getInsetsController().hide(
                            WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    getWindow().getInsetsController().setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } catch (Exception e) {
                // API 30 방식이 실패하면 구버전 방식으로 fallback
                enableFullScreenLegacy();
            }
        } else {
            enableFullScreenLegacy();
        }

        // API 28 이상에서 디스플레이 컷아웃 모드 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(layoutParams);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ★★★ 구버전 전체화면 모드 ★★★
    private void enableFullScreenLegacy() {
        // API 19-29 (구버전 방식) - 상단바와 네비게이션 바 모두 숨김
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ★★★ 전체화면 재적용 ★★★
        enableFullScreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullScreen();
        }
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
        Log.d(TAG, "앱 버전 확인을 시작합니다");

        NetworkClient.getApiService().getVersionInfo().enqueue(new Callback<VersionResponse>() {
            @Override
            public void onResponse(Call<VersionResponse> call, Response<VersionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "버전 확인 성공: " + response.body());
                    handleVersionResponse(response.body());
                } else {
                    Log.e(TAG, "버전 확인 실패 - 응답 코드: " + response.code());
                    // 네트워크 에러 시 재확인
                    if (!utilHelper.isNetworkConnected()) {
                        Log.w(TAG, "네트워크 연결이 끊어짐 - 네트워크 오류 다이얼로그 표시");
                        utilHelper.showNetworkErrorDialog(SplashActivity.this);
                        return;
                    }
                    onVersionCheckCompleted();
                }
            }

            @Override
            public void onFailure(Call<VersionResponse> call, Throwable t) {
                Log.e(TAG, "버전 확인 중 오류 발생", t);
                // 네트워크 에러 시 재확인
                if (!utilHelper.isNetworkConnected()) {
                    Log.w(TAG, "네트워크 연결이 끊어짐 - 네트워크 오류 다이얼로그 표시");
                    utilHelper.showNetworkErrorDialog(SplashActivity.this);
                    return;
                }
                Log.d(TAG, "버전 확인 실패했지만 앱을 계속 진행합니다");
                onVersionCheckCompleted();
            }
        });
    }

    private void handleVersionResponse(VersionResponse versionResponse) {
        VersionResponse.Root root = versionResponse.getRoot();

        if (root.isCheck()) {
            Log.d(TAG, "서버에서 버전 체크가 활성화됨");

            String currentPackageName = getPackageName();
            String serverPackageName = root.getPackageName();

            if (!currentPackageName.equals(serverPackageName)) {
                Log.w(TAG, "패키지명이 다름 - 현재: " + currentPackageName + ", 서버: " + serverPackageName);
                showPackageUpdateDialog(serverPackageName);
                return;
            }

            int currentVersionCode = getCurrentVersionCode();
            int serverVersionCode = root.getNowVersionCode();

            Log.d(TAG, "버전 코드 확인 - 현재: " + currentVersionCode + ", 서버: " + serverVersionCode);

            if (currentVersionCode < serverVersionCode) {
                Log.i(TAG, "새 버전이 있습니다. 업데이트가 필요합니다");
                showVersionUpdateDialog();
                return;
            }

            Log.d(TAG, "최신 버전입니다. 광고 설정을 저장합니다");
            // 광고 설정 저장
            saveAdSettingsFromServer(root);

            checkGameListVersion(root.getGameListVersion());
        } else {
            Log.d(TAG, "서버에서 버전 체크가 비활성화됨 - 바로 진행");
            onVersionCheckCompleted();
            checkGameListVersionForced();
        }
    }

    private void saveAdSettingsFromServer(VersionResponse.Root root) {
        try {
            // 서버에서 받은 광고 설정 값들을 가져와서 저장
            boolean adBannerUse = root.isAdBannerUse(); // 배너 광고 사용 여부
            int adFullCnt = root.getAdFullCnt(); // 전면 광고 주기
            int adFullCoinCnt = root.getAdFullCoinCnt(); // 보상형 광고 코인 개수
            int adNativeCnt = root.getAdNativeCnt(); // 네이티브 광고 주기

            Log.d(TAG, "광고 설정 저장 중 - 배너: " + adBannerUse + ", 전면주기: " + adFullCnt +
                    ", 보상코인: " + adFullCoinCnt + ", 네이티브주기: " + adNativeCnt);

            // UtilHelper를 통해 광고 설정 저장
            utilHelper.saveAdSettings(adBannerUse, adFullCnt, adFullCoinCnt, adNativeCnt);

            Log.d(TAG, "서버에서 받은 광고 설정 저장 완료");
            utilHelper.logAdSettings(); // 디버그용 로깅

        } catch (Exception e) {
            Log.e(TAG, "광고 설정 저장 중 오류 발생: " + e.getMessage(), e);
            // 오류 발생 시 기본값으로 저장
            Log.w(TAG, "기본 광고 설정으로 저장합니다");
            utilHelper.saveAdSettings(true, 1, 5, 10);
        }
    }

    private void checkGameListVersion(int serverGameListVersion) {
        Log.d(TAG, "게임 리스트 버전 확인 중 - 서버: " + serverGameListVersion +
                ", 현재: " + gameListManager.getCurrentGameListVersion());

        if (gameListManager.needsUpdate(serverGameListVersion)) {
            Log.i(TAG, "게임 리스트 업데이트가 필요합니다. 다운로드를 시작합니다");
            updateGameList(serverGameListVersion);
        } else {
            Log.d(TAG, "게임 리스트가 최신 버전입니다. 업데이트가 필요하지 않습니다");
            onGameListUpdateCompleted();
        }
        onVersionCheckCompleted();
    }

    private void checkGameListVersionForced() {
        int currentVersion = gameListManager.getCurrentGameListVersion();
        Log.d(TAG, "강제 게임 리스트 버전 확인 - 현재 버전: " + currentVersion);

        if (currentVersion == 0) {
            Log.i(TAG, "첫 설치가 감지되었습니다. 게임 리스트를 다운로드합니다");
            updateGameList(1);
        } else {
            Log.d(TAG, "게임 리스트가 이미 존재합니다");
            onGameListUpdateCompleted();
        }
    }

    private void updateGameList(int newVersion) {
        Log.d(TAG, "게임 리스트 업데이트 시작 - 새 버전: " + newVersion);

        gameListManager.updateGameList(newVersion, new GameListManager.GameListUpdateListener() {
            @Override
            public void onUpdateStarted() {
                Log.d(TAG, "게임 리스트 업데이트가 시작되었습니다");
            }

            @Override
            public void onUpdateCompleted() {
                Log.i(TAG, "게임 리스트 업데이트가 성공적으로 완료되었습니다");
                onGameListUpdateCompleted();
            }

            @Override
            public void onUpdateFailed(String error) {
                Log.e(TAG, "게임 리스트 업데이트 실패: " + error);
                // 네트워크 에러로 인한 실패인지 확인
                if (!utilHelper.isNetworkConnected()) {
                    Log.w(TAG, "네트워크 연결 문제로 게임 리스트 업데이트 실패");
                    utilHelper.showNetworkErrorDialog(SplashActivity.this);
                    return;
                }
                Log.w(TAG, "게임 리스트 업데이트에 실패했지만 앱을 계속 진행합니다");
                onGameListUpdateCompleted();
            }
        });
    }

    private void onGameListUpdateCompleted() {
        Log.d(TAG, "게임 리스트 업데이트 작업 완료");
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
            Log.e(TAG, "앱의 버전 코드를 가져올 수 없습니다", e);
            return 0;
        }
    }

    private void showPackageUpdateDialog(String packageName) {
        Log.i(TAG, "패키지 업데이트 다이얼로그 표시");
        new AlertDialog.Builder(this)
                .setTitle("App Update Required")
                .setMessage("A new version of the app is available. Please update from the Play Store.")
                .setPositiveButton("Update", (dialog, which) -> {
                    Log.d(TAG, "사용자가 업데이트 버튼을 클릭했습니다");
                    openPlayStore(packageName);
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    Log.d(TAG, "사용자가 나중에 버튼을 클릭했습니다. 앱을 종료합니다");
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    private void showVersionUpdateDialog() {
        Log.i(TAG, "버전 업데이트 다이얼로그 표시");
        new AlertDialog.Builder(this)
                .setTitle("App Update")
                .setMessage("A new version has been released. Would you like to update?")
                .setPositiveButton("Update", (dialog, which) -> {
                    Log.d(TAG, "사용자가 업데이트 버튼을 클릭했습니다");
                    openPlayStore(getPackageName());
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    Log.d(TAG, "사용자가 나중에 버튼을 클릭했습니다. 앱을 종료합니다");
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    private void openPlayStore(String packageName) {
        Log.d(TAG, "플레이스토어 열기 시도: " + packageName);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
            Log.d(TAG, "플레이스토어 앱으로 이동 성공");
        } catch (android.content.ActivityNotFoundException e) {
            Log.w(TAG, "플레이스토어 앱이 없어 웹브라우저로 이동합니다");
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
        finish();
    }

    private void onVersionCheckCompleted() {
        Log.d(TAG, "버전 확인 작업 완료");
        isVersionCheckCompleted = true;
        checkAndProceedToMain();
    }

    private void startSplashTimer() {
        Log.d(TAG, "스플래시 타이머 시작 (" + SPLASH_DURATION + "ms)");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "스플래시 타이머 완료");
            isSplashTimeCompleted = true;
            checkAndProceedToMain();
        }, SPLASH_DURATION);
    }

    private void checkAndProceedToMain() {
        Log.d(TAG, "메인 화면 진행 조건 확인 - 버전체크: " + isVersionCheckCompleted +
                ", 스플래시시간: " + isSplashTimeCompleted +
                ", 게임리스트: " + isGameListUpdateCompleted);

        if (isVersionCheckCompleted && isSplashTimeCompleted && isGameListUpdateCompleted) {
            Log.i(TAG, "모든 조건이 완료되어 메인 화면으로 이동합니다");
            startMainActivity();
        }
    }

    private void startMainActivity() {
        Log.d(TAG, "MainActivity 시작");
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // 스플래시 화면에서 뒤로가기 버튼 비활성화
        Log.d(TAG, "스플래시 화면에서 뒤로가기 버튼이 눌렸지만 무시됩니다");
    }
}