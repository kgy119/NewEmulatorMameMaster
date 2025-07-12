package com.ingcorp.webhard.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ingcorp.webhard.R;
import com.ingcorp.webhard.adapter.GameAdapter;
import com.ingcorp.webhard.base.Constants;
import com.ingcorp.webhard.database.entity.Game;
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.manager.GameListManager;
import com.ingcorp.webhard.model.AdItem;
import com.ingcorp.webhard.model.BaseItem;
import com.ingcorp.webhard.model.GameItem;
import com.ingcorp.webhard.network.ApiService;
import com.ingcorp.webhard.network.NetworkClient;
import com.ingcorp.webhard.MAME4droid;
import com.ingcorp.webhard.network.ProgressInterceptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameFragment extends Fragment {
    private static final String ARG_POSITION = "position";

    private GameListManager gameListManager;
    private RecyclerView recyclerView;
    private GameAdapter gameAdapter;
    private List<BaseItem> itemList;
    private View loadingView;
    private View emptyView;
    private boolean isDataLoaded = false;
    private UtilHelper utilHelper;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private android.app.AlertDialog customProgressDialog;
    private android.widget.ProgressBar customProgressBar;
    private android.widget.TextView progressText;

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

        // UtilHelper 초기화
        utilHelper = UtilHelper.getInstance(getContext());

        initViews(view);
        setupRecyclerView();
        loadGames(position);

        return view;
    }

    /**
     * 게임 카테고리 배열을 가져오는 헬퍼 메서드
     */
    private String[] getGameCategories() {
        return getResources().getStringArray(R.array.game_categories);
    }

    /**
     * 특정 위치의 게임 카테고리를 가져오는 헬퍼 메서드
     */
    private String getCategoryByPosition(int position) {
        String[] categories = getGameCategories();
        return (position >= 0 && position < categories.length) ? categories[position] : categories[0];
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_games);
        loadingView = view.findViewById(R.id.loading_view);
        emptyView = view.findViewById(R.id.empty_view);
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);

        // 광고와 게임 모두 같은 크기(1칸)로 설정
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });

        recyclerView.setLayoutManager(gridLayoutManager);

        itemList = new ArrayList<>();
        gameAdapter = new GameAdapter(itemList, getContext());

        gameAdapter.setOnGameClickListener(new GameAdapter.OnGameClickListener() {
            @Override
            public void onGameClick(Game game) {
                checkRomAndLaunchGame(game);
            }

            @Override
            public void onGameLongClick(Game game) {
                // showGameInfo(game);
            }
        });

        recyclerView.setAdapter(gameAdapter);
    }

    private void loadGames(int position) {
        if (isDataLoaded) return;

        showLoading(true);
        String category = getCategoryByPosition(position);

        gameListManager.getGamesByCategory(category, new GameListManager.GameListListener() {
            @Override
            public void onGamesLoaded(List<Game> games) {
                updateGameList(games);
                showLoading(false);
                isDataLoaded = true;
            }
        });
    }

    private void updateGameList(List<Game> games) {
        if (games != null && !games.isEmpty()) {
            // 게임 리스트에 네이티브 광고를 삽입하여 통합 리스트 생성
            List<BaseItem> newItemList = createUnifiedListWithAds(games);

            // 어댑터의 updateItemList 메서드 사용
            gameAdapter.updateItemList(newItemList);

            recyclerView.setVisibility(View.VISIBLE);
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        } else {
            // 빈 리스트로 업데이트
            gameAdapter.updateItemList(new ArrayList<>());

            recyclerView.setVisibility(View.GONE);
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 게임 리스트에 네이티브 광고를 삽입하여 통합 리스트를 생성
     */
    private List<BaseItem> createUnifiedListWithAds(List<Game> games) {
        List<BaseItem> unifiedList = new ArrayList<>();

        // 네이티브 광고 주기 가져오기
        int adNativeCnt = utilHelper.getAdNativeCount();

        Log.d(Constants.LOG_TAG, "=== 통합 리스트 생성 시작 ===");
        Log.d(Constants.LOG_TAG, "게임 수: " + games.size());
        Log.d(Constants.LOG_TAG, "광고 주기: " + adNativeCnt);

        if (adNativeCnt <= 0 || games.size() < 3) {
            // 광고 주기가 0 이하이거나 게임이 3개 미만이면 광고 없이 게임만 추가
            for (Game game : games) {
                unifiedList.add(new GameItem(game));
            }
            Log.d(Constants.LOG_TAG, "광고 없이 게임만 추가 (게임: " + games.size() + "개)");
            return unifiedList;
        }

        Random random = new Random();

        for (int startIdx = 0; startIdx < games.size(); startIdx += adNativeCnt) {
            int endIdx = Math.min(startIdx + adNativeCnt, games.size());

            // 현재 그룹의 게임들을 추가
            List<Game> currentGroup = games.subList(startIdx, endIdx);

            // 광고를 삽입할 랜덤 위치 계산 (첫번째, 마지막 제외)
            int groupSize = currentGroup.size();
            int adInsertPosition = -1;

            if (groupSize >= 3) {
                // 1부터 groupSize-2 사이의 랜덤 위치 (첫번째=0, 마지막=groupSize-1 제외)
                adInsertPosition = 1 + random.nextInt(groupSize - 2);
            }

            Log.d(Constants.LOG_TAG, "그룹 [" + startIdx + "-" + (endIdx-1) + "], 크기: " + groupSize +
                    ", 광고 삽입 위치: " + adInsertPosition);

            // 그룹의 게임들을 순서대로 추가하면서 중간에 광고 삽입
            for (int i = 0; i < currentGroup.size(); i++) {
                // 광고 삽입 위치에 도달하면 광고 먼저 추가
                if (i == adInsertPosition) {
                    unifiedList.add(new AdItem());
                    Log.d(Constants.LOG_TAG, "위치 " + unifiedList.size() + "에 광고 삽입");
                }

                // 게임 추가
                unifiedList.add(new GameItem(currentGroup.get(i)));
            }
        }

        Log.d(Constants.LOG_TAG, "최종 통합 리스트 크기: " + unifiedList.size());

        // 디버그: 리스트 구성 출력
        for (int i = 0; i < unifiedList.size(); i++) {
            BaseItem item = unifiedList.get(i);
            String type = item.getItemType() == BaseItem.TYPE_AD ? "광고" : "게임";
            Log.d(Constants.LOG_TAG, "위치 " + i + ": " + type);
        }

        Log.d(Constants.LOG_TAG, "=========================");

        return unifiedList;
    }

    private void showLoading(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
            if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        }
    }

    private String getRomsPath() {
        try {
            File appDir = getContext().getExternalFilesDir(null);
            if (appDir != null) {
                File romsDir = new File(appDir, "roms");
                if (!romsDir.exists()) romsDir.mkdirs();
                return romsDir.getAbsolutePath();
            }

            File fallbackDir = new File(getContext().getFilesDir(), "roms");
            if (!fallbackDir.exists()) fallbackDir.mkdirs();
            return fallbackDir.getAbsolutePath();

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Error getting roms path", e);
            return null;
        }
    }

    private void launchGame(Game game, String romFilePath) {
        try {
            android.content.Intent intent = new android.content.Intent(getContext(), MAME4droid.class);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.fromFile(new File(romFilePath)));

            intent.putExtra("game_name", game.getGameName());
            intent.putExtra("game_id", game.getGameId());

            showToast("Starting game: " + game.getGameName());
            startActivity(intent);

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Error launching game", e);
            showToast("Failed to launch game: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAndLaunchGameWithProgress(Game game, String romFileName, String romsPath) {
        String downloadUrl = Constants.BASE_ROM_URL + game.getGameRom();

        // 커스텀 프로그레스 다이얼로그 표시
        showCustomProgressDialog(game.getGameName());

        // 프로그레스 리스너 생성
        ProgressInterceptor.ProgressListener progressListener = new ProgressInterceptor.ProgressListener() {
            @Override
            public void onProgress(long bytesRead, long contentLength, boolean done) {
                if (contentLength > 0) {
                    int progress = (int) ((bytesRead * 100) / contentLength);

                    // UI 스레드에서 프로그레스 업데이트
                    mainHandler.post(() -> {
                        updateCustomProgress(progress);

                        if (done) {
                            Log.d(Constants.LOG_TAG, "Download completed via Retrofit Progress");
                        }
                    });
                }
            }
        };

        // 프로그레스 지원 API 서비스 생성
        ApiService progressApiService = NetworkClient.getProgressApiService(progressListener);
        Call<ResponseBody> call = progressApiService.downloadRom(downloadUrl);

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

                            // UI 스레드에서 완료 처리
                            mainHandler.post(() -> {
                                hideCustomProgressDialog();
                                launchGame(game, romFile.getAbsolutePath());
                            });

                        } catch (Exception e) {
                            Log.e(Constants.LOG_TAG, "Error saving ROM file", e);
                            mainHandler.post(() -> {
                                hideCustomProgressDialog(); // 수정: 커스텀 다이얼로그 숨김
                                showDownloadError(e.getMessage());
                            });
                        }
                    }).start();
                } else {
                    hideCustomProgressDialog(); // 수정: 커스텀 다이얼로그 숨김
                    showDownloadError("Server response error");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(Constants.LOG_TAG, "ROM download failed", t);
                hideCustomProgressDialog(); // 수정: 커스텀 다이얼로그 숨김
                showDownloadError(t.getMessage());
            }
        });
    }

    private void showCustomProgressDialog(String gameName) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_download_progress, null);
            android.widget.TextView titleText = dialogView.findViewById(R.id.progress_title);
            customProgressBar = dialogView.findViewById(R.id.progress_bar);
            progressText = dialogView.findViewById(R.id.progress_text); // 진행률 텍스트 참조 추가

            titleText.setText(gameName); // "Downloading" 제거하고 게임이름만
            customProgressBar.setMax(100);
            customProgressBar.setProgress(0);
            progressText.setText("0 / 100"); // 초기 진행률 설정

            customProgressDialog = new android.app.AlertDialog.Builder(getActivity(), R.style.DialogTheme)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            customProgressDialog.show();
        }
    }


    private void updateCustomProgress(int progress) {
        if (customProgressBar != null && progressText != null) {
            customProgressBar.setProgress(progress);
            progressText.setText(progress + " / 100"); // 진행률 텍스트 업데이트
        }
    }


    private void hideCustomProgressDialog() {
        if (customProgressDialog != null && customProgressDialog.isShowing()) {
            customProgressDialog.dismiss();
            customProgressDialog = null;
            customProgressBar = null;
            progressText = null; // 참조 정리
        }
    }

    private void showDownloadError(String error) {
        if (getActivity() != null) {
            utilHelper.showCustomDialog(
                    (android.app.Activity) getActivity(),
                    "Download Failed",
                    "Failed to download ROM file.\nError: " + error,
                    "OK",
                    null,
                    null,
                    null
            );
        }
    }

    private void checkRomAndLaunchGame(Game game) {
        if (getContext() == null) return;

        String romFileName = game.getGameRom();
        if (romFileName == null || romFileName.isEmpty()) {
            showToast("ROM file information not available.");
            return;
        }

        String romsPath = getRomsPath();
        if (romsPath == null) {
            showToast("Cannot find ROM storage path.");
            return;
        }

        File romFile = new File(romsPath, romFileName);

        if (romFile.exists()) {
            Log.d(Constants.LOG_TAG, "ROM file exists, launching game directly");
            launchGame(game, romFile.getAbsolutePath());
        } else {
            Log.d(Constants.LOG_TAG, "ROM file not found, starting Retrofit progress download");
            downloadAndLaunchGameWithProgress(game, romFileName, romsPath);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isDataLoaded && getArguments() != null) {
            int position = getArguments().getInt(ARG_POSITION, 0);
            loadGames(position);
        }
    }
}