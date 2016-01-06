package com.tau.blwatch;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class DataBaseSelectHelper {
    public final static String ARG_START_TIME = "StartTime:";
    public final static String ARG_TIME_TYPE = ",TimeType:";
    public final static String ARG_FROM = ",From:";
    public final static String ARG_DATA = ",Date:";
    public final static String ARG_VALUE = ",Value:";

    private static HashMap<String,ArrayList<Float>> mPointCollection;
//    private static final int cursorWindowLimitOfMonthSelect = 3;

    private static HistoryDBHelper mHistoryDBHelper;
    private static long mStartTime;
    private static int mNumBlock;
    private static String mTypeTimeBlock,mTableName;
    private static int startTimeFlag,currentTimeFlag,lastYearTimeFlag;

    public DataBaseSelectHelper(HistoryDBHelper historyDBHelper,long startTime, int numBlock, String typeTimeBlock, String tableName) {
        mHistoryDBHelper = historyDBHelper;
        mStartTime = startTime;
        mNumBlock = numBlock;
        mTypeTimeBlock = typeTimeBlock;
        mTableName = tableName;

        startTimeFlag = -1;
        currentTimeFlag = -1;
        lastYearTimeFlag = 0;
        mPointCollection = new HashMap<>();
        onSelect();
    }

    public void onSelect(){
        Log.d("onSelectHeartData", "start");

//        if(!isPart)
//            pointCollection = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(mStartTime));

        switch (mTypeTimeBlock){
            case HistoryDBHelper.TYPE_BLOCK_HOUR:
                startTimeFlag = calendar.get(Calendar.HOUR_OF_DAY);
                calendar.add(Calendar.HOUR,mNumBlock);
                break;
            case HistoryDBHelper.TYPE_BLOCK_DAY:
                if(mNumBlock <= 7)
                    startTimeFlag = calendar.get(Calendar.DAY_OF_WEEK);
                else
                    startTimeFlag = calendar.get(Calendar.DAY_OF_MONTH);
                calendar.add(Calendar.DATE,mNumBlock);
                break;
            case HistoryDBHelper.TYPE_BLOCK_WEEK:
                startTimeFlag = calendar.get(Calendar.WEEK_OF_YEAR);
                calendar.add(Calendar.DATE,7 * mNumBlock);
                break;
            case HistoryDBHelper.TYPE_BLOCK_MONTH:
                startTimeFlag = calendar.get(Calendar.MONTH);
//                if(numBlock > cursorWindowLimitOfMonthSelect && tableName.equals(HistoryDBHelper.STEP_TABLE_NAME)){
//                    //查询三个月以上的步数信息时，易发生CursorWindow大于2M的情况，需要分段查询
//                    long startTimeOfLimit = startTime;
//                    for(int i = 0 ; i < numBlock / cursorWindowLimitOfMonthSelect ; i++){
//                        onSelectData(startTimeOfLimit,cursorWindowLimitOfMonthSelect,typeTimeBlock,tableName,true);
//                        calendar.add(Calendar.MONTH, cursorWindowLimitOfMonthSelect);
//                        startTimeOfLimit = calendar.getTimeInMillis();
//                    }
//                    int remainder = numBlock % cursorWindowLimitOfMonthSelect;
//                    if(remainder > 0)
//                        onSelectData(startTimeOfLimit,remainder,typeTimeBlock,tableName,true);
//                    return pointCollection;
//
//                }else   //不需要分段查询的情况
                calendar.add(Calendar.MONTH, mNumBlock);
                break;
        }
        currentTimeFlag = startTimeFlag;
        long endTime = calendar.getTimeInMillis();

        Cursor pointCursor = mHistoryDBHelper.selectData(mStartTime,endTime,mTypeTimeBlock,mTableName);
        if(pointCursor.getCount() == 0) //若查询不到任何数据，则直接返回
            return;

        Log.d("DBHelper.selectData",mStartTime + "," + endTime + "," + mTypeTimeBlock + "," + mTableName);
        Log.d("pointCursor", "getCount()=" + pointCursor.getCount());
        Log.d("pointCursor", "getColumnCount()=" + pointCursor.getColumnCount());

        if(mTableName.equals(HistoryDBHelper.STEP_TABLE_NAME)){
            pointCursor.moveToFirst();
            String dateBlock = pointCursor.getString(0);
            int stepSumOfBlock = 0;
            int stepMaxi = pointCursor.getInt(1);
            int lastBlockEndAt = stepMaxi;
            boolean hasRestartFromZeroInBlock = false;
//            int countLastWeek = 1;
//            boolean hasCurrentToNextBlock = false;
            while (pointCursor.moveToNext()){
//                Log.d("pointCursor",pointCursor.getColumnCount() + "");
//                if(!hasCurrentToNextBlock){
//                if(countLastWeek == 13){
//                    Date date = new Date(pointCursor.getInt(2) * 1000L);
//                    Log.d("pointCursor",date.toString());
//                }

                String dateTemp = pointCursor.getString(0);
                int stepTemp = pointCursor.getInt(1);
                if(!dateBlock.equals(dateTemp)){    //抵达下一时间片
//                    hasCurrentToNextBlock = true;
//                    countLastWeek ++;
                    if(!hasRestartFromZeroInBlock){ //若本次递增函数为时间片内的第一个递增函数
                        stepSumOfBlock += stepMaxi - lastBlockEndAt;    //则将此极大值与上一时间片的结束值之差加入小计
                    }else{  //若不是第一个递增函数
                        stepSumOfBlock += stepMaxi; //则直接将此极大值加入小计
                    }

                    //存储上一时间片与其步数小计
                    String keyNameDate = createKeyName(true, pointCursor.getColumnName(0));
                    String keyNameValue = createKeyName(false, pointCursor.getColumnName(1));

                    onPassTimeBlockInMaxiCalculate(keyNameDate, keyNameValue, dateBlock, stepSumOfBlock);

                    //置极大值为当前值，开始下一个递增函数极大值的寻找
                    stepMaxi = stepTemp;
                    //记录本时间片结束时的步数数据
                    lastBlockEndAt = stepTemp;
                    //初始化下一时间片内第一次递增函数结束状态为未结束
                    hasRestartFromZeroInBlock = false;
                    //重置时间片内步数小计
                    stepSumOfBlock = 0;
                    //置时间片的值为当前时间片
                    dateBlock = dateTemp;
                }else{  //未抵达下一时间片
                    if(stepTemp < stepMaxi){    //当前值小于上一次的值，则上一次的值为极大值
                        if(!hasRestartFromZeroInBlock){ //若本次递增函数为时间片内的第一个递增函数
                            stepSumOfBlock += stepMaxi - lastBlockEndAt;    //则将此极大值与上一时间片的结束值之差加入小计
                        }else{  //若不是第一个递增函数
                            stepSumOfBlock += stepMaxi; //则直接将此极大值加入小计
                        }
                        //置极大值为当前值，开始下一个递增函数的极大值寻找
                        stepMaxi = stepTemp;
                        //置时间片内第一次递增函数结束状态为已结束
                        hasRestartFromZeroInBlock = true;
                    }else{
                        stepMaxi = stepTemp;
                    }
                }
            }

            //在数据遍历结束后，处理最后一个时间片的值
            if(!hasRestartFromZeroInBlock){ //若本次递增函数为时间片内的第一个递增函数
                stepSumOfBlock += stepMaxi - lastBlockEndAt;    //则将此极大值与上一时间片的结束值之差加入小计
            }else{  //若不是第一个递增函数
                stepSumOfBlock += stepMaxi; //则直接将此极大值加入小计
            }

            //存储上一时间片与其步数小计
            String keyNameDate = createKeyName(true, pointCursor.getColumnName(0));
            String keyNameValue = createKeyName(false, pointCursor.getColumnName(1));

            onPassTimeBlockInMaxiCalculate(keyNameDate, keyNameValue, dateBlock, stepSumOfBlock);

        }else{
            while(pointCursor.moveToNext()){//遍历每一行
                String dateBlock = pointCursor.getString(0);
                Log.d("pointCursor", "not step:" + dateBlock);
                for(int j = 0;j < pointCursor.getColumnCount();j++){//遍历每一列
                    boolean isDate = (pointCursor.getColumnName(j).equals(HistoryDBHelper.ARG_TIME_BLOCK));
                    String keyName = createKeyName(isDate, pointCursor.getColumnName(j));
                    ArrayList<Float> tempList = mPointCollection.get(keyName);

                    if(tempList == null)
                        tempList = new ArrayList<>();

                    int listLength = tempList.size();
                    //若在年-星期时间片下，此星期跨过了两个年份时
                    if(mTypeTimeBlock.equals(HistoryDBHelper.TYPE_BLOCK_WEEK) && dateBlock.endsWith("-00")
                            && listLength != 0){
                        if(isDate){
                            lastYearTimeFlag = currentTimeFlag;
                        }else{
                            //将在第二年中的后半星期的值加到前半星期
                            float fLastValue = tempList.get(listLength - 1);
                            fLastValue = (fLastValue + pointCursor.getFloat(j)) / 2;
                            tempList.set(listLength - 1, fLastValue);
                        }
                    }else{
                        if(isDate){
                            Log.d("pointCursor",pointCursor.getString(0) + " " + pointCursor.getString(1));
                            currentTimeFlag = Integer.valueOf(dateBlock.substring(dateBlock.lastIndexOf("-") + 1));
                            tempList.add((float)currentTimeFlag - startTimeFlag + lastYearTimeFlag);
                            mPointCollection.put(keyName, tempList);
                        }else{
                            tempList.add(pointCursor.getFloat(j));
                            mPointCollection.put(keyName,tempList);
                        }
                    }
                }
            }
        }
    }

    private static void onPassTimeBlockInMaxiCalculate(String keyNameDate,String keyNameValue,String dateBlock,int stepSumOfBlock){

        ArrayList<Float> tempListDate = mPointCollection.get(keyNameDate);
        ArrayList<Float> tempListValue = mPointCollection.get(keyNameValue);
        if(tempListDate == null)
            tempListDate = new ArrayList<>();
        if(tempListValue == null)
            tempListValue = new ArrayList<>();

        int listLength = tempListValue.size();
        //若在年-星期时间片下，此星期跨过了两个年份时
        if(mTypeTimeBlock.equals(HistoryDBHelper.TYPE_BLOCK_WEEK) && dateBlock.endsWith("-00")
                && listLength != 0){
            lastYearTimeFlag = currentTimeFlag;
            //将在第二年中的后半星期的值加到前半星期
            float fLastValue = tempListValue.get(listLength - 1);
            fLastValue += stepSumOfBlock;
            tempListValue.set(listLength - 1,fLastValue);
        }else{
            currentTimeFlag = Integer.valueOf(dateBlock.substring(dateBlock.lastIndexOf("-") + 1));
            tempListDate.add((float)currentTimeFlag - startTimeFlag + lastYearTimeFlag);
            mPointCollection.put(keyNameDate, tempListDate);

            tempListValue.add((float) stepSumOfBlock);
            mPointCollection.put(keyNameValue, tempListValue);
            Log.d("pointCursor", dateBlock + " " + stepSumOfBlock);
        }
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
        return createKeyName(startTime, numBlock, typeTimeBlock, tableName, isDate, "");
    }

    private static String createKeyName(boolean isDate, String columnName){
        return createKeyName(mStartTime,mNumBlock,mTypeTimeBlock,mTableName,isDate,columnName);
    }

    public HashMap<String,ArrayList<Float>> getPointCollection(){
        return mPointCollection;
    }
}

