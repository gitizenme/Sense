package me.izen.sense;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import com.sensorcon.sensordrone.android.Drone;
import com.variable.framework.android.bluetooth.BluetoothService;
import com.variable.framework.node.NodeDevice;

import java.util.ArrayList;


public class SenseApplication extends Application {

    public Drone myDrone;

    public static String token;


    // Set some streaming rates, so we can switch back to a default rate when
    // coming back from graphing.
    public int defaultRate = 1000;
    public int streamingRate;

    @Override
    public void onCreate() {
        super.onCreate();

        myDrone = new Drone();
        streamingRate = defaultRate;
    }

    private static BluetoothService mBluetoothService;
    public  final ArrayList<BluetoothDevice> mDiscoveredDevices = new ArrayList<BluetoothDevice>();
    public static NodeDevice mActiveNode;

    public static final BluetoothService getService(){
        return mBluetoothService;
    }

    public static final BluetoothService setServiceAPI(BluetoothService api){
        mBluetoothService = api;
        return mBluetoothService;
    }

    public static void setActiveNode(NodeDevice node){ mActiveNode = node; }

    public static NodeDevice getActiveNode(){  return mActiveNode; }
}
