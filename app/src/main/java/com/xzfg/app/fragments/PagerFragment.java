package com.xzfg.app.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.fragments.RemovablePagerAdapter.AgentTabs;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.managers.AlertManager;
import com.xzfg.app.managers.ChatManager;
import com.xzfg.app.managers.CollectedMediaManager;
import com.xzfg.app.managers.PoiManager;
import com.xzfg.app.model.AgentRoleComponent;
import com.xzfg.app.model.AgentRoles;
import com.xzfg.app.model.AgentSettings;

import java.util.ArrayList;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PagerFragment extends Fragment implements AgentRoleComponent {
    @Inject
    Application application;
    @Inject
    ChatManager chatManager;
    @Inject
    CollectedMediaManager collectedMediaManager;
    @Inject
    PoiManager poiManager;
    @Inject
    AlertManager alertManager;

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private final ArrayList<AgentTabs> tabs = new ArrayList<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.pager, viewGroup, false);
        mViewPager = (ViewPager) v.findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(3);

        // Create the adapter that will return a fragment for each of tabs
        AgentRoles roles = application.getAgentSettings().getAgentRoles();
        updateRoles(roles);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), tabs);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        Activity activity = getActivity();
        if (activity != null && activity instanceof ViewPager.OnPageChangeListener) {
            ViewPager.OnPageChangeListener listener = (ViewPager.OnPageChangeListener) getActivity();
            mViewPager.addOnPageChangeListener(listener);
        }

        int screen = getScreen();
        toggleLoading(screen);
