package com.bamless.chromiumsweupdater.updater;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bamless.chromiumsweupdater.utils.Constants;

import java.util.Calendar;

/**
 * Starts the alarm for checking updates at device boot.
 */
public class BootReceiver extends BroadcastReceiver {
    public final static String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent i) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, Constants.ALARM_HOUR);
        calendar.set(Calendar.MINUTE, Constants.ALARM_MINUTE);

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                Constants.DAY_INTERVAL, alarmIntent);

        Log.d(TAG, "started update alarm");
    }
}
