package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.squareup.picasso.Picasso;

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
import java.util.List;

public class CyclewayActivity extends AppCompatActivity {

    static String APP_NAME_DEBUGGER = "Cycling_Fizz@CyclewayActivity";
    public final static String COORDINATES = "pt.ulisboa.tecnico.cmov.cyclingfizz.COORDINATES";

     Point coord;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cycleway);

        Feature feature = Feature.fromJson(getIntent().getStringExtra(MapActivity.CYCLEWAY_INFO));
        JsonObject tags = feature.getProperty("tags").getAsJsonObject();
        coord = Point.fromJson(getIntent().getStringExtra(MapActivity.CYCLEWAY_INFO + ".point"));

        // Set top bar title & icon
        String name = tags.has("name") ? Utils.capitalize(tags.get("name").getAsString()) : getString(R.string.not_name);
        TextView title = findViewById(R.id.map_info_name);
        title.setText(name);

        ImageView icon = findViewById(R.id.map_info_icon);
        icon.setImageResource(R.drawable.ic_cycleway);

        // Set click listener for close btn
        MaterialToolbar materialToolbar = findViewById(R.id.map_info_bar);
        materialToolbar.setNavigationOnClickListener(this::closeBtnClicked);

        // Set type card info
        View card = findViewById(R.id.map_info_type);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(R.drawable.ic_map_info_type);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText(R.string.map_info_type);
        TextView subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(tags.has("type") ? parseType(tags.get("type").getAsString()) : getString(R.string.not_available));

        // Set terrain card info
        card = findViewById(R.id.map_info_terrain);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(R.drawable.ic_map_info_terrain);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText(R.string.map_info_terrain);
        subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(tags.has("surface") ? Utils.capitalize(tags.get("surface").getAsString()) : getString(R.string.not_available));

        // Set thumbnail
        ImageView thumbnail = findViewById(R.id.cycleway_thumbnail);
        String lat = String.valueOf(coord.latitude());
        String lon = String.valueOf(coord.longitude());
        try {
            String API_KEY = getString(R.string.google_API_KEY);
            String url = Utils.signRequest("https://maps.googleapis.com/maps/api/streetview?size=600x300&location=" + lat + "," + lon + "&key=" + API_KEY, getString(R.string.google_signing_secret));
            Picasso.get().load(url).into(thumbnail);
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException | URISyntaxException | MalformedURLException e) {
            Log.e(APP_NAME_DEBUGGER, e.getMessage());
            thumbnail.setVisibility(View.GONE);
        }

        // Set thumbnail click listener
        thumbnail.setOnClickListener(this::thumbnailClicked);

        // Set distance & times
        Location userLocation = (Location) getIntent().getParcelableExtra(MapActivity.USER_LOCATION);
        Log.d("user_location", String.valueOf(userLocation));
        String[] travelingModes = {"driving", "walking", "transit", "bicycling"};
        for (String mode : travelingModes) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setTravelingModeDistanceAndDuration(userLocation, coord, mode); // FIXME: currently picking the point clicked
                    } catch (IOException e) {
                        Log.e(APP_NAME_DEBUGGER, e.getMessage());
                    }
                }
            });
            thread.start();
        }
    }

    public void closeBtnClicked(View view) {
        finish();
    }

    public void thumbnailClicked(View view) {
        Intent intent = new Intent(this, StreetViewActivity.class);
        intent.putExtra(COORDINATES, coord.toJson());
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }

    public void setTravelingModeDistanceAndDuration(Location origin, Point destination, String mode) throws IOException {
        // Make http request
        URL url = new URL("https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                origin.getLatitude() + "," + origin.getLongitude() +
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
                return null;
        }
    }
}