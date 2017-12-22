package com.bugsnag.android;

import android.net.ConnectivityManager;

import java.util.Map;

abstract class HarnessErrorApiClient implements ErrorReportApiClient {

    private final DefaultHttpClient httpClient;

    HarnessErrorApiClient(ConnectivityManager cm) {
        httpClient = new DefaultHttpClient(cm);
    }

    @Override
    public void postReport(String urlString, Report report, Map<String, String> headers) throws NetworkException, BadResponseException {
        httpClient.postReport(urlString, report, headers);
        onRequestCompleted();
    }

    abstract void onRequestCompleted();

}
