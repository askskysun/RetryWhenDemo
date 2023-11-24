package com.hero.retrywhendo;

import androidx.lifecycle.LifecycleOwner;

import com.hero.retrywhendo.interfaces.FinalCallBack;
import com.hero.retrywhendo.interfaces.OperationCallBack;
import com.hero.retrywhendo.interfaces.OnDoOperationListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 配置
 */
public class Builder<T> {

    /**
     * 是否调试
     */
    private boolean isDebug;

    /**
     * 操作所携带的参数
     */
    private T t;

    /**
     * 重试结束，最终的回调
     */
    private FinalCallBack finalOperationCallBack;

    /**
     * 操作暴露的接口
     */
    private OnDoOperationListener onDoOperationListener;

    /**
     * 延迟执行时间，默认为0，不延迟
     */
    private long delay;

    /**
     * 重试列表，即每次重试相隔的时间  默认3秒重试一次
     */
    private List<Integer> delayTimeList = Arrays.asList(3);

    /**
     * 单位
     */
    private TimeUnit unit = TimeUnit.SECONDS;

    /**
     * 执行线程 默认io线程
     */
    private Scheduler subscribeOnScheduler = Schedulers.io();

    /**
     * 回调线程  默认主线程
     */
    private Scheduler observeOnScheduler = AndroidSchedulers.mainThread();

    /**
     * 使用AutoDispose 防止内存泄漏
     */
    private LifecycleOwner owner;

    public long getDelay() {
        return delay;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public T getT() {
        return t;
    }

    public FinalCallBack getFinalCallBack() {
        return finalOperationCallBack;
    }

    public OnDoOperationListener getOnDoOperationListener() {
        return onDoOperationListener;
    }

    public List<Integer> getDelayTimeList() {
        return delayTimeList;
    }

    public Scheduler getSubscribeOnScheduler() {
        return subscribeOnScheduler;
    }

    public Scheduler getObserveOnScheduler() {
        return observeOnScheduler;
    }

    public LifecycleOwner getOwner() {
        return owner;
    }

    public Builder setIsDebug(boolean isDebug) {
        this.isDebug = isDebug;
        return this;
    }

    public Builder setSubscribeOnScheduler(Scheduler subscribeOnScheduler) {
        if (subscribeOnScheduler != null) {
            this.subscribeOnScheduler = subscribeOnScheduler;
        }
        return this;
    }

    public Builder setObserveOnScheduler(Scheduler observeOnScheduler) {
        if (observeOnScheduler != null) {
            this.observeOnScheduler = observeOnScheduler;
        }
        return this;
    }

    /**
     * 内部已有判断  可以传空值
     *
     * @param owner
     * @return
     */
    public Builder setOwner(LifecycleOwner owner) {
        this.owner = owner;
        return this;
    }

    /**
     * 传入参数
     *
     * @param t
     * @return
     */
    public Builder setParam(T t) {
        this.t = t;
        return this;
    }

    /**
     * 延迟执行时间，默认为0，不延迟
     */
    public Builder setDelay(long delay) {
        if (delay > 0) {
            this.delay = delay;
        }
        return this;
    }

    public Builder setDelayTimeList(List<Integer> delayTimeList) {
        if (delayTimeList != null && !delayTimeList.isEmpty()) {
            this.delayTimeList = delayTimeList;
        }
        return this;
    }

    public Builder setUnit(TimeUnit unit) {
        this.unit = unit;
        return this;
    }

    public Builder setFinalCallBack(FinalCallBack finalOperationCallBack) {
        this.finalOperationCallBack = finalOperationCallBack;
        return this;
    }

    public Builder setOnDoOperationListener(OnDoOperationListener onDoOperationListener) {
        this.onDoOperationListener = onDoOperationListener;
        return this;
    }

    public RetryWhenDoOperationHelper build() {
        return new RetryWhenDoOperationHelper(this);
    }

    @Override
    public String toString() {
        return "Builder{" +
                "t=" + JsonUtils.javabeanToJson(t) +
                ", finalCallBack=" + finalOperationCallBack +
                ", onDoOperationListener=" + onDoOperationListener +
                ", delayTimeList=" + delayTimeList.toString() +
                ", subscribeOnScheduler=" + subscribeOnScheduler +
                ", observeOnScheduler=" + observeOnScheduler +
                ", owner=" + owner +
                '}';
    }
}
