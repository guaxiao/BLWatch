package com.tau.blwatch;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ns.nsbletizhilib.ITiZhiBleGattCallbackHelper;
import com.ns.nsbletizhilib.TiZhiData;
import com.ns.nsbletizhilib.TiZhiGattAttributesHelper;

import java.util.ArrayList;
import java.util.List;

public class WalkFragment extends Fragment {
    private static final String ARG_USERINFO = "userInfo";
    private static final String ARG_LASTFRAGMENT = "lastFragment";
    private static final String ARG_DEVICE_SER = "deviceSerializable";

    private String mUserInfo;
    private String mLastFragment;
    private SerializableDevice mSerializableDevice;
    private BluetoothDevice mDevice;

    private static final String DEVICE_PTWATCH = "PTWATCH";
    private static final String DEVICE_NSW04 = "NS-W04";

    private OnJumpToOtherFragmentCallBack mJumpCallBack;
    private OnSqlIOCallBack mSqlIOCallBack;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>>
            mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();// 蓝牙协议特征
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean mConnected = false;//设备链接状态，默认未连接
    /**
     * 停止同步
     */
    private static final String[] countShutDown =
            {"04"+"00"+"0c"+"00"+"00"+"00"+"00",
                    "03"+"00"+"2e"+"00"+"00"+"00"};
    private static final int simulateMaxHeart = 180,simulateMinHeart = 65;

    private boolean isAtomConnect = true; //app会在连接到设备后自动上传默认的设置

    private String[] countTempConf = new String[2] ;//存储设备上传频率设置的列表
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

//    private FrameLayout mBaseLayout;
    private FloatingActionButton mFab_bottom, mFab_top, mFab_bottom_stop;
    private TextView mHeartCount,mWalkStep;
    private TextView deRecvDeviceName, deRecvDeviceAdd;

    private int countStep;

    // Code to manage Service lifecycle.
    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            Log.d("mServiceConnection","onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("WalkFragment", "Unable to initialize Bluetooth");
                getActivity().finish();
            }else{
                Log.d("WalkFragment", "initialize mBluetoothLeService");
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            // 连接上设备后并且成功初始化
            mBluetoothLeService.connect(mDevice.getName());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    /**
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device,
     *      This can be a result of read or notification operations.
     */
    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d("WalkFragment","BroadcastReceiver Start");
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {  //已连接到GATT服务端
                mConnected = true;
                Log.d("mGattUpdateReceiver","ACTION_GATT_CONNECTED");
                //更新UI
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {  //未连接GATT服务端
                mConnected = false;
                Log.d("mGattUpdateReceiver","ACTION_GATT_DISCONNECTED");
                //更新UI
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) { //发现GATT服务
                Log.d("mGattUpdateReceiver","ACTION_GATT_SERVICES_DISCOVERED");
                // 在用户界面显示所有的所支持的服务和特征
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { //正接受来自设备的数据
                Log.d("mGattUpdateReceiver","ACTION_DATA_AVAILABLE");
                Log.d("getStringExtra_EXTRA",intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                Log.d("getStringExtra_HorS", intent.getStringExtra(BluetoothLeService.HorS));
                catchData(
                        intent.getStringExtra(BluetoothLeService.EXTRA_DATA),
                        intent.getStringExtra(BluetoothLeService.HorS));
            }
        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userInfo 用户信息，为空或NULL时代表用户未登录.
     * @param device 设备信息的序列化
     * @param lastFragment 跳转源页面.
     * @return A new instance of fragment WalkFragment.
     */
    public static WalkFragment newInstance(String userInfo, BluetoothDevice device, String lastFragment) {
        WalkFragment fragment = new WalkFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERINFO, userInfo);
        args.putSerializable(ARG_DEVICE_SER, new SerializableDevice().setDevice(device));
        args.putString(ARG_LASTFRAGMENT, lastFragment);
        fragment.setArguments(args);
        return fragment;
    }

    public WalkFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mJumpCallBack = (OnJumpToOtherFragmentCallBack) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnJumpToOtherFragmentCallBack");
        }
        try {
            mSqlIOCallBack = (OnSqlIOCallBack) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSqlIOCallBack");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserInfo = getArguments().getString(ARG_USERINFO);
            mSerializableDevice = (SerializableDevice)getArguments().getSerializable(ARG_DEVICE_SER);
            if(mSerializableDevice != null)
                mDevice = mSerializableDevice.getDevice();
            else
                mDevice = null;
            mLastFragment = getArguments().getString(ARG_LASTFRAGMENT);
        }

        /*
         * 之后使用SharedPreferences配合设置菜单改变默认值
         */
        countTempConf[0] = "04000c01000202";//心跳-2秒钟
        countTempConf[1] = "03002E000101"; //步数-1分钟
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.fragment_walk, container,
                false);

        //得到容器ViewGroup
//        mBaseLayout = (FrameLayout) fragmentView.findViewById(R.id.baseLayout_walk);

        //定义步数计数器
        mWalkStep = (TextView) fragmentView.findViewById(R.id.textStep);
        //定义心跳计数器
        mHeartCount = (TextView) fragmentView.findViewById(R.id.textHeartCount);

        //定义浮动按钮
        mFab_bottom = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom);
        mFab_top = (FloatingActionButton) getActivity().findViewById(R.id.fab_top);
        mFab_bottom_stop= (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom_stop);

