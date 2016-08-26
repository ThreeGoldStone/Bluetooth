package com.djl.bluetooth;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.djl.bluetooth.bluetoothhandler.BluetoothOperation;

import static com.djl.bluetooth.bluetoothhandler.BluetoothOperation.WHAT_CONNECT;

public class MainActivity extends AppCompatActivity {
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_CONNECT:
                    break;
//                case W

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new BluetoothOperation(this, mHandler);
    }
}
