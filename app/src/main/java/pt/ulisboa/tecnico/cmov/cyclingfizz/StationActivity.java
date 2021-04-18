package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;

enum TravelingMode {
    DRIVING("driving"), WALKING("walking"),
    PUBLIC_TRANSPORT("transit"), BIKE("bicycling");

    private final String label;

    TravelingMode(String label) { this.label = label; }

    public String getLabel() { return this.label; }
}

public class StationActivity extends AppCompatActivity implements SimWifiP2pManager.PeerListListener {

    SharedState sharedState;

    static String TAG = "Cycling_Fizz@StationActivity";
    public final static String COORDINATES = "pt.ulisboa.tecnico.cmov.cyclingfizz.COORDINATES";
    private final static String COUNTER_DEFAULT = "?";

    String STATIONS_SERVER_URL = "https://stations.cfservertest.ga";
    String GOOGLE_STREET_VIEW_URL = "https://maps.googleapis.com/maps/api/streetview";
    private FirebaseAuth mAuth;

    Point coord;
    String stationID;

    int numBikes;
    int numFreeDocks;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedState = (SharedState) getApplicationContext();

        mAuth = FirebaseAuth.getInstance();

        setContentView(R.layout.station);

        Feature feature = Feature.fromJson(getIntent().getStringExtra(MapActivity.STATION_INFO));
        stationID = feature.getProperty("id_expl").getAsString();
        coord = (Point) feature.geometry();

        uiInit(feature);
        getCurrentBikesAndFreeDocks();
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiInit(Feature feature) {
        // Set top bar
        uiUpdateTopBar(feature);

        // Set navigation estimates
        uiUpdateNavigationEstimates();

        // Set #bikes & #free_docks cards
        uiUpdateCard(findViewById(R.id.map_info_bikes), R.drawable.ic_map_info_bikes,
                COUNTER_DEFAULT, getString(R.string.map_info_bikes));
        uiUpdateCard(findViewById(R.id.map_info_free_docks), R.drawable.ic_map_info_free_docks,
                COUNTER_DEFAULT, getString(R.string.map_info_free_docks));

        // Set state card
        String state = feature.getProperty("estado").getAsString();
        uiUpdateCard(findViewById(R.id.map_info_state),
                state.equals("active") ? R.drawable.ic_map_info_active : R.drawable.ic_map_info_inactive,
                getString(R.string.map_info_state), Utils.capitalize(state), state);

        // Set street view thumbnail
        uiUpdateStreetViewThumbnail();

        // Set click listeners
        uiSetClickListeners();
    }

    private void uiUpdateTopBar(Feature feature) {
        // Set top bar title
        String name = feature.getProperty("desig_comercial").getAsString();
        TextView title = findViewById(R.id.map_info_name);
        title.setText(name);

        // Set top bar icon
        ImageView icon = findViewById(R.id.map_info_icon);
        icon.setImageResource(R.drawable.ic_station);
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
        ImageView icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(iconId);

        // Set card title
        TextView title = card.findViewById(R.id.map_info_card_title);
        title.setText(textTitle);

        // Set card subtitle
        TextView subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(textSubtitle);
    }

    private void uiUpdateCard(View card, @DrawableRes int iconId, CharSequence textTitle, CharSequence textSubtitle, String state) {
        uiUpdateCard(card, iconId, textTitle, textSubtitle);

        // Set state color
        TextView subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setTextColor(state.equals("active") ?
                getResources().getColor(R.color.success) : getResources().getColor(R.color.pink));
    }

