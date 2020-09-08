package com.elinkthings.mqttsdklibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.elinkthings.mqttlibrary.MqttSdk;
import com.elinkthings.mqttlibrary.config.BroadcastConfig;
import com.elinkthings.mqttlibrary.utils.L;


/**
 * xing
 * 2019/10/14
 * MyApplication
 */
public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {


    private static String TAG = MyApplication.class.getName();

    private static MyApplication instance;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(this);//注册进入前台,后台的回调
        //userId是唯一的,请自行输入,重复可能会导致mqtt连接互顶
        MqttSdk.initMqtt(this,1,"9d542167bd11229174438b1d69469349","mqtt-49");
        L.init(this,true);
    }





    public static synchronized MyApplication getInstance() {
        return instance;
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    //------------------------------------------------------------------------------------------
    /**
     * 记录activity进入前台的次数
     */
    private int refCount = 0;
    /**
     * 是否为前台状态
     */
    private boolean mFrontDesk = false;
    private OnFrontDeskListener mOnFrontDeskListener;

    public void setOnFrontDeskListener(OnFrontDeskListener onFrontDeskListener) {
        mOnFrontDeskListener = onFrontDeskListener;
    }

    /**
     * 是否为前台
     *
     * @return
     */
    public boolean isFrontDesk() {
        return mFrontDesk;
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        refCount++;
        if (!mFrontDesk) {
            L.i(TAG, "进入前台的通知");
            mFrontDesk = true;
            if (mOnFrontDeskListener != null) {
                mOnFrontDeskListener.onFrontDesk(true);
            }
            Intent intent = new Intent(BroadcastConfig.APP_FRONT_DESK);
            intent.putExtra(BroadcastConfig.APP_FRONT_DESK_DATA, mFrontDesk);
            sendBroadcast(intent);
        }

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        refCount--;
        if (refCount == 0) {
            L.i(TAG, "进入后台的通知");
            mFrontDesk = false;
            if (mOnFrontDeskListener != null) {
                mOnFrontDeskListener.onFrontDesk(false);
            }
            Intent intent = new Intent(BroadcastConfig.APP_FRONT_DESK);
            intent.putExtra(BroadcastConfig.APP_FRONT_DESK_DATA, mFrontDesk);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    //------------------------------------------------------------------------------------------

}
