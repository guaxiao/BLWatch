package com.tau.blwatch;

import java.util.Calendar;

/**
 * Created by John on 2015/12/30.
 */
public class TimeHelper {
    public static int DAY_PER_MONTH = 31;
    public static int DAY_PER_QUARTER = 90;
    static {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.DATE, 1);
        mCalendar.roll(Calendar.DATE, -1);
        DAY_PER_MONTH = mCalendar.get(Calendar.DATE);   //1月=31/30/29/28天
        DAY_PER_QUARTER = DAY_PER_MONTH;
        mCalendar.add(Calendar.MONTH, -1);
        DAY_PER_QUARTER += mCalendar.get(Calendar.DATE);
        mCalendar.add(Calendar.MONTH, -1);
        DAY_PER_QUARTER += mCalendar.get(Calendar.DATE);    //1季度=90/91天
    }

    public final static int POINT_PER_LINE_OF_YEAR = 12;    //1年=12个月
    public final static int POINT_PER_LINE_OF_QUARTER = 13; //1季度=13个星期
    public final static int POINT_PER_LINE_OF_MONTH = DAY_PER_MONTH;
    public final static int POINT_PER_LINE_OF_WEEK = 7;     //1星期=7天
    public final static int POINT_PER_LINE_OF_DAY = 24;      //1天=24小时

    public final static long MI_SECOND_YEAR = (long)365 * 24 * 3600 * 1000;
    public final static long MI_SECOND_QUARTER = (long)365 * 24 * 3600 * 1000;
    public final static long MI_SECOND_MONTH = (long)DAY_PER_MONTH * 24 * 3600 * 1000;
    public final static long MI_SECOND_WEEK = (long)7 * 24 * 3600 * 1000;
    public final static long MI_SECOND_DAY = (long)24 * 3600 * 1000;
    public final static long MI_SECOND_HOUR = (long)3600 * 1000;
}
