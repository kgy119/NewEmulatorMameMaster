package com.ing.clubdamoim.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ing.clubdamoim.R;
import com.ing.clubdamoim.base.Constants;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = Constants.LOG_TAG;
    private String channelId;

    @Override
    public void onCreate() {
        super.onCreate();
        // strings.xml에서 채널 ID 가져오기
        channelId = getString(R.string.default_notification_channel_id);
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM 메시지 수신: " + remoteMessage.getFrom());

        // 알림 데이터 추출
        String title = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getTitle() : "Retrobit";
        String body = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getBody() : "";

        // 알림 표시
        showNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "새로운 FCM 토큰: " + token);

        // 필요시 서버에 토큰 전송
        // sendTokenToServer(token);
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Retrobit";
            String description = "Retrobit message";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 알림 표시
     */
    private void showNotification(String title, String body) {
        // SplashActivity로 이동하는 Intent (앱의 정상적인 진입점)
        Intent intent = new Intent(this, com.ing.clubdamoim.SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알림 빌드 (strings.xml의 채널 ID 사용)
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setVibrate(new long[]{100, 200, 300, 400, 500});

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }
}