package com.xzfg.app.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;


import com.xzfg.app.Application;
import com.xzfg.app.BaseFragment;
import com.xzfg.app.Events;
import com.xzfg.app.debug.DebugLog;
import com.xzfg.app.managers.G4Manager;
import com.xzfg.app.model.ListATCommand;
import com.xzfg.app.receivers.BluetoothReceiver;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static android.content.Context.BLUETOOTH_SERVICE;

public class GattClient  {

    private Timer mRssiTimer;
    public static UUID DESCRIPTOR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static UUID DESCRIPTOR_USER_DESC = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");

    public static UUID SERVICE_UUID = UUID.fromString("1D5688DE-866D-3AA4-EC46-A1BDDB37ECF6");
    public static UUID CHARACTERISTIC_COUNTER_UUID = UUID.fromString("AF20fBAC-2518-4998-9AF7-AF42540731B3");
    public static UUID CHARACTERISTIC_INTERACTOR_UUID = UUID.fromString("AF20fBAC-2518-4998-9AF7-AF42540731B3");
    private ArrayList<ListATCommand> ListCommandAll = new ArrayList<ListATCommand>();
    private ArrayList<ListATCommand> ListCommandEnter = new ArrayList<ListATCommand>();
    boolean isEnterCommand = false;
    StringBuilder sb = new StringBuilder(192);
    public static byte[] getUserDescription(UUID characteristicUUID) {
        String desc;

        if (CHARACTERISTIC_COUNTER_UUID.equals(characteristicUUID)) {
            desc = "Indicates the number of time you have been awesome so far";
        } else if (CHARACTERISTIC_INTERACTOR_UUID.equals(characteristicUUID)) {
            desc = "Write any value here to move the cat’s paw and increment the awesomeness counter";
        } else {
            desc = "";
        }

        return desc.getBytes(Charset.forName("UTF-8"));
    }
    private static final String TAG = GattClient.class.getSimpleName();
    public GattClient(){};
    public interface OnCounterReadListener {
        void onCounterRead(String value);

        void onConnected(boolean success);

        void onRSSIChange(int rssi);


    }

    private Context mContext;
    private OnCounterReadListener mListener;
    public String mDeviceAddress;
    int reconnect = 1;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
////
String temp="";
    private static GattClient instance;

    public static GattClient getInstance(){
        if (instance==null)
            instance=new GattClient();
        return instance;
    }
    public static abstract class DataResultBLE{
        public abstract void gotResult(String result);
    }
    public void getResultBLE(Context context,DataResultBLE  dataResultBLE){
        dataResultBLE.gotResult(temp);
    }

