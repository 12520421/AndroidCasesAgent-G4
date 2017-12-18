package com.xzfg.app.widgets;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.xzfg.app.Application;
import com.xzfg.app.Events;
import com.xzfg.app.R;
import com.xzfg.app.model.AgentRoleComponent;
import com.xzfg.app.model.AgentRoles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * This widget implements the custom elements in the action bar. It listens for various events, and
 * updates the icons inside it.
 */
public class NavdrawerContentsView extends LinearLayout implements View.OnClickListener,AgentRoleComponent {

    Button homeButton;
    Button timerButton;
    Button checkinButton;
    Button contactsButton;
    Button chatButton;
    Button collectButton;
    Button mapButton;
    Button alertsButton;
    Button mediaButton;
    Button poisButton;
    Button preferencesButton;
    Button settingsButton;
    Button wipeButton;
    Button bossButton;
    Button sosButton;
    Button helpButton;
    Button commandsButton;
    Button markalertButton;
    Button statusButton;
    Button configurationButton;
    Button unregisterButton;
    private List<Button> buttons = new ArrayList<>();
    private HashMap<Integer, Integer> onStates = new HashMap<>();
    private HashMap<Integer, Integer> offStates = new HashMap<>();

    public NavdrawerContentsView(Context context) {
        super(context);
        init();
    }

