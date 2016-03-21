package com.tau.blwatch.fragment;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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
import com.tau.blwatch.MainActivity;
import com.tau.blwatch.R;
import com.tau.blwatch.callBack.DataBaseTranslator;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.fragment.base.BaseFragment;
import com.tau.blwatch.util.BluetoothLeService;
import com.tau.blwatch.util.DataBaseSelectHelper;
import com.tau.blwatch.util.FormKeyHelper;
import com.tau.blwatch.util.UserEntity;
import com.tau.blwatch.util.UrlHelper;
import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.listeners.RealtimeStepsNotifyListener;

import org.xutils.http.RequestParams;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import dmax.dialog.SpotsDialog;

//TODO：更改数据上传服务器的逻辑
//TODO：将此页面分离成若干针对不同设备的页面

//@ContentView(R.layout.fragment_walk)
public class WalkFragment extends BaseFragment {
    private static final String	TAG_XUtils		= "fragment_walk-xUtils3";
    private static final String	TAG_MIBAND		= "miband_pangliang";

    private static final String DEVICE_PTWATCH = "PTWATCH";
    private static final String DEVICE_NSW04 = "NS-W04";
    private static final String DEVICE_MI1S = "MI1S";

    private FragmentJumpController mFragmentJumpController;
    private DataBaseTranslator mDataBaseTranslator;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>>
            mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();// 蓝牙协议特征
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;//设备链接状态，默认未连接

    private MiBand MiBandConnectHelper = new MiBand(getActivity());

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

    private static TextView mCircleCenterTextView, mCircleBottomTextView, mFragmentBottomTextView;
    private static TextView deRecvDeviceName, deRecvDeviceAdd;

    private DecimalFormat mDecimalFormat = new DecimalFormat(".00");

//    @ViewInject(R.id.debug_updata)
    private TextView mDebugUpdata;
//    @ViewInject(R.id.debug_step_cache)
    private TextView mDebugStepCache;
//    @ViewInject(R.id.debug_maxheart_cache)
    private TextView mDebugMaxHeart;
//    @ViewInject(R.id.debug_avgheart_cache)
    private TextView mDebugAvgHeart;
//    @ViewInject(R.id.debug_minheart_cache)
    private TextView mDebugMinHeart;

    //TODO：修改数值缓存的逻辑，包括何时重置，何时计算，何时上传等
    private static int countHeart;
    private static float cacheAvgHeart,cacheMaxHeart,cacheMinHeart;
    private static int cacheStep;

    private static final int Message_Circle_Center = 1;
    private static final int Message_Circle_Bottom = 2;
    private static final int Message_Fragment_Bottom = 3;

    static class ViewControlHandler extends Handler{
        public ViewControlHandler(Looper looper)    {super(looper);}

        @Override
        public void handleMessage(Message msg) {
            String text = (String)msg.obj;

            switch (msg.what) {
                case Message_Circle_Center:
                    mCircleCenterTextView.setText(text);
                    break;
                case Message_Circle_Bottom:
                    mCircleBottomTextView.setText(text);
                    break;
                case Message_Fragment_Bottom:
                    mFragmentBottomTextView.setText(text);
                    break;
            }
        }
    }

    private static ViewControlHandler  mViewControlHandler = new ViewControlHandler(Looper.getMainLooper());

    //TODO：将蓝牙手表PTWATCH的监听服务与GATT广播接收器包装成独立的类进行调用

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
            mBluetoothLeService.connect(mBluetoothDevice.getName());
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

    public WalkFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentJumpController = (FragmentJumpController) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FragmentJumpController");
        }
        try {
            mDataBaseTranslator = (DataBaseTranslator) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DataBaseTranslator");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * 之后使用SharedPreferences配合设置菜单改变默认值
         */
        countTempConf[0] = "04000c01000202";//心跳-2秒钟
        countTempConf[1] = "03002E000101"; //步数-1分钟
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_walk, container, false);

        //得到容器ViewGroup
