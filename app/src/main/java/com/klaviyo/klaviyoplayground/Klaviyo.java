package com.klaviyo.klaviyoplayground;

import android.app.IntentService;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import android.content.Intent;

/**
 * Created by katherinekeuper on 4/26/16.
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

    // TBD: Extract to be package independent (i.e. no klaviyoplayground needed)
    public static final String KL_GCM_OPEN = "com.klaviyo.klaviyoplayround.GCM_OPEN";
    protected  static final String KL_GCM_Metadata = "$kl_metadata";

    // API Endpoints:
    //https://a.klaviyo.com/api
    private static final String KLAVIYO_SERVER_URL_STRING = "https://e2d0947f.ngrok.io/api";
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
    private static final String CUSTOMER_PROPERTIES_APN_TOKENS_KEY = "$ios_tokens";
    private static final String KL_REGISTER_APN_TOKEN_EVENT = "KL_ReceiveNotificationsDeviceToken";

    // GCM Push
    protected String senderID;
    protected Boolean remoteNotificationsEnabled = false;
    private String apnDeviceToken;

    // Context is required for android access
    private static Context context;
    private static final int MAX_CONNECTIONS = 5;
    private static final String SHARED_PREF_KEY = "klaviyo";
    private static final String KLAVIYO_KEY = "klaviyoAPIKey";
    private static final String GCM_KEY = "gcmSenderKey";
    private static final String PUSH_ENABLED_KEY = "klaviyoPushEnabled";
    private static final  String FINISH_SETUP_KEY = "setUpWithPublicAPIKey";
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
    protected static Klaviyo getInstance() {
        return sharedInstance;
    }

    /* Set up for non-push users */
    protected void setUpWithPublicAPIKey(String apiKey, Context context) {
        sharedInstance.apiKey = apiKey;
        sharedInstance.context = context;

        Intent k = new Intent(context, KlaviyoService.class);
        k.putExtra(FINISH_SETUP_KEY, true);
        k.putExtra(KLAVIYO_KEY, apiKey);
        k.putExtra(PUSH_ENABLED_KEY, false);
        context.startService(k);
    }

    /* Set up for push */
    protected void setUpWithPublicAPIKey(String apiKey, Context context, String senderID) {
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

    /* If users would like to dictate archiving/safety of data */
    protected void stopKlaviyoTracking() {
        // start instance of service with STOP as the arg
        Intent k = new Intent(context, KlaviyoService.class);
       // k.putExtra(FINISH_SETUP_KEY, false);
        k.putExtra("$archive", true);
        context.startService(k);
    }

    protected void setUpUserEmail(String email) {
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
       // k.putExtra(FINISH_SETUP_KEY, false);
        k.putExtra(KL_EVENT, event);
        k.putExtra(KL_EVENT_TRACK_CUSTOMER_PROP_KEY, customerProperties.toString());
        k.putExtra(KL_EVENT_TRACK_PROPERTIES_KEY, propertiesDict.toString());
        k.putExtra(DATE, eventDate);
        k.putExtra(DATE_INCLUDED, didUserIncludeDate);
        k.putExtra(TRACK_EVENT, true);
        context.startService(k);
    }

    /**
     trackEvent: KL Event tracking for event name, customer & event properties

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
                System.out.println(e);
            }
        }
        if (eventDict.length() > 0) {
            try {
                trackEvent(KL_PERSON_OPENED_PUSH, eventDict);
            } catch (JSONException e){
                System.out.println(e);
            }
        }
    }

    protected void connectivityChanged(Context context) {
        Intent k = new Intent(context, KlaviyoService.class);

        if (!sharedInstance.isApiKeySet()) {
            // initialize service
            k.putExtra("$initialize", true);
        } else {
            // flush service
            k.putExtra("$unarchiveAndFlush", true);
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
            appendDict.put(CUSTOMER_PROPERTIES_APN_TOKENS_KEY, deviceToken);
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
    /* This gets started and stopped anytime tracking needs to take place */
    public static class KlaviyoService extends IntentService {
        public KlaviyoService() {
            super("KlaviyoService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            System.out.println("onhandleintent");

            // Can remove once this is thorougly tested
            boolean isOnMainThread = (Looper.myLooper() == Looper.getMainLooper());
            System.out.println("service is on main thread: " + isOnMainThread);

            // Finish Set Up
            if (intent.getExtras().getBoolean("setUpWithPublicAPIKey")) {
                String apiKey = intent.getExtras().getString(KLAVIYO_KEY);
                Boolean pushEnabled = intent.getExtras().getBoolean(PUSH_ENABLED_KEY);
                String gcmKey = (pushEnabled) ? intent.getExtras().getString(GCM_KEY) : "" ;
                finishKlaviyoSetUp(apiKey, gcmKey, pushEnabled);
            } else if (intent.getExtras().getBoolean(TRACK_EVENT)) {
                handleEventTracking(intent);
            } else if (intent.getExtras().getBoolean(TRACK_PERSON)) {
                handlePersonTracking(intent);
            } else if (intent.getExtras().getBoolean("$unarchiveAndFlush")) {
                unarchiveAndFlush();
            } else if (intent.getExtras().getBoolean("$archive")) {
                // set up from default settings
                archive();
            }
        }

        private void handleEventTracking(Intent intent) {
            boolean isNetworkReachable;

            if (isNetworkAvailable(context)) {
                try {
                    System.out.println("checking reachability");
                    isNetworkReachable = new KlaviyoReachabilityTask().execute().get();
                    // Grab the data and pass it
                    String eventName = intent.getExtras().getString(KL_EVENT);
                    try {
                        JSONObject customerProps = new JSONObject(intent.getExtras().getString(KL_EVENT_TRACK_CUSTOMER_PROP_KEY));
                        JSONObject properties = new JSONObject(intent.getExtras().getString(KL_EVENT_TRACK_PROPERTIES_KEY));
                        Date date;
                        if (intent.getExtras().getBoolean(DATE_INCLUDED)) {
                            date = (Date) intent.getExtras().get(DATE);
                        } else {
                            date = new Date();
                        }
                        // update last param
                        internalTrackEvent(eventName, customerProps, properties, date, false);
                    } catch (JSONException je) {
                        // shouldn't happen since we encoded it before to pass it to this method
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // something went wrong attempting the connection
                    isNetworkReachable = false;
                }
            } else {
                isNetworkReachable = false;
            }
        }

        private void handlePersonTracking(Intent intent) {
            boolean isNetworkReachable;
            if (isNetworkAvailable(context)) {
               try {
                   System.out.println("checking reachability");
                    isNetworkReachable = new KlaviyoReachabilityTask().execute().get();
                   try {
                       JSONObject personDict = new JSONObject(intent.getExtras().getString(KL_PERSON_PROPERTIES_KEY));
                       internalTrackPersonWithInfo(personDict);
                   } catch (JSONException je) {

                   }
                } catch (InterruptedException | ExecutionException e) {
                    // something went wrong attempting the connection
                    isNetworkReachable = false;
                }
            } else {
                isNetworkReachable = false;
            }
        }

        private void finishKlaviyoSetUp(String apiKey, String senderID, Boolean push) {
            saveUserInfo(apiKey, senderID, push);

            //If user is not connected don't flush anything
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            if (activeNetwork != null) {
                // unarchive data & send
                unarchive();
                flush();
            } else {
                System.out.println("no connection, don't send");
            }
        }

        private void saveUserInfo(String apiKey, String senderID, Boolean isPushEnabled) {
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(KLAVIYO_KEY, apiKey);
            edit.putString(GCM_KEY, senderID);
            edit.putBoolean(PUSH_ENABLED_KEY, isPushEnabled);
            edit.apply();
        }

        private void unarchiveAndFlush() {
            unarchive();
            flush();
        }

        /*
        * Beginning of Private Methods
        * */
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
            System.out.println("in flushQueue");
            System.out.println(queue);

            boolean isMainThread = (Looper.myLooper() == Looper.getMainLooper());
            System.out.println("currently on main thread is: " + isMainThread);

            ArrayList<JSONObject> currentQueue = queue;
            ArrayList<JSONObject> toRemove = new ArrayList<JSONObject>();

            for (JSONObject item : currentQueue) {
                String request = apiRequestWithEndpoint(endPoint, item);
                System.out.println("sending to klaviyo: " + item);
                // Execute in separate thread
                try {
                    String result = new InternalKlaviyoTask().execute(request).get();
                    System.out.println("executed:" + result);
                    if (result == "Connection Error") {
                        // don't do anything
                    } else {
                        // Remove event from the queue
                        System.out.println("removing item: " + item);
                        toRemove.add(item);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println(e);
                    // There was an error connecting. Leave the event in the queue
                }
            }
            queue.removeAll(toRemove);
        }

        private void archive() {
            try {
                System.out.println("archiving");
                archiveEvents();
                archivePeople();
            } catch (IOException io) {
                // ignoring write-to-file exceptions
            }
        }

        private void unarchive() {
            try {
                unarchiveEvents();
                unarchivePeople();
            } catch (IOException | JSONException e) {
            /* No file to unarchive or json encoding went awry (that should not happen) */
            }

        }

        private void unarchivePeople() throws  IOException, JSONException {
            File peopleFile = peopleFile();
            int length = (int) peopleFile.length();

            byte[] bytes = new byte[length];

            try {
                FileInputStream in = new FileInputStream(peopleFile);
                in.read(bytes);
                in.close();

                String contents = new String(bytes);
                JSONObject events = new JSONObject(contents);

                // temp. test writing the output
                System.out.println("unarchived file for people length of: " + events.length());
                System.out.println(events);

                // tbd; make sure this works
                sharedInstance.peopleQueue.add(events);

            } catch (FileNotFoundException fe) {
                // this is ok. means no files have been archived yet
            }
        }
        //  String contents = new String(bytes);
        private String eventFileName() {
            return "klaviyo-"+sharedInstance.apiKey+"-events";
        }

        private File eventFile() {
            String name = eventFileName();
            File dir = context.getApplicationContext().getFilesDir();
            return new File(dir, name);
        }
        private String peopleFileName() {
            return "klaviyo-"+sharedInstance.apiKey+"-events";
        }

        private File peopleFile() {
            String name = peopleFileName();
            File dir = context.getApplicationContext().getFilesDir();
            return new File(dir, name);
        }

        private void archiveEvents() throws IOException {
            File file = eventFile();
            FileOutputStream stream = new FileOutputStream(file);

            // If it doesn't exists, create a new file
            for (JSONObject item : sharedInstance.eventsQueue) {
                stream.write(item.toString().getBytes());
            }
            stream.close();
            System.out.println("archived events");
        }

        private void archivePeople() throws IOException {
            File file = peopleFile();
            FileOutputStream stream = new FileOutputStream(file);

            for (JSONObject item : sharedInstance.peopleQueue) {
                stream.write(item.toString().getBytes());
            }
            stream.close();
            System.out.println("archived people");
        }

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
        private JSONObject updatePropertiesDictionary(JSONObject props) throws JSONException {
            JSONObject properties = props;

            // Make sure email exists
            if (props.has(KL_PERSON_EMAIL_KEY)) {
                properties.put(KL_PERSON_EMAIL_KEY, props.getString(KL_PERSON_EMAIL_KEY));
            } else if (emailAddressExists()) {
                properties.put(KL_PERSON_EMAIL_KEY, sharedInstance.userEmail);
            } else {
                // use the DeviceID unique key: TBD for anonymous profiles
            }

            // If push notifications are used, append them
            if (sharedInstance.apnDeviceToken != null) {
                JSONObject tokens = new JSONObject();
                tokens.put(CUSTOMER_PROPERTIES_APN_TOKENS_KEY, sharedInstance.apnDeviceToken);
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

        protected void connectivityChanged(Context context) {
            if (!sharedInstance.isApiKeySet()) {
                System.out.println("api key not set");
                SharedPreferences pref = context.getSharedPreferences("klaviyo", Context.MODE_PRIVATE);
                String apiKey = pref.getString(KLAVIYO_KEY, "none");
                String senderID = pref.getString(GCM_KEY, "none");
                Boolean isPushEnabled = pref.getBoolean(PUSH_ENABLED_KEY, false);
                if (isPushEnabled) {
                    sharedInstance.setUpWithPublicAPIKey(apiKey, context, senderID);
                } else {
                    sharedInstance.setUpWithPublicAPIKey(apiKey, context);
                }
            }
            unarchive();
            flush();
        }

        private void unarchiveEvents() throws IOException, JSONException {
            File eventFile = eventFile();
            int length = (int) eventFile.length();

            byte[] bytes = new byte[length];

            try {
                FileInputStream in = new FileInputStream(eventFile);
                in.read(bytes);
                in.close();

                String contents = new String(bytes);
                JSONObject events = new JSONObject(contents);

                sharedInstance.eventsQueue.add(events);
            } catch (FileNotFoundException fe) {
                // this is ok. means no files have been archived yet
                System.out.println("No archived file");
            }
        }

        /**
         trackPersonWithInfo: method that creates a Klaviyo person tracking instance that is separate from an event

         - Parameter personInfoDictionary: dictionary of user attributes that you wish to track. These can be special properties provided by Klaviyo, such as KLPersonFirstNameDictKey, or created by the user on the fly.

         - Returns: Void
         */
        public void internalTrackPersonWithInfo(JSONObject personDictionary) throws JSONException {
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

            flush();
        }

        /**
         trackEvent: KL Event tracking using all possible parameters

         - Parameter eventName: name of the event
         - Parameter customerPropertiesDict: dictionary for user info
         - Parameter propertiesDict: dictionary for event info
         - Parameter eventDate: date of the event
         */
        public void internalTrackEvent(String event, JSONObject customerProperties, JSONObject propertiesDict, Date eventDate, boolean didUserIncludeDate) throws JSONException {

            String eventName = event;
            String service = "api";
            Boolean isNetworkReachable;

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

            /*  TBD: Code Review
            Do we want to automatically add event dates at time of attempted send?
            *  Currently we don't unless the user passes it in, but if the data gets archived and sent later
            *  it's timestamp will be off
            * */
            sharedInstance.eventsQueue.add(eventParameters);

            if (sharedInstance.eventsQueue.size() > 500) {
                sharedInstance.eventsQueue.remove(0);
            }
            flush();
        }
    }
}
