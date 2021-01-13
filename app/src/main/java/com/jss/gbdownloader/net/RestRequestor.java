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
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestRequestor {

    private static String TAG = RestRequestor.class.getSimpleName();

    private static ExecutorService netExec = Executors.newSingleThreadExecutor();

/*    public static void shutdown(){
        netExec.shutdownNow();
    }

    public static void startup()
    {
        netExec = Executors.newSingleThreadExecutor();
    }*/

//region  queries

    public static void getVidListQuery(final VidListRestRequest.RestCallback callback, String apiKey, int limit, int offset, NetUtils.VidQuality quality){

        if(limit < 1 || limit > 100){
            limit = 100; //default per api
        }

        List<Pair<String, String>> qPairs = new ArrayList<>(5);
        qPairs.add(new Pair<>("format", "json"));
        qPairs.add(new Pair<>("limit", String.valueOf(limit)));

        if(offset > -1){
            qPairs.add(new Pair<>("offset", String.valueOf(offset)));
        }

        qPairs.add(new Pair<>("field_list", "deck,image,name,length_seconds,premium," + quality.getQual()));

        String query = NetUtils.buildQueryString(apiKey, qPairs);

        String url = NetUtils.buildUrl(NetUtils.VID_LIST_PATH, query, null);

        Log.d(TAG,"GETTING: "+url);

        //do the query
        VidListRestRequest request = (VidListRestRequest) new VidListRestRequest.Builder(callback, url, RestRequest.ReqMethod.GET, quality)
//                .setConnectTimeout(10000)
//                .setReadTimeout(10000)
                .build();

        netExec.execute(request);
    }

    public static void getImgQuery(final NetUtils.ImgDataCallback callback, String url, String contentType){
        Log.d(TAG,"GETTING: "+url);

        RawDataRequest request = (RawDataRequest) new RawDataRequest.Builder(callback, url, RestRequest.ReqMethod.GET, contentType)
                .build();

        netExec.execute(request);
    }

//endregion

}