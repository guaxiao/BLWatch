package com.tau.blwatch.util;

import java.util.regex.Pattern;

public class DeviceMacPair {
    private static final char DefaultSplitChar = ':';
    private static final int ListLength = 6;
    public static final String DEFAULT_MAC_START = "00:00:00:00:00:00";
    public static final String DEFAULT_MAC_END = "FF:FF:FF:FF:FF:FF";
    private char mSplitChar;
    public int[] macStartIntList = new int[ListLength], macEndIntList = new int[ListLength];

    public DeviceMacPair(){
        this(DEFAULT_MAC_START, DEFAULT_MAC_END, DefaultSplitChar);
    }

    public DeviceMacPair(String start, String end, char split){
        mSplitChar = split;
        Pattern pattern = Pattern.compile(
                "([0-9A-F]{2}" + mSplitChar + "){" + (ListLength - 1) + "}[0-9A-F]{2}"); //MAC地址的正则表达式验证

        if(pattern.matcher(start).matches())
            macStartIntList = toIntList(start);
        else
            throw new StringFormatError();

        if(pattern.matcher(end).matches())
            macEndIntList = toIntList(end);
        else
            throw new StringFormatError();

    }

    public DeviceMacPair(String start, String end){
        this(start, end, DefaultSplitChar);
        mSplitChar = DefaultSplitChar;
    }

    private int[] toIntList(String str){
        int[] tempList = new int[ListLength];
        String strList[] = str.split(mSplitChar + "");
        for(int i = 0;i < ListLength; i++)
            tempList[i] = Integer.parseInt(strList[i],16);  //将十六进制的MAC地址转换为int

        return tempList;
    }

    public boolean inInterval(String sourceMac){
        int[] sourceIntList = toIntList(sourceMac);
        //若源MAC地址中的任一位不在目标MAC地址范围内，则返回false
        for(int i = 0;i < ListLength; i++)
            if(sourceIntList[i] < this.macStartIntList[i]
                    && sourceIntList[i] > this.macEndIntList[i])
                return false;

        //反之，均在范围内则返回true
        return true;
    }

    private class StringFormatError extends Error{}
}
