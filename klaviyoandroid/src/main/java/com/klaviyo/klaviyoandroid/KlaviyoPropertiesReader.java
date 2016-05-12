package com.klaviyo.klaviyoandroid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * Created by Klaviyo on 5/12/16.
 */
public class KlaviyoPropertiesReader {
        private Context context;
        private Properties properties;

        public KlaviyoPropertiesReader(Context context) {
            this.context = context;
            properties = new Properties();
        }

        public Properties getProperties(String FileName) {
            try {
                /**
                 * getAssets() Return an AssetManager instance for your
                 * application's package. AssetManager Provides access to an
                 * application's raw asset files;
                 */
                AssetManager assetManager = context.getAssets();
                InputStream inputStream = assetManager.open(FileName);
                properties.load(inputStream);

            } catch (IOException e) {
                Log.e("KlaviyoPropertyReader", e.toString());
            }
            return properties;
        }

}

