package me.izen.sense.sensors;

/**
 * Created by joe on 9/15/14.
 */
public abstract class MessageConstants {

    public static final int MESSAGE_ACCELEROMETER_READING = 0;
    public static final int MESSAGE_GYROSCOPE_READING = 1;
    public static final int MESSAGE_MAGNETOMETER_READING = 2;

    public static final int MESSAGE_CLIMA_HUMIDITY = 3;
    public static final int MESSAGE_CLIMA_LIGHT = 4;
    public static final int MESSAGE_CLIMA_TEMPERATURE = 5;
    public static final int MESSAGE_CLIMA_PRESSURE = 6;

    public static final int MESSAGE_THERMA_TEMPERATURE = 7;
    public static final int MESSAGE_CHANGE_IR_THERMA   = 8;
    public static final int MESSAGE_EMISSIVITY_NUMBER_UPDATE = 10;
    public static final int MESSAGE_OXA_READING = 9;
    public static final int MESSAGE_OXA_BASELINE_A = 10;

    public static final String FLOAT_VALUE_KEY = "com.variable.api.demo.FLOAT_READING_KEY";

    public static final String X_VALUE_KEY = "com.variable.api.demo.X_READING_VALUE_KEY";
    public static final String Y_VALUE_KEY = "com.variable.api.demo.Y_READING_VALUE_KEY";
    public static final String Z_VALUE_KEY = "com.variable.api.demo.Z_READING_VALUE_KEY";


    public static final String TIME_STAMP  = "com.variable.api.demo.TIME_STAMP_KEY";
    public static final String TIME_SOURCE = "com.variable.api.demo.TIME_SOURCE";

    public static final int MESSAGE_INIT_NODE_PROGRESS = 11;
}
