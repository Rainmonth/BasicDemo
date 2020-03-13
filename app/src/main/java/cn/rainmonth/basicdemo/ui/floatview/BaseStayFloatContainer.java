package cn.rainmonth.basicdemo.ui.floatview;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 支持吸附拖动的容器
 *
 * @author RandyZhang
 * @date 2020/3/12 1:24 PM
 */
public class BaseStayFloatContainer extends FrameLayout {

    private FloatCallback mCallback;                                // 悬浮回调
    public static final int SNAP_TOP = 0;                           // 吸附在顶部
    public static final int SNAP_BOTTOM = 1;                        // 吸附在底部
    public static final int SNAP_LEFT = 2;                          // 吸附在左边
    public static final int SNAP_RIGHT = 3;                         // 吸附在右边

    @IntDef({SNAP_TOP, SNAP_BOTTOM, SNAP_LEFT, SNAP_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    @interface StayPosition {                                           // 吸附的方向

    }

    public static final int DEFAULT_STAY_DISTANCE = 200;              // 默认的吸附助理

    private int leftStayDistance, rightStayDistance, topStayDistance, bottomStayDistance;
    private static final int TOUCH_TIME_THRESHOLD_IN_MM = 150;      // 点击判断事件
    private float mOriginalX, mOriginalY, mOriginalRawX, mOriginalRawY;
    private long mLastTouchDownTime;                                // 上次按下的时间

    private float mStatusBarHeight;
    private float mScreenWidth, mScreenHeight;
    private boolean isCoverStatusBar = true;                               // 是否覆盖状态栏

    public BaseStayFloatContainer(@NonNull Context context) {
        this(context, null);
    }

    public BaseStayFloatContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseStayFloatContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化
        mScreenWidth = DpUtils.getScreenWidth(getContext());
        mScreenHeight = DpUtils.getScreenHeight(getContext());
        mStatusBarHeight = DpUtils.getStatusBarHeight(getContext());
        leftStayDistance = rightStayDistance = topStayDistance = bottomStayDistance = DEFAULT_STAY_DISTANCE;
    }

    @Override
    public void dispatchConfigurationChanged(Configuration newConfig) {
        super.dispatchConfigurationChanged(newConfig);
        // todo
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
        }
        return true;
    }

    /**
     * Action Down处理
     * 主要记录down时的位置信息即时间
     */
    private void handleActionDown(MotionEvent event) {
        // todo 暂停动画
        mOriginalX = getX();
        mOriginalY = getY();
        mOriginalRawX = event.getRawX();
        mOriginalRawY = event.getRawY();
        mLastTouchDownTime = System.currentTimeMillis();

        if (mCallback != null) {
            mCallback.onFloatPress();
        }
    }

    /**
     * Action Move处理
     * 更新View的位置
     */
    private void handleActionMove(MotionEvent event) {
        float desX = mOriginalX + event.getRawX() - mOriginalRawX;
        float desY = mOriginalY + event.getRawY() - mOriginalRawY;

        fixPositionWhileMoving(desX, desY);

        if (mCallback != null) {
            mCallback.onFloatMove();
        }
    }

    /**
     * 修正悬浮窗的位置
     *
     * @param desX 期待的目标x坐标
     * @param desY 期待的目标y坐标
     */
    private void fixPositionWhileMoving(float desX, float desY) {
        if (desX < -getWidth() * 3 / 4f) {
            desX = -getWidth() * 3 / 4f;
        }
        if (desX > mScreenWidth - getWidth() / 4f) {
            desX = mScreenWidth - getWidth() / 4f;
        }
        setX(desX);
        if (isCoverStatusBar) {
            if (desY < -mStatusBarHeight) {
                desY = -mStatusBarHeight;
            }
        } else {
            if (desY < 0) {
                desY = 0;
            }
        }
        if (desY > mScreenHeight - getHeight()) {
            desY = mScreenHeight - getHeight();
        }
        setY(desY);
    }

    /**
     * Action Up处理
     * 1、判断是否需要执行吸附效果；
     * 2、判断是否需要执行点击事件；
     */
    private void handleActionUp(MotionEvent event) {
        if (isNeedStayLeft()) {
            performStayToEdge(SNAP_LEFT);
        } else if (isNeedStayRight()) {
            performStayToEdge(SNAP_RIGHT);
        } else if (isNeedStayTop()) {
            performStayToEdge(SNAP_TOP);
        } else if (isNeedStayBottom()) {
            performStayToEdge(SNAP_BOTTOM);
        }
        // doNoting

        // 根据时间差判断是否要进行时间点击处理
        if (isNeedPerformClick()) {
            if (mCallback != null) {
                mCallback.onFloatClick();
            }
        } else {
            // todo 恢复动画
            if (getX() < 0) {
                if (mCallback != null) {
                    mCallback.onPlayLeftSnapAnim(this, getX(), -getWidth() * 3 / 4f);
                }
                return;
            }
            if (getX() > mScreenWidth - getWidth()) {
                if (mCallback != null) {
                    mCallback.onPlayRightSnapAnim(this, getX(), mScreenWidth - getWidth() / 4f);
                }
                return;
            }
            if (mCallback != null) {
                mCallback.onPlayMiddleAnim();
            }
        }
    }

    /**
     * 是否需要吸附效果
     */
    public boolean isNeedStay() {
        return isNeedStayLeft() || isNeedStayRight() || isNeedStayTop() || isNeedStayBottom();
    }

    /**
     * 执行吸附到边缘
     */
    private void performStayToEdge(@StayPosition int stayPosition) {
        switch (stayPosition) {
            case SNAP_BOTTOM:
                // todo 执行吸附到底部动画
                break;
            case SNAP_RIGHT:
                // todo 执行吸附到右部动画
                break;
            case SNAP_TOP:
                // todo 执行吸附到顶部动画
                break;
            case SNAP_LEFT:
            default:
                // todo 执行吸附到左部动画
                break;
        }
    }

    /**
     * 是否当做点击处理
     */
    private boolean isNeedPerformClick() {
        return System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD_IN_MM;
    }

    private boolean isNeedStayLeft() {
        return getX() < leftStayDistance;
    }

    private boolean isNeedStayRight() {
        return getX() < mScreenWidth - rightStayDistance;
    }

    private boolean isNeedStayTop() {
        if (isCoverStatusBar) { // 覆盖
            return getY() < topStayDistance;
        } else {
            return (getY() > mStatusBarHeight && getY() < topStayDistance + mStatusBarHeight);
        }
    }

    private boolean isNeedStayBottom() {
        return getY() < mScreenHeight - bottomStayDistance;
    }

    public void setCallback(FloatCallback callback) {
        this.mCallback = callback;
    }

    public interface FloatCallback {
        void onFloatPress();

        /**
         * never do too much create ops!!!
         */
        void onFloatMove();

        void onFloatClick();

        /**
         * 左边吸附动画
         *
         * @param targetView 目标View
         * @param currentX   当前的x坐标
         * @param maxLeftX   最左边的位置
         */
        void onPlayLeftSnapAnim(View targetView, float currentX, float maxLeftX);

        /**
         * 右边吸附动画
         *
         * @param targetView 目标View
         * @param currentX   当前的x坐标
         * @param maxRightX  最右边的位置
         */
        void onPlayRightSnapAnim(View targetView, float currentX, float maxRightX);

        /**
         * 正常动画
         */
        void onPlayMiddleAnim();
    }
}


