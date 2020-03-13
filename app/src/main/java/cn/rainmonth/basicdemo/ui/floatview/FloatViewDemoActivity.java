package cn.rainmonth.basicdemo.ui.floatview;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import cn.rainmonth.basicdemo.R;

public class FloatViewDemoActivity extends AppCompatActivity implements BaseStayFloatContainer.FloatCallback {
    TextView tvInfo;
    BaseStayFloatContainer floatContainer;

    private ObjectAnimator rotateAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_float_view_demo);

        tvInfo = findViewById(R.id.tv_info);
        floatContainer = findViewById(R.id.tv_show_float);
        TextView tvHideFloat = findViewById(R.id.tv_hide_float);
        onFloatMove();
        floatContainer.setCallback(this);

        tvHideFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onFloatPress() {
        toast("按下");
        stopRotateAnim(floatContainer);
    }

    @Override
    public void onFloatMove() {
        tvInfo.setText(String.format("x:%s\ny:%s\ntranslationX:%s", floatContainer.getX(), floatContainer.getY(), floatContainer.getTranslationX()));
    }

    @Override
    public void onFloatClick() {
        toast("点击");
    }

    @Override
    public void onPlayLeftSnapAnim(View targetView, float currentX, float maxLeftX) {
        toast("播放向左吸附动画");
        playLeftSnapAnim(targetView, currentX, maxLeftX);
    }

    @Override
    public void onPlayRightSnapAnim(View targetView, float currentX, float maxRightX) {
        toast("播放向右吸附动画");
        playRightSnapAnim(targetView, currentX, maxRightX);
    }

    @Override
    public void onPlayMiddleAnim() {
        toast("播放正常动画");
        playMidAnim(floatContainer);
    }

    private void playLeftSnapAnim(View targetView, float currentX, float maxLeftX) {
        Log.d("FloatView", "translationX:" + targetView.getTranslationX());
        Log.d("FloatView", "currentX:" + currentX + ",maxLeftX:" + maxLeftX);
        // 因为currentX和maxLeftX一直会变，故用局部变量
        ObjectAnimator translateLeftAnim = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() - Math.abs(maxLeftX - currentX));
        translateLeftAnim.setInterpolator(new LinearInterpolator());
        translateLeftAnim.setDuration(1000);

        checkRotateAnim(targetView);

        AnimatorSet snapLeftSet = new AnimatorSet();
        snapLeftSet.play(rotateAnim).after(translateLeftAnim);
        snapLeftSet.start();
    }

    private void playRightSnapAnim(View targetView, float currentX, float maxRightX) {
        Log.d("FloatView", "translationX:" + targetView.getTranslationX());
        Log.d("FloatView", "currentX:" + currentX + ",maxRightX:" + maxRightX);
        // 因为currentX和maxRightX一直会变，故用局部变量
        ObjectAnimator translateRightAnim = ObjectAnimator.ofFloat(targetView, View.TRANSLATION_X,
                targetView.getTranslationX(), targetView.getTranslationX() + Math.abs(maxRightX - currentX));
        translateRightAnim.setInterpolator(new LinearInterpolator());
        translateRightAnim.setDuration(1000);

        checkRotateAnim(targetView);

        AnimatorSet snapRightSet = new AnimatorSet();
        snapRightSet.play(rotateAnim).after(translateRightAnim);
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

    private void stopRotateAnim(View targetView) {
        checkRotateAnim(targetView);
        rotateAnim.pause();
    }

    private void toast(String content) {
        Toast.makeText(FloatViewDemoActivity.this, content, Toast.LENGTH_SHORT).show();
    }

    // todo 资源释放操作
}
