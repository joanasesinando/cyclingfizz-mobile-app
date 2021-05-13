package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.internal.NavigationMenuItemView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public final class Sidebar {

    static String TAG = "Cycling_Fizz@Sidebar";

    private final Activity activity;
    private final NavigationView sidebar;
    private NavigationMenuItemView menuItem = null;
    private boolean isOpen = false;

    public Sidebar(Activity activity) {
        this.activity = activity;
        this.sidebar = activity.findViewById(R.id.sidebar);

        if (sidebar != null) {
            sidebar.setNavigationItemSelectedListener(this::itemClicked);
        }
    }

    public boolean itemClicked(MenuItem item) {
        Log.d(TAG, "Clicked on \"" + item.getTitle() + "\"");

        int id = item.getItemId();
        Log.d(TAG + "itemclicked", String.valueOf(menuItem));
        if (id == menuItem.getId()) return false;

        if (id == R.id.sidebar_logout) {
            FirebaseAuth.getInstance().signOut();
            changeUserUI();
            return false;

        } else if (id == R.id.sidebar_routes) {
            toggleSidebar();
            Intent intent = new Intent(activity, RoutesListActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
            return true;

        } else if (id == R.id.sidebar_map) {
            toggleSidebar();
            Intent intent = new Intent(activity, MapActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
            return true;

        } else {
            return false;
        }
    }

    @SuppressLint("RestrictedApi")
    public void toggleSidebar() {
        RelativeLayout overlay = activity.findViewById(R.id.overlay);

        // MapActivity
        FloatingActionButton layersBtn = activity.findViewById(R.id.btn_map_layers);
        FloatingActionButton bearingBtn = activity.findViewById(R.id.btn_map_bearing);
        FloatingActionButton locationBtn = activity.findViewById(R.id.btn_map_current_location);
        FloatingActionButton addPOIBtn = activity.findViewById(R.id.btn_map_add_poi);
        FloatingActionButton stopBtn = activity.findViewById(R.id.btn_map_stop);
        ExtendedFloatingActionButton flagRecording = activity.findViewById(R.id.flag_recording);
        ExtendedFloatingActionButton flagPlaying = activity.findViewById(R.id.flag_playing);
        ExtendedFloatingActionButton startRecordingBtn = activity.findViewById(R.id.btn_map_record_route);
        FloatingActionButton cancelRecordingBtn = activity.findViewById(R.id.btn_cancel_recording);
        View rentingMenu = activity.findViewById(R.id.renting_info);

        // RoutesListActivity
        FloatingActionButton createRouteBtn = activity.findViewById(R.id.btn_create_route);

        if (isOpen) {  // close it
            sidebar.animate().translationX(-(sidebar.getWidth()));
            overlay.setVisibility(View.GONE);

            if (activity instanceof MapActivity) {
                layersBtn.setVisibility(View.VISIBLE);
                locationBtn.setVisibility(View.VISIBLE);

                PathRecorder pathRecorder = PathRecorder.getInstance();
                PathPlayer pathPlayer = PathPlayer.getInstance();
                SharedState sharedState = (SharedState) activity.getApplicationContext();

                if (pathRecorder.isRecording()) {
                    addPOIBtn.setVisibility(View.VISIBLE);
                    stopBtn.setVisibility(View.VISIBLE);
                    flagRecording.setVisibility(View.VISIBLE);

                } else if (pathRecorder.isPreparingToRecord()) {
                    startRecordingBtn.setVisibility(View.VISIBLE);
                    cancelRecordingBtn.setVisibility(View.VISIBLE);

                } else if (sharedState.isRenting()) {
                    rentingMenu.setVisibility(View.VISIBLE);

                } else if (pathPlayer.isPlayingRoute()) {
                    flagPlaying.setVisibility(View.VISIBLE);
                    stopBtn.setVisibility(View.VISIBLE);
                }

            } else if (activity instanceof RoutesListActivity) {
                createRouteBtn.setVisibility(View.VISIBLE);
            }

        } else {    // open it
            sidebar.animate().translationX(0);
            overlay.setVisibility(View.VISIBLE);

            if (activity instanceof MapActivity) {
                menuItem = activity.findViewById(R.id.sidebar_map);
                layersBtn.setVisibility(View.GONE);
                bearingBtn.setVisibility(View.GONE);
                locationBtn.setVisibility(View.GONE);
                rentingMenu.setVisibility(View.GONE);
                addPOIBtn.setVisibility(View.GONE);
                stopBtn.setVisibility(View.GONE);
                flagRecording.setVisibility(View.GONE);
                flagPlaying.setVisibility(View.GONE);
                startRecordingBtn.setVisibility(View.GONE);
                cancelRecordingBtn.setVisibility(View.GONE);

            } else if (activity instanceof RoutesListActivity) {
                menuItem = activity.findViewById(R.id.sidebar_routes);
                createRouteBtn.setVisibility(View.GONE);
            }

            if (menuItem != null) {
                menuItem.setChecked(true);
                menuItem.getItemData().setChecked(true);
            }

            overlay.setOnClickListener(item -> {
                Log.d(TAG, String.valueOf(activity));
                toggleSidebar();
            });
            LinearLayout sidebarUser = activity.findViewById(R.id.sidebar_user);

            changeUserUI();
            sidebarUser.setOnClickListener(v -> {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Intent intent = new Intent(activity, LoginActivity.class);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
                }
            });
        }
        isOpen = !isOpen;
    }

    public void changeUserUI() {
        TextView userEmail = activity.findViewById(R.id.logged_user_email);
        TextView userName = activity.findViewById(R.id.logged_user_name);
        ImageView userAvatar = activity.findViewById(R.id.logged_user_avatar);
        NavigationMenuItemView logoutBtn = activity.findViewById(R.id.sidebar_logout);

        if (userEmail == null || userName == null || userAvatar == null || logoutBtn == null)
            return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            userAvatar.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_default_avatar));
            userEmail.setText(R.string.sign_in_msg);
            userName.setText(R.string.sign_in);
            logoutBtn.setVisibility(View.GONE);

        } else {
            if (user.getPhotoUrl() != null) {
                (new Utils.httpRequestImage(bitmap -> {
                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 128, 128);
                    userAvatar.setImageBitmap(thumbImage);
                })).execute(user.getPhotoUrl().toString());
            } else {
                userAvatar.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_default_avatar));
            }
            userEmail.setText(user.getEmail());
            userName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ?
                    user.getDisplayName() :
                    Utils.capitalize(Objects.requireNonNull(user.getEmail()).split("@")[0]));
            logoutBtn.setVisibility(View.VISIBLE);
        }
    }
}
