package cn.rainmonth.basicdemo.ui.floatview.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.rainmonth.basicdemo.R;
import cn.rainmonth.basicdemo.ui.floatview.DpUtils;


/**
 * 听书悬浮View
 *
 * @author 张豪成
 * @date 2020/3/12 1:24 PM
 */
public class KaDaStoryFloatView extends FrameLayout {
    private static String TAG = "FloatView";

    public static final int POS_DEFAULT = -1;                      // 默认
    public static final int POS_TOP = 0;                           // 吸附在顶部
    public static final int POS_BOTTOM = 1;                        // 吸附在底部
    public static final int POS_LEFT = 2;                          // 吸附在左边
    public static final int POS_RIGHT = 3;                         // 吸附在右边

    @IntDef({POS_DEFAULT, POS_TOP, POS_BOTTOM, POS_LEFT, POS_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    @interface Position {                                           // 吸附的方向

    }

    // 停靠的上下左右边界距离（不包括）
    private int leftStayEdge, rightStayEdge, topStayEdge, bottomStayEdge;
    private static final int TOUCH_TIME_THRESHOLD_IN_MM = 150;      // 点击判断时间间隔（单位：毫秒）
    private static final int CLICK_DISTANCE_THRESHOLD_IN_PX = 5;    // 点击判断位移间隔（单位：px）
    private static final int MOVE_DISTANCE_THRESHOLD_IN_PX = 5;    // ACTION_MOVE处理阀值（单位：px）
    private float mDeltaX, mDeltaY;                                 //
    private float mOriginalX, mOriginalY, mOriginalRawX, mOriginalRawY;
    private long mLastTouchDownTime;                                // 上次按下的时间

    private FloatViewListener mListener;                            // 悬浮回调
    private Handler mHandler;                                       // handler
    private float mStatusBarHeight;                                 // 状态栏高度
    private float mScreenWidth, mScreenHeight;                      // 屏幕宽高
    private float mVisibleWidth;                                    // 吸附状态下可见的宽度
    private boolean mIsCoverStatusBar = false;                      // 是否覆盖状态栏
    private boolean mIsUnderStay = false;                           // 是否处于吸附状态
    private boolean mIsPlay = false;                                // 是否正在播放

    private ObjectAnimator coverRotateAnim;                         // 封面旋转动画
    private AnimatorSet musicMarkAnimSet1, musicMarkAnimSet2;       // 音符动画
    private long rotateAnimTimeInMillis = 6000;                     // 封面旋转动画时长
    private long extendTranslateTimeInMillis = 500;                 // 水平扩展动画时长
    private long extendBackDelayTimeInMillis = 4000;                // 水平扩展动画播放完成后再播放停留动画的时间间隔
    private long stayTranslateTimeInMillis = 500;                   // 停留动画的时长
    private long musicMarkAnimTimeInMIlls = 2000;                   // 音符单次动画时长
    private long musicMarkAnimIntervalTimeInMillis = 600;           // 音符动效间隔时长

    //解决重复点击动画坚挺多次回调的问题 添加的变量
    boolean mIsPlayStayLeftKadaAnim = false;                        // 是否增在播放停留在左边时的kada动画
    boolean mIsPlayStayRightKadaAnim = false;                       // 是否增在播放停留在右边时的kada动画
    int mBodyWidth = DpUtils.dp2px(getContext(), 31);                        // body的宽度
    int mArmWidth = DpUtils.dp2px(getContext(), 19);                         // arm的宽度
    private long kadaAppearAnimTimeInMills = 500;                   // kada出现动画的执行时间
    private long kadaArmRotateAnimTimeInMillis = 500;               // 手臂旋转动画执行一次的时间
    private long kadaArmRotateAnimDelayTimeInMillis = 500;          // 手臂旋转动画延时执行的时间
    private long kadaDisappearAnimTimeInMillis = 500;               // kada消失动画执行时间
    private long kadaDisappearAnimDelayTimeInMillis = 1000;         // kada消失动画延时执行的时间
    private int kadaArmRotateAnimRepeatCount = 2;                   // 手臂旋转动画重复次数

    //<editor-fold>相关控件
    private ImageView ivCover;                               // 封面图
    private ImageView ivPlay;                                       // 暂停播放时的播放按钮
    private Group groupStayLeft, groupStayRight;
    private ImageView ivStayLeftBody, ivStayLeftArm;
    private ImageView ivStayRightBody, ivStayRightArm;
    private ImageView ivMusicMark1, ivMusicMark2;
    //</editor-fold>

    public KaDaStoryFloatView(@NonNull Context context) {
        this(context, null);
    }

    public KaDaStoryFloatView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KaDaStoryFloatView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化
        mHandler = new Handler(Looper.getMainLooper());
        mScreenWidth = DpUtils.getScreenWidth(getContext());
        mScreenHeight = DpUtils.getScreenHeight(getContext());
        Log.d(TAG, "init()->w:" + mScreenWidth + ",h:" + mScreenHeight);
        mStatusBarHeight = DpUtils.getStatusBarHeight(getContext());
        mVisibleWidth = DpUtils.dp2px(getContext(), 30);
        leftStayEdge = rightStayEdge = topStayEdge = bottomStayEdge = 0;
        View.inflate(getContext(), R.layout.view_kada_story_float_view, this);

        ivCover = findViewById(R.id.float_view_iv_cover);
        ivPlay = findViewById(R.id.float_view_iv_play);
        groupStayLeft = findViewById(R.id.grout_stay_left);
        ivStayLeftBody = findViewById(R.id.float_view_iv_stay_left_body);
        ivStayLeftArm = findViewById(R.id.float_view_iv_stay_left_arm);
        groupStayRight = findViewById(R.id.grout_stay_right);
        ivStayRightBody = findViewById(R.id.float_view_iv_stay_right_body);
        ivStayRightArm = findViewById(R.id.float_view_iv_stay_right_arm);
        ivMusicMark1 = findViewById(R.id.float_view_iv_music_mark1);
        ivMusicMark2 = findViewById(R.id.float_view_iv_music_mark2);
    }

    /**
     * 横竖屏切换处理（目前是将其移到左下角重置）
     */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScreenWidth = DpUtils.getScreenWidth(getContext());
        mScreenHeight = DpUtils.getScreenHeight(getContext());
        Log.d(TAG, "onConfigurationChanged()->w:" + mScreenWidth + ",h:" + mScreenHeight);
        // 使用屏幕旋转的变化
        // 1、移动到初始位置
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    reset();
                }
            }, 500); // 这个500毫秒很重要，因为横竖屏切换如果立即移动View达不到一起效果
        }

        if (mIsPlay) {
            play();
        } else {
            pause();
        }

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
                if (isNeedPerformMove(event)) {
                    Log.d(TAG, "需要处理ACTION_MOVE");
                    handleActionMove(event);
                    return false;
                } else {
                    Log.d(TAG, "无需处理ACTION_MOVE");
                }
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
        mOriginalX = getX();
        mOriginalY = getY();
        mOriginalRawX = event.getRawX();
        mOriginalRawY = event.getRawY();
        mLastTouchDownTime = System.currentTimeMillis();

        if (mListener != null) {
            mListener.onFloatPress();
        }
        Log.d(TAG, "handleActionDown()->mOriginalX:" + mOriginalX + ",mOriginalRawX:" + mOriginalRawX);
    }


    private boolean isNeedPerformMove(MotionEvent event) {
        boolean isNeedPerformMove = Math.abs(event.getRawX() - mOriginalRawX) > MOVE_DISTANCE_THRESHOLD_IN_PX
                || Math.abs(event.getRawY() - mOriginalRawY) > MOVE_DISTANCE_THRESHOLD_IN_PX;
        Log.d(TAG, "isNeedPerformMove()->isXNeedMove：" + (Math.abs(event.getRawX() - mOriginalRawX) > MOVE_DISTANCE_THRESHOLD_IN_PX));
        Log.d(TAG, "isNeedPerformMove()->isYNeedMove：" + (Math.abs(event.getRawY() - mOriginalRawY) > MOVE_DISTANCE_THRESHOLD_IN_PX));
        Log.d(TAG, "isNeedPerformMove()->" + isNeedPerformMove);
        return isNeedPerformMove;
    }

    /**
     * Action Move处理
     * 更新View的位置
     */
    private void handleActionMove(MotionEvent event) {
        float desX = mOriginalX + event.getRawX() - mOriginalRawX;
        float desY = mOriginalY + event.getRawY() - mOriginalRawY;

        fixPositionWhileMoving(desX, desY);
        if (isUnderStay()) {
            if (mStayToLeftAnim != null) {
                mStayToLeftAnim.cancel();
            }
            if (mStayToRightAnim != null) {
                mStayToRightAnim.cancel();
            }
            if (groupStayLeft.getVisibility() == VISIBLE) {
                groupStayLeft.setVisibility(GONE);
            }
            if (groupStayRight.getVisibility() == VISIBLE) {
                groupStayRight.setVisibility(GONE);
            }
            Log.d(TAG, "handleActionMove()->mIsUnderStay:改为false");
            mIsUnderStay = false;
            if (mHandler != null) {
                mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
                mHandler.removeCallbacks(stayRightKadaAnimRunnable);
            }
        }

        if (mListener != null) {
            mListener.onFloatMove();
        }
    }

    /**
     * 修正悬浮窗的位置
     *
     * @param desX 期待的目标x坐标
     * @param desY 期待的目标y坐标
     */
    private void fixPositionWhileMoving(float desX, float desY) {
        if (desX < -(getWidth() - mVisibleWidth)) {
            desX = -(getWidth() - mVisibleWidth);
        }
        if (desX > mScreenWidth - mVisibleWidth) {
            desX = mScreenWidth - mVisibleWidth;
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
        if (desY > mScreenHeight - getHeight() - mStatusBarHeight) {
            desY = mScreenHeight - getHeight() - mStatusBarHeight - bottomStayEdge;
        }

        Log.d(TAG, "fixPositionWhileMoving()->x:" + desX + ",y:" + desY);
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
                if (mListener != null) {
                    mListener.onFloatClick();
                }
            }
        } else {
            if (isNeedStay()) {
                playStayAnim(this, getStayPosition());
            } else {
                if (mIsPlay) {
                    playNormalAnim();
                    if (mListener != null) {
                        mListener.onPlayNormalAnim();
                    }
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
    public @Position
    int getStayPosition() {
        Log.d(TAG, "getStayPosition()->getX():" + getX());
        if (getX() < 0 && getX() >= -(getWidth() - mVisibleWidth)) {
            Log.d(TAG, "getStayPosition()->direction:left");
            return POS_LEFT;
        }
        if (getX() > mScreenWidth - getWidth() && getX() <= mScreenWidth - mVisibleWidth) {
            Log.d(TAG, "getStayPosition()->direction:right");
            return POS_RIGHT;
        }
        Log.d(TAG, "getStayPosition()->direction:default");
        return POS_DEFAULT;
    }

    /**
     * 是否当做点击处理
     */
    private boolean isNeedPerformClick() {
        return (System.currentTimeMillis() - mLastTouchDownTime < TOUCH_TIME_THRESHOLD_IN_MM)
                && Math.abs(mDeltaX) <= CLICK_DISTANCE_THRESHOLD_IN_PX
                && Math.abs(mDeltaY) <= CLICK_DISTANCE_THRESHOLD_IN_PX;
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
        leftStayEdge = 0;
        return getX() < leftStayEdge;
    }

    /**
     * 是否需要靠右停留
     */
    private boolean isNeedStayRight() {
        rightStayEdge = getWidth();
        return getX() > mScreenWidth - rightStayEdge;
    }

    /**
     * 是否需要靠顶停留
     */
    private boolean isNeedStayTop() {
        if (mIsCoverStatusBar) { // 覆盖
            return getY() < topStayEdge;
        } else {
            return (getY() > mStatusBarHeight && getY() < topStayEdge + mStatusBarHeight);
        }
    }

    /**
     * 是否需要靠底停留
     */
    private boolean isNeedStayBottom() {
        return getY() < mScreenHeight - bottomStayEdge;
    }

    public void setCallback(FloatViewListener callback) {
        this.mListener = callback;
    }

    //<editor-fold>动画效果处理

    /**
     * 播放展开动画
     *
     * @param extendTargetView 弹出动画目标View
     * @param direction        当前停留的位置
     */
    private void playExtendAnim(View extendTargetView, @Position int direction) {
        if (direction == POS_LEFT) {
            // 从左边往右扩展
            playExtendFromLeft(extendTargetView);
        } else if (direction == POS_RIGHT) {
            // 从右边往左扩展
            playExtendFromRight(extendTargetView);
        } else {
            // doNoting
            if (mListener != null) {
                mListener.onFloatClick();
            }
        }
    }

    /**
     * 从左边弹出动画
     *
     * @param extendTargetView 弹出动画目标View
     */
    public void playExtendFromLeft(final View extendTargetView) {
        Log.d(TAG, "playExtendFromLeft()");
        // 弹出时只显示封面和音符，隐藏螃蟹
        groupStayLeft.setVisibility(GONE);
        groupStayRight.setVisibility(GONE);

        ObjectAnimator extendFromLeftTranX = ObjectAnimator.ofFloat(extendTargetView, View.TRANSLATION_X,
                extendTargetView.getTranslationX(), extendTargetView.getTranslationX() + (getWidth() - mVisibleWidth));
        extendFromLeftTranX.setInterpolator(new LinearInterpolator());
        extendFromLeftTranX.setDuration(extendTranslateTimeInMillis);

        extendFromLeftTranX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playExtendFromLeft()->onAnimationStart()");
                Log.d(TAG, "playExtendFromLeft()->mIsUnderStay:改为false");
                mIsUnderStay = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playExtendFromLeft()->onAnimationEnd()");
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        extendFromLeftTranX.start();
        if (mListener != null) {
            mListener.onPlayExtendFromLeft();
        }
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 判断是否需要进行回弹动画
                    if (isNeedStayBackToLeft()) {
                        Log.d(TAG, "playExtendFromLeft()->吸附到左边");
                        playStayToLeft(extendTargetView, getWidth() - mVisibleWidth, true);
                    } else {
                        Log.d(TAG, "playExtendFromLeft()->不需要吸附到左边");
                    }
                }
            }, extendBackDelayTimeInMillis);
        }
    }

    /**
     * 从右边弹出动画
     *
     * @param extendTargetView 弹出动画目标View
     */
    public void playExtendFromRight(final View extendTargetView) {
        Log.d(TAG, "playExtendFromRight()");
        // 弹出时只显示封面和音符，隐藏螃蟹
        groupStayLeft.setVisibility(GONE);
        groupStayRight.setVisibility(GONE);
        ObjectAnimator extendFromRightTranX = ObjectAnimator.ofFloat(extendTargetView, View.TRANSLATION_X,
                extendTargetView.getTranslationX(), extendTargetView.getTranslationX() - (getWidth() - mVisibleWidth));
        extendFromRightTranX.setInterpolator(new LinearInterpolator());
        extendFromRightTranX.setDuration(extendTranslateTimeInMillis);

        extendFromRightTranX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playExtendFromRight()->onAnimationStart()");
                Log.d(TAG, "playExtendFromRight()->mIsUnderStay:改为false");
                mIsUnderStay = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playExtendFromRight()->onAnimationEnd()");
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        extendFromRightTranX.start();
        if (mListener != null) {
            mListener.onPlayExtendFromRight();
        }
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 判断是否需要进行回弹动画
                    if (isNeedStayBackToRight()) {
                        Log.d(TAG, "playExtendFromRight()->吸附到右边");
                        playStayToRight(extendTargetView, getWidth() - mVisibleWidth, true);
                    } else {
                        Log.d(TAG, "playExtendFromRight()->不需要吸附到右边");
                    }
                }
            }, extendBackDelayTimeInMillis);
        }
    }

    /**
     * 扩展后是否需要停靠到左边
     *
     * @return 需要时返回true
     */
    private boolean isNeedStayBackToLeft() {
        Log.d(TAG, "mOriginalRawX=" + mOriginalRawX + ",mVisibleWidth=" + mVisibleWidth + ",isNeedStayBackToLeft:" + (mOriginalRawX <= mVisibleWidth));
        return mOriginalRawX <= mVisibleWidth;
    }

    /**
     * 展开后是否需要停靠到右边
     *
     * @return 需要时返回true
     */
    private boolean isNeedStayBackToRight() {
        Log.d(TAG, "mOriginalRawX=" + mOriginalRawX + ",(mScreenWidth-mVisibleWidth)=" + (mScreenWidth - mVisibleWidth) + ",isNeedStayBackToRight:" + (mOriginalRawX >= mScreenWidth - mVisibleWidth));
        return mOriginalRawX >= mScreenWidth - mVisibleWidth;
    }

    /**
     * 播放停靠动画
     *
     * @param stayTargetView 停留动画目标View
     * @param stayPosition   停留的位置
     */
    public void playStayAnim(View stayTargetView, @Position int stayPosition) {
        float moveDistance = getStayMoveDistance(stayPosition);
        if (stayPosition == POS_LEFT) {
            playStayToLeft(stayTargetView, moveDistance, false);
        } else if (stayPosition == POS_RIGHT) {
            playStayToRight(stayTargetView, moveDistance, false);
        }
    }

    private Runnable stayLeftKadaAnimRunnable = new Runnable() {
        @Override
        public void run() {
            playStayLeftKadaAnim();
        }
    };

    private Runnable stayRightKadaAnimRunnable = new Runnable() {
        @Override
        public void run() {
            playStayRightKadaAnim();
        }
    };

    /**
     * 播放停留到左侧动画
     * 动画一开始，mIsUnderStay就标记为true
     *
     * @param stayTargetView   停留动画目标View
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否是扩展动画导致的播放
     */
    public void playStayToLeft(View stayTargetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayToLeft()");
        Log.d(TAG, "playStayToLeft()->getX():" + getX());
        Log.d(TAG, "playStayToLeft()->translationX:" + stayTargetView.getTranslationX());
        Log.d(TAG, "playStayToLeft()->moveDistance:" + moveDistance);
        Log.d(TAG, "playStayToLeft()->isPlayFromExtend:" + isPlayFromExtend);

        if (mIsUnderStay) {
            Log.e(TAG, "playStayToLeft: 当前已处于stay状态：停留在左");
            return;
        }

        Log.d(TAG, "playStayToLeft()->mIsUnderStay:改为true");
        mIsUnderStay = true;

        if (mStayToLeftAnim == null) {
            mStayToLeftAnim = new ObjectAnimator();
            mStayToLeftAnim.setTarget(this);
            mStayToLeftAnim.setProperty(View.TRANSLATION_X);
            mStayToLeftAnim.setInterpolator(new LinearInterpolator());
            mStayToLeftAnim.setDuration(stayTranslateTimeInMillis);
            mStayToLeftAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    Log.d(TAG, "playStayToLeft()->onAnimationStart()");
                    Log.d(TAG, "playStayToLeft()->tx:" + getTranslationX());
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG, "playStayToLeft()->onAnimationEnd()");
                    Log.d(TAG, "playStayToLeft()->tx:" + getTranslationX());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    Log.d(TAG, "playStayToLeft()->onAnimationCancel()");
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        // 因为moveDistance会变
        mStayToLeftAnim.setFloatValues(stayTargetView.getTranslationX(), stayTargetView.getTranslationX() - moveDistance);
        mStayToLeftAnim.start();
        if (mListener != null) {
            mListener.onPlayStayToLeft(stayTargetView, moveDistance, true);
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
            // 这里采用handler来延时而不是采用startDelayTime主要是因为，前者可保证目标在开始的动画的时候才显示
            mHandler.postDelayed(stayLeftKadaAnimRunnable, stayTranslateTimeInMillis);
        }

    }


    private ObjectAnimator mStayToLeftAnim, mStayToRightAnim;

    /**
     * 播放停留在右侧东话
     * 动画一开始，mIsUnderStay就标记为true
     *
     * @param stayTargetView   停留动画目标View
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否是扩展动画导致的播放
     */
    public void playStayToRight(View stayTargetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayToRight()");
        Log.d(TAG, "playStayToRight()->translationX:" + stayTargetView.getTranslationX());
        Log.d(TAG, "playStayToRight()->moveDistance:" + moveDistance);
        Log.d(TAG, "playStayToRight()->isPlayFromExtend:" + isPlayFromExtend);

        if (mIsUnderStay) {
            Log.e(TAG, "playStayToRight: 当前已处于stay状态：停留在右");
            return;
        }

        Log.d(TAG, "playStayToRight()->mIsUnderStay:改为true");
        mIsUnderStay = true;

        if (mStayToRightAnim == null) {
            mStayToRightAnim = new ObjectAnimator();
            mStayToRightAnim.setTarget(this);
            mStayToRightAnim.setProperty(View.TRANSLATION_X);
            mStayToRightAnim.setInterpolator(new LinearInterpolator());
            mStayToRightAnim.setDuration(stayTranslateTimeInMillis);
            mStayToRightAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    Log.d(TAG, "playStayToRight()->onAnimationStart()");
                    Log.d(TAG, "playStayToRight()->tx:" + getTranslationX());
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG, "playStayToRight()->onAnimationEnd()");
                    Log.d(TAG, "playStayToRight()->tx:" + getTranslationX());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    Log.d(TAG, "playStayToRight()->onAnimationCancel()");
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        // 因为moveDistance会变
        mStayToRightAnim.setFloatValues(stayTargetView.getTranslationX(), stayTargetView.getTranslationX() + moveDistance);
        mStayToRightAnim.start();

        if (mListener != null) {
            mListener.onPlayStayToRight(stayTargetView, moveDistance, isPlayFromExtend);
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(stayRightKadaAnimRunnable);
            mHandler.postDelayed(stayRightKadaAnimRunnable, stayTranslateTimeInMillis);
        }
    }

    /**
     * 播放正常动画
     */
    private void playNormalAnim() {
        if (groupStayLeft.getVisibility() == VISIBLE) {
            groupStayLeft.setVisibility(GONE);
        }
        if (groupStayRight.getVisibility() == VISIBLE) {
            groupStayRight.setVisibility(GONE);
        }
        playCoverRotateAnim();
        playMusicMarkAnim();
    }

    /**
     * 播放kada动画
     */
    private void playStayKadaAnim() {
        ObjectAnimator kadaTranslateY = ObjectAnimator.ofFloat(groupStayLeft, View.TRANSLATION_X, -ivStayLeftBody.getTranslationX(), 0);
        kadaTranslateY.setDuration(500);
        kadaTranslateY.setStartDelay(500);
        kadaTranslateY.start();
        int position = getStayPosition();
        if (position == POS_LEFT) {
            playStayLeftKadaAnim();
        } else if (position == POS_RIGHT) {
            playStayRightKadaAnim();
        } else {
            // do nothing
            Log.d(TAG, "playStayKadaAnim()->there is no match kadaAnim for current position");
        }
    }

    /**
     * kada动画
     */
    public void playStayLeftKadaAnim() {
        if (groupStayRight.getVisibility() == VISIBLE) {
            groupStayRight.setVisibility(GONE);
        }
        if (mIsPlayStayLeftKadaAnim) {
            return;
        }
        Log.d(TAG, "playStayLeftKadaAnim()");
        // 出现动画
        AnimatorSet kadaLeftAppearSet = new AnimatorSet();
        ObjectAnimator kadaStayLeftBodyInTranslateX = ObjectAnimator.ofFloat(ivStayLeftBody, View.TRANSLATION_X,
                ivStayLeftBody.getTranslationX() - mBodyWidth, ivStayLeftBody.getTranslationX());
        ObjectAnimator kadaStayLeftArmInTranslateX = ObjectAnimator.ofFloat(ivStayLeftArm, View.TRANSLATION_X,
                ivStayLeftArm.getTranslationX() - mBodyWidth, ivStayLeftArm.getTranslationX());
        kadaLeftAppearSet.playTogether(kadaStayLeftBodyInTranslateX, kadaStayLeftArmInTranslateX);
        kadaLeftAppearSet.setDuration(kadaAppearAnimTimeInMills);

        // 手臂摆动旋转动画
        ivStayLeftArm.setPivotX(mArmWidth / 2f);
        ivStayLeftArm.setPivotY(0);
        ObjectAnimator kadaStayLeftArmRotate = ObjectAnimator.ofFloat(ivStayLeftArm, View.ROTATION, 0, -45, 0, 15, 0);
        kadaStayLeftArmRotate.setDuration(kadaArmRotateAnimTimeInMillis);
        kadaStayLeftArmRotate.setStartDelay(kadaArmRotateAnimDelayTimeInMillis);
        kadaStayLeftArmRotate.setRepeatCount(kadaArmRotateAnimRepeatCount);

        // 消失动画
        AnimatorSet kadaDisappearSet = new AnimatorSet();
        ObjectAnimator kadaStayLeftBodyOutTranslateX = ObjectAnimator.ofFloat(ivStayLeftBody, View.TRANSLATION_X,
                ivStayLeftBody.getTranslationX(), ivStayLeftBody.getTranslationX() - mBodyWidth);
        ObjectAnimator kadaStayLeftArmOutTranslateX = ObjectAnimator.ofFloat(ivStayLeftArm, View.TRANSLATION_X,
                ivStayLeftArm.getTranslationX(), ivStayLeftArm.getTranslationX() - mBodyWidth);
        kadaDisappearSet.playTogether(kadaStayLeftBodyOutTranslateX, kadaStayLeftArmOutTranslateX);
        kadaDisappearSet.setStartDelay(kadaDisappearAnimDelayTimeInMillis);
        kadaDisappearSet.setDuration(kadaDisappearAnimTimeInMillis);
        kadaDisappearSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playStayToLeft()->outAnimStart()->translationX:" + ivStayLeftBody.getTranslationX());
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playStayToLeft()->outAnimEnd()->translationX:" + ivStayLeftBody.getTranslationX());
                // 恢复控件的TranslationX的值
                ivStayLeftBody.setTranslationX(ivStayLeftBody.getTranslationX() + mBodyWidth);
                ivStayLeftArm.setTranslationX(ivStayLeftArm.getTranslationX() + mBodyWidth);
                groupStayLeft.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        AnimatorSet stayLeftAnimSet = new AnimatorSet();
        stayLeftAnimSet.play(kadaStayLeftArmRotate).after(kadaLeftAppearSet).before(kadaDisappearSet);
        stayLeftAnimSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (groupStayLeft.getVisibility() != VISIBLE) {
                    groupStayLeft.setVisibility(VISIBLE);
                }
                mIsPlayStayLeftKadaAnim = true;
                Log.d(TAG, "mIsPlayStayLeftKadaAnim:" + true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsPlayStayLeftKadaAnim = false;
                Log.d(TAG, "mIsPlayStayLeftKadaAnim:" + false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        stayLeftAnimSet.start();
    }

    /**
     * kada动画
     */
    public void playStayRightKadaAnim() {
        if (groupStayLeft.getVisibility() == VISIBLE) {
            groupStayLeft.setVisibility(GONE);
        }
        if (mIsPlayStayRightKadaAnim) {
            return;
        }
        Log.d(TAG, "playStayRightKadaAnim()");
        AnimatorSet kadaRightAppearSet = new AnimatorSet();
        // 身体显示位移动画
        ObjectAnimator kadaStayRightBodyTranslateX = ObjectAnimator.ofFloat(ivStayRightBody, View.TRANSLATION_X,
                ivStayRightBody.getTranslationX() + mBodyWidth, ivStayRightBody.getTranslationX());
        // 手臂显示位移动画
        ObjectAnimator kadaStayRightArmTranslateX = ObjectAnimator.ofFloat(ivStayRightArm, View.TRANSLATION_X,
                ivStayRightArm.getTranslationX() + mBodyWidth, ivStayRightArm.getTranslationX());
        kadaRightAppearSet.playTogether(kadaStayRightBodyTranslateX, kadaStayRightArmTranslateX);
        kadaRightAppearSet.setDuration(500);


        // 手臂摆动旋转动画
        ivStayRightArm.setPivotX(mArmWidth / 2f);
        ivStayRightArm.setPivotY(0);
        ObjectAnimator kadaStayRightArmRotate = ObjectAnimator.ofFloat(ivStayRightArm, View.ROTATION, 0, 45, 0, -15, 0);
        kadaStayRightArmRotate.setDuration(500);
        kadaStayRightArmRotate.setStartDelay(500);
        kadaStayRightArmRotate.setRepeatCount(2);

        AnimatorSet kadaRightDisappearSet = new AnimatorSet();
        ObjectAnimator kadaStayLeftBodyOutTranslateX = ObjectAnimator.ofFloat(ivStayRightBody, View.TRANSLATION_X,
                ivStayRightBody.getTranslationX(), ivStayRightBody.getTranslationX() + mBodyWidth);
        ObjectAnimator kadaStayLeftArmOutTranslateX = ObjectAnimator.ofFloat(ivStayRightArm, View.TRANSLATION_X,
                ivStayRightArm.getTranslationX(), ivStayRightArm.getTranslationX() + mBodyWidth);
        kadaRightDisappearSet.playTogether(kadaStayLeftBodyOutTranslateX, kadaStayLeftArmOutTranslateX);
        kadaRightDisappearSet.setStartDelay(1000);
        kadaRightDisappearSet.setDuration(500);
        kadaRightDisappearSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "right outAnimStart()->translationX:" + ivStayRightBody.getTranslationX());
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "right outAnimEnd()->translationX:" + ivStayRightBody.getTranslationX());
                // 恢复控件的TranslationX的值
                ivStayRightBody.setTranslationX(ivStayRightBody.getTranslationX() - mBodyWidth);
                ivStayRightArm.setTranslationX(ivStayRightArm.getTranslationX() - mBodyWidth);
                groupStayRight.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(kadaStayRightArmRotate).after(kadaRightAppearSet).before(kadaRightDisappearSet);
        animatorSet.play(kadaStayRightBodyTranslateX).with(kadaStayRightArmTranslateX).before(kadaStayRightArmRotate);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (groupStayRight.getVisibility() != VISIBLE) {
                    groupStayRight.setVisibility(VISIBLE);
                }
                mIsPlayStayRightKadaAnim = true;
                Log.d(TAG, "mIsPlayStayRightKadaAnim:" + true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsPlayStayRightKadaAnim = false;
                Log.d(TAG, "mIsPlayStayRightKadaAnim:" + false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    /**
     * 检查音符动画集合
     * 这个动画应该需要区分当前所在屏幕的位置的
     */
    private void checkMusicMarkAnimSet() {
        if (musicMarkAnimSet1 == null) {
            musicMarkAnimSet1 = initMusicMarkAnimSet(ivMusicMark1);
        }
        if (musicMarkAnimSet2 == null) {
            musicMarkAnimSet2 = initMusicMarkAnimSet(ivMusicMark2);
        }
    }

    /**
     * 初始化音符动画
     *
     * @param musicMartView 对应的音符View
     */
    private AnimatorSet initMusicMarkAnimSet(View musicMartView) {
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(musicMartView, View.SCALE_X, 0, 1.5f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(musicMartView, View.SCALE_Y, 0, 1.5f);
        ObjectAnimator translateXAnim = ObjectAnimator.ofFloat(musicMartView, View.TRANSLATION_X, 0, DpUtils.dp2px(getContext(), 18));
        ObjectAnimator translateYAnim = ObjectAnimator.ofFloat(musicMartView, View.TRANSLATION_Y, 0, -DpUtils.dp2px(getContext(), 48));
//            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(ivMusicMark, View.ALPHA, 0, 1);
        scaleXAnim.setRepeatCount(ValueAnimator.INFINITE);
        scaleYAnim.setRepeatCount(ValueAnimator.INFINITE);
        translateXAnim.setRepeatCount(ValueAnimator.INFINITE);
        translateYAnim.setRepeatCount(ValueAnimator.INFINITE);
//            alphaAnim.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet musicMarkAnimSet = new AnimatorSet();
        musicMarkAnimSet.playTogether(scaleXAnim, scaleYAnim, translateXAnim, translateYAnim/*, alphaAnim*/);
        musicMarkAnimSet.setDuration(musicMarkAnimTimeInMIlls);

        return musicMarkAnimSet;
    }

    /**
     * 检查封面动画集合
     */
    private void checkCoverRotateAnim() {
        if (coverRotateAnim == null) {
            coverRotateAnim = ObjectAnimator.ofFloat(ivCover, View.ROTATION, 0, 360);
            coverRotateAnim.setInterpolator(new LinearInterpolator());
            coverRotateAnim.setRepeatCount(ValueAnimator.INFINITE);
            coverRotateAnim.setDuration(rotateAnimTimeInMillis);
        }
    }

    /**
     * 播放封面旋转动画
     */
    private void playCoverRotateAnim() {
        if (ivPlay.getVisibility() != GONE) {
            ivPlay.setVisibility(GONE);
        }
        checkCoverRotateAnim();
        coverRotateAnim.start();
    }

    /**
     * 隐藏封面旋转动画
     */
    private void pauseCoverRotateAnim() {
        if (ivPlay.getVisibility() != VISIBLE) {
            ivPlay.setVisibility(VISIBLE);
        }
        checkCoverRotateAnim();
        coverRotateAnim.pause();
    }

    /**
     * 音符动画
     * 1、缩放动画
     * 2、唯一动画
     * 3、alpha动画
     */
    private void playMusicMarkAnim() {
        checkMusicMarkAnimSet();
        if (ivMusicMark1.getVisibility() != VISIBLE) {
            ivMusicMark1.setVisibility(VISIBLE);
        }
        musicMarkAnimSet1.start();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ivMusicMark2.getVisibility() != VISIBLE) {
                        ivMusicMark2.setVisibility(VISIBLE);
                    }
                    musicMarkAnimSet2.start();
                }
            }, musicMarkAnimIntervalTimeInMillis);
        }

    }

    private void pauseMusicMarkAnim() {
        checkMusicMarkAnimSet();
        ivMusicMark1.setVisibility(GONE);
        ivMusicMark2.setVisibility(GONE);
        musicMarkAnimSet1.pause();
        musicMarkAnimSet2.pause();
    }
    //</editor-fold>

    //<editor-fold>对外提供的API

    /**
     * 更新信息
     * todo 及时更新封面信息（需要更改事件处理，现有的两个事件无法做到及时更新）
     *
     * @param storyInfo 正在播放的
     */