    private void uiUpdateCardTitle(View card, CharSequence textTitle) {
        // Set card title
        TextView title = card.findViewById(R.id.map_info_card_title);
        title.setText(textTitle);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void uiUpdateStreetViewThumbnail() {
        ImageView thumbnail = findViewById(R.id.station_thumbnail);
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
        MaterialToolbar topBar = findViewById(R.id.map_info_bar);
        topBar.setNavigationOnClickListener(v -> finish());

        // Set street view thumbnail click listener
        ImageView thumbnail = findViewById(R.id.station_thumbnail);
        thumbnail.setOnClickListener(v -> {
            Intent intent = new Intent(this, StreetViewActivity.class);
            intent.putExtra(COORDINATES, coord.toJson());
            startActivity(intent);
            overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
        });

        MaterialButton rentBtn = findViewById(R.id.rent_bike);
        rentBtn.setOnClickListener(this::rentBike);

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


    /*** -------------------------------------------- ***/
    /*** --------------- BIKES RELATED -------------- ***/
    /*** -------------------------------------------- ***/

    private void getCurrentBikesAndFreeDocks() {
        (new Utils.httpRequestJson(obj -> {
            if (obj.get("status").getAsString().equals("success")) {
                JsonObject data = obj.get("data").getAsJsonObject();

                // Update counters
                numBikes = data.get("num_bikes").getAsInt();
                numFreeDocks = data.get("num_docks").getAsInt() - numBikes;

                // Update counters' views
                uiUpdateCardTitle(findViewById(R.id.map_info_bikes), String.valueOf(numBikes));
                uiUpdateCardTitle(findViewById(R.id.map_info_free_docks), String.valueOf(numFreeDocks));

                // Check for stations around
                checkForStationsInRange();

            } else {
                Log.e(TAG, "Could not get station's info");
            }
        })).execute(STATIONS_SERVER_URL + "/get-station-info?stationID=" + stationID);
    }

    public void getStationInfo(TextView numBikesView, TextView numDockView) {
        MaterialButton rentBtn = findViewById(R.id.rent_bike);

        (new Utils.httpRequestJson(obj -> {
            if (obj.get("status").getAsString().equals("success")) {
                JsonObject data = obj.get("data").getAsJsonObject();

                int num_bikes = data.get("num_bikes").getAsInt();
                int num_free_docks = data.get("num_docks").getAsInt() - num_bikes;

                rentBtn.setEnabled(num_bikes > 0);
                numBikesView.setText(String.valueOf(num_bikes));
                numDockView.setText(String.valueOf(num_free_docks));

                FirebaseUser user = mAuth.getCurrentUser();

                if (user != null) {
                    rentBtn.setEnabled(false);
                    user.getIdToken(true).addOnSuccessListener(result -> {
                        String idToken = result.getToken();

                        (new Utils.httpRequestJson(statusObj -> {

                            if (statusObj.get("status").getAsString().equals("success")) {
                                JsonObject statusData = statusObj.get("data").getAsJsonObject();

                                boolean renting = statusData.get("renting").getAsBoolean();
                                rentBtn.setEnabled(!renting && num_bikes > 0);

                            } else {
                                Log.e(TAG, "Could not get renting status");
                            }

                        })).execute(STATIONS_SERVER_URL + "/get-rent-status?idToken=" + idToken);
                    });
                }



            } else {
                Log.e(TAG, "Could not get Station Info");
            }

        })).execute(STATIONS_SERVER_URL + "/get-station-info?stationID=" + stationID);
    }

    public void rentBike(View view) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                (new Utils.httpRequestJson(obj -> {
                    sharedState.setRenting(true);
                    finish();
                })).execute(STATIONS_SERVER_URL + "/rent-a-bike?idToken=" + idToken + "&stationID=" + stationID);

            });

        } else {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sign_in_required)
                .setMessage(R.string.renting_dialog_warning)
                .setNeutralButton(R.string.cancel, (dialog, which) -> {
                    // Respond to neutral button press
                })
                .setPositiveButton(R.string.sign_in, (dialog, which) -> {
                    // Respond to positive button press
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
                })
                .show();
        }
    }


    /*** -------------------------------------------- ***/
    /*** ------- (WIFI DIRECT) STATIONS IN RANGE ---- ***/
    /*** -------------------------------------------- ***/

    private void checkForStationsInRange() {
        if (MapActivity.mBound) {
            MapActivity.mManager.requestPeers(MapActivity.mChannel, StationActivity.this);
        } else {
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList peers) {
        boolean isClose = false;

        for (SimWifiP2pDevice device : peers.getDeviceList()) {
            // Get beacon ID
            String beaconName = device.deviceName;
            String beaconID = beaconName.contains("_") ? beaconName.split("_")[1] : beaconName;

            if (beaconID.equals(stationID)) {
                isClose = true;
                Toast.makeText(this, "Station in range", Toast.LENGTH_SHORT).show();
                break;
            }
        }

        if (!isClose)
            Toast.makeText(this, "Station is far", Toast.LENGTH_SHORT).show();

        // Update rent btn
        MaterialButton rentBtn = findViewById(R.id.rent_bike);
        rentBtn.setEnabled(isClose && !sharedState.isRenting() && numBikes > 0);
    }
}