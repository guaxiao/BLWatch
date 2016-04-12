package com.tau.blwatch.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tau.blwatch.R;
import com.tau.blwatch.callBack.BackThreadController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import dmax.dialog.SpotsDialog;

public class HistoryDBHelper extends SQLiteOpenHelper {
    private Context rootContext;

    private final static String DATABASE_NAME = "HistoryData.db";
    private final static int DATABASE_VERSION = 1;
    public final static String HEART_TABLE_NAME = "heart_table";
    public final static String STEP_TABLE_NAME = "step_table";
    public final static String WEIGHT_TABLE_NAME = "weight_table";
    public final static String DEVICE_HISTORY_TABLE_NAME = "device_history_table";
    public final static String DEVICE_TYPE_TABLE_NAME = "device_type_table";

    public final static String INFO_ID = "id";
    public final static String INFO_TIME = "time";
    public final static String INFO_DEVICE_ADD = "device_add";
    public final static String INFO_DEVICE_NAME = "device_name";
    public final static String INFO_DEVICE_TYPE = "device_type";
    public final static String INFO_DEVICE_ADD_START = "device_add_start";
    public final static String INFO_DEVICE_ADD_END = "device_add_end";
    public final static String AVG_HEART = "avg_heart";
    public final static String MAX_HEART = "max_heart";
    public final static String MIN_HEART = "min_heart";
    public final static String VALUE_STEP = "value_step";
    public final static String VALUE_WEIGHT = "value_weight";

    public final static String ARG_TIME_BLOCK = "time_block";

    private final static int SQLITE_DATE_PER_JAVA_DATE = 1000;

    public final static String TYPE_BLOCK_MONTH = "'%Y-%m'";    //年-月
    public final static String TYPE_BLOCK_WEEK = "'%Y-%W'";     //年-周
    public final static String TYPE_BLOCK_DAY = "'%m-%d'";      //月-天
    public final static String TYPE_BLOCK_HOUR = "'%d-%H'";     //日-小时

    //以下为模拟注入数据时使用的量
    private long writeTime = 0L;
    public final static String sDeviceAdd_TPTWATCH = "00:2C:00:00:00:FF";
    public final static String sDeviceAdd_TPTESc = "00:2D:00:00:00:FF";
    public final static String sDeviceAdd_TMI1S = "00:1C:00:00:00:FF";
    public final static String sDeviceAdd_START = "00:00:00:00:00:00";
    public final static String sDeviceAdd_END = "FF:FF:FF:FF:FF:FF";
    public final static long sTestIntervals =  5 * 60 * 1000;   //测试数据数据库计入间隔5分钟
    public final static long sHeartAndStepIntervals = 5 * 60 * 1000;   //心跳数据库计入间隔5分钟
    public final static long sWeightIntervals = 8 * 60 * 60 * 1000; //体重数据库计入间隔8小时
    public final static long sSumTime = (long)365 * 24 * 3600 * 1000; //模拟历史数据注入，从一年前开始
    public final static long sObligateTime = (long)180 * 24 * 3600 * 1000; //模拟未来数据注入，注入到180天后

//    public final static long sSumTime = (long)12 * 3600 * 1000; //模拟注入时间跨度12h
//    public final static long sObligateTime = (long)0 * 24 * 3600 * 1000; //模拟预留注入时间0

    private int sStepBench = 0;//步数基准值

    public final static int sHeartBench = 80,sHeartMax = 180,sHeartMin = 60;    //心跳的基准值及上下限
    private double dHeartAvg = sHeartBench, dHeartMax = sHeartBench, dHeartMin = sHeartBench;   //心跳模拟平均值、最大及最小值

    public final static int sWeightBench = 70, sWeightMax = 105, sWeightMin = 35;//体重的基准值及其震荡函数的上下限
    private double dWeightBench = sWeightBench; //体重模拟值
    private double sWeightParaUp = 1 , sWeightParaDown = 1; //震荡函数的增减因子

    private long sTimeShake = (long)(Math.random() * 100);  //数据库注入时间的震荡参数

