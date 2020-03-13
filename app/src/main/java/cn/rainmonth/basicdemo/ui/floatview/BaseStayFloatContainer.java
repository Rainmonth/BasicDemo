package cn.rainmonth.basicdemo.ui.floatview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
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
    public static final int SNAP_DEFAULT = -1;                      // 默认
    public static final int SNAP_TOP = 0;                           // 吸附在顶部
    public static final int SNAP_BOTTOM = 1;                        // 吸附在底部
    public static final int SNAP_LEFT = 2;                          // 吸附在左边
    public static final int SNAP_RIGHT = 3;                         // 吸附在右边

    @IntDef({SNAP_DEFAULT, SNAP_TOP, SNAP_BOTTOM, SNAP_LEFT, SNAP_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    @interface StayPosition {                                           // 吸附的方向

    }

    public static final int DEFAULT_STAY_DISTANCE = 200;              // 默认的吸附助理

    private int leftStayDistance, rightStayDistance, topStayDistance, bottomStayDistance;
    private static final int TOUCH_TIME_THRESHOLD_IN_MM = 150;      // 点击判断事件
    private float mOriginalX, mOriginalY, mOriginalRawX, mOriginalRawY;
    private long mLastTouchDownTime;                                // 上次按下的时间

    private Handler mHandler;                                       //
    private float mStatusBarHeight;
    private float mScreenWidth, mScreenHeight;
    private float mFloatInvisibleWidth;                             // 吸附状态下不可见的宽度
    private boolean mIsCoverStatusBar = true;                       // 是否覆盖状态栏
    private boolean mIsUnderSnap = false;                           // 是否处于吸附状态

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
        mHandler = new Handler(Looper.getMainLooper());
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

        stopRotateAnim(this);
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
        if (mIsCoverStatusBar) {
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

        // 优先点击处理，根据时间差判断是否要进行时间点击处理
        if (isNeedPerformClick()) {
            if (isUnderSnap()) {// 吸附状态
                // todo 展开，这个展开一段时间后需要自动吸附，注意状态的控制
                playExtendAnim(this, getSnapDirection());
            } else {// 非吸附状态
                if (mCallback != null) {
                    mCallback.onFloatClick();
                }
            }
        } else {
            if (getX() < 0) {
                float moveDistance = Math.abs(getX() + getWidth() * 3 / 4f);
                playLeftSnapAnim(this, moveDistance);
                if (mCallback != null) {
                    mCallback.onPlayLeftSnapAnim(this, getX(), -getWidth() * 3 / 4f);
                }
                return;
            }
            if (getX() > mScreenWidth - getWidth()) {
                float moveDistance = Math.abs(mScreenWidth - getWidth() / 4f - getX());
                playRightSnapAnim(this, moveDistance);
                if (mCallback != null) {
                    mCallback.onPlayRightSnapAnim(this, getX(), mScreenWidth - getWidth() / 4f);
                }
                return;
            }
            playMidAnim(this);
            if (mCallback != null) {
                mCallback.onPlayMiddleAnim();
            }
        }
    }

    /**
     * 是否处于吸附状态
     */
    private boolean isUnderSnap() {
        Log.d("FloatView", "isUnderSnap()->" + mIsUnderSnap);
        return mIsUnderSnap;
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
        if (mIsCoverStatusBar) { // 覆盖
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

    //<editor-fold>动画效果处理
    private ObjectAnimator rotateAnim;


    /**
     * 播放扩展动画
     */
    private void playExtendAnim(View targetView, @StayPosition int direction) {
        if (direction == SNAP_LEFT) {
            // 从左边往右扩展
            playExtendAnimFromLeft(targetView);
        } else if (direction == SNAP_RIGHT) {
            // 从右边往左扩展
            playExtendAnimFromRight(targetView);
        } else {
            // doNoting
            if (mCallback != null) {
                mCallback.onFloatClick();
            }
        }
    }

    private void playExtendAnimFromLeft(final View targetView) {
        Log.d("FloatView", "playExtendAnimFromLeft()");
        ObjectAnimator extendFromLeftTranX = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() + getWidth() * 3 / 4f);
        extendFromLeftTranX.setInterpolator(new LinearInterpolator());
        extendFromLeftTranX.setDuration(500);

        checkRotateAnim(targetView);
        AnimatorSet extendFromLeftSet = new AnimatorSet();
        extendFromLeftSet.play(rotateAnim).after(extendFromLeftTranX);

        extendFromLeftSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d("FloatView", "playExtendAnimFromLeft()->onAnimationStart()");
                Log.d("FloatView", "playExtendAnimFromLeft()->mIsUnderSnap:改为false");
                mIsUnderSnap = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d("FloatView", "playExtendAnimFromLeft()->onAnimationEnd()");
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        extendFromLeftSet.start();

        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playLeftSnapAnim(targetView, getWidth() * 3 / 4f);
                }
            }, 8000);
        }
    }

    private void playExtendAnimFromRight(final View targetView) {
        Log.d("FloatView", "playExtendAnimFromRight()");
        ObjectAnimator extendFromRightTranX = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() - getWidth() * 3 / 4f);
        extendFromRightTranX.setInterpolator(new LinearInterpolator());
        extendFromRightTranX.setDuration(500);

        checkRotateAnim(targetView);
        AnimatorSet extendFromRightSet = new AnimatorSet();
        extendFromRightSet.play(rotateAnim).after(extendFromRightTranX);

        extendFromRightSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d("FloatView", "playExtendAnimFromRight()->onAnimationStart()");
                Log.d("FloatView", "playExtendAnimFromRight()->mIsUnderSnap:改为false");
                mIsUnderSnap = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d("FloatView", "playExtendAnimFromRight()->onAnimationEnd()");
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        extendFromRightSet.start();

        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playRightSnapAnim(targetView, getWidth() * 3 / 4f);
                }
            }, 4000);
        }
    }

    private @StayPosition
    int getSnapDirection() {
        Log.d("FloatView", "getSnapDirection()->getX():" + getX());
        if (getX() < 0 && getX() >= -getWidth() * 3 / 4f) {
            Log.d("FloatView", "getSnapDirection()->direction:left");
            return SNAP_LEFT;
        }
        if (getX() > mScreenWidth - getWidth() && getX() <= mScreenWidth - getWidth() / 4f) {
            Log.d("FloatView", "getSnapDirection()->direction:right");
            return SNAP_RIGHT;
        }
        Log.d("FloatView", "getSnapDirection()->direction:default");
        return SNAP_DEFAULT;
    }

    /**
     * 动画一开始，mIsUnderSnap就标记为true
     */
    private void playLeftSnapAnim(View targetView, float moveDistance) {
        Log.d("FloatView", "playLeftSnapAnim()");
        Log.d("FloatView", "playLeftSnapAnim()->getX():" + getX());
        Log.d("FloatView", "translationX:" + targetView.getTranslationX());
        Log.d("FloatView", "moveDistance:" + moveDistance);
        // 因为currentX和maxLeftX一直会变，故用局部变量
        ObjectAnimator translateLeftAnim = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() - moveDistance);
        translateLeftAnim.setInterpolator(new LinearInterpolator());
        translateLeftAnim.setDuration(500);

        checkRotateAnim(targetView);

        AnimatorSet snapLeftSet = new AnimatorSet();
        snapLeftSet.play(rotateAnim).after(translateLeftAnim);

        snapLeftSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d("FloatView", "playLeftSnapAnim()->onAnimationStart()");
                Log.d("FloatView", "playLeftSnapAnim()->mIsUnderSnap:改为true");
                mIsUnderSnap = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d("FloatView", "playLeftSnapAnim()->onAnimationEnd()");
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        snapLeftSet.start();
    }

    /**
     * 动画一开始，mIsUnderSnap就标记为true
     */
    private void playRightSnapAnim(View targetView, float moveDistance) {
        Log.d("FloatView", "playRightSnapAnim()");
        Log.d("FloatView", "translationX:" + targetView.getTranslationX());
        Log.d("FloatView", "moveDistance:" + moveDistance);
        // 因为currentX和maxRightX一直会变，故用局部变量
        ObjectAnimator translateRightAnim = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() + moveDistance);
        translateRightAnim.setInterpolator(new LinearInterpolator());
        translateRightAnim.setDuration(500);

        checkRotateAnim(targetView);

        AnimatorSet snapRightSet = new AnimatorSet();
        snapRightSet.play(rotateAnim).after(translateRightAnim);

        snapRightSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d("FloatView", "playRightSnapAnim()->onAnimationStart()");
                Log.d("FloatView", "playRightSnapAnim()->mIsUnderSnap:改为true");
                mIsUnderSnap = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d("FloatView", "playRightSnapAnim()->onAnimationEnd()");
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        snapRightSet.start();
    }

    private void playMidAnim(View targetView) {
        checkRotateAnim(targetView);
        rotateAnim.start();
    }

    private void checkRotateAnim(View targetView) {
        if (rotateAnim == null) {
            rotateAnim = ObjectAnimator.ofFloat(targetView, View.ROTATION, 0, 360);
            rotateAnim.setInterpolator(new LinearInterpolator());
            rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnim.setDuration(2000);
        }
    }

    private void playRotateAnim(View targetView) {
        checkRotateAnim(targetView);
        rotateAnim.start();
    }

    private void stopRotateAnim(View targetView) {
        checkRotateAnim(targetView);
        rotateAnim.pause();
    }

    //</editor-fold>

    public interface FloatCallback {

        /**
         * 悬浮窗按下回调
         */
        void onFloatPress();

        /**
         * 悬浮窗移动回调
         * never do too much create ops!!!
         */
        void onFloatMove();

        /**
         * 悬浮窗点击回调
         */
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

    /**
     * 动画接口
     */
    public interface FloatAnimCallback {

    }
}


