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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
@SuppressLint("LongLogTag")
public class RoutesCustomAdapter extends RecyclerView.Adapter<RoutesCustomAdapter.ViewHolder> {
    private static final String TAG = "Cycling_Fizz@RoutesCustomAdapter";
    public final static String ROUTE_ID = "pt.ulisboa.tecnico.cmov.cyclingfizz.ROUTE_ID";

    private final Activity mActivity;
    private final ArrayList<Route> mDataSet;

    DecimalFormat oneDecimalFormatter = new DecimalFormat("#.0");

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView thumbnail;
        private final TextView title;
        private final TextView description;
        private final ImageView rateIcon;
        private final TextView rateValue;

        public ViewHolder(Activity activity, View v, ArrayList<Route> dataset) {
            super(v);

            thumbnail = v.findViewById(R.id.route_card_thumbnail);
            title = v.findViewById(R.id.route_card_title);
            description = v.findViewById(R.id.route_card_description);
            rateIcon = v.findViewById(R.id.route_card_rate_icon);
            rateValue = v.findViewById(R.id.route_card_rate_value);

            // Set click listener
            v.setOnClickListener(view -> {
                Route route = dataset.get(getAdapterPosition());

                Intent intent = new Intent(activity, RouteActivity.class);
                intent.putExtra(ROUTE_ID, route.getId());
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_leave);
            });
        }
        // END_INCLUDE(recyclerViewSampleViewHolder)

        public ImageView getThumbnail() {
            return thumbnail;
        }

        public TextView getTitle() {
            return title;
        }

        public TextView getDescription() {
            return description;
        }

        public ImageView getRateIcon() {
            return rateIcon;
        }

        public TextView getRateValue() {
            return rateValue;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet ArrayList<Route> containing the data to populate views to be used by RecyclerView.
     */
    public RoutesCustomAdapter(Activity activity, ArrayList<Route> dataSet) {
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
                .inflate(R.layout.route_card, viewGroup, false);

        return new ViewHolder(mActivity, v, mDataSet);
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    // BEGIN_INCLUDE(recyclerViewOnBindViewHolder)
    // Replace the contents of a view (invoked by the layout manager)
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        Route route = mDataSet.get(position);

        // Set thumbnail
        if (route.getMediaLink() != null) {

            // Download image
            if (route.getImage() == null) {
                (new Thread(() -> {
                    route.downloadImage(ignored -> {
                        mActivity.runOnUiThread(() -> {
                            Bitmap thumbImage = ThumbnailUtils.extractThumbnail(route.getImage(), Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                            viewHolder.getThumbnail().setImageBitmap(thumbImage);
                        });
                    });
                })).start();

            } else {
                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(route.getImage(), Utils.THUMBNAIL_SIZE_SMALL, Utils.THUMBNAIL_SIZE_SMALL);
                viewHolder.getThumbnail().setImageBitmap(thumbImage);
            }
        }

        // Set title & description
        viewHolder.getTitle().setText(route.getTitle());
        viewHolder.getDescription().setText(route.getDescription());

        // Set avg rate
        if (route.getRates().size() > 0) {
            float rateAvg = route.getAvgRateNotFlagged();
            viewHolder.getRateValue().setText(oneDecimalFormatter.format(rateAvg));
            viewHolder.getRateValue().setTextColor(getColorFromRate(rateAvg));
            viewHolder.getRateIcon().setColorFilter(getColorFromRate(rateAvg));
        }
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
