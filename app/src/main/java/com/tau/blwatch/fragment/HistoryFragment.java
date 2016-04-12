package com.tau.blwatch.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tau.blwatch.R;
import com.tau.blwatch.callBack.DataBaseTranslator;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.fragment.base.BaseFragment;
import com.tau.blwatch.ui.LineRecyclerAdapter;
import com.tau.blwatch.util.DataBaseSelectHelper;
import com.tau.blwatch.util.HistoryDBHelper;
import com.tau.blwatch.util.TimeBlockEntity;
import com.tau.blwatch.util.TimeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;

public class HistoryFragment extends BaseFragment {
    private static ComboLineColumnChartView chart;
    private static ComboLineColumnChartData data;

    private HashMap<String,ArrayList<Float>> mStatisticsCollection = new HashMap<>();

    private boolean hasAxes = false;
    private boolean hasAxesNames = false;
    private boolean hasPoints = true;
    private boolean hasLines = true;
    private boolean isCubic = false;
    private boolean hasLabels = false;

    private static final int WEIGHT_MAX = 400;

    private RecyclerView mTimeRecyclerView,mTypeRecyclerView;
    private String[] mTimeDataList = {"最近一天","最近一周","近一个月","近三个月","最近一年","    总计    "};
    private String[] mTypeDataList = {"    步数    ","    心率    ","    体重    ","    综合    "};
    private LineRecyclerAdapter mTimeRecyclerAdapter,mTypeRecyclerAdapter;

