package com.nomenubar.GoodVibes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: binroot
 * Date: 9/8/13
 * Time: 2:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class MessageActivity extends Activity {
    final String TAG = "MessageActivity";

    String lat = "";
    String lon = "";
    String name = "";

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.message);

        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        String timestamp = extras.getString("timestamp");
        String sender = extras.getString("sender");
        int code = extras.getInt("code");
        String body = extras.getString("body");
        name = extras.getString("name");

        ((TextView)findViewById(R.id.name)).setText(name);


        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Long.parseLong(timestamp));
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        String dateStr = dateFormat.format(cal.getTime());

        ((TextView)findViewById(R.id.time)).setText(dateStr);

        if (code == 1) {
            ((TextView)findViewById(R.id.title)).setText("Here I am:");
            lat = body.split(",")[0];
            lon = body.split(",")[1];
            new DownloadFilesTask().execute("http://a.tiles.mapbox.com/v3/lowkaseo.map-2c2dakp2/"+lat+","+lon+",16/600x400.png");
        } else if (code == 2) {

            ((ImageView)findViewById(R.id.metaimg)).setVisibility(View.VISIBLE);
            int numerator = Integer.parseInt(body.split("/")[0]);
            int denominator = Integer.parseInt(body.split("/")[1]);
            double frac = (numerator+0.0)/(denominator+0.0);
            ((TextView)findViewById(R.id.title)).setText("Current battery status: "+body);
            if (frac <= 0.20) {
                ((ImageView)findViewById(R.id.metaimg)).setImageResource(R.drawable.battlow);
            } else if (frac <= 0.85) {
                ((ImageView)findViewById(R.id.metaimg)).setImageResource(R.drawable.battmid);
            } else {
                ((ImageView)findViewById(R.id.metaimg)).setImageResource(R.drawable.batthigh);
            }

        } else if (code == 3) {
            ((ImageView)findViewById(R.id.metaimg)).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.title)).setText("SOS");
        } else if (code == 4) {
            ((ImageView)findViewById(R.id.metaimg)).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.title)).setText(body);
        }

    }

    public class DownloadFilesTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... urls) {
            InputStream in = null;

            Bitmap b = null;
            try {
                in = new java.net.URL(urls[0]).openStream();
                b = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }

            return b;
        }

        protected void onPostExecute(Bitmap b) {
            Log.d(TAG, "bitmap, "+b);

            ((ImageView)findViewById(R.id.metaimg)).setImageBitmap(b);
            ((ImageView)findViewById(R.id.metaimg)).setVisibility(View.VISIBLE);
        }


    }

    public void imgClicked(View v) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
        Uri.parse("geo:0,0?q="+lon+","+lat+" (" + name + ")"));
        startActivity(intent);
    }


}