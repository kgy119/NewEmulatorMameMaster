package com.ingcorp.webhard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.ingcorp.webhard.adapter.GamePagerAdapter;
import com.ingcorp.webhard.base.Constants;
import com.ingcorp.webhard.fragment.GameFragment;
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.manager.AdMobManager;
import com.ingcorp.webhard.manager.GameListManager;

import java.io.File;
import java.util.Map;

public class MainActivity extends FragmentActivity {

    private TextView[] tabViews;
    private ViewPager2 viewPager;
    private HorizontalScrollView tabScrollView;
    private LinearLayout tabLayout;
    private int selectedTabIndex = 0;
    private GameListManager gameListManager;
    private String[] tabs;

    // AdMob 관련
    private AdMobManager adMobManager;
    private FrameLayout adContainerView;
    private UtilHelper utilHelper;
    private ReviewManager reviewManager;
    private static final String REVIEW_PREF_KEY = "app_review_requested";

    private static final String NOTIFICATION_PERMISSION_KEY = "notification_permission_requested";
    private static final String LAST_NOTIFICATION_REQUEST_KEY = "last_notification_request_time";
    private static final long NOTIFICATION_REQUEST_INTERVAL = 17 * 24 * 60 * 60 * 1000L; // 17일 (밀리초)
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // GameListManager를 가장 먼저 초기화
        gameListManager = new GameListManager(this);

        // 즉시 정적 변수에 설정
        GameFragment.setGameListManager(gameListManager);

        // setContentView 호출
        setContentView(R.layout.activity_main);

        // tabs 배열 로드
        tabs = getGameTabs();

        // 다른 매니저들 초기화
        adMobManager = AdMobManager.getInstance(this);
        utilHelper = UtilHelper.getInstance(this);
        reviewManager = ReviewManagerFactory.create(this);

        // UI 컴포넌트 초기화
        initViews();
        createTabs();
        setupViewPager();

        // 나머지 초기화
        loadCollapsibleBannerIfEnabled();
        loadInterstitialAd();
        initializeAndCleanup();

        // 알림 권한 요청만 먼저 실행
//        if (BuildConfig.DEBUG) {
//            resetReviewRequestState();
//            resetNotificationRequestState();
//        }

