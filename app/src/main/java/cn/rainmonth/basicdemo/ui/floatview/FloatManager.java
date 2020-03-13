package cn.rainmonth.basicdemo.ui.floatview;

import android.content.Context;
import android.view.WindowManager;

/**
 * @author RandyZhang
 * @date 2020/3/12 1:13 PM
 */
public class FloatManager {

    private static FloatManager mInstance;

    public static FloatManager getInstance() {
        if (mInstance == null) {
            synchronized (FloatManager.class) {
                if (mInstance == null) {
                    mInstance = new FloatManager();
                }
            }

        }
        return mInstance;
    }

    private FloatManager() {
    }

    public WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }



}
