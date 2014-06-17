package com.monadpad.omgbananas;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: m
 * Date: 7/4/13
 * Time: 11:35 PM
 */
public class SaveToOMG {

    public String desc = "";
    public String responseString = "";


    public SaveToOMG() {
    }

    private boolean doHttp(String saveUrl, String type, String tags, String data) {
        boolean saved = false;
        HttpClient httpclientup = new DefaultHttpClient();
        Log.d("MGH doHttp", "1");
        try {
            HttpPost hPost = new HttpPost(saveUrl);
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            postParams.add(new BasicNameValuePair("data", data));
            postParams.add(new BasicNameValuePair("type", type));
            postParams.add(new BasicNameValuePair("tags", tags));
            hPost.setEntity(new UrlEncodedFormEntity(postParams));

            HttpResponse response = httpclientup.execute(hPost);
            StatusLine statusLine = response.getStatusLine();
            //if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();

                getIdFromResponse(responseString);

                Log.d("MGH doHttp", responseString);
                if (!responseString.equals("bad")){
                    saved = true;
                }   else{
                    desc = responseString;
                }
            //}

        } catch (ClientProtocolException ee) {
            desc = ee.getMessage();

        } catch (IOException ee) {
            desc = ee.getMessage();
        }
        Log.d("MGH doHttp saved?", desc);
        return saved;

    }

    public void execute(String saveUrl, String type, String tags, String data) {
        new SendJam().execute(saveUrl, type, tags, data);
    }

    private class SendJam extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... args) {

            doHttp(args[0], args[1], args[2], args[3]);
            return null;
        }

        protected void onPreExecute(){
        }

        protected void onPostExecute(String result) {

        }
    }

    private int getIdFromResponse(String responseString) {
        int ret = -1;
        try {
            JSONObject response = new JSONObject(responseString);


            if (response.has("result") && response.getString("result").equals("good")) {
                if (response.has("id")) {
                    ret = response.getInt("id");
                }
            }
        }
        catch (JSONException ex) {

        }

        return ret;
    }
}
