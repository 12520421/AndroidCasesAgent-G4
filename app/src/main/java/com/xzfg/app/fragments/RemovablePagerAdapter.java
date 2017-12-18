/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xzfg.app.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.xzfg.app.R;
import com.xzfg.app.fragments.alerts.AlertRootFragment;
import com.xzfg.app.fragments.chat.ChatRootFragment;
import com.xzfg.app.fragments.collect.CollectFragment;
import com.xzfg.app.fragments.home.HomeFragment;
import com.xzfg.app.fragments.media.MediaRootFragment;
import com.xzfg.app.fragments.poi.PoiRootFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

public abstract class RemovablePagerAdapter extends PagerAdapter {
    private static final String TAG = "FragmentStatePagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager mFragmentManager;

    private ArrayList<Fragment.SavedState> mSavedState = new ArrayList<>();
    private ArrayList<Fragment> mFragments = new ArrayList<>();
    CopyOnWriteArrayList<AgentTabs> oldTabs = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<AgentTabs> pagerTabs = new CopyOnWriteArrayList<>();
    private Fragment mCurrentPrimaryItem = null;


    public enum AgentTabs {
        // Order of values will define tabs orders in the PagerFragment
        TAB_STATUS(1),TAB_MARKALERT(2),TAB_CHAT(3);

        private int mValue;

        AgentTabs(int value) {
            this.mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static int indexOfValue(int value)
        {
            AgentTabs[] values = AgentTabs.values();
            for(int i = 0; i < values.length; i++)
            {
                if(values[i].getValue() == value)
                    return i;
            }
            return 0;
        }

        public static AgentTabs fromValue(int value)
        {
            AgentTabs[] values = AgentTabs.values();
            for(int i = 0; i < values.length; i++)
            {
                if(values[i].getValue() == value)
                    return values[i];
            }
            return TAB_STATUS;
        }
    }


    public RemovablePagerAdapter(FragmentManager fm, ArrayList<AgentTabs> tabs) {
        mFragmentManager = fm;
        pagerTabs.addAll(tabs);
    }

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @SuppressLint("LongLogTag")
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if (mFragments.size() > position) {
            Fragment f = mFragments.get(position);
            if (f != null) {
                return f;
            }
        }


