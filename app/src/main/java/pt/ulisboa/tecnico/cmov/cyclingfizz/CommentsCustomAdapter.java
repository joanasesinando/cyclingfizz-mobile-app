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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
public class CommentsCustomAdapter extends RecyclerView.Adapter<CommentsCustomAdapter.ViewHolder> {
    private static final String TAG = "Cycling_Fizz@CommentsCustomAdapter";

    private final Activity mActivity;
    private final ArrayList<PointOfInterest.Comment> mDataSet;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatar;
        private final TextView name;
        private final TextView message;
        private final GridLayout gallery;
        private final TextView date;

        public ViewHolder(Activity activity, View v, ArrayList<PointOfInterest.Comment> dataset) {
            super(v);

            avatar = v.findViewById(R.id.comment_item_avatar);
            name = v.findViewById(R.id.comment_item_name);
            message = v.findViewById(R.id.comment_item_msg);
            gallery = v.findViewById(R.id.comment_item_gallery);
            date = v.findViewById(R.id.comment_item_date);

            // Set click listener
            v.setOnClickListener(view -> {
                PointOfInterest.Comment comment = dataset.get(getAdapterPosition());

                // Set context menu for flagging as inappropriate
                view.setTag(comment);
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

        public TextView getDate() {
            return date;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet ArrayList<Comment> containing the data to populate views to be used by RecyclerView.
     */
    public CommentsCustomAdapter(Activity activity, ArrayList<PointOfInterest.Comment> dataSet) {
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
                .inflate(R.layout.comment_item, viewGroup, false);

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
        PointOfInterest.Comment comment = mDataSet.get(position);

        // Set avatar & name
        if (comment.getUserName() == null || comment.getUserAvatar() == null) {

            // Get name & avatar
            (new Utils.httpRequestJson(obj -> {
                if (!obj.get("status").getAsString().equals("success")) return;

                String userName = obj.get("data").getAsJsonObject().get("name").getAsString();
                viewHolder.getName().setText(userName);
                comment.setUserName(userName);

                JsonElement avatarURLElement = obj.get("data").getAsJsonObject().get("avatar");
                if (!avatarURLElement.isJsonNull()) {
                    String avatarURL = avatarURLElement.getAsString();
                    (new Utils.httpRequestImage(bitmap -> {
                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                        viewHolder.getAvatar().setImageBitmap(thumbImage);
                        comment.setUserAvatar(bitmap);
                    })).execute(avatarURL);
                }

            })).execute(Utils.STATIONS_SERVER_URL + "/get-user-info?uid=" + comment.getAuthorUID());

        } else {
            viewHolder.getName().setText(comment.getUserName());
            Bitmap thumbImage = ThumbnailUtils.extractThumbnail(comment.getUserAvatar(), Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
            viewHolder.getAvatar().setImageBitmap(thumbImage);
        }

        // Set message
        viewHolder.getMessage().setText(comment.getMsg() != null && !comment.getMsg().equals("") ? comment.getMsg() : mActivity.getString(R.string.no_comment));

        // Set images
        if (comment.getMediaLinks() != null && comment.getMediaLinks().size() > 0) {

            // Download images
            if (comment.getImages() == null || comment.getImages().size() == 0) {
                (new Thread(() -> {
                    comment.downloadImages(ignored -> {
                        mActivity.runOnUiThread(() -> {
                            ArrayList<Bitmap> commentImages = comment.getImages();

                            for (int i = 0; i < commentImages.size(); i++) {
                                Bitmap bitmap = commentImages.get(i);
                                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
                                ViewGroup imgWrapper = Utils.addImageToGallery(mActivity, thumbImage, viewHolder.getGallery(), Utils.GALLERY_IMAGE_SIZE_SMALL, false, Utils.NO_COLOR);

                                // Set click listeners
                                final int index = i;
                                imgWrapper.setOnClickListener(v -> {
                                    ((SharedState) mActivity.getApplicationContext()).slideshowImages = commentImages;
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
                ArrayList<Bitmap> commentImages = comment.getImages();

                for (int i = 0; i < commentImages.size(); i++) {
                    Bitmap bitmap = commentImages.get(i);
                    Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_MEDIUM, Utils.THUMBNAIL_SIZE_MEDIUM);
                    ViewGroup imgWrapper = Utils.addImageToGallery(mActivity, thumbImage, viewHolder.getGallery(), Utils.GALLERY_IMAGE_SIZE_SMALL, false, Utils.NO_COLOR);

                    // Set click listeners
                    final int index = i;
                    imgWrapper.setOnClickListener(v -> {
                        ((SharedState) mActivity.getApplicationContext()).slideshowImages = commentImages;
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
        Timestamp timestamp = new Timestamp(Long.parseLong(comment.getCreationTimestamp()));
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
