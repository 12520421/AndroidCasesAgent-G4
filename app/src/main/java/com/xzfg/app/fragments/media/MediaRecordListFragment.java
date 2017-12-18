package com.xzfg.app.fragments.media;

import android.content.Intent;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.xzfg.app.managers.CollectedMediaManager;
import com.xzfg.app.model.Media;
import com.xzfg.app.util.DateUtil;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.text.NumberFormat;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class MediaRecordListFragment extends Fragment implements View.OnClickListener {


    @Inject
    Application application;

    @Inject
    CollectedMediaManager collectedMediaManager;
    String caseNumber;
    private RecyclerView recyclerView;
    private MediaRecordListAdapter adapter;

    public static MediaRecordListFragment newInstance(String caseNumber) {
        MediaRecordListFragment f = new MediaRecordListFragment();
        Bundle args = new Bundle();
        args.putString("caseNumber", caseNumber);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        caseNumber = getArguments().getString("caseNumber", null);
        adapter = new MediaRecordListAdapter();
        adapter.setHasStableIds(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mediarecord_list, viewGroup, false);
        TextView caseNumberView = (TextView) view.findViewById(R.id.title);
        if (caseNumber == null || caseNumber.trim().isEmpty()) {
            caseNumberView.setText(getString(R.string.case_number_, getString(R.string.null_case_number)));
        } else {
            caseNumberView.setText(getString(R.string.case_number_, caseNumber));
        }
        recyclerView = (RecyclerView) view.findViewById(R.id.mediarecord_list);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.back_button).setOnClickListener(this);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.CollectedMediaDataUpdated.class));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.CollectedMediaDataUpdated event) {
        if (event != null) {
            EventBus.getDefault().removeStickyEvent(event);
            adapter.update();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.back_button: {
                collectedMediaManager.clearSelectedCase();
                getActivity().onBackPressed();
                break;
            }
        }
    }


    @SuppressWarnings("deprecation")
    private class MediaRecordListAdapter extends RecyclerView.Adapter<MediaRecordListAdapter.ViewHolder> {


        public MediaRecordListAdapter() {

        }

        public synchronized void update() {
            notifyDataSetChanged();
        }

        @Override
        public MediaRecordListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.mediarecordlist_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MediaRecordListAdapter.ViewHolder viewHolder, int position) {
            List<Media.Record> cases = collectedMediaManager.getCaseRecords(caseNumber);
            if (cases == null) {
                Timber.e(new Exception("Records are null."), "Records are null");
                return;
            }
            if (position > cases.size()) {
                Timber.e(new Exception("Position is beyond record size."), "Position: " + position + ", Size: " + cases.size());
                return;
            }

            Media.Record record = cases.get(position);
            if (record == null) {
                Timber.e(new Exception("Record is null"), "Record is null");
                return;
            }


            if (record.getName() != null) {
                viewHolder.messageView.setText(record.getName());
            } else {
                viewHolder.messageView.setText("");
            }

            // Set address
            if (record.getAddress() != null && !record.getAddress().isEmpty()) {
                SpannableString spanStr = new SpannableString(record.getAddress());
                spanStr.setSpan(new UnderlineSpan(), 0, spanStr.length(), 0);
                viewHolder.addressView.setText(spanStr);
                viewHolder.addressView.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.addressView.setVisibility(View.GONE);
            }

            if ((record.getDistance() != null && !record.getDistance().trim().isEmpty())) {
                Double distance = Double.parseDouble(record.getDistance());
                if (distance != 0) {
                    viewHolder.location.setVisibility(View.VISIBLE);
                    viewHolder.distanceText.setText(NumberFormat.getInstance(getResources().getConfiguration().locale).format(distance));
                    if (record.getDirection() != null) {
                        Matrix matrix = new Matrix();
                        //Timber.d("Direction: " + record.getDirection());
                        matrix.postRotate(record.getDirection(), viewHolder.directionArrow.getDrawable().getIntrinsicWidth() / 2, viewHolder.directionArrow.getDrawable().getIntrinsicHeight() / 2);
                        viewHolder.directionArrow.setScaleType(ImageView.ScaleType.MATRIX);
                        viewHolder.directionArrow.setImageMatrix(matrix);
                    }
                } else {
                    viewHolder.location.setVisibility(View.INVISIBLE);
                }
            } else {
                viewHolder.location.setVisibility(View.INVISIBLE);
            }


            DateUtil.formatDate(viewHolder.timeView, record.getCreated());

            if (record.getType() != null) {
                if (record.getType().equalsIgnoreCase("audio")) {
                    viewHolder.statusIndicator.setImageDrawable(getResources().getDrawable(R.drawable.cm_audio));
                }
                if (record.getType().equalsIgnoreCase("video")) {
                    viewHolder.statusIndicator.setImageDrawable(getResources().getDrawable(R.drawable.cm_video));
                }
                if (record.getType().equalsIgnoreCase("img")) {
                    viewHolder.statusIndicator.setImageDrawable(getResources().getDrawable(R.drawable.cm_photo));
                }
                viewHolder.statusIndicator.setVisibility(View.VISIBLE);
            } else {
                viewHolder.statusIndicator.setVisibility(View.INVISIBLE);
            }

            if (record.getDescription() != null) {
                viewHolder.contactName.setText(record.getDescription().trim());
            } else {
                viewHolder.contactName.setText("");
            }
        }

        @Override
        public int getItemCount() {
            if (collectedMediaManager.getCaseRecords(caseNumber) == null) {
                return 0;
            }
            return collectedMediaManager.getCaseRecords(caseNumber).size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public ImageView statusIndicator;
            public TextView contactName;
            public TextView messageView;
            public TextView addressView;
            public TextView timeView;
            public TextView distanceText;
            public ImageView directionArrow;
            public LinearLayout location;

            public ViewHolder(View contactView) {
                super(contactView);
                contactView.setClickable(true);
                contactView.setOnClickListener(this);

                statusIndicator = (ImageView) contactView.findViewById(R.id.online_status_indicator);
                contactName = (TextView) contactView.findViewById(R.id.contact_name);
                messageView = (TextView) contactView.findViewById(R.id.latest_message);
                addressView = (TextView) contactView.findViewById(R.id.address);
                addressView.setTag(contactView);
                addressView.setOnClickListener(this);
                timeView = (TextView) contactView.findViewById(R.id.time);
                location = (LinearLayout) contactView.findViewById(R.id.location);
                location.setTag(contactView);
                location.setOnClickListener(this);
                distanceText = (TextView) contactView.findViewById(R.id.distance);
                directionArrow = (ImageView) contactView.findViewById(R.id.arrow);
            }


            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.location) {
                    //Timber.d("LOCATION CLICKED");
                    View x = (View) v.getTag();
                    Media.Record record = collectedMediaManager.getCaseRecords(caseNumber).get(recyclerView.getChildPosition(x));
                    EventBus.getDefault().post(new Events.OpenMediaMap(record.getLatitude(), record.getLongitude(), record.getId()));
                    return;
                }

                if (v.getId() == R.id.address) {
                    // Open map
                    View x = (View) v.getTag();
                    Media.Record record = collectedMediaManager.getCaseRecords(caseNumber).get(recyclerView.getChildPosition(x));
                    Uri address = Uri.parse("geo:" + record.getLatitude() + "," + record.getLongitude() + "?q=" + record.getAddress());
                    Intent intent = new Intent(Intent.ACTION_VIEW, address);
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        intent.setData(Uri.parse("http://maps.google.com/maps?q=" + record.getAddress()));
                        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    }
                    return;
                }

                Media.Record record = collectedMediaManager.getCaseRecords(caseNumber).get(recyclerView.getChildPosition(v));
                EventBus.getDefault().post(new Events.OpenRecord(record));
            }
        }

    }
}
