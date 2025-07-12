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

    /**
     * 임시 파일을 최종 파일로 안전하게 이동하는 메서드
     */
    private boolean moveTemporaryFileToFinal(File tempFile, File finalFile) {
        try {
            Log.d(Constants.LOG_TAG, "임시 파일을 최종 파일로 이동 시작");
            Log.d(Constants.LOG_TAG, "임시 파일: " + tempFile.getAbsolutePath() + " (크기: " + tempFile.length() + ")");
            Log.d(Constants.LOG_TAG, "최종 파일: " + finalFile.getAbsolutePath());

            // 최종 파일이 이미 존재한다면 백업 생성
            File backupFile = null;
            if (finalFile.exists()) {
                backupFile = new File(finalFile.getAbsolutePath() + ".backup");
                if (finalFile.renameTo(backupFile)) {
                    Log.d(Constants.LOG_TAG, "기존 파일을 백업으로 이동: " + backupFile.getAbsolutePath());
                } else {
                    Log.w(Constants.LOG_TAG, "기존 파일 백업 실패, 삭제 시도");
                    if (!finalFile.delete()) {
                        Log.e(Constants.LOG_TAG, "기존 파일 삭제 실패");
                        return false;
                    }
                }
            }

            // 임시 파일을 최종 파일로 이동
            if (tempFile.renameTo(finalFile)) {
                Log.d(Constants.LOG_TAG, "파일 이동 성공");

                // 백업 파일이 있다면 삭제
                if (backupFile != null && backupFile.exists()) {
                    if (backupFile.delete()) {
                        Log.d(Constants.LOG_TAG, "백업 파일 삭제됨");
                    } else {
                        Log.w(Constants.LOG_TAG, "백업 파일 삭제 실패: " + backupFile.getAbsolutePath());
                    }
                }

                // 최종 파일 검증
                if (finalFile.exists() && finalFile.length() > 0) {
                    Log.d(Constants.LOG_TAG, "최종 파일 검증 성공 - 크기: " + finalFile.length());
                    return true;
                } else {
                    Log.e(Constants.LOG_TAG, "최종 파일 검증 실패");
                    return false;
                }

            } else {
                Log.e(Constants.LOG_TAG, "파일 이동 실패");

                // 이동 실패시 백업 파일 복원 시도
                if (backupFile != null && backupFile.exists()) {
                    if (backupFile.renameTo(finalFile)) {
                        Log.d(Constants.LOG_TAG, "백업 파일 복원됨");
                    } else {
                        Log.e(Constants.LOG_TAG, "백업 파일 복원 실패");
                    }
                }
                return false;
            }

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "파일 이동 중 예외 발생", e);
            return false;
        }
    }

    /**
     * ROM 파일 존재 여부를 더 안전하게 확인하는 메서드
     */
    private boolean isRomFileValid(File romFile) {
        try {
            if (!romFile.exists()) {
                Log.d(Constants.LOG_TAG, "ROM 파일이 존재하지 않음: " + romFile.getAbsolutePath());
                return false;
            }

            if (romFile.length() == 0) {
                Log.w(Constants.LOG_TAG, "ROM 파일이 비어있음: " + romFile.getAbsolutePath());
                return false;
            }

            if (!romFile.canRead()) {
                Log.w(Constants.LOG_TAG, "ROM 파일을 읽을 수 없음: " + romFile.getAbsolutePath());
                return false;
            }

            Log.d(Constants.LOG_TAG, "ROM 파일 유효성 검사 통과: " + romFile.getAbsolutePath() + " (크기: " + romFile.length() + ")");
            return true;

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "ROM 파일 유효성 검사 중 오류", e);
            return false;
        }
    }

    /**
     * 수정된 checkRomAndLaunchGame 메서드
     */
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

        // ROM 파일의 유효성을 더 엄격하게 검사
        if (isRomFileValid(romFile)) {
            Log.d(Constants.LOG_TAG, "유효한 ROM 파일 발견, 게임 직접 실행");
            launchGame(game, romFile.getAbsolutePath());
        } else {
            // 유효하지 않은 파일이 있다면 삭제하고 다시 다운로드
            if (romFile.exists()) {
                Log.w(Constants.LOG_TAG, "유효하지 않은 ROM 파일 삭제: " + romFile.getAbsolutePath());
                if (romFile.delete()) {
                    Log.d(Constants.LOG_TAG, "손상된 ROM 파일 삭제됨");
                } else {
                    Log.e(Constants.LOG_TAG, "손상된 ROM 파일 삭제 실패");
                }
            }

            // 임시 파일도 확인하고 있다면 삭제
            File tempFile = new File(romsPath, romFileName + ".tmp");
            if (tempFile.exists()) {
                Log.w(Constants.LOG_TAG, "기존 임시 파일 발견, 삭제: " + tempFile.getAbsolutePath());
                if (tempFile.delete()) {
                    Log.d(Constants.LOG_TAG, "기존 임시 파일 삭제됨");
                } else {
                    Log.e(Constants.LOG_TAG, "기존 임시 파일 삭제 실패");
                }
            }

            Log.d(Constants.LOG_TAG, "ROM 파일을 새로 다운로드 시작");
            downloadAndLaunchGameWithProgress(game, romFileName, romsPath);
        }
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


    @Override
    public void onResume() {
        super.onResume();
        if (!isDataLoaded && getArguments() != null) {
            int position = getArguments().getInt(ARG_POSITION, 0);
            loadGames(position);
        }
    }

    /**
     * 다운로드 시작 전 사전 검사를 수행하는 메서드
     */
    private boolean preDownloadCheck(Game game, String romFileName, String romsPath) {
        try {
            UtilHelper utilHelper = UtilHelper.getInstance(getContext());

            // 1. 네트워크 연결 확인
            if (!utilHelper.isNetworkConnected()) {
                Log.w(Constants.LOG_TAG, "네트워크 연결 없음");
                utilHelper.showGameNetworkErrorDialog((android.app.Activity) getContext());
                return false;
            }

            // 2. 이미 다운로드 중인지 확인
            String downloadState = utilHelper.getDownloadState(romFileName);
            if ("downloading".equals(downloadState)) {
                Log.w(Constants.LOG_TAG, "이미 다운로드 중인 파일: " + romFileName);
                showToast("This game is already being downloaded.");
                return false;
            }

            // 3. 디스크 공간 확인 (예상 크기 50MB로 가정)
            long estimatedSize = 50 * 1024 * 1024; // 50MB
            if (!utilHelper.hasEnoughDiskSpace(romsPath, estimatedSize)) {
                Log.w(Constants.LOG_TAG, "디스크 공간 부족");
                showToast("Not enough storage space available.");
                return false;
            }

            // 4. 다운로드 상태를 downloading으로 설정
            utilHelper.saveDownloadState(romFileName, "downloading");

            Log.d(Constants.LOG_TAG, "다운로드 사전 검사 통과: " + romFileName);
            return true;

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "다운로드 사전 검사 중 오류", e);
            return false;
        }
    }

    /**
     * 개선된 다운로드 메서드 - 에러 처리 및 재시도 기능 강화
     */
    private void downloadAndLaunchGameWithProgress(Game game, String romFileName, String romsPath) {
        // 사전 검사 수행
        if (!preDownloadCheck(game, romFileName, romsPath)) {
            return;
        }

        String downloadUrl = Constants.BASE_ROM_URL + game.getGameRom();

        // 임시 파일 경로 생성
        String tempFileName = romFileName + ".tmp";
        File tempFile = new File(romsPath, tempFileName);
        File finalFile = new File(romsPath, romFileName);

        // 기존 임시 파일 정리
        cleanupExistingTempFile(tempFile);

        // 커스텀 프로그레스 다이얼로그 표시
        showCustomProgressDialog(game.getGameName());

        // 프로그레스 리스너 생성
        ProgressInterceptor.ProgressListener progressListener = new ProgressInterceptor.ProgressListener() {
            @Override
            public void onProgress(long bytesRead, long contentLength, boolean done) {
                if (contentLength > 0) {
                    int progress = (int) ((bytesRead * 100) / contentLength);

                    mainHandler.post(() -> {
                        updateCustomProgress(progress);

                        if (done) {
                            Log.d(Constants.LOG_TAG, "다운로드 완료 신호 수신");
                        }
                    });
                }
            }
        };

        // API 서비스 생성 및 다운로드 시작
        ApiService progressApiService = NetworkClient.getProgressApiService(progressListener);
        Call<ResponseBody> call = progressApiService.downloadRom(downloadUrl);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 백그라운드에서 파일 저장
                    new Thread(() -> saveDownloadedFile(response, game, tempFile, finalFile, romFileName)).start();
                } else {
                    handleDownloadFailure(game, romFileName, "Server response error. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(Constants.LOG_TAG, "다운로드 네트워크 오류", t);
                handleDownloadFailure(game, romFileName, "Network error: " + t.getMessage());
            }
        });
    }

    /**
     * 기존 임시 파일을 정리하는 메서드
     */
    private void cleanupExistingTempFile(File tempFile) {
        if (tempFile.exists()) {
            if (tempFile.delete()) {
                Log.d(Constants.LOG_TAG, "기존 임시 파일 삭제됨: " + tempFile.getName());
            } else {
                Log.w(Constants.LOG_TAG, "기존 임시 파일 삭제 실패: " + tempFile.getName());
            }
        }
    }

    /**
     * 다운로드된 파일을 저장하는 메서드
     */
    private void saveDownloadedFile(Response<ResponseBody> response, Game game, File tempFile, File finalFile, String romFileName) {
        FileOutputStream fos = null;
        InputStream inputStream = null;
        boolean downloadSuccess = false;
        long expectedSize = -1;
        long actualSize = 0;

        try {
            // Content-Length 확인
            String contentLengthHeader = response.headers().get("Content-Length");
            if (contentLengthHeader != null) {
                expectedSize = Long.parseLong(contentLengthHeader);
                Log.d(Constants.LOG_TAG, "예상 파일 크기: " + formatBytes(expectedSize));
            }

            // 임시 파일에 저장
            fos = new FileOutputStream(tempFile);
            inputStream = response.body().byteStream();

            byte[] buffer = new byte[8192];
            int bytesRead;

            Log.d(Constants.LOG_TAG, "임시 파일에 저장 시작: " + tempFile.getAbsolutePath());

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                actualSize += bytesRead;
            }

            fos.flush();

            // 파일 크기 검증
            if (expectedSize > 0 && actualSize != expectedSize) {
                throw new Exception("File size mismatch. Expected: " + expectedSize + ", Actual: " + actualSize);
            }

            // 임시 파일 유효성 검증
            if (!tempFile.exists() || tempFile.length() == 0) {
                throw new Exception("Temporary file is empty or does not exist");
            }

            Log.d(Constants.LOG_TAG, "다운로드 완료 - 크기: " + formatBytes(actualSize));
            downloadSuccess = true;

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "파일 저장 중 오류", e);
            handleDownloadFailure(game, romFileName, e.getMessage());

            // 실패한 임시 파일 정리
            cleanupExistingTempFile(tempFile);

        } finally {
            // 리소스 정리
            closeStream(fos);
            closeStream(inputStream);
        }

        // 성공시 파일 이동 및 게임 시작
        if (downloadSuccess) {
            finalizeDownload(game, tempFile, finalFile, romFileName);
        }
    }

    /**
     * 스트림을 안전하게 닫는 헬퍼 메서드
     */
    private void closeStream(java.io.Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, "스트림 닫기 실패", e);
            }
        }
    }

    /**
     * 다운로드 완료 후 파일 이동 및 게임 시작 처리
     */
    private void finalizeDownload(Game game, File tempFile, File finalFile, String romFileName) {
        UtilHelper utilHelper = UtilHelper.getInstance(getContext());
        boolean moveSuccess = moveTemporaryFileToFinal(tempFile, finalFile);

        mainHandler.post(() -> {
            hideCustomProgressDialog();

            if (moveSuccess) {
                // 다운로드 상태를 완료로 설정
                utilHelper.saveDownloadState(romFileName, "completed");

                Log.d(Constants.LOG_TAG, "ROM 다운로드 및 설치 완료: " + finalFile.getAbsolutePath());
                launchGame(game, finalFile.getAbsolutePath());
            } else {
                // 파일 이동 실패
                utilHelper.saveDownloadState(romFileName, "failed");
                showDownloadErrorWithRetry(game, "Failed to finalize downloaded file");
            }
        });
    }

    /**
     * 다운로드 실패 처리 메서드
     */
    private void handleDownloadFailure(Game game, String romFileName, String errorMessage) {
        UtilHelper utilHelper = UtilHelper.getInstance(getContext());

        // 다운로드 상태를 실패로 설정
        utilHelper.saveDownloadState(romFileName, "failed");

        mainHandler.post(() -> {
            hideCustomProgressDialog();
            showDownloadErrorWithRetry(game, errorMessage);
        });
    }

    /**
     * 재시도 옵션이 있는 다운로드 에러 다이얼로그
     */
    private void showDownloadErrorWithRetry(Game game, String error) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            UtilHelper utilHelper = UtilHelper.getInstance(getContext());

            utilHelper.showDownloadErrorDialog(
                    (android.app.Activity) getActivity(),
                    game.getGameName(),
                    error,
                    () -> {
                        // 재시도 콜백
                        Log.d(Constants.LOG_TAG, "다운로드 재시도: " + game.getGameName());
                        checkRomAndLaunchGame(game);
                    }
            );
        }
    }

    /**
     * 바이트를 읽기 쉬운 형태로 포맷하는 헬퍼 메서드
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}