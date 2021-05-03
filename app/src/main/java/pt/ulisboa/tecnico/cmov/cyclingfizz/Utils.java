package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Message;
import android.util.JsonReader;
import android.util.Log;
import android.util.LruCache;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.util.IOUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

enum TravelingMode {
    DRIVING("driving"), WALKING("walking"),
    PUBLIC_TRANSPORT("transit"), BIKE("bicycling");

    private final String label;

    TravelingMode(String label) { this.label = label; }

    public String getLabel() { return this.label; }
}

public final class Utils {

    static String TAG = "Cycling_Fizz@Utils";


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static void setStatusBarColor(Activity activity, int color) {
        Window window = activity.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(activity,color));
    }

    public static void keepMenuOpen(MenuItem item, Context context) {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(context));
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return false;
            }
        });
    }


    /*** -------------------------------------------- ***/
    /*** ------------------ STRINGS ----------------- ***/
    /*** -------------------------------------------- ***/

    static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    /*** -------------------------------------------- ***/
    /*** ----------------- VALIDITY ----------------- ***/
    /*** -------------------------------------------- ***/

    public static boolean isValidEmail(CharSequence target) {
        return Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }


    /*** -------------------------------------------- ***/
    /*** -------------------- MAP ------------------- ***/
    /*** -------------------------------------------- ***/

    public static double distanceBetweenPointsInMeters(Point p1, Point p2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(p2.latitude() - p1.latitude());
        double lonDistance = Math.toRadians(p2.longitude() - p1.longitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(p1.latitude())) * Math.cos(Math.toRadians(p2.latitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = 0;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }


    /*** -------------------------------------------- ***/
    /*** ------------ AUTH & CREDENTIALS ------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    static String signRequest(String urlString, String SECRET) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException, URISyntaxException, MalformedURLException {

        // This variable stores the binary key, which is computed from the string (Base64) key
        byte[] key;

        // Convert the string to a URL so we can parse it
        URL url = new URL(urlString);

        // Convert the key from 'web safe' base 64 to binary
        String keyString = SECRET;
        keyString = keyString.replace('-', '+');
        keyString = keyString.replace('_', '/');
        key = Base64.getDecoder().decode(keyString);

        // Retrieve the proper URL components to sign
        String resource = url.getPath() + '?' + url.getQuery();

        // Get an HMAC-SHA1 signing key from the raw key bytes
        SecretKeySpec sha1Key = new SecretKeySpec(key, "HmacSHA1");

        // Get an HMAC-SHA1 Mac instance and initialize it with the HMAC-SHA1 key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(sha1Key);

        // compute the binary signature for the request
        byte[] sigBytes = mac.doFinal(resource.getBytes());

        // base 64 encode the binary signature
        // Base64 is JDK 1.8 only - older versions may need to use Apache Commons or similar.
        String signature = Base64.getEncoder().encodeToString(sigBytes);

        // convert the signature to 'web safe' base 64
        signature = signature.replace('+', '-');
        signature = signature.replace('/', '_');

        return url.getProtocol() + "://" + url.getHost() + resource + "&signature=" + signature;
    }


    /*** -------------------------------------------- ***/
    /*** --------------- HTTP REQUESTS -------------- ***/
    /*** -------------------------------------------- ***/



    public interface OnTaskCompleted<T> {
        void onTaskCompleted(T obj);
    }

    public static class httpRequestString extends AsyncTask<String, Void, String> {

        private final OnTaskCompleted<String> callback;
        private final Cache cache;

        public httpRequestString(OnTaskCompleted<String> callback) {
            this.callback = callback;
            this.cache = Cache.getInstanceSmallFiles();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String[] urls) {
            URL url;

            try {
                url = new URL(urls[0]);
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                BufferedReader r = new BufferedReader(new InputStreamReader(input));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }

                return total.toString();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());

            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            callback.onTaskCompleted(result);
        }
    }

    public static class httpRequestJson extends AsyncTask<String, Void, JsonObject> {

        private final OnTaskCompleted<JsonObject> callback;
        private final Cache cache;

        public httpRequestJson(OnTaskCompleted<JsonObject> callback) {
            this.callback = callback;
            this.cache = Cache.getInstanceSmallFiles();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JsonObject doInBackground(String[] urls) {
            URL url;



            try {
                url = new URL(urls[0]);
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                JsonObject jsonObject = JsonParser.parseReader( new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();

                cache.save(urls[0], jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                return jsonObject;

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());

                byte[] cachedValue = cache.get(urls[0]);

                if (cachedValue != null && cachedValue.length > 0) {
                    return JsonParser.parseString(new String(cachedValue, StandardCharsets.UTF_8)).getAsJsonObject();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JsonObject result) {
            callback.onTaskCompleted(result);
        }
    }

    public static class httpRequestImage extends AsyncTask<String, Void, Bitmap> {

        private final OnTaskCompleted<Bitmap> callback;
        private final Cache cache;


        public httpRequestImage(OnTaskCompleted<Bitmap> callback) {
            this.callback = callback;
            this.cache = Cache.getInstanceBigFiles();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Bitmap doInBackground(String[] urls) {
            URL url;
            byte[] cachedValue = cache.get(urls[0]);

            if (cachedValue != null && cachedValue.length > 0) {
                Log.d(TAG, "Got " + urls[0] + " from cache");
                Log.d(TAG, "Got value -> " + Arrays.toString(cachedValue));

                return BitmapFactory.decodeByteArray(cachedValue, 0, cachedValue.length);
            }

            try {
                url = new URL(urls[0]);
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                byte[] bytes = IOUtils.toByteArray(input);

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                cache.save(urls[0], bytes);

                return bitmap;

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());

                cachedValue = cache.get(urls[0]);

                if (cachedValue != null && cachedValue.length > 0) {
                    return BitmapFactory.decodeByteArray(cachedValue, 0, cachedValue.length);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            callback.onTaskCompleted(result);
        }
    }

    public static class httpPostRequestJson extends AsyncTask<String, Void, JsonObject> {

        private final OnTaskCompleted<JsonObject> callback;
        private final String jsonString;

        public httpPostRequestJson(OnTaskCompleted<JsonObject> callback, String jsonString) {
            this.callback = callback;
            this.jsonString = jsonString;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JsonObject doInBackground(String[] urls) {
            URL url;

            try {
                url = new URL(urls[0]);
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                OutputStream outputStream = connection.getOutputStream();
                byte[] inputJson = jsonString.getBytes(StandardCharsets.UTF_8);
                outputStream.write(inputJson, 0, inputJson.length);

                InputStream input = connection.getInputStream();

                return JsonParser.parseReader( new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JsonObject result) {
            callback.onTaskCompleted(result);
        }
    }
}
