package com.sms.bluetoothble1;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    public static final int GET_DEVICE_ADDRESS = 4;

    public static BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress = "30:ae:a4:2c:38:3e";
    private String mDeviceName = "LED_STRIP";
    private boolean mConnected = false;
    private BluetoothLeService mBluetoothLeService;
    private static final String TAG = "MainActivity";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                List<BluetoothGattService> list = mBluetoothLeService.getSupportedGattServices();
                if (mConnected) {
                    mConnected = true;  // temporary for breakpt
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    /* ------------------------------------------------------------------------------
        onCreate()
    ------------------------------------------------------------------------------ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showConnectionState(false);
        Button btnSelectDevice = findViewById(R.id.btnSelectDevice);


        btnSelectDevice.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        Intent i = new Intent(MainActivity.this, DeviceScanActivity.class);
                        startActivityForResult(i, GET_DEVICE_ADDRESS);
                    }
                }
        );


        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    /* ------------------------------------------------------------------------------
       onResume()
   ------------------------------------------------------------------------------ */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        //Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        //bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /* ------------------------------------------------------------------------------
        onPause()
    ------------------------------------------------------------------------------ */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    /* ------------------------------------------------------------------------------
        onDestroy()
    ------------------------------------------------------------------------------ */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /* ------------------------------------------------------------------------------
        showConnectionState()
    ------------------------------------------------------------------------------ */
    private void showConnectionState(boolean connected) {
        TextView t = findViewById(R.id.txtDevAddress);
        if (connected ) {
            t.setText("Connected:\n" + mDeviceName + "\n" + mDeviceAddress);
        } else {
            t.setText("Not Connected");
        }
    }

    /* ------------------------------------------------------------------------------
        onActivityResult() -Return from DeviceScanActivity()
    ------------------------------------------------------------------------------ */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_DEVICE_ADDRESS:
                if (data.hasExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS)) {
                    mDeviceAddress = data.getExtras().getString(DeviceScanActivity.EXTRA_DEVICE_ADDRESS);
                    if (mDeviceAddress.isEmpty()) {
                        //TextView addr = (TextView) findViewById(R.id.txtDevAddress);
                        //addr.setText("No Device Selected");
                        showConnectionState(false);
                    } else {
                        mDeviceName = data.getExtras().getString(DeviceScanActivity.EXTRA_DEVICE_NAME);
                        //TextView addr = (TextView) findViewById(R.id.txtDevAddress);
                        //addr.setText("Connected:\n" + mDeviceName + "\n" + mDeviceAddress);
                        showConnectionState(true);
                        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                    }
                }
                break;

            default:
                break;
        }
     }





    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        //mDataField.setText(R.string.no_data);
    }


    private void displayData(String data) {
        if (data != null) {
            //mDataField.setText(data);
        }
    }

    /* ------------------------------------------------------------------------------
        makeGattUpdateIntentFilter()
    ------------------------------------------------------------------------------ */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
