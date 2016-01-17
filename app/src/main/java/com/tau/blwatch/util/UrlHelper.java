package com.tau.blwatch.util;

public class UrlHelper {
    //测试服务器
    //TODO：记录所有的Action字段
	public static String url = "http://115.28.169.230:9000/";// 外网
//    public static String url = "http://mobile.yczhyx.com/";
	public static String imgurl = url;
	public static String health_ask_url = url + "Phone/uploadHandler.ashx/";
    public static String login_url = url + "Phone/LoginHandler.ashx";
    public static String upload_data_url = url + "MobileHandler/MobileHandler.ashx";

    public static String login_action = "Phone_Login";
    public static String upload_heart_action = "HeartRateMeter";
    public static String upload_step_action = "StepTable";
}
