package com.klaviyo.klaviyoandroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by Klaviyo on 5/2/16.
 * Receives push notifications, builds and sends to the user
 */
public class KlaviyoGCMListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        /* Grab the Message Dictionary*/
        try {
            System.out.println("message received");
            JSONObject json = new JSONObject(data.getString("data"));
            System.out.println("json: " + json);
            parseBundleDataAndSend(data);
        } catch (JSONException e) {
            /*Can't convert to JSON. This means we sent bad data to GCM */
        }
    }

    private void parseBundleDataAndSend(Bundle data) {
        try {
            // Minimum mappings required to build a push
            JSONObject json = new JSONObject(data.getString("data"));
            String message = json.getString("message");
            String title = json.getString("title");

            // Grab the metadata for klaviyo to handle open tracking
            JSONObject metadata = json.getJSONObject("$_k");

            // Convert to a bundle
            Bundle klBundle = jsonToBundle(metadata);

            // send the message
            sendNotification(message, title, klBundle);
        } catch (JSONException e) {
            // nothing we can do here. we can't parse the data sent from our servers
        }
    }

    private void sendNotification(String message, String title, Bundle klBundle) {
        /* Build a notification that will launch the GCM Receiver upon click */
        Intent intent = new Intent(this, KlaviyoGCMReceiver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        /* If users want to launch their app thye must add this action to the manifest file */
        intent.setAction("com.klaviyo.klaviyoplayround.GCM_OPEN");
        intent.putExtra("$kl_metadata", klBundle);

        // On Open, Broadcast the event. This won't launch the app unless the action is set.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        try {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setSmallIcon(R.drawable.common_plus_signin_btn_text_light)
                    .setContentIntent(pendingIntent)
                    .setExtras(klBundle);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(0, notificationBuilder.build());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert the returned data into a bundle.
     *
     * @param jsonObject JSONObject
     */

    private static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator iter = jsonObject.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = jsonObject.getString(key);
            bundle.putString(key, value);
        }
        return bundle;
    }
}
