package com.tau.blwatch.fragment.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.tau.blwatch.R;
import com.tau.blwatch.callBack.BackThreadController;
import com.tau.blwatch.callBack.DataBaseTranslator;
import com.tau.blwatch.callBack.FragmentJumpController;
import com.tau.blwatch.util.UserEntity;

import java.util.ArrayList;
import java.util.HashMap;

import dmax.dialog.SpotsDialog;

/**
 * 一般fragment的基类
 * 实现fragment的工厂模式生产，公用UI组件的控制等
 */
public class BaseFragment extends Fragment {
    protected final String TAG = getClass().getSimpleName();
    private static final String TAG_THIS = "BaseFragment";

    protected FragmentJumpController mFragmentJumpController;
    protected BackThreadController mBackThreadController;
    protected DataBaseTranslator mDataBaseTranslator;

    protected static final String ARG_USER_INFO = "userInfo";
    protected static final String ARG_DEVICE = "bluetoothDevice";
    protected static final String ARG_FLAG = "createFlag";
    protected static final String ARG_LAST_FRAGMENT = "lastFragment";

    protected UserEntity mUserInfo;
    protected static BluetoothDevice mBluetoothDevice;
    protected HashMap<Integer,ArrayList<Integer>> mCreateFlag;
    protected Class mLastFragment;

    protected FloatingActionButton mFab_bottom, mFab_top, mFab_bottom_stop;

    protected static AlertDialog mChartLoadingDialog;


    /**
     * 使用工厂模式的思想产生fragment，并使用反射实例化泛型以返回子fragment的实例
     *
     * @param clazz 子类型的Class
     * @param userInfo 用户信息，为空或NULL时代表用户未登录.
     * @param device 设备信息的序列化
     * @param createFlag 页面的其他详细设置
     * @param lastFragment 跳转源页面.
     * @return A new instance of fragment extends BaseFragment.
     */
    public static <T extends BaseFragment> T newInstance(Class<T> clazz,
                                                         UserEntity userInfo, BluetoothDevice device,
                                                         HashMap<Integer,ArrayList<Integer>> createFlag, Class lastFragment) {
        Log.d(TAG_THIS, "newInstance from " + clazz.getSimpleName());
        try {
            T fragment = clazz.newInstance();
            Bundle args = new Bundle();
            args.putParcelable(ARG_USER_INFO, userInfo);
            args.putParcelable(ARG_DEVICE, device);
            args.putSerializable(ARG_FLAG, createFlag);
            args.putSerializable(ARG_LAST_FRAGMENT, lastFragment);
            fragment.setArguments(args);
            return fragment;

        } catch (java.lang.InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
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

        try {
            mBackThreadController = (BackThreadController) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement BackThreadController");
        }

        try {
            mDataBaseTranslator = (DataBaseTranslator) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DataBaseTranslator");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        //基于工厂模式的思想，在fragment进行到onCreate时将序列化参数传递至全局变量中
        if (getArguments() != null) {
            mUserInfo = getArguments().getParcelable(ARG_USER_INFO);
            mBluetoothDevice = getArguments().getParcelable(ARG_DEVICE);
            mCreateFlag = (HashMap<Integer,ArrayList<Integer>>)getArguments().getSerializable(ARG_FLAG);
            mLastFragment = (Class)getArguments().getSerializable(ARG_LAST_FRAGMENT);
        }

        //初始化SpotsDialog
        mChartLoadingDialog = new SpotsDialog(getActivity());

        //定义浮动按钮
        mFab_bottom = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom);
        mFab_top = (FloatingActionButton) getActivity().findViewById(R.id.fab_top);
        mFab_bottom_stop = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom_stop);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach");
        //注销在fragment下的浮动按钮
        mFab_top = null;
        mFab_bottom = null;
        mFab_bottom_stop= null;

        mFragmentJumpController = null;
        mBackThreadController = null;
        mDataBaseTranslator = null;
    }

    protected void putCreateFlagItem(int key, int item){
        checkCreateFlag();
        ArrayList<Integer> flagList = getCreateFlagList(key);

        if(flagList.indexOf(item) != -1)
            flagList.add(item);
    }

    protected void addCreateFlagItem(int key, int item){
        checkCreateFlag();
        ArrayList<Integer> flagList = getCreateFlagList(key);

        flagList.add(item);
        mCreateFlag.put(key,flagList);
    }

    protected boolean hasCreateFlagItem(int key, int item){
        checkCreateFlag();
        ArrayList<Integer> flagList = getCreateFlagList(key);

        return (flagList.indexOf(item) != -1);
    }

    protected void checkCreateFlag(){
        if(mCreateFlag == null)
            mCreateFlag = new HashMap<>();
    }

    protected ArrayList<Integer> getCreateFlagList(int key){
        if(mCreateFlag.get(key) == null)
            return new ArrayList<>();
        else
            return mCreateFlag.get(key);
    }
}
