package com.tau.blwatch;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
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

public class HistoryFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_USERINFO = "userInfo";
    private static final String ARG_DEVICE_NAME = "deviceName";
    private static final String ARG_DEVICE_ADD = "deviceAdd";
    private static final String ARG_LASTFRAGMENT = "lastFragment";

    // TODO: Rename and change types of parameters
    private String mUserInfo;
    private String mDeviceName;
    private String mDeviceAdd;
    private String mLastFragment;

    private OnJumpToOtherFragmentCallBack mJumpCallBack;
    private OnSelectDataBaseCallBack mSelectDBCallBack;

    private FloatingActionButton mFab_bottom, mFab_top, mFab_bottom_stop;

    private ComboLineColumnChartView chart;
    private ComboLineColumnChartData data;

    private int numberOfLines = 1;
    private int maxNumberOfLines = 4;
    private int numberOfPoints = 12;

    float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];

    private int numberOfData = 3;

    private HashMap<String,ArrayList<Float>> mStatisticsCollection = new HashMap<>();

    private boolean hasAxes = false;
    private boolean hasAxesNames = false;
    private boolean hasPoints = true;
    private boolean hasLines = true;
    private boolean isCubic = false;
    private boolean hasLabels = false;

    public final static int RECYCLER_VIEW_OF_TIME = 1;
    public final static int RECYCLER_VIEW_OF_TYPE = 2;

    private RecyclerView mTimeRecyclerView,mTypeRecyclerView;
    private String[] mTimeDataList = {"最近一天","最近一周","近一个月","近三个月","最近一年","总计"};
    private String[] mTypeDataList = {"    步数    ","    心率    ","    体重    ","    综合    "};
    private LineRecyclerAdapter mTimeRecyclerAdapter,mTypeRecyclerAdapter;

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

    private String mTagOfChartTime = null;
    private String mTagOfChartType = null;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userInfo 用户信息，为空或NULL时代表用户未登录.
     * @param deviceName 手表名称，同一时间只能使用一个手表.
     * @param deviceAdd 手表MAC，同一时间只能使用一个手表.
     * @param lastFragment 跳转源页面.
     * @return A new instance of fragment WalkFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HistoryFragment newInstance(String userInfo, String deviceName, String deviceAdd, String lastFragment) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERINFO, userInfo);
        args.putString(ARG_DEVICE_NAME, deviceName);
        args.putString(ARG_DEVICE_ADD, deviceAdd);
        args.putString(ARG_LASTFRAGMENT, lastFragment);
        fragment.setArguments(args);
        return fragment;
    }

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mJumpCallBack = (OnJumpToOtherFragmentCallBack) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnJumpToOtherFragmentCallBack");
        }

        try {
            mSelectDBCallBack = (OnSelectDataBaseCallBack) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectDataBaseCallBack");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserInfo = getArguments().getString(ARG_USERINFO);
            mDeviceName = getArguments().getString(ARG_DEVICE_NAME);
            mDeviceAdd = getArguments().getString(ARG_DEVICE_ADD);
            mLastFragment = getArguments().getString(ARG_LASTFRAGMENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_history, container,
                false);

        //定义浮动按钮
        mFab_bottom = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom);
        mFab_top = (FloatingActionButton) getActivity().findViewById(R.id.fab_top);
        mFab_bottom_stop = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom_stop);
        mFab_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //跳转至设备列表界面
                mJumpCallBack.onJumpToDeviceList(mDeviceName, mDeviceAdd);
                Log.i("FragmentWList", "From " + this.getClass().getSimpleName());
            }
        });

        //初始化图表控件
        chart = (ComboLineColumnChartView) fragmentView.findViewById(R.id.chart);
        chart.setOnValueTouchListener(new ValueTouchListener());
        mTagOfChartType = CHART_TYPE_HEART;
        mTagOfChartTime = CHART_TIME_YEAR;
        //绘制图表
        generateValues(mTagOfChartTime,mTagOfChartType);
        generateChart(mTagOfChartTime,mTagOfChartType);

        //构建时间选择控件
        mTimeRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.id_recycler_view_of_time);
        mTimeRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL, true));
        mTimeRecyclerAdapter = new LineRecyclerAdapter(
                getActivity(),
                mTimeDataList,
                R.id.recycler_text,
                R.layout.item_recycler);
        mTimeRecyclerAdapter.setOnItemClickListener(new LineRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getActivity(), position + " click",
                        Toast.LENGTH_SHORT).show();
                switch (position){
                    case 0:
                        mTagOfChartTime = CHART_TIME_DAY;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 1:
                        mTagOfChartTime = CHART_TIME_WEEK;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 2:
                        mTagOfChartTime = CHART_TIME_MONTH;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 3:
                        mTagOfChartTime = CHART_TIME_QUARTER;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 4:
                        mTagOfChartTime = CHART_TIME_YEAR;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 5:
//                        mTagOfChartTime = CHART_TIME_SUM;
//                        generateData(mTagOfChartTime,mTagOfChartType);
                        break;
                }
            }
        });
        mTimeRecyclerView.setAdapter(mTimeRecyclerAdapter);

        //构建类型选择控件
        mTypeRecyclerView = (RecyclerView) fragmentView.findViewById(R.id.id_recycler_view_of_type);
        mTypeRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL,true));
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
                        mTagOfChartType = CHART_TYPE_WEIGHT;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 1:
                        mTagOfChartType = CHART_TYPE_HEART;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime, mTagOfChartType);
                        break;
                    case 2:
                        mTagOfChartType = CHART_TYPE_STEP;
                        generateValues(mTagOfChartTime,mTagOfChartType);
                        generateChart(mTagOfChartTime,mTagOfChartType);
                        break;
