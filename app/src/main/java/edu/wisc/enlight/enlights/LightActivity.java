package edu.wisc.enlight.enlights;

import android.app.ActionBar;
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
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.util.Timer;
import java.util.TimerTask;


public class LightActivity extends Activity {
    private final static String TAG = LightActivity.class.getSimpleName();
    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;

    private byte[] data = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;
    private final int REFRESHTIME = 200;

    private LinearLayout buttonLayout;
    private LayoutInflater inflater;
    private Button connectButton;
    private Timer sendTimer;
    private ProgressBar connectProgress;

    private ColorPicker picker;
    private SVBar svBar;
    private OpacityBar opacityBar;
    private SaturationBar saturationBar;
    private ValueBar valueBar;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light);
        this.setTitle("Control the Lights!");
        connectProgress = (ProgressBar) findViewById(R.id.progress_connect);
        buttonLayout = (LinearLayout) findViewById(R.id.layout_buttons);
        inflater = (LayoutInflater.from(this));
        picker = (ColorPicker) findViewById(R.id.picker);
        svBar = (SVBar) findViewById(R.id.svbar);
        picker.addSVBar(svBar);

        picker.setShowOldCenterColor(false);

        picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int i) {
                //change the color of all active buttons
                for (int j = 0; j < 6; j++) {
                    View currView = buttonLayout.getChildAt(j);
                    ToggleButton button = (ToggleButton) currView.findViewById(R.id.button_toggle);
                    if (button.isChecked()) {
                        long color = (long) picker.getColor();
                        long redColor = color & 0x00FF0000;
                        redColor = 0x00FF0000 - redColor;
                        long blueColor = color & 0x000000FF;
                        blueColor = 0x000000FF - blueColor;
                        long greenColor = color & 0x0000FF00;
                        greenColor = 0x0000FF00 - greenColor;
                        button.setTextColor((int) (0xFF000000 + redColor + greenColor + blueColor));
                        button.setBackgroundColor(picker.getColor());
                    }
                }
            }
        });
        connectButton = (Button) findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanFlag == false) {
                    connectProgress.setVisibility(View.VISIBLE);
                    connectButton.setVisibility(View.INVISIBLE);
                    scanLeDevice();

                    Timer mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mDevice != null) {
                                mDeviceAddress = mDevice.getAddress();
                                mBluetoothLeService.connect(mDeviceAddress);
                                scanFlag = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast toast = Toast
                                                .makeText(
                                                        LightActivity.this,
                                                        "No device found!",
                                                        Toast.LENGTH_SHORT);
                                        toast.setGravity(0, 0, Gravity.CENTER);
                                        toast.show();
                                    }
                                });
                            }
                        }
                    }, SCAN_PERIOD);
                }

                System.out.println(connState);
                if (connState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    connectProgress.setVisibility(View.VISIBLE);
                    connectButton.setVisibility(View.INVISIBLE);
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }
            }
        });

        for (int i = 0; i < 6; i++) {
            final View view = inflater.inflate(R.layout.button_light, null);
            final ToggleButton button = (ToggleButton) view.findViewById(R.id.button_toggle);
            final RelativeLayout borderView = (RelativeLayout) view.findViewById(R.id.button_background);
            final int buttonNum = i + 1;
            view.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
            button.setBackgroundColor(picker.getColor());
            button.setChecked(true);
            button.setText("" + (i + 1));
            button.setTextColor(0xFF000000);
            button.setBackgroundColor(picker.getColor());
            borderView.setBackgroundColor(0xFF000000);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (button.isChecked()) {
                        button.setText("" + (buttonNum));
                        long color = (long) picker.getColor();
                        long redColor = color & 0x00FF0000;
                        redColor = 0x00FF0000 - redColor;
                        long blueColor = color & 0x000000FF;
                        blueColor = 0x000000FF - blueColor;
                        long greenColor = color & 0x0000FF00;
                        greenColor = 0x0000FF00 - greenColor;
                        button.setTextColor((int) (0xFF000000 + redColor + greenColor + blueColor));
                        button.setBackgroundColor(picker.getColor());
                        borderView.setBackgroundColor(0xFF000000);
                    } else {
                        button.setText("" + (buttonNum));
                        borderView.setBackgroundColor(0x00000000);
                    }
                }
            });
            buttonLayout.addView(view);
        }


        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(this,
                RBLService.class);
        if (!bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE))
            Log.e("Connection", "Binding service failed!");
    }

    private void TimerMethod(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm.isScreenOn()){
                    if (connState){
                        String sendString = "";
                        for (int i = 0; i < 6; i++) {
                            ToggleButton currButton = (ToggleButton) buttonLayout.getChildAt(i).findViewById(R.id.button_toggle);
                            long color = ((ColorDrawable) currButton.getBackground()).getColor();
                            long redColor = color & 0x00FF0000;
                            redColor = redColor / 0x00010000;
                            long blueColor = color & 0x000000FF;
                            long greenColor = color & 0x0000FF00;
                            greenColor = greenColor / 0x00000100;
                            if (redColor > 255){
                                redColor = 255;
                            }
                            if (greenColor > 255){
                                greenColor = 255;
                            }
                            if (blueColor > 255){
                                blueColor = 255;
                            }
                            //Log.e("Color", "color is " + redColor + " " + greenColor + " " + blueColor);
                            sendString = (i + 1) + "|" + redColor + "|" + greenColor + "|" + blueColor + "^";
                            byte[] buf = sendString.getBytes();
                            if (characteristicTx != null) {
                                characteristicTx.setValue(buf);
                                mBluetoothLeService.writeCharacteristic(characteristicTx);
                            }
                        }
                    }
                }
            }
        });
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();
                connectButton.setVisibility(View.VISIBLE);
                connectProgress.setVisibility(View.INVISIBLE);
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

                //readAnalogInValue(data);
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                //displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_light, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        this.sendTimer = new Timer();
        this.sendTimer.schedule(new TimerTask(){
            @Override
            public void run(){
                TimerMethod();
            }
        }, 0, REFRESHTIME);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device != null) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    @Override
    protected void onStop() {
        super.onStop();

        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            try {
                unbindService(mServiceConnection);
            }catch(IllegalArgumentException e){
                Log.e(TAG, "Receiver never registered");
            }
    }

    @Override
    protected void onPause(){
        super.onPause();
        this.sendTimer.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setButtonEnable() {
        flag = true;
        connState = true;
        connectButton.setText("Disconnect");
    }

    private void setButtonDisable() {
        flag = false;
        connState = false;
        connectButton.setVisibility(View.VISIBLE);
        connectProgress.setVisibility(View.INVISIBLE);
        connectButton.setText("Connect");
    }

}