    boolean isConnected;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState==BluetoothProfile.STATE_CONNECTED){
                DebugLog.d("check state:::");
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT client. Attempting to start service discovery");
                gatt.discoverServices();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }
                TimerTask task = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        try{
                            mBluetoothGatt.readRemoteRssi();

                        }
                        catch(NullPointerException e){
                            e.toString();


                    }

                    }
                };
                mRssiTimer = new Timer();
                mRssiTimer.schedule(task, 5000, 1000 );
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                Log.i(TAG, "Disconnected from GATT client");
                mRssiTimer.cancel();
                EventBus.getDefault().postSticky(new Events.ReceiverMessageG4(false));
                Log.i("failed", "Disconnected from GATT client");
                EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
                try {
                    EventBus.getDefault().post(new Events.SendConnectionState(false));
                    mListener.onConnected(false);
                }catch (Exception e){
                    e.toString();
                }

                //onDestroy();
            }

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            //EventBus.getDefault().post(new Events.SendRssi(rssi));
            if (mListener!=null) {
                mListener.onRSSIChange(rssi);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean connected = false;

                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_COUNTER_UUID);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_CONFIG);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            connected = gatt.writeDescriptor(descriptor);
                        }
                    }
                }
                mListener.onConnected(connected);
                EventBus.getDefault().post(new Events.SendConnectionState(connected));
          /*      if(mBluetoothAdapter == null)
                {
                    mBluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                }
                BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                EventBus.getDefault().post(new Events.Interac(bluetoothDevice));*/


            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readCounterCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readCounterCharacteristic(characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (DESCRIPTOR_CONFIG.equals(descriptor.getUuid())) {
                BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_COUNTER_UUID);
                gatt.readCharacteristic(characteristic);
            }
        }

        private synchronized void readCounterCharacteristic(BluetoothGattCharacteristic characteristic) {
            try {
                if (CHARACTERISTIC_COUNTER_UUID.equals(characteristic.getUuid())) {
                    byte[] data = characteristic.getValue();

             //       String str = new String(data, Charset.defaultCharset());



                    String str = new String(data,java.nio.charset.Charset.forName("UTF-8"));
                    DebugLog.d("data check: "+str);

                    EventBus.getDefault().post(new Events.SendData(str));
                    //int value = Ints.fromByteArray(data);
                    Log.d("G4Chat1", "readCounter: " + str);
                    ListATCommand command = new ListATCommand();
                    command.setData(str);
                    if (isEnterCommand) {
                        command.setType(false);
                        ListCommandEnter.add(command);

                    } else {
                        command.setType(false);

                    }
                    ListCommandAll.add(command);
                    DebugLog.d("result :"+str);
                    mListener.onCounterRead(str);
                    Log.d("G4Chat1", "Listener run 1");


                    EventBus.getDefault().post(new Events.BluetoothData(str));
                    Log.d("G4Chat1", "Listener run 2");
                }
            }catch (Exception e){}
        }
    };

    public ArrayList<ListATCommand> getListCommandAll() {
        return ListCommandAll;
    }

    public void setListCommandAll(ArrayList<ListATCommand> listCommandAll) {
        ListCommandAll = listCommandAll;
    }

    public ArrayList<ListATCommand> getListCommandEnter() {
        return ListCommandEnter;
    }

    public void setListCommandEnter(ArrayList<ListATCommand> listCommandEnter) {
        ListCommandEnter = listCommandEnter;
    }



    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startClient(context);
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopClient();
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
    };

    public GattClient getGatt(){
        return this;
    }
    public GattClient(Application application){
        application.inject(this);
    }
    public void setContext(Context context){
        this.mContext = context;
    }

    public void setListener(OnCounterReadListener listener){
        mListener = listener;
    }

    public void onCreate(Context context, String deviceAddress, OnCounterReadListener listener) throws RuntimeException {
        mContext = context;
        mListener = listener;
        mDeviceAddress = deviceAddress;
        reconnect = 1;
        mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            throw new RuntimeException("GATT client requires Bluetooth support");
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceiver, filter);
        if (!mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is currently disabled... enabling");
            mBluetoothAdapter.enable();
        } else {
            Log.i(TAG, "Bluetooth enabled... starting client");
            startClient(mContext);
        }
    }
    public void unregister(){
        if (mBluetoothReceiver!=null){
            mContext.unregisterReceiver(mBluetoothReceiver);
        }
    }

    public void onDestroy() {
        mListener = null;
      //  mDeviceAddress = null;
        unregister();
        if (mBluetoothReceiver!=null) {
            mContext.unregisterReceiver(mBluetoothReceiver);
        }
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopClient();
        }

        try{
            mContext.unregisterReceiver(mBluetoothReceiver);

        }
        catch (IllegalArgumentException e)
        {

        }

    }

    public void writeInteractor(String command, boolean isEnter) {
        BluetoothGattCharacteristic interactor;

                ListATCommand listATCommand = new ListATCommand();
                listATCommand.setData(command);
                listATCommand.setType(isEnter);
                ListCommandAll.add(listATCommand);
                if( ListCommandAll.size()>600)
                {
                    Log.d("Size","size :"+ListCommandAll.size());
                    for(int i=0;i<=ListCommandAll.size()/2;i++){
                        ListCommandAll.remove(0);
                    }
                }
                if( ListCommandEnter.size()>600)
                {
                    Log.d("Size","size :"+ListCommandEnter.size());
                    for(int i=0;i<=ListCommandEnter.size()/2;i++){
                        ListCommandEnter.remove(0);
                    }
                }
        Log.d("Size","size :" + ListCommandEnter.size());
        Log.d("Size","size :" + ListCommandAll.size());
                if(isEnter)
                 {
                   ListCommandEnter.add(listATCommand);
                  }
            Log.d("G4Chat1","@write send command: "+command);

            try
            {
                interactor = mBluetoothGatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_INTERACTOR_UUID);
                StringBuffer strBuilder = new StringBuffer(command);
                if(!command.contains("AT$GMGS")) {
                    if(command.equals("\r") || command.equals("\n")){
                        return;
                    }
                    strBuilder.append("\r\n");
                }
                //interactor.setValue(URLEncoder.encode(strBuilder.toString(), "utf-8"));
                interactor.setValue(strBuilder.toString());
                Log.d("ExcuteCommand", "execute Command 1: " + command);
                isEnterCommand = isEnter;
                mBluetoothGatt.writeCharacteristic(interactor);

            }
            catch (Exception r)
            {
                Log.d("G4Chat1", "writeInteractor: send failed :" +r.toString());
                onCreate(mContext,mDeviceAddress,mListener);
            }
    }
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }
    public void stopNotification(){
        mBluetoothGatt.close();
        mBluetoothGatt.connect();
    }
    public void startClient(Context context) {
        mContext = context;
        if(mBluetoothAdapter == null)
        {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceiver, filter);
        if(mDeviceAddress !=null ) {

            BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            if (Build.VERSION.SDK_INT >= 23) {
                mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            }
            else{
                mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);

            }
            bluetoothDevice.getName();
            if (mBluetoothGatt == null) {

                Log.w(TAG, "Unable to create GATT client");
                return;
            }
        }
        else{
            SharedPreferences prefs = context.getSharedPreferences("mac_address", 0);
            String macaddress = prefs.getString("value","");
            if(prefs.getString("value","")!=null){
                BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(macaddress);
                if (Build.VERSION.SDK_INT >= 23) {
                    try {
                        bluetoothDevice.createBond();
                    }
                    catch (Exception e)
                    {
                    }
                    mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                    try {
                        bluetoothDevice.createBond();
                    }
                    catch (Exception e)
                    {

                    }
                }
                else{
                    mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);
                }
                bluetoothDevice.getName();
                if (mBluetoothGatt == null) {
                    Log.w(TAG, "Unable to create GATT client");
                    return;
                }
            }
        }


    }

    public void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        //    mDeviceAddress = null;
             mBluetoothGatt.close();
            mBluetoothGatt = null;
         //   EventBus.getDefault().postSticky(new Events.handleDisConnect("Connect"));
        }

        if (mBluetoothAdapter != null) {

            mBluetoothAdapter = null;
        }
    }




}
