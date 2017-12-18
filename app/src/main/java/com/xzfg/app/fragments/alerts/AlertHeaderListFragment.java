package com.xzfg.app.fragments.alerts;

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
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.model.AlertHeader;
import com.xzfg.app.widgets.DividerItemDecoration;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class AlertHeaderListFragment extends Fragment {


    @Inject
    Application application;

    @Inject
    AlertManager alertManager;


    private RecyclerView recyclerView;
    private AlertHeaderListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        adapter = new AlertHeaderListAdapter();
        adapter.setHasStableIds(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alertheader_list, viewGroup, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.alertheader_list);
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
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.AlertsHeaderUpdated.class));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AlertsHeaderUpdated event) {
        if (event != null) {
            EventBus.getDefault().removeStickyEvent(event);
            adapter.update();
        }
    }


    private class AlertHeaderListAdapter extends RecyclerView.Adapter<AlertHeaderListAdapter.ViewHolder> {


        public AlertHeaderListAdapter() {

        }

        public synchronized void update() {
            notifyDataSetChanged();
        }

        @Override
        public AlertHeaderListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.alertheader_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(AlertHeaderListAdapter.ViewHolder viewHolder, int position) {
            if (alertManager.getHeaders() == null) {
                Timber.e(new Exception("Alert Headers are null."), "Alert Headers are null");
                return;
            }
            if (position > alertManager.getHeaders().size()) {
                Timber.e(new Exception("Position is beyond alert headers size."), "Position: " + position + ", Size: " + alertManager.getHeaders().size());
                return;
            }

            AlertHeader.Record record = alertManager.getHeaders().get(position);
            if (record == null) {
                Timber.e(new Exception("Record is null"), "Record is null");
                return;
            }

            viewHolder.titleView.setText(getString(R.string.title_count, record.getGroupName(), record.getCount()));


        }

        @Override
        public int getItemCount() {
            if (alertManager.getHeaders() == null) {
                return 0;
            }
            return alertManager.getHeaders().size();
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
            }


            @Override
            public void onClick(View v) {
                //Timber.d("Got Click");
                int position = recyclerView.getChildPosition(v);
                if (alertManager.getHeaders() == null || position >= alertManager.getHeaders().size()) {
                    Timber.w("Click occurred on dead alert header item.");
                    return;
                }

                try {
                    AlertHeader.Record record = alertManager.getHeaders().get(position);
                    alertManager.setSelectedId(record.getGroupId());

                    EventBus.getDefault().post(new Events.OpenAlertGroup(record));
                } catch (IndexOutOfBoundsException e) {
                    // Do nothing. This happens sometimes, difficult to debug.
                }
            }
        }

    }
}
