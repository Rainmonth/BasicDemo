package cn.rainmonth.kadafloatview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 功能：
 * 1. 支持拖拽
 * 2. 支持吸附效果
 *
 * @author RandyZhang
 * @date 2020/3/12 11:01 AM
 */
public class FloatView extends FrameLayout {
    public FloatView(@NonNull Context context) {
        super(context);
    }

    public FloatView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
}
