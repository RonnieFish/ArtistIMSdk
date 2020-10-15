package com.example.artistim;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class WsApplication extends Application {

    private String TAG = this.getClass().getSimpleName();

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getContext();
        initAppStatusListener();
    }

    public static Context getContext(){
        return mContext;
    }

    private void initAppStatusListener() {
        ForegroundCallbacks.init(this).addListener(new ForegroundCallbacks.Listener() {
            @Override
            public void onBecameForeground() {
                Log.d(TAG, "应用回到前台调用重连方法: ");
                IMManager.getInstance().reconnect();
            }

            @Override
            public void onBecameBackground() {

            }
        });
    }
}
