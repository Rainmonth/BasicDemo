package cn.rainmonth.basicdemo.ui.floatview;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import cn.rainmonth.basicdemo.R;
import cn.rainmonth.basicdemo.ui.floatview.manager.FloatWindowManager;
import cn.rainmonth.basicdemo.ui.floatview.permission.FloatPermissionManager;
import cn.rainmonth.basicdemo.ui.floatview.view.IFloatView;
import cn.rainmonth.basicdemo.ui.floatview.view.KaDaStoryFloatView;

import static cn.rainmonth.basicdemo.ui.floatview.util.C.Position.POS_LEFT;

public class FloatViewDemoActivity extends AppCompatActivity implements KaDaStoryFloatView.FloatViewListener, View.OnClickListener {
    ConstraintLayout csMainContainer;
    TextView tvInfo;
    KaDaStoryFloatView floatContainer;
    Button btnPlay, btnPause, btnReset, btnShowFloat, btnHideFloat;
    Button btnMoveToLeft, btnMoveToRight, btnMoveToTop, btnMoveToBottom, btnMoveToCenter;
    Button btnLeftKadaAnim, btnRightKadaAnim;
    Button btnStayLeftAnim, btnStayRightAnim;
    Button btnLeftExtendAnim, btnRightExtendAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_float_view_demo);

        csMainContainer = findViewById(R.id.cs_main_container);
        tvInfo = findViewById(R.id.tv_info);

        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnReset = findViewById(R.id.btn_reset);
        btnShowFloat = findViewById(R.id.btn_show_float);
        btnHideFloat = findViewById(R.id.btn_hide_float);
        btnMoveToLeft = findViewById(R.id.btn_move_to_left);
        btnMoveToRight = findViewById(R.id.btn_move_to_right);
        btnMoveToTop = findViewById(R.id.btn_move_to_top);
        btnMoveToBottom = findViewById(R.id.btn_move_to_bottom);
        btnMoveToCenter = findViewById(R.id.btn_move_to_center);
        btnLeftKadaAnim = findViewById(R.id.btn_play_left_kada_anim);
        btnRightKadaAnim = findViewById(R.id.btn_play_right_kada_anim);
        btnStayLeftAnim = findViewById(R.id.btn_play_left_stay_anim);
        btnStayRightAnim = findViewById(R.id.btn_play_right_stay_anim);
        btnLeftExtendAnim = findViewById(R.id.btn_play_left_extend_anim);
        btnRightExtendAnim = findViewById(R.id.btn_play_right_extend_anim);


        btnShowFloat.setOnClickListener(this);
        btnHideFloat.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnMoveToLeft.setOnClickListener(this);
        btnMoveToRight.setOnClickListener(this);
        btnMoveToTop.setOnClickListener(this);
        btnMoveToBottom.setOnClickListener(this);
        btnMoveToCenter.setOnClickListener(this);
        btnLeftKadaAnim.setOnClickListener(this);
        btnRightKadaAnim.setOnClickListener(this);
        btnStayLeftAnim.setOnClickListener(this);
        btnStayRightAnim.setOnClickListener(this);
        btnLeftExtendAnim.setOnClickListener(this);
        btnRightExtendAnim.setOnClickListener(this);
    }


    private void addInnerFloatContainer() {
        if (floatContainer == null) {
            floatContainer = new KaDaStoryFloatView(this);
        }
        floatContainer.setCallback(this);
        floatContainer.setBottomStayEdge(DpUtils.dp2px(this, 50));
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(DpUtils.dp2px(this, 84), DpUtils.dp2px(this, 84));
        params.bottomToBottom = R.id.cs_main_container;
        params.startToStart = R.id.cs_main_container;
        removeInnerFloatContainer();
        csMainContainer.addView(floatContainer, params);
    }

    private void removeInnerFloatContainer() {
        if (floatContainer != null && floatContainer.isAttachedToWindow()) {
            csMainContainer.removeView(floatContainer);
            floatContainer.release();
            floatContainer = null;
        }
    }

    @Override
    public void onFloatPress() {
        toast("按下");
    }

    @Override
    public void onFloatMove() {
        tvInfo.setText(String.format("getX():%s\ngetY():%s\ntranslationX:%s", floatContainer.getX(), floatContainer.getY(), floatContainer.getTranslationX()));
    }

    @Override
    public void onFloatClick() {
        toast("点击");
    }

    public void onPlayStayToLeft(View targetView, float moveDistance, boolean isFromExtend) {
        toast("播放向左吸附动画");
    }

    public void onPlayStayToRight(View targetView, float moveDistance, boolean isFromExtend) {
        toast("播放向右吸附动画");
    }

    @Override
    public void onPlayExtendFromLeft() {
        toast("播放从左扩展动画");
    }

    @Override
    public void onPlayExtendFromRight() {
        toast("播放从右扩展动画");
    }

    @Override
    public void onPlayNormalAnim() {
        toast("播放正常动画");
    }

    private void toast(String content) {
        Toast.makeText(FloatViewDemoActivity.this, content, Toast.LENGTH_SHORT).show();
    }

    public IFloatView getRealFloatView() {
        boolean isPermission = FloatPermissionManager.getInstance().applyFloatWindow(this);
        if (isPermission || Build.VERSION.SDK_INT < 24) {
            return FloatWindowManager.getFloatWindow();
        } else {
            if (floatContainer == null) {
                addInnerFloatContainer();
            }
            // todo
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
//                if (floatContainer != null) {
//                    floatContainer.play();
//        }
                FloatWindowManager.play();
                break;
            case R.id.btn_pause:
//                if (floatContainer != null) {
//                    floatContainer.pause();
//                }
                FloatWindowManager.pause();
                break;
            case R.id.btn_reset:
//                if (floatContainer != null) {
//                    floatContainer.reset();
//                }
                FloatActionController.getInstance().stopMonkServer(this);
//                FloatActionController.getInstance().startMonkServer(this);
                break;
            case R.id.btn_show_float:
//                floatContainer.show(false);
                boolean isPermission = FloatPermissionManager.getInstance().applyFloatWindow(this);
                //有对应权限或者系统版本小于7.0
                if (isPermission || Build.VERSION.SDK_INT < 24) {
                    //开启悬浮窗
                    removeInnerFloatContainer();
                    FloatActionController.getInstance().startMonkServer(this);
                } else {
                    addInnerFloatContainer();
                    floatContainer.show(false);
                }
                break;
            case R.id.btn_hide_float:
//                if (floatContainer != null) {
//                    floatContainer.hide();
//                }
                FloatActionController.getInstance().hide();
                break;
            case R.id.btn_move_to_left:
                floatContainer.move(0, (DpUtils.getScreenHeight(this) - floatContainer.getHeight()) / 2f);
                break;
            case R.id.btn_move_to_right:
                if (floatContainer != null) {
                    floatContainer.move(DpUtils.getScreenWidth(this) - floatContainer.getWidth(), (DpUtils.getScreenHeight(this) - floatContainer.getHeight()) / 2f);
                }
                break;
            case R.id.btn_move_to_top:
                if (floatContainer != null) {
                    floatContainer.move((DpUtils.getScreenWidth(this) - floatContainer.getWidth()) / 2f, 0);
                }
                break;
            case R.id.btn_move_to_bottom:
                if (floatContainer != null) {
                    floatContainer.move((DpUtils.getScreenWidth(this) - floatContainer.getWidth()) / 2f, DpUtils.getScreenHeight(this));
                }
                break;
            case R.id.btn_move_to_center:
                if (floatContainer != null) {
                    floatContainer.move((DpUtils.getScreenWidth(this) - floatContainer.getWidth()) / 2f, (DpUtils.getScreenHeight(this) - floatContainer.getHeight()) / 2f);
                }
                break;
            case R.id.btn_play_left_kada_anim:
                if (floatContainer != null) {
                    floatContainer.playStayLeftKadaAnim();
                }
                break;
            case R.id.btn_play_right_kada_anim:
                if (floatContainer != null) {
                    floatContainer.playStayRightKadaAnim();
                }
                break;
            case R.id.btn_play_left_stay_anim:
//                if (floatContainer != null) {
//                    floatContainer.playStayToLeft(floatContainer, floatContainer.getStayMoveDistance(POS_LEFT), false);
//                }
                float leftMoveDistance = FloatWindowManager.getMoveDistance(POS_LEFT);
                FloatWindowManager.playStayToLeft(leftMoveDistance, false);
                break;
            case R.id.btn_play_right_stay_anim:
//                if (floatContainer != null) {
//                    floatContainer.playStayToRight(floatContainer, floatContainer.getStayMoveDistance(POS_RIGHT), false);
//                }

                float rightMoveDistance = FloatWindowManager.getMoveDistance(POS_LEFT);
                FloatWindowManager.playStayToRight(rightMoveDistance, false);
                break;
            case R.id.btn_play_left_extend_anim:
//                if (floatContainer != null) {
//                    if (floatContainer.getStayPosition() == POS_LEFT) {
//                        floatContainer.playExtendFromLeft(floatContainer);
//                    } else {
//                        toast("请先将目标View移动道左边");
//                    }
//                }
                FloatWindowManager.playExtendFromLeft();
                break;
            case R.id.btn_play_right_extend_anim:
//                if (floatContainer != null) {
//                    if (floatContainer.getStayPosition() == POS_RIGHT) {
//                        floatContainer.playExtendFromRight(floatContainer);
//                    } else {
//                        toast("请先将目标View移动道右边");
//                    }
//                }
                FloatWindowManager.playExtendFromRight();
                break;
        }
    }
}
