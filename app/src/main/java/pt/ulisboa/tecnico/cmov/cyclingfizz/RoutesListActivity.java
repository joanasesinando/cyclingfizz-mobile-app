package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;

public class RoutesListActivity extends AppCompatActivity {

    Sidebar sidebar;
    FirebaseAuth mAuth;
    static String TAG = "Cycling_Fizz@RoutesList";
    DecimalFormat oneDecimalFormatter = new DecimalFormat("#.0");



    static String SERVER_URL = "https://stations.cfservertest.ga";


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

                    TextView title = layout.findViewById(R.id.route_card_title);
                    TextView description = layout.findViewById(R.id.route_card_description);
                    TextView routeCardRateValue = layout.findViewById(R.id.route_card_rate_value);
                    ImageView routeCardRateIcon = layout.findViewById(R.id.route_card_rate_icon);

                    if (!routeJson.get("title").isJsonNull()) title.setText(routeJson.get("title").getAsString());
                    if (!routeJson.get("description").isJsonNull()) description.setText(routeJson.get("description").getAsString());


                    if (routeJson.get("rates") != null && !routeJson.get("rates").isJsonNull()) {
                        float rateAvg = 0;
                        JsonArray ratesJson = routeJson.get("rates").getAsJsonArray();
                        for (JsonElement rateJson : ratesJson) {
                            rateAvg += rateJson.getAsFloat() / ratesJson.size();
                        }

                        routeCardRateValue.setText(oneDecimalFormatter.format(rateAvg));
                        routeCardRateValue.setTextColor(getColorFromRate(rateAvg));
                        routeCardRateIcon.setColorFilter(getColorFromRate(rateAvg));
                    }

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
            }
        });
    }
}