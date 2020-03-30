package cn.rainmonth.basicdemo.ui.floatview.view;

import android.view.View;

import cn.rainmonth.basicdemo.ui.floatview.util.C;

public interface IFloatView {
    /**
     * 播放
     */
    void play();

    /**
     * 暂停
     */
    void pause();

    /**
     * 显示
     *
     * @param isPlay 是否在播放
     */
    void show(boolean isPlay);

    /**
     * 隐藏
     */
    void hide();

    /**
     * 移动
     *
     * @param desX x坐标
     * @param dexY y坐标
     */
    void move(float desX, float dexY);

    /**
     * 重置
     */
    void reset();

    /**
     * 资源释放
     */
    void release();

    /**
     * 获取停留的位置
     *
     * @return {@link C.Position#POS_LEFT,C.Position#POS_RIGHT}, etc
     */
    int getStayPosition();

    /**
     * 是否需要停靠
     *
     * @return 如果需要停靠返回true
     */
    boolean isNeedStay();

    /**
     * 停到左侧动画
     *
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否来自扩展动画
     */
    void playStayToLeft(float moveDistance, boolean isPlayFromExtend);

    /**
     * 停到右侧动画
     *
     * @param moveDistance     移动距离
     * @param isPlayFromExtend 是否来自扩展动画
     */
    void playStayToRight(float moveDistance, boolean isPlayFromExtend);

    /**
     * 从左侧扩展动画
     */
    void playExtendFromLeft();

    /**
     * 扩展后是否需要停靠到左边
     *
     * @return 需要时返回true
     */
    boolean isNeedStayBackToLeft();

    /**
     * 展开后是否需要停靠到右边
     *
     * @return 需要时返回true
     */
    boolean isNeedStayBackToRight();

    /**
     * 从右侧扩展动画
     */
    void playExtendFromRight();

}