    //自带的构造方法
    public HistoryDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                           int version) {
        super(context, name, factory, version);
        rootContext = context;
    }

    //免于dbName与版本号的简易构造方法
    public HistoryDBHelper(Context context) {
        // TODO Auto-generated constructor stub
        this(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //版本变更
    public HistoryDBHelper(Context cxt, int version) {
        this(cxt, DATABASE_NAME, null, version);
    }

    //在调用getReadableDatabase或getWritableDatabase时，若指定数据库不存在，自动调用此方法
    //创建数据库的各个表
    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建表记录心跳
        createTable(db, HEART_TABLE_NAME);
        //创建表记录步数
        createTable(db, STEP_TABLE_NAME);
        //创建表记录体重
        createTable(db, WEIGHT_TABLE_NAME);
        //创建表记录设备历史
        createTable(db, DEVICE_HISTORY_TABLE_NAME);
        //创建表记录设备类型与对应的MAC地址段
        createTable(db, DEVICE_TYPE_TABLE_NAME);
    }

    public void createTable(SQLiteDatabase db, String tableName){
        String sql = "nil";
        switch (tableName){
            case HEART_TABLE_NAME:
                sql = "CREATE TABLE " + HEART_TABLE_NAME + "(" +
                        INFO_ID + " INTEGER " + "primary key autoincrement," +
                        INFO_TIME + " INTEGER ," +
                        INFO_DEVICE_ADD + " TEXT ," +
                        AVG_HEART + " INTEGER ," +
                        MAX_HEART + " INTEGER ," +
                        MIN_HEART + " INTEGER " +");";
                break;
            case STEP_TABLE_NAME:
                sql = "CREATE TABLE " + STEP_TABLE_NAME + "(" +
                        INFO_ID + " INTEGER " + "primary key autoincrement," +
                        INFO_TIME + " INTEGER ," +
                        INFO_DEVICE_ADD + " TEXT ," +
                        VALUE_STEP + " INTEGER " + ");";
                break;
            case WEIGHT_TABLE_NAME:
                sql = "CREATE TABLE " + WEIGHT_TABLE_NAME + "(" +
                        INFO_ID + " INTEGER " + "primary key autoincrement," +
                        INFO_TIME + " INTEGER ," +
                        INFO_DEVICE_ADD + " TEXT ," +
                        VALUE_WEIGHT + " INTEGER " + ");";
                break;
            case DEVICE_HISTORY_TABLE_NAME:
                sql = "CREATE TABLE " + DEVICE_HISTORY_TABLE_NAME + "(" +
                        INFO_DEVICE_ADD + " TEXT " + "primary key," +
                        INFO_DEVICE_NAME + " TEXT ," +
                        INFO_DEVICE_TYPE + " TEXT ," +
                        INFO_TIME + " INTEGER " + ");";
                break;
            case DEVICE_TYPE_TABLE_NAME:
                sql = "CREATE TABLE " + DEVICE_TYPE_TABLE_NAME + "(" +
                        INFO_ID + " INTEGER " + "primary key autoincrement," +
                        INFO_DEVICE_NAME + " TEXT ," +
                        INFO_DEVICE_TYPE + " TEXT ," +
                        INFO_DEVICE_ADD_START + " TEXT ," +
                        INFO_DEVICE_ADD_END + " TEXT " + ");";
                break;
        }

        if (!sql.equals("nil")) {
            db.execSQL(sql);
        }
    }

    public void createTable(String tableName){
        SQLiteDatabase db = this.getWritableDatabase();
        createTable(db,tableName);
    }

    //升级数据库
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //增加心跳数据
    public long insertHeart(long writeTime, String deviceAdd, int heartDAvg, int heartDMax, int heartDMin){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_TIME, writeTime / SQLITE_DATE_PER_JAVA_DATE);
        cv.put(INFO_DEVICE_ADD, deviceAdd);
        cv.put(AVG_HEART, heartDAvg);
        cv.put(MAX_HEART, heartDMax);
        cv.put(MIN_HEART, heartDMin);
        return db.insert(HEART_TABLE_NAME, null, cv);
    }

    //增加步数数据
    public long insertStep(long writeTime, String deviceAdd, int stepValue){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_TIME, writeTime / SQLITE_DATE_PER_JAVA_DATE);
        cv.put(INFO_DEVICE_ADD, deviceAdd);
        cv.put(VALUE_STEP, stepValue);
        return db.insert(STEP_TABLE_NAME, null, cv);
    }

    //增加体重数据
    public long insertWeight(long writeTime, String deviceAdd, double weightValue){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_TIME, writeTime / SQLITE_DATE_PER_JAVA_DATE);
        cv.put(INFO_DEVICE_ADD, deviceAdd);
        cv.put(VALUE_WEIGHT, (int) (weightValue * 100) / 100d);
        return db.insert(WEIGHT_TABLE_NAME, null, cv);
    }

    //增加设备历史数据
    public long insertDeviceHistory(long writeTime, String deviceAdd, String deviceName, String deviceType){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_TIME, writeTime / SQLITE_DATE_PER_JAVA_DATE);
        cv.put(INFO_DEVICE_ADD, deviceAdd);
        cv.put(INFO_DEVICE_NAME, deviceName);
        cv.put(INFO_DEVICE_TYPE, deviceType);
        return db.insert(DEVICE_HISTORY_TABLE_NAME, null, cv);
    }


    public long insertDeviceHistory(long writeTime, String deviceAdd, String deviceName){
        return 0;
    }

    public long insertDeviceType(String deviceName, String deviceType, String deviceAddStrat, String deviceAddEnd){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_DEVICE_NAME, deviceName);
        cv.put(INFO_DEVICE_TYPE, deviceType);
        cv.put(INFO_DEVICE_ADD_START, deviceAddStrat);
        cv.put(INFO_DEVICE_ADD_END, deviceAddEnd);
        return db.insert(DEVICE_TYPE_TABLE_NAME, null, cv);
    }

    public Cursor selectData(String tableName){
        //当tableName为空或者不为设备列表table时
        if(tableName == null
                || !tableName.equals(DEVICE_HISTORY_TABLE_NAME))
            return null;

        //参数验证合法后
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + DEVICE_HISTORY_TABLE_NAME, null);
    }

    public Cursor selectData(String deviceType, String tableName){
        if(deviceType == null)  return null;
        //当tableName为空或者不为设备类型table时
        if(tableName == null
                || tableName.equals(DEVICE_TYPE_TABLE_NAME))
            return null;

        //参数验证合法后
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + DEVICE_TYPE_TABLE_NAME +
                        "WHERE " + INFO_DEVICE_TYPE + "=" + deviceType, null);
    }
    /**
     * 按时间区间查询数据
     *
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @param typeTimeBlock  时间片类型
     * @param tableName 表名
     * @return  查询结果
     */
    public Cursor selectData(long startTime, long endTime, String typeTimeBlock, String tableName){
        //当typeTime为空或者不为已有日期格式输入之一时
        if(typeTimeBlock == null
                || !(typeTimeBlock.equals(TYPE_BLOCK_DAY)
                    || typeTimeBlock.equals(TYPE_BLOCK_HOUR)
                    || typeTimeBlock.equals(TYPE_BLOCK_WEEK )
                    || typeTimeBlock.equals(TYPE_BLOCK_MONTH)))
            return null;
        //当tableName为空或不为心跳、步数、体重表之一时
        if(tableName == null
                || !(tableName.equals(HEART_TABLE_NAME)
                    ||tableName.equals(STEP_TABLE_NAME)
                    ||tableName.equals(WEIGHT_TABLE_NAME)))
            return null;

        //验证参数合法后
        startTime /= SQLITE_DATE_PER_JAVA_DATE;
        endTime /= SQLITE_DATE_PER_JAVA_DATE;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;

        switch (tableName){
            case HEART_TABLE_NAME:  //查询心率
                cursor = db.rawQuery(
                        "SELECT " + "strftime(" + typeTimeBlock + "," + INFO_TIME + "," + "'unixepoch','localtime') AS " + ARG_TIME_BLOCK + ","
                                + "avg(" + AVG_HEART + ")," + "avg(" + MAX_HEART + ")," + "avg(" + MIN_HEART + ")" +
                                " FROM " + HEART_TABLE_NAME +
                                " WHERE " + INFO_TIME + ">=" + startTime +
                                " and " + INFO_TIME + "<" + endTime +
                                " GROUP BY " + ARG_TIME_BLOCK, null);
                return cursor;
            case STEP_TABLE_NAME:   //查询步数
                cursor = db.rawQuery(
                        "SELECT " + "strftime(" + typeTimeBlock + "," + INFO_TIME + "," + "'unixepoch','localtime') AS " + ARG_TIME_BLOCK + ","
                                + VALUE_STEP + "," + INFO_TIME +
                                " FROM " + STEP_TABLE_NAME +
                                " WHERE " + INFO_TIME + ">=" + startTime +
                                " and " + INFO_TIME + "<" + endTime, null);
                return cursor;
            case WEIGHT_TABLE_NAME: //查询体重
                cursor = db.rawQuery(
                        "SELECT " + "strftime(" + typeTimeBlock + "," + INFO_TIME + "," + "'unixepoch','localtime') AS " + ARG_TIME_BLOCK + ","
                                + "avg(" + VALUE_WEIGHT + ")" +
                                " FROM " + WEIGHT_TABLE_NAME +
                                " WHERE " + INFO_TIME + ">=" + startTime +
                                " and " + INFO_TIME + "<" + endTime +
                                " GROUP BY " + ARG_TIME_BLOCK, null);
                return cursor;

        }
        //当tableName不为已有表名之一时
        return null;
    }

    /**
     * 清除指定的数据表
     * @param tableName 表名
     */
    public void clearTable(String tableName){
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(sql);
    }

    /**
     * 重置指定的数据表
     * @param tableName 表名
     */
    public void resetTable(String tableName){
        clearTable(tableName);
        createTable(tableName);
    }


    /**
     * 重置所有数据表
     */
    public void resetTable(){
        resetTable(HEART_TABLE_NAME);
        resetTable(STEP_TABLE_NAME);
        resetTable(WEIGHT_TABLE_NAME);
        resetTable(DEVICE_TYPE_TABLE_NAME);
        resetTable(DEVICE_HISTORY_TABLE_NAME);
    }

    /**
     *  模拟设备列表历史记录
     */
    public void simulateDeviceData(){
        final SQLiteDatabase db = this.getWritableDatabase();
        resetTable(DEVICE_HISTORY_TABLE_NAME);

        writeTime = new Date().getTime();
        db.beginTransaction();  //手动设置开始事务

        insertDeviceHistory(writeTime, sDeviceAdd_TPTWATCH,
                rootContext.getString(R.string.initial_test_ptwatch),
                rootContext.getString(R.string.name_ptwatch));
        insertDeviceHistory(writeTime, sDeviceAdd_TPTESc,
                rootContext.getString(R.string.initial_test_ptscale),
                rootContext.getString(R.string.name_ptscale));
        insertDeviceHistory(writeTime, sDeviceAdd_TMI1S,
                rootContext.getString(R.string.initial_test_mi_band),
                rootContext.getString(R.string.name_mi_band));

        db.setTransactionSuccessful();        //设置事务处理状态至成功，不设置会自动回滚不提交
        db.endTransaction();        //处理完成
    }

    /**
     *  模拟设备类型的缺省表
     */
    public void simulateDeviceType(){
        final SQLiteDatabase db = this.getWritableDatabase();
        resetTable(DEVICE_TYPE_TABLE_NAME);

        db.beginTransaction();  //手动设置开始事务

        insertDeviceType(rootContext.getString(R.string.name_mi_band),
                rootContext.getString(R.string.initial_test_mi_band),
                sDeviceAdd_START,
                sDeviceAdd_END);
        insertDeviceType(rootContext.getString(R.string.name_ptscale),
                rootContext.getString(R.string.initial_test_ptscale),
                sDeviceAdd_START,
                sDeviceAdd_END);
        insertDeviceType(rootContext.getString(R.string.name_ptwatch),
                rootContext.getString(R.string.initial_test_ptwatch),
                sDeviceAdd_START,
                sDeviceAdd_END);

        db.setTransactionSuccessful();        //设置事务处理状态至成功，不设置会自动回滚不提交
        db.endTransaction();        //处理完成
    }

    /**
     *  模拟为期一年半的全部数据注入
     */
