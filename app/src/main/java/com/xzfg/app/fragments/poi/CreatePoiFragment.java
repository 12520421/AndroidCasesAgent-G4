package com.xzfg.app.fragments.poi;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.fragments.dialogs.AlertDialogFragment;
import com.xzfg.app.fragments.dialogs.CreatePoiProgressDialogFragment;
import com.xzfg.app.managers.PoiManager;
import com.xzfg.app.model.PoiCategory;
import com.xzfg.app.model.PoiGroup;
import com.xzfg.app.services.CreatePoiService;

import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 *
 */
public class CreatePoiFragment extends Fragment implements View.OnClickListener {

    @Inject
    Application application;

    @Inject
    PoiManager poiManager;

    Double latitude;
    Double longitude;
    String address;
    LinkedList<String> categoryIds = new LinkedList<>();
    LinkedList<String> categories = new LinkedList<>();
    LinkedList<String> groups = new LinkedList<>();
    ArrayAdapter<String> groupsAdapter;
    ArrayAdapter<String> categoryAdapter;
    EditText nameView;
    EditText descriptionView;
    EditText addressView;
    EditText latitudeView;
    EditText longitudeView;
    Spinner groupView;
    Spinner categoryView;

    AlertDialogFragment alert;
    CreatePoiProgressDialogFragment create;
    AlertDialogFragment errorDialogFragment;
    AlertDialogFragment successDialogFragment;

