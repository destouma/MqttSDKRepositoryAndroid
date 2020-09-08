package com.elinkthings.mqttsdklibrary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.elinkthings.mqttlibrary.listener.OnMessageListener;
import com.elinkthings.mqttlibrary.service.CollectMoneyService;
import com.elinkthings.mqttlibrary.service.MqttHttpUtils;
import com.elinkthings.mqttlibrary.utils.L;
import com.elinkthings.mqttlibrary.utils.MqttMessageUtils;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements OnMessageListener, View.OnClickListener {


    private String TAG = MainActivity.class.getName();

    private TextView tv_data;
    private EditText et, et_device_imei,et_ad;
    private Button btn, btn_device_imei, btn_ota, btn_heartbeat, btn_volume_add, btn_volume_less,btn_ad;
    private Context mContext;
    private MqttMessageUtils mMqttMessageUtils;
    private MqttHttpUtils mMqttHttpUtils;
    private String mVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mMqttMessageUtils = new MqttMessageUtils();
        mMqttHttpUtils = new MqttHttpUtils();

        et = findViewById(R.id.et);
        et_device_imei = findViewById(R.id.et_device_imei);
        btn = findViewById(R.id.btn);
        btn_device_imei = findViewById(R.id.btn_device_imei);
        tv_data = findViewById(R.id.tv_data);
        btn_ota = findViewById(R.id.btn_ota);
        btn_heartbeat = findViewById(R.id.btn_heartbeat);
        btn_volume_add = findViewById(R.id.btn_volume_add);
        btn_volume_less = findViewById(R.id.btn_volume_less);
        et_ad = findViewById(R.id.et_ad);
        btn_ad = findViewById(R.id.btn_ad);


        btn_device_imei.setOnClickListener(this);
        btn.setOnClickListener(this);
        btn_ota.setOnClickListener(this);
        btn_heartbeat.setOnClickListener(this);
        btn_volume_add.setOnClickListener(this);
        btn_volume_less.setOnClickListener(this);
        btn_ad.setOnClickListener(this);


        bindService();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_device_imei:
                String imei = et_device_imei.getText().toString().trim();
                mMqttHttpUtils.getDeviceId(imei, new MqttHttpUtils.OnDeviceInfoListener() {
                    @Override
                    public void OnDeviceInfo(int deviceId) {
                        if (deviceId > 0) {
                            L.i("获取设备id:" + deviceId);
                            mService.bindDevice(deviceId);
                        } else {
                            Toast.makeText(mContext, "获取设备ID失败!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                break;


            case R.id.btn:
                String data = et.getText().toString().trim();
                mMqttMessageUtils.sendTTSMessage(mContext, data);
                break;


            case R.id.btn_ota:
                if (TextUtils.isEmpty(mVersion)) {
                    Toast.makeText(mContext, "版本号为空", Toast.LENGTH_LONG).show();
                    return;
                }
                mMqttHttpUtils.checkUpdateDevice(mVersion, new MqttHttpUtils.OnUpdateDeviceListener() {
                    @Override
                    public void OnNewVersion(String newVersion,String url, boolean forciblyUpdate) {
                        if (TextUtils.isEmpty(newVersion)) {
                            Toast.makeText(mContext, "网络异常" + newVersion, Toast.LENGTH_LONG).show();
                            L.i("网络异常");
                            return;
                        }


                        if (forciblyUpdate) {
                            //强制升级
                            Toast.makeText(mContext, "发现新版本要求强制升级:" + newVersion, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mContext, "当前最新支持的版本:" + newVersion, Toast.LENGTH_LONG).show();
                        }
                        if (!newVersion.equalsIgnoreCase(mVersion)){
                            mMqttMessageUtils.sendOTAMessage(mContext,url);
                            L.i("开始OTA升级");
                        }
                    }
                });

                break;


            case R.id.btn_heartbeat:
                mMqttMessageUtils.sendHeartbeatMessage(mContext);
                break;
            case R.id.btn_volume_add:
                mMqttMessageUtils.sendVolumeMessage(mContext, true);
                break;
            case R.id.btn_volume_less:
                mMqttMessageUtils.sendVolumeMessage(mContext, false);
                break;

                //发送广告
            case R.id.btn_ad:

                String trim = et_ad.getText().toString().trim();
                //status:Y=开;N=关,T=试听
                mMqttMessageUtils.sendAdMessage(mContext, "N",8*60*60,20*60*60,5,trim,trim);
                break;


        }

    }


    //---------------------------------服务---------------------------------------------------
    /**
     * 服务Intent
     */
    private Intent bindIntent;

    private void bindService() {
        L.i(TAG, "绑定服务");
        if (bindIntent == null) {
            bindIntent = new Intent(this, CollectMoneyService.class);
            if (mFhrSCon != null)
                this.bindService(bindIntent, mFhrSCon, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if (mFhrSCon != null)
            this.unbindService(mFhrSCon);
        bindIntent = null;
    }

    private CollectMoneyService mService;
    /**
     * 服务连接与界面的连接
     */
    private ServiceConnection mFhrSCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            L.i(TAG, "服务与界面建立连接成功");
            //与服务建立连接
            mService = ((CollectMoneyService.MainBinder) service).getService();
            mService.setOnMessageListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            L.e(TAG, "服务与界面连接断开");
            //与服务断开连接
            mService = null;
        }
    };


    @Override
    public void onReceive(String topic, String message) {
        //收到MQTT消息,设备返回的
        L.i("收到MQTT消息:topic=" + topic + " || message=" + message);
        if (!TextUtils.isEmpty(message)) {
            tv_data.post(() -> {
                tv_data.setText("收到的消息:" + message);
            });
        }
        if (MqttMessageUtils.isHeartbeat(message)) {
            String[] strings = MqttMessageUtils.parsingHeartbeatData(message);
            if (strings != null && strings.length > 1) {
                mVersion = strings[1];
            }
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }
}