        checkAndRequestNotificationPermission();
    }

    /**
     * 앱 초기화 및 정리 작업을 수행하는 메서드
     */
    private void initializeAndCleanup() {
        // 백그라운드 스레드에서 정리 작업 수행
        new Thread(() -> {
            try {
                UtilHelper utilHelper = UtilHelper.getInstance(this);

                // ROMs 경로 가져오기
                String romsPath = getRomsPath();
                if (romsPath != null) {
                    Log.d(Constants.LOG_TAG, "앱 시작시 정리 작업 시작");

                    // 디렉토리 상태 로깅
                    utilHelper.logRomsDirectoryStatus(romsPath);

                    // 임시 파일들 정리
                    utilHelper.cleanupAllTemporaryFiles(romsPath);

                    // 진행 중이던 다운로드 상태들 정리
                    cleanupDownloadStates(utilHelper);

                    Log.d(Constants.LOG_TAG, "앱 시작시 정리 작업 완료");
                } else {
                    Log.e(Constants.LOG_TAG, "ROMs 경로를 가져올 수 없어 정리 작업을 건너뜀");
                }

            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, "앱 시작시 정리 작업 중 오류", e);
            }
        }).start();
    }

    /**
     * ROMs 디렉토리 경로를 가져오는 메서드
     */
    private String getRomsPath() {
        try {
            File appDir = getExternalFilesDir(null);
            if (appDir != null) {
                File romsDir = new File(appDir, "roms");
                if (!romsDir.exists()) romsDir.mkdirs();
                return romsDir.getAbsolutePath();
            }

            File fallbackDir = new File(getFilesDir(), "roms");
            if (!fallbackDir.exists()) fallbackDir.mkdirs();
            return fallbackDir.getAbsolutePath();

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Error getting roms path", e);
            return null;
        }
    }

    /**
     * 진행 중이던 다운로드 상태들을 정리하는 메서드
     */
    private void cleanupDownloadStates(UtilHelper utilHelper) {
        try {
            // SharedPreferences에서 download_state_로 시작하는 모든 키를 찾아서 정리
            SharedPreferences prefs = getSharedPreferences("WebHardPrefs", Context.MODE_PRIVATE);
            Map<String, ?> allPrefs = prefs.getAll();

            SharedPreferences.Editor editor = prefs.edit();
            int cleanedCount = 0;

            for (String key : allPrefs.keySet()) {
                if (key.startsWith("download_state_")) {
                    String value = (String) allPrefs.get(key);

                    // downloading 상태로 남아있는 것들은 정리
                    if (value != null && value.startsWith("downloading")) {
                        editor.putString(key, "none");
                        cleanedCount++;
                    }
                }
            }

            if (cleanedCount > 0) {
                editor.apply();
                Log.d(Constants.LOG_TAG, "다운로드 상태 " + cleanedCount + "개 정리됨");
            }

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "다운로드 상태 정리 중 오류", e);
        }
    }

    /**
     * 게임 탭 배열을 가져오는 헬퍼 메서드
     */
    private String[] getGameTabs() {
        return getResources().getStringArray(R.array.game_categories);
    }

    /**
     * 탭 개수를 반환하는 헬퍼 메서드
     */
    private int getTabCount() {
        return tabs.length; // strings.xml에서 로드된 탭 배열 길이 사용
    }


    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        tabScrollView = findViewById(R.id.tab_scroll_view);
        tabLayout = findViewById(R.id.tab_layout);
        adContainerView = findViewById(R.id.ad_view_banner);
    }

    private void createTabs() {
        int tabCount = getTabCount();
        if (tabCount == 0) {
            Log.e(Constants.LOG_TAG, "탭 배열이 비어있습니다.");
            return;
        }

        tabViews = new TextView[tabCount];
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < tabCount; i++) {
            View tabView = inflater.inflate(R.layout.tab_item, tabLayout, false);
            TextView tabText = tabView.findViewById(R.id.tab_text);
            tabText.setText(tabs[i]);

            final int index = i;
            tabView.setOnClickListener(v -> selectTab(index));

            tabViews[i] = tabText;
            tabLayout.addView(tabView);
        }

        updateTabSelection(0);
    }

    private void setupViewPager() {
        if (gameListManager == null) {
            Log.e(Constants.LOG_TAG, "GameListManager가 null입니다! ViewPager 설정 중단");
            return;
        }

        GameFragment.setGameListManager(gameListManager);

        GamePagerAdapter adapter = new GamePagerAdapter(
                getSupportFragmentManager(),
                getLifecycle(),
                gameListManager,
                this  // Context 추가
        );

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(getTabCount());
        viewPager.setUserInputEnabled(true);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabSelection(position);
                scrollToSelectedTab(position);
            }
        });
    }


    private void selectTab(int index) {
        if (selectedTabIndex != index) {
            viewPager.setCurrentItem(index, true);
        }
    }

    private void updateTabSelection(int index) {
        selectedTabIndex = index;

        for (int i = 0; i < tabViews.length; i++) {
            TextView tabText = tabViews[i];
            View tabContainer = (View) tabText.getParent();
            View indicator = tabContainer.findViewById(R.id.tab_indicator);

            if (i == index) {
                // 선택된 탭: 흰색 텍스트 + 하단 인디케이터 표시
                tabText.setTextColor(getResources().getColor(R.color.white));
                tabText.setTextSize(18);
                tabText.setTypeface(null, android.graphics.Typeface.BOLD);

                if (indicator != null) {
                    indicator.setVisibility(View.VISIBLE);
                }
            } else {
                // 선택되지 않은 탭: 희미한 흰색 + 인디케이터 숨김
                tabText.setTextColor(android.graphics.Color.argb(128, 255, 255, 255));
                tabText.setTextSize(16);
                tabText.setTypeface(null, android.graphics.Typeface.NORMAL);

                if (indicator != null) {
                    indicator.setVisibility(View.GONE);
                }
            }
        }
    }

    private void scrollToSelectedTab(int index) {
        if (tabViews != null && index < tabViews.length) {
            TextView selectedTabText = tabViews[index];
            View selectedTabContainer = (View) selectedTabText.getParent();
            int scrollX = selectedTabContainer.getLeft() - (tabScrollView.getWidth() / 2) + (selectedTabContainer.getWidth() / 2);
            tabScrollView.smoothScrollTo(Math.max(0, scrollX), 0);
        }
    }

    private void loadCollapsibleBannerIfEnabled() {
        // 배너 광고 설정 확인
        if (!utilHelper.isAdBannerEnabled()) {
            Log.d(Constants.LOG_TAG, "배너 광고가 비활성화됨");
            adContainerView.setVisibility(View.GONE);
            return;
        }

        loadCollapsibleBanner();
    }

    private void loadCollapsibleBanner() {
        if (adMobManager != null && adContainerView != null) {
            adMobManager.loadCollapsibleBanner(this, adContainerView, new AdMobManager.OnBannerAdLoadedListener() {
                @Override
                public void onAdLoaded(AdView adView) {
                    adContainerView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(Constants.LOG_TAG, "배너 광고 로드 실패: " + error);
                    adContainerView.setVisibility(View.GONE);
                }
            });
        }
    }

    private void loadInterstitialAd() {
        if (adMobManager != null) {
            adMobManager.loadInterstitialAd(new AdMobManager.OnInterstitialAdLoadedListener() {
                @Override
                public void onAdLoaded() {
                    // 전면광고 로드 완료
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(Constants.LOG_TAG, "전면광고 로드 실패: " + error);
                }

                @Override
                public void onAdClosed() {
                    // 전면광고 닫힌 후 처리
                }

                @Override
                public void onAdShown() {
                    // 전면광고 표시됨
                }

                @Override
                public void onAdShowFailed(String error) {
                    Log.e(Constants.LOG_TAG, "전면광고 표시 실패: " + error);
                }
            });
        }
    }

    /**
     * 앱 설치 후 첫 실행시에만 리뷰를 요청하는 메서드
     */
    private void requestAppReviewIfFirstTime() {
        SharedPreferences prefs = getSharedPreferences("WebHardPrefs", Context.MODE_PRIVATE);
        boolean reviewRequested = prefs.getBoolean(REVIEW_PREF_KEY, false);

        if (!reviewRequested) {
            Log.d(Constants.LOG_TAG, "첫 실행 - 앱 리뷰 요청 시작");

            // 리뷰 요청 상태를 즉시 true로 설정 (중복 방지)
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(REVIEW_PREF_KEY, true);
            editor.apply();

            showInAppReview();
        } else {
            Log.d(Constants.LOG_TAG, "이미 리뷰 요청됨 - 건너뜀");
        }
    }

    /**
     * Google Play In-App Review API를 사용하여 리뷰 요청
     */
    private void showInAppReview() {
        if (reviewManager == null) {
            Log.e(Constants.LOG_TAG, "ReviewManager가 null입니다");
            return;
        }

        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(reviewTask -> {
                    if (reviewTask.isSuccessful()) {
                        Log.d(Constants.LOG_TAG, "리뷰 플로우 완료");
                    } else {
                        Log.e(Constants.LOG_TAG, "리뷰 플로우 실패", reviewTask.getException());
                    }
                });
            } else {
                Log.e(Constants.LOG_TAG, "리뷰 요청 실패", task.getException());
                // 대체 방법: 플레이스토어로 직접 이동
                showAlternativeReviewDialog();
            }
        });
    }

    /**
     * In-App Review가 실패할 경우 대체 다이얼로그 표시
     */
    private void showAlternativeReviewDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("앱 평가")
                .setMessage("앱이 마음에 드시나요? 플레이스토어에서 평가해주세요!")
                .setPositiveButton("평가하기", (dialog, which) -> {
                    openPlayStore();
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * 플레이스토어 앱 페이지로 이동
     */
    private void openPlayStore() {
        try {
            String packageName = getPackageName();
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=" + packageName)
            );
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // 플레이스토어 앱이 없는 경우 웹브라우저로 이동
            String packageName = getPackageName();
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
            );
            startActivity(intent);
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "플레이스토어 열기 실패", e);
        }
    }

    /**
     * 알림 권한 상태를 확인하고 필요시 요청하는 메서드
     */
    private void checkAndRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted()) {
                if (shouldRequestNotificationPermission()) {
                    Log.d(Constants.LOG_TAG, "알림 권한 요청 시작");
                    requestNotificationPermissionWithDelay();
                } else {
                    Log.d(Constants.LOG_TAG, "알림 권한 요청 주기가 아직 되지 않음");
                    // 권한 요청을 안 하는 경우에도 앱 리뷰 체크
                    requestAppReviewIfFirstTime();
                }
            } else {
                Log.d(Constants.LOG_TAG, "알림 권한이 이미 허용됨");
                // 이미 권한이 있는 경우에도 앱 리뷰 체크
                requestAppReviewIfFirstTime();
            }
        } else {
            Log.d(Constants.LOG_TAG, "Android 13 미만 - 알림 권한 확인 불필요");
            // Android 13 미만에서는 바로 앱 리뷰 체크
            requestAppReviewIfFirstTime();
        }
    }


    /**
     * 알림 권한이 허용되어 있는지 확인
     */
    private boolean isNotificationPermissionGranted() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 미만에서는 항상 true
    }

    /**
     * 알림 권한을 요청해야 하는지 판단
     */
    private boolean shouldRequestNotificationPermission() {
        SharedPreferences prefs = getSharedPreferences("WebHardPrefs", Context.MODE_PRIVATE);
        long lastRequestTime = prefs.getLong(LAST_NOTIFICATION_REQUEST_KEY, 0);
        long currentTime = System.currentTimeMillis();

        // 첫 설치 시 (lastRequestTime == 0) 또는 지정된 간격이 지났으면 요청
        return (lastRequestTime == 0) || (currentTime - lastRequestTime >= NOTIFICATION_REQUEST_INTERVAL);
    }

    /**
     * 앱 설치 시간을 가져오는 메서드
     */
    private long getAppInstallTime() {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return packageInfo.firstInstallTime;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.e(Constants.LOG_TAG, "패키지 정보를 가져올 수 없음", e);
            return System.currentTimeMillis(); // 현재 시간을 기본값으로 사용
        }
    }

    /**
     * 지연 후 알림 권한 요청
     */
    private void requestNotificationPermissionWithDelay() {
        // 사용자 경험을 위해 5초 후에 요청
        new android.os.Handler().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                requestNotificationPermission();
            }
        }, 2000);
    }

    /**
     * 시스템 알림 권한 요청
     */
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE
            );
        }
        updateLastNotificationRequestTime();
    }

    /**
     * 마지막 알림 권한 요청 시간 업데이트
     */
    private void updateLastNotificationRequestTime() {
        SharedPreferences prefs = getSharedPreferences("WebHardPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_NOTIFICATION_REQUEST_KEY, System.currentTimeMillis());
        editor.apply();
        Log.d(Constants.LOG_TAG, "알림 권한 요청 시간 업데이트됨");
    }

    /**
     * 권한 요청 결과 처리
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(Constants.LOG_TAG, "알림 권한 허용됨");
            } else {
                Log.d(Constants.LOG_TAG, "알림 권한 거부됨");
            }

            // 권한 요청 완료 후 앱 리뷰 요청 (첫 실행시에만)
            requestAppReviewIfFirstTime();
        }
    }


    /**
     * 알림 권한 요청 상태를 재설정하는 메서드 (디버깅용)
     * 실제 배포시에는 제거하거나 주석처리
     */
//    private void resetNotificationRequestState() {
//        SharedPreferences prefs = getSharedPreferences("WebHardPrefs", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putLong(LAST_NOTIFICATION_REQUEST_KEY, 0);
//        editor.apply();
//        Log.d(Constants.LOG_TAG, "알림 권한 요청 상태 재설정됨");
//    }

    /**
     * 리뷰 요청 상태를 재설정하는 메서드 (디버깅용)
     * 실제 배포시에는 제거하거나 주석처리
     */
//    private void resetReviewRequestState() {
//        SharedPreferences prefs = getSharedPreferences("WebHardPrefs", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putBoolean(REVIEW_PREF_KEY, false);
//        editor.apply();
//        Log.d(Constants.LOG_TAG, "리뷰 요청 상태 재설정됨");
//    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameListManager != null) {
            gameListManager.cleanup();
        }
        if (adMobManager != null) {
            adMobManager.destroyBannerAd();
        }
    }
}