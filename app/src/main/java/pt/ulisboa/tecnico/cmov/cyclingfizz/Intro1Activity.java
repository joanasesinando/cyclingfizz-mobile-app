package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class Intro1Activity extends AppCompatActivity {

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro1);

        // Set status bar color
        Utils.setStatusBarColor(this, R.color.offWhite);
    }

    public void goToIntro2(View view) {
        Intent intent = new Intent(this, Intro2Activity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }
}