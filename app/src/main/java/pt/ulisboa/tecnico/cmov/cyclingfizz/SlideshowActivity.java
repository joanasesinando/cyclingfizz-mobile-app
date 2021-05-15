package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class SlideshowActivity extends AppCompatActivity {

    float initialX;
    ArrayList<Bitmap> images;
    int startIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.forceLightModeOn(); // FIXME: remove when dark mode implemented
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slideshow);

        Intent intent = getIntent();
        images = ((SharedState) getApplicationContext()).slideshowImages;
        startIndex = intent.getIntExtra("index", 0);

        // Set images
        uiUpdateImages();

        // Set first image
        ViewFlipper viewFlipper = findViewById(R.id.view_flipper);
        viewFlipper.setDisplayedChild(startIndex);
        setCounter();

        // Change toolbar title
        MaterialToolbar toolbar = findViewById(R.id.slideshow_toolbar).findViewById(R.id.backBar);
        toolbar.setTitle("This is a drill"); // TODO

        // Set click listener for back btn
        toolbar.setNavigationOnClickListener(v -> finish());
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
        if (images.size() == 1) return false;

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