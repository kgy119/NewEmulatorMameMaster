package com.ingcorp.webhard.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ingcorp.webhard.R;
import com.ingcorp.webhard.adapter.GameAdapter;
import com.ingcorp.webhard.database.entity.Game;
import com.ingcorp.webhard.manager.GameListManager;
import com.ingcorp.webhard.network.ApiService;
import com.ingcorp.webhard.network.NetworkClient;
import com.ingcorp.webhard.MAME4droid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private static final String[] CATEGORIES = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};

    private GameListManager gameListManager;
    private RecyclerView recyclerView;
    private GameAdapter gameAdapter;
    private List<Game> gameList;
    private View loadingView;
    private View emptyView;
    private boolean isDataLoaded = false;

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

        initViews(view);
        setupRecyclerView();
        loadGames(position);

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_games);
        loadingView = view.findViewById(R.id.loading_view);
        emptyView = view.findViewById(R.id.empty_view);
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        gameList = new ArrayList<>();
        gameAdapter = new GameAdapter(gameList, getContext());

        gameAdapter.setOnGameClickListener(new GameAdapter.OnGameClickListener() {
            @Override
            public void onGameClick(Game game) {
                checkRomAndLaunchGame(game);
            }

            @Override
            public void onGameLongClick(Game game) {
                showGameInfo(game);
            }
        });

        recyclerView.setAdapter(gameAdapter);
    }

    private void loadGames(int position) {
        if (isDataLoaded) return;

        showLoading(true);
        String category = CATEGORIES[position];

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
            gameList.clear();
            gameList.addAll(games);
            gameAdapter.notifyDataSetChanged();

            recyclerView.setVisibility(View.VISIBLE);
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        } else {
            gameList.clear();
            if (gameAdapter != null) gameAdapter.notifyDataSetChanged();

            recyclerView.setVisibility(View.GONE);
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
            if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        }
    }

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

    private void checkRomAndLaunchGame(Game game) {
        if (getContext() == null) return;

        String romFileName = game.getGameRom();
        if (romFileName == null || romFileName.isEmpty()) {
            showToast("ROM 파일 정보가 없습니다.");
            return;
        }

        String romsPath = getRomsPath();
        if (romsPath == null) {
            showToast("ROM 저장 경로를 찾을 수 없습니다.");
            return;
        }

        File romFile = new File(romsPath, romFileName);

        if (romFile.exists()) {
            launchGame(game, romFile.getAbsolutePath());
        } else {
            downloadAndLaunchGame(game, romFileName, romsPath);
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
            android.util.Log.e("GameFragment", "Error getting roms path", e);
            return null;
        }
    }

    private void downloadAndLaunchGame(Game game, String romFileName, String romsPath) {
        String downloadUrl = "http://retrogamemaster.net/r2/" + game.getGameRom();
        showToast("ROM 파일을 다운로드하는 중...");

        ApiService apiService = NetworkClient.getApiService();
        Call<ResponseBody> call = apiService.downloadRom(downloadUrl);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
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

    private void launchGame(Game game, String romFilePath) {
        try {
            android.content.Intent intent = new android.content.Intent(getContext(), MAME4droid.class);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.fromFile(new File(romFilePath)));

            intent.putExtra("game_name", game.getGameName());
            intent.putExtra("game_id", game.getGameId());

            showToast("게임을 시작합니다: " + game.getGameName());
            startActivity(intent);

        } catch (Exception e) {
            android.util.Log.e("GameFragment", "Error launching game", e);
            showToast("게임 실행에 실패했습니다: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
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