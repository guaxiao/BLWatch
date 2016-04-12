package com.tau.blwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import android.widget.Toast;

import com.ns.nsbletizhilib.TiZhiGattAttributesHelper;
import com.tau.blwatch.callBack.BackThreadController;
import com.tau.blwatch.callBack.DataBaseTranslator;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.fragment.DeviceListFragment;
import com.tau.blwatch.fragment.DeviceTypeFragment;
import com.tau.blwatch.fragment.HistoryFragment;
import com.tau.blwatch.fragment.LoginFragment;
import com.tau.blwatch.fragment.MainFragment;
import com.tau.blwatch.fragment.WalkFragment;
import com.tau.blwatch.util.DataBaseSelectHelper;
import com.tau.blwatch.util.HistoryDBHelper;
import com.tau.blwatch.util.UserEntity;
import com.tau.blwatch.util.SharePrefUtil;
import com.tau.blwatch.util.FormKeyHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
            FragmentJumpController,
            BackThreadController,
            DataBaseTranslator{

    private static final String	TAG_MIBAND		= "==[mibandtest]==";


    //fragment的本地对象
    private static WalkFragment mWalkFragment = new WalkFragment();

    //fragment管理器
    private FragmentManager mFragmentManager = getSupportFragmentManager();

    //app内的通信信息 <基于fragment工厂模式化>
    private UserEntity mUserInfo = new UserEntity();
    private BluetoothDevice mDevice;
    protected HashMap<Integer,ArrayList<Integer>> mCreateFlag;
    private Class mLastFragment;

    //来自 BluetoothAdapter.ACTION_REQUEST_ENABLE 弹出窗口的activity回退请求码
    private static final int REQUEST_ENABLE_BT = 65537;

    private DrawerLayout mDrawerLayout;
    private HistoryDBHelper mHistoryDBHelper;

    private boolean isMenuLocked = false;
    private ActionBarDrawerToggle mActionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHistoryDBHelper = new HistoryDBHelper(this);
        //在此HistoryDBHelper的源码中自动调用了openOrCreateDatabase，无需担心数据库未建立的情况
        //mHistoryDatabase = mHistoryDBHelper.getWritableDatabase();
        mHistoryDBHelper.simulateDeviceType();

        //定义全局View
        setContentView(R.layout.activity_main);
        //定义ToolBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //侧滑容器布局，将内容填充入DrawerLayout来给侧滑菜单填充内容
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        //将DrawerLayout与ActionBar聚合的功能类
        /*
         --实现以下接口以同步此对象的状态--
         onConfigurationChanged
         onOptionsItemSelected [ok]
         */
        mActionBarDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);
        mActionBarDrawerToggle.syncState();

        //填充进DrawerLayout，用作导航菜单的框架
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if(SharePrefUtil.getBoolean(this, FormKeyHelper.is_login, false)){ //用户上次已登录
            //跳转到主页
            mUserInfo.setUserID(SharePrefUtil.getString(this, FormKeyHelper.user_id, null));
            mUserInfo.setUserName(SharePrefUtil.getString(this, FormKeyHelper.user_name, null));
            mUserInfo.setUserImgPath(SharePrefUtil.getString(this, FormKeyHelper.user_imageUrl, null));

            onJumpToMain(mUserInfo, mDevice, mCreateFlag, this.getClass());
        }else{  //用户上次未登录
            //锁定侧滑菜单与主菜单
            lockMenus();

            //跳转到登录页面
            onJumpToLogin(mUserInfo, mDevice, mCreateFlag, this.getClass());
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        //销毁后台的服务、广播及其他线程
        onBackThreadDestroy();
    }

    //TODO：加入Fragment回退栈管理
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

        //TODO：寻找更好的屏蔽菜单方法
        //当菜单被锁定时，屏蔽所有的菜单监听
        if(isMenuLocked)
            id = -1;

        //菜单栏的监听 --在此添加菜单项目的事件--
        switch (id){
            case R.id.action_settings:   //设置界面
                return true;
            case R.id.action_reset_db:   //重置数据库
                mHistoryDBHelper.resetTable();
                return true;
            case R.id.action_simulation_db:  //模拟注入数据
                mHistoryDBHelper.simulateData();
                Log.d("simData","click");
                return true;
            case R.id.action_simulation_device: //模拟注入设备历史
                mHistoryDBHelper.simulateDeviceData();
                return true;
            case R.id.action_simulation_device_type: //模拟注入设备类型
                mHistoryDBHelper.simulateDeviceType();
                return true;
            case R.id.action_logout: //注销账户
                if (!SharePrefUtil.getBoolean(this, FormKeyHelper.is_login, false)) //如果缓存数值为未登录或者无此项缓存数值
                    Toast.makeText(this, "未登录", Toast.LENGTH_SHORT).show();
                else {  //若已登录，则置状态为未登录并回退到登录界面
                    SharePrefUtil.saveBoolean(this, FormKeyHelper.is_login, false);
                    onJumpToLogin(null, null, mCreateFlag, MenuItem.class);
                    lockMenus();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //侧滑菜单框架的监听 --在此添加侧滑菜单功能事件--
        switch (item.getItemId()){
            case R.id.nav_home://进入主页面
                onJumpToMain(mUserInfo, mDevice, mCreateFlag, mLastFragment);
                break;

            case R.id.nav_walk://进入Walk页面
                onJumpToWalk(mUserInfo, mDevice, mCreateFlag, mLastFragment);
                break;

            case R.id.nav_history: //进入History页面
                onJumpToHistory(mUserInfo, mDevice, mCreateFlag, mLastFragment);
                break;

            case R.id.nav_watch_list: //进入DeviceList界面
                onJumpToDeviceList(mUserInfo, mDevice, mCreateFlag, mLastFragment);
                break;

            case R.id.nav_manage:
                Toast.makeText(this, " 开发中...",
                        Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_share:
                Toast.makeText(this, " 开发中...",
                        Toast.LENGTH_SHORT).show();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * 锁定侧滑菜单与主菜单
     */
    private void lockMenus(){
        isMenuLocked = true;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mActionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * 将侧滑菜单与主菜单解锁
     */
    private void unlockMenus(){
        isMenuLocked = false;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mActionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    private void notifyFragmentValueChanged(UserEntity userInfo, BluetoothDevice device,
                                            HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        mUserInfo = userInfo;
        mDevice = device;
        mCreateFlag = createFlag;
        mLastFragment = lastFragment;
    }

    //-----------------------------------------callback--------------------------------------------

    /**
     * 回调实现：销毁后台的服务、广播及其他线程
     */
    public void onBackThreadDestroy(){
        try{
            unregisterReceiver(mWalkFragment.mGattUpdateReceiver);  //注销广播监听
        }catch (IllegalArgumentException e){
            Log.d("deviceChange","mGattUpdateReceiver is NULL");
        }

        try{
            unbindService(mWalkFragment.mServiceConnection); //注销服务
        }catch (IllegalArgumentException e){
            Log.d("deviceChange","mServiceConnection is NULL");
        }

        try{
            TiZhiGattAttributesHelper.terminate();  //注销体脂秤蓝牙连接服务
        }catch (IllegalArgumentException | NullPointerException e){
            Log.d("deviceChange","TiZhiGattAttributesHelper is NULL");
        }

//        try{
//            MiBandConnectHelper.disableRealtimeStepsNotify(); //注销小米手环步数监听
//        }catch (NullPointerException | MiBandNotConnectException e){
//            Log.d("deviceChange","MiBandConnectHelper is NULL or MiBand not connected");
//        }
//
//        try{
//            MiBandConnectHelper.disableSensorDataNotify(); //注销小米手环重力感应监听
//        }catch (NullPointerException | MiBandNotConnectException e){
//            Log.d("deviceChange","MiBandConnectHelper is NULL or MiBand not connected");
//        }
    }

    /**
     * 回调实现：跳转DeviceList界面
     */
    public void onJumpToDeviceList(UserEntity userInfo, BluetoothDevice device,
                                   HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        notifyFragmentValueChanged(userInfo, device, createFlag, lastFragment);

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, DeviceListFragment
                        .newInstance(DeviceListFragment.class, mUserInfo, mDevice, mCreateFlag, mLastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_watch_list_title);
    }

    /**
     * 回调实现：跳转History界面
     */
    public void onJumpToHistory(UserEntity userInfo, BluetoothDevice device,
                                HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        notifyFragmentValueChanged(userInfo, device, createFlag, lastFragment);

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, HistoryFragment
                        .newInstance(HistoryFragment.class, mUserInfo, mDevice, mCreateFlag, mLastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_history_title);
    }

    /**
     * 回调实现：跳转Main界面
     */
    public void onJumpToMain(UserEntity userInfo, BluetoothDevice device,
                             HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        notifyFragmentValueChanged(userInfo, device, createFlag, lastFragment);
        if(isMenuLocked) unlockMenus();

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, MainFragment
                .newInstance(MainFragment.class, mUserInfo, mDevice, mCreateFlag, mLastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_main_title);
    }

    /**
     * 回调实现：跳转Walk界面
     */
    public void onJumpToWalk(UserEntity userInfo, BluetoothDevice device,
                             HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment) {
        notifyFragmentValueChanged(userInfo, device, createFlag, lastFragment);

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, WalkFragment
                        .newInstance(WalkFragment.class, mUserInfo, mDevice, mCreateFlag, mLastFragment))
                .commit();
        //设置actionBar标题
        setTitleByDevice(mDevice);
    }

    /**
     * 回调实现：跳转DeviceTypeList界面
     */
    public void onJumpToDeviceTypeList(UserEntity userInfo, BluetoothDevice device,
                                       HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        notifyFragmentValueChanged(userInfo, device, createFlag, lastFragment);

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, DeviceTypeFragment
                        .newInstance(DeviceTypeFragment.class, mUserInfo, mDevice, mCreateFlag, mLastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_deveice_type_list_title);
    }

    /**
     * 回调实现：跳转DeviceHistory界面
     */
    public void onJumpToDeviceHistory(UserEntity userInfo, BluetoothDevice device,
                                      HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        Toast.makeText(this,"JumpToDeviceHistory",Toast.LENGTH_SHORT).show();
    }

    /**
     * 回调实现：跳转Login界面
     */
    public void onJumpToLogin(UserEntity userInfo, BluetoothDevice device,
                              HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment){
        notifyFragmentValueChanged(userInfo, device, createFlag, lastFragment);

        mFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, LoginFragment
                        .newInstance(LoginFragment.class, mUserInfo, mDevice, mCreateFlag, mLastFragment))
                .commit();
        //设置actionBar标题
        setTitle(R.string.nav_login_title);
    }

    /**
     * 回调实现：从数据库获取统计界面有关图表的数据
     * @param startTime 起始时间
     * @param numBlock 时间的段数
     * @return  以DataBaseSelectHelper.createKeyName标准为Key，存储查询到的有效日期组及其对应的结果数据
     */
    public HashMap<String,ArrayList<Float>> onSelectChartData(long startTime, int numBlock, String typeTimeBlock, String tableName){
        DataBaseSelectHelper dataBaseSelectHelper = new DataBaseSelectHelper(mHistoryDBHelper,startTime,numBlock,typeTimeBlock,tableName);
        return dataBaseSelectHelper.getPointCollection();
    }

    /**
     * 回调实现：从数据库获取设备选择界面有关MAC地址范围的数据
     * @param deviceType 设备类型
     * @param tableName 表名
     * @return  deviceType类型的设备所在的MAC地址范围的集合（可能有多个范围），合法的返回值应该总是成对的（也可能为空）
     */
    public ArrayList<String> onSelectMacPair(String deviceType ,String tableName){
        ArrayList<String> macPairList = new ArrayList<>();
        Cursor cursor = mHistoryDBHelper.selectData(deviceType, tableName);

        int startIndex = cursor.getColumnIndex(HistoryDBHelper.INFO_DEVICE_ADD_START);
        int ENDIndex = cursor.getColumnIndex(HistoryDBHelper.INFO_DEVICE_ADD_END);

        while (cursor.moveToNext()){
            macPairList.add(cursor.getString(startIndex));
            macPairList.add(cursor.getString(ENDIndex));
        }

        return macPairList;
    }

    /**
     * 回调实现:数据库测试函数
     */
    public void onSQLTest(){
        mHistoryDBHelper.SQLTest();
    }

    /**
     * 回调实现：将心跳数值传至数据库
     * @param avgHeart 平均心率
     * @param maxHeart 最大心率
     * @param minHeart 最小心率
     */
    public void onSendHeartToDB(int avgHeart, int maxHeart, int minHeart){
        long writeTime = new Date().getTime();
        mHistoryDBHelper.insertHeart(writeTime, mDevice.getAddress(), avgHeart, maxHeart, minHeart);
    }

    /**
     * 回调实现：将步数数值传至数据库
     * @param countStep 总计步数
     */
    public void onSendStepToDB(int countStep){
        long writeTime = new Date().getTime();
        mHistoryDBHelper.insertStep(writeTime, mDevice.getAddress(), countStep);
    }

    /**
     * 回调实现：将体重数值传至数据库
     * @param countWeight   体重数值
     */
    public void onSendWeightToDB(double countWeight){
        long writeTime = new Date().getTime();
        mHistoryDBHelper.insertWeight(writeTime, mDevice.getAddress(), countWeight);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("Main_onActivityResult", "requestCode=" + requestCode + ",resultCode=" + resultCode);
        // 发起源-DeviceListFragment.onResume()
        // 若用户选择不启用蓝牙
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            onJumpToMain(mUserInfo, mDevice, mCreateFlag, DeviceListFragment.class);
        }
    }

    //-----------------------------------------tools------------------------------------------------

    public Fragment getVisibleFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }

    public void setTitleByDevice(BluetoothDevice device){
        if(device == null)
            setTitle(R.string.device_null);
        else{
            if(device.getName() == null)
                setTitle(R.string.unknown_device);
            else
                setTitle(device.getName());
        }
    }
}
