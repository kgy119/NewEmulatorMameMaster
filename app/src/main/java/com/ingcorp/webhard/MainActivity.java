package com.ingcorp.webhard;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends FragmentActivity {

    private static final String[] TABS = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};

    private TextView[] tabViews;
    private ViewPager2 viewPager;
    private HorizontalScrollView tabScrollView;
    private LinearLayout tabLayout;
    private int selectedTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge 설정
        setupEdgeToEdge();

        // 레이아웃 설정
        setContentView(R.layout.activity_main);

        // 뷰 초기화
        initViews();

        // 탭 생성
        createTabs();

        // ViewPager 설정
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
            TextView tabView = (TextView) inflater.inflate(R.layout.tab_item, tabLayout, false);
            tabView.setText(TABS[i]);

            final int index = i;
            tabView.setOnClickListener(v -> selectTab(index));

            tabViews[i] = tabView;
            tabLayout.addView(tabView);
        }

        // 첫 번째 탭 선택
        updateTabSelection(0);
    }

    private void setupViewPager() {
        GamePagerAdapter adapter = new GamePagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

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
        int colorAccent = getResources().getColor(R.color.colorAccent);

        for (int i = 0; i < tabViews.length; i++) {
            TextView tabView = tabViews[i];
            if (i == index) {
                // 선택된 탭
                tabView.setBackgroundColor(colorAccent);
                tabView.setTextSize(17);
            } else {
                // 선택되지 않은 탭
                tabView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                tabView.setTextSize(16);
            }
        }
    }

    private void scrollToSelectedTab(int index) {
        if (tabViews != null && index < tabViews.length) {
            TextView selectedTab = tabViews[index];
            int scrollX = selectedTab.getLeft() - (tabScrollView.getWidth() / 2) + (selectedTab.getWidth() / 2);
            tabScrollView.smoothScrollTo(Math.max(0, scrollX), 0);
        }
    }

    /**
     * ViewPager2 Fragment 어댑터
     */
    private static class GamePagerAdapter extends FragmentStateAdapter {

        public GamePagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return GameFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return TABS.length;
        }
    }

    /**
     * 게임 카테고리 Fragment
     */
    public static class GameFragment extends Fragment {
        private static final String ARG_POSITION = "position";

        public static GameFragment newInstance(int position) {
            GameFragment fragment = new GameFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_game, container, false);

            int position = getArguments() != null ? getArguments().getInt(ARG_POSITION, 0) : 0;

            TextView titleView = view.findViewById(R.id.title_text);
            TextView descriptionView = view.findViewById(R.id.description_text);
            TextView gameListView = view.findViewById(R.id.game_list_text);

            titleView.setText(TABS[position] + " Games");
            descriptionView.setText(getTabDescription(position));
            gameListView.setText(getGameListPlaceholder(position));

            return view;
        }

        private String getTabDescription(int tabIndex) {
            switch (tabIndex) {
                case 0: return "Browse all available arcade games\nin one convenient location.";
                case 1: return "Classic fighting games including\nStreet Fighter, King of Fighters, and more.";
                case 2: return "Action-packed adventures and\nplatform games for thrill seekers.";
                case 3: return "Shoot 'em up games and\nbullet hell classics.";
                case 4: return "Sports simulations including\nbaseball, football, and racing.";
                case 5: return "Brain teasers and puzzle games\nto challenge your mind.";
                default: return "Game category description";
            }
        }

        private String getGameListPlaceholder(int tabIndex) {
            switch (tabIndex) {
                case 0: return "• All Games (1000+)\n• Recently Added\n• Most Popular\n• Random Selection";
                case 1: return "• Street Fighter Series\n• King of Fighters\n• Tekken\n• Mortal Kombat";
                case 2: return "• Metal Slug Series\n• Contra\n• Double Dragon\n• Final Fight";
                case 3: return "• 1942, 1943, 1944\n• Gradius Series\n• R-Type\n• Galaga";
                case 4: return "• Track & Field\n• Baseball Stars\n• Goal! Goal! Goal!\n• Road Fighter";
                case 5: return "• Puzzle Bobble\n• Tetris\n• Columns\n• Money Puzzle";
                default: return "Game list loading...";
            }
        }
    }

    /**
     * EdgeToEdge 설정
     */
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