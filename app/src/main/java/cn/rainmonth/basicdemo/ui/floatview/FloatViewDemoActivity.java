package cn.rainmonth.basicdemo.ui.floatview;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.rainmonth.basicdemo.R;

public class FloatViewDemoActivity extends AppCompatActivity implements BaseStayFloatContainer.FloatCallback {
    TextView tvInfo;
    BaseStayFloatContainer floatContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_float_view_demo);

        tvInfo = findViewById(R.id.tv_info);
        floatContainer = findViewById(R.id.tv_show_float);
        TextView tvHideFloat = findViewById(R.id.tv_hide_float);
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
    public void onPlaySnapAnimToLeft(View targetView, float currentX, float maxLeftX) {
        toast("播放向左吸附动画");
    }

    @Override
    public void onPlaySnapAnimToRight(View targetView, float currentX, float maxRightX) {
        toast("播放向右吸附动画");
    }

    @Override
    public void onPlayMiddleAnim() {
        toast("播放正常动画");
    }

    private void toast(String content) {
        Toast.makeText(FloatViewDemoActivity.this, content, Toast.LENGTH_SHORT).show();
    }

    // todo 资源释放操作
}
