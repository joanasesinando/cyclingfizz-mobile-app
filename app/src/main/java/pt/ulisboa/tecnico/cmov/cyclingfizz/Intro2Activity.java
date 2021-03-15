package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Intro2Activity extends AppCompatActivity {

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro2);

        // Set status bar color
        Utils.setStatusBarColor(this, R.color.offWhite);

        Button skip_btn = findViewById(R.id.btn_skip_intro2);
        skip_btn.setOnClickListener(this::btnSkipClicked);
    }

    public void btnSkipClicked(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }

    public void goToIntro3(View view) {
        Intent intent = new Intent(this, Intro3Activity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_right_enter, R.anim.slide_right_leave);
    }
}