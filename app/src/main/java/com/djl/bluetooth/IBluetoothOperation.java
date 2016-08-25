package com.djl.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * 蓝牙操作封装
 * Created by DJl on 2016/8/25.
 * email:1554068430@qq.com
 */

public interface IBluetoothOperation {
    void openBluetooth();

    void discoverAble();

    void scanBluetoothDevice(ScanCallBack callBack);

    void connect(BluetoothDeviceConfig config, ConnectionCallback callback);

    void send(byte[] datum);

    void onReceive(byte[] datum);

    void disConnect();

    interface ScanCallBack {
        void onFinish();

        void onFindBluetoothDevice(BluetoothDevice device);
    }

    interface ConnectionCallback {
    }

    class BluetoothDeviceConfig {
        public BluetoothDevice device;
        public boolean secure;
    }

    class BluetoothOperation implements IBluetoothOperation {
        Context mContext;
        // Debugging
        private static final String TAG = "BluetoothChatService";

        // Name for the SDP record when creating server socket
        private static final String NAME_SECURE = "BluetoothChatSecure";
        private static final String NAME_INSECURE = "BluetoothChatInsecure";

        // Unique UUID for this application
        private static final UUID MY_UUID_SECURE =
                UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
        private static final UUID MY_UUID_INSECURE =
                UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

        // Member fields
        private final BluetoothAdapter mAdapter;
        private final Handler mHandler;
        private AcceptThread mSecureAcceptThread;
        private AcceptThread mInsecureAcceptThread;
        private ConnectThread mConnectThread;
        private ConnectedThread mConnectedThread;
        private int mState;

        // Constants that indicate the current connection state
        public static final int STATE_NONE = 0;       // we're doing nothing
        public static final int STATE_LISTEN = 1;     // now listening for incoming connections
        public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
        public static final int STATE_CONNECTED = 3;  // now connected to a remote device

        /**
         * The BroadcastReceiver that listens for discovered devices and changes the title when
         * discovery is finished
         */
        private final BroadcastReceiver mReceiver = new BlueToothBroadcastReceiver();
        private ScanCallBack mScallBack;

        public BluetoothOperation(Context mContext, Handler handler) {
            mAdapter = BluetoothAdapter.getDefaultAdapter();
            mHandler = new Handler();
            this.mContext = mContext;
        }

        @Override
        public void openBluetooth() {
        }

        @Override
        public void discoverAble() {
            if (mAdapter.getScanMode() !=
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                mContext.startActivity(discoverableIntent);
            }
        }

        @Override
        public void scanBluetoothDevice(ScanCallBack callBack) {
            mScallBack = callBack;
            // If we're already discovering, stop it
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
            }
            // Request discover from BluetoothAdapter
            mAdapter.startDiscovery();

