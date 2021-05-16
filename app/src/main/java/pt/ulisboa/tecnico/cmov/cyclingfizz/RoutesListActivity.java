package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MenuItem;
import android.widget.RadioGroup;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class RoutesListActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RoutesList";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

    Sidebar sidebar;
    FirebaseAuth mAuth;

    int sortBy = R.id.sort_best_rate;
    TextInputLayout searchInput;
    String query = "";


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        mAuth = FirebaseAuth.getInstance();
        uiInit();
    }

    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void uiInit() {
        // Set sidebar
        sidebar = new Sidebar(this);

        // Set routes
        updateRouteListView();

        // Set click listeners
        uiSetClickListeners();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateRouteListView() {
        getFlaggedRoutesId(flaggedRoutes -> {
            (new Utils.httpRequestJson(obj -> {
                if (!obj.get("status").getAsString().equals("success")) return;

                ArrayList<Route> routes = new ArrayList<>();
                for (JsonElement routeJsonElement : obj.get("data").getAsJsonArray()) {
                    JsonObject routeJson = routeJsonElement.getAsJsonObject();

                    if (!routeJson.isJsonNull()) {
                        Route route = Route.fromJson(routeJson);
                        if (Utils.searchInString(query, route.getTitle()) && !flaggedRoutes.contains(route.getId()))
                            routes.add(route);
                    }
                }

                routes.sort(getSorter());

                // Init RecyclerView
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                RecyclerViewFragment fragment = new RecyclerViewFragment(routes, RecyclerViewFragment.DatasetType.ROUTES);
                transaction.replace(R.id.routes_list, fragment);
                transaction.commit();

            })).execute(SERVER_URL + "/get-routes");
        });
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
        toolbar.setOnMenuItemClickListener(this::toolbarItemClicked);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean toolbarItemClicked(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.search) {
            View customDialog = LayoutInflater.from(this)
                    .inflate(R.layout.search_dialog, null, false);

            searchInput = customDialog.findViewById(R.id.search_text);
            Objects.requireNonNull(searchInput.getEditText()).setText(query);
            // Show end trip dialog
            new MaterialAlertDialogBuilder(this)
                    .setView(customDialog)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.search, (dialog, which) -> {
                        searchRoutes();
                    })
                    .show();

        } else if (id == R.id.sort) {
            View customDialog = LayoutInflater.from(this)
                    .inflate(R.layout.sort_dialog, null, false);
            RadioGroup radioGroup = customDialog.findViewById(R.id.sort_radio_group);
            radioGroup.check(sortBy);

            // Show sort dialog
            new MaterialAlertDialogBuilder(this)
                    .setView(customDialog)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.apply, (dialog, which) -> {
                        sortBy = radioGroup.getCheckedRadioButtonId();
                        updateRouteListView();
                    })
                    .show();
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void searchRoutes() {
        // Get message
        query = Objects.requireNonNull(searchInput.getEditText()).getText().toString();

        updateRouteListView();
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


    /*** -------------------------------------------- ***/
    /*** ------------ ACTIVITY LIFECYCLE ------------ ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRestart() {
        super.onRestart();
        updateRouteListView();
    }
}