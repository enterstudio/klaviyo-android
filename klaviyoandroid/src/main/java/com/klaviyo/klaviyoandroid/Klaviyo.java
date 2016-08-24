package com.klaviyo.klaviyoandroid;

import android.app.IntentService;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import android.content.Intent;
import android.util.Log;
import android.util.Property;

/**
 * Created by Klaviyo on 4/26/16.
 */
public class Klaviyo {

    // Public Constants for Events
    public static final String KL_EVENT_TRACK_PROPERTIES_KEY = "properties";
    public static final String KL_EVENT_TRACK_CUSTOMER_PROP_KEY = "customer_properties";
    public static final String KL_EVENT_TRACK_TIME_KEY = "time";
    public static final String KL_EVENT_TRACK_PURCHASE_PLATFORM_KEY = "platform";

    public static final String KL_PERSON_EMAIL_KEY = "$email";
    public static final String KL_PERSON_FIRST_NAME_KEY = "$first_name";
    public static final String KL_PERSON_LAST_NAME_KEY = "$last_name";
    public static final String KL_PERSON_PHONE_NUMBER_KEY = "$phone_number";
    public static final String KL_PERSON_TITLE_KEY = "$title";
    public static final String KL_PERSON_ORG_KEY = "$organization";
    public static final String KL_PERSON_CITY_KEY = "$city";
    public static final String KL_PERSON_REGION_KEY = "$region";
    public static final String KL_PERSON_COUNTRY_KEY = "$country";
    public static final String KL_PERSON_ZIP_KEY = "$zip";

    // Private Constants for Events
    public static final String KL_EVENT_TRACK_TOKEN_Key = "token";
    public static final String KL_EVENT_TRACK_KEY = "event";
    private static final String KL_EVENT_TRACK_SERVICE_KEY = "service";

    // Private Constants for People
    private static final String KL_PERSON_TRACK_TOKEN_KEY = "token";
    private static final String KL_PERSON_PROPERTIES_KEY = "properties";

    // Push Notification Keys
    private static final String KL_PERSON_OPENED_PUSH = "$opened_push";
    private static final String KL_MESSAGE_DIMENSION = "$message";

    // TBD: Extract
    public static final String KL_GCM_OPEN = "com.klaviyo.klaviyoplayround.GCM_OPEN";
    protected  static final String KL_GCM_METADATA = "$kl_metadata";

    // API Endpoints
    private static final String KLAVIYO_SERVER_URL_STRING = "https://a.klaviyo.com/api"; //"https://a.klaviyo.com/api";
    private static final String KLAVIYO_SERVER_TRACK_ENDPOINT = "/track";
    private static final String KLAVIYO_SERVER_IDENTIFY_ENDPOINT = "/identify";

    // Alternative tracking keys
    private static final String KL_PERSON_ID_KEY = "$id";
    private static final String KL_PERSON_DEVICE_ID_KEY = "$device_id";
    private static final String CUSTOMER_PROPERTIES_ID_KEY = "$anonymous";
    private static final String KL_SHAREDPREF_ID_KEY = "$klaviyo_ID";

    private static final String KL_EVENT_ID_KEY = "$event_id";
    private static final String KL_EVENT_VALUE_KEY = "$value";
    private static final String KL_EVENT = "$event";

    private static final String CUSTOMER_PROPERTIES_APPEND_KEY = "$append";
    private static final String CUSTOMER_PROPERTIES_GCM_TOKENS_KEY = "$android_tokens";
    private static final String CUSTOMER_FCM_TOKEN = "fcm_token";
    private static final String KL_REGISTER_APN_TOKEN_EVENT = "KL_ReceiveNotificationsDeviceToken";
    private static final String LAUNCHER_CLASS_KEY = "$kl_push_open_activity";

    // GCM Push
    protected String senderID;
    protected Boolean remoteNotificationsEnabled = false;
    private String apnDeviceToken;
    private String launcher_class;

