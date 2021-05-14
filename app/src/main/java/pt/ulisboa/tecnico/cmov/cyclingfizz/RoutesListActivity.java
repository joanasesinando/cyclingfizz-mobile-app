package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class RoutesListActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RoutesList";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
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

        getFlaggedRoutesId(flaggedRoutes -> {
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

                        Route route = Route.fromJson(routeJson);
                        if (route.isFlagged()) continue;
                        if (flaggedRoutes.contains(route.getId())) continue;

                        // Set thumbnail
                        if (routeJson.has("media_link")) {
                            (new Thread(() -> {
                                route.downloadImage(ignored -> {
                                    runOnUiThread(() -> {
                                        ImageView thumbnail = layout.findViewById(R.id.route_card_thumbnail);
                                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(route.getImage(), 128, 128);
                                        thumbnail.setImageBitmap(thumbImage);
                                    });
                                });
                            })).start();
                        }

                        // Set title & description
                        titleView.setText(route.getTitle());
                        descriptionView.setText(route.getDescription());

                        // Set avg rate
                        if (routeJson.get("reviews") != null && !routeJson.get("reviews").isJsonNull()) {
                            float rateSum = 0;
                            int rateCount = 0;

                            JsonObject reviewsJson = routeJson.get("reviews").getAsJsonObject();

                            for (String reviewID : reviewsJson.keySet()) {
                                route.addReviewFromJson(reviewsJson.get(reviewID).getAsJsonObject(), reviewID);
                            }

                            for (String reviewID : reviewsJson.keySet()) {
                                JsonElement reviewJson = reviewsJson.get(reviewID);
                                if (reviewJson.getAsJsonObject().has("rate")) {
                                    int rate = reviewJson.getAsJsonObject().get("rate").getAsInt();
                                    rateSum += rate;
                                    rateCount++;
                                }
                            }

                            float rateAvg = rateSum / rateCount;

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
                        });
                    }

                    linearLayout.addView(layout);
                }

            })).execute(SERVER_URL + "/get-routes");
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getColorFromRate(float rate) {
        if (rate < 2.5f) return getColor(R.color.pink);
        if (rate < 4.0f) return getColor(R.color.warning);
        return getColor(R.color.success);
    }

    private void getFlaggedRoutesId(Utils.OnTaskCompleted<ArrayList<String>> callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();
                (new Utils.httpRequestJson(response -> {
                    if (!response.get("status").getAsString().equals("success")) {
                        callback.onTaskCompleted(new ArrayList<>());
                        return;
                    }
                    ArrayList<String> flaggedRoutes = new ArrayList<>();
                    JsonArray flaggedRoutesJson = response.get("flagged_routes").getAsJsonArray();

                    for (JsonElement flaggedRouteJson : flaggedRoutesJson) {
                        flaggedRoutes.add(flaggedRouteJson.getAsString());
                    }

                    callback.onTaskCompleted(flaggedRoutes);
                })).execute(SERVER_URL + "/get-flagged-routes-by-user?idToken=" + idToken);

            });
        } else {
            callback.onTaskCompleted(new ArrayList<>());
        }
    }

    private void uiSetClickListeners() {
        // Set menu click listener for sidebar opening/closing
        MaterialToolbar toolbar = findViewById(R.id.routes_list_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> sidebar.toggleSidebar());

        // Set create route btn click listener
        FloatingActionButton createRouteBtn = findViewById(R.id.btn_create_route);
        createRouteBtn.setOnClickListener(v -> {
            if (PathPlayer.getInstance().isPlayingRoute()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.route_being_played)
                        .setMessage(R.string.route_being_played_warning)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                        })
                        .show();
                return;
            }

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


    /*** -------------------------------------------- ***/
    /*** ------------ ACTIVITY LIFECYCLE ------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRestart() {
        super.onRestart();

        LinearLayout linearLayout = findViewById(R.id.routes_list);
        linearLayout.removeAllViews();
        updateRouteListView();
    }
}