package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class RoutesListActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RoutesList";
    static String SERVER_URL = "https://stations.cfservertest.ga";
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

    Sidebar sidebar;
    FirebaseAuth mAuth;
    DecimalFormat oneDecimalFormatter = new DecimalFormat("#.0");

    ArrayList<Route> routes = new ArrayList<>();


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        mAuth = FirebaseAuth.getInstance();
        uiInit();
        updateRouteListView();
    }

    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    private void uiInit() {
        // Set sidebar
        sidebar = new Sidebar(this);

        // Set click listeners
        uiSetClickListeners();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateRouteListView() {

        (new Utils.httpRequestJson(obj -> {
            if (!obj.get("status").getAsString().equals("success")) return;
            LinearLayout linearLayout = findViewById(R.id.routes_list);

            for (JsonElement routeJsonElement : obj.get("data").getAsJsonArray()) {
                JsonObject routeJson = routeJsonElement.getAsJsonObject();
                LayoutInflater inflater = LayoutInflater.from(this);
                ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.route_card, null, false);

                if (!routeJson.isJsonNull()) {

                    // Get views to update
                    TextView titleView = layout.findViewById(R.id.route_card_title);
                    TextView descriptionView = layout.findViewById(R.id.route_card_description);
                    TextView routeCardRateValueView = layout.findViewById(R.id.route_card_rate_value);
                    ImageView routeCardRateIconView = layout.findViewById(R.id.route_card_rate_icon);

                    // Set thumbnail
                    // TODO

                    // Set title & description
                    String title = routeJson.get("title") != null && !routeJson.get("title").isJsonNull() ? routeJson.get("title").getAsString() : null;
                    String description = routeJson.get("description") != null && !routeJson.get("description").isJsonNull() ? routeJson.get("description").getAsString() : null;
                    if (title != null) titleView.setText(title);
                    if (description != null) descriptionView.setText(description);

                    // Set avg rate
                    if (routeJson.get("rates") != null && !routeJson.get("rates").isJsonNull()) {
                        float rateAvg = 0;
                        JsonArray ratesJson = routeJson.get("rates").getAsJsonArray();
                        for (JsonElement rateJson : ratesJson) {
                            rateAvg += rateJson.getAsFloat() / ratesJson.size();
                        }

                        routeCardRateValueView.setText(oneDecimalFormatter.format(rateAvg));
                        routeCardRateValueView.setTextColor(getColorFromRate(rateAvg));
                        routeCardRateIconView.setColorFilter(getColorFromRate(rateAvg));
                    }

                    String routeID = routeJson.get("id") != null && !routeJson.get("id").isJsonNull() ? routeJson.get("id").getAsString() : null;

                    if (routeID != null) layout.setOnClickListener(v -> {
                        Intent intent = new Intent(this, RouteActivity.class);
                        intent.putExtra(ROUTE_ID, routeID);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);

//                        PathPlayer.getInstance().playRouteFromRouteId(routeJson.get("id").getAsString(), success -> {
//                            if (success) {
//                                finish();
//                            } else {
//                                Toast.makeText(this, "Error: Could not play route", Toast.LENGTH_LONG).show();
//                            }
//                        });
                    });
                }

                linearLayout.addView(layout);
            }

        })).execute(SERVER_URL + "/get-routes");


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getColorFromRate(float rate) {
        if (rate < 2.5f) return getColor(R.color.pink);
        if (rate < 4.0f) return getColor(R.color.warning);
        return getColor(R.color.success);

    }

    private void uiSetClickListeners() {
        // Set menu click listener for sidebar opening/closing
        MaterialToolbar toolbar = findViewById(R.id.routes_list_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> sidebar.toggleSidebar());

        // Set create route btn click listener
        FloatingActionButton createRouteBtn = findViewById(R.id.btn_create_route);
        createRouteBtn.setOnClickListener(v -> {
            PathRecorder pathRecorder = PathRecorder.getInstance();

            if (!pathRecorder.isRecording()) {
                FirebaseUser user = mAuth.getCurrentUser();

                if (user != null) {
                    pathRecorder.setPreparingToRecord(true);
                    Intent intent = new Intent(this, MapActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(R.anim.fade_out, R.anim.fade_in);

                } else {
                    new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.sign_in_required)
                        .setMessage(R.string.creating_route_dialog_warning)
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

            } else {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.already_recording)
                    .setMessage(R.string.already_recording_warning)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(this, MapActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
                    })
                    .show();
            }
        });
    }
}