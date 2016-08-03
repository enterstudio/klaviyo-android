package com.klaviyo.klaviyoplayground;

import android.app.Application;
import android.content.Intent;

import com.klaviyo.klaviyoandroid.*;
import com.klaviyo.klaviyoandroid.Klaviyo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by katherinekeuper on 5/2/16.
 */
public class KlaviyoApplication extends Application {
    /* Users could initialize a global instance */
    public Klaviyo klaviyoInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create Klaviyo here
        Klaviyo instance = Klaviyo.getInstance(this);
        instance.sendUserNotifications(true);
        instance.setUpUserEmail("katy.keuper@klaviyo.com");
        instance.trackEvent("refactoring worked!");

    }
}
