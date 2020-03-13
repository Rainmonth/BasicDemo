package cn.rainmonth.basicdemo;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import cn.rainmonth.basicdemo.ui.floatview.FloatViewDemoActivity;
import cn.rainmonth.basicdemo.ui.material.ScrollingActivity;
import cn.rainmonth.basicdemo.ui.material.TabbedActivity;
import cn.rainmonth.basicdemo.ui.viewpager.ViewPagerDemoActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        TextView tvScroll = findViewById(R.id.tv_scroll);
        TextView tvTabbed = findViewById(R.id.tv_tabbed);
        TextView tvViewPager = findViewById(R.id.tv_view_pager);
        TextView tvFloatView = findViewById(R.id.tv_float_view);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        tvScroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(ScrollingActivity.class);
            }
        });

        tvTabbed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(TabbedActivity.class);
            }
        });

        tvViewPager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(ViewPagerDemoActivity.class);
            }
        });

        tvFloatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(FloatViewDemoActivity.class);
            }
        });
    }

    private void go(Class clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
