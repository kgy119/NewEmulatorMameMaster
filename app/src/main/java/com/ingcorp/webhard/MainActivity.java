package com.ingcorp.webhard;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ingcorp.webhard.adapter.GamePagerAdapter;
import com.ingcorp.webhard.manager.GameListManager;

public class MainActivity extends FragmentActivity {

    private static final String[] TABS = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};

    private TextView[] tabViews;
    private ViewPager2 viewPager;
    private HorizontalScrollView tabScrollView;
    private LinearLayout tabLayout;
    private int selectedTabIndex = 0;
    private GameListManager gameListManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameListManager = new GameListManager(this);
        setupEdgeToEdge();
        setContentView(R.layout.activity_main);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameListManager != null) {
            gameListManager.cleanup();
        }
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
}