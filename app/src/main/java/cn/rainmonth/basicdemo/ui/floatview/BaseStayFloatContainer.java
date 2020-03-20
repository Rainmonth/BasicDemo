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

import cn.rainmonth.basicdemo.R;

/**
 * 支持吸附拖动的容器
 *
 * @author RandyZhang
 * @date 2020/3/12 1:24 PM
 */
public class BaseStayFloatContainer extends FrameLayout {
    private static String TAG = "FloatView";

    private FloatCallback mCallback;                                // 悬浮回调
    public static final int POS_DEFAULT = -1;                      // 默认
    public static final int POS_TOP = 0;                           // 吸附在顶部
    public static final int POS_BOTTOM = 1;                        // 吸附在底部
    public static final int POS_LEFT = 2;                          // 吸附在左边
    public static final int POS_RIGHT = 3;                         // 吸附在右边

    @IntDef({POS_DEFAULT, POS_TOP, POS_BOTTOM, POS_LEFT, POS_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Position {                                           // 吸附的方向

    }

    public static final int DEFAULT_STAY_DISTANCE = 200;              // 默认的吸附助理

    private int leftStayDistance, rightStayDistance, topStayDistance, bottomStayDistance;
    private static final int TOUCH_TIME_THRESHOLD_IN_MM = 150;      // 点击判断时间间隔（单位：毫秒）
    private static final int TOUCH_DISTANCE_THRESHOLD_IN_PX = 5;    // 点击判断位移间隔（单位：px）
    private float mDeltaX, mDeltaY;                                 //
    private float mOriginalX, mOriginalY, mOriginalRawX, mOriginalRawY;
    private long mLastTouchDownTime;                                // 上次按下的时间

    private Handler mHandler;                                       //
    private float mStatusBarHeight;
    private float mScreenWidth, mScreenHeight;
    private float mFloatInvisibleWidth;                             // 吸附状态下不可见的宽度
    private boolean mIsCoverStatusBar = true;                       // 是否覆盖状态栏
    private boolean mIsUnderStay = false;                           // 是否处于吸附状态

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
        rightStayDistance = topStayDistance = bottomStayDistance = DEFAULT_STAY_DISTANCE;

        View.inflate(getContext(), R.layout.view_float_container, this);
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

//        stopRotateAnim(this);
        if (mCallback != null) {
            mCallback.onFloatPress();
        }
        Log.d(TAG, "handleActionDown()->mOriginalX:" + mOriginalX + ",mOriginalRawX:" + mOriginalRawX);
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
        mDeltaX = getX() - mOriginalX;
        mDeltaY = getY() - mOriginalY;
        Log.d(TAG, "handleActionUp()->mDeltaX:" + mDeltaX + ",mDeltaY:" + mDeltaY);
        // doNoting

        // 优先点击处理，根据时间差判断是否要进行时间点击处理
        if (isNeedPerformClick()) {
            if (isUnderStay()) {// 吸附状态
                // 展开，这个展开一段时间后需要自动吸附，注意状态的控制
                playExtendAnim(this, getStayPosition());
            } else {// 非吸附状态
                if (mCallback != null) {
                    mCallback.onFloatClick();
                }
            }
        } else {
            if (isNeedStay()) {
                playStayAnim(this, getStayPosition());
            } else {
                playMidAnim(this);
                if (mCallback != null) {
                    mCallback.onPlayMiddleAnim();
                }
            }
        }
    }

    /**
     * 是否处于吸附状态
     */
    private boolean isUnderStay() {
        Log.d(TAG, "isUnderStay()->" + mIsUnderStay);
        return mIsUnderStay;
    }

    /**
     * 获取停留的位置（左边还是右边）
     */
    private @Position
    int getStayPosition() {
        Log.d(TAG, "getSnapDirection()->getX():" + getX());
        if (getX() < 0 && getX() >= -getWidth() * 3 / 4f) {
            Log.d(TAG, "getSnapDirection()->direction:left");
            return POS_LEFT;
        }
        if (getX() > mScreenWidth - getWidth() && getX() <= mScreenWidth - getWidth() / 4f) {
            Log.d(TAG, "getSnapDirection()->direction:right");
            return POS_RIGHT;
        }
        Log.d(TAG, "getSnapDirection()->direction:default");
        return POS_DEFAULT;
    }

    /**
     * 是否当做点击处理
     */
    private boolean isNeedPerformClick() {
        return (System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD_IN_MM)
                && Math.abs(mDeltaX) <= TOUCH_DISTANCE_THRESHOLD_IN_PX
                && Math.abs(mDeltaY) <= TOUCH_DISTANCE_THRESHOLD_IN_PX;
    }

    /**
     * 是否需要吸附效果
     */
    public boolean isNeedStay() {
        return isNeedStayLeft() || isNeedStayRight() /*|| isNeedStayTop() || isNeedStayBottom()*/;
    }

    /**
     * 是否需要靠左停留
     */
    private boolean isNeedStayLeft() {
        leftStayDistance = 0;
        return getX() < leftStayDistance;
    }

    /**
     * 是否需要靠右停留
     */
    private boolean isNeedStayRight() {
        rightStayDistance = getWidth();
        return getX() > mScreenWidth - rightStayDistance;
    }

    /**
     * 是否需要靠顶停留
     */
    private boolean isNeedStayTop() {
        if (mIsCoverStatusBar) { // 覆盖
            return getY() < topStayDistance;
        } else {
            return (getY() > mStatusBarHeight && getY() < topStayDistance + mStatusBarHeight);
        }
    }

    /**
     * 是否需要靠底停留
     */
    private boolean isNeedStayBottom() {
        return getY() < mScreenHeight - bottomStayDistance;
    }

    public void setCallback(FloatCallback callback) {
        this.mCallback = callback;
    }

    //<editor-fold>动画效果处理
    private ObjectAnimator rotateAnim;
    /**
     * 封面旋转动画时长
     */
    private long rotateAnimTimeInMillis = 2000;
    /**
     * 水平扩展动画时长
     */
    private long extendTranslateTimeInMillis = 500;
    /**
     * 水平扩展动画播放完成后再播放停留动画的时间间隔
     */
    private long extendBackDelayTimeInMillis = 4000;
    /**
     * 停留动画的时长
     */
    private long stayTranslateTimeInMillis = 500;

    /**
     * 播放扩展动画
     */
    private void playExtendAnim(View targetView, @Position int direction) {
        if (direction == POS_LEFT) {
            // 从左边往右扩展
            playExtendAnimFromLeft(targetView);
        } else if (direction == POS_RIGHT) {
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
        Log.d(TAG, "playExtendAnimFromLeft()");
        ObjectAnimator extendFromLeftTranX = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() + getWidth() * 3 / 4f);
        extendFromLeftTranX.setInterpolator(new LinearInterpolator());
        extendFromLeftTranX.setDuration(extendTranslateTimeInMillis);

        checkRotateAnim(targetView);
        AnimatorSet extendFromLeftSet = new AnimatorSet();
        extendFromLeftSet.play(rotateAnim).after(extendFromLeftTranX);

        extendFromLeftSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playExtendAnimFromLeft()->onAnimationStart()");
                Log.d(TAG, "playExtendAnimFromLeft()->mIsUnderStay:改为false");
                mIsUnderStay = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playExtendAnimFromLeft()->onAnimationEnd()");
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
                    // 判断是否需要进行回弹动画
                    if (isNeedStayBackToLeft()) {
                        Log.d(TAG, "playExtendAnimFromLeft()->吸附到左边");
                        playStayAnimToLeft(targetView, getWidth() * 3 / 4f, true);
                    } else {
                        Log.d(TAG, "playExtendAnimFromLeft()->不需要吸附到左边");
                    }
                }
            }, extendBackDelayTimeInMillis);
        }
    }

    private void playExtendAnimFromRight(final View targetView) {
        Log.d(TAG, "playExtendAnimFromRight()");
        ObjectAnimator extendFromRightTranX = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() - getWidth() * 3 / 4f);
        extendFromRightTranX.setInterpolator(new LinearInterpolator());
        extendFromRightTranX.setDuration(extendTranslateTimeInMillis);

        checkRotateAnim(targetView);
        AnimatorSet extendFromRightSet = new AnimatorSet();
        extendFromRightSet.play(rotateAnim).after(extendFromRightTranX);

        extendFromRightSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playExtendAnimFromRight()->onAnimationStart()");
                Log.d(TAG, "playExtendAnimFromRight()->mIsUnderStay:改为false");
                mIsUnderStay = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playExtendAnimFromRight()->onAnimationEnd()");
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
                    // 判断是否需要进行回弹动画
                    if (isNeedStayBackToRight()) {
                        Log.d(TAG, "playExtendAnimFromRight()->吸附到右边");
                        playStayAnimToRight(targetView, getWidth() * 3 / 4f, true);
                    } else {
                        Log.d(TAG, "playExtendAnimFromRight()->不需要吸附到右边");
                    }
                }
            }, extendBackDelayTimeInMillis);
        }
    }

    private boolean isNeedStayBackToLeft() {
        Log.d(TAG, "mOriginalRawX=" + mOriginalRawX + ",getWidth()/4f=" + getWidth() / 4f + ",isNeedStayBackToLeft:" + (mOriginalRawX <= getWidth() / 4f));
        return mOriginalRawX <= getWidth() / 4f;
    }

    private boolean isNeedStayBackToRight() {
        Log.d(TAG, "mOriginalRawX=" + mOriginalRawX + ",(mScreenWidth-getWidth()/4f)=" + (mScreenWidth - getWidth() / 4f) + ",isNeedStayBackToRight:" + (mOriginalRawX >= mScreenWidth - getWidth() / 4f));
        return mOriginalRawX >= mScreenWidth - getWidth() / 4f;
    }

    private void playStayAnim(View targetView, @Position int stayPosition) {
        float moveDistance = getSnapMoveDistance(stayPosition);
        if (stayPosition == POS_LEFT) {
            playStayAnimToLeft(targetView, moveDistance, false);
        } else if (stayPosition == POS_RIGHT) {
            playStayAnimToRight(targetView, moveDistance, false);
        }
    }

    private float getSnapMoveDistance(@Position int stayPosition) {
        if (stayPosition == POS_LEFT) {
            return Math.abs(getX() + getWidth() * 3 / 4f);
        } else if (stayPosition == POS_RIGHT) {
            return Math.abs(mScreenWidth - getWidth() / 4f - getX());
        } else {
            return 0;
        }
    }

    /**
     * 播放停留在左侧动画
     * 动画一开始，mIsUnderStay就标记为true
     *
     * @param targetView       动画View
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否是扩展动画导致的播放
     */
    private void playStayAnimToLeft(View targetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayAnimToLeft()");
        Log.d(TAG, "playStayAnimToLeft()->getX():" + getX());
        Log.d(TAG, "playStayAnimToLeft()->translationX:" + targetView.getTranslationX());
        Log.d(TAG, "playStayAnimToLeft()->moveDistance:" + moveDistance);
        // 因为currentX和maxLeftX一直会变，故用局部变量
        ObjectAnimator translateLeftAnim = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() - moveDistance);
        translateLeftAnim.setInterpolator(new LinearInterpolator());
        translateLeftAnim.setDuration(stayTranslateTimeInMillis);

        checkRotateAnim(targetView);

        AnimatorSet snapLeftSet = new AnimatorSet();
        snapLeftSet.play(rotateAnim).after(translateLeftAnim);

        snapLeftSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playStayAnimToLeft()->onAnimationStart()");
                Log.d(TAG, "playStayAnimToLeft()->mIsUnderStay:改为true");
                mIsUnderStay = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playStayAnimToLeft()->onAnimationEnd()");
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
     * 播放停留在右侧东话
     * 动画一开始，mIsUnderStay就标记为true
     *
     * @param targetView       动画View
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否是扩展动画导致的播放
     */
    private void playStayAnimToRight(View targetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayAnimToRight()");
        Log.d(TAG, "playStayAnimToRight()->translationX:" + targetView.getTranslationX());
        Log.d(TAG, "playStayAnimToRight()->moveDistance:" + moveDistance);
        // 因为currentX和maxRightX一直会变，故用局部变量
        ObjectAnimator translateRightAnim = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() + moveDistance);
        translateRightAnim.setInterpolator(new LinearInterpolator());
        translateRightAnim.setDuration(stayTranslateTimeInMillis);

        checkRotateAnim(targetView);

        AnimatorSet snapRightSet = new AnimatorSet();
        snapRightSet.play(rotateAnim).after(translateRightAnim);

        snapRightSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playStayAnimToRight()->onAnimationStart()");
                Log.d(TAG, "playStayAnimToRight()->mIsUnderStay:改为true");
                mIsUnderStay = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playStayAnimToRight()->onAnimationEnd()");
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
            rotateAnim.setDuration(rotateAnimTimeInMillis);
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
        void onPlaySnapAnimToLeft(View targetView, float currentX, float maxLeftX);

        /**
         * 右边吸附动画
         *
         * @param targetView 目标View
         * @param currentX   当前的x坐标
         * @param maxRightX  最右边的位置
         */
        void onPlaySnapAnimToRight(View targetView, float currentX, float maxRightX);

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


