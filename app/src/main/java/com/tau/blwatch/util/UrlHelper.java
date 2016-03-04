package com.tau.blwatch.util;

public class UrlHelper {
    //测试服务器
    //TODO：记录所有的Action字段
//	public static final String url = "http://115.28.169.230:9000/";// 外网
    public static final String url = "http://mobile.yczhyx.com/";
	public static final String imgurl = url;
	public static final String health_ask_url = url + "Phone/uploadHandler.ashx/";
    public static final String login_url = url + "Phone/LoginHandler.ashx";
    public static final String upload_data_url = url + "MobileHandler/MobileHandler.ashx";

    public static final String login_action = "Phone_Login";
    public static final String upload_heart_action = "HeartRateMeter";
    public static final String upload_step_action = "StepTable";

    public static final String LOGIN_LOCAL_ROOT = "taunioned_test";
    public static final String LOGIN_LOCAL_ROOT_PW = "taunionotto";
    public static final String LOGIN_LOCAL_ROOT_PHONE = "18366000000";
}
