package com.klaviyo.klaviyoandroid;

import android.app.IntentService;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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
import java.util.concurrent.ExecutionException;

import android.content.Intent;

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
    protected  static final String KL_GCM_Metadata = "$kl_metadata";

    // API Endpoints
    private static final String KLAVIYO_SERVER_URL_STRING = "https://a.klaviyo.com/api";
    private static final String KLAVIYO_SERVER_TRACK_ENDPOINT = "/track";
    private static final String KLAVIYO_SERVER_IDENTIFY_ENDPOINT = "/identify";

    // Alternative tracking keys
    private static final String KL_PERSON_ID_KEY = "$id";
    private static final String KL_PERSON_DEVICE_ID_KEY = "$device_id";
    private static final String CUSTOMER_PROPERTIES_ID_KEY = "$anonymous";

    private static final String KL_EVENT_ID_KEY = "$event_id";
    private static final String KL_EVENT_VALUE_KEY = "$value";
    private static final String KL_EVENT = "$event";

    private static final String CUSTOMER_PROPERTIES_APPEND_KEY = "$append";
    private static final String CUSTOMER_PROPERTIES_GCM_TOKENS_KEY = "$android_tokens";
    private static final String KL_REGISTER_APN_TOKEN_EVENT = "KL_ReceiveNotificationsDeviceToken";
    private static final String LAUNCHER_CLASS_KEY = "$kl_push_open_activity";

    // GCM Push
    protected String senderID;
    protected Boolean remoteNotificationsEnabled = false;
    private String apnDeviceToken;
    private String LAUNCHER_CLASS;

    // Context is required for android access
    private static Context context;
    private static final int MAX_CONNECTIONS = 5;
    private static final String SHARED_PREF_KEY = "klaviyo";
    private static final String KLAVIYO_KEY = "klaviyoAPIKey";
    private static final String GCM_KEY = "gcmSenderKey";
    private static final String PUSH_ENABLED_KEY = "klaviyoPushEnabled";

    // Keys for IntentService Bundle
    private static final  String FINISH_SETUP_KEY = "setUpWithPublicAPIKey";
    private static final String UNARCHIVE_FLUSH = "$unarchiveAndFlush";
    private static final String ARCHIVE = "$archive";
    private  static final String TRACK_EVENT = "trackEvent";
    private static final String TRACK_PERSON = "trackPerson";
    private static final String DATE_INCLUDED = "isDateIncluded";
    private static final String DATE = "$date";

    // The internal sharedInstance
    private static Klaviyo sharedInstance = new Klaviyo( );

    // Event Handling
    private String apiKey;
    private String userEmail;
    private ArrayList<JSONObject> eventsQueue = new ArrayList<JSONObject>();
    private ArrayList<JSONObject> peopleQueue = new ArrayList<JSONObject>();

    //A private Constructor prevents any other class from instantiating
    private Klaviyo(){}

    /* Other methods protected by singleton-ness */

    /* Static 'instance' method */
    public static Klaviyo getInstance() {
        return sharedInstance;
    }

    protected static String getGCMSenderID() {
        return sharedInstance.senderID;
    }

    /* Set up for non-push users */
    public void setUpWithPublicAPIKey(String apiKey, Context context) {
        sharedInstance.apiKey = apiKey;
        sharedInstance.context = context;

        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(FINISH_SETUP_KEY, true);
        k.putExtra(KLAVIYO_KEY, apiKey);
        k.putExtra(PUSH_ENABLED_KEY, false);
        context.startService(k);
    }

    /* Users can pass in a string representing the activity they would like to launch upon notification open
    *  String has to be explicit, i.e. 'MainActivity' would fail, 'com.klaviyo.klaviyoplayground.MainActivity' works"
    * */
    public void setPushActivity(String activityName) {
        sharedInstance.LAUNCHER_CLASS = activityName;

        SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);

        SharedPreferences.Editor edit = pref.edit();
        edit.putString(LAUNCHER_CLASS_KEY, activityName);
        edit.apply();
    }

    /*  Used by the GCM Receiver to Trigger the designated activity */
    protected String getPushActivity() {
        if (sharedInstance.LAUNCHER_CLASS != null) {
            return sharedInstance.LAUNCHER_CLASS;
        } else {
            SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
            return pref.getString(LAUNCHER_CLASS_KEY, "none");
        }
    }

    /* Set up for push */
    public void setUpWithPublicAPIKey(String apiKey, Context context, String senderID) {
        sharedInstance.apiKey = apiKey;
        sharedInstance.context = context;
        sharedInstance.senderID = senderID;
        sharedInstance.remoteNotificationsEnabled = true;

        // set up for push
        Intent i = new Intent(context, KlaviyoRegistrationIntentService.class);
        context.startService(i);

        // set up the background thread service for flushing queues
        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(FINISH_SETUP_KEY, true);
        k.putExtra(KLAVIYO_KEY, apiKey);
        k.putExtra(GCM_KEY, senderID);
        k.putExtra(PUSH_ENABLED_KEY, true);
        context.startService(k);
    }
    protected boolean isApiKeySet() {
        return (sharedInstance.apiKey != null);
    }

    /* Archive data if connection drops */
    protected void stopKlaviyoTracking() {
        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(ARCHIVE, true);
        context.startService(k);
    }

    public void setUpUserEmail(String email) {
        sharedInstance.userEmail = email;
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

        if (!sharedInstance.isApiKeySet()) {
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
        sharedInstance.apnDeviceToken = deviceToken;

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
        k.putExtra(KL_PERSON_TRACK_TOKEN_KEY, true);
        k.putExtra(KL_PERSON_PROPERTIES_KEY, personDictionary.toString());
        context.startService(k);
    }

    /* Nested Service to handle Asynchronous Calling */
    public static class KlaviyoService extends IntentService {

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
            if (intent.getExtras().getBoolean(FINISH_SETUP_KEY)) {
                String apiKey = intent.getExtras().getString(KLAVIYO_KEY);
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
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
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
            flushQueue(sharedInstance.eventsQueue, KLAVIYO_SERVER_TRACK_ENDPOINT);
        }

        private void flushPeople() {
            flushQueue(sharedInstance.peopleQueue, KLAVIYO_SERVER_IDENTIFY_ENDPOINT);
        }

        /**
         flushQueue: Iterates through an array of events and produces the relevant API request
         - Parameter queue: an array of events
         - Parameter endpoint: the api endpoint /track or /identify
         */
        private void flushQueue(ArrayList<JSONObject> queue, String endPoint) {
            boolean isAbleToFlush = true;

            // If network is not available, archive the events
            if (!isNetworkAvailable(context)) {
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
                if (sharedInstance.eventsQueue.size() > 0) {
                    archiveEvents();
                }
                if (sharedInstance.peopleQueue.size() > 0) {
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
                for (JSONObject item : sharedInstance.eventsQueue) {
                    bw.write(item.toString());
                    bw.newLine();
                    sharedInstance.eventsQueue.remove(item);
                }
            } catch (IOException io) {
                io.printStackTrace();
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
                        sharedInstance.eventsQueue.add(newEvent);
                    } else {
                        sharedInstance.peopleQueue.add(newEvent);
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
            return "klaviyo-"+sharedInstance.apiKey+"-events";
        }

        private File eventFile() {
            String name = eventFileName();
            File dir = context.getApplicationContext().getFilesDir();
            return new File(dir, name);
        }
        private String peopleFileName() {
            return "klaviyo-"+sharedInstance.apiKey+"-people";
        }

        private File peopleFile() {
            String name = peopleFileName();
            File dir = context.getApplicationContext().getFilesDir();
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
                e.printStackTrace();
                return false;
            }
        }

        /* Helper Methods for Building the API Requests */
        private JSONObject updatePropertiesDictionary(JSONObject props) throws JSONException {
            JSONObject properties = props;

            // Email checks
            if (props.has(KL_PERSON_EMAIL_KEY)) {
                properties.put(KL_PERSON_EMAIL_KEY, props.getString(KL_PERSON_EMAIL_KEY));
            } else if (emailAddressExists()) {
                properties.put(KL_PERSON_EMAIL_KEY, sharedInstance.userEmail);
            } else {
                // TBD for anonymous profiles
            }

            // If push notifications are used, append them
            if (sharedInstance.apnDeviceToken != null) {
                JSONObject tokens = new JSONObject();
                tokens.put(CUSTOMER_PROPERTIES_GCM_TOKENS_KEY, sharedInstance.apnDeviceToken);
                properties.put(CUSTOMER_PROPERTIES_APPEND_KEY, tokens);
            }

            return properties;
        }

        private Boolean emailAddressExists() {
            return sharedInstance.userEmail.length() > 0;
        }

        private String apiRequestWithEndpoint(String endPoint, JSONObject params) {
            String urlString = sharedInstance.KLAVIYO_SERVER_URL_STRING+endPoint;

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
            trackPersonDict.put(KL_PERSON_TRACK_TOKEN_KEY, sharedInstance.apiKey);
            trackPersonDict.put(KL_EVENT_TRACK_SERVICE_KEY, "api");

            sharedInstance.peopleQueue.add(trackPersonDict);

            if (sharedInstance.peopleQueue.size() > 500) {
                sharedInstance.peopleQueue.remove(0);
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
            if (eventName == KL_PERSON_OPENED_PUSH) {
                service = "klaviyo";
            }

            // Create the main parameters dictionary
            JSONObject eventParameters = new JSONObject();
            eventParameters.put(KL_EVENT_TRACK_TOKEN_Key, sharedInstance.apiKey);
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

            sharedInstance.eventsQueue.add(eventParameters);

            if (sharedInstance.eventsQueue.size() > 500) {
                sharedInstance.eventsQueue.remove(0);
            }
            flushEvents();
        }
    }
}
