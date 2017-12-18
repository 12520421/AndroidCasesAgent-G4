package com.xzfg.app.receivers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.xzfg.app.Application;
import com.xzfg.app.DebugLog;
import com.xzfg.app.Events;
import com.xzfg.app.managers.AlertManager;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Created by VYLIEM on 5/9/2017.
 */

public class BluetoothReceiver extends BroadcastReceiver {
    @Inject
    public AlertManager alertManager;

    static String BLUETOOTH_NAME;


    @Override
    public void onReceive(Context context, Intent intent) {
        Application application = ((Application) context.getApplicationContext());
        application.inject(this);

        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            //Device found
        //    Toast.makeText(context, "Device found ", Toast.LENGTH_SHORT).show();
        }
        else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
          ///  Toast.makeText(context, "now device connected", Toast.LENGTH_SHORT).show();
              /*  BluetoothDevice devicename = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(devicename.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)){
                   if(devicename.getName().equals("Echo-3BR")){
                        alertManager.startPanicMode(true);
                    }
                }*/
        }
        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            //Done searching
         //   Toast.makeText(context, "Done searching", Toast.LENGTH_SHORT).show();
        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            //Device is about to disconnect
           /*   Toast.makeText(context, "Device is about to disconnect", Toast.LENGTH_SHORT).show();
            SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(context);
            String bluetoothname = pre.getString("device_name","");
            Log.d("Liem","ten bluetooth:"+bluetoothname);
            BluetoothDevice devicename = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(devicename.getName().equals(bluetoothname)){
                alertManager.startPanicMode(true);
            }
            if(device.getName().equals(devicename)){
                alertManager.startPanicMode(true);
            }*/
        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
           // EventBus.getDefault().post(new Events.handleDisConnect("Connect"));

        }
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch(state) {
                case BluetoothAdapter.STATE_OFF:
               //     EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
              //      EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
                    break;
                case BluetoothAdapter.STATE_ON:

                    break;
                case BluetoothAdapter.STATE_TURNING_ON:

                    break;
            }
        }
    }
}