//                    case 3:
//                        generateData();
//                        mTypeOfChart = CHART_TYPE_SUM;
//                        break;
                }
            }
        });
        mTypeRecyclerView.setAdapter(mTypeRecyclerAdapter);


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
        mJumpCallBack = null;
        mFab_top = null;    //注销在fragment下的浮动按钮
        mFab_bottom = null;
        mFab_bottom_stop= null;
    }

    //-----------------------------------------helloCharts------------------------------------------

//    private void generateValues() {
//        mSelectDBCallBack.onSQLTest();
//        Log.d("onSelectHeartData",mStatisticsCollection.toString());
//
//        for (int i = 0; i < maxNumberOfLines; ++i) {
//            for (int j = 0; j < numberOfPoints; ++j) {
//                randomNumbersTab[i][j] = (float) Math.random() * 50f + 5;
//            }
//        }
//    }

    /**
     * 按特定的时间区块类型和图表数据类型，将数据库中的数值放入图表数据Map中
     * @param tagOfTime 时间区块类型
     * @param tagOfType 图表数据类型
     */
    private void generateValues(String tagOfTime,String tagOfType) {
        HashMap<String,ArrayList<Float>> tempMap = new HashMap<>();
        PropOfTimeBlock propOfTimeBlock = getPropOfBlockData(tagOfTime,tagOfType);
        Log.d("propOfTimeBlock",propOfTimeBlock.toString());

        boolean hasCached = false;
        //构建本次构建图表数据时，应当记录入mStatisticsCollection的Key的关键字（即开始时间、时间片数量与时间片类型）
        String selectDataColumnNameKey =
                "StartTime:" + propOfTimeBlock.startTime
                        + "-" + propOfTimeBlock.numBlock + " * " + propOfTimeBlock.typeTimeBlock
                        + "From = " + propOfTimeBlock.tableName;

        for (Map.Entry<String, ArrayList<Float>> entry : mStatisticsCollection.entrySet()) {
            //如果Key的关键字在mStatisticsCollection中的Key列表中已经存在，则记本次构建指令命中缓存
            if (entry.getKey().startsWith(selectDataColumnNameKey))
                hasCached = true;
        }

        //当构建指令未命中缓存时，再向数据库查询数据
        if(!hasCached){
            tempMap = mSelectDBCallBack.onSelectData(
                    propOfTimeBlock.startTime,
                    propOfTimeBlock.numBlock,
                    propOfTimeBlock.typeTimeBlock,
                    propOfTimeBlock.tableName);
            mStatisticsCollection.putAll(tempMap);
            Log.d("mStatisticsCollection",mStatisticsCollection.toString());
        }
    }

