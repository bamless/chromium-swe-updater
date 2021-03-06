package com.bamless.chromiumsweupdater;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bamless.chromiumsweupdater.models.BuildDate;
import com.bamless.chromiumsweupdater.network.ChromiumUpdater;
import com.bamless.chromiumsweupdater.receivers.AlarmReceiver;
import com.bamless.chromiumsweupdater.utils.Constants;
import com.bamless.chromiumsweupdater.views.AnimatedImageButton;
import com.bamless.chromiumsweupdater.views.ProgressNotification;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    public final static String TAG = MainActivity.class.getSimpleName();

    /**Permission request code*/
    public final static int REQUEST_EXTERNAL_WRITE = 1;
    /**Argument key. Boolean indicating whether to reset the {@link AlarmReceiver}*/
    public final static String ARG_START_ALARM_ON_OPEN = "startAlarmOnOpen";

    /**The progressNotification used to show download progress*/
    private ProgressNotification progressNotification;
    /**The ChromiumUupdater used to check and update Chromium SWE*/
    private ChromiumUpdater cu;
    /**The button that checks the update on click*/
    @BindView(R.id.checkUpdateButton)
    protected AnimatedImageButton checkUpdateButton;
    @BindView(R.id.updateStatusIcon)
    protected AnimatedImageButton updateStatusIcon;


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
        ButterKnife.bind(this);

        cu = new ChromiumUpdater(this);
        progressNotification = new ProgressNotification(this, getString(R.string.updateNotificationText));

        //checks the external write permissions
        checkPermissions();
        //check arguments
        checkArguments();
        //init the status text
        updateStatusText();
        //checks for update at application start
        checkUpdateButton.performClick();
    }

    @OnClick(R.id.checkUpdateButton)
    protected void checkUpdateOnClick(final AnimatedImageButton b) {
        setStatusText(getResources().getString(R.string.updateCheckText));
        b.setClickable(false);
        cu.checkForUpdate(new ChromiumUpdater.ReturnCallback<Boolean>() {
            public void onReturn(Boolean returnValue) {
                b.stopButtonAnimationSmooth();
                b.setButtonAnimationListener(setClickableOnAnimEndListener(b));
                if(returnValue == null) {
                    setStatusText(getResources().getString(R.string.updateFailed));
                } else {
                    updateStatusText();
                }
            }
        });
    }

    @OnClick(R.id.updateStatusIcon)
    protected void startUpdateOnClick(final AnimatedImageButton b) {
        b.setClickable(false);
        checkUpdateButton.setClickable(false);
        progressNotification.start();
        setStatusText(getString(R.string.updateDownloadingText));
        //start the actual update
        cu.update(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), progressNotification, new ChromiumUpdater.ReturnCallback<Boolean>() {
            public void onReturn(Boolean returnValue) {
                if(returnValue)
                    updateStatusText();
                else
                    updateFailed();
                b.stopButtonAnimationSmooth();
                checkUpdateButton.setClickable(true);
            }
        });
    }


    /**Sets the status text to the string passed as input*/
    private void setStatusText(String message) {
        TextView updateStatusText = ButterKnife.findById(this, R.id.updateStatusText);
        updateStatusText.setText(message);
    }

    /**Automatically set the status text by checking if a new build is present. If there is
     * then display the build info ad set the download button.*/
    private void updateStatusText() {
        TextView updateStatusText = ButterKnife.findById(this, R.id.updateStatusText);

        BuildDate curr = cu.getInstalledBuildDate();
        BuildDate last = cu.getLatestBuildDate();

        //If there is a new build
        if(curr.compareTo(last) < 0) {
            //Update text with new build info and set it bold
            updateStatusText.setText(getResources().getString(R.string.newBuildText, last.dateToString()));
            updateStatusText.setPaintFlags(updateStatusText.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
            //enable the button to start the update
            updateStatusIcon.setVisibility(View.VISIBLE);
            updateStatusIcon.setClickable(true);
        } else {
            //There is no new build, reset status text to "no new build" and remove bold
            updateStatusText.setText(R.string.noUpdateText);
            updateStatusText.setPaintFlags(updateStatusText.getPaintFlags() & (~ Paint.FAKE_BOLD_TEXT_FLAG));
            //disable the button that starts the update
            updateStatusIcon.setVisibility(View.GONE);
            updateStatusIcon.setClickable(false);
        }
    }

    /**Checks for the WRITE permission on the external storage. If not present it asks for it*/
    private void checkPermissions() {
        int canRead =  ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
        setStatusText(getResources().getString(R.string.updateFailedText));
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

    private Animation.AnimationListener setClickableOnAnimEndListener(final AnimatedImageButton b) {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                b.setClickable(true);
            }
            public void onAnimationStart(Animation animation) {}
            public void onAnimationRepeat(Animation animation) {}
        };
    }


    @Override
    protected void onDestroy() {
        progressNotification.destroy();
        super.onDestroy();
    }
}
