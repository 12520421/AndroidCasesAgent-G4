package com.xzfg.app.fragments.alerts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.model.AlertContent;
import com.xzfg.app.model.AlertHeader;
import com.xzfg.app.util.DateUtil;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.text.NumberFormat;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class AlertRecordListFragment extends Fragment implements View.OnClickListener {


    @Inject
    Application application;

    @Inject
    AlertManager alertManager;
    AlertHeader.Record group;
    RecyclerView recyclerView;
    AlertRecordListAdapter adapter;

    public static AlertRecordListFragment newInstance(AlertHeader.Record group) {
        AlertRecordListFragment f = new AlertRecordListFragment();
        Bundle args = new Bundle();
        args.putParcelable("group", group);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        group = getArguments().getParcelable("group");
        adapter = new AlertRecordListAdapter(alertManager, group);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alertrecord_list, viewGroup, false);
        TextView caseNumberView = (TextView) view.findViewById(R.id.title);
        if (group != null && group.getGroupName() != null) {
            caseNumberView.setText(getString(R.string.title_count, group.getGroupName(), group.getCount()));
        }
        recyclerView = (RecyclerView) view.findViewById(R.id.alertrecord_list);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.back_button).setOnClickListener(this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().registerSticky(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        adapter = null;
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AlertsDataUpdated event) {
        if (event != null) {
            EventBus.getDefault().removeStickyEvent(event);
            if (adapter != null) {
                adapter.update();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AlertRecordUpdated event) {
        // Update acceptance status
        if (event != null && event.getRecord() != null) {
            EventBus.getDefault().removeStickyEvent(event);
            AlertContent.Record changedRecord = event.getRecord();
            List<AlertContent.Record> alerts = alertManager.getRecords(group.getGroupId());
            for (AlertContent.Record record : alerts) {
                 if (record.getAlertId() == changedRecord.getAlertId()) {
                     record.setUserAccepted(changedRecord.getUserAccepted());
                     record.setOtherAccepted(changedRecord.getOtherAccepted());
                 }
            }
            if (adapter != null) {
                adapter.update();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.back_button: {
                alertManager.clearSelectedId();
                getActivity().onBackPressed();
                break;
            }
        }
    }


    private class AlertRecordListAdapter extends RecyclerView.Adapter<AlertRecordListAdapter.ViewHolder> {

        List<AlertContent.Record> cases;

        AlertRecordListAdapter(AlertManager alertManager, AlertHeader.Record group) {
            if (alertManager != null && group != null) {
                update();
            }
        }

        public synchronized void update() {
            if (alertManager != null && group != null) {
                cases = alertManager.getRecords(group.getGroupId());
                notifyDataSetChanged();
            }
        }

        @Override
        public AlertRecordListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
            int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.alertrecordlist_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(AlertRecordListAdapter.ViewHolder viewHolder, int position) {
            AlertContent.Record record = null;
            try {
                if (cases != null && !cases.isEmpty()) {
                    record = cases.get(position);
                }
            } catch (Exception e) {
                Timber.w(e, "Couldn't get alert record");
            }
            viewHolder.bind(record);
        }

        @Override
        public int getItemCount() {
            if (cases == null || cases.isEmpty()) {
                return 0;
            }
            return cases.size();
        }

        @Override
        public long getItemId(int position) {
            if (cases != null && !cases.isEmpty() && position > cases.size()) {
                return cases.get(position).getAlertId();
            }
            return -1;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            TextView contactName;
            TextView messageView;
            TextView timeView;
            TextView distanceText;
            ImageView directionArrow;
            LinearLayout location;
            TextView addressView;
            ImageView statusIndicator;
            Uri addressGeoUri;
            Uri addressHttpUri;
            AlertContent.Record record;

            public ViewHolder(View contactView) {
                super(contactView);
                contactView.setClickable(true);
                contactView.setOnClickListener(this);

                contactName = (TextView) contactView.findViewById(R.id.contact_name);
                messageView = (TextView) contactView.findViewById(R.id.latest_message);
                timeView = (TextView) contactView.findViewById(R.id.time);
                location = (LinearLayout) contactView.findViewById(R.id.location);
                distanceText = (TextView) contactView.findViewById(R.id.distance);
                directionArrow = (ImageView) contactView.findViewById(R.id.arrow);
                statusIndicator = (ImageView) contactView.findViewById(R.id.status_indicator);

                addressView = (TextView) contactView.findViewById(R.id.address);
                addressView.setTag(contactView);
                addressView.setClickable(true);
                addressView.setOnClickListener(this);
            }


            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.address:
                        if (addressGeoUri != null && addressHttpUri != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, addressGeoUri);
                            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                                startActivity(intent);
                            } else {
                                intent.setData(addressHttpUri);
                                if (intent.resolveActivity(getActivity().getPackageManager())
                                    != null) {
                                    startActivity(intent);
                                }
                            }
                            return;
                        }
                        break;
                }

                if (record != null) {
                    EventBus.getDefault().post(new Events.OpenAlertRecord(record));
                }
            }


            void bind(final AlertContent.Record record) {
                this.record = record;
                if (record != null) {
                    addressGeoUri = Uri.parse(
                        "geo:" + record.getLatitude() + "," + record.getLongitude() + "?q=" + record
                            .getAddress());
                    addressHttpUri = Uri
                        .parse("http://maps.google.com/maps?q=" + record.getAddress());

                    if (record.getTargetName() != null) {
                        contactName.setText(record.getTargetName());
                    } else {
                        contactName.setText("");
                    }

                    if ((record.getDistance() != null && !record.getDistance().trim().isEmpty())) {
                        Double distance = Double.parseDouble(record.getDistance());
                        if (distance != 0) {
                            location.setVisibility(View.VISIBLE);
                            //noinspection deprecation
                            distanceText.setText(
                                NumberFormat.getInstance(getResources().getConfiguration().locale)
                                    .format(distance));
                            if (record.getDirection() != null) {
                                Matrix matrix = new Matrix();
                                matrix.postRotate(
                                    Float.parseFloat(record.getDirection()),
                                    directionArrow.getDrawable().getIntrinsicWidth() / 2,
                                    directionArrow.getDrawable().getIntrinsicHeight() / 2
                                );
                                directionArrow.setScaleType(ImageView.ScaleType.MATRIX);
                                directionArrow.setImageMatrix(matrix);
                            }
                        } else {
                            location.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        location.setVisibility(View.INVISIBLE);
                    }

                    DateUtil.formatDate(timeView, record.getCreated());

                    if (record.getAction() != null) {
                        messageView.setText(record.getAction().trim());
                    } else {
                        messageView.setText("");
                    }

                    // Set address
                    if (record.getAddress() != null && !record.getAddress().isEmpty()) {
                        SpannableString spanStr = new SpannableString(record.getAddress());
                        spanStr.setSpan(new UnderlineSpan(), 0, spanStr.length(), 0);
                        addressView.setText(spanStr);
                        addressView.setVisibility(View.VISIBLE);
                    } else {
                        addressView.setVisibility(View.GONE);
                    }

                    // Set status icon
                    Context ctx = getActivity();
                    if (record.getUserAccepted() != 0 && record.getOtherAccepted() != 0) {
                        statusIndicator.setImageDrawable(
                            ContextCompat.getDrawable(
                                ctx,
                                R.drawable.alertacceptedbymeandsomeoneelse)
                        );
                    } else if (record.getUserAccepted() != 0) {
                        statusIndicator.setImageDrawable(
                            ContextCompat.getDrawable(ctx, R.drawable.alertacceptedbyme)
                        );
                    } else if (record.getOtherAccepted() != 0) {
                        statusIndicator.setImageDrawable(
                            ContextCompat.getDrawable(ctx, R.drawable.alertacceptedbysomeoneelse));
                    } else {
                        statusIndicator.setImageDrawable(
                            ContextCompat.getDrawable(ctx, R.drawable.alertunaccepted));
                    }

                }
            }
        }
    }
}
