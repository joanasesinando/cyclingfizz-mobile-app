package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

enum RoutesSelected {
    CREATED, PLAYED
}

public class ProfileActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@Profile";
    static String SERVER_URL = Utils.STATIONS_SERVER_URL;

    FirebaseAuth mAuth;

    ArrayList<Route> createdRoutes;
    ArrayList<Route> playedRoutes;

    RoutesSelected selected;

    int sortBy = R.id.sort_best_rate;
    TextInputLayout searchInput;
    String query = "";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        // TODO

        mAuth = FirebaseAuth.getInstance();
        uiInit();
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void uiInit() {
        // Set user info
        uiSetUserInfo();

        // Set routes created & played
        uiSetRoutesBtns();

        // Set routes
        updateRouteListView();

        // Set click listeners
        uiSetClickListeners();
    }

    private void uiSetUserInfo() {
        ImageView userAvatar = findViewById(R.id.profile_avatar);
        TextView userName = findViewById(R.id.profile_name);
        TextView userEmail = findViewById(R.id.profile_username);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            if (user.getPhotoUrl() != null) {
                (new Utils.httpRequestImage(bitmap -> {
                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
                    userAvatar.setImageBitmap(thumbImage);
                })).execute(user.getPhotoUrl().toString());
            }
            userEmail.setText(user.getEmail());
            userName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ?
                    user.getDisplayName() :
                    Utils.capitalize(Objects.requireNonNull(user.getEmail()).split("@")[0]));

        } else {
            Log.d(TAG, "Null user");
        }
    }

    @SuppressLint("SetTextI18n")
    private void uiSetRoutesBtns() {
        MaterialButton routesCreatedBtn = findViewById(R.id.profile_routes_created_selected);
        TextView routesPlayedBtn = findViewById(R.id.profile_routes_played);

        routesCreatedBtn.setText(createdRoutes.size() + " " + getString(R.string.routes_created));
        routesPlayedBtn.setText(playedRoutes.size() + " " + getString(R.string.routes_played));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateRouteListView() {
        if (selected == RoutesSelected.CREATED) {
            (new Utils.httpRequestJson(this::updateRouteListView)).execute(SERVER_URL + "/get-routes"); // FIXME: get routes created

        } else if (selected == RoutesSelected.PLAYED) {
            (new Utils.httpRequestJson(this::updateRouteListView)).execute(SERVER_URL + "/get-routes"); // FIXME: get routes played
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateRouteListView(JsonObject obj) {
        if (!obj.get("status").getAsString().equals("success")) return;

        ArrayList<Route> routes = new ArrayList<>();
        for (JsonElement routeJsonElement : obj.get("data").getAsJsonArray()) {
            JsonObject routeJson = routeJsonElement.getAsJsonObject();

            if (!routeJson.isJsonNull())
                routes.add(Route.fromJson(routeJson));
        }

        routes.sort(getSorter());

        // Init RecyclerView
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        RecyclerViewFragment fragment = new RecyclerViewFragment(routes, RecyclerViewFragment.DatasetType.ROUTES);
        transaction.replace(R.id.profile_list, fragment);
        transaction.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    private void uiSetClickListeners() {
        // Set toolbar click listeners
        MaterialToolbar toolbar = findViewById(R.id.profile_toolbar).findViewById(R.id.taller_top_bar);
        toolbar.setOnMenuItemClickListener(this::toolbarItemClicked);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set routes counters btns click listeners
        TextView routesCreatedBtn = findViewById(R.id.profile_routes_created);
        MaterialButton routesCreatedBtnSelected = findViewById(R.id.profile_routes_created_selected);
        TextView routesPlayedBtn = findViewById(R.id.profile_routes_played);
        MaterialButton routesPlayedBtnSelected = findViewById(R.id.profile_routes_played_selected);

        routesCreatedBtn.setOnClickListener(v -> {
            routesCreatedBtn.setVisibility(View.GONE);
            routesCreatedBtnSelected.setVisibility(View.VISIBLE);
            routesCreatedBtnSelected.setText(createdRoutes.size() + " " + getString(R.string.routes_created));

            routesPlayedBtnSelected.setVisibility(View.GONE);
            routesPlayedBtn.setVisibility(View.VISIBLE);
            routesPlayedBtn.setText(playedRoutes.size() + " " + getString(R.string.routes_played));

            // Show routes created
            updateRouteListView();
        });

        routesPlayedBtn.setOnClickListener(v -> {
            routesPlayedBtn.setVisibility(View.GONE);
            routesPlayedBtnSelected.setVisibility(View.VISIBLE);
            routesPlayedBtnSelected.setText(playedRoutes.size() + " " + getString(R.string.routes_played));

            routesCreatedBtnSelected.setVisibility(View.GONE);
            routesCreatedBtn.setVisibility(View.VISIBLE);
            routesCreatedBtn.setText(createdRoutes.size() + " " + getString(R.string.routes_created));

            // Show routes played
            updateRouteListView();
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
}