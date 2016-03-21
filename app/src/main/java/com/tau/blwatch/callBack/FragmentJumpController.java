package com.tau.blwatch.callBack;

import android.bluetooth.BluetoothDevice;

import com.tau.blwatch.util.UserEntity;

import java.util.ArrayList;
import java.util.HashMap;

public interface FragmentJumpController {
    void onJumpToDeviceHistory(UserEntity userInfo, BluetoothDevice device,
                               HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

    void onJumpToDeviceList(UserEntity userInfo, BluetoothDevice device,
                            HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

    void onJumpToDeviceTypeList(UserEntity userInfo, BluetoothDevice device,
                                HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

    void onJumpToHistory(UserEntity userInfo, BluetoothDevice device,
                         HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

    void onJumpToLogin(UserEntity userInfo, BluetoothDevice device,
                       HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

    void onJumpToMain(UserEntity userInfo, BluetoothDevice device,
                      HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

    void onJumpToWalk(UserEntity userInfo, BluetoothDevice device,
                      HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment);

}
