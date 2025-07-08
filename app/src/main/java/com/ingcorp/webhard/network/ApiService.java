package com.ingcorp.webhard.network;

import com.ingcorp.webhard.model.VersionResponse;
import com.ingcorp.webhard.model.GameListResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("app/version.json")
    Call<VersionResponse> getVersionInfo();

    @GET("app/list.json")
    Call<GameListResponse> getGameList();
}