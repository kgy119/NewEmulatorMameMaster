package com.ing.clubdamoim.adapter;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ing.clubdamoim.R;
import com.ing.clubdamoim.fragment.GameFragment;
import com.ing.clubdamoim.manager.GameListManager;

public class GamePagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "GamePagerAdapter";
    private Context context;

    private GameListManager gameListManager;
    private SparseArray<GameFragment> fragmentCache = new SparseArray<>();

    public GamePagerAdapter(@NonNull FragmentManager fragmentManager,
                            @NonNull Lifecycle lifecycle,
                            GameListManager gameListManager,
                            Context context) {
        super(fragmentManager, lifecycle);
        this.gameListManager = gameListManager;
        this.context = context;
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
        if (context != null) {
            String[] categories = context.getResources().getStringArray(R.array.game_categories);
            return categories.length;
        }
        return 6; // fallback
    }


}