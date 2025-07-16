package com.ingcorp.webhard.base;

import android.app.Application;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.ingcorp.webhard.helpers.NotificationHelper;

public class AppBaseApplication extends Application {

    private static volatile AppBaseApplication instance = null;
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // Firebase 초기화 (가장 먼저)
        FirebaseApp.initializeApp(this);

        // 알림 도우미 초기화
        notificationHelper = new NotificationHelper(this);

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

    /**
     * 애플리케이션 인스턴스 반환
     */
    public static AppBaseApplication getInstance() {
        return instance;
    }

    /**
     * 알림 도우미 반환
     */
    public NotificationHelper getNotificationHelper() {
        return notificationHelper;
    }
}