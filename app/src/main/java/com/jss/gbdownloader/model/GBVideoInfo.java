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

package com.jss.gbdownloader.model;

import com.jss.gbdownloader.net.NetUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;


public class GBVideoInfo {

    private URI imageUrl;
    private String title;
    private String desc;
    private String length;
    private HashMap<NetUtils.VidQuality, URI> videoUrls;
    private boolean premium;
    private int qualPos = 0;

    public GBVideoInfo(JSONObject json, String qualStr) throws JSONException, URISyntaxException {
        JSONObject imgObj = json.getJSONObject("image");
        imageUrl = new URI(imgObj.getString("small_url"));

        title = json.getString("name");
        desc = json.getString("deck");
        length = json.getString("length_seconds");
        if(!length.isEmpty()){
            int sec = Integer.parseInt(length);
            int hrs = sec / 3600; sec %= 3600;
            int min = sec / 60; sec %= 60;

            //there might not be hours, but there's probably min/sec
            if(hrs > 0){
                length = String.format(Locale.getDefault(),"%1dh %2dm %2ds", hrs, min, sec);
            } else {
                length = String.format(Locale.getDefault(),"%1dm %2ds", min, sec);
            }

        }

        // handle more than one uri
        String[] quals = qualStr.split(",");
        videoUrls = new HashMap<>(quals.length);

        for(String qual : quals) {
            if(qual != null) {
                String url = json.getString(qual);
                if(!url.isEmpty() && !url.equals("null")) {
                    videoUrls.put(NetUtils.VidQuality.from(qual), new URI(url));
                }
            }
        }

        premium = json.getBoolean("premium");
    }

    public URI getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public String getLength() {
        return length;
    }

    public ArrayList<NetUtils.VidQuality> getVidQuals() {
        Set<NetUtils.VidQuality> keys = videoUrls.keySet();
        return new ArrayList<>(keys);
    }

    public ArrayList<String> getVidQualsStrings() {
        Set<NetUtils.VidQuality> keys = videoUrls.keySet();
        ArrayList<String> list = new ArrayList<>(keys.size());
        for(NetUtils.VidQuality q : keys){
            list.add(q.getQual());
        }
        return list;
    }

    public void setQualPos(int pos) {
        qualPos = pos;
    }

    public int getQualPos() {
        return qualPos;
    }

    public HashMap<NetUtils.VidQuality, URI> getVideoUrls() {
        return videoUrls;
    }

    public URI getVideoUrl(NetUtils.VidQuality qual) {
        URI uri = null;
        uri = (qual != null) ? videoUrls.get(qual) : null;
        return uri;
    }

    public boolean isPremium() {
        return premium;
    }

    @Override
    public String toString(){
        return "IMG URL: " + imageUrl.toString() + "\n" +
                "VID URL: " + videoUrls.toString() + "\n" + // TODO
                "TITLE: " + title + "\n" +
                "DESC: " + desc;
    }
}
