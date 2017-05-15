package com.bamless.chromiumsweupdater.updater;

import android.app.Activity;
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notification used to show download progress. To be used with an {@link okhttp3.OkHttpClient} using
 * a {@link ProgressResponseBody}.
 */
public class ProgressNotification implements ProgressResponseBody.ProgressListener {
    /**Update interval (in milliseconds)*/
    private static final int UPDATE_INTERVAL = 500;

    private Context ctx;
    private ServiceConnection connection;
    private NotificationManager notManager;
    private NotificationCompat.Builder notBuilder;

    private String title;
    private int notificationID;

    /**time at notification start. Used to calculate download rate*/
    private long startTime;
    /**time of last notification update. Used to decide when to update notification*/
    private long lastUpdate;

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
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.chromiumsweupdater64px))
                .setSmallIcon(R.mipmap.ic_update_black);
        notBuilder.setProgress(100, 0, false);
        //init the service used to dismiss the notification upon app closure
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

    /**Resets the state of the notification and shows it*/
    public void start() {
        startTime = System.currentTimeMillis();
        lastUpdate = 0;
        update(0, 0, false);
    }

    /**Dismiss the notification*/
    public void cancel() {
        notManager.cancel(notificationID);
    }

    @Override
    public void update(long bytesRead, long contentLength, boolean done) {
        int percent = (int) (((float) bytesRead / contentLength) * 100);
        long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        float downRate = elapsedTimeMillis == 0 ? 0 : ((float) bytesRead / (elapsedTimeMillis / 1000f));
        int timeRemaining = (int) ((double) (contentLength - bytesRead) / downRate);

        if(canUpdate()){
            notBuilder.setProgress(100, percent, false);
            notBuilder.setContentText(ctx.getString(R.string.progressNotText,
                    percent, formatDownloadRate(downRate), formatSeconds(timeRemaining)));
            notManager.notify(notificationID, notBuilder.build());
        }
        if(done) notManager.cancel(notificationID);
    }

    /**
     * Returns true if the notification should be updated. This limits the rate of update of the notification.
     * This is done both to increase performance (the creation of the notification takes time) and to
     * limit the updating of the text (if the text gets updated too frequently it gets difficult to read).
     */
    private boolean canUpdate() {
        long curr = System.currentTimeMillis();
        if(curr - lastUpdate >= UPDATE_INTERVAL) {
            lastUpdate = curr;
            return true;
        }
        return false;
    }

    /**
     * Formats the download rate.
     * @param bps the download rate in bytes per second
     * @return a string formatted with a different unit depending on the speed of the download rate
     */
    private String formatDownloadRate(float bps) {
        String rate;
        DecimalFormat decimal = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.getDefault()));
        DecimalFormat integer = new DecimalFormat("0");

        double kbps = bps/1024.0;
        double mbps = kbps/1024.0;

        if(mbps > 1) {
            rate = hasDecimal(mbps) ? decimal.format(mbps).concat(" MB/s") : integer.format(mbps).concat(" MB/s");
        } else if(kbps > 1) {
            rate = hasDecimal(kbps) ? decimal.format(kbps).concat(" KB/s") : integer.format(mbps).concat(" KB/s");
        } else {
            rate = integer.format(bps).concat(" B/s");
        }

        return rate;
    }

    /**
     * Formats seconds
     * @param seconds the number of seconds to format
     * @return a string formatted with a different unit depending on the number of seconds
     */
    private String formatSeconds(int seconds) {
        String time;
        DecimalFormat decimal = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.getDefault()));
        DecimalFormat integer = new DecimalFormat("0");

        float min = seconds / 60.0f;
        float hour = min / 60.0f;

        if(hour > 1) {
            time = hasDecimal(hour) ? decimal.format(hour).concat(" h") : integer.format(hour).concat(" h");
        } else if(min > 1) {
            time = hasDecimal(min) ? decimal.format(min).concat(" m") : integer.format(min).concat(" m");
        } else {
            time = integer.format(seconds).concat(" s");
        }

        return time;
    }

    /**Returns true if the input double has decimal figures*/
    private boolean hasDecimal(double num) {
        return !(num % 1 == 0.0);
    }

    /**Unbinds the kill notification service. This method should be called in {@link Activity#onDestroy()}*/
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
