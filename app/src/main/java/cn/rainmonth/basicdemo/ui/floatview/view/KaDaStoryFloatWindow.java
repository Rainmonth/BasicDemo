package cn.rainmonth.basicdemo.ui.floatview.view;

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
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import cn.rainmonth.basicdemo.R;
import cn.rainmonth.basicdemo.ui.floatview.DpUtils;
import cn.rainmonth.basicdemo.ui.floatview.util.C;

import static cn.rainmonth.basicdemo.ui.floatview.util.C.Position.POS_DEFAULT;
import static cn.rainmonth.basicdemo.ui.floatview.util.C.Position.POS_LEFT;
import static cn.rainmonth.basicdemo.ui.floatview.util.C.Position.POS_RIGHT;


/**
 * 主要不同在于onTouchEvent的处理上
 *
 * @author 张豪成
 * @date 2020/3/12 1:24 PM
 */
public class KaDaStoryFloatWindow extends FrameLayout implements IFloatView {
    private static String TAG = "FloatWindow";


    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWmParams;
    private Context mContext;

    // 停靠的上下左右边界距离（不包括）
    private int leftStayEdge, rightStayEdge, topStayEdge, bottomStayEdge;
    private static final int TOUCH_TIME_THRESHOLD_IN_MM = 150;      // 点击判断时间间隔（单位：毫秒）
    private static final int TOUCH_DISTANCE_THRESHOLD_IN_PX = 5;    // 点击判断位移间隔（单位：px）
    private float mDeltaX, mDeltaY;                                 //
    private float mTouchStartX, mTouchStartY;                       // 按下时的坐标
    private float mOriginalRawX, mOriginalRawY;                     // 相对屏幕左上角的坐标
    private long mTouchDownTime;                                    // 按下的时间
    private long mTouchUpTime;                                      // 松开的事件

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

    public KaDaStoryFloatWindow(@NonNull Context context) {
        this(context, null);
    }

    public KaDaStoryFloatWindow(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KaDaStoryFloatWindow(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        // 初始化
        mHandler = new Handler(Looper.getMainLooper());
        mScreenWidth = DpUtils.getScreenWidth(mContext);
        mScreenHeight = DpUtils.getScreenHeight(mContext);
        Log.d(TAG, "init()->w:" + mScreenWidth + ",h:" + mScreenHeight);
        mStatusBarHeight = DpUtils.getStatusBarHeight(mContext);
        mVisibleWidth = DpUtils.dp2px(mContext, 30);
        leftStayEdge = rightStayEdge = topStayEdge = bottomStayEdge = 0;
        View.inflate(context, R.layout.view_kada_story_float_window, this);

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
        mScreenWidth = DpUtils.getScreenWidth(mContext);
        mScreenHeight = DpUtils.getScreenHeight(mContext);
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
        // 获取相对屏幕的坐标，即以屏幕左上角为原点
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        //下面的这些事件，跟图标的移动无关，为了区分开拖动和点击事件
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownTime = System.currentTimeMillis();
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                //图标移动的逻辑在这里
                float mMoveStartX = event.getX();
                float mMoveStartY = event.getY();
                // 如果移动量大于3才移动
                if (Math.abs(mTouchStartX - mMoveStartX) > 3
                        && Math.abs(mTouchStartY - mMoveStartY) > 3) {
                    // 更新浮动窗口位置参数
                    mWmParams.x = (int) (x - mTouchStartX);
                    mWmParams.y = (int) (y - mTouchStartY);
                    mWindowManager.updateViewLayout(this, mWmParams);
                    if (mWmParams.x < mScreenWidth) {
                        if (isUnderStay()) {
                            setTranslationX(0);
                            if (groupStayLeft.getVisibility() == VISIBLE) {
                                groupStayLeft.setVisibility(GONE);
                            }
                            if (groupStayRight.getVisibility() == VISIBLE) {
                                groupStayRight.setVisibility(GONE);
                            }
                            mIsUnderStay = false;
                            if (mHandler != null) {
                                mHandler.removeCallbacks(stayRightKadaAnimRunnable);
                                mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
                            }
                        }
                    }
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent():" +
                        "\ngetWidth()=" + getWidth() +
                        "\ngetHeight()=" + getHeight() +
                        "\nmVisibleWidth=" + mVisibleWidth +
                        "\ntx=" + getTranslationX() +
                        "\ngetX()=" + getX() +
                        "\ngetY()=" + getY() +
                        "\ngetLeft()=" + getLeft() +
                        "\ngetRight()=" + getRight() +
                        "\nmWmParams.x=" + mWmParams.x +
                        "\nmWmParams.y=" + mWmParams.y);
                mTouchUpTime = System.currentTimeMillis();
                //当从点击到弹起小于半秒的时候,则判断为点击,如果超过则不响应点击事件
                isClick = (mTouchUpTime - mTouchDownTime) <= TOUCH_TIME_THRESHOLD_IN_MM;
                if (isClick) {
                    if (isUnderStay()) {
                        playExtendAnim(this, getStayPosition());
                    } else {
                        Toast.makeText(mContext, "Clicked", Toast.LENGTH_SHORT).show();
                        if (mListener != null) {
                            mListener.onFloatClick();
                        }
                    }
                } else {
                    if (isNeedStay()) {
                        playStayAnim(this, getStayPosition());
                    } else {
                        playNormalAnim();
//                        if (mIsPlay) {
//                            playNormalAnim();
//                            if (mListener != null) {
//                                mListener.onPlayNormalAnim();
//                            }
//                        }
                    }
                }
                break;
        }
        //响应点击事件

        return true;
    }

