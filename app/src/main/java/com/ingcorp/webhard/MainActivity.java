package com.ingcorp.webhard;

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
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.manager.AdMobManager;
import com.ingcorp.webhard.manager.GameListManager;

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

        gameListManager = new GameListManager(this);
        setContentView(R.layout.activity_main);

        // 리소스에서 탭 배열 로드
        tabs = getGameTabs();

        // AdMobManager 인스턴스 가져오기
        adMobManager = AdMobManager.getInstance(this);

        // UtilHelper 인스턴스 가져오기
        utilHelper = UtilHelper.getInstance(this);

        initViews();
        createTabs();
        setupViewPager();

        // 배너 광고 로드 (광고 설정에 따라)
        loadCollapsibleBannerIfEnabled();

        // 전면광고 미리 로드
        loadInterstitialAd();
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
        return tabs != null ? tabs.length : 0;
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
        GamePagerAdapter adapter = new GamePagerAdapter(getSupportFragmentManager(), getLifecycle(), gameListManager);
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