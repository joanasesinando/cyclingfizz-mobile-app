package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Objects;

public class LeaveCommentActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@LeaveComment";

    String routeID;
    PointOfInterest poi;

    TextInputLayout msgInput;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oi");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leave_comment);

        routeID = getIntent().getStringExtra(ViewPOIActivity.ROUTE_ID);
        poi = (PointOfInterest) getIntent().getSerializableExtra(MapPreviewActivity.POI);
        Log.d(TAG, routeID);
//        Log.d(TAG, String.valueOf(poi));

        setUI();
        setInput();
        uiSetClickListeners();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setUI() {
        // Set purple status bar
        getWindow().setStatusBarColor(getColor(R.color.purple_700));
    }

    private void uiSetClickListeners() {
        // Set close btn click listener
        MaterialToolbar toolbar = findViewById(R.id.leave_comment_toolbar).findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set toolbar action btn click listener
        toolbar.setOnMenuItemClickListener(item -> {
            addComment();
            return false;
        });

        // Set done btn click listener
        MaterialButton doneBtn = findViewById(R.id.add_comment);
        doneBtn.setOnClickListener(v -> addComment());
    }

    private void setInput() {
        msgInput = findViewById(R.id.leave_comment_message_input);

        Objects.requireNonNull(msgInput.getEditText()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                msgInput.setError(null);
            }
        });
    }

    private boolean checkForErrors(String msg) {
        boolean error = false;
        if (msg.isEmpty()) {
            msgInput.setError(getString(R.string.comment_required));
            error = true;
        }
        return error;
    }

    private void addComment() {
        // Clean error messages
        msgInput.setError(null);

        // Get message
        String message = Objects.requireNonNull(msgInput.getEditText()).getText().toString();

        // Check for errors
        boolean error = checkForErrors(message);

        if (!error) {
            //FIXME: null images
            poi.addComment(routeID, message, new ArrayList<>(), commentJson -> finish());
        }
    }
}