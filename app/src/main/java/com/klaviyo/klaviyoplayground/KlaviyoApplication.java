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
    @Override
    public void onCreate() {
        super.onCreate();

        // Create Klaviyo here
        Klaviyo.getInstance().setUpWithPublicAPIKey("9BX3wh", getApplicationContext(), "580320923582");

        // Optional: initialize a user's email
        Klaviyo.getInstance().setUpUserEmail("katy.keuper@klaviyo.com");

        // Optional: if implementing push & want app launch on push open
        Klaviyo.getInstance().setPushActivity("com.klaviyo.klaviyoplayground.MainActivity");
        try {
            JSONObject test = new JSONObject();
            test.put(Klaviyo.KL_EVENT_TRACK_KEY, "Java testing");

            JSONObject customerProperties = new JSONObject();

            Klaviyo.getInstance().trackEvent("Third party lib event!");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
