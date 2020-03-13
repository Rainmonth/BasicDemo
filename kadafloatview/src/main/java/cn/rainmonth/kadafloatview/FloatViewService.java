package cn.rainmonth.kadafloatview;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 *
 * @author RandyZhang
 * @date 2020/3/12 11:07 AM
 */
public class FloatViewService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
