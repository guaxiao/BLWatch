package com.tau.blwatch.util;

import java.util.Calendar;
import java.util.Date;

public class TimeBlockEntity {
    public final static String CHART_TIME_SUM = "sunTimeChart";
    public final static String CHART_TIME_YEAR = "yearTimeChart";
    public final static String CHART_TIME_QUARTER = "quarterTimeChart";
    public final static String CHART_TIME_MONTH = "monthTimeChart";
    public final static String CHART_TIME_WEEK = "weekTimeChart";
    public final static String CHART_TIME_DAY = "dayTimeChart";

    public final static String CHART_TYPE_SUM = "sunTypeChart";
    public final static String CHART_TYPE_STEP = "stepTypeChart";
    public final static String CHART_TYPE_HEART = "heartTypeChart";
    public final static String CHART_TYPE_WEIGHT = "weightTypeChart";


    public final static int CHART_COLOR_STEP = 0;
    public final static int CHART_COLOR_HEART = 1;
    public final static int CHART_COLOR_WEIGHT = 2;

    public long startTime = 0L;
    public Calendar calendar;
    public int numBlock, numChartColor;
    public String typeTimeBlock, tableName;

    @Override
    public String toString() {
        return "StartTime:" + startTime + ",NumBlock:" + numBlock
                + ",NumChartColor:" + numChartColor + ",TypeTimeBlock:" + typeTimeBlock
                + ",TableName:" + tableName;
    }


    public TimeBlockEntity(String tagOfTime, String tagOfType) {
        startTime = new Date().getTime();
        calendar = Calendar.getInstance();
        numBlock = 0;
        numChartColor = 0;
        typeTimeBlock = null;
        tableName = null;

        switch (tagOfTime) {
            case CHART_TIME_YEAR:
                //设置为十二个月前的第一天
                calendar.add(Calendar.YEAR, -1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                numBlock = TimeHelper.MONTH_OF_YEAR;
                typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_MONTH;
                break;
            case CHART_TIME_QUARTER:
                //设置为本周第一天的13*7-1天前
                calendar.set(Calendar.DAY_OF_WEEK, 1);
                calendar.add(Calendar.DATE, -(TimeHelper.WEEK_PER_QUARTER) * 7 + 1);
                numBlock = TimeHelper.WEEK_PER_QUARTER;
                typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_WEEK;
                break;
            case CHART_TIME_MONTH:
                //设置为当月第一天
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                numBlock = TimeHelper.DAY_PER_MONTH;
                typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_DAY;
                break;
            case CHART_TIME_WEEK:
                //设置为当前星期的第一天
                calendar.set(Calendar.DAY_OF_WEEK, 1);
                numBlock = TimeHelper.DAY_PER_WEEK;
                typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_DAY;
                break;
            case CHART_TIME_DAY:
                numBlock = TimeHelper.HOUR_PER_DAY;
                typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_HOUR;
                break;
        }

        //设置为00:00:00时刻
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        startTime = calendar.getTime().getTime();

        switch (tagOfType) {
            case CHART_TYPE_HEART:
                tableName = HistoryDBHelper.HEART_TABLE_NAME;
                numChartColor = CHART_COLOR_HEART;
                break;
            case CHART_TYPE_STEP:
                tableName = HistoryDBHelper.STEP_TABLE_NAME;
                numChartColor = CHART_COLOR_STEP;
                break;
            case CHART_TYPE_WEIGHT:
                tableName = HistoryDBHelper.WEIGHT_TABLE_NAME;
                numChartColor = CHART_COLOR_WEIGHT;
                break;
        }
    }
}