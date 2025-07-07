package com.ingcorp.webhard;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.HorizontalScrollView;
import android.graphics.Insets;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.Window;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainActivity extends FragmentActivity {

    private static final String[] TABS = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};
    private TextView[] tabViews;
    private ViewPager2 viewPager;
    private int selectedTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge 스타일 적용
        setupEdgeToEdge();

        // colorPrimary 및 colorPrimaryDark 색상 가져오기
        int colorPrimary = getResources().getColor(R.color.colorPrimary);
        int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark);
        int colorAccent = getResources().getColor(R.color.colorAccent);

        // 루트 레이아웃 생성
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(colorPrimary);

        // 레이아웃 파라미터 설정
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootLayout.setLayoutParams(rootParams);

        // 탭 컨테이너 생성
        LinearLayout tabContainer = createTabContainer(colorAccent);
        rootLayout.addView(tabContainer);

        // ViewPager2 생성
        viewPager = createViewPager2();
        rootLayout.addView(viewPager);

        // WindowInsets 처리
        setupWindowInsets(rootLayout);

        // 레이아웃을 액티비티의 콘텐츠 뷰로 설정
        setContentView(rootLayout);
    }

    /**
     * 탭 컨테이너 생성
     */
    private LinearLayout createTabContainer(int colorAccent) {
        // 탭 컨테이너
        LinearLayout tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.VERTICAL);
        tabContainer.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

        LinearLayout.LayoutParams tabContainerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabContainer.setLayoutParams(tabContainerParams);

        // 탭 스크롤뷰 (수평 스크롤)
        HorizontalScrollView tabScrollView = new HorizontalScrollView(this);
        tabScrollView.setHorizontalScrollBarEnabled(false);
        tabScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 탭 레이아웃 (탭들을 담는 컨테이너)
        LinearLayout tabLayout = new LinearLayout(this);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setPadding(20, 30, 20, 20);

        // 탭 뷰들 생성
        tabViews = new TextView[TABS.length];
        for (int i = 0; i < TABS.length; i++) {
            TextView tabView = createTabView(TABS[i], i, colorAccent);
            tabViews[i] = tabView;
            tabLayout.addView(tabView);

            // 탭 간격 추가 (마지막 탭 제외)
            if (i < TABS.length - 1) {
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                        30, ViewGroup.LayoutParams.MATCH_PARENT
                );
                spacer.setLayoutParams(spacerParams);
                tabLayout.addView(spacer);
            }
        }

        // 첫 번째 탭을 선택된 상태로 설정
        updateTabSelection(0);

        tabScrollView.addView(tabLayout);
        tabContainer.addView(tabScrollView);

        return tabContainer;
    }

    /**
     * 개별 탭 뷰 생성
     */
    private TextView createTabView(String tabText, int index, int colorAccent) {
        TextView tabView = new TextView(this);
        tabView.setText(tabText);
        tabView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tabView.setTextColor(getResources().getColor(R.color.white));
        tabView.setGravity(Gravity.CENTER);
        tabView.setPadding(30, 15, 30, 15);

        // 탭 레이아웃 파라미터
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tabView.setLayoutParams(tabParams);

        // 탭 클릭 리스너
        tabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(index);
            }
        });

        return tabView;
    }

    /**
     * ViewPager2 생성
     */
    private ViewPager2 createViewPager2() {
        ViewPager2 viewPager = new ViewPager2(this);

        LinearLayout.LayoutParams viewPagerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, // height
                1.0f // weight
        );
        viewPager.setLayoutParams(viewPagerParams);

        // ViewPager2 어댑터 설정
        GamePagerAdapter adapter = new GamePagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        // ViewPager2 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabSelection(position);
                scrollToSelectedTab(position);
            }
        });

        return viewPager;
    }

    /**
     * 탭 선택 처리
     */
    private void selectTab(int index) {
        if (selectedTabIndex != index) {
            viewPager.setCurrentItem(index, true);
        }
    }

    /**
     * 탭 선택 상태 업데이트
     */
    private void updateTabSelection(int index) {
        selectedTabIndex = index;
        int colorAccent = getResources().getColor(R.color.colorAccent);

        for (int i = 0; i < tabViews.length; i++) {
            TextView tabView = tabViews[i];
            if (i == index) {
                // 선택된 탭
                tabView.setBackgroundColor(colorAccent);
                tabView.setTextColor(getResources().getColor(R.color.white));
                tabView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            } else {
                // 선택되지 않은 탭
                tabView.setBackgroundColor(Color.TRANSPARENT);
                tabView.setTextColor(getResources().getColor(R.color.white));
                tabView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        }
    }

    /**
     * 선택된 탭으로 스크롤
     */
    private void scrollToSelectedTab(int index) {
        if (tabViews != null && index < tabViews.length) {
            TextView selectedTab = tabViews[index];
            ViewGroup parent = (ViewGroup) selectedTab.getParent();
            if (parent != null && parent.getParent() instanceof HorizontalScrollView) {
                HorizontalScrollView scrollView = (HorizontalScrollView) parent.getParent();

                // 선택된 탭을 중앙으로 스크롤
                int scrollX = selectedTab.getLeft() - (scrollView.getWidth() / 2) + (selectedTab.getWidth() / 2);
                scrollView.smoothScrollTo(Math.max(0, scrollX), 0);
            }
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
        public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                 android.view.ViewGroup container, Bundle savedInstanceState) {
            int position = getArguments() != null ? getArguments().getInt(ARG_POSITION, 0) : 0;

            // Fragment 레이아웃 생성
            LinearLayout fragmentLayout = new LinearLayout(getContext());
            fragmentLayout.setOrientation(LinearLayout.VERTICAL);
            fragmentLayout.setGravity(Gravity.CENTER);
            fragmentLayout.setBackgroundColor(getContext().getResources().getColor(R.color.colorPrimary));
            fragmentLayout.setPadding(40, 60, 40, 60);

            // 카테고리 제목
            TextView titleView = new TextView(getContext());
            titleView.setText(TABS[position] + " Games");
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            titleView.setTextColor(getContext().getResources().getColor(R.color.white));
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(20, 40, 20, 30);

            // 카테고리 설명
            TextView descriptionView = new TextView(getContext());
            descriptionView.setText(getTabDescription(position));
            descriptionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            descriptionView.setTextColor(getContext().getResources().getColor(R.color.white));
            descriptionView.setGravity(Gravity.CENTER);
            descriptionView.setPadding(20, 20, 20, 30);

            // 게임 목록 플레이스홀더
            TextView gameListView = new TextView(getContext());
            gameListView.setText(getGameListPlaceholder(position));
            gameListView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            gameListView.setTextColor(getContext().getResources().getColor(R.color.colorAccent));
            gameListView.setGravity(Gravity.CENTER);
            gameListView.setPadding(20, 20, 20, 40);

            fragmentLayout.addView(titleView);
            fragmentLayout.addView(descriptionView);
            fragmentLayout.addView(gameListView);

            return fragmentLayout;
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
        } else {
            setupEdgeToEdgeBase(window, decorView);
        }
    }

    private void setupEdgeToEdgeApi29(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
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
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi23(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi21(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);
    }

    private void setupEdgeToEdgeBase(Window window, View decorView) {
        // API 21 미만에서는 기본 동작
    }

    private void setDecorFitsSystemWindows(Window window, boolean decorFitsSystemWindows) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(decorFitsSystemWindows);
        } else {
            View decorView = window.getDecorView();
            if (!decorFitsSystemWindows) {
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    private void setupWindowInsets(LinearLayout rootLayout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return WindowInsets.CONSUMED;
                }
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    v.setPadding(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });
        }
    }
}