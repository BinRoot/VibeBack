package com.nomenubar.GoodVibes;

import android.content.*;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class SettingsContentObserver extends ContentObserver {

    private final String TAG = "SettingsContentObserver";

    int mPreviousVolume;
    Context mContext;
    SharedPreferences mSettings;
    HashMap<Integer, MessageType> mVolButtonMap;
    boolean boundaryVolHit = false;
    Vibrator mVibrator;
    LocationManager mLocationManager;
    MainActivity mMainActivity;

    public SettingsContentObserver(
            Context c,
            Handler handler,
            HashMap<Integer, MessageType> volButtonMap,
            Vibrator vibrator,
            LocationManager locationManager,
            MainActivity mainActivity) {
        super(handler);
        mContext = c;
        mVolButtonMap = volButtonMap;
        mVibrator = vibrator;
        mMainActivity = mainActivity;
        mLocationManager = locationManager;
        mSettings = c.getSharedPreferences("GoodVibes", 0);
        mSettings.edit().putString("seq", "").apply();
        mSettings.edit().putInt("state", 0).apply();

        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPreviousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

    }

    @Override
    public boolean deliverSelfNotifications() {
         return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        int delta = mPreviousVolume - currentVolume;

        if (delta != 0 && Math.abs(delta) < 5) {
            String seq = mSettings.getString("seq", "");
            if(delta>0) {
                getVolumeChange(delta, currentVolume, seq, "D");
            }
            else if(delta<0) {
                getVolumeChange(delta, currentVolume, seq, "U");
            }
        } else if(Math.abs(delta) >= 5) {
            mPreviousVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2;
        }

        if (currentVolume == 0 || currentVolume == audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
            // reset volume to mid level
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2, 0);
        }

    }

    Handler mHandler = new Handler();

    private void getVolumeChange(int delta, int currentVolume, String seq, String direction) {
        //Log.d(TAG, direction+" "+delta);

        mPreviousVolume = currentVolume;

        for(int i=0; i<Math.abs(delta); i++) {
            seq += direction;
        }
        mSettings.edit().putString("seq", seq).apply();

        Log.d(TAG, "seq: " + seq);

        int state = mSettings.getInt("state", 0);

        // Log.d(TAG, "in state "+state);

        if (state == 0) {
            if (seq.startsWith("UU")) {
                mSettings.edit().putInt("state", 1).apply();
                mSettings.edit().putString("seq", "").apply();
                long [] pattern = {0, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                        20, 20, 20, 20, 20, 20, 20, 20, 20};
                mVibrator.vibrate(pattern, -1);
            }
            else if (seq.startsWith("UD")) {
                mSettings.edit().putString("seq", "").apply();
            }
            else if (seq.startsWith("D")) {
                mSettings.edit().putString("seq", "").apply();
            }
        }
        else if (state == 1) {
            if (seq.endsWith("D")) {
                mVibrator.vibrate(getVibePattern(Math.abs(delta)), -1);
            }
            if (seq.matches("D+U+$")) {
                long [] pattern = {0, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
                        20, 20, 20, 20, 20, 20, 20, 20, 20};
                mVibrator.vibrate(pattern, -1);

                Log.d(TAG, "in DU");

                int downCount = 0;
                String uSeq = "";
                for (int i=0; i<seq.length(); i++) {
                    if (seq.charAt(i) == 'D') {
                        downCount++;
                    }
                    else if (seq.charAt(i) == 'U') {
                        uSeq += "U";
                    }
                }
                mSettings.edit().putInt("sendto", downCount).apply();
                mSettings.edit().putInt("state", 2).apply();
                mSettings.edit().putString("seq", uSeq).apply();
                mSettings.edit().putLong("timestamp", Calendar.getInstance().getTimeInMillis()).apply();

                Log.d(TAG, "right before post delayed");
                mHandler.postDelayed(new VolWaitRunnable(), 3000);
            }
            else if (seq.startsWith("U")) {
                mSettings.edit().putString("seq", "").apply();
            }
        }
        else if (state == 2) {
            mVibrator.vibrate(getVibePattern(Math.abs(delta)), -1);
            mSettings.edit().putLong("timestamp", Calendar.getInstance().getTimeInMillis()).apply();

        }
    }

    private class VolWaitRunnable implements Runnable {

        @Override
        public void run() {

            // done!
            mSettings.edit().putBoolean("done", true).apply();
            String seq = mSettings.getString("seq", "");
            int userNum = mSettings.getInt("sendto", -1);
            Log.d(TAG, "DONE! sending message " + seq + " to user " + userNum);

            mSettings.edit().putString("seq", "").apply();
            mSettings.edit().putInt("state", 0).apply();

            String userSendRaw = mSettings.getString("user"+userNum, "");
            String []userSendSplit = userSendRaw.split(" ``` ");
            String friendNumber = userSendSplit[1];
            String body = userSendSplit[3];

            int code = seq.length();

            sendMessage(friendNumber, code, body);

        }
    }

    public void sendMessage(final String friendPhoneNumber, final int code, String body) {
        Log.d(TAG, "sending message. "+friendPhoneNumber+", "+code+", "+body);
        final String myPhoneNumber = mSettings.getString("myphone", "");

        switch (code) {
            case 1: {
                Log.d(TAG, "sending LOC");

// Define a listener that responds to location updates
                LocationListener locationListener = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        NetworkAPI.sendMessage(myPhoneNumber,
                                friendPhoneNumber,
                                code,
                                location.getLongitude()+","+location.getLatitude(),
                                null);
                        mLocationManager.removeUpdates(this);
                    }

                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    public void onProviderEnabled(String provider) {}

                    public void onProviderDisabled(String provider) {}
                };
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);


                break;
            }
            case 2: {
                Log.d(TAG, "sending BAT");


                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

                BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
                    int scale = -1;
                    int level = -1;

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        double percentage = (double)100*level/scale;
                        String batteryLevel = (int)percentage + "";
                        NetworkAPI.sendMessage(myPhoneNumber, friendPhoneNumber, code, level+"/"+scale, null);
                        Log.d(TAG, "Level is " + level + "/" + scale + " "  + percentage);
                        mMainActivity.unregisterReceiver(this);
                    }
                };

                mMainActivity.registerReceiver(batteryReceiver, filter);



                break;
            }
            case 3: {
                Log.d(TAG, "sending SOS");
                NetworkAPI.sendMessage(myPhoneNumber, friendPhoneNumber, code, "SOS", null);
                break;
            }
            default: {
                Log.d(TAG, "sending new custom message");
                NetworkAPI.sendMessage(myPhoneNumber, friendPhoneNumber, code, body, null);
                break;
            }
        }
    }

    private long[] getVibePattern(int n) {
        long []vibPattern = new long[Math.abs(n)*2+1];

        int vibCounter = 0;
        vibPattern[vibCounter++] = 0;
        for(int i=0; i<Math.abs(n); i++) {
            vibPattern[vibCounter++] = 200;
            vibPattern[vibCounter++] = 100;
        }
        return vibPattern;
    }




}