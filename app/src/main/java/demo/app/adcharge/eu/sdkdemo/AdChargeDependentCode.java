package demo.app.adcharge.eu.sdkdemo;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.adcharge.api.ApiException;
import eu.adcharge.api.ApiValidationException;
import eu.adcharge.api.entities.User;
import eu.adcharge.sdk.logic.AdCharge;

class AdChargeDependentCode {
    private static final String URL = BuildConfig.SERVER_URL; // URL
    private static final String TRAFFIC_SOURCE_KEY = BuildConfig.INDIVIDUAL_KEY;  // Individual key of traffic source
    // # Settings to configure SDK
    static AdCharge.Settings ADCHARGE_SETTINGS = new AdCharge.Settings()
            // # Ask user for location, use location for targeting
            .useLocation(false)
            //       or .useLocation(true)
            .showAdsOnCall(true)
            //       or .showAdsOnCall(false)
            // # Configuration of in call small banner
            .smallBanner(
                    new AdCharge.Settings.SmallBanner()
                            // # show or do not show small banner
                            .enable(true)
                            //       or .enable(false)
                            //
                            // # initial placement on screen
                            .initialPosition(AdCharge.Settings.SmallBanner.InitialPosition.MIDDLE)
                            //       or .initialPosition(AdCharge.Settings.SmallBanner.InitialPosition.TOP)
                            //       or .initialPosition(AdCharge.Settings.SmallBanner.InitialPosition.BOTTOM)
                            //
                            // # chose draggable area
                            .dragSensitiveArea(AdCharge.Settings.SmallBanner.DragSensitiveArea.HOLE_BANNER)
                            //       or .dragSensitiveArea(AdCharge.Settings.SmallBanner.DragSensitiveArea.DRAG_ICON)
                            //       or .dragSensitiveArea(AdCharge.Settings.SmallBanner.DragSensitiveArea.NONE)
                            //
                            // # display or hide 'drag icon'
                            .dragIconDisplayed(false)
                    //       or .dragIconDisplayed(true)
            )
            // # Configuration of in call large banner
            .largeBanner(
                    new AdCharge.Settings.LargeBanner()
                            .bonusPointsBalanceDisplayed(false)
                    //       or .bonusPointsBalanceDisplayed(true)
            );


    static List<String> getAdChargeCredentialsForUniqueUser(String login) {
        List<String> credentials = new ArrayList<>();
        credentials.add(login);
        credentials.add("verySecureUserPassword"); // some generated pass (might be hashed login)
        return credentials;
    }

    static void initAdCharge(final Activity activity) {
        try {
            final AdCharge adCharge = new AdCharge(URL, activity, ADCHARGE_SETTINGS);
            ((Application) activity.getApplication()).setAdCharge(adCharge);
        } catch (MalformedURLException ignored) {
        }
    }

    static void deactivateAdCharge(AdCharge adCharge) {
        if (adCharge != null) {
            new LogoutUserTask(adCharge).execute();
        }
    }


    static class LoginUserTask extends AsyncTask<Object, Object, Object> {
        private String username;
        private String password;
        private Activity activity;
        private AdCharge adCharge;

        LoginUserTask(String username, String password, Activity activity, AdCharge adCharge) {
            this.username = username;
            this.password = password;
            this.activity = activity;
            this.adCharge = adCharge;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                adCharge.login(username, password, TRAFFIC_SOURCE_KEY, activity);
            } catch (ApiException | IOException | ApiValidationException e) {
                User toBeRegistered = new User();
                toBeRegistered.setUsername(username);
                toBeRegistered.setPassword(password);
                try {
                    adCharge.registerSubscriberUser(toBeRegistered, TRAFFIC_SOURCE_KEY);
                    adCharge.login(username, password, TRAFFIC_SOURCE_KEY, activity);
                } catch (ApiException | IOException | ApiValidationException e1) {
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }

    static class LogoutUserTask extends AsyncTask<Object, Object, Object> {
        private AdCharge adCharge;

        LogoutUserTask(AdCharge adCharge) {
            this.adCharge = adCharge;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            adCharge.logout();
            return null;
        }
    }

    /**
     * Equivalent to {@link AsyncTask}, but also adds a timeout.  If the task exceeds the timeout,
     * {@link AsyncTaskWithTimeout#onTimeout()} will be called via the UI {@link Thread}.
     */
    public abstract static class AsyncTaskWithTimeout<Params, Progress, Result>
            extends AsyncTask<Params, Progress, Result> {
        private final long timeout;
        private final TimeUnit units;
        private final Activity context;

        // used for interruption
        private Thread backgroundThread;

        public AsyncTaskWithTimeout(Activity context, long timeout, TimeUnit units) {
            this.context = context;
            this.timeout = timeout;
            this.units = units;
        }

        @Override
        protected final void onPreExecute() {
            Thread timeoutThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // start the timeout ticker
                        AsyncTaskWithTimeout.this.get(timeout, units);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        onException(e);
                    } catch (TimeoutException e) {
                        AsyncTaskWithTimeout.this.interruptTask();
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onTimeout();
                            }
                        });

                    }
                }
            });
            timeoutThread.setDaemon(true);
            onPreExec();
            timeoutThread.start();
        }

        /**
         * Equivalent to {@link AsyncTask#onPreExecute()}
         */
        protected void onPreExec() {
        }

        @Override
        protected final Result doInBackground(Params... params) {
            // save off reference to background thread so it can be interrupted on timeout
            this.backgroundThread = Thread.currentThread();
            return runInBackground(params);
        }

        /**
         * Equivalent to {@link AsyncTask#doInBackground(Object[])}
         */
        protected abstract Result runInBackground(Params... params);


        /**
         * This will be run on the UI thread if the timeout is reached.
         */
        protected void onTimeout() {
        }

        /**
         * Called if the AsyncTask throws an exception.
         * By default wrap in {@link RuntimeException} and let it bubble up to the system.
         */
        protected void onException(ExecutionException e) {
            throw new RuntimeException(e);
        }

        private final void interruptTask() {
            if (backgroundThread != null) {
                backgroundThread.interrupt();
            }
        }
    }
}