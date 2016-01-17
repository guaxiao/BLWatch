package com.tau.blwatch.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.binary.Base64;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class SharePrefUtil {
	private static String tag = SharePrefUtil.class.getSimpleName();
	private final static String SP_NAME = "config";
	private static SharedPreferences sp;

	/**
	 * 保存布尔值
	 * 
	 * @param context 根控件
	 * @param key   键值
	 * @param value 数值
	 */
	public static void saveBoolean(Context context, String key, boolean value) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		sp.edit().putBoolean(key, value).apply();
	}

	/**
	 * 保存字符串
	 *
     * @param context 根控件
     * @param key   键值
     * @param value 数值
	 */
	public static void saveString(Context context, String key, String value) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		sp.edit().putString(key, value).apply();

	}

	public static void clear(Context context) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		sp.edit().clear().apply();
	}

	/**
	 * 保存long型
	 *
     * @param context 根控件
     * @param key   键值
     * @param value 数值
	 */
	public static void saveLong(Context context, String key, long value) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		sp.edit().putLong(key, value).apply();
	}

	/**
	 * 保存int型
	 *
     * @param context 根控件
     * @param key   键值
     * @param value 数值
	 */
	public static void saveInt(Context context, String key, int value) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		sp.edit().putInt(key, value).apply();
	}

	/**
	 * 保存float型
	 *
     * @param context 根控件
     * @param key   键值
     * @param value 数值
	 */
	public static void saveFloat(Context context, String key, float value) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		sp.edit().putFloat(key, value).apply();
	}

	/**
	 * 获取字符值
	 *
     * @param context 根控件
     * @param key   键值
     * @param defValue 若键值不存在则返回此默认值
	 * @return  数值
	 */
	public static String getString(Context context, String key, @Nullable String defValue) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		return sp.getString(key, defValue);
	}

	/**
	 * 获取int值
	 *
     * @param context 根控件
     * @param key   键值
     * @param defValue 若键值不存在则返回此默认值
     * @return  数值
	 */
	public static int getInt(Context context, String key, int defValue) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		return sp.getInt(key, defValue);
	}

	/**
	 * 获取long值
	 *
     * @param context 根控件
     * @param key   键值
     * @param defValue 若键值不存在则返回此默认值
     * @return  数值
	 */
	public static long getLong(Context context, String key, long defValue) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		return sp.getLong(key, defValue);
	}

	/**
	 * 获取float值
	 *
     * @param context 根控件
     * @param key   键值
     * @param defValue 若键值不存在则返回此默认值
     * @return  数值
	 */
	public static float getFloat(Context context, String key, float defValue) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		return sp.getFloat(key, defValue);
	}

	/**
	 * 获取布尔值
	 *
     * @param context 根控件
     * @param key   键值
     * @param defValue 若键值不存在则返回此默认值
     * @return  数值
	 */
	public static boolean getBoolean(Context context, String key, boolean defValue) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		return sp.getBoolean(key, defValue);
	}

	/**
	 * 将对象进行base64编码后保存到SharePref中
	 *
     * @param context 根控件
     * @param key   键值
	 * @param object    传入的对象
	 */
	public static void saveObj(Context context, String key, Object object) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			// 将对象的转为base64码
			String objBase64 = new String(Base64.encodeBase64(baos.toByteArray()));

			sp.edit().putString(key, objBase64).apply();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将SharePref中经过base64编码的对象读取出来
	 *
     * @param context 根控件
     * @param key   键值
     * @param defValue 若键值不存在则返回此默认值
	 * @return  解码出的对象
	 */
	public static Object getObj(Context context, String key, Object defValue) {
		if (sp == null)
			sp = context.getSharedPreferences(SP_NAME, 0);
		String objBase64 = sp.getString(key, null);
		if (TextUtils.isEmpty(objBase64))
			return defValue;

		// 对Base64格式的字符串进行解码
		byte[] base64Bytes = Base64.decodeBase64(objBase64.getBytes());
		ByteArrayInputStream bais = new ByteArrayInputStream(base64Bytes);

		ObjectInputStream ois;
		Object obj = null;
		try {
			ois = new ObjectInputStream(bais);
			obj = (Object) ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

}