            // Register for broadcasts when a device is discovered
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(mReceiver, filter);
            // Register for broadcasts when discovery has finished
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);
        }

        public void exit() {
            mContext.unregisterReceiver(mReceiver);
        }

        @Override
        public void connect(BluetoothDeviceConfig config, ConnectionCallback callback) {
            Log.d(TAG, "connect to: " + config.device);

            // Cancel any thread attempting to make a connection
            if (mState == STATE_CONNECTING) {
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(config.device, config.secure);
            mConnectThread.start();
            setState(STATE_CONNECTING);
        }

        private void setState(int state) {
            this.mState = state;
        }

        @Override
        public void send(byte[] datum) {

        }

        @Override
        public void onReceive(byte[] datum) {

        }

        @Override
        public void disConnect() {

        }

        /**
         * This thread runs while listening for incoming connections. It behaves
         * like a server-side client. It runs until a connection is accepted
         * (or until cancelled).
         */
        private class AcceptThread extends Thread {
            // The local server socket
            private final BluetoothServerSocket mmServerSocket;
            private String mSocketType;

            public AcceptThread(boolean secure) {
                BluetoothServerSocket tmp = null;
                mSocketType = secure ? "Secure" : "Insecure";

                // Create a new listening server socket
//                try {
//                    if (secure) {
//                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
//                                MY_UUID_SECURE);
//                    } else {
//                        tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
//                                NAME_INSECURE, MY_UUID_INSECURE);
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
//                }
                mmServerSocket = tmp;
            }

            public void run() {
                Log.d(TAG, "Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this);
                setName("AcceptThread" + mSocketType);

                BluetoothSocket socket = null;

                // Listen to the server socket if we're not connected
                while (mState != STATE_CONNECTED) {
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        socket = mmServerSocket.accept();
                    } catch (IOException e) {
                        Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                        break;
                    }

                    // If a connection was accepted
                    if (socket != null) {
                        synchronized (BluetoothOperation.this) {
                            switch (mState) {
                                case STATE_LISTEN:
                                case STATE_CONNECTING:
                                    // Situation normal. Start the connected thread.
//                                    connected(socket, socket.getRemoteDevice(),
//                                            mSocketType);
                                    break;
                                case STATE_NONE:
                                case STATE_CONNECTED:
                                    // Either not ready or already connected. Terminate new socket.
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Could not close unwanted socket", e);
                                    }
                                    break;
                            }
                        }
                    }
                }
                Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

            }

            public void cancel() {
                Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
                }
            }
        }


        /**
         * This thread runs while attempting to make an outgoing connection
         * with a device. It runs straight through; the connection either
         * succeeds or fails.
         */
        private class ConnectThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final BluetoothDevice mmDevice;
            private String mSocketType;

            public ConnectThread(BluetoothDevice device, boolean secure) {
                mmDevice = device;
                BluetoothSocket tmp = null;
                mSocketType = secure ? "Secure" : "Insecure";

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    if (secure) {
                        tmp = device.createRfcommSocketToServiceRecord(
                                MY_UUID_SECURE);
                    } else {
                        tmp = device.createInsecureRfcommSocketToServiceRecord(
                                MY_UUID_INSECURE);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                }
                mmSocket = tmp;
            }

            public void run() {
                Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
                setName("ConnectThread" + mSocketType);

                // Always cancel discovery because it will slow down a connection
                mAdapter.cancelDiscovery();

                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();
                } catch (IOException e) {
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "unable to close() " + mSocketType +
                                " socket during connection failure", e2);
                    }
//                    connectionFailed();
                    return;
                }

                // Reset the ConnectThread because we're done
                synchronized (BluetoothOperation.this) {
                    mConnectThread = null;
                }

                // Start the connected thread
//                connected(mmSocket, mmDevice, mSocketType);
            }

            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
                }
            }
        }

        /**
         * This thread runs during a connection with a remote device.
         * It handles all incoming and outgoing transmissions.
         */
        private class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket, String socketType) {
                Log.d(TAG, "create ConnectedThread: " + socketType);
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the BluetoothSocket input and output streams
                try {
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "temp sockets not created", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                Log.i(TAG, "BEGIN mConnectedThread");
                byte[] buffer = new byte[1024];
                int bytes;

                // Keep listening to the InputStream while connected
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.read(buffer);

                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
//                        connectionLost();
                        // Start the service over to restart listening mode
//                        BluetoothOperation.this.start();
                        break;
                    }
                }
            }

            /**
             * Write to the connected OutStream.
             *
             * @param buffer The bytes to write
             */
            public void write(byte[] buffer) {
                try {
                    mmOutStream.write(buffer);

                    // Share the sent message back to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Exception during write", e);
                }
            }

            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of connect socket failed", e);
                }
            }
        }


        private class BlueToothBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // If it's already paired, skip it, because it's been listed already
//                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                    }
                    // When discovery is finished, change the Activity title
                    mScallBack.onFindBluetoothDevice(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                    setProgressBarIndeterminateVisibility(false);
//                    setTitle(R.string.select_device);
//                    if (mNewDevicesArrayAdapter.getCount() == 0) {
//                        String noDevices = getResources().getText(R.string.none_found).toString();
//                        mNewDevicesArrayAdapter.add(noDevices);
//                    }
                    mScallBack.onFinish();
                }
            }

        }
    }


}
