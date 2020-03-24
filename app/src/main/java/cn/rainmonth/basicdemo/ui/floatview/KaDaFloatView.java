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
import android.widget.ImageView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.rainmonth.basicdemo.R;

/**
 * 支持吸附拖动的容器
 *
 * @author RandyZhang
 * @date 2020/3/12 1:24 PM
 */
public class KaDaFloatView extends FrameLayout {
    private static String TAG = "KaDaFloatView";

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

    // 停靠的上下左右边界距离（不包括）
    private int leftStayEdge, rightStayEdge, topStayEdge, bottomStayEdge;
    private static final int TOUCH_TIME_THRESHOLD_IN_MM = 150;      // 点击判断时间间隔（单位：毫秒）
    private static final int TOUCH_DISTANCE_THRESHOLD_IN_PX = 5;    // 点击判断位移间隔（单位：px）
    private float mDeltaX, mDeltaY;                                 //
    private float mOriginalX, mOriginalY, mOriginalRawX, mOriginalRawY;
    private long mLastTouchDownTime;                                // 上次按下的时间

    private Handler mHandler;                                       //
    private float mStatusBarHeight;
    private float mScreenWidth, mScreenHeight;
    private float mVisibleWidth;                                    // 吸附状态下可见的宽度
    private boolean mIsCoverStatusBar = false;                      // 是否覆盖状态栏
    private boolean mIsUnderStay = false;                           // 是否处于吸附状态

    private ImageView ivCover;                                      // 封面图
    private ImageView ivPlay;                                       // 暂停播放时的播放按钮
    private Group groupStayLeft, groupStayRight;
    private ImageView ivStayLeftBody, ivStayLeftArm;
    private ImageView ivStayRightBody, ivStayRightArm;
    private ImageView ivMusicMark1, ivMusicMark2;

    public KaDaFloatView(@NonNull Context context) {
        this(context, null);
    }

    public KaDaFloatView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KaDaFloatView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化
        mHandler = new Handler(Looper.getMainLooper());
        mScreenWidth = DpUtils.getScreenWidth(getContext());
        mScreenHeight = DpUtils.getScreenHeight(getContext());
        mStatusBarHeight = DpUtils.getStatusBarHeight(getContext());
        mVisibleWidth = DpUtils.dp2px(getContext(), 30);
        leftStayEdge = rightStayEdge = topStayEdge = bottomStayEdge = 0;

        View.inflate(getContext(), R.layout.view_float_container, this);

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

