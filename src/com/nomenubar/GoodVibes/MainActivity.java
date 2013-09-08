package com.nomenubar.GoodVibes;

import android.app.*;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.firebase.client.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";
    private final int PICK_CONTACT = 100;
    SharedPreferences mSettings;
    Vibrator vibrator;
    String mMyPhoneNumber;
    NotificationManager mNotificationManager;
    LocationManager mLocationManager;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSettings = getSharedPreferences("GoodVibes", 0);

        TelephonyManager tMgr =(TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        mMyPhoneNumber = tMgr.getLine1Number();
        mSettings.edit().putString("myphone", mMyPhoneNumber).apply();
        Log.d(TAG, "my phone number: "+mMyPhoneNumber);
        NetworkAPI.register(mMyPhoneNumber);

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/gothamblack.otf");
        ((TextView)findViewById(R.id.user1name)).setTypeface(typeFace);
        ((TextView)findViewById(R.id.user2name)).setTypeface(typeFace);
        ((TextView)findViewById(R.id.user3name)).setTypeface(typeFace);
        ((TextView)findViewById(R.id.user4name)).setTypeface(typeFace);
        ((TextView)findViewById(R.id.vibeintro)).setTypeface(typeFace);
        ((TextView)findViewById(R.id.otherfriends)).setTypeface(typeFace);

        String user1 = mSettings.getString("user1", "");
        String user2 = mSettings.getString("user2", "");
        String user3 = mSettings.getString("user3", "");
        String user4 = mSettings.getString("user4", "");
        if (!user1.equals("")) {
            String [] userSplit = user1.split(" ``` ");
            displayUser(1, userSplit[0], userSplit[1], userSplit[2], userSplit[3]);
        }
        if (!user2.equals("")) {
            String [] userSplit = user2.split(" ``` ");
            displayUser(2, userSplit[0], userSplit[1], userSplit[2], userSplit[3]);
        }
        if (!user3.equals("")) {
            String [] userSplit = user3.split(" ``` ");
            displayUser(3, userSplit[0], userSplit[1], userSplit[2], userSplit[3]);
        }
        if (!user4.equals("")) {
            String [] userSplit = user4.split(" ``` ");
            displayUser(4, userSplit[0], userSplit[1], userSplit[2], userSplit[3]);
        }

        RelativeLayout user1rel = (RelativeLayout) findViewById(R.id.user1rel);
        RelativeLayout user2rel = (RelativeLayout) findViewById(R.id.user2rel);
        RelativeLayout user3rel = (RelativeLayout) findViewById(R.id.user3rel);
        RelativeLayout user4rel = (RelativeLayout) findViewById(R.id.user4rel);

        user1rel.setOnLongClickListener(new UserLongClickListener(1));
        user2rel.setOnLongClickListener(new UserLongClickListener(2));
        user3rel.setOnLongClickListener(new UserLongClickListener(3));
        user4rel.setOnLongClickListener(new UserLongClickListener(4));


        loopSound();

        HashMap<Integer, MessageType> volMap = setUpVolMap();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        ContentObserver mSettingsContentObserver = new SettingsContentObserver( this, new Handler(), volMap, vibrator, mLocationManager, this);
        this.getApplicationContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                mSettingsContentObserver );


        Firebase ref = new Firebase("https://goodvibes.firebaseio.com/users/"+mMyPhoneNumber+"/inbox");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String userName = snapshot.getName();
                GenericTypeIndicator<Map<String, Object>> t = new GenericTypeIndicator<Map<String, Object>>() {};
                Map<String, Object> userData = snapshot.getValue(t);

                String timestamp = (String)userData.get("timestamp");
                String sender = (String)userData.get("sender");
                String body = (String)userData.get("body");
                String recepient = (String)userData.get("recepient");
                String code = (String)userData.get("code");

                Log.d(TAG, timestamp + ", " + sender + ", " + body+ ", "+recepient +", "+code);
                Log.d(TAG, "now: "+Calendar.getInstance().getTimeInMillis());
                if (Calendar.getInstance().getTimeInMillis() - Long.parseLong(timestamp) < 60000) {
                    try {
                        showMessage(timestamp, sender, Integer.parseInt(code), body);
                    } catch (Exception e) {
                        Log.d(TAG, "shits null");
                    }

                }

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onCancelled() {

            }
        });

    }

    private void showMessage(String timestamp, String sender, int code, String body) {
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);

        String name = sender;
        while (phones.moveToNext()) {
            name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumberRaw = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            int digsFound = 0;
            StringBuilder sb = new StringBuilder();
            for(int i=phoneNumberRaw.length()-1; i>=0 && digsFound<10; i--) {
                if (Character.isDigit(phoneNumberRaw.charAt(i))) {
                    digsFound++;
                    sb.insert(0, phoneNumberRaw.charAt(i)+"");
                }
            }
            String phoneNumber = sb.toString();

            if (phoneNumber.equals(sender)) {
                Log.d(TAG, name +" just vibed you!");
                break;
            }
        }
        phones.close();

        String user1 = mSettings.getString("user1", "");
        String user2 = mSettings.getString("user2", "");
        String user3 = mSettings.getString("user3", "");
        String user4 = mSettings.getString("user4", "");

        int userPos = 5;
        if (!user1.equals("")) {
            if (user1.split(" ``` ")[1].equals(sender)) {
                userPos = 1;
            }
        } else if (!user2.equals("")) {
            if (user2.split(" ``` ")[1].equals(sender)) {
                userPos = 2;
            }
        } else if (!user3.equals("")) {
            if (user3.split(" ``` ")[1].equals(sender)) {
                userPos = 3;
            }
        } else if (!user4.equals("")) {
            if (user4.split(" ``` ")[1].equals(sender)) {
                userPos = 4;
            }
        }

        // --- - - -- -- --
        long [] pattern = new long[(1 + userPos + code)*2];
        int patternIndex = 0;
        pattern[patternIndex++] = 0;
        pattern[patternIndex++] = 2000;

        for(int i=0; i<userPos; i++) {
            pattern[patternIndex++] = 500;
            pattern[patternIndex++] = 500;
        }

        pattern[patternIndex++] = 1000;

        for(int i=0; i<code; i++) {
            pattern[patternIndex++] = 100;

            if (i != code-1) {
                pattern[patternIndex++] = 100;
            }
        }

        vibrator.vibrate(pattern, -1);

        String contentTitle = "";
        String contentText = "";
        if (code == 1) {
            contentTitle = "Bzz! New location vibe!";
            contentText = name+ " wants to share a location...";
        } else if (code == 2) {
            contentTitle = "Bzz! New battery-life vibe!";
            contentText = name+ "'s phone has a battery strength of "+body;
        } else if (code == 3) {
            contentTitle = "Bzz! New SOS vibe!";
            contentText = name+ " wants your help!";
        } else if (code == 4) {
            contentTitle = "Bzz! New vibe!";
            contentText = body;
        }

        Intent messageActivity = new Intent(this.getApplicationContext(), MessageActivity.class);
        messageActivity.putExtra("timestamp", timestamp);
        messageActivity.putExtra("sender", sender);
        messageActivity.putExtra("code", code);
        messageActivity.putExtra("body", body);
        messageActivity.putExtra("name", name);


        Notification notification = new Notification.Builder(this)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(
                        PendingIntent.getActivity(
                                this.getApplicationContext(),
                                (int) System.currentTimeMillis(),
                                messageActivity,
                                0))
                .setSmallIcon(R.drawable.ic_launcher).build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);


    }

    public class UserLongClickListener implements View.OnLongClickListener {

        int mPosition;

        public UserLongClickListener(int position) {
            mPosition = position;
        }

        @Override
        public boolean onLongClick(View view) {
            long pattern[] = {0, 20, 20, 30, 30, 50, 50};
            vibrator.vibrate(pattern, -1);

            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT+mPosition);
            return false;
        }
    }

    public HashMap<Integer, MessageType> setUpVolMap() {
        HashMap<Integer, MessageType> volMap = new HashMap<Integer, MessageType>();
        volMap.put(1, MessageType.LOC);
        volMap.put(2, MessageType.BAT);
        volMap.put(3, MessageType.SOS);
        volMap.put(4, MessageType.NEW);
        return volMap;
    }


    public void loopSound() {
        MediaPlayer mp;
        mp = MediaPlayer.create(getApplicationContext(), R.raw.blank1);
        mp.setLooping(true);
        mp.start();
    }


    public void contactChooser(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    int positionToUpdate;
    String mFriendPhoneNumber;
    String mCustomMessage;
    String mFriendName;
    String mFriendPic;

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        Log.d(TAG, "on result: " + reqCode +", "+resultCode+", "+data);

        positionToUpdate = reqCode - PICK_CONTACT;
        if (reqCode >= PICK_CONTACT && reqCode <= PICK_CONTACT+4) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = data.getData();
                Log.d(TAG, "contactData: "+contactData);
                Cursor c =  getContentResolver().query(contactData, null, null, null, null);
                if (c.moveToFirst()) {
                    mFriendName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    String hasNumber = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    final String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                    mFriendPic = c.getString(c.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));

                    Log.d(TAG, "hasNumber: "+hasNumber);
                    Log.d(TAG, "pic: "+mFriendPic);


                    if (hasNumber.equals("0")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(mFriendName+" doesn't have a phone number. Please try a different user.");
                        builder.setTitle("Woops!");
                        builder.show();
                    } else {

                        AlertDialog.Builder alert = new AlertDialog.Builder(this);

                        alert.setTitle("Adding "+mFriendName+"...");
                        alert.setMessage("Set a custom message:");

                        // Set an EditText view to get user input
                        final EditText input = new EditText(this);
                        input.setHint("I'm running late! :(");
                        alert.setView(input);

                        alert.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mCustomMessage = input.getText().toString();
                            if (mCustomMessage.isEmpty()) mCustomMessage = "poke!";

                            ArrayList<String> nums = getPhoneNumbers(id);
                            String phoneNumRaw = nums.get(0);
                            int digsFound = 0;
                            StringBuilder sb = new StringBuilder();
                            for(int i=phoneNumRaw.length()-1; i>=0 && digsFound<10; i--) {
                                if (Character.isDigit(phoneNumRaw.charAt(i))) {
                                    digsFound++;
                                    sb.insert(0, phoneNumRaw.charAt(i)+"");
                                }
                            }
                            mFriendPhoneNumber = sb.toString();
                            Log.d(TAG, mFriendPhoneNumber);

                            toggleProgress(true);
                            NetworkAPI.addFriend(mMyPhoneNumber, mFriendPhoneNumber, mCustomMessage, MainActivity.this);


                          }
                        });

                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                          }
                        });

                        alert.show();


                    }
                } else{
                    Log.d(TAG, "couldn't move to first");
                }
            }
        }
    }

    private void displayUser(int pos, String name, String phoneNum, String photoURI, String customMessage) {

        Log.d(TAG, pos+", "+name+", "+phoneNum+", "+photoURI+", "+customMessage);



        ImageView iv = null;
        TextView tv = null;
        switch (pos) {
            case 1:
                iv = (ImageView) findViewById(R.id.user1pic);
                tv = (TextView) findViewById(R.id.user1name);
                break;
            case 2:
                iv = (ImageView) findViewById(R.id.user2pic);
                tv = (TextView) findViewById(R.id.user2name);
                break;
            case 3:
                iv = (ImageView) findViewById(R.id.user3pic);
                tv = (TextView) findViewById(R.id.user3name);
                break;
            case 4:
                iv = (ImageView) findViewById(R.id.user4pic);
                tv = (TextView) findViewById(R.id.user4name);
                break;
        }

        try {
            if (photoURI == null) throw new FileNotFoundException();
            Bitmap b = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(photoURI)));
            Log.d(TAG, "bitmap, "+b);
            if (b!=null) {

                b = getCroppedBitmap(b);

                iv.setImageBitmap(b);
            }
            else {
                iv.setImageResource(R.drawable.ic_launcher);
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "err: not found. "+e.getMessage());
            iv.setImageResource(R.drawable.ic_launcher);
            e.printStackTrace();
        }

        tv.setText(name);
    }

    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }

    private ArrayList<String> getPhoneNumbers(String id)
    {
        ArrayList<String> phones = new ArrayList<String>();

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{id}, null);

        while (cursor.moveToNext()) {
            phones.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
        }

        cursor.close();
        return(phones);
    }

    private void toggleProgress(boolean on) {
        if (on) {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
            findViewById(R.id.otherfriends).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.progress).setVisibility(View.GONE);
            findViewById(R.id.otherfriends).setVisibility(View.VISIBLE);
        }
    }

    public void userExists(String friendPhoneNumber) {

        int friends = mSettings.getInt("friends", 0);

        int updating;
        if (positionToUpdate>=1 && positionToUpdate<=4) {
            updating = positionToUpdate;
        }
        else {
            updating = friends+1;
            mSettings.edit().putInt("friends", updating).apply();
        }

        toggleProgress(false);

        if (updating <= 4) {
            String userKey = "user"+updating;
            mSettings.edit().putString(userKey, mFriendName + " ``` " + mFriendPhoneNumber + " ``` " + mFriendPic + " ``` " + mCustomMessage).apply();
            displayUser(updating, mFriendName, mFriendPhoneNumber, mFriendPic, mCustomMessage);
        }
    }

    public void userDNE(String friendPhoneNumber) {
        toggleProgress(false);

        Toast.makeText(this.getApplicationContext(), mFriendName+" does not have the app installed!", Toast.LENGTH_LONG).show();

    }

    int selectMode = 0;

    public void user1Clicked(View v) {
        if (selectMode != 0) {
            selectMode = 0;
            hideComposer();
            return;
        }
        TextView tv = ((TextView)findViewById(R.id.user1name));
        if (!tv.getText().equals("")) {
            selectMode = 1;
            showComposer(selectMode);
        }
    }

    public void user2Clicked(View v) {
        if (selectMode != 0) {
            selectMode = 0;
            hideComposer();
            return;
        }
        TextView tv = ((TextView)findViewById(R.id.user2name));
        if (!tv.getText().equals("")) {
            selectMode = 2;
            showComposer(selectMode);
        }
    }

    public void user3Clicked(View v) {
        if (selectMode != 0) {
            selectMode = 0;
            hideComposer();
            return;
        }
        TextView tv = ((TextView)findViewById(R.id.user3name));
        if (!tv.getText().equals("")) {
            selectMode = 3;
            showComposer(selectMode);
        }
    }

    public void user4Clicked(View v) {
        if (selectMode != 0) {
            selectMode = 0;
            hideComposer();
            return;
        }
        TextView tv = ((TextView)findViewById(R.id.user4name));
        if (!tv.getText().equals("")) {
            selectMode = 4;
            showComposer(selectMode);
        }
    }

    String oldName1 = "";
    String oldName2 = "";
    String oldName3 = "";
    String oldName4 = "";
    public void showComposer(int pos) {
        (findViewById(R.id.code1img)).setVisibility(View.VISIBLE);
        (findViewById(R.id.code2img)).setVisibility(View.VISIBLE);
        (findViewById(R.id.code3img)).setVisibility(View.VISIBLE);
        (findViewById(R.id.code4img)).setVisibility(View.VISIBLE);

        (findViewById(R.id.vertical_arrowdiv)).setVisibility(View.VISIBLE);

        if (pos == 1) {
            ((ImageView)findViewById(R.id.vertical_arrowdiv)).setImageResource(R.drawable.arrow_1);
        } else if (pos == 2) {
            ((ImageView)findViewById(R.id.vertical_arrowdiv)).setImageResource(R.drawable.arrow_2);
        } else if (pos == 3) {
            ((ImageView)findViewById(R.id.vertical_arrowdiv)).setImageResource(R.drawable.arrow_3);
        } else if (pos == 4) {
            ((ImageView)findViewById(R.id.vertical_arrowdiv)).setImageResource(R.drawable.arrow_4);
        }

        oldName1 = ((TextView)findViewById(R.id.user1name)).getText().toString();
        ((TextView)findViewById(R.id.user1name)).setText("Your location");
        oldName2 = ((TextView)findViewById(R.id.user2name)).getText().toString();
        ((TextView)findViewById(R.id.user2name)).setText("Battery status");
        oldName3 = ((TextView)findViewById(R.id.user3name)).getText().toString();
        ((TextView)findViewById(R.id.user3name)).setText("SOS");
        oldName4 = ((TextView)findViewById(R.id.user4name)).getText().toString();
        String custom = mSettings.getString("user"+selectMode, "").split(" ``` ")[3];
        ((TextView)findViewById(R.id.user4name)).setText(custom);
    }

    public void hideComposer() {
        selectMode = 0;
        (findViewById(R.id.code1img)).setVisibility(View.INVISIBLE);
        (findViewById(R.id.code2img)).setVisibility(View.INVISIBLE);
        (findViewById(R.id.code3img)).setVisibility(View.INVISIBLE);
        (findViewById(R.id.code4img)).setVisibility(View.INVISIBLE);

        (findViewById(R.id.vertical_arrowdiv)).setVisibility(View.INVISIBLE);

        ((TextView)findViewById(R.id.user1name)).setText(oldName1);
        ((TextView)findViewById(R.id.user2name)).setText(oldName2);
        ((TextView)findViewById(R.id.user3name)).setText(oldName3);
        ((TextView)findViewById(R.id.user4name)).setText(oldName4);
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
                        MainActivity.this.unregisterReceiver(this);
                    }
                };

                this.registerReceiver(batteryReceiver, filter);



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

    public void code1Clicked(View v) {
        if (selectMode != 0) {
            String [] userSplit = mSettings.getString("user"+selectMode, "").split(" ``` ");
            sendMessage(userSplit[1], 1, userSplit[3]);
            Toast.makeText(getApplicationContext(), "Vibe sent!", Toast.LENGTH_SHORT).show();
            hideComposer();
        }
    }

    public void code2Clicked(View v) {
        if (selectMode != 0) {
            String [] userSplit = mSettings.getString("user"+selectMode, "").split(" ``` ");
            sendMessage(userSplit[1], 2, userSplit[3]);
            Toast.makeText(getApplicationContext(), "Vibe sent!", Toast.LENGTH_SHORT).show();
            hideComposer();
        }
    }

    public void code3Clicked(View v) {
        if (selectMode != 0) {
            String [] userSplit = mSettings.getString("user"+selectMode, "").split(" ``` ");
            sendMessage(userSplit[1], 3, userSplit[3]);
            Toast.makeText(getApplicationContext(), "Vibe sent!", Toast.LENGTH_SHORT).show();
            hideComposer();
        }
    }

    public void code4Clicked(View v) {
        if (selectMode != 0) {
            String [] userSplit = mSettings.getString("user"+selectMode, "").split(" ``` ");
            sendMessage(userSplit[1], 4, userSplit[3]);
            Toast.makeText(getApplicationContext(), "Vibe sent!", Toast.LENGTH_SHORT).show();
            hideComposer();
        }
    }
}
