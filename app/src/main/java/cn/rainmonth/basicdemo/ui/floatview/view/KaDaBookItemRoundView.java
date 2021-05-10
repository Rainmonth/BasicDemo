package cn.rainmonth.basicdemo.ui.floatview.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import android.util.AttributeSet;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.rainmonth.basicdemo.R;
import cn.rainmonth.basicdemo.ui.floatview.util.DensityUtil;

/**
 * 针对BookItemView单独写的一个KADaRoundView
 */
public class KaDaBookItemRoundView extends KaDaRateLayout {

    private Paint mPaint;
    private Path mPath;
    protected float[] mRadii;
    protected float mRadiusPercent;
    private boolean isHighQuality = false;

    private Paint mStrokePaint;
    private Path mStrokePath;
    //    private int mStrokeColor = Color.parseColor("F6F8FB");
    private int mStrokeColor = Color.RED;
    private int mStrokeWidth = DensityUtil.dp2px(getContext(), 10);

    public KaDaBookItemRoundView(@NonNull Context context) {
        super(context);
    }

    public KaDaBookItemRoundView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public KaDaBookItemRoundView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public KaDaBookItemRoundView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @CallSuper
    @Override
    protected void initView(Context context) {
        super.initView(context);
        mPaint = new Paint();
        // 不要带透明的颜色都可
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPath = new Path();

        mStrokePaint = new Paint();
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePath = new Path();
    }

    @Override
    protected void initAttrs(Context context, AttributeSet attrs) {
        super.initAttrs(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.KaDaBookItemRoundView);
        final float radius = ta.getDimension(R.styleable.KaDaBookItemRoundView_kada_round_corner_radius, DensityUtil.dp2px(getContext(), 12));
        final float radiusPercent = ta.getFloat(R.styleable.KaDaBookItemRoundView_kada_round_corner_radius_percent, 0);
        final float leftTopRadius = ta.getDimension(R.styleable.KaDaBookItemRoundView_kada_round_left_top_corner_radius, radius);
        final float rightTopRadius = ta.getDimension(R.styleable.KaDaBookItemRoundView_kada_round_right_top_corner_radius, radius);
        final float leftBottomRadius = ta.getDimension(R.styleable.KaDaBookItemRoundView_kada_round_left_bottom_corner_radius, radius);
        final float rightBottomRadius = ta.getDimension(R.styleable.KaDaBookItemRoundView_kada_round_right_bottom_corner_radius, radius);
        ta.recycle();
        if (radiusPercent <= 0) {
            mRadiusPercent = 0;
            mRadii = new float[]{leftTopRadius, leftTopRadius, rightTopRadius, rightTopRadius, leftBottomRadius, leftBottomRadius, rightBottomRadius, rightBottomRadius};
        } else if (radiusPercent >= 1) {
            mRadiusPercent = 1;
            mRadii = new float[8];
        } else {
            mRadiusPercent = radiusPercent;
            mRadii = new float[8];
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mRadiusPercent > 0 && mRadiusPercent <= 1) {
            final float radius = h * mRadiusPercent;
            mRadii[0] = radius;
            mRadii[1] = radius;
            mRadii[2] = radius;
            mRadii[3] = radius;
            mRadii[4] = radius;
            mRadii[5] = radius;
            mRadii[6] = radius;
            mRadii[7] = radius;
        }
        // 计算path
        mPath.reset();
        mStrokePath.reset();
        if (isHighQuality) {
            final Path path = new Path();
            mPath.addRect(0, 0, w, h, Path.Direction.CW);
            path.addRoundRect(new RectF(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom()),
                    mRadii,
                    Path.Direction.CW);
            mPath.op(path, Path.Op.DIFFERENCE);
        } else {
            mPath.addRoundRect(new RectF(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom()),
                    mRadii,
                    Path.Direction.CW);
        }
        mStrokePath.addRoundRect(new RectF(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom()),
                mRadii,
                Path.Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isHighQuality) {
            int count = canvas.saveLayer(new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), null, Canvas
                    .ALL_SAVE_FLAG);
            canvas.drawRect(new RectF(getPaddingLeft(), getPaddingTop(), canvas.getWidth(), canvas.getHeight()), mStrokePaint);
            super.dispatchDraw(canvas);
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawPath(mPath, mPaint);

            mPaint.setXfermode(null);
            canvas.restoreToCount(count);
            return;
        }
        int save = canvas.save();
        canvas.clipPath(mPath);
        canvas.drawPath(mStrokePath, mStrokePaint);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    public void setRoundCorner(float lt, float rt, float lb, float rb) {
        mRadii[0] = lt;
        mRadii[1] = lt;
        mRadii[2] = rt;
        mRadii[3] = rt;
        mRadii[4] = lb;
        mRadii[5] = lb;
        mRadii[6] = rb;
        mRadii[7] = rb;
        invalidate();
    }
}
