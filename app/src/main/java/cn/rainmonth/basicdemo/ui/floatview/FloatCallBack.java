package cn.rainmonth.basicdemo.ui.floatview;


/**
 * Author:xishuang
 * Date:2017.08.01
 * Des:暴露一些与悬浮窗交互的接口
 */
public interface FloatCallBack {

    void play();

    void pause();

    void show();

    void hide();

    void playExtendFromLeft();

    void playExtendFromRight();

    void playStayToLeft(float moveDistance, boolean isPlayFromExtend);

    void playStayToRight(float moveDistance, boolean isPlayFromExtend);

}
