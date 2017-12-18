package com.xzfg.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.xzfg.app.R;
import com.xzfg.app.model.AgentContacts.AgentContact;

import java.util.List;


public class AgentContactsAdapter extends ArrayAdapter<AgentContact> {
    private String mAgentEmail;

    private class ViewHolder {
        TextView name;
    }

    public AgentContactsAdapter(Context ctx, List<AgentContact> objects, String agentEmail) {
        super(ctx, 0);
        mAgentEmail = agentEmail.trim();

        if (objects != null) {
            for (AgentContact obj : objects) add(obj);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(android.R.layout.simple_list_item_1, null);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AgentContact item = getItem(position);
        if (mAgentEmail.equalsIgnoreCase(item.getEmailAddress().trim()))
            holder.name.setText(convertView.getContext().getString(R.string.__you_,item.getName()));
        else
            holder.name.setText(item.getName());

        return convertView;
    }
}

