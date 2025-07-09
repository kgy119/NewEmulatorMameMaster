package com.ingcorp.webhard;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.ingcorp.webhard.adapter.GameAdapter;
import com.ingcorp.webhard.adapter.GamePagerAdapter;
import com.ingcorp.webhard.manager.AdMobManager;
import com.ingcorp.webhard.manager.GameListManager;

public class MainActivity extends FragmentActivity {

    private String TAG = "mame00";
    private static final String[] TABS = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};

    private TextView[] tabViews;
    private ViewPager2 viewPager;
    private HorizontalScrollView tabScrollView;
    private LinearLayout tabLayout;
    private int selectedTabIndex = 0;
    private GameListManager gameListManager;

    private GameAdapter gameAdapter; // 클래스 멤버 변수로 선언
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameListManager = new GameListManager(this);
        setContentView(R.layout.activity_main);

        // AdMob 초기화 (앱 시작 시 한 번만)
        AdMobManager.getInstance(this).initialize(new AdMobManager.OnAdMobInitializedListener() {
            @Override
            public void onInitialized(boolean success) {
                if (success) {
                    Log.d(TAG, "AdMob 초기화 성공 - 광고를 로드할 준비가 완료됨");
                    // 어댑터가 이미 생성되어 있다면 갱신
                    refreshAdapterIfNeeded();
                } else {
                    Log.e(TAG, "AdMob 초기화 실패 - 광고가 표시되지 않음");
                }
            }
        });

        initViews();
        createTabs();
        setupViewPager();
    }

    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        tabScrollView = findViewById(R.id.tab_scroll_view);
        tabLayout = findViewById(R.id.tab_layout);
    }

    private void createTabs() {
        tabViews = new TextView[TABS.length];
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < TABS.length; i++) {
            View tabView = inflater.inflate(R.layout.tab_item, tabLayout, false);
            TextView tabText = tabView.findViewById(R.id.tab_text);
            tabText.setText(TABS[i]);

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

        viewPager.setOffscreenPageLimit(TABS.length);
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

    private void refreshAdapterIfNeeded() {
        // RecyclerView 어댑터가 있다면 갱신하여 광고 로드
        if (gameAdapter != null) {
            gameAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameListManager != null) {
            gameListManager.cleanup();
        }
    }

}