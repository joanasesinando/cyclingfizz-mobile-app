package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;

public class RoutesListActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RoutesList";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

    Sidebar sidebar;
    FirebaseAuth mAuth;
    DecimalFormat oneDecimalFormatter = new DecimalFormat("#.0");

    int sortBy = R.id.sort_best_rate;


    @RequiresApi(api = Build.VERSION_CODES.N)
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uiInit() {
        // Set sidebar
        sidebar = new Sidebar(this);

        // Set click listeners
        uiSetClickListeners();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateRouteListView() {
        getFlaggedRoutesId(flaggedRoutes -> {
            (new Utils.httpRequestJson(obj -> {
                if (!obj.get("status").getAsString().equals("success")) return;
                LinearLayout linearLayout = findViewById(R.id.routes_list);

                ArrayList<Route> routes = new ArrayList<>();

                for (JsonElement routeJsonElement : obj.get("data").getAsJsonArray()) {
                    JsonObject routeJson = routeJsonElement.getAsJsonObject();

                    if (!routeJson.isJsonNull()) {
                        routes.add(Route.fromJson(routeJson));
                    }
                }

                routes.sort(getSorter());

                for (Route route : routes) {
                    LayoutInflater inflater = LayoutInflater.from(this);
                    ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.route_card, null, false);

                    // Get views to update
                    TextView titleView = layout.findViewById(R.id.route_card_title);
                    TextView descriptionView = layout.findViewById(R.id.route_card_description);
                    TextView routeCardRateValueView = layout.findViewById(R.id.route_card_rate_value);
                    ImageView routeCardRateIconView = layout.findViewById(R.id.route_card_rate_icon);

                    if (flaggedRoutes.contains(route.getId())) continue;

                    // Set thumbnail
                    if (route.getMediaLink() != null) {
                        (new Thread(() -> {
                            route.downloadImage(ignored -> {
                                runOnUiThread(() -> {
                                    ImageView thumbnail = layout.findViewById(R.id.route_card_thumbnail);
                                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(route.getImage(), Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                                    thumbnail.setImageBitmap(thumbImage);
                                });
                            });
                        })).start();
                    }

                    // Set title & description
                    titleView.setText(route.getTitle());
                    descriptionView.setText(route.getDescription());

                    // Set avg rate
                    float rateAvg = route.getAvgRateNotFlagged();

                    routeCardRateValueView.setText(oneDecimalFormatter.format(rateAvg));
                    routeCardRateValueView.setTextColor(getColorFromRate(rateAvg));
                    routeCardRateIconView.setColorFilter(getColorFromRate(rateAvg));

                    layout.setOnClickListener(v -> {
                        Intent intent = new Intent(this, RouteActivity.class);
                        intent.putExtra(ROUTE_ID, route.getId());
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                    });


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

//     // Set sort btn click listener
//        MaterialButton sortBtn = findViewById(R.id.topAppBar).findViewById(R.id.sort);
//        sortBtn.setOnClickListener(view -> {
//            View customDialog = LayoutInflater.from(this)
//                    .inflate(R.layout.sort_dialog, null, false);
//            RadioGroup radioGroup = customDialog.findViewById(R.id.sort_radio_group);
//            radioGroup.check(sortBy);
//
//            // Show sort dialog
//            new MaterialAlertDialogBuilder(this)
//                    .setView(customDialog)
//                    .setNegativeButton(R.string.cancel, null)
//                    .setPositiveButton(R.string.apply, (dialog, which) -> {
//                        sortBy = radioGroup.getCheckedRadioButtonId();
//                        clearRoutes();
//                        updateRouteListView();
//                    })
//                    .show();
//        });


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

    @SuppressLint("NonConstantResourceId")
    Comparator<Route> getSorter() {
        switch (sortBy) {
            default:
            case R.id.sort_best_rate:
                return new Route.SortByBestRate();
            case R.id.sort_worst_rate:
                return new Route.SortByWorstRate();
            case R.id.sort_most_recent:
                return new Route.SortByMostRecent();
            case R.id.sort_least_recent:
                return new Route.SortByLeastRecent();
        }
    }


    public void clearRoutes() {
        LinearLayout linearLayout = findViewById(R.id.routes_list);
        linearLayout.removeAllViews();
    }

    /*** -------------------------------------------- ***/
    /*** ------------ ACTIVITY LIFECYCLE ------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRestart() {
        super.onRestart();

        clearRoutes();
        updateRouteListView();
    }
}