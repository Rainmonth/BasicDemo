package cn.rainmonth.basicdemo.ui.floatview.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.rainmonth.basicdemo.R;


/**
 * 用于展示宽高比控件
 * 提示：如果使用比例模式，高度最好使用wrap_content，防止出现懵逼的情况
 * 如果有特殊需要可以用{}
 *
 * @author sunjian
 * @date 2019-06-04 13:33
 */
public class KaDaRateLayout extends FrameLayout {

    /**
     * 宽高比（高:宽），默认不支持比例
     */
    protected float mRate = -1;

    public KaDaRateLayout(@NonNull Context context) {
        this(context, 1);
    }

    public KaDaRateLayout(@NonNull Context context, float rate) {
        super(context);
        mRate = rate;
        initView(context);
    }

    public KaDaRateLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
        initView(context);
    }

    public KaDaRateLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public KaDaRateLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
        initView(context);
    }

    /**
     * 初始化view
     *
     * @param context Context
     */
    protected void initView(Context context) {

    }

    protected void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.KaDaRateLayout);
        mRate = ta.getFloat(R.styleable.KaDaRateLayout_kada_rate_value, mRate);
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRate <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.round(width * mRate), MeasureSpec.EXACTLY));
        // 子view测量完，再重新调整宽高，例如一些特殊场景LinearLayout的weight
        setMeasuredDimension(width, Math.round(width * mRate));
    }
}
