package com.tau.blwatch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Created by Administrator on 2015/12/17 0017.
 */
public class HistoryDBHelper extends SQLiteOpenHelper {

    private final static String DATABASE_NAME = "HistoryData.db";
    private final static int DATABASE_VERSION = 1;
    public final static String HEART_TABLE_NAME = "heart_table";
    public final static String STEP_TABLE_NAME = "step_table";
    public final static String WEIGHT_TABLE_NAME = "weight_table";

    private final static String INFO_ID = "id";
    private final static String INFO_TIME = "time";
    private final static String INFO_DEVICE_ADD = "device_add";
    private final static String AVG_HEART = "avg_heart";
    private final static String MAX_HEART = "max_heart";
    private final static String MIN_HEART = "min_heart";
    private final static String VALUE_STEP = "value_step";
    private final static String VALUE_WEIGHT = "value_weight";

    public final static String ARG_TIME_BLOCK = "time_block";

    private final static int SQLITE_DATE_PER_JAVA_DATE = 1000;

    public final static String TYPE_BLOCK_MONTH = "'%Y-%m'";    //年-月
    public final static String TYPE_BLOCK_WEEK = "'%Y-%W'";     //年-周
    public final static String TYPE_BLOCK_DAY = "'%m-%d'";      //月-天
    public final static String TYPE_BLOCK_HOUR = "'%d-%H'";     //日-小时

    public final static String ARG_START_TIME = "StartTime:";
    public final static String ARG_TIME_TYPE = ",TimeType:";
    public final static String ARG_FROM = ",From:";
    public final static String ARG_DATA = ",Date:";
    public final static String ARG_VALUE = ",Value:";

    //以下为模拟注入数据时使用的量
    private long writeTime = 0L;
    public final static String sDeviceAdd = "00:2C:00:00:00:FF";
    public final static long sTestIntervals =  5 * 60 * 1000;   //测试数据数据库计入间隔5分钟
    public final static long sHeartAndStepIntervals = 5 * 60 * 1000;   //心跳数据库计入间隔5分钟
    public final static long sWeightIntervals = 8 * 60 * 60 * 1000; //体重数据库计入间隔8小时
    public final static long sSumTime = (long)365 * 24 * 3600 * 1000; //模拟注入时间跨度一年
    public final static long sObligateTime = (long)180 * 24 * 3600 * 1000; //模拟预留注入时间180天

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
    }

    //免于dbName与版本号的简易构造方法
    public HistoryDBHelper(Context context) {
        // TODO Auto-generated constructor stub
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        createTable(db,STEP_TABLE_NAME);

        //创建表记录体重
        createTable(db, WEIGHT_TABLE_NAME);
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
        long row = db.insert(HEART_TABLE_NAME, null, cv);
        return row;
    }

    //增加步数数据
    public long insertStep(long writeTime, String deviceAdd, int stepValue){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_TIME, writeTime / SQLITE_DATE_PER_JAVA_DATE);
        cv.put(INFO_DEVICE_ADD, deviceAdd);
        cv.put(VALUE_STEP, stepValue);
        long row = db.insert(STEP_TABLE_NAME, null, cv);
        return row;
    }

    //增加体重数据
    public long insertWeight(long writeTime, String deviceAdd, double weightValue){
        SQLiteDatabase db = this.getWritableDatabase();
        /* ContentValues */
        ContentValues cv = new ContentValues();
        cv.put(INFO_TIME, writeTime / SQLITE_DATE_PER_JAVA_DATE);
        cv.put(INFO_DEVICE_ADD, deviceAdd);
        cv.put(VALUE_WEIGHT, (int)(weightValue * 100) / 100d);
        long row = db.insert(WEIGHT_TABLE_NAME, null, cv);
        return row;
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
        //当tableName为空时
        if(tableName == null)
            return null;

        //验证参数合法后
        startTime /= SQLITE_DATE_PER_JAVA_DATE;
        endTime /= SQLITE_DATE_PER_JAVA_DATE;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;

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
    }

    public static String createKeyName(long startTime, int numBlock, String typeTimeBlock, String tableName, boolean isDate, String columnName){
        String keyName = ARG_START_TIME + startTime + ARG_TIME_TYPE + numBlock + " * " + typeTimeBlock + ARG_FROM + tableName;
        if(isDate)
            keyName += ARG_DATA + columnName;
        else
            keyName += ARG_VALUE + columnName;
        return keyName;
    }

    public static String createKeyName(long startTime, int numBlock, String typeTimeBlock, String tableName, boolean isDate){
        String keyName = createKeyName(startTime, numBlock, typeTimeBlock, tableName, isDate, "");
        return keyName;
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

                        Date mlDate = new Date(simulateTime);
                        insertHeart(simulateTime, sDeviceAdd, (int)dHeartAvg, (int)dHeartMax, (int)dHeartMin);
                        insertStep(simulateTime, sDeviceAdd, sStepBench);

                        Log.d("simData", "Date " + mlSimpleDateFormat.format(mlDate));
                        Log.d("simData", "insertHeart " + (int) dHeartAvg + "," + (int) dHeartMax +","+ (int) dHeartMin);
                        Log.d("simData", "insertStep " + sStepBench);
                    }
                }

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

                        insertWeight(simulateTime, sDeviceAdd, dWeightBench);
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