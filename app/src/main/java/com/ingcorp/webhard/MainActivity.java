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
import com.ingcorp.webhard.network.NetworkClient;
import com.ingcorp.webhard.network.ApiService;
import com.ingcorp.webhard.MAME4droid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                    // 게임 클릭 시 ROM 확인 후 실행
                    checkRomAndLaunchGame(game);
                }

                @Override
                public void onGameLongClick(Game game) {
                    // 게임 롱클릭 시 실행할 코드 (예: 상세 정보 표시)
                    showGameInfo(game);
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

        // 게임 정보 표시 (롱클릭)
        private void showGameInfo(Game game) {
            if (getContext() != null) {
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle(game.getGameName())
                        .setMessage("카테고리: " + game.getGameCate() + "\n" +
                                "ROM 파일: " + game.getGameRom() + "\n" +
                                "게임 ID: " + game.getGameId())
                        .setPositiveButton("확인", null)
                        .show();
            }
        }

        // ROM 확인 후 게임 실행
        private void checkRomAndLaunchGame(Game game) {
            if (getContext() == null) return;

            // ROM 파일 경로 확인
            String romFileName = game.getGameRom();
            if (romFileName == null || romFileName.isEmpty()) {
                showToast("ROM 파일 정보가 없습니다.");
                return;
            }

            // game_rom 값에 이미 .zip이 포함되어 있으므로 그대로 사용
            // 예: game_rom = "streetfighter.zip"

            // ROM 저장 경로 확인
            String romsPath = getRomsPath();
            if (romsPath == null) {
                showToast("ROM 저장 경로를 찾을 수 없습니다.");
                return;
            }

            File romFile = new File(romsPath, romFileName);

            if (romFile.exists()) {
                // ROM 파일이 있으면 바로 게임 실행
                launchGame(game, romFile.getAbsolutePath());
            } else {
                // ROM 파일이 없으면 다운로드 후 실행
                downloadAndLaunchGame(game, romFileName, romsPath);
            }
        }

        // ROM 저장 경로 가져오기
        private String getRomsPath() {
            try {
                // MAME4droid의 설치 경로에서 roms 폴더 경로 구성
                File appDir = getContext().getExternalFilesDir(null);
                if (appDir != null) {
                    File romsDir = new File(appDir, "roms");
                    if (!romsDir.exists()) {
                        romsDir.mkdirs(); // roms 폴더가 없으면 생성
                    }
                    return romsDir.getAbsolutePath();
                }

                // 대안 경로
                File fallbackDir = new File(getContext().getFilesDir(), "roms");
                if (!fallbackDir.exists()) {
                    fallbackDir.mkdirs();
                }
                return fallbackDir.getAbsolutePath();

            } catch (Exception e) {
                android.util.Log.e("GameFragment", "Error getting roms path", e);
                return null;
            }
        }

        // ROM 다운로드 후 게임 실행
        private void downloadAndLaunchGame(Game game, String romFileName, String romsPath) {
            // 다운로드 URL 구성
            String downloadUrl = "http://retrogamemaster.net/r2/" + game.getGameRom();

            showToast("ROM 파일을 다운로드하는 중...");

            // NetworkClient의 ApiService를 사용한 ROM 다운로드
            ApiService apiService = NetworkClient.getApiService();

            Call<ResponseBody> call = apiService.downloadRom(downloadUrl);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // 백그라운드에서 파일 저장
                        new Thread(() -> {
                            try {
                                File romFile = new File(romsPath, romFileName);
                                FileOutputStream fos = new FileOutputStream(romFile);

                                InputStream inputStream = response.body().byteStream();
                                byte[] buffer = new byte[4096];
                                int bytesRead;

                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }

                                fos.close();
                                inputStream.close();

                                // 메인 스레드에서 게임 실행
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        showToast("다운로드 완료! 게임을 시작합니다.");
                                        launchGame(game, romFile.getAbsolutePath());
                                    });
                                }

                            } catch (Exception e) {
                                android.util.Log.e("GameFragment", "Error saving ROM file", e);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            showToast("ROM 파일 저장에 실패했습니다."));
                                }
                            }
                        }).start();

                    } else {
                        showToast("ROM 다운로드에 실패했습니다. (응답 오류)");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    android.util.Log.e("GameFragment", "ROM download failed", t);
                    showToast("ROM 다운로드에 실패했습니다: " + t.getMessage());
                }
            });
        }

        // 게임 실행
        private void launchGame(Game game, String romFilePath) {
            try {
                // MAME4droid 액티비티로 인텐트 생성
                android.content.Intent intent = new android.content.Intent(getContext(), MAME4droid.class);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.fromFile(new File(romFilePath)));

                // 게임 정보 전달
                intent.putExtra("game_name", game.getGameName());
                intent.putExtra("game_id", game.getGameId());

                showToast("게임을 시작합니다: " + game.getGameName());
                startActivity(intent);

            } catch (Exception e) {
                android.util.Log.e("GameFragment", "Error launching game", e);
                showToast("게임 실행에 실패했습니다: " + e.getMessage());
            }
        }

        // 토스트 메시지 표시
        private void showToast(String message) {
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
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