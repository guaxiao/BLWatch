package com.tau.blwatch;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class SerializableDevice implements Serializable{
    private BluetoothDevice device;

    public BluetoothDevice getDevice(){
        return device;
    }

    public SerializableDevice setDevice(BluetoothDevice device){
        this.device = device;
        return this;
    }
}
