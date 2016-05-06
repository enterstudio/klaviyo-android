package com.klaviyo.klaviyoplayground;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Klaviyo on 5/3/16.
 */
public class KlaviyoGCMReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Bundle kBundle = intent.getExtras().getBundle(Klaviyo.KL_GCM_Metadata);

        //start the designated activity: should make this dynamic
       // Intent i = new Intent(context, MainActivity.class);
        Intent i = new Intent("com.klaviyo.klaviyoplayground.LAUNCH_GCM_OPEN");
        //i.setAction("android.intent.action.MAIN");
        i.addCategory("android.intent.category.DEFAULT");
        //i.addCategory("android.intent.category.DEFAULT");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);

        // If it's receiving a push open, pass that to Klaviyo to handle
        if (action == Klaviyo.KL_GCM_OPEN && kBundle != null) {
            Klaviyo.getInstance().handlePushOpen(kBundle);
        }

    }
}
