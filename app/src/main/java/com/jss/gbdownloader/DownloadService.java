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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.jss.gbdownloader.aidl.IDownloadServicelInterface;
import com.jss.gbdownloader.net.RestRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class DownloadService extends Service {

    private static final String TAG = DownloadService.class.getSimpleName();

    public static final String DOWNLOAD_URI_SERVICE_KEY = "DOWNLOAD_URI_SERVICE_KEY";
    public static final String CANCEL_URI_SERVICE_KEY = "CANCEL_URI_SERVICE_KEY";

    private ExecutorService dldExec;

    private final Object currentLockObject = new Object();
    private final Object boundLockObject = new Object();

    private Hashtable<String, Future<String>> pendingTasks;

    private RestRequest.ReqMethod method = RestRequest.ReqMethod.GET;
    private int readTimeout = 10000;
    private int connectTimeout = 10000;

    private String apiQuery;

    private boolean isBound = false;

    private Handler uiHandler;

    @Override
    public void onCreate(){
        uiHandler = new Handler();

        pendingTasks = new Hashtable<>();

        dldExec = Executors.newSingleThreadExecutor();

        SharedPreferences prefs = this.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        apiQuery = "?api_key=" + prefs.getString(Constants.API_KEY, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY_COMPATIBILITY;
    }

    private Future<String> startDownloadTask(final String uri){
        cleanDownloadQueue();

//        synchronized (currentLockObject){
            return dldExec.submit(new Callable<String>() {
                @Override
                public String call() {

                    int responseCode = -1;
                    String responseMessage = "bad_result";

                    if(uri != null && !uri.isEmpty()){

                        Pair<Long, FileOutputStream> pair = null;
                        FileOutputStream fos = null;

                        long at = FileUtils.checkIfPartialDownloaded(uri);
                        if(at < 0){ //returns -1 if no partial
                            at = 0;
                        }

                        // build request and download
                        // lots of reused code from RawDataRequest,
                        // but necessary so we can write to disk
                        // instead of a buffer in ram

                        HttpsURLConnection conn = null;
                        StringBuilder result = new StringBuilder();

                        try {
                            URL url = new URL(uri + apiQuery);
                            conn = (HttpsURLConnection) url.openConnection();
                            conn.setRequestMethod(method.toString());
                            // Set connection timeout and read timeout value.
                            conn.setConnectTimeout(connectTimeout);
                            conn.setReadTimeout(readTimeout);

                            if( method == RestRequest.ReqMethod.GET ){
                                conn.setRequestProperty("Accept", "video/mp4;video/*");
                                if(at > 0){
                                    conn.setRequestProperty("Range", "bytes=" + at + "-");
                                }

                                InputStream in = conn.getInputStream();

                                //grab the response code and message in case of failure
                                responseCode = conn.getResponseCode();
                                responseMessage = conn.getResponseMessage();

                                try {
                                    //if the response code is 206, it's sending a partial, so append
                                    pair = FileUtils.getFileOutputStreamForUrl(uri, responseCode == 206);
                                    fos = pair.second;

                                    // check length with an if or else the annotation freaks out
                                    long len = -1; //same as unknown
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        len = conn.getContentLengthLong();
                                    } else {
                                        len = conn.getContentLength();
                                    }

                                    if(at > len){
                                        //TODO let the app/user know so they can fix this (delete the file?)
                                        if(len == -1){ //handle case where content length can't be determined
                                            Log.e(TAG, "Content Length couldn't be determined");
                                        } else {
                                            throw new IOException("File size bigger than requested content");
                                        }

                                    }

                                    if(len != -1){
                                        len += at;//if it's resuming, len will only be the length of the download needed, so add the current progress
                                    }

                                    int oldProg = 0;
                                    int read;
                                    byte[] buf = new byte[2048];
                                    while ((read = in.read(buf)) > -1) {
                                        fos.write(buf, 0, read);

                                        //update progress
                                        at += read;
                                        if(len > 0 && (at <= len)){

                                            int prog = (int)(100 * (Double.longBitsToDouble(at) / Double.longBitsToDouble(len)));
                                            if(prog >= 0 && prog <= 100 && prog > oldProg){ //should be a percentage so this shouldn't be a problem
                                                oldProg = prog;

                                                sendBroadcast(new Intent()
                                                        //.setClass(DownloadService.this, MainActivity.class)
                                                        .setAction(Constants.DL_PROG_INTENT_ACTION)
                                                        .putExtra(Constants.DL_PROG_URL_KEY, uri)
                                                        .putExtra(Constants.DL_PROG_PROGRESS_KEY, prog)
                                                );
                                            }
                                        } else if (len == -1){//need some kind of indeterminate progress indicator without switching progress bar type
                                            double mod = at % 6.28; //get some decimal between 0 and 2pi
                                            double sin = Math.sin(mod); // get some decimal between -1 and 1
                                            int prog = Math.abs((int)((100 * sin) / 25) * 25); //multi to percentage and snap to interval of 25 (less updates-ish)
                                            if(prog >= 0 && prog <= 100 && prog != oldProg){ //should be a percentage so this shouldn't be a problem
                                                oldProg = prog;

                                                sendBroadcast(new Intent()
                                                        //.setClass(DownloadService.this, MainActivity.class)
                                                        .setAction(Constants.DL_PROG_INTENT_ACTION)
                                                        .putExtra(Constants.DL_PROG_URL_KEY, uri)
                                                        .putExtra(Constants.DL_PROG_PROGRESS_KEY, prog)
                                                );
                                            }
                                        }
                                    }

                                    fos.flush();
                                    fos.close();
                                    fos = null;

                                    if(at == len || len == -1){ // only rename if we got the whole file, or we don't know the content length
                                        File from = FileUtils.getPartialFileForUrl(uri);
                                        File to = FileUtils.getFileForUrl(uri);
                                        if(from != null && to != null){
                                            from.renameTo(to);
                                        }
                                    }

                                } catch (IOException e) {
                                    Log.e(TAG, "IOException getting file output stream", e);
                                }

                            }

                        } catch (SocketTimeoutException e) {
                            Log.e(TAG, "Timeout: " + uri, e);
                        } catch (ProtocolException e) {
                            Log.e(TAG, "Protocol Exception: " + uri, e);
                        } catch (MalformedURLException e) {
                            Log.e(TAG, "Malformed URL: " + uri, e);
                        } catch (FileNotFoundException e) {
                            Log.d(TAG, "No Resource found at: " + uri);
                        } catch (IOException e) {
                            Log.e(TAG, "IO Exception: " + uri, e);
                        } finally {
                            if(conn != null){
                                conn.disconnect();
                            }
                            if(fos != null){
                                try {
                                    fos.flush();
                                    fos.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "exception closing file stream in finally", e);
                                }

                            }
                        }
                    }

                    //check to see if the service needs to stop
                    synchronized (boundLockObject) {
                        if(!isBound){//not currently bound, so see if it needs to stop
                            cleanDownloadQueue();
                            synchronized (currentLockObject){
                                if(pendingTasks.isEmpty() || (pendingTasks.size() == 1 && pendingTasks.containsKey(uri))){
                                    //no more tasks (other than this one) and is unbound, so stop self
                                    uiHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            stopSelf();
                                        }
                                    });
                                }
                            }
                        }
                    }



                    return uri + "\t" + responseCode + "\t" + responseMessage;
                }
            });
