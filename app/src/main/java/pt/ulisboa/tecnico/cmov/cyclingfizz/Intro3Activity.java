package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Intro3Activity extends AppCompatActivity {

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro3);

        // Set status bar color
        Utils.setStatusBarColor(this, R.color.offWhite);

        Button start_btn = findViewById(R.id.btn_start_intro3);
        start_btn.setOnClickListener(this::btnStartClicked);
    }

    public void btnStartClicked(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_right_enter, R.anim.slide_right_leave);
    }
}