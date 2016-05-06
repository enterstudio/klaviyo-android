package com.klaviyo.klaviyoplayground;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
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
 * Receives push notifications, builds & sends to the user
 */
public class KlaviyoGCMListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        /* Grab the Message Dictionary*/
        try {
            JSONObject json = new JSONObject(data.getString("data"));
            parseBundleDataAndSend(data);
        } catch (JSONException e) {
            /*Can't convert to JSON. This means we sent bad data to GCM */
        }
    }

    private void parseBundleDataAndSend(Bundle data) {
        try {
            // swap out that key for the klaviyo constant: all these are required for a push
            JSONObject json = new JSONObject(data.getString("data"));
            String message = json.getString("message");
            String title = json.getString("title");
            // swap out that key for a new constant
            Class cls = Class.forName("com.klaviyo.klaviyoplayground.MainActivity");
            // other options: sound, icons
            // could eventually include message type for templating
            // Grab the metadata for klaviyo to handle open tracking
            JSONObject metadata = json.getJSONObject("$_k");
            // Convert to a bundle
            Bundle klBundle = jsonToBundle(metadata);
            // send the message
            sendNotification(message, title, cls, klBundle);
        } catch (JSONException e) {
            System.out.println("error in parse data and send" + e);
            // nothing we can do here. we can't parse the data sent from our servers
            // this means something went wrong. log it?
        } catch (ClassNotFoundException ce) {
            // something went wrong with the class mapping

            // depending how we implement this could be user error
            System.out.println(ce);
        }
    }

    private void sendNotification(String message, String title, Class cls, Bundle klBundle) {
        // tbd once a third party package is created. make sure to test this
        //Intent intent = new Intent(this, cls);
        Intent intent = new Intent(this, KlaviyoGCMReceiver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction("com.klaviyo.klaviyoplayround.GCM_OPEN");
        intent.putExtra("$kl_metadata", klBundle);

        //intent.putExtras(klBundle);

        // On Open, Broadcast the event. This won't launch the app.
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
                    .setExtras(klBundle); // if clicked, this is the intent that will be executed
            // the pending intent is where click opens should be handled + any other customization

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
