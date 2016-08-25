package com.djl.bluetooth;


import android.bluetooth.BluetoothManager;
import android.content.Context;

/**
 * 蓝牙操作封装
 * Created by DJl on 2016/8/25.
 * email:1554068430@qq.com
 */

public interface IBluetoothOperation {
    void openBluetooth();

    void scanBluetoothDevice(ScanCallBack callBack);

    void connect(BluetoothDeviceConfig config, ConnectionCallback callback);

    void send(byte[] datum);

    void onReceive(byte[] datum);

    interface ScanCallBack {
    }

    interface ConnectionCallback {
    }

    class BluetoothDeviceConfig {
    }

    class BluetoothOperation implements IBluetoothOperation {
        Context mContext;
        private BluetoothManager mBluetoothManager;

        public BluetoothOperation(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public void openBluetooth() {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothManager.getAdapter().enable();

        }

        @Override
        public void scanBluetoothDevice(ScanCallBack callBack) {

        }

        @Override
        public void connect(BluetoothDeviceConfig config, ConnectionCallback callback) {

        }

        @Override
        public void send(byte[] datum) {

        }

        @Override
        public void onReceive(byte[] datum) {

        }
    }


}
