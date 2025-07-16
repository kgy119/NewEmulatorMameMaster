package com.ingcorp.webhard.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.ingcorp.webhard.MainActivity;
import com.ingcorp.webhard.R;

public class NotificationHelper {

    private static final String CHANNEL_NAME = "MAME Emulator";
    private static final String CHANNEL_DESCRIPTION = "MAME4droid notifications";
    private static final int NOTIFICATION_ID = 1001;

    private Context context;
    private NotificationManagerCompat notificationManager;
    private String channelId;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        // strings.xml에서 채널 ID 가져오기
        this.channelId = context.getString(R.string.default_notification_channel_id);
        createNotificationChannel();
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상에서 필수)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 알림 권한 확인
     */
    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 에서는 POST_NOTIFICATIONS 권한 확인
            return ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 13 미만에서는 알림이 활성화되어 있는지 확인
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    /**
     * 기본 알림 표시
     */
    public void showNotification(String title, String message) {
        // 권한 확인
        if (!hasNotificationPermission()) {
            Log.w("NotificationHelper", "알림 권한이 없어 알림을 표시할 수 없습니다.");
            return;
        }

        try {
            // SplashActivity로 이동하는 Intent 생성 (앱의 정상적인 진입점)
            Intent intent = new Intent(context, com.ingcorp.webhard.SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 알림 빌드
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            // 알림 표시
            notificationManager.notify(NOTIFICATION_ID, builder.build());

        } catch (SecurityException e) {
            Log.e("NotificationHelper", "알림 표시 중 보안 예외 발생", e);
        } catch (Exception e) {
            Log.e("NotificationHelper", "알림 표시 중 오류 발생", e);
        }
    }

    /**
     * 게임 관련 알림 표시
     */
    public void showGameNotification(String gameName, String status) {
        // 권한 확인
        if (!hasNotificationPermission()) {
            Log.w("NotificationHelper", "알림 권한이 없어 게임 알림을 표시할 수 없습니다.");
            return;
        }

        String title = "MAME4droid";
        String message = gameName + " " + status;

        showNotification(title, message);
    }

    /**
     * 진행률 알림 표시 (다운로드, 로딩 등)
     */
    public void showProgressNotification(String title, String message, int progress, int maxProgress) {
        // 권한 확인
        if (!hasNotificationPermission()) {
            Log.w("NotificationHelper", "알림 권한이 없어 진행률 알림을 표시할 수 없습니다.");
            return;
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setProgress(maxProgress, progress, false)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            notificationManager.notify(NOTIFICATION_ID + 1, builder.build());

        } catch (SecurityException e) {
            Log.e("NotificationHelper", "진행률 알림 표시 중 보안 예외 발생", e);
        } catch (Exception e) {
            Log.e("NotificationHelper", "진행률 알림 표시 중 오류 발생", e);
        }
    }

    /**
     * 모든 알림 제거
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }

    /**
     * 특정 알림 제거
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
}