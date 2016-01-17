package com.tau.blwatch.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tau.blwatch.R;
import com.tau.blwatch.util.UserEntity;
import com.tau.blwatch.util.UrlHelper;
import com.tau.blwatch.util.SharePrefUtil;
import com.tau.blwatch.util.FormKeyHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmax.dialog.SpotsDialog;

public class LoginFragment extends Fragment {
    private static final String	TAG		= "LoginFragment";

    private static final String ARG_USERINFO = "userInfo";
    private static final String ARG_LASTFRAGMENT = "lastFragment";
    private static final String ARG_DEVICE = "bluetoothDevice";

    private UserEntity mUserInfo;
    private String mLastFragment;
    private static BluetoothDevice mBluetoothDevice;

    private OnJumpToOtherFragmentCallBack mJumpCallBack;

    private FloatingActionButton mFab_bottom, mFab_top, mFab_bottom_stop;

    private EditText mUserIdEditText,mUserPWordEditText;
    private Button mLoginButton;

    private static AlertDialog mChartLoadingDialog;

    private String mUserName,mUserPWordMD5;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userInfo 用户信息，为空或NULL时代表用户未登录.
     * @param device 设备信息的序列化
     * @param lastFragment 跳转源页面.
     * @return A new instance of fragment WalkFragment.
     */
    public static LoginFragment newInstance(UserEntity userInfo, BluetoothDevice device, String lastFragment) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USERINFO, userInfo);
        args.putParcelable(ARG_DEVICE, device);
        args.putString(ARG_LASTFRAGMENT, lastFragment);
        fragment.setArguments(args);
        return fragment;
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mJumpCallBack = (OnJumpToOtherFragmentCallBack) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserInfo = getArguments().getParcelable(ARG_USERINFO);
            mBluetoothDevice = getArguments().getParcelable(ARG_DEVICE);
            mLastFragment = getArguments().getString(ARG_LASTFRAGMENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_login, container, false);

        //定义浮动按钮
        mFab_bottom = (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom);
        mFab_top = (FloatingActionButton) getActivity().findViewById(R.id.fab_top);
        mFab_bottom_stop= (FloatingActionButton) getActivity().findViewById(R.id.fab_bottom_stop);

        mUserIdEditText = (EditText)fragmentView.findViewById(R.id.login_user_id);
        mUserPWordEditText = (EditText)fragmentView.findViewById(R.id.login_user_passwd);

        String historyUserName = SharePrefUtil.getString(getActivity(), FormKeyHelper.user_name, null);
        if(historyUserName != null){
            mUserIdEditText.setText(historyUserName);
            mUserPWordEditText.requestFocus();
        }

        mLoginButton = (Button)fragmentView.findViewById(R.id.login_button);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUserName = mUserIdEditText.getText().toString();
                mUserPWordMD5 = mUserPWordEditText.getText().toString();

                if (mUserName.equals("") || mUserPWordMD5.equals("")) {
                    Toast.makeText(getActivity(), "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                mChartLoadingDialog = new SpotsDialog(getActivity(), "正在登录...");
                mChartLoadingDialog.show();

                onLoginCenter();
            }
        });

        //TODO：对“注册”与“忘记密码”（或其他功能）站位TextView的功能完善
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LoginFragmrnt", "onResume");
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

    private void onLoginCenter(){
        RequestParams requestParams = new RequestParams(UrlHelper.login_url);
        requestParams.addHeader("ContentType", "application/json");
        requestParams.addBodyParameter(FormKeyHelper.action, UrlHelper.login_action);
        requestParams.addBodyParameter(FormKeyHelper.user_name, mUserName);
        requestParams.addBodyParameter(FormKeyHelper.user_password, mUserPWordMD5);

        x.http().post(requestParams, new Callback.CommonCallback<String>() {

            @Override
            public void onSuccess(String result) {
                Log.d("onSuccess", "login result = " + result);
                mChartLoadingDialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    boolean isJSONSuccess = jsonObject.getBoolean("status");
                    if (isJSONSuccess) {
                        String userId = jsonObject.getString(FormKeyHelper.user_id);
                        String userImg = jsonObject.getString(FormKeyHelper.user_imageUrl);

                        SharePrefUtil.saveBoolean(getActivity(), FormKeyHelper.is_login, true);
                        SharePrefUtil.saveString(getActivity(), FormKeyHelper.user_id, userId);
                        SharePrefUtil.saveString(getActivity(), FormKeyHelper.user_name, mUserName);
                        SharePrefUtil.saveString(getActivity(), FormKeyHelper.user_imageUrl, userImg);

                        mJumpCallBack.onJumpToMainFragment(null, mUserInfo);
                    } else {
                        Toast.makeText(getActivity(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                Log.d("onError", ex.getMessage());
                Toast.makeText(getActivity(), "登录失败请重试", Toast.LENGTH_SHORT).show();
                mChartLoadingDialog.dismiss();
            }

            @Override
            public void onCancelled(CancelledException cex) {
                Toast.makeText(getActivity(), "登录失败请重试", Toast.LENGTH_SHORT).show();
                mChartLoadingDialog.dismiss();
            }

            @Override
            public void onFinished() {
                Log.d("post", "onFinished");
            }
        });
        Log.d("post","posting");
    }

    /** @param source 需要加密的字符串
     *  @param hashType  加密类型 （MD5 和 SHA）
     *  @return
     */
    public static String getHash(String source, String hashType) {
        // 用来将字节转换成 16 进制表示的字符
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuilder sb = new StringBuilder();
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(hashType);
            md5.update(source.getBytes());
            byte[] encryptStr = md5.digest();
            for (int i = 0; i < encryptStr.length; i++) {
                int iRet = encryptStr[i];
                if (iRet < 0) {
                    iRet += 256;
                }
                int iD1 = iRet / 16;
                int iD2 = iRet % 16;
                sb.append(hexDigits[iD1] + "" + hexDigits[iD2]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    //-----------------------------------------interface--------------------------------------------

    /**
     * 回调：跳转至其他界面
     */
    public interface OnJumpToOtherFragmentCallBack {
        void onJumpToMainFragment(BluetoothDevice device, UserEntity userInfo);
    }
}
