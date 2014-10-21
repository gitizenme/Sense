package me.izen.sense.sensors;

import android.content.Intent;

import me.izen.sense.MainActivity;
import me.izen.sense.R;
import me.izen.sense.ble.OpenEEGService;

import java.io.UnsupportedEncodingException;
import java.text.*;
import java.util.*;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.*;

import com.google.android.glass.touchpad.Gesture;

import me.izen.sense.model.SensorBaseModel;

/**
 * Created by joe on 10/19/14.
 */
public class OpenEEGActivity  extends BaseSensorActivity {
    private final static String TAG = OpenEEGActivity.class.getSimpleName();

    private String mDeviceName;
    private String mDeviceAddress;
    private OpenEEGService mBluetoothLeService;

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private long[] dataValues;
    private XYPlot openEEGPlot = null;
    private SimpleXYSeries openEEGSeries = null;
    private SimpleXYSeries openEKGSeries = null;
    private SimpleXYSeries openBIOFSeries = null;

    private static final int HISTORY_SIZE = 30;

    boolean startDelimFound = false;
    private String dataBuffer = "";
    private String rawSensorData = "";


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(OpenEEGService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(OpenEEGService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(OpenEEGService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(OpenEEGService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        BluetoothGattCharacteristic characteristic = gattService
                .getCharacteristic(OpenEEGService.UUID_BLE_SHIELD_TX);
        map.put(characteristic.getUuid(), characteristic);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(OpenEEGService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((OpenEEGService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (OpenEEGService.ACTION_GATT_DISCONNECTED.equals(action)) {
            } else if (OpenEEGService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (OpenEEGService.ACTION_DATA_AVAILABLE.equals(action)) {
                try {
                    displayData(intent.getByteArrayExtra(OpenEEGService.EXTRA_DATA));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();

    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();


    public static char[] bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return hexChars;
    }


    private boolean processRawData(String rawSensorData, long[] dataValues) {

        int index = 0;
        byte[] rawBytes = rawSensorData.getBytes();

        Log.d(TAG, "rawBytes = " + bytesToHex(rawBytes));

        for (int i = 0; i < rawBytes.length; i++) {
            char c = (char) rawBytes[i];
            switch (index) {
                case 0:
                    dataValues[0] = 256 * 256 * c;
                    index++;
                    break;
                case 1:
                    dataValues[0] += 256 * c;
                    index++;
                    break;
                case 2:
                    dataValues[0] += c;
                    index++;
                    dataValues[0] = dataValues[0] - 8388608;
                    break;
                case 3:
                    index++;
                    break;                             ///< don't check ADC Ch1 status
                case 4:
                    dataValues[1] = 256 * 256 * c;
                    index++;
                    break;
                case 5:
                    dataValues[1] += 256 * c;
                    index++;
                    break;
                case 6:
                    dataValues[1] += c;
                    dataValues[1] = dataValues[1] - 8388608;
                    index++;
                    break;
                case 7:
                    index++;
                    break;                             ///< don't check ADC Ch2 status
                case 8:
                    dataValues[2] = c;                             ///< get stimulus value
                    index++;
                    break;
                default:
                    break;
            }
        }
        return true;
    }


    public static class KPM {
        /**
         * Search the data byte array for the first occurrence
         * of the byte array pattern.
         */
        public static int indexOf(byte[] data, byte[] pattern) {
            int[] failure = computeFailure(pattern);

            int j = 0;

            for (int i = 0; i < data.length; i++) {
                while (j > 0 && pattern[j] != data[i]) {
                    j = failure[j - 1];
                }
                if (pattern[j] == data[i]) {
                    j++;
                }
                if (j == pattern.length) {
                    return i - pattern.length + 1;
                }
            }
            return -1;
        }

        /**
         * Computes the failure function using a boot-strapping process,
         * where the pattern is matched against itself.
         */
        private static int[] computeFailure(byte[] pattern) {
            int[] failure = new int[pattern.length];

            int j = 0;
            for (int i = 1; i < pattern.length; i++) {
                while (j > 0 && pattern[j] != pattern[i]) {
                    j = failure[j - 1];
                }
                if (pattern[j] == pattern[i]) {
                    j++;
                }
                failure[i] = j;
            }

            return failure;
        }
    }

    private void displayData(byte[] byteArray) throws UnsupportedEncodingException {
        if (byteArray != null) {


            Log.d(TAG, "byteArray.length = " + byteArray.length);
            Log.d(TAG, "byteArray = " + Arrays.toString(byteArray));

            char[] charArray = new String(byteArray, "UTF-8").toCharArray();

            int indexOfA5 = KPM.indexOf(byteArray, new byte[]{(byte) 0xA5});
            int indexOf5A = KPM.indexOf(byteArray, new byte[]{(byte) 0x5A});

            Log.d(TAG, "indexOfA5 = " + indexOfA5);
            Log.d(TAG, "indexOf5A = " + indexOf5A);

            if (indexOfA5 == 0 && indexOf5A == 1 && charArray.length >= 16) {
                rawSensorData = String.copyValueOf(charArray, indexOfA5 + 3, 16);
                Log.d(TAG, "rawSensorData = " + rawSensorData.getBytes());
                dataValues = new long[3];
                processRawData(rawSensorData, dataValues);
                Log.i(TAG, "values = " +  Arrays.toString(dataValues));
                updatePlot(dataValues);

                if (charArray.length > 16 && charArray[17] == 0xA5 && charArray[18] == 0x5A) {
                    dataBuffer = String.copyValueOf(charArray, 17, charArray.length);
                    startDelimFound = true;
                }
            } else if (indexOfA5 > -1 && indexOf5A > indexOfA5 && !startDelimFound) {
                dataBuffer += String.copyValueOf(charArray, indexOfA5, charArray.length - indexOfA5);
                startDelimFound = true;
            } else if (startDelimFound && indexOfA5 > -1) {
                dataBuffer += String.copyValueOf(charArray, 0, indexOfA5);
                startDelimFound = false;

                if (dataBuffer.length() == 17) {
                    rawSensorData = dataBuffer.substring(4, 16);
                    Log.d(TAG, "rawSensorData = " + rawSensorData.getBytes());
                    dataValues = new long[3];
                    processRawData(rawSensorData, dataValues);
                    Log.i(TAG, "values = " + Arrays.toString(dataValues));
                }
                dataBuffer = "";

                if (charArray.length > 16 && indexOf5A > indexOfA5) {
                    dataBuffer = String.copyValueOf(charArray, indexOfA5, charArray.length - indexOfA5);
                    startDelimFound = true;
                }

            }

            Log.d(TAG, "dataBuffer.length = " + dataBuffer.length());
            Log.d(TAG, "dataBuffer = " + dataBuffer);



/*


A5 5A 02 01 02 00 01 D7 01 BF 01 A7 01 D4 01 F0 01


40, -91, 90, 2, 1, 2, 0, 1, -41, 1, -65, 1, -89, 1, -44, 1, -16, 1, 41
28A55A0201020001D701BF01A701D401F00129

40, -91, 90, 2, 2, 2, 40, -91, 90, 2, 122, 1, -5, 2, 0, 1, -1, 1, -5, 1, -15, 1, -21, 1, 41
28A55A02020228A55A02A01FB020001FF01FB01F101EB0129


40, -91, 90, 2, 1, 2, 0, 1, -41, 1, -65, 1, -89, 1, -44, 1, -16, 1, 41
28A55A0201020001D701BF01A701D401F00129

40, -91, 90, 2, 2, 2, 40, -91, 90, 2, -85, 1, -4, 1, 2, 2, 0, 1, -5, 1, -15, 1, -21, 1, 41
28A55A02020228A55A02AB01FC0102020001FB01F101EB0129
*/
        }
    }

    private void updatePlot(long[] dataValues) {
        // get rid the oldest sample in history:
        if (openEEGSeries.size() > HISTORY_SIZE) {
            openEEGSeries.removeFirst();
            openEKGSeries.removeFirst();
            openBIOFSeries.removeFirst();
        }

        // add the latest history sample:
        openEEGSeries.addLast(null, dataValues[0]);
        openEKGSeries.addLast(null, dataValues[1]);
        openBIOFSeries.addLast(null, dataValues[2]);

        openEEGPlot.redraw();
    }


    @Override
    protected SensorBaseModel createSensorModel() {
        return null;
    }

    private void initPlot() {

        Number[] intervals = {
                0,
                30,
                60,
                90,
                120
        };

        openEEGSeries = new SimpleXYSeries("EEG");
        openEEGSeries.useImplicitXVals();
        openEKGSeries = new SimpleXYSeries("EKG");
        openEKGSeries.useImplicitXVals();
        openBIOFSeries = new SimpleXYSeries("BIO");
        openBIOFSeries.useImplicitXVals();

//        openEEGPlot.setRangeBoundaries(-1000000, 1000000, BoundaryMode.FIXED);
//        openEEGPlot.setDomainBoundaries(0, 30000, BoundaryMode.FIXED);

        openEEGPlot.setDomainStep(XYStepMode.SUBDIVIDE, intervals.length);

        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);
        openEEGPlot.addSeries(openEEGSeries, series1Format);

        LineAndPointFormatter series2Format = new LineAndPointFormatter();
        series2Format.setPointLabelFormatter(new PointLabelFormatter());
        series2Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf2);
        openEEGPlot.addSeries(openEKGSeries, series2Format);

        LineAndPointFormatter series3Format = new LineAndPointFormatter();
        series3Format.setPointLabelFormatter(new PointLabelFormatter());
        series3Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf3);
        openEEGPlot.addSeries(openBIOFSeries, series3Format);


        openEEGPlot.setRangeValueFormat(new DecimalFormat("0"));
        openEEGPlot.setDomainValueFormat(new DecimalFormat("0"));
        openEEGPlot.setDomainStepValue(5);
        openEEGPlot.setTicksPerRangeLabel(3);
        openEEGPlot.setDomainLabel("seconds");
        openEEGPlot.getDomainLabelWidget().pack();
        openEEGPlot.setRangeLabel("readout");
        openEEGPlot.getRangeLabelWidget().pack();

        final PlotStatistics histStats = new PlotStatistics(1000, false);
        openEEGPlot.addListener(histStats);

    }


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(TAG, "BEGIN: onCreate");


        setContentView(R.layout.plot_view);

        openEEGPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        initPlot();

        Intent intent = getIntent();

        mDeviceAddress = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        mDeviceName = intent.getStringExtra(MainActivity.EXTRA_DEVICE_NAME);

        Intent gattServiceIntent = new Intent(this, OpenEEGService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected boolean handleSensorGesture(Gesture gesture) {
        return false;
    }


}
