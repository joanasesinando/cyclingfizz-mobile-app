package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Demonstrates the use of {@link androidx.recyclerview.widget.RecyclerView}
 * with a {@link androidx.recyclerview.widget.LinearLayoutManager} and a
 * {@link androidx.recyclerview.widget.GridLayoutManager}.
 */
@SuppressLint("LongLogTag")
public class RecyclerViewFragment<T> extends Fragment {
    private static final String TAG = "Cycling_Fizz@RecyclerViewFragment";

    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 2; // FIXME: nr. columns in GridLayout

    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    public enum DatasetType {
        ROUTES, POIS, COMMENTS, REVIEWS, IMAGES
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected DatasetType mDatasetType;
    protected ArrayList<T> mDataset;

    protected String mRouteID;

    public RecyclerViewFragment(ArrayList<T> dataset, DatasetType type) {
        setDataset(dataset);
        mDatasetType = type;
    }

    public RecyclerViewFragment(ArrayList<T> dataset, DatasetType type, String routeID) {
        this(dataset, type);
        mRouteID = routeID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);

        // BEGIN_INCLUDE(initializeRecyclerView)
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        // The RecyclerView.LayoutManager defines how elements are laid out.
        switch (mDatasetType) {
            case ROUTES:
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                mAdapter = new RoutesCustomAdapter(getActivity(), (ArrayList<Route>) mDataset);
                break;

            case POIS:
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                mAdapter = new POIsCustomAdapter(getActivity(), (ArrayList<PointOfInterest>) mDataset, mRouteID);
                mRecyclerView.setNestedScrollingEnabled(false);
                break;

            case COMMENTS:
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                //mAdapter = new RoutesCustomAdapter((ArrayList<Route>) mDataset); TODO
                break;

            case REVIEWS:
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                //mAdapter = new RoutesCustomAdapter((ArrayList<Route>) mDataset); TODO
                break;

            case IMAGES:
                mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
                //mAdapter = new RoutesCustomAdapter((ArrayList<Route>) mDataset); TODO
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + mDatasetType);
        }

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

        // Set CustomAdapter as the adapter for RecyclerView.
        mRecyclerView.setAdapter(mAdapter);
        // END_INCLUDE(initializeRecyclerView)

        return rootView;
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        if (layoutManagerType == LayoutManagerType.GRID_LAYOUT_MANAGER) {
            mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
            mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;

        } else if (layoutManagerType == LayoutManagerType.LINEAR_LAYOUT_MANAGER) {
            mLayoutManager = new LinearLayoutManager(getActivity());
            mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Sets the dataset for RecyclerView's adapter.
     */
    public void setDataset(ArrayList<T> dataset) {
        mDataset = dataset;
    }
}
