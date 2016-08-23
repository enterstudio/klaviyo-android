package com.klaviyo.klaviyoandroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
///import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Klaviyo on 5/2/16.
 * Receives push notifications, builds and sends to the user
 */

/*
* public class MyFcmListenerService extends FirebaseMessagingService {
  @Override
  public void onMessageReceived(RemoteMessage message){
    String from = message.getFrom();
    Map data = message.getData();
  }
  ...
}
* */
public class KlaviyoGCMListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String from = remoteMessage.getFrom();
        Map data = remoteMessage.getData();
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        parseAndSend(remoteMessage);
    }

    private void parseAndSend(RemoteMessage message) {
        String text = message.getNotification().getBody();
        String title = message.getNotification().getTitle();

        // get metadata
        Map data = message.getData();

        JSONObject json = new JSONObject(data);
        try {
            JSONObject metadata = json.getJSONObject("kl_data");
            Bundle klBundle = jsonToBundle(metadata);
            sendNotification(text, title, klBundle);
        } catch (JSONException je) {
            // if this fails it's due to bad json payloads coming from our servers
            // tbd: should add to logging platform
        }
    }


    private void sendNotification(String message, String title, Bundle klBundle) {
        /* Build a notification that will launch the GCM Receiver upon click */
        Intent intent = new Intent(this, KlaviyoGCMReceiver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        /* If users want to launch their app thye must add this action to the manifest file */

        KlaviyoPropertiesReader kpr = new KlaviyoPropertiesReader(getApplicationContext());
        Properties p = kpr.getProperties("klaviyoconfig.properties");
        String notificationOpenAction = p.getProperty("launcher_class_key", "none");

        intent.setAction(notificationOpenAction);
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
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this).setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setSmallIcon(R.drawable.common_plus_signin_btn_text_light)
                    .setContentIntent(pendingIntent)
                    .setExtras(klBundle);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(0, notificationBuilder.build());
        } catch (Exception e) {
            // TBD: log the error, something went wrong building the notification
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