//    private void generateData() {
//        // Chart looks the best when line data and column data have similar maximum viewports.
//        data = new ComboLineColumnChartData(generateColumnData(), generateLineData());
//
//        Axis axisX = new Axis();
//        Axis axisY = new Axis().setHasLines(true);
//        data.setAxisXBottom(axisX);
//        data.setAxisYLeft(axisY);
//        chart.setZoomType(ZoomType.HORIZONTAL);
//
//        chart.setComboLineColumnChartData(data);
////        chart.startDataAnimation();
//    }

    /**
     * 按特定图表数据类型，将数据Map中的数据转换成图像
     * @param tagOfType 图表数据类型
     */
    private void generateChart(String tagOfTime,String tagOfType) {
        if(tagOfType.equals(CHART_TYPE_WEIGHT))
            data = new ComboLineColumnChartData(generateColumnData(tagOfTime,tagOfType), null);
        else
            data = new ComboLineColumnChartData(null, generateLineData(tagOfTime,tagOfType));

        Axis axisX = new Axis();
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);

        chart.setComboLineColumnChartData(data);
        final Viewport v = new Viewport(chart.getMaximumViewport());

        switch (tagOfType){
            case CHART_TYPE_HEART:
                v.bottom = 0;
                v.top = v.top * 1.1F;
                chart.setMaximumViewport(v);
                chart.setCurrentViewport(v);
                break;

            case CHART_TYPE_STEP:
                chart.resetViewports();
                break;

            case CHART_TYPE_WEIGHT:
                v.bottom = 0;
                v.top = 140;
                chart.setMaximumViewport(v);
                chart.setCurrentViewport(v);
                break;
        }

        chart.setZoomType(ZoomType.HORIZONTAL);
//        chart.startDataAnimation();
    }

