package com.bugsnag.android;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Map;

public class MainActivity extends Activity {

    // alias for loopback address: https://developer.android.com/studio/run/emulator-networking.html
    private static final String BASE_URL = "http://10.0.2.2:16000/api/";
    private Configuration config;
    private ConnectivityManager connectivityManager;

    private volatile boolean hasSentError;
    private volatile boolean hasSentSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareApiHooks(getIntent().getBooleanExtra("sendSessions", false));
    }

    private void prepareApiHooks(boolean sendSessions) {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        config = new Configuration("abc123");
        Bugsnag.init(this, config);
        Logger.setEnabled(true);

        if (!sendSessions) {
            Logger.info("Waiting for payload flush");

            waitForPayloadFlush();
        } else { // generate session data for next launch
            Logger.info("Session data");

            Bugsnag.setSessionTrackingApiClient(getFakeSessionApiClient());
            Bugsnag.setUser("592093", "fake@bugsnag.com", "Jimmy");
            Bugsnag.addToTab("custom", "foo", "bar");
            Bugsnag.leaveBreadcrumb("MyBreadcrumb", BreadcrumbType.USER, Collections.singletonMap("foo", "bar"));

            // invokes a callback chain to send requests sequentially:
            //
            // 1. non-fatal, no session
            // 2. non-fatal, with session (payload stored for next launch)
            // 3. unhandled
            // 4. send unhandled/session on next launch
            notifyNonFatal();
        }
    }

    private void waitForPayloadFlush() {
        config.setEndpoint(BASE_URL + "exc_unhandled");
        config.setSessionEndpoint(BASE_URL + "session_manual");
        Logger.info("Waiting for payload flush");

        Bugsnag.setSessionTrackingApiClient(new HarnessSessionApiClient(connectivityManager) {
            @Override
            void onRequestCompleted() {
                Logger.info("Session tracking flushed");
                hasSentSession = true;
                checkIfSentPayloads();
            }
        });
        Bugsnag.setErrorReportApiClient(new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString,
                                   Report report,
                                   Map<String, String> headers) throws NetworkException, BadResponseException {
                Logger.info("Error report flushed");
                hasSentError = true;
                checkIfSentPayloads();
            }
        });
    }

    private void checkIfSentPayloads() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (hasSentError && hasSentSession) {
                    System.exit(1);
                }
            }
        });
    }

    private void notifyNonFatal() {
        Logger.info("Starting exc_handled");
        config.setEndpoint(BASE_URL + "exc_handled");
        Bugsnag.setErrorReportApiClient(new HarnessErrorApiClient(connectivityManager) {
            @Override
            void onRequestCompleted() {
                notifyWithSession();
            }
        });
        Bugsnag.notify(new RuntimeException());
    }

    private void notifyWithSession() {
        Logger.info("Starting exc_handled_session");
        config.setEndpoint(BASE_URL + "exc_handled_session");
        Bugsnag.startSession(); // stored + sent in next launch

        Bugsnag.setErrorReportApiClient(new HarnessErrorApiClient(connectivityManager) {
            @Override
            void onRequestCompleted() {
                unhandledException();
            }
        });
        Bugsnag.notify(new RuntimeException());
    }

    private void unhandledException() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Logger.info("Starting exc_unhandled");
                config.setEndpoint(BASE_URL + "exc_unhandled");
                Bugsnag.setErrorReportApiClient(getFakeErrorReportApiClient());
                throw new RuntimeException("Whoops"); // crashes app, sent again on next launch
            }
        });
    }

    @NonNull
    private SessionTrackingApiClient getFakeSessionApiClient() {
        return new SessionTrackingApiClient() {
            @Override
            public void postSessionTrackingPayload(String urlString, SessionTrackingPayload payload, Map<String, String> headers) throws NetworkException, BadResponseException {
                preventSend();
            }
        };
    }

    @NonNull
    private ErrorReportApiClient getFakeErrorReportApiClient() {
        return new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString,
                                   Report report,
                                   Map<String, String> headers) throws NetworkException, BadResponseException {
                preventSend();
            }
        };
    }

    private void preventSend() throws NetworkException {
        throw new NetworkException("", new RuntimeException());
    }

}
