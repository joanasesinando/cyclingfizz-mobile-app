package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RouteActivity";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    Route route;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route);

        // Get route
        String routeID = getIntent().getStringExtra(RoutesListActivity.ROUTE_ID);
        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) {
                Toast.makeText(this, "Error: couldn't get route", Toast.LENGTH_LONG).show();
                return;
            }

            route = Route.fromJson(obj.get("data").getAsJsonObject());
            uiInit();
        })).execute(SERVER_URL + "/get-route-by-id?routeID=" + routeID);
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    private void uiInit() {
        // Set top bar
        uiUpdateTopBar(route.getTitle());

        // Set click listeners
        uiSetClickListeners();
    }

    private void uiUpdateTopBar(String name) {
        // Set top bar title
        TextView title = findViewById(R.id.taller_top_bar_name);
        title.setText(name);

        // Set top bar icon
        ImageView icon = findViewById(R.id.taller_top_bar_icon);
        icon.setImageResource(R.drawable.ic_route);
    }

    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.taller_top_bar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set share btn click listener
        // TODO
    }
}