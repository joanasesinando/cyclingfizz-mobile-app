package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
}