//        mBaseLayout = (FrameLayout) fragmentView.findViewById(R.id.baseLayout_walk);

        //定义圆心TextView
        mCircleCenterTextView = (TextView) fragmentView.findViewById(R.id.textCircleCenter);
        //定义圆周底部TextView
        mCircleBottomTextView = (TextView) fragmentView.findViewById(R.id.textCircleBottom);
        //定义页面底部TextView
        mFragmentBottomTextView = (TextView) fragmentView.findViewById(R.id.textFragmentBottom);

        mDebugUpdata = (TextView) fragmentView.findViewById(R.id.debug_update);
        mDebugUpdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestParams requestParams = new RequestParams(UrlHelper.upload_data_url);
                requestParams.addHeader("ContentType", "application/json");
                requestParams.addBodyParameter(FormKeyHelper.action, UrlHelper.upload_heart_action);
                requestParams.addBodyParameter(FormKeyHelper.upload_user_id, mUserInfo.getUserID());
                requestParams.addBodyParameter(FormKeyHelper.bluetooth_address, mBluetoothDevice.getAddress());
                requestParams.addBodyParameter(FormKeyHelper.upload_heart, Float.toString(cacheAvgHeart));
            }
        });

        //右下重新连接按钮
        mFab_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBluetoothDevice == null){
                    //跳转至设备列表界面
                    mFragmentJumpController.onJumpToDeviceList(mUserInfo, null, mCreateFlag, this.getClass());
                    Log.d("FragmentWList","From " + this.getClass().getSimpleName());
                }else{
                    if (mBluetoothDevice.getName() != null)
                        switch (mBluetoothDevice.getName()){
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
                            case DEVICE_MI1S:
                                // TODO: 添加对小米手环的重新连接方法
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

                if(mBluetoothDevice == null){
                    //跳转至设备列表界面
                    mFragmentJumpController.onJumpToDeviceList(mUserInfo, null, mCreateFlag, this.getClass());
                    Log.d("FragmentWList","From " + this.getClass().getSimpleName());
                }else{
                    switch (mBluetoothDevice.getName()){
                        case DEVICE_NSW04:
                            break;
                        case DEVICE_PTWATCH:
                            //命令设备停止上传数据
                            BLEUploadData(countShutDown);
                            break;
                        case DEVICE_MI1S:
                            if (MiBandConnectHelper.getDevice() != null) {
                                // TODO: 添加对小米手环的断开连接方法
                                MiBandConnectHelper.disableRealtimeStepsNotify();
//                                MiBandConnectHelper.disableSensorDataNotify();
                            } else{
                                //跳转至设备列表界面
                                mFragmentJumpController.onJumpToDeviceList(mUserInfo, null, mCreateFlag, this.getClass());
                                Log.d("FragmentWList","From " + this.getClass().getSimpleName());
                            }
                            break;
                    }
                    //跳转至历史统计界面
                    mFragmentJumpController.onJumpToHistory(mUserInfo, mBluetoothDevice, mCreateFlag, this.getClass());
                }
            }
        });


         //从设备列表选择了设备跳转而来，则进行自动连接
        if(mBluetoothDevice != null && mLastFragment != null
                && mLastFragment.equals(WalkFragment.class)){
            isAtomConnect = true;
            mFab_bottom_stop.show();
            Toast.makeText(getActivity(), "Data Exchange Atom", Toast.LENGTH_SHORT).show();
//            Snackbar.make(fragmentView, "Data Uploading Atom", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
        }

        //设置左下角的设备名与设备地址显示TextView {debug}
        deRecvDeviceName = (TextView) fragmentView.findViewById(R.id.textRecvDeviceName);
        deRecvDeviceAdd = (TextView) fragmentView.findViewById(R.id.textRecvDeviceAdd);

        if(mBluetoothDevice == null){
            Log.d("WalkFragment","mDevice==null");
            deRecvDeviceName.setText("");
            deRecvDeviceAdd.setText("");
        }else{
            if(mBluetoothDevice.getName() == null)
                deRecvDeviceName.setText(R.string.unknown_device);
            else
                deRecvDeviceName.setText(mBluetoothDevice.getName());

            if(mBluetoothDevice.getAddress() == null)
                deRecvDeviceAdd.setText(R.string.unknown_device_address);
            else
                deRecvDeviceAdd.setText(mBluetoothDevice.getAddress());
        }

        //根据连接设备的类型不同启动相应的蓝牙连接协议
        if(mBluetoothDevice != null){
            switch (mBluetoothDevice.getName()){
                case DEVICE_PTWATCH:
                    Log.d("onCreate", DEVICE_PTWATCH);
                    //第一次与设备通讯前，更新UI至等待状态
                    if(isAtomConnect){
                        mCircleCenterTextView.setText(R.string.step_counting);
                        mFragmentBottomTextView.setText(R.string.heart_calculating);
                    }
                    //启动手表的蓝牙服务
                    Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
                    getActivity().bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
                    Log.d("mGattCharacteristics", mGattCharacteristics.size() + "");
                    break;

                case DEVICE_NSW04:
                    Log.d("onCreate", DEVICE_NSW04);
                    //第一次与设备通讯前，更新UI至等待状态
                    if(isAtomConnect){
                        mCircleCenterTextView.setText(R.string.weight_counting);
                        mFragmentBottomTextView.setText(R.string.weight_counting_info);
                    }
                    // 初始化蓝牙连接服务助手
                    TiZhiGattAttributesHelper.initialize(getActivity());
                    //启动蓝牙连接线程
                    TZGattThread mTZGattThread = new TZGattThread();
                    mTZGattThread.start();
                    break;

                case DEVICE_MI1S:
                    Log.d("onCreate", DEVICE_MI1S);
                    //第一次与设备通讯前，更新UI至等待状态
                    if(isAtomConnect){
                        mCircleCenterTextView.setText(R.string.step_counting);
                        mFragmentBottomTextView.setText(R.string.mi_band_info);
                    }
                    //设置dialog提示信息
                    mChartLoadingDialog = new SpotsDialog(getActivity(),getString(R.string.wait_mi_band));
                    mChartLoadingDialog.show();
                    // 初始化小米手环蓝牙连接助手
//                    MiBandConnectHelper.initialize(getActivity());
                    // 连接小米手环
                    MiBandConnectHelper.connect(mBluetoothDevice, new ActionCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            Log.d(TAG_MIBAND, "connect success");
                            setMiBandListeners();
                            mChartLoadingDialog.dismiss();
                        }

                        @Override
                        public void onFail(int errorCode, String msg) {
                            Log.d(TAG_MIBAND, "connect fail, code:" + errorCode + ",mgs:" + msg);
                        }
                    });
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

        if(mBluetoothDevice != null && mBluetoothDevice.getName().equals(DEVICE_PTWATCH)){
            //启动广播接收器
            getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(mBluetoothDevice.getAddress());
                Log.d("WalkFragment", "Connect request result=" + result);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentJumpController = null;  //注销设备列表回调
        mDataBaseTranslator = null;  //注销SQL-IO回调

        MiBandConnectHelper = null;
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
                    mFragmentBottomTextView.setText(R.string.heart_calculating);
                else {
                    mFragmentBottomTextView.setText(data);  //设置UI
                    try{
                        int heartValue = Integer.parseInt(data);
                        countHeart ++;
                        cacheAvgHeart = (cacheAvgHeart * (countHeart - 1) + heartValue) / countHeart;
                        if (heartValue > cacheMaxHeart)
                            cacheMaxHeart = heartValue;
                        if (heartValue < cacheMinHeart)
                            cacheMinHeart = heartValue;

                        mDebugAvgHeart.setText(Float.toString(cacheAvgHeart));
                        mDebugMaxHeart.setText(Float.toString(cacheMaxHeart));
                        mDebugMinHeart.setText(Float.toString(cacheMinHeart));

                        mDataBaseTranslator.onSendHeartToDB(
                                heartValue, simulateMaxHeart, simulateMinHeart); //通过回调经由activity上传数据库
                    }catch (java.lang.NumberFormatException e){
                        Log.d("IntegerCatch","data");//将不是数字的报文忽略
                    }
                }
                Log.i("mDataHeart",data);

            } else {    //步数报文
                if(data.equals("0"))
                    mCircleCenterTextView.setText(R.string.step_counting);
                else{
                    mCircleCenterTextView.setText(data);    //设置UI
                    try{
                        int stepValue = Integer.parseInt(data);
                        if(stepValue >= cacheStep)
                            cacheStep = stepValue;
                        else
                            Log.d("StepCount","Next Part");

                        mDebugStepCache.setText(Integer.toString(cacheStep));

                        mDataBaseTranslator.onSendStepToDB(Integer.parseInt(data)); //通过回调经由activity上传数据库
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
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
                        mBluetoothLeService.setCharacteristicNotification(characteristic, data, true);
                }
            }
        });

        mlBLEUploadDataThread.start();
    }

    // 遍历GATT服务
    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.d("WalkFragment", "displayGattServices");
        if (gattServices == null)
            return;
        String uuid;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals("0000180d-0000-1000-8000-00805f9b34fb")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                        .getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                }
                mGattCharacteristics.add(charas);
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

    class TZGattThread extends Thread {
        @Override
        public void run() {
            boolean isInitializing = true;
            while (isInitializing) {
                try {
                    // 连接蓝牙
                    // connBleTiZhi(当前的Activity，可为null, 连接超时时间, 蓝牙回调)
                    TiZhiGattAttributesHelper.getInstance().connBleTiZhi(null, 60 * 1000,
                            new ITiZhiBleGattCallbackHelper() {
                                /**
                                 * 接收到解析完的数据
                                 */
                                @Override
                                public void onTiZhiDataReceived(final TiZhiData data) {
                                    final float formattedWeight = (int) (data.getUserWeight() * 100) / 100F;
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mCircleCenterTextView.setText(Float.toString(formattedWeight));
                                        }
                                    });

                                    Log.d("onTiZhiDataReceived", data.toString());

                                    mDataBaseTranslator.onSendWeightToDB(formattedWeight);
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
                } catch (java.lang.NullPointerException NPE) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException InterE) {
                        InterE.printStackTrace();
                    }
                }
            }
        }
    }

    private void setMiBandListeners(){
        Log.d(TAG_MIBAND, "pair success");
        // 设置断开监听器, 方便在设备断开的时候进行重连或者别的处理
        MiBandConnectHelper.setDisconnectedListener(new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG_MIBAND, "connect break");
            }
        });

//                //获取普通通知, data一般len=1, 值为通知类型, 类型暂未收集
//                MiBandConnectHelper.setNormalNotifyListener(new NotifyListener() {
//
//                    @Override
//                    public void onNotify(byte[] data) {
//                        Log.d(TAG_MIBAND, "NormalNotifyListener:" + Arrays.toString(data));
//                    }
//                });

        // 获取实时步数通知, 设置好后, 摇晃手环(需要持续摇动10-20下才会触发), 会实时收到当天总步数通知，需要两步:
        // 1.设置监听器
        MiBandConnectHelper.setRealtimeStepsNotifyListener(new RealtimeStepsNotifyListener() {

            @Override
            public void onNotify(int steps) {
                Log.d(TAG_MIBAND, "RealTimeStepsNotifyListener:" + steps);
                Message message = new Message();
                message.what = Message_Circle_Center;
                message.obj = String.valueOf(steps);
                mViewControlHandler.sendMessage(message);
            }
        });

        // 2.开启通知
        MiBandConnectHelper.enableRealtimeStepsNotify();
    }

    //-----------------------------------------interface--------------------------------------------

    /**
     * 回调：与数据库进行交互
     */
    public interface OnSqlIOCallBack {

    }
}
