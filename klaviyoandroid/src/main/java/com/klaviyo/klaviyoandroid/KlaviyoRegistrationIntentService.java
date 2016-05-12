package com.klaviyo.klaviyoandroid;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by Klaviyo on 4/29/16.
 */
public class KlaviyoRegistrationIntentService extends  IntentService {

    public KlaviyoRegistrationIntentService() {
        super("KlaviyoRegistrationIntentService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(this);
        try {
            String tokenString = Klaviyo.getInstance(getApplicationContext()).getGCMSenderID();
            String token = instanceID.getToken(tokenString, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Klaviyo.getInstance(getApplicationContext()).addPushDeviceToken(token);
        } catch (IOException io) {
            /* Can't grab token */
            Log.d("klaviyo.klaviyoandroid", "onHandleIntent: unable to grab instance token " + io.toString());
        }
    }
}
