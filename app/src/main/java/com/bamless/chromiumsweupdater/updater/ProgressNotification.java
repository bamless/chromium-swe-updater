package com.bamless.chromiumsweupdater.updater;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import com.bamless.chromiumsweupdater.R;
import com.bamless.chromiumsweupdater.http.ProgressResponseBody;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notification used to show download progress. To be used with an {@link okhttp3.OkHttpClient} using
 * a {@link ProgressResponseBody}.
 */
public class ProgressNotification implements ProgressResponseBody.ProgressListener {
    private Context ctx;
    private ServiceConnection connection;
    private NotificationManager notManager;
    private NotificationCompat.Builder notBuilder;

    private String title;
    private int notificationID;

    public ProgressNotification(Context ctx, String title) {
        this.ctx = ctx;
        this.title = title;
        this.notManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notBuilder = new NotificationCompat.Builder(ctx);
        notificationID = NotificationID.getUniqueNotificationID();
        init();
    }

    private void init() {
        notBuilder.setContentTitle(this.title)
                .setContentText("0%")
                .setOngoing(true)
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.chromiumswe64px))
                .setSmallIcon(R.mipmap.ic_update_black);
        notBuilder.setProgress(100, 0, false);
        connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                ((KillNotificationsService.KillBinder) binder).service.startService(new Intent(
                        ctx, KillNotificationsService.class));
            }

            public void onServiceDisconnected(ComponentName className) {
            }
        };
        ctx.bindService(new Intent(ctx, KillNotificationsService.class), connection,
                Context.BIND_AUTO_CREATE);
    }

    public void start() {
        update(0, 0, false);
    }

    public void cancel() {
        notManager.cancel(notificationID);
    }

    @Override
    public void update(long bytesRead, long contentLength, boolean done) {
        int percent = (int) (((float) bytesRead / contentLength) * 100);
        notBuilder.setProgress(100, percent, false);
        notBuilder.setContentText(percent + "%");
        notManager.notify(notificationID, notBuilder.build());
        if (done) notManager.cancel(notificationID);
    }

    public void destroy() {
        ctx.unbindService(connection);
    }

    /**Generates a unique ID for the notification*/
    private static class NotificationID {
        private final static AtomicInteger id = new AtomicInteger();
        public static int getUniqueNotificationID() {
            return id.incrementAndGet();
        }
    }
}
