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

package com.jss.gbdownloader;

import android.os.Build;
import android.os.Environment;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

public class FileUtils {

    //region String versions

    public static boolean checkIfDownloaded(String uri){
        File file = getFileForUrl(uri);
        return file != null && file.exists();
    }

    public static long checkIfPartialDownloaded(String uri){
        long len = -1;
        File file = getPartialFileForUrl(uri);
        if(file != null &&
                file.exists() &&
                file.canWrite()){
            len = file.length();

        }
        return len;
    }

    public static Pair<Long, FileOutputStream> getFileOutputStreamForUrl(String uri, boolean append) throws IOException {
        FileOutputStream fos = null;
        long len = 0;

        File file = getPartialFileForUrl(uri);
        if(file != null){
            if(file.exists()){
                len = file.length();
                fos = new FileOutputStream(file, append);
            } else {
                if(file.createNewFile()){
                    fos = new FileOutputStream(file, append);
                }
            }
        }

        return (fos == null) ? null : new Pair<>(len, fos);
    }

    public static File getFileForUrl(String uri){
        if(Build.VERSION.SDK_INT < 29){
            //do it the old way
            String downloadsPath = Environment.getExternalStorageDirectory().getPath() + "/Download/";
            return new File(downloadsPath + getFilenameForUrl(uri));

        } else {
            //TODO use the Media API
        }

        return null;
    }

    public static File getPartialFileForUrl(String uri){
        if(Build.VERSION.SDK_INT < 29){
            //do it the old way
            String downloadsPath = Environment.getExternalStorageDirectory().getPath() + "/Download/";
            return new File(downloadsPath + getFilenameForUrl(uri) + ".partial");

        } else {
            //TODO use the Media API
        }

        return null;
    }

    public static String getFilenameForUrl(String suri){
        String filename = null;

        int len = suri.length();
        int indx = suri.lastIndexOf('/');
        int indx2 = suri.indexOf('?', indx);

        if(indx > -1 && indx < len){
            if(indx2 > -1){
                filename = suri.substring(indx, indx2);
            } else {
                filename = suri.substring(indx);
            }
        }

        return filename;
    }

    //endregion

    //region URI versions

    public static boolean checkIfDownloaded(URI uri){
        return checkIfDownloaded(uri.toString());
    }

    public static long checkIfPartialDownloaded(URI uri){
        return checkIfPartialDownloaded(uri.toString());
    }

    public static Pair<Long, FileOutputStream> getFileOutputStreamForUrl(URI uri, boolean append) throws IOException {
        return getFileOutputStreamForUrl(uri.toString(), append);
    }

    public static File getFileForUrl(URI uri){
        return getFileForUrl(uri.toString());
    }

    public static File getPartialFileForUrl(URI uri){
        return getPartialFileForUrl(uri.toString());
    }

    public static String getFilenameForUrl(URI uri){
        return getFilenameForUrl(uri.toString());
    }

    //endregion

}
