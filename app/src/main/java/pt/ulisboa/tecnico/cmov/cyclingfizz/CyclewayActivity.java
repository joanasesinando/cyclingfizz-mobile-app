package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;

public class CyclewayActivity extends AppCompatActivity {

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
    }

    public void closeBtnClicked(View view) {
        finish();
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