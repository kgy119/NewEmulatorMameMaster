package com.ingcorp.webhard.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private static final String TAG = "mame00";

    private GameListManager gameListManager;
    private RecyclerView recyclerView;
    private GameAdapter gameAdapter;
    private List<BaseItem> itemList;
    private View loadingView;
    private View emptyView;
    private boolean isDataLoaded = false;
    private UtilHelper utilHelper;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private int position;

    private android.app.AlertDialog customProgressDialog;
    private android.widget.ProgressBar customProgressBar;
    private android.widget.TextView progressText;

    private static GameListManager staticGameListManager;

    public static void setGameListManager(GameListManager manager) {
        staticGameListManager = manager;
    }

    public static GameFragment newInstance(int position, GameListManager gameListManager) {
        setGameListManager(gameListManager);

        GameFragment fragment = new GameFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
        }

        if (staticGameListManager != null) {
            this.gameListManager = staticGameListManager;
        } else {
            Log.e(TAG, "GameListManager null, attempting delayed recovery");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (staticGameListManager != null) {
                    this.gameListManager = staticGameListManager;
                    if (getView() != null && !isDataLoaded) {
                        loadGames();
                    }
                } else {
                    Log.e(TAG, "GameListManager recovery failed");
                }
            }, 100);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        utilHelper = UtilHelper.getInstance(getContext());
        initViews(view);
        setupRecyclerView();

        if (gameListManager != null) {
            loadGames();
        } else {
            showLoading(true);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (gameListManager != null || staticGameListManager != null) {
                    if (gameListManager == null) {
                        gameListManager = staticGameListManager;
                    }
                    loadGames();
                } else {
                    Log.e(TAG, "GameListManager unavailable, showing error");
                    showError();
                }
            }, 200);
        }

        return view;
    }

    private String getTabCategory(int position) {
        String[] categories = {"ALL", "FIGHT", "ACTION", "SHOOTING", "SPORTS", "PUZZLE"};
        return position < categories.length ? categories[position] : "UNKNOWN";
    }

    private String[] getGameCategories() {
        return getResources().getStringArray(R.array.game_categories);
    }

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

    private void loadGames() {
        if (isDataLoaded) return;

        if (gameListManager == null) {
            Log.e(TAG, "GameListManager null at position: " + position);
            showError();
            return;
        }

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

    private void showError() {
        showLoading(false);
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void updateGameList(List<Game> games) {
        if (games != null && !games.isEmpty()) {
            List<BaseItem> newItemList = createUnifiedListWithAds(games);
            gameAdapter.updateItemList(newItemList);

            recyclerView.setVisibility(View.VISIBLE);
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        } else {
            gameAdapter.updateItemList(new ArrayList<>());

            recyclerView.setVisibility(View.GONE);
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        }
    }

    private List<BaseItem> createUnifiedListWithAds(List<Game> games) {
        List<BaseItem> unifiedList = new ArrayList<>();
        int adNativeCnt = utilHelper.getAdNativeCount();

        if (adNativeCnt <= 0 || games.size() < 3) {
            for (Game game : games) {
                unifiedList.add(new GameItem(game));
            }
            return unifiedList;
        }

        Random random = new Random();

        for (int startIdx = 0; startIdx < games.size(); startIdx += adNativeCnt) {
            int endIdx = Math.min(startIdx + adNativeCnt, games.size());
            List<Game> currentGroup = games.subList(startIdx, endIdx);

            int groupSize = currentGroup.size();
            int adInsertPosition = -1;

            if (groupSize >= 3) {
                adInsertPosition = 1 + random.nextInt(groupSize - 2);
            }

            for (int i = 0; i < currentGroup.size(); i++) {
                if (i == adInsertPosition) {
                    unifiedList.add(new AdItem());
                }
                unifiedList.add(new GameItem(currentGroup.get(i)));
            }
        }

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
            Log.e(TAG, "Error getting roms path", e);
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
            Log.e(TAG, "Error launching game", e);
            showToast("Failed to launch game: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private boolean moveTemporaryFileToFinal(File tempFile, File finalFile) {
        try {
            File backupFile = null;
            if (finalFile.exists()) {
                backupFile = new File(finalFile.getAbsolutePath() + ".backup");
                if (finalFile.renameTo(backupFile)) {
                    Log.d(TAG, "Existing file backed up");
                } else {
                    Log.w(TAG, "Backup failed, attempting delete");
                    if (!finalFile.delete()) {
                        Log.e(TAG, "Failed to delete existing file");
                        return false;
                    }
                }
            }

            if (tempFile.renameTo(finalFile)) {
                if (backupFile != null && backupFile.exists()) {
                    backupFile.delete();
                }

                if (finalFile.exists() && finalFile.length() > 0) {
                    return true;
                } else {
                    Log.e(TAG, "Final file validation failed");
                    return false;
                }

            } else {
                Log.e(TAG, "File move failed");
                if (backupFile != null && backupFile.exists()) {
                    backupFile.renameTo(finalFile);
                }
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error moving file", e);
            return false;
        }
    }

    private boolean isRomFileValid(File romFile) {
        try {
            if (!romFile.exists()) {
                return false;
            }

            if (romFile.length() == 0) {
                Log.w(TAG, "ROM file is empty");
                return false;
            }

            if (!romFile.canRead()) {
                Log.w(TAG, "Cannot read ROM file");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error validating ROM file", e);
            return false;
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

        if (isRomFileValid(romFile)) {
            launchGame(game, romFile.getAbsolutePath());
        } else {
            if (romFile.exists()) {
                if (romFile.delete()) {
                    Log.d(TAG, "Corrupted ROM file deleted");
                } else {
                    Log.e(TAG, "Failed to delete corrupted ROM file");
                }
            }

            File tempFile = new File(romsPath, romFileName + ".tmp");
            if (tempFile.exists()) {
                if (tempFile.delete()) {
                    Log.d(TAG, "Existing temp file deleted");
                } else {
                    Log.e(TAG, "Failed to delete existing temp file");
                }
            }

            downloadAndLaunchGameWithProgress(game, romFileName, romsPath);
        }
    }

    private void showCustomProgressDialog(String gameName) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_download_progress, null);
            android.widget.TextView titleText = dialogView.findViewById(R.id.progress_title);
            customProgressBar = dialogView.findViewById(R.id.progress_bar);
            progressText = dialogView.findViewById(R.id.progress_text);

            titleText.setText(gameName);
            customProgressBar.setMax(100);
            customProgressBar.setProgress(0);
            progressText.setText("0 / 100");

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
            progressText.setText(progress + " / 100");
        }
    }

    private void hideCustomProgressDialog() {
        if (customProgressDialog != null && customProgressDialog.isShowing()) {
            customProgressDialog.dismiss();
            customProgressDialog = null;
            customProgressBar = null;
            progressText = null;
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

    @Override
    public void onResume() {
        super.onResume();
        if (!isDataLoaded) {
            loadGames();
        }
    }

    private boolean preDownloadCheck(Game game, String romFileName, String romsPath) {
        try {
            UtilHelper utilHelper = UtilHelper.getInstance(getContext());

            if (!utilHelper.isNetworkConnected()) {
                Log.w(TAG, "No network connection");
                utilHelper.showGameNetworkErrorDialog((android.app.Activity) getContext());
                return false;
            }

            String downloadState = utilHelper.getDownloadState(romFileName);
            if ("downloading".equals(downloadState)) {
                Log.w(TAG, "File already downloading: " + romFileName);
                showToast("This game is already being downloaded.");
                return false;
            }

            long estimatedSize = 50 * 1024 * 1024; // 50MB
            if (!utilHelper.hasEnoughDiskSpace(romsPath, estimatedSize)) {
                Log.w(TAG, "Insufficient disk space");
                showToast("Not enough storage space available.");
                return false;
            }

            utilHelper.saveDownloadState(romFileName, "downloading");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error in pre-download check", e);
            return false;
        }
    }

    private void downloadAndLaunchGameWithProgress(Game game, String romFileName, String romsPath) {
        if (!preDownloadCheck(game, romFileName, romsPath)) {
            return;
        }

        String downloadUrl = Constants.BASE_ROM_URL + game.getGameRom();
        String tempFileName = romFileName + ".tmp";
        File tempFile = new File(romsPath, tempFileName);
        File finalFile = new File(romsPath, romFileName);

        cleanupExistingTempFile(tempFile);
        showCustomProgressDialog(game.getGameName());

        ProgressInterceptor.ProgressListener progressListener = new ProgressInterceptor.ProgressListener() {
            @Override
            public void onProgress(long bytesRead, long contentLength, boolean done) {
                if (contentLength > 0) {
                    int progress = (int) ((bytesRead * 100) / contentLength);

                    mainHandler.post(() -> {
                        updateCustomProgress(progress);
                    });
                }
            }
        };

        ApiService progressApiService = NetworkClient.getProgressApiService(progressListener);
        Call<ResponseBody> call = progressApiService.downloadRom(downloadUrl);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> saveDownloadedFile(response, game, tempFile, finalFile, romFileName)).start();
                } else {
                    handleDownloadFailure(game, romFileName, "Server response error. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Download network error", t);
                handleDownloadFailure(game, romFileName, "Network error: " + t.getMessage());
            }
        });
    }

    private void cleanupExistingTempFile(File tempFile) {
        if (tempFile.exists()) {
            if (tempFile.delete()) {
                Log.d(TAG, "Existing temp file deleted");
            } else {
                Log.w(TAG, "Failed to delete existing temp file");
            }
        }
    }

    private void saveDownloadedFile(Response<ResponseBody> response, Game game, File tempFile, File finalFile, String romFileName) {
        FileOutputStream fos = null;
        InputStream inputStream = null;
        boolean downloadSuccess = false;
        long expectedSize = -1;
        long actualSize = 0;

        try {
            String contentLengthHeader = response.headers().get("Content-Length");
            if (contentLengthHeader != null) {
                expectedSize = Long.parseLong(contentLengthHeader);
            }

            fos = new FileOutputStream(tempFile);
            inputStream = response.body().byteStream();

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                actualSize += bytesRead;
            }

            fos.flush();

            if (expectedSize > 0 && actualSize != expectedSize) {
                throw new Exception("File size mismatch. Expected: " + expectedSize + ", Actual: " + actualSize);
            }

            if (!tempFile.exists() || tempFile.length() == 0) {
                throw new Exception("Temporary file is empty or does not exist");
            }

            downloadSuccess = true;

        } catch (Exception e) {
            Log.e(TAG, "Error saving file", e);
            handleDownloadFailure(game, romFileName, e.getMessage());
            cleanupExistingTempFile(tempFile);

        } finally {
            closeStream(fos);
            closeStream(inputStream);
        }

        if (downloadSuccess) {
            finalizeDownload(game, tempFile, finalFile, romFileName);
        }
    }

    private void closeStream(java.io.Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close stream", e);
            }
        }
    }

    private void finalizeDownload(Game game, File tempFile, File finalFile, String romFileName) {
        UtilHelper utilHelper = UtilHelper.getInstance(getContext());
        boolean moveSuccess = moveTemporaryFileToFinal(tempFile, finalFile);

        mainHandler.post(() -> {
            hideCustomProgressDialog();

            if (moveSuccess) {
                utilHelper.saveDownloadState(romFileName, "completed");
                launchGame(game, finalFile.getAbsolutePath());
            } else {
                utilHelper.saveDownloadState(romFileName, "failed");
                showDownloadErrorWithRetry(game, "Failed to finalize downloaded file");
            }
        });
    }

    private void handleDownloadFailure(Game game, String romFileName, String errorMessage) {
        UtilHelper utilHelper = UtilHelper.getInstance(getContext());
        utilHelper.saveDownloadState(romFileName, "failed");

        mainHandler.post(() -> {
            hideCustomProgressDialog();
            showDownloadErrorWithRetry(game, errorMessage);
        });
    }

    private void showDownloadErrorWithRetry(Game game, String error) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            UtilHelper utilHelper = UtilHelper.getInstance(getContext());

            utilHelper.showDownloadErrorDialog(
                    (android.app.Activity) getActivity(),
                    game.getGameName(),
                    error,
                    () -> {
                        Log.d(TAG, "Download retry for: " + game.getGameName());
                        checkRomAndLaunchGame(game);
                    }
            );
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    // Static GameListManager methods
    public static GameListManager getStaticGameListManager() {
        return staticGameListManager;
    }

    public void updateGameListManager(GameListManager gameListManager) {
        this.gameListManager = gameListManager;
        if (gameListManager != null && !isDataLoaded && getView() != null) {
            loadGames();
        }
    }

    public boolean isReady() {
        return gameListManager != null && getView() != null;
    }

    public void forceReload() {
        isDataLoaded = false;
        if (gameListManager != null) {
            loadGames();
        } else {
            showError();
        }
    }
}