package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class SlideshowActivity extends AppCompatActivity {

    float initialX;
    ArrayList<Bitmap> images;
    int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("OI", "wtf");
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slideshow);

        Intent intent = getIntent();
        images = ((SharedState) getApplicationContext()).slideshowImages;
        index = intent.getIntExtra("index", 0);

        uiUpdateImages();

        // Change toolbar title
        MaterialToolbar toolbar = findViewById(R.id.slideshow_toolbar).findViewById(R.id.backBar);
        toolbar.setTitle("This is a drill");

        // Set click listener for back btn
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set first image
        ViewFlipper viewFlipper = findViewById(R.id.view_flipper);
        viewFlipper.setDisplayedChild(index);
        setCounter();
    }

    public void uiUpdateImages() {
        ViewFlipper viewFlipper = findViewById(R.id.view_flipper);

        for (Bitmap image : images) {
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(image);
            viewFlipper.addView(imageView);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent touchEvent) {
        ViewFlipper viewFlipper = findViewById(R.id.view_flipper);

        switch (touchEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = touchEvent.getX();
                break;

            case MotionEvent.ACTION_UP:
                float finalX = touchEvent.getX();
                if (initialX > finalX) {
                    viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_left_enter));
                    viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_left_leave));

                    viewFlipper.showNext();

                } else {
                    viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right_enter));
                    viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right_leave));

                    viewFlipper.showPrevious();
                }

                setCounter();
                break;
        }
        return false;
    }

    public void setCounter() {
        ViewFlipper viewFlipper = findViewById(R.id.view_flipper);
        TextView textView = findViewById(R.id.slideshow_counter);
        String s = viewFlipper.getDisplayedChild() + 1 + " / " + images.size();
        textView.setText(s);
    }
}