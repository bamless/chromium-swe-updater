package com.bamless.chromiumsweupdater.updater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.bamless.chromiumsweupdater.http.ProgressResponseBody;
import com.bamless.chromiumsweupdater.utils.BuildTime;
import com.bamless.chromiumsweupdater.utils.Constants;
import com.bamless.chromiumsweupdater.utils.Prefs;

import java.io.File;
import java.io.FileNotFoundException;
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
    private final static String TAG = "ChromiumUpdater";

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
     * It checks if an update is available (asynchronously). The returncallback's method gets called
     * on the UI thread if the context passed at instantiation is an {@link Activity}.
     * @param returnCallback Callback for returning a value. It returns true if there is an update,
     *                       false if not. returns null if update failed.
     */
    public void checkForUpdate(final ReturnCallback<Boolean> returnCallback) {
        setProgressListener(null);
        Request request = new Request.Builder()
                .url(REPO + BUILD_FILE)
                .get().build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                returnOnUIThread(returnCallback, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                SharedPreferences prefs = context.getSharedPreferences(Prefs.BUILD_PREFS, Context.MODE_PRIVATE);
                BuildTime currBuild = BuildTime.parseBuildTime(prefs.
                        getString(Prefs.BUILD_LASTBUILDINST, Constants.EPOCH));
                BuildTime buildFromRepo = BuildTime.
                        parseBuildTime(response.body().string().replace("\n", ""));

                if(currBuild.compareTo(buildFromRepo) < 0) {
                    prefs.edit().putString(Prefs.BUILD_LASTBUILDFETCHED, buildFromRepo.toString()).apply();
                    returnOnUIThread(returnCallback, true);
                } else {
                    returnOnUIThread(returnCallback, false);
                }
            }
        });
    }

    /**
     * Downloads and install the Chromium SWE apk (asynchronously). returncallback's method gets called
     * on the UI thread if the context passed at instantiation is an {@link Activity}.
     * @param downloadPath The patch to which the apk will be downloaded
     * @param progressListener listener for the download progress
     * @param returnCallback callback for returning a value. It returns true if the download succeeded,
     *                       false if it failed.
     */
    public void update(final File downloadPath, ProgressResponseBody.ProgressListener progressListener,
                       final ReturnCallback<Boolean> returnCallback) {
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

                //update last installation time
                SharedPreferences prefs = context.getSharedPreferences(Prefs.BUILD_PREFS, Context.MODE_PRIVATE);
                prefs.edit().putString(Prefs.BUILD_LASTBUILDINST, prefs.
                        getString(Prefs.BUILD_LASTBUILDFETCHED, Constants.EPOCH)).apply();

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
        } else {
            uri = Uri.fromFile(apk);
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
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
