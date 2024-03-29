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

package com.jss.gbdownloader.views;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.jss.gbdownloader.Constants;
import com.jss.gbdownloader.FileUtils;
import com.jss.gbdownloader.R;
import com.jss.gbdownloader.aidl.IDownloadServicelInterface;
import com.jss.gbdownloader.model.GBVideoInfo;
import com.jss.gbdownloader.net.NetUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that can display a {@link GBVideoInfo}.
 */
public class GBVidsItemRecyclerViewAdapter extends RecyclerView.Adapter<GBVidsItemRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = GBVidsItemRecyclerViewAdapter.class.getSimpleName();

    private final List<GBVideoInfo> mValues = new ArrayList<>();
    private final Hashtable<String, ProgressListener> progressTable = new Hashtable<>();

    private Activity activity;

    private IDownloadServicelInterface svcInterface;

    public final ServiceConnection svcConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            svcInterface = (IDownloadServicelInterface)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            svcInterface = null;
        }
    };

    public GBVidsItemRecyclerViewAdapter(Activity activity) {
        this.activity = activity;
    }

    public void clearData(){
        mValues.clear();
        notifyDataSetChanged();
    }

    public void addData(List<GBVideoInfo> data){
        mValues.addAll(data);
        notifyDataSetChanged();
    }

    public void updateProgress(String uri, int progress){
        if(progressTable.containsKey(uri)){
            ProgressListener l = progressTable.get(uri);
            if(l != null){
                l.onProgressUpdated(progress);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gb_vid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        //set image view
        NetUtils.getImageForView(holder.mVidImgView, holder.mItem.getImageUrl());

        String title = holder.mItem.getTitle();
        holder.mVidTitleView.setText(title);
        holder.mVidDescView.setText(holder.mItem.getDesc());
        holder.mVidLengthView.setText(holder.mItem.getLength());

        holder.mTxtsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder bldr = new AlertDialog.Builder(view.getContext());
                bldr.setTitle(holder.mItem.getTitle())
                        .setMessage(holder.mItem.getDesc())
                        .show();
            }
        });

        holder.mVidQualSpinner.setAdapter(new ArrayAdapter<String>(activity.getApplicationContext(), android.R.layout.simple_spinner_item, holder.mItem.getVidQualsStrings()));
        holder.mVidQualSpinner.setSelection(holder.mItem.getQualPos(), false);

        URI uri = holder.mItem.getVideoUrl(NetUtils.VidQuality.from((String) holder.mVidQualSpinner.getSelectedItem()));
        String uriStr = null;
        // if uri is null, quality not available, try to find another
        if (uri == null) {
            for (int i = 0; i < holder.mVidQualSpinner.getAdapter().getCount(); i++) {
                uri = holder.mItem.getVideoUrl(NetUtils.VidQuality.from((String) holder.mVidQualSpinner.getItemAtPosition(i)));
                if(uri != null) {
                    // found one, break
                    uriStr = uri.toString();
                    break;
                }
            }
        } else {
            uriStr = uri.toString();
        }

        if(uriStr != null) {
            // set visible in case they were invisible before
            holder.mVidQualSpinner.setVisibility(View.VISIBLE);
            holder.dlLayoutView.setVisibility(View.VISIBLE);

            holder.checkProgressListenerAttached(uriStr);
            holder.updateDLButton();
        } else {
            holder.mVidQualSpinner.setVisibility(View.INVISIBLE);
            holder.dlLayoutView.setVisibility(View.INVISIBLE);
        }

        if(holder.mItem.isPremium()){
            holder.mView.setBackgroundColor(Color.rgb(220,220,188));
        } else {
            holder.mView.setBackgroundColor(Color.rgb(188,188,188));
        }


        //debug
        //Log.d("GB VID INFO", "\n" + holder.mItem.toString() + "\n");
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        for(URI uri : holder.mItem.getVideoUrls().values()) {
            progressTable.remove(uri.toString());
        }

        holder.mItem.setQualPos(holder.mVidQualSpinner.getSelectedItemPosition());

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public GBVideoInfo mItem;
        public final View mView;
        public final View dlLayoutView;
        public final ImageView mVidImgView;
        public final TextView mVidTitleView;
        public final TextView mVidDescView;
        public final TextView mVidLengthView;
        public final Spinner mVidQualSpinner;
        public final ImageButton mVidDLButton;
        public final ImageButton mVidDeleteButton;
        public final ProgressBar mProgressBar;
        public final TextView mProgressText;
        public final View mTxtsLayout;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            dlLayoutView = view.findViewById(R.id.dl_layout);
            mVidImgView = (ImageView) view.findViewById(R.id.vid_img);
            mVidTitleView = (TextView) view.findViewById(R.id.vid_title_view);
            mVidDescView = (TextView) view.findViewById(R.id.vid_desc_view);
            mVidLengthView = (TextView) view.findViewById(R.id.vid_time_len_view);
            mVidQualSpinner = (Spinner) view.findViewById(R.id.vidQualSpinner);
            mVidDLButton = (ImageButton) view.findViewById(R.id.dwnld_btn);
            mVidDeleteButton = (ImageButton) view.findViewById(R.id.delete_btn);
            mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            mProgressText = (TextView) view.findViewById(R.id.progressText);
            mTxtsLayout = view.findViewById(R.id.txts_layout);

            mVidQualSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if(ViewHolder.this.mItem != null) {
                        String uri = ViewHolder.this.mItem.getVideoUrl(NetUtils.VidQuality.from((String) ViewHolder.this.mVidQualSpinner.getSelectedItem())).toString();
                        checkProgressListenerAttached(uri);
                        updateDLButton();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mVidTitleView.getText() + "'";
        }

        public void checkProgressListenerAttached(String uri) {
            if(!progressTable.containsKey(uri)){
                progressTable.put(uri, new ProgressListener() {
                    @Override
                    public void onProgressUpdated(int progress) {
                        updateProgress(progress);
                    }
                });
            }
        }

        public void updateProgress(Integer progress) {

            final int prog = progress;
            Handler h = mProgressBar.getHandler();
            if(h != null){
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        updateDLButton(Constants.DLButtonState.DOWNLOADING);
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressText.setVisibility(View.VISIBLE);
                        mProgressBar.setProgress(prog);
                        mProgressText.setText(String.format(Locale.getDefault(), "%d", prog)); // TODO

                        if(prog >= 100){
                            updateDLButton(Constants.DLButtonState.DOWNLOADED);
                        }
                    }
                });
            }
        }

        public void updateDLButton(){
            if(mItem != null){
                String uri = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem())).toString();
                //check if the video is downloaded
                try{
                    if(FileUtils.checkIfDownloaded(uri)){
                        updateDLButton(Constants.DLButtonState.DOWNLOADED);
                    } else if(svcInterface != null && svcInterface.isQueued(uri)){
                        updateDLButton(Constants.DLButtonState.DOWNLOADING);
                    } else if (FileUtils.checkIfPartialDownloaded(uri) > -1L){
                        updateDLButton(Constants.DLButtonState.PARTIAL);
                    } else {
                        updateDLButton(Constants.DLButtonState.READY);
                    }
                } catch (RemoteException e){
                    Log.e(TAG, "RemoteException updating button (checking if queued)", e);
                }

            }
        }

        public void updateDLButton(Constants.DLButtonState state){
            mProgressBar.setVisibility(View.INVISIBLE);
            mProgressText.setVisibility(View.INVISIBLE);
            mVidDeleteButton.setVisibility(View.GONE);
            switch (state){
                case READY:

                    mVidDLButton.setImageResource(android.R.drawable.ic_input_add);
                    mVidDLButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(svcInterface != null){
                                try {

                                    String uri = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem())).toString();

                                    if(!progressTable.containsKey(uri)){
                                        progressTable.put(uri, new ProgressListener() {
                                            @Override
                                            public void onProgressUpdated(int progress) {
                                                updateProgress(progress);
                                            }
                                        });
                                    }

                                    svcInterface.enqueue(uri);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Remote Exception clicking download", e);
                                }
                            }
                            updateDLButton();
                        }
                    });

                    break;
                case DOWNLOADED:
                    mVidDeleteButton.setVisibility(View.VISIBLE);
                    mVidDeleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            URI uri = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem()));
                            promptDelete(uri, false, ViewHolder.this);
                        }
                    });
                    mVidDLButton.setImageResource(android.R.drawable.ic_media_play);
                    mVidDLButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // vlc intent from https://wiki.videolan.org/Android_Player_Intents/
                            int vlcRequestCode = 42;
                            URI url = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem()));
                            File f = FileUtils.getFileForUrl(url);
                            Uri uri = Uri.parse(f.getAbsolutePath());
                            Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
                            vlcIntent.setPackage("org.videolan.vlc");
                            vlcIntent.setDataAndTypeAndNormalize(uri, "video/*");
                            vlcIntent.setComponent(new ComponentName("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity"));
                            activity.startActivityForResult(vlcIntent, vlcRequestCode);
                        }
                    });
                    break;
                case DOWNLOADING:
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressText.setVisibility(View.VISIBLE);
                    mVidDLButton.setImageResource(android.R.drawable.ic_media_pause);
                    mVidDLButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(svcInterface != null){
                                try {
                                    String uri = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem())).toString();
                                    svcInterface.cancel(uri);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Remote Exception clicking stop dl", e);
                                }
                            }
                            updateDLButton();
                        }
                    });
                    break;
                case PARTIAL:
                    mVidDeleteButton.setVisibility(View.VISIBLE);
                    mVidDeleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            URI uri = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem()));
                            promptDelete(uri, true, ViewHolder.this);
                        }
                    });
                    mVidDLButton.setImageResource(android.R.drawable.ic_popup_sync);
                    mVidDLButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(svcInterface != null){
                                try {
                                    String uri = mItem.getVideoUrl(NetUtils.VidQuality.from((String) mVidQualSpinner.getSelectedItem())).toString();
                                    svcInterface.enqueue(uri);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Remote Exception clicking retry download", e);
                                }
                            }
                            updateDLButton();
                        }
                    });
                    break;

            }
        }

    }

    private void promptDelete(final URI videoUrl, final boolean partial, final ViewHolder view){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.delete)
                .setMessage("Delete " + FileUtils.getFilenameForUrl(videoUrl) + "?")
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File file = null;
                        if(partial){
                            file = FileUtils.getPartialFileForUrl(videoUrl);
                        } else {
                            file = FileUtils.getFileForUrl(videoUrl);
                        }

                        if(file != null){
                            file.delete();

                            if(view != null){
                                view.updateDLButton();
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    public interface ProgressListener {
        void onProgressUpdated(int progress);
    }

}