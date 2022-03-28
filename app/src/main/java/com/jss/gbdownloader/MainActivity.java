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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import com.jss.gbdownloader.net.NetUtils;
import com.jss.gbdownloader.net.RestRequestor;
import com.jss.gbdownloader.net.VidListRestRequest;
import com.jss.gbdownloader.views.GBVidsItemRecyclerViewAdapter;

public class MainActivity extends AppCompatActivity {

    private int lastQueried = 0;
    private int queryinterval = 50; //TODO make configurable?

    private RecyclerView vidView;
    private GBVidsItemRecyclerViewAdapter adapter;

    private BroadcastReceiver receiver;

    public static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) { //TODO HANDLE ROTATION
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        handler = new Handler();

        adapter = new GBVidsItemRecyclerViewAdapter(this);

        IntentFilter filter = new IntentFilter(Constants.DL_PROG_INTENT_ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String url = intent.getStringExtra(Constants.DL_PROG_URL_KEY);
                int progress = intent.getIntExtra(Constants.DL_PROG_PROGRESS_KEY, 0);
                adapter.updateProgress(url, progress);
            }
        };
        registerReceiver(receiver, filter);

        Intent scvIntent = new Intent().setClass(this, DownloadService.class);
        startService(scvIntent);

        vidView = findViewById(R.id.gb_vids_list_view);
        vidView.setLayoutManager(new LinearLayoutManager(this));
        vidView.setAdapter(adapter);
        vidView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                int pos = vidView.getChildAdapterPosition(view);

                if(pos == (lastQueried + queryinterval - 1)){
                    lastQueried += queryinterval;
                    fetchVidList(lastQueried, queryinterval);
                }

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

            }
        });

        //bind to service
        bindService(
                scvIntent,
                adapter.svcConnection,
                BIND_AUTO_CREATE
        );


        ImageButton optionsButton = findViewById(R.id.options_button);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditSettingsDialog(getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE));
            }
        });

        ImageButton refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //clear view
                ((GBVidsItemRecyclerViewAdapter)vidView.getAdapter()).clearData();
                fetchVidList(0, queryinterval);
            }
        });

        //get the first set of results
        fetchVidList(0, queryinterval);
    }

    public void showEditSettingsDialog(final SharedPreferences prefs){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View v = getLayoutInflater().inflate(R.layout.gb_api_dialog_layout, null);
        dialogBuilder
                .setTitle(R.string.action_settings)
                .setView(v)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        prefs.edit().putString(Constants.API_KEY, ((EditText)v.findViewById(R.id.apiEditText)).getText().toString()).apply();
                        ((GBVidsItemRecyclerViewAdapter)vidView.getAdapter()).clearData();
                        fetchVidList(0, queryinterval);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //? do something here?
                    }
                })
                .show();
    }

    private void fetchVidList(int offset, int num){
        //check for api key, display alert dialog if not present
        SharedPreferences prefs = this.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        if(!prefs.contains(Constants.API_KEY)){
            showEditSettingsDialog(prefs);
        } else {

            RestRequestor.getVidListQuery(
                    vidListRestCallback,
                    prefs.getString(Constants.API_KEY, null),
                    num,
                    offset,
                    NetUtils.VidQuality.ALL
            );
        }


    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

/*    @Override
    protected void onStart() {
        RestRequestor.startup();
        super.onStart();
    }

    @Override
    protected void onStop() {
        RestRequestor.shutdown();
        super.onStop();
    }*/

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        unbindService(adapter.svcConnection);
        super.onDestroy();
    }

    private VidListRestRequest.RestCallback vidListRestCallback = new VidListRestRequest.RestCallback() {

        @Override
        public void OnResult(VidListRestRequest.RestResult result) {

            if(result instanceof VidListRestRequest.VidListResult){
                final VidListRestRequest.VidListResult vResult = (VidListRestRequest.VidListResult) result;

                if(vResult.resultCode == NetUtils.ResultCode.OK.getCode()){
                    if(vidView != null && vidView.getAdapter() != null && vidView.getAdapter() instanceof GBVidsItemRecyclerViewAdapter){
                        final GBVidsItemRecyclerViewAdapter adapter = (GBVidsItemRecyclerViewAdapter)vidView.getAdapter();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                adapter.addData(vResult.vidList);
                            }
                        });

                    }
                } else {
                    //TODO error checking
                }
            }
        }
    };


}