//    public void simulateData(long sunTime, long intervalsTime, String tableName){
    public void simulateData(){
        final SQLiteDatabase db = this.getWritableDatabase();
        resetTable();

        writeTime = new Date().getTime();
        Thread sThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("simData", "run");
                Log.d("simData", "sSumTime" + sSumTime + (sSumTime > 0));

                SimpleDateFormat mlSimpleDateFormat= new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.CHINA);
                Random mlRandom = new Random();
                db.beginTransaction();          //手动设置开始事务
                // Transaction活动时间内所有的操作将被包装成统一的事务进行SQL操作

                //模拟设备的MAC（做识别码）
                String sDeviceAdd = sDeviceAdd_TMI1S;
                //模拟注入心跳与步数
                for(long i = sSumTime;i > - sObligateTime;i -= sHeartAndStepIntervals + sTimeShake){
                    sTimeShake = (long)(Math.random() * 100);

                    //注入时间模拟
                    long simulateTime = writeTime - i;

                    if((simulateTime / 3600 / 1000) % 24 > 7 || (simulateTime / 3600 / 1000) % 24 < 22  ){
                        //仅注入0700-2200时间段内的数据
                        //心跳数值模拟
                        dHeartMax = (double)sHeartBench + (double)(sHeartMax - sHeartBench) * Math.random();
                        dHeartMin = (double)sHeartBench - (double)(sHeartBench - sHeartMin) * Math.random();
                        dHeartAvg = dHeartMin + (dHeartMax - dHeartMin) * Math.random();

                        //步数数值与断点模拟
                        sStepBench += Math.random() * 800;
                        if(Math.random() < 0.02)
                            sStepBench = (int)(Math.random() * 800);

                        //反复注入MI1S与PTWATCH数据，模拟两种设备的数据输
                        if(sDeviceAdd.equals(sDeviceAdd_TMI1S))
                            sDeviceAdd = sDeviceAdd_TPTWATCH;
                        else
                            sDeviceAdd = sDeviceAdd_TMI1S;

                        Date mlDate = new Date(simulateTime);
                        insertHeart(simulateTime, sDeviceAdd, (int)dHeartAvg, (int)dHeartMax, (int)dHeartMin);
                        insertStep(simulateTime, sDeviceAdd, sStepBench);

                        Log.d("simData", "Date " + mlSimpleDateFormat.format(mlDate));
                        Log.d("simData", "insertHeart " + (int) dHeartAvg + "," + (int) dHeartMax +","+ (int) dHeartMin);
                        Log.d("simData", "insertStep " + sStepBench);
                    }
                }

                //模拟注入体重
                for(long i = sSumTime + sObligateTime;i > - sObligateTime;i -= sWeightIntervals + sTimeShake){
                    sTimeShake = (long)(Math.random() * 100);

                    //注入时间模拟
                    long simulateTime = writeTime - i;

                    if((simulateTime / 3600 / 1000) % 24 > 7 || (simulateTime / 3600 / 1000) % 24 < 22  ){
                        //仅注入0700-2200时间段内的数据
                        Date mlDate = new Date(simulateTime);

                        //体重模拟震荡函数
                        sWeightParaUp = (dWeightBench - sWeightMax) / (sWeightBench - sWeightMax);
                        sWeightParaDown = (dWeightBench - sWeightMin) / (sWeightBench - sWeightMin);
                        if(mlRandom.nextBoolean())
                            dWeightBench += Math.random() * sWeightParaUp;
                        else
                            dWeightBench -= Math.random() * sWeightParaDown;

                        insertWeight(simulateTime, sDeviceAdd_TPTESc, dWeightBench);
                        Log.d("simData", "Date " + mlSimpleDateFormat.format(mlDate));
                        Log.d("simData", "insertWeight " + dWeightBench);
                    }

                }

                db.setTransactionSuccessful();        //设置事务处理状态至成功，不设置会自动回滚不提交
                db.endTransaction();        //处理完成
            }
        });
        sThread.start();
    }

    public void SQLTest(){
        final SQLiteDatabase db = this.getWritableDatabase();
        final long timeNow = new Date().getTime();
//        final String sqlTest = "SELECT strftime('%Y%m'," + INFO_TIME + ")" +
//                " FROM " + WEIGHT_TABLE_NAME;
        final String sqlTest = "SELECT strftime('%Y-%m'," + INFO_TIME + "," + "'unixepoch')" +
                " FROM " + STEP_TABLE_NAME;
        Thread TThread = new Thread(new Runnable() {
            @Override
            public void run() {
//                Cursor cursor = db.rawQuery(sqlTest,null);
                Cursor cursor = selectData(timeNow - sSumTime, timeNow, TYPE_BLOCK_MONTH, HEART_TABLE_NAME);
                cursor.moveToFirst();
                while(cursor.moveToNext()){
                    Log.d("SQLTest", cursor.getString(0));
                    Log.d("SQLTest", cursor.getString(1));
                    Log.d("SQLTest", cursor.getString(2));
                    Log.d("SQLTest", cursor.getString(3));
                }
            }
        });
        TThread.start();
    }
}