    @Override
    public void dispatchConfigurationChanged(Configuration newConfig) {
        super.dispatchConfigurationChanged(newConfig);
        // todo 使用屏幕旋转的变化
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
                playNormalAnim();
                if (mCallback != null) {
                    mCallback.onPlayNormalAnim();
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
        Log.d(TAG, "getSnapDirection()->getX():" + getX());
        if (getX() < 0 && getX() >= -(getWidth() - mVisibleWidth)) {
            Log.d(TAG, "getSnapDirection()->direction:left");
            return POS_LEFT;
        }
        if (getX() > mScreenWidth - getWidth() && getX() <= mScreenWidth - mVisibleWidth) {
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

    public void setCallback(FloatCallback callback) {
        this.mCallback = callback;
    }

    //<editor-fold>动画效果处理
    private ObjectAnimator coverRotateAnim;
    private AnimatorSet musicMarkAnimSet1, musicMarkAnimSet2;
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
     * 音符单次动画时长
     */
    private long musicMarkAnimTimeInMIlls = 2000;


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
            if (mCallback != null) {
                mCallback.onFloatClick();
            }
        }
    }

    /**
     * 从左边弹出动画
     *
     * @param extendTargetView 弹出动画目标View
     */
    public void playExtendFromLeft(final View extendTargetView) {
        Log.d(TAG, "playExtendAnimFromLeft()");
        // 弹出时只显示封面和音符，隐藏螃蟹
        groupStayLeft.setVisibility(GONE);
        groupStayRight.setVisibility(GONE);

        ObjectAnimator extendFromLeftTranX = ObjectAnimator.ofFloat(extendTargetView, View.TRANSLATION_X,
                extendTargetView.getTranslationX(), extendTargetView.getTranslationX() + (getWidth() - mVisibleWidth));
        extendFromLeftTranX.setInterpolator(new LinearInterpolator());
        extendFromLeftTranX.setDuration(extendTranslateTimeInMillis);

        checkCoverRotateAnim();
        AnimatorSet extendFromLeftSet = new AnimatorSet();
        extendFromLeftSet.play(coverRotateAnim).after(extendFromLeftTranX);

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
        if (mCallback != null) {
            mCallback.onPlayExtendFromLeft();
        }
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 判断是否需要进行回弹动画
                    if (isNeedStayBackToLeft()) {
                        Log.d(TAG, "playExtendAnimFromLeft()->吸附到左边");
                        playStayToLeft(extendTargetView, getWidth() - mVisibleWidth, true);
                    } else {
                        Log.d(TAG, "playExtendAnimFromLeft()->不需要吸附到左边");
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
        Log.d(TAG, "playExtendAnimFromRight()");
        // 弹出时只显示封面和音符，隐藏螃蟹
        groupStayLeft.setVisibility(GONE);
        groupStayRight.setVisibility(GONE);
        ObjectAnimator extendFromRightTranX = ObjectAnimator.ofFloat(extendTargetView, View.TRANSLATION_X,
                extendTargetView.getTranslationX(), extendTargetView.getTranslationX() - (getWidth() - mVisibleWidth));
        extendFromRightTranX.setInterpolator(new LinearInterpolator());
        extendFromRightTranX.setDuration(extendTranslateTimeInMillis);

        checkCoverRotateAnim();
        AnimatorSet extendFromRightSet = new AnimatorSet();
        extendFromRightSet.play(coverRotateAnim).after(extendFromRightTranX);

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
        if (mCallback != null) {
            mCallback.onPlayExtendFromRight();
        }
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 判断是否需要进行回弹动画
                    if (isNeedStayBackToRight()) {
                        Log.d(TAG, "playExtendAnimFromRight()->吸附到右边");
                        playStayToRight(extendTargetView, getWidth() - mVisibleWidth, true);
                    } else {
                        Log.d(TAG, "playExtendAnimFromRight()->不需要吸附到右边");
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
        Log.d(TAG, "playStayAnimToLeft()");
        Log.d(TAG, "playStayAnimToLeft()->getX():" + getX());
        Log.d(TAG, "playStayAnimToLeft()->translationX:" + stayTargetView.getTranslationX());
        Log.d(TAG, "playStayAnimToLeft()->moveDistance:" + moveDistance);
        Log.d(TAG, "playStayAnimToLeft()->isPlayFromExtend:" + isPlayFromExtend);
        // 因为currentX和maxLeftX一直会变，故用局部变量
        ObjectAnimator translateLeftAnim = ObjectAnimator.ofFloat(stayTargetView, View.TRANSLATION_X,
                stayTargetView.getTranslationX(), stayTargetView.getTranslationX() - moveDistance);
        translateLeftAnim.setInterpolator(new LinearInterpolator());
        translateLeftAnim.setDuration(stayTranslateTimeInMillis);
//        checkCoverRotateAnim();
//        checkMusicMarkAnimSet(true);

        AnimatorSet stayToLeftSet = new AnimatorSet();
//        snapLeftSet.play(coverRotateAnim).with(musicMarkAnimSet1).after(translateLeftAnim);
        stayToLeftSet.play(translateLeftAnim);


        stayToLeftSet.addListener(new Animator.AnimatorListener() {
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
        stayToLeftSet.start();
        if (mCallback != null) {
            mCallback.onPlayStayToLeft(stayTargetView, moveDistance, true);
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
            // 这里采用handler来延时而不是采用startDelayTime主要是因为，前者可保证目标在开始的动画的时候才显示
            mHandler.postDelayed(stayLeftKadaAnimRunnable, stayTranslateTimeInMillis);
        }

    }

    /**
     * 播放停留在右侧东话
     * 动画一开始，mIsUnderStay就标记为true
     *
     * @param stayTargetView   停留动画目标View
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否是扩展动画导致的播放
     */
    public void playStayToRight(View stayTargetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayAnimToRight()");
        Log.d(TAG, "playStayAnimToRight()->translationX:" + stayTargetView.getTranslationX());
        Log.d(TAG, "playStayAnimToRight()->moveDistance:" + moveDistance);
        Log.d(TAG, "playStayAnimToRight()->isPlayFromExtend:" + isPlayFromExtend);
        // 因为currentX和maxRightX一直会变，故用局部变量
        ObjectAnimator translateRightAnim = ObjectAnimator.ofFloat(stayTargetView, View.TRANSLATION_X,
                stayTargetView.getTranslationX(), stayTargetView.getTranslationX() + moveDistance);
        translateRightAnim.setInterpolator(new LinearInterpolator());
        translateRightAnim.setDuration(stayTranslateTimeInMillis);

//        checkCoverRotateAnim();
//        checkMusicMarkAnimSet(true);

        AnimatorSet snapRightSet = new AnimatorSet();
//        snapRightSet.play(coverRotateAnim).with(musicMarkAnimSet1).after(translateRightAnim);
        snapRightSet.play(translateRightAnim);

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
        if (mCallback != null) {
            mCallback.onPlayStayToRight(stayTargetView, moveDistance, isPlayFromExtend);
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

    int mBodyWidth = DpUtils.dp2px(getContext(), 31);  // body的宽度
    int mArmWidth = DpUtils.dp2px(getContext(), 19);   // arm的宽度

    //解决重复点击动画坚挺多次回调的问题 添加的变量
    boolean mIsPlayStayLeftKadaAnim = false;                // 是否增在播放停留在左边时的kada动画
    boolean mIsPlayStayRightKadaAnim = false;               // 是否增在播放停留在右边时的kada动画

    private long kadaAppearAnimTimeInMills = 500;                       // kada出现动画的执行时间
    private long kadaArmRotateAnimTimeInMillis = 500;                   // 手臂旋转动画执行一次的时间
    private int kadaArmRotateAnimRepeatCount = 2;                       // 手臂旋转动画重复次数
    private long kadaArmRotateAnimDelayTimeInMillis = 500;              // 手臂旋转动画延时执行的时间
    private long kadaDisappearAnimTimeInMillis = 500;                   // kada消失动画执行时间
    private long kadaDisappearAnimDelayTimeInMillis = 1000;             // kada消失动画延时执行的时间

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
                Log.d(TAG, "left outAnimStart()->translationX:" + ivStayLeftBody.getTranslationX());
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "left outAnimEnd()->translationX:" + ivStayLeftBody.getTranslationX());
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
            }, 1000);
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
        pauseCoverRotateAnim();
        pauseMusicMarkAnim();
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
        // 1、移动到初始位置
        move(0, mScreenHeight - mStatusBarHeight - getHeight() - bottomStayEdge);
    }

    /**
     * 移动道指定位置
     *
     * @param desX 目标位置x坐标
     * @param desY 目标位置y坐标
     */
    public void move(float desX, float desY) {
        fixPositionWhileMoving(desX, desY);
    }
    //</editor-fold>

    /**
     * 资源释放操作
     */
    public void release() {
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
        if (mHandler != null) {
            mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
            mHandler.removeCallbacks(stayRightKadaAnimRunnable);
            stayLeftKadaAnimRunnable = null;
            stayRightKadaAnimRunnable = null;
            mHandler = null;
        }
    }

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
         * @param targetView   目标View
         * @param moveDistance 移动的距离
         * @param isFromExtend 是否扩展动画后的停靠
         */
        void onPlayStayToLeft(View targetView, float moveDistance, boolean isFromExtend);

        /**
         * 右边吸附动画
         *
         * @param targetView   目标View
         * @param moveDistance 移动的距离
         * @param isFromExtend 是否扩展动画后的停靠
         */
        void onPlayStayToRight(View targetView, float moveDistance, boolean isFromExtend);

        /**
         * 从左扩展
         */
        void onPlayExtendFromLeft();

        /**
         * 从右扩展
         */
        void onPlayExtendFromRight();

        /**
         * 正常动画
         */
        void onPlayNormalAnim();
    }

    /**
     * 动画接口
     */
    public interface FloatAnimCallback {

    }
}