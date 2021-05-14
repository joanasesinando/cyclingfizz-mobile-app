package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CyclewayActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@CyclewayActivity";
    public final static String COORDINATES = "pt.ulisboa.tecnico.cmov.cyclingfizz.COORDINATES";

    String GOOGLE_STREET_VIEW_URL = "https://maps.googleapis.com/maps/api/streetview";
    String GOOGLE_DISTANCE_URL = "https://maps.googleapis.com/maps/api/distancematrix";

    Point coord;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cycleway);

        Feature feature = Feature.fromJson(getIntent().getStringExtra(MapActivity.CYCLEWAY_INFO));
        JsonObject tags = feature.getProperty("tags").getAsJsonObject();
        coord = Point.fromJson(getIntent().getStringExtra(MapActivity.CYCLEWAY_INFO + ".point"));

        uiInit(tags);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiInit(JsonObject tags) {
        // Set top bar
        uiUpdateTopBar(tags.has("name") ?
                Utils.capitalize(tags.get("name").getAsString()) : getString(R.string.no_name));

        // Hide top bar menu
        MaterialToolbar topBar = findViewById(R.id.cycleway_toolbar).findViewById(R.id.taller_top_bar);
        topBar.getMenu().clear();

        // Set navigation estimates
        uiUpdateNavigationEstimates();

        // Set type & terrain cards
        uiUpdateCard(findViewById(R.id.map_info_type), R.drawable.ic_map_info_type, getString(R.string.map_info_type),
                tags.has("type") ? parseType(tags.get("type").getAsString()) : getString(R.string.not_available));
        uiUpdateCard(findViewById(R.id.map_info_terrain), R.drawable.ic_map_info_terrain, getString(R.string.map_info_terrain),
                tags.has("surface") ? Utils.capitalize(tags.get("surface").getAsString()) : getString(R.string.not_available));

        // Set street view thumbnail
        uiUpdateStreetViewThumbnail();

        // Set click listeners
        uiSetClickListeners();
    }

    private void uiUpdateTopBar(String name) {
        // Set top bar title
        TextView title = findViewById(R.id.taller_top_bar_name);
        title.setText(name);

        // Set top bar icon
        ImageView icon = findViewById(R.id.taller_top_bar_icon);
        icon.setImageResource(R.drawable.ic_cycleway);
    }

    private void uiUpdateNavigationEstimates() {
        Location userLocation = (Location) getIntent().getParcelableExtra(MapActivity.USER_LOCATION);

        TravelingMode[] travelingModes = TravelingMode.values();
        for (TravelingMode mode : travelingModes) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setTravelingModeEstimates(userLocation, coord, mode.getLabel());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                View modeCounter = findViewById(getResources()
                                        .getIdentifier("mode_" + mode.getLabel(), null, null));
                                modeCounter.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
            thread.start();
        }
    }

    private void uiUpdateCard(View card, @DrawableRes int iconId, CharSequence textTitle, CharSequence textSubtitle) {
        // Set card icon
        ImageView icon = card.findViewById(R.id.card_icon);
        icon.setImageResource(iconId);

        // Set card title
        TextView title = card.findViewById(R.id.card_title);
        title.setText(textTitle);

        // Set card subtitle
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        subtitle.setText(textSubtitle);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiUpdateStreetViewThumbnail() {
        ImageView thumbnail = findViewById(R.id.cycleway_thumbnail);
        String lat = String.valueOf(coord.latitude());
        String lon = String.valueOf(coord.longitude());

        try {
            String API_KEY = getString(R.string.google_API_KEY);
            String url = Utils.signRequest(GOOGLE_STREET_VIEW_URL + "?size=600x300&location=" + lat + "," + lon + "&key=" + API_KEY,
                    getString(R.string.google_signing_secret));
            (new Utils.httpRequestImage(thumbnail::setImageBitmap)).execute(url);

        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException | URISyntaxException | MalformedURLException e) {
            Log.e(TAG, e.getMessage());
            thumbnail.setVisibility(View.GONE);
        }
    }

    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar topBar = findViewById(R.id.taller_top_bar);
        topBar.setNavigationOnClickListener(v -> finish());

        // Set street view thumbnail click listener
        ImageView thumbnail = findViewById(R.id.cycleway_thumbnail);
        thumbnail.setOnClickListener(v -> {
            Intent intent = new Intent(this, StreetViewActivity.class);
            intent.putExtra(COORDINATES, coord.toJson());
            startActivity(intent);
            overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
        });

        // Set navigation btn click listener
        FloatingActionButton navBtn = (FloatingActionButton) findViewById(R.id.navigate_to);
        navBtn.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + coord.latitude() + "," + coord.longitude() + "&mode=w");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });
    }


    /*** -------------------------------------------- ***/
    /*** --------------- TRAVELING MODES ------------ ***/
    /*** -------------------------------------------- ***/

    public void setTravelingModeEstimates(Location origin, Point destination, String mode) throws IOException {
        // Make http request
        URL url = new URL(GOOGLE_DISTANCE_URL + "/json?origins=" + origin.getLatitude() + "," + origin.getLongitude() +
                "&destinations=" + destination.latitude() + "%2C" + destination.longitude() +
                "&mode=" + mode +
                "&language=en&key=" + getString(R.string.google_API_KEY));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());

        // Parse response
        JsonElement element = JsonParser.parseReader(new InputStreamReader(in));
        JsonObject json = element.getAsJsonObject();
        JsonObject info = json.get("rows").getAsJsonArray().get(0).getAsJsonObject().get("elements").getAsJsonArray().get(0).getAsJsonObject();
        String status = info.get("status").getAsString();
        String distanceText = status.equals("OK") ? info.get("distance").getAsJsonObject().get("text").getAsString() : null;
        String durationText = status.equals("OK") ? info.get("duration").getAsJsonObject().get("text").getAsString() : null;

        // Update views
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View item = null;
                ImageView icon;
                TextView duration;
                TextView distance;

                switch (mode) {
                    case "driving":
                        item = findViewById(R.id.mode_driving);
                        icon = item.findViewById(R.id.map_info_counter_icon);
                        icon.setImageResource(R.drawable.ic_baseline_directions_car_24);
                        break;

                    case "walking":
                        item = findViewById(R.id.mode_walking);
                        icon = item.findViewById(R.id.map_info_counter_icon);
                        icon.setImageResource(R.drawable.ic_round_directions_walk_24);
                        break;

                    case "transit":
                        item = findViewById(R.id.mode_transit);
                        icon = item.findViewById(R.id.map_info_counter_icon);
                        icon.setImageResource(R.drawable.ic_round_directions_bus_24);
                        break;

                    case "bicycling":
                        item = findViewById(R.id.mode_bicycling);
                        icon = item.findViewById(R.id.map_info_counter_icon);
                        icon.setImageResource(R.drawable.ic_round_directions_bike_24);
                        break;
                }

                urlConnection.disconnect();
                if (!status.equals("OK")) {
                    item.setVisibility(View.GONE);
                    return;
                }

                duration = item.findViewById(R.id.map_info_counter_duration);
                duration.setText(durationText);
                distance = item.findViewById(R.id.map_info_counter_distance);
                distance.setText(distanceText);
            }
        });
    }


    /*** -------------------------------------------- ***/
    /*** ------------------- PARSING --------------- ***/
    /*** -------------------------------------------- ***/

    private String parseType(String type) {
        switch (type) {
            case "shared_lane":
                return getString(R.string.map_info_type_shared_lane);

            case "segregated":
                return getString(R.string.map_info_type_segregated);

            case "shared_with_pedestrians":
                return getString(R.string.map_info_type_shared_pedestrians);

            case "other":
                return getString(R.string.map_info_type_shared_other);

            case "lane":
                return getString(R.string.map_info_type_lane);

            case "track":
                return getString(R.string.map_info_type_track);

            case "bridge":
                return getString(R.string.map_info_type_bridge);

            case "oneway":
                return getString(R.string.map_info_type_oneway);

            case "crossing":
                return getString(R.string.map_info_type_crossing);

            default:
                return getString(R.string.not_available);
        }
    }
}