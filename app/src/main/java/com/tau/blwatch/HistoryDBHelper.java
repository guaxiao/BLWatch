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
    private final static String HEART_TABLE_NAME = "heart_table";
    private final static String STEP_TABLE_NAME = "step_table";
    private final static String WEIGHT_TABLE_NAME = "weight_table";

    public final static String INFO_ID = "id";
    public final static String INFO_TIME = "time";
    public final static String INFO_DEVICE_ADD = "device_add";
    public final static String AVG_HEART = "avg_heart";
    public final static String MAX_HEART = "max_heart";
    public final static String MIN_HEART = "min_heart";
    public final static String VALUE_STEP = "value_step";
    public final static String VALUE_WEIGHT = "value_weight";
    public final static String CASE_TIME_BLOCK = "time_block";

    public final static int SIZE_OF_SELECT_HEART = 3;
    public final static int SIZE_OF_SELECT_STEP = 1;
    public final static int SIZE_OF_SELECT_WEIGHT = 1;

    public final static int SQLITE_DATE_PER_JAVA_DATE = 1000;

    //以下为模拟注入数据时使用的量
    private long writeTime = 0L;
    public final static String sDeviceAdd = "00:2C:00:00:00:FF";
    public final static long sStepIntervals =  5 * 60 * 1000;   //步数数据库计入间隔5分钟
    public final static long sHeartIntervals = 5 * 60 * 1000;   //心跳数据库计入间隔5分钟
    public final static long sWeightIntervals = 8 * 60 * 60 * 1000; //体重数据库计入间隔8小时
    public final static long sSumTime = (long)365 * 24 * 3600 * 1000; //模拟注入时间跨度一年

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
        String sql = "CREATE TABLE " + HEART_TABLE_NAME + "(" +
                INFO_ID + " INTEGER " + "primary key autoincrement," +
                INFO_TIME + " INTEGER ," +
                INFO_DEVICE_ADD + " TEXT ," +
                AVG_HEART + " INTEGER ," +
                MAX_HEART + " INTEGER ," +
                MIN_HEART + " INTEGER " +");";
        db.execSQL(sql);

        //创建表记录步数
        sql = "CREATE TABLE " + STEP_TABLE_NAME + "(" +
                INFO_ID + " INTEGER " + "primary key autoincrement," +
                INFO_TIME + " INTEGER ," +
                INFO_DEVICE_ADD + " TEXT ," +
                VALUE_STEP + " INTEGER " + ");";
        db.execSQL(sql);

        //创建表记录体重
        sql = "CREATE TABLE " + WEIGHT_TABLE_NAME + "(" +
                INFO_ID + " INTEGER " + "primary key autoincrement," +
                INFO_TIME + " INTEGER ," +
                INFO_DEVICE_ADD + " TEXT ," +
                VALUE_WEIGHT + " INTEGER " + ");";
        db.execSQL(sql);
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

    //按时间区间查询心跳
    public Cursor selectHeart(long startTime, long endTime){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + "avg(" + AVG_HEART + ")," + "max(" + MAX_HEART + ")," + "min(" + MIN_HEART + ")" +
                        " FROM " + HEART_TABLE_NAME +
                        " WHERE " + INFO_TIME + ">=" + startTime +
                        " and " + INFO_TIME + "<" + endTime +
                        " GROUP BY " + INFO_DEVICE_ADD, null);
        return cursor;
    }

    //按时间区间查询步数
    public Cursor selectStep(long startTime, long endTime){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + STEP_TABLE_NAME + " WHERE " +
                        INFO_TIME + ">=? and " + INFO_TIME + "<?",
                new String[]{String.valueOf(startTime),String.valueOf(endTime)}
        );
        return cursor;
    }

    //按时间区间查询体重
    public Cursor selectWeight(long startTime, long endTime){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + WEIGHT_TABLE_NAME + " WHERE " +
                        INFO_TIME + ">=? and " + INFO_TIME + "<?",
                new String[]{String.valueOf(startTime),String.valueOf(endTime)}
        );
        return cursor;
    }

    //清除心跳数据表
    public void clearHeart(){
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DROP TABLE IF EXISTS " + HEART_TABLE_NAME;
        db.execSQL(sql);
    }

    //清除步数数据表
    public void clearStep(){
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DROP TABLE IF EXISTS " + STEP_TABLE_NAME;
        db.execSQL(sql);
    }

    //清除体重数据表
    public void clearWeight(){
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DROP TABLE IF EXISTS " + WEIGHT_TABLE_NAME;
        db.execSQL(sql);
    }


    //重新创建数据表
    public void resetTable(){
        clearWeight();
        clearStep();
        clearHeart();
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);
    }

    //模拟为期一年的数据注入
    public void simulateData(){
        final SQLiteDatabase db = this.getWritableDatabase();
        clearHeart();
        clearStep();
        clearWeight();
        onCreate(db);

        writeTime = new Date().getTime();
        Thread sThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("simData", "run");
                Log.d("simData", "sSumTime" + sSumTime + (sSumTime > 0));

                SimpleDateFormat mlSimpleDateFormat= new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
                Random mlRandom = new Random();
                db.beginTransaction();          //手动设置开始事务
                                                // Transaction活动时间内所有的操作将被包装成统一的事务进行SQL操作

                //模拟注入心跳与步数
                for(long i = sSumTime;i > 0;i -= sHeartIntervals + sTimeShake){
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

                for(long i = sSumTime;i > 0;i -= sWeightIntervals + sTimeShake){
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
        SQLiteDatabase db = this.getWritableDatabase();
        long timeNow = new Date().getTime();
        Cursor cursor = db.rawQuery("SELECT strftime('%Y%m'," + "'now'" + ")",null);
        cursor.moveToFirst();
        Log.d("SQLTest", cursor.getString(0));
    }
}