//    private LineChartData generateLineData() {
//        List<Line> lines = new ArrayList<Line>();
//        for (int i = 0; i < numberOfLines; ++i) {
//
//            List<PointValue> values = new ArrayList<PointValue>();
//            for (int j = 0; j < numberOfPoints; ++j) {x34
//                values.add(new PointValue(j, randomNumbersTab[i][j]).setLabel("" + (int)randomNumbersTab[i][j]));
//            }
//
//            Line line = new Line(values);
//            line.setColor(ChartUtils.COLORS[i]);
//            line.setCubic(isCubic);
//            line.setHasLabels(hasLabels);
//            line.setHasLines(hasLines);
//            line.setHasPoints(hasPoints);
//            lines.add(line);
//        }
//
//        LineChartData lineChartData = new LineChartData(lines);
//
//        return lineChartData;
//
//    }

    private LineChartData generateLineData(String tagOfTime,String tagOfType) {
        List<Line> lines = new ArrayList<Line>();
        PropOfTimeBlock propOfTimeBlock = getPropOfBlockData(tagOfTime,tagOfType);

        String selectDataColumnNameKey =
                "StartTime:" + propOfTimeBlock.startTime
                        + "-" + propOfTimeBlock.numBlock + " * " + propOfTimeBlock.typeTimeBlock
                        + "From = " + propOfTimeBlock.tableName;

        boolean hasCached = false;
        int countColor = 0;
        for (Map.Entry<String, ArrayList<Float>> entry : mStatisticsCollection.entrySet()) {
            //如果找到mStatisticsCollection中有关于selectDataColumnNameKey的记录
            if (entry.getKey().startsWith(selectDataColumnNameKey)){
                //将此记录录入line中
                ArrayList<Float> tempList = entry.getValue();
                List<PointValue> values = new ArrayList<PointValue>();

                for(int j = 0; j < tempList.size(); j++){
                    values.add(new PointValue(j, tempList.get(j)));
                }

                Line line = new Line(values);
                //设置line的其他属性
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

        LineChartData lineChartData = new LineChartData(lines);

        return lineChartData;
    }

    private ColumnChartData generateColumnData(String tagOfTime,String tagOfType) {
        int numSubColumns = 1;
        int numColumns = 12;
        // Column can have many subColumns, here by default I use 1 subColumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = 0; i < numColumns; ++i) {

            values = new ArrayList<>();
            for (int j = 0; j < numSubColumns; ++j) {
                values.add(new SubcolumnValue((float) Math.random() * 50 + 5, ChartUtils.COLOR_GREEN));
            }

            columns.add(new Column(values));
        }

        ColumnChartData columnChartData = new ColumnChartData(columns);
        return columnChartData;
    }

    private void prepareDataAnimation(String tagOfTime,String tagOfType) {
        // Line animations
        for (Line line : data.getLineChartData().getLines()) {
            for (PointValue value : line.getValues()) {
                // Here I modify target only for Y values but it is OK to modify X targets as well.
                value.setTarget(value.getX(), (float) Math.random() * 50 + 5);
            }
        }

        // Columns animations
        for (Column column : data.getColumnChartData().getColumns()) {
            for (SubcolumnValue value : column.getValues()) {
                value.setTarget((float) Math.random() * 50 + 5);
            }
        }

        chart.startDataAnimation();
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

    private class PropOfTimeBlock{
        public long startTime = 0L;
        public Calendar calendar;
        public int numBlock,numChartColor;
        public String typeTimeBlock,tableName;

        public PropOfTimeBlock(){
            startTime = new Date().getTime();
            calendar = Calendar.getInstance();
            numBlock = 0;
            numChartColor = 0;
            typeTimeBlock = null;
            tableName = null;
        }

        @Override
        public String toString(){
            return "StartTime:" + startTime + ",NumBlock:" + numBlock + ",NumChartColor:" + numChartColor + ",TypeTimeBlock:" + typeTimeBlock +  ",TableName:" +tableName;
        }
    }

    private PropOfTimeBlock getPropOfBlockData(String tagOfTime,String tagOfType){
        PropOfTimeBlock poTimeBlock = new PropOfTimeBlock();

        switch (tagOfTime){
            case CHART_TIME_YEAR:
                //设置为十二个月前的第一天
                poTimeBlock.calendar.add(Calendar.YEAR,-1);
                poTimeBlock.calendar.set(Calendar.DAY_OF_MONTH, 1);
                poTimeBlock.numBlock = TimeHelper.POINT_PER_LINE_OF_YEAR;
                poTimeBlock.typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_MONTH;
                break;
            case CHART_TIME_QUARTER:
                //设置为三个月前的第一天所在星期的第一天
                poTimeBlock.calendar.add(Calendar.MONTH,-3);
                poTimeBlock.calendar.set(Calendar.DAY_OF_MONTH, 1);
                poTimeBlock.calendar.add(Calendar.DAY_OF_WEEK, 1);
                poTimeBlock.numBlock = TimeHelper.POINT_PER_LINE_OF_QUARTER;
                poTimeBlock.typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_WEEK;
                break;
            case CHART_TIME_MONTH:
                //设置为当月第一天
                poTimeBlock.calendar.set(Calendar.DAY_OF_MONTH, 1);
                poTimeBlock.numBlock = TimeHelper.POINT_PER_LINE_OF_MONTH;
                poTimeBlock.typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_DAY;
                break;
            case CHART_TIME_WEEK:
                //设置为当前星期的第一天
                poTimeBlock.calendar.set(Calendar.DAY_OF_WEEK, 1);
                poTimeBlock.numBlock = TimeHelper.POINT_PER_LINE_OF_WEEK;
                poTimeBlock.typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_DAY;
                break;
            case CHART_TIME_DAY:
                poTimeBlock.numBlock = TimeHelper.POINT_PER_LINE_OF_DAY;
                poTimeBlock.typeTimeBlock = HistoryDBHelper.TYPE_BLOCK_HOUR;
                break;
        }

        //设置为00:00:00时刻
        poTimeBlock.calendar.set(Calendar.HOUR_OF_DAY, 0);
        poTimeBlock.calendar.set(Calendar.MINUTE, 0);
        poTimeBlock.calendar.set(Calendar.SECOND, 0);
        poTimeBlock.calendar.set(Calendar.MILLISECOND, 0);

        poTimeBlock.startTime = poTimeBlock.calendar.getTime().getTime();

        switch (tagOfType){
            case CHART_TYPE_HEART:
                poTimeBlock.tableName = HistoryDBHelper.HEART_TABLE_NAME;
                poTimeBlock.numChartColor = CHART_COLOR_HEART;
                break;
            case CHART_TYPE_STEP:
                poTimeBlock.tableName = HistoryDBHelper.STEP_TABLE_NAME;
                poTimeBlock.numChartColor = CHART_COLOR_STEP;
                break;
            case CHART_TYPE_WEIGHT:
                poTimeBlock.tableName = HistoryDBHelper.WEIGHT_TABLE_NAME;
                poTimeBlock.numChartColor = CHART_COLOR_WEIGHT;
                break;
        }
        return poTimeBlock;
    }


    //----------------------------------------------------------------------------------------------

    /**
     * 回调：跳转至设备列表界面
     */
    public interface OnJumpToOtherFragmentCallBack {
        public void onJumpToDeviceList(String deviceName,String deviceADD);
    }

    /**
     * 回调：从数据库中获取相应数据
     */
    public interface OnSelectDataBaseCallBack {
        HashMap<String,ArrayList<Float>> onSelectData(long startTime, int numBlock, String typeTimeBlock, String tableName);
        void onSQLTest();
    }

}
