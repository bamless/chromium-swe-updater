package com.bamless.chromiumsweupdater.network;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.bamless.chromiumsweupdater.models.BuildDate;
import com.bamless.chromiumsweupdater.utils.Constants;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

/**
 * Class that implements logic for checking and downloading Chromium for SWE updates
 */
public class ChromiumUpdater {
    private final static String TAG = ChromiumUpdater.class.getSimpleName();

    /**Shared prefs name and shared prefs keys*/
    private static final String BUILD_PREFS = "buildPrefs";
    private static final String BUILD_LASTBUILD_INST = "lastbuild";
    private static final String BUILD_LASTBUILD_FETCHED = "lastbuildFetched";

    /**Base address*/
    private final static String REPO = "https://github.com/bamless/chromium-swe-builds/raw/master/";
    /**Name of the APK*/
    private final static String CHROMIUM_SWE_APK = "chromium-swe.apk";
    /**Name of the build file containing date and hour of last build*/
    private final static String BUILD_FILE = "build";

    /**Downloading progress listener*/
    private ProgressResponseBody.ProgressListener progressListener;
    private OkHttpClient http;
    private Context context;

    public ChromiumUpdater(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        http = new OkHttpClient.Builder().addNetworkInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder()
                        .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                        .build();
            }
        }).build();
    }

    /**
     * It checks if an update is available (asynchronously) from the repo updating the date of the
     * latest build available. The returncallback's method gets called on the UI thread if the context
     * passed at instan tiation is an {@link Activity}. This method should be called before
     * {@link ChromiumUpdater#update(File, ProgressResponseBody.ProgressListener, ReturnCallback)}
     * is called.
     * @param returnCallback Callback for returning a value. It returns true if there is an update,
     *                       false if not. returns null if check failed.
     * @see ChromiumUpdater#getLatestBuildDate()
     */
    public void checkForUpdate(final ReturnCallback<Boolean> returnCallback) {
        setProgressListener(null);
        Request request = new Request.Builder()
                .url(REPO + BUILD_FILE)
                .get().build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check the update", e);
                returnOnUIThread(returnCallback, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response == null) {
                    Log.e(TAG, "Failed to check the update");
                    returnOnUIThread(returnCallback, null);
                    return;
                }

                BuildDate currBuild = getInstalledBuildDate();
                BuildDate buildFromRepo = BuildDate.parseBuildTime(response.body().string().replace("\n", ""));

                if(currBuild.compareTo(buildFromRepo) < 0) {
                    setLatestBuildDate(buildFromRepo);
                    returnOnUIThread(returnCallback, true);
                } else {
                    returnOnUIThread(returnCallback, false);
                }
            }
        });
    }

    /**
     * Downloads and install the latest Chromium SWE apk (asynchronously). returncallback's method gets
     * called on the UI thread if the context passed at instantiation is an {@link Activity}.
     * If the latest build date fetched is not newer than the build installed, the function do not
     * execute and fails.
     * @param downloadPath The patch to which the apk will be downloaded
     * @param progressListener listener for the download progress
     * @param returnCallback callback for returning a value. It returns true if the update succeeded,
     *                       false if it failed.
     */
    public void update(final File downloadPath, ProgressResponseBody.ProgressListener progressListener,
                       final ReturnCallback<Boolean> returnCallback) {
        //stops if the latest build is not newer than the installed
        if(!(getInstalledBuildDate().compareTo(getLatestBuildDate()) < 0)) {
            returnCallback.onReturn(false);
            return;
        }

        setProgressListener(progressListener);
        Request request = new Request.Builder()
                            .url(REPO + CHROMIUM_SWE_APK)
                            .get().build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to download file: ", e);
                returnOnUIThread(returnCallback, false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.d(TAG,"Failed to download file: " + response);
                    returnOnUIThread(returnCallback, false);
                    return;
                }

                BufferedSink sink = null;
                try {
                    File out = new File(downloadPath, CHROMIUM_SWE_APK);
                    sink = Okio.buffer(Okio.sink(out));
                    sink.writeAll(response.body().source());
                } catch (IOException e) {
                    Log.e(TAG, "Error while writing the file", e);
                    returnOnUIThread(returnCallback, false);
                    return;
                } finally {
                    if(sink != null) sink.close();
                }

                installUpdate(downloadPath);

                //update last installation time and latest build time
                setInstalledBuildDate(getLatestBuildDate());

                returnOnUIThread(returnCallback, true);
            }
        });
    }

    /**Installs the update by invoking the default packet installer*/
    private void installUpdate(File downloadPath) {
        File apk = new File(downloadPath, ChromiumUpdater.CHROMIUM_SWE_APK);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        Uri uri;
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getApplicationContext()
                    .getPackageName() + ".provider", apk);
            context.grantUriPermission("com.android.packageinstaller", uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission("com.google.android.packageinstaller", uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apk);
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Returns the latest build time and date in a {@link BuildDate} object.
     * The {@link BuildDate} returned is the last date fetched from the repo by the last
     * {@link ChromiumUpdater#checkForUpdate(ReturnCallback)} call.
     * @return the latest build time and date in a {@link BuildDate} object.
     * @see ChromiumUpdater#update(File, ProgressResponseBody.ProgressListener, ReturnCallback)
     */
    public BuildDate getLatestBuildDate() {
        SharedPreferences prefs = context.getSharedPreferences(BUILD_PREFS, Context.MODE_PRIVATE);
        return BuildDate.parseBuildTime(prefs.getString(BUILD_LASTBUILD_FETCHED, Constants.EPOCH));
    }

    /**
     * @return the build time and date of the last build installed in a {@link BuildDate} object.
     */
    public BuildDate getInstalledBuildDate() {
        SharedPreferences prefs = context.getSharedPreferences(BUILD_PREFS, Context.MODE_PRIVATE);
        return BuildDate.parseBuildTime(prefs.getString(BUILD_LASTBUILD_INST, Constants.EPOCH));
    }

    private void setLatestBuildDate(BuildDate buildDate) {
        SharedPreferences prefs = context.getSharedPreferences(BUILD_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(BUILD_LASTBUILD_FETCHED, buildDate.toString()).apply();
    }

    private void setInstalledBuildDate(BuildDate buildDate) {
        SharedPreferences prefs = context.getSharedPreferences(BUILD_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(BUILD_LASTBUILD_INST, buildDate.toString()).apply();
    }

    /**
     * Runs the returncallback on the UI thread if the context passed at instantiation is a
     * {@link Activity}
     */
    private <T> void returnOnUIThread(final ReturnCallback<T> returnCallback, final T value) {
        if(context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    returnCallback.onReturn(value);
                }
            });
        } else {
            returnCallback.onReturn(value);
        }
    }

    private void setProgressListener(ProgressResponseBody.ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public interface ReturnCallback<T> {
        void onReturn(T returnValue);
    }

}
