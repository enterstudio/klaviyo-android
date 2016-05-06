package com.klaviyo.klaviyoplayground;

import android.app.Application;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by katherinekeuper on 5/2/16.
 */
public class KlaviyoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        System.out.println("in Klaviyo Application");
        // Create Klaviyo here
        // Klaviyo specific stuff
        Klaviyo instance = Klaviyo.getInstance();
        instance.setUpWithPublicAPIKey("9BX3wh", getApplicationContext(), "580320923582");
        instance.setUpUserEmail("katy.keuper@klaviyo.com");

        try {
            JSONObject test = new JSONObject();
            test.put(Klaviyo.KL_EVENT_TRACK_KEY, "Java testing");

            JSONObject customerProperties = new JSONObject();

            instance.trackEvent("Local Java Testing");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
