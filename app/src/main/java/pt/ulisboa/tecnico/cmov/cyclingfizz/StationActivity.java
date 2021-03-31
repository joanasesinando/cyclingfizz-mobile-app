package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;

public class StationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        Feature feature = Feature.fromJson(getIntent().getStringExtra(MapActivity.STATION_INFO));
        Log.d("oi", String.valueOf(feature));

        // Set top bar title & icon
        String name = feature.getProperty("desig_comercial").getAsString();
        TextView title = findViewById(R.id.map_info_name);
        title.setText(name);

        ImageView icon = findViewById(R.id.map_info_icon);
        icon.setImageResource(R.drawable.ic_station);

        // Set click listener for close btn
        MaterialToolbar materialToolbar = findViewById(R.id.map_info_bar);
        materialToolbar.setNavigationOnClickListener(this::closeBtnClicked);

        // Set bikes card info
        View card = findViewById(R.id.map_info_bikes);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(R.drawable.ic_map_info_bikes);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText("17"); // TODO
        TextView subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(getString(R.string.map_info_bikes));

        // Set free docks card info
        card = findViewById(R.id.map_info_free_docks);
        icon = card.findViewById(R.id.map_info_card_icon);
        icon.setImageResource(R.drawable.ic_map_info_free_docks);
        title = card.findViewById(R.id.map_info_card_title);
        title.setText("5"); // TODO
        subtitle = card.findViewById(R.id.map_info_card_subtitle);
        subtitle.setText(getString(R.string.map_info_free_docks));
    }

    public void closeBtnClicked(View view) {
        finish();
    }
}