//        if (AgentTabs.TAB_COLLECT.equals(AgentTabs.fromValue(screen))) {
//            EventBus.getDefault().postSticky(new Events.ActivityResumed());
//        }

        mViewPager.setCurrentItem(screen, false);


        return v;
    }

    public void toggleLoading(int screen) {
        if (screen >= tabs.size()) {
            return;
        }

        switch (tabs.get(screen)) {
            case TAB_CHAT:
                chatManager.setChatTabSelected(true);
                poiManager.setSelected(false);
                alertManager.setSelected(false);
                collectedMediaManager.setSelected(false);
                break;
//            case TAB_POI:
//                chatManager.setChatTabSelected(false);
//                poiManager.setSelected(true);
//                alertManager.setSelected(false);
//                collectedMediaManager.setSelected(false);
//                break;
//            case TAB_ALERTS:
//                chatManager.setChatTabSelected(false);
//                poiManager.setSelected(false);
//                alertManager.setSelected(true);
//                collectedMediaManager.setSelected(false);
//                break;
//            case TAB_COLLECT:
//                chatManager.setChatTabSelected(false);
//                alertManager.setSelected(false);
//                poiManager.setSelected(false);
//                collectedMediaManager.setSelected(true);
//                break;
            default:
                chatManager.setChatTabSelected(false);
                alertManager.setSelected(false);
                poiManager.setSelected(false);
                collectedMediaManager.setSelected(false);
                break;
        }
    }

    public int getScreen() {
        int initScreen = 0;

        if (application != null) {
            AgentSettings agentSettings = application.getAgentSettings();
            if (agentSettings != null) {
                AgentTabs screenTab = AgentTabs.fromValue(agentSettings.getScreen());
                initScreen = Math.max(0, tabs.indexOf(screenTab));
            }
        }

        return initScreen;
    }

    @Override
    public void onResume() {
        super.onResume();
/*
        if (application.isFromResult()) {
            if (mViewPager.getCurrentItem() != 1) {
                mViewPager.setCurrentItem(1, true);
                application.setFromResult(false);
                toggleLoading(1);
            }
        } else {
            AgentSettings agentSettings = application.getAgentSettings();
            if (agentSettings != null) {
                int screen = getScreen();
                toggleLoading(screen);
                mViewPager.setCurrentItem(screen, true);
            }
        }
*/
        int screen = getScreen();
        if (application != null) {
            AgentSettings agentSettings = application.getAgentSettings();
            if (agentSettings != null && agentSettings.getCurrentScreen() >= 0) {
                screen = agentSettings.getCurrentScreen();
            }
            application.setFromResult(false);
        }
        toggleLoading(screen);
        mViewPager.setCurrentItem(screen, true);

        if (getActivity() != null) {
            getActivity().supportInvalidateOptionsMenu();
        }

        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        if (application != null) {
            AgentSettings agentSettings = application.getAgentSettings();
            if (agentSettings != null) {
                agentSettings.setCurrentScreen(mViewPager.getCurrentItem());
            }
        }
        super.onPause();
    }

    public void onEventMainThread(Events.RecordingStoppedEvent recordingStoppedEvent) {
        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.recording_cancelled), getString(R.string.ran_out_of_space));
        alertDialogFragment.show(getActivity().getSupportFragmentManager(), "error-dialog");
        EventBus.getDefault().removeStickyEvent(recordingStoppedEvent);
    }

    public int getSelected() {

        if (mViewPager != null && !tabs.isEmpty()) {
            AgentTabs tab = tabs.get(mViewPager.getCurrentItem());
            switch (tab) {
//                case TAB_HOME:
//                    return R.id.home;
//                case TAB_CHECKIN:
//                    return R.id.checkin;
//                case TAB_TIMER:
//                    return R.id.timer;
//                case TAB_CONTACTS:
//                    return R.id.contacts;
                case TAB_CHAT:
                    return R.id.chat;
//                case TAB_COLLECT:
//                    return R.id.collect;
//                case TAB_MAP:
//                    return R.id.map;
//                case TAB_POI:
//                    return R.id.poi;
//                case TAB_ALERTS:
//                    return R.id.alerts;
//                case TAB_MEDIA:
//                    return R.id.collected_media;

                case TAB_MARKALERT:
                    return R.id.markalert;
                case TAB_STATUS:
                    return R.id.status;

            }
            return mViewPager.getCurrentItem();
        }
        return -1;
    }

    public void setSelected(int id) {
        Integer index = null;

        if (mViewPager != null) {
            switch (id) {
//                case R.id.home:
//                    index = tabs.indexOf(AgentTabs.TAB_HOME);
//                    break;
//                case R.id.checkin:
//                    index = tabs.indexOf(AgentTabs.TAB_CHECKIN);
//                    break;
//                case R.id.timer:
//                    index = tabs.indexOf(AgentTabs.TAB_TIMER);
//                    break;
//                case R.id.contacts:
//                    index = tabs.indexOf(AgentTabs.TAB_CONTACTS);
//                    break;
                case R.id.chat:
                    index = tabs.indexOf(AgentTabs.TAB_CHAT);
                    break;
//                case R.id.collect:
//                    index = tabs.indexOf(AgentTabs.TAB_COLLECT);
//                    break;
//                case R.id.map:
//                    index = tabs.indexOf(AgentTabs.TAB_MAP);
//                    break;
//                case R.id.poi:
//                    index = tabs.indexOf(AgentTabs.TAB_POI);
//                    break;
//                case R.id.alerts:
//                    index = tabs.indexOf(AgentTabs.TAB_ALERTS);
//                    break;
//                case R.id.collected_media:
//                    index = tabs.indexOf(AgentTabs.TAB_MEDIA);
//                    break;

                case R.id.markalert:
                    index = tabs.indexOf(AgentTabs.TAB_MARKALERT);
                    break;
                case R.id.status:
                    index = tabs.indexOf(AgentTabs.TAB_STATUS);
                    break;
            }
            if (index != null && index >= 0) {
                if (mViewPager.getCurrentItem() != index) {
                    mViewPager.setCurrentItem(index, true);
                }
            }
        }

    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.MenuItemSelected event) {
        //Timber.d("Handling selection event for " + event.getName());
        Integer index = null;

        if (mViewPager != null) {
            switch (event.getId()) {
//                case R.id.home:
//                    index = tabs.indexOf(AgentTabs.TAB_HOME);
//                    break;
//                case R.id.checkin:
//                    index = tabs.indexOf(AgentTabs.TAB_CHECKIN);
//                    break;
//                case R.id.timer:
//                    index = tabs.indexOf(AgentTabs.TAB_TIMER);
//                    break;
//                case R.id.contacts:
//                    index = tabs.indexOf(AgentTabs.TAB_CONTACTS);
//                    break;
                case R.id.sos:
                    // let the activity handle it.
                    break;
                case R.id.chat:
                    index = tabs.indexOf(AgentTabs.TAB_CHAT);
                    break;
//                case R.id.collect:
//                    index = tabs.indexOf(AgentTabs.TAB_COLLECT);
//                    break;
//                case R.id.map:
//                    index = tabs.indexOf(AgentTabs.TAB_MAP);
//                    break;
//                case R.id.poi:
//                    index = tabs.indexOf(AgentTabs.TAB_POI);
//                    break;
//                case R.id.alerts:
//                    index = tabs.indexOf(AgentTabs.TAB_ALERTS);
//                    break;
//                case R.id.collected_media:
//                    index = tabs.indexOf(AgentTabs.TAB_MEDIA);
//                    break;

                case R.id.markalert:
                    index= tabs.indexOf(AgentTabs.TAB_MARKALERT);
                    break;
                case R.id.status:
                    index=tabs.indexOf(AgentTabs.TAB_STATUS);
                    break;
                case R.id.settings:
                    // let the activity handle it.
                    break;
                case R.id.wipe:
                    // let the activity handle it.
                    break;
                case R.id.boss:
                    // let the activity handle it.
                    break;
                case R.id.preferences:
                    // let the activity handle it.
                    break;
                case R.id.help:
                    // let the activity handle it.
                    break;
                case R.id.configuration:
                    break;
                case R.id.atcommands:
                    break;
                case R.id.unregister:
                    break;
                default:
                    String name = event.getName();
                    if (name == null) {
                        name = getString(R.string.error_title);
                    }
                    AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(name, getString(R.string.not_available));
                    alertDialogFragment.show(getFragmentManager(), "error-dialog");
            }
            if (index != null && index >= 0) {
                mViewPager.setCurrentItem(index, true);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.AgentRolesUpdated event) {
        updateRoles(event.getAgentRoles());
    }

    @Override
    public void updateRoles(AgentRoles roles) {
        tabs.clear();
        // Add tabs according to the order of values in the AgentTabs enum
        for (AgentTabs tab : AgentTabs.values()) {
            switch (tab) {
//                case TAB_HOME: {
//                    if (roles.home()) {
//                      //  tabs.add(AgentTabs.TAB_HOME);
//                    }
//                    break;
//                }
//                case TAB_CHECKIN: {
//                    if (roles.home()) {
//                     //   tabs.add(AgentTabs.TAB_CHECKIN);
//                    }
//                    break;
//                }
//                case TAB_TIMER: {
//                    if (roles.panictimed()) {
//                      //  tabs.add(AgentTabs.TAB_TIMER);
//                    }
//                    break;
//                }
//                case TAB_CONTACTS: {
//                    if (roles.home()) {
//                    //    tabs.add(AgentTabs.TAB_CONTACTS);
//                    }
//                    break;
//                }
                case TAB_CHAT: {
                    if (roles.g4_messenger()) {
                        tabs.add(AgentTabs.TAB_CHAT);
                    }
                    break;
                }
//                case TAB_COLLECT: {
//                    if (roles.collect()) {
//                     //   tabs.add(AgentTabs.TAB_COLLECT);
//                    }
//                    break;
//                }
//                case TAB_MAP: {
//                    if (roles.map()) {
//                      //  tabs.add(AgentTabs.TAB_MAP);
//                    }
//                    break;
//                }
//                case TAB_POI: {
//                    if (roles.pointsofinterest()) {
//                     //   tabs.add(AgentTabs.TAB_POI);
//                    }
//                    break;
//                }
//                case TAB_ALERTS: {
//                    if (roles.alert()) {
//                      //  tabs.add(AgentTabs.TAB_ALERTS);
//                    }
//                    break;
//                }
//                case TAB_MEDIA: {
//                    if (roles.collectedmedia()) {
//                      //  tabs.add(AgentTabs.TAB_MEDIA);
//                    }
//                    break;
//                }

                case TAB_MARKALERT:
                    if(roles.g4_mark_alert()) {
                        tabs.add(AgentTabs.TAB_MARKALERT);
                    }
                    break;
                case TAB_STATUS:
                    if(roles.g4_status()) {
                        tabs.add(AgentTabs.TAB_STATUS);
                    }
                    break;
                default: {
                    // This will never happen?
                }
            }
        }

        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.setTabs(tabs);
        }

        // After updating the tabs, we need to fire off the page selected listener so that if the
        // current tab was changed, all the managers and UI elements will update appropriately as
        // well.
        Activity activity = getActivity();
        if (activity != null && activity instanceof ViewPager.OnPageChangeListener) {
            ViewPager.OnPageChangeListener listener = (ViewPager.OnPageChangeListener) getActivity();
            listener.onPageSelected(mViewPager.getCurrentItem());
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_agent, viewGroup, false);
        }
    }

    public class SectionsPagerAdapter extends RemovablePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm, ArrayList<AgentTabs> tabs) {
            super(fm, tabs);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (pagerTabs.isEmpty()) {
                return "";
            }

            AgentTabs tab = pagerTabs.get(position);

            return getPageTitle(getActivity(), tab);
        }
    }


}
