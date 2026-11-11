package org.blasllavero.wifilert;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.concurrent.atomic.AtomicInteger;

final class Notifier {
    static final String MONITOR_CHANNEL = "monitor_v1";
    static final String ALERT_CHANNEL = "alerts_v1";
    static final int MONITOR_ID = 1;
    private static final AtomicInteger ids = new AtomicInteger(100);
    private Notifier() { }

    static void setupChannels(Context context) {
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel monitor = new NotificationChannel(MONITOR_CHANNEL,
                    context.getString(R.string.notif_monitor_channel), NotificationManager.IMPORTANCE_LOW);
            monitor.setShowBadge(false);
            mgr.createNotificationChannel(monitor);
            NotificationChannel alerts = new NotificationChannel(ALERT_CHANNEL,
                    context.getString(R.string.notif_alert_channel), NotificationManager.IMPORTANCE_HIGH);
            alerts.enableVibration(true);
            alerts.setVibrationPattern(new long[]{0, 350, 200, 350});
            mgr.createNotificationChannel(alerts);
        }
    }

    static Notification monitorNotification(Context context) {
        setupChannels(context);
        return base(context, MONITOR_CHANNEL)
                .setContentTitle(context.getString(R.string.notif_service_title))
                .setContentText(context.getString(R.string.notif_service_body))
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    static void sendAlert(Context context, int titleId, int messageId) {
        setupChannels(context);
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(ids.incrementAndGet(), base(context, ALERT_CHANNEL)
                .setContentTitle(context.getString(titleId))
                .setContentText(context.getString(messageId))
                .setStyle(new Notification.BigTextStyle().bigText(context.getString(messageId)))
                .setAutoCancel(true).setCategory(Notification.CATEGORY_STATUS)
                .setPriority(Notification.PRIORITY_HIGH)
                .build());
    }
    static void sendTest(Context context) { sendAlert(context, R.string.notif_test_title, R.string.notif_test_body); }

    private static Notification.Builder base(Context context, String channel) {
        Intent launch = new Intent(context, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(context, channel)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentIntent(pi)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
    }
}
