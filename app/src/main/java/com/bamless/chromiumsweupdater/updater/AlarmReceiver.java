package com.bamless.chromiumsweupdater.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver that catches the "check for update" alarm, and launches the
 * {@link CheckUpdateService} service.
 */
public class AlarmReceiver extends BroadcastReceiver {
    public final static String TAG = AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, CheckUpdateService.class);
        context.startService(i);
        Log.d(TAG, "alarm check update received");
    }
}
