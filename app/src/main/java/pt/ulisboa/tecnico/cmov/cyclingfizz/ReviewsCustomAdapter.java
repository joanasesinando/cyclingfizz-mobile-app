package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonElement;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
@SuppressLint("LongLogTag")
public class ReviewsCustomAdapter extends RecyclerView.Adapter<ReviewsCustomAdapter.ViewHolder> {
    private static final String TAG = "Cycling_Fizz@ReviewsCustomAdapter";

    private final Activity mActivity;
    private final ArrayList<Route.Review> mDataSet;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatar;
        private final TextView name;
        private final TextView message;
        private final GridLayout gallery;
        private final ImageView rateIcon;
        private final TextView rateValue;
        private final TextView date;

        public ViewHolder(Activity activity, View v, ArrayList<Route.Review> dataset) {
            super(v);

            avatar = v.findViewById(R.id.review_item_avatar);
            name = v.findViewById(R.id.review_item_name);
            message = v.findViewById(R.id.review_item_comment);
            gallery = v.findViewById(R.id.review_item_gallery);
            rateIcon = v.findViewById(R.id.review_item_rate_icon);
            rateValue = v.findViewById(R.id.review_item_rate_value);
            date = v.findViewById(R.id.review_item_date);

            // Set click listener
            v.setOnClickListener(view -> {
                Route.Review review = dataset.get(getAdapterPosition());

                // Set context menu for flagging as inappropriate
                view.setTag(review);
                activity.registerForContextMenu(view);
            });
        }
        // END_INCLUDE(recyclerViewSampleViewHolder)

        public ImageView getAvatar() {
            return avatar;
        }

        public TextView getName() {
            return name;
        }

        public TextView getMessage() {
            return message;
        }

        public GridLayout getGallery() {
            return gallery;
        }

        public ImageView getRateIcon() {
            return rateIcon;
        }

        public TextView getRateValue() {
            return rateValue;
        }

        public TextView getDate() {
            return date;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet ArrayList<Review> containing the data to populate views to be used by RecyclerView.
     */
    public ReviewsCustomAdapter(Activity activity, ArrayList<Route.Review> dataSet) {
        mActivity = activity;
        mDataSet = dataSet;
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.review_item, viewGroup, false);

        return new ViewHolder(mActivity, v, mDataSet);
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    // BEGIN_INCLUDE(recyclerViewOnBindViewHolder)
    // Replace the contents of a view (invoked by the layout manager)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        Route.Review review = mDataSet.get(position);

        // Set avatar & name
        if (review.getUserName() == null || review.getUserAvatar() == null) {

            // Get name & avatar
            (new Utils.httpRequestJson(obj -> {
                if (!obj.get("status").getAsString().equals("success")) return;

                String userName = obj.get("data").getAsJsonObject().get("name").getAsString();
                viewHolder.getName().setText(userName);
                review.setUserName(userName);

                JsonElement avatarURLElement = obj.get("data").getAsJsonObject().get("avatar");
                if (!avatarURLElement.isJsonNull()) {
                    String avatarURL = avatarURLElement.getAsString();
                    (new Utils.httpRequestImage(bitmap -> {
                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                        viewHolder.getAvatar().setImageBitmap(thumbImage);
                        review.setUserAvatar(bitmap);
                    })).execute(avatarURL);
                }

            })).execute(Utils.STATIONS_SERVER_URL + "/get-user-info?uid=" + review.getAuthorUID());

        } else {
            viewHolder.getName().setText(review.getUserName());
            Bitmap thumbImage = ThumbnailUtils.extractThumbnail(review.getUserAvatar(), Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
            viewHolder.getAvatar().setImageBitmap(thumbImage);
        }

        // Set message
        viewHolder.getMessage().setText(review.getMsg() != null && !review.getMsg().equals("") ? review.getMsg() : mActivity.getString(R.string.no_comment));

        // Set rate
        int rate = review.getRate();
        viewHolder.getRateValue().setText(String.valueOf(rate));
        viewHolder.getRateValue().setTextColor(getColorFromRate(rate));
        viewHolder.getRateIcon().setColorFilter(getColorFromRate(rate));

        // Set images
        if (review.getMediaLinks() != null && review.getMediaLinks().size() > 0) {

            // Download images
            if (review.getImages() == null || review.getImages().size() == 0) {
                (new Thread(() -> {
                    review.downloadImages(ignored -> {
                        mActivity.runOnUiThread(() -> {
                            ArrayList<Bitmap> reviewImages = review.getImages();

                            for (int i = 0; i < reviewImages.size(); i++) {
                                Bitmap bitmap = reviewImages.get(i);
                                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
                                ViewGroup imgWrapper = Utils.addImageToGallery(mActivity, thumbImage, viewHolder.getGallery(), Utils.GALLERY_IMAGE_SIZE_SMALL, false, Utils.NO_COLOR);

                                // Set click listeners
                                final int index = i;
                                imgWrapper.setOnClickListener(v -> {
                                    ((SharedState) mActivity.getApplicationContext()).slideshowImages = reviewImages;
                                    Intent intent = new Intent(mActivity, SlideshowActivity.class);
                                    intent.putExtra("index", index);
                                    mActivity.startActivity(intent);
                                    mActivity.overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                                });
                            }
                        });
                    });
                })).start();

            } else {
                ArrayList<Bitmap> reviewImages = review.getImages();

                for (int i = 0; i < reviewImages.size(); i++) {
                    Bitmap bitmap = reviewImages.get(i);
                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
                    ViewGroup imgWrapper = Utils.addImageToGallery(mActivity, thumbImage, viewHolder.getGallery(), Utils.GALLERY_IMAGE_SIZE_SMALL, false, Utils.NO_COLOR);

                    // Set click listeners
                    final int index = i;
                    imgWrapper.setOnClickListener(v -> {
                        ((SharedState) mActivity.getApplicationContext()).slideshowImages = reviewImages;
                        Intent intent = new Intent(mActivity, SlideshowActivity.class);
                        intent.putExtra("index", index);
                        mActivity.startActivity(intent);
                        mActivity.overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
                    });
                }
            }
            viewHolder.getGallery().setVisibility(View.VISIBLE);
        }

        // Set date
        Timestamp timestamp = new Timestamp(Long.parseLong(review.getCreationTimestamp()));
        LocalDate localDate = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        viewHolder.getDate().setText(localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
    }
    // END_INCLUDE(recyclerViewOnBindViewHolder)

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getColorFromRate(float rate) {
        if (rate < 2.5f) return mActivity.getColor(R.color.pink);
        if (rate < 4.0f) return mActivity.getColor(R.color.warning);
        return mActivity.getColor(R.color.success);
    }
}
