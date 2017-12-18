package com.xzfg.app.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.xzfg.app.R;
import com.xzfg.app.model.AgentContacts.AgentContact;

import java.util.List;


public class CheckinContactsAdapter extends ArrayAdapter<CheckinContactsAdapter.CheckinContact> {

    private class ViewHolder {
        Switch contact;
    }

    public CheckinContactsAdapter(Context ctx, List<CheckinContact> objects) {
        super(ctx, 0);
        if (objects != null) {
            for (CheckinContact obj : objects) add(obj);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.row_checkin_contact, null, false);

            holder = new ViewHolder();
            holder.contact = (Switch) convertView.findViewById(R.id.contact_switch);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final CheckinContact item = getItem(position);
        holder.contact.setText(item.getName());
        holder.contact.setChecked(item.isSelected());
        holder.contact.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setSelected(isChecked);
            }
        });

        return convertView;
    }


    //
    public static class CheckinContact extends AgentContact {
        private boolean selected;

        public CheckinContact(AgentContact contact, boolean selected) {
            super(contact);
            this.selected = selected;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

}

