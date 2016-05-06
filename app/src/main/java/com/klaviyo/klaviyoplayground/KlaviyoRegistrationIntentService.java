package com.klaviyo.klaviyoplayground;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by katherinekeuper on 4/29/16.
 */
public class KlaviyoRegistrationIntentService extends  IntentService {

    public KlaviyoRegistrationIntentService() {
        super("KlaviyoRegistrationIntentService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
       InstanceID instanceID = InstanceID.getInstance(this);
       try {
           String tokenString = getString(R.string.gcm_defaultSenderId);
           System.out.println("grabbing token in registration intent service: " + tokenString);
           String token = instanceID.getToken(tokenString, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
           try {
               // Swap the below out eventually once we have proper token handling on the backend
               //Klaviyo.getInstance().addPushDeviceToken(token);
               JSONObject customerProp = new JSONObject();
               JSONObject eventProp = new JSONObject();
               customerProp.put(Klaviyo.KL_PERSON_EMAIL_KEY, "katy.keuper@klaviyo.com");
               eventProp.put(Klaviyo.KL_EVENT_TRACK_TOKEN_Key, token);
               Klaviyo.getInstance().trackEvent("Registered for Android Push", customerProp, eventProp);
           } catch (Exception e) {
               System.out.println(e);
           }
       } catch (IOException io) {
            System.out.println("Unable to getToken: " + io);
       }
    }
}

