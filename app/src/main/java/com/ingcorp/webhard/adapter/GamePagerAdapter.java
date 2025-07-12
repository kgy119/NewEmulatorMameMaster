package com.ingcorp.webhard.adapter;

import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ingcorp.webhard.fragment.GameFragment;
import com.ingcorp.webhard.manager.GameListManager;

public class GamePagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "GamePagerAdapter";
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
            // GameListManager 상태 확인 및 안전 처리
            GameListManager managerToUse = this.gameListManager;
            if (managerToUse == null) {
                // 정적 변수에서 가져오기 시도
                managerToUse = GameFragment.getStaticGameListManager();
                if (managerToUse != null) {
                    this.gameListManager = managerToUse;
                }
            }

            if (managerToUse == null) {
                Log.e(TAG, "GameListManager를 사용할 수 없음!");
            }

            fragment = GameFragment.newInstance(position, managerToUse);
            fragmentCache.put(position, fragment);
        }

        return fragment;
    }

    @Override
    public int getItemCount() {
        return TABS.length;
    }

    /**
     * GameListManager 업데이트 메서드
     */
    public void updateGameListManager(GameListManager gameListManager) {
        this.gameListManager = gameListManager;

        // 캐시된 프래그먼트들에도 업데이트 전파
        for (int i = 0; i < fragmentCache.size(); i++) {
            GameFragment fragment = fragmentCache.valueAt(i);
            if (fragment != null) {
                fragment.updateGameListManager(gameListManager);
            }
        }
    }
}