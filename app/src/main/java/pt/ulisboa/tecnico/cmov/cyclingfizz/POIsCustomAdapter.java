package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
@SuppressLint("LongLogTag")
public class POIsCustomAdapter extends RecyclerView.Adapter<POIsCustomAdapter.ViewHolder> {
    private static final String TAG = "Cycling_Fizz@POIsCustomAdapter";
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

    private final Activity mActivity;
    private final ArrayList<PointOfInterest> mDataSet;
    private final String mRouteID;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView order;
        private final ImageView thumbnail;
        private final TextView title;
        private final TextView description;

        public ViewHolder(Activity activity, View v, ArrayList<PointOfInterest> dataset, String routeID) {
            super(v);

            order = v.findViewById(R.id.poi_item_order);
            thumbnail = v.findViewById(R.id.poi_item_thumbnail);
            title = v.findViewById(R.id.poi_item_title);
            description = v.findViewById(R.id.route_card_description);

            // Set click listener
            v.setOnClickListener(view -> {
                PointOfInterest poi = dataset.get(getAdapterPosition());

                SharedState sharedState = (SharedState) activity.getApplicationContext();
                sharedState.viewingPOI = poi;

                Intent intent = new Intent(activity, ViewPOIActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(ROUTE_ID, routeID);
                intent.putExtras(bundle);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
            });
        }
        // END_INCLUDE(recyclerViewSampleViewHolder)

        public TextView getOrder() {
            return order;
        }

        public ImageView getThumbnail() {
            return thumbnail;
        }

        public TextView getTitle() {
            return title;
        }

        public TextView getDescription() {
            return description;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet ArrayList<PointOfInterest> containing the data to populate views to be used by RecyclerView.
     */
    public POIsCustomAdapter(Activity activity, ArrayList<PointOfInterest> dataSet, String routeID) {
        mActivity = activity;
        mDataSet = dataSet;
        mRouteID = routeID;
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.poi_item, viewGroup, false);

        return new ViewHolder(mActivity, v, mDataSet, mRouteID);
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    // BEGIN_INCLUDE(recyclerViewOnBindViewHolder)
    // Replace the contents of a view (invoked by the layout manager)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        PointOfInterest poi = mDataSet.get(position);

        // Set order
        viewHolder.getOrder().setText(String.valueOf(position + 1));

        // Set thumbnail
        if (poi.getMediaLinks().size() > 0) {

            // Download image
            if (poi.getImages() == null || poi.getImages().size() == 0) {
                (new Thread(() -> {
                    poi.downloadAndGetImage(0, bitmap -> {
                        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                        viewHolder.getThumbnail().setImageBitmap(thumbImage);
                    });
                })).start();

            } else {
                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(poi.getImages().get(0), Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                viewHolder.getThumbnail().setImageBitmap(thumbImage);
            }
        }

        //Set title & description
        viewHolder.getTitle().setText(poi.getName());
        viewHolder.getDescription().setText(poi.getDescription());
    }
    // END_INCLUDE(recyclerViewOnBindViewHolder)

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}
