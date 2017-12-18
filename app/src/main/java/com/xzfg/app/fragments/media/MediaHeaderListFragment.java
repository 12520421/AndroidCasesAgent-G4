package com.xzfg.app.fragments.media;

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
import com.xzfg.app.managers.CollectedMediaManager;
import com.xzfg.app.model.MediaHeader;
import com.xzfg.app.model.MediaHeader.Media;
import com.xzfg.app.util.DateUtil;
import com.xzfg.app.widgets.DividerItemDecoration;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 */
public class MediaHeaderListFragment extends Fragment {


    @Inject
    Application application;

    @Inject
    CollectedMediaManager collectedMediaManager;

    private RecyclerView recyclerView;
    private MediaHeaderListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        adapter = new MediaHeaderListAdapter();
        adapter.setHasStableIds(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mediaheader_list, viewGroup, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.mediaheader_list);
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
        onEventMainThread(EventBus.getDefault().getStickyEvent(Events.CollectedMediaHeaderUpdated.class));
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.CollectedMediaHeaderUpdated event) {
        if (event != null) {
            EventBus.getDefault().removeStickyEvent(event);
            adapter.update();
        }
    }


    private class MediaHeaderListAdapter extends RecyclerView.Adapter<MediaHeaderListAdapter.ViewHolder> {


        public MediaHeaderListAdapter() {

        }

        public synchronized void update() {
            notifyDataSetChanged();
        }

        @Override
        public MediaHeaderListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.mediaheader_row, viewGroup, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MediaHeaderListAdapter.ViewHolder viewHolder, int position) {
            if (collectedMediaManager.getMediaHeaders() == null || collectedMediaManager.getMediaHeaders().isEmpty()) {
                Timber.e(new Exception("MediaHeaders are null."), "MediaHeaders are null");
                return;
            }

            if (position > collectedMediaManager.getMediaHeaders().size()) {
                Timber.e(new Exception("Position is beyond media size."), "Position: " + position + ", Size: " + collectedMediaManager.getMediaHeaders().size());
                return;
            }

            MediaHeader.Media media = collectedMediaManager.getMediaHeaders().get(position);
            if (media == null) {
                Timber.e(new Exception("Media is null"), "Media is null");
                return;
            }

            if (media.getCaseNumber() == null || media.getCaseNumber().trim().isEmpty()) {
                viewHolder.caseNumberView.setText(getString(R.string.case_number_, getString(R.string.null_case_number)));
            } else {
                viewHolder.caseNumberView.setText(getString(R.string.case_number_, media.getCaseNumber()));
            }

            if (media.getAudio() == null || media.getAudio() == 0) {
                viewHolder.audioCountView.setText(String.valueOf(0));
            } else {
                viewHolder.audioCountView.setText(String.valueOf(media.getAudio()));
            }

            if (media.getImage() == null || media.getImage() == 0) {
                viewHolder.photoCountView.setText(String.valueOf(0));
            } else {
                viewHolder.photoCountView.setText(String.valueOf(media.getImage()));
            }

            if (media.getVideo() == null || media.getVideo() == 0) {
                viewHolder.videoCountView.setText(String.valueOf(0));
            } else {
                viewHolder.videoCountView.setText(String.valueOf(media.getVideo()));
            }

            DateUtil.formatDate(viewHolder.timeView, media.getLastUpload());

        }

        @Override
        public int getItemCount() {
            if (collectedMediaManager.getMediaHeaders() == null) {
                return 0;
            }
            return collectedMediaManager.getMediaHeaders().size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            // each data item is just a string in this case
            public TextView caseNumberView;
            public TextView audioCountView;
            public TextView videoCountView;
            public TextView photoCountView;
            public TextView timeView;

            public ViewHolder(View view) {
                super(view);
                view.setClickable(true);
                view.setOnClickListener(this);
                caseNumberView = (TextView) view.findViewById(R.id.case_number);
                audioCountView = (TextView) view.findViewById(R.id.audio_count);
                videoCountView = (TextView) view.findViewById(R.id.video_count);
                photoCountView = (TextView) view.findViewById(R.id.photo_count);
                timeView = (TextView) view.findViewById(R.id.time);
            }


            @Override
            public void onClick(View v) {
                //Timber.d("Got Click");
                int position = recyclerView.getChildPosition(v);
                List<Media> mediaList = collectedMediaManager.getMediaHeaders();
                if (mediaList == null || mediaList.isEmpty() || position >= mediaList.size()) {
                    Timber.e("Click occurred on dead media header item.");
                    return;
                }

                MediaHeader.Media media = mediaList.get(position);
                collectedMediaManager.setSelectedCase(media.getCaseNumber());

                EventBus.getDefault().post(new Events.OpenCase(media.getCaseNumber()));
            }
        }

    }
}
