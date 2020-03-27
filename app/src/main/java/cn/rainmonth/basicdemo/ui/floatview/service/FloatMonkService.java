package cn.rainmonth.basicdemo.ui.floatview.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import cn.rainmonth.basicdemo.ui.floatview.FloatActionController;
import cn.rainmonth.basicdemo.ui.floatview.FloatCallBack;
import cn.rainmonth.basicdemo.ui.floatview.manager.FloatWindowManager;
import cn.rainmonth.basicdemo.ui.floatview.receiver.HomeWatcherReceiver;

/**
 * 悬浮窗在服务中创建，通过暴露接口FloatCallBack与Activity进行交互
 */
public class FloatMonkService extends Service implements FloatCallBack {
    /**
     * home键监听
     */
    private HomeWatcherReceiver mHomeKeyReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        FloatActionController.getInstance().registerCallLittleMonk(this);
        //注册广播接收者
        mHomeKeyReceiver = new HomeWatcherReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeKeyReceiver, homeFilter);
        //初始化悬浮窗UI
        initWindowData();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 初始化WindowManager
     */
    private void initWindowData() {
        FloatWindowManager.createFloatWindow(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //移除悬浮窗
        FloatWindowManager.removeFloatWindowManager();
        //注销广播接收者
        if (null != mHomeKeyReceiver) {
            unregisterReceiver(mHomeKeyReceiver);
        }
    }

    /////////////////////////////////////////////////////////实现接口////////////////////////////////////////////////////


    @Override
    public void play() {
        FloatWindowManager.play();
    }

    @Override
    public void pause() {
        FloatWindowManager.pause();
    }

    /**
     * 悬浮窗的隐藏
     */
    @Override
    public void hide() {
        FloatWindowManager.hide();
    }

    /**
     * 悬浮窗的显示
     */
    @Override
    public void show() {
        FloatWindowManager.show();
    }


    @Override
    public void playExtendFromLeft() {
        FloatWindowManager.playExtendFromLeft();
    }

    @Override
    public void playExtendFromRight() {
        FloatWindowManager.playExtendFromRight();
    }

    @Override
    public void playStayToLeft(float moveDistance, boolean isPlayFromExtend) {
        FloatWindowManager.playStayToLeft(moveDistance, isPlayFromExtend);
    }

    @Override
    public void playStayToRight(float moveDistance, boolean isPlayFromExtend) {
        FloatWindowManager.playStayToRight(moveDistance, isPlayFromExtend);
    }
}
