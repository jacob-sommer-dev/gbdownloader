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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;
import android.widget.ImageView;

import com.jss.gbdownloader.MainActivity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NetUtils {

    public static final String BASE_HTTPS_SCHEME = "https";
    public static final String BASE_HOST = "www.giantbomb.com";
    public static final String BASE_LOCAL_HOST = "127.0.0.1";//testing

    public static final String VID_LIST_PATH = "/api/videos/";

    public static ConcurrentHashMap<String, Bitmap> bitmapCache = new ConcurrentHashMap<>();

    public enum VidQuality {
        ALL("low_url,high_url,hd_url"),
        LOW("low_url"),
        HIGH("high_url"),
        HD("hd_url");

        private String qVal;
        VidQuality(String q){
            qVal = q;
        }

        public String getQual() {
            return qVal;
        }

        public static VidQuality from(String str) {
            for (VidQuality q : VidQuality.values()){
                if(q.getQual().equals(str)) {
                    return q;
                }
            }
            return null;
        }
    }

    public enum ResultCode {
        OK(200);

        private int code;
        ResultCode(int code){
            this.code = code;
        }

        public int getCode(){
            return code;
        }
    }


    public static String buildUrl(String path, String query, String fragment, boolean... local){

        String encoded = null;
        String host = BASE_HOST;
        if(local != null && local.length == 1 && local[0]){
            host = BASE_LOCAL_HOST;
        }

        try {
            URI uri = new URI(BASE_HTTPS_SCHEME,
                    null,
                    host,
                    -1,
                    path,
                    query,
                    fragment);

            encoded = uri.toString();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return encoded;
    }

    public static String buildQueryString(String apiKey, List<Pair<String, String>> queries){

        //apiKey from prefs
        //String apiKey = "6e595f41a57e3912cdb7047646c6c802ef094d19";

        if(apiKey == null || apiKey.isEmpty()){
            return null;
        }

        StringBuilder queryBuilder = new StringBuilder();
        //do the api key first to ensure we have it
        queryBuilder.append("api_key").append('=').append(apiKey);

        //then do the rest
        if(queries != null){
            for(Pair<String, String> query : queries){
                queryBuilder.append('&').append(query.first).append('=').append(query.second);
            }
        }

        return queryBuilder.toString();
    }

    public static void getImageForView(ImageView view, URI uri){
        String uriString = uri.toString();

        if(bitmapCache.containsKey(uriString)){
            view.setImageBitmap(bitmapCache.get(uriString));
        } else {
            //fetch
            RestRequestor.getImgQuery(
                    new ImgDataCallback(uri.toString(), view),
                    uriString,
                    "image/*");
        }

    }

    public static class ImgDataCallback implements RestRequest.RestCallback {

        private String urlKey;
        private ImageView view;

        public ImgDataCallback(String urlKey, ImageView view){
            this.urlKey = urlKey;
            this.view = view;
        }

        @Override
        public void OnResult(RestRequest.RestResult result) {
            if(result instanceof RawDataRequest.RawDataResult){
                RawDataRequest.RawDataResult rawResult = (RawDataRequest.RawDataResult) result;

                if(rawResult.data != null){
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(rawResult.data, 0, rawResult.data.length);
                    if(bitmap != null){
                        bitmapCache.put(urlKey, bitmap);

                        MainActivity.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setImageBitmap(bitmap);
                            }
                        });
                    }
                }
            }

        }
    }

}
