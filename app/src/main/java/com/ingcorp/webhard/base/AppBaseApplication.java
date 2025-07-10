package com.ingcorp.webhard.base;

import android.app.Application;
import com.google.android.gms.ads.MobileAds;

public class AppBaseApplication extends Application {

    private static volatile AppBaseApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        MobileAds.initialize(this, AppBaseApplication -> { });

    }


    /**
     * 애플리케이션 종료시 singleton 어플리케이션 객체 초기화한다.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        instance = null;
    }
}
