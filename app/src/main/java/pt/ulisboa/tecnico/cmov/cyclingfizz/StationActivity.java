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

import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class StationActivity extends AppCompatActivity {

    static String APP_NAME_DEBUGGER = "Cycling_Fizz@StationActivity";
    public final static String COORDINATES = "pt.ulisboa.tecnico.cmov.cyclingfizz.COORDINATES";

    Point coord;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station);

        Feature feature = Feature.fromJson(getIntent().getStringExtra(MapActivity.STATION_INFO));

        // Set top bar title & icon
        String name = feature.getProperty("desig_comercial").getAsString();
        TextView title = findViewById(R.id.map_info_name);
        title.setText(name);

        ImageView icon = findViewById(R.id.map_info_icon);
        icon.setImageResource(R.drawable.ic_station);

        // Set click listener for close btn
        MaterialToolbar materialToolbar = findViewById(R.id.map_info_bar);
        materialToolbar.setNavigationOnClickListener(this::closeBtnClicked);

        // Calc num_bikes & num_free_docks
        int num_bikes = feature.getProperty("num_bicicletas").getAsInt();
        int num_free_docks = feature.getProperty("num_docas").getAsInt() - num_bikes;

        // Set bikes card info
        View card = findViewById(R.id.map_info_bikes);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(R.drawable.ic_map_info_bikes);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText(String.valueOf(num_bikes));
        TextView subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(getString(R.string.map_info_bikes));

        // Set free docks card info
        card = findViewById(R.id.map_info_free_docks);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(R.drawable.ic_map_info_free_docks);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText(String.valueOf(num_free_docks));
        subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(getString(R.string.map_info_free_docks));

        // Set state info
        String state = feature.getProperty("estado").getAsString();
        card = findViewById(R.id.map_info_state);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(state.equals("active") ? R.drawable.ic_map_info_active : R.drawable.ic_map_info_inactive);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText(getString(R.string.map_info_state));
        subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(Utils.capitalize(state));
        subtitle.setTextColor(state.equals("active") ? getColor(R.color.success) : getColor(R.color.pink));

        // Set thumbnail
        ImageView thumbnail = findViewById(R.id.station_thumbnail);
        coord = (Point) feature.geometry();
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
        String[] travelingModes = {"driving", "walking", "transit", "bicycling"};
        for (String mode : travelingModes) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setTravelingModeDistanceAndDuration(userLocation, coord, mode);
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
}