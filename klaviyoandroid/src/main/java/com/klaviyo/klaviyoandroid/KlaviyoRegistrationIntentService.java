package com.klaviyo.klaviyoandroid;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


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
       // InstanceID instanceID = InstanceID.getInstance(this);
        System.out.println("in KlaviyoRegistrationIntentService");
       //     String tokenString = Klaviyo.getInstance(getApplicationContext()).getGCMSenderID();
       //     String token = instanceID.getToken(tokenString, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
         //   System.out.println(token);
         //   Klaviyo.getInstance(getApplicationContext()).addPushDeviceToken(token)
    }
}
