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
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
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
    }

    private void initHelpers() {
        gameRepository = new GameRepository(this);
        networkManager = NetworkManager.getInstance();
    }

    private void startAnimation() {
        // 아이콘 페이드 인 애니메이션
        AlphaAnimation iconFadeIn = new AlphaAnimation(0.0f, 1.0f);
        iconFadeIn.setDuration(1000);
        iconFadeIn.setStartOffset(500);

        // 텍스트 슬라이드 업 + 페이드 인 애니메이션
        TranslateAnimation textSlideUp = new TranslateAnimation(0, 0, 100, 0);
        textSlideUp.setDuration(800);
        textSlideUp.setStartOffset(1200);

        AlphaAnimation textFadeIn = new AlphaAnimation(0.0f, 1.0f);
        textFadeIn.setDuration(800);
        textFadeIn.setStartOffset(1200);

        AnimationSet textAnimSet = new AnimationSet(true);
        textAnimSet.addAnimation(textSlideUp);
        textAnimSet.addAnimation(textFadeIn);

        splashIcon.startAnimation(iconFadeIn);
        splashText.startAnimation(textAnimSet);
    }

    private void checkVersion() {
        updateLoadingText("버전 정보 확인 중...");

        networkManager.getVersionInfo(new NetworkManager.NetworkCallback<VersionResponse>() {
            @Override
            public void onSuccess(VersionResponse versionResponse) {
                // 실제 API 구조에 맞춰 수정
                VersionResponse.VersionRoot root = versionResponse.getRoot();
                Log.d(TAG, "Version response: " + root);

                if (root != null && root.isCheckEnabled()) {
                    // 패키지명 확인
                    if (!getPackageName().equals(root.getPackageName())) {
                        redirectToPlayStore(getPackageName());
                        return;
                    }

                    // 버전 코드 확인
                    if (getCurrentVersionCode() < root.getNowVersionCodeInt()) {
                        redirectToPlayStore(getPackageName());
                        return;
                    }

                    // 게임 리스트 버전 확인
                    checkGameListVersion(root.getGameListVersion());
                } else {
                    // 버전 체크가 비활성화된 경우 바로 메인으로
                    goToMainActivity();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Version check error: " + error);
                // 네트워크 오류 시 메인으로 이동
                goToMainActivity();
            }
        });
    }

    private void checkGameListVersion(String serverGameListVersion) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String localGameListVersion = prefs.getString(KEY_GAME_LIST_VERSION, "");

        // 처음 설치하거나 버전이 다른 경우 게임 리스트 업데이트
        if (localGameListVersion.isEmpty() || !localGameListVersion.equals(serverGameListVersion)) {
            updateGameList(serverGameListVersion);
        } else {
            // 버전이 같으면 바로 메인으로
            goToMainActivity();
        }
    }

    private void updateGameList(String newGameListVersion) {
        updateLoadingText("게임 리스트 업데이트 중...");

        networkManager.getGameList(new NetworkManager.NetworkCallback<GameListResponse>() {
            @Override
            public void onSuccess(GameListResponse gameListResponse) {
                // 실제 API 구조에 맞춰 수정
                GameListResponse.GameListRoot root = gameListResponse.getRoot();
                if (root != null && root.getList() != null) {
                    List<Game> games = root.getList();
                    Log.d(TAG, "Retrieved " + games.size() + " games from server");

                    // Room 데이터베이스에 게임 리스트 저장
                    gameRepository.replaceAllGames(games, new GameRepository.RepositoryCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            Log.d(TAG, "Successfully saved " + count + " games to database");

                            // 게임 리스트 버전 저장
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            prefs.edit().putString(KEY_GAME_LIST_VERSION, newGameListVersion).apply();

                            // 메인 액티비티로 이동
                            goToMainActivity();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Database save error: " + error);
                            // 데이터베이스 오류가 있어도 메인으로 이동
                            goToMainActivity();
                        }
                    });
                } else {
                    Log.e(TAG, "Invalid game list response");
                    goToMainActivity();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Game list download error: " + error);
                goToMainActivity();
            }
        });
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