package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Point;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String STATIONS_SERVER_URL = "https://stations.cfservertest.ga";
//    public static String STATIONS_SERVER_URL = "https://3f0ac6bf1192.ngrok.io";
    public static String MAP_SERVER_URL = "https://map.cfservertest.ga";

    public static int MAX_FLAGS_FROM_BAN = 3;

    public static int THUMBNAIL_SIZE_SMALL = 128;
    public static int THUMBNAIL_SIZE_MEDIUM = 256;

    public static int GALLERY_IMAGE_SIZE_SMALL = 75;
    public static int GALLERY_IMAGE_SIZE_MEDIUM = 110;


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    static void forceLightModeOn() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    static void setStatusBarColor(Activity activity, int color) {
        Window window = activity.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(color);
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

    public static String getRealPathFromURIVideo(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Video.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static Bitmap retrieveVideoFrameFromVideo(String videoPath)throws Throwable
    {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try
        {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoPath, new HashMap<>());
            bitmap = mediaMetadataRetriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new Throwable("Exception in retrieveVideoFrameFromVideo(String videoPath)"+ e.getMessage());
        }
        finally
        {
            if (mediaMetadataRetriever != null)
            {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }


    /*** -------------------------------------------- ***/
    /*** ------------------ STRINGS ----------------- ***/
    /*** -------------------------------------------- ***/

    static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static boolean searchInString(String query, String target) {
        Pattern pattern = Pattern.compile(query.replace(" ", "(.)*"), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(target);
        return matcher.find();
    }


    /*** -------------------------------------------- ***/
    /*** ----------------- VALIDITY ----------------- ***/
    /*** -------------------------------------------- ***/

    public static boolean isValidEmail(CharSequence target) {
        return Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static boolean areAllTrue(Collection<Boolean> booleans) {
        for (boolean b : booleans) {
            if (!b) return false;
        }
        return true;
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
    /*** ------------------ GALLERY ----------------- ***/
    /*** -------------------------------------------- ***/

    public static int NO_COLOR = -1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static ViewGroup addImageToGallery(Activity activity, Bitmap bitmap, GridLayout gallery, int size,
                                   boolean isSelectable, int selectedColor) {

        final float scale = activity.getResources().getDisplayMetrics().density;
        final int padding = (int) (10 * scale);

        // Create wrapper
        ConstraintLayout imgWrapper = new ConstraintLayout(activity);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = (int) (size * scale);
        params.height = (int) (size * scale);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        imgWrapper.setLayoutParams(params);

        // Create image
        ImageView newImg = new ImageView(activity);
        newImg.setImageBitmap(bitmap);
        newImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams newImgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        newImg.setLayoutParams(newImgParams);
        imgWrapper.addView(newImg);

        if (isSelectable) {
            // Create overlay (when selected)
            LinearLayout overlay = new LinearLayout(activity);
            LinearLayout.LayoutParams overlayParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            overlay.setBackgroundColor(selectedColor);
            overlay.setAlpha(0.3f);
            overlay.setVisibility(View.GONE);
            imgWrapper.addView(overlay, overlayParams);

            // Create checked icon (when selected)
            ImageView icon = new ImageView(activity);
            icon.setImageResource(R.drawable.ic_round_check_circle_24);
            icon.setPadding(padding, padding, padding, padding);
            icon.setColorFilter(activity.getColor(R.color.white));
            icon.setVisibility(View.GONE);
            imgWrapper.addView(icon);
        }

        gallery.addView(imgWrapper, params);
        return imgWrapper;
    }

    public static void selectImage(GridLayout gallery, ArrayList<Integer> imagesToDeleteIndexes, View view, MaterialToolbar toolbar) {
        // Add index to delete
        imagesToDeleteIndexes.add(gallery.indexOfChild(view));

        // Update toolbar
        toolbar.setTitle(imagesToDeleteIndexes.size() + " selected");

        // Show overlay and check
        for (int i = 1; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            child.setVisibility(View.VISIBLE);
        }
    }

    public static void deselectImage(GridLayout gallery, ArrayList<Integer> imagesToDeleteIndexes, View view, MaterialToolbar toolbar) {
        // Remove index to delete
        imagesToDeleteIndexes.remove(gallery.indexOfChild(view));

        // Update toolbar
        toolbar.setTitle(imagesToDeleteIndexes.size() + " selected");

        // Hide overlay and check
        for (int i = 1; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            child.setVisibility(View.GONE);
        }
    }

    public static void deleteImages(GridLayout gallery, ArrayList<Integer> imagesToDeleteIndexes, ArrayList<Bitmap> images) {
        Collections.sort(imagesToDeleteIndexes, Collections.reverseOrder());

        for (int index : imagesToDeleteIndexes) {
            images.remove(index);
            gallery.removeViewAt(index);
        }
    }

    public static void quitDeletingImages(GridLayout gallery) {
        for (int i = 0; i < gallery.getChildCount(); i++) {
            View imgWrapper = gallery.getChildAt(i);

            for (int j = 1; j < ((ViewGroup) imgWrapper).getChildCount(); j++) {
                View child = ((ViewGroup) imgWrapper).getChildAt(j);
                child.setVisibility(View.GONE);
            }
        }
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


    public static class httpRequestVideoFile extends AsyncTask<String, Void, File> {

        private final OnTaskCompleted<File> callback;
        private final Cache cache;
        @SuppressLint("StaticFieldLeak")
        private final Context context;


        public httpRequestVideoFile(OnTaskCompleted<File> callback, Context context) {
            this.callback = callback;
            this.cache = Cache.getInstanceBigFiles();
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
        }

        private File readInputStreamToFile(InputStream inputStream) {
            File file;

            try {
                @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                file = File.createTempFile(timeStamp, null, context.getCacheDir());

                try (OutputStream output = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }

                    output.flush();
                }
                return file;

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected File doInBackground(String[] urls) {
            URL url;

            byte[] cachedValue = cache.get(urls[0]);


            if (cachedValue != null && cachedValue.length > 0) {
                Log.d(TAG, "Got " + urls[0] + " from cache");

                InputStream input = new ByteArrayInputStream(cachedValue);

                return readInputStreamToFile(input);
            }

            try {
                url = new URL(urls[0]);
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                byte[] bytes = IOUtils.toByteArray(input);

                File videoFile = readInputStreamToFile(input);

                cache.save(urls[0], bytes);

                return videoFile;

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());

                cachedValue = cache.get(urls[0]);

                if (cachedValue != null && cachedValue.length > 0) {
                    InputStream input = new ByteArrayInputStream(cachedValue);

                    return readInputStreamToFile(input);                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(File result) {
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
