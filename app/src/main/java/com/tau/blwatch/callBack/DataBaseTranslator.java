package com.tau.blwatch.callBack;

import java.util.ArrayList;
import java.util.HashMap;

public interface DataBaseTranslator {
    HashMap<String,ArrayList<Float>> onSelectChartData(long startTime, int numBlock, String typeTimeBlock, String tableName);
    ArrayList<String> onSelectMacPair(String deviceType, String tableName);
    void onSendHeartToDB(int avgHeart, int maxHeart, int minHeart);
    void onSendStepToDB(int countStep);
    void onSendWeightToDB(double countWeight);
//        void onSQLTest();
}