    // Context is required for android access
    private Context context;
    private static final int MAX_CONNECTIONS = 5;
    private static final String SHARED_PREF_KEY = "klaviyo";
    protected static final String KLAVIYO_KEY = "klaviyoAPIKey";
    protected static final String GCM_KEY = "gcmSenderKey";
    protected static final String PUSH_ENABLED_KEY = "klaviyoPushEnabled";

    // Keys for IntentService Bundle
    private static final  String FINISH_SETUP_KEY = "setUpWithPublicAPIKey";
    private static final String UNARCHIVE_FLUSH = "$unarchiveAndFlush";
    private static final String ARCHIVE = "$archive";
    private  static final String TRACK_EVENT = "trackEvent";
    private static final String TRACK_PERSON = "trackPerson";
    private static final String DATE_INCLUDED = "isDateIncluded";
    private static final String DATE = "$date";

    // The internal sharedInstance

    // Event Handling
    private String apiKey;
    private String userEmail;


    /* Creates Klaviyo using klaviyo-config file + google-json */
    public static Klaviyo getInstance(Context context) {
        return new Klaviyo(context);
    }

    public void sendUserNotifications(Boolean isEnabled) {
        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(PUSH_ENABLED_KEY, isEnabled);
        edit.apply();
    }

    /* Internal class for launching activities on push open */
    protected String getGCMActivity() {
        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
        return pref.getString(LAUNCHER_CLASS_KEY, "none");
    }

    /* Build Klaviyo from config file + Google-Services.json */
    private Klaviyo(Context context) {
        KlaviyoPropertiesReader kpr = new KlaviyoPropertiesReader(context);
        Properties p = kpr.getProperties("klaviyoconfig.properties");

        // grab api key: required
        String apiKey = p.getProperty("klaviyo_api_key", "none");

        // set the api key and context
        this.apiKey = apiKey;
        this.context = context;

        // check if push is enabled
        String sender_id = p.getProperty("gcm_sender_id");
        this.senderID = sender_id;

        if (sender_id != null) {
            String launcherClass = p.getProperty("launcher_class_key");
            if (launcherClass != null) {
                setPushActivity(launcherClass);
            }
        }

        startInitialIntent(apiKey, context);
    }

    private void startInitialIntent(String apiKey, Context context) {
        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(FINISH_SETUP_KEY, true);
        k.putExtra(KLAVIYO_KEY, apiKey);
        k.putExtra(PUSH_ENABLED_KEY, false);
        context.startService(k);
    }

    protected String getGCMSenderID() {
        return this.senderID;
    }

    /* Users can pass in a string representing the activity they would like to launch upon notification open
    *  String has to be explicit, i.e. 'MainActivity' would fail, 'com.klaviyo.klaviyoplayground.MainActivity' works"
    * */
    private void setPushActivity(String activityName) {
        this.launcher_class = activityName;

        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);

