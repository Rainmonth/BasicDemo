package cn.rainmonth.basicdemo.ui.floatview;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.rainmonth.basicdemo.R;

public class FloatViewDemoActivity extends AppCompatActivity implements KaDaFloatView.FloatCallback, View.OnClickListener {
    TextView tvInfo;
    KaDaFloatView floatContainer;
    Button btnPlay, btnPause, btnReset, btnShowFloat, btnHideFloat;
    Button btnMoveToLeft, btnMoveToRight, btnMoveToTop, btnMoveToBottom, btnMoveToCenter;
    Button btnLeftKadaAnim, btnRightKadaAnim;
    Button btnStayLeftAnim, btnStayRightAnim;
    Button btnLeftExtendAnim, btnRightExtendAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_float_view_demo);

        tvInfo = findViewById(R.id.tv_info);
        floatContainer = findViewById(R.id.float_view);

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

        floatContainer.setCallback(this);
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

        floatContainer.setBottomStayEdge(DpUtils.dp2px(this, 50));
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

    @Override
    public void onPlayStayToLeft(View targetView, float moveDistance, boolean isFromExtend) {
        toast("播放向左吸附动画");
    }

    @Override
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                floatContainer.play();
                break;
            case R.id.btn_pause:
                floatContainer.pause();
                break;
            case R.id.btn_reset:
                floatContainer.reset();
                break;
            case R.id.btn_show_float:
                floatContainer.show(false);
                break;
            case R.id.btn_hide_float:
                floatContainer.hide();
                break;
            case R.id.btn_move_to_left:
                floatContainer.move(0, (DpUtils.getScreenHeight(this) - floatContainer.getHeight()) / 2);
                break;
            case R.id.btn_move_to_right:
                floatContainer.move(DpUtils.getScreenWidth(this) - floatContainer.getWidth(), (DpUtils.getScreenHeight(this) - floatContainer.getHeight()) / 2);
                break;
            case R.id.btn_move_to_top:
                floatContainer.move((DpUtils.getScreenWidth(this) - floatContainer.getWidth()) / 2, 0);
                break;
            case R.id.btn_move_to_bottom:
                floatContainer.move((DpUtils.getScreenWidth(this) - floatContainer.getWidth()) / 2, DpUtils.getScreenHeight(this));
                break;
            case R.id.btn_move_to_center:
                floatContainer.move((DpUtils.getScreenWidth(this) - floatContainer.getWidth()) / 2, (DpUtils.getScreenHeight(this) - floatContainer.getHeight()) / 2);
                break;
            case R.id.btn_play_left_kada_anim:
                floatContainer.playStayLeftKadaAnim();
                break;
            case R.id.btn_play_right_kada_anim:
                floatContainer.playStayRightKadaAnim();
                break;
            case R.id.btn_play_left_stay_anim:
                floatContainer.playStayToLeft(floatContainer, floatContainer.getStayMoveDistance(KaDaFloatView.POS_LEFT), false);
                break;
            case R.id.btn_play_right_stay_anim:
                floatContainer.playStayToRight(floatContainer, floatContainer.getStayMoveDistance(KaDaFloatView.POS_RIGHT), false);
                break;
            case R.id.btn_play_left_extend_anim:
                if (floatContainer.getStayPosition() == KaDaFloatView.POS_LEFT) {
                    floatContainer.playExtendFromLeft(floatContainer);
                } else {
                    toast("请先将目标View移动道左边");
                }
                break;
            case R.id.btn_play_right_extend_anim:
                if (floatContainer.getStayPosition() == KaDaFloatView.POS_RIGHT) {
                    floatContainer.playExtendFromRight(floatContainer);
                } else {
                    toast("请先将目标View移动道右边");
                }
                break;
        }
    }
}
