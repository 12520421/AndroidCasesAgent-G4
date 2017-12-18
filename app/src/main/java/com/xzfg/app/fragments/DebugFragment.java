package com.xzfg.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xzfg.app.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by APP-PC on 10/27/2017.
 */

public class DebugFragment  extends Fragment {

    private TextView debugtext;
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_debug, container, false);
        debugtext = (TextView) v.findViewById(R.id.debug_text);
        return v;
    }
    public void pasteLogToDebugScreen() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("G4Chat1")){
                    // Removes log tag and PID from the log line
                    log.append(line.substring(line.indexOf(": ") + 2)).append("\n");
                }
            }
            log.append(log.toString().replace(log.toString(), ""));
            debugtext.setText(log.toString());
        }
        catch (Exception e)
        {

        }

    }

    @Override
    public void onResume() {
        super.onResume();
        pasteLogToDebugScreen();
    }
}