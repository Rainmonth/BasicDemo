package cn.rainmonth.basicdemo.ui.floatview.util;

import android.content.Context;
import android.util.DisplayMetrics;

public class DensityUtil {
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     */
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

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
}