    public static CreatePoiFragment newInstance(Events.CreatePoi createEvent) {
        CreatePoiFragment f = new CreatePoiFragment();
        Bundle args = new Bundle();
        if (createEvent.getLatitude() != null)
            args.putDouble("latitude", createEvent.getLatitude());
        if (createEvent.getLongitude() != null)
            args.putDouble("longitude", createEvent.getLongitude());
        if (createEvent.getAddress() != null) {
            try {
                args.putString("address", URLDecoder.decode(createEvent.getAddress(), "UTF-8"));
            } catch (Exception e) {
                Timber.w(e, "Couldn't decode address.");
            }
        }
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application) getActivity().getApplication()).inject(this);
        Bundle arguments = getArguments();

        List<PoiGroup> poiGroups = poiManager.getUnrestrictedGroups();
        if (poiGroups != null) {
            for (PoiGroup i : poiGroups) {
                groups.add(i.getName());
            }
        }
        List<PoiCategory> poiCategories = poiManager.getCategories();
        if (poiCategories != null) {
            for (PoiCategory i : poiCategories) {
                categories.add(i.getName());
                categoryIds.add(i.getId());
            }
        }

        groupsAdapter = new ArrayAdapter<>(getActivity(), R.layout.start_spinner_item, groups);
        groupsAdapter.setDropDownViewResource(R.layout.start_spinner_dropdown_item);

        categoryAdapter = new ArrayAdapter<>(getActivity(), R.layout.start_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(R.layout.start_spinner_dropdown_item);

        if (arguments != null) {
            if (arguments.containsKey("latitude")) {
                latitude = arguments.getDouble("latitude");
                //Timber.d("Got latitude: " + latitude);
            }
            if (arguments.containsKey("longitude")) {
                longitude = arguments.getDouble("longitude");
                //Timber.d("Got longitude: " + longitude);
            }
            if (arguments.containsKey("address")) {
                address = arguments.getString("address");
                //Timber.d("Got address: " + address);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.createpoi, viewGroup, false);

        nameView = (EditText) view.findViewById(R.id.name);
        descriptionView = (EditText) view.findViewById(R.id.description);

        addressView = (EditText) view.findViewById(R.id.address);
        if (address != null)
            addressView.setText(address);

        latitudeView = (EditText) view.findViewById(R.id.latitude);
        if (latitude != null)
            latitudeView.setText(String.valueOf(latitude));

        longitudeView = (EditText) view.findViewById(R.id.longitude);
        if (longitude != null)
            longitudeView.setText(String.valueOf(longitude));

        groupView = (Spinner) view.findViewById(R.id.group);
        groupView.setAdapter(groupsAdapter);

        categoryView = (Spinner) view.findViewById(R.id.category);
        categoryView.setAdapter(categoryAdapter);

        view.findViewById(R.id.btn_ok).setOnClickListener(this);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        if (alert != null) {
            alert.dismissAllowingStateLoss();
            alert = null;
        }
        if (create != null) {
            create.dismissAllowingStateLoss();
            create = null;
        }
        if (successDialogFragment != null) {
            successDialogFragment.dismissAllowingStateLoss();
            successDialogFragment = null;
        }
        if (errorDialogFragment != null) {
            errorDialogFragment.dismissAllowingStateLoss();
            errorDialogFragment = null;
        }
        super.onPause();
    }

    public void onEventMainThread(Events.PoiCreated poiCreated) {
        //Timber.d("POI Created!");
        // get rid of the registration in progress dialog.
        CreatePoiProgressDialogFragment f = (CreatePoiProgressDialogFragment) getFragmentManager().findFragmentByTag("createDialog");
        if (f != null) {
            f.setCustomCancelListener(null);
            f.setCustomDismissListener(null);
            f.setCustomOnDestroyListener(null);
            f.dismissAllowingStateLoss();
            getFragmentManager().executePendingTransactions();
        }

        if (poiCreated.getStatus()) {
            FragmentManager fm = getFragmentManager();
            successDialogFragment = AlertDialogFragment.newInstance(getString(R.string.success_title), "Poi Created Successfully");
            successDialogFragment.show(getFragmentManager(), "poi_success");
            successDialogFragment.setCustomCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    getActivity().onBackPressed();
                }
            });
        } else {
            errorDialogFragment = AlertDialogFragment.newInstance(getString(R.string.error_title), poiCreated.getMessage());
            errorDialogFragment.show(getFragmentManager(), "poi_error");
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_ok: {
                if (v.getId() == R.id.btn_ok) {

                    String latitudeStr = latitudeView.getText().toString().trim();
                    if (latitudeStr.length() <= 0) {
                        if (alert != null) {
                            alert.dismiss();
                        }
                        alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.latitude)));
                        alert.show(getFragmentManager(), "name");
                        return;
                    } else {
                        try {
                            latitude = Double.parseDouble(latitudeStr);
                        } catch (NumberFormatException nfe) {
                            if (alert != null) {
                                alert.dismiss();
                            }
                            alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.latitude)));
                            alert.show(getFragmentManager(), "name");
                            return;
                        }
                    }

                    String longitudeStr = longitudeView.getText().toString().trim();
                    if (longitudeStr.length() <= 0) {
                        if (alert != null) {
                            alert.dismiss();
                        }
                        alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.longitude)));
                        alert.show(getFragmentManager(), "name");
                        return;
                    } else {
                        try {
                            longitude = Double.parseDouble(longitudeStr);
                        } catch (NumberFormatException nfe) {
                            if (alert != null) {
                                alert.dismiss();
                            }
                            alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.longitude)));
                            alert.show(getFragmentManager(), "name");
                            return;
                        }
                    }

                    String name = nameView.getText().toString().trim();
                    if (name.length() == 0) {
                        if (alert != null) {
                            alert.dismiss();
                        }
                        alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.poi_name)));
                        alert.show(getFragmentManager(), "name");
                        return;
                    }
                    if (latitudeView.getText().toString().trim().length() == 0) {
                        if (alert != null) {
                            alert.dismiss();
                        }
                        alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.latitude)));
                        alert.show(getFragmentManager(), "latitude");
                        return;
                    }
                    if (longitudeView.getText().toString().trim().length() == 0) {
                        alert = AlertDialogFragment.newInstance(getString(R.string.error_title), getString(R.string.field_required, getString(R.string.longitude)));
                        alert.show(getFragmentManager(), "latitude");
                        return;
                    }
                    Intent i = new Intent(getActivity(), CreatePoiService.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("name", name);
                    bundle.putString("description", nameView.getText().toString().trim());
                    bundle.putString("address", addressView.getText().toString().trim());
                    bundle.putString("latitude", String.valueOf(latitude));
                    bundle.putString("longitude", String.valueOf(longitude));
                    if (categoryIds != null && categoryView != null && !categoryIds.isEmpty() && categoryView.getSelectedItemPosition() < categoryIds.size()) {
                        bundle.putString("categoryId", categoryIds.get(categoryView.getSelectedItemPosition()));
                    }
                    else {
                        if (create != null) {
                            create.dismiss();
                        }
                        Toast.makeText(getActivity(), getString(R.string.cantcreatepoi),Toast.LENGTH_SHORT).show();
                    }
                    bundle.putString("group", (String) groupView.getSelectedItem());
                    i.putExtras(bundle);
                    //Timber.d("Creating Dialog");
                    if (create != null) {
                        create.dismiss();
                    }
                    create = CreatePoiProgressDialogFragment.newInstance(getString(R.string.create_poi), getString(R.string.creating_poi, name, String.valueOf(latitude), String.valueOf(longitude)));
                    create.show(getFragmentManager(), "createDialog");
                    //Timber.d("Starting service.");
                    getActivity().startService(i);
                }
                break;
            }
            case R.id.cancel: {
                getActivity().onBackPressed();
            }
        }
    }
}
