package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RoutesListActivity extends AppCompatActivity {

    Sidebar sidebar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        // Set sidebar
        sidebar = new Sidebar(this);

        // Set menu click listener for sidebar opening/closing
        MaterialToolbar toolbar = findViewById(R.id.routes_list_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> sidebar.toggleSidebar(v));

        // Set create route btn click listener
        FloatingActionButton createRouteBtn = findViewById(R.id.btn_create_route);
        createRouteBtn.setOnClickListener(v -> {
            PathRecorder pathRecorder = PathRecorder.getInstance();
            if (!pathRecorder.isRecording()) {
                pathRecorder.setPreparingToRecord(true);
                Intent intent = new Intent(this, MapActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
            }
        });
    }
}