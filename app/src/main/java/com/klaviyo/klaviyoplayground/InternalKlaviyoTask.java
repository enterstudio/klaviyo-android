package com.klaviyo.klaviyoplayground;

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
public class InternalKlaviyoTask extends AsyncTask<String, Void, String> {
    protected boolean sentSuccessfully;

    @Override
    protected String doInBackground(String... urls) {
        String responseString = "true";

        try {
            return sendRequest(urls[0]);
        } catch (IOException io) {

        }
        return responseString;
    }

    @Override
    protected  void onPostExecute(String result) {
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

    private String sendRequest(String klURL) throws IOException {
        InputStream is = null;
        // look into refactoring response string to be a more concrete instance
        String responseString = "Connection Error";

        if (!isKlaviyoConnected()) {
            sentSuccessfully = false;
            return responseString;
        } else {
            try {
                URL url = new URL(klURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept-Encoding", "gzip");
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();
                int response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    sentSuccessfully = true;
                    BufferedReader bf = new BufferedReader((new InputStreamReader(connection.getInputStream())));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bf.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    bf.close();
                    System.out.println("api returned: " + sb.toString());
                } else {
                    // this means the call was invalid
                    sentSuccessfully = false;
                }
                responseString = "success";
            } finally {
                System.out.println("in finally");
            }
        }

        return responseString;
    }
}