        //右下重新连接按钮
        mFab_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mDevice == null){
                    //跳转至设备列表界面
                    mJumpCallBack.onJumpToDeviceList(null);
                    Log.d("FragmentWList","From " + this.getClass().getSimpleName());
                }else{
                    if (mDevice.getName() != null)
                        switch (mDevice.getName()){
                            case DEVICE_NSW04:
                                //重新与体重秤连接
                                mFab_bottom_stop.show();
                                break;
                            case DEVICE_PTWATCH:
                                //重新上传同步数据
                                BLEUploadData(countTempConf);  //上传同步率数据
                                Log.d("mGattCharacteristics", mGattCharacteristics.size() + "");
                                Toast.makeText(getActivity(), "Data Uploading", Toast.LENGTH_SHORT).show();
                                Log.d("WalkFragment", "updateData");
                                mFab_bottom_stop.show();
                                break;
                        }
                }

            }
        });

        //左下断开连接按钮
        mFab_bottom_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Disconnect Success ", Toast.LENGTH_SHORT).show();

                if(mDevice == null){
                    //跳转至设备列表界面
                    mJumpCallBack.onJumpToDeviceList(null);
                    Log.d("FragmentWList","From " + this.getClass().getSimpleName());
                }else{
                    switch (mDevice.getName()){
                        case DEVICE_NSW04:
                            break;
                        case DEVICE_PTWATCH:
                            //命令设备停止上传数据
                            BLEUploadData(countShutDown);
                            break;
                    }
                    //跳转至历史统计界面
                    mJumpCallBack.onJumpToHistoryTable(mDevice);
                }
            }
        });


         //从设备列表选择了设备跳转而来，则进行自动连接
        if(mDevice != null && mLastFragment != null
                && mLastFragment.equals(MainActivity.NAME_DeviceListFragment_JUMP)){
            isAtomConnect = true;
            mFab_bottom_stop.show();
            Toast.makeText(getActivity(), "Data Exchange Atom", Toast.LENGTH_SHORT).show();
//            Snackbar.make(fragmentView, "Data Uploading Atom", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
        }

        //设置左下角的设备名与设备地址显示TextView {debug}
        deRecvDeviceName = (TextView) fragmentView.findViewById(R.id.textRecvDeviceName);
        deRecvDeviceAdd = (TextView) fragmentView.findViewById(R.id.textRecvDeviceAdd);

        if(mDevice == null){
            Log.d("WalkFragment","mDevice==null");
            deRecvDeviceName.setText("");
            deRecvDeviceAdd.setText("");
        }else{
            if(mDevice.getName() == null)
                deRecvDeviceName.setText(R.string.unknown_device);
            else
                deRecvDeviceName.setText(mDevice.getName());

            if(mDevice.getAddress() == null)
                deRecvDeviceAdd.setText(R.string.unknown_device_address);
            else
                deRecvDeviceAdd.setText(mDevice.getAddress());
        }

        //根据连接设备的类型不同启动相应的蓝牙连接协议
        if(mDevice != null){
            switch (mDevice.getName()){
                case DEVICE_PTWATCH:
                    Log.d("onCreate", DEVICE_PTWATCH);
                    //启动手表的蓝牙服务
                    Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
                    getActivity().bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
                    Log.d("mGattCharacteristics", mGattCharacteristics.size() + "");
                    //第一次与设备通讯前，更新UI至等待状态
                    if(isAtomConnect){
                        mWalkStep.setText(R.string.step_counting);
                        mHeartCount.setText(R.string.heart_calculating);
                    }
                    break;

                case DEVICE_NSW04:
                    Log.d("onCreate", DEVICE_NSW04);
                    // 初始化蓝牙连接服务助手
                    TiZhiGattAttributesHelper.initialize(getActivity());
                    //启动蓝牙连接线程
                    TZGattThread mTZGattThread = new TZGattThread();
                    mTZGattThread.start();
                    //第一次与设备通讯前，更新UI至等待状态
                    if(isAtomConnect){
                        mWalkStep.setText(R.string.weight_counting);
                        mHeartCount.setText(R.string.weight_counting_info);
                    }
                    break;
            }
        }
        return fragmentView;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("WalkFragment", "onResume");
        //第一次与设备通讯前，更新UI至等待状态
        mFab_bottom.show();
        mFab_top.hide();
        mFab_bottom_stop.hide();

        if(mDevice != null && mDevice.getName().equals(DEVICE_PTWATCH)){
            //启动广播接收器
            getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(mDevice.getAddress());
                Log.d("WalkFragment", "Connect request result=" + result);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mJumpCallBack = null;  //注销设备列表回调
        mSqlIOCallBack = null;  //注销SQL-IO回调
        mFab_top = null;    //注销在fragment下的浮动按钮
        mFab_bottom = null;
        mFab_bottom_stop= null;
    }

    /**
     * 通过数据标志位判断数据的类型，并在UI界面进行赋值、上传至数据库
     * @param data 数据报
     * @param flag 数据标志位
     */
    private void catchData(final String data, String flag) {
        if (data != null) {
            Log.i("catchData_flag",flag);
            Log.i("catchData_data",data);

            if (flag.equals("H")) { //心跳报文
                if(data.equals("0"))
                    mHeartCount.setText(R.string.heart_calculating);
                else {
                    mHeartCount.setText(data);  //设置UI
                    try{
                        mSqlIOCallBack.onSendHeartToDB(
                                Integer.parseInt(data),simulateMaxHeart,simulateMinHeart); //通过回调经由activity上传数据库
                    }catch (java.lang.NumberFormatException e){
                        Log.d("IntegerCatch","data");//将不是数字的报文忽略
                    }
                }
                Log.i("mDataHeart",data);

            } else {    //步数报文
                if(data.equals("0"))
                    mWalkStep.setText(R.string.step_counting);
                else{
                    mWalkStep.setText(data);    //设置UI
                    try{
                        mSqlIOCallBack.onSendStepToDB(Integer.parseInt(data)); //通过回调经由activity上传数据库
                    }catch (java.lang.NumberFormatException e){
                        Log.d("IntegerCatch","data");//将不是数字的报文忽略
                    }
                }

                Log.i("mDataStep",data);
            }
        }
    }

    /**
     * 上传设定数据
     * @param data 一串预定的十六进制字符
     */
    private void BLEUploadData(final String[] data){
        Thread mlBLEUploadDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i >= 0; i--) {
                    final BluetoothGattCharacteristic characteristic = mGattCharacteristics
                            .get(0).get(i);
                    final int charaProp = characteristic.getProperties();

                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a
                        // characteristic, clear
                        // it first so it doesn't update the data field on the
                        // user interface.
				/*
				 * if (mNotifyCharacteristic != null) {
				 * mBluetoothLeService.setCharacteristicNotification(
				 * mNotifyCharacteristic, false); mNotifyCharacteristic
				 * = null; }
				 */
                        mBluetoothLeService.readCharacteristic(characteristic);
                    }

                    //上传设定数据
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, data, true);
                    }
                }
            }
        });

        mlBLEUploadDataThread.start();
    }

    // 遍历GATT服务
    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.d("WalkFragment","displayGattServices");
        if (gattServices == null)
            return;
        String uuid;
