package com.klaviyo.klaviyoandroid;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Klaviyo on 4/29/16.
 */
public class KlaviyoTokenRefreshListenerService extends InstanceIDListenerService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token & send it to Klaviyo if it has changed
        Intent i = new Intent(this, KlaviyoRegistrationIntentService.class);
        startService(i);

    }
}