        SharedPreferences.Editor edit = pref.edit();
        edit.putString(LAUNCHER_CLASS_KEY, activityName);
        edit.apply();
    }

    /*  Used by the GCM Receiver to Trigger the designated activity */
    protected String getPushActivity() {
        if (this.launcher_class != null) {
            return this.launcher_class;
        } else {
            SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            return pref.getString(LAUNCHER_CLASS_KEY, "none");
        }
    }


    protected boolean isApiKeySet() {
        return (this.apiKey != null);
    }

    /* Archive data if connection drops */
    protected void stopKlaviyoTracking() {
        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(ARCHIVE, true);
        k.putExtra(KLAVIYO_KEY, apiKey);
        context.startService(k);
    }

    /* Call this for user email persistence */
    public void setUpUserEmail(String email) {
        this.userEmail = email;
        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(KL_PERSON_EMAIL_KEY, email);
        edit.apply();
    }

    public void setUpCustomerID(String id) {
        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(id, KL_SHAREDPREF_ID_KEY);
        editor.apply();;
    }

    /**
     trackEvent: KL Event tracking using all possible parameters

     - Parameter eventName: name of the event
     - Parameter customerPropertiesDict: dictionary for user info
     - Parameter propertiesDict: dictionary for event info
     - Parameter eventDate: date of the event
     */
    public void trackEvent(String event, JSONObject customerProperties, JSONObject propertiesDict, Date eventDate, boolean didUserIncludeDate) throws JSONException {
        // set up the background thread service for flushing queues
        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(KL_EVENT, event);
        k.putExtra(KLAVIYO_KEY, apiKey);
        k.putExtra(KL_EVENT_TRACK_CUSTOMER_PROP_KEY, customerProperties.toString());
        k.putExtra(KL_EVENT_TRACK_PROPERTIES_KEY, propertiesDict.toString());
        k.putExtra(DATE, eventDate);
        k.putExtra(DATE_INCLUDED, didUserIncludeDate);
        k.putExtra(TRACK_EVENT, true);
        context.startService(k);
    }

    /**
     trackEvent: KL Event tracking for event name, customer and event properties

     - Parameter eventName: name of the event
     - Parameter customerPropertiesDict: dictionary for user info
     - Parameter properties: dictionary for event info
     */
    public void trackEvent(String eventName, JSONObject customerProperties, JSONObject properties) throws JSONException {
        trackEvent(eventName, customerProperties, properties, new Date(), false);
    }

    public void trackEvent(String event, JSONObject customerProperties, JSONObject propertiesDict, Date eventDate) throws JSONException {
        trackEvent(event, customerProperties, propertiesDict, eventDate, true);
    }

    /**
     trackEvent: KL Event tracking for event name and customer properties

     - Parameter eventName: name of the event
     - Parameter properties: customerProperties
     */
    public void trackEvent(String eventName, JSONObject properties) throws JSONException {
        trackEvent(eventName, new JSONObject(), properties);
    }

    /**
     trackEvent: KL Event tracking for event name only
     - Parameter eventName: name of the event
     */
    public void trackEvent(String eventName)  {
        try {
            trackEvent(eventName, new JSONObject());
        } catch (JSONException je) {
            // this would mean our SDK is encoding JSON incorrectly
            Log.e("klaviyo.klaviyoandroid", "trackEvent unable to process due to json encode error");
        }
    }

    /*
    * Converts the bundle data into a push open metric event for Klaviyo
    *
    * Parameter bundle: Bundle item that contains all metadata about the message
    * */
    public void handlePushOpen(Bundle bundle) {
        JSONObject eventDict = new JSONObject();
        for (String key : bundle.keySet()) {
            String value = (String) bundle.get(key);
            try {
                eventDict.put(key, value);
            } catch (JSONException e) {
                /* This is bad. It means the json GCM payload is sent incorrectly */
                Log.e("klaviyo.klaviyoandroid", "JSONException handlePushOpen: bad payload data received");
            }
        }
        if (eventDict.length() > 0) {
            try {
                trackEvent(KL_PERSON_OPENED_PUSH, eventDict);
            } catch (JSONException e){
                /* should not get triggered since we don't make it here unless our json encoding above works*/
            }
        }
    }

    protected void connectivityChanged(Context context) {
        Intent k = new Intent(context, KlaviyoService.class);

        if (!this.isApiKeySet()) {
            SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            String apiKey = pref.getString(KLAVIYO_KEY, "none");
            String senderID = pref.getString(GCM_KEY, "none");
            Boolean isPushEnabled = pref.getBoolean(PUSH_ENABLED_KEY, false);
            k.putExtra(FINISH_SETUP_KEY, true);
            k.putExtra(KLAVIYO_KEY, apiKey);
            k.putExtra(GCM_KEY, senderID);
            k.putExtra(PUSH_ENABLED_KEY, isPushEnabled);
        } else {
            k.putExtra(UNARCHIVE_FLUSH, true);
        }

        context.startService(k);
    }

    /**
     addPushDeviceToken: Registers Klaviyo with Google for Push Notifications (GCM)
     Private function creates a unique identifier for the device and uses it to track the event

     - Parameter deviceToken: token provided by Google that registers push notifications to the given device
     - Returns: Void
     */
    public void addPushDeviceToken(String deviceToken) {
        // Don't allow for empty tokens
        if (deviceToken.isEmpty()) {
            return;
        }
        // Update shared instance
        this.apnDeviceToken = deviceToken;

        // save the push token
        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(CUSTOMER_FCM_TOKEN, deviceToken);
        edit.apply();
        JSONObject personDict = new JSONObject();
        JSONObject appendDict = new JSONObject();

        try {
            appendDict.put(CUSTOMER_PROPERTIES_GCM_TOKENS_KEY, deviceToken);
            personDict.put(CUSTOMER_PROPERTIES_APPEND_KEY, appendDict);
        } catch (Exception e) {
            return;
        }

        try {
            trackPersonWithInfo(personDict);
        } catch (JSONException je){

        }
    }

    /**
     trackPersonWithInfo: method that creates a Klaviyo person tracking instance that is separate from an event

     - Parameter personInfoDictionary: dictionary of user attributes that you wish to track. These can be special properties provided by Klaviyo, such as KLPersonFirstNameDictKey, or created by the user on the fly.

     - Returns: Void
     */
    public void trackPersonWithInfo(JSONObject personDictionary) throws JSONException {
        if (personDictionary.length() == 0) {
            return;
        }
        // set up the background thread service for flushing queues
        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(FINISH_SETUP_KEY, false);
        k.putExtra(KLAVIYO_KEY, apiKey);
        k.putExtra(KL_PERSON_TRACK_TOKEN_KEY, true);
        k.putExtra(KL_PERSON_PROPERTIES_KEY, personDictionary.toString());
        context.startService(k);
    }

    /* Nested Service to handle Asynchronous Calling */
    public static class KlaviyoService extends IntentService {
        private ArrayList<JSONObject> eventsQueue = new ArrayList<JSONObject>();
        private ArrayList<JSONObject> peopleQueue = new ArrayList<JSONObject>();
        private String apiKey;

        public KlaviyoService() {
            super("KlaviyoService");
        }

        /* Entry point to Klaviyo. There are several actions that can be launched
        *  event tracking
        *  identifying a person
        *  initialization
        *  stopping/starting of services due to connectivity
        *  processing push notifications
        * */
        @Override
        protected void onHandleIntent(Intent intent) {
            this.apiKey = intent.getExtras().getString(KLAVIYO_KEY);

            if (intent.getExtras().getBoolean(FINISH_SETUP_KEY)) {
                Boolean pushEnabled = intent.getExtras().getBoolean(PUSH_ENABLED_KEY);
                String gcmKey = (pushEnabled) ? intent.getExtras().getString(GCM_KEY) : "" ;
                finishKlaviyoSetUp(apiKey, gcmKey, pushEnabled);
            } else if (intent.getExtras().getBoolean(TRACK_EVENT)) {
                handleEventTracking(intent);
            } else if (intent.getExtras().getBoolean(TRACK_PERSON)) {
                handlePersonTracking(intent);
            } else if (intent.getExtras().getBoolean(UNARCHIVE_FLUSH)) {
                unarchiveAndFlush();
            } else if (intent.getExtras().getBoolean(ARCHIVE)) {
                archive();
            }
        }

        /* Methods triggered from onHandleIntent */
        private void finishKlaviyoSetUp(String apiKey, String senderID, Boolean push) {
            saveUserInfo(apiKey, senderID, push);

            //If user is not connected don't flush anything
            final ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            if (activeNetwork != null) {
                unarchiveAndFlush();
            }
        }

        private void handleEventTracking(Intent intent) {
            try {
                Bundle dataBundle = intent.getExtras();
                // Grab the data from the bundle
                String eventName = dataBundle.getString(KL_EVENT);
                Date date = (isTrackingDate(dataBundle)) ? getEventDateFromBundle(dataBundle) : new Date();
                JSONObject customerProps = new JSONObject(intent.getExtras().getString(KL_EVENT_TRACK_CUSTOMER_PROP_KEY));
                JSONObject properties = new JSONObject(intent.getExtras().getString(KL_EVENT_TRACK_PROPERTIES_KEY));

                internalTrackEvent(eventName, customerProps, properties, date, isTrackingDate(dataBundle));
            } catch (JSONException je) {
                // shouldn't happen since we encoded it before to pass it to this method
            }
        }

        private void handlePersonTracking(Intent intent) {
            try {
                JSONObject personDict = new JSONObject(intent.getExtras().getString(KL_PERSON_PROPERTIES_KEY));
                internalTrackPersonWithInfo(personDict);
            } catch (JSONException je) {
                /* shouldn't happen since we encoded this earlier with success */
            }

        }

        /* Helper Methods */
        private boolean isTrackingDate(Bundle bundle) {
            return bundle.getBoolean(DATE_INCLUDED);
        }

        private Date getEventDateFromBundle(Bundle bundle) {
           return (Date) bundle.get(DATE);
        }

        private void saveUserInfo(String apiKey, String senderID, Boolean isPushEnabled) {
            SharedPreferences pref = this.getApplicationContext().getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(KLAVIYO_KEY, apiKey);
            edit.putString(GCM_KEY, senderID);
            edit.putBoolean(PUSH_ENABLED_KEY, isPushEnabled);
            edit.apply();
        }

        /** Flushing * */
        private void unarchiveAndFlush() {
            unarchive();
            flush();
        }

        private void flush() {
            flushEvents();
            flushPeople();
        }

        private void flushEvents() {
            flushQueue(eventsQueue, KLAVIYO_SERVER_TRACK_ENDPOINT);
        }

        private void flushPeople() {
            flushQueue(peopleQueue, KLAVIYO_SERVER_IDENTIFY_ENDPOINT);
        }

        /**
         flushQueue: Iterates through an array of events and produces the relevant API request
         - Parameter queue: an array of events
         - Parameter endpoint: the api endpoint /track or /identify
         */
        private void flushQueue(ArrayList<JSONObject> queue, String endPoint) {
            boolean isAbleToFlush = true;

            // If network is not available, archive the events
            if (!isNetworkAvailable(this.getApplicationContext())) {
                archive();
                return;
            }

            ArrayList<JSONObject> currentQueue = queue;
            ArrayList<JSONObject> toRemove = new ArrayList<JSONObject>();
            // Grab each item from the queue and send it to klaviyo
            for (JSONObject item : currentQueue) {
                String request = apiRequestWithEndpoint(endPoint, item);
                try {
                    Boolean result = new InternalKlaviyoTask().execute(request).get();
                    if (result) {
                        // Successful send. Remove it from the queue.
                        toRemove.add(item);
                    } else {
                        // stop sending data and archive until connection returns
                        isAbleToFlush = false;
                        break;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // There was an error connecting. Leave the event in the queue
                }
            }

            // Remove successful api calls from queue and archive if need be
            queue.removeAll(toRemove);

            // Something went wrong. Archive the queue until connectivity returns
            if (!isAbleToFlush) {
                archive();
            }
        }

        /* Archiving */

        private void archive() {
            /* Only bother archiving if there are events */
            try {
                if (eventsQueue.size() > 0) {
                    archiveEvents();
                }
                if (peopleQueue.size() > 0) {
                    archivePeople();
                }
            } catch (IOException io) {
                 /* File write failed */
            }
        }

        private void archiveEvents() throws IOException {
            File file = eventFile();
            archiveFile(file);
        }

        private void archivePeople() throws IOException {
            File file = peopleFile();
            archiveFile(file);
        }

        private void archiveFile(File file) throws IOException {
            FileWriter fw = null;
            BufferedWriter bw = null;

            try {
                fw = new FileWriter(file);
                bw = new BufferedWriter(fw);
                // write to file and remove from the queue
                for (JSONObject item : eventsQueue) {
                    bw.write(item.toString());
                    bw.newLine();
                    eventsQueue.remove(item);
                }
            } catch (IOException io) {
                //unable to write to file
            } finally {
                if (bw != null) {
                    bw.close();
                }
                if (fw != null) {
                    fw.flush();
                    fw.close();
                }
            }
        }

        /* Unarchive */
        private void unarchive() {
            try {
                unarchiveEvents();
                unarchivePeople();
            } catch (IOException | JSONException e) {
                /* File has not been created or is empty */
            }
        }

        private void unarchiveForQueue(boolean isEvents, File file) throws IOException, JSONException {

            try {
                FileReader fr = new FileReader(file);
                StringBuilder sb = new StringBuilder();

                BufferedReader br = new BufferedReader(fr);
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }

                br.close();
                fr.close();

                String[] lines = sb.toString().split("\\n");

                for (String s: lines) {
                    JSONObject newEvent = new JSONObject(s);
                    if (isEvents) {
                        eventsQueue.add(newEvent);
                    } else {
                        peopleQueue.add(newEvent);
                    }
                }
            } catch (FileNotFoundException fe) {
                // this is ok. means no files have been archived yet
            }
        }

        private void unarchiveEvents() throws IOException, JSONException {
            File eventFile = eventFile();
            unarchiveForQueue(true, eventFile);
        }

        private void unarchivePeople() throws  IOException, JSONException {
            File peopleFile = peopleFile();
            unarchiveForQueue(false, peopleFile);
        }

        /* Archive Helpers */
        private String eventFileName() {
            return "klaviyo-"+ this.apiKey +"-events";
        }

        private File eventFile() {
            String name = eventFileName();
            File dir = this.getApplicationContext().getFilesDir();
            return new File(dir, name);
        }
        private String peopleFileName() {
            return "klaviyo-"+this.apiKey+"-people";
        }

        private File peopleFile() {
            String name = peopleFileName();
            File dir = this.getApplicationContext().getFilesDir();
            return new File(dir, name);
        }


        /* Network Connectivity */
        private boolean isNetworkAvailable(Context context) {
            try
            {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                if (connectivityManager.getActiveNetworkInfo() != null &&
                        connectivityManager.getActiveNetworkInfo().isAvailable() &&
                        connectivityManager.getActiveNetworkInfo().isConnected())
                    return true;
                else
                    return false;
            }
            catch (Exception e)
            {
                return false;
            }
        }

        /* Helper Methods for Building the API Requests */
        private JSONObject updatePropertiesDictionary(JSONObject props) throws JSONException {
            JSONObject properties = props;

            // Email checks
            if (props.has(KL_PERSON_EMAIL_KEY)) {
                // if user passed in an email, save it and use
                saveUserEmail(props.getString(KL_PERSON_EMAIL_KEY));
                properties.put(KL_PERSON_EMAIL_KEY, props.getString(KL_PERSON_EMAIL_KEY));
            } else if (emailAddressExists()) {
                // email address has been entered and/or saved, so use it
                properties.put(KL_PERSON_EMAIL_KEY, getUserEmail());
            }

            // $anonymous ID
            properties.put(CUSTOMER_PROPERTIES_ID_KEY, androidIDString());

            // Customer ID
            if (props.has(KL_PERSON_ID_KEY)) {
                //
                saveCustomerID(props.getString(KL_PERSON_ID_KEY));
                properties.put(KL_PERSON_ID_KEY, props.getString(KL_PERSON_ID_KEY));
            } else if (customerIDExists()) {
                properties.put(KL_PERSON_ID_KEY, getCustomerID());
            }

            // FCM Token
            String token = getGCMToken();
            if (!token.isEmpty()) {
                JSONObject appendDict = new JSONObject();
                appendDict.put(CUSTOMER_PROPERTIES_GCM_TOKENS_KEY, token);
                properties.put(CUSTOMER_PROPERTIES_APPEND_KEY, appendDict);
            }
            return properties;
        }

        private String getGCMToken() {
            SharedPreferences pref = getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            return pref.getString(CUSTOMER_FCM_TOKEN, "");
        }

        private void saveUserEmail(String email) {
            SharedPreferences pref = getApplicationContext().getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(KL_PERSON_EMAIL_KEY, email);
            edit.apply();
        }

        private String getUserEmail() {
            SharedPreferences pref = getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            return pref.getString(KL_PERSON_EMAIL_KEY, "");
        }

        private void saveCustomerID(String id) {
            SharedPreferences pref = getApplicationContext().getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(KL_SHAREDPREF_ID_KEY, id);
            edit.apply();
        }

        private String getCustomerID() {
            SharedPreferences pref = getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            return pref.getString(KL_SHAREDPREF_ID_KEY, "");

        }

        private String androidIDString() {
            return Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        private Boolean customerIDExists() {
            String customerID = getCustomerID();
            return customerID.length() > 0;
        }

        private Boolean emailAddressExists() {
            String email = getUserEmail();
            return email.length() > 0;
        }

        private String apiRequestWithEndpoint(String endPoint, JSONObject params) {
            String urlString = KLAVIYO_SERVER_URL_STRING+endPoint;

            String paramString = params.toString();
            byte[] paramBytes = paramString.getBytes();
            String base64encoded = Base64.encodeToString(paramBytes, Base64.NO_WRAP);

            urlString = urlString + "?data=" + base64encoded;

            return urlString;
        }

        /**
         trackPersonWithInfo: method that creates a Klaviyo person tracking instance that is separate from an event

         - Parameter personInfoDictionary: dictionary of user attributes that you wish to track. These can be special properties provided by Klaviyo, such as KLPersonFirstNameDictKey, or created by the user on the fly.

         - Returns: Void
         */
        private void internalTrackPersonWithInfo(JSONObject personDictionary) throws JSONException {
            if (personDictionary.length() == 0) {
                return;
            }

            // Update properties
            JSONObject customerPropDict = updatePropertiesDictionary(personDictionary);
            JSONObject trackPersonDict = new JSONObject();
            trackPersonDict.put(KL_PERSON_PROPERTIES_KEY, customerPropDict);
            trackPersonDict.put(KL_PERSON_TRACK_TOKEN_KEY, apiKey);
            trackPersonDict.put(KL_EVENT_TRACK_SERVICE_KEY, "api");

            peopleQueue.add(trackPersonDict);

            if (peopleQueue.size() > 500) {
                peopleQueue.remove(0);
            }

            flushPeople();
        }

        /**
         trackEvent: KL Event tracking using all possible parameters

         - Parameter eventName: name of the event
         - Parameter customerPropertiesDict: dictionary for user info
         - Parameter propertiesDict: dictionary for event info
         - Parameter eventDate: date of the event
         */
        private void internalTrackEvent(String event, JSONObject customerProperties, JSONObject propertiesDict, Date eventDate, boolean didUserIncludeDate) throws JSONException {
            String eventName = event;
            String service = "api";

            if (eventName.isEmpty()) {
                eventName = "KL_Event";
            }

            customerProperties = updatePropertiesDictionary(customerProperties);

            // If it's a push event, handle differently
            if (KL_PERSON_OPENED_PUSH.equals(eventName)) {
                service = "klaviyo";
            }

            // Create the main parameters dictionary
            JSONObject eventParameters = new JSONObject();
            eventParameters.put(KL_EVENT_TRACK_TOKEN_Key, apiKey);
            eventParameters.put(KL_EVENT_TRACK_KEY, eventName);
            eventParameters.put(KL_EVENT_TRACK_SERVICE_KEY, service);
            eventParameters.put(KL_EVENT_TRACK_CUSTOMER_PROP_KEY, customerProperties);

            // Event_Properties could be empty
            if (propertiesDict.length() > 0) {
                eventParameters.put(KL_EVENT_TRACK_PROPERTIES_KEY, propertiesDict);
            }

            if (didUserIncludeDate) {
                eventParameters.put(KL_EVENT_TRACK_TIME_KEY, eventDate);
            }

            eventsQueue.add(eventParameters);

            if (eventsQueue.size() > 500) {
                eventsQueue.remove(0);
            }
            flushEvents();
        }
    }
}
