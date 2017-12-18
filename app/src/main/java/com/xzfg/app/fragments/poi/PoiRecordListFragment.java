package com.xzfg.app.fragments.poi;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
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
import com.xzfg.app.managers.PoiManager;
import com.xzfg.app.model.Poi;
import com.xzfg.app.model.PoiGroup;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.text.NumberFormat;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class PoiRecordListFragment extends Fragment implements View.OnClickListener {


    @Inject
    Application application;

    @Inject
    PoiManager poiManager;
    PoiGroup group;
    private RecyclerView recyclerView;
    private MediaRecordListAdapter adapter;

    public static PoiRecordListFragment newInstance(PoiGroup group) {
        PoiRecordListFragment f = new PoiRecordListFragment();
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
        adapter = new MediaRecordListAdapter();
        adapter.setHasStableIds(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poirecord_list, viewGroup, false);
        TextView caseNumberView = (TextView) view.findViewById(R.id.title);
        if (group.getName() != null && !group.getName().trim().isEmpty()) {
            caseNumberView.setText(getString(R.string.title_count, group.getName(), group.getRecords()));
        }
        recyclerView = (RecyclerView) view.findViewById(R.id.poirecord_list);
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
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.PoisDataUpdated.class));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.PoisDataUpdated event) {
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
                poiManager.clearSelectedId();
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
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.poirecordlist_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MediaRecordListAdapter.ViewHolder viewHolder, int position) {
            synchronized (PoiRecordListFragment.class) {
                List<Poi> pois = poiManager.getPois(group.getId());
                if (pois == null) {
                    Timber.e(new Exception("Records are null."), "Records are null");
                    return;
                }
                if (position > pois.size()) {
                    Timber.e(new Exception("Position is beyond record size."), "Position: " + position + ", Size: " + pois.size());
                    return;
                }

                Poi record = pois.get(position);
                if (record == null) {
                    Timber.e(new Exception("Record is null"), "Record is null");
                    return;
                }

                viewHolder.contactView.setTag(R.id.poi,record);

                if (record.getName() != null) {
                    viewHolder.contactName.setText(record.getName());
                } else {
                    viewHolder.contactName.setText("");
                }

                if ((record.getDistance() != null && !record.getDistance().trim().isEmpty())) {
                    Double distance = Double.parseDouble(record.getDistance());
                    if (distance != 0) {
                        viewHolder.location.setVisibility(View.VISIBLE);
                        viewHolder.distanceText.setText(NumberFormat.getInstance(getResources().getConfiguration().locale).format(distance));
                        if (record.getDirection() != null) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(Float.parseFloat(record.getDirection()), viewHolder.directionArrow.getDrawable().getIntrinsicWidth() / 2, viewHolder.directionArrow.getDrawable().getIntrinsicHeight() / 2);
                            viewHolder.directionArrow.setScaleType(ImageView.ScaleType.MATRIX);
                            viewHolder.directionArrow.setImageMatrix(matrix);
                        }
                    } else {
                        viewHolder.location.setVisibility(View.INVISIBLE);
                    }
                } else {
                    viewHolder.location.setVisibility(View.INVISIBLE);
                }


                if (record.getGroupName() != null) {
                    viewHolder.messageView.setText(record.getCategoryName().trim());
                } else {
                    viewHolder.contactName.setText("");
                }

                // Set address
                if (record.getAddress() != null && !record.getAddress().isEmpty()) {
                    SpannableString spanStr = new SpannableString(record.getAddress());
                    spanStr.setSpan(new UnderlineSpan(), 0, spanStr.length(), 0);
                    viewHolder.addressView.setText(spanStr);
                    viewHolder.addressView.setVisibility(View.VISIBLE);
                    viewHolder.addressView.setTag(R.id.poi,record);
                }
                else {
                    viewHolder.addressView.setVisibility(View.GONE);
                }

                Drawable d = getResources().getDrawable(R.drawable.poi_default);
                String categoryName = record.getCategoryName().toLowerCase().replaceAll(" ", "");
                if (categoryName != null) {
                    if (categoryName.startsWith("fire")) {
                        d = getResources().getDrawable(R.drawable.firestations);
                    }
                    if (categoryName.startsWith("gas")) {
                        d = getResources().getDrawable(R.drawable.gasstations);
                    }
                    if (categoryName.startsWith("police")) {
                        d = getResources().getDrawable(R.drawable.policestations);
                    }
                    if (categoryName.startsWith("hotel")) {
                        d = getResources().getDrawable(R.drawable.hotels);
                    }
                    if (categoryName.startsWith("hospital")) {
                        d = getResources().getDrawable(R.drawable.hospitals);
                    }
                    if (categoryName.startsWith("parking")) {
                        d = getResources().getDrawable(R.drawable.parkinglots);
                    }
                    if (categoryName.startsWith("pharmacy")) {
                        d = getResources().getDrawable(R.drawable.pharmacies);
                    }
                    if (categoryName.startsWith("restaurant")) {
                        d = getResources().getDrawable(R.drawable.restaurants);
                    }
                    if (categoryName.startsWith("venue")) {
                        d = getResources().getDrawable(R.drawable.venues);
                    }
                }

                viewHolder.poiIcon.setImageDrawable(d);
            }
        }

        @Override
        public int getItemCount() {
            if (poiManager.getPois(group.getId()) == null) {
                return 0;
            }
            return poiManager.getPois(group.getId()).size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public ImageView poiIcon;
            public TextView contactName;
            public TextView messageView;
            public TextView addressView;
            public TextView timeView;
            public TextView distanceText;
            public ImageView directionArrow;
            public LinearLayout location;
            public View contactView;


            public ViewHolder(View contactView) {
                super(contactView);
                this.contactView = contactView;
                contactView.setClickable(true);
                contactView.setOnClickListener(this);

                poiIcon = (ImageView) contactView.findViewById(R.id.poi_icon);
                contactName = (TextView) contactView.findViewById(R.id.contact_name);
                messageView = (TextView) contactView.findViewById(R.id.latest_message);
                addressView = (TextView) contactView.findViewById(R.id.address);
                addressView.setOnClickListener(this);
                timeView = (TextView) contactView.findViewById(R.id.time);
                location = (LinearLayout) contactView.findViewById(R.id.location);
                distanceText = (TextView) contactView.findViewById(R.id.distance);
                directionArrow = (ImageView) contactView.findViewById(R.id.arrow);

            }

            @Override
            public void onClick(View v) {
                Poi poi = (Poi)v.getTag(R.id.poi);
                if (v.getId() == R.id.address) {
                    // Open map
                    Uri address = Uri.parse("geo:" + poi.getLatitude() + "," + poi.getLongitude() + "?q=" + poi.getAddress());
                    Intent intent = new Intent(Intent.ACTION_VIEW, address);
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        intent.setData(Uri.parse("http://maps.google.com/maps?q=" + poi.getAddress()));
                        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    }
                    return;
                }
                if (poi != null) {
                    EventBus.getDefault().post(new Events.OpenPoiMap(poi.getId()));
                }
            }
        }

    }
}