    public NavdrawerContentsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NavdrawerContentsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.START);

        Context context = getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View contents = inflater.inflate(R.layout.navdrawer_contents_view, this, true);

        homeButton = (Button) contents.findViewById(R.id.home);
        homeButton.setOnClickListener(this);
        buttons.add(homeButton);
        onStates.put(R.id.home, R.drawable.menu_home_on);
        offStates.put(R.id.home, R.drawable.menu_home_off);

        timerButton = (Button) contents.findViewById(R.id.timer);
        timerButton.setOnClickListener(this);
        buttons.add(timerButton);
        onStates.put(R.id.timer, R.drawable.menu_timer_on);
        offStates.put(R.id.timer, R.drawable.menu_timer_off);

        checkinButton = (Button) contents.findViewById(R.id.checkin);
        checkinButton.setOnClickListener(this);
        buttons.add(checkinButton);
        onStates.put(R.id.checkin, R.drawable.menu_checkin_on);
        offStates.put(R.id.checkin, R.drawable.menu_checkin_off);

        contactsButton = (Button) contents.findViewById(R.id.contacts);
        contactsButton.setOnClickListener(this);
        buttons.add(contactsButton);
        onStates.put(R.id.contacts, R.drawable.menu_contacts_on);
        offStates.put(R.id.contacts, R.drawable.menu_contacts_off);

        chatButton = (Button) contents.findViewById(R.id.chat);
        chatButton.setOnClickListener(this);
        buttons.add(chatButton);
        onStates.put(R.id.chat, R.drawable.menu_chat_on);
        offStates.put(R.id.chat, R.drawable.menu_chat_off);

        collectButton = (Button) contents.findViewById(R.id.collect);
        collectButton.setOnClickListener(this);
        buttons.add(collectButton);
        onStates.put(R.id.collect, R.drawable.menu_collect_on);
        offStates.put(R.id.collect, R.drawable.menu_collect_off);

        sosButton = (Button) contents.findViewById(R.id.sos);
        sosButton.setOnClickListener(this);

        mapButton = (Button) contents.findViewById(R.id.map);
        mapButton.setOnClickListener(this);
        buttons.add(mapButton);
        onStates.put(R.id.map, R.drawable.menu_map_on);
        offStates.put(R.id.map, R.drawable.menu_map_off);

        alertsButton = (Button) contents.findViewById(R.id.alerts);
        alertsButton.setOnClickListener(this);
        buttons.add(alertsButton);
        onStates.put(R.id.alerts, R.drawable.menu_alerts_on);
        offStates.put(R.id.alerts, R.drawable.menu_alerts_off);

        mediaButton = (Button) contents.findViewById(R.id.collected_media);
        mediaButton.setOnClickListener(this);
        buttons.add(mediaButton);
        onStates.put(R.id.collected_media, R.drawable.menu_media_on);
        offStates.put(R.id.collected_media, R.drawable.menu_media_off);

        poisButton = (Button) contents.findViewById(R.id.poi);
        poisButton.setOnClickListener(this);
        buttons.add(poisButton);
        onStates.put(R.id.poi, R.drawable.menu_pois_on);
        offStates.put(R.id.poi, R.drawable.menu_pois_off);

        bossButton = (Button) contents.findViewById(R.id.boss);
        bossButton.setOnClickListener(this);
        buttons.add(bossButton);
        onStates.put(R.id.boss, R.drawable.menu_boss_on);
        offStates.put(R.id.boss, R.drawable.menu_boss_off);

        preferencesButton = (Button) contents.findViewById(R.id.preferences);
        preferencesButton.setOnClickListener(this);
        buttons.add(preferencesButton);
        onStates.put(R.id.preferences, R.drawable.menu_preferences_on);
        offStates.put(R.id.preferences, R.drawable.menu_preferences_off);

        settingsButton = (Button) contents.findViewById(R.id.settings);
        settingsButton.setOnClickListener(this);
        buttons.add(settingsButton);
        onStates.put(R.id.settings, R.drawable.menu_settings_on);
        offStates.put(R.id.settings, R.drawable.menu_settings_off);

        wipeButton = (Button) contents.findViewById(R.id.wipe);
        wipeButton.setOnClickListener(this);
        buttons.add(wipeButton);
        onStates.put(R.id.wipe, R.drawable.menu_wipe_on);
        offStates.put(R.id.wipe, R.drawable.menu_wipe_off);

        helpButton = (Button)contents.findViewById(R.id.help);
        helpButton.setOnClickListener(this);
        buttons.add(helpButton);
        onStates.put(R.id.help, R.drawable.menu_help_on);
        offStates.put(R.id.help, R.drawable.menu_help_off);
        ///

        commandsButton = (Button) contents.findViewById(R.id.atcommands);
        commandsButton.setOnClickListener(this);
        buttons.add(commandsButton);
        onStates.put(R.id.atcommands,R.drawable.menu_atcommand_on);
        offStates.put(R.id.atcommands,R.drawable.menu_atcommand_off);

        markalertButton = (Button) contents.findViewById(R.id.markalert);
        markalertButton.setOnClickListener(this);
        buttons.add(markalertButton);
        onStates.put(R.id.markalert,R.drawable.menu_markalert_on);
        offStates.put(R.id.markalert,R.drawable.menu_markalert_off);

        statusButton = (Button) contents.findViewById(R.id.status);
        statusButton.setOnClickListener(this);
        buttons.add(statusButton);
        offStates.put(R.id.status, R.drawable.menu_status_off);
        onStates.put(R.id.status,R.drawable.menu_status_on);

        configurationButton = (Button) contents.findViewById(R.id.configuration);
        configurationButton.setOnClickListener(this);
        buttons.add(configurationButton);
        offStates.put(R.id.configuration,R.drawable.menu_configuration_off);
        onStates.put(R.id.configuration,R.drawable.menu_configuration_on);

        unregisterButton = (Button) contents.findViewById(R.id.unregister);
        unregisterButton.setOnClickListener(this);
        buttons.add(unregisterButton);
        offStates.put(R.id.unregister,R.drawable.menu_unregister_off);
        onStates.put(R.id.unregister,R.drawable.menu_unregister_on);
        //    buttons.add(unregisterButton);

        if (context instanceof Activity) {
            updateRoles(((Application) ((Activity) context).getApplication()).getAgentSettings().getAgentRoles());
        }
        EventBus.getDefault().registerSticky(this);
    }

    public void enable(int resId) {
        //Timber.d("Enabling button for id: " + resId);
        for (Button button : buttons) {
            if (button.getId() == resId) {
                button.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(getContext(),onStates.get(button.getId())), null, null, null);
            } else {
                button.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getContext(),offStates.get(button.getId())), null, null, null);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.DisplayChanged displayChangedEvent) {
        //Timber.d("Enabling button for name: " + displayChangedEvent.getName());
        enable(displayChangedEvent.getId());
    }

    public void onEventMainThread(Events.AgentRolesUpdated event) {
        updateRoles(event.getAgentRoles());
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            Button b = (Button) v;
            EventBus.getDefault().post(new Events.MenuItemSelected(b.getText().toString(), b.getId()));
        }
    }

    @SuppressWarnings("ResourceType")
    @Override
    public void updateRoles(AgentRoles roles) {
      /*  homeButton.setVisibility(visible(roles.home()));
        timerButton.setVisibility(visible(roles.panictimed()));
        checkinButton.setVisibility(visible(roles.checkin()));
        chatButton.setVisibility(visible(roles.chat()));
        collectButton.setVisibility(visible(roles.collect()));
        mapButton.setVisibility(visible(roles.map()));
        poisButton.setVisibility(visible(roles.pointsofinterest()));
        alertsButton.setVisibility(visible(roles.alert()));
        mediaButton.setVisibility(visible(roles.collectedmedia()));
        bossButton.setVisibility(visible(roles.bossmode()));
        // Hide Wipe menu item if no role or no dev admin available
        wipeButton.setVisibility(visible(roles.wipedata() && ((Application) ((Activity) getContext()).getApplication()).isAdminReady()));
        preferencesButton.setVisibility(visible(roles.preferences()));
        sosButton.setVisibility(visible(roles.panicovert()));
        helpButton.setVisibility(visible(roles.help()));*/
        homeButton.setVisibility(View.GONE);
        timerButton.setVisibility(View.GONE);
        checkinButton.setVisibility(View.GONE);
        chatButton.setVisibility(visible(roles.g4_messenger()));
        contactsButton.setVisibility(View.GONE);
        collectButton.setVisibility(View.GONE);
        mapButton.setVisibility(View.GONE);
        poisButton.setVisibility(View.GONE);
        alertsButton.setVisibility(View.GONE);
        mediaButton.setVisibility(View.GONE);
        bossButton.setVisibility(View.GONE);
        // Hide Wipe menu item if no role or no dev admin available
        wipeButton.setVisibility(visible(roles.wipedata() && ((Application) ((Activity) getContext()).getApplication()).isAdminReady()));
        preferencesButton.setVisibility(View.GONE);
        settingsButton.setVisibility(View.GONE);
        sosButton.setVisibility(View.GONE);
        helpButton.setVisibility(View.GONE);

        commandsButton.setVisibility(visible(roles.g4_at_commands()));
        markalertButton.setVisibility(visible(roles.g4_mark_alert()));
        statusButton.setVisibility(visible(roles.g4_status()));
        configurationButton.setVisibility(visible(roles.g4_configuration()));
        unregisterButton.setVisibility(View.VISIBLE);
    }

    private int visible(boolean hasRole) {
        return hasRole ? View.VISIBLE : View.GONE;
    }
}