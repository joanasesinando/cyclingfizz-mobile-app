package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.mapbox.geojson.Feature;

public class StationActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
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
    }

    public void closeBtnClicked(View view) {
        finish();
    }
}