    private String mTagOfChartTime = null;
    private String mTagOfChartType = null;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_history, container, false);

        mFab_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //跳转至设备列表界面
                mFragmentJumpController.onJumpToDeviceList(mUserInfo, mBluetoothDevice, mCreateFlag, this.getClass());
                Log.i("FragmentWList", "From " + this.getClass().getSimpleName());
            }
        });

        //初始化图表控件
        chart = (ComboLineColumnChartView) fragmentView.findViewById(R.id.chart);
        chart.setOnValueTouchListener(new ValueTouchListener());
        mTagOfChartType = TimeBlockEntity.CHART_TYPE_STEP;
        mTagOfChartTime = TimeBlockEntity.CHART_TIME_DAY;

        //绘制图表
        generateChartThread(mTagOfChartTime, mTagOfChartType);

        //构建时间选择控件
        mTimeRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.id_recycler_view_of_time);
        mTimeRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL, false));
        mTimeRecyclerAdapter = new LineRecyclerAdapter(
                getActivity(),
                mTimeDataList,
                R.id.recycler_text,
                R.layout.item_recycler);
        mTimeRecyclerAdapter.setOnItemClickListener(new LineRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                switch (position) {
                    case 0:
                        mTagOfChartTime = TimeBlockEntity.CHART_TIME_DAY;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTimeRecyclerAdapter.setSelectItem(0);
                        break;
                    case 1:
                        mTagOfChartTime = TimeBlockEntity.CHART_TIME_WEEK;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTimeRecyclerAdapter.setSelectItem(1);
                        break;
                    case 2:
                        mTagOfChartTime = TimeBlockEntity.CHART_TIME_MONTH;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTimeRecyclerAdapter.setSelectItem(2);
                        break;
                    case 3:
                        mTagOfChartTime = TimeBlockEntity.CHART_TIME_QUARTER;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTimeRecyclerAdapter.setSelectItem(3);
                        break;
                    case 4:
                        mTagOfChartTime = TimeBlockEntity.CHART_TIME_YEAR;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTimeRecyclerAdapter.setSelectItem(4);
                        break;
                    case 5:
//                        mTagOfChartTime = CHART_TIME_SUM;
                        mTimeRecyclerAdapter.setSelectItem(5);
                        Toast.makeText(getActivity(), "Developing", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        mTimeRecyclerAdapter.setSelectItem(0);
        mTimeRecyclerView.setAdapter(mTimeRecyclerAdapter);
        mTimeRecyclerView.setHasFixedSize(true);

        //构建类型选择控件
        mTypeRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.id_recycler_view_of_type);
        mTypeRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL,false));
        mTypeRecyclerAdapter = new LineRecyclerAdapter(
                getActivity(),
                mTypeDataList,
                R.id.recycler_text,
                R.layout.item_recycler);
        mTypeRecyclerAdapter.setOnItemClickListener(new LineRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getActivity(), position + " click",
                        Toast.LENGTH_SHORT).show();
                switch (position){
                    case 0:
                        mTagOfChartType = TimeBlockEntity.CHART_TYPE_STEP;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTypeRecyclerAdapter.setSelectItem(0);
                        break;
                    case 1:
                        mTagOfChartType = TimeBlockEntity.CHART_TYPE_HEART;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTypeRecyclerAdapter.setSelectItem(1);
                        break;
                    case 2:
                        mTagOfChartType = TimeBlockEntity.CHART_TYPE_WEIGHT;
                        generateChartThread(mTagOfChartTime, mTagOfChartType);
                        mTypeRecyclerAdapter.setSelectItem(2);
                        break;
                    case 3:
//                        mTypeOfChart = CHART_TYPE_SUM;
                        mTypeRecyclerAdapter.setSelectItem(3);
                        Toast.makeText(getActivity(), "Developing",Toast.LENGTH_SHORT).show();
                        break;

                }
            }
        });
        mTypeRecyclerAdapter.setSelectItem(0);
        mTypeRecyclerView.setAdapter(mTypeRecyclerAdapter);
        mTypeRecyclerView.setHasFixedSize(true);

        return fragmentView;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("HistoryFragment", "onResume");
        mFab_bottom.hide();
        mFab_top.hide();
        mFab_bottom_stop.hide();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    //-----------------------------------------helloCharts------------------------------------------

    public class ChatDrawableTask implements Runnable{
        Handler mHandler;
        String mTagOfTime,mTagOfType;

        public ChatDrawableTask(Handler handler,String tagOfTime,String tagOfType) {
            super();
            mHandler = handler;
            mTagOfTime = tagOfTime;
            mTagOfType = tagOfType;
        }

        @Override
        public void run(){
            generateValues(mTagOfTime, mTagOfType);

            if(mTagOfType.equals(TimeBlockEntity.CHART_TYPE_STEP))
                data = new ComboLineColumnChartData(generateColumnData(mTagOfTime,mTagOfType), null);
            else
                data = new ComboLineColumnChartData(null, generateLineData(mTagOfTime,mTagOfType));

            mHandler.sendMessage(Message.obtain());
        }
    }

    static class ChatDrawableHandler extends Handler {
        String mTagOfTime,mTagOfType;
        public ChatDrawableHandler(String tagOfTime,String tagOfType) {
            super();
            mTagOfTime = tagOfTime;
            mTagOfType = tagOfType;
        }

        @Override
        public void handleMessage(Message msg) {
            // 更新UI
            generateChart(mTagOfTime,mTagOfType);
            mChartLoadingDialog.dismiss();
        }
    }

    /**
     * 按特定的时间区块类型和图表数据类型，将数据库中的数值放入图表数据Map中
     * @param tagOfTime 时间区块类型
     * @param tagOfType 图表数据类型
     */
    private void generateValues(String tagOfTime,String tagOfType) {
        HashMap<String,ArrayList<Float>> tempMap;
        TimeBlockEntity timeBlockEntity = new TimeBlockEntity(tagOfTime,tagOfType);
        Log.d("TimeBlockEntity", timeBlockEntity.toString());

        boolean hasCached = false;
        //构建本次构建图表数据时，应当记录入mStatisticsCollection的Key的关键字（即开始时间、时间片数量与时间片类型）
        String selectDataColumnNameKey = DataBaseSelectHelper.createKeyName(
                timeBlockEntity.startTime,
                timeBlockEntity.numBlock,
                timeBlockEntity.typeTimeBlock,
                timeBlockEntity.tableName,
                false);

        for (Map.Entry<String, ArrayList<Float>> entry : mStatisticsCollection.entrySet()) {
            //如果Key的关键字在mStatisticsCollection中的Key列表中已经存在，则记本次构建指令命中缓存
            if (entry.getKey().startsWith(selectDataColumnNameKey))
                hasCached = true;
        }

        //当构建指令未命中缓存时，再向数据库查询数据
        if(!hasCached){
            tempMap = mDataBaseTranslator.onSelectChartData(
                    timeBlockEntity.startTime,
                    timeBlockEntity.numBlock,
                    timeBlockEntity.typeTimeBlock,
                    timeBlockEntity.tableName);
            mStatisticsCollection.putAll(tempMap);
            Log.d("mStatisticsCollection",mStatisticsCollection.toString());
        }
    }

    /**
     * 按特定图表数据类型，将数据Map中的数据转换成图像
     * @param tagOfType 图表数据类型
     */
    private void generateChartThread(String tagOfTime,String tagOfType) {
        // 创建并启动绘制线程
        Thread darwinThread = new Thread(
                new ChatDrawableTask(
                        new ChatDrawableHandler(tagOfTime,tagOfType)
                        ,tagOfTime,tagOfType));
        darwinThread.start();
        mChartLoadingDialog.show();
    }

    private static class MillionValueFormatter extends SimpleAxisValueFormatter {
        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            value /= 1000 * 1000F;
            Log.d("MillionValueFormatter",value + "");
            return super.formatValueForAutoGeneratedAxis(formattedValue,value,autoDecimalDigits);
        }
    }

    private static class KiloValueFormatter extends SimpleAxisValueFormatter {
        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            value /= 1000F;
            Log.d("KiloValueFormatter",value + "");
            return super.formatValueForAutoGeneratedAxis(formattedValue,value,autoDecimalDigits);
        }
    }

    private static class TenThousandValueFormatter extends SimpleAxisValueFormatter {
        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            value /= 10000F;
            return super.formatValueForAutoGeneratedAxis(formattedValue,value,autoDecimalDigits);
        }
    }

    private static void generateChart(String tagOfTime,String tagOfType){
        Axis axisX = new Axis().setHasLines(true);
        Axis axisY = new Axis().setHasLines(true);
        boolean isWeightOverKilo = false, isWeightOverTenThousand = false,isWeightOverMillion = false;

        ArrayList<AxisValue> axisXValues;
        switch (tagOfTime){
            case TimeBlockEntity.CHART_TIME_YEAR:
                isWeightOverMillion = true;
                isWeightOverTenThousand = true;
                isWeightOverKilo = true;
                axisXValues = new ArrayList<>();
                for (int i = 0; i < TimeHelper.MONTH_OF_YEAR; i++)
                    axisXValues.add(new AxisValue(i).setLabel((i + 1) + ""));
                axisX = new Axis(axisXValues).setHasLines(true);
                break;
            case TimeBlockEntity.CHART_TIME_QUARTER:
                isWeightOverTenThousand = true;
                isWeightOverKilo = true;
                axisXValues = new ArrayList<>();
                for (int i = 0; i < TimeHelper.WEEK_PER_QUARTER; i++)
                    axisXValues.add(new AxisValue(i).setLabel((i + 1) + ""));
                axisX = new Axis(axisXValues).setHasLines(true);
                break;
            case TimeBlockEntity.CHART_TIME_MONTH:
                isWeightOverTenThousand = true;
                isWeightOverKilo = true;
                axisXValues = new ArrayList<>();
                for (int i = 0; i < TimeHelper.DAY_PER_MONTH; i++)
                    axisXValues.add(new AxisValue(i).setLabel((i + 1) + ""));
                axisX = new Axis(axisXValues).setHasLines(true);
                break;
            case TimeBlockEntity.CHART_TIME_WEEK:
                isWeightOverTenThousand = true;
                isWeightOverKilo = true;
                String [] days = {"SUN","MON","TUE","WED","THU","FRI","SAT"};
                axisXValues = new ArrayList<>();
                for (int i = 0; i < days.length; i++)
                    axisXValues.add(new AxisValue(i).setLabel(days[i]));
                axisX = new Axis(axisXValues).setHasLines(true);
                break;
            case TimeBlockEntity.CHART_TIME_DAY:
                break;
        }

        switch (tagOfType){
            case TimeBlockEntity.CHART_TYPE_HEART:
                axisY.setName("心率[/min]");
                break;
            case TimeBlockEntity.CHART_TYPE_STEP:
//                if(isWeightOverMillion)
//                    axisY.setFormatter(new MillionValueFormatter());
////                    axisY.setFormatter(new MillionValueFormatter().setAppendedText("M".toCharArray()));
//                else if(isWeightOverKilo)
//                    axisY.setFormatter(new KiloValueFormatter());
////                    axisY.setFormatter(new KiloValueFormatter().setAppendedText("K".toCharArray()));
                if(isWeightOverTenThousand)
                    axisY.setFormatter(new TenThousandValueFormatter()).setName("步数[万]");
                else
                    axisY.setName("步数");
                break;
            case TimeBlockEntity.CHART_TYPE_WEIGHT:
                axisY.setName("体重[kg]");
                break;
        }

        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);

        chart.setComboLineColumnChartData(data);
        chart.resetViewports();

        Viewport vMax = new Viewport(chart.getMaximumViewport());
        Viewport vCurrent = new Viewport(chart.getCurrentViewport());
        Log.d("Viewport","vMax=" + vMax.toString());
        Log.d("Viewport","vCurrent=" + vCurrent.toString());

        switch (tagOfTime){
            case TimeBlockEntity.CHART_TIME_YEAR:
                vMax.right = TimeHelper.MONTH_OF_YEAR;
                vCurrent.right = TimeHelper.MONTH_OF_YEAR;
                break;
            case TimeBlockEntity.CHART_TIME_QUARTER:
                vMax.right = TimeHelper.WEEK_PER_QUARTER;
                vCurrent.right = TimeHelper.WEEK_PER_QUARTER;
                break;
            case TimeBlockEntity.CHART_TIME_MONTH:
                vMax.right = TimeHelper.DAY_PER_MONTH;
                vCurrent.right = TimeHelper.DAY_PER_MONTH / 2;
                break;
            case TimeBlockEntity.CHART_TIME_WEEK:
                vMax.right = TimeHelper.DAY_PER_WEEK;
                vCurrent.right = TimeHelper.DAY_PER_WEEK;
                break;
            case TimeBlockEntity.CHART_TIME_DAY:
                vMax.right = TimeHelper.HOUR_PER_DAY;
                vCurrent.right = TimeHelper.HOUR_PER_DAY;
                break;
        }

        vMax.right -= 0.5;
        vCurrent.right -= 0.5;

        switch (tagOfType){
            case TimeBlockEntity.CHART_TYPE_STEP:
                break;
            case TimeBlockEntity.CHART_TYPE_HEART:
            case TimeBlockEntity.CHART_TYPE_WEIGHT:
                vMax.left -= 0.5;
                vCurrent.left -= 0.5;
                vMax.bottom = vMax.bottom * 0.96F;
                vMax.top = vMax.top * 1.05F;
                vCurrent.bottom = vCurrent.bottom * 0.96F;
                vCurrent.top = vCurrent.top * 1.05F;
                break;
        }

        chart.setMaximumViewport(vMax);
        chart.setCurrentViewport(vCurrent);

        Log.d("Viewport", "vMax=" + chart.getMaximumViewport().toString());
        Log.d("Viewport", "vCurrent=" + chart.getCurrentViewport().toString());

        chart.setZoomType(ZoomType.HORIZONTAL);
    }

    private LineChartData generateLineData(String tagOfTime,String tagOfType) {
        List<Line> lines = new ArrayList<>();
        TimeBlockEntity timeBlockEntity = new TimeBlockEntity(tagOfTime, tagOfType);

        String selectDataColumnNameKey = DataBaseSelectHelper.createKeyName(
                timeBlockEntity.startTime,
                timeBlockEntity.numBlock,
                timeBlockEntity.typeTimeBlock,
                timeBlockEntity.tableName,
                false);
        String selectDataTimeBlockNameKey = DataBaseSelectHelper.createKeyName(
                timeBlockEntity.startTime,
                timeBlockEntity.numBlock,
                timeBlockEntity.typeTimeBlock,
                timeBlockEntity.tableName,
                true,
                HistoryDBHelper.ARG_TIME_BLOCK);

        boolean hasCached = false;
        int countColor = 0;
        for (Map.Entry<String, ArrayList<Float>> entry : mStatisticsCollection.entrySet()) {
            String dataKey = entry.getKey();
            //如果找到mStatisticsCollection中有关于selectDataColumnNameKey的记录
            if (dataKey.startsWith(selectDataColumnNameKey)){
                //将此记录录入line中
                ArrayList<Float> tempTimeList = mStatisticsCollection.get(selectDataTimeBlockNameKey);
                ArrayList<Float> tempList = entry.getValue();
                List<PointValue> values = new ArrayList<>();

                for(int j = 0; j < tempList.size(); j++){
                    values.add(new PointValue(tempTimeList.get(j), tempList.get(j)));
                }

                Line line = new Line(values);
                //设置line的其他属性
//                switch (dataKey.substring(dataKey.indexOf(HistoryDBHelper.ARG_DATA_TYPE))){
//
//                }
                line.setColor(ChartUtils.COLORS[countColor++]);
                line.setCubic(isCubic);
                line.setHasLabels(hasLabels);
                line.setHasLines(hasLines);
                line.setHasPoints(hasPoints);
                //将line放入图表的line集合中
                lines.add(line);
                //记本次构建绘制命中缓存
                hasCached = true;
            }
        }

        //当绘制指令未命中缓存时，返回null
        if(!hasCached)
            return null;

        return new LineChartData(lines);
    }

    private ColumnChartData generateColumnData(String tagOfTime,String tagOfType) {
//        List<Column> columns = new ArrayList<Column>();
        TimeBlockEntity timeBlockEntity = new TimeBlockEntity(tagOfTime, tagOfType);

        String selectDataColumnNameKey = DataBaseSelectHelper.createKeyName(
                timeBlockEntity.startTime,
                timeBlockEntity.numBlock,
                timeBlockEntity.typeTimeBlock,
                timeBlockEntity.tableName,
                false);
        String selectDataTimeBlockNameKey = DataBaseSelectHelper.createKeyName(
                timeBlockEntity.startTime,
                timeBlockEntity.numBlock,
                timeBlockEntity.typeTimeBlock,
                timeBlockEntity.tableName,
                true,
                HistoryDBHelper.ARG_TIME_BLOCK);

        boolean hasCached = false;
        List<Column> columns = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Float>> entry : mStatisticsCollection.entrySet()) {
            String dataKey = entry.getKey();
            //如果找到mStatisticsCollection中有关于selectDataColumnNameKey的记录
            if (dataKey.startsWith(selectDataColumnNameKey)){
                ArrayList<Float> tempTimeList = mStatisticsCollection.get(selectDataTimeBlockNameKey);
                ArrayList<Float> tempList = entry.getValue();
                List<SubcolumnValue> values;

                int countOfList = 0;
                for(int j = 0; j < timeBlockEntity.numBlock; j++){
                    values = new ArrayList<>();
                    SubcolumnValue subcolumnValue;
                    if(tempTimeList.get(countOfList) == j) {
                        subcolumnValue = new SubcolumnValue(tempList.get(countOfList), ChartUtils.COLOR_GREEN);
                        countOfList ++;
                    } else
                        subcolumnValue = new SubcolumnValue(0, ChartUtils.COLOR_GREEN);

                    values.add(subcolumnValue);
                    columns.add(new Column(values));
                }
                //记本次构建绘制命中缓存
                hasCached = true;
            }
        }
        //当绘制指令未命中缓存时，返回null
        if(!hasCached)
            return null;

        return new ColumnChartData(columns);
    }

    private class ValueTouchListener implements ComboLineColumnChartOnValueSelectListener {

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onColumnValueSelected(int columnIndex, int subColumnIndex, SubcolumnValue value) {
            Toast.makeText(getActivity(), "Selected column: " + value, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPointValueSelected(int lineIndex, int pointIndex, PointValue value) {
            Toast.makeText(getActivity(), "Selected line point: " + value, Toast.LENGTH_SHORT).show();
        }

    }
}