//        String unknownServiceString = getResources().getString(
//                R.string.unknown_service);
//        String unknownCharaString = getResources().getString(
//                R.string.unknown_characteristic);
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
//        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
//            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            if (uuid.equals("0000180d-0000-1000-8000-00805f9b34fb")) {
//                currentServiceData
//                        .put(LIST_NAME, SampleGattAttributes.lookup(uuid,
//                                unknownServiceString));
//                currentServiceData.put(LIST_UUID, uuid);
//                gattServiceData.add(currentServiceData);

//                ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                        .getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
//                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
//                    uuid = gattCharacteristic.getUuid().toString();
//                    currentCharaData.put(LIST_NAME, SampleGattAttributes
//                            .lookup(uuid, unknownCharaString));
//                    currentCharaData.put(LIST_UUID, uuid);
//                    gattCharacteristicGroupData.add(currentCharaData);
                }
                mGattCharacteristics.add(charas);
//                gattCharacteristicData.add(gattCharacteristicGroupData);
            }
        }

        if(isAtomConnect){
            Log.d("displayGattServices", "updateData");
            BLEUploadData(countTempConf);  //上传同步率数据
            isAtomConnect = false;
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

//    // 16进制转为byte
//    public byte[] hexStringToBytes(String hexString) {
//        if (hexString == null || hexString.equals("")) {
//            return null;
//        }
//        hexString = hexString.toUpperCase();
//        int length = hexString.length() / 2;
//        char[] hexChars = hexString.toCharArray();
//        byte[] d = new byte[length];
//        for (int i = 0; i < length; i++) {
//            int pos = i * 2;
//            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
//        }
//        return d;
//    }
//
//    private byte charToByte(char c) {
//        return (byte) "0123456789ABCDEF".indexOf(c);
//
//    }

    class TZGattThread extends Thread{
        @Override
        public void run(){
            boolean isInitializing = true;
            while(isInitializing){
                try{
                    // 连接蓝牙
                    // connBleTiZhi(当前的Activity，可为null, 连接超时时间, 蓝牙回调)
                    TiZhiGattAttributesHelper.getInstance().connBleTiZhi(null, 60 * 1000,
                            new ITiZhiBleGattCallbackHelper() {
                                /**
                                 * 接收到解析完的数据
                                 */
                                @Override
                                public void onTiZhiDataReceived(final TiZhiData data) {
                                    final float formattedWeight = (int)(data.getUserWeight() * 100) / 100F;
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mWalkStep.setText(Float.toString(formattedWeight));
                                        }
                                    });

                                    Log.d("onTiZhiDataReceived", data.toString());

                                    mSqlIOCallBack.onSendWeightToDB(formattedWeight);
                                }

                                /**
                                 * 蓝牙连接成功回调
                                 */
                                @Override
                                public void onServicesDiscovered(int status) {
                                    Log.d("onServicesDiscovered", status + "");
                                }
                            });
                    isInitializing = false;
                }catch (java.lang.NullPointerException NPE){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException InterE) {
                        InterE.printStackTrace();
                    }
                }
            }
        }
    }
    /**
     * 回调：跳转至设备列表界面
     */
    public interface OnJumpToOtherFragmentCallBack {
        void onJumpToDeviceList(BluetoothDevice device);
        void onJumpToHistoryTable(BluetoothDevice device);
    }

    /**
     * 回调：与数据库进行交互
     */
    public interface OnSqlIOCallBack {
        void onSendHeartToDB(int avgHeart, int maxHeart, int minHeart);
        void onSendStepToDB(int countStep);
        void onSendWeightToDB(double countWeight);
    }

}
