package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class Intro3Activity extends AppCompatActivity {

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro3);

        // Set status bar color
        Utils.setStatusBarColor(this, R.color.offWhite);
    }

    public void btnStartClicked(View view) {
//        Intent intent = new Intent(this, Intro2Activity.class);
//        startActivity(intent);
//        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_right_enter, R.anim.slide_right_leave);
    }
}