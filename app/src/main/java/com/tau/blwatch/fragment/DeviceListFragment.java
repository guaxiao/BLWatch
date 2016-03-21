package com.tau.blwatch.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ns.nsbletizhilib.TiZhiGattAttributesHelper;
import com.tau.blwatch.R;
import com.tau.blwatch.callBack.BackThreadController;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.fragment.base.BaseListFragment;
import com.tau.blwatch.util.UserEntity;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link FragmentJumpController}
 * and the {@link BackThreadController} interface.
 */

public class DeviceListFragment extends BaseListFragment {

    private FragmentJumpController mFragmentJumpController;
    private BackThreadController mBackThreadController;

    /**
     * 设备列表视图
     */
    private ListView mListView;

    /**
     * 设备列表适配器
     */
    private LeDeviceListAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    /**
     * 指向BluetoothAdapter.ACTION_REQUEST_ENABLE 弹出窗口的activity请求码
     */
    private static final int REQUEST_ENABLE_BT = 1;
    /**
     * 扫描开关的循环周期
     */
    private static final long SCAN_PERIOD = 30000; //30秒

    private TextView mLeScanInfo;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    //另开线程处理数据，以免阻塞UI线程
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //添加蓝牙设备
                            mAdapter.addDevice(device);
                            if(device.getName() == null)
                                Log.i("addDevice","name=null");
                            else
                                Log.i("addDevice",device.getName());
                            //提醒数据观察者，同步数据
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DeviceListFragment() {
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
            mBackThreadController = (BackThreadController) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement BackThreadController");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//            getActivity().finish();
        }

        // 初始化蓝牙适配器
        // 对于API 18 及以上, 使用BluetoothManager得到BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查设备是否具有蓝牙功能
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
//            finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_device_list, container, false);

        //初始化设备列表
        mListView = (ListView) fragmentView.findViewById(android.R.id.list);
        //初始化设备列表adapter
        mAdapter = new LeDeviceListAdapter();
        mListView.setAdapter(mAdapter);

        setListAdapter(mAdapter);

        //定义浮动按钮
        mFab_bottom = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom);
        mFab_top = (FloatingActionButton) getActivity().findViewById(R.id.fab_top);
        mFab_bottom_stop = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom_stop);

        mFab_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //刷新列表
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                scanLeDevice(true);
            }
        });

        //定义更新状态提示文字
        mLeScanInfo = (TextView) fragmentView.findViewById(R.id.text_LeScanInfo);

        return fragmentView;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("DeviceListFragment", "onResume");
        mFab_bottom.show();
        mFab_top.hide();
        mFab_bottom_stop.hide();

        // 确保设备上的蓝牙已经打开。若蓝牙当前未打开，则调用 一个系统activity通知提醒用户是否允许启动蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.putExtra("LastFragment", this.getClass());
            //跳向通知用户开启蓝牙的系统activity，并在其结束时返回当前activity
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //开启蓝牙扫描
        scanLeDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        //关闭蓝牙扫描
        scanLeDevice(false);
        //清空上一次的设备列表
        mAdapter.clear();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBackThreadController = null;
        mFragmentJumpController = null;
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        Log.i("onListItemClick","position");
        //若回调实现正常
        if (null != mBackThreadController && null != mFragmentJumpController) {
            //获取被选择的设备
            final BluetoothDevice device = mAdapter.getDevice(position);
            //若选择的设备不为空
            if (device != null) {
                //若此已选择设备,且设备发生更新时
                if(mBluetoothDevice != null && !device.equals(mBluetoothDevice)){
                    //注销原设备的监听
                    mBackThreadController.onBackThreadDestroy();
                }
                //不论原设备情况如何，均跳转至Walk界面
                mFragmentJumpController.onJumpToWalk(mUserInfo, mBluetoothDevice, mCreateFlag, this.getClass());
            }
        }
    }


    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * 开关启蓝牙扫描的独立线程
     * @param enable 描述扫描的开或关
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) { //开启

            // 确保设备上的蓝牙已经打开。若蓝牙当前未打开，则调用 一个系统activity通知提醒用户是否允许启动蓝牙
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.putExtra("LastFragment", this.getClass());
                //跳向通知用户开启蓝牙的系统activity，并在其结束时返回当前activity
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            // Stops scanning after a pre-defined scan period.在预定义的扫描期过后停止扫描设备
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeStop();
                }
            }, SCAN_PERIOD);
            Log.d("SCAN_PERIOD","counting");
            scanLeStart();
        } else { //关闭
            scanLeStop();
        }

    }

    private void scanLeStart(){
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        //更新UI
        mLeScanInfo.setText(R.string.le_scan_active);
        Log.i("scanLe","scanLeStart()");
//        getActivity().recreate();
    }

    private void scanLeStop(){
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        //更新UI
        mLeScanInfo.setText("");
        Log.i("scanLe", "scanLeStop()");
//        getActivity().recreate();
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    //构建设备列表的adapter的内部类
    private class LeDeviceListAdapter extends BaseAdapter {
        /**
         * 存储获得的蓝牙设备列表
         */
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflater;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflater = getActivity().getLayoutInflater();
        }

        /**
         * 向LeDeviceList中添加新的蓝牙设备
         * @param device    设备信息
         */
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            if(device != null){
                final String deviceName = device.getName();
                if (deviceName != null && deviceName.length() > 0)
                    viewHolder.deviceName.setText(deviceName);
                else
                    viewHolder.deviceName.setText(R.string.unknown_device);

                viewHolder.deviceAddress.setText(device.getAddress());
            }else{
                viewHolder.deviceName.setText(R.string.warning_text);
                viewHolder.deviceAddress.setText(R.string.warning_text);
                Log.e(TAG,"listView get null item");
            }


            return view;
        }
    }
}
