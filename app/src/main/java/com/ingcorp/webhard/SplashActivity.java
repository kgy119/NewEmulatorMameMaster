package com.ingcorp.webhard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ingcorp.webhard.models.Game;
import com.ingcorp.webhard.models.GameListResponse;
import com.ingcorp.webhard.models.VersionResponse;
import com.ingcorp.webhard.network.NetworkManager;
import com.ingcorp.webhard.repository.GameRepository;

import java.util.List;

public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_GAME_LIST_VERSION = "gameListVersion";

    private TextView splashText;
    private ImageView splashIcon;
    private ProgressBar loadingSpinner; // ProgressBar로 변경
    private ProgressBar progressBar;
    private View loadingContainer;

    private GameRepository gameRepository;
    private NetworkManager networkManager;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        initHelpers();
        startAnimation();

        // 2초 후 버전 체크 시작
        mainHandler.postDelayed(this::checkVersion, 2000);
    }

    private void initViews() {
        splashText = findViewById(R.id.splash_text);
        splashIcon = findViewById(R.id.splash_icon);
        loadingSpinner = findViewById(R.id.loading_spinner); // ProgressBar
        progressBar = findViewById(R.id.progress_bar);
        loadingContainer = findViewById(R.id.loading_container);

        // 초기에는 로딩 요소들 숨김
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void initHelpers() {
        gameRepository = new GameRepository(this);
        networkManager = NetworkManager.getInstance();
    }

    private void startAnimation() {
        // 아이콘 페이드 인 애니메이션
        if (splashIcon != null) {
            AlphaAnimation iconFadeIn = new AlphaAnimation(0.0f, 1.0f);
            iconFadeIn.setDuration(1000);
            iconFadeIn.setStartOffset(500);
            iconFadeIn.setFillAfter(true);
            splashIcon.startAnimation(iconFadeIn);
        }

        // 텍스트 슬라이드 업 + 페이드 인 애니메이션
        if (splashText != null) {
            TranslateAnimation textSlideUp = new TranslateAnimation(0, 0, 100, 0);
            textSlideUp.setDuration(800);
            textSlideUp.setStartOffset(1200);

            AlphaAnimation textFadeIn = new AlphaAnimation(0.0f, 1.0f);
            textFadeIn.setDuration(800);
            textFadeIn.setStartOffset(1200);

            AnimationSet textAnimSet = new AnimationSet(true);
            textAnimSet.addAnimation(textSlideUp);
            textAnimSet.addAnimation(textFadeIn);
            textAnimSet.setFillAfter(true);

            splashText.startAnimation(textAnimSet);
        }
    }

    private void showLoadingIndicator() {
        runOnUiThread(() -> {
            // 로딩 컨테이너 표시
            if (loadingContainer != null) {
                loadingContainer.setVisibility(View.VISIBLE);
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(300);
                fadeIn.setFillAfter(true);
                loadingContainer.startAnimation(fadeIn);
            }

            // 로딩 스피너 표시 (ProgressBar는 자동으로 회전 애니메이션됨)
            if (loadingSpinner != null) {
                loadingSpinner.setVisibility(View.VISIBLE);
            }

            // 프로그레스 바 표시
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideLoadingIndicator() {
        runOnUiThread(() -> {
            // ProgressBar는 clearAnimation 불필요
            if (loadingSpinner != null) {
                loadingSpinner.setVisibility(View.GONE);
            }

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (loadingContainer != null) {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(300);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        loadingContainer.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                loadingContainer.startAnimation(fadeOut);
            }
        });
    }

    private void checkVersion() {
        updateLoadingText("버전 정보 확인 중...");
        showLoadingIndicator();

        networkManager.getVersionInfo(new NetworkManager.NetworkCallback<VersionResponse>() {
            @Override
            public void onSuccess(VersionResponse versionResponse) {
                VersionResponse.VersionRoot root = versionResponse.getRoot();
                Log.d(TAG, "Version response: " + root);

                if (root != null && root.isCheckEnabled()) {
                    // 패키지명 확인
                    if (!getPackageName().equals(root.getPackageName())) {
                        hideLoadingIndicator();
                        redirectToPlayStore(getPackageName());
                        return;
                    }

                    // 버전 코드 확인
                    if (getCurrentVersionCode() < root.getNowVersionCodeInt()) {
                        hideLoadingIndicator();
                        redirectToPlayStore(getPackageName());
                        return;
                    }

                    // 게임 리스트 버전 확인 (int형으로 직접 사용)
                    checkGameListVersion(root.getGameListVersionInt());

                    // 광고 설정 저장 (필요시)
                    saveAdSettings(root);
                } else {
                    // 버전 체크가 비활성화된 경우 바로 메인으로
                    hideLoadingIndicator();
                    goToMainActivity();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Version check error: " + error);
                hideLoadingIndicator();
                // 네트워크 오류 시 메인으로 이동
                goToMainActivity();
            }
        });
    }

    private void checkGameListVersion(int serverGameListVersion) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int localGameListVersion = prefs.getInt(KEY_GAME_LIST_VERSION, 0); // 기본값 0

        Log.d(TAG, "Local game list version: " + localGameListVersion);
        Log.d(TAG, "Server game list version: " + serverGameListVersion);

        // 로컬 버전이 서버 버전보다 작으면 업데이트
        if (localGameListVersion < serverGameListVersion) {
            Log.d(TAG, "Game list update required (local: " + localGameListVersion +
                    " < server: " + serverGameListVersion + ")");
            updateGameList(serverGameListVersion);
        } else {
            Log.d(TAG, "Game list is up to date (local: " + localGameListVersion +
                    " >= server: " + serverGameListVersion + ")");
            hideLoadingIndicator();
            goToMainActivity();
        }
    }

    private void updateGameList(int newGameListVersion) {
        updateLoadingText("게임 리스트 업데이트 중...");

        networkManager.getGameList(new NetworkManager.NetworkCallback<GameListResponse>() {
            @Override
            public void onSuccess(GameListResponse gameListResponse) {
                GameListResponse.GameListRoot root = gameListResponse.getRoot();
                if (root != null && root.getList() != null) {
                    List<Game> games = root.getList();
                    Log.d(TAG, "Retrieved " + games.size() + " games from server");

                    updateLoadingText("데이터베이스 저장 중...");

                    // Room 데이터베이스에 게임 리스트 저장
                    gameRepository.replaceAllGames(games, new GameRepository.RepositoryCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            Log.d(TAG, "Successfully saved " + count + " games to database");

                            // 게임 리스트 버전 저장 (int형으로 저장)
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            prefs.edit().putInt(KEY_GAME_LIST_VERSION, newGameListVersion).apply();

                            Log.d(TAG, "Game list version updated to: " + newGameListVersion);

                            updateLoadingText("완료!");
                            hideLoadingIndicator();

                            // 짧은 지연 후 메인 액티비티로 이동
                            mainHandler.postDelayed(() -> goToMainActivity(), 500);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Database save error: " + error);
                            hideLoadingIndicator();
                            // 데이터베이스 오류가 있어도 메인으로 이동
                            goToMainActivity();
                        }
                    });
                } else {
                    Log.e(TAG, "Invalid game list response");
                    hideLoadingIndicator();
                    goToMainActivity();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Game list download error: " + error);
                hideLoadingIndicator();
                goToMainActivity();
            }
        });
    }

    /**
     * 광고 설정을 SharedPreferences에 저장 (필요시 사용)
     */
    private void saveAdSettings(VersionResponse.VersionRoot root) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("ad_banner_enabled", root.isAdBannerEnabled());
        editor.putInt("ad_full_count", root.getAdFullCnt());
        editor.putInt("ad_full_coin_count", root.getAdFullCoinCnt());

        editor.apply();

        Log.d(TAG, "Ad settings saved - Banner: " + root.isAdBannerEnabled() +
                ", FullCnt: " + root.getAdFullCnt() +
                ", FullCoinCnt: " + root.getAdFullCoinCnt());
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found", e);
            return 0;
        }
    }

    private void redirectToPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            // Google Play 앱이 없는 경우 웹 브라우저로
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
        finish();
    }

    private void updateLoadingText(String text) {
        runOnUiThread(() -> {
            if (splashText != null) {
                splashText.setText(text);

                // 텍스트 업데이트 시 살짝 강조 애니메이션
                AlphaAnimation textPulse = new AlphaAnimation(0.7f, 1.0f);
                textPulse.setDuration(300);
                textPulse.setRepeatCount(1);
                textPulse.setRepeatMode(Animation.REVERSE);
                splashText.startAnimation(textPulse);
            }
        });
    }

    private void goToMainActivity() {
        runOnUiThread(() -> {
            Intent intent = new Intent(SplashActivity.this, MAME4droid.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ProgressBar는 별도 애니메이션 정리 불필요 (자동 관리됨)

        if (gameRepository != null) {
            gameRepository.cleanup();
        }
        if (networkManager != null) {
            networkManager.cancelAllRequests();
        }

        // Handler 콜백 제거
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}