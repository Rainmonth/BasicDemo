package cn.rainmonth.basicdemo;

import android.app.Application;

/**
 * @author RandyZhang
 * @date 2020/3/12 1:15 PM
 */
public class BasicApp extends Application {
    public static Application mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
}
