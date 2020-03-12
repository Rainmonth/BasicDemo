package cn.rainmonth.basicdemo.ui.viewpager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import cn.rainmonth.basicdemo.R;

public class ViewPagerDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager_demo);

        ViewPager vpDemo = findViewById(R.id.vp_demo);
        vpDemo.setPageMargin(80);
        vpDemo.setOffscreenPageLimit(3);
        List<Integer> list = new ArrayList<>();
        list.add(R.drawable.p02);
        list.add(R.drawable.p04);
        list.add(R.drawable.p05);
        list.add(R.drawable.p06);
        MyVpAdapter adapter = new MyVpAdapter(this, list);
        vpDemo.setAdapter(adapter);
    }

    public static class MyVpAdapter extends PagerAdapter {
        private List<Integer> list;
        private Context context;

        public MyVpAdapter(Context context, List<Integer> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ImageView iv = new ImageView(context);
            iv.setImageResource(list.get(position));
            container.addView(iv);
            return iv;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
