package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class WelcomeActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Set light mode on for now
        // FIXME: remove when dark mode implemented
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Set status bar color
        Utils.setStatusBarColor(this, R.color.offWhite);
    }

    public void goToIntro1(View view) {
        Intent intent = new Intent(this, Intro1Activity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }
}