package com.klaviyo.klaviyoandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

/**
 * Created by Klaviyo on 4/28/16.
 * Notes for Review: Should we combine this with KlaviyoGCMReceiver?
 * Pros: Creates one receiver; shortens the manifest file
 * Cons: Two separate events being handled (network change vs. push opens)
 */
public class KlaviyoNetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            /* Flush the Queues if connectivity switches back on*/
            Klaviyo.getInstance().connectivityChanged(context);
        } else {
            /* Connectivity switched off. Archive any lingering events */
            Klaviyo.getInstance().stopKlaviyoTracking();
        }

    }

}