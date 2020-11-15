/**
 *     Copyright 2020 Jacob Sommer
 *
 *     This file is part of gbdownloader.
 *
 *     gbdownloader is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     gbdownloader is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with gbdownloader.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jss.gbdownloader.net;

import android.util.Log;

import com.jss.gbdownloader.model.GBVideoInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

public class VidListRestRequest extends RestRequest {

    private final static String TAG = VidListRestRequest.class.getSimpleName();

    private NetUtils.VidQuality quality;

    private VidListRestRequest(Builder builder){
        super(builder);
        quality = builder.quality;
    }


    @Override
    public void run() {
        HttpsURLConnection conn = null;
        StringBuilder result = new StringBuilder();
        int responseCode = -1;
        String responseMessage = "bad_result";

        try {
            URL url = new URL(this.url);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod(method.toString());
            // Set connection timeout and read timeout value.
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            if( method == ReqMethod.GET ){
                conn.setRequestProperty("Accept", "application/json");

                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            //grab the response code and message in case of failure
            responseCode = conn.getResponseCode();
            responseMessage = conn.getResponseMessage();

            conn.disconnect();

        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Timeout: " + url, e);
        } catch (ProtocolException e) {
            Log.e(TAG, "Protocol Exception: " + url, e);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL: " + url, e);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "No Resource found at: " + url);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: " + url, e);
        } finally {
            if(conn != null){
                conn.disconnect();
            }
        }

        callback.OnResult(buildVidListResult(responseCode, responseMessage, result.toString()));
    }

    private VidListResult buildVidListResult(int resultCode, String resultMessage, String... result){
//        Log.d(TAG, "CODE: " + resultCode + ":" + resultMessage + " - " + result);

        ArrayList<GBVideoInfo> vidInfos = new ArrayList<>();

        if(result != null && result.length == 1 && result[0] != null && !result[0].isEmpty()) {
            //translate the json
            try {
                JSONObject req = new JSONObject(result[0]);
                JSONArray json = req.getJSONArray("results");
                for(int i = 0; i < json.length(); i++){
                    vidInfos.add(new GBVideoInfo(json.getJSONObject(i), quality.getQual()));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return new VidListResult(resultCode, resultMessage, vidInfos);
    }


    /**
     * Builder for the VidListRestRequest. URL and request method are required params.
     */
    public static class Builder extends RestRequest.Builder{

        private NetUtils.VidQuality quality;

        /**
         * Constructor for the RestRequest builder. URL and request method are required params.
         *
         * @param url    String representation of the RESt endpoint url.
         * @param method The request method to use.
         */
        public Builder(RestCallback callback, String url, ReqMethod method, NetUtils.VidQuality quality) {
            super(url, method, callback);
            this.quality = quality;
        }

        @Override
        public RestRequest build(){
            return new VidListRestRequest(VidListRestRequest.Builder.this);
        }

    }

    public static class VidListResult extends RestResult {
        public ArrayList<GBVideoInfo> vidList;

        public VidListResult(int resultCode, String resultMessage, ArrayList<GBVideoInfo> vids){
            super(resultCode, resultMessage);
            vidList = vids;
        }
    }

}