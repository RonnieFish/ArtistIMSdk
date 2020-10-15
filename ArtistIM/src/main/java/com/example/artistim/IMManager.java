package com.example.artistim;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class IMManager {
    
    private String TAG = this.getClass().getSimpleName();

    public static volatile IMManager imManager = null;


    /**
     * WebSocket config
     */
    private static final int FRAME_QUEUE_SIZE = 5;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final String DEF_TEST_URL = "测试服地址";//测试服默认地址
    private static final String DEF_RELEASE_URL = "正式服地址";//正式服默认地址
//    private static final String DEF_URL = BuildConfig.DEBUG ? DEF_TEST_URL : DEF_RELEASE_URL;
    private String url;
    private static final String DEF_URL = "";

    private Const.WsStatus status;
    private WebSocket ws;
    private WsAdapter wsAdapter;

    private Handler handler = new Handler();

    private IMManager(){

    }

    public static IMManager getInstance(){
        if(imManager == null){
            synchronized (IMManager.class){
                if(imManager == null){
                    imManager = new IMManager();
                }
            }
        }
        return imManager;
    }

    public void init(){
        try {
            /**
             * configUrl其实是缓存在本地的连接地址
             * 这个缓存本地连接地址是app启动的时候通过http请求去服务端获取的,
             * 每次app启动的时候会拿当前时间与缓存时间比较,超过6小时就再次去服务端获取新的连接地址更新本地缓存
             */
            String configUrl = "";
            url = TextUtils.isEmpty(configUrl) ? DEF_URL : configUrl;
            ws = new WebSocketFactory().createSocket(url, CONNECT_TIMEOUT)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(wsAdapter = new WsAdapter())//添加回调监听
                    .connectAsynchronously();//异步连接
            setStatus(Const.WsStatus.CONNECTING);
            Log.d(TAG, "init: 第一次链接");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Const.WsStatus getStatus() {
        return status;
    }

    public void setStatus(Const.WsStatus status) {
        this.status = status;
    }

    class WsAdapter extends WebSocketAdapter{

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            Log.d(TAG, "onTextMessage: " + text);
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            Log.d(TAG, "onConnected: 连接中");
            setStatus(Const.WsStatus.CONNECT_SUCCESS);
            cancelReconnect();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            Log.d(TAG, "onConnectError: 连接失败");
            setStatus(Const.WsStatus.CONNECT_FAIL);
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.d(TAG, "onDisconnected: 断开连接");
            setStatus(Const.WsStatus.CONNECT_FAIL);
        }
    }

    private Handler mHandler = new Handler();

    private int reconnectCount = 0;//重连次数
    private long minInterval = 3000;//重连最小时间间隔
    private long maxInterval = 60000;//重连最大时间间隔


    public void reconnect() {
        if (!isNetConnect()) {
            reconnectCount = 0;
            Log.d(TAG, "reconnect: 重连失败网络不可用");
            return;
        }

        //这里其实应该还有个用户是否登录了的判断 因为当连接成功后我们需要发送用户信息到服务端进行校验
        //由于我们这里是个demo所以省略了
        if (ws != null &&
                !ws.isOpen() &&//当前连接断开了
                getStatus() != Const.WsStatus.CONNECTING) {//不是正在重连状态

            reconnectCount++;
            setStatus(Const.WsStatus.CONNECTING);

            long reconnectTime = minInterval;
            if (reconnectCount > 3) {
                url = DEF_URL;
                long temp = minInterval * (reconnectCount - 2);
                reconnectTime = temp > maxInterval ? maxInterval : temp;
            }

            Log.d(TAG, "reconnect: 准备开始第%d次重连,重连间隔%d -- url:%s"+ reconnectCount+reconnectTime+url);
            mHandler.postDelayed(mReconnectTask, reconnectTime);
        }
    }


    private Runnable mReconnectTask = new Runnable() {

        @Override
        public void run() {
            try {
                ws = new WebSocketFactory().createSocket(url, CONNECT_TIMEOUT)
                        .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                        .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                        .addListener( wsAdapter= new WsAdapter())//添加回调监听
                        .connectAsynchronously();//异步连接
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    private void cancelReconnect() {
        reconnectCount = 0;
        mHandler.removeCallbacks(mReconnectTask);
    }

    private boolean isNetConnect() {
        ConnectivityManager connectivity = (ConnectivityManager) WsApplication.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }


}
