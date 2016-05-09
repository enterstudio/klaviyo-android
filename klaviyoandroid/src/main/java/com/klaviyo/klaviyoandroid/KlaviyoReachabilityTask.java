package com.klaviyo.klaviyoandroid;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by katherinekeuper on 5/6/16.
 */
public class KlaviyoReachabilityTask extends  AsyncTask<Void, String, Boolean> {
    @Override
    protected Boolean doInBackground(Void... params) {
        Boolean isReachable = false;
        try {
            InetAddress.getByName("klaviyo.com").isReachable(3000);
            isReachable = true;
        } catch (IOException | IllegalArgumentException e) {
            isReachable = false;
        }
        return isReachable;
    }
}
