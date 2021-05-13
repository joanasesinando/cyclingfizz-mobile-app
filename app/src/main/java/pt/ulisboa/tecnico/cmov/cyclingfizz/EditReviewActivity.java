package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;

public class EditReviewActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@RouteActivity";

    Route route;
    int rate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leave_review);

        route = ((SharedState) getApplicationContext()).reviewingRoute;
        rate = getIntent().getIntExtra(RouteActivity.RATE, 0);

        uiInit();
    }


    /*** -------------------------------------------- ***/
    /*** -------------- USER INTERFACE -------------- ***/
    /*** -------------------------------------------- ***/

    private void uiInit() {
        uiSetRate();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void uiSetRate() {
        for (int i = 1; i <= 5; i++) {
            ImageView star = findViewById(getResources().getIdentifier("rate_star" + i, "id", getPackageName()));

            if (i <= rate) {
                star.setImageDrawable(getDrawable(R.drawable.ic_round_star_24));
                star.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.orange_500)));
            }

            int starInt = i;
            star.setOnClickListener(v -> {
                rate = starInt;
                uiSetRate();
            });
        }
    }
}