package com.ingcorp.webhard;

import android.content.Context;
import android.content.SharedPreferences;
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
    private String[] tabs; // 리소스에서 로드할 탭 배열

    // AdMob 관련
    private AdMobManager adMobManager;
    private FrameLayout adContainerView;
    private UtilHelper utilHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 1. GameListManager를 가장 먼저 초기화
        gameListManager = new GameListManager(this);
        Log.d(Constants.LOG_TAG, "GameListManager 생성됨: " + (gameListManager != null));

        // ✅ 2. 즉시 정적 변수에 설정 (다른 모든 초기화 전에!)
        GameFragment.setGameListManager(gameListManager);
        Log.d(Constants.LOG_TAG, "정적 GameListManager 설정 완료");

        // ✅ 3. setContentView 호출 (여기서 Fragment들이 생성될 수 있음)
        setContentView(R.layout.activity_main);

        // ✅ 4. tabs 배열 로드
        tabs = getGameTabs();
        Log.d(Constants.LOG_TAG, "탭 배열 로드됨 - 개수: " + (tabs != null ? tabs.length : 0));

        // ✅ 5. 다른 매니저들 초기화
        adMobManager = AdMobManager.getInstance(this);
        utilHelper = UtilHelper.getInstance(this);

        // ✅ 6. UI 컴포넌트 초기화
        initViews();
        createTabs();

        // ✅ 7. ViewPager 설정 (이제 GameListManager가 이미 설정되어 있음)
        Log.d(Constants.LOG_TAG, "setupViewPager 호출 전 GameListManager 상태: " +
                (gameListManager != null ? "정상" : "NULL"));
        setupViewPager();

        // 나머지 초기화...
        loadCollapsibleBannerIfEnabled();
        loadInterstitialAd();
        initializeAndCleanup();
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
     * ROMs 디렉토리 경로를 가져오는 메서드 (GameFragment와 동일한 로직)
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
            Log.d(Constants.LOG_TAG, "다운로드 상태 정리 시작");

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

                        String romFileName = key.replace("download_state_", "");
                        Log.d(Constants.LOG_TAG, "미완료 다운로드 상태 정리: " + romFileName);
                    }
                }
            }

            if (cleanedCount > 0) {
                editor.apply();
                Log.d(Constants.LOG_TAG, "다운로드 상태 " + cleanedCount + "개 정리됨");
            } else {
                Log.d(Constants.LOG_TAG, "정리할 다운로드 상태 없음");
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
        // ✅ GamePagerAdapter의 TABS 배열 길이와 일치시키기
        return 6; // {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"}

        // 또는 더 안전하게:
        // return tabs != null ? tabs.length : 6;
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

            tabViews[i] = tabText; // TextView 참조 저장
            tabLayout.addView(tabView);
        }

        updateTabSelection(0);
    }

    private void setupViewPager() {
        if (gameListManager == null) {
            Log.e(Constants.LOG_TAG, "GameListManager가 null입니다! ViewPager 설정 중단");
            return;
        }

        Log.d(Constants.LOG_TAG, "ViewPager 설정 시작 - GameListManager 정상");
        Log.d(Constants.LOG_TAG, "어댑터 생성 직전 GameListManager 상태: " +
                (gameListManager != null ? "정상" : "NULL"));

        // ✅ GameFragment에 정적으로 GameListManager 설정
        GameFragment.setGameListManager(gameListManager);

        GamePagerAdapter adapter = new GamePagerAdapter(
                getSupportFragmentManager(),
                getLifecycle(),
                gameListManager
        );

        Log.d(Constants.LOG_TAG, "어댑터 생성 완료");

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

        Log.d(Constants.LOG_TAG, "ViewPager 설정 완료 - GameListManager: " + (gameListManager != null));
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

                // 인디케이터 표시
                if (indicator != null) {
                    indicator.setVisibility(View.VISIBLE);
                }
            } else {
                // 선택되지 않은 탭: 희미한 흰색 + 인디케이터 숨김
                tabText.setTextColor(android.graphics.Color.argb(128, 255, 255, 255)); // 50% 투명도의 흰색
                tabText.setTextSize(16);
                tabText.setTypeface(null, android.graphics.Typeface.NORMAL);

                // 인디케이터 숨김
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
            Log.d(Constants.LOG_TAG, "배너 광고가 비활성화됨 - 광고 로드하지 않음");
            adContainerView.setVisibility(View.GONE);
            return;
        }

        Log.d(Constants.LOG_TAG, "배너 광고가 활성화됨 - 광고 로드 시작");
        loadCollapsibleBanner();
    }

    private void loadCollapsibleBanner() {
        if (adMobManager != null && adContainerView != null) {
            adMobManager.loadCollapsibleBanner(this, adContainerView, new AdMobManager.OnBannerAdLoadedListener() {
                @Override
                public void onAdLoaded(AdView adView) {
                    Log.d(Constants.LOG_TAG, "배너 광고 로드 성공");
                    // 광고 로드 성공 시 배너 컨테이너 표시
                    adContainerView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(Constants.LOG_TAG, "배너 광고 로드 실패: " + error);
                    // 광고 로드 실패 시 배너 컨테이너 숨김
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
                    Log.d(Constants.LOG_TAG, "전면광고 미리 로드 완료");
                }

                @Override
                public void onAdLoadFailed(String error) {
                    Log.e(Constants.LOG_TAG, "전면광고 미리 로드 실패: " + error);
                }

                @Override
                public void onAdClosed() {
                    Log.d(Constants.LOG_TAG, "전면광고 닫힌 후 자동으로 다음 광고 로드됨");
                }

                @Override
                public void onAdShown() {
                    Log.d(Constants.LOG_TAG, "전면광고 표시됨");
                }

                @Override
                public void onAdShowFailed(String error) {
                    Log.e(Constants.LOG_TAG, "전면광고 표시 실패: " + error);
                }
            });
        }
    }

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
            // 전면광고는 자동으로 해제되므로 별도 메서드 불필요
        }
    }
}