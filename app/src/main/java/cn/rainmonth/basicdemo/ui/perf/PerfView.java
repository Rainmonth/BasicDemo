package cn.rainmonth.basicdemo.ui.perf;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.rainmonth.basicdemo.R;

/**
 * PorterDuffXfermode
 * 1. 需要圆角
 * 2. 需要边框
 */
public class PerfView extends View {


    private Paint mSrcPaint;
    private final int mTextColor = 0xFFFF0000;
    private final int mCircleColor = 0xFFFFCC44;
    private final int mRectColor = 0xFF66AAFF;

    private int mXfermodeInt = 6;
    private PorterDuff.Mode mMode = PorterDuff.Mode.DST_IN;
    private String modeName = "DST_IN";
    private Xfermode mXfermode;

    private int mWidth, mHeight;

    private float mRadius;
    private float mStrokeWidth;
    private int mStrokeColor;


    public PerfView(@NonNull Context context) {
        this(context, null);
    }

    public PerfView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PerfView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 关闭硬件加速，不然CLEAR、SRC_OUT、DST_OUT、XOR、ADD无效
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PerfView);

        mXfermodeInt = ta.getInteger(R.styleable.PerfView_xfermode, 6);
        mMode = getPorterDuffMode(mXfermodeInt);

        ta.recycle();

        init(context);
    }

    private PorterDuff.Mode getPorterDuffMode(int xfermode) {
        switch (xfermode) {
            case 0:
                mMode = PorterDuff.Mode.CLEAR;
                modeName = "CLEAR";
                break;
            case 1:
                mMode = PorterDuff.Mode.SRC;
                modeName = "SRC";
                break;
            case 2:
                mMode = PorterDuff.Mode.DST;
                modeName = "DST";
                break;
            case 3:
                mMode = PorterDuff.Mode.SRC_OVER;
                modeName = "SRC_OVER";
                break;
            case 4:
                mMode = PorterDuff.Mode.DST_OVER;
                modeName = "DST_OVER";
                break;
            case 5:
                mMode = PorterDuff.Mode.SRC_IN;
                modeName = "SRC_IN";
                break;
            case 6:
            default:
                mMode = PorterDuff.Mode.DST_IN;
                modeName = "DST_IN";
                break;
            case 7:
                mMode = PorterDuff.Mode.SRC_OUT;
                modeName = "SRC_OUT";
                break;
            case 8:
                mMode = PorterDuff.Mode.DST_OUT;
                modeName = "DST_OUT";
                break;
            case 9:
                mMode = PorterDuff.Mode.SRC_ATOP;
                modeName = "SRC_ATOP";
                break;
            case 10:
                mMode = PorterDuff.Mode.DST_ATOP;
                modeName = "DST_ATOP";
                break;
            case 11:
                mMode = PorterDuff.Mode.XOR;
                modeName = "XOR";
                break;
            case 12:
                mMode = PorterDuff.Mode.ADD;
                modeName = "ADD";
                break;
            case 13:
                mMode = PorterDuff.Mode.MULTIPLY;
                modeName = "MULTIPLY";
                break;
            case 14:
                mMode = PorterDuff.Mode.SCREEN;
                modeName = "SCREEN";
                break;
            case 15:
                mMode = PorterDuff.Mode.OVERLAY;
                modeName = "OVERLAY";
                break;
            case 16:
                mMode = PorterDuff.Mode.DARKEN;
                modeName = "DARKEN";
                break;
            case 17:
                mMode = PorterDuff.Mode.LIGHTEN;
                modeName = "LIGHTEN";
                break;
            case 18:
                mMode = null;
                modeName = "NONE";
                break;

        }

        return mMode;
    }

    private void init(Context context) {
        mSrcPaint = new Paint();
        mSrcPaint.setAntiAlias(true);
        mSrcPaint.setColor(mCircleColor);

        if (mMode != null) {
            mXfermode = new PorterDuffXfermode(mMode);
        }

//        mDstPaint = new Paint();
//        mDstPaint.setAntiAlias(true);
//        mDstPaint.setColor(mDstColor);
//        mDstPaint.setXfermode(new PorterDuffXfermode(mMode));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mSrcPaint.setAntiAlias(true);
        canvas.drawARGB(255, 139, 197, 186);
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, null, Canvas.ALL_SAVE_FLAG);

        drawCircle(canvas);
        mSrcPaint.setXfermode(mXfermode);
        drawRect(canvas);
        mSrcPaint.setXfermode(null);

        canvas.restoreToCount(layerId);
//
        mSrcPaint.setColor(mTextColor);
        canvas.drawText(modeName,0, 0.25f * mHeight, mSrcPaint);
    }

    private void drawRect(Canvas canvas) {
        mSrcPaint.setColor(mRectColor);
        canvas.drawRect(mWidth / 2f, mHeight / 2f, mWidth * 0.75f, mHeight * 0.75f, mSrcPaint);
    }

    private void drawCircle(Canvas canvas) {
        mSrcPaint.setColor(mCircleColor);
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, mWidth * 0.25f, mSrcPaint);
    }

}
