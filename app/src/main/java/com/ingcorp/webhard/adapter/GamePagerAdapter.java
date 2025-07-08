package com.ingcorp.webhard.adapter;

import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ingcorp.webhard.fragment.GameFragment;
import com.ingcorp.webhard.manager.GameListManager;

public class GamePagerAdapter extends FragmentStateAdapter {
    private static final String[] TABS = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};

    private GameListManager gameListManager;
    private SparseArray<GameFragment> fragmentCache = new SparseArray<>();

    public GamePagerAdapter(@NonNull FragmentManager fragmentManager,
                            @NonNull Lifecycle lifecycle,
                            GameListManager gameListManager) {
        super(fragmentManager, lifecycle);
        this.gameListManager = gameListManager;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        GameFragment fragment = fragmentCache.get(position);
        if (fragment == null) {
            fragment = GameFragment.newInstance(position, gameListManager);
            fragmentCache.put(position, fragment);
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return TABS.length;
    }
}