    boolean isClick = false;

    /**
     * 修正悬浮窗的位置
     *
     * @param desX 期待的目标x坐标
     * @param desY 期待的目标y坐标
     */
    private void fixPositionWhileMoving(float desX, float desY) {
        Log.d(TAG, "fixPositionWhileMoving()->w:" + getWidth() + ",h:" + getHeight());
        if (desX < -(getWidth() - mVisibleWidth)) {
            desX = -(getWidth() - mVisibleWidth);
        }
        if (desX > mScreenWidth - mVisibleWidth) {
            desX = mScreenWidth - mVisibleWidth;
        }
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
//        setX(desX);
//        setY(desY);
        if (mWmParams != null && mWindowManager != null) {
            mWmParams.x = (int) desX;
            mWmParams.y = (int) desY;
            mWindowManager.updateViewLayout(this, mWmParams);
        }
    }

    /**
     * 是否处于吸附状态
     */
    private boolean isUnderStay() {
        Log.d(TAG, "isUnderStay()->" + mIsUnderStay);
        return mIsUnderStay;
    }

    //<editor-fold> IFloatView实现

    @Override
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

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        mIsPlay = false;
        pauseCoverRotateAnim();
        pauseMusicMarkAnim();
    }

    @Override
    public void show(boolean isPlay) {
        Log.d(TAG, "show()->w:" + getWidth() + ",h:" + getHeight());
        setVisibility(VISIBLE);
        if (isPlay) {
            play();
        } else {
            pause();
        }
    }

    @Override
    public void hide() {
        setVisibility(GONE);
    }

    @Override
    public void reset() {
        Log.d(TAG, "reset()->reset to:(0, " + (mScreenHeight - mStatusBarHeight - getHeight() - bottomStayEdge) + ")");
        move(0, mScreenHeight - mStatusBarHeight - getHeight() - bottomStayEdge);
    }

    @Override
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
        if (mHandler != null) {
            mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
            mHandler.removeCallbacks(stayRightKadaAnimRunnable);
            stayLeftKadaAnimRunnable = null;
            stayRightKadaAnimRunnable = null;
            mHandler = null;
        }
    }

    @Override
    public void move(float desX, float desY) {
        Log.d(TAG, "move()->move to:(" + desX + "," + desY + "/");
        fixPositionWhileMoving(desX, desY);
    }

    @Override
    public int getStayPosition() {
        Log.d(TAG, "getStayPosition()->mWmParams.x:" + mWmParams.x);
        Log.d(TAG, "playStayToRight()->tx:" + getTranslationX());
        Log.d(TAG, "playStayToRight()->getWidth():" + getWidth());
        Log.d(TAG, "playStayToRight()->mVisibleWidth:" + mVisibleWidth);
        if (mWmParams.x <= 0) {
            Log.d(TAG, "getStayPosition()->direction:left");
            return POS_LEFT;
        }
        if (mWmParams.x >= mScreenWidth - getWidth()) {
            Log.d(TAG, "getStayPosition()->direction:right");
            return POS_RIGHT;
        }
        Log.d(TAG, "getStayPosition()->direction:default");
        return POS_DEFAULT;
    }

    @Override
    public boolean isNeedStay() {
        return isNeedStayLeft() || isNeedStayRight() /*|| isNeedStayTop() || isNeedStayBottom()*/;
    }

    @Override
    public void playStayToLeft(View stayTargetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayToLeft()");
        Log.d(TAG, "playStayToLeft()->mWmParams.x:" + mWmParams.x);
        Log.d(TAG, "playStayToLeft()->translationX:" + stayTargetView.getTranslationX());
        Log.d(TAG, "playStayToLeft()->moveDistance:" + moveDistance);
        Log.d(TAG, "playStayToLeft()->isPlayFromExtend:" + isPlayFromExtend);
        if (mIsUnderStay) {
            Log.e(TAG, "playStayToLeft()->当前已处于stay状态");
            return;
        }

        ValueAnimator windowTLAnim = ValueAnimator.ofInt(mWmParams.x, 0);
        windowTLAnim.setDuration(300);
        windowTLAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        windowTLAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mWmParams.x = (int) animation.getAnimatedValue();
                try {
                    mWindowManager.updateViewLayout(KaDaStoryFloatWindow.this, mWmParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // 因为currentX和maxLeftX一直会变，故用局部变量
        ObjectAnimator translateLeftAnim = ObjectAnimator.ofFloat(stayTargetView, View.TRANSLATION_X,
                stayTargetView.getTranslationX(), stayTargetView.getTranslationX() - (getWidth() - mVisibleWidth));
        translateLeftAnim.setInterpolator(new LinearInterpolator());
        translateLeftAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playStayToLeft()->onAnimationStart()");
                Log.d(TAG, "playStayToLeft()->mIsUnderStay:改为true");
                Log.d(TAG, "tx:" + stayTargetView.getTranslationX());
                mIsUnderStay = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playStayToLeft()->onAnimationEnd()");
                Log.d(TAG, "tx:" + stayTargetView.getTranslationX());
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(translateLeftAnim).after(windowTLAnim);
        animatorSet.setDuration(stayTranslateTimeInMillis);
        animatorSet.start();

        if (mListener != null) {
            mListener.onPlayStayToLeft(stayTargetView, moveDistance, true);
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(stayLeftKadaAnimRunnable);
            // 这里采用handler来延时而不是采用startDelayTime主要是因为，前者可保证目标在开始的动画的时候才显示
            mHandler.postDelayed(stayLeftKadaAnimRunnable, stayTranslateTimeInMillis + 500);
        }

    }

    @Override
    public void playStayToRight(View stayTargetView, float moveDistance, boolean isPlayFromExtend) {
        Log.d(TAG, "playStayToRight()");
        Log.d(TAG, "playStayToLeft()->mWmParams.x:" + mWmParams.x);
        Log.d(TAG, "playStayToLeft()->translationX:" + stayTargetView.getTranslationX());
        Log.d(TAG, "playStayToLeft()->moveDistance:" + moveDistance);
        Log.d(TAG, "playStayToRight()->isPlayFromExtend:" + isPlayFromExtend);
        if (mIsUnderStay) {
            Log.e(TAG, "playStayToRight()->当前已处于stay状态");
            return;
        }

        ValueAnimator windowTRAnim = ValueAnimator.ofInt(mWmParams.x, (int) mScreenWidth);
        windowTRAnim.setDuration(300);
        windowTRAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        windowTRAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mWmParams.x = (int) animation.getAnimatedValue();
                try {
                    mWindowManager.updateViewLayout(KaDaStoryFloatWindow.this, mWmParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        // 因为currentX和maxRightX一直会变，故用局部变量
        ObjectAnimator translateRightAnim = ObjectAnimator.ofFloat(stayTargetView, View.TRANSLATION_X,
                stayTargetView.getTranslationX(), stayTargetView.getTranslationX() + getWidth() - mVisibleWidth);
        translateRightAnim.setInterpolator(new LinearInterpolator());
        translateRightAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "playStayToRight()->onAnimationStart()");
                Log.d(TAG, "playStayToRight()->mIsUnderStay:改为true");
                Log.d(TAG, "tx:" + stayTargetView.getTranslationX());
                mIsUnderStay = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playStayToRight()->onAnimationEnd()");
                Log.d(TAG, "playStayToRight()->tx:" + stayTargetView.getTranslationX());
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(translateRightAnim).after(windowTRAnim);
        animatorSet.setDuration(stayTranslateTimeInMillis);
        animatorSet.start();

        if (mListener != null) {
            mListener.onPlayStayToRight(stayTargetView, moveDistance, isPlayFromExtend);
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(stayRightKadaAnimRunnable);
            // 这里采用handler来延时而不是采用startDelayTime主要是因为，前者可保证目标在开始的动画的时候才显示
            mHandler.postDelayed(stayRightKadaAnimRunnable, stayTranslateTimeInMillis + 500);
        }
    }

    @Override
    public void playExtendFromLeft(final View extendTargetView) {
        Log.d(TAG, "playExtendAnimFromLeft()");
        if (getStayPosition() != POS_LEFT) {
            Log.e(TAG, "playExtendFromLeft()->" + "当前未停留在左侧");
            return;
        }
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

    @Override
    public void playExtendFromRight(final View extendTargetView) {
        if (getStayPosition() != POS_RIGHT) {
            Log.e(TAG, "playExtendFromRight()->" + "当前未停留在左侧");
            return;
        }
        Log.d(TAG, "playExtendAnimFromRight()");
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
                Log.d(TAG, "playExtendFromRight()->tx:" + extendTargetView.getTranslationX());
                mIsUnderStay = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "playExtendFromRight()->onAnimationEnd()");
                Log.d(TAG, "playExtendFromRight()->tx:" + extendTargetView.getTranslationX());
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

    @Override
    public boolean isNeedStayBackToLeft() {
        Log.d(TAG, "mWmParams.x=" + mWmParams.x + ",mVisibleWidth=" + mVisibleWidth + ",isNeedStayBackToLeft:" + (mWmParams.x <= mVisibleWidth));
        return mWmParams.x <= mVisibleWidth;
    }

    @Override
    public boolean isNeedStayBackToRight() {
        Log.d(TAG, "mWmParams.x=" + mWmParams.x + ",(mScreenWidth-mVisibleWidth)=" + (mScreenWidth - mVisibleWidth) + ",isNeedStayBackToRight:" + (mWmParams.x >= mScreenWidth - mVisibleWidth));
        return mWmParams.x >= mScreenWidth - mVisibleWidth;
    }
    //</editor-fold>


    /**
     * 是否需要靠左停留
     */
    private boolean isNeedStayLeft() {
        leftStayEdge = 0;
        return mWmParams.x <= leftStayEdge;
    }

    /**
     * 是否需要靠右停留
     */
    private boolean isNeedStayRight() {
        rightStayEdge = getWidth();
        return mWmParams.x >= mScreenWidth - rightStayEdge;
    }

    /**
     * 是否需要靠顶停留
     */
    private boolean isNeedStayTop() {
        if (mIsCoverStatusBar) { // 覆盖
            return mWmParams.y < topStayEdge;
        } else {
            return (mWmParams.y > mStatusBarHeight && mWmParams.y < topStayEdge + mStatusBarHeight);
        }
    }

    /**
     * 是否需要靠底停留
     */
    private boolean isNeedStayBottom() {
        return mWmParams.y < mScreenHeight - bottomStayEdge;
    }

    public void setCallback(FloatViewListener callback) {
        this.mListener = callback;
    }

    //<editor-fold>动画效果处理

    /**
     * 播放展开动画
     *
     * @param extendTargetView 弹出动画目标View
     * @param direction        当前停留的位置 {@link C.Position}
     */
    private void playExtendAnim(View extendTargetView, int direction) {
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
     * 播放停靠动画
     *
     * @param stayTargetView 停留动画目标View
     * @param stayPosition   停留的位置 {@link C.Position}
     */
    public void playStayAnim(View stayTargetView, int stayPosition) {
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
        AnimatorSet kadaLeftDisappearSet = new AnimatorSet();
        ObjectAnimator kadaStayLeftBodyOutTranslateX = ObjectAnimator.ofFloat(ivStayLeftBody, View.TRANSLATION_X,
                ivStayLeftBody.getTranslationX(), ivStayLeftBody.getTranslationX() - mBodyWidth);
        ObjectAnimator kadaStayLeftArmOutTranslateX = ObjectAnimator.ofFloat(ivStayLeftArm, View.TRANSLATION_X,
                ivStayLeftArm.getTranslationX(), ivStayLeftArm.getTranslationX() - mBodyWidth);
        kadaLeftDisappearSet.playTogether(kadaStayLeftBodyOutTranslateX, kadaStayLeftArmOutTranslateX);
        kadaLeftDisappearSet.setStartDelay(kadaDisappearAnimDelayTimeInMillis);
        kadaLeftDisappearSet.setDuration(kadaDisappearAnimTimeInMillis);
        kadaLeftDisappearSet.addListener(new Animator.AnimatorListener() {
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
        stayLeftAnimSet.play(kadaStayLeftArmRotate).after(kadaLeftAppearSet).before(kadaLeftDisappearSet);
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
        // 整体显示动画
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

        // kada整体消失
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
        ObjectAnimator translateXAnim = ObjectAnimator.ofFloat(musicMartView, View.TRANSLATION_X, 0, DpUtils.dp2px(mContext, 18));
        ObjectAnimator translateYAnim = ObjectAnimator.ofFloat(musicMartView, View.TRANSLATION_Y, 0, -DpUtils.dp2px(mContext, 48));
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

    public void setWindowLayoutParams(WindowManager.LayoutParams params) {
        mWmParams = params;
    }

    /**
     * 更新信息
     * todo 及时更新封面信息（需要更改事件处理，现有的两个事件无法做到及时更新）
     */
//    public void update(StoryInfo storyInfo) {
    public void update() {
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
    }

    /**
     * 获取移动的距离
     *
     * @param stayPosition 停留的位置{@link C.Position#POS_LEFT},{@link C.Position#POS_RIGHT},etc
     * @return 移动的距离
     */
    public float getStayMoveDistance(int stayPosition) {
        if (stayPosition == POS_LEFT) {
            return Math.abs(mWmParams.x + getWidth() - mVisibleWidth);
        } else if (stayPosition == POS_RIGHT) {
            return Math.abs(mScreenWidth - mVisibleWidth - mWmParams.x);
        } else {
            return 0;
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