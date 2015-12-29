package com.tau.blwatch;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
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
    private int numberOfPoints = 30;

    float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];



    private int numberOfData = 3;

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
    private String[] mTimeDataList = {"总计","最近一年","近三个月","近一个月","最近一周","最近一天"};
    private String[] mTypeDataList = {"    综合    ","    步数    ","    心率    ","    体重    "};
    private LineRecyclerAdapter mTimeRecyclerAdapter,mTypeRecyclerAdapter;

    private LineChartData mLineChartData = null;
    private ColumnChartData mColumnChartData = null;

    public final static long MI_SECOND_YEAR = (long)365 * 24 * 3600 * 1000;
    public final static long MI_SECOND_QUARTER = (long)365 * 24 * 3600 * 1000;
    public final static long MI_SECOND_MONTH = (long)DAY_PER_MONTH * 24 * 3600 * 1000;
    public final static long MI_SECOND_WEEK = (long)7 * 24 * 3600 * 1000;
    public final static long MI_SECOND_DAY = (long)24 * 3600 * 1000;

    public final static int CHART_TIME_SUM = 0;
    public final static int CHART_TIME_YEAR = 1;
    public final static int CHART_TIME_QUARTER = 2;
    public final static int CHART_TIME_MONTH = 3;
    public final static int CHART_TIME_WEEK = 4;
    public final static int CHART_TIME_DAY = 5;

    public final static int CHART_TYPE_SUM = 0;
    public final static int CHART_TYPE_STEP = 1;
    public final static int CHART_TYPE_HEART = 2;
    public final static int CHART_TYPE_WEIGHT = 3;

    private int mTimeOfChart = 1;
    private int mTypeOfChart = 1;


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
        //绘制图表
        generateValues();
        generateData();
        addLineToData();

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
                prepareDataAnimation();
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

    private void generateValues() {
        HashMap<String,ArrayList<Float>> tempMap = new HashMap<>();
        for (int j = 0; j < POINT_PER_LINE_OF_YEAR; ++j) {
            long miSecondNow = new Date().getTime();
//            tempMap = mSelectDBCallBack.onSelectHeartData(miSecondNow, miSecondNow + MI_SECOND_YEAR, POINT_PER_LINE_OF_YEAR);
//            tempMap = mSelectDBCallBack.onSelectHeartData(miSecondNow - MI_SECOND_YEAR, miSecondNow, 3);    //for test
            mSelectDBCallBack.onSQLTest();
        }
        mStatisticsCollection.putAll(tempMap);
        Log.d("onSelectHeartData",mStatisticsCollection.toString());
    }

    private void generateValues(int tagOfLine) {

    }

    private void generateData() {
        // Chart looks the best when line data and column data have similar maximum viewports.
        data = new ComboLineColumnChartData(generateColumnData(), generateLineData());

        Axis axisX = new Axis();
        data.setAxisXBottom(axisX);

        chart.setComboLineColumnChartData(data);
    }

    private LineChartData generateLineData() {

        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < numberOfLines; ++i) {

            List<PointValue> values = new ArrayList<PointValue>();
            for (int j = 0; j < numberOfPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[i][j]).setLabel("" + (int)randomNumbersTab[i][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLORS[i]);
            line.setCubic(isCubic);
            line.setHasLabels(hasLabels);
            line.setHasLines(hasLines);
            line.setHasPoints(hasPoints);
            lines.add(line);
        }

        LineChartData lineChartData = new LineChartData(lines);

        return lineChartData;

    }

    private LineChartData generateLineData(int tagOfLine) {

        if(tagOfLine > numberOfLines || tagOfLine < 0){
            Toast.makeText(getActivity(), "wrong line ",Toast.LENGTH_SHORT).show();
            return null;
        }else{
            List<Line> lines = new ArrayList<Line>();
            List<PointValue> values = new ArrayList<PointValue>();

            for (int j = 0; j < numberOfPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[tagOfLine][j]).setLabel("" + (int)randomNumbersTab[tagOfLine][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLORS[tagOfLine]);
            line.setCubic(isCubic);
            line.setHasLabels(hasLabels);
            line.setHasLines(hasLines);
            line.setHasPoints(hasPoints);
            lines.add(line);

            LineChartData lineChartData = new LineChartData(lines);

            return lineChartData;
        }
    }

    private ColumnChartData generateColumnData() {
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

    private void addLineToData() {
        if (data.getLineChartData().getLines().size() >= maxNumberOfLines) {
            Toast.makeText(getActivity(), "Samples app uses max 4 lines!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            ++numberOfLines;
        }

        generateData();
    }


    private void prepareDataAnimation() {

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
        HashMap<String,ArrayList<Float>> onSelectHeartData(long startTime, long endTime, int blockNum);
        HashMap<String,ArrayList<Float>> onSelectStepData(long startTime, long endTime, int blockNum);
        HashMap<String,ArrayList<Float>> onSelectWeightData(long startTime, long endTime, int blockNum);
        void onSQLTest();

    }

}
