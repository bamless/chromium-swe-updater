package com.bamless.chromiumsweupdater;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bamless.chromiumsweupdater.updater.AlarmReceiver;
import com.bamless.chromiumsweupdater.updater.ChromiumUpdater;
import com.bamless.chromiumsweupdater.updater.ProgressNotification;
import com.bamless.chromiumsweupdater.utils.BuildTime;
import com.bamless.chromiumsweupdater.utils.Constants;
import com.bamless.chromiumsweupdater.utils.Prefs;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    public final static String TAG = "MainActivity";

    /**Permission request code*/
    public final static int REQUEST_EXTERNAL_WRITE = 1;
    /**Argument key. Boolean indicating wheter to reset the {@link com.bamless.chromiumsweupdater.updater.AlarmReceiver}*/
    public final static String ARG_START_ALARM_ON_OPEN = "startAlarmOnOpen";

    /**The progressNotification used to show download progress*/
    private ProgressNotification progressNotification;
    private ChromiumUpdater cu;
    private ImageButton checkUpdateButton;

    /**{@link android.view.View.OnClickListener} to start the update*/
    private View.OnClickListener startUpdateOnClickListener = new View.OnClickListener() {
        public void onClick(final View v) {
            v.setOnClickListener(null);
            progressNotification.start();

            cu.update(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), progressNotification, new ChromiumUpdater.ReturnCallback<Boolean>() {
                public void onReturn(Boolean returnValue) {
                    if(returnValue) {
                        //update succeeded, change status text to "no update available"
                        updateStatusText(false);
                    } else {
                        updateFailed();
                    }
                }
            });
        }
    };

    /**
     * Creates the intent to start the {@link android.app.Activity}
     * @param context the current {@link Context}
     * @param restartTimer whether the {@link android.app.Activity} should reset the {@link AlarmReceiver} at startup
     * @return the {@link Intent}
     */
    public static Intent createIntent(Context context, boolean restartTimer) {
        Bundle b = new Bundle();
        b.putBoolean(ARG_START_ALARM_ON_OPEN, restartTimer);
        Intent i = new Intent(context, MainActivity.class);
        i.putExtras(b);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cu = new ChromiumUpdater(this);
        progressNotification = new ProgressNotification(this, getString(R.string.updateNotificationText));

        //checks the external write permissions
        checkPermissions();
        //check arguments
        checkArguments();
        //creates and setup refresh button
        createCheckUpdateButton();
        //setup the status text with default value
        updateStatusText(false);
        //checks for an update at application start
        checkUpdateButton.performClick();
    }

    /**Setup the check update button*/
    private void createCheckUpdateButton() {
        checkUpdateButton = (ImageButton) findViewById(R.id.refreshButton);
        final RotateAnimation ranim = (RotateAnimation) AnimationUtils.loadAnimation(this, R.anim.rotate);

        //set listener to check for update
        checkUpdateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //start refresh button animation (if not already started)
                Animation anim = checkUpdateButton.getAnimation();
                if(anim == null || !anim.hasStarted())
                    checkUpdateButton.startAnimation(ranim);

                cu.checkForUpdate(new ChromiumUpdater.ReturnCallback<Boolean>() {
                    public void onReturn(Boolean returnValue) {
                        if(returnValue == null) {
                            Toast.makeText(MainActivity.this, R.string.updateFailed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updateStatusText(returnValue);
                        //stop animation
                        ranim.setRepeatCount(0);
                    }
                });
            }
        });
    }

    /**
     * Updates the status text
     * @param isUpdateAvailable whether or not there is a new update
     */
    private void updateStatusText(boolean isUpdateAvailable) {
        TextView updateStatusText = (TextView) findViewById(R.id.updateStatusText);

        //If there is a new build
        if(isUpdateAvailable) {
            //Update text with new build info, change color, add underline and set update listener
            SharedPreferences prefs = getSharedPreferences(Prefs.BUILD_PREFS, Context.MODE_PRIVATE);
            BuildTime last = BuildTime.parseBuildTime(prefs.getString(Prefs.BUILD_LASTBUILDFETCHED, Constants.EPOCH));

            String newBuildText = getResources().getString(R.string.newBuildText);
            updateStatusText.setText(newBuildText.replace("/date/", last.dateToString()));
            updateStatusText.setTextColor(Color.BLUE);
            updateStatusText.setPaintFlags(updateStatusText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            //set onclick listener to start update
            updateStatusText.setOnClickListener(startUpdateOnClickListener);
        } else {
            //There is no new build, reset color, underline and remove update listener
            updateStatusText.setText(R.string.noUpdateText);
            updateStatusText.setTextColor(Color.GRAY);
            updateStatusText.setPaintFlags(updateStatusText.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
        }
    }

    /**Checks for the WRITE permission on the external storage. If not present it asks for it*/
    private void checkPermissions() {
        int canRead =  ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(canRead != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_WRITE);
    }

    /**Checks the arguments passed via the bundle*/
    private void checkArguments() {
        Bundle extras = getIntent().getExtras();
        boolean restartAlarm = extras == null || extras.getBoolean(ARG_START_ALARM_ON_OPEN, true);
        Log.d(TAG, ARG_START_ALARM_ON_OPEN + ": " + restartAlarm);

        if(restartAlarm) startAlarm();
    }

    /**Restart {@link AlarmReceiver}*/
    private void  startAlarm() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, Constants.ALARM_HOUR);
        calendar.set(Calendar.MINUTE, Constants.ALARM_MINUTE);

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                Constants.DAY_INTERVAL, alarmIntent);

        Log.d(TAG, "started update alarm");
    }

    /**Called upon update failure*/
    private void updateFailed() {
        //the update failed, reset status text to last build available for download
        Toast.makeText(MainActivity.this, R.string.updateFailed, Toast.LENGTH_SHORT).show();
        updateStatusText(true);
        //dismiss progress notification
        progressNotification.cancel();
    }

    /**Returns the result of the permission request. If the WRITE permissions on external storage was
     * negated it stops the app*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQUEST_EXTERNAL_WRITE:
                //exit if the permission is not granted
                if(grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.permDeniedError, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        progressNotification.destroy();
        super.onDestroy();
    }
}
