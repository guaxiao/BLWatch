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
import com.tau.blwatch.fragment.base.BaseListFragment;
import com.tau.blwatch.util.UserEntity;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.listener.PieChartOnValueSelectListener;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.PieChartView;

//TODO：实现导航列表的功能
//TODO：完善饼图关于每日步数目标与已完成步数的显示

public class MainFragment extends BaseListFragment{
    private OnJumpToOtherFragmentCallBack mJumpCallBack;

    private String[][] mMessageContent = {
            {MainActivity.NAME_DeviceTypeListFragment_JUMP,"搜索周围设备","按类型搜索"},
            {MainActivity.NAME_DeviceHistoryFragment_JUMP,"快速连接","根据历史记录完成连接"}};

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
            mJumpCallBack = (OnJumpToOtherFragmentCallBack) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnChooseMessageItemCallBack");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mJumpCallBack = null;
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
        for(String[] content:mMessageContent)
            mAdapter.addDevice(new MainMessageItem(content[0],content[1],content[2]));

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        Log.i("onListItemClick", "position");
        if (null != mJumpCallBack) {
            final MainMessageItem messageItem = (MainMessageItem)mAdapter.getItem(position);
            if (messageItem == null)
                return;
            else{
                switch (messageItem.getFragmentName()){
                    case MainActivity.NAME_DeviceTypeListFragment_JUMP:
                        mJumpCallBack.onJumpToDeviceTypeList(mBluetoothDevice, mUserInfo);
                        break;
                    case MainActivity.NAME_DeviceHistoryFragment_JUMP:
                        mJumpCallBack.onJumpToDeviceHistory(mBluetoothDevice, mUserInfo);
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
            // General ListView optimization code.
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_main, null);
                viewHolder = new ViewHolder();
                viewHolder.messageGist = (TextView) view.findViewById(R.id.message_gist);
                viewHolder.messageTitle = (TextView) view.findViewById(R.id.message_title);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            MainMessageItem fragmentName = mMsgItem.get(i);
            if(fragmentName != null){
                if (fragmentName.getMsgTitle() != null && fragmentName.getMsgTitle().length() > 0)
                    viewHolder.messageTitle.setText(fragmentName.getMsgTitle());
                else
                    viewHolder.messageTitle.setText(R.string.unknown_device);

                viewHolder.messageGist.setText(fragmentName.getMsgGist());
            }else{
                viewHolder.messageTitle.setText(R.string.warning_list_item_title);
                viewHolder.messageGist.setText(R.string.warning_list_item_content);
                Log.e(TAG, "listView get null item");
            }
            return view;
        }
    }

    public class MainMessageItem{
        private String fragmentName;
        private String msgTitle;
        private String msgGist;

        public MainMessageItem(String fragmentName, String msgTitle, String msgGist){
            this.fragmentName = fragmentName;
            this.msgTitle = msgTitle;
            this.msgGist = msgGist;
        }

        public String getFragmentName() {
            return fragmentName;
        }

        public void setFragmentName(String fragmentName) {
            this.fragmentName = fragmentName;
        }

        public String getMsgTitle() {
            return msgTitle;
        }

        public void setMsgTitle(String msgTitle) {
            this.msgTitle = msgTitle;
        }

        public String getMsgGist() {
            return msgGist;
        }

        public void setMsgGist(String msgGist) {
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

    //-----------------------------------------interface--------------------------------------------

    /**
     * 回调：跳转至其他界面
     */
    public interface OnJumpToOtherFragmentCallBack {
        void onJumpToDeviceList(BluetoothDevice device, UserEntity userInfo);
        void onJumpToHistoryTable(BluetoothDevice device, UserEntity userInfo);
        void onJumpToDeviceTypeList(BluetoothDevice device, UserEntity userInfo);
        void onJumpToDeviceHistory(BluetoothDevice device, UserEntity userInfo);
    }
}
