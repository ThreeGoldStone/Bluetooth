package com.djl.bluetooth.bluetoothhandler;


import android.bluetooth.BluetoothDevice;

/**
 * 蓝牙操作封装
 * Created by DJl on 2016/8/25.
 * email:1554068430@qq.com
 */

public interface IBluetoothOperation {
    void openBluetooth();

    void discoverAble();

    void scanBluetoothDevice();

    void connect(BluetoothDeviceConfig config, ConnectionCallback callback);

    void send(byte[] datum);

    void onReceive(byte[] datum);

    void disConnect();



    interface ConnectionCallback {
    }

    class BluetoothDeviceConfig {
        public BluetoothDevice device;
        public boolean secure;
    }


}
