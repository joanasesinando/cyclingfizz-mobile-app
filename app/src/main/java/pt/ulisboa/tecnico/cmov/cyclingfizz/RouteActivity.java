package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class RouteActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RouteActivity";
    static String SERVER_URL = "https://stations.cfservertest.ga";

    Route route;

    DecimalFormat oneDecimalFormatter = new DecimalFormat("#.0");

    @RequiresApi(api = Build.VERSION_CODES.M)
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uiInit() {
        // Set top bar
        uiUpdateTopBar(route.getTitle());

        // Set rate
        uiUpdateRate();

        // Set author
        updateAuthor("Joana Sesinando"); //FIXME: get author from route

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uiUpdateRate() {
        float rateAvg = 0;
        int rateCount = route.getRates().size();
        for (Integer rate : route.getRates()) {
            rateAvg += rate.floatValue() / rateCount;
        }
        Log.d(TAG, Arrays.toString(route.getRates().toArray()));

        // Update view
        TextView rateValue = findViewById(R.id.route_rate_value);
        rateValue.setText(oneDecimalFormatter.format(rateAvg));
        rateValue.setTextColor(getColorFromRate(rateAvg));

        ImageView rateIcon = findViewById(R.id.route_rate_icon);
        rateIcon.setColorFilter(getColorFromRate(rateAvg));

        // TODO: get #reviews
        TextView reviews = findViewById(R.id.route_nr_reviews);
        String s = "(" + rateCount + " reviews)";
        reviews.setText(s);
    }

    private void updateAuthor(String author) {
        TextView creator = findViewById(R.id.route_author);
        String s = getString(R.string.created_by) + " " + author;
        creator.setText(s);
    }

    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.taller_top_bar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set share btn click listener
        // TODO
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getColorFromRate(float rate) {
        if (rate < 2.5f) return getColor(R.color.pink);
        if (rate < 4.0f) return getColor(R.color.warning);
        return getColor(R.color.success);
    }
}