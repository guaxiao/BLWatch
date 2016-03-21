package com.tau.blwatch.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tau.blwatch.MainActivity;
import com.tau.blwatch.R;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.fragment.base.BaseListFragment;
import com.tau.blwatch.util.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lecho.lib.hellocharts.listener.PieChartOnValueSelectListener;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.PieChartView;

//TODO：实现导航列表的功能
//TODO：完善饼图关于每日步数目标与已完成步数的显示

public class MainFragment extends BaseListFragment{
    private FragmentJumpController mFragmentJumpController;

    private int[][] mMessageContent = {
            {R.string.main_list_title_search,R.string.main_list_gist_search},
            {R.string.main_list_title_history,R.string.main_list_gist_history}};

    private static final int DEFAULT_R_VALUE = -1;

    private PieChartView chart;
    private PieChartData data;

    /**
     * 跳转条目列表视图
     */
    private ListView mListView;
    /**
     * 跳转条目列表适配器
     */
    private MessageItemListAdapter mAdapter;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_main, container, false);

        //初始化设备列表
        mListView = (ListView) fragmentView.findViewById(android.R.id.list);
        //初始化设备列表adapter
        mAdapter = new MessageItemListAdapter();
        mListView.setAdapter(mAdapter);

        setListAdapter(mAdapter);
        onCreateList();

        chart = (PieChartView) fragmentView.findViewById(R.id.chart);
        chart.setOnValueTouchListener(new ValueTouchListener());

        generateData();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFab_bottom.hide();
        mFab_top.hide();
        mFab_bottom_stop.hide();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentJumpController = (FragmentJumpController) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FragmentJumpController");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentJumpController = null;
    }

    //------------------------------------------ListView--------------------------------------------

    private void onCreateList(){
        //另开线程处理数据，为未来在线更新list做预备，以免阻塞UI线程
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //添加Message

                //提醒数据观察者，同步数据
                mAdapter.notifyDataSetChanged();
            }
        });

        //添加静态的list内容
        for(int[] content:mMessageContent)
            mAdapter.addDevice(new MainMessageItem(content[0],content[1]));

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        Log.i("onListItemClick", "position");
        if (null != mFragmentJumpController) {
            final MainMessageItem messageItem = (MainMessageItem)mAdapter.getItem(position);
            if (messageItem == null)
                return;
            else{
                switch (messageItem.getMsgTitle()){
                    case R.string.main_list_title_search:
                        mFragmentJumpController.onJumpToDeviceTypeList(mUserInfo, mBluetoothDevice, mCreateFlag, this.getClass());
                        break;
                    case R.string.main_list_title_history:
                        mFragmentJumpController.onJumpToDeviceHistory(mUserInfo, mBluetoothDevice, mCreateFlag, this.getClass());
                        break;
                }
            }
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }


    static class ViewHolder {
        TextView messageTitle;
        TextView messageGist;
    }


    //构建设备列表的adapter的内部类
    private class MessageItemListAdapter extends BaseAdapter {
        /**
         * 存储信息列表
         */
        private ArrayList<MainMessageItem> mMsgItem;
        private LayoutInflater mInflater;

        public MessageItemListAdapter() {
            super();
            mMsgItem = new ArrayList<>();
            mInflater = getActivity().getLayoutInflater();
        }

        /**
         * 向MessageItemList中添加新的信息条目
         * @param item    设备信息
         */
        public void addDevice(MainMessageItem item) {
            if(!mMsgItem.contains(item)) {
                mMsgItem.add(item);
            }
        }

        public void clear() {
            mMsgItem.clear();
        }

        @Override
        public int getCount() {
            return mMsgItem.size();
        }

        @Override
        public Object getItem(int i) {
            return mMsgItem.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            //若此view不为空
            if (view == null) {
                //将view替代为自定义item
                view = mInflater.inflate(R.layout.listitem_main, null);
                //使用ViewHolder获取并控制自定义item中的各个控件
                viewHolder = new ViewHolder();
                viewHolder.messageGist = (TextView) view.findViewById(R.id.message_gist);
                viewHolder.messageTitle = (TextView) view.findViewById(R.id.message_title);
                //将ViewHolder装入item
                view.setTag(viewHolder);
            } else {
                //若view为空则装入默认的tag
                viewHolder = (ViewHolder) view.getTag();
            }

            //获取相应标号的MainMessageItem
            MainMessageItem mainMessageItem = mMsgItem.get(i);
            //若item信息不为空
            if(mainMessageItem != null){
                //若item的title非缺省，则装入定义好了的title
                if (mainMessageItem.getMsgTitle() != DEFAULT_R_VALUE)
                    viewHolder.messageTitle.setText(mainMessageItem.getMsgTitle());
                else    //否则标记title为缺省
                    viewHolder.messageTitle.setText(R.string.warning_text);

                //若item的gist非缺省，则装入定义好了的title
                if(mainMessageItem.getMsgGist() != DEFAULT_R_VALUE)
                    viewHolder.messageGist.setText(mainMessageItem.getMsgGist());
                else    //否则标记gist为缺省
                    viewHolder.messageGist.setText(R.string.nil_text);
            }else{  //若item信息为空，则将title与gist标记为缺省
                viewHolder.messageTitle.setText(R.string.warning_text);
                viewHolder.messageGist.setText(R.string.nil_text);
                Log.e(TAG, "listView get null item");
            }
            return view;
        }
    }

    public class MainMessageItem{
        private int msgTitle = DEFAULT_R_VALUE;
        private int msgGist = DEFAULT_R_VALUE;

        public MainMessageItem(int msgTitle, int msgGist){
            this.msgTitle = msgTitle;
            this.msgGist = msgGist;
        }

        public int getMsgTitle() {
            return msgTitle;
        }

        public void setMsgTitle(int msgTitle) {
            this.msgTitle = msgTitle;
        }

        public int getMsgGist() {
            return msgGist;
        }


        public void setMsgGist(int msgGist) {
            this.msgGist = msgGist;
        }
    }

    //-----------------------------------------helloCharts------------------------------------------

    private void generateData() {
        int numValues = 6;

        List<SliceValue> values = new ArrayList<>();
        for (int i = 0; i < numValues; ++i) {
            SliceValue sliceValue = new SliceValue((float) Math.random() * 30 + 15, ChartUtils.pickColor());
            values.add(sliceValue);
        }

        data = new PieChartData(values);

        data.setHasLabels(true);
        data.setHasCenterCircle(true);

        data.setCenterText1("0000");
        // Get roboto-italic font.
        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "MIUI-Bold.ttf");
        data.setCenterText1Typeface(tf);
        data.setCenterText1Color(R.color.miGray);
        // Get font size from dimens.xml and convert it to sp(library uses sp values).
        data.setCenterText1FontSize(ChartUtils.px2sp(getResources().getDisplayMetrics().scaledDensity,
                (int) getResources().getDimension(R.dimen.pie_chart_text1_size)));

        data.setCenterText2("need update");
//        tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Italic.ttf");
//        data.setCenterText2Typeface(tf);
        data.setCenterText2Color(R.color.miGray);
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources().getDisplayMetrics().scaledDensity,
                (int) getResources().getDimension(R.dimen.pie_chart_text2_size)));

        chart.setPieChartData(data);
    }

    /**
     * To animate values you have to change targets values and then call
     * {@link lecho.lib.hellocharts.view.PieChartView#startDataAnimation()}
     * method(don't confuse with View.animate()).
     */
    private void prepareDataAnimation() {
        for (SliceValue value : data.getValues()) {
            value.setTarget((float) Math.random() * 30 + 15);
        }
    }

    private class ValueTouchListener implements PieChartOnValueSelectListener {

        @Override
        public void onValueSelected(int arcIndex, SliceValue value) {
            Toast.makeText(getActivity(), "Selected: " + value, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

    }
}