        Fragment fragment = getItem(position);
        //if (DEBUG) Log.v(TAG, "Adding item #" + position + ": f=" + fragment);
        if (mSavedState.size() > position) {
            Fragment.SavedState fss = mSavedState.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        mFragments.set(position, fragment);
        mFragmentManager.beginTransaction().add(container.getId(),fragment).commitNow();
        return fragment;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;

        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
        mFragments.set(position, null);
        mFragmentManager.beginTransaction().remove(fragment).commitNow();
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }


    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedState.size() > 0) {
            state = new Bundle();
            Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
            mSavedState.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i = 0; i < mFragments.size(); i++) {
            Fragment f = mFragments.get(i);
            if (f != null && f.isAdded()) {
                if (state == null) {
                    state = new Bundle();
                }
                mFragmentManager.putFragment(state, "f_" + f.getClass().getSimpleName(), f);
            }
        }
        return state;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle) state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            mSavedState.clear();
            mFragments.clear();
            if (fss != null) {
                for (int i = 0; i < fss.length; i++) {
                    mSavedState.add((Fragment.SavedState) fss[i]);
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key : keys) {
                if (key.startsWith("f_")) {
                    Fragment f = null;
                    try {
                        f = mFragmentManager.getFragment(bundle, key);
                    } catch (Exception e) {
                        Timber.w(e, "Couldn't get fragment from manager.");
                    }
                    if (f != null) {
                        while (mFragments.size() <= pagerTabs.size()) {
                            mFragments.add(null);
                        }
                        f.setMenuVisibility(false);
                        if (f instanceof HomeFragment) {
                          //  mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_HOME), f);
                        }

                        if (f instanceof CheckinFragment) {
                        //    mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_CHECKIN), f);
                        }

                        if (f instanceof PanicTimerFragment) {
                         //   mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_TIMER), f);
                        }

                        if (f instanceof ContactsFragment) {
                         //   mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_CONTACTS), f);
                        }

                        if (f instanceof CollectFragment) {
                         //   mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_COLLECT), f);
                        }

                        if (f instanceof WebMapFragment) {
                         //   mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_MAP), f);
                        }

                        if (f instanceof PoiRootFragment) {
                         //   mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_POI), f);
                        }

                        if (f instanceof AlertRootFragment) {
                          //  mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_ALERTS), f);
                        }

                        if (f instanceof MediaRootFragment) {
                         //   mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_MEDIA), f);
                        }

                        if(f instanceof MarkAlertFragment){
                            mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_MARKALERT),f);
                        }
                        if(f instanceof StatusFragment){
                            mFragments.set(pagerTabs.indexOf(AgentTabs.TAB_STATUS),f);
                        }

                    } else {
                        Log.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }


    @Override
    public int getCount() {
        int size = pagerTabs.size();
        //Timber.v("Getting Count: " + size);
        return size;
    }

    @Override
    public int getItemPosition(Object item) {
        //Timber.w("getItemPosition called on " + item.getClass().getSimpleName());

        if (item instanceof HomeFragment) {
          //  return positionOf(AgentTabs.TAB_HOME);
        }

        if (item instanceof CheckinFragment) {
          //  return positionOf(AgentTabs.TAB_CHECKIN);
        }

        if (item instanceof PanicTimerFragment) {
          //  return positionOf(AgentTabs.TAB_TIMER);
        }

        if (item instanceof ContactsFragment) {
          //  return positionOf(AgentTabs.TAB_CONTACTS);
        }

        if (item instanceof G4ChatFragment) {
            return positionOf(AgentTabs.TAB_CHAT);
        }

        if (item instanceof CollectFragment) {
         //   return positionOf(AgentTabs.TAB_COLLECT);
        }

        if (item instanceof WebMapFragment) {
          //  return positionOf(AgentTabs.TAB_MAP);
        }

        if (item instanceof PoiRootFragment) {
          //  return positionOf(AgentTabs.TAB_POI);
        }

        if (item instanceof AlertRootFragment) {
         //   return positionOf(AgentTabs.TAB_ALERTS);
        }

        if (item instanceof MediaRootFragment) {
          //  return positionOf(AgentTabs.TAB_MEDIA);
        }

        if(item instanceof  MarkAlertFragment){
            return  positionOf(AgentTabs.TAB_MARKALERT);
        }
        if(item instanceof StatusFragment){
            return positionOf(AgentTabs.TAB_STATUS);
        }

        //Timber.w("UNKNOWN FRAGMENT: " + item.getClass().getSimpleName());

        return POSITION_NONE;

    }

    public int positionOf(AgentTabs key) {
        int oldIndex = oldTabs.indexOf(key);
        int index = pagerTabs.indexOf(key);
        //Timber.w("old_index: " + oldIndex);
        //Timber.w("index: " + index);
        if (oldIndex == index && index >= 0) {
            //Timber.w("position unchanged for " + key);
            return POSITION_UNCHANGED;
        }
        //Timber.w("POSITION CHANGED for " + key);
        return POSITION_NONE;
    }

    public Fragment getItem(int position) {
        //Timber.d("Get item " + position);
        AgentTabs tab = pagerTabs.get(position);
        Fragment f;
        switch (tab) {
//            case TAB_HOME: {
//                //Timber.d("Getting Home Fragment: " + position);
//                f = new HomeFragment();
//                break;
//            }
//            case TAB_CHECKIN: {
//                //Timber.d("Getting Check-in Fragment: " + position);
//                f = new CheckinFragment();
//                break;
//            }
//            case TAB_TIMER: {
//                //Timber.d("Getting PanicTimer Fragment: " + position);
//                f = new PanicTimerFragment();
//                break;
//            }
//            case TAB_CONTACTS: {
//                //Timber.d("Getting Contacts Fragment: " + position);
//                f = new ContactsFragment();
//                break;
//            }
            case TAB_CHAT: {
                //Timber.d("Getting ChatList Fragment: " + position);
                f = new G4ChatFragment();
                break;
            }
//            case TAB_COLLECT: {
//                //Timber.d("Getting Collect Fragment: " + position);
//                f = new CollectFragment();
//                break;
//            }
//            case TAB_MAP: {
//                //Timber.d("Getting Map Fragment: " + position);
//                f = new WebMapFragment();
//                break;
//            }
//            case TAB_POI: {
//                //Timber.d("Getting Poi Fragment: " + position);
//                f = new PoiRootFragment();
//                break;
//            }
//            case TAB_ALERTS: {
//                //Timber.d("Getting Alert Fragment: " + position);
//                f = new AlertRootFragment();
//                break;
////            }
//            case TAB_MEDIA: {
//                //Timber.d("Getting Collected Fragment: " + position);
//                f = new MediaRootFragment();
//                break;
//            }

            case TAB_MARKALERT:
                f = new MarkAlertFragment();
                break;
            case TAB_STATUS:
                f= new StatusFragment();

                break;
            default: {
                return null;
            }
        }
        return f;
    }

    public void setTabs(List<AgentTabs> tabs) {
        oldTabs.clear();
        oldTabs.addAll(pagerTabs);

        pagerTabs.clear();
        pagerTabs.addAll(tabs);

        notifyDataSetChanged();
    }

    public static CharSequence getPageTitle(Context context, AgentTabs tab) {
        switch (tab) {
//            case TAB_HOME:
//                return context.getString(R.string.tab_home);
//            case TAB_CHECKIN:
//                return context.getString(R.string.tab_checkin);
//            case TAB_TIMER:
//                return context.getString(R.string.tab_timer);
//            case TAB_CONTACTS:
//                return context.getString(R.string.tab_contacts);
            case TAB_CHAT:
                return context.getString(R.string.tab_chat);
//            case TAB_COLLECT:
//                return context.getString(R.string.tab_collect);
//            case TAB_MAP:
//                return context.getString(R.string.tab_map);
//            case TAB_POI:
//                return context.getString(R.string.tab_poi);
//            case TAB_ALERTS:
//                return context.getString(R.string.tab_alerts);
//            case TAB_MEDIA:
//                return context.getString(R.string.tab_media);

            case TAB_MARKALERT:
                return context.getString(R.string.tab_markalert);
            case TAB_STATUS:
                return context.getString(R.string.tab_status);
        }
        return null;
    }


}
