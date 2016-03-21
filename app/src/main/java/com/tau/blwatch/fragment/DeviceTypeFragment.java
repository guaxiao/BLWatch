package com.tau.blwatch.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tau.blwatch.R;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.fragment.base.BaseListFragment;

import java.util.ArrayList;

public class DeviceTypeFragment extends BaseListFragment {
    private FragmentJumpController mFragmentJumpController;

    private int[][] mMessageContent = {
            {R.mipmap.band,R.string.mi_band_name},
            {R.mipmap.watch,R.string.ptwatch_name},
            {R.mipmap.scale,R.string.ptscale_name}};
    /**
     * 跳转条目列表视图
     */
    private ListView mListView;
    /**
     * 跳转条目列表适配器
     */
    private DeviceTypeListAdapter mAdapter;



    public DeviceTypeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_device_type, container, false);

        //初始化设备列表
        mListView = (ListView) fragmentView.findViewById(android.R.id.list);
        //初始化设备列表adapter
        mAdapter = new DeviceTypeListAdapter();
        mListView.setAdapter(mAdapter);

        setListAdapter(mAdapter);
        onCreateList();

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
            mAdapter.addDevice(new DeviceTypeItem(content[0],content[1]));

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        Log.i("onListItemClick", "position");
        if (null != mFragmentJumpController) {
            final DeviceTypeItem messageItem = (DeviceTypeItem)mAdapter.getItem(position);
            if (messageItem != null){
                putCreateFlagItem(R.string.table_device_type, messageItem.getTypeName());
                Toast.makeText(getActivity(), TAG + "onListItemClick " + position, Toast.LENGTH_SHORT).show();
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
        ImageView typeImg;
        TextView typeName;
    }


    //构建设备列表的adapter的内部类
    private class DeviceTypeListAdapter extends BaseAdapter {
        /**
         * 存储信息列表
         */
        private ArrayList<DeviceTypeItem> mTypeItem;
        private LayoutInflater mInflater;

        public DeviceTypeListAdapter() {
            super();
            mTypeItem = new ArrayList<>();
            mInflater = getActivity().getLayoutInflater();
        }

        /**
         * 向MessageItemList中添加新的信息条目
         * @param item    设备信息
         */
        public void addDevice(DeviceTypeItem item) {
            if(!mTypeItem.contains(item)) {
                mTypeItem.add(item);
            }
        }

        public void clear() {
            mTypeItem.clear();
        }

        @Override
        public int getCount() {
            return mTypeItem.size();
        }

        @Override
        public Object getItem(int i) {
            return mTypeItem.get(i);
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
                view = mInflater.inflate(R.layout.listitem_device_type, null);
                viewHolder = new ViewHolder();
                viewHolder.typeImg = (ImageView) view.findViewById(R.id.typeImageView);
                viewHolder.typeName = (TextView) view.findViewById(R.id.typeNameTextView);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            DeviceTypeItem deviceTypeItem = mTypeItem.get(i);
            if(deviceTypeItem != null){
                viewHolder.typeImg.setImageResource(deviceTypeItem.getTypeImg());
                viewHolder.typeName.setText(deviceTypeItem.getTypeName());
            }else{
                viewHolder.typeImg.setImageResource(R.mipmap.question);
                viewHolder.typeName.setText(R.string.warning_text);
                Log.e(TAG, "listView get null item");
            }
            return view;
        }
    }

    public class DeviceTypeItem{
        private int typeImg;
        private int typeName;

        public DeviceTypeItem(int typeImg, int typeName){
            this.typeImg = typeImg;
            this.typeName = typeName;
        }

        public int getTypeImg() {
            return typeImg;
        }

        public void setTypeImg(int typeImg) {
            this.typeImg = typeImg;
        }

        public int getTypeName() {
            return typeName;
        }

        public void setTypeName(int typeName) {
            this.typeName = typeName;
        }
    }
}
