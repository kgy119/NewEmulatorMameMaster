package com.ingcorp.webhard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ingcorp.webhard.database.GameDao;
import com.ingcorp.webhard.database.AppDatabase;
import com.ingcorp.webhard.database.GameEntity;
import com.ingcorp.webhard.adapters.GameGridAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AdView adView;
    private AppDatabase gameDatabase;
    private GameDao gameDao;
    private ExecutorService executor;

    // Category tab information
    private final String[] CATEGORIES = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};
    private final String[] CATEGORY_NAMES = {"All", "Fighting", "Action", "Shooting", "Sports", "Puzzle"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database
        gameDatabase = AppDatabase.getInstance(this);
        gameDao = gameDatabase.gameDao();
        executor = Executors.newFixedThreadPool(2);

        // Initialize views
        initViews();

        // Initialize AdMob
        initAdMob();

        // Setup tabs and ViewPager
        setupTabsAndViewPager();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        adView = findViewById(R.id.adView);
    }

    private void initAdMob() {
        // Initialize AdMob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // AdMob initialization complete
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void setupTabsAndViewPager() {
        GamePagerAdapter adapter = new GamePagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(CATEGORY_NAMES[position]);
        }).attach();
    }

    // Game execution method
    private void startGameActivity(GameEntity game) {
        try {
            Intent intent = new Intent(this, Class.forName("com.ingcorp.webhard.MAME4droid"));
            intent.putExtra("GAME_ROM", game.getGameRom());
            intent.putExtra("GAME_NAME", game.getGameName());
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "Game execution error occurred.", Toast.LENGTH_SHORT).show();
        }
    }

    // ViewPager2 adapter
    private class GamePagerAdapter extends FragmentStateAdapter {
        public GamePagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public Fragment createFragment(int position) {
            String category = CATEGORIES[position];
            return GameListFragment.newInstance(category);
        }

        @Override
        public int getItemCount() {
            return CATEGORIES.length;
        }
    }

    // Game list fragment
    public static class GameListFragment extends Fragment {
        private static final String ARG_CATEGORY = "category";
        private RecyclerView recyclerView;
        private GameGridAdapter adapter;
        private String category;

        public static GameListFragment newInstance(String category) {
            GameListFragment fragment = new GameListFragment();
            Bundle args = new Bundle();
            args.putString(ARG_CATEGORY, category);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                category = getArguments().getString(ARG_CATEGORY);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_game_list, container, false);

            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

            adapter = new GameGridAdapter(new ArrayList<>(), this::onGameClick);
            recyclerView.setAdapter(adapter);

            loadGames();

            return view;
        }

        private void loadGames() {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity == null) return;

            mainActivity.executor.execute(() -> {
                List<GameEntity> games;

                if ("ALL".equals(category)) {
                    // Get all games sorted by gameCount
                    games = mainActivity.gameDao.getGamesSorted("count");
                } else {
                    // Get games by specific category sorted by gameCount
                    games = mainActivity.gameDao.getGamesByCategorySorted(category, "count");
                }

                // Update adapter on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.updateGames(games);
                    });
                }
            });
        }

        private void onGameClick(GameEntity game) {
            // Handle game click
            Toast.makeText(getContext(), "Starting game: " + game.getGameName(), Toast.LENGTH_SHORT).show();

            // Increment game count
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.executor.execute(() -> {
                    mainActivity.gameDao.incrementGameCount(game.getGameRom());
                });

                // Start MAME4droid activity to run the game
                mainActivity.startGameActivity(game);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}