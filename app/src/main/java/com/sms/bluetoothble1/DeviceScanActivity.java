package com.sms.bluetoothble1;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.Manifest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceScanActivity extends Activity
{
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_DEVICE_NAME = "device_name";
    private static final String[] INITIAL_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
    };
    private static final String[] LOCATION_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int INITIAL_REQUEST=1337;
    private static final int LOCATION_REQUEST=INITIAL_REQUEST+3;
    //private LeDeviceListAdapter mLeDeviceListAdapter;
    //private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothAdapter mBtAdapter;
    private String mSelectedDeviceAddress;
    private String mSelectedDeviceName;


    /* -----------------------------------------------------------------------------
     *   onCreate()
     * -------------------------------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        //
        //  Location information from the phone is required for the Bluetooth scanner
        //  to return information.
        //  If it is not available, as the user to allow it.
        //
        if (!canAccessLocation()) {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }

        mHandler = new Handler();
        //mLeDeviceListAdapter = new LeDeviceListAdapter();
        Button btnScan = (Button)findViewById(R.id.btnScan);
        Button btnCancel = (Button)findViewById(R.id.btnCancel);
        ListView lstPairedDevices = (ListView)findViewById(R.id.lstPairedDevices);
        pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        //
        //  Get set of currently paired devices.
        //
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.lstPairedDevices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device: pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        //
        //  Setup Listview
        //
        lstPairedDevices.setAdapter(pairedDevicesArrayAdapter);
        lstPairedDevices.setOnItemClickListener(mDeviceClickListener);

        btnScan.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        scanLeDevice(true);
                    }
                }
        );

        btnCancel.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        cancelScan();
                    }
                }
        );
    }

    /* -----------------------------------------------------------------------------
     *   cancelScan() - Click event for the Cancel button.
     * -------------------------------------------------------------------------- */
    private void cancelScan()
    {
        mSelectedDeviceAddress = "";
        mSelectedDeviceName = "";
        finish();
    }


    /* -----------------------------------------------------------------------------
     *   canAccessLocation() - Bluetooth scanner will not return anything unless it
     *                         has access to location information from the phone.
     *                         This function check to see if location is available.
     *   return = true if available
     *            false otherwise
     * -------------------------------------------------------------------------- */
    private boolean canAccessLocation() {
        //return(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
        return (PackageManager.PERMISSION_GRANTED==checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }


    //private boolean hasPermission(String perm) {
    //    return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
    //}



    /* -----------------------------------------------------------------------------
     *   scanLeDevice() - Scan for BLE devices. Times out after x seconds.
     * -------------------------------------------------------------------------- */
    private void scanLeDevice(final boolean enable) {
        final long SCAN_PERIOD = 5000;
        final BluetoothLeScanner bleScanner = MainActivity.mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //MainActivity.gBluetoothAdapter.stopLeScan(mLeScanCallback);
                    bleScanner.stopScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            //MainActivity.gBluetoothAdapter.startLeScan(mLeScanCallback);
            bleScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            //MainActivity.gBluetoothAdapter.stopLeScan(mLeScanCallback);
            bleScanner.stopScan(mLeScanCallback);
        }
    }


    /* -----------------------------------------------------------------------------
     *   mLeScanCallback() - Callback for the BLE scan.
     * -------------------------------------------------------------------------- */
    private ScanCallback mLeScanCallback =
            new ScanCallback() {

                //@Override
//                public void onScanResult(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                public void onScanResult(final int callbackType, final ScanResult result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothDevice dev = result.getDevice();
                            String showName = dev.getName() + "\n" + dev.getAddress();
                            for (int j = 0; j<pairedDevicesArrayAdapter.getCount(); j++) {
                                if (pairedDevicesArrayAdapter.getItem(j).equals(showName))
                                    return;
                            }
                            pairedDevicesArrayAdapter.add(showName);
                            pairedDevicesArrayAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    /* -----------------------------------------------------------------------------------
        OnItemClickListener() - The on-click listener for all devices in the ListViews
    ------------------------------------------------------------------------------------ */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            mSelectedDeviceName = info.substring(0, info.length() - 18);
            mSelectedDeviceAddress = info.substring(info.length() - 17);
            finish();
        }
    };

    /* -----------------------------------------------------------------------------------
        finish()
    ------------------------------------------------------------------------------------ */
    @Override
    public void finish() {
        // Create the result Intent and include the MAC address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, mSelectedDeviceAddress);
        intent.putExtra(EXTRA_DEVICE_NAME, mSelectedDeviceName);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        super.finish();
    }

    /* Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private LeDeviceListAdapter mLeDeviceListAdapter;
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            //if(!mLeDevices.contains(device)) {
            //    mLeDevices.add(device);
            //}
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.l, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }


    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
    }
    */
}
