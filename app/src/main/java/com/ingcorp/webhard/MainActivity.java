package com.ingcorp.webhard;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.ingcorp.webhard.database.entity.Game;
import com.ingcorp.webhard.manager.GameListManager;
import com.ingcorp.webhard.adapter.GameAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String[] TABS = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};
    private static final String[] CATEGORIES = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};

    private TextView[] tabViews;
    private ViewPager2 viewPager;
    private HorizontalScrollView tabScrollView;
    private LinearLayout tabLayout;
    private int selectedTabIndex = 0;
    private GameListManager gameListManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // GameListManager 초기화
        gameListManager = new GameListManager(this);

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
        GamePagerAdapter adapter = new GamePagerAdapter(getSupportFragmentManager(), getLifecycle(), gameListManager);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameListManager != null) {
            gameListManager.cleanup();
        }
    }

    /**
     * ViewPager2 Fragment 어댑터
     */
    private static class GamePagerAdapter extends FragmentStateAdapter {
        private GameListManager gameListManager;

        public GamePagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, GameListManager gameListManager) {
            super(fragmentManager, lifecycle);
            this.gameListManager = gameListManager;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return GameFragment.newInstance(position, gameListManager);
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

        private GameListManager gameListManager;
        private RecyclerView recyclerView;
        private GameAdapter gameAdapter;
        private List<Game> gameList;
        private View loadingView;
        private View emptyView;

        public static GameFragment newInstance(int position, GameListManager gameListManager) {
            GameFragment fragment = new GameFragment();
            fragment.gameListManager = gameListManager;
            Bundle args = new Bundle();
            args.putInt(ARG_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_game, container, false);

            int position = getArguments() != null ? getArguments().getInt(ARG_POSITION, 0) : 0;

            // 뷰 초기화
            initViews(view);

            // RecyclerView 설정
            setupRecyclerView();

            // 게임 목록 로드
            loadGames(position);

            return view;
        }

        private void initViews(View view) {
            recyclerView = view.findViewById(R.id.recycler_view_games);
            loadingView = view.findViewById(R.id.loading_view);
            emptyView = view.findViewById(R.id.empty_view);

            // 디버깅용 로그
            android.util.Log.d("GameFragment", "initViews - recyclerView: " + (recyclerView != null));
            android.util.Log.d("GameFragment", "initViews - loadingView: " + (loadingView != null));
            android.util.Log.d("GameFragment", "initViews - emptyView: " + (emptyView != null));
        }

        private void setupRecyclerView() {
            // 2열 그리드 레이아웃 매니저 설정
            androidx.recyclerview.widget.GridLayoutManager gridLayoutManager =
                    new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2);
            recyclerView.setLayoutManager(gridLayoutManager);

            gameList = new ArrayList<>();
            gameAdapter = new GameAdapter(gameList, getContext());

            // 게임 클릭 리스너 설정
            gameAdapter.setOnGameClickListener(new GameAdapter.OnGameClickListener() {
                @Override
                public void onGameClick(Game game) {
                    // 게임 클릭 시 실행할 코드
                    // 예: 게임 실행 액티비티로 이동
                    // Intent intent = new Intent(getContext(), GameActivity.class);
                    // intent.putExtra("game_id", game.getGameId());
                    // startActivity(intent);
                }

                @Override
                public void onGameLongClick(Game game) {
                    // 게임 롱클릭 시 실행할 코드 (예: 상세 정보 표시)
                }
            });

            recyclerView.setAdapter(gameAdapter);
        }

        // setupUI 메서드와 getTabDescription 메서드 제거

        private void loadGames(int position) {
            // 로딩 상태 표시
            showLoading(true);

            String category = CATEGORIES[position];

            gameListManager.getGamesByCategory(category, new GameListManager.GameListListener() {
                @Override
                public void onGamesLoaded(List<Game> games) {
                    android.util.Log.d("GameFragment", "Games loaded for category " + category + ": " +
                            (games == null ? "null" : games.size() + " games"));
                    updateGameList(games);
                    showLoading(false);
                }
            });
        }

        private void updateGameList(List<Game> games) {
            // 디버깅용 로그
            android.util.Log.d("GameFragment", "updateGameList called with " +
                    (games == null ? "null" : games.size() + " games"));

            if (games != null && !games.isEmpty()) {
                // 게임이 있는 경우
                gameList.clear();
                gameList.addAll(games);
                gameAdapter.notifyDataSetChanged();

                // RecyclerView만 표시
                recyclerView.setVisibility(View.VISIBLE);
                if (loadingView != null) loadingView.setVisibility(View.GONE);
                if (emptyView != null) emptyView.setVisibility(View.GONE);

                android.util.Log.d("GameFragment", "Showing RecyclerView only");
            } else {
                // 빈 목록 처리
                gameList.clear();
                if (gameAdapter != null) {
                    gameAdapter.notifyDataSetChanged();
                }

                // EmptyView만 표시
                recyclerView.setVisibility(View.GONE);
                if (loadingView != null) loadingView.setVisibility(View.GONE);
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                    android.util.Log.d("GameFragment", "Showing EmptyView only");
                } else {
                    android.util.Log.d("GameFragment", "EmptyView is null!");
                }
            }
        }

        private void showLoading(boolean show) {
            if (show) {
                // 로딩 중: 모든 뷰 숨김
                recyclerView.setVisibility(View.GONE);
                if (emptyView != null) emptyView.setVisibility(View.GONE);
                if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
            }
            // 로딩 완료는 updateGameList에서 처리
        }



        // Fragment가 다시 보여질 때 데이터 새로고침
        @Override
        public void onResume() {
            super.onResume();
            if (getArguments() != null && gameListManager != null) {
                int position = getArguments().getInt(ARG_POSITION, 0);
                loadGames(position);
            }
        }

        // updateGameCount 메서드와 사용하지 않는 변수들 제거
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