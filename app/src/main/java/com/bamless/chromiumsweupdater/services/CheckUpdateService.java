package com.bamless.chromiumsweupdater.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.bamless.chromiumsweupdater.network.ChromiumUpdater;
import com.bamless.chromiumsweupdater.MainActivity;
import com.bamless.chromiumsweupdater.R;

/**
 * Service that checks if an update is available, and notifies if it is.
 */
public class CheckUpdateService extends Service {
    public final static String TAG = CheckUpdateService.class.getSimpleName();
    /**The notification's ID*/
    private final static int NOTID = 500;
    private ChromiumUpdater updater;

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        updater = new ChromiumUpdater(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updater.checkForUpdate(new ChromiumUpdater.ReturnCallback<Boolean>() {
            @Override
            public void onReturn(Boolean returnValue) {
                if(returnValue)
                    showUpdateNotification();
            }
        });
        Log.d(TAG, "notify update");
        return Service.START_STICKY;
    }

    private void showUpdateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = MainActivity.createIntent(this, false);
        PendingIntent intent = PendingIntent.getActivity(this, 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this);
        notBuilder.setContentTitle(getString(R.string.newUpdateNotificationText))
                .setContentText(getString(R.string.newUpdateNotificationContentText))
                .setSmallIcon(R.mipmap.ic_update_black)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.chromiumsweupdater))
                .setDefaults(-1 )
                .setAutoCancel(true)
                .setContentIntent(intent);

        nm.notify(NOTID, notBuilder.build());
    }
}