//    public void update(StoryInfo storyInfo) {
//        if (storyInfo == null) {
//            return;
//        }
//        String coverNoLetterUrl = storyInfo.getCoverNoLetter();
//        if (!TextUtils.isEmpty(coverNoLetterUrl)) {
//            boolean isNeedReset = true;
//            String tagUrl = (String) ivCover.getTag(R.id.itemView);
//            if (!TextUtils.isEmpty(tagUrl)) {
//                if (TextUtils.equals(coverNoLetterUrl, tagUrl)) {
//                    isNeedReset = false;
//                }
//            }
//
//            if (isNeedReset) {
//                ivCover.setTag(R.id.itemView, coverNoLetterUrl);
//                FrescoUtils.showImg(ivCover, coverNoLetterUrl);
//            }
//        }
//    }

    /**
     * 获取移动的距离
     *
     * @param stayPosition 停留的位置{@link Position#POS_LEFT},{@link Position#POS_RIGHT},etc
     * @return 移动的距离
     */
    public float getStayMoveDistance(@Position int stayPosition) {
        if (stayPosition == POS_LEFT) {
            return Math.abs(getX() + getWidth() - mVisibleWidth);
        } else if (stayPosition == POS_RIGHT) {
            return Math.abs(mScreenWidth - mVisibleWidth - getX());
        } else {
            return 0;
        }
    }

    /**
     * 显示
     */
    public void show(boolean isPlay) {
        Log.d(TAG, "show()->w:" + getWidth() + ",h:" + getHeight());
        setVisibility(VISIBLE);
        if (isPlay) {
            play();
        } else {
            pause();
        }
    }

    /**
     * 隐藏
     */
    public void hide() {
        setVisibility(GONE);
    }

    /**
     * 播放
     */
    public void play() {
        Log.d(TAG, "play()");
        mIsPlay = true;
        playCoverRotateAnim();
        playMusicMarkAnim();

        if (isNeedStay()) {
            playStayAnim(this, getStayPosition());
        } else {
            playNormalAnim();
        }

    }

    /**
     * 暂停
     */
    public void pause() {
        Log.d(TAG, "pause");
        mIsPlay = false;
        pauseCoverRotateAnim();
        pauseMusicMarkAnim();
    }

    /**
     * 资源释放操作
     */
    public void release() {
        Log.d(TAG, "release()");
        if (coverRotateAnim != null) {
            coverRotateAnim.removeAllListeners();
            coverRotateAnim.removeAllUpdateListeners();
            coverRotateAnim = null;
        }
        if (musicMarkAnimSet1 != null) {
            musicMarkAnimSet1.removeAllListeners();
            musicMarkAnimSet1 = null;
        }
        if (musicMarkAnimSet2 != null) {
            musicMarkAnimSet2.removeAllListeners();
            musicMarkAnimSet2 = null;
        }

        if (mStayToLeftAnim != null) {
            mStayToLeftAnim.removeAllListeners();
            mStayToLeftAnim = null;
        }
        if (mStayToRightAnim != null) {
            mStayToRightAnim.removeAllListeners();
            mStayToRightAnim = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
            mHandler.removeCallbacks(stayRightKadaAnimRunnable);
            stayLeftKadaAnimRunnable = null;
            stayRightKadaAnimRunnable = null;
            mHandler = null;
        }
    }

    public void setIsCoverStatusBar(boolean isCoverStatusBar) {
        this.mIsCoverStatusBar = isCoverStatusBar;
    }

    public void setLeftStayEdge(int leftStayEdge) {
        this.leftStayEdge = leftStayEdge;
    }

    public void setRightStayEdge(int rightStayEdge) {
        this.rightStayEdge = rightStayEdge;
    }

    public void setTopStayEdge(int topStayEdge) {
        this.topStayEdge = topStayEdge;
    }

    public void setBottomStayEdge(int bottomStayEdge) {
        this.bottomStayEdge = bottomStayEdge;
    }

    /**
     * 重置 回到左下方
     */
    public void reset() {
        Log.d(TAG, "reset()->reset to:(0, " + (mScreenHeight - mStatusBarHeight - getHeight() - bottomStayEdge) + ")");
        move(0, mScreenHeight - mStatusBarHeight - getHeight() - bottomStayEdge);
    }

    /**
     * 移动道指定位置
     *
     * @param desX 目标位置x坐标
     * @param desY 目标位置y坐标
     */
    public void move(float desX, float desY) {
        Log.d(TAG, "move()->move to:(" + desX + "," + desY + "/");
        fixPositionWhileMoving(desX, desY);
    }
    //</editor-fold>

    public interface FloatViewListener {

        /**
         * 悬浮窗按下回调
         */
        default void onFloatPress() {

        }

        /**
         * 悬浮窗移动回调
         * never do too much create ops!!!
         */
        default void onFloatMove() {

        }

        /**
         * 悬浮窗点击回调
         */
        void onFloatClick();

        /**
         * 左边吸附动画
         *
         * @param targetView   目标View
         * @param moveDistance 移动的距离
         * @param isFromExtend 是否扩展动画后的停靠
         */
        default void onPlayStayToLeft(View targetView, float moveDistance, boolean isFromExtend) {

        }

        /**
         * 右边吸附动画
         *
         * @param targetView   目标View
         * @param moveDistance 移动的距离
         * @param isFromExtend 是否扩展动画后的停靠
         */
        default void onPlayStayToRight(View targetView, float moveDistance, boolean isFromExtend) {

        }

        /**
         * 从左扩展
         */
        default void onPlayExtendFromLeft() {

        }

        /**
         * 从右扩展
         */
        default void onPlayExtendFromRight() {

        }

        /**
         * 正常动画
         */
        default void onPlayNormalAnim() {

        }
    }
}