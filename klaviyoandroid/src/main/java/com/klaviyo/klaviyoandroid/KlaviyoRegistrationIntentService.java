package com.klaviyo.klaviyoandroid;

import android.app.IntentService;
import android.content.Intent;

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
            String tokenString = Klaviyo.getGCMSenderID();
            String token = instanceID.getToken(tokenString, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            try {
                Klaviyo.getInstance().addPushDeviceToken(token);
            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (IOException io) {
        /* Can't grab token */
        }
    }
}
