package com.monadpad.omgbananas;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;

public class BluetoothConnectFragment extends OMGFragment {

    private View mView;

    private int devicesUsed = 0;

    private Button[] buttons;

    private Channel mChannel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.bluetooth_connect,
                container, false);

        getActivityMembers();
        setup();

        return mView;
    }

    public void setChannel(Channel channel) {
        mChannel = channel;
    }

    private void setup() {
        final TextView statusView = (TextView)mView.findViewById(R.id.bt_status);

        buttons = new Button[]{
                (Button) mView.findViewById(R.id.bt_device_1),
                (Button) mView.findViewById(R.id.bt_device_2),
                (Button) mView.findViewById(R.id.bt_device_3)
        };

        for (BluetoothConnection connection : mBtf.getConnections()) {
            setupNextButton(connection);
        }

        final Activity activity = getActivity();

        final View tryAgain = mView.findViewById(R.id.bt_try_again);
        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToPairedDevices();
            }
        });

        mBtf.whenReady(new BluetoothReadyCallback() {
            @Override
            public void onReady() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusView.setText("Select a Remote:");
                        tryAgain.setVisibility(View.VISIBLE);

                    }
                });

                connectToPairedDevices();


            }
        });



    }

    private void setupNextButton(final BluetoothConnection connection) {
        Button button = buttons[devicesUsed];
        button.setCompoundDrawablesWithIntrinsicBounds(0,
                R.drawable.device_blue, 0, 0);
        button.setEnabled(true);
        button.setText(connection.getDevice().getName());

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mChannel instanceof DrumChannel) {
                    String pattern = getPatternString();
                    Log.d("MGH bluetooth pattern", pattern);
                    connection.writeString("LAUNCH_DRUMPAD=" + pattern + ";");

                }
                else {
                    connection.writeString("LAUNCH_FRETBOARD=" +
                            mChannel.getLowNote() + "," + mChannel.getHighNote() + "," +
                            mChannel.getOctave() + ";");
                }

                //used to setup the data callback here, now it's channel independent

                getActivity().getFragmentManager().popBackStack();


            }
        });
        devicesUsed++;

    }

    private void connectToPairedDevices() {
        final TextView logView = (TextView)mView.findViewById(R.id.bluetooth_log);

        final Activity activity = getActivity();
        mBtf.connectToPairedDevices(new BluetoothConnectCallback() {
            @Override
            public void newStatus(final String status) {

                /*activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //logView.append("\n");
                        //logView.append(status);
                        Log.d("MGH bt connect callback", status);
                    }
                });*/

            }


            @Override
            public void onConnected(final BluetoothConnection connection) {

                setupDatacallBackForConnection(connection);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupNextButton(connection);
                    }
                });

            }
        });

    }

    public void setupDatacallBackForConnection(BluetoothConnection connection) {
        connection.setDataCallback(new CommandProcessor(connection, mJam));
    }

    public String getPatternString() {

        boolean[][] pattern =((DrumChannel) mChannel).pattern;

        JSONArray json = new JSONArray();
        JSONArray beats;
        for (int itrack = 0; itrack < pattern.length; itrack++) {
            beats = new JSONArray();

            for (int ibeat = 0; ibeat < pattern[itrack].length; ibeat++) {
                beats.put(pattern[itrack][ibeat]);
            }
            json.put(beats);
        }

        return json.toString();

    }
}
