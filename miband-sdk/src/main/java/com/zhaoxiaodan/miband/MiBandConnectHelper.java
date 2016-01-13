package com.zhaoxiaodan.miband;

import java.util.Arrays;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.util.Log;

import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.listeners.RealtimeStepsNotifyListener;
import com.zhaoxiaodan.miband.model.BatteryInfo;
import com.zhaoxiaodan.miband.model.LedColor;
import com.zhaoxiaodan.miband.model.Profile;
import com.zhaoxiaodan.miband.model.Protocol;
import com.zhaoxiaodan.miband.model.UserInfo;
import com.zhaoxiaodan.miband.model.VibrationMode;

public class MiBandConnectHelper {

    private static final String TAG = "miband-android";

    private static Context mContext;
    private static BluetoothIO mBluetoothIO;

    private MiBandConnectHelper() {
    }

    public static void initialize(Context context) {
        mContext = context;
        mBluetoothIO = new BluetoothIO();
        getInstance();
    }

    public static MiBandConnectHelper getInstance() {
        return INSTANCE.MY_INSTANCE;
    }

    public static void startScan(ScanCallback callback) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (null == adapter) {
            Log.e(TAG, "BluetoothAdapter is null");
            return;
        }
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (null == scanner) {
            Log.e(TAG, "BluetoothLeScanner is null");
            return;
        }
        scanner.startScan(callback);
    }

    public static void stopScan(ScanCallback callback) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (null == adapter) {
            Log.e(TAG, "BluetoothAdapter is null");
            return;
        }
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (null == scanner) {
            Log.e(TAG, "BluetoothLeScanner is null");
            return;
        }
        scanner.stopScan(callback);
    }

    /**
     * 连接指定的手环
     *
     * @param callback
     */
    public static void connect(BluetoothDevice device, final ActionCallback callback) {
        mBluetoothIO.connect(mContext, device, callback);
    }

    public static void setDisconnectedListener(NotifyListener disconnectedListener) {
        mBluetoothIO.setDisconnectedListener(disconnectedListener);
    }

    /**
     * 和手环配对, 实际用途未知, 不配对也可以做其他的操作
     *
     * @return data = null
     */
    public static void pair(final ActionCallback callback) {
        ActionCallback ioCallback = new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) data;
                Log.d(TAG, "pair result " + Arrays.toString(characteristic.getValue()));
                if (characteristic.getValue().length == 1 && characteristic.getValue()[0] == 2) {
                    callback.onSuccess(null);
                } else {
                    callback.onFail(-1, "respone values no succ!");
                }
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        };
        mBluetoothIO.writeAndRead(Profile.UUID_CHAR_PAIR, Protocol.PAIR, ioCallback);
    }

    public static BluetoothDevice getDevice() {
        return mBluetoothIO.getDevice();
    }

    /**
     * 读取和连接设备的信号强度RSSI值
     *
     * @param callback
     * @return data : int, rssi值
     */
    public static void readRssi(ActionCallback callback) {
        mBluetoothIO.readRssi(callback);
    }

    /**
     * 读取手环电池信息
     *
     * @return {@link BatteryInfo}
     */
    public static void getBatteryInfo(final ActionCallback callback) {
        ActionCallback ioCallback = new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) data;
                Log.d(TAG, "getBatteryInfo result " + Arrays.toString(characteristic.getValue()));
                if (characteristic.getValue().length == 10) {
                    BatteryInfo info = BatteryInfo.fromByteData(characteristic.getValue());
                    callback.onSuccess(info);
                } else {
                    callback.onFail(-1, "result format wrong!");
                }
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        };
        mBluetoothIO.readCharacteristic(Profile.UUID_CHAR_BATTERY, ioCallback);
    }

    /**
     * 让手环震动
     */
    public static void startVibration(VibrationMode mode) {
        byte[] protocal;
        switch (mode) {
            case VIBRATION_WITH_LED:
                protocal = Protocol.VIBRATION_WITH_LED;
                break;
            case VIBRATION_10_TIMES_WITH_LED:
                protocal = Protocol.VIBRATION_10_TIMES_WITH_LED;
                break;
            case VIBRATION_WITHOUT_LED:
                protocal = Protocol.VIBRATION_WITHOUT_LED;
                break;
            default:
                return;
        }
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, protocal, null);
    }

    /**
     * 停止以模式Protocol.VIBRATION_10_TIMES_WITH_LED 开始的震动
     */
    public static void stopVibration() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION, null);
    }

    public static void setNormalNotifyListener(NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_CHAR_NOTIFICATION, listener);
    }

    /**
     * 重力感应器数据通知监听, 设置完之后需要另外使用 {@link MiBandConnectHelper#enableRealtimeStepsNotify} 开启 和
     * {@link MiBandConnectHelper##disableRealtimeStepsNotify} 关闭通知
     *
     * @param listener
     */
    public static void setSensorDataNotifyListener(final NotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_CHAR_SENSOR_DATA, new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {

                listener.onNotify(data);

            }
        });
    }

    /**
     * 开启重力感应器数据通知
     */
    public static void enableSensorDataNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.ENABLE_SENSOR_DATA_NOTIFY, null);
    }

    /**
     * 关闭重力感应器数据通知
     */
    public static void disableSensorDataNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.DISABLE_SENSOR_DATA_NOTIFY, null);
    }

    /**
     * 实时步数通知监听器, 设置完之后需要另外使用 {@link MiBandConnectHelper#enableRealtimeStepsNotify} 开启 和
     * {@link MiBandConnectHelper##disableRealtimeStepsNotify} 关闭通知
     *
     * @param listener
     */
    public static void setRealtimeStepsNotifyListener(final RealtimeStepsNotifyListener listener) {
        mBluetoothIO.setNotifyListener(Profile.UUID_CHAR_REALTIME_STEPS, new NotifyListener() {

            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 4) {
                    int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                    listener.onNotify(steps);
                }
            }
        });
    }

    /**
     * 开启实时步数通知
     */
    public static void enableRealtimeStepsNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.ENABLE_REALTIME_STEPS_NOTIFY, null);
    }

    /**
     * 关闭实时步数通知
     */
    public static void disableRealtimeStepsNotify() {
        mBluetoothIO.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.DISABLE_REALTIME_STEPS_NOTIFY, null);
    }

    /**
     * 设置led灯颜色
     */
    public static void setLedColor(LedColor color) {
        byte[] protocal;
        switch (color) {
            case RED:
                protocal = Protocol.SET_COLOR_RED;
                break;
            case BLUE:
                protocal = Protocol.SET_COLOR_BLUE;
                break;
            case GREEN:
                protocal = Protocol.SET_COLOR_GREEN;
                break;
            case ORANGE:
                protocal = Protocol.SET_COLOR_ORANGE;
                break;
            default:
                return;
        }
        mBluetoothIO.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, protocal, null);
    }

    /**
     * 设置用户信息
     *
     * @param userInfo
     */
    public static void setUserInfo(UserInfo userInfo) {
        BluetoothDevice device = mBluetoothIO.getDevice();
        mBluetoothIO.writeCharacteristic(Profile.UUID_CHAR_USER_INFO, userInfo.getBytes(device.getAddress()), null);
    }

    public static void showServicesAndCharacteristics() {
        for (BluetoothGattService service : mBluetoothIO.gatt.getServices()) {
            Log.d(TAG, "onServicesDiscovered:" + service.getUuid());

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "characteristic:" + characteristic.getUuid());
            }
        }
    }

    public static boolean getIsConnected(){
        if(mBluetoothIO.gatt == null)
            return false;
        else
            return true;
    }

    private static class INSTANCE {
        private static final MiBandConnectHelper MY_INSTANCE = new MiBandConnectHelper();

        private INSTANCE() {
        }
    }
}
