package cn.rainmonth.basicdemo.ui.floatview;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * @author RandyZhang
 * @date 2020/3/13 10:26 AM
 */
public class DpUtils {
    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        if (context != null) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                //根据资源ID获取响应的尺寸值
                return context.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return 0;
    }

    public static int getScreenWidth(Context context) {
        if (context != null && context.getResources() != null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            if (dm != null) {
                return dm.widthPixels;
            }
        }

        return 0;
    }

    public static int getScreenHeight(Context context) {
        if (context != null && context.getResources() != null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            if (dm != null) {
                return dm.heightPixels;
            }
        }
        return 0;
    }

    public static int dp2px(Context context, float dp) {
        return (int) (dp * getDensity(context) + 0.5f);
    }

    public static int px2dp(Context context, float px) {
        try {
            return (int) ((px - 0.5f) / getDensity(context));
        } catch (Exception ignored) {
            return (int) px;
        }
    }

    public static float getDensity(Context context) {
        if (context != null && context.getResources() != null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            if (dm != null) {
                return dm.density;
            }
        }
        return 0;
    }
}
