package me.izen.sense;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joe on 9/24/14.
 */
public class ShareSensorOnTimelineTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = ShareSensorOnTimelineTask.class.getSimpleName();

    @Override
    protected Void doInBackground(String... params) {


        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("https://sensemirror.appspot.com/main");

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("operation", "insertItem"));
            nameValuePairs.add(new BasicNameValuePair("message", params[0]));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

            if (response.getStatusLine().getStatusCode() == 200) {
                Log.d(TAG, "Sensor data shared on timeline");
            } else {
                Log.d(TAG, "Share failed");
            }

        } catch (ClientProtocolException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return null;
    }
}
