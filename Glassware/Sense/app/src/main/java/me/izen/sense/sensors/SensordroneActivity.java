package me.izen.sense.sensors;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.sensorcon.sensordrone.DroneEventListener;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.DroneStatusListener;
import com.sensorcon.sensordrone.android.tools.DroneConnectionHelper;
import com.sensorcon.sensordrone.android.tools.DroneQSStreamer;
import com.sensorcon.sensordrone.android.tools.DroneStreamer;

import me.izen.sense.R;
import me.izen.sense.SenseApplication;
import me.izen.sense.model.SensorBaseModel;

/**
 * Created by joe on 9/11/14.
 */
public class SensordroneActivity extends BaseSensorActivity {
    private static final String TAG = SensordroneActivity.class.getSimpleName();
    // Text to display
    private static final String[] SENSOR_NAMES = {"Temperature (Ambient)",
            "Humidity", "Pressure", "Object Temperature (IR)",
            "Illuminance (calculated)", "Precision Gas (CO equivalent)",
            "Proximity Capacitance", "External Voltage (0-3V)",
            "Altitude (calculated)"};
    /*
     * We will use some stuff from our Sensordrone Helper library
     */
    public DroneConnectionHelper myHelper = new DroneConnectionHelper();
    private SenseApplication droneApp;
    // A ConnectionBLinker from the SDHelper Library
    private DroneStreamer myBlinker;
    // Toggle our LED
    private boolean ledToggle = true;
    // Our Listeners
    private DroneEventListener deListener;
    private DroneStatusListener dsListener;
    private String deviceName;
    private TextView lightText;
    private TextView pressureText;
    private TextView humidityText;
    private TextView temperatureText;
    private TextView deviceNameTextView;
    private Chronometer chrono;
    // An int[] that will hold the QS_TYPEs for our sensors of interest
    private int[] qsSensors;
    // Figure out how many sensors we have based on the length of our labels
    private int numberOfSensors = SENSOR_NAMES.length;

    // Another object from the SDHelper library. It helps us set up our
    // pseudo streaming
    private DroneQSStreamer[] streamerArray = new DroneQSStreamer[numberOfSensors];


