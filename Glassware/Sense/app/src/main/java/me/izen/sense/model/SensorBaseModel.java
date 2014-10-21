package me.izen.sense.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by joe on 9/11/14.
 */
public class SensorBaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] sensorNames;

    public SensorBaseModel(List<String> sensorNames) {
        this.sensorNames = sensorNames.toArray(new String[sensorNames.size()]);
    }



}
