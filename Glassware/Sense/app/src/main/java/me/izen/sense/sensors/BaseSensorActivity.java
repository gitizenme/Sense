package me.izen.sense.sensors;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import me.izen.sense.R;
import me.izen.sense.model.SensorBaseModel;

/**
 * Created by joe on 9/11/14.
 */
public abstract class BaseSensorActivity extends Activity {
    /**
     * The intent extra that holds the instance of {@link me.izen.sense.model.SensorBaseModel} to display in the
     * results.
     */
    public static final String EXTRA_MODEL = "model";
    private static final String TAG = BaseSensorActivity.class.getSimpleName();
    /**
     * Listener for tap and swipe gestures during the game.
     */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            Log.d(TAG, "BEGIN: handleSensorGesture");
            if (areGesturesEnabled()) {
                return handleSensorGesture(gesture);
            }
            return false;
        }
    };
    /**
     * Handler used to post a delayed animation
     */
    protected final Handler mHandler = new Handler();
    /**
     * Audio manager used to play system sound effects.
     */
    protected AudioManager mAudioManager;
    private boolean mGesturesEnabled = true;
    /**
     * Model that stores the state of the sensor.
     */
    private SensorBaseModel mModel;
    /**
     * Gesture detector used to present the options menu.
     */
    private GestureDetector mGestureDetector;

    /**
     * Subclasses must override this method to create and return the data model that will be used
     * by the game.
     */
    protected abstract SensorBaseModel createSensorModel();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(TAG, "BEGIN: onCreate");
        setContentView(R.layout.activity_app_activity);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);
        mModel = createSensorModel();

    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector.onMotionEvent(event);
    }


    /**
     * Returns true if gestures should be processed or false if they should be ignored.
     */
    private boolean areGesturesEnabled() {
        return mGesturesEnabled;
    }

    /**
     * Returns the data model used by this instance of the game.
     */
    protected SensorBaseModel getSensorModel() {
        return mModel;
    }


    /**
     * Subclasses must override this method to handle {@link Gesture#TAP} and
     * {@link Gesture#SWIPE_RIGHT} gestures that occur during game play. T
     */
    protected abstract boolean handleSensorGesture(Gesture gesture);

}