//        }
    }

    private boolean stopDownloadTask(String uri){

        synchronized (currentLockObject){
            if(pendingTasks.containsKey(uri)){
                Future<String> task = pendingTasks.get(uri);
                if(task != null && !(task.isDone() || task.isCancelled())){
                    return task.cancel(true);
                }
            }
        }

        cleanDownloadQueue();

        return false;
    }

    private boolean hasDownloadTask(String uri){
        cleanDownloadQueue();

        synchronized (currentLockObject) {
            return pendingTasks.containsKey(uri);
        }
    }

    private boolean cleanDownloadQueue(){
        List<String> toRemove = new ArrayList<>();

        synchronized (currentLockObject) {
            for (String futureKey : pendingTasks.keySet()){
                Future<String> future = pendingTasks.get(futureKey);
                if(future != null && (future.isCancelled() || future.isDone())){

                    toRemove.add(futureKey);
                }
            }

            for(String futureKey : toRemove){
                pendingTasks.remove(futureKey);
            }

            return pendingTasks.isEmpty();
        }
    }

    @Override
    public void onDestroy() {
        synchronized (currentLockObject) {
            for (Future<String> future : pendingTasks.values()){
                future.cancel(true);
            }
            pendingTasks.clear();
        }
        dldExec.shutdownNow();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        synchronized (boundLockObject){
            isBound = true;
        }

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        synchronized (boundLockObject) {
            isBound = false;
        }

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (currentLockObject){

                    while(!cleanDownloadQueue()){
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "interrupted shutting down, just kill it...", e);
                            dldExec.shutdownNow();
                            stopSelf();
                        }
                    }

                    dldExec.shutdownNow();
                    stopSelf();

                }
            }
        }).start();*/


        return super.onUnbind(intent);
    }

    private final IDownloadServicelInterface.Stub mBinder = new IDownloadServicelInterface.Stub() {

        @Override
        public boolean enqueue(String uri) throws RemoteException {
            boolean success = true;
            synchronized(currentLockObject){
                if(!pendingTasks.containsKey(uri)){
                    Future<String> future = startDownloadTask(uri);
                    if(future != null){
                        pendingTasks.put(uri, future);
                    } else {
                        success = false;
                    }
                } else {
                    success = false;
                }
            }

            return success;
        }

        @Override
        public boolean cancel(String uri) throws RemoteException {
            return stopDownloadTask(uri);
        }

        @Override
        public boolean isQueued(String uri) throws RemoteException {
            return hasDownloadTask(uri);
        }
    };


}
