package com.sms.bluetoothble1;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    public static final int GET_DEVICE_ADDRESS = 4;

    public static BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private String mDeviceName;

    /* ------------------------------------------------------------------------------
        onCreate()
    ------------------------------------------------------------------------------ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSelectDevice = (Button) findViewById(R.id.btnSelectDevice);

        btnSelectDevice.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        //Intent i = new Intent(this, DeviceScanActivity.class);
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
        onActivityResult() -Return from DeviceScanActivity()
    ------------------------------------------------------------------------------ */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_DEVICE_ADDRESS:
                if (data.hasExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS)) {
                    mDeviceAddress = data.getExtras().getString(DeviceScanActivity.EXTRA_DEVICE_ADDRESS);
                    if (mDeviceAddress.isEmpty()) {
                        TextView addr = (TextView) findViewById(R.id.txtDevAddress);
                        addr.setText("No Device Selected");
                    } else {
                        mDeviceName = data.getExtras().getString(DeviceScanActivity.EXTRA_DEVICE_NAME);
                        TextView addr = (TextView) findViewById(R.id.txtDevAddress);
                        addr.setText("Selected Device =\n" + mDeviceName);
                        connectDevice();
                    }
                }
                break;

            default:
                break;
        }
     }



    /* -----------------------------------------------------------------------------------
        connectDevice()
    ------------------------------------------------------------------------------------ */
    private void connectDevice() {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        //mChatService.connect(device, true);
    }

}
