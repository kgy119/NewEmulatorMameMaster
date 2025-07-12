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

        Log.d(TAG, "===== GamePagerAdapter 생성자 호출 =====");
        Log.d(TAG, "전달받은 GameListManager: " + (gameListManager != null ? "있음" : "NULL"));
        Log.d(TAG, "this.gameListManager 설정: " + (this.gameListManager != null ? "있음" : "NULL"));
        Log.d(TAG, "=========================================");
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "===== createFragment 호출 =====");
        Log.d(TAG, "요청된 위치: " + position);
        Log.d(TAG, "this.gameListManager: " + (this.gameListManager != null ? "있음" : "NULL"));

        GameFragment fragment = fragmentCache.get(position);
        if (fragment == null) {
            Log.d(TAG, "새 프래그먼트 생성 시작");

            // ✅ GameListManager 상태 확인 및 안전 처리
            GameListManager managerToUse = this.gameListManager;
            if (managerToUse == null) {
                Log.w(TAG, "⚠️ this.gameListManager가 null, 정적 변수 확인");
                // 정적 변수에서 가져오기 시도
                managerToUse = GameFragment.getStaticGameListManager();
                if (managerToUse != null) {
                    this.gameListManager = managerToUse;
                    Log.d(TAG, "✅ 정적 변수에서 GameListManager 복원");
                }
            }

            if (managerToUse != null) {
                Log.d(TAG, "✅ GameListManager 정상, 프래그먼트에 전달");
            } else {
                Log.e(TAG, "❌ GameListManager를 사용할 수 없음!");
            }

            fragment = GameFragment.newInstance(position, managerToUse);
            fragmentCache.put(position, fragment);

            Log.d(TAG, "프래그먼트 생성 완료 및 캐시 저장");
        } else {
            Log.d(TAG, "캐시된 프래그먼트 사용");
        }

        Log.d(TAG, "=================================");
        return fragment;
    }

    @Override
    public int getItemCount() {
        // ✅ 너무 많은 로그 출력 방지
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "getItemCount() 호출됨: " + TABS.length);
        }
        return TABS.length;
    }

    /**
     * GameListManager 업데이트 메서드
     */
    public void updateGameListManager(GameListManager gameListManager) {
        this.gameListManager = gameListManager;
        Log.d(TAG, "GameListManager 업데이트됨: " + (gameListManager != null ? "있음" : "NULL"));

        // 캐시된 프래그먼트들에도 업데이트 전파
        for (int i = 0; i < fragmentCache.size(); i++) {
            GameFragment fragment = fragmentCache.valueAt(i);
            if (fragment != null) {
                fragment.updateGameListManager(gameListManager);
            }
        }
    }
}