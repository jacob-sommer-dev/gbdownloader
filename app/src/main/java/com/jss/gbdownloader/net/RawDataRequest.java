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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class RawDataRequest extends RestRequest {

    private final static String TAG = RawDataRequest.class.getSimpleName();

    private String contentType;

    private RawDataRequest(Builder builder){
        super(builder);
        this.contentType = builder.contentType;
    }

    @Override
    public void run() {
        HttpsURLConnection conn = null;
        StringBuilder result = new StringBuilder();
        int responseCode = -1;
        String responseMessage = "bad_result";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            URL url = new URL(this.url);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod(method.toString());
            // Set connection timeout and read timeout value.
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            if( method == ReqMethod.GET ){
                conn.setRequestProperty("Accept", contentType);

                InputStream in = conn.getInputStream();

                int read;
                byte[] buf = new byte[2048];
                while ((read = in.read(buf)) > -1) {
                    baos.write(buf, 0, read);
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

        callback.OnResult(new RawDataResult(responseCode, responseMessage, baos.toByteArray()));
    }

    public static class Builder extends RestRequest.Builder{

        private String contentType;

        public Builder(RestCallback callback, String url, ReqMethod method, String contentType) {
            super(url, method, callback);
            this.contentType = contentType;
        }

        @Override
        public RestRequest build(){
            return new RawDataRequest(RawDataRequest.Builder.this);
        }

    }

    public static class RawDataResult extends RestResult {
        public byte[] data;

        public RawDataResult(int resultCode, String resultMessage, byte[] data){
            super(resultCode, resultMessage);
            this.data = data;
        }
    }
}