    @Override
    protected SensorBaseModel createSensorModel() {
        return null;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(TAG, "BEGIN: onCreate");

//        View root = this.getLayoutInflater().inflate(R.layout.sensordrone, null, false);
//
//        temperatureText = (TextView) root.findViewById(R.id.txtTemperature);

        setContentView(R.layout.activity_sensordrone);

        deviceName = this.getIntent().getStringExtra("deviceName");
        deviceNameTextView = (TextView) findViewById(R.id.device_name);
        deviceNameTextView.setText(deviceName);

        // Get out Application so we have access to our Drone
        droneApp = (SenseApplication) getApplication();

        qsSensors = new int[]{droneApp.myDrone.QS_TYPE_TEMPERATURE,
                droneApp.myDrone.QS_TYPE_HUMIDITY,
                droneApp.myDrone.QS_TYPE_PRESSURE,
                droneApp.myDrone.QS_TYPE_IR_TEMPERATURE,
                droneApp.myDrone.QS_TYPE_RGBC,
                droneApp.myDrone.QS_TYPE_PRECISION_GAS,
                droneApp.myDrone.QS_TYPE_CAPACITANCE,
                droneApp.myDrone.QS_TYPE_ADC, droneApp.myDrone.QS_TYPE_ALTITUDE};

        for (int i = 0; i < numberOfSensors; i++) {

            // The clickListener will need a final type of i
            final int counter = i;

            streamerArray[i] = new DroneQSStreamer(droneApp.myDrone, qsSensors[i]);
        }


        // This will Blink our Drone, once a second, Blue
        myBlinker = new DroneStreamer(droneApp.myDrone, 1000) {
            @Override
            public void repeatableTask() {
                if (ledToggle) {
                    droneApp.myDrone.setLEDs(0, 0, 126);
                } else {
                    droneApp.myDrone.setLEDs(0, 0, 0);
                }
                ledToggle = !ledToggle;
            }
        };

		/*
         * Let's set up our Drone Event Listener.
		 *
		 * See adcMeasured for the general flow for when a sensor is measured.
		 */
        deListener = new DroneEventListener() {
            @Override
            public void connectEvent(DroneEventObject arg0) {

                quickMessage(deviceName + " Connected!");

                // Turn on our blinker
                myBlinker.start();

                // Enable our steamer
                streamerArray[0].enable();
                streamerArray[1].enable();
                streamerArray[2].enable();
                streamerArray[4].enable();
                droneApp.myDrone.quickEnable(qsSensors[0]);
                droneApp.myDrone.quickEnable(qsSensors[1]);
                droneApp.myDrone.quickEnable(qsSensors[2]);
                droneApp.myDrone.quickEnable(qsSensors[4]);

            }

            @Override
            public void connectionLostEvent(DroneEventObject arg0) {

                // Things to do if we think the connection has been lost.

                // Turn off the blinker
                myBlinker.stop();

                // notify the user
                quickMessage(deviceName + " Connection lost! Trying to re-connect!");

                // Try to reconnect once, automatically
                if (droneApp.myDrone.btConnect(droneApp.myDrone.lastMAC)) {
                    // A brief pause
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connectionLostReconnect();
                } else {
                    quickMessage("Re-connect failed");
                    doOnDisconnect();
                }
            }

            @Override
            public void unknown(DroneEventObject droneEventObject) {

            }

            @Override
            public void capacitanceMeasured(DroneEventObject droneEventObject) {
            }

            @Override
            public void adcMeasured(DroneEventObject droneEventObject) {

            }

            @Override
            public void rgbcMeasured(DroneEventObject droneEventObject) {
                Log.d(TAG, "BEGIN: rgbcMeasured");

                String msg = "";
                if (droneApp.myDrone.rgbcLux >= 0) {
                    msg = String.format("%.0f", droneApp.myDrone.rgbcLux)
                            + " Lux";
                } else {
                    msg = String.format("%.0f", 0.0) + " Lux";
                }
                tvUpdate(lightText, msg);
                streamerArray[4].streamHandler.postDelayed(streamerArray[4],
                        droneApp.streamingRate);

            }

            @Override
            public void pressureMeasured(DroneEventObject droneEventObject) {
                Log.d(TAG, "BEGIN: pressureMeasured");

                tvUpdate(
                        pressureText,
                        String.format("%.2f",
                                droneApp.myDrone.pressure_Pascals / 1000)
                                + " kPa");
                streamerArray[2].streamHandler.postDelayed(streamerArray[2],
                        droneApp.streamingRate);

            }

            @Override
            public void altitudeMeasured(DroneEventObject droneEventObject) {

            }

            @Override
            public void irTemperatureMeasured(DroneEventObject droneEventObject) {

            }

            @Override
            public void humidityMeasured(DroneEventObject droneEventObject) {
                Log.d(TAG, "BEGIN: humidityMeasured");

                tvUpdate(
                        humidityText,
                        String.format("%.1f", droneApp.myDrone.humidity_Percent)
                                + " %RH");
                streamerArray[1].streamHandler.postDelayed(streamerArray[1],
                        droneApp.streamingRate);
            }

            @Override
            public void temperatureMeasured(DroneEventObject droneEventObject) {
                Log.d(TAG, "BEGIN: temperatureMeasured");

                tvUpdate(
                        temperatureText,
                        String.format("%.1f C",
                                droneApp.myDrone.temperature_Celsius)
                );
                streamerArray[0].streamHandler.postDelayed(streamerArray[0],
                        droneApp.streamingRate);
            }

            @Override
            public void reducingGasMeasured(DroneEventObject droneEventObject) {

            }

            @Override
            public void oxidizingGasMeasured(DroneEventObject droneEventObject) {

            }

            @Override
            public void precisionGasMeasured(DroneEventObject droneEventObject) {

            }

            @Override
            public void uartRead(DroneEventObject droneEventObject) {

            }

            @Override
            public void i2cRead(DroneEventObject droneEventObject) {

            }

            @Override
            public void usbUartRead(DroneEventObject droneEventObject) {

            }

            @Override
            public void customEvent(DroneEventObject arg0) {

            }

            @Override
            public void disconnectEvent(DroneEventObject arg0) {
                quickMessage(deviceName + " Disconnected!");
            }

        };


        		/*
         * Set up our status listener
		 *
		 * see adcStatus for the general flow for sensors.
		 */
        dsListener = new DroneStatusListener() {

            @Override
            public void adcStatus(DroneEventObject arg0) {
                // This is triggered when the status of the external ADC has
                // been
                // enable, disabled, or checked.

                // If status has been triggered to true (on)
                if (droneApp.myDrone.adcStatus) {
                    // then start the streaming by taking the first
                    // measurement
                    streamerArray[7].run();
                }
                // Don't do anything if false (off)
            }

            @Override
            public void altitudeStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.altitudeStatus) {
                    streamerArray[8].run();
                }

            }

            @Override
            public void batteryVoltageStatus(DroneEventObject arg0) {
                // This is triggered when the battery voltage has been
                // measured.
/*
                String bVoltage = String.format("%.2f",
                        droneApp.myDrone.batteryVoltage_Volts) + " V";
                tvUpdate(bvValue, bVoltage);
                // We might need to update the rate due to graphing
                bvStreamer.setRate(droneApp.streamingRate);
*/
            }

