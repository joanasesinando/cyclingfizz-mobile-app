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
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
        LineString lineString = (LineString) feature.geometry();
        List<Point> points = lineString.coordinates();
        coord = points.get(Math.round((points.size() - 1) >> 1));
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