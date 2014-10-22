package me.izen.sense.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.variable.framework.android.bluetooth.BluetoothService;
import com.variable.framework.android.bluetooth.DefaultBluetoothDevice;
import com.variable.framework.dispatcher.DefaultNotifier;
import com.variable.framework.node.AndroidNodeDevice;
import com.variable.framework.node.BaseSensor;
import com.variable.framework.node.ClimaSensor;
import com.variable.framework.node.LumaSensor;
import com.variable.framework.node.NodeDevice;
import com.variable.framework.node.enums.NodeEnums;
import com.variable.framework.node.interfaces.ProgressUpdateListener;
import com.variable.framework.node.reading.SensorReading;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.Set;

import me.izen.sense.R;
import me.izen.sense.SenseApplication;
import me.izen.sense.ShareSensorOnTimelineTask;
import me.izen.sense.model.SensorBaseModel;

/**
 * Created by joe on 9/11/14.
 */
public class NodePlusActivity extends BaseSensorActivity implements
        ClimaSensor.ClimaHumidityListener,
        ClimaSensor.ClimaLightListener,
        ClimaSensor.ClimaPressureListener,
        ClimaSensor.ClimaTemperatureListener,
        NodeDevice.SensorDetector,
        NodeDevice.ConnectionListener,
        ProgressUpdateListener {

    private static final String TAG = NodePlusActivity.class.getSimpleName();
    private final Handler mHandler = new Handler() {
        private final DecimalFormat formatter = new DecimalFormat("0.00");

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "BEGIN: handleMessage");

            float value = msg.getData().getFloat(MessageConstants.FLOAT_VALUE_KEY);
            switch (msg.what) {
                case MessageConstants.MESSAGE_CLIMA_HUMIDITY:
                    humidityText.setText(formatter.format(value * 10) + " %RH");
                    break;
                case MessageConstants.MESSAGE_CLIMA_LIGHT:
                    lightText.setText(formatter.format(value) + " LUX");
                    break;

                case MessageConstants.MESSAGE_CLIMA_PRESSURE:
                    pressureText.setText(formatter.format(value / 1000) + " kPa");
                    break;

                case MessageConstants.MESSAGE_CLIMA_TEMPERATURE:
                    temperatureText.setText(formatter.format(value) + " C");
                    break;

            }

        }
    };
    private static BluetoothService mService;
    private BluetoothDevice nodeDevice;
    private String nodeDeviceName;
    private TextView deviceNameTextView;
    private TextView lightText;
    private TextView pressureText;
    private TextView temperatureText;
    private TextView humidityText;
    private ClimaSensor clima;
    private LumaSensor luma;
    private TextView sensorOptions;
    private NodeDevice selectedNODE;
    private Chronometer chrono;
    private Menu sensorMenu;
    private boolean isLumaOn;

    @Override
    protected SensorBaseModel createSensorModel() {
        return null;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(TAG, "BEGIN: onCreate");

        mService = new BluetoothService();
        SenseApplication.setServiceAPI(mService);

        initNodePlusUI();
        nodeDeviceName = this.getIntent().getStringExtra("deviceName");
        deviceNameTextView.setText(nodeDeviceName);

    }

    private void initNodePlusUI() {
        setContentView(R.layout.activity_nodeplus);


        deviceNameTextView = (TextView) findViewById(R.id.device_name);
        sensorOptions = (TextView) findViewById(R.id.tip_tap_for_options);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "BEGIN: onResume");

        if (chrono != null) {
            chrono.start();
        }

        NodeDevice node = ((SenseApplication) getApplication()).getActiveNode();
        if (node != null) {
            //Located the first available clima sensor.
            clima = node.findSensor(NodeEnums.ModuleType.CLIMA);

            luma = node.findSensor(NodeEnums.ModuleType.LUMA);

        }

        //Registering for Events.
        DefaultNotifier.instance().addConnectionListener(this);
        DefaultNotifier.instance().addSensorDetectorListener(this);
    }

    private void enableClima() {
        DefaultNotifier.instance().addClimaHumidityListener(this);
        DefaultNotifier.instance().addClimaLightListener(this);
        DefaultNotifier.instance().addClimaTemperatureListener(this);
        DefaultNotifier.instance().addClimaPressureListener(this);

        //Turn on all streaming
        clima.setStreamMode(true, true, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "BEGIN: onPause");

        if (chrono != null) {
            chrono.stop();
        }

        //Unregister for Events
        DefaultNotifier.instance().removeConnectionListener(this);
        DefaultNotifier.instance().removeSensorDetectorListener(this);

        stopClima();
        stopLuma();

        NodeDevice node = ((SenseApplication) getApplication()).getActiveNode();
        if (isNodeConnected(node)) {
            stopNode(node);
        }

    }

    private void stopLuma() {
        if (luma != null) {
            luma.stopSensor();
            MenuItem item = sensorMenu.findItem(R.id.start_sensor_luma);
            item.setTitle("Start Luma");
            isLumaOn = false;
        }
    }

    private void stopClima() {
        if (clima != null && clima.isStreaming()) {

            MenuItem item = sensorMenu.findItem(R.id.start_sensor_clima);
            item.setTitle("Start Clima");

//            MenuItem climaSareMenuItem = sensorMenu.findItem(R.id.share_sensor_clima);
//            climaSareMenuItem.setVisible(false);

            //Turn off clima sensor
            clima.setStreamMode(false, false, false);

            //Unregister for clima events.
            DefaultNotifier.instance().removeClimaHumidityListener(this);
            DefaultNotifier.instance().removeClimaLightListener(this);
            DefaultNotifier.instance().removeClimaTemperatureListener(this);
            DefaultNotifier.instance().removeClimaPressureListener(this);

            initNodePlusUI();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensor_node, menu);
        this.sensorMenu = menu;
        return true;
    }

    @Override
    protected boolean handleSensorGesture(Gesture gesture) {
        Log.d(TAG, "BEGIN: handleSensorGesture");
        if (gesture == Gesture.TAP) {
            mAudioManager.playSoundEffect(Sounds.TAP);
            openOptionsMenu();
            return true;
        } else {
            return false;
        }
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
            case R.id.start_sensor_node:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        NodeDevice node = ((SenseApplication) getApplication()).getActiveNode();
                        if (isNodeConnected(node)) {
                            stopNode(node);
                        } else {
                            startNode();
                        }
                    }
                });
                return true;

            case R.id.start_sensor_clima:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        NodeDevice node = ((SenseApplication) getApplication()).getActiveNode();
                        if (isNodeConnected(node) && clima != null && clima.isStreaming()) {
                            stopClima();
                        } else {
                            startClima();
                        }
                    }
                });
                return true;

            case R.id.share_sensor_clima:
                shareSensorOnTimeline();
                return true;

            case R.id.start_sensor_luma:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        NodeDevice node = ((SenseApplication) getApplication()).getActiveNode();
                        if (isNodeConnected(node) && luma != null && isLumaOn) {
                            stopLuma();
                        } else {
                            startLuma();
                        }
                    }
                });
                return true;

            case R.id.back_sense:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        backToSense();
                    }
                });
                return true;

            default:
                return false;
        }
    }

    private void shareSensorOnTimeline() {

        new ShareSensorOnTimelineTask().execute("This is a test...");


    }

    private void stopNode(NodeDevice node) {
        node.disableAllStreaming();
        node.disconnect();
        MenuItem item = sensorMenu.findItem(R.id.start_sensor_node);
        item.setTitle("Start Node");
    }

    private void startLuma() {
        if (luma != null) {
            luma.setLumaMode((short) (new Random().nextInt(255 - 0 + 1) + 0));
            MenuItem item = sensorMenu.findItem(R.id.start_sensor_luma);
            item.setTitle("Stop Luma");
            isLumaOn = true;
        }

    }

    private void backToSense() {
        finish();
    }

    private void startClima() {
        if (clima != null) {
            enableClima();

            setContentView(R.layout.clima);

            humidityText = (TextView) findViewById(R.id.txtHumidity);
            lightText = (TextView) findViewById(R.id.txtLight);
            pressureText = (TextView) findViewById(R.id.txtPressure);
            temperatureText = (TextView) findViewById(R.id.txtTemperature);

            chrono = (Chronometer) findViewById(R.id.chronometer);
            chrono.start();

            MenuItem climaMenuItem = sensorMenu.findItem(R.id.start_sensor_clima);
            climaMenuItem.setTitle("Stop Clima");

//            MenuItem climaSareMenuItem = sensorMenu.findItem(R.id.share_sensor_clima);
//            climaSareMenuItem.setVisible(true);


        }
    }

    private void startNode() {
        Log.d(TAG, "BEGIN: startNode");
        final Set<BluetoothDevice> mBondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        for (BluetoothDevice device : mBondedDevices) {
            if (device.getName().equals(nodeDeviceName)) {
                nodeDevice = device;
                break;
            }
        }

        if (nodeDevice != null) {
            Log.d(TAG, "Selected Device Name: " + nodeDevice.getName());
            Log.d(TAG, "Selected Device Address: " + nodeDevice.getAddress());
            BluetoothService mService = SenseApplication.getService();
            NodeDevice node = SenseApplication.getActiveNode();

            if (mService != null) {
                // EC:FE:7E:12:D2:DC
                //One way to connect to a device
                //mService.connect(device.getAddress());

                //Second way, using the NodeDevice implementation
                NodeDevice selectedNODE = AndroidNodeDevice.getOrCreateNodeFromBluetoothDevice(nodeDevice, new DefaultBluetoothDevice(mService));

                //Ensure One Connection At a Time...
                if (node != null && !selectedNODE.equals(node) && node.isConnected()) {
                    node.disconnect();
                }

                //Store the Active DEVICE_NODE in the application space for other fragments to use
                SenseApplication.setActiveNode(selectedNODE);

                //initiate connection
                selectedNODE.connect();

            }
        } else {
            Toast.makeText(this, "Unable to connect...", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onClimaHumidityUpdate(ClimaSensor climaSensor, SensorReading<Float> humidityLevel) {
        Log.d(TAG, "BEGIN: onClimaHumidityUpdate");
        Message m = mHandler.obtainMessage(MessageConstants.MESSAGE_CLIMA_HUMIDITY);
        m.getData().putFloat(MessageConstants.FLOAT_VALUE_KEY, humidityLevel.getValue());
        m.sendToTarget();
    }

    @Override
    public void onClimaLightUpdate(ClimaSensor climaSensor, SensorReading<Float> lightLevel) {
        Log.d(TAG, "BEGIN: onClimaLightUpdate");
        Message m = mHandler.obtainMessage(MessageConstants.MESSAGE_CLIMA_LIGHT);
        m.getData().putFloat(MessageConstants.FLOAT_VALUE_KEY, lightLevel.getValue());
        m.sendToTarget();
    }

    @Override
    public void onClimaPressureUpdate(ClimaSensor climaSensor, SensorReading<Integer> kPa) {
        Log.d(TAG, "BEGIN: onClimaPressureUpdate");
        Message m = mHandler.obtainMessage(MessageConstants.MESSAGE_CLIMA_PRESSURE);
        m.getData().putFloat(MessageConstants.FLOAT_VALUE_KEY, kPa.getValue());
        m.sendToTarget();
    }

    @Override
    public void onClimaTemperatureUpdate(ClimaSensor climaSensor, SensorReading<Float> temperature) {
        Log.d(TAG, "BEGIN: onClimaTemperatureUpdate");
        Message m = mHandler.obtainMessage(MessageConstants.MESSAGE_CLIMA_TEMPERATURE);
        m.getData().putFloat(MessageConstants.FLOAT_VALUE_KEY, temperature.getValue());
        m.sendToTarget();
    }

    @Override
    public void onCommunicationInitCompleted(NodeDevice node) {
        Toast.makeText(this, node.getName() + " is now ready for use.", Toast.LENGTH_SHORT).show();
        if (!isNodeConnected(node)) {
            Toast.makeText(this, "No Connection Available", Toast.LENGTH_SHORT).show();
            return;
        }

        MenuItem item = null;
        if (sensorMenu != null) {
            item = sensorMenu.findItem(R.id.start_sensor_node);
            item.setTitle("Stop Node");
        }

        if (checkForSensor(node, NodeEnums.ModuleType.CLIMA, true)) {
            Log.d(TAG, "connected to DEVICE_NODE:" + nodeDevice.getName());


            //Located the first available clima sensor.
            clima = node.findSensor(NodeEnums.ModuleType.CLIMA);

            item = sensorMenu.findItem(R.id.start_sensor_clima);
            item.setTitle("Start Clima");
            item.setVisible(true);

        }

        if (checkForSensor(node, NodeEnums.ModuleType.LUMA, true)) {
            Log.d(TAG, "connected to DEVICE_NODE:" + nodeDevice.getName());

            //Located the first available clima sensor.
            luma = node.findSensor(NodeEnums.ModuleType.LUMA);

            item = sensorMenu.findItem(R.id.start_sensor_luma);
            item.setTitle("Start Luma");
            item.setVisible(true);

        }

    }

    private boolean checkForSensor(NodeDevice node, NodeEnums.ModuleType type, boolean displayIfNotFound) {
        BaseSensor sensor = node.findSensor(type);
        if (sensor == null && displayIfNotFound) {
            Toast.makeText(NodePlusActivity.this, type.toString() + " not found on " + node.getName(), Toast.LENGTH_SHORT).show();
        }

        return sensor != null;
    }

    private boolean isNodeConnected(NodeDevice node) {
        return node != null && node.isConnected();
    }

    @Override
    public void onConnected(NodeDevice node) {
        Toast.makeText(this, node.getName() + " is connected.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect(NodeDevice node) {
        Toast.makeText(this, node.getName() + " disconnected.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(NodeDevice node, Exception e) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNodeDiscovered(NodeDevice node) {

    }

    @Override
    public void nodeDeviceFailedToInit(NodeDevice node) {
        Toast.makeText(this, "Failed to Initialize DEVICE_NODE...Disconnecting Now", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnecting(NodeDevice node) {
        Toast.makeText(this, node.getName() + " is connecting.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgressUpdated(String s) {
        Log.d(TAG, "BEGIN: onProgressUpdated: " + s);
    }

    @Override
    public void onTaskFinished(boolean isSuccessful) {
        if (isSuccessful) {
            Toast.makeText(this, "Chroma is ready to use", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Chroma failed to find suitable internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorConnected(NodeDevice nodeDevice, BaseSensor baseSensor) {
        Log.d(TAG, "Sensor Found: " + baseSensor.getModuleType() + " SubType: " + baseSensor.getSubtype() + " Serial: " + baseSensor.getSerialNumber());
        Toast.makeText(NodePlusActivity.this, baseSensor.getModuleType() + " has been detected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorDisconnected(NodeDevice nodeDevice, BaseSensor baseSensor) {
        Log.d(TAG, "Sensor Found: " + baseSensor.getModuleType() + " SubType: " + baseSensor.getSubtype() + " Serial: " + baseSensor.getSerialNumber());
        Toast.makeText(NodePlusActivity.this, baseSensor.getModuleType() + " has been removed", Toast.LENGTH_SHORT).show();
    }
}
