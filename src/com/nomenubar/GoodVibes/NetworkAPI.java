package com.nomenubar.GoodVibes;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NetworkAPI {
    private static String URL = "http://good-vibes.herokuapp.com/";
    private static final String TAG = "NetworkAPI";

    public static void register(String phoneNumber) {
        String reqUrl = URL + "u/add?id=" + phoneNumber;
        new RequestTask(null).execute(reqUrl);
    }

    public static void addFriend(String myNumber, String friendNumber, String customMessage, MainActivity mainActivityCallback) {
        String reqUrl = URL + "u/"+myNumber+"/add?friend_id="+ friendNumber +"&message=" + customMessage;
        new RequestTask(mainActivityCallback, "addFriend", friendNumber).execute(reqUrl);
    }

    public static void sendMessage(String myNumber, String friendNumber, int code, String body, MainActivity mainActivityCallback) {
        Log.d(TAG, myNumber + ", " + friendNumber + ", " + code + ", "+ body +", " + mainActivityCallback);
        String reqUrl = URL + "u/"+myNumber+"/send?friend_id="+friendNumber+"&code="+code+"&body="+body;
        new RequestTask(mainActivityCallback, "sendMessage", friendNumber).execute(reqUrl);
    }


    private static class RequestTask extends AsyncTask<String, String, String> {

        MainActivity mCallback;
        String mMeta = "";
        String mMeta2 = "";

        public RequestTask(MainActivity callback) {
            mCallback = callback;
        }

        public RequestTask(MainActivity callback, String meta, String meta2) {
            mCallback = callback;
            mMeta = meta;
            mMeta2 = meta2;
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, "post execute: " + result);

            if (mMeta.equals("addFriend")) {
                try {
                    String errorMessage = (new JSONObject(result)).getString("error");
                    if (errorMessage!=null && !errorMessage.isEmpty()) {
                        // disabled mode
                        Log.d(TAG, "error is not empty, "+errorMessage);
                        mCallback.userDNE(mMeta2);
                    }
                    else {

                    }
                } catch (JSONException e) {
                    Log.d(TAG, "json err: "+e.getMessage());
                    if (e.getMessage().contains("No value")) {
                        mCallback.userExists(mMeta2);
                    }
                }
            } else if (mMeta.equals("sendMessage")) {
                Log.d(TAG, "just sent a message to "+mMeta2);
            }
        }
    }
}
