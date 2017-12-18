package com.xzfg.app.fragments.poi;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.PoiManager;
import com.xzfg.app.model.PoiGroup;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class PoiHeaderListFragment extends Fragment {


    @Inject
    Application application;

    @Inject
    PoiManager poiManager;

    private RecyclerView recyclerView;
    private PoiHeaderListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        adapter = new PoiHeaderListAdapter();
        adapter.setHasStableIds(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poiheader_list, viewGroup, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.poiheader_list);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.PoisHeaderUpdated.class));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.PoisHeaderUpdated event) {
        if (event != null) {
            EventBus.getDefault().removeStickyEvent(event);
            adapter.update();
        }
    }


    private class PoiHeaderListAdapter extends RecyclerView.Adapter<PoiHeaderListAdapter.ViewHolder> {


        public PoiHeaderListAdapter() {

        }

        public synchronized void update() {
            notifyDataSetChanged();
        }

        @Override
        public PoiHeaderListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.poiheader_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(PoiHeaderListAdapter.ViewHolder viewHolder, int position) {
            if (poiManager.getGroups() == null) {
                Timber.e(new Exception("Categories are null."), "Categories are null");
                return;
            }
            if (position > poiManager.getGroups().size()) {
                Timber.e(new Exception("Position is beyond poi category size."), "Position: " + position + ", Size: " + poiManager.getGroups().size());
                return;
            }

            PoiGroup record = poiManager.getGroups().get(position);
            if (record == null) {
                Timber.e(new Exception("Record is null"), "Record is null");
                return;
            }

            viewHolder.titleView.setText(getString(R.string.title_count, record.getName(), record.getRecords()));


        }

        @Override
        public int getItemCount() {
            if (poiManager.getGroups() == null) {
                return 0;
            }
            return poiManager.getGroups().size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public TextView titleView;
            public TextView countView;

            public ViewHolder(View view) {
                super(view);
                view.setClickable(true);
                view.setOnClickListener(this);
                titleView = (TextView) view.findViewById(R.id.title);
                countView = (TextView) view.findViewById(R.id.alert_count);
            }


            @Override
            public void onClick(View v) {
                //Timber.d("Got Click");
                int position = recyclerView.getChildPosition(v);
                List<PoiGroup> groups = poiManager.getGroups();
                if (groups == null || position > groups.size()) {
                    Timber.w("Click occurred on dead media header item.");
                    return;
                }

                PoiGroup group = groups.get(position);
                if (group != null) {
                    poiManager.setSelectedId(group.getId());
                    //Timber.d("Got poi group, posting OpenPoiGroup event.");
                    EventBus.getDefault().post(new Events.OpenPoiGroup(group));
                }
            }
        }

    }
}
