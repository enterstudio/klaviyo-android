package com.klaviyo.klaviyoandroid;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Klaviyo on 5/3/16.
 * TBD: expose this or mask it?
 * -If exposed, it will allow users to customize their app launch beyond an implicit intent
 * -Hide it, and users will be limited to launching
 */
public class KlaviyoGCMReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Bundle kBundle = intent.getExtras().getBundle(Klaviyo.KL_GCM_METADATA);

        // Start the user designated activity
        String launcherClassName = Klaviyo.getInstance(context).getPushActivity();

        try {
            Class launcherClass = Class.forName(launcherClassName);
            Intent i = new Intent(context, launcherClass);
            i.addCategory("android.intent.category.LAUNCHER");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(i);
        } catch (ClassNotFoundException cd) {
            /* class does not exist: either user did not configure this option or they did so incorrectly */
            Log.e("Klaviyo", "ClassNotFound in GCMReceiver - bad class name passed to Klaviyo");
        }

        // If it's receiving a push open, pass that to Klaviyo to handle
        if (Klaviyo.KL_GCM_OPEN.equals(action) && kBundle != null) {
            Klaviyo.getInstance(context).handlePushOpen(kBundle);
        }

    }
}
