package com.ing.clubdamoim.network;

import com.ing.clubdamoim.model.VersionResponse;
import com.ing.clubdamoim.model.GameListResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface ApiService {
    @GET("app/version.json")
    Call<VersionResponse> getVersionInfo();

    @GET("app/list.json")
    Call<GameListResponse> getGameList();

    // ROM 다운로드 추가
    @GET
    Call<ResponseBody> downloadRom(@Url String fileUrl);
}