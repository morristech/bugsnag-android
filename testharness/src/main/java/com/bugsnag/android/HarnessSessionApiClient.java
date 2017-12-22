package com.bugsnag.android;

import android.net.ConnectivityManager;

import java.util.Map;

abstract class HarnessSessionApiClient implements SessionTrackingApiClient {

    private final DefaultHttpClient httpClient;

    HarnessSessionApiClient(ConnectivityManager cm) {
        httpClient = new DefaultHttpClient(cm);
    }

    @Override
    public void postSessionTrackingPayload(String urlString,
                                           SessionTrackingPayload payload,
                                           Map<String, String> headers) throws NetworkException, BadResponseException {
        httpClient.postSessionTrackingPayload(urlString, payload, headers);
        onRequestCompleted();
    }

    abstract void onRequestCompleted();

}
