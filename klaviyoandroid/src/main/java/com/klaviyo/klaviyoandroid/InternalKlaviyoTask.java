package com.klaviyo.klaviyoandroid;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Created by katherinekeuper on 4/27/16.
 */
    /*Refactor to return Boolean*/
public class InternalKlaviyoTask extends AsyncTask<String, Void, Boolean> {

    @Override
    protected Boolean doInBackground(String... urls) {
        Boolean responseString = true;

        try {
            return sendRequest(urls[0]);
        } catch (IOException io) {

        }
        return responseString;
    }

    @Override
    protected  void onPostExecute(Boolean result) {
        super.onPostExecute(result);

    }

    private boolean isKlaviyoConnected() {
        try {
            InetAddress.getByName("klaviyo.com").isReachable(3000);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private Boolean sendRequest(String klURL) throws IOException {
        InputStream is = null;

        // look into refactoring response string to be a more concrete instance
        String responseString = "Connection Error";

        if (!isKlaviyoConnected()) {
            return false;
        } else {
            URL url = new URL(klURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            int response = connection.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                // this means the call was invalid
                return false;
            }
        }

    }
}