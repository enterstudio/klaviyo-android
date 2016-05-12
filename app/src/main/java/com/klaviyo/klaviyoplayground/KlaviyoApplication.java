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

        // Optional: initialize a user's email

        // Optional: if implementing push & want app launch on push open
        try {
            JSONObject test = new JSONObject();
            test.put(Klaviyo.KL_EVENT_TRACK_KEY, "Java testing");

            JSONObject customerProperties = new JSONObject();

       //   Klaviyo.getInstance().trackEvent("Third party lib event!");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