            @Override
            public void capacitanceStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.capacitanceStatus) {
                    streamerArray[6].run();
                }
            }

            @Override
            public void chargingStatus(DroneEventObject arg0) {

            }

            @Override
            public void customStatus(DroneEventObject arg0) {

            }

            @Override
            public void humidityStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.humidityStatus) {
                    streamerArray[1].run();
                }

            }

            @Override
            public void irStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.irTemperatureStatus) {
                    streamerArray[3].run();
                }

            }

            @Override
            public void lowBatteryStatus(DroneEventObject arg0) {
                // If we get a low battery, notify the user
                // and disconnect

                // This might trigger a lot (making a call the the LEDS will
                // trigger it,
                // so the myBlinker will trigger this once a second.
                // calling myBlinker.disable() even sets LEDS off, which
                // will trigger it...

                // We wil also add in a voltage check, to allow users to use their
                // Sensordrone a little more
/*                if (lowbatNotify && droneApp.myDrone.batteryVoltage_Volts < 3.1) {
                    lowbatNotify = false; // Set true again in connectEvent
                    myBlinker.stop();
                    doOnDisconnect(); // run our disconnect routine
                    // Notify the user
                    tvUpdate(tvConnectionStatus, "Low Battery: Disconnected!");
                    AlertInfo.lowBattery(SensordroneControl.this);
                }*/

            }

            @Override
            public void oxidizingGasStatus(DroneEventObject arg0) {

            }

            @Override
            public void precisionGasStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.precisionGasStatus) {
                    streamerArray[5].run();
                }

            }

            @Override
            public void pressureStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.pressureStatus) {
                    streamerArray[2].run();
                }

            }

            @Override
            public void reducingGasStatus(DroneEventObject arg0) {

            }

            @Override
            public void rgbcStatus(DroneEventObject arg0) {
                if (droneApp.myDrone.rgbcStatus) {
                    streamerArray[4].run();
                }

            }

            @Override
            public void temperatureStatus(DroneEventObject arg0) {
                Log.d(TAG, "BEGIN: temperatureStatus");
                if (droneApp.myDrone.temperatureStatus) {
                    streamerArray[0].run();
                }

            }

            @Override
            public void unknownStatus(DroneEventObject arg0) {

            }
        };

        droneApp.myDrone.registerDroneListener(deListener);
        droneApp.myDrone.registerDroneListener(dsListener);


    }

    /*
     * A function to update a TextView
     *
     * We have it run on the UI thread to make sure it safely updates.
     */
    public void tvUpdate(final TextView tv, final String msg) {
        Log.d(TAG, "BEGIN: tvUpdate");

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "BEGIN: tvUpdate.run");
                tv.setText(msg);
            }
        });
    }

    public void doOnDisconnect() {
        // Shut off any sensors that are on
        SensordroneActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                // Turn off myBlinker
                myBlinker.stop();

                // Make sure the LEDs go off
                if (droneApp.myDrone.isConnected) {
                    droneApp.myDrone.setLEDs(0, 0, 0);
                }


                // Only try and disconnect if already connected
                if (droneApp.myDrone.isConnected) {
                    droneApp.myDrone.disconnect();
                }

            }
        });

    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "BEGIN: onDestroy");
        super.onDestroy();

        if (isFinishing()) {
            // Try and nicely shut down
            doOnDisconnect();

            droneApp.myDrone.unregisterDroneListener(deListener);
            droneApp.myDrone.unregisterDroneListener(dsListener);
        }
    }

    public void connectionLostReconnect() {
        // Re-Toggle and sensors that were on
        SensordroneActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
            }
        });
    }

    /*
     * A function to display Toast Messages.
     *
     * By having it run on the UI thread, we will be sure that the message is
     * displays no matter what thread tries to use it.
     */
    public void quickMessage(final String msg) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
                        .show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensor_sensordrone, menu);
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
            case R.id.start_sensor_sensordrone:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startDataCollection();
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

    private void backToSense() {

        doOnDisconnect();

        droneApp.myDrone.unregisterDroneListener(deListener);
        droneApp.myDrone.unregisterDroneListener(dsListener);

        // Stop taking measurements
        streamerArray[0].disable();
        streamerArray[1].disable();
        streamerArray[2].disable();
        streamerArray[4].disable();

        // Disable the sensor
        droneApp.myDrone.quickDisable(qsSensors[0]);
        droneApp.myDrone.quickDisable(qsSensors[1]);
        droneApp.myDrone.quickDisable(qsSensors[2]);
        droneApp.myDrone.quickDisable(qsSensors[4]);

        if (chrono != null) {
            chrono.stop();
        }

        finish();
    }

    private void startDataCollection() {
        if (!droneApp.myDrone.isConnected) {
            myHelper.connectFromPairedDevices(droneApp.myDrone, SensordroneActivity.this);

            setContentView(R.layout.sensordrone);

            temperatureText = (TextView) findViewById(R.id.txtTemperature);
            humidityText = (TextView) findViewById(R.id.txtHumidity);
            lightText = (TextView) findViewById(R.id.txtLight);
            pressureText = (TextView) findViewById(R.id.txtPressure);

            chrono = (Chronometer) findViewById(R.id.chronometer);
            chrono.start();

        } else {
            quickMessage("Already connected to: " + deviceName);
        }
    }
}
