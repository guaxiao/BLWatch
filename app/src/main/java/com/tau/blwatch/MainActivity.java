package com.tau.blwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.ns.nsbletizhilib.TiZhiGattAttributesHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                HistoryFragment.OnJumpToOtherFragmentCallBack,
                HistoryFragment.OnSelectDataBaseCallBack,
                WalkFragment.OnJumpToOtherFragmentCallBack,
                WalkFragment.OnSqlIOCallBack,
                DeviceListFragment.OnChooseLeDeviceCallBack{

    //fragment的本地对象
    private static WalkFragment mWalkFragment = new WalkFragment();
    private static HistoryFragment mHistoryFragment = new HistoryFragment();
    private static DeviceListFragment mDeviceListFragment = new DeviceListFragment();

    public static final String NAME_WalkFragment_TAB = "WalkFragment";
    public static final String NAME_HistoryFragment_TAB = "HistoryFragment";
    public static final String NAME_DeviceListFragment_TAB = "DeviceListFragment";
    public static final String NAME_DeviceListFragment_JUMP = "DeviceListFragmentJump";

    //fragment管理器
    private FragmentManager mFragmentManager = getSupportFragmentManager();

    //app内的通信信息 <基于fragment工厂模式化>
    private String mUserInfo;
    private String mDeviceName;
    private String mDeviceAdd;
    private String lastFragment = "";
    private String unusedString = "";

    private FloatingActionButton mFab_bottom,  mFab_top, mFab_bottom_stop;

    //来自 BluetoothAdapter.ACTION_REQUEST_ENABLE 弹出窗口的activity回退请求码
    private static final int REQUEST_ENABLE_BT = 65537;

    private HistoryDBHelper mHistoryDBHelper;
    private SQLiteDatabase mHistoryDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * 以下数据库初始化操作可能需要另开线程
         */
        //打开或创建HistoryData.db数据库
//        mHistoryDatabase = openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
//        mHistoryDatabase.close();

        mHistoryDBHelper = new HistoryDBHelper(this);
        //在此HistoryDBHelper的源码中自动调用了openOrCreateDatabase，无需担心数据库未建立的情况
        mHistoryDatabase = mHistoryDBHelper.getWritableDatabase();


        //定义全局View
        setContentView(R.layout.activity_main);
        //定义ToolBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //侧滑容器布局，将内容填充入DrawerLayout来给侧滑菜单填充内容
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        //将DrawerLayout与ActionBar聚合的功能类
        /*
         --实现以下接口以同步此对象的状态--
         onConfigurationChanged
         onOptionsItemSelected [ok]
         */
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //填充进DrawerLayout，用作导航菜单的框架
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //定义浮动按钮
        mFab_bottom = (FloatingActionButton) findViewById(R.id.fab_bottom);
        mFab_top = (FloatingActionButton) findViewById(R.id.fab_top);
        mFab_bottom_stop = (FloatingActionButton) findViewById(R.id.fab_bottom_stop);


        //初始化fragment
        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, WalkFragment
                        .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                .commit();
        //设置初始化actionBar标题
        setTitle(R.string.device_null);

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try{
            unregisterReceiver(mWalkFragment.mGattUpdateReceiver);  //注销广播监听
            unbindService(mWalkFragment.mServiceConnection); //注销服务
            TiZhiGattAttributesHelper.terminate();  //注销体脂秤蓝牙连接服务
        }catch (IllegalArgumentException e){
            Log.d("deviceChange","mGattUpdateReceiver | mServiceConnection | TiZhiGattAttributesHelper is NULL");
        }catch (NullPointerException e){
            Log.d("deviceChange","TiZhiGattAttributesHelper is NULL");
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 菜单栏的构建 --在此添加菜单项目--
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //菜单栏的监听 --在此添加菜单项目的事件--
        if (id == R.id.action_settings) {   //设置界面
            return true;
        }else if(id == R.id.action_reset_db) {  //重置数据库
            mHistoryDBHelper.resetTable();
            return true;
        }else if(id == R.id.action_simulation_db){  //模拟注入数据
            mHistoryDBHelper.simulateData();
            Log.d("simData","click");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //侧滑菜单框架的监听 --在此添加侧滑菜单功能事件--
        if (id == R.id.nav_walk) {//进入WALK页面
            mFragmentManager.beginTransaction()
                    .replace(R.id.mainFrame, WalkFragment
                            .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                    .commit();
            //设置actionBar标题
            if(mDeviceName == null || mDeviceAdd == null)
                setTitle(R.string.device_null);
            else
                setTitle(mDeviceName);

            Log.d("ItemSelected", getVisibleFragment().toString());

        } else if (id == R.id.nav_history) { //进入HISTORY页面
            mFragmentManager.beginTransaction()
                    .replace(R.id.mainFrame, HistoryFragment
                            .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                    .commit();
            //设置actionBar标题
            setTitle(R.string.nav_history_title);

            Log.d("ItemSelected", getVisibleFragment().toString());

        } else if (id == R.id.nav_watch_list) { //进入WATCHLIST界面
            mFragmentManager.beginTransaction()
                    .replace(R.id.mainFrame, DeviceListFragment
                            .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                    .commit();
            //设置actionBar标题
            setTitle(R.string.nav_watch_list_title);

            Log.d("ItemSelected", getVisibleFragment().toString());

        } else if (id == R.id.nav_manage) {
            Toast.makeText(this, " 开发中...",
                    Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_share) {
            Toast.makeText(this, " 开发中...",
                    Toast.LENGTH_SHORT).show();
        }
//        } else if (id == R.id.nav_send) {
//            Toast.makeText(this, " 开发中...",
//                    Toast.LENGTH_SHORT).show();
//        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * 回调实现：跳转WATCHLIST界面
     */
    public void onJumpToDeviceList(String deviceName,String deviceADD){
        mDeviceName = deviceName;
        mDeviceAdd = deviceADD;
        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, DeviceListFragment
                        .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_watch_list_title);
    }

    public void onJumpToHistoryTable(String deviceName,String deviceADD){
        mDeviceName = deviceName;
        mDeviceAdd = deviceADD;
        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, HistoryFragment
                        .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_history_title);
    }

    /**
     * 回调实现：从数据库获取数据
     * @param startTime 起始时间
     * @param numBlock 时间的段数
     * @return
     */
    public HashMap<String,ArrayList<Float>> onSelectData(long startTime, int numBlock, String typeTimeBlock, String tableName){
        Log.d("onSelectHeartData", "start");

        HashMap<String,ArrayList<Float>> pointCollection = new HashMap<>();
        long endTime = startTime;
        switch (typeTimeBlock){
            case HistoryDBHelper.TYPE_BLOCK_HOUR:
                endTime += TimeHelper.MI_SECOND_HOUR * numBlock;
                break;
            case HistoryDBHelper.TYPE_BLOCK_DAY:
                endTime += TimeHelper.MI_SECOND_DAY * numBlock;
                break;
            case HistoryDBHelper.TYPE_BLOCK_WEEK:
                endTime += TimeHelper.MI_SECOND_WEEK * numBlock;
                break;
            case HistoryDBHelper.TYPE_BLOCK_MONTH:
                endTime += TimeHelper.MI_SECOND_MONTH * numBlock;
                break;
        }

        Cursor pointCursor = mHistoryDBHelper.selectData(startTime,endTime,typeTimeBlock,tableName);
        Log.d("DBHelper.selectData",startTime + "," + endTime + "," + typeTimeBlock + "," + tableName);
//        pointCursor.moveToFirst();
        Log.d("pointCursor", "getCount()=" + pointCursor.getCount());
        Log.d("pointCursor", "getColumnCount()=" + pointCursor.getColumnCount());
        while(pointCursor.moveToNext()){//遍历每一行
            for(int j = 0;j < pointCursor.getColumnCount();j++){//遍历每一列
                boolean isDate = false;
                if(pointCursor.getColumnName(j).equals(HistoryDBHelper.ARG_TIME_BLOCK))
                    isDate = true;
                else
                    isDate = false;

                String keyName = HistoryDBHelper.createKeyName(startTime,numBlock,typeTimeBlock,tableName,isDate,pointCursor.getColumnName(j));

                ArrayList<Float> tempList = pointCollection.get(keyName);
                if(tempList == null)
                    tempList = new ArrayList<>();

                if(pointCursor.getCount() != 0){
                    tempList.add(pointCursor.getFloat(j));
                }else
                    tempList.add(0F);

                pointCollection.put(keyName,tempList);
            }
        }
        return pointCollection;
    }

    /**
     * 回调实现:数据库测试函数
     */
    public void onSQLTest(){
        mHistoryDBHelper.SQLTest();
    }

    /**
     * 回调实现：选择LE设备，跳转到DEVICE_LIST
     * @param deviceName LE设备名
     * @param deviceAdd LE设备地址
     */
    public void onChooseLeDevice(String deviceName, String deviceAdd){
        if(deviceAdd != null && mDeviceAdd != null && !deviceAdd.equals(mDeviceAdd)){
            //若选择了非当前设备
            try{
                unregisterReceiver(mWalkFragment.mGattUpdateReceiver);  //注销广播监听
                unbindService(mWalkFragment.mServiceConnection); //注销服务
                TiZhiGattAttributesHelper.terminate();  //注销体脂秤蓝牙连接服务
            }catch (IllegalArgumentException e){
                Log.d("deviceChange","mGattUpdateReceiver | mServiceConnection is NULL");
            }catch (NullPointerException e){
                Log.d("deviceChange","TiZhiGattAttributesHelper is NULL");
            }
        }
        mDeviceName = deviceName;
        mDeviceAdd = deviceAdd;

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, WalkFragment
                        .newInstance(mUserInfo, mDeviceName, mDeviceAdd, NAME_DeviceListFragment_JUMP))
                .commit();
        //设置actionBar标题
        if(mDeviceName == null || mDeviceAdd == null)
            setTitle(R.string.device_null);
        else
            setTitle(mDeviceName);
    }

    /**
     * 回调实现：将心跳数值传至数据库
     * @param avgHeart 平均心率
     * @param maxHeart 最大心率
     * @param minHeart 最小心率
     */
    public void onSendHeartToDB(int avgHeart, int maxHeart, int minHeart){
        long writeTime = new Date().getTime();
        mHistoryDBHelper.insertHeart(writeTime, mDeviceAdd, avgHeart, maxHeart ,minHeart);
    }

    /**
     * 回调实现：将步数数值传至数据库
     * @param countStep 总计步数
     */
    public void onSendStepToDB(int countStep){
        long writeTime = new Date().getTime();
        mHistoryDBHelper.insertStep(writeTime, mDeviceAdd, countStep);
    }

    /**
     * 回调实现：将体重数值传至数据库
     * @param countWeight
     */
    public void onSendWeightToDB(double countWeight){
        long writeTime = new Date().getTime();
        mHistoryDBHelper.insertWeight(writeTime, mDeviceAdd, countWeight);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("Main_onActivityResult", "requestCode=" + requestCode + ",resultCode=" + resultCode);
        // 发起源-DeviceListFragment.onResume()
        // 若用户选择不启用蓝牙
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            mFragmentManager.beginTransaction()
                    .replace(R.id.mainFrame, WalkFragment
                            .newInstance(mUserInfo, mDeviceName, mDeviceAdd, lastFragment))
                    .commit();
            setTitle(R.string.nav_walk_title);
        }
    }

    public Fragment getVisibleFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }
}
