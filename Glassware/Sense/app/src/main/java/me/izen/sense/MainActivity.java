package me.izen.sense;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.variable.framework.android.bluetooth.BluetoothService;

import java.util.LinkedHashMap;
import java.util.Set;

import me.izen.sense.sensors.NodePlusActivity;
import me.izen.sense.sensors.OpenEEGActivity;
import me.izen.sense.sensors.SensordroneActivity;


/**
 * An {@link Activity} showing a menu of sensors that can be connected to Glass
 * <p/>
 * The main content view is composed of a menu of sensors.
 */
public class MainActivity extends Activity {

    public static final String DEVICE_EKG_EMG = "EKG/EMG";
    public static final String DEVICE_SENSORDRONE = "Sensordrone";
    public static final String DEVICE_NODE = "NODE-";
    public final static String EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS";
    public final static String EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME";
    private static final String TAG = MainActivity.class.getSimpleName();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "BluetoothAdapter.STATE_ON");
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "BluetoothAdapter.STATE_OFF");
                } else if (state == BluetoothAdapter.STATE_CONNECTED) {
                    Log.d(TAG, "BluetoothAdapter.STATE_CONNECTED");
                } else if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                    Log.d(TAG, "BluetoothAdapter.STATE_DISCONNECTED");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                updateStatus("scanning...");
                Log.d(TAG, "BluetoothAdapter.ACTION_DISCOVERY_STARTED");

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                updateStatus("scanning complete, tap to connect");
                Log.d(TAG, "BluetoothAdapter.ACTION_DISCOVERY_FINISHED");

            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                Log.d(TAG, "BluetoothAdapter.ACTION_PAIRING_REQUEST");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Pair device " + device.getName());

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Found device " + device.getName());
                if (device.getName() != null) {
                    if (!mDeviceList.containsKey(device.getName())) {
                        if (device.getName().startsWith(DEVICE_NODE) || device.getName().startsWith(DEVICE_SENSORDRONE) ||
                                device.getName().startsWith(DEVICE_EKG_EMG)) {
                            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                                saveSensor(device);
                            } else if (device.getName().startsWith(DEVICE_SENSORDRONE)) {
                                device.setPairingConfirmation(false);
                                device.setPin("0000".getBytes());
                                device.createBond();
                            } else if (device.getName().startsWith(DEVICE_EKG_EMG)) {
                                saveSensor(device);
                            }
                        }
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    updateStatus("Paired with: " + device.getName());
                    saveSensor(device);
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    updateStatus("Unpaired device: " + device.getName());
                }
            }
        }
    };
    private static BluetoothService mService;
    /**
     * Handler used to post requests to start new activities so that the menu closing animation
     * works properly.
     */
    private final Handler mHandler = new Handler();
    /**
     * Listener that displays the options menu when the touchpad is tapped.
     */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            if (gesture == Gesture.TAP) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                openOptionsMenu();
                return true;
            } else {
                return false;
            }
        }
    };
    private LinkedHashMap<String, BluetoothDevice> mDeviceList = new LinkedHashMap<String, BluetoothDevice>();
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * Audio manager used to play system sound effects.
     */
    private AudioManager mAudioManager;
    /**
     * Gesture detector used to present the options menu.
     */
    private GestureDetector mGestureDetector;
    private TextView appActivity;
    private Menu sensorMenu;

    private void saveSensor(BluetoothDevice device) {
        updateStatus("found sensor: " + device.getName());
        if (device.getName().startsWith(DEVICE_NODE)) {
            MenuItem item = sensorMenu.findItem(R.id.sensor_node);
            item.setVisible(true);
        } else if (device.getName().startsWith(DEVICE_SENSORDRONE)) {
            MenuItem item = sensorMenu.findItem(R.id.sensor_sensordrone);
            item.setVisible(true);
        } else if (device.getName().startsWith(DEVICE_EKG_EMG)) {
            MenuItem item = sensorMenu.findItem(R.id.sensor_eeg);
            item.setVisible(true);
        }
        mDeviceList.put(device.getName(), device);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_start_sensor);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);

        mService = new BluetoothService();
        SenseApplication.setServiceAPI(mService);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        appActivity = (TextView) findViewById(R.id.tip_tap_for_options);

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mReceiver, filter);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private void updateStatus(String status) {
        appActivity.setText(status);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            ensureBluetoothIsOn();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.start_sensor, menu);
        this.sensorMenu = menu;
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector.onMotionEvent(event);
    }

    /**
     * The act of starting an activity here is wrapped inside a posted {@code Runnable} to avoid
     * animation problems between the closing menu and the new activity. The post ensures that the
     * menu gets the chance to slide down off the screen before the activity is started.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The startXXX() methods start a new activity, and if we call them directly here then
        // the new activity will start without giving the menu a chance to slide back down first.
        // By posting the calls to a handler instead, they will be processed on an upcoming pass
        // through the message queue, after the animation has completed, which results in a
        // smoother transition between activities.
        switch (item.getItemId()) {
            case R.id.sensor_node:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startNodePlus();
                    }
                });
                return true;

            case R.id.sensor_sensordrone:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startSensorDrone();
                    }
                });
                return true;

            case R.id.sensor_eeg:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startOpenEEG();
                    }
                });
                return true;

            case R.id.sensor_scan:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startScan();
                    }
                });
                return true;

            default:
                return false;
        }
    }

    private void startScan() {
        Log.d(TAG, "BEGIN: startScan");
        mBluetoothAdapter.startDiscovery();

    }

    /**
     * Starts the main game activity, and finishes this activity so that the user is not returned
     * to the splash screen when they exit.
     */
    private void startNodePlus() {
        Log.d(TAG, "BEGIN: startNodePlus");


        final Set<BluetoothDevice> mBondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        String bondedDevice = new String();
        int i = 0;
        for (BluetoothDevice device : mBondedDevices) {
            if (device.getName().startsWith(DEVICE_NODE)) {
                bondedDevice = device.getName();
                break;
            }
        }

        final Intent intent = new Intent(this, NodePlusActivity.class);
        intent.putExtra("deviceName", bondedDevice);
        startActivity(intent);
    }

    /**
     * Starts the tutorial activity, but does not finish this activity so that the splash screen
     * reappears when the tutorial is over.
     */
    private void startSensorDrone() {
        Log.d(TAG, "BEGIN: startSensorDrone");
        final Intent intent = new Intent(this, SensordroneActivity.class);

        final Set<BluetoothDevice> mBondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        String bondedDevice = new String();
        int i = 0;
        for (BluetoothDevice device : mBondedDevices) {
            if (device.getName().startsWith(DEVICE_SENSORDRONE)) {
                bondedDevice = device.getName();
                break;
            }
        }

        intent.putExtra("deviceName", bondedDevice);
        startActivity(intent);
    }

    private void startOpenEEG() {

        BluetoothDevice device = null;
        while (mDeviceList.keySet().iterator().hasNext()) {
            String deviceName = mDeviceList.keySet().iterator().next();

            if (deviceName.startsWith(DEVICE_EKG_EMG)) {
                device = mDeviceList.get(deviceName);
                break;
            }
        }

        String deviceAddress = device.getAddress(); // TODO - get the device address
        String name = device.getName(); // TODO - get the name

        Intent intent = new Intent(this, OpenEEGActivity.class);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
        intent.putExtra(EXTRA_DEVICE_NAME, name);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        ensureBluetoothIsOn();

    }

    /**
     * Invokes a new intent to request to start the bluetooth, if not already on.
     */
    private boolean ensureBluetoothIsOn() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(btIntent, 200);
            return false;
        }

        return true;
    }

}
